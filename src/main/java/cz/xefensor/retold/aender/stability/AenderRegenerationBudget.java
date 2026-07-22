package cz.xefensor.retold.aender.stability;

/**
 * Pure tick-time policy for adaptive Aender regeneration work.
 */
final class AenderRegenerationBudget {
    private static final long MIN_USEFUL_BUDGET_NANOS = 500_000L;

    private AenderRegenerationBudget() {
    }

    static long calculate(long averageTickNanos, long targetTickNanos, long maximumBudgetNanos) {
        if (targetTickNanos <= 0L || maximumBudgetNanos <= 0L) {
            return 0L;
        }

        long safeTickCeiling = targetTickNanos * 9L / 10L;
        long availableHeadroom = safeTickCeiling - Math.max(0L, averageTickNanos);

        if (availableHeadroom <= 0L) {
            return 0L;
        }

        // Spend only half of measured headroom so unrelated work still has room to spike.
        long budget = Math.min(maximumBudgetNanos, availableHeadroom / 2L);
        return budget >= MIN_USEFUL_BUDGET_NANOS ? budget : 0L;
    }
}
