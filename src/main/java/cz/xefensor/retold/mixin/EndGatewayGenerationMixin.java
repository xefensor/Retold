package cz.xefensor.retold.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnderDragonFight.class)
public abstract class EndGatewayGenerationMixin {
    @Inject(
            method = "spawnNewGateway()V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void retold$preventRandomEndGateway(CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(
            method = "spawnNewGateway(Lnet/minecraft/core/BlockPos;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void retold$preventEndGatewayAtPos(BlockPos pos, CallbackInfo ci) {
        ci.cancel();
    }
}