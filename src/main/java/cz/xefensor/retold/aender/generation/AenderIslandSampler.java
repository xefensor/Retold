package cz.xefensor.retold.aender.generation;

import net.minecraft.util.Mth;
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

        double radiusX = 75.0D + unit(s ^ 0xB1L) * 80.0D;
        double radiusZ = 75.0D + unit(s ^ 0xB2L) * 80.0D;
        double height = 34.0D + unit(s ^ 0xB3L) * 26.0D;

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
        public int minX() {
            return (int) Math.floor(centerX - radiusX * 1.60D - 24.0D);
        }

        public int maxX() {
            return (int) Math.ceil(centerX + radiusX * 1.60D + 24.0D);
        }

        public int minY() {
            return Math.max(MIN_Y, (int) Math.floor(centerY - height * 1.35D - 24.0D));
        }

        public int maxY() {
            return Math.min(MAX_Y - 1, (int) Math.ceil(centerY + height * 0.45D + 24.0D));
        }

        public int minZ() {
            return (int) Math.floor(centerZ - radiusZ * 1.60D - 24.0D);
        }

        public int maxZ() {
            return (int) Math.ceil(centerZ + radiusZ * 1.60D + 24.0D);
        }

        public double densityAt(int x, int y, int z) {
            double lx = x - centerX;
            double lz = z - centerZ;

            // Domain warp: rozbije kulatý obrys ještě před výpočtem vzdálenosti.
            double warpX =
                    signedNoise2D(x * 0.012D, z * 0.012D, seed ^ 0x1010L) * radiusX * 0.16D +
                            signedNoise2D(x * 0.031D, z * 0.031D, seed ^ 0x1011L) * radiusX * 0.06D;

            double warpZ =
                    signedNoise2D(x * 0.012D, z * 0.012D, seed ^ 0x2020L) * radiusZ * 0.16D +
                            signedNoise2D(x * 0.031D, z * 0.031D, seed ^ 0x2021L) * radiusZ * 0.06D;

            double wx = lx + warpX;
            double wz = lz + warpZ;

            double angle = Math.atan2(wz / radiusZ, wx / radiusX);
            double coast = coastScale(angle);

            double nx = wx / (radiusX * coast);
            double nz = wz / (radiusZ * coast);
            double r = Math.sqrt(nx * nx + nz * nz);

            // Tohle je hlavní rozdíl:
            // žádný kulatý kopec, ale skoro plochý top a pokles až u kraje.
            double rim = smoothStep(0.52D, 1.0D, r);

            double topY = centerY + 5.0D;

            // Okraje jsou trochu níž, střed zůstane použitelně plochý.
            topY -= rim * height * 0.24D;

            // Jemné zvlnění povrchu, hlavně u pobřeží.
            topY += signedNoise2D(x * 0.026D, z * 0.026D, seed ^ 0x3030L) * 2.0D;
            topY += signedNoise2D(x * 0.009D, z * 0.009D, seed ^ 0x3031L) * 5.0D * smoothStep(0.42D, 1.0D, r);

            // Ostrov je uprostřed silnější, u okrajů tenčí.
            double clampedR = Math.min(r, 1.15D);
            double thickness = height * (1.02D - 0.58D * Math.pow(clampedR, 1.65D));

            thickness += signedNoise2D(x * 0.018D, z * 0.018D, seed ^ 0x4040L) * height * 0.08D;
            thickness = Math.max(10.0D, thickness);

            double bottomY = topY - thickness;

            // Čím níž jdeš, tím víc se ostrov zužuje.
            double vertical01 = (topY - y) / Math.max(1.0D, topY - bottomY);
            double bottomTaper = 0.30D * smoothStep(0.25D, 1.0D, vertical01);

            double sideDistance = (1.0D - r - bottomTaper) * 30.0D;
            double topDistance = topY - y;
            double bottomDistance = y - bottomY;

            double density = Math.min(sideDistance, Math.min(topDistance, bottomDistance));

            // Jemné vykousnutí zespoda / z boků, ale ne tak moc, aby to celý rozbilo.
            if (r > 0.55D && y < centerY - height * 0.12D) {
                double undercutNoise = signedNoise3D(
                        x * 0.030D,
                        y * 0.040D,
                        z * 0.030D,
                        seed ^ 0x5050L
                );

                if (undercutNoise > 0.45D) {
                    density -= (undercutNoise - 0.45D) * 4.0D * smoothStep(0.55D, 0.95D, r);
                }
            }

            return density;
        }

        private double coastScale(double angle) {
            double cx = Math.cos(angle);
            double cz = Math.sin(angle);

            double scale = 1.0D;

            // Velké nepravidelnosti obrysu.
            scale += signedNoise2D(cx * 1.6D, cz * 1.6D, seed ^ 0x7000L) * 0.26D;

            // Menší nepravidelnosti obrysu.
            scale += signedNoise2D(cx * 4.3D, cz * 4.3D, seed ^ 0x7001L) * 0.16D;

            // Výběžky.
            for (int i = 0; i < 4; i++) {
                long s = seed ^ (0x8000L + i * 97L);

                double lobeAngle = unit(s ^ 0x01L) * Math.PI * 2.0D;
                double width = 0.20D + unit(s ^ 0x02L) * 0.22D;
                double power = 0.14D + unit(s ^ 0x03L) * 0.20D;

                double d = angleDistance(angle, lobeAngle);
                scale += Math.exp(-(d * d) / (width * width)) * power;
            }

            // Zálivy / zářezy.
            for (int i = 0; i < 5; i++) {
                long s = seed ^ (0x9000L + i * 113L);

                double biteAngle = unit(s ^ 0x11L) * Math.PI * 2.0D;
                double width = 0.12D + unit(s ^ 0x12L) * 0.20D;
                double power = 0.12D + unit(s ^ 0x13L) * 0.18D;

                double d = angleDistance(angle, biteAngle);
                scale -= Math.exp(-(d * d) / (width * width)) * power;
            }

            return clamp(scale, 0.58D, 1.48D);
        }

        private static double angleDistance(double a, double b) {
            double d = Math.abs(a - b) % (Math.PI * 2.0D);
            return d > Math.PI ? Math.PI * 2.0D - d : d;
        }

        private static double smoothStep(double edge0, double edge1, double value) {
            double t = clamp((value - edge0) / (edge1 - edge0), 0.0D, 1.0D);
            return t * t * (3.0D - 2.0D * t);
        }

        private static double clamp(double value, double min, double max) {
            return Math.max(min, Math.min(max, value));
        }
    }
}