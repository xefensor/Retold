package cz.xefensor.retold.aender.generation;

import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.ArrayList;
import java.util.List;

public final class AenderIslandSampler {
    public static final int MIN_Y = 0;
    public static final int HEIGHT = 256;
    public static final int MAX_Y = MIN_Y + HEIGHT;

    public static final int REGION_SIZE = 384;
    public static final int LAYER_HEIGHT = 80;

    private static final double MIN_ISLAND_RADIUS = 50.0D;
    private static final double MAX_ISLAND_RADIUS = 120.0D;
    private static final double MIN_ISLAND_HEIGHT = 22.0D;
    private static final double MAX_ISLAND_HEIGHT = 42.0D;

    private AenderIslandSampler() {
    }

    public static List<Island> islandsForChunk(ChunkAccess chunk) {
        return islandsForChunk(chunk.getPos().x(), chunk.getPos().z());
    }

    static List<Island> islandsForChunk(int chunkX, int chunkZ) {
        List<Island> result = new ArrayList<>();
        int minX = chunkX << 4;
        int maxX = minX + 15;
        int minZ = chunkZ << 4;
        int maxZ = minZ + 15;

        int minRegionX = Math.floorDiv(minX, REGION_SIZE) - 1;
        int maxRegionX = Math.floorDiv(maxX, REGION_SIZE) + 1;
        int minRegionZ = Math.floorDiv(minZ, REGION_SIZE) - 1;
        int maxRegionZ = Math.floorDiv(maxZ, REGION_SIZE) + 1;

        int minLayer = Math.floorDiv(MIN_Y, LAYER_HEIGHT) - 1;
        int maxLayer = Math.floorDiv(MAX_Y - 1, LAYER_HEIGHT) + 1;

        for (int rx = minRegionX; rx <= maxRegionX; rx++) {
            for (int rz = minRegionZ; rz <= maxRegionZ; rz++) {
                for (int ly = minLayer; ly <= maxLayer; ly++) {
                    Island island = islandAt(rx, rz, ly);

                    if (island == null) {
                        continue;
                    }

                    if (island.maxX() < minX || island.minX() > maxX) {
                        continue;
                    }

                    if (island.maxZ() < minZ || island.minZ() > maxZ) {
                        continue;
                    }

                    result.add(island);
                }
            }
        }

        return result;
    }

    static List<BiomeColumn> biomeColumnsAt(List<Island> islands, int x, int z) {
        List<BiomeColumn> result = new ArrayList<>(islands.size());

        for (Island island : islands) {
            Island.Column column = island.columnAt(x, z);

            if (!column.empty()) {
                result.add(new BiomeColumn(island.seed(), island.biome(), column));
            }
        }

        return List.copyOf(result);
    }

    static AenderBiomeKind biomeFromColumns(List<BiomeColumn> columns, int y) {
        BiomeColumn closest = null;
        int closestDistance = Integer.MAX_VALUE;

        for (BiomeColumn candidate : columns) {
            int distance;

            if (y < candidate.column().minY()) {
                distance = candidate.column().minY() - y;
            } else if (y > candidate.column().maxY()) {
                distance = y - candidate.column().maxY();
            } else {
                distance = 0;
            }

            if (distance < closestDistance
                    || distance == closestDistance
                    && closest != null
                    && Long.compareUnsigned(candidate.islandSeed(), closest.islandSeed()) < 0) {
                closest = candidate;
                closestDistance = distance;
            }
        }

        return closest == null ? AenderBiomeKind.PLAINS : closest.biome();
    }

    static List<Island> islandsAtColumn(int x, int z) {
        List<Island> result = new ArrayList<>();
        int regionX = Math.floorDiv(x, REGION_SIZE);
        int regionZ = Math.floorDiv(z, REGION_SIZE);
        int minLayer = Math.floorDiv(MIN_Y, LAYER_HEIGHT) - 1;
        int maxLayer = Math.floorDiv(MAX_Y - 1, LAYER_HEIGHT) + 1;

        for (int rx = regionX - 1; rx <= regionX + 1; rx++) {
            for (int rz = regionZ - 1; rz <= regionZ + 1; rz++) {
                for (int ly = minLayer; ly <= maxLayer; ly++) {
                    Island island = islandAt(rx, rz, ly);

                    if (island != null && !island.columnAt(x, z).empty()) {
                        result.add(island);
                    }
                }
            }
        }

        return result;
    }

