package cz.xefensor.retold.aender.generation;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AenderOrePlannerTest {
    @Test
    void realIslandSamplerProducesDiamondLikeFrequency() {
        int sampledChunks = 0;
        int chunksWithVeins = 0;
        int plannedBlocks = 0;

        for (int chunkX = -32; chunkX <= 32; chunkX++) {
            for (int chunkZ = -32; chunkZ <= 32; chunkZ++) {
                sampledChunks++;
                List<AenderOrePlanner.Vein> veins = AenderOrePlanner.veinsForChunk(
                        AenderIslandSampler.islandsForChunk(chunkX, chunkZ),
                        chunkX,
                        chunkZ
                );

                if (!veins.isEmpty()) {
                    chunksWithVeins++;
                }

                Set<AenderOrePlanner.OreBlock> blocks = new HashSet<>();

                for (AenderOrePlanner.Vein vein : veins) {
                    for (AenderOrePlanner.OreBlock block : vein.blocks()) {
                        if (Math.floorDiv(block.x(), 16) == chunkX
                                && Math.floorDiv(block.z(), 16) == chunkZ) {
                            blocks.add(block);
                        }
                    }
                }

                plannedBlocks += blocks.size();
            }
        }

        double oreBearingRatio = chunksWithVeins / (double) sampledChunks;
        double blocksPerChunk = plannedBlocks / (double) sampledChunks;
        assertTrue(
                oreBearingRatio >= 0.25D,
                "Aenderite should occur with diamond-like regularity in Aender terrain"
        );
        assertTrue(
                oreBearingRatio <= 0.40D,
                "Aenderite should remain an ore rather than becoming a common terrain block"
        );
        assertTrue(
                blocksPerChunk >= 2.5D && blocksPerChunk <= 4.0D,
                "Aenderite should average a diamond-like number of planned blocks per chunk"
        );
    }

    @Test
    void veinsAreDeterministicCompactAndDiamondSized() {
        AenderIslandSampler.Island island = island(41L);
        List<AenderOrePlanner.Vein> first = sampleVeins(island);
        List<AenderOrePlanner.Vein> second = sampleVeins(island);

        assertFalse(first.isEmpty(), "The sampled island should contain Aenderite veins");
        assertEquals(first, second, "The same island reality must produce identical veins");

        for (AenderOrePlanner.Vein vein : first) {
            assertTrue(vein.blocks().size() >= 3 && vein.blocks().size() <= 12);

            for (AenderOrePlanner.OreBlock block : vein.blocks()) {
                assertTrue(
                        vein.origin().manhattanDistance(block) <= 11,
                        "A vein must remain a compact local cluster"
                );
                AenderIslandSampler.Island.Column column = island.columnAt(block.x(), block.z());
                assertTrue(
                        !column.empty()
                                && block.y() >= column.minY() + 1
                                && block.y() <= column.maxY() - 4,
                        "Every planned block must remain safely embedded in its island"
                );
            }
        }
    }

    @Test
    void veinOriginsFavorIslandUndersides() {
        double normalizedHeightTotal = 0.0D;
        int originCount = 0;

        for (long seed = 0; seed < 32; seed++) {
            AenderIslandSampler.Island island = island(seed);

            for (AenderOrePlanner.Vein vein : sampleVeins(island)) {
                AenderIslandSampler.Island.Column column = island.columnAt(
                        vein.origin().x(),
                        vein.origin().z()
                );
                double normalizedHeight = (vein.origin().y() - column.minY())
                        / (double) (column.maxY() - column.minY() + 1);
                assertTrue(normalizedHeight <= 0.56D, "Origins must stay in the lower island body");
                normalizedHeightTotal += normalizedHeight;
                originCount++;
            }
        }

        assertTrue(originCount > 100, "The distribution check needs a useful sample");
        assertTrue(
                normalizedHeightTotal / originCount < 0.36D,
                "Aenderite should be biased toward island undersides"
        );
    }

    @Test
    void crossChunkVeinsReuseTheSameWholePlan() {
        for (long seed = 0; seed < 128; seed++) {
            AenderIslandSampler.Island island = island(seed);

            for (int chunkX = -5; chunkX < 5; chunkX++) {
                List<AenderOrePlanner.Vein> left = AenderOrePlanner.veinsForChunk(
                        List.of(island),
                        chunkX,
                        0
                );
                List<AenderOrePlanner.Vein> right = AenderOrePlanner.veinsForChunk(
                        List.of(island),
                        chunkX + 1,
                        0
                );

                for (AenderOrePlanner.Vein leftVein : left) {
                    for (AenderOrePlanner.Vein rightVein : right) {
                        if (leftVein.origin().equals(rightVein.origin())) {
                            assertEquals(
                                    leftVein.blocks(),
                                    rightVein.blocks(),
                                    "Both chunks must see the same complete border vein"
                            );
                            return;
                        }
                    }
                }
            }
        }

        throw new AssertionError("Expected to find a vein crossing a sampled chunk border");
    }

    private static List<AenderOrePlanner.Vein> sampleVeins(AenderIslandSampler.Island island) {
        List<AenderOrePlanner.Vein> veins = new ArrayList<>();

        for (int chunkX = -7; chunkX <= 7; chunkX++) {
            for (int chunkZ = -7; chunkZ <= 7; chunkZ++) {
                for (AenderOrePlanner.Vein vein : AenderOrePlanner.veinsForChunk(
                        List.of(island),
                        chunkX,
                        chunkZ
                )) {
                    if (!veins.contains(vein)) {
                        veins.add(vein);
                    }
                }
            }
        }

        return List.copyOf(veins);
    }

    private static AenderIslandSampler.Island island(long seed) {
        return new AenderIslandSampler.Island(
                0,
                120,
                0,
                110.0D,
                96.0D,
                48.0D,
                seed,
                AenderBiomeKind.PLAINS,
                AenderIslandArchetype.ROUND,
                AenderUndersideProfile.ROOTED
        );
    }
}
