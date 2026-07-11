package cz.xefensor.retold.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import cz.xefensor.retold.aender.entity.AenderEye;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;

public final class RetoldAenderEyeRenderer extends EntityRenderer<AenderEye, EntityRenderState> {
    private static final Identifier TEXTURE =
            Identifier.withDefaultNamespace("textures/item/ender_eye.png");
    private static final RenderType RENDER_TYPE =
            RenderTypes.entityTranslucentEmissive(TEXTURE, true);
    private static final float SIZE = 1.25F;

    public RetoldAenderEyeRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
        this.shadowStrength = 0.0F;
    }

    @Override
    protected int getBlockLightLevel(AenderEye entity, BlockPos blockPos) {
        return 15;
    }

    @Override
    protected int getSkyLightLevel(AenderEye entity, BlockPos blockPos) {
        return 15;
    }

    @Override
    public void submit(
            EntityRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            CameraRenderState camera
    ) {
        poseStack.pushPose();
        poseStack.mulPose(camera.orientation);
        poseStack.scale(SIZE, SIZE, SIZE);

        submitNodeCollector.submitCustomGeometry(
                poseStack,
                RENDER_TYPE,
                (pose, buffer) -> buildQuad(pose, buffer)
        );

        poseStack.popPose();
        super.submit(state, poseStack, submitNodeCollector, camera);
    }

    @Override
    protected float getShadowStrength(EntityRenderState state) {
        return 0.0F;
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }

    private static void buildQuad(PoseStack.Pose pose, VertexConsumer buffer) {
        vertex(buffer, pose, -0.5F, -0.25F, 0.0F, 1.0F);
        vertex(buffer, pose, 0.5F, -0.25F, 1.0F, 1.0F);
        vertex(buffer, pose, 0.5F, 0.75F, 1.0F, 0.0F);
        vertex(buffer, pose, -0.5F, 0.75F, 0.0F, 0.0F);
    }

    private static void vertex(
            VertexConsumer buffer,
            PoseStack.Pose pose,
            float x,
            float y,
            float u,
            float v
    ) {
        buffer.addVertex(pose, x, y, 0.0F)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightCoordsUtil.FULL_BRIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }
}
