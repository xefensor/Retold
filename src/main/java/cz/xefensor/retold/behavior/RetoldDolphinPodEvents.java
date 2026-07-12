package cz.xefensor.retold.behavior;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.List;

public final class RetoldDolphinPodEvents {
    private static final int THINK_INTERVAL_TICKS = 10;
    private static final int POD_SCAN_CACHE_TICKS = 5;
    private static final int POD_PATH_INTERVAL_TICKS = 6;
    private static final int POD_HUNT_CONTROL_TICKS = 20 * 4;
    private static final int POD_HUNT_PRIORITY = RetoldAiPriorities.above(RetoldAiPriorities.HUNT, 1);

    private static final double POD_SHARE_RADIUS_BLOCKS = 28.0D;
    private static final double POD_SHARE_RADIUS_SQUARED =
            POD_SHARE_RADIUS_BLOCKS * POD_SHARE_RADIUS_BLOCKS;

    private static final double POD_HUNT_SPEED = 1.34D;

    private RetoldDolphinPodEvents() {
    }

    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof PathfinderMob dolphin)) {
            return;
        }

        if (!(dolphin.level() instanceof ServerLevel level)) {
            return;
        }

        if (!isDolphin(dolphin)) {
            return;
        }

        long gameTime = level.getGameTime();

        if (!shouldThink(dolphin, gameTime)) {
            return;
        }

        LivingEntity target = dolphin.getTarget();

        if (isValidPodPrey(dolphin, target, gameTime)) {
            sharePodTarget(
                    level,
                    dolphin,
                    target,
                    gameTime
            );
            return;
        }

        LivingEntity sharedTarget = findSharedPodTarget(
                level,
                dolphin,
                gameTime
        );

        if (sharedTarget == null || !canJoinPodHunt(dolphin, gameTime)) {
            return;
        }

        joinPodHunt(
                dolphin,
                sharedTarget,
                gameTime
        );
    }

    private static boolean isDolphin(PathfinderMob mob) {
        return RetoldMobRules.isDolphin(mob);
    }

    private static boolean shouldThink(
            PathfinderMob dolphin,
            long gameTime
    ) {
        return RetoldBehaviorTiming.shouldThink(
                dolphin,
                gameTime,
                THINK_INTERVAL_TICKS
        );
    }

    private static LivingEntity findSharedPodTarget(
            ServerLevel level,
            PathfinderMob dolphin,
            long gameTime
    ) {
        List<PathfinderMob> sources = RetoldAiScanCache.nearby(
                level,
                dolphin,
                PathfinderMob.class,
                POD_SHARE_RADIUS_BLOCKS,
                gameTime,
                POD_SCAN_CACHE_TICKS
        );

        LivingEntity bestTarget = null;
        double bestScore = Double.MAX_VALUE;

        for (PathfinderMob source : sources) {
            if (!isValidPodSource(dolphin, source, gameTime)) {
                continue;
            }

            LivingEntity target = source.getTarget();

            if (!isValidPodPrey(dolphin, target, gameTime)) {
                continue;
            }

            double score = dolphin.distanceToSqr(source);

            if (RetoldAiSightCache.canSee(source, target, gameTime)) {
                score -= 20.0D;
            }

            if (RetoldAiSightCache.canSee(dolphin, source, gameTime)) {
                score -= 8.0D;
            }

            if (score < bestScore) {
                bestScore = score;
                bestTarget = target;
            }
        }

        return bestTarget;
    }

    private static void sharePodTarget(
            ServerLevel level,
            PathfinderMob source,
            LivingEntity target,
            long gameTime
    ) {
        for (PathfinderMob recruit : RetoldAiScanCache.nearby(
                level,
                source,
                PathfinderMob.class,
                POD_SHARE_RADIUS_BLOCKS,
                gameTime,
                POD_SCAN_CACHE_TICKS
        )) {
            if (!isValidPodRecruit(source, recruit, gameTime)) {
                continue;
            }

            joinPodHunt(
                    recruit,
                    target,
                    gameTime
            );
        }
    }

    private static boolean isValidPodSource(
            PathfinderMob dolphin,
            PathfinderMob source,
            long gameTime
    ) {
        if (source == null || source == dolphin) {
            return false;
        }

        if (!isDolphin(source)) {
            return false;
        }

        if (!RetoldBehaviorCoordinator.isAliveInSameLevel(dolphin, source)) {
            return false;
        }

        if (dolphin.distanceToSqr(source) > POD_SHARE_RADIUS_SQUARED) {
            return false;
        }

        return isValidPodPrey(
                dolphin,
                source.getTarget(),
                gameTime
        );
    }

    private static boolean isValidPodRecruit(
            PathfinderMob source,
            PathfinderMob recruit,
            long gameTime
    ) {
        if (recruit == null || recruit == source) {
            return false;
        }

        if (!isDolphin(recruit)) {
            return false;
        }

        if (!RetoldBehaviorCoordinator.isAliveInSameLevel(source, recruit)) {
            return false;
        }

        if (source.distanceToSqr(recruit) > POD_SHARE_RADIUS_SQUARED) {
            return false;
        }

        return canJoinPodHunt(
                recruit,
                gameTime
        );
    }

    private static boolean canJoinPodHunt(
            PathfinderMob dolphin,
            long gameTime
    ) {
        if (RetoldBehaviorCoordinator.hasLiveTarget(dolphin)) {
            return false;
        }

        RetoldAiControlMode mode = RetoldAiControl.getMode(dolphin);

        if (
                mode != RetoldAiControlMode.NONE
                        && !RetoldAiControl.isControlledAsBy(
                        dolphin,
                        RetoldAiControlMode.HUNT,
                        RetoldAiControlOwner.AQUATIC_POD
                )
        ) {
            return false;
        }

        RetoldMobState state = RetoldMobStates.getOrCreate(
                dolphin,
                gameTime
        );

        return RetoldMobRules.hasHuntDrive(
                dolphin,
                state
        );
    }

    private static boolean isValidPodPrey(
            PathfinderMob dolphin,
            LivingEntity target,
            long gameTime
    ) {
        return RetoldPreyTargeting.isValidMobRulePrey(
                dolphin,
                target,
                gameTime
        );
    }

    private static void joinPodHunt(
            PathfinderMob dolphin,
            LivingEntity target,
            long gameTime
    ) {
        if (!RetoldAiControl.tryClaim(
                dolphin,
                RetoldAiControlMode.HUNT,
                RetoldAiControlOwner.AQUATIC_POD,
                POD_HUNT_PRIORITY,
                "dolphin_pod_hunt",
                gameTime,
                POD_HUNT_CONTROL_TICKS
        )) {
            return;
        }

        if (!RetoldBehaviorTargets.setAttackTargetOrClearOwner(
                dolphin,
                target,
                RetoldAiControlOwner.AQUATIC_POD
        )) {
            return;
        }

        RetoldBehaviorMovement.throttledMoveTo(
                dolphin,
                target,
                POD_HUNT_SPEED,
                gameTime,
                POD_PATH_INTERVAL_TICKS,
                2.0D * 2.0D
        );
    }
}
