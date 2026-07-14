package cz.xefensor.retold.worldgen.air.wind;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class AirTempleWindZone {
    private static final int DIRECTION_INTERVAL_TICKS = 400;
    private static final double PUSH_STRENGTH = 0.075D;
    private static final double MAX_SPEED_ALONG_WIND = 1.15D;
    private static final double MAX_WIND_SHIELD_DISTANCE = 8.0D;
    private static final int PARTICLE_COUNT = 18;
    private static final int PARTICLE_ATTEMPT_MULTIPLIER = 3;
    private static final double PARTICLE_RADIUS = 14.0D;
    private static final double PARTICLE_SPEED = 0.85D;
    private static final double PARTICLE_VISIBLE_TRAVEL_DISTANCE = 10.0D;

    private final AirTempleWindSource source;

    public AirTempleWindZone(AirTempleWindSource source) {
        this.source = source;
    }

    public boolean contains(Entity entity) {
        return source.bounds().intersects(entity.getBoundingBox());
    }

    public void apply(ServerLevel level, Entity entity) {
        Vec3 direction = direction(level.getGameTime());

        if (isProtectedByUpwindBlock(level, entity, direction)) {
            return;
        }

        double speedAlongWind = entity.getDeltaMovement().dot(direction);

        if (speedAlongWind >= MAX_SPEED_ALONG_WIND) {
            return;
        }

        entity.push(direction.scale(PUSH_STRENGTH));

        if (entity instanceof ServerPlayer player) {
            player.connection.send(new ClientboundSetEntityMotionPacket(player));
        }
    }

    public void emitParticles(ServerLevel level, ServerPlayer player, RandomSource random) {
        Vec3 direction = direction(level.getGameTime());
        int emitted = 0;
        int attempts = 0;

        while (emitted < PARTICLE_COUNT && attempts++ < PARTICLE_COUNT * PARTICLE_ATTEMPT_MULTIPLIER) {
            double x = clamp(
                    player.getX() + randomBetween(random, -PARTICLE_RADIUS, PARTICLE_RADIUS),
                    source.bounds().minX,
                    source.bounds().maxX
            );
            double y = clamp(
                    player.getY() + randomBetween(random, -3.0D, 7.0D),
                    source.bounds().minY,
                    source.bounds().maxY
            );
            double z = clamp(
                    player.getZ() + randomBetween(random, -PARTICLE_RADIUS, PARTICLE_RADIUS),
                    source.bounds().minZ,
                    source.bounds().maxZ
            );
            Vec3 particlePos = new Vec3(x, y, z);

            if (isBlockedParticle(level, player, particlePos, direction)) {
                continue;
            }

            level.sendParticles(
                    ParticleTypes.CLOUD,
                    x,
                    y,
                    z,
                    0,
                    direction.x,
                    0.0D,
                    direction.z,
                    PARTICLE_SPEED
            );
            emitted++;
        }
    }

    private boolean isProtectedByUpwindBlock(ServerLevel level, Entity entity, Vec3 windDirection) {
        Vec3 upwind = windDirection.scale(-1.0D);
        double traceDistance = shieldTraceDistance(entity, upwind);
        double minY = entity.getBoundingBox().minY + 0.15D;
        double midY = (entity.getBoundingBox().minY + entity.getBoundingBox().maxY) * 0.5D;
        double maxY = entity.getBoundingBox().maxY - 0.1D;

        return trace(level, entity, new Vec3(entity.getX(), minY, entity.getZ()), upwind, traceDistance)
                && trace(level, entity, new Vec3(entity.getX(), midY, entity.getZ()), upwind, traceDistance)
                && trace(level, entity, new Vec3(entity.getX(), maxY, entity.getZ()), upwind, traceDistance);
    }

    private double shieldTraceDistance(Entity entity, Vec3 upwind) {
        return Math.min(
                distanceToSourceEdge(entity.position(), upwind) + 1.0D,
                MAX_WIND_SHIELD_DISTANCE
        );
    }

    private boolean trace(
            ServerLevel level,
            Entity entity,
            Vec3 from,
            Vec3 upwind,
            double distance
    ) {
        Vec3 to = from.add(upwind.scale(distance));
        BlockHitResult hit = level.clip(new ClipContext(
                from,
                to,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                entity
        ));

        return hit.getType() == HitResult.Type.BLOCK;
    }

    private boolean isBlockedParticle(
            ServerLevel level,
            Entity entity,
            Vec3 particlePos,
            Vec3 windDirection
    ) {
        BlockPos blockPos = BlockPos.containing(particlePos);

        if (!level.getBlockState(blockPos).isAir() || !level.getFluidState(blockPos).isEmpty()) {
            return true;
        }

        Vec3 upwind = windDirection.scale(-1.0D);
        double upwindTraceDistance = Math.min(
                distanceToSourceEdge(particlePos, upwind) + 1.0D,
                MAX_WIND_SHIELD_DISTANCE
        );
        double downwindTraceDistance = Math.min(
                distanceToSourceEdge(particlePos, windDirection) + 1.0D,
                PARTICLE_VISIBLE_TRAVEL_DISTANCE
        );

        return particleTraceHitsBlock(level, entity, particlePos, upwind, upwindTraceDistance)
                || particleTraceHitsBlock(level, entity, particlePos, windDirection, downwindTraceDistance);
    }

    private boolean particleTraceHitsBlock(
            ServerLevel level,
            Entity entity,
            Vec3 particlePos,
            Vec3 direction,
            double distance
    ) {
        if (distance <= 0.0D) {
            return false;
        }

        BlockHitResult hit = level.clip(new ClipContext(
                particlePos,
                particlePos.add(direction.scale(distance)),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                entity
        ));

        return hit.getType() == HitResult.Type.BLOCK;
    }

    private double distanceToSourceEdge(Vec3 pos, Vec3 upwind) {
        if (upwind.x > 0.0D) {
            return source.bounds().maxX - pos.x;
        }

        if (upwind.x < 0.0D) {
            return pos.x - source.bounds().minX;
        }

        if (upwind.z > 0.0D) {
            return source.bounds().maxZ - pos.z;
        }

        return pos.z - source.bounds().minZ;
    }

    private static Vec3 direction(long gameTime) {
        return switch ((int) ((gameTime / DIRECTION_INTERVAL_TICKS) & 3L)) {
            case 0 -> new Vec3(1.0D, 0.0D, 0.0D);
            case 1 -> new Vec3(0.0D, 0.0D, 1.0D);
            case 2 -> new Vec3(-1.0D, 0.0D, 0.0D);
            default -> new Vec3(0.0D, 0.0D, -1.0D);
        };
    }

    private static double randomBetween(RandomSource random, double min, double max) {
        return min + random.nextDouble() * (max - min);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
