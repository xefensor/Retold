package cz.xefensor.retold.module;

import net.neoforged.bus.api.IEventBus;

public final class RetoldSubsystems {
    private RetoldSubsystems() {
    }

    public static void register(
            IEventBus modEventBus,
            IEventBus gameEventBus
    ) {
        registerModBus(modEventBus);
        registerGameBus(gameEventBus);
    }

    private static void registerModBus(IEventBus modEventBus) {
        RetoldFoundationModule.registerModBus(modEventBus);
        RetoldWorldgenModule.registerModBus(modEventBus);
        RetoldAenderModule.registerModBus(modEventBus);
    }

    private static void registerGameBus(IEventBus gameEventBus) {
        RetoldFoundationModule.registerGameBus(gameEventBus);
        RetoldStageModule.registerGameBus(gameEventBus);
        RetoldMobModule.registerGameBus(gameEventBus);
        RetoldWorldgenModule.registerGameBus(gameEventBus);
        RetoldAenderModule.registerGameBus(gameEventBus);
        RetoldFactionModule.registerGameBus(gameEventBus);
        RetoldTerritoryModule.registerGameBus(gameEventBus);
        RetoldBehaviorModule.registerGameBus(gameEventBus);
    }
}
