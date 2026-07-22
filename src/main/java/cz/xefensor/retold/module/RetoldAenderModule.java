package cz.xefensor.retold.module;

import cz.xefensor.retold.aender.RetoldAenderRegistries;
import cz.xefensor.retold.aender.generation.AenderAttachments;
import cz.xefensor.retold.aender.stability.AenderChunkEvents;
import cz.xefensor.retold.aender.stability.AenderRealityTickEvents;
import cz.xefensor.retold.aender.stability.AenderStabilizerEvents;
import cz.xefensor.retold.aender.stability.AenderWorldTickEvents;
import cz.xefensor.retold.event.AenderChronolithEvents;
import cz.xefensor.retold.registry.RetoldAenderWood;
import cz.xefensor.retold.registry.RetoldCreativeModeTabs;
import net.neoforged.bus.api.IEventBus;

public final class RetoldAenderModule {
    private RetoldAenderModule() {
    }

    public static void registerModBus(IEventBus modEventBus) {
        RetoldAenderRegistries.register(modEventBus);
        RetoldAenderWood.register(modEventBus);
        RetoldCreativeModeTabs.register(modEventBus);
        AenderAttachments.register(modEventBus);
    }

    public static void registerGameBus(IEventBus gameEventBus) {
        gameEventBus.register(AenderChunkEvents.class);
        gameEventBus.register(AenderStabilizerEvents.class);
        gameEventBus.register(AenderWorldTickEvents.class);
        gameEventBus.register(AenderRealityTickEvents.class);
        gameEventBus.register(AenderChronolithEvents.class);
    }
}
