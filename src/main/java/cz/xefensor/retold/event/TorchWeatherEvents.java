package cz.xefensor.retold.event;

import cz.xefensor.retold.aender.RetoldAenderDimensions;
import cz.xefensor.retold.registry.RetoldBlocks;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public final class TorchWeatherEvents {
    private static final int CHECK_INTERVAL_TICKS = 60;
    private static final int EXTINGUISH_CHANCE = 3;
    private static final int MAX_CHUNK_INDEXES_PER_TICK = 4;

    private static final RandomSource RANDOM = RandomSource.create();

    private static final Map<ResourceKey<Level>, Long2ObjectOpenHashMap<LongOpenHashSet>> TRACKED_TORCHES = new HashMap<>();
    private static final Map<ResourceKey<Level>, Long2ObjectOpenHashMap<LongOpenHashSet>> TRACKED_EXTINGUISHED_TORCHES = new HashMap<>();
    private static final Map<ResourceKey<Level>, LongOpenHashSet> PENDING_CHUNK_INDEXES = new HashMap<>();

    private TorchWeatherEvents() {
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        if (isAender(level)) {
            return;
        }

        long chunkKey = chunkKey(event.getChunk().getPos());

        PENDING_CHUNK_INDEXES
                .computeIfAbsent(level.dimension(), ignored -> new LongOpenHashSet())
                .add(chunkKey);
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        if (isAender(level)) {
            return;
        }

        ResourceKey<Level> dimension = level.dimension();
        long chunkKey = chunkKey(event.getChunk().getPos());

        Long2ObjectOpenHashMap<LongOpenHashSet> trackedByChunk = TRACKED_TORCHES.get(dimension);
        if (trackedByChunk != null) {
            trackedByChunk.remove(chunkKey);

            if (trackedByChunk.isEmpty()) {
                TRACKED_TORCHES.remove(dimension);
            }
        }

        Long2ObjectOpenHashMap<LongOpenHashSet> extinguishedByChunk =
                TRACKED_EXTINGUISHED_TORCHES.get(dimension);
        if (extinguishedByChunk != null) {
            extinguishedByChunk.remove(chunkKey);

            if (extinguishedByChunk.isEmpty()) {
                TRACKED_EXTINGUISHED_TORCHES.remove(dimension);
            }
        }

        LongOpenHashSet pending = PENDING_CHUNK_INDEXES.get(dimension);
        if (pending != null) {
            pending.remove(chunkKey);

            if (pending.isEmpty()) {
                PENDING_CHUNK_INDEXES.remove(dimension);
            }
        }
    }

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        if (isAender(level)) {
            return;
        }

        BlockState state = event.getPlacedBlock();

        if (isLitTorch(state)) {
            trackTorch(level, event.getPos(), state);
        } else if (isExtinguishedTorch(state)) {
            trackExtinguishedTorch(level, event.getPos(), state);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        if (isAender(level)) {
            return;
        }

        indexPendingChunks(level);

        if (!level.isRaining()) {
            return;
        }

        if (level.getGameTime() % CHECK_INTERVAL_TICKS != 0) {
            return;
        }

        checkTrackedTorches(level);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        TRACKED_TORCHES.clear();
        TRACKED_EXTINGUISHED_TORCHES.clear();
        PENDING_CHUNK_INDEXES.clear();
    }

    public static void trackTorch(Level level, BlockPos pos, BlockState state) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (isAender(serverLevel)) {
            return;
        }

        if (!isLitTorch(state)) {
            return;
        }

        long chunkKey = chunkKey(pos);
        long posKey = pos.asLong();

        TRACKED_TORCHES
                .computeIfAbsent(serverLevel.dimension(), ignored -> new Long2ObjectOpenHashMap<>())
                .computeIfAbsent(chunkKey, ignored -> new LongOpenHashSet())
                .add(posKey);
    }

    public static void untrackTorch(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (isAender(serverLevel)) {
            return;
        }

        ResourceKey<Level> dimension = serverLevel.dimension();
        long chunkKey = chunkKey(pos);

        Long2ObjectOpenHashMap<LongOpenHashSet> trackedByChunk = TRACKED_TORCHES.get(dimension);
        if (trackedByChunk == null) {
            return;
        }

        LongOpenHashSet torchesInChunk = trackedByChunk.get(chunkKey);
        if (torchesInChunk == null) {
            return;
        }

        torchesInChunk.remove(pos.asLong());

        if (torchesInChunk.isEmpty()) {
            trackedByChunk.remove(chunkKey);
        }

        if (trackedByChunk.isEmpty()) {
            TRACKED_TORCHES.remove(dimension);
        }
    }

    public static void trackExtinguishedTorch(
            Level level,
            BlockPos pos,
            BlockState state
    ) {
        if (!(level instanceof ServerLevel serverLevel)
                || isAender(serverLevel)
                || !isExtinguishedTorch(state)) {
            return;
        }

        TRACKED_EXTINGUISHED_TORCHES
                .computeIfAbsent(
                        serverLevel.dimension(),
                        ignored -> new Long2ObjectOpenHashMap<>()
                )
                .computeIfAbsent(
                        chunkKey(pos),
                        ignored -> new LongOpenHashSet()
                )
                .add(pos.asLong());
    }

    public static void untrackExtinguishedTorch(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)
                || isAender(serverLevel)) {
            return;
        }

        removeTrackedPosition(
                TRACKED_EXTINGUISHED_TORCHES,
                serverLevel.dimension(),
                pos
        );
    }

    public static boolean hasTrackedExtinguishedTorches(ServerLevel level) {
        if (level == null || isAender(level)) {
            return false;
        }

        Long2ObjectOpenHashMap<LongOpenHashSet> tracked =
                TRACKED_EXTINGUISHED_TORCHES.get(level.dimension());
        return tracked != null && !tracked.isEmpty();
    }

    public static BlockPos findNearestExtinguishedTorch(
            ServerLevel level,
            BlockPos center,
            int horizontalRadius,
            int verticalRadius,
            Predicate<BlockPos> positionFilter
    ) {
        if (level == null
                || center == null
                || horizontalRadius < 0
                || verticalRadius < 0
                || isAender(level)) {
            return null;
        }

        Long2ObjectOpenHashMap<LongOpenHashSet> trackedByChunk =
                TRACKED_EXTINGUISHED_TORCHES.get(level.dimension());

        if (trackedByChunk == null || trackedByChunk.isEmpty()) {
            return null;
        }

        BlockPos best = null;
        double bestDistanceSquared = Double.MAX_VALUE;
        int minChunkX = Math.floorDiv(center.getX() - horizontalRadius, 16);
        int maxChunkX = Math.floorDiv(center.getX() + horizontalRadius, 16);
        int minChunkZ = Math.floorDiv(center.getZ() - horizontalRadius, 16);
        int maxChunkZ = Math.floorDiv(center.getZ() + horizontalRadius, 16);

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                LongOpenHashSet positions = trackedByChunk.get(
                        ChunkPos.pack(chunkX, chunkZ)
                );

                if (positions == null || positions.isEmpty()) {
                    continue;
                }

                LongIterator iterator = positions.iterator();

                while (iterator.hasNext()) {
                    BlockPos candidate = BlockPos.of(iterator.nextLong());

                    if (!level.hasChunkAt(candidate)
                            || !isExtinguishedTorch(
                            level.getBlockState(candidate)
                    )) {
                        iterator.remove();
                        continue;
                    }

                    int dx = Math.abs(candidate.getX() - center.getX());
                    int dy = Math.abs(candidate.getY() - center.getY());
                    int dz = Math.abs(candidate.getZ() - center.getZ());

                    if (dx * dx + dz * dz
                            > horizontalRadius * horizontalRadius
                            || dy > verticalRadius
                            || (positionFilter != null
                            && !positionFilter.test(candidate))) {
                        continue;
                    }

                    double distanceSquared = center.distSqr(candidate);

                    if (distanceSquared < bestDistanceSquared) {
                        bestDistanceSquared = distanceSquared;
                        best = candidate.immutable();
                    }
                }

                if (positions.isEmpty()) {
                    trackedByChunk.remove(ChunkPos.pack(chunkX, chunkZ));
                }
            }
        }

        if (trackedByChunk.isEmpty()) {
            TRACKED_EXTINGUISHED_TORCHES.remove(level.dimension());
        }

        return best;
    }

    public static boolean relightMagically(
            ServerLevel level,
            BlockPos pos
    ) {
        return relight(
                level,
                pos,
                SoundEvents.ENCHANTMENT_TABLE_USE,
                0.7F,
                1.5F,
                true
        );
    }

    public static boolean relightWithFlintAndSteel(
            ServerLevel level,
            BlockPos pos
    ) {
        return relight(
                level,
                pos,
                SoundEvents.FLINTANDSTEEL_USE,
                1.0F,
                1.0F,
                false
        );
    }

    private static boolean relight(
            ServerLevel level,
            BlockPos pos,
            SoundEvent sound,
            float volume,
            float pitch,
            boolean magical
    ) {
        if (level == null || pos == null || isPrecipitatingAt(level, pos)) {
            return false;
        }

        BlockState litState = getLitState(level.getBlockState(pos));

        if (litState == null || !level.setBlock(pos, litState, Block.UPDATE_ALL)) {
            return false;
        }

        untrackExtinguishedTorch(level, pos);
        trackTorch(level, pos, litState);
        level.playSound(
                null,
                pos,
                sound,
                SoundSource.BLOCKS,
                volume,
                pitch
        );
        level.sendParticles(
                ParticleTypes.FLAME,
                pos.getX() + 0.5D,
                pos.getY() + 0.7D,
                pos.getZ() + 0.5D,
                8,
                0.15D,
                0.18D,
                0.15D,
                0.01D
        );
        if (magical) {
            level.sendParticles(
                    ParticleTypes.HAPPY_VILLAGER,
                    pos.getX() + 0.5D,
                    pos.getY() + 0.7D,
                    pos.getZ() + 0.5D,
                    5,
                    0.2D,
                    0.25D,
                    0.2D,
                    0.01D
            );
        }
        return true;
    }

    private static void indexPendingChunks(ServerLevel level) {
        ResourceKey<Level> dimension = level.dimension();

        LongOpenHashSet pending = PENDING_CHUNK_INDEXES.get(dimension);
        if (pending == null || pending.isEmpty()) {
            return;
        }

        int indexed = 0;
        LongIterator iterator = pending.iterator();

        while (iterator.hasNext() && indexed < MAX_CHUNK_INDEXES_PER_TICK) {
            long packedChunkPos = iterator.nextLong();
            iterator.remove();

            ChunkPos chunkPos = ChunkPos.unpack(packedChunkPos);

            BlockPos chunkOrigin = new BlockPos(
                    chunkPos.x() << 4,
                    level.getMinY(),
                    chunkPos.z() << 4
            );

            if (!level.hasChunkAt(chunkOrigin)) {
                continue;
            }

            indexChunk(level, chunkPos);
            indexed++;
        }

        if (pending.isEmpty()) {
            PENDING_CHUNK_INDEXES.remove(dimension);
        }
    }

    private static void indexChunk(ServerLevel level, ChunkPos chunkPos) {
        int minX = chunkPos.x() << 4;
        int maxX = minX + 15;
        int minZ = chunkPos.z() << 4;
        int maxZ = minZ + 15;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = minX; x <= maxX; x++) {
            for (int y = level.getMinY(); y < level.getMaxY(); y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    pos.set(x, y, z);

                    BlockState state = level.getBlockState(pos);

                    if (isLitTorch(state)) {
                        trackTorch(level, pos.immutable(), state);
                    } else if (isExtinguishedTorch(state)) {
                        trackExtinguishedTorch(
                                level,
                                pos.immutable(),
                                state
                        );
                    }
                }
            }
        }
    }

    private static void checkTrackedTorches(ServerLevel level) {
        ResourceKey<Level> dimension = level.dimension();

        Long2ObjectOpenHashMap<LongOpenHashSet> trackedByChunk = TRACKED_TORCHES.get(dimension);
        if (trackedByChunk == null || trackedByChunk.isEmpty()) {
            return;
        }

        LongIterator chunkIterator = trackedByChunk.keySet().iterator();

        while (chunkIterator.hasNext()) {
            long packedChunkPos = chunkIterator.nextLong();
            ChunkPos chunkPos = ChunkPos.unpack(packedChunkPos);

            BlockPos chunkOrigin = new BlockPos(
                    chunkPos.x() << 4,
                    level.getMinY(),
                    chunkPos.z() << 4
            );

            if (!level.hasChunkAt(chunkOrigin)) {
                chunkIterator.remove();
                continue;
            }

            LongOpenHashSet torchesInChunk = trackedByChunk.get(packedChunkPos);
            if (torchesInChunk == null || torchesInChunk.isEmpty()) {
                chunkIterator.remove();
                continue;
            }

            checkTorchesInChunk(level, torchesInChunk);

            if (torchesInChunk.isEmpty()) {
                chunkIterator.remove();
            }
        }

        if (trackedByChunk.isEmpty()) {
            TRACKED_TORCHES.remove(dimension);
        }
    }

    private static void checkTorchesInChunk(ServerLevel level, LongOpenHashSet torchesInChunk) {
        LongIterator torchIterator = torchesInChunk.iterator();

        while (torchIterator.hasNext()) {
            long posKey = torchIterator.nextLong();
            BlockPos pos = BlockPos.of(posKey);

            BlockState state = level.getBlockState(pos);

            if (!isLitTorch(state)) {
                torchIterator.remove();
                continue;
            }

            if (!isPrecipitatingAt(level, pos)) {
                continue;
            }

            if (RANDOM.nextInt(EXTINGUISH_CHANCE) != 0) {
                continue;
            }

            BlockState extinguishedState = getExtinguishedState(state);
            if (extinguishedState == null) {
                torchIterator.remove();
                continue;
            }

            level.setBlock(pos, extinguishedState, Block.UPDATE_ALL);
            trackExtinguishedTorch(level, pos, extinguishedState);
            playExtinguishEffects(level, pos);

            torchIterator.remove();
        }
    }

    public static boolean isPrecipitatingAt(
            ServerLevel level,
            BlockPos torchPos
    ) {
        if (!level.isRaining()) {
            return false;
        }

        BlockPos checkPos = torchPos.above();

        if (!level.canSeeSky(checkPos)) {
            return false;
        }

        Biome.Precipitation precipitation = level
                .getBiome(checkPos)
                .value()
                .getPrecipitationAt(checkPos, level.getSeaLevel());

        return precipitation == Biome.Precipitation.RAIN
                || precipitation == Biome.Precipitation.SNOW;
    }

    private static boolean isLitTorch(BlockState state) {
        return state.is(Blocks.TORCH)
                || state.is(Blocks.WALL_TORCH)
                || state.is(Blocks.SOUL_TORCH)
                || state.is(Blocks.SOUL_WALL_TORCH)
                || state.is(Blocks.COPPER_TORCH)
                || state.is(Blocks.COPPER_WALL_TORCH);
    }

    public static boolean isExtinguishedTorch(BlockState state) {
        return state.is(RetoldBlocks.EXTINGUISHED_TORCH.get())
                || state.is(RetoldBlocks.EXTINGUISHED_WALL_TORCH.get())
                || state.is(RetoldBlocks.EXTINGUISHED_SOUL_TORCH.get())
                || state.is(RetoldBlocks.EXTINGUISHED_SOUL_WALL_TORCH.get())
                || state.is(RetoldBlocks.EXTINGUISHED_COPPER_TORCH.get())
                || state.is(
                RetoldBlocks.EXTINGUISHED_COPPER_WALL_TORCH.get()
        );
    }

    private static BlockState getLitState(BlockState state) {
        if (state.is(RetoldBlocks.EXTINGUISHED_TORCH.get())) {
            return Blocks.TORCH.defaultBlockState();
        }

        if (state.is(RetoldBlocks.EXTINGUISHED_SOUL_TORCH.get())) {
            return Blocks.SOUL_TORCH.defaultBlockState();
        }

        if (state.is(RetoldBlocks.EXTINGUISHED_COPPER_TORCH.get())) {
            return Blocks.COPPER_TORCH.defaultBlockState();
        }

        if (state.is(RetoldBlocks.EXTINGUISHED_WALL_TORCH.get())) {
            return Blocks.WALL_TORCH.defaultBlockState().setValue(
                    WallTorchBlock.FACING,
                    state.getValue(WallTorchBlock.FACING)
            );
        }

        if (state.is(RetoldBlocks.EXTINGUISHED_SOUL_WALL_TORCH.get())) {
            return Blocks.SOUL_WALL_TORCH.defaultBlockState().setValue(
                    WallTorchBlock.FACING,
                    state.getValue(WallTorchBlock.FACING)
            );
        }

        if (state.is(
                RetoldBlocks.EXTINGUISHED_COPPER_WALL_TORCH.get()
        )) {
            return Blocks.COPPER_WALL_TORCH.defaultBlockState().setValue(
                    WallTorchBlock.FACING,
                    state.getValue(WallTorchBlock.FACING)
            );
        }

        return null;
    }

    private static BlockState getExtinguishedState(BlockState state) {
        if (state.is(Blocks.TORCH)) {
            return RetoldBlocks.EXTINGUISHED_TORCH.get().defaultBlockState();
        }

        if (state.is(Blocks.SOUL_TORCH)) {
            return RetoldBlocks.EXTINGUISHED_SOUL_TORCH.get().defaultBlockState();
        }

        if (state.is(Blocks.COPPER_TORCH)) {
            return RetoldBlocks.EXTINGUISHED_COPPER_TORCH.get().defaultBlockState();
        }

        if (state.is(Blocks.WALL_TORCH)) {
            return RetoldBlocks.EXTINGUISHED_WALL_TORCH.get()
                    .defaultBlockState()
                    .setValue(WallTorchBlock.FACING, state.getValue(WallTorchBlock.FACING));
        }

        if (state.is(Blocks.SOUL_WALL_TORCH)) {
            return RetoldBlocks.EXTINGUISHED_SOUL_WALL_TORCH.get()
                    .defaultBlockState()
                    .setValue(WallTorchBlock.FACING, state.getValue(WallTorchBlock.FACING));
        }

        if (state.is(Blocks.COPPER_WALL_TORCH)) {
            return RetoldBlocks.EXTINGUISHED_COPPER_WALL_TORCH.get()
                    .defaultBlockState()
                    .setValue(WallTorchBlock.FACING, state.getValue(WallTorchBlock.FACING));
        }

        return null;
    }

    private static void playExtinguishEffects(ServerLevel level, BlockPos pos) {
        level.playSound(
                null,
                pos,
                SoundEvents.FIRE_EXTINGUISH,
                SoundSource.BLOCKS,
                0.7F,
                1.4F
        );

        level.sendParticles(
                ParticleTypes.SMOKE,
                pos.getX() + 0.5,
                pos.getY() + 0.7,
                pos.getZ() + 0.5,
                12,
                0.18,
                0.18,
                0.18,
                0.01
        );
    }

    private static long chunkKey(BlockPos pos) {
        return ChunkPos.pack(pos.getX() >> 4, pos.getZ() >> 4);
    }

    private static long chunkKey(ChunkPos chunkPos) {
        return ChunkPos.pack(chunkPos.x(), chunkPos.z());
    }

    private static void removeTrackedPosition(
            Map<ResourceKey<Level>, Long2ObjectOpenHashMap<LongOpenHashSet>> index,
            ResourceKey<Level> dimension,
            BlockPos pos
    ) {
        Long2ObjectOpenHashMap<LongOpenHashSet> trackedByChunk =
                index.get(dimension);

        if (trackedByChunk == null) {
            return;
        }

        LongOpenHashSet positions = trackedByChunk.get(chunkKey(pos));

        if (positions == null) {
            return;
        }

        positions.remove(pos.asLong());

        if (positions.isEmpty()) {
            trackedByChunk.remove(chunkKey(pos));
        }

        if (trackedByChunk.isEmpty()) {
            index.remove(dimension);
        }
    }

    private static boolean isAender(ServerLevel level) {
        return level.dimension() == RetoldAenderDimensions.AENDER;
    }
}
