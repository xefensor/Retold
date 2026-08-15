package cz.xefensor.retold.behavior.ecology;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RetoldUnloadedBreedingProgressTest {
    @Test
    void advancesOnlyWhileContinuouslyFull() {
        var progress = new RetoldUnloadedBreedingProgress(
                1_000L,
                0,
                10_000L,
                false,
                true
        );

        progress.updateHunger(20, 12_000L);
        progress.updateHunger(21, 13_000L);
        progress.updateHunger(0, 14_000L);

        assertEquals(2_000L, progress.finish(16_000L));
    }

    @Test
    void cappedHistoryDiscardsEarlierProgress() {
        var progress = new RetoldUnloadedBreedingProgress(
                5_900L,
                0,
                10_000L,
                true,
                true
        );

        assertEquals(500L, progress.finish(10_500L));
    }

    @Test
    void hungryHistoryCannotPreserveEarlierProgress() {
        var progress = new RetoldUnloadedBreedingProgress(
                5_900L,
                30,
                10_000L,
                false,
                true
        );

        progress.updateHunger(0, 11_000L);

        assertEquals(1_000L, progress.finish(12_000L));
    }

    @Test
    void ineligibleAnimalNeverAccumulatesProgress() {
        var progress = new RetoldUnloadedBreedingProgress(
                5_900L,
                0,
                10_000L,
                false,
                false
        );

        assertEquals(0L, progress.finish(20_000L));
    }
}
