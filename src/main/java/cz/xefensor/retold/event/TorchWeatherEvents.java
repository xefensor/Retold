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

public final class TorchWeatherEvents {
    private static final int CHECK_INTERVAL_TICKS = 60;
    private static final int EXTINGUISH_CHANCE = 3;
    private static final int MAX_CHUNK_INDEXES_PER_TICK = 4;

    private static final RandomSource RANDOM = RandomSource.create();

    private static final Map<ResourceKey<Level>, Long2ObjectOpenHashMap<LongOpenHashSet>> TRACKED_TORCHES = new HashMap<>();
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
            playExtinguishEffects(level, pos);

            torchIterator.remove();
        }
    }

    private static boolean isPrecipitatingAt(ServerLevel level, BlockPos torchPos) {
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

    private static boolean isAender(ServerLevel level) {
        return level.dimension() == RetoldAenderDimensions.AENDER;
    }
}
