package cz.xefensor.retold.behavior.flee;

import cz.xefensor.retold.behavior.core.RetoldBehaviorMovement;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;

/** Shared destination and route safety for urgent Retold flight. */
final class RetoldFleeMovement {
    private static final int MIN_DESTINATION_COMMIT_TICKS = 20;
    private static final double MINIMUM_LAND_SEARCH_BLOCKS = 4.0D;
    private static final double DESTINATION_REACHED_DISTANCE_SQUARED = 1.5D * 1.5D;
    private static final double MINIMUM_DIRECTION_ALIGNMENT = 0.35D;
    private static final double DETOUR_SIDE_WEIGHT = 0.85D;

    private static final Map<PathfinderMob, FleeDestinationMemory> DESTINATIONS =
            new WeakHashMap<>();

    private RetoldFleeMovement() {
    }

    static BlockPos chooseDestination(
            PathfinderMob mob,
            Vec3 directDestination,
            double fleeDistance,
            long gameTime
    ) {
        BlockPos fallback = BlockPos.containing(directDestination);
        Vec3 desiredDirection = horizontalDirectionTo(mob, directDestination);
        FleeDestinationMemory existing = DESTINATIONS.get(mob);

        if (canReuseDestination(mob, existing, desiredDirection, gameTime)) {
            return existing.destination();
        }

        BlockPos destination = chooseDryForwardDestination(
                mob,
                fallback,
                desiredDirection,
                fleeDistance
        );

        rememberDestination(mob, destination, desiredDirection, gameTime);

        return destination;
    }

    static void clearDestination(PathfinderMob mob) {
        if (mob != null) {
            DESTINATIONS.remove(mob);
        }
    }

    private static boolean canReuseDestination(
            PathfinderMob mob,
            FleeDestinationMemory memory,
            Vec3 desiredDirection,
            long gameTime
    ) {
        if (mob == null || memory == null) {
            return false;
        }

        if (mob.getNavigation().isDone()
                && mob.distanceToSqr(Vec3.atCenterOf(memory.destination()))
                <= DESTINATION_REACHED_DISTANCE_SQUARED) {
            return false;
        }

        if (mob.getNavigation() instanceof GroundPathNavigation
                && !mob.isInWater()
                && mob.level().getFluidState(memory.destination()).is(FluidTags.WATER)) {
            return false;
        }

        if (memory.direction().dot(desiredDirection) < MINIMUM_DIRECTION_ALIGNMENT) {
            return false;
        }

        return !mob.getNavigation().isDone()
                || gameTime - memory.selectedAt() < MIN_DESTINATION_COMMIT_TICKS;
    }

    private static Vec3 horizontalDirectionTo(
            PathfinderMob mob,
            Vec3 destination
    ) {
        Vec3 direction = new Vec3(
                destination.x() - mob.getX(),
                0.0D,
                destination.z() - mob.getZ()
        );

        return direction.lengthSqr() <= 0.0001D
                ? Vec3.ZERO
                : direction.normalize();
    }

    static boolean moveTo(
            PathfinderMob mob,
            BlockPos target,
            double speed,
            long gameTime,
            int pathIntervalTicks,
            double repathDistanceSquared
    ) {
        if (mob.getNavigation() instanceof GroundPathNavigation && !mob.isInWater()) {
            RetoldBehaviorMovement.MovementOutcome directOutcome =
                    RetoldBehaviorMovement.throttledMoveToAvoidingWaterWithOutcome(
                            mob,
                            target,
                            speed,
                            gameTime,
                            pathIntervalTicks,
                            repathDistanceSquared
                    );

            if (directOutcome.isMoving()) {
                return true;
            }

            if (directOutcome == RetoldBehaviorMovement.MovementOutcome.DEFERRED) {
                return false;
            }

            FleeDestinationMemory memory = DESTINATIONS.get(mob);
            Vec3 forward = memory == null
                    ? horizontalDirectionTo(mob, Vec3.atCenterOf(target))
                    : memory.direction();
            double distance = Math.max(
                    MINIMUM_LAND_SEARCH_BLOCKS,
                    horizontalDistance(mob.position(), Vec3.atCenterOf(target))
            );
            int preferredSign = Math.floorMod(mob.getId(), 2) == 0 ? 1 : -1;
            RetoldBehaviorMovement.MovementOutcome detourOutcome = moveToDetour(
                    mob,
                    target,
                    forward,
                    distance,
                    preferredSign,
                    speed,
                    gameTime,
                    pathIntervalTicks,
                    repathDistanceSquared
            );

            if (detourOutcome.isMoving()) {
                return true;
            }

            if (detourOutcome == RetoldBehaviorMovement.MovementOutcome.DEFERRED) {
                return false;
            }

            return moveToDetour(
                    mob,
                    target,
                    forward,
                    distance,
                    -preferredSign,
                    speed,
                    gameTime,
                    pathIntervalTicks,
                    repathDistanceSquared
            ).isMoving();
        }

        return RetoldBehaviorMovement.throttledMoveTo(
                mob,
                target,
                speed,
                gameTime,
                pathIntervalTicks,
                repathDistanceSquared
        );
    }

