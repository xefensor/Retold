package cz.xefensor.retold.behavior.food;

import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.control.RetoldAiControlMode;
import cz.xefensor.retold.behavior.control.RetoldAiControlOwner;
import cz.xefensor.retold.behavior.core.RetoldBehaviorCoordinator;
import cz.xefensor.retold.behavior.core.RetoldBehaviorMovement;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;
import cz.xefensor.retold.behavior.profiles.RetoldMobState;
import cz.xefensor.retold.block.AnimalFeederBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public final class RetoldAnimalFeederBehavior {
    private static final int HORIZONTAL_RADIUS = 8;
    private static final int VERTICAL_RADIUS = 2;
    private static final int CACHE_TICKS = 60;
    private static final int PATH_INTERVAL_TICKS = 8;
    private static final double USE_DISTANCE_SQUARED = 2.35D * 2.35D;
    private static final double MOVEMENT_SPEED = 0.68D;

    private RetoldAnimalFeederBehavior() {
    }

    public static boolean hasUsableFoodNearby(
            ServerLevel level,
            PathfinderMob mob,
            BlockPos center,
            long gameTime
    ) {
        if (level == null
                || mob == null
                || center == null
                || mob.level() != level
                || !RetoldMobRules.canUseAnimalFeeder(mob)) {
            return false;
        }

        return RetoldAnimalFeederSearch.findAt(
                level,
                mob,
                center,
                HORIZONTAL_RADIUS,
                VERTICAL_RADIUS,
                gameTime,
                CACHE_TICKS
        ) != null;
    }

    public static CatchUpFeederResult findCatchUpFeeder(
            ServerLevel level,
            PathfinderMob mob,
            BlockPos center,
            long gameTime
    ) {
        if (level == null
                || mob == null
                || center == null
                || mob.level() != level
                || !RetoldMobRules.canUseAnimalFeeder(mob)) {
            return CatchUpFeederResult.none();
        }

        RetoldAnimalFeederSearch.FindResult search =
                RetoldAnimalFeederSearch.findAtResult(
                        level,
                        mob,
                        center,
                        HORIZONTAL_RADIUS,
                        VERTICAL_RADIUS,
                        gameTime,
                        CACHE_TICKS
                );

        if (search.deferred()) {
            return CatchUpFeederResult.deferredResult();
        }

        BlockPos feederPos = search.target();

        if (feederPos == null
                || findAccessPos(level, mob, feederPos) == null
                || !(level.getBlockEntity(feederPos)
                instanceof AnimalFeederBlockEntity feeder)) {
            return CatchUpFeederResult.none();
        }

        return new CatchUpFeederResult(feeder, false);
    }

    public static boolean tryUse(
            ServerLevel level,
            PathfinderMob mob,
            RetoldMobState state,
            long gameTime
    ) {
        if (level == null
                || mob == null
                || state == null
                || mob.level() != level
                || mob.isNoAi()
                || !RetoldMobRules.canUseAnimalFeeder(mob)
                || !RetoldMobRules.hasEatDrive(mob, state)
                || !RetoldBehaviorCoordinator.canFeedNow(mob)) {
            return false;
        }

        BlockPos feederPos = RetoldAnimalFeederSearch.find(
                level,
                mob,
                HORIZONTAL_RADIUS,
                VERTICAL_RADIUS,
                gameTime,
                CACHE_TICKS
        );

        if (feederPos == null) {
            return false;
        }

        if (mob.distanceToSqr(Vec3.atCenterOf(feederPos)) <= USE_DISTANCE_SQUARED) {
            return tryConsume(level, mob, state, feederPos, gameTime);
        }

        BlockPos accessPos = findAccessPos(level, mob, feederPos);

        if (accessPos == null) {
            return false;
        }

        if (!RetoldFoodBehaviorEvents.claimFoodControl(
                mob,
                gameTime,
                "use_animal_feeder"
        )) {
            return false;
        }

        boolean moving = RetoldBehaviorMovement.throttledMoveToExact(
                mob,
                accessPos,
                MOVEMENT_SPEED,
                gameTime,
                PATH_INTERVAL_TICKS,
                1.5D * 1.5D
        );

        if (!moving) {
            RetoldAiControl.clearIfControlledAsByAny(
                    mob,
                    RetoldAiControlOwner.FOOD,
                    RetoldAiControlMode.FEED
            );
            RetoldBehaviorMovement.stopOwnedMovement(
                    mob,
                    RetoldAiControlOwner.FOOD
            );
        }

        return moving;
    }

    static BlockPos findAccessPos(
            ServerLevel level,
            PathfinderMob mob,
            BlockPos feederPos
    ) {
        BlockPos best = null;
        double bestDistanceSquared = Double.MAX_VALUE;

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos candidate = feederPos.relative(direction);

            if (!mob.getNavigation().isStableDestination(candidate)
                    || !level.getBlockState(candidate)
                    .getCollisionShape(level, candidate)
                    .isEmpty()
                    || !level.getBlockState(candidate.above())
                    .getCollisionShape(level, candidate.above())
                    .isEmpty()) {
                continue;
            }

            double distanceSquared = mob.distanceToSqr(
                    Vec3.atBottomCenterOf(candidate)
            );

            if (distanceSquared < bestDistanceSquared) {
                bestDistanceSquared = distanceSquared;
                best = candidate;
            }
        }

        return best;
    }

    public static boolean tryConsume(
            ServerLevel level,
            PathfinderMob mob,
            RetoldMobState state,
            BlockPos feederPos,
            long gameTime
    ) {
        if (level == null
                || mob == null
                || state == null
                || feederPos == null
                || mob.level() != level
                || !RetoldMobRules.canUseAnimalFeeder(mob)
                || !RetoldMobRules.hasEatDrive(mob, state)
                || !RetoldBehaviorCoordinator.canFeedNow(mob)
                || mob.distanceToSqr(Vec3.atCenterOf(feederPos)) > USE_DISTANCE_SQUARED
                || !(level.getBlockEntity(feederPos) instanceof AnimalFeederBlockEntity feeder)) {
            return false;
        }

        ItemStack consumed = feeder.takeOneFor(mob);

        if (consumed.isEmpty()) {
            return false;
        }

        state.addHunger(-RetoldMobRules.foodRelief(
                mob,
                consumed
        ));
        state.markFed(gameTime);
        RetoldFeedingAnimations.play(mob);
        mob.getNavigation().stop();
        RetoldAiControl.clearIfControlledAsByAny(
                mob,
                RetoldAiControlOwner.FOOD,
                RetoldAiControlMode.FEED,
                RetoldAiControlMode.SEARCH
        );
        RetoldFeedingPose.begin(
                mob,
                Vec3.atCenterOf(feederPos),
                gameTime
        );
        return true;
    }

    public record CatchUpFeederResult(
            AnimalFeederBlockEntity feeder,
            boolean deferred
    ) {
        private static CatchUpFeederResult none() {
            return new CatchUpFeederResult(null, false);
        }

        private static CatchUpFeederResult deferredResult() {
            return new CatchUpFeederResult(null, true);
        }
    }
}
