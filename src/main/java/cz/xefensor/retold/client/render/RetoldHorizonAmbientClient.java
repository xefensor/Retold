package cz.xefensor.retold.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import cz.xefensor.retold.client.texture.RetoldGeneratedHorizonTexture;
import cz.xefensor.retold.network.RetoldHorizonCuePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.Lightmap;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.List;

public final class RetoldHorizonAmbientClient {
    private static final int PLACEMENT_ATTEMPTS_PER_RETRY = 24;
    private static final int PLACEMENT_RETRY_INTERVAL_TICKS = 10;
    private static final int MAXIMUM_PLACEMENT_RETRIES = 8;
    private static final double FIGURE_EYE_HEIGHT = 1.62D;
    private static final int MINIMUM_RENDER_DISTANCE_BLOCKS = 24;
    private static final int RENDER_BOUNDARY_INSET_BLOCKS = 1;
    private static final int ENCLOSED_VERTICAL_SEARCH_RADIUS = 32;
    private static final double APPROACH_DISMISSAL_FRACTION = 0.25D;
    private static final double MINIMUM_APPROACH_DISMISSAL_BLOCKS = 12.0D;
    private static final int SUSTAINED_LOOK_DISMISSAL_TICKS = 2 * 20;
    private static final double SUSTAINED_LOOK_DOT_PRODUCT = 0.995D;
    private static final float PLAYER_SHADOW_RADIUS = 0.5F;

    private static PlayerModel model;
    private static AvatarRenderState renderState;
    private static RenderType renderType;
    private static PendingCue pendingCue;
    private static ActiveCue activeCue;
    private static ClientLevel currentLevel;

