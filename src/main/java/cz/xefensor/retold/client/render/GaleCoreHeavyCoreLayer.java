package cz.xefensor.retold.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;

/** Render the actual installed reward model, allowing resource packs to replace it normally. */
public final class GaleCoreHeavyCoreLayer extends RenderLayer<GaleCoreRenderState, GaleCoreModel> {
    public GaleCoreHeavyCoreLayer(RenderLayerParent<GaleCoreRenderState, GaleCoreModel> renderer) {
        super(renderer);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords,
                       GaleCoreRenderState state, float yRot, float xRot) {
        if (state.isInvisible || state.heavyCore.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        applyCorePose(poseStack, getParentModel().body(), getParentModel().heavyCoreAnchor());
        state.heavyCore.submitMultiLayer(poseStack, collector, lightCoords,
                LivingEntityRenderer.getOverlayCoords(state, 0.0F), state.outlineColor);
        poseStack.popPose();
    }

    static void applyCorePose(PoseStack poseStack, ModelPart body, ModelPart anchor) {
        body.translateAndRotate(poseStack);
        anchor.translateAndRotate(poseStack);
        // Convert the living model's downward Y to block coordinates and center its half-block cube.
        poseStack.scale(1.0F, -1.0F, -1.0F);
        poseStack.translate(-0.5F, -0.25F, -0.5F);
    }
}
