package cz.xefensor.retold.module;

import cz.xefensor.retold.territory.RetoldTerritoryEvents;
import cz.xefensor.retold.territory.RetoldTerritoryIllegalActionEvents;
import cz.xefensor.retold.territory.RetoldTerritoryReputationDebugEvents;
import net.neoforged.bus.api.IEventBus;

public final class RetoldTerritoryModule {
    private RetoldTerritoryModule() {
    }

    public static void registerGameBus(IEventBus gameEventBus) {
        gameEventBus.register(RetoldTerritoryEvents.class);
        gameEventBus.register(RetoldTerritoryIllegalActionEvents.class);
        gameEventBus.register(RetoldTerritoryReputationDebugEvents.class);
    }
}
