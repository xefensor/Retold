package cz.xefensor.retold.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import cz.xefensor.retold.network.RetoldChronolithBeamPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Quaternionfc;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class RetoldChronolithBeamClient {
    private static final Identifier EXPERIENCE_ORB_TEXTURE =
            Identifier.withDefaultNamespace("textures/entity/experience/experience_orb.png");
    private static final RenderType EXPERIENCE_ORB_RENDER_TYPE =
            RenderTypes.entityTranslucentCullItemTarget(EXPERIENCE_ORB_TEXTURE);
    private static final double MAX_RENDER_DISTANCE_SQR = 96.0D * 96.0D;
    private static final int ORB_COUNT = 42;
    private static final float ORB_FLOW_SPEED_BLOCKS_PER_TICK = 0.34F;
    private static final double ORB_SPACING_BLOCKS = 0.24D;
    private static final double ORB_LOOP_LENGTH_BLOCKS = ORB_COUNT * ORB_SPACING_BLOCKS;
    private static final double ORB_LANE_SPREAD = 0.22D;
    private static final float RAMP_UP_TICKS = 30.0F;
    private static final float FADE_OUT_TICKS = 12.0F;
    private static final int FULL_BRIGHT_LIGHT = 15728880;
    private static final int EXPERIENCE_GREEN = 0x8CFF18;
    private static final int EXPERIENCE_YELLOW = 0xFFF84A;
    private static final int EXPERIENCE_WARM_YELLOW = 0xFFD545;

    private static final Map<UUID, Beam> ACTIVE_BEAMS = new HashMap<>();

    private RetoldChronolithBeamClient() {
    }

    public static void register(IEventBus modEventBus) {
        NeoForge.EVENT_BUS.addListener(RetoldChronolithBeamClient::submitCustomGeometry);
    }

    public static void handleSync(RetoldChronolithBeamPayload payload) {
        if (payload.active()) {
            ACTIVE_BEAMS.put(payload.playerId(), new Beam(
                    payload.pos(),
                    randomPhase(payload.playerId(), payload.pos()),
                    Float.NaN,
                    true,
                    Float.NaN
            ));
        } else if (ACTIVE_BEAMS.containsKey(payload.playerId())) {
            Beam beam = ACTIVE_BEAMS.get(payload.playerId());
            ACTIVE_BEAMS.put(payload.playerId(), new Beam(
                    beam.pos(),
                    beam.phase(),
                    beam.startRenderTime(),
                    false,
                    Float.NaN
            ));
        } else {
            ACTIVE_BEAMS.remove(payload.playerId());
        }
    }

    private static void submitCustomGeometry(SubmitCustomGeometryEvent event) {
        if (ACTIVE_BEAMS.isEmpty()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;

        if (level == null) {
            ACTIVE_BEAMS.clear();
            return;
        }

        Vec3 cameraPos = event.getLevelRenderState().cameraRenderState.pos;
        Quaternionfc cameraOrientation = event.getLevelRenderState().cameraRenderState.orientation;
        float partialTick = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        float renderTime = event.getLevelRenderState().gameTime + partialTick;
        Iterator<Map.Entry<UUID, Beam>> iterator = ACTIVE_BEAMS.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, Beam> entry = iterator.next();
            Player player = level.getPlayerByUUID(entry.getKey());
            Beam beam = entry.getValue();

            if (player == null || !player.isAlive()) {
                if (!beam.active()) {
                    iterator.remove();
                }

                continue;
            }

            if (Float.isNaN(beam.startRenderTime())) {
                beam = new Beam(beam.pos(), beam.phase(), renderTime, beam.active(), beam.stopRenderTime());
                entry.setValue(beam);
            }

            if (!beam.active() && Float.isNaN(beam.stopRenderTime())) {
                beam = new Beam(beam.pos(), beam.phase(), beam.startRenderTime(), false, renderTime);
                entry.setValue(beam);
            }

            float power = beamPower(beam, renderTime);

            if (power <= 0.0F) {
                iterator.remove();
                continue;
            }

            Vec3 start = interpolatedPlayerPosition(player, partialTick)
                    .add(0.0D, player.getBbHeight() * 0.55D, 0.0D);
            Vec3 end = Vec3.atCenterOf(beam.pos()).add(0.0D, 0.25D, 0.0D);
            float flowTime = renderTime - beam.startRenderTime();

            if (cameraPos.distanceToSqr(start) > MAX_RENDER_DISTANCE_SQR
                    && cameraPos.distanceToSqr(end) > MAX_RENDER_DISTANCE_SQR) {
                continue;
            }

            submitOrbStream(
                    event.getPoseStack(),
                    event.getSubmitNodeCollector(),
                    cameraPos,
                    cameraOrientation,
                    start,
                    end,
                    renderTime,
                    flowTime,
                    beam.phase(),
                    power
            );
        }
    }

    private static float beamPower(Beam beam, float renderTime) {
        if (beam.active()) {
            return Mth.clamp((renderTime - beam.startRenderTime()) / RAMP_UP_TICKS, 0.15F, 1.0F);
        }

        return 1.0F - Mth.clamp((renderTime - beam.stopRenderTime()) / FADE_OUT_TICKS, 0.0F, 1.0F);
    }

    private static Vec3 interpolatedPlayerPosition(Player player, float partialTick) {
        double x = Mth.lerp(partialTick, player.xo, player.getX());
        double y = Mth.lerp(partialTick, player.yo, player.getY());
        double z = Mth.lerp(partialTick, player.zo, player.getZ());

        return new Vec3(x, y, z);
    }

    private static void submitOrbStream(
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            Vec3 cameraPos,
            Quaternionfc cameraOrientation,
            Vec3 start,
            Vec3 end,
            float renderTime,
            float flowTime,
            float phase,
            float power
    ) {
        Vec3 streamVector = end.subtract(start);

        if (streamVector.lengthSqr() < 0.0001D) {
            return;
        }

        double streamLength = streamVector.length();
        double flowOffset = positiveModulo(
                flowTime * ORB_FLOW_SPEED_BLOCKS_PER_TICK + phase * ORB_LOOP_LENGTH_BLOCKS,
                ORB_LOOP_LENGTH_BLOCKS
        );
        Vec3 direction = streamVector.normalize();
        Vec3 referenceUp = Math.abs(direction.y) > 0.92D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 right = direction.cross(referenceUp).normalize();
        Vec3 localUp = right.cross(direction).normalize();
        int orbCount = Mth.ceil(ORB_COUNT * (0.25F + 0.75F * power));

        for (int index = 0; index < orbCount; index++) {
            float seed = phase * 1000.0F + index * 19.37F;
            double distanceFromPlayer = positiveModulo(
                    index * ORB_SPACING_BLOCKS + flowOffset,
                    ORB_LOOP_LENGTH_BLOCKS
            );

            if (distanceFromPlayer > streamLength) {
                continue;
            }

            double progress = distanceFromPlayer / streamLength;
            double laneAngle = stableUnit(seed) * Math.PI * 2.0D + renderTime * 0.08D;
            double laneRadius = ORB_LANE_SPREAD * power * (0.25D + stableUnit(seed + 4.0F) * 0.75D);
            Vec3 laneOffset = right.scale(Math.cos(laneAngle) * laneRadius)
                    .add(localUp.scale(Math.sin(laneAngle) * laneRadius));
            Vec3 orbPos = start.lerp(end, progress).add(laneOffset);
            int icon = (int) (stableUnit(seed + 2.0F) * 8.0F);
            float size = (0.12F + stableUnit(seed + 1.0F) * 0.14F)
                    * (0.82F + 0.18F * Mth.sin(renderTime * 0.25F + seed));
            int color = cyclingExperienceTint(renderTime + seed);
            int alpha = (int) ((165 + stableUnit(seed + 3.0F) * 70.0F) * edgeFade(progress) * power);

            submitOrb(
                    poseStack,
                    submitNodeCollector,
                    cameraPos,
                    cameraOrientation,
                    orbPos,
                    size,
                    icon,
                    color,
                    alpha
            );
        }
    }

    private static double edgeFade(double progress) {
        double fadeIn = Mth.clamp(progress / 0.12D, 0.0D, 1.0D);
        double fadeOut = Mth.clamp((1.0D - progress) / 0.12D, 0.0D, 1.0D);

        return fadeIn * fadeOut;
    }

    private static void submitOrb(
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            Vec3 cameraPos,
            Quaternionfc cameraOrientation,
            Vec3 orbPos,
            float size,
            int icon,
            int color,
            int alpha
    ) {
        float u0 = (icon % 4 * 16) / 64.0F;
        float u1 = (icon % 4 * 16 + 16) / 64.0F;
        float v0 = (icon / 4 * 16) / 64.0F;
        float v1 = (icon / 4 * 16 + 16) / 64.0F;
        int red = color >> 16 & 255;
        int green = color >> 8 & 255;
        int blue = color & 255;

        poseStack.pushPose();
        poseStack.translate(orbPos.x - cameraPos.x, orbPos.y - cameraPos.y, orbPos.z - cameraPos.z);
        poseStack.mulPose(cameraOrientation);
        poseStack.scale(size, size, size);
        submitNodeCollector.submitCustomGeometry(poseStack, EXPERIENCE_ORB_RENDER_TYPE, (pose, buffer) -> {
            vertex(buffer, pose, -0.5F, -0.25F, red, green, blue, alpha, u0, v1);
            vertex(buffer, pose, 0.5F, -0.25F, red, green, blue, alpha, u1, v1);
            vertex(buffer, pose, 0.5F, 0.75F, red, green, blue, alpha, u1, v0);
            vertex(buffer, pose, -0.5F, 0.75F, red, green, blue, alpha, u0, v0);
        });
        poseStack.popPose();
    }

    private static int cyclingExperienceTint(float time) {
        float cycle = (time * 0.055F) % 1.0F;

        if (cycle < 0.45F) {
            return lerpColor(EXPERIENCE_GREEN, EXPERIENCE_YELLOW, cycle / 0.45F);
        }

        if (cycle < 0.6F) {
            return lerpColor(EXPERIENCE_YELLOW, EXPERIENCE_WARM_YELLOW, (cycle - 0.45F) / 0.15F);
        }

        return lerpColor(EXPERIENCE_WARM_YELLOW, EXPERIENCE_GREEN, (cycle - 0.6F) / 0.4F);
    }

    private static int lerpColor(int from, int to, float amount) {
        int fromRed = from >> 16 & 255;
        int fromGreen = from >> 8 & 255;
        int fromBlue = from & 255;
        int toRed = to >> 16 & 255;
        int toGreen = to >> 8 & 255;
        int toBlue = to & 255;
        int red = Mth.lerpInt(amount, fromRed, toRed);
        int green = Mth.lerpInt(amount, fromGreen, toGreen);
        int blue = Mth.lerpInt(amount, fromBlue, toBlue);

        return red << 16 | green << 8 | blue;
    }

    private static float randomPhase(UUID playerId, BlockPos pos) {
        long hash = playerId.getMostSignificantBits()
                ^ playerId.getLeastSignificantBits()
                ^ pos.asLong() * 31L;

        return Math.floorMod(hash, 10_000L) / 10_000.0F;
    }

    private static float stableUnit(float seed) {
        double value = Math.sin(seed * 12.9898D) * 43758.5453D;

        return (float) (value - Math.floor(value));
    }

    private static double positiveModulo(double value, double modulus) {
        double result = value % modulus;

        return result < 0.0D ? result + modulus : result;
    }

    private static void vertex(
            VertexConsumer builder,
            PoseStack.Pose pose,
            float x,
            float y,
            int red,
            int green,
            int blue,
            int alpha,
            float u,
            float v
    ) {
        builder.addVertex(pose, x, y, 0.0F)
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(FULL_BRIGHT_LIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    private record Beam(BlockPos pos, float phase, float startRenderTime, boolean active, float stopRenderTime) {
    }
}
