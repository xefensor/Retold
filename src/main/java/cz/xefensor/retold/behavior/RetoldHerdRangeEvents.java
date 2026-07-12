package cz.xefensor.retold.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class RetoldHerdRangeEvents {
    private static final RetoldAiControlOwner CONTROL_OWNER = RetoldAiControlOwner.REGROUP;
    private static final String REASON_RETURN_RANGE = "return_herd_range";
    private static final String REASON_RANGE_IDLE = "herd_range_idle";
    private static final String REASON_MIGRATE_RANGE = "migrate_depleted_range";

    private static final int THINK_INTERVAL_TICKS = 40;
    private static final int HERD_RANGE_SCAN_CACHE_TICKS = 15;
    private static final int HERD_RANGE_POSITION_SCAN_CACHE_TICKS = 15;
    private static final int HERD_RANGE_PATH_INTERVAL_TICKS = 12;
    private static final int RANGE_RETURN_CONTROL_TICKS = 20 * 6;
    private static final int RANGE_RETURN_PRIORITY = RetoldAiPriorities.above(RetoldAiPriorities.REST, 3);
    private static final int RANGE_IDLE_CONTROL_TICKS = 20 * 5;
    private static final int RANGE_IDLE_PRIORITY = RetoldAiPriorities.HOME_IDLE;
    private static final int RANGE_IDLE_MOVE_INTERVAL_TICKS = 20 * 24;
    private static final int RANGE_MIGRATION_CONTROL_TICKS = 20 * 8;
    private static final int RANGE_MIGRATION_PRIORITY = RetoldAiPriorities.above(RetoldAiPriorities.REST, 1);
    private static final int RANGE_MIGRATION_HUNGER = 48;
    private static final int RANGE_DEPLETED_FORAGE_SCORE = 8;
    private static final int RANGE_TARGET_FORAGE_SCORE = 18;
    private static final int PANIC_RECOVERY_TICKS = 20 * 18;

    private static final double RANGE_CREATION_RADIUS_BLOCKS = 18.0D;
    private static final double RANGE_CREATION_RADIUS_SQUARED =
            RANGE_CREATION_RADIUS_BLOCKS * RANGE_CREATION_RADIUS_BLOCKS;

    private static final double RANGE_RETURN_START_BLOCKS = 26.0D;
    private static final double RANGE_RETURN_START_SQUARED =
            RANGE_RETURN_START_BLOCKS * RANGE_RETURN_START_BLOCKS;

    private static final double PANIC_RECOVERY_RETURN_START_BLOCKS = 11.0D;
    private static final double PANIC_RECOVERY_RETURN_START_SQUARED =
            PANIC_RECOVERY_RETURN_START_BLOCKS * PANIC_RECOVERY_RETURN_START_BLOCKS;

    private static final double RANGE_RETURN_STOP_BLOCKS = 9.0D;
    private static final double RANGE_RETURN_STOP_SQUARED =
            RANGE_RETURN_STOP_BLOCKS * RANGE_RETURN_STOP_BLOCKS;

    private static final double RANGE_IDLE_RADIUS_BLOCKS = 12.0D;
    private static final double RANGE_IDLE_RADIUS_SQUARED =
            RANGE_IDLE_RADIUS_BLOCKS * RANGE_IDLE_RADIUS_BLOCKS;

    private static final double RANGE_IDLE_CLOSE_BLOCKS = 7.0D;
    private static final double RANGE_IDLE_CLOSE_SQUARED =
            RANGE_IDLE_CLOSE_BLOCKS * RANGE_IDLE_CLOSE_BLOCKS;

    private static final double RANGE_IDLE_MIN_STROLL_BLOCKS = 2.5D;
    private static final double RANGE_IDLE_EXTRA_STROLL_BLOCKS = 4.5D;

    private static final int RANGE_FORAGE_SCAN_HORIZONTAL_BLOCKS = 10;
    private static final int RANGE_FORAGE_SCAN_VERTICAL_BLOCKS = 2;

    private static final double RANGE_RETURN_SPEED = 0.72D;
    private static final double RANGE_IDLE_STROLL_SPEED = 0.46D;
    private static final double RANGE_MIGRATION_SPEED = 0.62D;

    private RetoldHerdRangeEvents() {
    }

    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof PathfinderMob animal)) {
            return;
        }

        if (!(animal.level() instanceof ServerLevel level)) {
            return;
        }

        if (!isGrazer(animal)) {
            return;
        }

        long gameTime = level.getGameTime();

        if (!shouldThink(animal, gameTime)) {
            return;
        }

        RetoldAnimalHomeMemory range = RetoldAnimalHomes.get(animal);

        if (!RetoldAnimalHomes.isValidFor(level, animal, range)) {
            range = tryCreateHerdRange(
                    level,
                    animal,
                    gameTime
            );

            if (!RetoldAnimalHomes.isValidFor(level, animal, range)) {
                return;
            }
        }

        updateRangeReturn(
                level,
                animal,
                range,
                gameTime
        );
    }

    private static boolean shouldThink(
            PathfinderMob animal,
            long gameTime
    ) {
        return RetoldBehaviorTiming.shouldThink(
                animal,
                gameTime,
                THINK_INTERVAL_TICKS
        );
    }

    private static RetoldAnimalHomeMemory tryCreateHerdRange(
            ServerLevel level,
            PathfinderMob animal,
            long gameTime
    ) {
        if (!canCreateRange(animal)) {
            return null;
        }

        List<PathfinderMob> candidates = new ArrayList<>(RetoldAiScanCache.nearby(
                level,
                animal,
                PathfinderMob.class,
                RANGE_CREATION_RADIUS_BLOCKS,
                gameTime,
                HERD_RANGE_SCAN_CACHE_TICKS
        ));
        candidates.removeIf(candidate -> !isRangeCandidate(level, animal, candidate));

        candidates.sort(
                Comparator.comparingDouble(candidate -> animal.distanceToSqr(candidate))
        );

        List<PathfinderMob> members = new ArrayList<>();
        int maxMembers = Math.max(
                0,
                RetoldAnimalSocialGroups.maxHomeGroupSize(animal) - 1
        );

        for (PathfinderMob candidate : candidates) {
            if (members.size() >= maxMembers) {
                break;
            }

            members.add(candidate);
        }

        return RetoldAnimalHomes.getOrCreatePackHome(
                level,
                animal,
                members,
                calculateRangeCenter(animal, members),
                gameTime
        );
    }

    private static boolean canCreateRange(PathfinderMob animal) {
        return RetoldBehaviorCoordinator.canStartLowPriorityHomeBehavior(animal);
    }

    private static boolean isRangeCandidate(
            ServerLevel level,
            PathfinderMob animal,
            PathfinderMob candidate
    ) {
        if (animal == null || candidate == null || animal == candidate) {
            return false;
        }

        if (!isGrazer(candidate)) {
            return false;
        }

        if (!RetoldAnimalSocialGroups.canShareHomeOrRange(animal, candidate)) {
            return false;
        }

        if (!RetoldBehaviorCoordinator.isAliveInSameLevel(animal, candidate)) {
            return false;
        }

        if (animal.distanceToSqr(candidate) > RANGE_CREATION_RADIUS_SQUARED) {
            return false;
        }

        if (RetoldBehaviorCoordinator.hasLiveTarget(candidate)) {
            return false;
        }

        RetoldAiControlMode mode = RetoldAiControl.getMode(candidate);

        if (
                mode != RetoldAiControlMode.NONE
                        && !RetoldAiControl.isControlledAsBy(
                        candidate,
                        RetoldAiControlMode.REGROUP,
                        CONTROL_OWNER
                )
        ) {
            return false;
        }

        RetoldAnimalHomeMemory candidateRange = RetoldAnimalHomes.get(candidate);

        return candidateRange == null
                || RetoldAnimalHomes.isValidFor(
                level,
                candidate,
                candidateRange
        );
    }

    private static void updateRangeReturn(
            ServerLevel level,
            PathfinderMob animal,
            RetoldAnimalHomeMemory range,
            long gameTime
    ) {
        RetoldAiControlMode mode = RetoldAiControl.getMode(animal);
        RetoldAiControlOwner owner = RetoldAiControl.getOwner(animal);

        if (!canUseRangeReturn(animal, mode)) {
            RetoldHomeRestAnimations.stopResting(animal);
            return;
        }

        if (tryMigrateFromDepletedRange(level, animal, range, gameTime)) {
            return;
        }

        double distanceSquared = animal.blockPosition().distSqr(range.pos());
        boolean recoveringFromPanic = isRecoveringFromPanic(
                animal,
                gameTime
        );

        if (
                RetoldAnimalDailyRhythm.isActive(level, animal)
                        && !RetoldAnimalDailyRhythm.shouldRestAtHome(level, animal)
                        && distanceSquared < getReturnStartDistanceSquared(recoveringFromPanic)
        ) {
            releaseRangeIdleIfOwned(animal);
            RetoldHomeRestAnimations.stopResting(animal);
            RetoldAnimalHomes.markUsed(
                    animal,
                    gameTime
            );
            return;
        }

        if (distanceSquared <= RANGE_RETURN_STOP_SQUARED) {
            boolean shouldRest = shouldRestAtRange(
                    level,
                    animal,
                    recoveringFromPanic,
                    gameTime
            );

            if (!RetoldAnimalDailyRhythm.shouldIdleAtHome(level, animal) && !shouldRest) {
                releaseRangeIdleIfOwned(animal);
                RetoldHomeRestAnimations.stopResting(animal);
                RetoldAnimalHomes.markUsed(
                        animal,
                        gameTime
                );
                return;
            }

            if (
                    RetoldAnimalHomeIdle.shouldIdleAtHome(
                            animal,
                            range,
                            mode,
                            owner,
                            CONTROL_OWNER,
                            REASON_RANGE_IDLE,
                            distanceSquared,
                            RANGE_IDLE_RADIUS_SQUARED,
                            gameTime
                    )
            ) {
                RetoldAnimalHomeIdle.idleAtHome(
                        level,
                        animal,
                        range,
                        gameTime,
                        REASON_RANGE_IDLE,
                        RANGE_IDLE_PRIORITY,
                        RANGE_IDLE_CONTROL_TICKS,
                        RANGE_IDLE_CLOSE_SQUARED,
                        RANGE_RETURN_SPEED,
                        RANGE_IDLE_STROLL_SPEED,
                        RANGE_IDLE_MIN_STROLL_BLOCKS,
                        RANGE_IDLE_EXTRA_STROLL_BLOCKS,
                        RANGE_IDLE_MOVE_INTERVAL_TICKS,
                        shouldRest
                );
                return;
            }

            if (RetoldAiControl.isControlledAsByWithReason(
                    animal,
                    RetoldAiControlMode.REGROUP,
                    CONTROL_OWNER,
                    REASON_RETURN_RANGE
            )) {
                RetoldAiControl.clear(animal);
                animal.getNavigation().stop();
            }

            RetoldAnimalHomes.markUsed(
                    animal,
                    gameTime
            );

            if (shouldRest) {
                RetoldHomeRestAnimations.startResting(animal);
            } else {
                RetoldHomeRestAnimations.stopResting(animal);
            }
            return;
        }

        if (
                mode == RetoldAiControlMode.NONE
                        && distanceSquared < getReturnStartDistanceSquared(recoveringFromPanic)
        ) {
            return;
        }

        if (!RetoldAiControl.tryClaim(
                animal,
                RetoldAiControlMode.REGROUP,
                CONTROL_OWNER,
                RANGE_RETURN_PRIORITY,
                REASON_RETURN_RANGE,
                gameTime,
                RANGE_RETURN_CONTROL_TICKS
        )) {
            return;
        }

        RetoldHomeRestAnimations.stopResting(animal);

        animal.setSprinting(false);

        RetoldAnimalHomes.markUsed(
                animal,
                gameTime
        );

        RetoldBehaviorMovement.throttledMoveTo(
                animal,
                range.pos(),
                RANGE_RETURN_SPEED,
                gameTime,
                HERD_RANGE_PATH_INTERVAL_TICKS,
                2.0D * 2.0D
        );
    }

    private static double getReturnStartDistanceSquared(boolean recoveringFromPanic) {
        return recoveringFromPanic
                ? PANIC_RECOVERY_RETURN_START_SQUARED
                : RANGE_RETURN_START_SQUARED;
    }

    private static boolean isRecoveringFromPanic(
            PathfinderMob animal,
            long gameTime
    ) {
        RetoldMobState state = RetoldMobStates.get(animal);

        return state != null
                && gameTime - state.lastFleeEndedAt() <= PANIC_RECOVERY_TICKS;
    }

    private static boolean tryMigrateFromDepletedRange(
            ServerLevel level,
            PathfinderMob animal,
            RetoldAnimalHomeMemory range,
            long gameTime
    ) {
        RetoldMobState state = RetoldMobStates.getOrCreate(
                animal,
                gameTime
        );

        if (!RetoldMobRules.hasHungerAtLeast(
                state,
                RANGE_MIGRATION_HUNGER
        )) {
            return false;
        }

        int currentScore = RetoldRangeForage.forageScore(
                level,
                animal,
                range.pos(),
                RANGE_FORAGE_SCAN_HORIZONTAL_BLOCKS,
                RANGE_FORAGE_SCAN_VERTICAL_BLOCKS
        );

        if (currentScore > RANGE_DEPLETED_FORAGE_SCORE) {
            return false;
        }

        BlockPos newRangeCenter = RetoldRangeForage.findBetterForageCenter(
                level,
                animal,
                range.pos(),
                RANGE_FORAGE_SCAN_HORIZONTAL_BLOCKS,
                RANGE_FORAGE_SCAN_VERTICAL_BLOCKS,
                currentScore,
                RANGE_TARGET_FORAGE_SCORE
        );

        if (newRangeCenter == null) {
            return false;
        }

        if (!RetoldAiControl.tryClaim(
                animal,
                RetoldAiControlMode.REGROUP,
                CONTROL_OWNER,
                RANGE_MIGRATION_PRIORITY,
                REASON_MIGRATE_RANGE,
                gameTime,
                RANGE_MIGRATION_CONTROL_TICKS
        )) {
            return false;
        }

        RetoldAnimalHomes.replacePackHome(
                level,
                animal,
                findCurrentRangeMembers(
                        level,
                        animal,
                        range
                ),
                newRangeCenter,
                gameTime
        );
        RetoldHomeRestAnimations.stopResting(animal);
        animal.setSprinting(false);

        RetoldBehaviorMovement.throttledMoveTo(
                animal,
                newRangeCenter,
                RANGE_MIGRATION_SPEED,
                gameTime,
                HERD_RANGE_PATH_INTERVAL_TICKS,
                2.0D * 2.0D
        );

        return true;
    }

    private static List<PathfinderMob> findCurrentRangeMembers(
            ServerLevel level,
            PathfinderMob animal,
            RetoldAnimalHomeMemory range
    ) {
        double radius = RetoldAnimalSocialGroups.homeSeparationBlocks(animal);

        return RetoldAiScanCache.nearbyAt(
                level,
                range.pos(),
                PathfinderMob.class,
                radius,
                level.getGameTime(),
                HERD_RANGE_POSITION_SCAN_CACHE_TICKS
        ).stream()
                .filter(
                candidate -> candidate != animal
                        && isGrazer(candidate)
                        && RetoldAnimalHomes.hasSameValidHomeAs(
                        level,
                        candidate,
                        range
                )
        ).toList();
    }

    private static boolean shouldRestAtRange(
            ServerLevel level,
            PathfinderMob animal,
            boolean recoveringFromPanic,
            long gameTime
    ) {
        if (recoveringFromPanic) {
            return false;
        }

        RetoldMobState state = RetoldMobStates.getOrCreate(
                animal,
                gameTime
        );

        if (RetoldMobRules.hasHuntDrive(animal, state)) {
            return false;
        }

        return RetoldMobRules.isCamel(animal)
                && RetoldAnimalDailyRhythm.shouldRestAtHome(level, animal);
    }

    private static boolean canUseRangeReturn(
            PathfinderMob animal,
            RetoldAiControlMode mode
    ) {
        return RetoldBehaviorCoordinator.canUseOwnedModeWithoutLiveTarget(
                animal,
                mode,
                RetoldAiControlMode.REGROUP,
                CONTROL_OWNER
        );
    }

    private static void releaseRangeIdleIfOwned(PathfinderMob animal) {
        if (RetoldAiControl.clearIfControlledAsByWithReason(
                animal,
                RetoldAiControlMode.REGROUP,
                CONTROL_OWNER,
                REASON_RANGE_IDLE
        )) {
            animal.getNavigation().stop();
        }
    }

    private static BlockPos calculateRangeCenter(
            PathfinderMob animal,
            List<PathfinderMob> members
    ) {
        double x = animal.getX();
        double y = animal.getY();
        double z = animal.getZ();
        int count = 1;

        for (PathfinderMob member : members) {
            x += member.getX();
            y += member.getY();
            z += member.getZ();
            count++;
        }

        return new BlockPos(
                (int) Math.floor(x / count),
                (int) Math.floor(y / count),
                (int) Math.floor(z / count)
        ).immutable();
    }

    private static boolean isGrazer(PathfinderMob mob) {
        return mob != null
                && mob.isAlive()
                && !mob.isRemoved()
                && RetoldMobRules.canUseOrdinaryLifeSystems(mob)
                && RetoldMobRules.isHungryGrazer(mob);
    }

}
