package cz.xefensor.retold.aender.generation;

import cz.xefensor.retold.registry.RetoldBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;

import java.util.List;

/** Full-pipeline Aender generation checks that require real Minecraft chunks. */
public final class AenderGenerationGameTests {
    private static final int SEARCH_RADIUS_CHUNKS = 48;
    private static final int MAX_GENERATED_CANDIDATES = 12;

    private AenderGenerationGameTests() {
    }

    public static void aenderiteIsWrittenByFullChunkGeneration(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        int generatedCandidates = 0;
        int plannedBlocks = 0;

        AenderVolatility.enableCurrentGeneratorForFreshWorld(level);

        try {
            for (int chunkX = -SEARCH_RADIUS_CHUNKS;
                    chunkX <= SEARCH_RADIUS_CHUNKS;
                    chunkX++) {
                for (int chunkZ = -SEARCH_RADIUS_CHUNKS;
                        chunkZ <= SEARCH_RADIUS_CHUNKS;
                        chunkZ++) {
                    List<AenderIslandSampler.Island> islands =
                            AenderIslandSampler.islandsForChunk(chunkX, chunkZ);
                    List<AenderOrePlanner.Vein> veins =
                            AenderOrePlanner.veinsForChunk(islands, chunkX, chunkZ);

                    if (veins.isEmpty()) {
                        continue;
                    }

                    ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
                    ProtoChunk chunk = createChunk(level, chunkPos);
                    generatedCandidates++;
                    plannedBlocks += countPlannedBlocksInChunk(veins, chunkPos);
                    AenderChunkGenerator.generateChunk(chunk);

                    int generatedOre = countGeneratedOre(chunk, veins);

                    if (generatedOre > 0) {
                        helper.succeed();
                        return;
                    }

                    if (generatedCandidates >= MAX_GENERATED_CANDIDATES) {
                        helper.fail(
                                "Aenderite was planned in " + generatedCandidates
                                        + " chunks (" + plannedBlocks
                                        + " in-chunk blocks) but full chunk generation wrote none"
                        );
                        return;
                    }
                }
            }

            helper.fail("No planned Aenderite vein was found in the deterministic search area");
        } finally {
            AenderVolatility.clearRuntime();
        }
    }

    public static void cavesAreCarvedByFullChunkGeneration(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        int generatedCandidates = 0;

        AenderVolatility.enableCurrentGeneratorForFreshWorld(level);

        try {
            for (int chunkX = -SEARCH_RADIUS_CHUNKS;
                    chunkX <= SEARCH_RADIUS_CHUNKS;
                    chunkX++) {
                for (int chunkZ = -SEARCH_RADIUS_CHUNKS;
                        chunkZ <= SEARCH_RADIUS_CHUNKS;
                        chunkZ++) {
                    ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
                    List<AenderIslandSampler.Island> islands =
                            AenderIslandSampler.islandsForChunk(chunkX, chunkZ);
                    List<AenderCavePlanner.Node> nodes = caveNodesInsideChunk(islands, chunkPos);

                    if (nodes.isEmpty()) {
                        continue;
                    }

                    ProtoChunk chunk = createChunk(level, chunkPos);
                    generatedCandidates++;
                    AenderChunkGenerator.generateChunk(chunk);

                    for (AenderCavePlanner.Node node : nodes) {
                        BlockPos pos = new BlockPos(
                                (int) Math.floor(node.x()),
                                (int) Math.floor(node.y()),
                                (int) Math.floor(node.z())
                        );

                        if (chunk.getBlockState(pos).isAir()) {
                            helper.succeed();
                            return;
                        }
                    }

                    if (generatedCandidates >= MAX_GENERATED_CANDIDATES) {
                        helper.fail(
                                "Cave nodes were planned in " + generatedCandidates
                                        + " chunks but full chunk generation carved none"
                        );
                        return;
                    }
                }
            }

            helper.fail("No planned Aender cave was found in the deterministic search area");
        } finally {
            AenderVolatility.clearRuntime();
        }
    }

    public static void surfaceCaveEntrancesBreachTerrainCap(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        SurfaceEntrance entrance = findSyntheticSurfaceEntrance();

        if (entrance == null) {
            helper.fail("No deterministic synthetic surface entrance could be planned");
            return;
        }

        ChunkPos chunkPos = new ChunkPos(
                SectionPos.blockToSectionCoord((int) Math.floor(entrance.node().x())),
                SectionPos.blockToSectionCoord((int) Math.floor(entrance.node().z()))
        );
        ProtoChunk chunk = createChunk(level, chunkPos);
        fillSyntheticIsland(chunk, entrance.island());
        AenderCaveCarver.carve(chunk, List.of(new TestIslandView(entrance.island())));

        helper.assertTrue(
                hasOpenSurfaceNearEntrance(chunk, entrance),
                "A planned surface cave entrance must breach the island terrain cap"
        );
        helper.succeed();
    }

    private static List<AenderCavePlanner.Node> caveNodesInsideChunk(
            List<AenderIslandSampler.Island> islands,
            ChunkPos chunkPos
    ) {
        return islands.stream()
                .flatMap(island -> AenderCavePlanner.tunnelsForIsland(island).stream())
                .flatMap(tunnel -> tunnel.nodes().stream())
                .filter(node -> SectionPos.blockToSectionCoord((int) Math.floor(node.x())) == chunkPos.x()
                        && SectionPos.blockToSectionCoord((int) Math.floor(node.z())) == chunkPos.z())
                .toList();
    }

