package cz.xefensor.retold.worldgen.earth;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Immutable, block-independent topology for one two-level Earth Labyrinth.
 * Cell coordinates are local to the labyrinth rather than world coordinates.
 */
record EarthLabyrinthLayout(
        int sideCells,
        List<Passage> passages,
        Map<Cell, CellKind> cellKinds
) {
    static final int LEVEL_COUNT = 2;
    static final int CELL_PITCH_BLOCKS = 6;
    static final int CELL_ROOM_WIDTH_BLOCKS = 5;

    EarthLabyrinthLayout {
        if (sideCells < 3 || sideCells % 2 == 0) {
            throw new IllegalArgumentException("Earth Labyrinth sides must use an odd cell count");
        }

        passages = List.copyOf(passages);
        cellKinds = Map.copyOf(cellKinds);
    }

    int footprintWidthBlocks() {
        return (sideCells - 1) * CELL_PITCH_BLOCKS + CELL_ROOM_WIDTH_BLOCKS;
    }

    Cell entrance() {
        return new Cell(sideCells / 2, 0, 0);
    }

    Cell guardianChamber() {
        return new Cell(sideCells / 2, sideCells / 2, LEVEL_COUNT - 1);
    }

    List<Cell> cells() {
        List<Cell> cells = new ArrayList<>(sideCells * sideCells * LEVEL_COUNT);

        for (int level = 0; level < LEVEL_COUNT; level++) {
            for (int z = 0; z < sideCells; z++) {
                for (int x = 0; x < sideCells; x++) {
                    cells.add(new Cell(x, z, level));
                }
            }
        }

        return List.copyOf(cells);
    }

    List<Cell> neighbors(Cell cell) {
        List<Cell> neighbors = new ArrayList<>();

        for (Passage passage : passages) {
            if (passage.first().equals(cell)) {
                neighbors.add(passage.second());
            } else if (passage.second().equals(cell)) {
                neighbors.add(passage.first());
            }
        }

        neighbors.sort(Cell::compareTo);
        return List.copyOf(neighbors);
    }

    CellKind kindAt(Cell cell) {
        return cellKinds.getOrDefault(cell, CellKind.PASSAGE);
    }

    long verticalPassageCount() {
        return passages.stream().filter(Passage::isVertical).count();
    }

    enum CellKind {
        PASSAGE,
        ENTRANCE,
        STAIR,
        GUARDIAN_CHAMBER
    }

    record Cell(int x, int z, int level) implements Comparable<Cell> {
        @Override
        public int compareTo(Cell other) {
            int levelOrder = Integer.compare(level, other.level);

            if (levelOrder != 0) {
                return levelOrder;
            }

            int zOrder = Integer.compare(z, other.z);
            return zOrder != 0 ? zOrder : Integer.compare(x, other.x);
        }
    }

    record Passage(Cell first, Cell second) {
        Passage {
            if (!areAdjacent(first, second)) {
                throw new IllegalArgumentException("Labyrinth passages must join adjacent cells");
            }

            if (first.compareTo(second) > 0) {
                Cell previousFirst = first;
                first = second;
                second = previousFirst;
            }
        }

        boolean isVertical() {
            return first.level() != second.level();
        }

        private static boolean areAdjacent(Cell first, Cell second) {
            int distance = Math.abs(first.x() - second.x())
                    + Math.abs(first.z() - second.z())
                    + Math.abs(first.level() - second.level());
            return distance == 1;
        }
    }
}
