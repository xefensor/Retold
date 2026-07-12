package cz.xefensor.retold.worldgen.air;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public record AirTempleWindZone(
        AABB bounds,
        Vec3 impulse,
        double maxSpeedAlongWind,
        boolean blockedByWalls,
        int pulsePeriodTicks,
        int pulsePhaseTicks,
        double minPulseStrength
) {
    private static final int SHIELD_TRACE_BLOCKS = 96;
    private static final double SHIELD_TRACE_STEP = 0.5D;
    private static final int PARTICLES_PER_BURST = 12;
    private static final double PARTICLE_SPEED = 1.1D;

    public void apply(ServerLevel level, Entity entity) {
        Vec3 currentImpulse = currentImpulse(level.getGameTime());
        Vec3 direction = currentImpulse.normalize();

        if (!direction.isFinite()) {
            return;
        }

        double shieldMultiplier = blockedByWalls
                ? shieldMultiplier(level, entity, direction)
                : 1.0D;

        if (shieldMultiplier <= 0.0D) {
            return;
        }

        Vec3 adjustedImpulse = currentImpulse.scale(shieldMultiplier);
        double speedAlongWind = entity.getDeltaMovement().dot(direction);

        if (speedAlongWind >= maxSpeedAlongWind) {
            return;
        }

        entity.push(adjustedImpulse);

        if (entity instanceof ServerPlayer player) {
            player.connection.send(new ClientboundSetEntityMotionPacket(player));
        }
    }

    public void emitParticles(ServerLevel level, RandomSource random) {
        Vec3 currentImpulse = currentImpulse(level.getGameTime());
        Vec3 direction = currentImpulse.normalize();

        if (!direction.isFinite()) {
            return;
        }

        for (int i = 0; i < PARTICLES_PER_BURST; i++) {
            double x = randomBetween(random, bounds.minX, bounds.maxX);
            double y = randomBetween(random, bounds.minY, bounds.maxY);
            double z = randomBetween(random, bounds.minZ, bounds.maxZ);
            double shieldMultiplier = blockedByWalls
                    ? shieldMultiplier(level, x, y, z, direction)
                    : 1.0D;

            if (shieldMultiplier <= 0.0D) {
                continue;
            }

            if (shieldMultiplier < 1.0D && random.nextDouble() > shieldMultiplier) {
                continue;
            }

            level.sendParticles(
                    ParticleTypes.CLOUD,
                    x,
                    y,
                    z,
                    0,
                    direction.x,
                    direction.y,
                    direction.z,
                    PARTICLE_SPEED * currentPulseStrength(level.getGameTime()) * Math.max(0.35D, shieldMultiplier)
            );
        }
    }

    private Vec3 currentImpulse(long gameTime) {
        return impulse.scale(currentPulseStrength(gameTime));
    }

    private double currentPulseStrength(long gameTime) {
        if (pulsePeriodTicks <= 0) {
            return 1.0D;
        }

        double angle = ((gameTime + pulsePhaseTicks) % pulsePeriodTicks)
                / (double) pulsePeriodTicks
                * Math.PI
                * 2.0D;
        double wave = (Math.sin(angle) + 1.0D) * 0.5D;

        return minPulseStrength + (1.0D - minPulseStrength) * wave;
    }

    private static double shieldMultiplier(
            ServerLevel level,
            Entity entity,
            Vec3 windDirection
    ) {
        if (Math.abs(windDirection.y) > 0.35D) {
            return 1.0D;
        }

        Vec3 upwind = windDirection.scale(-1.0D);

        return shieldMultiplier(
                level,
                entity.getX(),
                entity.getY(),
                entity.getZ(),
                upwind,
                new double[]{0.15D, 1.15D}
        );
    }

    private static double shieldMultiplier(
            ServerLevel level,
            double x,
            double y,
            double z,
            Vec3 windDirection
    ) {
        if (Math.abs(windDirection.y) > 0.35D) {
            return 1.0D;
        }

        Vec3 upwind = windDirection.scale(-1.0D);

        return shieldMultiplier(
                level,
                x,
                y,
                z,
                upwind,
                new double[]{0.0D}
        );
    }

    private static double shieldMultiplier(
            ServerLevel level,
            double x,
            double y,
            double z,
            Vec3 upwind,
            double[] yOffsets
    ) {
        for (double distance = SHIELD_TRACE_STEP;
             distance <= SHIELD_TRACE_BLOCKS;
             distance += SHIELD_TRACE_STEP) {
            for (double yOffset : yOffsets) {
                BlockPos pos = BlockPos.containing(
                        x + upwind.x * distance,
                        y + yOffset,
                        z + upwind.z * distance
                );

                if (level.getBlockState(pos).blocksMotion()) {
                    return 0.0D;
                }
            }
        }

        return 1.0D;
    }

    private static double randomBetween(RandomSource random, double min, double max) {
        return min + random.nextDouble() * (max - min);
    }
}
