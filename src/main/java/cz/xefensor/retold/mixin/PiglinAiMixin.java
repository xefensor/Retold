package cz.xefensor.retold.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PiglinAi.class)
public abstract class PiglinAiMixin {
    @Inject(method = "isWearingSafeArmor", at = @At("HEAD"), cancellable = true)
    private static void retold$disableGoldArmorPiglinNeutrality(
            LivingEntity livingEntity,
            CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        callbackInfo.setReturnValue(false);
    }
}
