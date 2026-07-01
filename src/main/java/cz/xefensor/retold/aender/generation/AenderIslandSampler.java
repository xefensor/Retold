package cz.xefensor.retold.aender.generation;
import net.minecraft.world.level.chunk.ChunkAccess;
import java.util.ArrayList;
import java.util.List;

public final class AenderIslandSampler {
    public static final int MIN_Y = 0;
    public static final int HEIGHT = 384;
    public static final int MAX_Y = MIN_Y + HEIGHT;

    public static final int REGION_SIZE = 384;
    public static final int LAYER_HEIGHT = 96;

    private AenderIslandSampler() {
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

                    double dx = (x - island.centerX) / island.radiusX;
                    double dz = (z - island.centerZ) / island.radiusZ;
                    double dy = (y - island.centerY) / island.height;

                    double horizontal = dx * dx + dz * dz;
                    double vertical = dy * dy * 1.75D;

                    double edgeNoise = signedNoise2D(x * 0.018D, z * 0.018D, island.seed) * 0.22D;
                    double roughness = signedNoise3D(x * 0.055D, y * 0.035D, z * 0.055D, island.seed ^ 0x91E10DA5L) * 0.12D;

                    double shape = 1.0D - horizontal - vertical + edgeNoise + roughness;

                    // Flatten and strengthen the top a little, End-island style.
                    if (y <= island.centerY + 5 && y >= island.centerY - island.height * 0.45D) {
                        shape += 0.18D;
                    }

                    // Taper underside.
                    if (y < island.centerY - island.height * 0.55D) {
                        shape -= ((island.centerY - island.height * 0.55D) - y) / island.height * 0.45D;
                    }

                    best = Math.max(best, shape);
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

        // Empty space. Lower means more islands.
        if (unit(s ^ 0x1234L) < 0.48D) {
            return null;
        }

        int baseX = regionX * REGION_SIZE;
        int baseZ = regionZ * REGION_SIZE;
        int baseY = layerY * LAYER_HEIGHT;

        int centerX = baseX + 96 + (int) (unit(s ^ 0xA1L) * (REGION_SIZE - 192));
        int centerZ = baseZ + 96 + (int) (unit(s ^ 0xA2L) * (REGION_SIZE - 192));
        int centerY = baseY + 32 + (int) (unit(s ^ 0xA3L) * 64);

        // Clamp into dimension.
        if (centerY < 32 || centerY > MAX_Y - 32) {
            return null;
        }

        double radiusX = 80.0D + unit(s ^ 0xB1L) * 130.0D;
        double radiusZ = 80.0D + unit(s ^ 0xB2L) * 130.0D;
        double height = 22.0D + unit(s ^ 0xB3L) * 42.0D;

        return new Island(centerX, centerY, centerZ, radiusX, radiusZ, height, s);
    }

    private static double signedNoise2D(double x, double z, long seed) {
        return valueNoise2D(x, z, seed) * 2.0D - 1.0D;
    }

    private static double signedNoise3D(double x, double y, double z, long seed) {
        // Cheap fake 3D noise from layered 2D noise.
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
        public int minX() {
            return (int) Math.floor(centerX - radiusX - 12.0D);
        }

        public int maxX() {
            return (int) Math.ceil(centerX + radiusX + 12.0D);
        }

        public int minY() {
            return Math.max(MIN_Y, (int) Math.floor(centerY - height - 12.0D));
        }

        public int maxY() {
            return Math.min(MAX_Y - 1, (int) Math.ceil(centerY + height + 12.0D));
        }

        public int minZ() {
            return (int) Math.floor(centerZ - radiusZ - 12.0D);
        }

        public int maxZ() {
            return (int) Math.ceil(centerZ + radiusZ + 12.0D);
        }

        public double densityAt(int x, int y, int z) {
            double dx = (x - centerX) / radiusX;
            double dz = (z - centerZ) / radiusZ;
            double dy = (y - centerY) / height;

            double horizontal = dx * dx + dz * dz;
            double vertical = dy * dy * 1.75D;

            double edgeNoise = signedNoise2D(x * 0.018D, z * 0.018D, seed) * 0.22D;
            double roughness = signedNoise3D(x * 0.055D, y * 0.035D, z * 0.055D, seed ^ 0x91E10DA5L) * 0.12D;

            double shape = 1.0D - horizontal - vertical + edgeNoise + roughness;

            if (y <= centerY + 5 && y >= centerY - height * 0.45D) {
                shape += 0.18D;
            }

            if (y < centerY - height * 0.55D) {
                shape -= ((centerY - height * 0.55D) - y) / height * 0.45D;
            }

            return shape;
        }
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
}