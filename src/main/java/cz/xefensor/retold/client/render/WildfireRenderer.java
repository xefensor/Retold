package cz.xefensor.retold.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import cz.xefensor.retold.Retold;
import cz.xefensor.retold.client.RetoldClientEvents;
import cz.xefensor.retold.worldgen.fire.Wildfire;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

public final class WildfireRenderer
        extends MobRenderer<Wildfire, WildfireRenderState, WildfireModel> {
    private static final float MODEL_SCALE = 1.3F;
    private static final Identifier TEXTURE_LOCATION =
            Identifier.fromNamespaceAndPath(Retold.MODID, "textures/entity/wildfire.png");

    public WildfireRenderer(EntityRendererProvider.Context context) {
        super(context, new WildfireModel(context.bakeLayer(RetoldClientEvents.WILDFIRE_LAYER)), 0.9F);
    }

    @Override
    protected int getBlockLightLevel(Wildfire entity, BlockPos blockPos) {
        return 15;
    }

    @Override
    public Identifier getTextureLocation(WildfireRenderState state) {
        return TEXTURE_LOCATION;
    }

    @Override
    public WildfireRenderState createRenderState() {
        return new WildfireRenderState();
    }

    @Override
    public void extractRenderState(
            Wildfire entity,
            WildfireRenderState state,
            float partialTicks
    ) {
        super.extractRenderState(entity, state, partialTicks);
        state.shieldCount = entity.getShieldCount();
    }

    @Override
    protected void scale(WildfireRenderState state, PoseStack poseStack) {
        poseStack.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
    }
}
