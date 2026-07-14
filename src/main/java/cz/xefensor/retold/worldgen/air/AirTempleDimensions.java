package cz.xefensor.retold.worldgen.air;

public final class AirTempleDimensions {
    public static final int MAIN_ISLAND_RADIUS = 22;
    public static final int HORIZONTAL_RADIUS = 48;
    public static final int MAX_ISLAND_DEPTH = 22;
    public static final int CRATER_RADIUS = 24;
    public static final int CRATER_MAX_DEPTH = 14;
    public static final int MIN_ISLAND_Y = 224;
    public static final int SURFACE_CLEARANCE = 92;
    public static final int TOWER_HEIGHT = 34;
    private static final int[] TOWER_FLOOR_OFFSETS = {2, 9, 16, 23, 30};
    public static final int TOP_CLEARANCE = 12;
    public static final int WIND_BELOW_UNDERSIDE = 5;
    public static final int WIND_ABOVE_TOWER = 10;

    private AirTempleDimensions() {
    }

    public static int islandUndersideY(int islandY) {
        return islandY - MAX_ISLAND_DEPTH;
    }

    public static int towerTopY(int islandY) {
        return islandY + TOWER_HEIGHT;
    }

    public static int[] towerFloorOffsets() {
        return TOWER_FLOOR_OFFSETS.clone();
    }

    public static int windMinY(int islandY) {
        return islandUndersideY(islandY) - WIND_BELOW_UNDERSIDE;
    }

    public static int windMaxY(int islandY) {
        return towerTopY(islandY) + WIND_ABOVE_TOWER;
    }
}
