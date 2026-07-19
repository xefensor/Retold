package cz.xefensor.retold.territory;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RetoldTerritoryReputationStoreTest {
    private static final String TERRITORY = "ILLAGERS|minecraft:overworld|32|48";
    private static final UUID INTRUDER = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @Test
    void serializedStateRoundTripPreservesEveryEntryField() {
        RetoldTerritoryReputationStore store = new RetoldTerritoryReputationStore();
        RetoldTerritoryReputationStore.SerializedEntry expected = entry(73);

        store.importLegacyEntries(List.of(expected));
        RetoldTerritoryReputationStore.SerializedState serialized = store.serialize();
        RetoldTerritoryReputationStore reloaded =
                RetoldTerritoryReputationStore.fromSerializedState(serialized);

        assertEquals(RetoldTerritoryConstants.REPUTATION_SAVE_VERSION, serialized.version());
        assertEquals(expected, reloaded.snapshot(TERRITORY, INTRUDER));
        assertTrue(reloaded.isLegacyJsonMigrated());
        assertTrue(reloaded.isLegacyMigrationConfirmedOnLoad());
    }

    @Test
    void independentInstancesDoNotShareEntriesOrRuntimeClocks() {
        RetoldTerritoryReputationStore firstWorld = new RetoldTerritoryReputationStore();
        RetoldTerritoryReputationStore secondWorld = new RetoldTerritoryReputationStore();

        firstWorld.addSuspicion(TERRITORY, INTRUDER, 10, 1_000L);
        firstWorld.markSeen(TERRITORY, INTRUDER, 1_000L);
        firstWorld.tickDecay(1_000L);

        assertEquals(10, firstWorld.getSuspicion(TERRITORY, INTRUDER));
        assertEquals(0, secondWorld.getSuspicion(TERRITORY, INTRUDER));

        secondWorld.addSuspicion(TERRITORY, INTRUDER, 4, 0L);
        secondWorld.tickDecay(RetoldTerritoryConstants.REPUTATION_SEEN_DECAY_BLOCK_TICKS + 1L);

        assertEquals(3, secondWorld.getSuspicion(TERRITORY, INTRUDER));
        assertEquals(10, firstWorld.getSuspicion(TERRITORY, INTRUDER));

        assertTrue(firstWorld.shouldRequestSave(5_000L, true));
        assertFalse(firstWorld.shouldRequestSave(5_001L, true));
        assertTrue(secondWorld.shouldRequestSave(0L, true));
    }

    @Test
    void decayKeepsExistingCadenceAndRemovesExhaustedEntries() {
        RetoldTerritoryReputationStore store = new RetoldTerritoryReputationStore();

        store.addSuspicion(TERRITORY, INTRUDER, 2, 0L);
        long firstDecayAt = RetoldTerritoryConstants.REPUTATION_SEEN_DECAY_BLOCK_TICKS + 1L;
        store.tickDecay(firstDecayAt);
        assertEquals(1, store.getSuspicion(TERRITORY, INTRUDER));

        store.tickDecay(firstDecayAt + RetoldTerritoryConstants.REPUTATION_DECAY_INTERVAL_TICKS - 1L);
        assertEquals(1, store.getSuspicion(TERRITORY, INTRUDER));

        store.tickDecay(firstDecayAt + RetoldTerritoryConstants.REPUTATION_DECAY_INTERVAL_TICKS);
        assertEquals(0, store.getSuspicion(TERRITORY, INTRUDER));
        assertNull(store.snapshot(TERRITORY, INTRUDER));
    }

    @Test
    void missingAndUnsupportedVersionsAreRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> RetoldTerritoryReputationStore.validateSerializedState(
                        new RetoldTerritoryReputationStore.SerializedState(0, false, List.of())
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> RetoldTerritoryReputationStore.validateSerializedState(
                        new RetoldTerritoryReputationStore.SerializedState(
                                RetoldTerritoryConstants.REPUTATION_SAVE_VERSION + 1,
                                false,
                                List.of()
                        )
                )
        );
    }

    @Test
    void corruptOrDuplicateEntriesAreRejectedAsAWhole() {
        RetoldTerritoryReputationStore.SerializedEntry corrupt = new RetoldTerritoryReputationStore.SerializedEntry(
                "",
                INTRUDER,
                0,
                0L,
                0L,
                0L,
                0L,
                0L,
                0L
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> RetoldTerritoryReputationStore.fromSerializedState(
                        new RetoldTerritoryReputationStore.SerializedState(
                                RetoldTerritoryConstants.REPUTATION_SAVE_VERSION,
                                false,
                                List.of(corrupt)
                        )
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> RetoldTerritoryReputationStore.fromSerializedState(
                        new RetoldTerritoryReputationStore.SerializedState(
                                RetoldTerritoryConstants.REPUTATION_SAVE_VERSION,
                                false,
                                List.of(entry(5), entry(6))
                        )
                )
        );
    }

    @Test
    void legacyImportIsOnlyConfirmedAfterSerializedStateReload() {
        RetoldTerritoryReputationStore firstLoad = new RetoldTerritoryReputationStore();
        firstLoad.importLegacyEntries(List.of(entry(73)));

        assertTrue(firstLoad.isLegacyJsonMigrated());
        assertFalse(firstLoad.isLegacyMigrationConfirmedOnLoad());

        RetoldTerritoryReputationStore reloaded =
                RetoldTerritoryReputationStore.fromSerializedState(firstLoad.serialize());

        assertTrue(reloaded.isLegacyMigrationConfirmedOnLoad());
        assertEquals(73, reloaded.getSuspicion(TERRITORY, INTRUDER));
    }

    private static RetoldTerritoryReputationStore.SerializedEntry entry(int suspicion) {
        return new RetoldTerritoryReputationStore.SerializedEntry(
                TERRITORY,
                INTRUDER,
                suspicion,
                11L,
                22L,
                33L,
                44L,
                55L,
                66L
        );
    }
}
