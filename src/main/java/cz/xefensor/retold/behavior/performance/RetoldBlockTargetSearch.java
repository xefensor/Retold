package cz.xefensor.retold.behavior.performance;

import cz.xefensor.retold.behavior.profiles.RetoldMobRules;
import cz.xefensor.retold.registry.RetoldTags;
import cz.xefensor.retold.behavior.core.RetoldWeakBarriers;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class RetoldBlockTargetSearch {
    private static final int MIN_BLOCK_TARGET_CACHE_TICKS = 40;

    private static final Map<Entity, List<BlockTargetEntry>> TARGETS = new WeakHashMap<>();

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

    public static synchronized BlockPos findArmadilloGrubSoil(
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
                BlockSearchMode.ARMADILLO_GRUB,
                horizontalRadius,
                verticalRadius,
                Double.MAX_VALUE,
                gameTime,
                cacheTicks
        );
    }

    public static synchronized BlockPos findWeakBarrier(
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
                BlockSearchMode.WEAK_BARRIER,
                horizontalRadius,
                verticalRadius,
                Double.MAX_VALUE,
                gameTime,
                cacheTicks
        );
    }

    public static synchronized BlockPos findCobwebPlacement(
            ServerLevel level,
            PathfinderMob mob,
            BlockPos searchCenter,
            int horizontalRadius,
            int verticalRadius,
            double maxDistanceSquared,
            long gameTime,
            int cacheTicks
    ) {
        return findTarget(
                level,
                mob,
                searchCenter,
                BlockSearchMode.COBWEB_PLACEMENT,
                horizontalRadius,
                verticalRadius,
                maxDistanceSquared,
                gameTime,
                cacheTicks
        );
    }

    public static synchronized BlockPos findBatRoost(
            ServerLevel level,
            Mob bat,
            int horizontalRadius,
            int verticalRadius,
            long gameTime,
            int cacheTicks
    ) {
        return findTarget(
                level,
                bat,
                bat != null ? bat.blockPosition() : null,
                BlockSearchMode.BAT_ROOST,
                horizontalRadius,
                verticalRadius,
                Double.MAX_VALUE,
                gameTime,
                cacheTicks
        );
    }

    public static synchronized BlockPos findFireSource(
            ServerLevel level,
            Mob mob,
            int horizontalRadius,
            int verticalRadius,
            long gameTime,
            int cacheTicks
    ) {
        return findTarget(
                level,
                mob,
                mob != null ? mob.blockPosition() : null,
                BlockSearchMode.FIRE_SOURCE,
                horizontalRadius,
                verticalRadius,
                Double.MAX_VALUE,
                gameTime,
                cacheTicks
        );
    }

    public static synchronized BlockPos findLavaSource(
            ServerLevel level,
            Mob mob,
            int horizontalRadius,
            int verticalRadius,
            long gameTime,
            int cacheTicks
    ) {
        return findTarget(
                level,
                mob,
                mob != null ? mob.blockPosition() : null,
                BlockSearchMode.LAVA_SOURCE,
                horizontalRadius,
                verticalRadius,
                Double.MAX_VALUE,
                gameTime,
                cacheTicks
        );
    }

    public static synchronized BlockPos findDeepLavaSource(
            ServerLevel level,
            Mob mob,
            int horizontalRadius,
            int verticalRadius,
            long gameTime,
            int cacheTicks
    ) {
        return findTarget(
                level,
                mob,
                mob != null ? mob.blockPosition() : null,
                BlockSearchMode.DEEP_LAVA_SOURCE,
                horizontalRadius,
                verticalRadius,
                Double.MAX_VALUE,
                gameTime,
                cacheTicks
        );
    }

    public static synchronized BlockPos findBatRoost(
            ServerLevel level,
            Mob bat,
            BlockPos searchCenter,
            int horizontalRadius,
            int verticalRadius,
            long gameTime,
            int cacheTicks
    ) {
        return findTarget(
                level,
                bat,
                searchCenter,
                BlockSearchMode.BAT_ROOST,
                horizontalRadius,
                verticalRadius,
                Double.MAX_VALUE,
                gameTime,
                cacheTicks
        );
    }

    private static BlockPos findTarget(
            ServerLevel level,
            Entity mob,
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
                target
        ));

        return target;
    }

    private static BlockPos scan(
            ServerLevel level,
            Entity mob,
            BlockPos center,
            BlockSearchMode mode,
            int horizontalRadius,
            int verticalRadius,
            double maxDistanceSquared
    ) {
        if (mode == BlockSearchMode.BAT_ROOST) {
            return scanBatRoost(
                    level,
                    center,
                    horizontalRadius,
                    verticalRadius
            );
        }

        BlockPos best = null;
        double bestScore = Double.MAX_VALUE;
        long positionsChecked = 0L;
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int dx = -horizontalRadius; dx <= horizontalRadius; dx++) {
            for (int dy = -verticalRadius; dy <= verticalRadius; dy++) {
                for (int dz = -horizontalRadius; dz <= horizontalRadius; dz++) {
                    double baseDistanceSquared = dx * dx + dy * dy + dz * dz;

                    if (baseDistanceSquared > maxDistanceSquared) {
                        continue;
                    }

                    positionsChecked++;

                    mutable.set(
                            center.getX() + dx,
                            center.getY() + dy,
                            center.getZ() + dz
                    );

                    if (level.isOutsideBuildHeight(mutable) || !isValid(level, mode, mutable)) {
                        continue;
                    }

                    if (mode == BlockSearchMode.COBWEB_PLACEMENT
                            && mutable.equals(mob.blockPosition())) {
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

        RetoldBehaviorPerf.recordBlockTargetPositionsChecked(positionsChecked);
        return best;
    }

    private static BlockPos scanBatRoost(
            ServerLevel level,
            BlockPos center,
            int horizontalRadius,
            int verticalRadius
    ) {
        int maxRing = horizontalRadius * 2;
        long positionsChecked = 0L;
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int ring = 0; ring <= maxRing; ring++) {
            for (int offsetX = -ring; offsetX <= ring; offsetX++) {
                for (int offsetZ = -ring; offsetZ <= ring; offsetZ++) {
                    if (Math.max(Math.abs(offsetX), Math.abs(offsetZ)) != ring) {
                        continue;
                    }

                    int dx = offsetX;
                    int dz = offsetZ;

                    if (Math.abs(dx) > horizontalRadius
                            || Math.abs(dz) > horizontalRadius) {
                        continue;
                    }

                    for (int dy = verticalRadius; dy >= 0; dy--) {
                        positionsChecked++;
                        mutable.set(
                                center.getX() + dx,
                                center.getY() + dy,
                                center.getZ() + dz
                        );

                        if (!level.isOutsideBuildHeight(mutable)
                                && isBatRoostAt(level, mutable)) {
                            RetoldBehaviorPerf.recordBlockTargetPositionsChecked(
                                    positionsChecked
                            );
                            return mutable.immutable();
                        }
                    }
                }
            }
        }

        RetoldBehaviorPerf.recordBlockTargetPositionsChecked(positionsChecked);
        return null;
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
            /*
             * A newly discovered range anchor can be the diggable block itself.
             * Checking a second 11x5x11 volume for every candidate here made one
             * nominal 37x9x37 range scan perform millions of block lookups.
             * Existing shared range memories may still be near (rather than on)
             * diggable ground; RetoldSnifferForagerEvents validates those separately.
             */
            case SNIFFER_RANGE -> isDiggable(level, pos);
            case SNIFFER_DIGGABLE -> isDiggable(level, pos);
            case BAMBOO -> isBamboo(level, pos);
            case WATER_RANGE -> isWater(level, pos) && hasNearbyWater(level, pos, 4);
            case SCRUB_RANGE -> isScrubRange(level, pos);
            case ARMADILLO_GRUB -> isArmadilloGrubSoil(level, pos);
            case WEAK_BARRIER -> RetoldWeakBarriers.isBreakable(level.getBlockState(pos));
            case COBWEB_PLACEMENT -> canPlaceCobwebAt(level, pos);
            case BAT_ROOST -> isBatRoostAt(level, pos);
            case FIRE_SOURCE -> isFireSourceAt(level, pos);
            case LAVA_SOURCE -> isLavaSourceAt(level, pos);
            case DEEP_LAVA_SOURCE -> isDeepLavaSourceAt(level, pos);
        };
    }

    private static double score(
            ServerLevel level,
            Entity mob,
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
            case BAMBOO, WATER_RANGE, SCRUB_RANGE, ARMADILLO_GRUB ->
                    dx * dx + dy * dy * 1.4D + dz * dz;
            case WEAK_BARRIER -> dx * dx + dy * dy * 1.5D + dz * dz;
            case COBWEB_PLACEMENT -> dx * dx + dy * dy * 1.25D + dz * dz;
            case BAT_ROOST -> batRoostScore(mob, dx, dy, dz);
            case FIRE_SOURCE, LAVA_SOURCE, DEEP_LAVA_SOURCE ->
                    dx * dx + dy * dy * 1.25D + dz * dz;
        };
    }

    public static boolean isFireSourceAt(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null || level.isOutsideBuildHeight(pos)) {
            return false;
        }

        return level.getFluidState(pos).is(FluidTags.LAVA)
                || level.getBlockState(pos).is(Blocks.FIRE)
                || level.getBlockState(pos).is(Blocks.SOUL_FIRE);
    }

    public static boolean isLavaSourceAt(ServerLevel level, BlockPos pos) {
        return level != null
                && pos != null
                && !level.isOutsideBuildHeight(pos)
                && level.getFluidState(pos).is(FluidTags.LAVA);
    }

    /** Returns whether {@code pos} is the surface of a lava column at least three blocks deep. */
    public static boolean isDeepLavaSourceAt(ServerLevel level, BlockPos pos) {
        return isLavaSourceAt(level, pos)
                && !isLavaSourceAt(level, pos.above())
                && isLavaSourceAt(level, pos.below())
                && isLavaSourceAt(level, pos.below(2));
    }

    private static double batRoostScore(
            Entity bat,
            int dx,
            int dy,
            int dz
    ) {
        int slot = Math.floorMod(bat.getId() * 7, 25);
        int preferredX = slot % 5 - 2;
        int preferredZ = slot / 5 - 2;
        int offsetX = dx - preferredX;
        int offsetZ = dz - preferredZ;

        return offsetX * offsetX + dy * dy * 1.25D + offsetZ * offsetZ;
    }

    public static boolean isBatRoostAt(
            ServerLevel level,
            BlockPos pos
    ) {
        if (level == null
                || pos == null
                || level.isOutsideBuildHeight(pos)
                || !level.getBlockState(pos).isAir()
                || level.getMaxLocalRawBrightness(pos) >= 8
                || !hasLowRawSkyLight(level, pos)) {
            return false;
        }

        BlockPos ceiling = pos.above();

        return level.getBlockState(ceiling).isRedstoneConductor(level, ceiling);
    }

    public static boolean canPlaceCobwebAt(
            ServerLevel level,
            BlockPos pos
    ) {
        if (level == null
                || pos == null
                || level.isOutsideBuildHeight(pos)
                || !level.getBlockState(pos).isAir()
                || !hasLowRawSkyLight(level, pos)
                || level.getMaxLocalRawBrightness(pos) >= 8) {
            return false;
        }

        for (Direction direction : Direction.values()) {
            BlockPos supportPos = pos.relative(direction);

            if (level.getBlockState(supportPos).isFaceSturdy(
                    level,
                    supportPos,
                    direction.getOpposite()
            )) {
                return true;
            }
        }

        return false;
    }

    public static boolean hasLowRawSkyLight(
            ServerLevel level,
            BlockPos pos
    ) {
        return level != null
                && pos != null
                && !level.isOutsideBuildHeight(pos)
                && level.getBrightness(LightLayer.SKY, pos) < 8;
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

        return state.is(RetoldTags.TURTLE_BEACH_BLOCKS);
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

        return state.is(RetoldTags.PANDA_BAMBOO_BLOCKS);
    }

    private static boolean isScrubRange(
            ServerLevel level,
            BlockPos pos
    ) {
        if (!level.getBlockState(pos.above()).isAir()) {
            return false;
        }

        BlockState state = level.getBlockState(pos);

        return state.is(RetoldTags.ARMADILLO_SCRUB_RANGE_BLOCKS);
    }

    private static boolean isArmadilloGrubSoil(
            ServerLevel level,
            BlockPos pos
    ) {
        return level.getBlockState(pos.above()).isAir()
                && RetoldMobRules.canDigForGrubs(level.getBlockState(pos));
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
        SCRUB_RANGE,
        ARMADILLO_GRUB,
        WEAK_BARRIER,
        COBWEB_PLACEMENT,
        BAT_ROOST,
        FIRE_SOURCE,
        LAVA_SOURCE,
        DEEP_LAVA_SOURCE
    }

    private record BlockTargetEntry(
            BlockSearchMode mode,
            BlockPos center,
            int horizontalRadius,
            int verticalRadius,
            double maxDistanceSquared,
            long expiresAt,
            BlockPos target
    ) {
    }
}