    public static AenderBiomeKind biomeAt(int x, int y, int z) {
        return biomeAt(islandsAtColumn(x, z), x, y, z);
    }

    static AenderBiomeKind biomeAt(List<Island> islands, int x, int y, int z) {
        return biomeFromColumns(biomeColumnsAt(islands, x, z), y);
    }

    record BiomeColumn(long islandSeed, AenderBiomeKind biome, Island.Column column) {
    }

    public static double density(int x, int y, int z) {
        double best = -10.0D;

        int regionX = Math.floorDiv(x, REGION_SIZE);
        int regionZ = Math.floorDiv(z, REGION_SIZE);
        int layerY = Math.floorDiv(y, LAYER_HEIGHT);

        for (int rx = regionX - 1; rx <= regionX + 1; rx++) {
            for (int rz = regionZ - 1; rz <= regionZ + 1; rz++) {
                for (int ly = layerY - 1; ly <= layerY + 1; ly++) {
                    Island island = islandAt(rx, rz, ly);

                    if (island == null) {
                        continue;
                    }

                    best = Math.max(best, island.densityAt(x, y, z));
                }
            }
        }

        return best;
    }

    public static int highestBlockYAt(int x, int z) {
        Island.Column column = highestColumnAt(x, z);
        return column.empty() ? MIN_Y - 1 : column.maxY();
    }

    public static int surfaceDepthAt(int x, int y, int z) {
        int bestDepth = Integer.MAX_VALUE;

        int regionX = Math.floorDiv(x, REGION_SIZE);
        int regionZ = Math.floorDiv(z, REGION_SIZE);
        int layerY = Math.floorDiv(y, LAYER_HEIGHT);

        for (int rx = regionX - 1; rx <= regionX + 1; rx++) {
            for (int rz = regionZ - 1; rz <= regionZ + 1; rz++) {
                for (int ly = layerY - 1; ly <= layerY + 1; ly++) {
                    Island island = islandAt(rx, rz, ly);

                    if (island == null) {
                        continue;
                    }

                    Island.Column column = island.columnAt(x, z);

                    if (column.empty() || y < column.minY() || y > column.maxY()) {
                        continue;
                    }

                    bestDepth = Math.min(bestDepth, column.maxY() - y);
                }
            }
        }

        return bestDepth == Integer.MAX_VALUE ? -1 : bestDepth;
    }

    private static Island.Column highestColumnAt(int x, int z) {
        Island.Column best = Island.Column.EMPTY;

        int regionX = Math.floorDiv(x, REGION_SIZE);
        int regionZ = Math.floorDiv(z, REGION_SIZE);

        int minLayer = Math.floorDiv(MIN_Y, LAYER_HEIGHT) - 1;
        int maxLayer = Math.floorDiv(MAX_Y - 1, LAYER_HEIGHT) + 1;

        for (int rx = regionX - 1; rx <= regionX + 1; rx++) {
            for (int rz = regionZ - 1; rz <= regionZ + 1; rz++) {
                for (int ly = minLayer; ly <= maxLayer; ly++) {
                    Island island = islandAt(rx, rz, ly);

                    if (island == null) {
                        continue;
                    }

                    Island.Column column = island.columnAt(x, z);

                    if (!column.empty() && (best.empty() || column.maxY() > best.maxY())) {
                        best = column;
                    }
                }
            }
        }

        return best;
    }

