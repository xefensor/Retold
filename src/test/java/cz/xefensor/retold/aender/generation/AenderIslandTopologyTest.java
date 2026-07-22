package cz.xefensor.retold.aender.generation;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AenderIslandTopologyTest {
    @Test
    void biomeFamiliesSelectOnlyTheirOwnArchetypes() {
        EnumSet<AenderIslandArchetype> plains = EnumSet.noneOf(AenderIslandArchetype.class);
        EnumSet<AenderIslandArchetype> desert = EnumSet.noneOf(AenderIslandArchetype.class);

        for (long seed = 0; seed < 512; seed++) {
            plains.add(AenderIslandArchetype.fromSeed(AenderBiomeKind.PLAINS, seed));
            desert.add(AenderIslandArchetype.fromSeed(AenderBiomeKind.DESERT, seed));
        }

        assertEquals(
                EnumSet.of(
                        AenderIslandArchetype.ROUND,
                        AenderIslandArchetype.ELONGATED,
                        AenderIslandArchetype.CRESCENT,
                        AenderIslandArchetype.SPLIT,
                        AenderIslandArchetype.TWIN
                ),
                plains
        );
        assertEquals(
                EnumSet.of(
                        AenderIslandArchetype.PLATEAU,
                        AenderIslandArchetype.DUNES,
                        AenderIslandArchetype.ERODED_MESA,
                        AenderIslandArchetype.SHELF
                ),
                desert
        );
    }

    @Test
    void splitAndCrescentArchetypesRemoveTerrainFromTheBaseFootprint() {
        long seed = seedWithoutSatellites();
        int roundColumns = occupiedColumns(island(seed, AenderIslandArchetype.ROUND));
        int crescentColumns = occupiedColumns(island(seed, AenderIslandArchetype.CRESCENT));
        int splitColumns = occupiedColumns(island(seed, AenderIslandArchetype.SPLIT));

        assertTrue(crescentColumns < roundColumns, "A crescent must remove a substantial inner bite");
        assertTrue(splitColumns < roundColumns, "A split island must remove its central ravine");
        assertFalse(crescentColumns == splitColumns, "Crescent and split silhouettes must remain distinct");
    }

    @Test
    void satelliteCountsIncludeSparseAndFragmentedIslands() {
        boolean foundNone = false;
        boolean foundMultiple = false;

        for (long seed = 0; seed < 512; seed++) {
            int satellites = island(seed, AenderIslandArchetype.ROUND).satelliteCount();
            foundNone |= satellites == 0;
            foundMultiple |= satellites >= 2;
        }

        assertTrue(foundNone, "Some islands must remain clean silhouettes without satellites");
        assertTrue(foundMultiple, "Some islands must form multi-fragment clusters");
    }

    private static long seedWithoutSatellites() {
        for (long seed = 0; seed < 512; seed++) {
            if (island(seed, AenderIslandArchetype.ROUND).satelliteCount() == 0) {
                return seed;
            }
        }

        throw new AssertionError("Expected at least one seed without satellites");
    }

    private static int occupiedColumns(AenderIslandSampler.Island island) {
        int count = 0;

        for (int x = -150; x <= 150; x += 3) {
            for (int z = -150; z <= 150; z += 3) {
                if (!island.columnAt(x, z).empty()) {
                    count++;
                }
            }
        }

        return count;
    }

    private static AenderIslandSampler.Island island(long seed, AenderIslandArchetype archetype) {
        return new AenderIslandSampler.Island(
                0,
                120,
                0,
                100.0D,
                92.0D,
                44.0D,
                seed,
                AenderBiomeKind.PLAINS,
                archetype,
                AenderUndersideProfile.ROOTED
        );
    }
}
