package cz.xefensor.retold.aender;

import cz.xefensor.retold.Retold;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public final class RetoldAenderUnstableChunkEvents {
    private static final Queue<ChunkPos> REGENERATION_QUEUE =
            new ArrayDeque<>();

    private static final Set<Long> QUEUED_CHUNKS =
            new HashSet<>();

    private static final Map<Long, Integer> WATCHERS =
            new HashMap<>();

    private static final int CHUNKS_PER_TICK = 1;
    private static final int BLOCK_UPDATE_FLAGS = 2;

    private RetoldAenderUnstableChunkEvents() {
    }

    @SubscribeEvent
    public static void onChunkSent(ChunkWatchEvent.Sent event) {
        ServerLevel level = event.getLevel();

        if (!RetoldAenderChunkStability.isAender(level)) {
            return;
        }

        ChunkPos pos = event.getPos();
        long key = packChunk(pos.x(), pos.z());

        WATCHERS.merge(key, 1, Integer::sum);
    }

    @SubscribeEvent
    public static void onChunkUnwatch(ChunkWatchEvent.UnWatch event) {
        ServerLevel level = event.getLevel();

        if (!RetoldAenderChunkStability.isAender(level)) {
            return;
        }

        ChunkPos pos = event.getPos();
        long key = packChunk(pos.x(), pos.z());

        int watchers = WATCHERS.getOrDefault(key, 0) - 1;

        if (watchers <= 0) {
            WATCHERS.remove(key);
            scheduleUnwatchedChunkRegeneration(level, pos);
        } else {
            WATCHERS.put(key, watchers);
        }
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        if (!RetoldAenderChunkStability.isAender(level)) {
            return;
        }

        ChunkAccess chunk = event.getChunk();

        RetoldAenderChunkData data =
                chunk.getData(RetoldAenderAttachments.CHUNK_DATA.get());

        if (!data.shouldRegenerateOnNextLoad()) {
            return;
        }

        enqueue(chunk.getPos());
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ServerLevel aenderLevel =
                event.getServer().getLevel(RetoldAenderDimensions.AENDER);

        if (aenderLevel == null) {
            return;
        }

        for (int i = 0; i < CHUNKS_PER_TICK && !REGENERATION_QUEUE.isEmpty(); i++) {
            ChunkPos pos = REGENERATION_QUEUE.poll();
            QUEUED_CHUNKS.remove(packChunk(pos.x(), pos.z()));

            if (!aenderLevel.hasChunk(pos.x(), pos.z())) {
                continue;
            }

            if (isWatched(pos)) {
                continue;
            }

            if (hasPlayerInsideChunk(aenderLevel, pos)) {
                continue;
            }

            regenerateIfStillUnstable(aenderLevel, pos);
        }
    }

    private static void scheduleUnwatchedChunkRegeneration(
            ServerLevel level,
            ChunkPos pos
    ) {
        if (!level.hasChunk(pos.x(), pos.z())) {
            return;
        }

        ChunkAccess chunk = level.getChunk(pos.x(), pos.z());

        RetoldAenderChunkData data =
                chunk.getData(RetoldAenderAttachments.CHUNK_DATA.get());

        if (data.isStabilized()) {
            return;
        }

        RetoldAenderChunkStability.scheduleRegenerationOnNextLoad(
                level,
                chunk,
                ThreadLocalRandom.current().nextLong()
        );

        enqueue(pos);
    }

    private static void enqueue(ChunkPos pos) {
        long key = packChunk(pos.x(), pos.z());

        if (QUEUED_CHUNKS.add(key)) {
            REGENERATION_QUEUE.add(pos);
        }
    }

    private static boolean isWatched(ChunkPos pos) {
        return WATCHERS.containsKey(packChunk(pos.x(), pos.z()));
    }

    private static boolean hasPlayerInsideChunk(
            ServerLevel level,
            ChunkPos pos
    ) {
        for (var player : level.players()) {
            int playerChunkX = ((int) Math.floor(player.getX())) >> 4;
            int playerChunkZ = ((int) Math.floor(player.getZ())) >> 4;

            if (playerChunkX == pos.x() && playerChunkZ == pos.z()) {
                return true;
            }
        }

        return false;
    }

    private static void regenerateIfStillUnstable(
            ServerLevel level,
            ChunkPos pos
    ) {
        ChunkAccess chunk = level.getChunk(pos.x(), pos.z());

        RetoldAenderChunkData data =
                chunk.getData(RetoldAenderAttachments.CHUNK_DATA.get());

        if (!data.shouldRegenerateOnNextLoad()) {
            return;
        }

        if (data.isStabilized()) {
            return;
        }

        regenerateFlatVariant(level, pos, data.regenerationSalt());

        RetoldAenderChunkStability.markRegenerationFinished(level, chunk);

        Retold.LOGGER.info(
                "Regenerated unwatched unstable Aender chunk [{}, {}]",
                pos.x(),
                pos.z()
        );
    }

    private static void regenerateFlatVariant(
            ServerLevel level,
            ChunkPos pos,
            long salt
    ) {
        Random random = new Random(
                level.getSeed()
                        ^ salt
                        ^ packChunk(pos.x(), pos.z())
        );

        int minY = level.getMinY();
        int maxY = level.getMaxY();

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int worldX = pos.getMinBlockX() + localX;
                int worldZ = pos.getMinBlockZ() + localZ;

                int surfaceHeight = minY + 3 + random.nextInt(4);

                for (int y = minY; y < maxY; y++) {
                    BlockState state = getStateForY(
                            random,
                            minY,
                            y,
                            surfaceHeight
                    );

                    level.setBlock(
                            new BlockPos(worldX, y, worldZ),
                            state,
                            BLOCK_UPDATE_FLAGS
                    );
                }
            }
        }
    }

    private static BlockState getStateForY(
            Random random,
            int minY,
            int y,
            int surfaceHeight
    ) {
        if (y == minY) {
            return Blocks.BEDROCK.defaultBlockState();
        }

        if (y > surfaceHeight) {
            return Blocks.AIR.defaultBlockState();
        }

        int roll = random.nextInt(100);

        if (roll < 2) {
            return Blocks.AMETHYST_BLOCK.defaultBlockState();
        }

        if (roll < 8) {
            return Blocks.OBSIDIAN.defaultBlockState();
        }

        return Blocks.END_STONE.defaultBlockState();
    }

    private static long packChunk(int chunkX, int chunkZ) {
        return ((long) chunkX & 4294967295L)
                | (((long) chunkZ & 4294967295L) << 32);
    }
}