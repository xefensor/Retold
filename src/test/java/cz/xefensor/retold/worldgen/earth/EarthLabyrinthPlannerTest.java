package cz.xefensor.retold.worldgen.earth;

import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EarthLabyrinthPlannerTest {
    @Test
    void pyramidSeedsAreStableAndPositionSpecific() {
        long worldSeed = 0x5EEDBEEFL;
        Set<Long> seeds = new HashSet<>();

        for (int chunkZ = -4; chunkZ <= 4; chunkZ++) {
            for (int chunkX = -4; chunkX <= 4; chunkX++) {
                long first = EarthLabyrinthPlanner.seedForPyramid(worldSeed, chunkX, chunkZ);
                long second = EarthLabyrinthPlanner.seedForPyramid(worldSeed, chunkX, chunkZ);
                assertEquals(first, second);
                seeds.add(first);
            }
        }

        assertEquals(81, seeds.size(), "Nearby pyramids must not share layout seeds");
        assertNotEquals(
                EarthLabyrinthPlanner.seedForPyramid(worldSeed, 3, 7),
                EarthLabyrinthPlanner.seedForPyramid(worldSeed + 1L, 3, 7)
        );
    }

    @Test
    void layoutsAreDeterministicButVaryAcrossPyramids() {
        Set<EarthLabyrinthLayout> distinctLayouts = new HashSet<>();
        Set<Integer> widths = new HashSet<>();

        for (long seed = 0; seed < 64; seed++) {
            EarthLabyrinthLayout first = EarthLabyrinthPlanner.generate(seed);
            EarthLabyrinthLayout second = EarthLabyrinthPlanner.generate(seed);

            assertEquals(first, second, "The same pyramid seed must retain its labyrinth");
            distinctLayouts.add(first);
            widths.add(first.footprintWidthBlocks());
        }

        assertTrue(distinctLayouts.size() > 56, "Pyramid seeds must produce varied topologies");
        assertEquals(Set.of(77, 101), widths);
        assertNotEquals(
                EarthLabyrinthPlanner.generate(10L),
                EarthLabyrinthPlanner.generate(11L)
        );
    }

    @Test
    void everyCellIsReachableAcrossBothLevels() {
        for (long seed = 0; seed < 512; seed++) {
            EarthLabyrinthLayout layout = EarthLabyrinthPlanner.generate(seed);
            Set<EarthLabyrinthLayout.Cell> reachable = reachableFrom(
                    layout,
                    layout.entrance()
            );

            assertEquals(
                    layout.sideCells() * layout.sideCells() * EarthLabyrinthLayout.LEVEL_COUNT,
                    reachable.size(),
                    "Every generated maze cell must be connected to its entrance"
            );
            assertTrue(
                    reachable.contains(layout.guardianChamber()),
                    "The guardian chamber must always be reachable"
            );
        }
    }

    @Test
    void layoutsKeepFixedAnchorsMultipleStairsAndLoops() {
        for (long seed = 0; seed < 512; seed++) {
            EarthLabyrinthLayout layout = EarthLabyrinthPlanner.generate(seed);
            int side = layout.sideCells();
            int cellCount = side * side * EarthLabyrinthLayout.LEVEL_COUNT;

            assertEquals(
                    new EarthLabyrinthLayout.Cell(side / 2, 0, 0),
                    layout.entrance()
            );
            assertEquals(
                    new EarthLabyrinthLayout.Cell(side / 2, side / 2, 1),
                    layout.guardianChamber()
            );
            assertEquals(
                    EarthLabyrinthLayout.CellKind.ENTRANCE,
                    layout.kindAt(layout.entrance())
            );
            assertEquals(
                    EarthLabyrinthLayout.CellKind.GUARDIAN_CHAMBER,
                    layout.kindAt(layout.guardianChamber())
            );
            assertTrue(layout.verticalPassageCount() >= 4L);
            assertTrue(layout.verticalPassageCount() <= 5L);
            for (EarthLabyrinthLayout.Passage passage : layout.passages()) {
                if (!passage.isVertical()) {
                    continue;
                }

                EarthLabyrinthLayout.Cell lower = passage.first().level() == 1
                        ? passage.first()
                        : passage.second();
                assertTrue(
                        Math.abs(lower.x() - layout.guardianChamber().x())
                                > EarthLabyrinthDimensions.GUARDIAN_ARENA_CELL_RADIUS
                                || Math.abs(lower.z() - layout.guardianChamber().z())
                                > EarthLabyrinthDimensions.GUARDIAN_ARENA_CELL_RADIUS,
                        "Vertical links must stay outside the guardian arena"
                );
            }
            assertTrue(
                    layout.passages().size() >= cellCount,
                    "Extra passages and stairs must give every labyrinth at least one loop"
            );
            assertTrue(layout.footprintWidthBlocks() >= 77);
            assertTrue(layout.footprintWidthBlocks() <= 101);
        }
    }

    @Test
    void everyPassageIsUniqueAdjacentAndInsideTheLayout() {
        for (long seed = 0; seed < 256; seed++) {
            EarthLabyrinthLayout layout = EarthLabyrinthPlanner.generate(seed);
            Set<EarthLabyrinthLayout.Passage> unique = new HashSet<>(layout.passages());

            assertEquals(layout.passages().size(), unique.size());

            for (EarthLabyrinthLayout.Passage passage : layout.passages()) {
                assertInside(layout, passage.first());
                assertInside(layout, passage.second());

                int distance = Math.abs(passage.first().x() - passage.second().x())
                        + Math.abs(passage.first().z() - passage.second().z())
                        + Math.abs(passage.first().level() - passage.second().level());
                assertEquals(1, distance);
            }
        }
    }

    private static Set<EarthLabyrinthLayout.Cell> reachableFrom(
            EarthLabyrinthLayout layout,
            EarthLabyrinthLayout.Cell start
    ) {
        Set<EarthLabyrinthLayout.Cell> reachable = new HashSet<>();
        ArrayDeque<EarthLabyrinthLayout.Cell> queue = new ArrayDeque<>();
        reachable.add(start);
        queue.add(start);

        while (!queue.isEmpty()) {
            EarthLabyrinthLayout.Cell current = queue.remove();

            for (EarthLabyrinthLayout.Cell neighbor : layout.neighbors(current)) {
                if (reachable.add(neighbor)) {
                    queue.add(neighbor);
                }
            }
        }

        return reachable;
    }

    private static void assertInside(
            EarthLabyrinthLayout layout,
            EarthLabyrinthLayout.Cell cell
    ) {
        assertTrue(cell.x() >= 0 && cell.x() < layout.sideCells());
        assertTrue(cell.z() >= 0 && cell.z() < layout.sideCells());
        assertTrue(cell.level() >= 0 && cell.level() < EarthLabyrinthLayout.LEVEL_COUNT);
    }
}
