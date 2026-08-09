package cz.xefensor.retold.client.enchanting;

import cz.xefensor.retold.enchanting.RetoldEnchantmentSpellDefinition;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Presentation-only filtering for spells already known by the local player. */
public final class RetoldClientEnchantingOptions {
    private RetoldClientEnchantingOptions() {
    }

    public static List<RetoldEnchantmentSpellDefinition> availableKnownSpells(
            RegistryAccess registryAccess,
            Collection<RetoldEnchantmentSpellDefinition> definitions,
            Set<Identifier> knownEnchantments,
            ItemStack target
    ) {
        return definitions.stream()
                .filter(definition -> knownEnchantments.contains(
                        Identifier.parse(definition.enchantment())
                ))
                .filter(definition -> canOffer(registryAccess, definition, target))
                .sorted(Comparator.comparing(
                        RetoldEnchantmentSpellDefinition::enchantment
                ))
                .toList();
    }

    public static Optional<Holder.Reference<Enchantment>> resolve(
            RegistryAccess registryAccess,
            RetoldEnchantmentSpellDefinition definition
    ) {
        ResourceKey<Enchantment> key = ResourceKey.create(
                Registries.ENCHANTMENT,
                Identifier.parse(definition.enchantment())
        );
        return registryAccess.lookupOrThrow(Registries.ENCHANTMENT).get(key);
    }

    private static boolean canOffer(
            RegistryAccess registryAccess,
            RetoldEnchantmentSpellDefinition definition,
            ItemStack target
    ) {
        Holder.Reference<Enchantment> enchantment =
                resolve(registryAccess, definition).orElse(null);
        if (enchantment == null || !enchantment.is(EnchantmentTags.IN_ENCHANTING_TABLE)) {
            return false;
        }
        if (target.isEmpty()) {
            return true;
        }
        if (!target.is(Items.BOOK) && !target.supportsEnchantment(enchantment)) {
            return false;
        }

        ItemEnchantments existing = EnchantmentHelper.getEnchantmentsForCrafting(target);
        if (existing.getLevel(enchantment) >= enchantment.value().getMaxLevel()) {
            return false;
        }
        for (var entry : existing.entrySet()) {
            Holder<Enchantment> applied = entry.getKey();
            if (!applied.equals(enchantment)
                    && !Enchantment.areCompatible(applied, enchantment)) {
                return false;
            }
        }
        return true;
    }
}
