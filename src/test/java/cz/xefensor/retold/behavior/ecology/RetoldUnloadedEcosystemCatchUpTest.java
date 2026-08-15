package cz.xefensor.retold.behavior.ecology;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RetoldUnloadedEcosystemCatchUpTest {
    private static final int HUNGER_INTERVAL = 400;

    @Test
    void preservesRemainderForUncappedCatchUp() {
        long lastSimulatedAt = 1_000L;
        long gameTime = lastSimulatedAt + 2L * HUNGER_INTERVAL + 75L;

        var plan = RetoldUnloadedCatchUpPlan.calculate(
                lastSimulatedAt,
                gameTime,
                HUNGER_INTERVAL
        );

        assertEquals(2, plan.hungerPulses());
        assertEquals(lastSimulatedAt, plan.simulatedFrom());
        assertEquals(lastSimulatedAt + 2L * HUNGER_INTERVAL, plan.simulatedThrough());
        assertEquals(0, plan.mealOpportunities());
        assertFalse(plan.capped());
    }

    @Test
    void capsCatchUpAtSevenMinecraftDaysAndDiscardsOlderDebt() {
        long lastSimulatedAt = 1_000L;
        long gameTime = lastSimulatedAt
                + RetoldUnloadedCatchUpPlan.MAX_CATCH_UP_TICKS
                + 10L * 24_000L;

        var plan = RetoldUnloadedCatchUpPlan.calculate(
                lastSimulatedAt,
                gameTime,
                HUNGER_INTERVAL
        );

        assertEquals(
                RetoldUnloadedCatchUpPlan.MAX_CATCH_UP_TICKS / HUNGER_INTERVAL,
                plan.hungerPulses()
        );
        assertEquals(gameTime, plan.simulatedThrough());
        assertEquals(
                gameTime - RetoldUnloadedCatchUpPlan.MAX_CATCH_UP_TICKS,
                plan.simulatedFrom()
        );
        assertEquals(7, plan.mealOpportunities());
        assertTrue(plan.capped());
    }

    @Test
    void ignoresInvalidOrNotYetDueTimestamps() {
        assertEquals(
                0,
                RetoldUnloadedCatchUpPlan.calculate(0L, 5_000L, HUNGER_INTERVAL)
                        .hungerPulses()
        );
        assertEquals(
                0,
                RetoldUnloadedCatchUpPlan.calculate(5_000L, 4_000L, HUNGER_INTERVAL)
                        .hungerPulses()
        );
        assertEquals(
                0,
                RetoldUnloadedCatchUpPlan.calculate(1_000L, 1_399L, HUNGER_INTERVAL)
                        .hungerPulses()
        );
    }
}
