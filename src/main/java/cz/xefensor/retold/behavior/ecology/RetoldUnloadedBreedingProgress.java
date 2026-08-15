package cz.xefensor.retold.behavior.ecology;

import cz.xefensor.retold.behavior.profiles.RetoldHungerStage;

/** Tracks one continuous well-fed interval across catch-up hunger events. */
final class RetoldUnloadedBreedingProgress {
    private long satisfiedTicks;
    private int hunger;
    private long progressedThrough;
    private final boolean enabled;

    RetoldUnloadedBreedingProgress(
            long existingSatisfiedTicks,
            int initialHunger,
            long simulatedFrom,
            boolean resetExistingProgress,
            boolean enabled
    ) {
        this.enabled = enabled;
        hunger = clampHunger(initialHunger);
        progressedThrough = Math.max(0L, simulatedFrom);
        satisfiedTicks = !enabled
                || resetExistingProgress
                || !isFull(hunger)
                ? 0L
                : Math.max(0L, existingSatisfiedTicks);
    }

    void updateHunger(int updatedHunger, long at) {
        advanceTo(at);
        hunger = clampHunger(updatedHunger);

        if (!enabled || !isFull(hunger)) {
            satisfiedTicks = 0L;
        }
    }

    long finish(long simulatedThrough) {
        advanceTo(simulatedThrough);
        return satisfiedTicks;
    }

    private void advanceTo(long at) {
        long updatedAt = Math.max(progressedThrough, at);

        if (enabled && isFull(hunger)) {
            satisfiedTicks = saturatingAdd(
                    satisfiedTicks,
                    updatedAt - progressedThrough
            );
        } else {
            satisfiedTicks = 0L;
        }

        progressedThrough = updatedAt;
    }

    private static boolean isFull(int hunger) {
        return hunger <= RetoldHungerStage.FULL.maxInclusive();
    }

    private static int clampHunger(int hunger) {
        return Math.max(0, Math.min(100, hunger));
    }

    private static long saturatingAdd(long first, long second) {
        if (second <= 0L) {
            return first;
        }

        return first > Long.MAX_VALUE - second
                ? Long.MAX_VALUE
                : first + second;
    }
}
