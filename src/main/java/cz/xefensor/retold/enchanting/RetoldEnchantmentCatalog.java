package cz.xefensor.retold.enchanting;

import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Atomically replaced server spell catalog loaded from datapacks. */
public final class RetoldEnchantmentCatalog {
    private static volatile RetoldEnchantmentCatalogSnapshot snapshot =
            RetoldEnchantmentCatalogSnapshot.empty();

    private RetoldEnchantmentCatalog() {
    }

    public static Optional<RetoldEnchantmentSpellDefinition> byEnchantment(Identifier enchantment) {
        return Optional.ofNullable(snapshot.byEnchantment().get(enchantment.toString()));
    }

    public static Optional<RetoldEnchantmentSpellDefinition> byWord(RetoldEnchantmentWord word) {
        return Optional.ofNullable(snapshot.byWord().get(word));
    }

    public static int size() {
        return snapshot.byEnchantment().size();
    }

    public static List<RetoldEnchantmentSpellDefinition> definitions() {
        return snapshot.definitions();
    }

    static int replace(Map<Identifier, RetoldEnchantmentSpellDefinition> loadedDefinitions) {
        RetoldEnchantmentCatalogSnapshot replacement =
                RetoldEnchantmentCatalogSnapshot.from(loadedDefinitions.values());
        snapshot = replacement;
        return replacement.byEnchantment().size();
    }
}
