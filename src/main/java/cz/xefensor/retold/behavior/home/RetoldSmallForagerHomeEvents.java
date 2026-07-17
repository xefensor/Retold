package cz.xefensor.retold.behavior.home;

import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.control.RetoldAiControlMode;
import cz.xefensor.retold.behavior.control.RetoldAiControlOwner;
import cz.xefensor.retold.behavior.control.RetoldAiPriorities;
import cz.xefensor.retold.behavior.performance.RetoldAiScanCache;
import cz.xefensor.retold.behavior.core.RetoldBehaviorCoordinator;
import cz.xefensor.retold.behavior.core.RetoldBehaviorMovement;
import cz.xefensor.retold.behavior.core.RetoldBehaviorTiming;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;
import cz.xefensor.retold.behavior.profiles.RetoldMobState;
import cz.xefensor.retold.behavior.profiles.RetoldMobStates;
import cz.xefensor.retold.behavior.food.RetoldRangeForage;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class RetoldSmallForagerHomeEvents {
    private static final RetoldAiControlOwner CONTROL_OWNER = RetoldAiControlOwner.REGROUP;
    private static final String REASON_RETURN_HOME = "return_small_home";
    private static final String REASON_ROOSTING = "roosting";
    private static final String REASON_RESTING = "resting";
    private static final String REASON_HIDING = "hiding";
    private static final String REASON_HOME_IDLE = "small_home_idle";
    private static final String REASON_MIGRATE_RANGE = "migrate_foraging_range";

    private static final int THINK_INTERVAL_TICKS = 40;
    private static final int SMALL_HOME_SCAN_CACHE_TICKS = 15;
    private static final int SMALL_HOME_POSITION_SCAN_CACHE_TICKS = 15;
    private static final int SMALL_HOME_PATH_INTERVAL_TICKS = 12;
    private static final int HOME_RETURN_CONTROL_TICKS = 20 * 5;
    private static final int HOME_RETURN_PRIORITY = RetoldAiPriorities.above(RetoldAiPriorities.REST, 2);
    private static final int ROOST_CONTROL_TICKS = 20 * 5;
    private static final int ROOST_PRIORITY = RetoldAiPriorities.above(RetoldAiPriorities.REST, 1);
    private static final int REST_CONTROL_TICKS = 20 * 5;
    private static final int REST_PRIORITY = RetoldAiPriorities.REST;
    private static final int HIDE_CONTROL_TICKS = 20 * 8;
    private static final int HIDE_PRIORITY = RetoldAiPriorities.above(RetoldAiPriorities.REGROUP, 2);
    private static final int HOME_IDLE_CONTROL_TICKS = 20 * 5;
    private static final int HOME_IDLE_PRIORITY = RetoldAiPriorities.HOME_IDLE;
    private static final int HOME_IDLE_MOVE_INTERVAL_TICKS = 20 * 20;
    private static final int RANGE_MIGRATION_CONTROL_TICKS = 20 * 7;
    private static final int RANGE_MIGRATION_PRIORITY = RetoldAiPriorities.REST;
    private static final int RANGE_MIGRATION_HUNGER = 45;
    private static final int RANGE_DEPLETED_FORAGE_SCORE = 5;
    private static final int RANGE_TARGET_FORAGE_SCORE = 12;
    private static final int PANIC_RECOVERY_TICKS = 20 * 18;

    private static final double HOME_CREATION_RADIUS_BLOCKS = 14.0D;
    private static final double HOME_CREATION_RADIUS_SQUARED =
            HOME_CREATION_RADIUS_BLOCKS * HOME_CREATION_RADIUS_BLOCKS;

    private static final double HOME_RETURN_START_BLOCKS = 20.0D;
    private static final double HOME_RETURN_START_SQUARED =
            HOME_RETURN_START_BLOCKS * HOME_RETURN_START_BLOCKS;

    private static final double PANIC_RECOVERY_RETURN_START_BLOCKS = 8.0D;
    private static final double PANIC_RECOVERY_RETURN_START_SQUARED =
            PANIC_RECOVERY_RETURN_START_BLOCKS * PANIC_RECOVERY_RETURN_START_BLOCKS;

    private static final double HOME_RETURN_STOP_BLOCKS = 6.0D;
    private static final double HOME_RETURN_STOP_SQUARED =
            HOME_RETURN_STOP_BLOCKS * HOME_RETURN_STOP_BLOCKS;

    private static final double HOME_IDLE_RADIUS_BLOCKS = 8.0D;
    private static final double HOME_IDLE_RADIUS_SQUARED =
            HOME_IDLE_RADIUS_BLOCKS * HOME_IDLE_RADIUS_BLOCKS;

    private static final double HOME_IDLE_CLOSE_BLOCKS = 4.0D;
    private static final double HOME_IDLE_CLOSE_SQUARED =
            HOME_IDLE_CLOSE_BLOCKS * HOME_IDLE_CLOSE_BLOCKS;

    private static final double HOME_IDLE_MIN_STROLL_BLOCKS = 1.5D;
    private static final double HOME_IDLE_EXTRA_STROLL_BLOCKS = 3.0D;

    private static final double HOME_RETURN_SPEED = 0.68D;
    private static final double HOME_IDLE_STROLL_SPEED = 0.42D;
    private static final double RANGE_MIGRATION_SPEED = 0.56D;

    private static final int RANGE_FORAGE_SCAN_HORIZONTAL_BLOCKS = 8;
    private static final int RANGE_FORAGE_SCAN_VERTICAL_BLOCKS = 2;

    private RetoldSmallForagerHomeEvents() {
    }

    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof PathfinderMob animal)) {
            return;
        }

        if (!(animal.level() instanceof ServerLevel level)) {
            return;
        }

        if (!isSmallForager(animal)) {
            return;
        }

        long gameTime = level.getGameTime();

        if (!shouldThink(animal, gameTime)) {
            return;
        }

        RetoldAnimalHomeMemory home = RetoldAnimalHomes.get(animal);

        if (!RetoldAnimalHomes.isValidFor(level, animal, home)) {
            home = tryCreateSmallHome(
                    level,
                    animal,
                    gameTime
            );

            if (!RetoldAnimalHomes.isValidFor(level, animal, home)) {
                return;
            }
        }

        updateHomeReturn(
                level,
                animal,
                home,
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

    private static RetoldAnimalHomeMemory tryCreateSmallHome(
            ServerLevel level,
            PathfinderMob animal,
            long gameTime
    ) {
        if (!canCreateHome(animal)) {
            return null;
        }

        List<PathfinderMob> candidates = new ArrayList<>(RetoldAiScanCache.nearby(
                level,
                animal,
                PathfinderMob.class,
                HOME_CREATION_RADIUS_BLOCKS,
                gameTime,
                SMALL_HOME_SCAN_CACHE_TICKS
        ));
        candidates.removeIf(candidate -> !isHomeCandidate(level, animal, candidate));

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
                calculateHomeCenter(animal, members),
                gameTime
        );
    }

    private static boolean canCreateHome(PathfinderMob animal) {
        return RetoldBehaviorCoordinator.canStartLowPriorityHomeBehavior(animal);
    }

    private static boolean isHomeCandidate(
            ServerLevel level,
            PathfinderMob animal,
            PathfinderMob candidate
    ) {
        if (animal == null || candidate == null || animal == candidate) {
            return false;
        }

        if (!isSmallForager(candidate)) {
            return false;
        }

        if (!RetoldAnimalSocialGroups.canShareHomeOrRange(animal, candidate)) {
            return false;
        }

        if (animal.level() != candidate.level()) {
            return false;
        }

        if (animal.distanceToSqr(candidate) > HOME_CREATION_RADIUS_SQUARED) {
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

        RetoldAnimalHomeMemory candidateHome = RetoldAnimalHomes.get(candidate);

        return candidateHome == null
                || RetoldAnimalHomes.isValidFor(
                level,
                candidate,
                candidateHome
        );
    }

    private static void updateHomeReturn(
            ServerLevel level,
            PathfinderMob animal,
            RetoldAnimalHomeMemory home,
            long gameTime
    ) {
        RetoldAiControlMode mode = RetoldAiControl.getMode(animal);
        RetoldAiControlOwner owner = RetoldAiControl.getOwner(animal);

        if (!canUseHomeReturn(animal, mode)) {
            RetoldHomeRestAnimations.stopResting(animal);
            return;
        }

        if (tryMigrateFromDepletedForagingRange(level, animal, home, gameTime)) {
            return;
        }

        double distanceSquared = animal.blockPosition().distSqr(home.pos());
        boolean shouldRoost = shouldReturnToRoost(
                level,
                animal,
                home
        );
        boolean shouldRest = shouldReturnToRestSite(
                level,
                animal,
                home
        );
        boolean recoveringFromPanic = isRecoveringFromPanic(
                animal,
                gameTime
        );
        boolean shouldHide = shouldHideAtWarren(
                animal,
                home,
                recoveringFromPanic
        );
        boolean forcedReturn = shouldRoost || shouldRest || shouldHide;

        if (
                !forcedReturn
                        && RetoldAnimalDailyRhythm.isActive(level, animal)
                        && distanceSquared < getReturnStartDistanceSquared(recoveringFromPanic)
        ) {
            releaseHomeIdleIfOwned(animal);
            RetoldHomeRestAnimations.stopResting(animal);
            RetoldAnimalHomes.markUsed(
                    animal,
                    gameTime
            );
            return;
        }

        if (distanceSquared <= HOME_RETURN_STOP_SQUARED) {
            if (shouldHide) {
                holdAtHome(
                        level,
                        animal,
                        home,
                        gameTime,
                        HIDE_PRIORITY,
                        REASON_HIDING,
                        HIDE_CONTROL_TICKS,
                        true
                );
                return;
            }

            if (shouldRoost) {
                holdAtHome(
                        level,
                        animal,
                        home,
                        gameTime,
                        ROOST_PRIORITY,
                        REASON_ROOSTING,
                        ROOST_CONTROL_TICKS,
                        true
                );
                return;
            }

            if (shouldRest) {
                holdAtHome(
                        level,
                        animal,
                        home,
                        gameTime,
                        REST_PRIORITY,
                        REASON_RESTING,
                        REST_CONTROL_TICKS,
                        true
                );
                return;
            }

            if (RetoldAnimalDailyRhythm.isActive(level, animal)) {
                releaseHomeIdleIfOwned(animal);
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
                            home,
                            mode,
                            owner,
                            CONTROL_OWNER,
                            REASON_HOME_IDLE,
                            distanceSquared,
                            HOME_IDLE_RADIUS_SQUARED,
                            gameTime
                    )
            ) {
                RetoldAnimalHomeIdle.idleAtHome(
                        level,
                        animal,
                        home,
                        gameTime,
                        REASON_HOME_IDLE,
                        HOME_IDLE_PRIORITY,
                        HOME_IDLE_CONTROL_TICKS,
                        HOME_IDLE_CLOSE_SQUARED,
                        HOME_RETURN_SPEED,
                        HOME_IDLE_STROLL_SPEED,
                        HOME_IDLE_MIN_STROLL_BLOCKS,
                        HOME_IDLE_EXTRA_STROLL_BLOCKS,
                        HOME_IDLE_MOVE_INTERVAL_TICKS,
                        false
                );
                return;
            }

            if (RetoldAiControl.isControlledAsByWithAnyReason(
                    animal,
                    RetoldAiControlMode.REGROUP,
                    CONTROL_OWNER,
                    REASON_RETURN_HOME,
                    REASON_ROOSTING,
                    REASON_RESTING,
                    REASON_HIDING
            )) {
                RetoldAiControl.clear(animal);
                animal.getNavigation().stop();
            }

            RetoldHomeRestAnimations.stopResting(animal);

            RetoldAnimalHomes.markUsed(
                    animal,
                    gameTime
            );
            return;
        }

        if (
                !forcedReturn
                        && mode == RetoldAiControlMode.NONE
                        && distanceSquared < getReturnStartDistanceSquared(recoveringFromPanic)
        ) {
            return;
        }

        int priority = getReturnPriority(
                shouldRoost,
                shouldRest,
                shouldHide
        );
        String reason = getReturnReason(
                shouldRoost,
                shouldRest,
                shouldHide
        );

        if (!RetoldAiControl.tryClaim(
                animal,
                RetoldAiControlMode.REGROUP,
                CONTROL_OWNER,
                priority,
                reason,
                gameTime,
                HOME_RETURN_CONTROL_TICKS
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
                home.pos(),
                HOME_RETURN_SPEED,
                gameTime,
                SMALL_HOME_PATH_INTERVAL_TICKS,
                2.0D * 2.0D
        );
    }

    private static double getReturnStartDistanceSquared(boolean recoveringFromPanic) {
        return recoveringFromPanic
                ? PANIC_RECOVERY_RETURN_START_SQUARED
                : HOME_RETURN_START_SQUARED;
    }

    private static boolean isRecoveringFromPanic(
            PathfinderMob animal,
            long gameTime
    ) {
        RetoldMobState state = RetoldMobStates.get(animal);

        return state != null
                && gameTime - state.lastFleeEndedAt() <= PANIC_RECOVERY_TICKS;
    }

    private static boolean tryMigrateFromDepletedForagingRange(
            ServerLevel level,
            PathfinderMob animal,
            RetoldAnimalHomeMemory home,
            long gameTime
    ) {
        if (home.type() != RetoldAnimalHomeType.FORAGING_RANGE) {
            return false;
        }

        if (!RetoldMobRules.isPig(animal)) {
            return false;
        }

        if (!RetoldAnimalDailyRhythm.isActive(level, animal)) {
            return false;
        }

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
                home.pos(),
                RANGE_FORAGE_SCAN_HORIZONTAL_BLOCKS,
                RANGE_FORAGE_SCAN_VERTICAL_BLOCKS
        );

        if (currentScore > RANGE_DEPLETED_FORAGE_SCORE) {
            return false;
        }

        BlockPos newRangeCenter = RetoldRangeForage.findBetterForageCenter(
                level,
                animal,
                home.pos(),
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
                findCurrentForagingRangeMembers(
                        level,
                        animal,
                        home
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
                SMALL_HOME_PATH_INTERVAL_TICKS,
                2.0D * 2.0D
        );

        return true;
    }

    private static List<PathfinderMob> findCurrentForagingRangeMembers(
            ServerLevel level,
            PathfinderMob animal,
            RetoldAnimalHomeMemory home
    ) {
        double radius = RetoldAnimalSocialGroups.homeSeparationBlocks(animal);

        return RetoldAiScanCache.nearbyAt(
                level,
                home.pos(),
                PathfinderMob.class,
                radius,
                level.getGameTime(),
                SMALL_HOME_POSITION_SCAN_CACHE_TICKS
        ).stream()
                .filter(
                candidate -> candidate != animal
                        && isSmallForager(candidate)
                        && RetoldMobRules.isPig(candidate)
                        && RetoldAnimalHomes.hasSameValidHomeAs(
                        level,
                        candidate,
                        home
                )
        ).toList();
    }

    private static void holdAtHome(
            ServerLevel level,
            PathfinderMob animal,
            RetoldAnimalHomeMemory home,
            long gameTime,
            int priority,
            String reason,
            int ticks,
            boolean resting
    ) {
        RetoldAnimalHomeIdle.idleAtHome(
                level,
                animal,
                home,
                gameTime,
                reason,
                priority,
                ticks,
                HOME_IDLE_CLOSE_SQUARED,
                HOME_RETURN_SPEED,
                HOME_IDLE_STROLL_SPEED,
                HOME_IDLE_MIN_STROLL_BLOCKS,
                HOME_IDLE_EXTRA_STROLL_BLOCKS,
                HOME_IDLE_MOVE_INTERVAL_TICKS,
                resting
        );
    }

    private static boolean shouldReturnToRoost(
            ServerLevel level,
            PathfinderMob animal,
            RetoldAnimalHomeMemory home
    ) {
        if (home.type() != RetoldAnimalHomeType.ROOST) {
            return false;
        }

        if (!RetoldMobRules.isChicken(animal)) {
            return false;
        }

        return !RetoldAnimalDailyRhythm.isActive(level, animal)
                || level.isRainingAt(animal.blockPosition());
    }

    private static boolean shouldReturnToRestSite(
            ServerLevel level,
            PathfinderMob animal,
            RetoldAnimalHomeMemory home
    ) {
        if (home.type() != RetoldAnimalHomeType.FORAGING_RANGE) {
            return false;
        }

        if (!RetoldMobRules.isPig(animal)) {
            return false;
        }

        return RetoldAnimalDailyRhythm.shouldRestAtHome(level, animal);
    }

    private static boolean shouldHideAtWarren(
            PathfinderMob animal,
            RetoldAnimalHomeMemory home,
            boolean recoveringFromPanic
    ) {
        if (home.type() != RetoldAnimalHomeType.WARREN) {
            return false;
        }

        return recoveringFromPanic
                && RetoldMobRules.isRabbit(animal);
    }

    private static int getReturnPriority(
            boolean shouldRoost,
            boolean shouldRest,
            boolean shouldHide
    ) {
        if (shouldHide) {
            return HIDE_PRIORITY;
        }

        if (shouldRoost) {
            return ROOST_PRIORITY;
        }

        if (shouldRest) {
            return REST_PRIORITY;
        }

        return HOME_RETURN_PRIORITY;
    }

    private static String getReturnReason(
            boolean shouldRoost,
            boolean shouldRest,
            boolean shouldHide
    ) {
        if (shouldHide) {
            return REASON_HIDING;
        }

        if (shouldRoost) {
            return REASON_ROOSTING;
        }

        if (shouldRest) {
            return REASON_RESTING;
        }

        return REASON_RETURN_HOME;
    }

    private static boolean isNight(ServerLevel level) {
        long dayTime = Math.floorMod(
                level.getOverworldClockTime(),
                24000L
        );

        return dayTime >= 12500L && dayTime <= 23500L;
    }

    private static boolean canUseHomeReturn(
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

    private static void releaseHomeIdleIfOwned(PathfinderMob animal) {
        if (RetoldAiControl.clearIfControlledAsByWithReason(
                animal,
                RetoldAiControlMode.REGROUP,
                CONTROL_OWNER,
                REASON_HOME_IDLE
        )) {
            animal.getNavigation().stop();
        }
    }

    private static BlockPos calculateHomeCenter(
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

    private static boolean isSmallForager(PathfinderMob mob) {
        return mob != null
                && mob.isAlive()
                && !mob.isRemoved()
                && RetoldMobRules.canUseOrdinaryLifeSystems(mob)
                && RetoldMobRules.isSmallForager(mob);
    }

}
