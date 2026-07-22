package cz.xefensor.retold.aender.generation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AenderIslandBiomeTest {
    @Test
    void verticallyStackedIslandsKeepIndependentBiomes() {
        AenderIslandSampler.Island lower = island(64, 1L, AenderBiomeKind.DESERT);
        AenderIslandSampler.Island upper = island(176, 2L, AenderBiomeKind.PLAINS);
        int lowerSurface = lower.columnAt(0, 0).maxY();
        int upperSurface = upper.columnAt(0, 0).maxY();

        assertEquals(
                AenderBiomeKind.DESERT,
                AenderIslandSampler.biomeAt(List.of(lower, upper), 0, lowerSurface + 1, 0)
        );
        assertEquals(
                AenderBiomeKind.PLAINS,
                AenderIslandSampler.biomeAt(List.of(lower, upper), 0, upperSurface + 1, 0)
        );
    }

    @Test
    void voidWithoutAnIslandUsesAenderPlainsAsTheAtmosphericFallback() {
        assertEquals(
                AenderBiomeKind.PLAINS,
                AenderIslandSampler.biomeAt(List.of(), 0, 100, 0)
        );
    }

    @Test
    void cachedBiomeColumnsMatchDirectThreeDimensionalSelection() {
        List<AenderIslandSampler.Island> islands = List.of(
                island(64, 1L, AenderBiomeKind.DESERT),
                island(176, 2L, AenderBiomeKind.PLAINS)
        );
        List<AenderIslandSampler.BiomeColumn> columns =
                AenderIslandSampler.biomeColumnsAt(islands, 0, 0);

        for (int y = AenderIslandSampler.MIN_Y; y < AenderIslandSampler.MAX_Y; y += 4) {
            assertEquals(
                    AenderIslandSampler.biomeAt(islands, 0, y, 0),
                    AenderIslandSampler.biomeFromColumns(columns, y)
            );
        }
    }

    @Test
    void desertSurfaceHasLessCentralReliefThanPlains() {
        AenderIslandSampler.Island plains = island(112, 0xA3D1E41FL, AenderBiomeKind.PLAINS);
        AenderIslandSampler.Island desert = island(112, 0xA3D1E41FL, AenderBiomeKind.DESERT);

        int plainsRelief = centralSurfaceRelief(plains);
        int desertRelief = centralSurfaceRelief(desert);

        assertTrue(
                desertRelief < plainsRelief,
                () -> "Desert relief " + desertRelief + " must be lower than plains relief " + plainsRelief
        );
    }

    private static int centralSurfaceRelief(AenderIslandSampler.Island island) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;

        for (int x = -48; x <= 48; x += 4) {
            for (int z = -48; z <= 48; z += 4) {
                AenderIslandSampler.Island.Column column = island.columnAt(x, z);

                if (!column.empty()) {
                    min = Math.min(min, column.maxY());
                    max = Math.max(max, column.maxY());
                }
            }
        }

        return max - min;
    }

    private static AenderIslandSampler.Island island(
            int centerY,
            long seed,
            AenderBiomeKind biome
    ) {
        return new AenderIslandSampler.Island(
                0,
                centerY,
                0,
                96.0D,
                96.0D,
                40.0D,
                seed,
                biome,
                biome == AenderBiomeKind.DESERT
                        ? AenderIslandArchetype.PLATEAU
                        : AenderIslandArchetype.ROUND,
                biome == AenderBiomeKind.DESERT
                        ? AenderUndersideProfile.TERRACED
                        : AenderUndersideProfile.ROOTED
        );
    }
}
