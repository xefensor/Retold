package cz.xefensor.retold.behavior.pack;

import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.control.RetoldAiControlMode;
import cz.xefensor.retold.behavior.control.RetoldAiControlOwner;
import cz.xefensor.retold.behavior.performance.RetoldAiScanCache;
import cz.xefensor.retold.behavior.home.RetoldAnimalHomeMemory;
import cz.xefensor.retold.behavior.home.RetoldAnimalHomes;
import cz.xefensor.retold.behavior.home.RetoldAnimalSocialGroups;
import cz.xefensor.retold.behavior.core.RetoldBehaviorCoordinator;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;
import cz.xefensor.retold.behavior.profiles.RetoldMobState;
import cz.xefensor.retold.behavior.profiles.RetoldMobStates;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class RetoldWolfDenCreation {
    private static final int WOLF_DEN_CREATION_SCAN_CACHE_TICKS = 10;

    private static final double WOLF_PASSIVE_DEN_RADIUS_BLOCKS = 18.0D;
    private static final double WOLF_PASSIVE_DEN_RADIUS_SQUARED =
            WOLF_PASSIVE_DEN_RADIUS_BLOCKS * WOLF_PASSIVE_DEN_RADIUS_BLOCKS;

    private RetoldWolfDenCreation() {
    }

    static RetoldAnimalHomeMemory tryCreatePassiveDenHome(
            ServerLevel level,
            PathfinderMob wolf,
            long gameTime
    ) {
        if (!canCreatePassiveDenHome(wolf, gameTime)) {
            return null;
        }

        List<PathfinderMob> candidates = new ArrayList<>(RetoldAiScanCache.nearby(
                level,
                wolf,
                PathfinderMob.class,
                WOLF_PASSIVE_DEN_RADIUS_BLOCKS,
                gameTime,
                WOLF_DEN_CREATION_SCAN_CACHE_TICKS
        ));
        candidates.removeIf(candidate -> !isPassiveDenCandidate(level, wolf, candidate, gameTime));

        if (candidates.isEmpty()) {
            return null;
        }

        candidates.sort(
                Comparator.comparingDouble(candidate -> wolf.distanceToSqr(candidate))
        );

        List<PathfinderMob> members = new ArrayList<>();
        int maxMembers = Math.max(
                0,
                RetoldAnimalSocialGroups.maxHomeGroupSize(wolf) - 1
        );

        for (PathfinderMob candidate : candidates) {
            if (members.size() >= maxMembers) {
                break;
            }

            members.add(candidate);
        }

        return RetoldAnimalHomes.getOrCreatePackHome(
                level,
                wolf,
                members,
                calculateHomeCenter(wolf, members),
                gameTime
        );
    }

    private static boolean canCreatePassiveDenHome(
            PathfinderMob wolf,
            long gameTime
    ) {
        if (!isWolf(wolf)) {
            return false;
        }

        if (!RetoldBehaviorCoordinator.canStartLowPriorityHomeBehavior(wolf)) {
            return false;
        }

        RetoldMobState state = RetoldMobStates.getOrCreate(
                wolf,
                gameTime
        );

        return !RetoldMobRules.hasHuntDrive(wolf, state);
    }

    private static boolean isPassiveDenCandidate(
            ServerLevel level,
            PathfinderMob leader,
            PathfinderMob candidate,
            long gameTime
    ) {
        if (leader == null || candidate == null || leader == candidate) {
            return false;
        }

        if (!isWolf(candidate)) {
            return false;
        }

        if (!RetoldPackAnimals.isValidPackAnimal(candidate)) {
            return false;
        }

        if (leader.level() != candidate.level()) {
            return false;
        }

        if (leader.distanceToSqr(candidate) > WOLF_PASSIVE_DEN_RADIUS_SQUARED) {
            return false;
        }

        if (RetoldBehaviorCoordinator.hasLiveTarget(candidate)) {
            return false;
        }

        RetoldAiControlMode candidateMode = RetoldAiControl.getMode(candidate);

        if (
                candidateMode != RetoldAiControlMode.NONE
                        && !RetoldAiControl.isControlledAsBy(
                        candidate,
                        RetoldAiControlMode.REGROUP,
                        RetoldAiControlOwner.REGROUP
                )
        ) {
            return false;
        }

        RetoldAnimalHomeMemory candidateHome = RetoldAnimalHomes.get(candidate);

        if (
                candidateHome != null
                        && !RetoldAnimalHomes.isValidFor(
                        level,
                        candidate,
                        candidateHome
                )
        ) {
            return false;
        }

        RetoldMobState state = RetoldMobStates.getOrCreate(
                candidate,
                gameTime
        );

        return !RetoldMobRules.hasHuntDrive(candidate, state);
    }

    private static BlockPos calculateHomeCenter(
            PathfinderMob leader,
            List<PathfinderMob> members
    ) {
        double x = leader.getX();
        double y = leader.getY();
        double z = leader.getZ();
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

    private static boolean isWolf(PathfinderMob mob) {
        return RetoldMobRules.isWolf(mob);
    }
}
