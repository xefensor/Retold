package cz.xefensor.retold.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.environment.AtmosphericFogEnvironment;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AtmosphericFogEnvironment.class)
public abstract class NetherFogColorMixin {
    private static final int NETHER_WASTES_FOG_COLOR = 0x330808;

    @ModifyExpressionValue(
            method = "getBaseColor",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/attribute/EnvironmentAttributeProbe;getValue(Lnet/minecraft/world/attribute/EnvironmentAttribute;F)Ljava/lang/Object;",
                    ordinal = 0
            )
    )
    private Object retold$useClassicNetherFogColor(
            Object original,
            @Local(argsOnly = true) ClientLevel level
    ) {
        if (level.dimension() == Level.NETHER) {
            return NETHER_WASTES_FOG_COLOR;
        }

        return original;
    }
}
