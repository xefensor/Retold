package cz.xefensor.retold.behavior;

public record RetoldMobProfile(
        RetoldMobProfileType type,
        boolean managed,
        boolean predator,
        boolean packSocial,
        boolean territoryGuard,
        int hungerIntervalTicks,
        int eatThreshold,
        int huntThreshold
) {
    public boolean is(RetoldMobProfileType type) {
        return this.type == type;
    }
}
