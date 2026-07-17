package cz.xefensor.retold.module;

import cz.xefensor.retold.event.RetoldEndProgressionEvents;
import cz.xefensor.retold.event.RetoldStageRuntimeEvents;
import cz.xefensor.retold.recipe.RetoldRecipeBookEvents;
import cz.xefensor.retold.worldgen.delayed.RetoldPatrolStageEvents;
import net.neoforged.bus.api.IEventBus;

public final class RetoldStageModule {
    private RetoldStageModule() {
    }

    public static void registerGameBus(IEventBus gameEventBus) {
        gameEventBus.register(RetoldStageRuntimeEvents.class);
        gameEventBus.register(RetoldEndProgressionEvents.class);
        gameEventBus.register(RetoldRecipeBookEvents.class);
        gameEventBus.register(RetoldPatrolStageEvents.class);
    }
}
