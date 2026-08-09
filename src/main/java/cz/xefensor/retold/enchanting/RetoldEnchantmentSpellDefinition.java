package cz.xefensor.retold.enchanting;

import java.util.Objects;

/** Datapack definition connecting one enchantment to its semantic spell word. */
public record RetoldEnchantmentSpellDefinition(
        String enchantment,
        RetoldEnchantmentWord word
) {
    public RetoldEnchantmentSpellDefinition {
        Objects.requireNonNull(enchantment, "enchantment");
        Objects.requireNonNull(word, "word");

        if (enchantment.isBlank()) {
            throw new IllegalArgumentException("Enchantment id must not be blank");
        }
    }
}
