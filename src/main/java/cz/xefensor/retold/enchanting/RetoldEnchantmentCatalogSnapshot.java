package cz.xefensor.retold.enchanting;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Immutable, validated spell-catalog snapshot shared by server and client views. */
public final class RetoldEnchantmentCatalogSnapshot {
    private final Map<String, RetoldEnchantmentSpellDefinition> byEnchantment;
    private final Map<RetoldEnchantmentWord, RetoldEnchantmentSpellDefinition> byWord;
    private final List<RetoldEnchantmentSpellDefinition> definitions;

    private RetoldEnchantmentCatalogSnapshot(
            Map<String, RetoldEnchantmentSpellDefinition> byEnchantment,
            Map<RetoldEnchantmentWord, RetoldEnchantmentSpellDefinition> byWord,
            List<RetoldEnchantmentSpellDefinition> definitions
    ) {
        this.byEnchantment = byEnchantment;
        this.byWord = byWord;
        this.definitions = definitions;
    }

    public static RetoldEnchantmentCatalogSnapshot empty() {
        return new RetoldEnchantmentCatalogSnapshot(Map.of(), Map.of(), List.of());
    }

    public static RetoldEnchantmentCatalogSnapshot from(
            Collection<RetoldEnchantmentSpellDefinition> loadedDefinitions
    ) {
        Map<String, RetoldEnchantmentSpellDefinition> enchantments = new HashMap<>();
        Map<RetoldEnchantmentWord, RetoldEnchantmentSpellDefinition> words = new HashMap<>();

        for (RetoldEnchantmentSpellDefinition definition : loadedDefinitions) {
            RetoldEnchantmentGlyphVocabulary.validate(definition.word());

            RetoldEnchantmentSpellDefinition duplicateEnchantment = enchantments.put(
                    definition.enchantment(),
                    definition
            );
            if (duplicateEnchantment != null) {
                throw new IllegalArgumentException(
                        "Duplicate spell definition for enchantment " + definition.enchantment()
                );
            }

            RetoldEnchantmentSpellDefinition duplicateWord = words.put(definition.word(), definition);
            if (duplicateWord != null) {
                throw new IllegalArgumentException(
                        "Duplicate semantic spell word for enchantment " + definition.enchantment()
                );
            }
        }

        List<RetoldEnchantmentSpellDefinition> orderedDefinitions =
                enchantments.values().stream()
                        .sorted(Comparator.comparing(RetoldEnchantmentSpellDefinition::enchantment))
                        .toList();

        return new RetoldEnchantmentCatalogSnapshot(
                Map.copyOf(enchantments),
                Map.copyOf(words),
                orderedDefinitions
        );
    }

    public Map<String, RetoldEnchantmentSpellDefinition> byEnchantment() {
        return byEnchantment;
    }

    public Map<RetoldEnchantmentWord, RetoldEnchantmentSpellDefinition> byWord() {
        return byWord;
    }

    public List<RetoldEnchantmentSpellDefinition> definitions() {
        return definitions;
    }
}
