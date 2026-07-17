package cz.xefensor.retold.aender.stability;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.aender.RetoldAenderDimensions;
import cz.xefensor.retold.aender.generation.AenderChunkGenerator;
import cz.xefensor.retold.aender.generation.AenderIslandSampler;
import cz.xefensor.retold.aender.generation.AenderVolatility;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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

    private static final int EXTRA_REGEN_RADIUS_CHUNKS = 2;
    private static final int MAX_REGEN_CHUNKS_PER_TICK = 1;
    private static final int MIN_REGEN_INTERVAL_TICKS = 2;
    private static final int MAX_QUEUE_SIZE = 8192;
    private static final long MAX_REGEN_NANOS_PER_TICK = 2_000_000L;

    private static final Queue<Long> REGEN_QUEUE = new ArrayDeque<>();
    private static final Set<Long> QUEUED = new HashSet<>();
    private static long nextRegenGameTime;

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

    public static void enqueueIfNeeded(ServerLevel level, ChunkAccess chunk) {
        if (level.dimension() != RetoldAenderDimensions.AENDER) {
            return;
        }

        if (AenderStabilityData.get(level).isStable(chunk.getPos())) {
            return;
        }

        AenderVolatility.retainForChunk(chunk);

        if (AenderVolatility.needsRegeneration(chunk)) {
            enqueue(chunk.getPos().x(), chunk.getPos().z());
        }
    }

    public static void clearPendingRegeneration() {
        REGEN_QUEUE.clear();
        QUEUED.clear();
        nextRegenGameTime = 0L;
    }

    public static void regenerateLoadedArea(ServerLevel level, BlockPos center) {
        if (level.dimension() != RetoldAenderDimensions.AENDER) {
            return;
        }

        int centerChunkX = center.getX() >> 4;
        int centerChunkZ = center.getZ() >> 4;
        int radius = regenerationRadius(level);
        int regenerated = 0;

        // Ensure the arrival chunk exists. Newly generated chunks already carry the current signature.
        level.getChunk(centerChunkX, centerChunkZ);

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                ChunkAccess chunk = getLoadedChunk(level, centerChunkX + dx, centerChunkZ + dz);

                if (chunk == null || AenderStabilityData.get(level).isStable(chunk.getPos())) {
                    continue;
                }

                AenderVolatility.retainForChunk(chunk);

                if (AenderVolatility.needsRegeneration(chunk)) {
                    AenderChunkGenerator.regenerateLoadedChunk(level, chunk);
                    regenerated++;
                }
            }
        }

        if (regenerated > 0) {
            Retold.LOGGER.debug(
                    "Regenerated {} stale loaded Aender chunks in the arrival view around {}, {}",
                    regenerated,
                    centerChunkX,
                    centerChunkZ
            );
        }
    }

    /**
     * Loads and prepares the complete client view before a player arrives in the Aender.
     *
     * Unlike {@link #regenerateLoadedArea(ServerLevel, BlockPos)}, this deliberately
     * forces missing chunks to load. Doing that while the portal transition is still
     * being resolved keeps the player on the source side until the new reality is
     * complete, instead of exposing the one-chunk-at-a-time fallback queue.
     */
    public static void prepareArrivalView(ServerLevel level, BlockPos center) {
        if (level.dimension() != RetoldAenderDimensions.AENDER) {
            return;
        }

        int centerChunkX = center.getX() >> 4;
        int centerChunkZ = center.getZ() >> 4;
        int radius = arrivalPreparationRadius(level);
        int loaded = 0;
        int regenerated = 0;
        long startedAtNanos = System.nanoTime();
        AenderStabilityData stability = AenderStabilityData.get(level);

        // Work from the center out so the essential arrival chunks are prepared first.
        for (int ring = 0; ring <= radius; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) {
                        continue;
                    }

                    int chunkX = centerChunkX + dx;
                    int chunkZ = centerChunkZ + dz;
                    ChunkAccess chunk = getLoadedChunk(level, chunkX, chunkZ);

                    if (chunk == null) {
                        chunk = level.getChunk(chunkX, chunkZ);
                        loaded++;
                    }

                    if (stability.isStable(chunk.getPos())) {
                        continue;
                    }

                    AenderVolatility.retainForChunk(chunk);

                    if (AenderVolatility.needsRegeneration(chunk)) {
                        AenderChunkGenerator.regenerateLoadedChunk(level, chunk);
                        regenerated++;
                    }
                }
            }
        }

        if (loaded > 0 || regenerated > 0) {
            Retold.LOGGER.debug(
                    "Prepared Aender arrival view around {}, {} in {} ms: loaded {} chunks and regenerated {} chunks",
                    centerChunkX,
                    centerChunkZ,
                    (System.nanoTime() - startedAtNanos) / 1_000_000L,
                    loaded,
                    regenerated
            );
        }
    }

    /**
     * Refreshes one chunk only if Minecraft's asynchronous ticket pipeline has
     * already made it available. This never blocks waiting for generation and is
     * therefore safe to call in small batches during a portal charge.
     */
    public static boolean prepareLoadedArrivalChunk(ServerLevel level, int chunkX, int chunkZ) {
        if (level.dimension() != RetoldAenderDimensions.AENDER) {
            return false;
        }

        ChunkAccess chunk = getLoadedChunk(level, chunkX, chunkZ);

        if (chunk == null) {
            return false;
        }

        if (AenderStabilityData.get(level).isStable(chunk.getPos())) {
            return true;
        }

        AenderVolatility.retainForChunk(chunk);

        if (AenderVolatility.needsRegeneration(chunk)) {
            AenderChunkGenerator.regenerateLoadedChunk(level, chunk);
        }

        return true;
    }

    private static void queueNearbyStaleChunks(ServerLevel level) {
        AenderStabilityData stability = AenderStabilityData.get(level);
        int radius = regenerationRadius(level);

        for (ServerPlayer player : level.players()) {
            BlockPos playerPos = player.blockPosition();

            int centerChunkX = playerPos.getX() >> 4;
            int centerChunkZ = playerPos.getZ() >> 4;

            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
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
        long gameTime = level.getGameTime();

        if (gameTime < nextRegenGameTime) {
            return;
        }

        int regenerated = 0;
        long startNanos = System.nanoTime();

        while (regenerated < MAX_REGEN_CHUNKS_PER_TICK && !REGEN_QUEUE.isEmpty()) {
            if (System.nanoTime() - startNanos >= MAX_REGEN_NANOS_PER_TICK) {
                break;
            }

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

            AenderChunkGenerator.regenerateLoadedChunk(level, chunk);
            regenerated++;
        }

        if (regenerated > 0) {
            nextRegenGameTime = gameTime + MIN_REGEN_INTERVAL_TICKS;
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

    private static int regenerationRadius(ServerLevel level) {
        return level.getServer().getPlayerList().getViewDistance() + EXTRA_REGEN_RADIUS_CHUNKS;
    }

    public static int arrivalPreparationRadius(ServerLevel level) {
        return level.getServer().getPlayerList().getViewDistance();
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
