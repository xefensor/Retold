package cz.xefensor.retold.behavior.pack;

import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.control.RetoldAiControlMode;
import cz.xefensor.retold.behavior.control.RetoldAiControlOwner;
import cz.xefensor.retold.behavior.control.RetoldAiPriorities;
import cz.xefensor.retold.behavior.performance.RetoldAiScanCache;
import cz.xefensor.retold.behavior.home.RetoldAnimalHomeMemory;
import cz.xefensor.retold.behavior.home.RetoldAnimalHomeType;
import cz.xefensor.retold.behavior.home.RetoldAnimalHomes;
import cz.xefensor.retold.behavior.core.RetoldBehaviorCoordinator;
import cz.xefensor.retold.behavior.core.RetoldBehaviorMovement;
import cz.xefensor.retold.behavior.home.RetoldHomeRestAnimations;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;
import cz.xefensor.retold.behavior.profiles.RetoldMobState;
import cz.xefensor.retold.behavior.profiles.RetoldMobStates;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;

final class RetoldWolfDenIdle {
    private static final String REASON_DEN_IDLE = "den_idle";

    private static final int HOME_IDLE_CONTROL_TICKS = 20 * 5;
    private static final int HOME_IDLE_PRIORITY = RetoldAiPriorities.HOME_IDLE;
    private static final int HOME_IDLE_MOVE_INTERVAL_TICKS = 20 * 18;
    private static final int HOME_IDLE_CROWD_MOVE_INTERVAL_TICKS = 20 * 7;
    private static final int DEN_CROWD_SCAN_CACHE_TICKS = 8;
    private static final int DEN_IDLE_PATH_INTERVAL_TICKS = 12;

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

            RetoldBehaviorMovement.throttledMoveTo(
                    mob,
                    homePos,
                    WOLF_HOME_RETURN_SPEED,
                    gameTime,
                    DEN_IDLE_PATH_INTERVAL_TICKS,
                    2.0D * 2.0D
            );
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
                    homePos,
                    gameTime
            );
            return;
        }

        mob.getNavigation().stop();
        RetoldHomeRestAnimations.startResting(mob);
    }

    private static void moveToRandomDenPoint(
            PathfinderMob mob,
            BlockPos homePos,
            long gameTime
    ) {
        double angle = mob.getRandom().nextDouble() * Math.PI * 2.0D;
        double distance = WOLF_DEN_IDLE_MIN_STROLL_BLOCKS
                + mob.getRandom().nextDouble() * WOLF_DEN_IDLE_EXTRA_STROLL_BLOCKS;
        double x = homePos.getX() + 0.5D + Math.cos(angle) * distance;
        double z = homePos.getZ() + 0.5D + Math.sin(angle) * distance;

        RetoldBehaviorMovement.throttledMoveTo(
                mob,
                x,
                homePos.getY(),
                z,
                WOLF_DEN_IDLE_STROLL_SPEED,
                gameTime,
                DEN_IDLE_PATH_INTERVAL_TICKS,
                1.5D * 1.5D
        );
    }

    private static boolean isCrowdedAtDen(PathfinderMob mob) {
        if (!(mob.level() instanceof ServerLevel level)) {
            return false;
        }

        for (PathfinderMob candidate : RetoldAiScanCache.nearby(
                level,
                mob,
                PathfinderMob.class,
                WOLF_DEN_CROWD_RADIUS_BLOCKS,
                level.getGameTime(),
                DEN_CROWD_SCAN_CACHE_TICKS
        )) {
            if (
                    candidate != mob
                            && isWolf(candidate)
                            && candidate.isAlive()
                            && !candidate.isRemoved()
                            && mob.distanceToSqr(candidate) <= WOLF_DEN_CROWD_RADIUS_SQUARED
            ) {
                return true;
            }
        }

        return false;
    }

    private static boolean isWolf(PathfinderMob mob) {
        return RetoldMobRules.isWolf(mob);
    }
}
