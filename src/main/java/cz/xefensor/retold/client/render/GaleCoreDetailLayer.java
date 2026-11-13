package cz.xefensor.retold.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import cz.xefensor.retold.Retold;
import cz.xefensor.retold.client.RetoldClientEvents;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.breeze.BreezeModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/** Installed Breeze wind and glowing face with a separate original commander armor material. */
public final class GaleCoreDetailLayer extends RenderLayer<GaleCoreRenderState, GaleCoreModel> {
    private static final Identifier WIND = Identifier.withDefaultNamespace("textures/entity/breeze/breeze_wind.png");
    private static final Identifier EYES = Identifier.withDefaultNamespace("textures/entity/breeze/breeze_eyes.png");
    private static final Identifier ARMOR = Identifier.fromNamespaceAndPath(Retold.MODID, "textures/entity/gale_core.png");
    private final BreezeModel wind;
    private final BreezeModel eyes;
    private final GaleCoreArmorModel armor;

    public GaleCoreDetailLayer(RenderLayerParent<GaleCoreRenderState, GaleCoreModel> renderer, EntityModelSet models) {
        super(renderer);
        wind = new BreezeModel(models.bakeLayer(ModelLayers.BREEZE_WIND));
        eyes = new BreezeModel(models.bakeLayer(ModelLayers.BREEZE_EYES));
        armor = new GaleCoreArmorModel(models.bakeLayer(RetoldClientEvents.GALE_CORE_ARMOR_LAYER),
                new GaleCoreModel(models.bakeLayer(ModelLayers.BREEZE)));
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords,
                       GaleCoreRenderState state, float yRot, float xRot) {
        if (state.isInvisible) {
            return;
        }
        collector.submitModel(armor, state, poseStack, RenderTypes.entityCutout(ARMOR), lightCoords,
                LivingEntityRenderer.getOverlayCoords(state, 0.0F), state.outlineColor, null);
        collector.order(1).submitModel(wind, state, poseStack,
                RenderTypes.breezeWind(WIND, state.ageInTicks * 0.02F % 1.0F, 0.0F),
                lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor, null);
        collector.order(1).submitModel(eyes, state, poseStack, RenderTypes.breezeEyes(EYES),
                lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor, null);
    }
}
