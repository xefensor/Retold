package cz.xefensor.retold.client.render;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.client.stage.RetoldClientStage;
import net.minecraft.client.model.monster.enderman.EndermanModel;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.state.EndermanRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public class RetoldEndermanEyesLayer
        extends EyesLayer<EndermanRenderState, EndermanModel<EndermanRenderState>> {

    private static final RenderType PURPLE_EYES = RenderTypes.eyes(
            Identifier.fromNamespaceAndPath(
                    Retold.MODID,
                    "textures/entity/enderman/enderman_eyes_purple.png"
            )
    );

    private static final RenderType GREEN_EYES = RenderTypes.eyes(
            Identifier.fromNamespaceAndPath(
                    Retold.MODID,
                    "textures/entity/enderman/enderman_eyes_green.png"
            )
    );

    public RetoldEndermanEyesLayer(
            RenderLayerParent<EndermanRenderState, EndermanModel<EndermanRenderState>> parent
    ) {
        super(parent);
    }

    @Override
    public RenderType renderType() {
        if (RetoldClientStage.shouldUseGreenEndermanEyes()) {
            return GREEN_EYES;
        }

        return PURPLE_EYES;
    }
}