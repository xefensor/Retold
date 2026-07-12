package cz.xefensor.retold.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class RetoldRangeForage {
    private static final int[] SAMPLE_DISTANCES = {10, 16, 24, 32};
    private static final int[] SAMPLE_DIRECTIONS_X = {1, 1, 0, -1, -1, -1, 0, 1};
    private static final int[] SAMPLE_DIRECTIONS_Z = {0, 1, 1, 1, 0, -1, -1, -1};
    private static final int DEFAULT_FORAGE_SCORE_CACHE_TICKS = 80;

    private static final Map<ServerLevel, List<ForageScoreEntry>> FORAGE_SCORES = new WeakHashMap<>();

    private RetoldRangeForage() {
    }

    public static int forageScore(
            ServerLevel level,
            PathfinderMob mob,
            BlockPos center,
            int horizontalRadius,
            int verticalRadius
    ) {
        return forageScore(
                level,
                mob,
                center,
                horizontalRadius,
                verticalRadius,
                level != null ? level.getGameTime() : 0L,
                DEFAULT_FORAGE_SCORE_CACHE_TICKS
        );
    }

    public static synchronized int forageScore(
            ServerLevel level,
            PathfinderMob mob,
            BlockPos center,
            int horizontalRadius,
            int verticalRadius,
            long gameTime,
            int cacheTicks
    ) {
        if (level == null || mob == null || center == null) {
            return 0;
        }

        if (!isChunkLoaded(level, center)) {
            return 0;
        }

        BlockPos immutableCenter = center.immutable();
        Identifier mobType = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        List<ForageScoreEntry> entries = FORAGE_SCORES.computeIfAbsent(
                level,
                ignored -> new ArrayList<>()
        );

        entries.removeIf(entry -> gameTime >= entry.expiresAt);

        for (ForageScoreEntry entry : entries) {
            if (
                    entry.mobType.equals(mobType)
                            && entry.center.equals(immutableCenter)
                            && entry.horizontalRadius == horizontalRadius
                            && entry.verticalRadius == verticalRadius
            ) {
                RetoldBehaviorPerf.recordBlockSearchCache(true);
                return entry.score;
            }
        }

        RetoldBehaviorPerf.recordBlockSearchCache(false);

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

        entries.add(new ForageScoreEntry(
                mobType,
                immutableCenter,
                horizontalRadius,
                verticalRadius,
                gameTime + Math.max(1, cacheTicks),
                score
        ));

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
        return findBetterForageCenter(
                level,
                mob,
                origin,
                horizontalRadius,
                verticalRadius,
                currentScore,
                minimumCandidateScore,
                level != null ? level.getGameTime() : 0L,
                DEFAULT_FORAGE_SCORE_CACHE_TICKS
        );
    }

    public static BlockPos findBetterForageCenter(
            ServerLevel level,
            PathfinderMob mob,
            BlockPos origin,
            int horizontalRadius,
            int verticalRadius,
            int currentScore,
            int minimumCandidateScore,
            long gameTime,
            int cacheTicks
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
                        verticalRadius,
                        gameTime,
                        cacheTicks
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

    private record ForageScoreEntry(
            Identifier mobType,
            BlockPos center,
            int horizontalRadius,
            int verticalRadius,
            long expiresAt,
            int score
    ) {
    }
}
