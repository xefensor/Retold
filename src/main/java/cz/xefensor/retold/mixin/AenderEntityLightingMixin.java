package cz.xefensor.retold.mixin;

import cz.xefensor.retold.aender.RetoldAenderDimensions;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class AenderEntityLightingMixin<T extends Entity, S extends EntityRenderState> {
    @Inject(method = "getPackedLightCoords", at = @At("HEAD"), cancellable = true)
    private void retold$useFlatAenderEntityLighting(
            T entity,
            float partialTick,
            CallbackInfoReturnable<Integer> cir
    ) {
        if (entity.level().dimension() == RetoldAenderDimensions.AENDER) {
            cir.setReturnValue(LightCoordsUtil.FULL_BRIGHT);
        }
    }
}
