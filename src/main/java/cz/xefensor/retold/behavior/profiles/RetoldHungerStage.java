package cz.xefensor.retold.behavior.profiles;

public enum RetoldHungerStage {
    FULL(0, 20),
    EASY_FOOD(21, 40),
    ACTIVE_SEARCH(41, 65),
    RISKY_FOOD(66, 85),
    DESPERATE(86, 100),
    DISABLED(101, Integer.MAX_VALUE);

    private final int minInclusive;
    private final int maxInclusive;

    RetoldHungerStage(
            int minInclusive,
            int maxInclusive
    ) {
        this.minInclusive = minInclusive;
        this.maxInclusive = maxInclusive;
    }

    public int minInclusive() {
        return minInclusive;
    }

    public int maxInclusive() {
        return maxInclusive;
    }

    public boolean isAtLeast(RetoldHungerStage other) {
        if (other == null) {
            return false;
        }

        if (this == DISABLED || other == DISABLED) {
            return this == other;
        }

        return ordinal() >= other.ordinal();
    }

    public static RetoldHungerStage fromHunger(int hunger) {
        if (hunger > 100) {
            return DISABLED;
        }

        int clamped = Math.max(0, hunger);

        for (RetoldHungerStage stage : values()) {
            if (clamped >= stage.minInclusive && clamped <= stage.maxInclusive) {
                return stage;
            }
        }

        return DESPERATE;
    }
}
