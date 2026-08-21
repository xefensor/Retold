package cz.xefensor.retold.mixin;

import cz.xefensor.retold.event.RetoldPiglinEvents;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Lets Nether Remnant fire attacks burn their Zombified Piglin enemies. */
@Mixin(Entity.class)
public abstract class ZombifiedPiglinFireImmunityMixin {
    @Inject(method = "fireImmune", at = @At("RETURN"), cancellable = true)
    private void retold$removeZombifiedPiglinFireImmunity(
            CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        Entity self = (Entity) (Object) this;

        if (RetoldPiglinEvents.shouldRemoveFireImmunity(self)) {
            callbackInfo.setReturnValue(false);
        }
    }
}
