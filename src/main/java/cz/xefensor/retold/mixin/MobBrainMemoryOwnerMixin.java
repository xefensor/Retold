package cz.xefensor.retold.mixin;

import cz.xefensor.retold.territory.RetoldTerritoryBrainGuards;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class MobBrainMemoryOwnerMixin {
    @Inject(method = "serverAiStep", at = @At("HEAD"))
    private void retold$pushBrainMemoryOwner(CallbackInfo callbackInfo) {
        RetoldTerritoryBrainGuards.pushCurrentMob((Mob) (Object) this);
    }

    @Inject(method = "serverAiStep", at = @At("RETURN"))
    private void retold$popBrainMemoryOwner(CallbackInfo callbackInfo) {
        RetoldTerritoryBrainGuards.popCurrentMob((Mob) (Object) this);
    }
}