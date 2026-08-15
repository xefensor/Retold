package cz.xefensor.retold.behavior.food;

import cz.xefensor.retold.behavior.performance.RetoldAiLod;
import cz.xefensor.retold.behavior.performance.RetoldAiWorkBudget;
import cz.xefensor.retold.behavior.performance.RetoldBehaviorPerf;
import cz.xefensor.retold.behavior.core.RetoldMobGriefing;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class RetoldForageBlockSearch {
    private static final double MAX_CENTER_DRIFT_SQUARED = 5.0D * 5.0D;
    private static final int MIN_FORAGE_TARGET_CACHE_TICKS = 40;
    private static final int ENVIRONMENTAL_FORAGE_MISS_CACHE_TICKS = 8;

    private static final Map<PathfinderMob, List<ForageTargetEntry>> TARGETS = new WeakHashMap<>();
    private static final Map<PathfinderMob, Boolean> DEFERRED_SEARCHES = new WeakHashMap<>();

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

    /**
     * Performs one budgeted local scan for the real forage blocks that may be
     * consumed during unloaded reconciliation. Unlike the loaded target cache,
     * this returns several distinct blocks so a multi-day transaction never
     * needs to mutate the world before asking for another search budget.
     */
    public static synchronized CatchUpFindResult findCatchUpForageBlocks(
            ServerLevel level,
            PathfinderMob mob,
            int horizontalRadius,
            int verticalRadius,
            double maxDistanceSquared,
            long gameTime,
            int maximumTargets
    ) {
        if (level == null
                || mob == null
                || mob.level() != level
                || maximumTargets <= 0) {
            return CatchUpFindResult.none();
        }

        if (!RetoldAiWorkBudget.tryUseFairBlockSearch(mob, gameTime)) {
            RetoldBehaviorPerf.recordBlockSearchCache(false);
            RetoldBehaviorPerf.recordBlockSearchBudgetSkip();
            return CatchUpFindResult.deferredResult();
        }

        RetoldBehaviorPerf.recordBlockSearchCache(false);
        BlockPos center = mob.blockPosition();
        List<BlockPos> targets = new ArrayList<>();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        long positionsChecked = 0L;

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
                    positionsChecked++;

                    if (level.isOutsideBuildHeight(mutable)) {
                        continue;
                    }

                    BlockState state = level.getBlockState(mutable);
                    boolean renewable = RetoldMobRules
                            .isRenewableEnvironmentalForage(mob, state);

                    if (!RetoldMobRules.canForageBlock(mob, state)
                            || !renewable
                            && !RetoldMobGriefing.canBreakBlock(
                            level,
                            mob,
                            mutable
                    )
                            || !hasCatchUpAccess(level, mob, mutable, state)) {
                        continue;
                    }

                    targets.add(mutable.immutable());
                }
            }
        }

        targets.sort(Comparator.comparingDouble(center::distSqr));
        RetoldBehaviorPerf.recordBlockTargetPositionsChecked(positionsChecked);

        if (targets.size() > maximumTargets) {
            targets = new ArrayList<>(targets.subList(0, maximumTargets));
        }

        return new CatchUpFindResult(List.copyOf(targets), false);
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
                DEFERRED_SEARCHES.remove(mob);
                RetoldBehaviorPerf.recordBlockSearchCache(true);
                return entry.target;
            }
        }

        if (!RetoldAiWorkBudget.tryUseFairBlockSearch(mob, gameTime)) {
            DEFERRED_SEARCHES.put(mob, Boolean.TRUE);
            RetoldBehaviorPerf.recordBlockSearchCache(false);
            RetoldBehaviorPerf.recordBlockSearchBudgetSkip();
            return null;
        }

        DEFERRED_SEARCHES.remove(mob);
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
                gameTime + cacheLifetimeTicks(mob, target, cacheTicks),
                target
        ));

        return target;
    }

    public static synchronized boolean isSearchDeferred(PathfinderMob mob) {
        return mob != null && DEFERRED_SEARCHES.containsKey(mob);
    }

    private static boolean hasCatchUpAccess(
            ServerLevel level,
            PathfinderMob mob,
            BlockPos foragePos,
            BlockState state
    ) {
        if (state.getCollisionShape(level, foragePos).isEmpty()) {
            return mob.getNavigation().isStableDestination(foragePos);
        }

        int accessDistance = mob.getBbWidth() > 1.0F ? 2 : 1;

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos candidate = foragePos.relative(direction, accessDistance);

            if (mob.getNavigation().isStableDestination(candidate)
                    && level.getBlockState(candidate)
                    .getCollisionShape(level, candidate)
                    .isEmpty()
                    && level.getBlockState(candidate.above())
                    .getCollisionShape(level, candidate.above())
                    .isEmpty()) {
                return true;
            }
        }

        return false;
    }

    private static int cacheLifetimeTicks(
            PathfinderMob mob,
            BlockPos target,
            int cacheTicks
    ) {
        if (target == null && RetoldMobRules.usesRenewableEnvironmentalForage(mob)) {
            return ENVIRONMENTAL_FORAGE_MISS_CACHE_TICKS;
        }

        return Math.max(
                MIN_FORAGE_TARGET_CACHE_TICKS,
                RetoldAiLod.cacheTicks(mob, cacheTicks)
        );
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

    public record CatchUpFindResult(
            List<BlockPos> targets,
            boolean deferred
    ) {
        private static CatchUpFindResult none() {
            return new CatchUpFindResult(List.of(), false);
        }

        private static CatchUpFindResult deferredResult() {
            return new CatchUpFindResult(List.of(), true);
        }
    }
}
