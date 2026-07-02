package cz.xefensor.retold.aender.generation;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AenderVolatility {
    private static final long ROOT_SALT = mix64(System.nanoTime() ^ 0xA3D1E41F29B7C53DL);

    private static final Map<RegionKey, Long> ACTIVE_REGION_SALTS = new HashMap<>();
    private static final Map<RegionKey, Integer> REGION_REFS = new HashMap<>();

    private static final Set<Long> RETAINED_CHUNKS = new HashSet<>();

    /*
     * Stores the "reality signature" that a chunk was last generated with.
     * If surrounding region epochs change, this signature changes and the chunk must regenerate.
     */
    private static final Map<Long, Long> CHUNK_GENERATION_SIGNATURES = new HashMap<>();

    /*
     * Region-column epoch. Incrementing this makes the region generate differently.
     */
    private static final Map<RegionColumn, Long> REGION_EPOCHS = new HashMap<>();

    private static long serial = 0L;

    private AenderVolatility() {
    }

    public static synchronized void retainForChunk(ChunkAccess chunk) {
        long chunkKey = chunkKey(chunk);

        if (!RETAINED_CHUNKS.add(chunkKey)) {
            return;
        }

        forEachRegionNeededByChunk(chunk, key -> {
            ACTIVE_REGION_SALTS.computeIfAbsent(key, AenderVolatility::createRegionSalt);
            REGION_REFS.merge(key, 1, Integer::sum);
        });
    }

    public static synchronized void releaseForChunk(ChunkAccess chunk) {
        long chunkKey = chunkKey(chunk);

        if (!RETAINED_CHUNKS.remove(chunkKey)) {
            return;
        }

        forEachRegionNeededByChunk(chunk, key -> {
            int next = REGION_REFS.getOrDefault(key, 0) - 1;

            if (next <= 0) {
                REGION_REFS.remove(key);
                ACTIVE_REGION_SALTS.remove(key);
            } else {
                REGION_REFS.put(key, next);
            }
        });

        cleanupUnreferencedSalts();
    }

    public static synchronized long islandSeed(int regionX, int regionZ, int layerY) {
        RegionKey key = new RegionKey(regionX, regionZ, layerY);
        return ACTIVE_REGION_SALTS.computeIfAbsent(key, AenderVolatility::createRegionSalt);
    }

    public static synchronized void markGenerated(ChunkAccess chunk) {
        CHUNK_GENERATION_SIGNATURES.put(chunkKey(chunk), chunkEpochSignature(chunk));
    }

    public static synchronized boolean wasGeneratedThisSession(ChunkAccess chunk) {
        return !needsRegeneration(chunk);
    }

    public static synchronized boolean needsRegeneration(ChunkAccess chunk) {
        Long previous = CHUNK_GENERATION_SIGNATURES.get(chunkKey(chunk));

        if (previous == null) {
            return true;
        }

        return previous.longValue() != chunkEpochSignature(chunk);
    }

    public static synchronized void forgetGeneratedMark(ChunkAccess chunk) {
        CHUNK_GENERATION_SIGNATURES.remove(chunkKey(chunk));
    }

    public static synchronized void clearForgottenWorld() {
        ACTIVE_REGION_SALTS.clear();
        REGION_REFS.clear();
        RETAINED_CHUNKS.clear();
        CHUNK_GENERATION_SIGNATURES.clear();
        REGION_EPOCHS.clear();
        serial++;
    }

    public static synchronized void forgetFarRegionColumns(List<ServerPlayer> players, int forgetDistanceBlocks) {
        if (players.isEmpty()) {
            clearForgottenWorld();
            return;
        }

        Set<RegionColumn> columns = new HashSet<>();

        for (RegionKey key : ACTIVE_REGION_SALTS.keySet()) {
            columns.add(new RegionColumn(key.regionX(), key.regionZ()));
        }

        int forgotten = 0;

        for (RegionColumn column : columns) {
            if (isColumnNearAnyPlayer(column, players, forgetDistanceBlocks)) {
                continue;
            }

            forgetRegionColumn(column);
            forgotten++;
        }

        if (forgotten > 0) {
            System.out.println("[Aender] forgot " + forgotten + " far region columns");
        }
    }

    private static void forgetRegionColumn(RegionColumn column) {
        REGION_EPOCHS.merge(column, 1L, Long::sum);

        /*
         * Important:
         * Remove salts, but DO NOT remove REGION_REFS.
         * Some chunks may still be loaded in memory. Keeping refs prevents the new salts
         * from being deleted immediately by cleanup.
         */
        ACTIVE_REGION_SALTS.keySet().removeIf(key ->
                key.regionX() == column.regionX() && key.regionZ() == column.regionZ()
        );

        serial++;
    }

    private static boolean isColumnNearAnyPlayer(
            RegionColumn column,
            List<ServerPlayer> players,
            int forgetDistanceBlocks
    ) {
        double centerX = (column.regionX() + 0.5D) * AenderIslandSampler.REGION_SIZE;
        double centerZ = (column.regionZ() + 0.5D) * AenderIslandSampler.REGION_SIZE;

        double maxDistanceSq = (double) forgetDistanceBlocks * (double) forgetDistanceBlocks;

        for (ServerPlayer player : players) {
            double dx = player.getX() - centerX;
            double dz = player.getZ() - centerZ;

            if (dx * dx + dz * dz <= maxDistanceSq) {
                return true;
            }
        }

        return false;
    }

    public static synchronized int activeRegionCount() {
        return ACTIVE_REGION_SALTS.size();
    }

    public static synchronized int retainedChunkCount() {
        return RETAINED_CHUNKS.size();
    }

    public static synchronized int generatedThisSessionCount() {
        return CHUNK_GENERATION_SIGNATURES.size();
    }

    public static synchronized int refsForRegionColumn(int regionX, int regionZ) {
        int refs = 0;

        for (Map.Entry<RegionKey, Integer> entry : REGION_REFS.entrySet()) {
            RegionKey key = entry.getKey();

            if (key.regionX() == regionX && key.regionZ() == regionZ) {
                refs += entry.getValue();
            }
        }

        return refs;
    }

    public static int regionXForBlock(int blockX) {
        return Math.floorDiv(blockX, AenderIslandSampler.REGION_SIZE);
    }

    public static int regionZForBlock(int blockZ) {
        return Math.floorDiv(blockZ, AenderIslandSampler.REGION_SIZE);
    }

    private static void forEachRegionNeededByChunk(ChunkAccess chunk, RegionConsumer consumer) {
        int minX = chunk.getPos().getMinBlockX();
        int maxX = minX + 15;
        int minZ = chunk.getPos().getMinBlockZ();
        int maxZ = minZ + 15;

        int minRegionX = Math.floorDiv(minX, AenderIslandSampler.REGION_SIZE) - 1;
        int maxRegionX = Math.floorDiv(maxX, AenderIslandSampler.REGION_SIZE) + 1;
        int minRegionZ = Math.floorDiv(minZ, AenderIslandSampler.REGION_SIZE) - 1;
        int maxRegionZ = Math.floorDiv(maxZ, AenderIslandSampler.REGION_SIZE) + 1;

        int minLayer = Math.floorDiv(AenderIslandSampler.MIN_Y, AenderIslandSampler.LAYER_HEIGHT) - 1;
        int maxLayer = Math.floorDiv(AenderIslandSampler.MAX_Y - 1, AenderIslandSampler.LAYER_HEIGHT) + 1;

        for (int regionX = minRegionX; regionX <= maxRegionX; regionX++) {
            for (int regionZ = minRegionZ; regionZ <= maxRegionZ; regionZ++) {
                for (int layerY = minLayer; layerY <= maxLayer; layerY++) {
                    consumer.accept(new RegionKey(regionX, regionZ, layerY));
                }
            }
        }
    }

    private static long createRegionSalt(RegionKey key) {
        long epoch = regionEpoch(key.regionX(), key.regionZ());

        return mix64(
                ROOT_SALT
                        ^ (long) key.regionX() * 0x632BE59BD9B4E019L
                        ^ (long) key.regionZ() * 0x85157AF5C91D1B35L
                        ^ (long) key.layerY() * 0x9E3779B97F4A7C15L
                        ^ epoch * 0xC2B2AE3D27D4EB4FL
        );
    }

    private static long chunkEpochSignature(ChunkAccess chunk) {
        int minX = chunk.getPos().getMinBlockX();
        int maxX = minX + 15;
        int minZ = chunk.getPos().getMinBlockZ();
        int maxZ = minZ + 15;

        /*
         * Include neighboring region columns because islandsForChunk samples neighbors.
         * Otherwise border chunks may think they are fresh even when a neighboring
         * island region changed.
         */
        int minRegionX = Math.floorDiv(minX, AenderIslandSampler.REGION_SIZE) - 1;
        int maxRegionX = Math.floorDiv(maxX, AenderIslandSampler.REGION_SIZE) + 1;
        int minRegionZ = Math.floorDiv(minZ, AenderIslandSampler.REGION_SIZE) - 1;
        int maxRegionZ = Math.floorDiv(maxZ, AenderIslandSampler.REGION_SIZE) + 1;

        long signature = 0x9E3779B97F4A7C15L;

        for (int regionX = minRegionX; regionX <= maxRegionX; regionX++) {
            for (int regionZ = minRegionZ; regionZ <= maxRegionZ; regionZ++) {
                long epoch = regionEpoch(regionX, regionZ);

                signature = mix64(signature
                        ^ (long) regionX * 0x632BE59BD9B4E019L
                        ^ (long) regionZ * 0x85157AF5C91D1B35L
                        ^ epoch * 0xC2B2AE3D27D4EB4FL);
            }
        }

        return signature;
    }

    private static long regionEpoch(int regionX, int regionZ) {
        return REGION_EPOCHS.getOrDefault(new RegionColumn(regionX, regionZ), 0L);
    }

    private static void cleanupUnreferencedSalts() {
        ACTIVE_REGION_SALTS.keySet().removeIf(key -> !REGION_REFS.containsKey(key));
    }

    private static long chunkKey(ChunkAccess chunk) {
        int x = chunk.getPos().x();
        int z = chunk.getPos().z();

        return ((long) x & 0xffffffffL) | (((long) z & 0xffffffffL) << 32);
    }

    private static long mix64(long value) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= value >>> 33;
        return value;
    }

    private record RegionKey(int regionX, int regionZ, int layerY) {
    }

    private record RegionColumn(int regionX, int regionZ) {
    }

    @FunctionalInterface
    private interface RegionConsumer {
        void accept(RegionKey key);
    }
}