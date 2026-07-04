package cz.xefensor.retold.mixin;

import cz.xefensor.retold.event.RetoldFactionTargetGuards;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class MobAggressiveMixin {
    @Inject(method = "setAggressive", at = @At("HEAD"), cancellable = true)
    private void retold$blockIdlePiglinAggressivePose(boolean aggressive, CallbackInfo callbackInfo) {
        Mob mob = (Mob) (Object) this;

        if (RetoldFactionTargetGuards.shouldBlockAggressiveState(mob, aggressive)) {
            callbackInfo.cancel();
            RetoldFactionTargetGuards.setAggressiveIgnoringGuard(mob, false);
        }
    }
}