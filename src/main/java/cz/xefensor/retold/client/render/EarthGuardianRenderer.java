package cz.xefensor.retold.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import cz.xefensor.retold.worldgen.earth.EarthGuardian;
import net.minecraft.client.model.animal.golem.IronGolemModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.IronGolemRenderState;
import net.minecraft.resources.Identifier;

public final class EarthGuardianRenderer
        extends MobRenderer<EarthGuardian, IronGolemRenderState, IronGolemModel> {
    private static final Identifier PLACEHOLDER_TEXTURE = Identifier.withDefaultNamespace(
            "textures/entity/iron_golem/iron_golem.png"
    );

    public EarthGuardianRenderer(EntityRendererProvider.Context context) {
        super(context, new IronGolemModel(context.bakeLayer(ModelLayers.IRON_GOLEM)), 0.9F);
    }

    @Override
    public Identifier getTextureLocation(IronGolemRenderState state) {
        return PLACEHOLDER_TEXTURE;
    }

    @Override
    public IronGolemRenderState createRenderState() {
        return new IronGolemRenderState();
    }

    @Override
    public void extractRenderState(
            EarthGuardian entity,
            IronGolemRenderState state,
            float partialTicks
    ) {
        super.extractRenderState(entity, state, partialTicks);
        state.attackTicksRemaining = entity.getAttackAnimationTick() > 0.0F
                ? entity.getAttackAnimationTick() - partialTicks
                : 0.0F;
        state.offerFlowerTick = 0;
        state.flowerBlock.clear();
        state.crackiness = entity.getCrackiness();
    }

    @Override
    protected void scale(IronGolemRenderState state, PoseStack poseStack) {
        poseStack.scale(1.18F, 1.18F, 1.18F);
    }
}
