package cz.xefensor.retold.behavior.control;

import cz.xefensor.retold.behavior.core.RetoldBehaviorTargets;
import cz.xefensor.retold.behavior.core.RetoldBehaviorTiming;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;

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

        if (!RetoldMobRules.canUseOrdinaryPredatorSystems(mob)) {
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
            RetoldBehaviorTargets.clearTargetAndAggression(mob, target, true);
        }
    }

    private static boolean shouldRun(
            PathfinderMob mob,
            long gameTime
    ) {
        return RetoldBehaviorTiming.shouldThink(
                mob,
                gameTime,
                CLEANUP_INTERVAL_TICKS
        );
    }
}
