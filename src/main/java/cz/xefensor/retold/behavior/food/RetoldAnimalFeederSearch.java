package cz.xefensor.retold.behavior.food;

import cz.xefensor.retold.behavior.performance.RetoldAiLod;
import cz.xefensor.retold.behavior.performance.RetoldAiWorkBudget;
import cz.xefensor.retold.behavior.performance.RetoldBehaviorPerf;
import cz.xefensor.retold.block.AnimalFeederBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;

import java.util.Map;
import java.util.WeakHashMap;

final class RetoldAnimalFeederSearch {
    private static final double MAX_CENTER_DRIFT_SQUARED = 5.0D * 5.0D;
    private static final int MIN_CACHE_TICKS = 60;

    private static final Map<PathfinderMob, FeederTarget> TARGETS = new WeakHashMap<>();

    private RetoldAnimalFeederSearch() {
    }

    static synchronized BlockPos find(
            ServerLevel level,
            PathfinderMob mob,
            int horizontalRadius,
            int verticalRadius,
            long gameTime,
            int cacheTicks
    ) {
        if (level == null || mob == null) {
            return null;
        }

        BlockPos center = mob.blockPosition();
        FeederTarget cached = TARGETS.get(mob);

        if (cached != null
                && gameTime < cached.expiresAt()
                && cached.horizontalRadius() == horizontalRadius
                && cached.verticalRadius() == verticalRadius
                && center.distSqr(cached.center()) <= MAX_CENTER_DRIFT_SQUARED
                && isValid(level, mob, cached.target())) {
            RetoldBehaviorPerf.recordBlockSearchCache(true);
            return cached.target();
        }

        if (!RetoldAiWorkBudget.tryUseBlockSearch(gameTime)) {
            RetoldBehaviorPerf.recordBlockSearchCache(false);
            RetoldBehaviorPerf.recordBlockSearchBudgetSkip();
            return null;
        }

        RetoldBehaviorPerf.recordBlockSearchCache(false);

        BlockPos target = scan(
                level,
                mob,
                center,
                horizontalRadius,
                verticalRadius
        );

        TARGETS.put(
                mob,
                new FeederTarget(
                        center.immutable(),
                        horizontalRadius,
                        verticalRadius,
                        gameTime + Math.max(
                                MIN_CACHE_TICKS,
                                RetoldAiLod.cacheTicks(mob, cacheTicks)
                        ),
                        target
                )
        );

        return target;
    }

    private static BlockPos scan(
            ServerLevel level,
            PathfinderMob mob,
            BlockPos center,
            int horizontalRadius,
            int verticalRadius
    ) {
        BlockPos best = null;
        double bestScore = Double.MAX_VALUE;
        long positionsChecked = 0L;
        double maxDistanceSquared = horizontalRadius * horizontalRadius;
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int dx = -horizontalRadius; dx <= horizontalRadius; dx++) {
            for (int dy = -verticalRadius; dy <= verticalRadius; dy++) {
                for (int dz = -horizontalRadius; dz <= horizontalRadius; dz++) {
                    double distanceSquared = dx * dx + dy * dy + dz * dz;

                    if (distanceSquared > maxDistanceSquared) {
                        continue;
                    }

                    positionsChecked++;
                    mutable.set(
                            center.getX() + dx,
                            center.getY() + dy,
                            center.getZ() + dz
                    );

                    if (!isValid(level, mob, mutable)) {
                        continue;
                    }

                    double score = dx * dx + dy * dy * 1.5D + dz * dz;

                    if (score < bestScore) {
                        bestScore = score;
                        best = mutable.immutable();
                    }
                }
            }
        }

        RetoldBehaviorPerf.recordBlockTargetPositionsChecked(positionsChecked);
        return best;
    }

    private static boolean isValid(
            ServerLevel level,
            PathfinderMob mob,
            BlockPos pos
    ) {
        if (pos == null) {
            return true;
        }

        if (level.isOutsideBuildHeight(pos)) {
            return false;
        }

        return level.getBlockEntity(pos) instanceof AnimalFeederBlockEntity feeder
                && feeder.hasFoodFor(mob);
    }

    private record FeederTarget(
            BlockPos center,
            int horizontalRadius,
            int verticalRadius,
            long expiresAt,
            BlockPos target
    ) {
    }
}
