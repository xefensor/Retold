package cz.xefensor.retold.client.render;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

import java.util.Set;

/** Java model shaped around the Minecraft Dungeons 64x64 Wildfire atlas. */
public final class WildfireModel extends EntityModel<WildfireRenderState> {
    private static final int SHIELD_COUNT = 4;
    private static final int BODY_PART_COUNT = 12;

    private final ModelPart head;
    private final ModelPart[] bodyParts = new ModelPart[BODY_PART_COUNT];
    private final ModelPart[] shields = new ModelPart[SHIELD_COUNT];

    public WildfireModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");

        for (int index = 0; index < bodyParts.length; index++) {
            bodyParts[index] = root.getChild("body_" + index);
        }

        for (int index = 0; index < shields.length; index++) {
            shields[index] = root.getChild("shield_" + index);
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild(
                "head",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, -3.0F, 0.0F)
        );
        head.addOrReplaceChild(
                "helmet",
                CubeListBuilder.create()
                        .texOffs(32, 48)
                        .addBox(
                                -4.0F,
                                -4.0F,
                                -4.0F,
                                8.0F,
                                8.0F,
                                8.0F,
                                new CubeDeformation(0.65F)
                ),
                PartPose.ZERO
        );
        head.addOrReplaceChild(
                "helmet_eyes",
                CubeListBuilder.create()
                        // The reference places two white eyes in the band below the jewel.
                        .texOffs(6, 6)
                        .addBox(
                                -3.0F,
                                -1.0F,
                                -4.7F,
                                1.0F,
                                1.0F,
                                0.0F,
                                Set.of(Direction.NORTH)
                        )
                        .texOffs(7, 6)
                        .addBox(
                                2.0F,
                                -1.0F,
                                -4.7F,
                                1.0F,
                                1.0F,
                                0.0F,
                                Set.of(Direction.NORTH)
                        ),
                PartPose.ZERO
        );

        CubeListBuilder bodyRod = CubeListBuilder.create()
                .texOffs(0, 16)
                .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 10.0F, 2.0F);

        for (int index = 0; index < BODY_PART_COUNT; index++) {
            root.addOrReplaceChild("body_" + index, bodyRod, PartPose.ZERO);
        }

        CubeListBuilder shield = CubeListBuilder.create()
                .texOffs(0, 16)
                .addBox(-3.5F, -6.0F, -1.0F, 7.0F, 14.0F, 2.0F);
        root.addOrReplaceChild(
                "shield_0",
                shield,
                PartPose.offsetAndRotation(0.0F, 3.0F, -9.5F, 0.0F, 0.0F, 0.0F)
        );
        root.addOrReplaceChild(
                "shield_1",
                shield,
                PartPose.offsetAndRotation(9.5F, 3.0F, 0.0F, 0.0F, Mth.HALF_PI, 0.0F)
        );
        root.addOrReplaceChild(
                "shield_2",
                shield,
                PartPose.offsetAndRotation(0.0F, 3.0F, 9.5F, 0.0F, 0.0F, 0.0F)
        );
        root.addOrReplaceChild(
                "shield_3",
                shield,
                PartPose.offsetAndRotation(-9.5F, 3.0F, 0.0F, 0.0F, Mth.HALF_PI, 0.0F)
        );

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(WildfireRenderState state) {
        super.setupAnim(state);
        head.yRot = state.yRot * Mth.DEG_TO_RAD;
        head.xRot = state.xRot * Mth.DEG_TO_RAD;

        float bodyOrbit = state.ageInTicks * Mth.PI * -0.035F;

        for (int index = 0; index < bodyParts.length; index++) {
            ModelPart bodyPart = bodyParts[index];
            int ring = index / 4;
            float radius = 5.5F - ring * 1.5F;
            float angle = bodyOrbit * (ring == 1 ? -0.65F : 1.0F)
                    + (index % 4) * Mth.HALF_PI
                    + ring * Mth.PI * 0.25F;
            bodyPart.x = Mth.cos(angle) * radius;
            bodyPart.z = Mth.sin(angle) * radius;
            bodyPart.y = 1.0F + ring * 4.5F
                    + Mth.sin(state.ageInTicks * 0.12F + index) * 0.45F;
        }

        float orbit = state.ageInTicks * 0.025F;

        for (int index = 0; index < shields.length; index++) {
            ModelPart shield = shields[index];
            float angle = orbit + index * Mth.HALF_PI;
            shield.visible = index < state.shieldCount;
            shield.x = Mth.sin(angle) * 9.5F;
            shield.z = -Mth.cos(angle) * 9.5F;
            shield.y = 3.0F + Mth.sin(state.ageInTicks * 0.12F + index) * 0.45F;
            shield.yRot = -angle;
        }
    }
}
