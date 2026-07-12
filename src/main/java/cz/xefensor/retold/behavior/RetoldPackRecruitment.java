package cz.xefensor.retold.behavior;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class RetoldPackRecruitment {
    private static final int PACK_RECRUIT_SCAN_CACHE_TICKS = 6;

    private RetoldPackRecruitment() {
    }

    static void recruitPartyIfOpen(
            ServerLevel level,
            PathfinderMob leader,
            RetoldPackParty party,
            long gameTime
    ) {
        if (level == null || leader == null || party == null) {
            return;
        }

        if (!RetoldPackAnimals.isValidPackAnimal(leader)) {
            return;
        }

        String path = getPath(leader);
        int maxPartySize = RetoldPackTuning.maxPartySize(path);
        int currentPartySize = 1 + party.members.size();

        if (currentPartySize >= maxPartySize) {
            return;
        }

        double radius = RetoldPackTuning.packRadius(path);
        double radiusSquared = radius * radius;
        LivingEntity currentPrey = RetoldAiControl.isControlledAs(leader, RetoldAiControlMode.HUNT)
                ? leader.getTarget()
                : null;

        List<PathfinderMob> candidates = new ArrayList<>(RetoldAiScanCache.nearby(
                level,
                leader,
                PathfinderMob.class,
                radius,
                gameTime,
                PACK_RECRUIT_SCAN_CACHE_TICKS
        ));
        candidates.removeIf(candidate -> !isLateJoinCandidate(
                leader,
                candidate,
                party,
                currentPrey,
                gameTime,
                radiusSquared
        ));

        candidates.sort(
                Comparator.comparingDouble(candidate -> leader.distanceToSqr(candidate))
        );

        int openSlots = maxPartySize - currentPartySize;
        int joined = 0;

        for (PathfinderMob candidate : candidates) {
            if (joined >= openSlots) {
                return;
            }

            RetoldPackParties.addMember(
                    leader,
                    party,
                    candidate
            );
            RetoldAnimalHomes.getOrCreatePackHome(
                    level,
                    leader,
                    party.members,
                    party.packCenter,
                    gameTime
            );

            joined++;
        }
    }

    static boolean canBeSelectedForHuntingParty(
            PathfinderMob leader,
            PathfinderMob candidate,
            long gameTime
    ) {
        if (candidate == null) {
            return false;
        }

        if (RetoldPackParties.hasLeader(candidate)) {
            return false;
        }

        RetoldAiControlMode mode = RetoldAiControl.getMode(candidate);

        if (
                mode != RetoldAiControlMode.NONE
                        && mode != RetoldAiControlMode.SEARCH
                        && mode != RetoldAiControlMode.REGROUP
        ) {
            return false;
        }

        if (RetoldBehaviorCoordinator.hasLiveTarget(candidate)) {
            return false;
        }

        RetoldMobState state = RetoldMobStates.getOrCreate(
                candidate,
                gameTime
        );

        if (RetoldMobRules.hasHuntDrive(candidate, state)) {
            return true;
        }

        return leader != null
                && RetoldMobRules.isPackSocialHunter(candidate)
                && RetoldMobRules.hasEatDrive(
                        candidate,
                        state
                );
    }

    private static boolean isLateJoinCandidate(
            PathfinderMob leader,
            PathfinderMob candidate,
            RetoldPackParty party,
            LivingEntity currentPrey,
            long gameTime,
            double radiusSquared
    ) {
        if (leader == null || candidate == null || party == null) {
            return false;
        }

        if (leader == candidate) {
            return false;
        }

        if (party.members.contains(candidate)) {
            return false;
        }

        if (RetoldPackParties.hasLeader(candidate)) {
            return false;
        }

        if (!isNormalPackCandidate(
                leader,
                candidate,
                radiusSquared
        )) {
            return false;
        }

        if (!canBeSelectedForHuntingParty(leader, candidate, gameTime)) {
            return false;
        }

        if (currentPrey != null) {
            return RetoldPackSenses.isValidPartyPrey(
                    candidate,
                    currentPrey,
                    gameTime
            );
        }

        return true;
    }

    private static boolean isNormalPackCandidate(
            PathfinderMob leader,
            PathfinderMob candidate,
            double radiusSquared
    ) {
        if (leader == null || candidate == null) {
            return false;
        }

        if (!RetoldPackAnimals.isValidPackAnimal(candidate)) {
            return false;
        }

        if (leader.level() != candidate.level()) {
            return false;
        }

        if (!getPath(leader).equals(getPath(candidate))) {
            return false;
        }

        return leader.distanceToSqr(candidate) <= radiusSquared;
    }

    private static String getPath(PathfinderMob mob) {
        return RetoldMobRules.getEntityTypePath(
                mob.getType()
        );
    }
}
