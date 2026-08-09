package cz.xefensor.retold.client.enchanting;

import net.minecraft.resources.Identifier;

import java.util.Collection;
import java.util.Set;

/** Read-only client snapshot used by future enchanting screens and tooltips. */
public final class RetoldClientEnchantmentKnowledge {
    private static Set<Identifier> knownEnchantments = Set.of();

    private RetoldClientEnchantmentKnowledge() {
    }

    public static boolean isKnown(Identifier enchantment) {
        return knownEnchantments.contains(enchantment);
    }

    public static Set<Identifier> snapshot() {
        return knownEnchantments;
    }

    public static void replace(Collection<Identifier> enchantments) {
        knownEnchantments = Set.copyOf(enchantments);
    }

    public static void clear() {
        knownEnchantments = Set.of();
    }
}
