package cz.xefensor.retold.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import cz.xefensor.retold.worldgen.earth.EarthGuardianLifecycle;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.monster.breeze.BreezeModel;
import net.minecraft.client.renderer.entity.state.BreezeRenderState;
import org.joml.Vector3f;

/** Offline checks against the installed client models; no game, display, or renderer is started. */
public final class RetoldBossModelChecks {
    private RetoldBossModelChecks() {
    }

    public static void main(String[] args) {
        guardianUsesSynchronizedProgressAndResetsBetweenEntities();
        guardianSilhouetteMatchesItsHeightAndCullingBounds();
        galeLayersFollowVanillaPosesRegardlessOfSubmissionOrder();
    }

    private static void guardianUsesSynchronizedProgressAndResetsBetweenEntities() {
        ModelPart root = EarthGuardianModel.createBodyLayer().bakeRoot();
        EarthGuardianModel model = new EarthGuardianModel(root);
        EarthGuardianRenderState state = new EarthGuardianRenderState();
        model.setupAnim(state);
        float dormantHead = root.getChild("head").xRot;
        state.lifecycle = EarthGuardianLifecycle.AWAKENING;
        state.ageInTicks = 10_000.0F;
        model.setupAnim(state);
        close(dormantHead, root.getChild("head").xRot, "Awakening starts at the statue pose");
        state.awakeningProgress = 0.5F;
        model.setupAnim(state);
        require(root.getChild("head").xRot > 0.0F && root.getChild("head").xRot < dormantHead,
                "Late observers use synchronized encounter progress instead of entity age");
        state.awakeningProgress = 1.0F;
        model.setupAnim(state);
        close(0.0F, root.getChild("head").xRot, "Awakening reaches the roaming pose");

        state.lifecycle = EarthGuardianLifecycle.ROAMING;
        state.walkAnimationSpeed = 0.8F;
        state.walkAnimationPos = 3.0F;
        state.attackTicksRemaining = 5.0F;
        model.setupAnim(state);
        float raisedArm = root.getChild("right_arm").xRot;
        state.attackTicksRemaining = 1.0F;
        model.setupAnim(state);
        require(Math.abs(raisedArm - root.getChild("right_arm").xRot) > 0.1F,
                "Existing melee synchronization drives distinct arm poses");
        model.setupAnim(new EarthGuardianRenderState());
        close(dormantHead, root.getChild("head").xRot, "The next statue resets its head");
        close(0.0F, root.getChild("left_leg").xRot, "The next statue resets its stride");
    }

    private static void guardianSilhouetteMatchesItsHeightAndCullingBounds() {
        ModelPart root = EarthGuardianModel.createBodyLayer().bakeRoot();
        EarthGuardianModel model = new EarthGuardianModel(root);
        EarthGuardianRenderState state = new EarthGuardianRenderState();
        state.lifecycle = EarthGuardianLifecycle.ROAMING;
        model.setupAnim(state);
        float top = boundsAndUvs(root)[1];
        closeWithin(3.2F, (1.501F - top) * 1.18F, 0.12F,
                "The standing statue fits the established boss height");
        for (int ticks = 1; ticks <= 10; ticks++) {
            state.attackTicksRemaining = ticks;
            model.setupAnim(state);
            float[] bounds = boundsAndUvs(root);
            require((1.501F - bounds[1]) * 1.18F <= 4.45F,
                    "Raised arms remain inside the expanded vertical culling bounds");
            for (int yaw = 0; yaw < 360; yaw += 45) {
                float[] rotated = boundsAndUvs(root, yaw);
                float horizontalExtent = Math.max(
                        Math.max(Math.abs(rotated[0]), Math.abs(rotated[3])),
                        Math.max(Math.abs(rotated[2]), Math.abs(rotated[5]))
                ) * 1.18F;
                require(horizontalExtent <= 2.3F,
                        "Melee arms remain inside both horizontal culling bounds at body yaw " + yaw);
            }
        }
    }