    private RetoldHorizonAmbientClient() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(RetoldHorizonAmbientClient::onClientTickPost);
        NeoForge.EVENT_BUS.addListener(RetoldHorizonAmbientClient::submitCustomGeometry);
    }

    public static void handleCue(RetoldHorizonCuePayload payload) {
        ClientLevel level = Minecraft.getInstance().level;

        if (currentLevel != level) {
            pendingCue = null;
            activeCue = null;
            currentLevel = level;
        }

        int duration = Mth.clamp(payload.durationTicks(), 5 * 20, 10 * 20);
        pendingCue = new PendingCue(payload.phase(), duration, 0, 0);
        activeCue = null;
    }

    public static void clear() {
        pendingCue = null;
        activeCue = null;
        currentLevel = null;
    }

    private static void onClientTickPost(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;

        if (level == null || player == null) {
            clear();
            return;
        }

        if (currentLevel != level) {
            pendingCue = null;
            activeCue = null;
            currentLevel = level;
            return;
        }

        if (activeCue != null) {
            int remainingTicks = activeCue.remainingTicks() - 1;
            int lookTicks = isLookingAtFigure(level, player, activeCue.position())
                    ? activeCue.lookTicks() + 1
                    : Math.max(0, activeCue.lookTicks() - 2);

            if (remainingTicks <= 0
                    || !level.isLoaded(BlockPos.containing(activeCue.position()))
                    || player.position().distanceToSqr(activeCue.position())
                    <= activeCue.approachDismissalDistanceSqr()
                    || lookTicks >= SUSTAINED_LOOK_DISMISSAL_TICKS) {
                activeCue = null;
            } else {
                activeCue = new ActiveCue(
                        activeCue.position(),
                        remainingTicks,
                        activeCue.approachDismissalDistanceSqr(),
                        lookTicks
                );
            }
        }

        if (pendingCue == null) {
            return;
        }

        if (pendingCue.retryDelayTicks() > 0) {
            pendingCue = new PendingCue(
                    pendingCue.phase(),
                    pendingCue.durationTicks(),
                    pendingCue.retries(),
                    pendingCue.retryDelayTicks() - 1
            );
            return;
        }

        Vec3 position = findPlacement(minecraft, pendingCue.phase(), pendingCue.retries());

        if (position != null) {
            double initialDistance = player.position().distanceTo(position);
            double requiredApproachDistance = Math.max(
                    MINIMUM_APPROACH_DISMISSAL_BLOCKS,
                    initialDistance * APPROACH_DISMISSAL_FRACTION
            );
            double dismissalDistance = Math.max(
                    4.0D,
                    initialDistance - requiredApproachDistance
            );
            activeCue = new ActiveCue(
                    position,
                    pendingCue.durationTicks(),
                    dismissalDistance * dismissalDistance,
                    0
            );
            pendingCue = null;
            return;
        }

        int retries = pendingCue.retries() + 1;

        if (retries >= MAXIMUM_PLACEMENT_RETRIES) {
            pendingCue = null;
        } else {
            pendingCue = new PendingCue(
                    pendingCue.phase(),
                    pendingCue.durationTicks(),
                    retries,
                    PLACEMENT_RETRY_INTERVAL_TICKS
            );
        }
    }

    private static Vec3 findPlacement(Minecraft minecraft, long phase, int retry) {
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;

        if (level == null || player == null) {
            return null;
        }

        int configuredDistance = minecraft.options.renderDistance().get() * 16;
        int edgeDistance = Math.max(
                MINIMUM_RENDER_DISTANCE_BLOCKS,
                configuredDistance - RENDER_BOUNDARY_INSET_BLOCKS
        );
        RandomSource random = RandomSource.create(phase + retry * 0x6A09E667F3BCC909L);
        float viewYaw = player.getYRot();

        for (int attempt = 0; attempt < PLACEMENT_ATTEMPTS_PER_RETRY; attempt++) {
            double angleOffset = (random.nextDouble() - 0.5D) * 80.0D;
            double yawRadians = Math.toRadians(viewYaw + angleOffset);
            int x = Mth.floor(player.getX() - Math.sin(yawRadians) * edgeDistance);
            int z = Mth.floor(player.getZ() + Math.cos(yawRadians) * edgeDistance);

            if (!level.hasChunk(x >> 4, z >> 4)) {
                continue;
            }

            BlockPos feet = findStandingPosition(level, player, x, z);

            if (feet == null) {
                continue;
            }

            Vec3 position = Vec3.atBottomCenterOf(feet);

            if (hasClearView(level, player, position)) {
                return position;
            }
        }

        return null;
    }

    private static BlockPos findStandingPosition(
            ClientLevel level,
            LocalPlayer player,
            int x,
            int z
    ) {
        if (level.dimensionType().hasCeiling()) {
            int minimumY = level.getMinY() + 1;
            int maximumY = level.getMaxY() - 2;
            int originY = Mth.clamp(player.blockPosition().getY(), minimumY, maximumY);

            for (int offset = 0; offset <= ENCLOSED_VERTICAL_SEARCH_RADIUS; offset++) {
                int belowY = originY - offset;

                if (belowY >= minimumY) {
                    BlockPos belowCandidate = new BlockPos(x, belowY, z);

                    if (isValidStandingPosition(level, belowCandidate)) {
                        return belowCandidate;
                    }
                }

                if (offset == 0) {
                    continue;
                }

                int aboveY = originY + offset;

                if (aboveY <= maximumY) {
                    BlockPos aboveCandidate = new BlockPos(x, aboveY, z);

                    if (isValidStandingPosition(level, aboveCandidate)) {
                        return aboveCandidate;
                    }
                }
            }

            return null;
        }

        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos surface = new BlockPos(x, surfaceY, z);
        return isValidStandingPosition(level, surface) ? surface : null;
    }

    private static boolean isValidStandingPosition(ClientLevel level, BlockPos feet) {
        BlockPos head = feet.above();
        BlockPos ground = feet.below();

        return level.isInsideBuildHeight(feet)
                && level.isInsideBuildHeight(head)
                && level.isLoaded(feet)
                && level.getBlockState(feet).isAir()
                && level.getBlockState(head).isAir()
                && level.getFluidState(ground).isEmpty()
                && level.getBlockState(ground).isFaceSturdy(level, ground, Direction.UP);
    }

    private static boolean hasClearView(
            ClientLevel level,
            LocalPlayer player,
            Vec3 position
    ) {
        Vec3 target = position.add(0.0D, FIGURE_EYE_HEIGHT, 0.0D);
        HitResult hit = level.clip(new ClipContext(
                player.getEyePosition(),
                target,
                ClipContext.Block.VISUAL,
                ClipContext.Fluid.NONE,
                player
        ));

        return hit.getType() == HitResult.Type.MISS
                || hit.getLocation().distanceToSqr(target) < 0.25D;
    }

    private static boolean isLookingAtFigure(
            ClientLevel level,
            LocalPlayer player,
            Vec3 position
    ) {
        Vec3 targetOffset = position
                .add(0.0D, FIGURE_EYE_HEIGHT, 0.0D)
                .subtract(player.getEyePosition());
        double targetDistanceSqr = targetOffset.lengthSqr();

        if (targetDistanceSqr < 1.0E-6D) {
            return true;
        }

        double alignment = player.getViewVector(1.0F).dot(
                targetOffset.scale(1.0D / Math.sqrt(targetDistanceSqr))
        );
        return alignment >= SUSTAINED_LOOK_DOT_PRODUCT
                && hasClearView(level, player, position);
    }

    private static void submitCustomGeometry(SubmitCustomGeometryEvent event) {
        if (activeCue == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;

        if (level == null || player == null) {
            return;
        }

        ensureRenderResources(minecraft);

        Vec3 figure = activeCue.position();
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        double deltaX = player.getX() - figure.x;
        double deltaY = player.getEyeY() - (figure.y + FIGURE_EYE_HEIGHT);
        double deltaZ = player.getZ() - figure.z;
        double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        float bodyYaw = (float) Math.toDegrees(Math.atan2(-deltaX, deltaZ));
        float headPitch = (float) -Math.toDegrees(Math.atan2(deltaY, horizontalDistance));

        renderState.yRot = 0.0F;
        renderState.xRot = Mth.clamp(headPitch, -45.0F, 45.0F);
        renderState.ageInTicks = level.getGameTime();
        renderState.lightCoords = LightCoordsUtil.getLightCoords(
                level,
                BlockPos.containing(figure.add(0.0D, FIGURE_EYE_HEIGHT, 0.0D))
        );

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(figure.x - camera.x, figure.y - camera.y, figure.z - camera.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0F, -1.501F, 0.0F);

        event.getSubmitNodeCollector().submitModel(
                model,
                renderState,
                poseStack,
                renderType,
                renderState.lightCoords,
                OverlayTexture.NO_OVERLAY,
                -1,
                null
        );
        poseStack.popPose();

        submitPlayerShadow(event, minecraft, level, figure, camera);
    }

    private static void submitPlayerShadow(
            SubmitCustomGeometryEvent event,
            Minecraft minecraft,
            ClientLevel level,
            Vec3 figure,
            Vec3 camera
    ) {
        if (!minecraft.options.entityShadows().get()) {
            return;
        }

        BlockPos feet = BlockPos.containing(figure);
        BlockPos ground = feet.below();
        BlockState groundState = level.getBlockState(ground);
        int brightness = level.getMaxLocalRawBrightness(feet);

        if (brightness <= 3
                || groundState.getRenderShape() == RenderShape.INVISIBLE
                || !groundState.isCollisionShapeFullBlock(level, ground)) {
            return;
        }

        VoxelShape groundShape = groundState.getShape(level, ground);

        if (groundShape.isEmpty()) {
            return;
        }

        float alpha = 0.5F * Lightmap.getBrightness(level.dimensionType(), brightness);
        EntityRenderState.ShadowPiece shadow = new EntityRenderState.ShadowPiece(
                (float) (feet.getX() - figure.x),
                (float) (feet.getY() - figure.y),
                (float) (feet.getZ() - figure.z),
                groundShape,
                alpha
        );
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(figure.x - camera.x, figure.y - camera.y, figure.z - camera.z);
        event.getSubmitNodeCollector().submitShadow(
                poseStack,
                PLAYER_SHADOW_RADIUS,
                List.of(shadow)
        );
        poseStack.popPose();
    }

    private static void ensureRenderResources(Minecraft minecraft) {
        if (model != null) {
            return;
        }

        model = new PlayerModel(
                minecraft.getEntityModels().bakeLayer(ModelLayers.PLAYER),
                false
        );
        renderState = new AvatarRenderState();
        renderState.scale = 1.0F;
        renderState.ageScale = 1.0F;
        renderState.speedValue = 1.0F;
        renderState.pose = Pose.STANDING;
        renderState.mainArm = HumanoidArm.RIGHT;
        renderState.attackArm = HumanoidArm.RIGHT;
        renderState.leftArmPose = HumanoidModel.ArmPose.EMPTY;
        renderState.rightArmPose = HumanoidModel.ArmPose.EMPTY;
        renderState.showHat = false;
        renderState.showJacket = false;
        renderState.showLeftPants = false;
        renderState.showRightPants = false;
        renderState.showLeftSleeve = false;
        renderState.showRightSleeve = false;
        // Custom geometry must not participate in the entity outline pass. Doing so makes the
        // otherwise ordinary world-lit model look as if it has the glowing status effect.
        renderType = RenderTypes.entityCutout(RetoldGeneratedHorizonTexture.get(), false);
    }

    private record PendingCue(
            long phase,
            int durationTicks,
            int retries,
            int retryDelayTicks
    ) {
    }

    private record ActiveCue(
            Vec3 position,
            int remainingTicks,
            double approachDismissalDistanceSqr,
            int lookTicks
    ) {
    }
}
