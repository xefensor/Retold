package cz.xefensor.retold.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;

final class RetoldPackHomeReturn {
    private static final RetoldAiControlOwner CONTROL_OWNER = RetoldAiControlOwner.REGROUP;
    private static final String REASON_RETURN_HOME = "return_home";

    private static final int HOME_RETURN_CONTROL_TICKS = 20 * 5;
    private static final int HOME_RETURN_PATH_INTERVAL_TICKS = 12;

    private static final double HOME_CLOSE_DISTANCE_BLOCKS = 5.0D;
    private static final double HOME_CLOSE_DISTANCE_SQUARED =
            HOME_CLOSE_DISTANCE_BLOCKS * HOME_CLOSE_DISTANCE_BLOCKS;

    private static final double HOME_IDLE_RETURN_DISTANCE_BLOCKS = 28.0D;
    private static final double HOME_IDLE_RETURN_DISTANCE_SQUARED =
            HOME_IDLE_RETURN_DISTANCE_BLOCKS * HOME_IDLE_RETURN_DISTANCE_BLOCKS;

    private static final double WOLF_HOME_RETURN_SPEED = 0.82D;
    private static final double DOLPHIN_HOME_RETURN_SPEED = 1.02D;

    private RetoldPackHomeReturn() {
    }

    static void updateHomeReturn(
            ServerLevel level,
            PathfinderMob mob,
            RetoldAnimalHomeMemory home,
            long gameTime
    ) {
        RetoldAiControlMode mode = RetoldAiControl.getMode(mob);
        RetoldAiControlOwner owner = RetoldAiControl.getOwner(mob);

        if (!canUseHomeReturnControl(mob)) {
            RetoldHomeRestAnimations.stopResting(mob);
            return;
        }

        if (RetoldWolfDenDefense.tryDefendHome(level, mob, home, mode, owner, gameTime)) {
            return;
        }

        double distanceSquared = mob.blockPosition().distSqr(home.pos());

        if (
                RetoldAnimalDailyRhythm.isActive(level, mob)
                        && distanceSquared < HOME_IDLE_RETURN_DISTANCE_SQUARED
        ) {
            releaseActiveHomeControl(mob, gameTime);
            return;
        }

        if (RetoldWolfDenIdle.shouldIdleAtHome(mob, home, mode, owner, distanceSquared, gameTime)) {
            RetoldWolfDenIdle.idleAtHome(
                    mob,
                    home.pos(),
                    distanceSquared,
                    gameTime
            );
            return;
        }

        if (distanceSquared <= HOME_CLOSE_DISTANCE_SQUARED) {
            stopHomeReturn(mob, gameTime);
            return;
        }

        if (
                mode == RetoldAiControlMode.NONE
                        && distanceSquared < HOME_IDLE_RETURN_DISTANCE_SQUARED
        ) {
            return;
        }

        moveHome(
                mob,
                home.pos(),
                gameTime
        );
    }

    private static boolean canUseHomeReturnControl(PathfinderMob mob) {
        return RetoldBehaviorCoordinator.canContinueOwnedRegroup(
                mob,
                CONTROL_OWNER
        );
    }

    private static void moveHome(
            PathfinderMob mob,
            BlockPos homePos,
            long gameTime
    ) {
        if (!RetoldAiControl.tryClaim(
                mob,
                RetoldAiControlMode.REGROUP,
                CONTROL_OWNER,
                21,
                REASON_RETURN_HOME,
                gameTime,
                HOME_RETURN_CONTROL_TICKS
        )) {
            return;
        }

        RetoldHomeRestAnimations.stopResting(mob);

        RetoldPackHomeControl.clearCombatState(mob);

        RetoldAnimalHomes.markUsed(
                mob,
                gameTime
        );

        double speed = getHomeReturnSpeed(mob);

        RetoldBehaviorMovement.throttledMoveTo(
                mob,
                homePos,
                speed,
                gameTime,
                HOME_RETURN_PATH_INTERVAL_TICKS,
                2.0D * 2.0D
        );
    }

    private static void stopHomeReturn(
            PathfinderMob mob,
            long gameTime
    ) {
        RetoldAiControl.clearIfOwnedBy(mob, CONTROL_OWNER);

        RetoldAnimalHomes.markUsed(
                mob,
                gameTime
        );

        RetoldPackHomeControl.clearCombatState(mob);
        RetoldHomeRestAnimations.stopResting(mob);
        mob.getNavigation().stop();
    }

    private static void releaseActiveHomeControl(
            PathfinderMob mob,
            long gameTime
    ) {
        if (RetoldAiControl.isControlledBy(mob, CONTROL_OWNER)) {
            RetoldAiControl.clearIfOwnedBy(mob, CONTROL_OWNER);
            RetoldPackHomeControl.clearCombatState(mob);
        }

        RetoldAnimalHomes.markUsed(
                mob,
                gameTime
        );

        RetoldHomeRestAnimations.stopResting(mob);
    }

    private static double getHomeReturnSpeed(PathfinderMob mob) {
        if (RetoldMobRules.isDolphin(mob)) {
            return DOLPHIN_HOME_RETURN_SPEED;
        }

        return WOLF_HOME_RETURN_SPEED;
    }
}
