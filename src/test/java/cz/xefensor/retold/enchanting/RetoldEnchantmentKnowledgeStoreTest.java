package cz.xefensor.retold.enchanting;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RetoldEnchantmentKnowledgeStoreTest {
    private static final UUID FIRST_PLAYER =
            UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID SECOND_PLAYER =
            UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private static final String SHARPNESS = "minecraft:sharpness";
    private static final String SMITE = "minecraft:smite";

    @Test
    void markingKnowledgeIsIdempotentAndPlayerScoped() {
        RetoldEnchantmentKnowledgeStore store = new RetoldEnchantmentKnowledgeStore();

        assertTrue(store.markKnown(FIRST_PLAYER, SHARPNESS));
        assertFalse(store.markKnown(FIRST_PLAYER, SHARPNESS));

        assertTrue(store.hasKnown(FIRST_PLAYER, SHARPNESS));
        assertFalse(store.hasKnown(FIRST_PLAYER, SMITE));
        assertFalse(store.hasKnown(SECOND_PLAYER, SHARPNESS));
        assertEquals(List.of(SHARPNESS), store.knownEnchantments(FIRST_PLAYER).stream().toList());
    }

    @Test
    void batchLearningRecordsEveryTransferredSpellAsOneChange() {
        RetoldEnchantmentKnowledgeStore store = new RetoldEnchantmentKnowledgeStore();

        assertTrue(store.markKnown(FIRST_PLAYER, List.of(SHARPNESS, SMITE)));
        assertFalse(store.markKnown(FIRST_PLAYER, List.of(SHARPNESS, SMITE)));
        assertEquals(
                Set.of(SHARPNESS, SMITE),
                store.knownEnchantments(FIRST_PLAYER)
        );
    }

    @Test
    void serializedStateRoundTripPreservesSortedKnowledge() {
        RetoldEnchantmentKnowledgeStore store = new RetoldEnchantmentKnowledgeStore();
        store.markKnown(FIRST_PLAYER, SMITE);
        store.markKnown(FIRST_PLAYER, SHARPNESS);
        store.markKnown(SECOND_PLAYER, SHARPNESS);

        RetoldEnchantmentKnowledgeStore.SerializedState serialized = store.serialize();
        RetoldEnchantmentKnowledgeStore reloaded =
                RetoldEnchantmentKnowledgeStore.fromSerializedState(serialized);

        assertEquals(RetoldEnchantmentKnowledgeStore.SAVE_VERSION, serialized.version());
        assertEquals(List.of(SHARPNESS, SMITE), serialized.players().getFirst().enchantments());
        assertTrue(reloaded.hasKnown(FIRST_PLAYER, SHARPNESS));
        assertTrue(reloaded.hasKnown(FIRST_PLAYER, SMITE));
        assertTrue(reloaded.hasKnown(SECOND_PLAYER, SHARPNESS));
    }

    @Test
    void unsupportedVersionsAndDuplicateEntriesAreRejected() {
        RetoldEnchantmentKnowledgeStore.PlayerKnowledge duplicateSpells =
                new RetoldEnchantmentKnowledgeStore.PlayerKnowledge(
                        FIRST_PLAYER,
                        List.of(SHARPNESS, SHARPNESS)
                );
        RetoldEnchantmentKnowledgeStore.PlayerKnowledge first =
                new RetoldEnchantmentKnowledgeStore.PlayerKnowledge(FIRST_PLAYER, List.of(SHARPNESS));
        RetoldEnchantmentKnowledgeStore.PlayerKnowledge duplicatePlayer =
                new RetoldEnchantmentKnowledgeStore.PlayerKnowledge(FIRST_PLAYER, List.of(SMITE));

        assertThrows(
                IllegalArgumentException.class,
                () -> RetoldEnchantmentKnowledgeStore.fromSerializedState(
                        new RetoldEnchantmentKnowledgeStore.SerializedState(0, List.of())
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> RetoldEnchantmentKnowledgeStore.fromSerializedState(
                        new RetoldEnchantmentKnowledgeStore.SerializedState(
                                RetoldEnchantmentKnowledgeStore.SAVE_VERSION,
                                List.of(duplicateSpells)
                        )
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> RetoldEnchantmentKnowledgeStore.fromSerializedState(
                        new RetoldEnchantmentKnowledgeStore.SerializedState(
                                RetoldEnchantmentKnowledgeStore.SAVE_VERSION,
                                List.of(first, duplicatePlayer)
                        )
                )
        );
    }
}
