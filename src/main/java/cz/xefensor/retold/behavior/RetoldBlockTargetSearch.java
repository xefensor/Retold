package cz.xefensor.retold.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class RetoldBlockTargetSearch {
    private static final double MAX_MOB_DRIFT_SQUARED = 5.0D * 5.0D;
    private static final int MIN_BLOCK_TARGET_CACHE_TICKS = 40;

    private static final Map<PathfinderMob, List<BlockTargetEntry>> TARGETS = new WeakHashMap<>();

    private RetoldBlockTargetSearch() {
    }

    public static synchronized BlockPos findFlower(
            ServerLevel level,
            PathfinderMob mob,
            int horizontalRadius,
            int verticalRadius,
            double maxDistanceSquared,
            long gameTime,
            int cacheTicks
    ) {
        return findTarget(
                level,
                mob,
                mob != null ? mob.blockPosition() : null,
                BlockSearchMode.FLOWER,
                horizontalRadius,
                verticalRadius,
                maxDistanceSquared,
                gameTime,
                cacheTicks
        );
    }

    public static synchronized BlockPos findBeachSand(
            ServerLevel level,
            PathfinderMob mob,
            int horizontalRadius,
            int verticalRadius,
            long gameTime,
            int cacheTicks
    ) {
        return findTarget(
                level,
                mob,
                mob != null ? mob.blockPosition() : null,
                BlockSearchMode.BEACH_SAND,
                horizontalRadius,
                verticalRadius,
                Double.MAX_VALUE,
                gameTime,
                cacheTicks
        );
    }

    public static synchronized BlockPos findWater(
            ServerLevel level,
            PathfinderMob mob,
            int horizontalRadius,
            int verticalRadius,
            long gameTime,
            int cacheTicks
    ) {
        return findTarget(
                level,
                mob,
                mob != null ? mob.blockPosition() : null,
                BlockSearchMode.WATER,
                horizontalRadius,
                verticalRadius,
                Double.MAX_VALUE,
                gameTime,
                cacheTicks
        );
    }

    public static synchronized BlockPos findWetland(
            ServerLevel level,
            PathfinderMob mob,
            int horizontalRadius,
            int verticalRadius,
            long gameTime,
            int cacheTicks
    ) {
        return findTarget(
                level,
                mob,
                mob != null ? mob.blockPosition() : null,
                BlockSearchMode.WETLAND,
                horizontalRadius,
                verticalRadius,
                Double.MAX_VALUE,
                gameTime,
                cacheTicks
        );
    }

    public static synchronized BlockPos findSnifferRangeAnchor(
            ServerLevel level,
            PathfinderMob mob,
            int horizontalRadius,
            int verticalRadius,
            long gameTime,
            int cacheTicks
    ) {
        return findTarget(
                level,
                mob,
                mob != null ? mob.blockPosition() : null,
                BlockSearchMode.SNIFFER_RANGE,
                horizontalRadius,
                verticalRadius,
                Double.MAX_VALUE,
                gameTime,
                cacheTicks
        );
    }

    public static synchronized BlockPos findSnifferDiggable(
            ServerLevel level,
            PathfinderMob mob,
            BlockPos searchCenter,
            int horizontalRadius,
            int verticalRadius,
            long gameTime,
            int cacheTicks
    ) {
        return findTarget(
                level,
                mob,
                searchCenter,
                BlockSearchMode.SNIFFER_DIGGABLE,
                horizontalRadius,
                verticalRadius,
                Double.MAX_VALUE,
                gameTime,
                cacheTicks
        );
    }

    public static synchronized BlockPos findBamboo(
            ServerLevel level,
            PathfinderMob mob,
            int horizontalRadius,
            int verticalRadius,
            long gameTime,
            int cacheTicks
    ) {
        return findTarget(
                level,
                mob,
                mob != null ? mob.blockPosition() : null,
                BlockSearchMode.BAMBOO,
                horizontalRadius,
                verticalRadius,
                Double.MAX_VALUE,
                gameTime,
                cacheTicks
        );
    }

    public static synchronized BlockPos findWaterRange(
            ServerLevel level,
            PathfinderMob mob,
            int horizontalRadius,
            int verticalRadius,
            long gameTime,
            int cacheTicks
    ) {
        return findTarget(
                level,
                mob,
                mob != null ? mob.blockPosition() : null,
                BlockSearchMode.WATER_RANGE,
                horizontalRadius,
                verticalRadius,
                Double.MAX_VALUE,
                gameTime,
                cacheTicks
        );
    }

    public static synchronized BlockPos findScrubRange(
            ServerLevel level,
            PathfinderMob mob,
            int horizontalRadius,
            int verticalRadius,
            long gameTime,
            int cacheTicks
    ) {
        return findTarget(
                level,
                mob,
                mob != null ? mob.blockPosition() : null,
                BlockSearchMode.SCRUB_RANGE,
                horizontalRadius,
                verticalRadius,
                Double.MAX_VALUE,
                gameTime,
                cacheTicks
        );
    }

    private static BlockPos findTarget(
            ServerLevel level,
            PathfinderMob mob,
            BlockPos searchCenter,
            BlockSearchMode mode,
            int horizontalRadius,
            int verticalRadius,
            double maxDistanceSquared,
            long gameTime,
            int cacheTicks
    ) {
        if (level == null || mob == null || searchCenter == null || mode == null) {
            return null;
        }

        BlockPos center = searchCenter.immutable();
        List<BlockTargetEntry> entries = TARGETS.computeIfAbsent(
                mob,
                ignored -> new ArrayList<>()
        );

        entries.removeIf(entry -> gameTime >= entry.expiresAt);

        for (BlockTargetEntry entry : entries) {
            if (
                    entry.mode == mode
                            && entry.center.equals(center)
                            && entry.horizontalRadius == horizontalRadius
                            && entry.verticalRadius == verticalRadius
                            && Double.compare(entry.maxDistanceSquared, maxDistanceSquared) == 0
                            && mob.distanceToSqr(entry.mobX, entry.mobY, entry.mobZ) <= MAX_MOB_DRIFT_SQUARED
                            && isCachedTargetStillValid(level, entry.mode, entry.target)
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

        BlockPos target = scan(
                level,
                mob,
                center,
                mode,
                horizontalRadius,
                verticalRadius,
                maxDistanceSquared
        );

        entries.removeIf(entry ->
                entry.mode == mode
                        && entry.center.equals(center)
                        && entry.horizontalRadius == horizontalRadius
                        && entry.verticalRadius == verticalRadius
                        && Double.compare(entry.maxDistanceSquared, maxDistanceSquared) == 0
        );
        entries.add(new BlockTargetEntry(
                mode,
                center,
                horizontalRadius,
                verticalRadius,
                maxDistanceSquared,
                gameTime + Math.max(MIN_BLOCK_TARGET_CACHE_TICKS, RetoldAiLod.cacheTicks(mob, cacheTicks)),
                mob.getX(),
                mob.getY(),
                mob.getZ(),
                target
        ));

        return target;
    }

    private static BlockPos scan(
            ServerLevel level,
            PathfinderMob mob,
            BlockPos center,
            BlockSearchMode mode,
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
                    double baseDistanceSquared = dx * dx + dy * dy + dz * dz;

                    if (baseDistanceSquared > maxDistanceSquared) {
                        continue;
                    }

                    mutable.set(
                            center.getX() + dx,
                            center.getY() + dy,
                            center.getZ() + dz
                    );

                    if (level.isOutsideBuildHeight(mutable) || !isValid(level, mode, mutable)) {
                        continue;
                    }

                    double score = score(
                            level,
                            mob,
                            mode,
                            dx,
                            dy,
                            dz,
                            mutable
                    );

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
            BlockSearchMode mode,
            BlockPos target
    ) {
        return target == null || !level.isOutsideBuildHeight(target) && isValid(level, mode, target);
    }

    private static boolean isValid(
            ServerLevel level,
            BlockSearchMode mode,
            BlockPos pos
    ) {
        return switch (mode) {
            case FLOWER -> RetoldMobRules.isFlowerBlock(level.getBlockState(pos));
            case BEACH_SAND -> isBeachSand(level, pos);
            case WATER -> isWater(level, pos);
            case WETLAND -> isWater(level, pos) && hasNearbyLand(level, pos, 4);
            case SNIFFER_RANGE -> isDiggable(level, pos) || hasNearbyDiggable(level, pos, 5);
            case SNIFFER_DIGGABLE -> isDiggable(level, pos);
            case BAMBOO -> isBamboo(level, pos);
            case WATER_RANGE -> isWater(level, pos) && hasNearbyWater(level, pos, 4);
            case SCRUB_RANGE -> isScrubRange(level, pos);
        };
    }

    private static double score(
            ServerLevel level,
            PathfinderMob mob,
            BlockSearchMode mode,
            int dx,
            int dy,
            int dz,
            BlockPos pos
    ) {
        return switch (mode) {
            case FLOWER -> dx * dx + dy * dy + dz * dz;
            case BEACH_SAND -> {
                double score = dx * dx + dy * dy * 2.0D + dz * dz;
                yield level.canSeeSky(pos) ? score - 6.0D : score;
            }
            case WATER, WETLAND, SNIFFER_RANGE -> dx * dx + dy * dy * 1.5D + dz * dz;
            case SNIFFER_DIGGABLE -> mob.blockPosition().distSqr(pos);
            case BAMBOO, WATER_RANGE, SCRUB_RANGE -> dx * dx + dy * dy * 1.4D + dz * dz;
        };
    }

    private static boolean isBeachSand(
            ServerLevel level,
            BlockPos pos
    ) {
        return isSand(level, pos)
                && level.getBlockState(pos.above()).isAir()
                && hasNearbyWater(level, pos, 4);
    }

    private static boolean isSand(
            ServerLevel level,
            BlockPos pos
    ) {
        BlockState state = level.getBlockState(pos);

        return state.is(Blocks.SAND)
                || state.is(Blocks.RED_SAND);
    }

    private static boolean isWater(
            ServerLevel level,
            BlockPos pos
    ) {
        return level.getFluidState(pos).is(FluidTags.WATER);
    }

    private static boolean hasNearbyWater(
            ServerLevel level,
            BlockPos pos,
            int radius
    ) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    mutable.set(
                            pos.getX() + dx,
                            pos.getY() + dy,
                            pos.getZ() + dz
                    );

                    if (isWater(level, mutable)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static boolean hasNearbyLand(
            ServerLevel level,
            BlockPos pos,
            int radius
    ) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    mutable.set(
                            pos.getX() + dx,
                            pos.getY() + dy,
                            pos.getZ() + dz
                    );

                    if (!level.getBlockState(mutable).isAir() && !isWater(level, mutable)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static boolean hasNearbyDiggable(
            ServerLevel level,
            BlockPos pos,
            int radius
    ) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    mutable.set(
                            pos.getX() + dx,
                            pos.getY() + dy,
                            pos.getZ() + dz
                    );

                    if (isDiggable(level, mutable)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static boolean isDiggable(
            ServerLevel level,
            BlockPos pos
    ) {
        if (!level.getBlockState(pos.above()).isAir()) {
            return false;
        }

        BlockState state = level.getBlockState(pos);

        return state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.DIRT)
                || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.ROOTED_DIRT)
                || state.is(Blocks.PODZOL)
                || state.is(Blocks.MUD)
                || state.is(Blocks.MOSS_BLOCK)
                || state.is(Blocks.FARMLAND);
    }

    private static boolean isBamboo(
            ServerLevel level,
            BlockPos pos
    ) {
        BlockState state = level.getBlockState(pos);

        return state.is(Blocks.BAMBOO)
                || state.is(Blocks.BAMBOO_SAPLING);
    }

    private static boolean isScrubRange(
            ServerLevel level,
            BlockPos pos
    ) {
        if (!level.getBlockState(pos.above()).isAir()) {
            return false;
        }

        BlockState state = level.getBlockState(pos);

        return state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.DIRT)
                || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.ROOTED_DIRT)
                || state.is(Blocks.PODZOL)
                || state.is(Blocks.SAND)
                || state.is(Blocks.RED_SAND)
                || state.is(Blocks.TERRACOTTA);
    }

    private enum BlockSearchMode {
        FLOWER,
        BEACH_SAND,
        WATER,
        WETLAND,
        SNIFFER_RANGE,
        SNIFFER_DIGGABLE,
        BAMBOO,
        WATER_RANGE,
        SCRUB_RANGE
    }

    private record BlockTargetEntry(
            BlockSearchMode mode,
            BlockPos center,
            int horizontalRadius,
            int verticalRadius,
            double maxDistanceSquared,
            long expiresAt,
            double mobX,
            double mobY,
            double mobZ,
            BlockPos target
    ) {
    }
}
