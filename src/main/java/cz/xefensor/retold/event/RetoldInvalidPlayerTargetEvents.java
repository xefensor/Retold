package cz.xefensor.retold.event;

import cz.xefensor.retold.behavior.RetoldAiControl;
import cz.xefensor.retold.combat.RetoldAiTargets;
import cz.xefensor.retold.combat.RetoldCombatTargets;
import cz.xefensor.retold.combat.RetoldFactionTargetMemory;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public final class RetoldInvalidPlayerTargetEvents {
    private RetoldInvalidPlayerTargetEvents() {
    }

    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }

        if (!(mob.level() instanceof ServerLevel level)) {
            return;
        }

        RetoldFactionTargetMemory.cleanupTargetState(mob);

        boolean cleared = clearInvalidMobTarget(mob);
        cleared = clearInvalidBrainTarget(level, mob) || cleared;

        if (!cleared) {
            return;
        }

        RetoldAiControl.clear(mob);
        RetoldAiTargets.setAggression(mob, false);

        if (mob instanceof PathfinderMob pathfinderMob) {
            pathfinderMob.getNavigation().stop();
        }
    }

    private static boolean clearInvalidMobTarget(Mob mob) {
        LivingEntity target = mob.getTarget();

        if (!RetoldAiTargets.isInvalidPlayerTarget(target)) {
            return false;
        }

        RetoldCombatTargets.clearTargetReferencesAndAggression(
                mob,
                target,
                false
        );

        return true;
    }

    private static boolean clearInvalidBrainTarget(
            ServerLevel level,
            Mob mob
    ) {
        LivingEntity attackTarget = RetoldAiTargets.getBrainAttackTargetSafely(mob);

        if (RetoldAiTargets.isInvalidPlayerTarget(attackTarget)) {
            RetoldCombatTargets.clearTargetReferencesAndAggression(
                    mob,
                    attackTarget,
                    false
            );
            RetoldAiTargets.eraseMemorySafely(
                    mob,
                    MemoryModuleType.ATTACK_TARGET
            );
            RetoldAiTargets.eraseMemorySafely(
                    mob,
                    MemoryModuleType.ANGRY_AT
            );
            return true;
        }

        Entity angryEntity = RetoldAiTargets.resolveAngryAt(level, mob);

        if (!RetoldAiTargets.isInvalidPlayerTarget(angryEntity)) {
            return false;
        }

        RetoldAiTargets.eraseMemorySafely(
                mob,
                MemoryModuleType.ANGRY_AT
        );
        return true;
    }
}