    private static Island islandAt(int regionX, int regionZ, int layerY) {
        long regionSeed = AenderVolatility.islandSeed(regionX, regionZ, layerY);
        long columnSeed = mix64(
                AenderVolatility.islandSeed(regionX, regionZ, 0)
                        ^ (long) regionX * 0xD1342543DE82EF95L
                        ^ (long) regionZ * 0xC6BC279692B5CC83L
        );

        long s = mix64(regionSeed
                ^ (long) regionX * 0x632BE59BD9B4E019L
                ^ (long) regionZ * 0x85157AF5C91D1B35L
                ^ (long) layerY * 0x9E3779B97F4A7C15L);

        if (unit(s ^ 0x1234L) < 0.48D) {
            return null;
        }

        int baseX = regionX * REGION_SIZE;
        int baseZ = regionZ * REGION_SIZE;
        int baseY = layerY * LAYER_HEIGHT;

        boolean stackedCluster = unit(columnSeed ^ 0x57ACCA11L) < 0.28D;
        int centerX;
        int centerZ;

        if (stackedCluster) {
            int anchorX = baseX + 96 + (int) (unit(columnSeed ^ 0xA1L) * (REGION_SIZE - 192));
            int anchorZ = baseZ + 96 + (int) (unit(columnSeed ^ 0xA2L) * (REGION_SIZE - 192));
            centerX = anchorX - 22 + (int) (unit(s ^ 0xA1L) * 44.0D);
            centerZ = anchorZ - 22 + (int) (unit(s ^ 0xA2L) * 44.0D);
        } else {
            centerX = baseX + 96 + (int) (unit(s ^ 0xA1L) * (REGION_SIZE - 192));
            centerZ = baseZ + 96 + (int) (unit(s ^ 0xA2L) * (REGION_SIZE - 192));
        }

        int regionalDrift = -18 + (int) (unit(columnSeed ^ 0xA3L) * 36.0D);
        int centerY = baseY + 26 + (int) (unit(s ^ 0xA3L) * 54.0D) + regionalDrift;

        if (centerY < 32 || centerY > MAX_Y - 32) {
            return null;
        }

        double radiusX = 68.0D + unit(s ^ 0xB1L) * 96.0D;
        double radiusZ = 68.0D + unit(s ^ 0xB2L) * 96.0D;
        double height = 34.0D + unit(s ^ 0xB3L) * 36.0D;
        AenderBiomeKind biome = AenderBiomeKind.fromIslandSeed(s);
        AenderIslandArchetype archetype = AenderIslandArchetype.fromSeed(biome, s);
        AenderUndersideProfile underside = AenderUndersideProfile.fromSeed(biome, s);

        if (biome == AenderBiomeKind.DESERT) {
            radiusX *= 1.12D;
            radiusZ *= 1.12D;
            height *= 0.72D;
        } else {
            height *= 0.90D;
        }

        if (archetype == AenderIslandArchetype.ELONGATED) {
            if (unit(s ^ 0xB6L) < 0.5D) {
                radiusX *= 1.30D;
                radiusZ *= 0.76D;
            } else {
                radiusX *= 0.76D;
                radiusZ *= 1.30D;
            }
        } else if (archetype == AenderIslandArchetype.SHELF) {
            radiusX *= 1.18D;
            radiusZ *= 1.18D;
            height *= 0.72D;
        }

        if (unit(s ^ 0xB4L) < 0.30D) {
            radiusX *= 1.28D;
            radiusZ *= 0.78D;
        } else if (unit(s ^ 0xB5L) < 0.30D) {
            radiusX *= 0.78D;
            radiusZ *= 1.28D;
        }

        return new Island(
                centerX,
                centerY,
                centerZ,
                radiusX,
                radiusZ,
                height,
                s,
                biome,
                archetype,
                underside
        );
    }

    private static double signedNoise2D(double x, double z, long seed) {
        return valueNoise2D(x, z, seed) * 2.0D - 1.0D;
    }

    private static double signedNoise3D(double x, double y, double z, long seed) {
        double a = valueNoise2D(x + y * 0.31D, z, seed);
        double b = valueNoise2D(x, z + y * 0.27D, seed ^ 0xBADC0FFEE0DDF00DL);
        return ((a + b) * 0.5D) * 2.0D - 1.0D;
    }

    private static double valueNoise2D(double x, double z, long seed) {
        int x0 = fastFloor(x);
        int z0 = fastFloor(z);
        int x1 = x0 + 1;
        int z1 = z0 + 1;

        double tx = smooth(x - x0);
        double tz = smooth(z - z0);

        double a = lerp(rand(x0, z0, seed), rand(x1, z0, seed), tx);
        double b = lerp(rand(x0, z1, seed), rand(x1, z1, seed), tx);

        return lerp(a, b, tz);
    }

    private static double rand(int x, int z, long seed) {
        return unit(seed ^ (long) x * 0x632BE59BD9B4E019L ^ (long) z * 0x85157AF5C91D1B35L);
    }

