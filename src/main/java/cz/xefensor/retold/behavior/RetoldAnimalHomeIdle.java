package cz.xefensor.retold.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;

final class RetoldAnimalHomeIdle {
    private static final int CROWD_MOVE_INTERVAL_TICKS = 20 * 7;
    private static final int CROWD_SCAN_CACHE_TICKS = 8;
    private static final int HOME_IDLE_PATH_INTERVAL_TICKS = 12;
    private static final double DEFAULT_CROWD_RADIUS_BLOCKS = 1.45D;

    private RetoldAnimalHomeIdle() {
    }

    static boolean shouldIdleAtHome(
            PathfinderMob mob,
            RetoldAnimalHomeMemory home,
            RetoldAiControlMode mode,
            RetoldAiControlOwner owner,
            RetoldAiControlOwner controlOwner,
            String reason,
            double distanceSquared,
            double idleRadiusSquared,
            long gameTime
    ) {
        if (home == null || distanceSquared > idleRadiusSquared) {
            return false;
        }

        if (RetoldBehaviorCoordinator.hasLiveTarget(mob)) {
            return false;
        }

        RetoldMobState state = RetoldMobStates.getOrCreate(
                mob,
                gameTime
        );

        if (RetoldMobRules.hasHuntDrive(mob, state)) {
            return false;
        }

        if (mode == RetoldAiControlMode.NONE) {
            return true;
        }

        return RetoldAiControl.isControlledAsByWithReason(
                mob,
                RetoldAiControlMode.REGROUP,
                controlOwner,
                reason
        );
    }

    static void idleAtHome(
            ServerLevel level,
            PathfinderMob mob,
            RetoldAnimalHomeMemory home,
            long gameTime,
            String reason,
            int priority,
            int controlTicks,
            double closeDistanceSquared,
            double returnSpeed,
            double strollSpeed,
            double minStrollBlocks,
            double extraStrollBlocks,
            int idleMoveIntervalTicks,
            boolean resting
    ) {
        if (!RetoldAiControl.tryClaim(
                mob,
                RetoldAiControlMode.REGROUP,
                RetoldAiControlOwner.REGROUP,
                priority,
                reason,
                gameTime,
                controlTicks
        )) {
            return;
        }

        RetoldAnimalHomes.markUsed(
                mob,
                gameTime
        );

        double distanceSquared = mob.blockPosition().distSqr(home.pos());

        if (distanceSquared > closeDistanceSquared) {
            RetoldHomeRestAnimations.stopResting(mob);
            moveToHome(
                    mob,
                    home.pos(),
                    returnSpeed,
                    gameTime
            );
            return;
        }

        if (
                shouldMoveAtHome(
                        level,
                        mob,
                        home,
                        gameTime,
                        idleMoveIntervalTicks
                )
        ) {
            RetoldHomeRestAnimations.stopResting(mob);
            moveToRandomHomePoint(
                    mob,
                    home.pos(),
                    strollSpeed,
                    minStrollBlocks,
                    extraStrollBlocks,
                    gameTime
            );
            return;
        }

        mob.setSprinting(false);
        mob.getNavigation().stop();

        if (resting) {
            RetoldHomeRestAnimations.startResting(mob);
        } else {
            RetoldHomeRestAnimations.stopResting(mob);
        }
    }

    private static boolean shouldMoveAtHome(
            ServerLevel level,
            PathfinderMob mob,
            RetoldAnimalHomeMemory home,
            long gameTime,
            int idleMoveIntervalTicks
    ) {
        if (
                isCrowdedAtHome(level, mob, home)
                        && (gameTime + mob.getId()) % CROWD_MOVE_INTERVAL_TICKS == 0L
        ) {
            return true;
        }

        return idleMoveIntervalTicks > 0
                && (gameTime + mob.getId()) % idleMoveIntervalTicks == 0L;
    }

    private static void moveToHome(
            PathfinderMob mob,
            BlockPos homePos,
            double speed,
            long gameTime
    ) {
        mob.setSprinting(false);

        RetoldBehaviorMovement.throttledMoveTo(
                mob,
                homePos,
                speed,
                gameTime,
                HOME_IDLE_PATH_INTERVAL_TICKS,
                2.0D * 2.0D
        );
    }

    private static void moveToRandomHomePoint(
            PathfinderMob mob,
            BlockPos homePos,
            double speed,
            double minStrollBlocks,
            double extraStrollBlocks,
            long gameTime
    ) {
        double angle = mob.getRandom().nextDouble() * Math.PI * 2.0D;
        double distance = minStrollBlocks
                + mob.getRandom().nextDouble() * extraStrollBlocks;
        double x = homePos.getX() + 0.5D + Math.cos(angle) * distance;
        double z = homePos.getZ() + 0.5D + Math.sin(angle) * distance;

        mob.setSprinting(false);

        RetoldBehaviorMovement.throttledMoveTo(
                mob,
                x,
                homePos.getY(),
                z,
                speed,
                gameTime,
                HOME_IDLE_PATH_INTERVAL_TICKS,
                1.5D * 1.5D
        );
    }

    private static boolean isCrowdedAtHome(
            ServerLevel level,
            PathfinderMob mob,
            RetoldAnimalHomeMemory home
    ) {
        double radius = crowdRadiusBlocks(mob);
        double radiusSquared = radius * radius;

        for (PathfinderMob candidate : RetoldAiScanCache.nearby(
                level,
                mob,
                PathfinderMob.class,
                radius,
                level.getGameTime(),
                CROWD_SCAN_CACHE_TICKS
        )) {
            if (
                    candidate != mob
                            && RetoldAnimalHomes.hasSameValidHomeAs(level, candidate, home)
                            && RetoldAnimalSocialGroups.canShareHomeOrRange(mob, candidate)
                            && mob.distanceToSqr(candidate) <= radiusSquared
            ) {
                return true;
            }
        }

        return false;
    }

    private static double crowdRadiusBlocks(PathfinderMob mob) {
        String path = RetoldMobRules.getEntityTypePath(
                mob.getType()
        );

        return switch (path) {
            case "horse", "donkey", "mule", "camel" -> 2.2D;
            case "cow", "llama", "trader_llama" -> 1.9D;
            case "sheep", "goat", "pig" -> 1.55D;
            case "chicken", "rabbit", "fox", "cat", "ocelot" -> 1.15D;
            default -> DEFAULT_CROWD_RADIUS_BLOCKS;
        };
    }
}
