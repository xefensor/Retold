package cz.xefensor.retold.module;

import cz.xefensor.retold.event.RetoldElderGuardianEvents;
import cz.xefensor.retold.event.RetoldEndermanEvents;
import cz.xefensor.retold.event.RetoldGolemEvents;
import cz.xefensor.retold.event.RetoldPiglinEvents;
import cz.xefensor.retold.event.RetoldUndeadEvents;
import net.neoforged.bus.api.IEventBus;

public final class RetoldMobModule {
    private RetoldMobModule() {
    }

    public static void registerGameBus(IEventBus gameEventBus) {
        gameEventBus.register(RetoldUndeadEvents.class);
        gameEventBus.register(RetoldPiglinEvents.class);
        gameEventBus.register(RetoldGolemEvents.class);
        gameEventBus.register(RetoldEndermanEvents.class);
        gameEventBus.register(RetoldElderGuardianEvents.class);
    }
}
