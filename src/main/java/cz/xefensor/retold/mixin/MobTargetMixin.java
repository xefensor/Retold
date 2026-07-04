package cz.xefensor.retold.mixin;

import cz.xefensor.retold.event.RetoldFactionTargetGuards;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class MobTargetMixin {
    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void retold$blockWarningSuppressedTarget(LivingEntity target, CallbackInfo callbackInfo) {
        if (target == null) {
            return;
        }

        Mob mob = (Mob) (Object) this;

        if (RetoldFactionTargetGuards.shouldBlockTarget(mob, target)) {
            callbackInfo.cancel();
        }
    }
}