package cz.xefensor.retold.aender.generation;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AenderOrePlannerTest {
    @Test
    void veinsAreDeterministicCompactAndTwoToFiveBlocks() {
        AenderIslandSampler.Island island = island(41L);
        List<AenderOrePlanner.Vein> first = sampleVeins(island);
        List<AenderOrePlanner.Vein> second = sampleVeins(island);

        assertFalse(first.isEmpty(), "The sampled island should contain Aenderite veins");
        assertEquals(first, second, "The same island reality must produce identical veins");

        for (AenderOrePlanner.Vein vein : first) {
            assertTrue(vein.blocks().size() >= 2 && vein.blocks().size() <= 5);

            for (AenderOrePlanner.OreBlock block : vein.blocks()) {
                assertTrue(
                        vein.origin().manhattanDistance(block) <= 4,
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
