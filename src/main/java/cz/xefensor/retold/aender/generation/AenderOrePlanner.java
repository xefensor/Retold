package cz.xefensor.retold.aender.generation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Pure, coordinate-deterministic planning for Aenderite veins. */
final class AenderOrePlanner {
    private static final int CELL_SIZE = 12;
    private static final int MAX_VEIN_REACH = 12;
    /*
     * Vanilla diamond placement attempts weight seven size-4 deposits, six
     * size-8 deposits, and one size-12 deposit per nine chunks. Aenderite uses
     * the same relative profile while its island-aware planner supplies the
     * horizontal frequency.
     */
    private static final double SMALL_VEIN_SHARE = 7.0D / (13.0D + 1.0D / 9.0D);
    private static final double LARGE_VEIN_SHARE = (1.0D / 9.0D) / (13.0D + 1.0D / 9.0D);

    private AenderOrePlanner() {
    }

    static List<Vein> veinsForChunk(
            List<AenderIslandSampler.Island> islands,
            int chunkX,
            int chunkZ
    ) {
        int chunkMinX = chunkX << 4;
        int chunkMaxX = chunkMinX + 15;
        int chunkMinZ = chunkZ << 4;
        int chunkMaxZ = chunkMinZ + 15;
        int minCellX = Math.floorDiv(chunkMinX - MAX_VEIN_REACH, CELL_SIZE);
        int maxCellX = Math.floorDiv(chunkMaxX + MAX_VEIN_REACH, CELL_SIZE);
        int minCellZ = Math.floorDiv(chunkMinZ - MAX_VEIN_REACH, CELL_SIZE);
        int maxCellZ = Math.floorDiv(chunkMaxZ + MAX_VEIN_REACH, CELL_SIZE);
        List<Vein> veins = new ArrayList<>();

        for (AenderIslandSampler.Island island : islands) {
            for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
                for (int cellZ = minCellZ; cellZ <= maxCellZ; cellZ++) {
                    Vein vein = planVein(island, cellX, cellZ);

                    if (vein != null && vein.intersects(chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ)) {
                        veins.add(vein);
                    }
                }
            }
        }

        return List.copyOf(veins);
    }

    private static Vein planVein(AenderIslandSampler.Island island, int cellX, int cellZ) {
        long seed = mix64(
                island.seed()
                        ^ (long) cellX * 0x632BE59BD9B4E019L
                        ^ (long) cellZ * 0x85157AF5C91D1B35L
                        ^ 0xAE6DE217E0B5C41DL
        );

        int x = cellX * CELL_SIZE + (int) (unit(seed ^ 0x12L) * CELL_SIZE);
        int z = cellZ * CELL_SIZE + (int) (unit(seed ^ 0x13L) * CELL_SIZE);
        AenderIslandSampler.Island.Column column = island.columnAt(x, z);

        if (column.empty()) {
            return null;
        }

        int height = column.maxY() - column.minY() + 1;
        int minCenterY = column.minY() + 2;
        int maxCenterY = Math.min(
                column.maxY() - 4,
                column.minY() + Math.max(2, (int) Math.floor(height * 0.55D))
        );

        if (maxCenterY < minCenterY) {
            return null;
        }

        int y = minCenterY + (int) (unit(seed ^ 0x14L) * (maxCenterY - minCenterY + 1));
        int blockCount = diamondLikeVeinSize(seed);
        OreBlock origin = new OreBlock(x, y, z);
        Set<OreBlock> blocks = new LinkedHashSet<>();
        OreBlock cursor = origin;
        blocks.add(origin);

        for (int attempt = 0; blocks.size() < blockCount && attempt < blockCount * 12; attempt++) {
            long stepSeed = mix64(seed ^ (long) attempt * 0x9E3779B97F4A7C15L);
            int direction = (int) (unit(stepSeed ^ 0x21L) * 6.0D);
            OreBlock candidate = switch (direction) {
                case 0 -> new OreBlock(cursor.x() + 1, cursor.y(), cursor.z());
                case 1 -> new OreBlock(cursor.x() - 1, cursor.y(), cursor.z());
                case 2 -> new OreBlock(cursor.x(), cursor.y() + 1, cursor.z());
                case 3 -> new OreBlock(cursor.x(), cursor.y() - 1, cursor.z());
                case 4 -> new OreBlock(cursor.x(), cursor.y(), cursor.z() + 1);
                default -> new OreBlock(cursor.x(), cursor.y(), cursor.z() - 1);
            };

            if (!canContainOre(island, candidate)) {
                continue;
            }

            cursor = candidate;
            blocks.add(cursor);
        }

        return blocks.size() < 2 ? null : new Vein(origin, List.copyOf(blocks));
    }

    private static int diamondLikeVeinSize(long seed) {
        double profile = unit(seed ^ 0x15L);
        double size = unit(seed ^ 0x16L);

        if (profile < SMALL_VEIN_SHARE) {
            return 3 + (int) (size * 2.0D);
        }

        if (profile >= 1.0D - LARGE_VEIN_SHARE) {
            return 8 + (int) (size * 5.0D);
        }

        return 4 + (int) (size * 5.0D);
    }

    private static boolean canContainOre(AenderIslandSampler.Island island, OreBlock block) {
        AenderIslandSampler.Island.Column column = island.columnAt(block.x(), block.z());
        return !column.empty()
                && block.y() >= column.minY() + 1
                && block.y() <= column.maxY() - 4;
    }

    private static double unit(long seed) {
        return (mix64(seed) >>> 11) * 0x1.0p-53;
    }

    private static long mix64(long value) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= value >>> 33;
        return value;
    }

    record Vein(OreBlock origin, List<OreBlock> blocks) {
        private boolean intersects(int minX, int maxX, int minZ, int maxZ) {
            return blocks.stream().anyMatch(pos -> pos.x() >= minX
                    && pos.x() <= maxX
                    && pos.z() >= minZ
                    && pos.z() <= maxZ);
        }
    }

    record OreBlock(int x, int y, int z) {
        int manhattanDistance(OreBlock other) {
            return Math.abs(x - other.x) + Math.abs(y - other.y) + Math.abs(z - other.z);
        }
    }
}
