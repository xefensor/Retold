package cz.xefensor.retold.module;

import cz.xefensor.retold.aender.RetoldAenderRegistries;
import cz.xefensor.retold.aender.stability.AenderChunkEvents;
import cz.xefensor.retold.aender.stability.AenderRealityTickEvents;
import cz.xefensor.retold.aender.stability.AenderStabilizerEvents;
import cz.xefensor.retold.aender.stability.AenderWorldTickEvents;
import cz.xefensor.retold.event.AenderChronolithEvents;
import net.neoforged.bus.api.IEventBus;

public final class RetoldAenderModule {
    private RetoldAenderModule() {
    }

    public static void registerModBus(IEventBus modEventBus) {
        RetoldAenderRegistries.register(modEventBus);
    }

    public static void registerGameBus(IEventBus gameEventBus) {
        gameEventBus.register(AenderChunkEvents.class);
        gameEventBus.register(AenderStabilizerEvents.class);
        gameEventBus.register(AenderWorldTickEvents.class);
        gameEventBus.register(AenderRealityTickEvents.class);
        gameEventBus.register(AenderChronolithEvents.class);
    }
}
