package cz.xefensor.retold.stage;

/**
 * One of the six distinct offerings in the Dragon Egg ritual. The bit values
 * for the original four elements are save-format state and must not move.
 */
public enum RetoldRitualOffering {
    WATER(0, "Heart of the Sea", "water"),
    FIRE(1, "Nether Reactor Core", "fire"),
    EARTH(2, "Lodestone", "earth"),
    AIR(3, "Heavy Core", "air"),
    LIFE(4, "Totem of Undying", "life"),
    DEATH(5, "Nether Star", "death");

    private final int bit;
    private final String artifactName;
    private final String forceName;

    RetoldRitualOffering(int bit, String artifactName, String forceName) {
        this.bit = bit;
        this.artifactName = artifactName;
        this.forceName = forceName;
    }

    public int mask() {
        return 1 << bit;
    }

    public String artifactName() {
        return artifactName;
    }

    public String forceName() {
        return forceName;
    }
}
