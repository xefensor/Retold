package cz.xefensor.retold.module;

import cz.xefensor.retold.behavior.control.RetoldControlledCombatEvents;
import cz.xefensor.retold.behavior.core.RetoldBehaviorEntityTickDispatcher;
import cz.xefensor.retold.behavior.debug.RetoldBehaviorDebugEvents;
import cz.xefensor.retold.behavior.food.RetoldFoodBehaviorEvents;
import cz.xefensor.retold.behavior.hunting.RetoldControlledHuntingEvents;
import cz.xefensor.retold.behavior.hunting.RetoldPredatorStaminaEvents;
import net.neoforged.bus.api.IEventBus;

public final class RetoldBehaviorModule {
    private RetoldBehaviorModule() {
    }

    public static void registerGameBus(IEventBus gameEventBus) {
        gameEventBus.register(RetoldBehaviorEntityTickDispatcher.class);
        gameEventBus.register(RetoldFoodBehaviorEvents.class);
        gameEventBus.register(RetoldControlledHuntingEvents.class);
        gameEventBus.register(RetoldBehaviorDebugEvents.class);
        gameEventBus.register(RetoldControlledCombatEvents.class);
        gameEventBus.register(RetoldPredatorStaminaEvents.class);
    }
}
