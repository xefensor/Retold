package cz.xefensor.retold.aender.stability;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AenderDeferredReleasePolicyTest {
    @Test
    void watchedReleasedChunkRemainsDeferred() {
        assertFalse(AenderDeferredReleasePolicy.shouldRelease(false, true, true));
    }

    @Test
    void lastWatcherMakesReleasedChunkEligible() {
        assertTrue(AenderDeferredReleasePolicy.shouldRelease(false, false, true));
    }

    @Test
    void overlappingStabilizerStillProtectsChunk() {
        assertFalse(AenderDeferredReleasePolicy.shouldRelease(true, false, true));
    }

    @Test
    void ordinaryCurrentChunkIsNotReleased() {
        assertFalse(AenderDeferredReleasePolicy.shouldRelease(false, false, false));
    }
}
