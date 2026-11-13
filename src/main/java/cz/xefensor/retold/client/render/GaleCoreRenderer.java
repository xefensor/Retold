package cz.xefensor.retold.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import cz.xefensor.retold.client.RetoldClientEvents;
import cz.xefensor.retold.worldgen.air.GaleCore;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

public class GaleCoreRenderer extends MobRenderer<GaleCore, GaleCoreRenderState, GaleCoreModel> {
    private static final BlockDisplayContext CORE_DISPLAY_CONTEXT = BlockDisplayContext.create();
    private static final Identifier TEXTURE_LOCATION =
            Identifier.withDefaultNamespace("textures/entity/breeze/breeze.png");
    private final BlockModelResolver blockModelResolver;

    public GaleCoreRenderer(EntityRendererProvider.Context context) {
        super(context, new GaleCoreModel(context.bakeLayer(RetoldClientEvents.GALE_CORE_LAYER)), 1.35F);
        blockModelResolver = context.getBlockModelResolver();
        addLayer(new GaleCoreHeavyCoreLayer(this));
        addLayer(new GaleCoreDetailLayer(this, context.getModelSet()));
    }

    @Override
    protected AABB getBoundingBoxForCulling(GaleCore entity) {
        // The enlarged Breeze wind and floating plates extend beyond its narrow collision box.
        return super.getBoundingBoxForCulling(entity).inflate(0.75D);
    }

    @Override
    public Identifier getTextureLocation(GaleCoreRenderState state) {
        return TEXTURE_LOCATION;
    }

    @Override
    public GaleCoreRenderState createRenderState() {
        return new GaleCoreRenderState();
    }

    @Override
    public void extractRenderState(GaleCore entity, GaleCoreRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        blockModelResolver.update(state.heavyCore, Blocks.HEAVY_CORE.defaultBlockState(), CORE_DISPLAY_CONTEXT);
        state.idle.copyFrom(entity.idle);
        state.shoot.copyFrom(entity.shoot);
        state.slide.copyFrom(entity.slide);
        state.slideBack.copyFrom(entity.slideBack);
        state.inhale.copyFrom(entity.inhale);
        state.longJump.copyFrom(entity.longJump);
    }

    @Override
    protected void scale(GaleCoreRenderState state, PoseStack poseStack) {
        poseStack.scale(2.25F, 2.25F, 2.25F);
    }
}
