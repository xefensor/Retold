package cz.xefensor.retold.aender.generation;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AenderVolatility {
    private static long realitySalt = mix64(0xA3D1E41F29B7C53DL);
    private static long realityEpoch = 0L;
    private static int generatorVersion = AenderRealityData.LEGACY_GENERATOR_VERSION;
    private static AenderRealityData realityData;

    private static final Map<RegionKey, Long> ACTIVE_REGION_SALTS = new HashMap<>();
    private static final Map<RegionKey, Integer> REGION_REFS = new HashMap<>();

    private static final Set<Long> RETAINED_CHUNKS = new HashSet<>();
    /*
     * Runtime cache of the persistent per-chunk reality attachment. If surrounding
     * region epochs change, the signature changes and the chunk must regenerate.
     */
    private static final Map<Long, Long> CHUNK_GENERATION_SIGNATURES = new HashMap<>();

    /*
     * Region-column epoch. Incrementing this makes the region generate differently.
     */
    private static final Map<RegionColumn, Long> REGION_EPOCHS = new HashMap<>();

    private AenderVolatility() {
    }

    public static synchronized void initializeReality(ServerLevel level) {
        installReality(AenderRealityData.get(level));
    }

    public static synchronized void enableCurrentGeneratorForFreshWorld(ServerLevel level) {
        AenderRealityData data = AenderRealityData.get(level);
        data.enableCurrentGeneratorForFreshWorld();
        level.getServer().getDataStorage().saveAndJoin();
        installReality(data);
    }

    public static synchronized void advanceReality(ServerLevel level) {
        AenderRealityData data = AenderRealityData.get(level);
        data.advanceReality();

        /*
         * Persist the new seed before any destination warm-up can generate chunks
         * from it. A normal quit also joins pending SavedData writes, while this
         * immediate save protects the reality boundary from a subsequent crash.
         */
        level.getServer().getDataStorage().saveAndJoin();
        installReality(data);
    }

    public static synchronized void clearRuntime() {
        ACTIVE_REGION_SALTS.clear();
        REGION_REFS.clear();
        RETAINED_CHUNKS.clear();
        CHUNK_GENERATION_SIGNATURES.clear();
        REGION_EPOCHS.clear();
        realityData = null;
        realitySalt = mix64(0xA3D1E41F29B7C53DL);
        realityEpoch = 0L;
        generatorVersion = AenderRealityData.LEGACY_GENERATOR_VERSION;
    }

    private static void installReality(AenderRealityData data) {
        ACTIVE_REGION_SALTS.clear();
        REGION_REFS.clear();
        RETAINED_CHUNKS.clear();
        CHUNK_GENERATION_SIGNATURES.clear();
        REGION_EPOCHS.clear();
        realityData = data;
        realitySalt = data.seed();
        realityEpoch = data.epoch();
        generatorVersion = data.generatorVersion();
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

        /*
         * The chunk attachment is the durable source of truth. Do not retain a
         * duplicate signature for every chunk visited during a long-running
         * server session after that chunk has unloaded.
         */
        CHUNK_GENERATION_SIGNATURES.remove(chunkKey);

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

    public static synchronized List<ChunkPos> retainedChunkPositions() {
        List<ChunkPos> positions = new ArrayList<>(RETAINED_CHUNKS.size());

        for (long key : RETAINED_CHUNKS) {
            positions.add(new ChunkPos((int) key, (int) (key >> 32)));
        }

        positions.sort(Comparator.comparingInt(ChunkPos::x).thenComparingInt(ChunkPos::z));
        return List.copyOf(positions);
    }

    public static synchronized long islandSeed(int regionX, int regionZ, int layerY) {
        RegionKey key = new RegionKey(regionX, regionZ, layerY);
        return ACTIVE_REGION_SALTS.computeIfAbsent(key, AenderVolatility::createRegionSalt);
    }

    public static synchronized void markGenerated(ChunkAccess chunk) {
        long signature = chunkEpochSignature(chunk);
        CHUNK_GENERATION_SIGNATURES.put(chunkKey(chunk), signature);
        chunk.setData(AenderAttachments.CHUNK_REALITY, AenderChunkRealityData.current(signature));
        chunk.markUnsaved();
    }

    public static synchronized boolean wasGeneratedThisSession(ChunkAccess chunk) {
        return !needsRegeneration(chunk);
    }

    public static synchronized boolean needsRegeneration(ChunkAccess chunk) {
        Long previous = CHUNK_GENERATION_SIGNATURES.get(chunkKey(chunk));

        if (previous != null) {
            return previous.longValue() != chunkEpochSignature(chunk);
        }

        AenderChunkRealityData persisted = chunk.getExistingDataOrNull(AenderAttachments.CHUNK_REALITY);
        long currentSignature = chunkEpochSignature(chunk);

        if (persisted == null) {
            /*
             * Migration for chunks saved before persistent reality signatures.
             * Adopt their blocks into the current reality instead of destroying
             * player builds merely because the old runtime cache was not saved.
             */
            CHUNK_GENERATION_SIGNATURES.put(chunkKey(chunk), currentSignature);
            chunk.setData(AenderAttachments.CHUNK_REALITY, AenderChunkRealityData.current(currentSignature));
            chunk.markUnsaved();
            return false;
        }

        if (persisted.stale()) {
            return true;
        }

        CHUNK_GENERATION_SIGNATURES.put(chunkKey(chunk), persisted.signature());
        return persisted.signature() != currentSignature;
    }

    public static synchronized void forgetGeneratedMark(ChunkAccess chunk) {
        CHUNK_GENERATION_SIGNATURES.remove(chunkKey(chunk));
        chunk.setData(AenderAttachments.CHUNK_REALITY, AenderChunkRealityData.STALE);
        chunk.markUnsaved();
    }

    public static synchronized void advanceTransientRealityForTest() {
        ACTIVE_REGION_SALTS.clear();
        REGION_REFS.clear();
        RETAINED_CHUNKS.clear();
        CHUNK_GENERATION_SIGNATURES.clear();
        REGION_EPOCHS.clear();

        realityEpoch++;
        realitySalt = mix64(
                realitySalt
                        ^ realityEpoch * 0x9E3779B97F4A7C15L
                        ^ 0xA3D1E41F29B7C53DL
        );
        realityData = null;
    }

    public static synchronized long currentRealityEpoch() {
        return realityEpoch;
    }

    public static synchronized int currentGeneratorVersion() {
        return generatorVersion;
    }

    public static synchronized void advanceRegion(ServerLevel level, int regionX, int regionZ) {
        forgetRegionColumn(new RegionColumn(regionX, regionZ));
        level.getServer().getDataStorage().scheduleSave();
    }

    private static void forgetRegionColumn(RegionColumn column) {
        if (realityData == null) {
            REGION_EPOCHS.merge(column, 1L, Long::sum);
        } else {
            realityData.advanceRegion(column.regionX(), column.regionZ());
        }

        /*
         * Important:
         * Remove salts, but DO NOT remove REGION_REFS.
         * Some chunks may still be loaded in memory. Keeping refs prevents the new salts
         * from being deleted immediately by cleanup.
         */
        ACTIVE_REGION_SALTS.keySet().removeIf(key ->
                key.regionX() == column.regionX() && key.regionZ() == column.regionZ()
        );
    }

    public static synchronized int activeRegionCount() {
        return ACTIVE_REGION_SALTS.size();
    }

    public static synchronized int retainedChunkCount() {
        return RETAINED_CHUNKS.size();
    }

    public static synchronized int cachedChunkSignatureCount() {
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
                realitySalt
                        ^ realityEpoch * 0xD1B54A32D192ED03L
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

        long signature = mix64(
                realitySalt
                        ^ realityEpoch * 0xD1B54A32D192ED03L
                        ^ (long) generatorVersion * 0x94D049BB133111EBL
                        ^ 0x9E3779B97F4A7C15L
        );

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
        if (realityData != null) {
            return realityData.regionEpoch(regionX, regionZ);
        }

        return REGION_EPOCHS.getOrDefault(new RegionColumn(regionX, regionZ), 0L);
    }

    private static void cleanupUnreferencedSalts() {
        ACTIVE_REGION_SALTS.keySet().removeIf(key -> !REGION_REFS.containsKey(key));
    }

    private static long chunkKey(ChunkAccess chunk) {
        return chunkKey(chunk.getPos());
    }

    private static long chunkKey(ChunkPos pos) {
        int x = pos.x();
        int z = pos.z();

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
