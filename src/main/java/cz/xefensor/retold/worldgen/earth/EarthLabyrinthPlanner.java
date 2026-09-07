package cz.xefensor.retold.worldgen.earth;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Pure deterministic topology planning for the Earth Labyrinth. */
final class EarthLabyrinthPlanner {
    private static final int SMALL_SIDE_CELLS = 13;
    private static final int LARGE_SIDE_CELLS = 17;
    private static final int MIN_VERTICAL_CONNECTIONS = 4;
    private static final int CLOSED_EDGES_PER_LOOP = 18;

    private EarthLabyrinthPlanner() {
    }

    static long seedForPyramid(long worldSeed, int chunkX, int chunkZ) {
        long positionedSeed = worldSeed
                ^ (long) chunkX * 0x4F9939F508L
                ^ (long) chunkZ * 0x1EF1565BD5L;
        return mix64(positionedSeed ^ 0x6A09E667F3BCC909L);
    }

    static EarthLabyrinthLayout generate(long seed) {
        LayoutRandom random = new LayoutRandom(seed ^ 0x45A27B1D6E3C890FL);
        int sideCells = random.nextBoolean() ? SMALL_SIDE_CELLS : LARGE_SIDE_CELLS;
        Set<EarthLabyrinthLayout.Passage> passages = new LinkedHashSet<>();
        EarthLabyrinthLayout.Cell entrance = new EarthLabyrinthLayout.Cell(
                sideCells / 2,
                0,
                0
        );
        EarthLabyrinthLayout.Cell guardian = new EarthLabyrinthLayout.Cell(
                sideCells / 2,
                sideCells / 2,
                EarthLabyrinthLayout.LEVEL_COUNT - 1
        );

        carveLevel(sideCells, 0, entrance, random, passages);
        carveLevel(sideCells, 1, guardian, random, passages);
        addLoops(sideCells, 0, random, passages);
        addLoops(sideCells, 1, random, passages);

        List<EarthLabyrinthLayout.Passage> stairs = addVerticalConnections(
                sideCells,
                entrance,
                guardian,
                random,
                passages
        );
        Map<EarthLabyrinthLayout.Cell, EarthLabyrinthLayout.CellKind> cellKinds =
                new HashMap<>();

        for (EarthLabyrinthLayout.Passage stair : stairs) {
            cellKinds.put(stair.first(), EarthLabyrinthLayout.CellKind.STAIR);
            cellKinds.put(stair.second(), EarthLabyrinthLayout.CellKind.STAIR);
        }

        cellKinds.put(entrance, EarthLabyrinthLayout.CellKind.ENTRANCE);
        cellKinds.put(guardian, EarthLabyrinthLayout.CellKind.GUARDIAN_CHAMBER);

        return new EarthLabyrinthLayout(
                sideCells,
                List.copyOf(passages),
                cellKinds
        );
    }

    private static long mix64(long value) {
        value = (value ^ value >>> 30) * 0xBF58476D1CE4E5B9L;
        value = (value ^ value >>> 27) * 0x94D049BB133111EBL;
        return value ^ value >>> 31;
    }

    private static void carveLevel(
            int sideCells,
            int level,
            EarthLabyrinthLayout.Cell start,
            LayoutRandom random,
            Set<EarthLabyrinthLayout.Passage> passages
    ) {
        Set<EarthLabyrinthLayout.Cell> visited = new HashSet<>();
        Deque<EarthLabyrinthLayout.Cell> path = new ArrayDeque<>();

        visited.add(start);
        path.push(start);

        while (!path.isEmpty()) {
            EarthLabyrinthLayout.Cell current = path.peek();
            List<EarthLabyrinthLayout.Cell> candidates = unvisitedNeighbors(
                    current,
                    sideCells,
                    level,
                    visited
            );

            if (candidates.isEmpty()) {
                path.pop();
                continue;
            }

            EarthLabyrinthLayout.Cell next = candidates.get(random.nextInt(candidates.size()));
            passages.add(new EarthLabyrinthLayout.Passage(current, next));
            visited.add(next);
            path.push(next);
        }
    }

    private static List<EarthLabyrinthLayout.Cell> unvisitedNeighbors(
            EarthLabyrinthLayout.Cell cell,
            int sideCells,
            int level,
            Set<EarthLabyrinthLayout.Cell> visited
    ) {
        List<EarthLabyrinthLayout.Cell> candidates = new ArrayList<>(4);

        addCandidate(candidates, visited, cell.x(), cell.z() - 1, level, sideCells);
        addCandidate(candidates, visited, cell.x() + 1, cell.z(), level, sideCells);
        addCandidate(candidates, visited, cell.x(), cell.z() + 1, level, sideCells);
        addCandidate(candidates, visited, cell.x() - 1, cell.z(), level, sideCells);
        return candidates;
    }

    private static void addCandidate(
            List<EarthLabyrinthLayout.Cell> candidates,
            Set<EarthLabyrinthLayout.Cell> visited,
            int x,
            int z,
            int level,
            int sideCells
    ) {
        if (x < 0 || z < 0 || x >= sideCells || z >= sideCells) {
            return;
        }

        EarthLabyrinthLayout.Cell candidate = new EarthLabyrinthLayout.Cell(x, z, level);

        if (!visited.contains(candidate)) {
            candidates.add(candidate);
        }
    }

