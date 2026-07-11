package cz.xefensor.retold.mixin;

import cz.xefensor.retold.aender.RetoldAenderDimensions;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class AenderWeatherMixin {
    @Inject(method = "canHaveWeather", at = @At("HEAD"), cancellable = true)
    private void retold$aenderCannotHaveWeather(CallbackInfoReturnable<Boolean> cir) {
        if (((Level) (Object) this).dimension() == RetoldAenderDimensions.AENDER) {
            cir.setReturnValue(false);
        }
    }
}
