package cz.xefensor.retold.mixin;

import net.minecraft.world.level.dimension.end.EnderDragonFight;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnderDragonFight.class)
public abstract class EndDragonFightMixin {
    @Inject(
            method = "spawnExitPortal",
            at = @At("HEAD"),
            cancellable = true
    )
    private void retold$skipInitialExitPortal(boolean previouslyKilled, CallbackInfo ci) {
        if (!previouslyKilled) {
            ci.cancel();
        }
    }
}