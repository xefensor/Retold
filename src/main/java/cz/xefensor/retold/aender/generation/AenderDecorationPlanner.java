package cz.xefensor.retold.aender.generation;

import java.util.ArrayList;
import java.util.List;

/** Pure deterministic placement policy for clustered vegetation and rare terrain formations. */
final class AenderDecorationPlanner {
    private static final int FORMATION_CELL_SIZE = 112;

    private AenderDecorationPlanner() {
    }

    static double plainsVegetationStrength(long islandSeed, int x, int z) {
        return patchStrength(islandSeed, x, z, 42, 0.76D, 9.0D, 19.0D, 0x6A2A551L);
    }

    static double flowerPatchStrength(long islandSeed, int x, int z) {
        return patchStrength(islandSeed, x, z, 56, 0.46D, 6.0D, 13.0D, 0xF10AE25L);
    }

    static double groveStrength(long islandSeed, int x, int z) {
        return patchStrength(islandSeed, x, z, 72, 0.58D, 18.0D, 34.0D, 0x620AE5L);
    }

    static double cactusPatchStrength(long islandSeed, int x, int z) {
        return patchStrength(islandSeed, x, z, 48, 0.68D, 10.0D, 22.0D, 0xCAC705L);
    }

    static Formation formationAtOrigin(long islandSeed, int x, int z) {
        Formation formation = formationForCell(
                islandSeed,
                Math.floorDiv(x, FORMATION_CELL_SIZE),
                Math.floorDiv(z, FORMATION_CELL_SIZE)
        );
        return formation != null && formation.x() == x && formation.z() == z ? formation : null;
    }

    static Formation formationContaining(long islandSeed, int x, int z) {
        int cellX = Math.floorDiv(x, FORMATION_CELL_SIZE);
        int cellZ = Math.floorDiv(z, FORMATION_CELL_SIZE);
        Formation closest = null;
        double closestDistance = Double.MAX_VALUE;

        for (int cx = cellX - 1; cx <= cellX + 1; cx++) {
            for (int cz = cellZ - 1; cz <= cellZ + 1; cz++) {
                Formation candidate = formationForCell(islandSeed, cx, cz);

                if (candidate == null) {
                    continue;
                }

                double distance = Math.hypot(x - candidate.x(), z - candidate.z());

                if (distance > candidate.reservationRadius() || distance >= closestDistance) {
                    continue;
                }

                closest = candidate;
                closestDistance = distance;
            }
        }

        return closest;
    }

    static List<Formation> formationsIntersecting(
            long islandSeed,
            int minX,
            int maxX,
            int minZ,
            int maxZ
    ) {
        int margin = FormationKind.maxReservationRadius();
        int minCellX = Math.floorDiv(minX - margin, FORMATION_CELL_SIZE);
        int maxCellX = Math.floorDiv(maxX + margin, FORMATION_CELL_SIZE);
        int minCellZ = Math.floorDiv(minZ - margin, FORMATION_CELL_SIZE);
        int maxCellZ = Math.floorDiv(maxZ + margin, FORMATION_CELL_SIZE);
        List<Formation> formations = new ArrayList<>();

        for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
            for (int cellZ = minCellZ; cellZ <= maxCellZ; cellZ++) {
                Formation formation = formationForCell(islandSeed, cellX, cellZ);

                if (formation != null
                        && formation.x() + formation.reservationRadius() >= minX
                        && formation.x() - formation.reservationRadius() <= maxX
                        && formation.z() + formation.reservationRadius() >= minZ
                        && formation.z() - formation.reservationRadius() <= maxZ) {
                    formations.add(formation);
                }
            }
        }

        return List.copyOf(formations);
    }

    private static double patchStrength(
            long islandSeed,
            int x,
            int z,
            int cellSize,
            double chance,
            double minRadius,
            double maxRadius,
            long salt
    ) {
        int cellX = Math.floorDiv(x, cellSize);
        int cellZ = Math.floorDiv(z, cellSize);
        double strength = 0.0D;

        for (int cx = cellX - 1; cx <= cellX + 1; cx++) {
            for (int cz = cellZ - 1; cz <= cellZ + 1; cz++) {
                long patchSeed = mix64(
                        islandSeed
                                ^ salt
                                ^ (long) cx * 0x9E3779B97F4A7C15L
                                ^ (long) cz * 0xC2B2AE3D27D4EB4FL
                );

                if (unit(patchSeed) >= chance) {
                    continue;
                }

                int centerX = cx * cellSize + (int) (unit(patchSeed ^ 0x11L) * cellSize);
                int centerZ = cz * cellSize + (int) (unit(patchSeed ^ 0x12L) * cellSize);
                double radiusX = minRadius + unit(patchSeed ^ 0x13L) * (maxRadius - minRadius);
                double radiusZ = minRadius + unit(patchSeed ^ 0x14L) * (maxRadius - minRadius);
                double dx = (x - centerX) / radiusX;
                double dz = (z - centerZ) / radiusZ;
                double distance = Math.sqrt(dx * dx + dz * dz);

                if (distance < 1.0D) {
                    strength = Math.max(strength, 1.0D - distance);
                }
            }
        }

        return strength;
    }

    static Formation formationForCell(long islandSeed, int cellX, int cellZ) {
        long formationSeed = mix64(
                islandSeed
                        ^ (long) cellX * 0xD1342543DE82EF95L
                        ^ (long) cellZ * 0xC6BC279692B5CC83L
                        ^ 0xF02AA710EL
        );

        if (unit(formationSeed) >= 0.18D) {
            return null;
        }

        int x = cellX * FORMATION_CELL_SIZE + 18
                + (int) (unit(formationSeed ^ 0x11L) * (FORMATION_CELL_SIZE - 36));
        int z = cellZ * FORMATION_CELL_SIZE + 18
                + (int) (unit(formationSeed ^ 0x12L) * (FORMATION_CELL_SIZE - 36));
        double kindRoll = unit(formationSeed ^ 0x13L);
        FormationKind kind;

        if (kindRoll < 0.58D) {
            kind = FormationKind.SPIRE;
        } else {
            kind = FormationKind.CRATER;
        }

        return new Formation(x, z, kind, formationSeed);
    }

    private static double unit(long seed) {
        long value = mix64(seed);
        return (value >>> 11) * 0x1.0p-53;
    }

    private static long mix64(long value) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= value >>> 33;
        return value;
    }

    enum FormationKind {
        SPIRE(5),
        CRATER(9);

        private final int reservationRadius;

        FormationKind(int reservationRadius) {
            this.reservationRadius = reservationRadius;
        }

        private static int maxReservationRadius() {
            int maximum = 0;

            for (FormationKind kind : values()) {
                maximum = Math.max(maximum, kind.reservationRadius);
            }

            return maximum;
        }
    }

    record Formation(int x, int z, FormationKind kind, long seed) {
        int reservationRadius() {
            return kind.reservationRadius;
        }
    }
}