    private static BlockPos chooseDryForwardDestination(
            PathfinderMob mob,
            BlockPos direct,
            Vec3 forward,
            double distance
    ) {
        if (!(mob.getNavigation() instanceof GroundPathNavigation)
                || mob.isInWater()
                || !mob.level().getFluidState(direct).is(FluidTags.WATER)) {
            return direct;
        }

        int preferredSign = Math.floorMod(mob.getId(), 2) == 0 ? 1 : -1;
        BlockPos preferred = detourDestination(mob, forward, distance, preferredSign);

        if (!mob.level().getFluidState(preferred).is(FluidTags.WATER)) {
            return preferred;
        }

        BlockPos opposite = detourDestination(mob, forward, distance, -preferredSign);

        if (!mob.level().getFluidState(opposite).is(FluidTags.WATER)) {
            return opposite;
        }

        return mob.blockPosition();
    }

    private static BlockPos detourDestination(
            PathfinderMob mob,
            Vec3 forward,
            double distance,
            int sideSign
    ) {
        Vec3 safeForward = forward.lengthSqr() <= 0.0001D
                ? new Vec3(1.0D, 0.0D, 0.0D)
                : forward.normalize();
        Vec3 side = new Vec3(-safeForward.z, 0.0D, safeForward.x);
        Vec3 detourDirection = safeForward
                .add(side.scale(sideSign * DETOUR_SIDE_WEIGHT))
                .normalize();
        Vec3 detour = mob.position().add(
                detourDirection.scale(Math.max(MINIMUM_LAND_SEARCH_BLOCKS, distance))
        );

        return BlockPos.containing(detour.x, mob.getY(), detour.z);
    }

    private static RetoldBehaviorMovement.MovementOutcome moveToDetour(
            PathfinderMob mob,
            BlockPos originalTarget,
            Vec3 forward,
            double distance,
            int sideSign,
            double speed,
            long gameTime,
            int pathIntervalTicks,
            double repathDistanceSquared
    ) {
        BlockPos detour = detourDestination(mob, forward, distance, sideSign);

        if (detour.equals(originalTarget)
                || mob.level().getFluidState(detour).is(FluidTags.WATER)) {
            return RetoldBehaviorMovement.MovementOutcome.UNSUPPORTED;
        }

        RetoldBehaviorMovement.MovementOutcome outcome =
                RetoldBehaviorMovement.throttledMoveToAvoidingWaterWithOutcome(
                        mob,
                        detour,
                        speed,
                        gameTime,
                        pathIntervalTicks,
                        repathDistanceSquared
                );

        if (outcome.isMoving()) {
            rememberDestination(mob, detour, forward, gameTime);
        }

        return outcome;
    }

    private static double horizontalDistance(Vec3 first, Vec3 second) {
        double x = first.x - second.x;
        double z = first.z - second.z;
        return Math.sqrt(x * x + z * z);
    }

    private static void rememberDestination(
            PathfinderMob mob,
            BlockPos destination,
            Vec3 direction,
            long gameTime
    ) {
        DESTINATIONS.put(
                mob,
                new FleeDestinationMemory(
                        destination.immutable(),
                        direction,
                        gameTime
                )
        );
    }

    private record FleeDestinationMemory(
            BlockPos destination,
            Vec3 direction,
            long selectedAt
    ) {
    }
}
