package cz.xefensor.retold.enchanting;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RetoldEnchantmentCatalogTest {
    private static final String SHARPNESS = "minecraft:sharpness";
    private static final String SMITE = "minecraft:smite";
    private static final RetoldEnchantmentWord SHARPNESS_WORD = word("general");
    private static final RetoldEnchantmentWord SMITE_WORD = word("undead");

    @Test
    void snapshotIndexesDefinitionsInBothDirections() {
        RetoldEnchantmentSpellDefinition sharpness = definition(SHARPNESS, SHARPNESS_WORD);
        RetoldEnchantmentSpellDefinition smite = definition(SMITE, SMITE_WORD);

        RetoldEnchantmentCatalogSnapshot snapshot =
                RetoldEnchantmentCatalogSnapshot.from(List.of(smite, sharpness));

        assertEquals(sharpness, snapshot.byEnchantment().get(SHARPNESS));
        assertEquals(smite, snapshot.byWord().get(SMITE_WORD));
        assertEquals(List.of(sharpness, smite), snapshot.definitions());
    }

    @Test
    void duplicateEnchantmentOrWordRejectsWholeSnapshot() {
        RetoldEnchantmentSpellDefinition sharpness = definition(SHARPNESS, SHARPNESS_WORD);
        RetoldEnchantmentSpellDefinition duplicateEnchantment = definition(SHARPNESS, SMITE_WORD);
        RetoldEnchantmentSpellDefinition duplicateWord = definition(SMITE, SHARPNESS_WORD);

        assertThrows(
                IllegalArgumentException.class,
                () -> RetoldEnchantmentCatalogSnapshot.from(
                        List.of(sharpness, duplicateEnchantment)
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> RetoldEnchantmentCatalogSnapshot.from(
                        List.of(sharpness, duplicateWord)
                )
        );
    }

    @Test
    void vocabularyUsesEverySgaGlyphOnceAndRejectsUnknownConcepts() {
        assertEquals(26, RetoldEnchantmentGlyphVocabulary.concepts().size());
        assertEquals(
                26,
                RetoldEnchantmentGlyphVocabulary.concepts().stream().distinct().count()
        );
        assertEquals(
                "XEJ",
                RetoldEnchantmentGlyphVocabulary.glyphWord(
                        new RetoldEnchantmentWord(
                                "retold:weapon",
                                "retold:damage",
                                "retold:general"
                        )
                )
        );
        assertTrue(
                RetoldEnchantmentGlyphVocabulary.concepts().stream()
                        .allMatch(concept ->
                                RetoldEnchantmentGlyphVocabulary.glyphFor(concept).isPresent()
                        )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> RetoldEnchantmentCatalogSnapshot.from(List.of(
                        definition(
                                SHARPNESS,
                                new RetoldEnchantmentWord(
                                        "retold:unknown",
                                        "retold:damage",
                                        "retold:general"
                                )
                        )
                ))
        );
    }

    private static RetoldEnchantmentSpellDefinition definition(
            String enchantment,
            RetoldEnchantmentWord word
    ) {
        return new RetoldEnchantmentSpellDefinition(enchantment, word);
    }

    private static RetoldEnchantmentWord word(String modifier) {
        return new RetoldEnchantmentWord(
                "retold:weapon",
                "retold:damage",
                "retold:" + modifier
        );
    }
}
