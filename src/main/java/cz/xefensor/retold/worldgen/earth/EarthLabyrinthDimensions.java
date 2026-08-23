package cz.xefensor.retold.worldgen.earth;

final class EarthLabyrinthDimensions {
    static final int PYRAMID_WIDTH = 12;
    static final int PYRAMID_DEPTH = 15;
    static final int UPPER_LEVEL_DEPTH = 64;
    static final int LEVEL_SEPARATION = 7;
    static final int CHAMBER_HEIGHT = 5;
    static final int CONNECTION_LOCAL_X = 9;
    static final int CONNECTION_LOCAL_Y = -4;
    static final int CONNECTION_LOCAL_Z = 9;
    static final int ENTRANCE_SHAFT_DEPTH = 8;

    private EarthLabyrinthDimensions() {
    }

    static int upperFloorY(int pyramidBaseY) {
        return pyramidBaseY - UPPER_LEVEL_DEPTH;
    }

    static int floorY(int pyramidBaseY, int level) {
        return upperFloorY(pyramidBaseY) - level * LEVEL_SEPARATION;
    }
}
