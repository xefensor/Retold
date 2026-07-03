package cz.xefensor.retold.aender.stability;

import cz.xefensor.retold.aender.RetoldAenderDimensions;
import cz.xefensor.retold.aender.generation.AenderVolatility;
import cz.xefensor.retold.registry.RetoldBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class AenderStabilizerEvents {
    private static final int WALL_INTERVAL_TICKS = 4;
    private static final int WALL_VIEW_RADIUS_CHUNKS = 8;

    // Random floating membrane points.
    private static final int FLOATING_PARTICLES_PER_16_BLOCKS = 128;

    // Soft organic particle clusters.
    private static final int EDDIES_PER_16_BLOCKS = 3;
    private static final int PARTICLES_PER_EDDY = 7;

    private static final double WALL_THICKNESS = 1.10D;

    private static final DustParticleOptions GREEN_WALL_PARTICLE =
            new DustParticleOptions(0x26FF40, 1.00F);

    private static final DustParticleOptions BRIGHT_GREEN_WALL_PARTICLE =
            new DustParticleOptions(0x9DFF75, 1.22F);

    private static final DustParticleOptions CYAN_GREEN_WALL_PARTICLE =
            new DustParticleOptions(0x00FFC8, 0.82F);

    private static int wallTick;

    private AenderStabilizerEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.isCanceled()) {
            return;
        }

        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        if (level.dimension() != RetoldAenderDimensions.AENDER) {
            return;
        }

        if (!event.getPlacedBlock().is(RetoldBlocks.AENDER_STABILIZER.get())) {
            return;
        }

        AenderStabilityData.get(level).addStabilizer(chunkOf(event.getPos()));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBreak(BreakBlockEvent event) {
        if (event.isCanceled()) {
            return;
        }

        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        if (level.dimension() != RetoldAenderDimensions.AENDER) {
            return;
        }

        if (!level.getBlockState(event.getPos()).is(RetoldBlocks.AENDER_STABILIZER)) {
            return;
        }

        ChunkPos center = chunkOf(event.getPos());

        markLoadedHaloAsCurrent(level, center);

        AenderStabilityData.get(level).removeStabilizer(center);
    }

    private static void markLoadedHaloAsCurrent(ServerLevel level, ChunkPos center) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int chunkX = center.x() + dx;
                int chunkZ = center.z() + dz;

                ChunkAccess chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);

                if (chunk == null) {
                    continue;
                }

                AenderVolatility.retainForChunk(chunk);
                AenderVolatility.markGenerated(chunk);
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        wallTick++;

        if (wallTick % WALL_INTERVAL_TICKS != 0) {
            return;
        }

        ServerLevel level = event.getServer().getLevel(RetoldAenderDimensions.AENDER);

        if (level == null) {
            return;
        }

        AenderStabilityData stabilityData = AenderStabilityData.get(level);

        for (ServerPlayer player : level.players()) {
            showMergedOuterForcefield(level, stabilityData, player);
        }
    }

    private static void showMergedOuterForcefield(
            ServerLevel level,
            AenderStabilityData stabilityData,
            ServerPlayer player
    ) {
        ChunkPos playerChunk = player.chunkPosition();

        int minBuildY = level.getMinY();
        int maxBuildY = level.getMinY() + level.getHeight() - 1;

        Map<Integer, List<HorizontalSegment>> horizontalSegmentsByZ = new HashMap<>();
        Map<Integer, List<VerticalSegment>> verticalSegmentsByX = new HashMap<>();

        for (int dx = -WALL_VIEW_RADIUS_CHUNKS; dx <= WALL_VIEW_RADIUS_CHUNKS; dx++) {
            for (int dz = -WALL_VIEW_RADIUS_CHUNKS; dz <= WALL_VIEW_RADIUS_CHUNKS; dz++) {
                int chunkX = playerChunk.x() + dx;
                int chunkZ = playerChunk.z() + dz;

                if (!stabilityData.isStable(new ChunkPos(chunkX, chunkZ))) {
                    continue;
                }

                int minX = chunkX << 4;
                int minZ = chunkZ << 4;
                int maxX = minX + 16;
                int maxZ = minZ + 16;

                // Only outer edges. Inner 3x3 chunk seams are ignored.
                if (!isStable(stabilityData, chunkX, chunkZ - 1)) {
                    horizontalSegmentsByZ
                            .computeIfAbsent(minZ, ignored -> new ArrayList<>())
                            .add(new HorizontalSegment(minX, maxX));
                }

                if (!isStable(stabilityData, chunkX, chunkZ + 1)) {
                    horizontalSegmentsByZ
                            .computeIfAbsent(maxZ, ignored -> new ArrayList<>())
                            .add(new HorizontalSegment(minX, maxX));
                }

                if (!isStable(stabilityData, chunkX - 1, chunkZ)) {
                    verticalSegmentsByX
                            .computeIfAbsent(minX, ignored -> new ArrayList<>())
                            .add(new VerticalSegment(minZ, maxZ));
                }

                if (!isStable(stabilityData, chunkX + 1, chunkZ)) {
                    verticalSegmentsByX
                            .computeIfAbsent(maxX, ignored -> new ArrayList<>())
                            .add(new VerticalSegment(minZ, maxZ));
                }
            }
        }

        spawnMergedHorizontalWalls(level, horizontalSegmentsByZ, minBuildY, maxBuildY);
        spawnMergedVerticalWalls(level, verticalSegmentsByX, minBuildY, maxBuildY);
    }

    private static boolean isStable(AenderStabilityData stabilityData, int chunkX, int chunkZ) {
        return stabilityData.isStable(new ChunkPos(chunkX, chunkZ));
    }

    private static void spawnMergedHorizontalWalls(
            ServerLevel level,
            Map<Integer, List<HorizontalSegment>> segmentsByZ,
            int minBuildY,
            int maxBuildY
    ) {
        for (Map.Entry<Integer, List<HorizontalSegment>> entry : segmentsByZ.entrySet()) {
            int z = entry.getKey();
            List<HorizontalSegment> segments = entry.getValue();

            segments.sort(Comparator.comparingInt(HorizontalSegment::startX));

            int runStart = Integer.MIN_VALUE;
            int runEnd = Integer.MIN_VALUE;

            for (HorizontalSegment segment : segments) {
                if (runStart == Integer.MIN_VALUE) {
                    runStart = segment.startX();
                    runEnd = segment.endX();
                    continue;
                }

                if (segment.startX() <= runEnd) {
                    runEnd = Math.max(runEnd, segment.endX());
                    continue;
                }

                spawnOrganicWallRun(level, true, z, runStart, runEnd, minBuildY, maxBuildY);

                runStart = segment.startX();
                runEnd = segment.endX();
            }

            if (runStart != Integer.MIN_VALUE) {
                spawnOrganicWallRun(level, true, z, runStart, runEnd, minBuildY, maxBuildY);
            }
        }
    }

    private static void spawnMergedVerticalWalls(
            ServerLevel level,
            Map<Integer, List<VerticalSegment>> segmentsByX,
            int minBuildY,
            int maxBuildY
    ) {
        for (Map.Entry<Integer, List<VerticalSegment>> entry : segmentsByX.entrySet()) {
            int x = entry.getKey();
            List<VerticalSegment> segments = entry.getValue();

            segments.sort(Comparator.comparingInt(VerticalSegment::startZ));

            int runStart = Integer.MIN_VALUE;
            int runEnd = Integer.MIN_VALUE;

            for (VerticalSegment segment : segments) {
                if (runStart == Integer.MIN_VALUE) {
                    runStart = segment.startZ();
                    runEnd = segment.endZ();
                    continue;
                }

                if (segment.startZ() <= runEnd) {
                    runEnd = Math.max(runEnd, segment.endZ());
                    continue;
                }

                spawnOrganicWallRun(level, false, x, runStart, runEnd, minBuildY, maxBuildY);

                runStart = segment.startZ();
                runEnd = segment.endZ();
            }

            if (runStart != Integer.MIN_VALUE) {
                spawnOrganicWallRun(level, false, x, runStart, runEnd, minBuildY, maxBuildY);
            }
        }
    }

    private static void spawnOrganicWallRun(
            ServerLevel level,
            boolean horizontal,
            int fixedCoord,
            int runStart,
            int runEnd,
            int minBuildY,
            int maxBuildY
    ) {
        int length = Math.max(1, runEnd - runStart);
        double height = Math.max(1.0D, maxBuildY - minBuildY);
        long gameTime = level.getGameTime();

        long runSeed = hashRun(horizontal, fixedCoord, runStart, runEnd);

        int floatingCount = Math.max(36, length * FLOATING_PARTICLES_PER_16_BLOCKS / 16);
        int eddyCount = Math.max(2, length * EDDIES_PER_16_BLOCKS / 16);

        // Main membrane: random floating points.
        // No downward flow, no straight streams. Every point just slowly wanders.
        for (int i = 0; i < floatingCount; i++) {
            long pointSeed = runSeed ^ ((long) i * 0x9E3779B97F4A7C15L);

            double lifeLength = 90.0D + stableUnit(pointSeed ^ 0x01L) * 140.0D;
            double lifeOffset = stableUnit(pointSeed ^ 0x02L) * lifeLength;
            double lifeAge = positiveModulo(gameTime + lifeOffset, lifeLength);
            double life01 = lifeAge / lifeLength;

            // Soft appear/disappear gate, but without alpha.
            double visibility = Math.sin(life01 * Math.PI);
            if (visibility < 0.23D && stableUnit(pointSeed ^ (gameTime / 20L)) > visibility * 3.8D) {
                continue;
            }

            double baseAlong = stableUnit(pointSeed ^ 0x11L) * length;
            double baseY = minBuildY + stableUnit(pointSeed ^ 0x12L) * height;

            double phaseA = stableUnit(pointSeed ^ 0x21L) * Math.PI * 2.0D;
            double phaseB = stableUnit(pointSeed ^ 0x22L) * Math.PI * 2.0D;
            double phaseC = stableUnit(pointSeed ^ 0x23L) * Math.PI * 2.0D;
            double phaseD = stableUnit(pointSeed ^ 0x24L) * Math.PI * 2.0D;

            // Random-looking but smooth organic wandering.
            double along =
                    baseAlong +
                            Math.sin(gameTime * 0.012D + phaseA) * 3.2D +
                            Math.sin(gameTime * 0.027D + phaseB) * 1.15D +
                            Math.sin(baseY * 0.019D + gameTime * 0.006D + phaseC) * 1.8D;

            double y =
                    baseY +
                            Math.sin(gameTime * 0.010D + phaseC) * 5.2D +
                            Math.sin(gameTime * 0.021D + phaseD) * 1.7D +
                            Math.sin(baseAlong * 0.041D + gameTime * 0.007D + phaseA) * 2.4D;

            along = positiveModulo(along, length);
            y = minBuildY + positiveModulo(y - minBuildY, height);

            double energy =
                    0.5D +
                            0.5D * Math.sin(
                                    gameTime * 0.026D +
                                            baseAlong * 0.083D +
                                            baseY * 0.017D +
                                            phaseB
                            );

            ParticleOptions particle = pickOrganicParticle(energy, stableUnit(pointSeed ^ 0x31L));

            spawnParticleOnOrganicRun(
                    level,
                    particle,
                    horizontal,
                    fixedCoord,
                    runStart,
                    along,
                    y,
                    pointSeed,
                    gameTime,
                    1.0D
            );
        }

        // Organic eddies: soft moving clusters, not lines.
        for (int e = 0; e < eddyCount; e++) {
            long eddySeed = runSeed ^ ((long) e * 0xD1B54A32D192ED03L);

            double centerAlong =
                    stableUnit(eddySeed ^ 0x41L) * length +
                            Math.sin(gameTime * 0.009D + stableUnit(eddySeed ^ 0x42L) * Math.PI * 2.0D) * 4.5D;

            double centerY =
                    minBuildY +
                            stableUnit(eddySeed ^ 0x43L) * height +
                            Math.sin(gameTime * 0.008D + stableUnit(eddySeed ^ 0x44L) * Math.PI * 2.0D) * 8.0D;

            centerAlong = positiveModulo(centerAlong, length);
            centerY = minBuildY + positiveModulo(centerY - minBuildY, height);

            double eddyRadiusAlong = 2.8D + stableUnit(eddySeed ^ 0x45L) * 4.5D;
            double eddyRadiusY = 3.2D + stableUnit(eddySeed ^ 0x46L) * 7.5D;
            double spin = gameTime * (0.012D + stableUnit(eddySeed ^ 0x47L) * 0.018D);

            for (int p = 0; p < PARTICLES_PER_EDDY; p++) {
                long pointSeed = eddySeed ^ ((long) p * 0x94D049BB133111EBL);

                double angle =
                        stableUnit(pointSeed ^ 0x51L) * Math.PI * 2.0D +
                                spin +
                                Math.sin(gameTime * 0.011D + stableUnit(pointSeed ^ 0x52L) * Math.PI * 2.0D) * 0.65D;

                double radius =
                        0.25D +
                                stableUnit(pointSeed ^ 0x53L) * 0.95D +
                                Math.sin(gameTime * 0.015D + stableUnit(pointSeed ^ 0x54L) * Math.PI * 2.0D) * 0.18D;

                double along = centerAlong + Math.cos(angle) * eddyRadiusAlong * radius;
                double y = centerY + Math.sin(angle) * eddyRadiusY * radius;

                along = positiveModulo(along, length);
                y = minBuildY + positiveModulo(y - minBuildY, height);

                ParticleOptions particle =
                        p == 0 ? CYAN_GREEN_WALL_PARTICLE :
                                stableUnit(pointSeed ^ 0x55L) < 0.45D ? BRIGHT_GREEN_WALL_PARTICLE : GREEN_WALL_PARTICLE;

                spawnParticleOnOrganicRun(
                        level,
                        particle,
                        horizontal,
                        fixedCoord,
                        runStart,
                        along,
                        y,
                        pointSeed,
                        gameTime,
                        1.35D
                );
            }
        }
    }

    private static ParticleOptions pickOrganicParticle(double energy, double roll) {
        if (energy > 0.92D || roll < 0.04D) {
            return CYAN_GREEN_WALL_PARTICLE;
        }

        if (energy > 0.70D || roll < 0.18D) {
            return BRIGHT_GREEN_WALL_PARTICLE;
        }

        return GREEN_WALL_PARTICLE;
    }

    private static void spawnParticleOnOrganicRun(
            ServerLevel level,
            ParticleOptions particle,
            boolean horizontal,
            int fixedCoord,
            int runStart,
            double along,
            double y,
            long pointSeed,
            long gameTime,
            double movementScale
    ) {
        double phaseA = stableUnit(pointSeed ^ 0x61L) * Math.PI * 2.0D;
        double phaseB = stableUnit(pointSeed ^ 0x62L) * Math.PI * 2.0D;
        double phaseC = stableUnit(pointSeed ^ 0x63L) * Math.PI * 2.0D;

        // This is membrane wobble, not falling.
        double wallOffset =
                Math.sin(gameTime * 0.014D + along * 0.079D + phaseA) * 0.34D +
                        Math.sin(gameTime * 0.006D + y * 0.023D + phaseB) * 0.28D +
                        Math.sin(gameTime * 0.019D + along * 0.031D + y * 0.011D + phaseC) * 0.18D;

        wallOffset *= movementScale;
        wallOffset += (stableUnit(pointSeed ^ 0x64L) - 0.5D) * WALL_THICKNESS * 0.55D;

        double alongOffset =
                Math.sin(gameTime * 0.010D + y * 0.033D + phaseB) * 0.38D +
                        Math.sin(gameTime * 0.017D + along * 0.044D + phaseC) * 0.30D;

        double x;
        double z;

        if (horizontal) {
            x = runStart + along + alongOffset;
            z = fixedCoord + wallOffset;
        } else {
            x = fixedCoord + wallOffset;
            z = runStart + along + alongOffset;
        }

        level.sendParticles(
                particle,
                x,
                y + 0.5D,
                z,
                1,
                0.0D,
                0.0D,
                0.0D,
                0.0D
        );
    }

    private static long hashRun(boolean horizontal, int fixedCoord, int runStart, int runEnd) {
        long h = horizontal ? 0x51A7D3C2B19E45L : 0x6B8F23A91D4C77L;

        h ^= (long) fixedCoord * 0x632BE59BD9B4E019L;
        h ^= (long) runStart * 0x85157AF5L;
        h ^= (long) runEnd * 0x94D049BBL;

        h ^= h >>> 33;
        h *= 0xff51afd7ed558ccdL;
        h ^= h >>> 33;
        h *= 0xc4ceb9fe1a85ec53L;
        h ^= h >>> 33;

        return h;
    }

    private static double stableUnit(long value) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= value >>> 33;

        return (value >>> 11) * 0x1.0p-53;
    }

    private static double positiveModulo(double value, double mod) {
        double result = value % mod;
        return result < 0.0D ? result + mod : result;
    }

    private static ChunkPos chunkOf(BlockPos pos) {
        return new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4);
    }

    private record HorizontalSegment(int startX, int endX) {
    }

    private record VerticalSegment(int startZ, int endZ) {
    }
}