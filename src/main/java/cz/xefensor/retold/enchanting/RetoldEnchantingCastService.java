package cz.xefensor.retold.enchanting;

import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.neoforge.common.CommonHooks;

import java.util.List;

/** Server-authoritative validation and commit owner for deterministic enchanting casts. */
public final class RetoldEnchantingCastService {
    private RetoldEnchantingCastService() {
    }

    public static Result tryCast(
            ServerPlayer player,
            ItemStack target,
            ItemStack lapis,
            RetoldEnchantmentWord word,
            int requestedLevel
    ) {
        RetoldEnchantmentSpellDefinition definition =
                RetoldEnchantmentCatalog.byWord(word).orElse(null);
        if (definition == null) {
            return Result.failure(Status.INVALID_WORD);
        }

        Holder.Reference<Enchantment> enchantment = resolveEnchantment(player, definition);
        if (enchantment == null || !enchantment.is(EnchantmentTags.IN_ENCHANTING_TABLE)) {
            return Result.failure(Status.UNAVAILABLE_ENCHANTMENT);
        }
        if (requestedLevel <= 0 || requestedLevel > enchantment.value().getMaxLevel()) {
            return Result.failure(Status.INVALID_LEVEL);
        }
        boolean plainBook = target.is(Items.BOOK);
        if (target.isEmpty()
                || (!plainBook && !target.supportsEnchantment(enchantment))) {
            return Result.failure(Status.INCOMPATIBLE_ITEM);
        }

        ItemEnchantments existingEnchantments =
                EnchantmentHelper.getEnchantmentsForCrafting(target);
        int existingLevel = existingEnchantments.getLevel(enchantment);
        if (existingLevel >= requestedLevel) {
            return Result.failure(Status.NO_UPGRADE);
        }
        for (var entry : existingEnchantments.entrySet()) {
            Holder<Enchantment> existing = entry.getKey();
            if (!existing.equals(enchantment)
                    && !Enchantment.areCompatible(existing, enchantment)) {
                return Result.failure(Status.CONFLICTING_ENCHANTMENT);
            }
        }

        int experienceCost = RetoldEnchantingCosts.experienceLevelCost(requestedLevel);
        boolean infiniteMaterials = player.hasInfiniteMaterials();
        if (!infiniteMaterials
                && (!lapis.is(Items.LAPIS_LAZULI)
                || lapis.getCount() < RetoldEnchantingCosts.LAPIS_PER_CAST)) {
            return Result.failure(Status.INSUFFICIENT_LAPIS);
        }
        if (!infiniteMaterials && player.experienceLevel < experienceCost) {
            return Result.failure(Status.INSUFFICIENT_EXPERIENCE);
        }

        EnchantmentInstance appliedEnchantment =
                new EnchantmentInstance(enchantment, requestedLevel);
        ItemStack output = target.getItem().applyEnchantments(
                target.copy(),
                List.of(appliedEnchantment)
        );
        if (output.isEmpty()
                || EnchantmentHelper.getEnchantmentsForCrafting(output)
                .getLevel(enchantment) < requestedLevel) {
            return Result.failure(Status.APPLICATION_FAILED);
        }

        if (!infiniteMaterials) {
            player.onEnchantmentPerformed(output, experienceCost);
            lapis.consume(RetoldEnchantingCosts.LAPIS_PER_CAST, player);
        }
        CommonHooks.onPlayerEnchantItem(player, output, List.of(appliedEnchantment));
        player.awardStat(Stats.ENCHANT_ITEM);
        CriteriaTriggers.ENCHANTED_ITEM.trigger(player, output, experienceCost);
        RetoldEnchantmentKnowledge.markKnown(
                player,
                Identifier.parse(definition.enchantment())
        );

        return new Result(Status.SUCCESS, output);
    }

    private static Holder.Reference<Enchantment> resolveEnchantment(
            ServerPlayer player,
            RetoldEnchantmentSpellDefinition definition
    ) {
        Identifier enchantmentId = Identifier.parse(definition.enchantment());
        ResourceKey<Enchantment> enchantmentKey = ResourceKey.create(
                Registries.ENCHANTMENT,
                enchantmentId
        );
        return player.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .get(enchantmentKey)
                .orElse(null);
    }

    public enum Status {
        SUCCESS,
        INVALID_WORD,
        UNAVAILABLE_ENCHANTMENT,
        INVALID_LEVEL,
        INCOMPATIBLE_ITEM,
        CONFLICTING_ENCHANTMENT,
        NO_UPGRADE,
        INSUFFICIENT_LAPIS,
        INSUFFICIENT_EXPERIENCE,
        APPLICATION_FAILED
    }

    public record Result(Status status, ItemStack output) {
        private static Result failure(Status status) {
            return new Result(status, ItemStack.EMPTY);
        }

        public boolean success() {
            return status == Status.SUCCESS;
        }
    }
}
