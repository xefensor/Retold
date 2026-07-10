package cz.xefensor.retold.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class RetoldPackFormation {
    private RetoldPackFormation() {
    }

    static RetoldPackParty tryCreateHuntingParty(
            ServerLevel level,
            PathfinderMob leader,
            long gameTime
    ) {
        String path = getPath(leader);

        double radius = RetoldPackTuning.packRadius(path);
        double radiusSquared = radius * radius;
        int maxPartySize = RetoldPackTuning.maxPartySize(path);

        AABB area = leader.getBoundingBox().inflate(radius);

        List<PathfinderMob> nearbyPack = level.getEntitiesOfClass(
                PathfinderMob.class,
                area,
                candidate -> isNormalPackCandidate(
                        leader,
                        candidate,
                        radiusSquared
                )
        );

        nearbyPack.sort(
                Comparator.comparingDouble(candidate -> leader.distanceToSqr(candidate))
        );

        List<PathfinderMob> selectedMembers = new ArrayList<>();

        for (PathfinderMob candidate : nearbyPack) {
            if (candidate == leader) {
                continue;
            }

            if (selectedMembers.size() >= maxPartySize - 1) {
                break;
            }

            if (!RetoldPackRecruitment.canBeSelectedForHuntingParty(leader, candidate, gameTime)) {
                continue;
            }

            selectedMembers.add(candidate);
        }

        int totalPartySize = selectedMembers.size() + 1;

        if (totalPartySize < RetoldPackTuning.minPartySize(path)) {
            return null;
        }

        BlockPos calculatedPackCenter = RetoldPackGeometry.packCenter(
                leader,
                selectedMembers
        );
        RetoldAnimalHomeMemory home = RetoldAnimalHomes.getOrCreatePackHome(
                level,
                leader,
                selectedMembers,
                calculatedPackCenter,
                gameTime
        );
        BlockPos packCenter = home == null ? calculatedPackCenter : home.pos();

        return RetoldPackParties.createParty(
                leader,
                selectedMembers,
                packCenter,
                gameTime
        );
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
