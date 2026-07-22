package cz.xefensor.retold.aender.generation;

/** Large-scale silhouette shared by terrain, decoration, and biome sampling. */
public enum AenderIslandArchetype {
    ROUND,
    ELONGATED,
    CRESCENT,
    SPLIT,
    TWIN,
    PLATEAU,
    DUNES,
    ERODED_MESA,
    SHELF;

    static AenderIslandArchetype fromSeed(AenderBiomeKind biome, long seed) {
        double roll = unit(seed ^ 0x4152434845545950L);

        if (biome == AenderBiomeKind.DESERT) {
            if (roll < 0.32D) {
                return PLATEAU;
            }
            if (roll < 0.58D) {
                return DUNES;
            }
            if (roll < 0.82D) {
                return ERODED_MESA;
            }
            return SHELF;
        }

        if (roll < 0.22D) {
            return ROUND;
        }
        if (roll < 0.40D) {
            return ELONGATED;
        }
        if (roll < 0.58D) {
            return CRESCENT;
        }
        if (roll < 0.74D) {
            return SPLIT;
        }
        return TWIN;
    }

    boolean hasStrongErosion() {
        return this == CRESCENT || this == SPLIT || this == ERODED_MESA;
    }

    boolean isDesertShape() {
        return this == PLATEAU || this == DUNES || this == ERODED_MESA || this == SHELF;
    }

    private static double unit(long seed) {
        long value = mix64(seed);
        return (value >>> 11) * 0x1.0p-53;
    }

    private static long mix64(long value) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= value >>> 33;
        return value;
    }
}
