package cz.xefensor.retold.module;

import cz.xefensor.retold.ambient.RetoldHorizonEvents;
import net.neoforged.bus.api.IEventBus;

public final class RetoldAtmosphereModule {
    private RetoldAtmosphereModule() {
    }

    public static void registerGameBus(IEventBus gameEventBus) {
        gameEventBus.register(RetoldHorizonEvents.class);
    }
}
