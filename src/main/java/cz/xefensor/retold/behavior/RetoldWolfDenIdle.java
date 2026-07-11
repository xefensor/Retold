package cz.xefensor.retold.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.AABB;

import java.util.List;

final class RetoldWolfDenIdle {
    private static final String REASON_DEN_IDLE = "den_idle";

    private static final int HOME_IDLE_CONTROL_TICKS = 20 * 5;
    private static final int HOME_IDLE_PRIORITY = 10;
    private static final int HOME_IDLE_MOVE_INTERVAL_TICKS = 20 * 18;
    private static final int HOME_IDLE_CROWD_MOVE_INTERVAL_TICKS = 20 * 7;

    private static final double HOME_CLOSE_DISTANCE_BLOCKS = 5.0D;
    private static final double HOME_CLOSE_DISTANCE_SQUARED =
            HOME_CLOSE_DISTANCE_BLOCKS * HOME_CLOSE_DISTANCE_BLOCKS;

    private static final double WOLF_DEN_IDLE_RADIUS_BLOCKS = 8.0D;
    private static final double WOLF_DEN_IDLE_RADIUS_SQUARED =
            WOLF_DEN_IDLE_RADIUS_BLOCKS * WOLF_DEN_IDLE_RADIUS_BLOCKS;
    private static final double WOLF_DEN_IDLE_MIN_STROLL_BLOCKS = 2.0D;
    private static final double WOLF_DEN_IDLE_EXTRA_STROLL_BLOCKS = 3.0D;
    private static final double WOLF_DEN_CROWD_RADIUS_BLOCKS = 1.45D;
    private static final double WOLF_DEN_CROWD_RADIUS_SQUARED =
            WOLF_DEN_CROWD_RADIUS_BLOCKS * WOLF_DEN_CROWD_RADIUS_BLOCKS;

    private static final double WOLF_HOME_RETURN_SPEED = 0.82D;
    private static final double WOLF_DEN_IDLE_STROLL_SPEED = 0.48D;

    private RetoldWolfDenIdle() {
    }

    static boolean shouldIdleAtHome(
            PathfinderMob mob,
            RetoldAnimalHomeMemory home,
            RetoldAiControlMode mode,
            RetoldAiControlOwner owner,
            double distanceSquared,
            long gameTime
    ) {
        if (home.type() != RetoldAnimalHomeType.WOLF_DEN) {
            return false;
        }

        if (distanceSquared > WOLF_DEN_IDLE_RADIUS_SQUARED) {
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
                RetoldAiControlOwner.REGROUP,
                REASON_DEN_IDLE
        );
    }

    static void idleAtHome(
            PathfinderMob mob,
            BlockPos homePos,
            double distanceSquared,
            long gameTime
    ) {
        if (!RetoldAiControl.tryClaim(
                mob,
                RetoldAiControlMode.REGROUP,
                RetoldAiControlOwner.REGROUP,
                HOME_IDLE_PRIORITY,
                REASON_DEN_IDLE,
                gameTime,
                HOME_IDLE_CONTROL_TICKS
        )) {
            return;
        }

        RetoldPackHomeControl.clearCombatState(mob);

        RetoldAnimalHomes.markUsed(
                mob,
                gameTime
        );

        if (distanceSquared > HOME_CLOSE_DISTANCE_SQUARED) {
            RetoldHomeRestAnimations.stopResting(mob);

            RetoldAiControl.withNavigationBypass(() -> {
                mob.getNavigation().moveTo(
                        homePos.getX() + 0.5D,
                        homePos.getY(),
                        homePos.getZ() + 0.5D,
                        WOLF_HOME_RETURN_SPEED
                );
            });
            return;
        }

        if (
                (
                        isCrowdedAtDen(mob)
                                && (gameTime + mob.getId()) % HOME_IDLE_CROWD_MOVE_INTERVAL_TICKS == 0L
                )
                        || (gameTime + mob.getId()) % HOME_IDLE_MOVE_INTERVAL_TICKS == 0L
        ) {
            RetoldHomeRestAnimations.stopResting(mob);

            moveToRandomDenPoint(
                    mob,
                    homePos
            );
            return;
        }

        mob.getNavigation().stop();
        RetoldHomeRestAnimations.startResting(mob);
    }

    private static void moveToRandomDenPoint(
            PathfinderMob mob,
            BlockPos homePos
    ) {
        double angle = mob.getRandom().nextDouble() * Math.PI * 2.0D;
        double distance = WOLF_DEN_IDLE_MIN_STROLL_BLOCKS
                + mob.getRandom().nextDouble() * WOLF_DEN_IDLE_EXTRA_STROLL_BLOCKS;
        double x = homePos.getX() + 0.5D + Math.cos(angle) * distance;
        double z = homePos.getZ() + 0.5D + Math.sin(angle) * distance;

        RetoldAiControl.withNavigationBypass(() -> {
            mob.getNavigation().moveTo(
                    x,
                    homePos.getY(),
                    z,
                    WOLF_DEN_IDLE_STROLL_SPEED
            );
        });
    }

    private static boolean isCrowdedAtDen(PathfinderMob mob) {
        if (!(mob.level() instanceof ServerLevel level)) {
            return false;
        }

        AABB area = mob.getBoundingBox().inflate(WOLF_DEN_CROWD_RADIUS_BLOCKS);

        List<PathfinderMob> nearbyWolves = level.getEntitiesOfClass(
                PathfinderMob.class,
                area,
                candidate -> candidate != mob
                        && isWolf(candidate)
                        && candidate.isAlive()
                        && !candidate.isRemoved()
                        && mob.distanceToSqr(candidate) <= WOLF_DEN_CROWD_RADIUS_SQUARED
        );

        return !nearbyWolves.isEmpty();
    }

    private static boolean isWolf(PathfinderMob mob) {
        return RetoldMobRules.isEntityPath(
                mob,
                "wolf"
        );
    }
}
