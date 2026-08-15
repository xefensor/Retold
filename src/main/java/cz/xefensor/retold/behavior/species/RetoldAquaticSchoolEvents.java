package cz.xefensor.retold.behavior.species;

import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.control.RetoldAiControlMode;
import cz.xefensor.retold.behavior.control.RetoldAiControlOwner;
import cz.xefensor.retold.behavior.control.RetoldAiPriorities;
import cz.xefensor.retold.behavior.core.RetoldBehaviorCoordinator;
import cz.xefensor.retold.behavior.core.RetoldBehaviorMovement;
import cz.xefensor.retold.behavior.food.RetoldRangeForage;
import cz.xefensor.retold.behavior.home.RetoldAnimalHomeMemory;
import cz.xefensor.retold.behavior.home.RetoldAnimalHomeType;
import cz.xefensor.retold.behavior.home.RetoldAnimalHomes;
import cz.xefensor.retold.behavior.home.RetoldAnimalSocialGroups;
import cz.xefensor.retold.behavior.performance.RetoldAiScanCache;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;
import cz.xefensor.retold.behavior.profiles.RetoldMobState;
import cz.xefensor.retold.behavior.profiles.RetoldMobStates;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class RetoldAquaticSchoolEvents {
    private static final RetoldAiControlOwner CONTROL_OWNER =
            RetoldAiControlOwner.AQUATIC_SCHOOL;
    private static final String REASON_FISH_SCHOOL = "join_fish_school";
    private static final String REASON_RETURN_SCHOOL_RANGE = "return_aquatic_school_range";
    private static final String REASON_MIGRATE_SCHOOL_RANGE = "migrate_depleted_aquatic_range";

    private static final int SCAN_CACHE_TICKS = 10;
    private static final int RANGE_MEMBER_SCAN_CACHE_TICKS = 15;
    private static final int PATH_INTERVAL_TICKS = 10;
    private static final int CONTROL_TICKS = 20 * 4;
    private static final int RANGE_CONTROL_TICKS = 20 * 8;
    private static final int RANGE_MIGRATION_HUNGER = 48;
    private static final int RANGE_DEPLETED_FORAGE_SCORE = 2;
    private static final int RANGE_TARGET_FORAGE_SCORE = 8;
    private static final int RANGE_FORAGE_HORIZONTAL_BLOCKS = 8;
    private static final int RANGE_FORAGE_VERTICAL_BLOCKS = 3;
    private static final int RANGE_FORAGE_CACHE_TICKS = 80;

    private static final double FISH_SEARCH_RADIUS_BLOCKS = 12.0D;
    private static final double FISH_JOIN_DISTANCE_BLOCKS = 6.0D;
    private static final double FISH_SETTLE_DISTANCE_BLOCKS = 3.0D;
    private static final double RANGE_RETURN_START_BLOCKS = 12.0D;
    private static final double RANGE_RETURN_START_SQUARED =
            RANGE_RETURN_START_BLOCKS * RANGE_RETURN_START_BLOCKS;
    private static final double RANGE_RETURN_STOP_BLOCKS = 5.0D;
    private static final double RANGE_RETURN_STOP_SQUARED =
            RANGE_RETURN_STOP_BLOCKS * RANGE_RETURN_STOP_BLOCKS;
    private static final double FISH_SPEED = 1.0D;
    private static final double RANGE_MIGRATION_SPEED = 0.9D;

    private RetoldAquaticSchoolEvents() {
    }

    public static boolean tick(
            ServerLevel level,
            PathfinderMob aquaticMob,
            long gameTime
    ) {
        if (!isSupported(aquaticMob)) {
            return false;
        }

        if (!canSchoolNow(aquaticMob)) {
            return false;
        }

        SchoolInfo school = findSchool(
                level,
                aquaticMob,
                gameTime
        );

        RetoldAnimalHomeMemory range = ensureSchoolRange(
                level,
                aquaticMob,
                school,
                gameTime
        );

        if (range != null) {
            if (tryMigrateFromDepletedRange(
                    level,
                    aquaticMob,
                    range,
                    school,
                    gameTime
            )) {
                return true;
            }

            if (tryReturnToSchoolRange(
                    aquaticMob,
                    range,
                    gameTime
            )) {
                return true;
            }
        }

        if (!school.hasMembers()) {
            stopSchooling(aquaticMob);
            return false;
        }

        double distanceSquared = aquaticMob.position().distanceToSqr(school.center());

        if (distanceSquared <= FISH_SETTLE_DISTANCE_BLOCKS * FISH_SETTLE_DISTANCE_BLOCKS) {
            stopSchooling(aquaticMob);
            return false;
        }

        boolean alreadySchooling = RetoldAiControl.isControlledAsBy(
                aquaticMob,
                RetoldAiControlMode.REGROUP,
                CONTROL_OWNER
        );
        if (!alreadySchooling
                && distanceSquared < FISH_JOIN_DISTANCE_BLOCKS * FISH_JOIN_DISTANCE_BLOCKS) {
            return false;
        }

        if (!RetoldAiControl.tryClaim(
                aquaticMob,
                RetoldAiControlMode.REGROUP,
                CONTROL_OWNER,
                RetoldAiPriorities.REGROUP,
                REASON_FISH_SCHOOL,
                gameTime,
                CONTROL_TICKS
        )) {
            return false;
        }

        aquaticMob.setSprinting(false);

        return RetoldBehaviorMovement.throttledMoveTo(
                aquaticMob,
                school.center().x,
                school.center().y,
                school.center().z,
                FISH_SPEED,
                gameTime,
                PATH_INTERVAL_TICKS,
                2.0D * 2.0D
        );
    }

    private static SchoolInfo findSchool(
            ServerLevel level,
            PathfinderMob aquaticMob,
            long gameTime
    ) {
        List<PathfinderMob> candidates = RetoldAiScanCache.nearby(
                level,
                aquaticMob,
                PathfinderMob.class,
                FISH_SEARCH_RADIUS_BLOCKS,
                gameTime,
                SCAN_CACHE_TICKS
        );

        double x = 0.0D;
        double y = 0.0D;
        double z = 0.0D;
        List<PathfinderMob> members = new ArrayList<>();

        for (PathfinderMob candidate : candidates) {
            if (!isAvailableSchoolMember(aquaticMob, candidate)) {
                continue;
            }

            x += candidate.getX();
            y += candidate.getY();
            z += candidate.getZ();
            members.add(candidate);
        }

        if (members.isEmpty()) {
            return SchoolInfo.empty();
        }

        return new SchoolInfo(
                new Vec3(
                        x / members.size(),
                        y / members.size(),
                        z / members.size()
                ),
                List.copyOf(members)
        );
    }

    private static RetoldAnimalHomeMemory ensureSchoolRange(
            ServerLevel level,
            PathfinderMob aquaticMob,
            SchoolInfo school,
            long gameTime
    ) {
        RetoldAnimalHomeMemory existing = RetoldAnimalHomes.get(aquaticMob);

        if (RetoldAnimalHomes.isValidFor(level, aquaticMob, existing)) {
            if ((gameTime + aquaticMob.getId()) % (20 * 10) == 0L) {
                RetoldAnimalHomes.markUsed(aquaticMob, gameTime);
            }
            return existing;
        }

        if (!school.hasMembers()) {
            return null;
        }

        List<PathfinderMob> members = school.members().stream()
                .limit(Math.max(
                        0,
                        RetoldAnimalSocialGroups.maxHomeGroupSize(aquaticMob) - 1L
                ))
                .toList();

        return RetoldAnimalHomes.getOrCreatePackHome(
                level,
                aquaticMob,
                members,
                schoolCenter(aquaticMob, members),
                gameTime
        );
    }

    private static boolean tryMigrateFromDepletedRange(
            ServerLevel level,
            PathfinderMob aquaticMob,
            RetoldAnimalHomeMemory range,
            SchoolInfo school,
            long gameTime
    ) {
        if (range.type() != RetoldAnimalHomeType.AQUATIC_SCHOOL_RANGE
                || !isRangeLeader(level, aquaticMob, range, school)
                || RetoldAiControl.getMode(aquaticMob) != RetoldAiControlMode.NONE) {
            return false;
        }

        RetoldMobState state = RetoldMobStates.getOrCreate(
                aquaticMob,
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
                aquaticMob,
                range.pos(),
                RANGE_FORAGE_HORIZONTAL_BLOCKS,
                RANGE_FORAGE_VERTICAL_BLOCKS,
                gameTime,
                RANGE_FORAGE_CACHE_TICKS
        );

        if (currentScore > RANGE_DEPLETED_FORAGE_SCORE) {
            return false;
        }

        BlockPos newRangeCenter = RetoldRangeForage.findBetterForageCenter(
                level,
                aquaticMob,
                range.pos(),
                RANGE_FORAGE_HORIZONTAL_BLOCKS,
                RANGE_FORAGE_VERTICAL_BLOCKS,
                currentScore,
                RANGE_TARGET_FORAGE_SCORE,
                gameTime,
                RANGE_FORAGE_CACHE_TICKS
        );

        if (newRangeCenter == null
                || !RetoldAiControl.tryClaim(
                aquaticMob,
                RetoldAiControlMode.REGROUP,
                CONTROL_OWNER,
                RetoldAiPriorities.above(RetoldAiPriorities.REGROUP, 1),
                REASON_MIGRATE_SCHOOL_RANGE,
                gameTime,
                RANGE_CONTROL_TICKS
        )) {
            return false;
        }

        RetoldAnimalHomes.replacePackHome(
                level,
                aquaticMob,
                currentRangeMembers(level, aquaticMob, range, school),
                newRangeCenter,
                gameTime
        );

        aquaticMob.setSprinting(false);

        RetoldBehaviorMovement.throttledMoveTo(
                aquaticMob,
                newRangeCenter,
                RANGE_MIGRATION_SPEED,
                gameTime,
                PATH_INTERVAL_TICKS,
                2.0D * 2.0D
        );
        return true;
    }

    private static boolean tryReturnToSchoolRange(
            PathfinderMob aquaticMob,
            RetoldAnimalHomeMemory range,
            long gameTime
    ) {
        double distanceSquared = aquaticMob.blockPosition().distSqr(range.pos());

        if (distanceSquared <= RANGE_RETURN_STOP_SQUARED) {
            if (RetoldAiControl.clearIfControlledAsByWithReason(
                    aquaticMob,
                    RetoldAiControlMode.REGROUP,
                    CONTROL_OWNER,
                    REASON_RETURN_SCHOOL_RANGE
            )) {
                aquaticMob.getNavigation().stop();
            }

            return false;
        }

        if (distanceSquared < RANGE_RETURN_START_SQUARED
                && !RetoldAiControl.isControlledAsByWithReason(
                aquaticMob,
                RetoldAiControlMode.REGROUP,
                CONTROL_OWNER,
                REASON_RETURN_SCHOOL_RANGE
        )) {
            return false;
        }

        if (!RetoldAiControl.tryClaim(
                aquaticMob,
                RetoldAiControlMode.REGROUP,
                CONTROL_OWNER,
                RetoldAiPriorities.REGROUP,
                REASON_RETURN_SCHOOL_RANGE,
                gameTime,
                RANGE_CONTROL_TICKS
        )) {
            return false;
        }

        aquaticMob.setSprinting(false);

        return RetoldBehaviorMovement.throttledMoveTo(
                aquaticMob,
                range.pos(),
                FISH_SPEED,
                gameTime,
                PATH_INTERVAL_TICKS,
                2.0D * 2.0D
        );
    }

    private static boolean isRangeLeader(
            ServerLevel level,
            PathfinderMob aquaticMob,
            RetoldAnimalHomeMemory range,
            SchoolInfo school
    ) {
        for (PathfinderMob member : currentRangeMembers(
                level,
                aquaticMob,
                range,
                school
        )) {
            if (member.getId() < aquaticMob.getId()) {
                return false;
            }
        }

        return true;
    }

    private static List<PathfinderMob> currentRangeMembers(
            ServerLevel level,
            PathfinderMob aquaticMob,
            RetoldAnimalHomeMemory range,
            SchoolInfo school
    ) {
        List<PathfinderMob> members = new ArrayList<>();

        for (PathfinderMob member : school.members()) {
            if (RetoldAnimalHomes.hasSameValidHomeAs(level, member, range)) {
                members.add(member);
            }
        }

        if (!members.isEmpty()) {
            return List.copyOf(members);
        }

        double radius = RetoldAnimalSocialGroups.homeSeparationBlocks(aquaticMob);

        return RetoldAiScanCache.nearbyAt(
                level,
                range.pos(),
                PathfinderMob.class,
                radius,
                level.getGameTime(),
                RANGE_MEMBER_SCAN_CACHE_TICKS
        ).stream()
                .filter(candidate -> candidate != aquaticMob
                        && RetoldAnimalSocialGroups.canSchoolWith(aquaticMob, candidate)
                        && RetoldAnimalHomes.hasSameValidHomeAs(level, candidate, range))
                .toList();
    }

    private static BlockPos schoolCenter(
            PathfinderMob aquaticMob,
            List<PathfinderMob> members
    ) {
        double x = aquaticMob.getX();
        double y = aquaticMob.getY();
        double z = aquaticMob.getZ();

        for (PathfinderMob member : members) {
            x += member.getX();
            y += member.getY();
            z += member.getZ();
        }

        int count = members.size() + 1;
        return BlockPos.containing(x / count, y / count, z / count).immutable();
    }

    private static boolean isAvailableSchoolMember(
            PathfinderMob aquaticMob,
            PathfinderMob candidate
    ) {
        if (!RetoldAnimalSocialGroups.canSchoolWith(aquaticMob, candidate)) {
            return false;
        }

        if (!RetoldBehaviorCoordinator.isAliveInSameLevel(aquaticMob, candidate)) {
            return false;
        }

        if (RetoldBehaviorCoordinator.hasLiveTarget(candidate)) {
            return false;
        }

        RetoldAiControlMode mode = RetoldAiControl.getMode(candidate);

        return mode == RetoldAiControlMode.NONE
                || RetoldAiControl.isControlledAsBy(
                candidate,
                RetoldAiControlMode.REGROUP,
                CONTROL_OWNER
        );
    }

    private static boolean canSchoolNow(PathfinderMob aquaticMob) {
        if (!aquaticMob.isAlive()
                || aquaticMob.isRemoved()
                || aquaticMob.isNoAi()
                || aquaticMob.isPassenger()
                || aquaticMob.isLeashed()
                || RetoldBehaviorCoordinator.hasLiveTarget(aquaticMob)) {
            stopSchooling(aquaticMob);
            return false;
        }

        RetoldAiControlMode mode = RetoldAiControl.getMode(aquaticMob);

        return mode == RetoldAiControlMode.NONE
                || RetoldAiControl.isControlledAsBy(
                aquaticMob,
                RetoldAiControlMode.REGROUP,
                CONTROL_OWNER
        );
    }

    private static boolean isSupported(PathfinderMob aquaticMob) {
        return aquaticMob != null
                && RetoldMobRules.isAquaticSchool(aquaticMob);
    }

    private static void stopSchooling(PathfinderMob aquaticMob) {
        if (aquaticMob == null) {
            return;
        }

        if (RetoldAiControl.isControlledAsBy(
                aquaticMob,
                RetoldAiControlMode.REGROUP,
                CONTROL_OWNER
        )) {
            RetoldAiControl.clear(aquaticMob);
            aquaticMob.getNavigation().stop();
        }
    }

    private record SchoolInfo(Vec3 center, List<PathfinderMob> members) {
        private static SchoolInfo empty() {
            return new SchoolInfo(Vec3.ZERO, List.of());
        }

        private boolean hasMembers() {
            return !members.isEmpty();
        }
    }
}
