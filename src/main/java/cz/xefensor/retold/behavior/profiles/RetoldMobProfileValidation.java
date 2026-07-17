package cz.xefensor.retold.behavior.profiles;

import java.util.Optional;

public final class RetoldMobProfileValidation {
    private static final int DISABLED_THRESHOLD = 101;

    private RetoldMobProfileValidation() {
    }

    public static Optional<String> validate(
            String serializedType,
            int hungerIntervalTicks,
            int eatThreshold,
            int huntThreshold
    ) {
        if ("none".equals(serializedType)) {
            return Optional.of("The NONE profile type is reserved for fallback behavior");
        }

        if (hungerIntervalTicks < 0) {
            return Optional.of("hunger_interval_ticks must be non-negative");
        }

        if (!isValidThreshold(eatThreshold)) {
            return Optional.of("eat_threshold must be between 0 and 101");
        }

        if (!isValidThreshold(huntThreshold)) {
            return Optional.of("hunt_threshold must be between 0 and 101");
        }

        return Optional.empty();
    }

    private static boolean isValidThreshold(int threshold) {
        return threshold >= 0 && threshold <= DISABLED_THRESHOLD;
    }
}
