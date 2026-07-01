package cz.xefensor.retold.mixin;

import cz.xefensor.retold.aender.RetoldAenderDimensions;
import cz.xefensor.retold.aender.stability.AenderStabilityData;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkMap.class)
public abstract class AenderChunkMapSaveMixin {
    @Shadow
    @Final
    ServerLevel level;

    @Inject(method = "save", at = @At("HEAD"), cancellable = true)
    private void retold$skipUnstableAenderSave(
            ChunkAccess chunk,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (level.dimension() != RetoldAenderDimensions.AENDER) {
            return;
        }

        if (AenderStabilityData.get(level).isStable(chunk.getPos())) {
            return;
        }

        cir.setReturnValue(false);
    }
}