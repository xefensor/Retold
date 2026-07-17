package cz.xefensor.retold.behavior.pack;

final class RetoldPackTuning {
    private static final double WOLF_PACK_RADIUS_BLOCKS = 30.0D;
    private static final double DOLPHIN_PACK_RADIUS_BLOCKS = 36.0D;

    private static final int MAX_WOLF_HUNTING_PARTY_SIZE = 3;
    private static final int MAX_DOLPHIN_HUNTING_PARTY_SIZE = 5;

    private static final int MIN_WOLF_HUNTING_PARTY_SIZE = 2;
    private static final int MIN_DOLPHIN_HUNTING_PARTY_SIZE = 2;

    private static final double WOLF_SEARCH_SPEED = 0.98D;
    private static final double DOLPHIN_SEARCH_SPEED = 1.08D;

    private static final double WOLF_HUNT_SPEED = 1.30D;
    private static final double DOLPHIN_HUNT_SPEED = 1.36D;

    private RetoldPackTuning() {
    }

    static double packRadius(String path) {
        if (isDolphin(path)) {
            return DOLPHIN_PACK_RADIUS_BLOCKS;
        }

        return WOLF_PACK_RADIUS_BLOCKS;
    }

    static int maxPartySize(String path) {
        if (isDolphin(path)) {
            return MAX_DOLPHIN_HUNTING_PARTY_SIZE;
        }

        return MAX_WOLF_HUNTING_PARTY_SIZE;
    }

    static int minPartySize(String path) {
        if (isDolphin(path)) {
            return MIN_DOLPHIN_HUNTING_PARTY_SIZE;
        }

        return MIN_WOLF_HUNTING_PARTY_SIZE;
    }

    static double searchSpeed(String path) {
        if (isDolphin(path)) {
            return DOLPHIN_SEARCH_SPEED;
        }

        return WOLF_SEARCH_SPEED;
    }

    static double huntSpeed(String path) {
        if (isDolphin(path)) {
            return DOLPHIN_HUNT_SPEED;
        }

        return WOLF_HUNT_SPEED;
    }

    private static boolean isDolphin(String path) {
        return path.equals("dolphin");
    }
}
