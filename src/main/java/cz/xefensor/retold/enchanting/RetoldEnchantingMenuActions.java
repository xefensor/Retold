package cz.xefensor.retold.enchanting;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;

/** Server owner for applying a validated cast to the active enchanting menu slots. */
public final class RetoldEnchantingMenuActions {
    private RetoldEnchantingMenuActions() {
    }

    public static boolean tryCast(
            ServerPlayer player,
            int containerId,
            RetoldEnchantmentWord word,
            int level
    ) {
        if (!(player.containerMenu instanceof EnchantmentMenu menu)
                || menu.containerId != containerId) {
            return false;
        }

        ItemStack target = menu.getSlot(0).getItem();
        ItemStack lapis = menu.getSlot(1).getItem();
        RetoldEnchantingCastService.Result result =
                RetoldEnchantingCastService.tryCast(player, target, lapis, word, level);
        if (!result.success()) {
            return false;
        }

        menu.getSlot(0).set(result.output());
        if (lapis.isEmpty()) {
            menu.getSlot(1).set(ItemStack.EMPTY);
        } else {
            menu.getSlot(1).setChanged();
        }
        menu.broadcastChanges();

        player.level().playSound(
                null,
                player.blockPosition(),
                SoundEvents.ENCHANTMENT_TABLE_USE,
                SoundSource.BLOCKS,
                1.0F,
                player.getRandom().nextFloat() * 0.1F + 0.9F
        );
        return true;
    }
}