    private static SurfaceEntrance findSyntheticSurfaceEntrance() {
        for (long seed = 0; seed < 1024; seed++) {
            AenderIslandSampler.Island island = new AenderIslandSampler.Island(
                    520,
                    128,
                    520,
                    104.0D,
                    82.0D,
                    38.0D,
                    seed,
                    AenderBiomeKind.PLAINS,
                    AenderIslandArchetype.ROUND,
                    AenderUndersideProfile.ROOTED
            );

            for (AenderCavePlanner.Tunnel tunnel : AenderCavePlanner.tunnelsForIsland(island)) {
                if (tunnel.endEntrance() == AenderCavePlanner.EntranceKind.SURFACE) {
                    return new SurfaceEntrance(island, tunnel.nodes().getLast());
                }
            }
        }

        return null;
    }

    private static void fillSyntheticIsland(
            ProtoChunk chunk,
            AenderIslandSampler.Island island
    ) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = chunk.getPos().getMinBlockX(); x <= chunk.getPos().getMaxBlockX(); x++) {
            for (int z = chunk.getPos().getMinBlockZ(); z <= chunk.getPos().getMaxBlockZ(); z++) {
                AenderIslandSampler.Island.Column column = island.columnAt(x, z);

                if (column.empty()) {
                    continue;
                }

                for (int y = column.minY(); y <= column.maxY(); y++) {
                    pos.set(x, y, z);

                    if (y == column.maxY()) {
                        chunk.setBlockState(
                                pos,
                                RetoldBlocks.AENDER_GRASS_BLOCK.get().defaultBlockState(),
                                0
                        );
                    } else if (y >= column.maxY() - 3) {
                        chunk.setBlockState(
                                pos,
                                RetoldBlocks.AENDER_SOIL.get().defaultBlockState(),
                                0
                        );
                    } else {
                        chunk.setBlockState(
                                pos,
                                RetoldBlocks.AENDER_STONE.get().defaultBlockState(),
                                0
                        );
                    }
                }
            }
        }
    }

    private static boolean hasOpenSurfaceNearEntrance(
            ProtoChunk chunk,
            SurfaceEntrance entrance
    ) {
        AenderCavePlanner.Node node = entrance.node();
        int radius = (int) Math.ceil(node.horizontalRadius());
        int minX = Math.max(chunk.getPos().getMinBlockX(), (int) Math.floor(node.x()) - radius);
        int maxX = Math.min(chunk.getPos().getMaxBlockX(), (int) Math.ceil(node.x()) + radius);
        int minZ = Math.max(chunk.getPos().getMinBlockZ(), (int) Math.floor(node.z()) - radius);
        int maxZ = Math.min(chunk.getPos().getMaxBlockZ(), (int) Math.ceil(node.z()) + radius);

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                AenderIslandSampler.Island.Column column = entrance.island().columnAt(x, z);

                if (!column.empty()
                        && chunk.getBlockState(new BlockPos(x, column.maxY(), z)).isAir()) {
                    return true;
                }
            }
        }

        return false;
    }

    private static ProtoChunk createChunk(ServerLevel level, ChunkPos chunkPos) {
        return new ProtoChunk(
                chunkPos,
                UpgradeData.EMPTY,
                LevelHeightAccessor.create(
                        AenderIslandSampler.MIN_Y,
                        AenderIslandSampler.HEIGHT
                ),
                PalettedContainerFactory.create(level.registryAccess()),
                null
        );
    }

    private static int countPlannedBlocksInChunk(
            List<AenderOrePlanner.Vein> veins,
            ChunkPos chunkPos
    ) {
        int count = 0;

        for (AenderOrePlanner.Vein vein : veins) {
            for (AenderOrePlanner.OreBlock block : vein.blocks()) {
                if (isInsideChunk(block, chunkPos)) {
                    count++;
                }
            }
        }

        return count;
    }

    private static int countGeneratedOre(
            ProtoChunk chunk,
            List<AenderOrePlanner.Vein> veins
    ) {
        int count = 0;

        for (AenderOrePlanner.Vein vein : veins) {
            for (AenderOrePlanner.OreBlock block : vein.blocks()) {
                if (!isInsideChunk(block, chunk.getPos())) {
                    continue;
                }

                BlockPos pos = new BlockPos(block.x(), block.y(), block.z());

                if (chunk.getBlockState(pos).is(RetoldBlocks.AENDERITE_ORE)) {
                    count++;
                }
            }
        }

        return count;
    }

    private static boolean isInsideChunk(
            AenderOrePlanner.OreBlock block,
            ChunkPos chunkPos
    ) {
        return SectionPos.blockToSectionCoord(block.x()) == chunkPos.x()
                && SectionPos.blockToSectionCoord(block.z()) == chunkPos.z();
    }

    private record SurfaceEntrance(
            AenderIslandSampler.Island island,
            AenderCavePlanner.Node node
    ) {
    }

    private record TestIslandView(
            AenderIslandSampler.Island island
    ) implements AenderCaveCarver.IslandView {
        @Override
        public AenderIslandSampler.Island.Column columnAt(int x, int z) {
            return island.columnAt(x, z);
        }
    }
}
