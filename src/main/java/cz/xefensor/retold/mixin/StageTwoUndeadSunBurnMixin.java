package cz.xefensor.retold.mixin;

import cz.xefensor.retold.event.RetoldUndeadEvents;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Suppresses only vanilla sunlight ignition for Stage 2 Retold Undead. */
@Mixin(Mob.class)
public abstract class StageTwoUndeadSunBurnMixin {
    @Inject(method = "isSunBurnTick", at = @At("HEAD"), cancellable = true)
    private void retold$preventStageTwoUndeadSunBurn(
            CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        Mob self = (Mob) (Object) this;

        if (RetoldUndeadEvents.shouldPreventSunBurn(self)) {
            callbackInfo.setReturnValue(false);
        }
    }
}
