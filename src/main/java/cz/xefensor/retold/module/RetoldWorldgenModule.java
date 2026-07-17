package cz.xefensor.retold.module;

import cz.xefensor.retold.worldgen.RetoldWorldSpawnCache;
import cz.xefensor.retold.worldgen.RetoldWorldgenRegistries;
import cz.xefensor.retold.worldgen.air.GaleCoreAttackEvents;
import cz.xefensor.retold.worldgen.air.wind.AirTempleWindEvents;
import cz.xefensor.retold.worldgen.delayed.RetoldAttachments;
import cz.xefensor.retold.worldgen.delayed.RetoldChunkEditEvents;
import cz.xefensor.retold.worldgen.delayed.RetoldClientChunkTracker;
import cz.xefensor.retold.worldgen.delayed.RetoldDelayedStructureMobBlocker;
import cz.xefensor.retold.worldgen.delayed.RetoldDelayedStructureRetrogen;
import cz.xefensor.retold.worldgen.delayed.RetoldRetrogenDropBlocker;
import net.neoforged.bus.api.IEventBus;

public final class RetoldWorldgenModule {
    private RetoldWorldgenModule() {
    }

    public static void registerModBus(IEventBus modEventBus) {
        RetoldWorldgenRegistries.register(modEventBus);
        RetoldAttachments.register(modEventBus);
    }

    public static void registerGameBus(IEventBus gameEventBus) {
        gameEventBus.register(RetoldWorldSpawnCache.class);
        gameEventBus.register(AirTempleWindEvents.class);
        gameEventBus.register(GaleCoreAttackEvents.class);
        gameEventBus.register(RetoldChunkEditEvents.class);
        gameEventBus.register(RetoldDelayedStructureRetrogen.class);
        gameEventBus.register(RetoldDelayedStructureMobBlocker.class);
        gameEventBus.register(RetoldClientChunkTracker.class);
        gameEventBus.register(RetoldRetrogenDropBlocker.class);
    }
}