    private static double unit(long seed) {
        long value = mix64(seed);
        return (value >>> 11) * 0x1.0p-53;
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
        int i = (int) value;
        return value < i ? i - 1 : i;
    }

    private static double smooth(double value) {
        return value * value * (3.0D - 2.0D * value);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    public record Island(
            int centerX,
            int centerY,
            int centerZ,
            double radiusX,
            double radiusZ,
            double height,
            long seed,
            AenderBiomeKind biome,
            AenderIslandArchetype archetype,
            AenderUndersideProfile underside
    ) {
        private static final SurfaceProfile PLAINS_SURFACE = new SurfaceProfile(
                0.075D,
                0.060D,
                3.0D,
                1.05D,
                0.060D,
                0.30D,
                0.14D
        );
        private static final SurfaceProfile DESERT_SURFACE = new SurfaceProfile(
                0.020D,
                0.018D,
                1.25D,
                0.45D,
                0.012D,
                0.20D,
                0.09D
        );

        /*
         * columnAt() first displaces a column by up to 0.375 of the island
         * radius, then applies a coast scale of up to 1.48 and lobes of up to
         * 1.066. That gives a maximum horizontal reach of:
         *
         *   0.375 + 1.48 * 1.066 = 1.95268 radii
         *
         * These bounds are used both to decide which chunks contain an island
         * and to limit terrain generation. They therefore have to be
         * conservative; an underestimated bound clips the island into the huge
         * flat walls seen at chunk boundaries. Satellite centers stay within
         * 1.44 radii and their own radius is at most 0.35 radii, so the same
         * two-radius bound also contains detached fragments.
         */
        private static final double HORIZONTAL_BOUND_SCALE = 2.0D;
        private static final double LOWER_VERTICAL_BOUND_SCALE = 2.50D;
        private static final double UPPER_VERTICAL_BOUND_SCALE = 0.35D;
        private static final double BOUND_MARGIN = 32.0D;

        public int minX() {
            return (int) Math.floor(centerX - radiusX * HORIZONTAL_BOUND_SCALE - BOUND_MARGIN);
        }

        public int maxX() {
            return (int) Math.ceil(centerX + radiusX * HORIZONTAL_BOUND_SCALE + BOUND_MARGIN);
        }

        public int minY() {
            return Math.max(
                    MIN_Y,
                    (int) Math.floor(centerY - height * LOWER_VERTICAL_BOUND_SCALE - BOUND_MARGIN)
            );
        }

        public int maxY() {
            return Math.min(
                    MAX_Y - 1,
                    (int) Math.ceil(centerY + height * UPPER_VERTICAL_BOUND_SCALE + BOUND_MARGIN)
            );
        }

        public int minZ() {
            return (int) Math.floor(centerZ - radiusZ * HORIZONTAL_BOUND_SCALE - BOUND_MARGIN);
        }

        public int maxZ() {
            return (int) Math.ceil(centerZ + radiusZ * HORIZONTAL_BOUND_SCALE + BOUND_MARGIN);
        }

        public Column columnAt(int x, int z) {
            SurfaceProfile surface = biome == AenderBiomeKind.DESERT ? DESERT_SURFACE : PLAINS_SURFACE;
            double lx = x - centerX;
            double lz = z - centerZ;

            double warpX =
                    signedNoise2D(x * 0.010D, z * 0.010D, seed ^ 0x1010L) * radiusX * 0.25D +
                            signedNoise2D(x * 0.031D, z * 0.031D, seed ^ 0x1011L) * radiusX * 0.10D +
                            signedNoise2D(x * 0.070D, z * 0.070D, seed ^ 0x1012L) * radiusX * 0.025D;

            double warpZ =
                    signedNoise2D(x * 0.010D, z * 0.010D, seed ^ 0x2020L) * radiusZ * 0.25D +
                            signedNoise2D(x * 0.031D, z * 0.031D, seed ^ 0x2021L) * radiusZ * 0.10D +
                            signedNoise2D(x * 0.070D, z * 0.070D, seed ^ 0x2022L) * radiusZ * 0.025D;

            double coast =
                    1.0D +
                            signedNoise2D(x * 0.007D, z * 0.007D, seed ^ 0x3030L) * 0.28D +
                            signedNoise2D(x * 0.020D, z * 0.020D, seed ^ 0x3031L) * 0.16D +
                            signedNoise2D(x * 0.052D, z * 0.052D, seed ^ 0x3032L) * 0.045D;

            coast = clamp(coast, 0.60D, 1.48D);

            double dx = (lx + warpX) / (radiusX * coast);
            double dz = (lz + warpZ) / (radiusZ * coast);

            Footprint footprint = footprintAt(dx, dz);
            Column result = Column.EMPTY;

            if (footprint.present() && !isErodedOpening(footprint.rotatedX(), footprint.rotatedZ())) {
                result = terrainColumnAt(
                        x,
                        z,
                        footprint.radius(),
                        footprint.rotatedX(),
                        footprint.rotatedZ(),
                        surface
                );
            }

            int satellites = satelliteCount();

            for (int satellite = 0; satellite < satellites; satellite++) {
                result = Column.union(result, satelliteColumnAt(x, z, satellite));
            }

            return result;
        }

        private Footprint footprintAt(double dx, double dz) {
            RotatedPoint rotated = rotatedPoint(dx, dz);
            double rotatedX = rotated.x();
            double rotatedZ = rotated.z();
            double radius;

            if (archetype == AenderIslandArchetype.TWIN) {
                double leftX = (rotatedX + 0.31D) / 0.82D;
                double rightX = (rotatedX - 0.31D) / 0.82D;
                double twinZ = rotatedZ / 0.92D;
                radius = Math.min(
                        Math.sqrt(leftX * leftX + twinZ * twinZ),
                        Math.sqrt(rightX * rightX + twinZ * twinZ)
                );
            } else {
                radius = Math.sqrt(dx * dx + dz * dz);
            }

            double angle = Math.atan2(dz, dx);
            double lobes =
                    Math.sin(angle * 3.0D + unit(seed ^ 0x6060L) * Math.PI * 2.0D) * 0.040D +
                            Math.sin(angle * 7.0D + unit(seed ^ 0x6061L) * Math.PI * 2.0D) * 0.026D;

            if (radius > 1.0D + lobes) {
                return Footprint.EMPTY;
            }

            if (archetype == AenderIslandArchetype.CRESCENT) {
                double biteX = (rotatedX - 0.28D) / 0.56D;
                double biteZ = rotatedZ / 0.72D;

                if (biteX * biteX + biteZ * biteZ < 1.0D && rotatedX > -0.18D) {
                    return Footprint.EMPTY;
                }
            } else if (archetype == AenderIslandArchetype.SPLIT) {
                double splitWidth = 0.075D
                        + signedNoise2D(rotatedX * 2.8D, rotatedZ * 2.8D, seed ^ 0x5A117L) * 0.028D;

                if (Math.abs(rotatedZ) < splitWidth && Math.abs(rotatedX) < 1.05D) {
                    return Footprint.EMPTY;
                }
            }

            return new Footprint(true, radius, rotatedX, rotatedZ);
        }

        private RotatedPoint rotatedPoint(double dx, double dz) {
            double orientation = unit(seed ^ 0x0A71E17L) * Math.PI * 2.0D;
            double cos = Math.cos(orientation);
            double sin = Math.sin(orientation);
            double rotatedX = dx * cos + dz * sin;
            double rotatedZ = -dx * sin + dz * cos;
            return new RotatedPoint(rotatedX, rotatedZ);
        }

        private boolean isErodedOpening(double rotatedX, double rotatedZ) {
            for (int opening = 0; opening < erosionOpeningCount(); opening++) {
                ErosionOpening shape = erosionOpening(opening);
                double dx = (rotatedX - shape.centerX()) / shape.radiusX();
                double dz = (rotatedZ - shape.centerZ()) / shape.radiusZ();

                if (dx * dx + dz * dz < 1.0D) {
                    return true;
                }
            }

            return false;
        }

        private int erosionOpeningCount() {
            if (!archetype.hasStrongErosion() && unit(seed ^ 0xE20510A1L) >= 0.24D) {
                return 0;
            }

            return archetype.hasStrongErosion() ? 3 : 1;
        }

        private ErosionOpening erosionOpening(int index) {
            long openingSeed = mix64(seed ^ 0xE20510A1L ^ index * 0x632BE59BD9B4E019L);
            double angle = unit(openingSeed ^ 0x11L) * Math.PI * 2.0D;
            double distance = 0.34D + unit(openingSeed ^ 0x12L) * 0.46D;
            return new ErosionOpening(
                    Math.cos(angle) * distance,
                    Math.sin(angle) * distance,
                    0.055D + unit(openingSeed ^ 0x13L) * 0.10D,
                    0.055D + unit(openingSeed ^ 0x14L) * 0.10D,
                    openingSeed
            );
        }

        private Column terrainColumnAt(
                int x,
                int z,
                double radius,
                double shapeX,
                double shapeZ,
                SurfaceProfile surface
        ) {
            double r = clamp(radius, 0.0D, 1.2D);

            double rim = smoothStep(0.56D, 1.0D, r);
            double core = 1.0D - smoothStep(0.0D, 0.72D, r);
            double mid = 1.0D - Math.abs(r - 0.42D) / 0.42D;
            mid = clamp(mid, 0.0D, 1.0D);

            double topY = centerY + 5.0D;
            topY += core * height * surface.coreRise();
            topY += mid
                    * signedNoise2D(x * 0.012D, z * 0.012D, seed ^ 0x4042L)
                    * height
                    * surface.midVariation();
            topY -= rim * height * surface.rimDrop();
            topY -= smoothStep(0.88D, 1.0D, r) * height * surface.edgeDrop();

            topY += signedNoise2D(x * 0.017D, z * 0.017D, seed ^ 0x4040L)
                    * surface.broadVariation();
            topY += signedNoise2D(x * 0.043D, z * 0.043D, seed ^ 0x4041L)
                    * surface.detailVariation()
                    * (0.35D + rim);
            topY += ridgeAt(shapeX, shapeZ, seed ^ 0x7070L) * height * surface.ridgeRise();

            double basinNoise = signedNoise2D(x * 0.008D, z * 0.008D, seed ^ 0xBA51A5L);

            if (biome == AenderBiomeKind.DESERT && basinNoise < -0.42D) {
                topY -= smoothStep(0.42D, 0.92D, -basinNoise) * 3.2D;
            } else if (biome == AenderBiomeKind.PLAINS && basinNoise < -0.55D) {
                topY -= smoothStep(0.55D, 0.94D, -basinNoise) * 2.0D;
            }

            if (archetype == AenderIslandArchetype.DUNES) {
                topY += duneHeightAt(x, z);
            } else if (archetype == AenderIslandArchetype.ERODED_MESA && r > 0.32D) {
                topY = Math.floor(topY / 2.0D) * 2.0D;
            } else if (archetype == AenderIslandArchetype.PLATEAU) {
                topY = centerY + 5.0D + (topY - (centerY + 5.0D)) * (0.42D + rim * 0.38D);
            }

            double thickness = height * (1.16D - 0.78D * Math.pow(r, 1.55D));
            thickness += core * height * 0.18D;
            thickness += ridgeAt(
                    shapeX * 1.2D + 0.19D,
                    shapeZ * 1.2D - 0.11D,
                    seed ^ 0x9090L
            ) * height * 0.20D;
            thickness += signedNoise2D(x * 0.014D, z * 0.014D, seed ^ 0x5050L) * height * 0.09D;
            thickness -= smoothStep(0.82D, 1.0D, r) * height * 0.18D;

            thickness = undersideThickness(thickness, core, r, x, z);

            double hangingRoot = signedNoise2D(x * 0.050D, z * 0.050D, seed ^ 0xA0A0L);
            if (r < 0.78D && hangingRoot > 0.55D) {
                thickness += (hangingRoot - 0.55D) * height * 0.55D;
            }

            thickness = Math.max(archetype == AenderIslandArchetype.SHELF ? 4.0D : 6.0D, thickness);

            double bottomY = topY - thickness;

            int minY = Math.max(MIN_Y, (int) Math.floor(bottomY));
            int maxY = Math.min(MAX_Y - 1, (int) Math.floor(topY));

            return maxY < minY ? Column.EMPTY : new Column(minY, maxY);
        }

        private double duneHeightAt(int x, int z) {
            double orientation = unit(seed ^ 0xD00E0A1L) * Math.PI;
            double along = x * Math.cos(orientation) + z * Math.sin(orientation);
            double broad = Math.sin(along * 0.052D + unit(seed ^ 0xD00E0A2L) * Math.PI * 2.0D);
            double crossing = Math.sin(along * 0.021D + unit(seed ^ 0xD00E0A3L) * Math.PI * 2.0D);
            return broad * 1.35D + crossing * 0.70D;
        }

        private double undersideThickness(double base, double core, double r, int x, int z) {
            return switch (underside) {
                case TAPERED -> base + Math.pow(core, 1.7D) * height * 0.34D;
                case ROOTED -> base
                        + core * height * 0.28D
                        + Math.max(0.0D, signedNoise2D(x * 0.035D, z * 0.035D, seed ^ 0x20075L))
                        * height
                        * 0.22D;
                case FRACTURED -> base
                        + signedNoise2D(x * 0.028D, z * 0.028D, seed ^ 0xF2AC7L) * height * 0.19D;
                case TERRACED -> Math.floor((base + (1.0D - r) * height * 0.08D) / 4.0D) * 4.0D;
            };
        }

        int satelliteCount() {
            double roll = unit(seed ^ 0x5A7E1117EL);

            if (roll < 0.42D) {
                return 0;
            }
            if (roll < 0.78D) {
                return 1;
            }
            return roll < 0.94D ? 2 : 3;
        }

        private Column satelliteColumnAt(int x, int z, int index) {
            Satellite satellite = satellite(index);
            double localX = x - satellite.centerX();
            double localZ = z - satellite.centerZ();
            double warpX = signedNoise2D(
                    x * 0.021D,
                    z * 0.021D,
                    satellite.seed() ^ 0x21L
            ) * satellite.radiusX() * 0.22D;
            double warpZ = signedNoise2D(
                    x * 0.021D,
                    z * 0.021D,
                    satellite.seed() ^ 0x22L
            ) * satellite.radiusZ() * 0.22D;
            double coast = clamp(
                    1.0D
                            + signedNoise2D(x * 0.035D, z * 0.035D, satellite.seed() ^ 0x23L) * 0.20D
                            + signedNoise2D(x * 0.090D, z * 0.090D, satellite.seed() ^ 0x24L) * 0.07D,
                    0.68D,
                    1.30D
            );
            double dx = (localX + warpX) / (satellite.radiusX() * coast);
            double dz = (localZ + warpZ) / (satellite.radiusZ() * coast);
            double r = Math.sqrt(dx * dx + dz * dz);
            double angle = Math.atan2(dz, dx);
            double lobes = Math.sin(angle * 3.0D + unit(satellite.seed() ^ 0x25L) * Math.PI * 2.0D) * 0.10D
                    + Math.sin(angle * 5.0D + unit(satellite.seed() ^ 0x26L) * Math.PI * 2.0D) * 0.055D;

            if (r > 1.0D + lobes || isSatelliteBite(dx, dz, satellite.seed())) {
                return Column.EMPTY;
            }

            double core = 1.0D - smoothStep(0.0D, 0.72D, r);
            double rim = smoothStep(0.58D, 1.0D, r);
            double top = satellite.centerY() + 2.0D
                    + core * satellite.height() * 0.10D
                    - rim * satellite.height() * 0.24D
                    + signedNoise2D(x * 0.025D, z * 0.025D, satellite.seed() ^ 0x27L) * 2.2D
                    + signedNoise2D(x * 0.075D, z * 0.075D, satellite.seed() ^ 0x28L) * 0.9D
                    + ridgeAt(dx, dz, satellite.seed() ^ 0x29L) * satellite.height() * 0.08D;
            double thickness = Math.max(
                    5.0D,
                    satellite.height() * (0.90D - 0.55D * Math.pow(r, 1.4D))
                            + signedNoise2D(x * 0.045D, z * 0.045D, satellite.seed() ^ 0x2AL)
                            * satellite.height()
                            * 0.12D
            );
            int minY = Math.max(MIN_Y, (int) Math.floor(top - thickness));
            int maxY = Math.min(MAX_Y - 1, (int) Math.floor(top));
            return maxY < minY ? Column.EMPTY : new Column(minY, maxY);
        }

        private Satellite satellite(int index) {
            long satelliteSeed = mix64(seed ^ 0x5A7E1117EL ^ index * 0x9E3779B97F4A7C15L);
            double angle = unit(satelliteSeed ^ 0x11L) * Math.PI * 2.0D;
            double distance = 1.10D + unit(satelliteSeed ^ 0x12L) * 0.34D;
            double scale = 0.13D + unit(satelliteSeed ^ 0x13L) * 0.14D;
            double stretchX = 0.72D + unit(satelliteSeed ^ 0x15L) * 0.55D;
            double stretchZ = 0.72D + unit(satelliteSeed ^ 0x16L) * 0.55D;
            int satelliteX = centerX + (int) Math.round(Math.cos(angle) * radiusX * distance);
            int satelliteZ = centerZ + (int) Math.round(Math.sin(angle) * radiusZ * distance);
            int satelliteY = centerY - 8 + (int) (unit(satelliteSeed ^ 0x14L) * 17.0D);
            return new Satellite(
                    satelliteX,
                    satelliteY,
                    satelliteZ,
                    Math.max(9.0D, radiusX * scale * stretchX),
                    Math.max(9.0D, radiusZ * scale * stretchZ),
                    Math.max(9.0D, height * (0.24D + scale * 0.45D)),
                    satelliteSeed
            );
        }

        private boolean isSatelliteBite(double dx, double dz, long satelliteSeed) {
            if (unit(satelliteSeed ^ 0x31L) >= 0.46D) {
                return false;
            }

            double angle = unit(satelliteSeed ^ 0x32L) * Math.PI * 2.0D;
            double centerX = Math.cos(angle) * 0.62D;
            double centerZ = Math.sin(angle) * 0.62D;
            double radiusX = 0.12D + unit(satelliteSeed ^ 0x33L) * 0.13D;
            double radiusZ = 0.12D + unit(satelliteSeed ^ 0x34L) * 0.13D;
            double biteX = (dx - centerX) / radiusX;
            double biteZ = (dz - centerZ) / radiusZ;
            return biteX * biteX + biteZ * biteZ < 1.0D;
        }

        // Fallback pro staré metody typu getBaseHeight/getBaseColumn.
        public double densityAt(int x, int y, int z) {
            Column column = columnAt(x, z);

            if (column.empty()) {
                return -1.0D;
            }

            return y >= column.minY && y <= column.maxY ? 1.0D : -1.0D;
        }

        private static double smoothStep(double edge0, double edge1, double value) {
            double t = clamp((value - edge0) / (edge1 - edge0), 0.0D, 1.0D);
            return t * t * (3.0D - 2.0D * t);
        }

        private static double ridgeAt(double x, double z, long seed) {
            double a = signedNoise2D(x * 1.4D + 19.0D, z * 1.4D - 7.0D, seed);
            double b = signedNoise2D(x * 2.2D - 3.0D, z * 2.2D + 11.0D, seed ^ 0x5F3759DFL);
            double ridge = 1.0D - Math.abs(a * 0.72D + b * 0.28D);
            return smoothStep(0.58D, 0.92D, ridge);
        }

        private static double clamp(double value, double min, double max) {
            return Math.max(min, Math.min(max, value));
        }

        private record SurfaceProfile(
                double coreRise,
                double midVariation,
                double broadVariation,
                double detailVariation,
                double ridgeRise,
                double rimDrop,
                double edgeDrop
        ) {
        }

        private record Footprint(boolean present, double radius, double rotatedX, double rotatedZ) {
            private static final Footprint EMPTY = new Footprint(false, 0.0D, 0.0D, 0.0D);
        }

        private record RotatedPoint(double x, double z) {
        }

        private record ErosionOpening(
                double centerX,
                double centerZ,
                double radiusX,
                double radiusZ,
                long seed
        ) {
        }

        private record Satellite(
                int centerX,
                int centerY,
                int centerZ,
                double radiusX,
                double radiusZ,
                double height,
                long seed
        ) {
        }

        public record Column(int minY, int maxY) {
            public static final Column EMPTY = new Column(1, 0);

            private static Column union(Column first, Column second) {
                if (first.empty()) {
                    return second;
                }
                if (second.empty()) {
                    return first;
                }
                return new Column(Math.min(first.minY, second.minY), Math.max(first.maxY, second.maxY));
            }

            public boolean empty() {
                return minY > maxY;
            }
        }
    }
}
