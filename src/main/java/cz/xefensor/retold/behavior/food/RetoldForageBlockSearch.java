package cz.xefensor.retold.behavior.food;

import cz.xefensor.retold.behavior.performance.RetoldAiLod;
import cz.xefensor.retold.behavior.performance.RetoldAiWorkBudget;
import cz.xefensor.retold.behavior.performance.RetoldBehaviorPerf;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class RetoldForageBlockSearch {
    private static final double MAX_CENTER_DRIFT_SQUARED = 5.0D * 5.0D;
    private static final int MIN_FORAGE_TARGET_CACHE_TICKS = 40;

    private static final Map<PathfinderMob, List<ForageTargetEntry>> TARGETS = new WeakHashMap<>();

    private RetoldForageBlockSearch() {
    }

    public static synchronized BlockPos findOrdinaryForageBlock(
            ServerLevel level,
            PathfinderMob mob,
            int horizontalRadius,
            int verticalRadius,
            double maxDistanceSquared,
            long gameTime,
            int cacheTicks
    ) {
        return findForageBlock(
                level,
                mob,
                ForageSearchMode.ORDINARY,
                horizontalRadius,
                verticalRadius,
                maxDistanceSquared,
                gameTime,
                cacheTicks
        );
    }

    public static synchronized BlockPos findNetherForageBlock(
            ServerLevel level,
            PathfinderMob mob,
            int horizontalRadius,
            int verticalRadius,
            double maxDistanceSquared,
            long gameTime,
            int cacheTicks
    ) {
        return findForageBlock(
                level,
                mob,
                ForageSearchMode.NETHER,
                horizontalRadius,
                verticalRadius,
                maxDistanceSquared,
                gameTime,
                cacheTicks
        );
    }

    private static BlockPos findForageBlock(
            ServerLevel level,
            PathfinderMob mob,
            ForageSearchMode mode,
            int horizontalRadius,
            int verticalRadius,
            double maxDistanceSquared,
            long gameTime,
            int cacheTicks
    ) {
        if (level == null || mob == null || mode == null) {
            return null;
        }

        BlockPos center = mob.blockPosition();
        Identifier mobType = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        List<ForageTargetEntry> entries = TARGETS.computeIfAbsent(
                mob,
                ignored -> new ArrayList<>()
        );

        entries.removeIf(entry -> gameTime >= entry.expiresAt);

        for (ForageTargetEntry entry : entries) {
            if (
                    entry.mode == mode
                            && entry.mobType.equals(mobType)
                            && entry.horizontalRadius == horizontalRadius
                            && entry.verticalRadius == verticalRadius
                            && Double.compare(entry.maxDistanceSquared, maxDistanceSquared) == 0
                            && center.distSqr(entry.center) <= MAX_CENTER_DRIFT_SQUARED
                            && isCachedTargetStillValid(level, mob, entry.target)
            ) {
                RetoldBehaviorPerf.recordBlockSearchCache(true);
                return entry.target;
            }
        }

        if (!RetoldAiWorkBudget.tryUseBlockSearch(gameTime)) {
            RetoldBehaviorPerf.recordBlockSearchCache(false);
            RetoldBehaviorPerf.recordBlockSearchBudgetSkip();
            return null;
        }

        RetoldBehaviorPerf.recordBlockSearchCache(false);

        BlockPos target = scanForageBlock(
                level,
                mob,
                mode,
                center,
                horizontalRadius,
                verticalRadius,
                maxDistanceSquared
        );

        entries.removeIf(entry ->
                entry.mode == mode
                        && entry.mobType.equals(mobType)
                        && entry.horizontalRadius == horizontalRadius
                        && entry.verticalRadius == verticalRadius
                        && Double.compare(entry.maxDistanceSquared, maxDistanceSquared) == 0
        );
        entries.add(new ForageTargetEntry(
                mode,
                mobType,
                center.immutable(),
                horizontalRadius,
                verticalRadius,
                maxDistanceSquared,
                gameTime + Math.max(MIN_FORAGE_TARGET_CACHE_TICKS, RetoldAiLod.cacheTicks(mob, cacheTicks)),
                target
        ));

        return target;
    }

    private static BlockPos scanForageBlock(
            ServerLevel level,
            PathfinderMob mob,
            ForageSearchMode mode,
            BlockPos center,
            int horizontalRadius,
            int verticalRadius,
            double maxDistanceSquared
    ) {
        BlockPos best = null;
        double bestScore = Double.MAX_VALUE;
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int dx = -horizontalRadius; dx <= horizontalRadius; dx++) {
            for (int dy = -verticalRadius; dy <= verticalRadius; dy++) {
                for (int dz = -horizontalRadius; dz <= horizontalRadius; dz++) {
                    double distanceSquared = dx * dx + dy * dy + dz * dz;

                    if (distanceSquared > maxDistanceSquared) {
                        continue;
                    }

                    mutable.set(
                            center.getX() + dx,
                            center.getY() + dy,
                            center.getZ() + dz
                    );

                    if (level.isOutsideBuildHeight(mutable)) {
                        continue;
                    }

                    BlockState state = level.getBlockState(mutable);

                    if (!RetoldMobRules.canForageBlock(mob, state)) {
                        continue;
                    }

                    double score = distanceSquared;

                    if (mode == ForageSearchMode.NETHER && state.is(Blocks.CRIMSON_FUNGUS)) {
                        score -= 12.0D;
                    }

                    if (score < bestScore) {
                        bestScore = score;
                        best = mutable.immutable();
                    }
                }
            }
        }

        return best;
    }

    private static boolean isCachedTargetStillValid(
            ServerLevel level,
            PathfinderMob mob,
            BlockPos target
    ) {
        if (target == null) {
            return true;
        }

        if (level.isOutsideBuildHeight(target)) {
            return false;
        }

        return RetoldMobRules.canForageBlock(
                mob,
                level.getBlockState(target)
        );
    }

    private enum ForageSearchMode {
        ORDINARY,
        NETHER
    }

    private record ForageTargetEntry(
            ForageSearchMode mode,
            Identifier mobType,
            BlockPos center,
            int horizontalRadius,
            int verticalRadius,
            double maxDistanceSquared,
            long expiresAt,
            BlockPos target
    ) {
    }
}
