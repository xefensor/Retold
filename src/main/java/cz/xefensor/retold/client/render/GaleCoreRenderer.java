package cz.xefensor.retold.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import cz.xefensor.retold.worldgen.air.GaleCore;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.breeze.BreezeModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.BreezeEyesLayer;
import net.minecraft.client.renderer.entity.layers.BreezeWindLayer;
import net.minecraft.client.renderer.entity.state.BreezeRenderState;
import net.minecraft.resources.Identifier;

public class GaleCoreRenderer extends MobRenderer<GaleCore, BreezeRenderState, BreezeModel> {
    private static final Identifier TEXTURE_LOCATION =
            Identifier.withDefaultNamespace("textures/entity/breeze/breeze.png");

    public GaleCoreRenderer(EntityRendererProvider.Context context) {
        super(context, new BreezeModel(context.bakeLayer(ModelLayers.BREEZE)), 1.35F);
        this.addLayer(new BreezeWindLayer(this, context.getModelSet()));
        this.addLayer(new BreezeEyesLayer(this, context.getModelSet()));
    }

    @Override
    public Identifier getTextureLocation(BreezeRenderState state) {
        return TEXTURE_LOCATION;
    }

    @Override
    public BreezeRenderState createRenderState() {
        return new BreezeRenderState();
    }

    @Override
    public void extractRenderState(GaleCore entity, BreezeRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.idle.copyFrom(entity.idle);
        state.shoot.copyFrom(entity.shoot);
        state.slide.copyFrom(entity.slide);
        state.slideBack.copyFrom(entity.slideBack);
        state.inhale.copyFrom(entity.inhale);
        state.longJump.copyFrom(entity.longJump);
    }

    @Override
    protected void scale(BreezeRenderState state, PoseStack poseStack) {
        poseStack.scale(2.25F, 2.25F, 2.25F);
    }
}
