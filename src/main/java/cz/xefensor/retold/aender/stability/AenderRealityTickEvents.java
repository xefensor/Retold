package cz.xefensor.retold.aender.stability;

import cz.xefensor.retold.aender.RetoldAenderDimensions;
import cz.xefensor.retold.aender.generation.AenderChunkGenerator;
import cz.xefensor.retold.aender.generation.AenderIslandSampler;
import cz.xefensor.retold.aender.generation.AenderVolatility;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

public final class AenderRealityTickEvents {
    private static final int MIN_FORGET_DISTANCE_BLOCKS = 640;
    private static final int EXTRA_FORGET_MARGIN_BLOCKS = 128;
    private static final int MAX_ISLAND_RADIUS_BLOCKS = 224;

    private static final int REGEN_RADIUS_CHUNKS = 5;
    private static final int MAX_REGEN_CHUNKS_PER_TICK = 8;
    private static final int MAX_QUEUE_SIZE = 512;

    private static final Queue<Long> REGEN_QUEUE = new ArrayDeque<>();
    private static final Set<Long> QUEUED = new HashSet<>();

    private AenderRealityTickEvents() {
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        if (level.dimension() != RetoldAenderDimensions.AENDER) {
            return;
        }

        if ((level.getGameTime() & 15L) == 0L) {
            int forgetDistanceBlocks = dynamicForgetDistance(level);
            AenderVolatility.forgetFarRegionColumns(level.players(), forgetDistanceBlocks);
            queueNearbyStaleChunks(level);
        }

        processRegenQueue(level);
    }

    private static void queueNearbyStaleChunks(ServerLevel level) {
        AenderStabilityData stability = AenderStabilityData.get(level);

        for (ServerPlayer player : level.players()) {
            BlockPos playerPos = player.blockPosition();

            int centerChunkX = playerPos.getX() >> 4;
            int centerChunkZ = playerPos.getZ() >> 4;

            for (int dx = -REGEN_RADIUS_CHUNKS; dx <= REGEN_RADIUS_CHUNKS; dx++) {
                for (int dz = -REGEN_RADIUS_CHUNKS; dz <= REGEN_RADIUS_CHUNKS; dz++) {
                    int chunkX = centerChunkX + dx;
                    int chunkZ = centerChunkZ + dz;

                    ChunkAccess chunk = getLoadedChunk(level, chunkX, chunkZ);

                    if (chunk == null) {
                        continue;
                    }

                    if (stability.isStable(chunk.getPos())) {
                        continue;
                    }

                    AenderVolatility.retainForChunk(chunk);

                    if (!AenderVolatility.needsRegeneration(chunk)) {
                        continue;
                    }

                    enqueue(chunkX, chunkZ);
                }
            }
        }
    }

    private static void processRegenQueue(ServerLevel level) {
        int regenerated = 0;

        while (regenerated < MAX_REGEN_CHUNKS_PER_TICK && !REGEN_QUEUE.isEmpty()) {
            long key = REGEN_QUEUE.poll();
            QUEUED.remove(key);

            int chunkX = unpackX(key);
            int chunkZ = unpackZ(key);

            ChunkAccess chunk = getLoadedChunk(level, chunkX, chunkZ);

            if (chunk == null) {
                continue;
            }

            if (AenderStabilityData.get(level).isStable(chunk.getPos())) {
                continue;
            }

            if (!AenderVolatility.needsRegeneration(chunk)) {
                continue;
            }

            AenderChunkGenerator.regenerateLoadedChunk(chunk);
            regenerated++;
        }
    }

    private static ChunkAccess getLoadedChunk(ServerLevel level, int chunkX, int chunkZ) {
        return level.getChunkSource().getChunkNow(chunkX, chunkZ);
    }

    private static void enqueue(int chunkX, int chunkZ) {
        if (REGEN_QUEUE.size() >= MAX_QUEUE_SIZE) {
            return;
        }

        long key = pack(chunkX, chunkZ);

        if (QUEUED.add(key)) {
            REGEN_QUEUE.add(key);
        }
    }

    private static int dynamicForgetDistance(ServerLevel level) {
        int viewDistanceChunks = level.getServer().getPlayerList().getViewDistance();
        int viewDistanceBlocks = viewDistanceChunks * 16;

        int islandSafetyMargin =
                AenderIslandSampler.REGION_SIZE / 2
                        + MAX_ISLAND_RADIUS_BLOCKS
                        + EXTRA_FORGET_MARGIN_BLOCKS;

        return Math.max(
                MIN_FORGET_DISTANCE_BLOCKS,
                viewDistanceBlocks + islandSafetyMargin
        );
    }

    private static long pack(int x, int z) {
        return ((long) x & 0xffffffffL) | (((long) z & 0xffffffffL) << 32);
    }

    private static int unpackX(long key) {
        return (int) key;
    }

    private static int unpackZ(long key) {
        return (int) (key >> 32);
    }
}
