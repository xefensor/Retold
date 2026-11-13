package cz.xefensor.retold.client.render;

import cz.xefensor.retold.worldgen.earth.EarthGuardianLifecycle;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/** Original stone statue geometry; poses consume existing encounter and melee synchronization. */
public final class EarthGuardianModel extends EntityModel<EarthGuardianRenderState> {
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public EarthGuardianModel(ModelPart root) {
        super(root);
        head = root.getChild("head");
        body = root.getChild("body");
        rightArm = root.getChild("right_arm");
        leftArm = root.getChild("left_arm");
        rightLeg = root.getChild("right_leg");
        leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer() {
        // 43 model pixels at the established 1.18 scale match the existing 3.2-block boss height.
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.offset(0.0F, -11.0F, 0.0F));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 16)
                .addBox(-6.0F, 0.0F, -3.0F, 12.0F, 19.0F, 6.0F)
                .texOffs(0, 40).addBox(-5.0F, 2.0F, -4.0F, 10.0F, 6.0F, 1.0F),
                PartPose.offset(0.0F, -11.0F, 0.0F));
        for (int side = -1; side <= 1; side += 2) {
            root.addOrReplaceChild(side < 0 ? "right_arm" : "left_arm", CubeListBuilder.create()
                    .texOffs(40, 0).addBox(-3.0F, -2.0F, -3.0F, 6.0F, 28.0F, 6.0F)
                    .texOffs(64, 0).addBox(-4.0F, -3.0F, -4.0F, 8.0F, 5.0F, 8.0F)
                    .texOffs(64, 16).addBox(-3.5F, 18.0F, -3.5F, 7.0F, 8.0F, 7.0F),
                    PartPose.offset(side * 8.5F, -9.0F, 0.0F));
            root.addOrReplaceChild(side < 0 ? "right_leg" : "left_leg", CubeListBuilder.create()
                    .texOffs(40, 32).addBox(-2.5F, 0.0F, -2.5F, 5.0F, 16.0F, 5.0F),
                    PartPose.offset(side * 3.5F, 8.0F, 0.0F));
        }
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void setupAnim(EarthGuardianRenderState state) {
        super.setupAnim(state);
        float awake = switch (state.lifecycle) {
            case DORMANT, DEFEATED -> 0.0F;
            case AWAKENING -> Mth.clamp(state.awakeningProgress, 0.0F, 1.0F);
            case ROAMING -> 1.0F;
        };
        float eased = awake * awake * (3.0F - 2.0F * awake);
        head.xRot = (1.0F - eased) * 0.35F + state.xRot * Mth.DEG_TO_RAD * eased;
        head.yRot = state.yRot * Mth.DEG_TO_RAD * eased;
        body.xRot = (1.0F - eased) * 0.08F;
        rightArm.xRot = leftArm.xRot = (1.0F - eased) * -0.18F;
        if (state.lifecycle == EarthGuardianLifecycle.ROAMING) {
            float stride = Mth.cos(state.walkAnimationPos * 0.6662F) * state.walkAnimationSpeed;
            rightLeg.xRot = stride * 0.9F;
            leftLeg.xRot = -stride * 0.9F;
            rightArm.xRot = -stride * 0.55F;
            leftArm.xRot = stride * 0.55F;
            if (state.attackTicksRemaining > 0.0F) {
                float swing = Mth.triangleWave(state.attackTicksRemaining, 10.0F);
                rightArm.xRot = leftArm.xRot = -1.7F + 1.5F * swing;
            }
        }
    }
}
