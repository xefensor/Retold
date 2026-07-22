package cz.xefensor.retold.aender.generation;

import cz.xefensor.retold.registry.RetoldBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.List;

/** Applies planned Aender cave volumes without coupling cave policy to the chunk generator. */
final class AenderCaveCarver {
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    private AenderCaveCarver() {
    }

    static void carve(
            ChunkAccess chunk,
            List<? extends IslandView> islands
    ) {
        int chunkMinX = chunk.getPos().getMinBlockX();
        int chunkMaxX = chunkMinX + 15;
        int chunkMinZ = chunk.getPos().getMinBlockZ();
        int chunkMaxZ = chunkMinZ + 15;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (IslandView island : islands) {
            for (AenderCavePlanner.Tunnel tunnel :
                    AenderCavePlanner.tunnelsForIsland(island.island())) {
                for (AenderCavePlanner.Node node : tunnel.nodes()) {
                    int radius = (int) Math.ceil(node.horizontalRadius());

                    if (node.x() + radius < chunkMinX
                            || node.x() - radius > chunkMaxX
                            || node.z() + radius < chunkMinZ
                            || node.z() - radius > chunkMaxZ) {
                        continue;
                    }

                    carveNode(chunk, islands, island, node, pos);
                }
            }
        }
    }

    private static void carveNode(
            ChunkAccess chunk,
            List<? extends IslandView> islands,
            IslandView owner,
            AenderCavePlanner.Node node,
            BlockPos.MutableBlockPos pos
    ) {
        int horizontalRadius = (int) Math.ceil(node.horizontalRadius());
        int verticalRadius = (int) Math.ceil(node.verticalRadius());
        int minX = Math.max(
                chunk.getPos().getMinBlockX(),
                (int) Math.floor(node.x()) - horizontalRadius
        );
        int maxX = Math.min(
                chunk.getPos().getMaxBlockX(),
                (int) Math.ceil(node.x()) + horizontalRadius
        );
        int minZ = Math.max(
                chunk.getPos().getMinBlockZ(),
                (int) Math.floor(node.z()) - horizontalRadius
        );
        int maxZ = Math.min(
                chunk.getPos().getMaxBlockZ(),
                (int) Math.ceil(node.z()) + horizontalRadius
        );
        int minY = Math.max(
                AenderIslandSampler.MIN_Y,
                (int) Math.floor(node.y()) - verticalRadius
        );
        int maxY = Math.min(
                AenderIslandSampler.MAX_Y - 1,
                (int) Math.ceil(node.y()) + verticalRadius
        );
        int[] overlapMins = new int[Math.max(0, islands.size() - 1)];
        int[] overlapMaxes = new int[overlapMins.length];

        for (int x = minX; x <= maxX; x++) {
            double normalizedX = (x + 0.5D - node.x()) / node.horizontalRadius();

            for (int z = minZ; z <= maxZ; z++) {
                double normalizedZ = (z + 0.5D - node.z()) / node.horizontalRadius();
                double horizontalDistance = normalizedX * normalizedX + normalizedZ * normalizedZ;

                if (horizontalDistance > 1.18D) {
                    continue;
                }

                AenderIslandSampler.Island.Column ownerColumn = owner.columnAt(x, z);

                if (ownerColumn.empty()) {
                    continue;
                }

                int overlapCount = collectOtherIslandIntervals(
                        islands,
                        owner,
                        x,
                        z,
                        overlapMins,
                        overlapMaxes
                );

                for (int y = minY; y <= maxY; y++) {
                    double normalizedY = (y + 0.5D - node.y()) / node.verticalRadius();
                    double distance = horizontalDistance + normalizedY * normalizedY;
                    double roughness = (unit(
                            node.seed()
                                    ^ (long) x * 0x9E3779B97F4A7C15L
                                    ^ (long) y * 0xC2B2AE3D27D4EB4FL
                                    ^ (long) z * 0x632BE59BD9B4E019L
                    ) - 0.5D) * 0.16D;

                    if (distance > 1.0D + roughness
                            || !insideShell(ownerColumn, y, node.entrance())
                            || coveredByAnotherIsland(overlapMins, overlapMaxes, overlapCount, y)) {
                        continue;
                    }

                    pos.set(x, y, z);
                    BlockState state = chunk.getBlockState(pos);

                    if (state.is(RetoldBlocks.AENDER_STONE)
                            || state.is(RetoldBlocks.AENDER_SANDSTONE)
                            || (node.entrance() == AenderCavePlanner.EntranceKind.SURFACE
                            && isSurfaceTerrain(state))) {
                        chunk.setBlockState(pos, AIR, 0);
                    }
                }
            }
        }
    }

    private static boolean insideShell(
            AenderIslandSampler.Island.Column column,
            int y,
            AenderCavePlanner.EntranceKind entrance
    ) {
        return switch (entrance) {
            case NONE -> y >= column.minY() + 3 && y <= column.maxY() - 5;
            case SIDE -> y >= column.minY() + 2 && y <= column.maxY() - 3;
            case UNDERSIDE -> y >= column.minY() && y <= column.maxY() - 4;
            case SURFACE -> y >= column.minY() + 4 && y <= column.maxY();
        };
    }

    private static boolean isSurfaceTerrain(BlockState state) {
        return state.is(RetoldBlocks.AENDER_GRASS_BLOCK)
                || state.is(RetoldBlocks.AENDER_SOIL)
                || state.is(RetoldBlocks.AENDER_SAND);
    }

    private static int collectOtherIslandIntervals(
            List<? extends IslandView> islands,
            IslandView owner,
            int x,
            int z,
            int[] mins,
            int[] maxes
    ) {
        int count = 0;

        for (IslandView island : islands) {
            if (island == owner) {
                continue;
            }

            AenderIslandSampler.Island.Column column = island.columnAt(x, z);

            if (!column.empty()) {
                mins[count] = column.minY();
                maxes[count] = column.maxY();
                count++;
            }
        }

        return count;
    }

    private static boolean coveredByAnotherIsland(
            int[] mins,
            int[] maxes,
            int count,
            int y
    ) {
        for (int index = 0; index < count; index++) {
            if (y >= mins[index] && y <= maxes[index]) {
                return true;
            }
        }

        return false;
    }

    private static double unit(long seed) {
        long value = seed;
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= value >>> 33;
        return (value >>> 11) * 0x1.0p-53;
    }

    interface IslandView {
        AenderIslandSampler.Island island();

        AenderIslandSampler.Island.Column columnAt(int x, int z);
    }
}
