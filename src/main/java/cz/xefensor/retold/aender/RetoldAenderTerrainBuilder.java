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

    /*
     * Important:
     * The current Aender ChunkGenerator calls this builder with worldSeed = 0L,
     * because this ChunkGenerator hook does not currently pass the real level seed.
     *
     * Regeneration must therefore use the same effective terrain seed,
     * otherwise regenerated chunks will not match initially generated chunks.
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

        BlockPos.MutableBlockPos mutablePos =
                new BlockPos.MutableBlockPos();

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
            long regenerationSalt
    ) {
        ChunkPos pos = chunk.getPos();

        BlockPos.MutableBlockPos mutablePos =
                new BlockPos.MutableBlockPos();

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int worldX = pos.getMinBlockX() + localX;
                int worldZ = pos.getMinBlockZ() + localZ;

                double regenerationBlend =
                        getChunkInteriorBlend(localX, localZ);

                for (int y = MIN_Y; y < MAX_Y; y++) {
                    BlockState state =
                            getRegeneratedBlockStateAt(
                                    worldX,
                                    y,
                                    worldZ,
                                    worldSeed,
                                    regenerationSalt,
                                    regenerationBlend
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

        if (density > 0.0D) {
            return END_STONE;
        }

        return AIR;
    }

    private static BlockState getRegeneratedBlockStateAt(
            int worldX,
            int y,
            int worldZ,
            long worldSeed,
            long regenerationSalt,
            double regenerationBlend
    ) {
        double baseDensity =
                getBaseDensityAt(
                        worldX,
                        y,
                        worldZ,
                        worldSeed
                );

        if (regenerationBlend <= 0.0D) {
            return baseDensity > 0.0D ? END_STONE : AIR;
        }

        /*
         * Do not create detached new islands far away from the base terrain.
         * Regeneration should alter the existing island shape, not replace it.
         */
        if (baseDensity < -0.18D) {
            return AIR;
        }

        double mutation =
                getMutationDensity(
                        worldX,
                        y,
                        worldZ,
                        worldSeed,
                        regenerationSalt
                );

        double surfaceBias =
                1.0D - clamp(Math.abs(baseDensity) * 4.0D, 0.0D, 1.0D);

        /*
         * Most mutation happens near the surface.
         * Deep interiors stay mostly solid, far air stays air.
         */
        double mutationStrength =
                0.16D
                        * regenerationBlend
                        * (0.25D + surfaceBias * 0.75D);

        double density =
                baseDensity + mutation * mutationStrength;

        if (density > 0.0D) {
            return END_STONE;
        }

        return AIR;
    }

    private static double getBaseDensityAt(
            int worldX,
            int y,
            int worldZ,
            long worldSeed
    ) {
        long seed =
                getBaseTerrainSeed(worldSeed);

        ColumnShape shape =
                getIslandColumnShape(
                        worldX,
                        worldZ,
                        seed
                );

        return getIslandDensityAtY(shape, y);
    }

    private static double getMutationDensity(
            int worldX,
            int y,
            int worldZ,
            long worldSeed,
            long regenerationSalt
    ) {
        long seed =
                getMutationTerrainSeed(
                        worldSeed,
                        regenerationSalt
                );

        double broadChange =
                signedFractalNoise2D(
                        worldX * 0.035D + y * 0.004D,
                        worldZ * 0.035D - y * 0.003D,
                        seed ^ 0x95A672F4D3C12B11L,
                        3
                );

        double surfaceDetail =
                signedFractalNoise2D(
                        worldX * 0.145D + y * 0.019D,
                        worldZ * 0.145D - y * 0.017D,
                        seed ^ 0xC1D7A63EF2198B45L,
                        2
                );

        double layerChange =
                signedFractalNoise2D(
                        worldX * 0.07D,
                        y * 0.055D + worldZ * 0.011D,
                        seed ^ 0x7D42C93B1E6A90F1L,
                        2
                );

        return broadChange * 0.55D
                + surfaceDetail * 0.35D
                + layerChange * 0.10D;
    }

    private static double getChunkInteriorBlend(
            int localX,
            int localZ
    ) {
        int distanceToEdge = Math.min(
                Math.min(localX, 15 - localX),
                Math.min(localZ, 15 - localZ)
        );

        /*
         * Edge blocks stay exactly base terrain.
         * The middle of the chunk can mutate.
         */
        double blend = distanceToEdge / 7.0D;
        blend = clamp(blend, 0.0D, 1.0D);

        return smoothStep(blend);
    }

    private static ColumnShape getIslandColumnShape(
            int worldX,
            int worldZ,
            long seed
    ) {
        double continentNoise = fractalNoise2D(
                worldX * 0.018D,
                worldZ * 0.018D,
                seed,
                4
        );

        double detailNoise = fractalNoise2D(
                worldX * 0.065D + 91.7D,
                worldZ * 0.065D - 37.4D,
                seed ^ 0x6C8E9CF570932BD5L,
                3
        );

        double islandStrength =
                continentNoise * 0.78D
                        + detailNoise * 0.22D;

        double heightNoise = fractalNoise2D(
                worldX * 0.012D - 118.0D,
                worldZ * 0.012D + 53.0D,
                seed ^ 0x2B5A4D07E31F9013L,
                3
        );

        int centerY = MIN_Y
                + (int) (HEIGHT * 0.28D)
                + (int) (heightNoise * HEIGHT * 0.42D);

        centerY = clamp(centerY, MIN_Y + 16, MAX_Y - 16);

        double normalizedStrength =
                Math.max(0.0D, islandStrength - 0.48D) / 0.52D;

        int thickness = 3 + (int) (normalizedStrength * 18.0D);

        double undersideNoise = fractalNoise2D(
                worldX * 0.11D + 420.0D,
                worldZ * 0.11D - 240.0D,
                seed ^ 0x5149D2AF0B44E281L,
                2
        );

        int undersideExtra = (int) (undersideNoise * 5.0D);

        return new ColumnShape(
                islandStrength,
                centerY,
                thickness,
                undersideExtra
        );
    }

    private static double getIslandDensityAtY(
            ColumnShape shape,
            int y
    ) {
        int top = shape.centerY()
                + Math.max(1, shape.thickness() / 3);

        int bottom = shape.centerY()
                - shape.thickness()
                - shape.undersideExtra();

        double verticalDistance;

        if (y >= shape.centerY()) {
            verticalDistance =
                    (double) (y - shape.centerY())
                            / Math.max(1.0D, top - shape.centerY());
        } else {
            verticalDistance =
                    (double) (shape.centerY() - y)
                            / Math.max(1.0D, shape.centerY() - bottom);
        }

        return ((shape.islandStrength() - 0.48D) / 0.52D)
                - verticalDistance * 0.82D;
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

    private record ColumnShape(
            double islandStrength,
            int centerY,
            int thickness,
            int undersideExtra
    ) {
    }
}