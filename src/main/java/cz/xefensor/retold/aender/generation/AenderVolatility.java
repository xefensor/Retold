package cz.xefensor.retold.aender.generation;

import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class AenderVolatility {
    private static final long ROOT_SALT = mix64(System.nanoTime() ^ 0xA3D1E41F29B7C53DL);

    private static final Map<RegionKey, Long> ACTIVE_REGION_SALTS = new HashMap<>();
    private static final Map<RegionKey, Integer> REGION_REFS = new HashMap<>();
    private static final Set<Long> RETAINED_CHUNKS = new HashSet<>();

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

    public static synchronized int activeRegionCount() {
        return ACTIVE_REGION_SALTS.size();
    }

    public static synchronized int retainedChunkCount() {
        return RETAINED_CHUNKS.size();
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
        serial++;

        return mix64(
                ROOT_SALT
                        ^ (long) key.regionX * 0x632BE59BD9B4E019L
                        ^ (long) key.regionZ * 0x85157AF5C91D1B35L
                        ^ (long) key.layerY * 0x9E3779B97F4A7C15L
                        ^ serial * 0xD1B54A32D192ED03L
                        ^ System.nanoTime()
        );
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

    @FunctionalInterface
    private interface RegionConsumer {
        void accept(RegionKey key);
    }
}