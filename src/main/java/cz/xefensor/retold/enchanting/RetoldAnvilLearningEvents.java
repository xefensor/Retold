package cz.xefensor.retold.enchanting;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.AnvilCraftEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class RetoldAnvilLearningEvents {
    private RetoldAnvilLearningEvents() {
    }

    @SubscribeEvent
    public static void onAnvilCrafted(AnvilCraftEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ItemStack book = event.getRight();
        if (!book.has(DataComponents.STORED_ENCHANTMENTS)) {
            return;
        }

        Set<Identifier> transferred = RetoldTransferredEnchantments.find(
                        enchantmentsById(event.getLeft()),
                        enchantmentsById(book),
                        enchantmentsById(event.getOutput())
                ).stream()
                .map(Identifier::parse)
                .collect(Collectors.toUnmodifiableSet());

        if (!transferred.isEmpty()) {
            RetoldEnchantmentKnowledge.markKnown(player, transferred);
        }
    }

    private static Map<String, Integer> enchantmentsById(ItemStack stack) {
        ItemEnchantments enchantments = EnchantmentHelper.getEnchantmentsForCrafting(stack);
        Map<String, Integer> byId = new HashMap<>();

        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
            entry.getKey().unwrapKey().ifPresent(key ->
                    byId.put(key.identifier().toString(), entry.getIntValue())
            );
        }

        return Map.copyOf(byId);
    }
}
