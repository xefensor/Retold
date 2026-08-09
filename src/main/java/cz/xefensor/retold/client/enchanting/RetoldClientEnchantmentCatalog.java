package cz.xefensor.retold.client.enchanting;

import cz.xefensor.retold.enchanting.RetoldEnchantmentCatalogSnapshot;
import cz.xefensor.retold.enchanting.RetoldEnchantmentSpellDefinition;
import cz.xefensor.retold.enchanting.RetoldEnchantmentWord;
import net.minecraft.resources.Identifier;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Read-only client spell catalog used by future enchanting screens and tooltips. */
public final class RetoldClientEnchantmentCatalog {
    private static volatile RetoldEnchantmentCatalogSnapshot snapshot =
            RetoldEnchantmentCatalogSnapshot.empty();

    private RetoldClientEnchantmentCatalog() {
    }

    public static Optional<RetoldEnchantmentSpellDefinition> byEnchantment(
            Identifier enchantment
    ) {
        return Optional.ofNullable(snapshot.byEnchantment().get(enchantment.toString()));
    }

    public static Optional<RetoldEnchantmentSpellDefinition> byWord(
            RetoldEnchantmentWord word
    ) {
        return Optional.ofNullable(snapshot.byWord().get(word));
    }

    public static List<RetoldEnchantmentSpellDefinition> definitions() {
        return snapshot.definitions();
    }

    public static void replace(Collection<RetoldEnchantmentSpellDefinition> definitions) {
        snapshot = RetoldEnchantmentCatalogSnapshot.from(definitions);
    }

    public static void clear() {
        snapshot = RetoldEnchantmentCatalogSnapshot.empty();
    }
}
