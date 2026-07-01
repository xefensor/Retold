package cz.xefensor.retold.aender;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

public final class RetoldAenderTerrainBuilder {
    public static final int MIN_Y = 0;
    public static final int MAX_Y = 256;
    public static final int HEIGHT = MAX_Y - MIN_Y;

    private static final int CHUNK_WRITE_FLAGS = 0;

    private static final long BASE_TERRAIN_SALT =
            0x4E2A19D7C05B8F33L;

    private static final BlockState AIR =
            Blocks.AIR.defaultBlockState();

    private static final BlockState END_STONE =
            Blocks.END_STONE.defaultBlockState();

    private RetoldAenderTerrainBuilder() {
    }

    public static void generateInitialFloatingIslands(
            ChunkAccess chunk,
            long worldSeed
    ) {
        ChunkPos pos =
                chunk.getPos();

        BlockPos.MutableBlockPos mutablePos =
                new BlockPos.MutableBlockPos();

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int worldX =
                        pos.getMinBlockX() + localX;

                int worldZ =
                        pos.getMinBlockZ() + localZ;

                for (int y = MIN_Y; y < MAX_Y; y++) {
                    BlockState state =
                            getBaseBlockStateAt(
                                    worldX,
                                    y,
                                    worldZ,
                                    worldSeed
                            );

                    mutablePos.set(worldX, y, worldZ);

                    chunk.setBlockState(
                            mutablePos,
                            state,
                            CHUNK_WRITE_FLAGS
                    );
                }
            }
        }
    }

    public static void regenerateFloatingIslands(
            ChunkAccess chunk,
            long worldSeed,
            RetoldAenderTerrainData terrainData
    ) {
        ChunkPos pos =
                chunk.getPos();

        BlockPos.MutableBlockPos mutablePos =
                new BlockPos.MutableBlockPos();

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int worldX =
                        pos.getMinBlockX() + localX;

                int worldZ =
                        pos.getMinBlockZ() + localZ;

                for (int y = MIN_Y; y < MAX_Y; y++) {
                    BlockState state =
                            getRegeneratedBlockStateAt(
                                    worldX,
                                    y,
                                    worldZ,
                                    worldSeed,
                                    terrainData
                            );

                    mutablePos.set(worldX, y, worldZ);

                    chunk.setBlockState(
                            mutablePos,
                            state,
                            CHUNK_WRITE_FLAGS
                    );
                }
            }
        }
    }

    public static BlockState getBaseBlockStateAt(
            int worldX,
            int y,
            int worldZ,
            long worldSeed
    ) {
        double density =
                getBaseDensityAt(
                        worldX,
                        y,
                        worldZ,
                        worldSeed
                );

        return density > 0.0D ? END_STONE : AIR;
    }

    private static BlockState getRegeneratedBlockStateAt(
            int worldX,
            int y,
            int worldZ,
            long worldSeed,
            RetoldAenderTerrainData terrainData
    ) {
        long salt =
                terrainData.getSalt();

        if (salt == 0L) {
            return getBaseBlockStateAt(
                    worldX,
                    y,
                    worldZ,
                    worldSeed
            );
        }

        IslandColumn baseColumn =
                getIslandColumn(
                        worldX,
                        worldZ,
                        getBaseTerrainSeed(worldSeed)
                );

        IslandColumn mutatedColumn =
                mutateIslandColumn(
                        baseColumn,
                        worldX,
                        worldZ,
                        salt
                );

        double baseDensity =
                getIslandDensityAtY(baseColumn, y);

        double mutatedDensity =
                getIslandDensityAtY(mutatedColumn, y);

        double detail =
                getMutationDetailDensity(
                        worldX,
                        y,
                        worldZ,
                        salt
                );

        double surfaceBias =
                1.0D - clamp(
                        Math.abs(baseDensity) * 1.1D,
                        0.0D,
                        1.0D
                );

        double finalDensity =
                mutatedDensity
                        + detail * (0.16D + surfaceBias * 0.18D);

        if (baseColumn.mass() < 0.02D
                && mutatedColumn.mass() < 0.16D
                && finalDensity < 0.14D) {
            return AIR;
        }

        return finalDensity > 0.0D ? END_STONE : AIR;
    }

    private static double getBaseDensityAt(
            int worldX,
            int y,
            int worldZ,
            long worldSeed
    ) {
        IslandColumn column =
                getIslandColumn(
                        worldX,
                        worldZ,
                        getBaseTerrainSeed(worldSeed)
                );

        return getIslandDensityAtY(column, y);
    }

    private static IslandColumn mutateIslandColumn(
            IslandColumn baseColumn,
            int worldX,
            int worldZ,
            long salt
    ) {
        long seed =
                getMutationTerrainSeed(salt);

        double massNoise =
                signedFractalNoise2D(
                        worldX * 0.014D,
                        worldZ * 0.014D,
                        seed ^ 0x11F2AB37C94E58D1L,
                        3
                );

        double surfaceNoise =
                signedFractalNoise2D(
                        worldX * 0.011D + 80.0D,
                        worldZ * 0.011D - 160.0D,
                        seed ^ 0xA87D46F012CE59B3L,
                        3
                );

        double depthNoise =
                signedFractalNoise2D(
                        worldX * 0.018D + 190.0D,
                        worldZ * 0.018D - 91.0D,
                        seed ^ 0xD719AC43E660B221L,
                        3
                );

        double mass =
                clamp(
                        baseColumn.mass()
                                + massNoise * 1.05D,
                        0.0D,
                        1.60D
                );

        /*
         * Keep mutation inside the same rough height band.
         * This preserves the feeling that one landmass has one chosen height.
         */
        int surfaceY =
                clamp(
                        baseColumn.surfaceY()
                                + (int) Math.round(surfaceNoise * 6.0D),
                        28,
                        176
                );

        int depth =
                clamp(
                        baseColumn.depth()
                                + (int) Math.round(depthNoise * 18.0D),
                        38,
                        88
                );

        if (mass < baseColumn.mass()) {
            double shrink =
                    baseColumn.mass() - mass;

            depth =
                    Math.max(
                            38,
                            depth - (int) Math.round(shrink * 18.0D)
                    );
        } else {
            double growth =
                    mass - baseColumn.mass();

            depth +=
                    (int) Math.round(growth * 20.0D);
        }

        return new IslandColumn(
                mass,
                surfaceY,
                depth
        );
    }

    private static double getMutationDetailDensity(
            int worldX,
            int y,
            int worldZ,
            long salt
    ) {
        long seed =
                getMutationTerrainSeed(salt);

        double erosion =
                signedFractalNoise2D(
                        worldX * 0.090D + y * 0.012D,
                        worldZ * 0.090D - y * 0.011D,
                        seed ^ 0x95A672F4D3C12B11L,
                        3
                );

        double cliffScars =
                signedFractalNoise2D(
                        worldX * 0.045D,
                        y * 0.055D + worldZ * 0.012D,
                        seed ^ 0xC1D7A63EF2198B45L,
                        2
                );

        double pockets =
                signedFractalNoise2D(
                        worldX * 0.145D + y * 0.018D,
                        worldZ * 0.145D - y * 0.016D,
                        seed ^ 0x7D42C93B1E6A90F1L,
                        2
                );

        return erosion * 0.45D
                + cliffScars * 0.35D
                + pockets * 0.20D;
    }

    private static IslandColumn getIslandColumn(
            int worldX,
            int worldZ,
            long seed
    ) {
        /*
         * Island-anchor system.
         *
         * Each large island has one anchor.
         * The anchor decides:
         * - island center
         * - island size
         * - island surface height
         * - island depth
         *
         * This prevents one connected island from being split into different
         * height bands.
         */
        int islandCellSize = 448;

        int baseCellX =
                Math.floorDiv(worldX, islandCellSize);

        int baseCellZ =
                Math.floorDiv(worldZ, islandCellSize);

        IslandColumn bestColumn =
                new IslandColumn(
                        0.0D,
                        64,
                        48
                );

        double bestMass = 0.0D;

        for (int offsetX = -1; offsetX <= 1; offsetX++) {
            for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
                IslandAnchor anchor =
                        createIslandAnchor(
                                baseCellX + offsetX,
                                baseCellZ + offsetZ,
                                seed,
                                islandCellSize
                        );

                if (anchor == null) {
                    continue;
                }

                double deltaX =
                        worldX - anchor.centerX();

                double deltaZ =
                        worldZ - anchor.centerZ();

                double distance =
                        Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

                double edgeNoise =
                        signedFractalNoise2D(
                                worldX * 0.010D,
                                worldZ * 0.010D,
                                anchor.seed() ^ 0x8E76172B4F139C5DL,
                                3
                        );

                double radius =
                        anchor.radius()
                                * (1.0D + edgeNoise * 0.18D);

                double edge =
                        1.0D - distance / Math.max(1.0D, radius);

                double mass =
                        smoothThreshold(
                                edge,
                                0.02D,
                                0.34D
                        ) * anchor.strength();

                double edgeDetail =
                        signedFractalNoise2D(
                                worldX * 0.030D + 91.7D,
                                worldZ * 0.030D - 37.4D,
                                anchor.seed() ^ 0x6C8E9CF570932BD5L,
                                2
                        );

                /*
                 * Only roughen the edge, not the whole surface.
                 */
                double edgeInfluence =
                        1.0D - clamp(edge * 3.0D, 0.0D, 1.0D);

                mass =
                        clamp(
                                mass + edgeDetail * 0.045D * edgeInfluence,
                                0.0D,
                                1.60D
                        );

                if (mass <= bestMass) {
                    continue;
                }

                double surfaceNoise =
                        signedFractalNoise2D(
                                worldX * 0.008D - 118.0D,
                                worldZ * 0.008D + 53.0D,
                                anchor.seed() ^ 0x2B5A4D07E31F9013L,
                                3
                        );

                double surfaceDetail =
                        signedFractalNoise2D(
                                worldX * 0.026D + 240.0D,
                                worldZ * 0.026D - 120.0D,
                                anchor.seed() ^ 0x1F37BCE92A60D4C3L,
                                2
                        );

                /*
                 * Same island = same base height.
                 * Only tiny local variation is added.
                 */
                int surfaceY =
                        anchor.surfaceY()
                                + (int) Math.round(surfaceNoise * 2.0D)
                                + (int) Math.round(surfaceDetail * 1.0D);

                surfaceY =
                        clamp(
                                surfaceY,
                                28,
                                176
                        );

                double undersideNoise =
                        signedFractalNoise2D(
                                worldX * 0.020D + 420.0D,
                                worldZ * 0.020D - 240.0D,
                                anchor.seed() ^ 0x5149D2AF0B44E281L,
                                3
                        );

                /*
                 * Important:
                 * Depth gets smaller near the edge.
                 * This makes sides less vertical and more island-like.
                 */
                double normalizedMass =
                        clamp(
                                mass,
                                0.0D,
                                1.0D
                        );

                int depth =
                        (int) Math.round(
                                anchor.depth()
                                        * (0.45D + normalizedMass * 0.55D)
                        )
                                + (int) Math.round(undersideNoise * 4.0D);

                depth =
                        clamp(
                                depth,
                                22,
                                92
                        );

                bestMass = mass;

                bestColumn =
                        new IslandColumn(
                                mass,
                                surfaceY,
                                depth
                        );
            }
        }

        return bestColumn;
    }

    private static IslandAnchor createIslandAnchor(
            int cellX,
            int cellZ,
            long seed,
            int islandCellSize
    ) {
        long cellSeed =
                seed
                        ^ (long) cellX * 0x632BE59BD9B4E019L
                        ^ (long) cellZ * 0x85157AF5C91D1B35L
                        ^ 0x41F28BA76D903E11L;

        /*
         * Some cells intentionally have no island.
         * This creates outer-End-like void spacing.
         */
        double presence =
                randomUnitDouble(cellSeed ^ 0x7D42C93B1E6A90F1L);

        if (presence < 0.18D) {
            return null;
        }

        int cellMinX =
                cellX * islandCellSize;

        int cellMinZ =
                cellZ * islandCellSize;

        int centerMargin =
                96;

        int centerRange =
                islandCellSize - centerMargin * 2;

        int centerX =
                cellMinX
                        + centerMargin
                        + (int) Math.round(
                        randomUnitDouble(cellSeed ^ 0xB89F1246D73AC55DL)
                                * centerRange
                );

        int centerZ =
                cellMinZ
                        + centerMargin
                        + (int) Math.round(
                        randomUnitDouble(cellSeed ^ 0xA87D46F012CE59B3L)
                                * centerRange
                );

        int radius =
                135
                        + (int) Math.round(
                        randomUnitDouble(cellSeed ^ 0x11F2AB37C94E58D1L)
                                * 95.0D
                );

        int surfaceY =
                getIslandSurfaceHeightForSeed(
                        cellSeed ^ 0x95A672F4D3C12B11L
                );

        int depth =
                46
                        + (int) Math.round(
                        randomUnitDouble(cellSeed ^ 0xC1D7A63EF2198B45L)
                                * 22.0D
                )
                        + (int) Math.round(
                        Math.max(0, surfaceY - 90) * 0.16D
                );

        depth =
                clamp(
                        depth,
                        40,
                        88
                );

        double strength =
                0.92D
                        + randomUnitDouble(cellSeed ^ 0x3CB92EA7764A19DDL)
                        * 0.24D;

        return new IslandAnchor(
                centerX,
                centerZ,
                radius,
                surfaceY,
                depth,
                strength,
                cellSeed
        );
    }

    private static int getIslandSurfaceHeightForSeed(long seed) {
        double selector =
                randomUnitDouble(seed);

        if (selector < 0.14D) {
            return 40;
        }

        if (selector < 0.32D) {
            return 58;
        }

        if (selector < 0.54D) {
            return 72;
        }

        if (selector < 0.72D) {
            return 96;
        }

        if (selector < 0.88D) {
            return 128;
        }

        return 156;
    }

    private record IslandAnchor(
            int centerX,
            int centerZ,
            int radius,
            int surfaceY,
            int depth,
            double strength,
            long seed
    ) {
    }

    private static double getIslandDensityAtY(
            IslandColumn column,
            int y
    ) {
        if (column.mass() < 0.035D) {
            return -1.0D;
        }

        int surfaceY =
                column.surfaceY();

        int bottomY =
                column.surfaceY() - column.depth();

        if (y > surfaceY || y < bottomY) {
            return -1.0D;
        }

        double depthProgress =
                (double) (surfaceY - y)
                        / Math.max(1.0D, column.depth());

        /*
         * Gentler End-like profile:
         *
         * - top remains solid
         * - center is thick
         * - underside/edges taper more gradually
         */
        double topPlateau =
                1.0D - Math.pow(depthProgress, 2.55D);

        double bottomTaper =
                Math.pow(depthProgress, 2.05D) * 0.54D;

        double density =
                column.mass() * 1.28D
                        + topPlateau * 0.68D
                        - bottomTaper
                        - 0.50D;

        /*
         * Solid top cap, but not too chunky.
         */
        if (surfaceY - y <= 5) {
            density += 0.16D;
        }

        /*
         * Softer underside taper.
         */
        if (depthProgress > 0.84D) {
            density -= (depthProgress - 0.84D) * 0.75D;
        }

        return density;
    }

    private static long getBaseTerrainSeed(long worldSeed) {
        return worldSeed
                ^ BASE_TERRAIN_SALT
                ^ 0xA35D3F1B4C9E27AFL;
    }

    private static long getMutationTerrainSeed(long salt) {
        return salt
                ^ 0xB89F1246D73AC55DL;
    }

    private static double smoothThreshold(
            double value,
            double min,
            double max
    ) {
        double normalized =
                clamp(
                        (value - min) / (max - min),
                        0.0D,
                        1.0D
                );

        return smoothStep(normalized);
    }

    private static double signedFractalNoise2D(
            double x,
            double z,
            long seed,
            int octaves
    ) {
        return fractalNoise2D(x, z, seed, octaves) * 2.0D - 1.0D;
    }

    private static double fractalNoise2D(
            double x,
            double z,
            long seed,
            int octaves
    ) {
        double value = 0.0D;
        double amplitude = 1.0D;
        double frequency = 1.0D;
        double max = 0.0D;

        for (int octave = 0; octave < octaves; octave++) {
            value += smoothNoise2D(
                    x * frequency,
                    z * frequency,
                    seed + octave * 0x9E3779B97F4A7C15L
            ) * amplitude;

            max += amplitude;
            amplitude *= 0.5D;
            frequency *= 2.0D;
        }

        if (max <= 0.0D) {
            return 0.0D;
        }

        return value / max;
    }

    private static double smoothNoise2D(
            double x,
            double z,
            long seed
    ) {
        int x0 =
                fastFloor(x);

        int z0 =
                fastFloor(z);

        int x1 =
                x0 + 1;

        int z1 =
                z0 + 1;

        double tx =
                smoothStep(x - x0);

        double tz =
                smoothStep(z - z0);

        double v00 =
                randomDoubleAt(x0, z0, seed);

        double v10 =
                randomDoubleAt(x1, z0, seed);

        double v01 =
                randomDoubleAt(x0, z1, seed);

        double v11 =
                randomDoubleAt(x1, z1, seed);

        double a =
                lerp(v00, v10, tx);

        double b =
                lerp(v01, v11, tx);

        return lerp(a, b, tz);
    }

    private static double randomDoubleAt(
            int x,
            int z,
            long seed
    ) {
        long value =
                seed;

        value ^= (long) x * 0x632BE59BD9B4E019L;
        value ^= (long) z * 0x85157AF5C91D1B35L;
        value = mix64(value);

        long bits =
                value >>> 11;

        return bits * 0x1.0p-53;
    }

    private static double randomUnitDouble(long seed) {
        long value =
                mix64(seed);

        long bits =
                value >>> 11;

        return bits * 0x1.0p-53;
    }

    private static long mix64(long value) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= value >>> 33;

        return value;
    }

    private static int fastFloor(double value) {
        int integer =
                (int) value;

        return value < integer ? integer - 1 : integer;
    }

    private static double smoothStep(double value) {
        return value * value * (3.0D - 2.0D * value);
    }

    private static double lerp(
            double from,
            double to,
            double delta
    ) {
        return from + (to - from) * delta;
    }

    private static int clamp(
            int value,
            int min,
            int max
    ) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clamp(
            double value,
            double min,
            double max
    ) {
        return Math.max(min, Math.min(max, value));
    }

    private record IslandColumn(
            double mass,
            int surfaceY,
            int depth
    ) {
    }
}