    private static void addLoops(
            int sideCells,
            int level,
            LayoutRandom random,
            Set<EarthLabyrinthLayout.Passage> passages
    ) {
        List<EarthLabyrinthLayout.Passage> closedEdges = new ArrayList<>();

        for (int z = 0; z < sideCells; z++) {
            for (int x = 0; x < sideCells; x++) {
                EarthLabyrinthLayout.Cell cell = new EarthLabyrinthLayout.Cell(x, z, level);

                if (x + 1 < sideCells) {
                    addClosedEdge(
                            closedEdges,
                            passages,
                            cell,
                            new EarthLabyrinthLayout.Cell(x + 1, z, level)
                    );
                }
                if (z + 1 < sideCells) {
                    addClosedEdge(
                            closedEdges,
                            passages,
                            cell,
                            new EarthLabyrinthLayout.Cell(x, z + 1, level)
                    );
                }
            }
        }

        random.shuffle(closedEdges);
        int loopCount = Math.max(3, sideCells * sideCells / CLOSED_EDGES_PER_LOOP);

        for (int index = 0; index < Math.min(loopCount, closedEdges.size()); index++) {
            passages.add(closedEdges.get(index));
        }
    }

    private static void addClosedEdge(
            List<EarthLabyrinthLayout.Passage> closedEdges,
            Set<EarthLabyrinthLayout.Passage> passages,
            EarthLabyrinthLayout.Cell first,
            EarthLabyrinthLayout.Cell second
    ) {
        EarthLabyrinthLayout.Passage passage = new EarthLabyrinthLayout.Passage(first, second);

        if (!passages.contains(passage)) {
            closedEdges.add(passage);
        }
    }

    private static List<EarthLabyrinthLayout.Passage> addVerticalConnections(
            int sideCells,
            EarthLabyrinthLayout.Cell entrance,
            EarthLabyrinthLayout.Cell guardian,
            LayoutRandom random,
            Set<EarthLabyrinthLayout.Passage> passages
    ) {
        List<EarthLabyrinthLayout.Cell> candidates = new ArrayList<>();

        for (int z = 1; z < sideCells - 1; z++) {
            for (int x = 1; x < sideCells - 1; x++) {
                EarthLabyrinthLayout.Cell upper = new EarthLabyrinthLayout.Cell(x, z, 0);
                EarthLabyrinthLayout.Cell lower = new EarthLabyrinthLayout.Cell(x, z, 1);

                if (!upper.equals(entrance)
                        && !isInsideGuardianArena(lower, guardian)) {
                    candidates.add(upper);
                }
            }
        }

        random.shuffle(candidates);
        int targetCount = MIN_VERTICAL_CONNECTIONS + (random.nextBoolean() ? 1 : 0);
        List<EarthLabyrinthLayout.Passage> stairs = new ArrayList<>(targetCount);

        for (EarthLabyrinthLayout.Cell upper : candidates) {
            if (stairs.size() >= targetCount) {
                break;
            }
            if (!isSeparatedFromExistingStair(upper, stairs, sideCells / 3)) {
                continue;
            }

            EarthLabyrinthLayout.Cell lower = new EarthLabyrinthLayout.Cell(
                    upper.x(),
                    upper.z(),
                    1
            );
            EarthLabyrinthLayout.Passage stair = new EarthLabyrinthLayout.Passage(upper, lower);
            passages.add(stair);
            stairs.add(stair);
        }

        if (stairs.size() < MIN_VERTICAL_CONNECTIONS) {
            throw new IllegalStateException("Could not place separated Earth Labyrinth stairs");
        }

        return List.copyOf(stairs);
    }

    private static boolean isInsideGuardianArena(
            EarthLabyrinthLayout.Cell cell,
            EarthLabyrinthLayout.Cell guardian
    ) {
        return cell.level() == guardian.level()
                && Math.abs(cell.x() - guardian.x())
                <= EarthLabyrinthDimensions.GUARDIAN_ARENA_CELL_RADIUS
                && Math.abs(cell.z() - guardian.z())
                <= EarthLabyrinthDimensions.GUARDIAN_ARENA_CELL_RADIUS;
    }

    private static boolean isSeparatedFromExistingStair(
            EarthLabyrinthLayout.Cell candidate,
            List<EarthLabyrinthLayout.Passage> stairs,
            int requiredDistance
    ) {
        for (EarthLabyrinthLayout.Passage stair : stairs) {
            EarthLabyrinthLayout.Cell existing = stair.first();
            int distance = Math.abs(candidate.x() - existing.x())
                    + Math.abs(candidate.z() - existing.z());

            if (distance < requiredDistance) {
                return false;
            }
        }

        return true;
    }

    /** Stable SplitMix64-backed random source so saved world seeds retain their layouts. */
    private static final class LayoutRandom {
        private long state;

        private LayoutRandom(long seed) {
            state = seed;
        }

        private boolean nextBoolean() {
            return (nextLong() & 1L) == 0L;
        }

        private int nextInt(int bound) {
            if (bound <= 0) {
                throw new IllegalArgumentException("Random bound must be positive");
            }

            return (int) Long.remainderUnsigned(nextLong(), bound);
        }

        private <T> void shuffle(List<T> values) {
            for (int index = values.size() - 1; index > 0; index--) {
                int swapIndex = nextInt(index + 1);
                T previous = values.get(index);
                values.set(index, values.get(swapIndex));
                values.set(swapIndex, previous);
            }
        }

        private long nextLong() {
            state += 0x9E3779B97F4A7C15L;
            long value = state;
            value = (value ^ value >>> 30) * 0xBF58476D1CE4E5B9L;
            value = (value ^ value >>> 27) * 0x94D049BB133111EBL;
            return value ^ value >>> 31;
        }
    }
}
