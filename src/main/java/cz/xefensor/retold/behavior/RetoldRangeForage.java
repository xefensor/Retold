package cz.xefensor.retold.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;

public final class RetoldRangeForage {
    private static final int[] SAMPLE_DISTANCES = {10, 16, 24, 32};
    private static final int[] SAMPLE_DIRECTIONS_X = {1, 1, 0, -1, -1, -1, 0, 1};
    private static final int[] SAMPLE_DIRECTIONS_Z = {0, 1, 1, 1, 0, -1, -1, -1};

    private RetoldRangeForage() {
    }

    public static int forageScore(
            ServerLevel level,
            PathfinderMob mob,
            BlockPos center,
            int horizontalRadius,
            int verticalRadius
    ) {
        if (level == null || mob == null || center == null) {
            return 0;
        }

        if (!isChunkLoaded(level, center)) {
            return 0;
        }

        int score = 0;
        int radiusSquared = horizontalRadius * horizontalRadius;
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int dx = -horizontalRadius; dx <= horizontalRadius; dx++) {
            for (int dz = -horizontalRadius; dz <= horizontalRadius; dz++) {
                if (dx * dx + dz * dz > radiusSquared) {
                    continue;
                }

                for (int dy = -verticalRadius; dy <= verticalRadius; dy++) {
                    mutable.set(
                            center.getX() + dx,
                            center.getY() + dy,
                            center.getZ() + dz
                    );

                    if (level.isOutsideBuildHeight(mutable)) {
                        continue;
                    }

                    if (RetoldMobRules.canForageBlock(mob, level.getBlockState(mutable))) {
                        score++;
                    }
                }
            }
        }

        return score;
    }

    public static BlockPos findBetterForageCenter(
            ServerLevel level,
            PathfinderMob mob,
            BlockPos origin,
            int horizontalRadius,
            int verticalRadius,
            int currentScore,
            int minimumCandidateScore
    ) {
        if (level == null || mob == null || origin == null) {
            return null;
        }

        BlockPos best = null;
        int bestScore = Math.max(
                currentScore,
                minimumCandidateScore - 1
        );

        for (int distance : SAMPLE_DISTANCES) {
            for (int index = 0; index < SAMPLE_DIRECTIONS_X.length; index++) {
                BlockPos candidate = origin.offset(
                        SAMPLE_DIRECTIONS_X[index] * distance,
                        0,
                        SAMPLE_DIRECTIONS_Z[index] * distance
                );

                if (!isChunkLoaded(level, candidate)) {
                    continue;
                }

                int score = forageScore(
                        level,
                        mob,
                        candidate,
                        horizontalRadius,
                        verticalRadius
                );

                if (score > bestScore) {
                    bestScore = score;
                    best = candidate.immutable();
                }
            }
        }

        return best;
    }

    private static boolean isChunkLoaded(
            ServerLevel level,
            BlockPos pos
    ) {
        return level.hasChunk(
                SectionPos.blockToSectionCoord(pos.getX()),
                SectionPos.blockToSectionCoord(pos.getZ())
        );
    }
}
