package cz.xefensor.retold.behavior.ecology;

public final class RetoldUnloadedCatchUpPlan {
    public static final long MAX_CATCH_UP_TICKS = 7L * 24_000L;

    private static final long MEAL_INTERVAL_TICKS = 24_000L;

    private RetoldUnloadedCatchUpPlan() {
    }

    public static Plan calculate(
            long lastSimulatedAt,
            long gameTime,
            int hungerInterval
    ) {
        if (lastSimulatedAt <= 0L
                || gameTime <= lastSimulatedAt
                || hungerInterval <= 0) {
            return Plan.none(lastSimulatedAt);
        }

        long elapsedTicks = gameTime - lastSimulatedAt;
        long simulatedTicks = Math.min(elapsedTicks, MAX_CATCH_UP_TICKS);
        int hungerPulses = (int) Math.min(
                Integer.MAX_VALUE,
                simulatedTicks / hungerInterval
        );

        if (hungerPulses <= 0) {
            return Plan.none(lastSimulatedAt);
        }

        boolean capped = elapsedTicks > MAX_CATCH_UP_TICKS;
        long simulatedFrom = capped
                ? gameTime - simulatedTicks
                : lastSimulatedAt;
        long simulatedThrough = capped
                ? gameTime
                : lastSimulatedAt + (long) hungerPulses * hungerInterval;

        return new Plan(
                hungerPulses,
                simulatedFrom,
                simulatedThrough,
                simulatedTicks,
                (int) (simulatedTicks / MEAL_INTERVAL_TICKS),
                capped
        );
    }

    public record Plan(
            int hungerPulses,
            long simulatedFrom,
            long simulatedThrough,
            long simulatedTicks,
            int mealOpportunities,
            boolean capped
    ) {
        private static Plan none(long lastSimulatedAt) {
            return new Plan(0, lastSimulatedAt, lastSimulatedAt, 0L, 0, false);
        }
    }
}
