package cz.xefensor.retold.aender.generation;

public enum AenderBiomeKind {
    PLAINS,
    DESERT;

    private static final double DESERT_CHANCE = 0.34D;

    static AenderBiomeKind fromIslandSeed(long islandSeed) {
        if (AenderVolatility.currentGeneratorVersion() < AenderRealityData.ISLAND_BIOMES_GENERATOR_VERSION) {
            return PLAINS;
        }

        return unit(islandSeed ^ 0xD35E47A3B10C4E5FL) < DESERT_CHANCE ? DESERT : PLAINS;
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
