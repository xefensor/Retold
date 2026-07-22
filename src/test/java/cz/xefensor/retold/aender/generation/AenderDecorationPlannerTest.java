package cz.xefensor.retold.aender.generation;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AenderDecorationPlannerTest {
    @Test
    void vegetationPlannersCreateClearingsAndDensePatches() {
        long seed = 0xA3D1E41F29B7C53DL;
        double minimum = 1.0D;
        double maximum = 0.0D;

        for (int x = -512; x <= 512; x += 8) {
            for (int z = -512; z <= 512; z += 8) {
                double strength = AenderDecorationPlanner.plainsVegetationStrength(seed, x, z);
                minimum = Math.min(minimum, strength);
                maximum = Math.max(maximum, strength);
                assertTrue(strength >= 0.0D && strength <= 1.0D);
            }
        }

        assertEquals(0.0D, minimum, "The vegetation plan must leave intentional clearings");
        assertTrue(maximum > 0.85D, "The vegetation plan must also create dense patch centers");
    }

    @Test
    void rareFormationPlanProducesEveryFormationKindAndRoundTripsOrigins() {
        long seed = 0x632BE59BD9B4E019L;
        EnumSet<AenderDecorationPlanner.FormationKind> kinds =
                EnumSet.noneOf(AenderDecorationPlanner.FormationKind.class);

        for (int cellX = -32; cellX <= 32; cellX++) {
            for (int cellZ = -32; cellZ <= 32; cellZ++) {
                AenderDecorationPlanner.Formation formation = AenderDecorationPlanner.formationForCell(
                        seed,
                        cellX,
                        cellZ
                );

                if (formation == null) {
                    continue;
                }

                kinds.add(formation.kind());
                assertEquals(
                        formation,
                        AenderDecorationPlanner.formationAtOrigin(seed, formation.x(), formation.z())
                );
                assertNotNull(AenderDecorationPlanner.formationContaining(seed, formation.x(), formation.z()));
                assertTrue(
                        AenderDecorationPlanner.formationsIntersecting(
                                seed,
                                formation.x() + formation.reservationRadius(),
                                formation.x() + formation.reservationRadius(),
                                formation.z(),
                                formation.z()
                        ).contains(formation),
                        "A formation must be planned for every chunk area touched by its reservation radius"
                );
            }
        }

        assertEquals(EnumSet.allOf(AenderDecorationPlanner.FormationKind.class), kinds);
    }
}
