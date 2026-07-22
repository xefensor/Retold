package cz.xefensor.retold.aender.stability;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AenderRegenerationBudgetTest {
    private static final long TARGET_TICK_NANOS = 50_000_000L;
    private static final long MAXIMUM_BUDGET_NANOS = 12_000_000L;

    @Test
    void usesMaximumBudgetWhenServerHasPlentyOfHeadroom() {
        assertEquals(
                MAXIMUM_BUDGET_NANOS,
                AenderRegenerationBudget.calculate(10_000_000L, TARGET_TICK_NANOS, MAXIMUM_BUDGET_NANOS)
        );
    }

    @Test
    void scalesBudgetDownAsTickTimeRises() {
        assertEquals(
                5_000_000L,
                AenderRegenerationBudget.calculate(35_000_000L, TARGET_TICK_NANOS, MAXIMUM_BUDGET_NANOS)
        );
        assertEquals(
                500_000L,
                AenderRegenerationBudget.calculate(44_000_000L, TARGET_TICK_NANOS, MAXIMUM_BUDGET_NANOS)
        );
    }

    @Test
    void pausesBeforeTickBudgetIsExhausted() {
        assertEquals(
                0L,
                AenderRegenerationBudget.calculate(45_000_000L, TARGET_TICK_NANOS, MAXIMUM_BUDGET_NANOS)
        );
        assertEquals(
                0L,
                AenderRegenerationBudget.calculate(60_000_000L, TARGET_TICK_NANOS, MAXIMUM_BUDGET_NANOS)
        );
    }
}
