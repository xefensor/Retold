package cz.xefensor.retold.behavior;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.List;

public final class RetoldDolphinPodEvents {
    private static final int THINK_INTERVAL_TICKS = 10;
    private static final int POD_HUNT_CONTROL_TICKS = 20 * 4;
    private static final int POD_HUNT_PRIORITY = 46;

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
        return RetoldMobRules.isEntityPath(
                mob,
                "dolphin"
        );
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
        AABB area = dolphin.getBoundingBox().inflate(POD_SHARE_RADIUS_BLOCKS);

        List<PathfinderMob> sources = level.getEntitiesOfClass(
                PathfinderMob.class,
                area,
                source -> isValidPodSource(
                        dolphin,
                        source,
                        gameTime
                )
        );

        LivingEntity bestTarget = null;
        double bestScore = Double.MAX_VALUE;

        for (PathfinderMob source : sources) {
            LivingEntity target = source.getTarget();

            if (!isValidPodPrey(dolphin, target, gameTime)) {
                continue;
            }

            double score = dolphin.distanceToSqr(source);

            if (source.hasLineOfSight(target)) {
                score -= 20.0D;
            }

            if (dolphin.hasLineOfSight(source)) {
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
        AABB area = source.getBoundingBox().inflate(POD_SHARE_RADIUS_BLOCKS);

        for (PathfinderMob recruit : level.getEntitiesOfClass(
                PathfinderMob.class,
                area,
                candidate -> isValidPodRecruit(
                        source,
                        candidate,
                        gameTime
                )
        )) {
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

        RetoldBehaviorTargets.setTargetAndAggression(dolphin, target, true);

        RetoldAiControl.withNavigationBypass(() -> {
            dolphin.getNavigation().moveTo(
                    target,
                    POD_HUNT_SPEED
            );
        });
    }
}
