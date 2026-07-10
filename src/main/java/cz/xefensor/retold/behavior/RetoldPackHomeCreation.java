package cz.xefensor.retold.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class RetoldPackHomeCreation {
    private static final double PASSIVE_HOME_RADIUS_BLOCKS = 18.0D;
    private static final double PASSIVE_HOME_RADIUS_SQUARED =
            PASSIVE_HOME_RADIUS_BLOCKS * PASSIVE_HOME_RADIUS_BLOCKS;

    private RetoldPackHomeCreation() {
    }

    static RetoldAnimalHomeMemory tryCreatePassiveHome(
            ServerLevel level,
            PathfinderMob leader,
            long gameTime
    ) {
        if (!canCreatePassiveHome(leader, gameTime)) {
            return null;
        }

        AABB area = leader.getBoundingBox().inflate(PASSIVE_HOME_RADIUS_BLOCKS);

        List<PathfinderMob> candidates = level.getEntitiesOfClass(
                PathfinderMob.class,
                area,
                candidate -> isPassiveHomeCandidate(
                        level,
                        leader,
                        candidate,
                        gameTime
                )
        );

        if (candidates.isEmpty()) {
            return null;
        }

        candidates.sort(
                Comparator.comparingDouble(candidate -> leader.distanceToSqr(candidate))
        );

        List<PathfinderMob> members = new ArrayList<>();
        int maxMembers = Math.max(
                0,
                RetoldAnimalSocialGroups.maxHomeGroupSize(leader) - 1
        );

        for (PathfinderMob candidate : candidates) {
            if (members.size() >= maxMembers) {
                break;
            }

            members.add(candidate);
        }

        return RetoldAnimalHomes.getOrCreatePackHome(
                level,
                leader,
                members,
                calculateHomeCenter(leader, members),
                gameTime
        );
    }

    private static boolean canCreatePassiveHome(
            PathfinderMob leader,
            long gameTime
    ) {
        if (!RetoldPackAnimals.isValidPackAnimal(leader)) {
            return false;
        }

        if (!RetoldBehaviorCoordinator.canStartLowPriorityHomeBehavior(leader)) {
            return false;
        }

        RetoldMobState state = RetoldMobStates.getOrCreate(
                leader,
                gameTime
        );

        return !RetoldMobRules.hasHuntDrive(leader, state);
    }

    private static boolean isPassiveHomeCandidate(
            ServerLevel level,
            PathfinderMob leader,
            PathfinderMob candidate,
            long gameTime
    ) {
        if (leader == null || candidate == null || leader == candidate) {
            return false;
        }

        if (!RetoldPackAnimals.isValidPackAnimal(candidate)) {
            return false;
        }

        if (!RetoldAnimalSocialGroups.canShareHomeOrRange(leader, candidate)) {
            return false;
        }

        if (leader.level() != candidate.level()) {
            return false;
        }

        if (leader.distanceToSqr(candidate) > PASSIVE_HOME_RADIUS_SQUARED) {
            return false;
        }

        if (candidate.getTarget() != null && candidate.getTarget().isAlive()) {
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
}
