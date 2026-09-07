package cz.xefensor.retold.worldgen.earth;

public enum EarthGuardianLifecycle {
    DORMANT,
    AWAKENING,
    ROAMING,
    DEFEATED;

    static EarthGuardianLifecycle bySerializedId(int id) {
        EarthGuardianLifecycle[] values = values();
        return id >= 0 && id < values.length ? values[id] : DORMANT;
    }
}
