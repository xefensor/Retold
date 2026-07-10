package cz.xefensor.retold.mixin;

import cz.xefensor.retold.aender.RetoldAenderDimensions;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderSectionRegion.class)
public abstract class AenderRenderSectionRegionLightingMixin {
    private static final CardinalLighting AENDER_FLAT_LIGHTING =
            new CardinalLighting(1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F);

    @Shadow
    @Final
    private ClientLevel level;

    @Shadow
    public abstract LevelLightEngine getLightEngine();

    @Inject(method = "cardinalLighting", at = @At("HEAD"), cancellable = true)
    private void retold$useFlatAenderFaceLighting(CallbackInfoReturnable<CardinalLighting> cir) {
        if (retold$isAender()) {
            cir.setReturnValue(AENDER_FLAT_LIGHTING);
        }
    }

    public int getBrightness(LightLayer lightLayer, BlockPos pos) {
        if (retold$isAender()) {
            return 15;
        }

        return getLightEngine().getLayerListener(lightLayer).getLightValue(pos);
    }

    public int getRawBrightness(BlockPos pos, int amount) {
        if (retold$isAender()) {
            return 15;
        }

        return getLightEngine().getRawBrightness(pos, amount);
    }

    public boolean canSeeSky(BlockPos pos) {
        if (retold$isAender()) {
            return true;
        }

        return getBrightness(LightLayer.SKY, pos) >= 15;
    }

    private boolean retold$isAender() {
        return level.dimension() == RetoldAenderDimensions.AENDER;
    }
}
