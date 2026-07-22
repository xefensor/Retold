package cz.xefensor.retold.aender.generation;

enum AenderUndersideProfile {
    TAPERED,
    ROOTED,
    FRACTURED,
    TERRACED;

    static AenderUndersideProfile fromSeed(AenderBiomeKind biome, long seed) {
        double roll = unit(seed ^ 0x554E444552534944L);

        if (biome == AenderBiomeKind.DESERT) {
            if (roll < 0.45D) {
                return TERRACED;
            }
            if (roll < 0.78D) {
                return FRACTURED;
            }
            return TAPERED;
        }

        if (roll < 0.40D) {
            return ROOTED;
        }
        if (roll < 0.70D) {
            return TAPERED;
        }
        return FRACTURED;
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
