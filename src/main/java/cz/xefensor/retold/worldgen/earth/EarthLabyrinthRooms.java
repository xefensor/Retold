package cz.xefensor.retold.worldgen.earth;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Stable side-branch content selection, independent of topology and its saved random stream. */
final class EarthLabyrinthRooms {
    static final int CONTENT_VERSION = 1;

    private EarthLabyrinthRooms() {
    }

    static List<Room> plan(EarthLabyrinthLayout layout, long seed) {
        List<Room> result = new ArrayList<>();
        for (int level = 0; level < EarthLabyrinthLayout.LEVEL_COUNT; level++) {
            List<EarthLabyrinthLayout.Cell> candidates = new ArrayList<>();
            for (EarthLabyrinthLayout.Cell cell : layout.cells()) {
                if (cell.level() == level && eligible(layout, cell)) {
                    candidates.add(cell);
                }
            }
            candidates.sort(Comparator.comparingLong((EarthLabyrinthLayout.Cell cell) -> roomSeed(seed, cell))
                    .thenComparing(Comparator.naturalOrder()));
            int count = 0;
            for (EarthLabyrinthLayout.Cell cell : candidates) {
                if (count >= 6) {
                    break;
                }
                if (result.stream().anyMatch(room -> distance(room.cell(), cell) <= 2)) {
                    continue;
                }
                EarthLabyrinthLayout.Cell mouth = layout.neighbors(cell).getFirst();
                result.add(new Room(cell, cell.x() - mouth.x(), cell.z() - mouth.z(),
                        count % 3 == 1 ? Kind.TREASURE : Kind.ARROW_TRAP, roomSeed(seed, cell)));
                count++;
            }
        }
        return List.copyOf(result);
    }

    private static boolean eligible(EarthLabyrinthLayout layout, EarthLabyrinthLayout.Cell cell) {
        if (layout.kindAt(cell) != EarthLabyrinthLayout.CellKind.PASSAGE
                || layout.neighbors(cell).size() != 1
                || distance(cell, layout.entrance()) <= 2) {
            return false;
        }
        EarthLabyrinthLayout.Cell guardian = layout.guardianChamber();
        if (cell.level() == guardian.level()
                && Math.abs(cell.x() - guardian.x()) <= EarthLabyrinthDimensions.GUARDIAN_ARENA_CELL_RADIUS + 1
                && Math.abs(cell.z() - guardian.z()) <= EarthLabyrinthDimensions.GUARDIAN_ARENA_CELL_RADIUS + 1) {
            return false;
        }
        return layout.cellKinds().entrySet().stream().noneMatch(entry ->
                entry.getValue() == EarthLabyrinthLayout.CellKind.STAIR && distance(entry.getKey(), cell) <= 1);
    }

    private static int distance(EarthLabyrinthLayout.Cell first, EarthLabyrinthLayout.Cell second) {
        return first.level() == second.level()
                ? Math.abs(first.x() - second.x()) + Math.abs(first.z() - second.z())
                : Integer.MAX_VALUE;
    }

    private static long roomSeed(long seed, EarthLabyrinthLayout.Cell cell) {
        long value = seed ^ (long) cell.x() * 0x9E3779B185EBCA87L
                ^ (long) cell.z() * 0xC2B2AE3D27D4EB4FL ^ (long) cell.level() * 0x165667B19E3779F9L;
        value = (value ^ value >>> 30) * 0xBF58476D1CE4E5B9L;
        return (value ^ value >>> 27) * 0x94D049BB133111EBL;
    }

    enum Kind {
        ARROW_TRAP,
        TREASURE
    }

    record Room(EarthLabyrinthLayout.Cell cell, int backX, int backZ, Kind kind, long lootSeed) {
    }
}
