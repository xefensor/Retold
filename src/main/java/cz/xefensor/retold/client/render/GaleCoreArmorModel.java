package cz.xefensor.retold.client.render;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.BreezeRenderState;
import net.minecraft.util.Mth;

/** Original low-detail commander accents, driven by the vanilla Breeze animation state. */
public final class GaleCoreArmorModel extends EntityModel<BreezeRenderState> {
    private final GaleCoreModel poseModel;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart core;
    private final ModelPart[] guards = new ModelPart[4];

    public GaleCoreArmorModel(ModelPart root, GaleCoreModel poseModel) {
        super(root);
        this.poseModel = poseModel;
        body = root.getChild("body");
        head = body.getChild("head");
        core = body.getChild("core");
        for (int index = 0; index < guards.length; index++) {
            guards[index] = core.getChild("guard_" + index);
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition body = mesh.getRoot().addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0.0F, 4.0F, 0.0F));
        PartDefinition core = body.addOrReplaceChild("core", CubeListBuilder.create(), PartPose.offset(0.0F, 9.0F, 0.0F));
        for (int index = 0; index < 4; index++) {
            // A shallow cool circlet leaves the Breeze's broad brows and face unobstructed.
            head.addOrReplaceChild("crown_" + index, CubeListBuilder.create().texOffs(14, 0)
                    .addBox(-4.0F, -8.25F, -4.25F, 8.0F, 1.0F, 0.5F)
                    .addBox(-3.0F, -8.75F, -4.25F, 1.0F, 0.5F, 0.5F)
                    .addBox(2.0F, -8.75F, -4.25F, 1.0F, 0.5F, 0.5F),
                    PartPose.rotation(0.0F, index * Mth.HALF_PI, 0.0F));
            float angle = Mth.PI * 0.25F + index * Mth.HALF_PI;
            // Thin diagonal plates preserve a frontal gap around the reward.
            core.addOrReplaceChild("guard_" + index, CubeListBuilder.create().texOffs(0, 0)
                    .addBox(-2.5F, -4.0F, -0.5F, 5.0F, 8.0F, 1.0F),
                    PartPose.offsetAndRotation(Mth.sin(angle) * 7.0F, 0.0F, -Mth.cos(angle) * 7.0F,
                            0.0F, -angle, 0.0F));
        }
        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void setupAnim(BreezeRenderState state) {
        super.setupAnim(state);
        // Independent posing avoids depending on deferred collector submission order.
        poseModel.setupAnim(state);
        body.loadPose(poseModel.body().storePose());
        head.loadPose(poseModel.head().storePose());
        core.loadPose(poseModel.heavyCoreAnchor().storePose());
        float breath = state.inhale.isStarted() ? 1.0F : 0.0F;
        float shot = state.shoot.isStarted()
                ? Mth.sin(Mth.clamp(state.shoot.getTimeInMillis(state.ageInTicks) / 500.0F, 0.0F, 1.0F) * Mth.PI)
                : 0.0F;
        for (int index = 0; index < guards.length; index++) {
            float angle = Mth.PI * 0.25F + index * Mth.HALF_PI;
            float radius = 7.0F - breath * 0.5F + shot * 0.5F;
            guards[index].x = Mth.sin(angle) * radius;
            guards[index].z = -Mth.cos(angle) * radius;
            guards[index].y = Mth.sin(state.ageInTicks * 0.08F + index) * 0.25F;
        }
    }
}
