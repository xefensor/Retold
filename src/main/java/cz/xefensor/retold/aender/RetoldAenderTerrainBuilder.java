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
    private static final int CONSTRAINED_EDGE_BLEND_WIDTH = 6;

    /*
     * Keep this fixed for now because the current ChunkGenerator path uses 0L.
     * Regeneration must use the same effective base terrain seed, otherwise
     * regenerated chunks will not visually match initially generated chunks.
     */
    private static final long EFFECTIVE_WORLD_TERRAIN_SEED = 0L;

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
        ChunkPos pos = chunk.getPos();
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int worldX = pos.getMinBlockX() + localX;
                int worldZ = pos.getMinBlockZ() + localZ;

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
            long regenerationSalt,
            RetoldAenderTerrainConstraints constraints
    ) {
        ChunkPos pos = chunk.getPos();
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int worldX = pos.getMinBlockX() + localX;
                int worldZ = pos.getMinBlockZ() + localZ;

                double mutationBlend =
                        getMutationBlend(localX, localZ, constraints);

                for (int y = MIN_Y; y < MAX_Y; y++) {
                    BlockState state =
                            getRegeneratedBlockStateAt(
                                    worldX,
                                    y,
                                    worldZ,
                                    worldSeed,
                                    regenerationSalt,
                                    mutationBlend
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
            long regenerationSalt,
            double mutationBlend
    ) {
        if (mutationBlend <= 0.0D) {
            return getBaseBlockStateAt(worldX, y, worldZ, worldSeed);
        }

        long baseSeed = getBaseTerrainSeed(worldSeed);
        long mutationSeed = getMutationTerrainSeed(
                worldSeed,
                regenerationSalt
        );

        IslandColumn baseColumn =
                getIslandColumn(
                        worldX,
                        worldZ,
                        baseSeed
                );

        IslandColumn mutatedColumn =
                mutateIslandColumn(
                        baseColumn,
                        worldX,
                        worldZ,
                        mutationSeed,
                        mutationBlend
                );

        double baseDensity =
                getIslandDensityAtY(baseColumn, y);

        double mutatedDensity =
                getIslandDensityAtY(mutatedColumn, y);

        double detailMutation =
                getMutationDetailDensity(
                        worldX,
                        y,
                        worldZ,
                        mutationSeed
                );

        double surfaceBias =
                1.0D - clamp(Math.abs(baseDensity) * 1.25D, 0.0D, 1.0D);

        /*
         * Stronger detail mutation.
         * Since constrained edges now handle safety, the free interior can
         * visibly retract, extend, erode, and fill.
         */
        double detailStrength =
                0.36D
                        * mutationBlend
                        * (0.35D + surfaceBias * 0.65D);

        double finalDensity =
                mutatedDensity + detailMutation * detailStrength;

        /*
         * Do not fill totally unrelated empty sky too easily,
         * but allow real extension near low-mass island borders.
         */
        if (baseColumn.mass() < 0.03D && finalDensity < 0.12D) {
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
        long seed = getBaseTerrainSeed(worldSeed);

        IslandColumn column =
                getIslandColumn(
                        worldX,
                        worldZ,
                        seed
                );

        return getIslandDensityAtY(column, y);
    }

    private static IslandColumn mutateIslandColumn(
            IslandColumn baseColumn,
            int worldX,
            int worldZ,
            long mutationSeed,
            double blend
    ) {
        double massNoise =
                signedFractalNoise2D(
                        worldX * 0.024D,
                        worldZ * 0.024D,
                        mutationSeed ^ 0x11F2AB37C94E58D1L,
                        3
                );

        double heightNoise =
                signedFractalNoise2D(
                        worldX * 0.018D + 80.0D,
                        worldZ * 0.018D - 160.0D,
                        mutationSeed ^ 0xA87D46F012CE59B3L,
                        3
                );

        double topNoise =
                signedFractalNoise2D(
                        worldX * 0.038D - 42.0D,
                        worldZ * 0.038D + 77.0D,
                        mutationSeed ^ 0x3CB92EA7764A19DDL,
                        2
                );

        double bottomNoise =
                signedFractalNoise2D(
                        worldX * 0.032D + 190.0D,
                        worldZ * 0.032D - 91.0D,
                        mutationSeed ^ 0xD719AC43E660B221L,
                        2
                );

        /*
         * Much stronger than previous versions.
         * This is safe because mutationBlend is constrained only on sides
         * touching watched/stabilized chunks.
         */
        double mass =
                clamp(
                        baseColumn.mass()
                                + massNoise * 0.85D * blend,
                        0.0D,
                        1.45D
                );

        int centerY =
                clamp(
                        baseColumn.centerY()
                                + (int) Math.round(heightNoise * 34.0D * blend),
                        MIN_Y + 16,
                        MAX_Y - 16
                );

        int topRadius =
                clamp(
                        baseColumn.topRadius()
                                + (int) Math.round(topNoise * 16.0D * blend),
                        2,
                        42
                );

        int bottomRadius =
                clamp(
                        baseColumn.bottomRadius()
                                + (int) Math.round(bottomNoise * 42.0D * blend),
                        6,
                        92
                );

        if (mass < baseColumn.mass()) {
            double shrink = (baseColumn.mass() - mass) * blend;

            topRadius =
                    Math.max(
                            2,
                            topRadius - (int) Math.round(shrink * 12.0D)
                    );

            bottomRadius =
                    Math.max(
                            6,
                            bottomRadius - (int) Math.round(shrink * 26.0D)
                    );
        } else {
            double growth = (mass - baseColumn.mass()) * blend;

            topRadius += (int) Math.round(growth * 8.0D);
            bottomRadius += (int) Math.round(growth * 22.0D);
        }

        return new IslandColumn(
                mass,
                centerY,
                topRadius,
                bottomRadius
        );
    }

    private static double getMutationDetailDensity(
            int worldX,
            int y,
            int worldZ,
            long mutationSeed
    ) {
        double erosion =
                signedFractalNoise2D(
                        worldX * 0.105D + y * 0.018D,
                        worldZ * 0.105D - y * 0.015D,
                        mutationSeed ^ 0x95A672F4D3C12B11L,
                        3
                );

        double scars =
                signedFractalNoise2D(
                        worldX * 0.052D,
                        y * 0.070D + worldZ * 0.014D,
                        mutationSeed ^ 0xC1D7A63EF2198B45L,
                        2
                );

        double pockets =
                signedFractalNoise2D(
                        worldX * 0.165D + y * 0.025D,
                        worldZ * 0.165D - y * 0.021D,
                        mutationSeed ^ 0x7D42C93B1E6A90F1L,
                        2
                );

        return erosion * 0.48D
                + scars * 0.30D
                + pockets * 0.22D;
    }

    private static double getMutationBlend(
            int localX,
            int localZ,
            RetoldAenderTerrainConstraints constraints
    ) {
        if (constraints == null || !constraints.hasAnyConstraint()) {
            return 1.0D;
        }

        double blend = 1.0D;

        if (constraints.constrainWest()) {
            blend = Math.min(
                    blend,
                    edgeRamp(localX)
            );
        }

        if (constraints.constrainEast()) {
            blend = Math.min(
                    blend,
                    edgeRamp(15 - localX)
            );
        }

        if (constraints.constrainNorth()) {
            blend = Math.min(
                    blend,
                    edgeRamp(localZ)
            );
        }

        if (constraints.constrainSouth()) {
            blend = Math.min(
                    blend,
                    edgeRamp(15 - localZ)
            );
        }

        return smoothStep(blend);
    }

    private static double edgeRamp(int distanceFromEdge) {
        return clamp(
                distanceFromEdge / (double) CONSTRAINED_EDGE_BLEND_WIDTH,
                0.0D,
                1.0D
        );
    }

    private static IslandColumn getIslandColumn(
            int worldX,
            int worldZ,
            long seed
    ) {
        double largeMask =
                fractalNoise2D(
                        worldX * 0.009D,
                        worldZ * 0.009D,
                        seed,
                        4
                );

        double mediumMask =
                fractalNoise2D(
                        worldX * 0.030D + 91.7D,
                        worldZ * 0.030D - 37.4D,
                        seed ^ 0x6C8E9CF570932BD5L,
                        3
                );

        double edgeMask =
                fractalNoise2D(
                        worldX * 0.072D - 34.0D,
                        worldZ * 0.072D + 129.0D,
                        seed ^ 0x8E76172B4F139C5DL,
                        2
                );

        double islandMask =
                largeMask * 0.58D
                        + mediumMask * 0.32D
                        + edgeMask * 0.10D;

        double mass =
                smoothThreshold(
                        islandMask,
                        0.49D,
                        0.76D
                );

        double heightNoise =
                fractalNoise2D(
                        worldX * 0.011D - 118.0D,
                        worldZ * 0.011D + 53.0D,
                        seed ^ 0x2B5A4D07E31F9013L,
                        3
                );

        int centerY =
                MIN_Y
                        + (int) (HEIGHT * 0.36D)
                        + (int) (heightNoise * HEIGHT * 0.32D);

        centerY = clamp(centerY, MIN_Y + 24, MAX_Y - 32);

        double domeNoise =
                fractalNoise2D(
                        worldX * 0.080D + 240.0D,
                        worldZ * 0.080D - 120.0D,
                        seed ^ 0x1F37BCE92A60D4C3L,
                        2
                );

        double undersideNoise =
                fractalNoise2D(
                        worldX * 0.067D + 420.0D,
                        worldZ * 0.067D - 240.0D,
                        seed ^ 0x5149D2AF0B44E281L,
                        2
                );

        int topRadius =
                5
                        + (int) (mass * 14.0D)
                        + (int) (domeNoise * 4.0D);

        int bottomRadius =
                14
                        + (int) (mass * 42.0D)
                        + (int) (undersideNoise * 14.0D);

        return new IslandColumn(
                mass,
                centerY,
                Math.max(3, topRadius),
                Math.max(8, bottomRadius)
        );
    }

    private static double getIslandDensityAtY(
            IslandColumn column,
            int y
    ) {
        int top = column.centerY() + column.topRadius();
        int bottom = column.centerY() - column.bottomRadius();

        if (y > top || y < bottom) {
            return -1.0D;
        }

        double verticalDistance;

        if (y >= column.centerY()) {
            verticalDistance =
                    (double) (y - column.centerY())
                            / Math.max(1.0D, column.topRadius());

            verticalDistance = Math.pow(verticalDistance, 1.55D);
        } else {
            verticalDistance =
                    (double) (column.centerY() - y)
                            / Math.max(1.0D, column.bottomRadius());

            verticalDistance = Math.pow(verticalDistance, 0.76D);
        }

        return column.mass() - verticalDistance * 0.82D;
    }

    private static long getBaseTerrainSeed(long worldSeed) {
        return EFFECTIVE_WORLD_TERRAIN_SEED
                ^ BASE_TERRAIN_SALT
                ^ 0xA35D3F1B4C9E27AFL;
    }

    private static long getMutationTerrainSeed(
            long worldSeed,
            long regenerationSalt
    ) {
        return EFFECTIVE_WORLD_TERRAIN_SEED
                ^ regenerationSalt
                ^ 0xB89F1246D73AC55DL;
    }

    private static double smoothThreshold(
            double value,
            double min,
            double max
    ) {
        double normalized =
                clamp((value - min) / (max - min), 0.0D, 1.0D);

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
        int x0 = fastFloor(x);
        int z0 = fastFloor(z);
        int x1 = x0 + 1;
        int z1 = z0 + 1;

        double tx = smoothStep(x - x0);
        double tz = smoothStep(z - z0);

        double v00 = randomDoubleAt(x0, z0, seed);
        double v10 = randomDoubleAt(x1, z0, seed);
        double v01 = randomDoubleAt(x0, z1, seed);
        double v11 = randomDoubleAt(x1, z1, seed);

        double a = lerp(v00, v10, tx);
        double b = lerp(v01, v11, tx);

        return lerp(a, b, tz);
    }

    private static double randomDoubleAt(
            int x,
            int z,
            long seed
    ) {
        long value = seed;
        value ^= (long) x * 0x632BE59BD9B4E019L;
        value ^= (long) z * 0x85157AF5C91D1B35L;
        value = mix64(value);

        long bits = value >>> 11;

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
        int integer = (int) value;

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
            int centerY,
            int topRadius,
            int bottomRadius
    ) {
    }
}