package cz.xefensor.retold.mixin.client;

import net.minecraft.client.gui.components.toasts.RecipeToast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RecipeToast.class)
public abstract class NoRecipeToastMixin {
    @Inject(method = "addOrUpdate", at = @At("HEAD"), cancellable = true)
    private static void retold$hideRecipeUnlockToast(
            ToastManager toastManager,
            RecipeDisplay recipeDisplay,
            CallbackInfo ci
    ) {
        ci.cancel();
    }
}