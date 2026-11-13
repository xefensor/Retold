package cz.xefensor.retold.client.render;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.monster.breeze.BreezeModel;
import net.minecraft.client.renderer.entity.state.BreezeRenderState;

import java.util.List;
import java.util.Map;

/** Installed Breeze silhouette and animations; floating armor replaces its rods. */
public final class GaleCoreModel extends BreezeModel {
    private final ModelPart body;
    private final ModelPart core = new ModelPart(List.of(), Map.of());

    public GaleCoreModel(ModelPart root) {
        super(root);
        body = root.getChild("body");
        core.setInitialPose(PartPose.offset(0.0F, 9.0F, 0.0F));
        core.resetPose();
        rods().visible = false;
    }

    public ModelPart body() {
        return body;
    }

    public ModelPart heavyCoreAnchor() {
        return core;
    }

    public static LayerDefinition createBodyLayer() {
        return BreezeModel.createBodyLayer();
    }

    @Override
    public void setupAnim(BreezeRenderState state) {
        super.setupAnim(state);
        core.resetPose();
        // Follow vertical breathing motion without spinning the reward at rod speed.
        core.y = rods().y + 1.0F;
    }
}
