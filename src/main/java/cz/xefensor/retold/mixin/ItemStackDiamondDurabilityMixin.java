package cz.xefensor.retold.mixin;

import cz.xefensor.retold.progression.RetoldDiamondDurability;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackDiamondDurabilityMixin {
    @Inject(method = "getMaxDamage", at = @At("RETURN"), cancellable = true)
    private void retold$applyDynamicDiamondDurability(
            CallbackInfoReturnable<Integer> callbackInfo
    ) {
        ItemStack stack = (ItemStack) (Object) this;
        callbackInfo.setReturnValue(
                RetoldDiamondDurability.effectiveMaxDamage(
                        stack,
                        callbackInfo.getReturnValue()
                )
        );
    }
}
