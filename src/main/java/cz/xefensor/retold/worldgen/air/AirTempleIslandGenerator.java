package cz.xefensor.retold.worldgen.air;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

final class AirTempleIslandGenerator {
    private static final int[][] SATELLITES = {
            {30, 7, -4, 8},
            {-27, 24, 0, 7},
            {12, -33, 3, 6}
    };

    private AirTempleIslandGenerator() {
    }

    static void generate(
            WorldGenLevel level,
            BoundingBox chunkBB,
            int centerX,
            int centerZ,
            int groundY,
            int islandY,
            AirTemplePalette palette
    ) {
        generateCrater(level, chunkBB, centerX, centerZ, groundY, palette);
        generateMainIsland(level, chunkBB, centerX, centerZ, islandY, palette);

        for (int[] satellite : SATELLITES) {
            generateSatellite(
                    level,
                    chunkBB,
                    centerX + satellite[0],
                    centerZ + satellite[1],
                    islandY + satellite[2],
                    satellite[3],
                    palette
            );
        }
    }

    private static void generateCrater(
            WorldGenLevel level,
            BoundingBox chunkBB,
            int centerX,
            int centerZ,
            int groundY,
            AirTemplePalette palette
    ) {
        int radius = AirTempleDimensions.CRATER_RADIUS;
        int radiusSq = radius * radius;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int distanceSq = dx * dx + dz * dz;

                if (distanceSq > radiusSq) {
                    continue;
                }

                int x = centerX + dx;
                int z = centerZ + dz;
                int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;

                if (surfaceY < level.getMinY()) {
                    continue;
                }

                double normalized = Math.sqrt(distanceSq) / radius;
                int roughness = AirTempleBlocks.noise2(x, z, 5519) % 3;

                if (normalized > 0.84D) {
                    AirTempleBlocks.place(level, chunkBB, new BlockPos(x, surfaceY, z), palette.craterOuter());
                    continue;
                }

                int depth = 2 + (int) Math.round((1.0D - normalized) * (1.0D - normalized) * AirTempleDimensions.CRATER_MAX_DEPTH) + roughness;
                int floorY = Math.max(level.getMinY(), surfaceY - depth);

                for (int y = floorY + 1; y <= surfaceY + 1; y++) {
                    AirTempleBlocks.place(level, chunkBB, new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
                }

                AirTempleBlocks.place(
                        level,
                        chunkBB,
                        new BlockPos(x, floorY, z),
                        normalized < 0.42D ? palette.craterInner() : palette.craterMiddle()
                );
            }
        }
    }

    private static void generateMainIsland(
            WorldGenLevel level,
            BoundingBox chunkBB,
            int centerX,
            int centerZ,
            int islandY,
            AirTemplePalette palette
    ) {
        int radius = AirTempleDimensions.MAIN_ISLAND_RADIUS;
        int radiusSq = radius * radius;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int distanceSq = dx * dx + dz * dz;

                if (distanceSq > radiusSq) {
                    continue;
                }

                int x = centerX + dx;
                int z = centerZ + dz;
                double edge = mainIslandEdge(centerX, centerZ, x, z, radius);

                if (edge > 1.0D) {
                    continue;
                }

                double core = 1.0D - edge;
                int surfaceNoise = smoothNoise(x, z, 2003);
                int rimDrop = edge > 0.66D ? (int) Math.round((edge - 0.66D) * 12.0D) : 0;
                int topY = islandY + surfaceNoise - rimDrop;
                int depth = Math.max(3, (int) Math.round(core * 17.0D) + 4);
                int bottomY = topY - depth;

                for (int y = bottomY; y <= topY; y++) {
                    double vertical = (topY - y) / (double) Math.max(1, depth);
                    double taperAllowance = edge + vertical * 0.34D;

                    if (taperAllowance > 1.04D) {
                        continue;
                    }

                    AirTempleBlocks.place(
                            level,
                            chunkBB,
                            new BlockPos(x, y, z),
                            y == topY ? palette.islandTop(x, z) : palette.islandBody(x, y, z)
                    );
                }
            }
        }
    }

    private static void generateSatellite(
            WorldGenLevel level,
            BoundingBox chunkBB,
            int satelliteX,
            int satelliteZ,
            int satelliteY,
            int radius,
            AirTemplePalette palette
    ) {
        int radiusSq = radius * radius;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int distanceSq = dx * dx + dz * dz;

                if (distanceSq > radiusSq) {
                    continue;
                }

                int x = satelliteX + dx;
                int z = satelliteZ + dz;
                double edge = satelliteEdge(satelliteX, satelliteZ, x, z, radius);

                if (edge > 1.0D) {
                    continue;
                }

                int roughness = smoothNoise(x, z, 7013);
                int topY = satelliteY + roughness - (edge > 0.72D ? 1 : 0);
                int depth = Math.max(2, (int) Math.round((1.0D - edge) * 7.0D) + 1);

                for (int y = topY - depth; y <= topY; y++) {
                    double vertical = (topY - y) / (double) Math.max(1, depth);

                    if (edge + vertical * 0.42D > 1.05D) {
                        continue;
                    }

                    AirTempleBlocks.place(
                            level,
                            chunkBB,
                            new BlockPos(x, y, z),
                            y == topY ? palette.islandTop(x, z) : palette.islandBody(x, y, z)
                    );
                }
            }
        }
    }

    private static double mainIslandEdge(int centerX, int centerZ, int x, int z, int radius) {
        double dx = x - centerX;
        double dz = z - centerZ;
        double angle = Math.atan2(dz, dx);
        double localRadius = radius
                + Math.sin(angle * 3.0D) * 2.5D
                + Math.cos(angle * 5.0D) * 1.8D
                + (AirTempleBlocks.noise2((int) Math.round(angle * 20.0D), radius, 9337) % 3 - 1);

        return Math.sqrt(dx * dx + dz * dz) / localRadius;
    }

    private static double satelliteEdge(int centerX, int centerZ, int x, int z, int radius) {
        double dx = x - centerX;
        double dz = z - centerZ;
        double angle = Math.atan2(dz, dx);
        double localRadius = radius
                + Math.sin(angle * 2.0D) * 1.2D
                + Math.cos(angle * 4.0D) * 0.8D;

        return Math.sqrt(dx * dx + dz * dz) / localRadius;
    }

    private static int smoothNoise(int x, int z, int salt) {
        int total = 0;

        total += AirTempleBlocks.noise2(x >> 2, z >> 2, salt) % 3;
        total += AirTempleBlocks.noise2((x + 3) >> 3, (z - 5) >> 3, salt + 97) % 3;

        return Math.max(-1, Math.min(2, total / 2 - 1));
    }
}
