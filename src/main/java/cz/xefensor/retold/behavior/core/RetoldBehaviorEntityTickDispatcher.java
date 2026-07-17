package cz.xefensor.retold.behavior.core;

import cz.xefensor.retold.behavior.performance.RetoldAiTickContext;
import cz.xefensor.retold.behavior.species.RetoldAmphibianForagerEvents;
import cz.xefensor.retold.behavior.home.RetoldAnimalHomeRepairEvents;
import cz.xefensor.retold.behavior.species.RetoldArmadilloDefenseEvents;
import cz.xefensor.retold.behavior.species.RetoldAxolotlHelperEvents;
import cz.xefensor.retold.behavior.species.RetoldCommanderSupportEvents;
import cz.xefensor.retold.behavior.flee.RetoldControlledFleeEvents;
import cz.xefensor.retold.behavior.hunting.RetoldControlledRegroupEvents;
import cz.xefensor.retold.behavior.species.RetoldDolphinPodEvents;
import cz.xefensor.retold.behavior.species.RetoldGhastArtilleryEvents;
import cz.xefensor.retold.behavior.food.RetoldHeldFoodConsumptionEvents;
import cz.xefensor.retold.behavior.home.RetoldHerdRangeEvents;
import cz.xefensor.retold.behavior.species.RetoldHiveColonyEvents;
import cz.xefensor.retold.behavior.species.RetoldIllagerRoamingEvents;
import cz.xefensor.retold.behavior.profiles.RetoldMobProfile;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;
import cz.xefensor.retold.behavior.profiles.RetoldMobStateRecoveryEvents;
import cz.xefensor.retold.behavior.species.RetoldNetherBehaviorEvents;
import cz.xefensor.retold.behavior.species.RetoldNeutralWildlifeEvents;
import cz.xefensor.retold.behavior.pack.RetoldPackHomeEvents;
import cz.xefensor.retold.behavior.pack.RetoldPackHuntingEvents;
import cz.xefensor.retold.behavior.species.RetoldPandaBambooEvents;
import cz.xefensor.retold.behavior.species.RetoldPhantomStalkerEvents;
import cz.xefensor.retold.behavior.hunting.RetoldPredatorSearchEvents;
import cz.xefensor.retold.behavior.species.RetoldSkeletonRangedEvents;
import cz.xefensor.retold.behavior.home.RetoldSmallForagerHomeEvents;
import cz.xefensor.retold.behavior.species.RetoldSnifferForagerEvents;
import cz.xefensor.retold.behavior.home.RetoldSoloOpportunistHomeEvents;
import cz.xefensor.retold.behavior.species.RetoldSwarmScavengerEvents;
import cz.xefensor.retold.behavior.species.RetoldTerritoryGuardEvents;
import cz.xefensor.retold.behavior.species.RetoldTurtleBeachEvents;
import cz.xefensor.retold.behavior.species.RetoldUndeadHordeEvents;
import cz.xefensor.retold.behavior.control.RetoldVanillaAiBlockerEvents;
import cz.xefensor.retold.behavior.species.RetoldZoglinRampagerEvents;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public final class RetoldBehaviorEntityTickDispatcher {
    private RetoldBehaviorEntityTickDispatcher() {
    }

    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();

        if (!(entity instanceof PathfinderMob mob)) {
            dispatchNonPathfinder(
                    event,
                    entity
            );
            return;
        }

        if (!(mob.level() instanceof ServerLevel level)) {
            return;
        }

        long gameTime = level.getGameTime();
        RetoldMobProfile profile = RetoldAiTickContext.profile(mob);

        if (shouldDispatch(mob, gameTime, 4)) {
            RetoldControlledFleeEvents.onEntityTickPost(event);
        }

        if (
                RetoldMobRules.canUseOrdinaryPredatorSystems(mob)
                        && shouldDispatch(mob, gameTime, 2)
        ) {
            RetoldVanillaAiBlockerEvents.onEntityTickPost(event);
        }

        if (RetoldMobRules.canUseOrdinaryLifeSystems(mob)) {
            if (shouldDispatch(mob, gameTime, 20)) {
                RetoldMobStateRecoveryEvents.onEntityTickPost(event);
                RetoldAnimalHomeRepairEvents.onEntityTickPost(event);
            }
        }

        switch (profile.type()) {
            case HUNGRY_GRAZER -> dispatchGrazer(event, mob, gameTime);
            case SMALL_FORAGER -> dispatchSmallForager(event, mob, gameTime);
            case PACK_PREDATOR -> dispatchPackPredator(event, mob, gameTime);
            case SOLO_OPPORTUNIST -> dispatchSoloOpportunist(event, mob, gameTime);
            case AQUATIC_PREDATOR -> dispatchAquaticPredator(event, mob, gameTime);
            case HUNGRY_SWARM_PREDATOR, SLIME_HUNGRY, SMALL_ARTHROPOD_SWARM -> dispatchSwarm(event, mob, gameTime);
            case HIVE_COLONY -> dispatchEvery(event, mob, gameTime, 6, RetoldHiveColonyEvents::onEntityTickPost);
            case NETHER_HUNGRY -> dispatchEvery(event, mob, gameTime, 7, RetoldNetherBehaviorEvents::onEntityTickPost);
            case UNDEAD_HUNGRY -> dispatchEvery(event, mob, gameTime, 6, RetoldUndeadHordeEvents::onEntityTickPost);
            case UNDEAD_TOLERANT -> dispatchEvery(event, mob, gameTime, 6, RetoldSkeletonRangedEvents::onEntityTickPost);
            case PHANTOM_STALKER -> dispatchEvery(event, mob, gameTime, 6, RetoldPhantomStalkerEvents::onEntityTickPost);
            case GHAST_ARTILLERY -> dispatchEvery(event, mob, gameTime, 8, RetoldGhastArtilleryEvents::onEntityTickPost);
            case ZOGLIN_RAMPAGER -> dispatchEvery(event, mob, gameTime, 5, RetoldZoglinRampagerEvents::onEntityTickPost);
            case PROTECTIVE_NEUTRAL -> dispatchEvery(event, mob, gameTime, 5, RetoldNeutralWildlifeEvents::onEntityTickPost);
            case PANDA_BAMBOO -> dispatchEvery(event, mob, gameTime, 10, RetoldPandaBambooEvents::onEntityTickPost);
            case SNIFFER_FORAGER -> dispatchEvery(event, mob, gameTime, 10, RetoldSnifferForagerEvents::onEntityTickPost);
            case ARMADILLO_DEFENSIVE -> dispatchEvery(event, mob, gameTime, 4, RetoldArmadilloDefenseEvents::onEntityTickPost);
            case TURTLE_BEACH -> dispatchEvery(event, mob, gameTime, 10, RetoldTurtleBeachEvents::onEntityTickPost);
            case AMPHIBIAN_FORAGER -> dispatchEvery(event, mob, gameTime, 6, RetoldAmphibianForagerEvents::onEntityTickPost);
            case AQUATIC_HELPER_PREDATOR -> dispatchEvery(event, mob, gameTime, 5, RetoldAxolotlHelperEvents::onEntityTickPost);
            case AQUATIC_TERRITORY_GUARD, TERRITORY_GUARD -> dispatchEvery(event, mob, gameTime, 10, RetoldTerritoryGuardEvents::onEntityTickPost);
            case COMMANDER_SUPPORT -> dispatchCommanderSupport(event, mob, gameTime);
            case ILLAGER_RAIDER -> dispatchIllagerRaider(event, mob, gameTime);
            case NONE, SPECIAL_VANILLA, APEX_OR_BOSS -> {
            }
        }
    }

    private static void dispatchNonPathfinder(
            EntityTickEvent.Post event,
            Entity entity
    ) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }

        long gameTime = level.getGameTime();

        if (shouldDispatch(entity, gameTime, 8)) {
            RetoldGhastArtilleryEvents.onEntityTickPost(event);
        }

        if (shouldDispatch(entity, gameTime, 6)) {
            RetoldPhantomStalkerEvents.onEntityTickPost(event);
        }
    }

    private static void dispatchGrazer(
            EntityTickEvent.Post event,
            PathfinderMob mob,
            long gameTime
    ) {
        dispatchEvery(event, mob, gameTime, 10, RetoldControlledRegroupEvents::onEntityTickPost);
        dispatchEvery(event, mob, gameTime, 20, RetoldHerdRangeEvents::onEntityTickPost);
    }

    private static void dispatchSmallForager(
            EntityTickEvent.Post event,
            PathfinderMob mob,
            long gameTime
    ) {
        dispatchEvery(event, mob, gameTime, 10, RetoldControlledRegroupEvents::onEntityTickPost);
        dispatchEvery(event, mob, gameTime, 20, RetoldSmallForagerHomeEvents::onEntityTickPost);
    }

    private static void dispatchPackPredator(
            EntityTickEvent.Post event,
            PathfinderMob mob,
            long gameTime
    ) {
        dispatchEvery(event, mob, gameTime, 10, RetoldPredatorSearchEvents::onEntityTickPost);
        dispatchEvery(event, mob, gameTime, 5, RetoldPackHuntingEvents::onEntityTickPost);
        dispatchEvery(event, mob, gameTime, 10, RetoldPackHomeEvents::onEntityTickPost);
    }

    private static void dispatchSoloOpportunist(
            EntityTickEvent.Post event,
            PathfinderMob mob,
            long gameTime
    ) {
        dispatchEvery(event, mob, gameTime, 10, RetoldPredatorSearchEvents::onEntityTickPost);
        dispatchEvery(event, mob, gameTime, 20, RetoldSoloOpportunistHomeEvents::onEntityTickPost);
        dispatchEvery(event, mob, gameTime, 5, RetoldHeldFoodConsumptionEvents::onEntityTickPost);
    }

    private static void dispatchAquaticPredator(
            EntityTickEvent.Post event,
            PathfinderMob mob,
            long gameTime
    ) {
        dispatchEvery(event, mob, gameTime, 10, RetoldPredatorSearchEvents::onEntityTickPost);
        dispatchEvery(event, mob, gameTime, 5, RetoldDolphinPodEvents::onEntityTickPost);
        dispatchEvery(event, mob, gameTime, 5, RetoldHeldFoodConsumptionEvents::onEntityTickPost);
    }

    private static void dispatchSwarm(
            EntityTickEvent.Post event,
            PathfinderMob mob,
            long gameTime
    ) {
        dispatchEvery(event, mob, gameTime, 10, RetoldPredatorSearchEvents::onEntityTickPost);
        dispatchEvery(event, mob, gameTime, 6, RetoldSwarmScavengerEvents::onEntityTickPost);
    }

    private static void dispatchCommanderSupport(
            EntityTickEvent.Post event,
            PathfinderMob mob,
            long gameTime
    ) {
        dispatchEvery(event, mob, gameTime, 6, RetoldCommanderSupportEvents::onEntityTickPost);
        dispatchEvery(event, mob, gameTime, 10, RetoldTerritoryGuardEvents::onEntityTickPost);
    }

    private static void dispatchIllagerRaider(
            EntityTickEvent.Post event,
            PathfinderMob mob,
            long gameTime
    ) {
        dispatchEvery(event, mob, gameTime, 10, RetoldIllagerRoamingEvents::onEntityTickPost);
        dispatchEvery(event, mob, gameTime, 10, RetoldTerritoryGuardEvents::onEntityTickPost);
    }

    private static void dispatchEvery(
            EntityTickEvent.Post event,
            Entity entity,
            long gameTime,
            int cadenceTicks,
            EntityTickHandler handler
    ) {
        if (shouldDispatch(entity, gameTime, cadenceTicks)) {
            handler.onEntityTickPost(event);
        }
    }

    private static boolean shouldDispatch(
            Entity entity,
            long gameTime,
            int cadenceTicks
    ) {
        int cadence = Math.max(1, cadenceTicks);

        return Math.floorMod(
                gameTime + entity.getId(),
                cadence
        ) == 0;
    }

    @FunctionalInterface
    private interface EntityTickHandler {
        void onEntityTickPost(EntityTickEvent.Post event);
    }
}
