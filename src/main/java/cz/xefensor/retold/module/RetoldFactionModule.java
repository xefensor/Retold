package cz.xefensor.retold.module;

import cz.xefensor.retold.event.RetoldFactionAssistEvents;
import cz.xefensor.retold.event.RetoldFactionCombatEvents;
import cz.xefensor.retold.event.RetoldInvalidPlayerTargetEvents;
import net.neoforged.bus.api.IEventBus;

public final class RetoldFactionModule {
    private RetoldFactionModule() {
    }

    public static void registerGameBus(IEventBus gameEventBus) {
        gameEventBus.register(RetoldInvalidPlayerTargetEvents.class);
        gameEventBus.register(RetoldFactionCombatEvents.class);
        gameEventBus.register(RetoldFactionAssistEvents.class);
    }
}
