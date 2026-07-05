package cz.xefensor.retold.event;

import cz.xefensor.retold.faction.RetoldFaction;

public record RetoldTerritoryContext(
        RetoldFaction faction,
        String dimensionId,
        int structureX,
        int structureZ
) {
    public String reputationKey() {
        return faction.name() + "|" + dimensionId + "|" + structureX + "|" + structureZ;
    }

    public String debugName() {
        return faction.name() + " @ " + structureX + ", " + structureZ;
    }
}