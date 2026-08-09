package cz.xefensor.retold.client.enchanting;

import cz.xefensor.retold.enchanting.RetoldEnchantmentGlyphVocabulary;
import cz.xefensor.retold.enchanting.RetoldEnchantmentWord;

import java.util.Arrays;
import java.util.Optional;
import java.util.OptionalInt;

/** Client-local editing state for the three-glyph inscription and requested level. */
public final class RetoldEnchantingScreenState {
    private static final int WORD_LENGTH = 3;
    private static final int MIN_LEVEL = 1;
    private static final int MAX_LEVEL = 5;
    private static final int EMPTY_GLYPH = -1;

    private final int[] glyphs = new int[WORD_LENGTH];
    private int level = MIN_LEVEL;

    public RetoldEnchantingScreenState() {
        Arrays.fill(this.glyphs, EMPTY_GLYPH);
    }

    public void appendGlyph(int glyphIndex) {
        validateGlyphIndex(glyphIndex);
        for (int index = 0; index < this.glyphs.length; index++) {
            if (this.glyphs[index] == EMPTY_GLYPH) {
                this.glyphs[index] = glyphIndex;
                return;
            }
        }
    }

    public void backspace() {
        for (int index = this.glyphs.length - 1; index >= 0; index--) {
            if (this.glyphs[index] != EMPTY_GLYPH) {
                this.glyphs[index] = EMPTY_GLYPH;
                return;
            }
        }
    }

    public void clearFrom(int position) {
        if (position < 0 || position >= this.glyphs.length) {
            throw new IndexOutOfBoundsException(position);
        }
        Arrays.fill(this.glyphs, position, this.glyphs.length, EMPTY_GLYPH);
    }

    public void clear() {
        Arrays.fill(this.glyphs, EMPTY_GLYPH);
    }

    public void selectWord(RetoldEnchantmentWord word) {
        this.glyphs[0] = conceptIndex(word.domain());
        this.glyphs[1] = conceptIndex(word.effect());
        this.glyphs[2] = conceptIndex(word.modifier());
    }

    public OptionalInt glyph(int position) {
        if (position < 0 || position >= this.glyphs.length) {
            throw new IndexOutOfBoundsException(position);
        }
        int glyph = this.glyphs[position];
        return glyph == EMPTY_GLYPH ? OptionalInt.empty() : OptionalInt.of(glyph);
    }

    public Optional<RetoldEnchantmentWord> word() {
        for (int glyph : this.glyphs) {
            if (glyph == EMPTY_GLYPH) {
                return Optional.empty();
            }
        }

        var concepts = RetoldEnchantmentGlyphVocabulary.concepts();
        return Optional.of(new RetoldEnchantmentWord(
                concepts.get(this.glyphs[0]),
                concepts.get(this.glyphs[1]),
                concepts.get(this.glyphs[2])
        ));
    }

    public int level() {
        return this.level;
    }

    public void setLevel(int level) {
        if (level < MIN_LEVEL || level > MAX_LEVEL) {
            throw new IllegalArgumentException("Enchanting level must be between 1 and 5");
        }
        this.level = level;
    }

    private static int conceptIndex(String concept) {
        int index = RetoldEnchantmentGlyphVocabulary.concepts().indexOf(concept);
        if (index < 0) {
            throw new IllegalArgumentException("Unknown enchanting glyph concept " + concept);
        }
        return index;
    }

    private static void validateGlyphIndex(int glyphIndex) {
        if (glyphIndex < 0
                || glyphIndex >= RetoldEnchantmentGlyphVocabulary.concepts().size()) {
            throw new IllegalArgumentException("Glyph index out of range: " + glyphIndex);
        }
    }
}