    private static void galeLayersFollowVanillaPosesRegardlessOfSubmissionOrder() {
        ModelPart root = GaleCoreModel.createBodyLayer().bakeRoot();
        GaleCoreModel model = new GaleCoreModel(root);
        ModelPart armorRoot = GaleCoreArmorModel.createBodyLayer().bakeRoot();
        GaleCoreArmorModel armor = new GaleCoreArmorModel(armorRoot,
                new GaleCoreModel(GaleCoreModel.createBodyLayer().bakeRoot()));
        BreezeModel reference = new BreezeModel(BreezeModel.createBodyLayer().bakeRoot());
        for (int animation = 0; animation < 7; animation++) {
            BreezeRenderState state = breezeState(animation);
            // Armor is intentionally posed before the visible body, just as deferred submissions may be.
            armor.setupAnim(state);
            model.setupAnim(state);
            reference.setupAnim(state);
            require(model.head().storePose().equals(reference.head().storePose()),
                    "The Gale Core retains the installed Breeze head keyframes");
            require(!model.rods().visible, "Original plates replace the vanilla rods");
            ModelPart body = armorRoot.getChild("body");
            require(body.storePose().equals(model.body().storePose()), "Armor follows animated body transforms");
            require(body.getChild("head").storePose().equals(model.head().storePose()),
                    "The shallow circlet follows animated head transforms");
            require(body.getChild("core").storePose().equals(model.heavyCoreAnchor().storePose()),
                    "Floating plates and the reward share an anchor");
            verifyRewardAlignment(model);
            boundsAndUvs(root);
            boundsAndUvs(armorRoot);
            BreezeModel wind = new BreezeModel(BreezeModel.createWindLayer().bakeRoot());
            BreezeModel eyes = new BreezeModel(BreezeModel.createEyesLayer().bakeRoot());
            wind.setupAnim(state);
            eyes.setupAnim(state);
            boundsAndUvs(wind.root());
            boundsAndUvs(eyes.root());
        }
        BreezeRenderState idle = breezeState(0);
        model.setupAnim(idle);
        armor.setupAnim(idle);
        close(0.0F, model.body().xRot, "The next idle Gale Core resets its body pose");
        close(0.0F, model.head().xRot, "The next idle Gale Core resets its head pose");
        float top = boundsAndUvs(armorRoot)[1];
        closeWithin(3.98F, (1.501F - top) * 2.25F, 0.15F,
                "Commander accents preserve the established boss height");
    }

    private static BreezeRenderState breezeState(int animation) {
        BreezeRenderState state = new BreezeRenderState();
        state.ageInTicks = 40.0F;
        state.idle.start(0);
        switch (animation) {
            case 1 -> state.inhale.start(35);
            case 2 -> state.shoot.start(35);
            case 3 -> state.slide.start(35);
            case 4 -> state.slideBack.start(35);
            case 5 -> state.longJump.start(35);
            case 6 -> state.shoot.start(20);
            default -> { }
        }
        return state;
    }

    private static void verifyRewardAlignment(GaleCoreModel model) {
        PoseStack reward = new PoseStack();
        GaleCoreHeavyCoreLayer.applyCorePose(reward, model.body(), model.heavyCoreAnchor());
        Vector3f center = reward.last().pose().transformPosition(0.5F, 0.25F, 0.5F, new Vector3f());
        PoseStack expected = new PoseStack();
        model.body().translateAndRotate(expected);
        model.heavyCoreAnchor().translateAndRotate(expected);
        Vector3f anchor = expected.last().pose().transformPosition(0.0F, 0.0F, 0.0F, new Vector3f());
        close(0.0F, center.distance(anchor), "The installed Heavy Core stays centered during movement and attacks");
        Vector3f blockUp = reward.last().pose().transformDirection(0.0F, 1.0F, 0.0F, new Vector3f());
        Vector3f modelUp = expected.last().pose().transformDirection(0.0F, -1.0F, 0.0F, new Vector3f());
        close(0.0F, blockUp.distance(modelUp), "The Heavy Core stays upright relative to its animated body");
    }

    private static float[] boundsAndUvs(ModelPart root) {
        return boundsAndUvs(root, 0.0F);
    }

    private static float[] boundsAndUvs(ModelPart root, float bodyYaw) {
        float[] bounds = {Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
                Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY};
        PoseStack transformed = new PoseStack();
        transformed.mulPose(Axis.YP.rotationDegrees(bodyYaw));
        root.visit(transformed, (pose, path, index, cube) -> {
            for (ModelPart.Polygon face : cube.polygons) {
                for (ModelPart.Vertex vertex : face.vertices()) {
                    require(vertex.u() >= 0.0F && vertex.u() <= 1.0F
                                    && vertex.v() >= 0.0F && vertex.v() <= 1.0F,
                            path + " texture coordinates stay inside their atlas");
                    Vector3f point = pose.pose().transformPosition(
                            vertex.worldX(), vertex.worldY(), vertex.worldZ(), new Vector3f());
                    require(Float.isFinite(point.x()) && Float.isFinite(point.y()) && Float.isFinite(point.z()),
                            path + " produces finite geometry");
                    bounds[0] = Math.min(bounds[0], point.x());
                    bounds[1] = Math.min(bounds[1], point.y());
                    bounds[2] = Math.min(bounds[2], point.z());
                    bounds[3] = Math.max(bounds[3], point.x());
                    bounds[4] = Math.max(bounds[4], point.y());
                    bounds[5] = Math.max(bounds[5], point.z());
                }
            }
        });
        require(Float.isFinite(bounds[0]), "A model layer contains geometry");
        return bounds;
    }

    private static void close(float expected, float actual, String message) {
        closeWithin(expected, actual, 0.00001F, message);
    }

    private static void closeWithin(float expected, float actual, float tolerance, String message) {
        require(Math.abs(expected - actual) <= tolerance, message + ": " + actual + " versus " + expected);
    }

    private static void require(boolean valid, String message) {
        if (!valid) {
            throw new AssertionError(message);
        }
    }
}
