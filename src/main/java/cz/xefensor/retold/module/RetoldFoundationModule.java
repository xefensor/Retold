package cz.xefensor.retold.module;

import cz.xefensor.retold.client.RetoldClientEvents;
import cz.xefensor.retold.event.RetoldCommandEvents;
import cz.xefensor.retold.event.RetoldPlayerSyncEvents;
import cz.xefensor.retold.event.RetoldSleepEvents;
import cz.xefensor.retold.event.TorchWeatherEvents;
import cz.xefensor.retold.gametest.RetoldGameTests;
import cz.xefensor.retold.network.RetoldNetworking;
import cz.xefensor.retold.registry.RetoldBlocks;
import cz.xefensor.retold.registry.RetoldEntityEvents;
import cz.xefensor.retold.registry.RetoldEntityTypes;
import cz.xefensor.retold.registry.RetoldGameRules;
import cz.xefensor.retold.villager.RetoldVillagerTeachingReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;

public final class RetoldFoundationModule {
    private RetoldFoundationModule() {
    }

    public static void registerModBus(IEventBus modEventBus) {
        RetoldBlocks.register(modEventBus);
        RetoldEntityTypes.register(modEventBus);
        RetoldGameRules.register(modEventBus);

        modEventBus.addListener(RetoldNetworking::registerPayloads);
        modEventBus.addListener(RetoldEntityEvents::registerAttributes);
        modEventBus.addListener(RetoldEntityEvents::registerSpawnPlacements);
        modEventBus.addListener(RetoldGameTests::register);

        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            RetoldClientEvents.register(modEventBus);
        }
    }

    public static void registerGameBus(IEventBus gameEventBus) {
        gameEventBus.register(RetoldCommandEvents.class);
        gameEventBus.register(RetoldPlayerSyncEvents.class);
        gameEventBus.register(RetoldSleepEvents.class);
        gameEventBus.register(TorchWeatherEvents.class);
        gameEventBus.addListener(RetoldFoundationModule::addServerReloadListeners);
    }

    private static void addServerReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(
                RetoldVillagerTeachingReloadListener.ID,
                new RetoldVillagerTeachingReloadListener()
        );
    }
}
