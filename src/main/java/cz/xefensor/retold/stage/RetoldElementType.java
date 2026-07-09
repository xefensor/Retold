package cz.xefensor.retold.stage;

public enum RetoldElementType {
    WATER(0, "Water Element", "water"),
    FIRE(1, "Fire Element", "fire"),
    EARTH(2, "Earth Element", "earth"),
    AIR(3, "Air Element", "air");

    private final int bit;
    private final String displayName;
    private final String absorbedName;

    RetoldElementType(int bit, String displayName, String absorbedName) {
        this.bit = bit;
        this.displayName = displayName;
        this.absorbedName = absorbedName;
    }

    public int mask() {
        return 1 << bit;
    }

    public String displayName() {
        return displayName;
    }

    public String absorbedName() {
        return absorbedName;
    }
}
