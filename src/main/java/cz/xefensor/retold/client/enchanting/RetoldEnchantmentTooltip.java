package cz.xefensor.retold.client.enchanting;

import cz.xefensor.retold.enchanting.RetoldEnchantmentGlyphVocabulary;
import cz.xefensor.retold.enchanting.RetoldEnchantmentSpellDefinition;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

/** Replaces mapped vanilla enchantment-name lines with knowledge-aware SGA presentation. */
public final class RetoldEnchantmentTooltip {
    private static final FontDescription.Resource SGA_FONT =
            new FontDescription.Resource(Identifier.withDefaultNamespace("alt"));
    private static final Style SGA_STYLE = Style.EMPTY
            .withColor(ChatFormatting.DARK_PURPLE)
            .withFont(SGA_FONT);
    private static final Style LEVEL_STYLE = Style.EMPTY
            .withColor(ChatFormatting.DARK_PURPLE)
            .withFont(FontDescription.DEFAULT);

    private RetoldEnchantmentTooltip() {
    }

    public static void onItemTooltip(ItemTooltipEvent event) {
        rewriteTooltip(event.getItemStack(), event.getToolTip());
    }

    public static void rewriteTooltip(ItemStack itemStack, List<Component> tooltip) {
        ItemEnchantments enchantments =
                EnchantmentHelper.getEnchantmentsForCrafting(itemStack);

        for (var entry : enchantments.entrySet()) {
            Holder<Enchantment> enchantment = entry.getKey();
            int level = entry.getIntValue();
            Component vanillaLine = Enchantment.getFullname(enchantment, level);
            int vanillaLineIndex = tooltip.indexOf(vanillaLine);
            if (vanillaLineIndex < 0) {
                continue;
            }

            RetoldEnchantmentSpellDefinition definition = enchantment.unwrapKey()
                    .flatMap(key -> RetoldClientEnchantmentCatalog.byEnchantment(
                            key.identifier()
                    ))
                    .orElse(null);
            if (definition == null) {
                continue;
            }

            boolean known = enchantment.unwrapKey()
                    .map(key -> RetoldClientEnchantmentKnowledge.isKnown(
                            key.identifier()
                    ))
                    .orElse(false);

            if (known) {
                tooltip.add(
                        vanillaLineIndex + 1,
                        glyphLine(definition, enchantment, level, false)
                );
            } else {
                tooltip.set(
                        vanillaLineIndex,
                        glyphLine(definition, enchantment, level, true)
                );
            }
        }
    }

    private static Component glyphLine(
            RetoldEnchantmentSpellDefinition definition,
            Holder<Enchantment> enchantment,
            int level,
            boolean showLevel
    ) {
        MutableComponent result = Component.literal(
                RetoldEnchantmentGlyphVocabulary.glyphWord(definition.word())
        ).setStyle(SGA_STYLE);

        if (showLevel && (level != 1 || enchantment.value().getMaxLevel() != 1)) {
            result.append(Component.literal(" ").setStyle(LEVEL_STYLE));
            result.append(
                    Component.translatable("enchantment.level." + level)
                            .setStyle(LEVEL_STYLE)
            );
        }

        return result;
    }
}
