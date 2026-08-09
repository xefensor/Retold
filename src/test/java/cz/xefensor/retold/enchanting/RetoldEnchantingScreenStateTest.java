package cz.xefensor.retold.enchanting;

import cz.xefensor.retold.client.enchanting.RetoldEnchantingScreenState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RetoldEnchantingScreenStateTest {
    @Test
    void assemblesAndEditsAThreeGlyphWord() {
        RetoldEnchantingScreenState state = new RetoldEnchantingScreenState();

        state.appendGlyph(23);
        state.appendGlyph(4);
        state.appendGlyph(9);

        assertEquals(
                new RetoldEnchantmentWord(
                        "retold:weapon",
                        "retold:damage",
                        "retold:general"
                ),
                state.word().orElseThrow()
        );

        state.backspace();
        assertFalse(state.word().isPresent());
        state.appendGlyph(21);
        assertEquals("retold:undead", state.word().orElseThrow().modifier());

        state.clearFrom(1);
        assertTrue(state.glyph(0).isPresent());
        assertTrue(state.glyph(1).isEmpty());
        assertTrue(state.glyph(2).isEmpty());

        state.clear();
        assertTrue(state.glyph(0).isEmpty());
        assertFalse(state.word().isPresent());
    }

    @Test
    void knownSpellSelectionRefillsTheWholeWord() {
        RetoldEnchantingScreenState state = new RetoldEnchantingScreenState();
        RetoldEnchantmentWord word = new RetoldEnchantmentWord(
                "retold:armor",
                "retold:protect",
                "retold:fire"
        );

        state.selectWord(word);

        assertEquals(word, state.word().orElseThrow());
    }

    @Test
    void strengthIsLimitedToTheFiveButtonRange() {
        RetoldEnchantingScreenState state = new RetoldEnchantingScreenState();

        state.setLevel(5);

        assertEquals(5, state.level());
        assertThrows(IllegalArgumentException.class, () -> state.setLevel(0));
        assertThrows(IllegalArgumentException.class, () -> state.setLevel(6));
    }
}
