package cz.xefensor.retold.ambient;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RetoldHorizonScheduleTest {
    private static final UUID PLAYER_ID =
            UUID.fromString("11111111-2222-3333-4444-555555555555");

    @Test
    void scheduleWaitsUntilDueAndReschedulesFromCurrentTime() {
        RetoldHorizonSchedule schedule = new RetoldHorizonSchedule();

        assertTrue(schedule.scheduleIfAbsent(PLAYER_ID, 1_000L, 240_000L));
        assertFalse(schedule.scheduleIfAbsent(PLAYER_ID, 2_000L, 480_000L));
        assertFalse(schedule.isDue(PLAYER_ID, 240_999L));
        assertTrue(schedule.isDue(PLAYER_ID, 241_000L));

        schedule.reschedule(PLAYER_ID, 250_000L, 480_000L);

        assertEquals(730_000L, schedule.nextCue(PLAYER_ID));
        assertFalse(schedule.isDue(PLAYER_ID, 729_999L));
        assertTrue(schedule.isDue(PLAYER_ID, 730_000L));
    }

    @Test
    void dayIntervalsConvertToExactWorldTicks() {
        assertEquals(48_000L, RetoldHorizonSchedule.intervalTicksForDays(2));
        assertEquals(168_000L, RetoldHorizonSchedule.intervalTicksForDays(7));
        assertEquals(120_000L, RetoldHorizonSchedule.intervalTicksForDays(5));
        assertEquals(480_000L, RetoldHorizonSchedule.intervalTicksForDays(20));
        assertThrows(
                IllegalArgumentException.class,
                () -> RetoldHorizonSchedule.intervalTicksForDays(0)
        );
    }

    @Test
    void oldLongDelayIsCappedWithoutPostponingSoonerCue() {
        RetoldHorizonSchedule schedule = new RetoldHorizonSchedule();
        schedule.scheduleIfAbsent(PLAYER_ID, 1_000L, 2_400_000L);

        assertTrue(schedule.capDelay(PLAYER_ID, 2_000L, 480_000L));
        assertEquals(482_000L, schedule.nextCue(PLAYER_ID));
        assertFalse(schedule.capDelay(PLAYER_ID, 3_000L, 480_000L));
        assertEquals(482_000L, schedule.nextCue(PLAYER_ID));
    }

    @Test
    void serializedStateRoundTripPreservesNextCue() {
        RetoldHorizonSchedule schedule = new RetoldHorizonSchedule();
        schedule.scheduleIfAbsent(PLAYER_ID, 1_000L, 240_000L);

        RetoldHorizonSchedule restored = RetoldHorizonSchedule.fromSerializedState(
                schedule.serialize()
        );

        assertEquals(241_000L, restored.nextCue(PLAYER_ID));
    }

    @Test
    void invalidStateIsRejected() {
        RetoldHorizonSchedule.PlayerEntry entry =
                new RetoldHorizonSchedule.PlayerEntry(PLAYER_ID, 240_000L);

        assertThrows(
                IllegalArgumentException.class,
                () -> RetoldHorizonSchedule.fromSerializedState(
                        new RetoldHorizonSchedule.SerializedState(0, List.of())
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> RetoldHorizonSchedule.fromSerializedState(
                        new RetoldHorizonSchedule.SerializedState(1, List.of(entry, entry))
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new RetoldHorizonSchedule().scheduleIfAbsent(PLAYER_ID, 0L, 0L)
        );
    }
}
