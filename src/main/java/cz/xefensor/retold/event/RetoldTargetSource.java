package cz.xefensor.retold.event;

public enum RetoldTargetSource {
    VANILLA(0),
    FACTION_COMBAT(10),
    FACTION_ASSIST(20),
    TERRITORY_ATTACK(30),
    RETALIATION(40);

    private final int priority;

    RetoldTargetSource(int priority) {
        this.priority = priority;
    }

    public int priority() {
        return priority;
    }
}