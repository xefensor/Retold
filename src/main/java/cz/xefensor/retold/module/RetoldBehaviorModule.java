package cz.xefensor.retold.module;

import cz.xefensor.retold.behavior.control.RetoldControlledCombatEvents;
import cz.xefensor.retold.behavior.breeding.RetoldAnimalBreeding;
import cz.xefensor.retold.behavior.core.RetoldBehaviorEntityTickDispatcher;
import cz.xefensor.retold.behavior.debug.RetoldBehaviorDebugEvents;
import cz.xefensor.retold.behavior.ecology.RetoldUnloadedEcosystemCatchUp;
import cz.xefensor.retold.behavior.flee.RetoldControlledFleeEvents;
import cz.xefensor.retold.behavior.food.RetoldFoodBehaviorEvents;
import cz.xefensor.retold.behavior.hunting.RetoldControlledHuntingEvents;
import cz.xefensor.retold.behavior.hunting.RetoldPredatorStaminaEvents;
import cz.xefensor.retold.behavior.profiles.RetoldMobProfileReloadListener;
import cz.xefensor.retold.behavior.species.RetoldSlimeItemStorage;
import cz.xefensor.retold.behavior.species.RetoldSlimeSplitBehavior;
import cz.xefensor.retold.behavior.species.RetoldAxolotlGuardianCombatEvents;
import cz.xefensor.retold.behavior.species.RetoldBatColonyEvents;
import cz.xefensor.retold.behavior.species.RetoldDolphinPodEvents;
import cz.xefensor.retold.behavior.species.RetoldHiveColonyEvents;
import cz.xefensor.retold.behavior.species.RetoldParrotForagerEvents;
import cz.xefensor.retold.behavior.species.RetoldPhantomStalkerEvents;
import cz.xefensor.retold.behavior.species.RetoldUndeadMountEvents;
import cz.xefensor.retold.villager.RetoldVillageReputationEvents;
import cz.xefensor.retold.villager.RetoldVillageAnimalEvents;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;

public final class RetoldBehaviorModule {
    private RetoldBehaviorModule() {
    }

    public static void registerModBus(IEventBus modEventBus) {
        modEventBus.addListener(RetoldUndeadMountEvents::onModifyAttributes);
    }

    public static void registerGameBus(IEventBus gameEventBus) {
        gameEventBus.register(RetoldBehaviorEntityTickDispatcher.class);
        gameEventBus.register(RetoldUnloadedEcosystemCatchUp.class);
        gameEventBus.register(RetoldFoodBehaviorEvents.class);
        gameEventBus.register(RetoldControlledHuntingEvents.class);
        gameEventBus.register(RetoldBehaviorDebugEvents.class);
        gameEventBus.register(RetoldControlledCombatEvents.class);
        gameEventBus.register(RetoldPredatorStaminaEvents.class);
        gameEventBus.register(RetoldSlimeItemStorage.class);
        gameEventBus.register(RetoldSlimeSplitBehavior.class);
        gameEventBus.register(RetoldAxolotlGuardianCombatEvents.class);
        gameEventBus.register(RetoldBatColonyEvents.class);
        gameEventBus.register(RetoldParrotForagerEvents.class);
        gameEventBus.register(RetoldVillageReputationEvents.class);
        gameEventBus.register(RetoldVillageAnimalEvents.class);
        gameEventBus.addListener(RetoldControlledFleeEvents::onLivingDamage);
        gameEventBus.addListener(RetoldDolphinPodEvents::onLivingDamage);
        gameEventBus.addListener(RetoldHiveColonyEvents::onLivingDamage);
        gameEventBus.addListener(RetoldUndeadMountEvents::onLivingDamage);
        gameEventBus.addListener(
                EventPriority.LOWEST,
                RetoldPhantomStalkerEvents::onPlayerSpawnPhantoms
        );
        gameEventBus.addListener(
                EventPriority.LOWEST,
                RetoldUndeadMountEvents::onEntityMount
        );
        gameEventBus.addListener(
                EventPriority.LOWEST,
                RetoldHiveColonyEvents::onHiveHarvest
        );
        gameEventBus.addListener(
                EventPriority.LOWEST,
                RetoldHiveColonyEvents::onHiveBreak
        );
        gameEventBus.addListener(RetoldAnimalBreeding::onLivingDamage);
        gameEventBus.addListener(RetoldBehaviorModule::addServerReloadListeners);
    }

    private static void addServerReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(
                RetoldMobProfileReloadListener.ID,
                new RetoldMobProfileReloadListener()
        );
    }
}
