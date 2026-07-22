package cz.xefensor.retold.aender.stability;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AenderActiveRegionTrackerTest {
    @Test
    void onlyRegionsNoLongerWatchedBecomeEligible() {
        AenderActiveRegionTracker tracker = new AenderActiveRegionTracker();

        assertTrue(tracker.update(Set.of(1L, 2L), true).isEmpty());
        assertEquals(Set.of(1L), tracker.update(Set.of(2L, 3L), true));
        assertTrue(tracker.update(Set.of(2L, 3L), true).isEmpty());
    }

    @Test
    void finalDisconnectDoesNotMakeRegionsEligible() {
        AenderActiveRegionTracker tracker = new AenderActiveRegionTracker();

        tracker.update(Set.of(1L, 2L), true);
        assertTrue(tracker.update(Set.of(), false).isEmpty());
        assertTrue(tracker.update(Set.of(7L), true).isEmpty());
    }

    @Test
    void regionSharedWithAnotherPlayerRemainsActive() {
        AenderActiveRegionTracker tracker = new AenderActiveRegionTracker();

        tracker.update(Set.of(10L, 11L, 12L), true);
        assertEquals(Set.of(10L), tracker.update(Set.of(11L, 12L), true));
    }

    @Test
    void transientEmptyTrackingFrameDoesNotAbandonRegions() {
        AenderActiveRegionTracker tracker = new AenderActiveRegionTracker();

        tracker.update(Set.of(20L, 21L), true);
        assertTrue(tracker.update(Set.of(), true).isEmpty());
        assertEquals(Set.of(20L), tracker.update(Set.of(21L), true));
    }
}
