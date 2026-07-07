package cz.xefensor.retold.behavior;

import cz.xefensor.retold.combat.RetoldFactionTargetGuards;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public final class RetoldVanillaAiBlockerEvents {
    private static final int CLEANUP_INTERVAL_TICKS = 2;

    private RetoldVanillaAiBlockerEvents() {
    }

    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof PathfinderMob mob)) {
            return;
        }

        if (!(mob.level() instanceof ServerLevel level)) {
            return;
        }

        long gameTime = level.getGameTime();

        if (!shouldRun(mob, gameTime)) {
            return;
        }

        LivingEntity target = mob.getTarget();

        if (
                target != null
                        && RetoldMobRules.shouldBlockVanillaPredatorTarget(mob, target)
        ) {
            RetoldFactionTargetGuards.setTargetIgnoringGuard(
                    mob,
                    null
            );

            RetoldFactionTargetGuards.setAggressiveIgnoringGuard(
                    mob,
                    false
            );

            mob.getNavigation().stop();
        }
    }

    private static boolean shouldRun(
            PathfinderMob mob,
            long gameTime
    ) {
        int offset = Math.floorMod(
                mob.getId(),
                CLEANUP_INTERVAL_TICKS
        );

        return (gameTime + offset) % CLEANUP_INTERVAL_TICKS == 0L;
    }
}