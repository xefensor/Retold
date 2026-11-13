package cz.xefensor.retold.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import cz.xefensor.retold.Retold;
import cz.xefensor.retold.client.RetoldClientEvents;
import cz.xefensor.retold.worldgen.earth.EarthGuardian;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;

public final class EarthGuardianRenderer
        extends MobRenderer<EarthGuardian, EarthGuardianRenderState, EarthGuardianModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Retold.MODID,
            "textures/entity/earth_guardian.png"
    );

    public EarthGuardianRenderer(EntityRendererProvider.Context context) {
        super(context, new EarthGuardianModel(context.bakeLayer(RetoldClientEvents.EARTH_GUARDIAN_LAYER)), 0.9F);
    }

    @Override
    protected AABB getBoundingBoxForCulling(EarthGuardian entity) {
        // Melee arms extend above and ahead of the body. Body yaw can turn their reach onto
        // either world horizontal axis, so both margins must cover the full swept silhouette.
        return super.getBoundingBoxForCulling(entity).inflate(1.5D, 1.25D, 1.5D);
    }

    @Override
    public Identifier getTextureLocation(EarthGuardianRenderState state) {
        return TEXTURE;
    }

    @Override
    public EarthGuardianRenderState createRenderState() {
        return new EarthGuardianRenderState();
    }

    @Override
    public void extractRenderState(
            EarthGuardian entity,
            EarthGuardianRenderState state,
            float partialTicks
    ) {
        super.extractRenderState(entity, state, partialTicks);
        state.attackTicksRemaining = entity.getAttackAnimationTick() > 0.0F
                ? entity.getAttackAnimationTick() - partialTicks
                : 0.0F;
        state.lifecycle = entity.lifecycle();
        state.awakeningProgress = entity.awakeningProgress(partialTicks);
    }

    @Override
    protected void scale(EarthGuardianRenderState state, PoseStack poseStack) {
        poseStack.scale(1.18F, 1.18F, 1.18F);
    }
}
