package cz.xefensor.retold.mixin;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.EnchantmentMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;

/** Disables vanilla random offers while retaining the table's synchronized inventory slots. */
@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuMixin {
    @Inject(method = "slotsChanged", at = @At("HEAD"), cancellable = true)
    private void retold$disableRandomOffers(Container container, CallbackInfo ci) {
        EnchantmentMenu menu = (EnchantmentMenu) (Object) this;
        Arrays.fill(menu.costs, 0);
        Arrays.fill(menu.enchantClue, -1);
        Arrays.fill(menu.levelClue, -1);
        ci.cancel();
    }

    @Inject(method = "clickMenuButton", at = @At("HEAD"), cancellable = true)
    private void retold$disableRandomButtons(
            Player player,
            int buttonId,
            CallbackInfoReturnable<Boolean> cir
    ) {
        cir.setReturnValue(false);
    }
}
