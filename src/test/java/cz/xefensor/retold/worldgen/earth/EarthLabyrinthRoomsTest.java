package cz.xefensor.retold.worldgen.earth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EarthLabyrinthRoomsTest {
    @Test
    void roomsAreStableBoundedSideBranchesAwayFromCriticalAnchors() {
        for (long seed = 0; seed < 512; seed++) {
            EarthLabyrinthLayout layout = EarthLabyrinthPlanner.generate(seed);
            var rooms = EarthLabyrinthRooms.plan(layout, seed);
            assertEquals(rooms, EarthLabyrinthRooms.plan(layout, seed));
            assertTrue(rooms.size() <= 12);
            for (int level = 0; level < 2; level++) {
                int floor = level;
                assertTrue(rooms.stream().anyMatch(room -> room.cell().level() == floor
                        && room.kind() == EarthLabyrinthRooms.Kind.TREASURE), "Treasure on each floor: " + seed);
                assertTrue(rooms.stream().anyMatch(room -> room.cell().level() == floor
                        && room.kind() == EarthLabyrinthRooms.Kind.ARROW_TRAP));
            }
            for (var room : rooms) {
                assertEquals(EarthLabyrinthLayout.CellKind.PASSAGE, layout.kindAt(room.cell()));
                assertEquals(1, layout.neighbors(room.cell()).size());
                var mouth = layout.neighbors(room.cell()).getFirst();
                assertEquals(room.cell().x() - mouth.x(), room.backX());
                assertEquals(room.cell().z() - mouth.z(), room.backZ());
                if (room.cell().level() == 1) {
                    assertTrue(Math.abs(room.cell().x() - layout.guardianChamber().x()) > 4
                            || Math.abs(room.cell().z() - layout.guardianChamber().z()) > 4);
                }
            }
        }
    }
}
