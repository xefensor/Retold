package cz.xefensor.retold.enchanting;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Fixed semantic concepts assigned to Minecraft's built-in SGA A-Z glyphs. */
public final class RetoldEnchantmentGlyphVocabulary {
    private static final List<String> CONCEPTS = List.of(
            "retold:armor",
            "retold:arthropod",
            "retold:bind",
            "retold:bow",
            "retold:damage",
            "retold:explosion",
            "retold:fall",
            "retold:fire",
            "retold:fishing",
            "retold:general",
            "retold:ice",
            "retold:item",
            "retold:more",
            "retold:move",
            "retold:multiple",
            "retold:projectile",
            "retold:protect",
            "retold:push",
            "retold:restore",
            "retold:self",
            "retold:tool",
            "retold:undead",
            "retold:water",
            "retold:weapon",
            "retold:work",
            "retold:yield"
    );
    private static final Map<String, Character> GLYPHS = createGlyphMap();

    private RetoldEnchantmentGlyphVocabulary() {
    }

    public static List<String> concepts() {
        return CONCEPTS;
    }

    public static Optional<Character> glyphFor(String concept) {
        return Optional.ofNullable(GLYPHS.get(concept));
    }

    public static String glyphWord(RetoldEnchantmentWord word) {
        return new String(new char[] {
                requireGlyph(word.domain()),
                requireGlyph(word.effect()),
                requireGlyph(word.modifier())
        });
    }

    static void validate(RetoldEnchantmentWord word) {
        requireGlyph(word.domain());
        requireGlyph(word.effect());
        requireGlyph(word.modifier());
    }

    private static char requireGlyph(String concept) {
        Character glyph = GLYPHS.get(concept);
        if (glyph == null) {
            throw new IllegalArgumentException(
                    "Unknown enchantment glyph concept " + concept
            );
        }
        return glyph;
    }

    private static Map<String, Character> createGlyphMap() {
        Map<String, Character> glyphs = new HashMap<>();
        for (int index = 0; index < CONCEPTS.size(); index++) {
            glyphs.put(CONCEPTS.get(index), (char) ('A' + index));
        }
        return Map.copyOf(glyphs);
    }
}
