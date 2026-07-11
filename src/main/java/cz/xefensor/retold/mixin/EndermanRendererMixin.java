package cz.xefensor.retold.mixin;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.client.stage.RetoldClientStage;
import cz.xefensor.retold.stage.RetoldWorldStage;
import net.minecraft.client.renderer.entity.EndermanRenderer;
import net.minecraft.client.renderer.entity.state.EndermanRenderState;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EndermanRenderer.class)
public abstract class EndermanRendererMixin {
    private static final Identifier RETOLD_WHITE_ENDERMAN_TEXTURE =
            Identifier.fromNamespaceAndPath(
                    Retold.MODID,
                    "textures/entity/enderman/enderman_white.png"
            );

    @Inject(
            method = "getTextureLocation(Lnet/minecraft/client/renderer/entity/state/EndermanRenderState;)Lnet/minecraft/resources/Identifier;",
            at = @At("RETURN"),
            cancellable = true
    )
    private void retold$useWhiteEndermanTextureInStage3(
            EndermanRenderState state,
            CallbackInfoReturnable<Identifier> cir
    ) {
        if (RetoldClientStage.getStage() == RetoldWorldStage.STAGE_3) {
            cir.setReturnValue(RETOLD_WHITE_ENDERMAN_TEXTURE);
        }
    }
}
