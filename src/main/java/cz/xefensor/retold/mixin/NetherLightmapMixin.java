package cz.xefensor.retold.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LightmapRenderStateExtractor.class)
public abstract class NetherLightmapMixin {
    private static final int CLASSIC_NETHER_AMBIENT_COLOR = 0x1A1A1A;

    @ModifyExpressionValue(
            method = "extract",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/attribute/EnvironmentAttributeProbe;getValue(Lnet/minecraft/world/attribute/EnvironmentAttribute;F)Ljava/lang/Object;",
                    ordinal = 3
            )
    )
    private Object retold$useClassicNetherAmbientLight(
            Object original,
            @Local ClientLevel level
    ) {
        if (level.dimension() == Level.NETHER) {
            return CLASSIC_NETHER_AMBIENT_COLOR;
        }

        return original;
    }
}
