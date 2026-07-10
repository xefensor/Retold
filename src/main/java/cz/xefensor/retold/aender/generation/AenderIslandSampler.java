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

    private static final int BOUND_MARGIN = 64;

    private AenderIslandSampler() {
    }

    public static List<Island> islandsForChunk(ChunkAccess chunk) {
        List<Island> result = new ArrayList<>();

        int minX = chunk.getPos().getMinBlockX();
        int maxX = minX + 15;
        int minZ = chunk.getPos().getMinBlockZ();
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

        int centerX = baseX + 96 + (int) (unit(s ^ 0xA1L) * (REGION_SIZE - 192));
        int centerZ = baseZ + 96 + (int) (unit(s ^ 0xA2L) * (REGION_SIZE - 192));
        int centerY = baseY + 32 + (int) (unit(s ^ 0xA3L) * 64);

        if (centerY < 32 || centerY > MAX_Y - 32) {
            return null;
        }

        double radiusX = 68.0D + unit(s ^ 0xB1L) * 96.0D;
        double radiusZ = 68.0D + unit(s ^ 0xB2L) * 96.0D;
        double height = 34.0D + unit(s ^ 0xB3L) * 36.0D;

        if (unit(s ^ 0xB4L) < 0.30D) {
            radiusX *= 1.28D;
            radiusZ *= 0.78D;
        } else if (unit(s ^ 0xB5L) < 0.30D) {
            radiusX *= 0.78D;
            radiusZ *= 1.28D;
        }

        return new Island(centerX, centerY, centerZ, radiusX, radiusZ, height, s);
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
            long seed
    ) {
        private static final double BOUND_SCALE = 1.48D;
        private static final double BOUND_MARGIN = 32.0D;

        public int minX() {
            return (int) Math.floor(centerX - radiusX * BOUND_SCALE - BOUND_MARGIN);
        }

        public int maxX() {
            return (int) Math.ceil(centerX + radiusX * BOUND_SCALE + BOUND_MARGIN);
        }

        public int minY() {
            return Math.max(MIN_Y, (int) Math.floor(centerY - height * 1.20D - BOUND_MARGIN));
        }

        public int maxY() {
            return Math.min(MAX_Y - 1, (int) Math.ceil(centerY + height * 0.35D + BOUND_MARGIN));
        }

        public int minZ() {
            return (int) Math.floor(centerZ - radiusZ * BOUND_SCALE - BOUND_MARGIN);
        }

        public int maxZ() {
            return (int) Math.ceil(centerZ + radiusZ * BOUND_SCALE + BOUND_MARGIN);
        }

        public Column columnAt(int x, int z) {
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

            double r = Math.sqrt(dx * dx + dz * dz);
            double angle = Math.atan2(dz, dx);
            double lobes =
                    Math.sin(angle * 3.0D + unit(seed ^ 0x6060L) * Math.PI * 2.0D) * 0.040D +
                            Math.sin(angle * 7.0D + unit(seed ^ 0x6061L) * Math.PI * 2.0D) * 0.026D;

            if (r > 1.0D + lobes) {
                return Column.EMPTY;
            }

            double rim = smoothStep(0.56D, 1.0D, r);
            double core = 1.0D - smoothStep(0.0D, 0.72D, r);
            double mid = 1.0D - Math.abs(r - 0.42D) / 0.42D;
            mid = clamp(mid, 0.0D, 1.0D);

            double topY = centerY + 5.0D;
            topY += core * height * 0.11D;
            topY += mid * signedNoise2D(x * 0.012D, z * 0.012D, seed ^ 0x4042L) * height * 0.10D;
            topY -= rim * height * 0.34D;
            topY -= smoothStep(0.88D, 1.0D, r) * height * 0.18D;

            topY += signedNoise2D(x * 0.017D, z * 0.017D, seed ^ 0x4040L) * 4.0D;
            topY += signedNoise2D(x * 0.043D, z * 0.043D, seed ^ 0x4041L) * 1.8D * (0.35D + rim);
            topY += ridgeAt(dx, dz, seed ^ 0x7070L) * height * 0.11D;

            double terrace = signedNoise2D(x * 0.010D, z * 0.010D, seed ^ 0x8080L);
            if (r > 0.34D && terrace > 0.22D) {
                topY = Math.floor(topY / 3.0D) * 3.0D;
            }

            double thickness = height * (1.16D - 0.78D * Math.pow(r, 1.55D));
            thickness += core * height * 0.18D;
            thickness += ridgeAt(dx * 1.2D + 0.19D, dz * 1.2D - 0.11D, seed ^ 0x9090L) * height * 0.20D;
            thickness += signedNoise2D(x * 0.014D, z * 0.014D, seed ^ 0x5050L) * height * 0.09D;
            thickness -= smoothStep(0.82D, 1.0D, r) * height * 0.18D;

            double hangingRoot = signedNoise2D(x * 0.050D, z * 0.050D, seed ^ 0xA0A0L);
            if (r < 0.78D && hangingRoot > 0.55D) {
                thickness += (hangingRoot - 0.55D) * height * 0.55D;
            }

            thickness = Math.max(6.0D, thickness);

            double bottomY = topY - thickness;

            int minY = Math.max(MIN_Y, (int) Math.floor(bottomY));
            int maxY = Math.min(MAX_Y - 1, (int) Math.floor(topY));

            if (maxY < minY) {
                return Column.EMPTY;
            }

            return new Column(minY, maxY);
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

        public record Column(int minY, int maxY) {
            public static final Column EMPTY = new Column(1, 0);

            public boolean empty() {
                return minY > maxY;
            }
        }
    }
}
