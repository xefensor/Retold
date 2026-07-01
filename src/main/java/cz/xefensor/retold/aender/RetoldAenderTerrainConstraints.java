package cz.xefensor.retold.aender;

public record RetoldAenderTerrainConstraints(
        boolean constrainNorth,
        boolean constrainSouth,
        boolean constrainWest,
        boolean constrainEast
) {
    public static final RetoldAenderTerrainConstraints NONE =
            new RetoldAenderTerrainConstraints(false, false, false, false);

    public boolean hasAnyConstraint() {
        return constrainNorth
                || constrainSouth
                || constrainWest
                || constrainEast;
    }
}