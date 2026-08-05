package cz.xefensor.retold.behavior.species;

import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.control.RetoldAiControlMode;
import cz.xefensor.retold.behavior.control.RetoldAiControlOwner;
import cz.xefensor.retold.behavior.control.RetoldAiPriorities;
import cz.xefensor.retold.behavior.core.RetoldBehaviorCoordinator;
import cz.xefensor.retold.behavior.core.RetoldBehaviorMovement;
import cz.xefensor.retold.behavior.home.RetoldAnimalSocialGroups;
import cz.xefensor.retold.behavior.performance.RetoldAiScanCache;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class RetoldAquaticSchoolEvents {
    private static final RetoldAiControlOwner CONTROL_OWNER =
            RetoldAiControlOwner.AQUATIC_SCHOOL;
    private static final String REASON_FISH_SCHOOL = "join_fish_school";

    private static final int SCAN_CACHE_TICKS = 10;
    private static final int PATH_INTERVAL_TICKS = 10;
    private static final int CONTROL_TICKS = 20 * 4;

    private static final double FISH_SEARCH_RADIUS_BLOCKS = 12.0D;
    private static final double FISH_JOIN_DISTANCE_BLOCKS = 6.0D;
    private static final double FISH_SETTLE_DISTANCE_BLOCKS = 3.0D;
    private static final double FISH_SPEED = 1.0D;

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
        int count = 0;

        for (PathfinderMob candidate : candidates) {
            if (!isAvailableSchoolMember(aquaticMob, candidate)) {
                continue;
            }

            x += candidate.getX();
            y += candidate.getY();
            z += candidate.getZ();
            count++;
        }

        if (count == 0) {
            return SchoolInfo.empty();
        }

        return new SchoolInfo(
                new Vec3(x / count, y / count, z / count),
                count
        );
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

    private record SchoolInfo(Vec3 center, int members) {
        private static SchoolInfo empty() {
            return new SchoolInfo(Vec3.ZERO, 0);
        }

        private boolean hasMembers() {
            return members > 0;
        }
    }
}
