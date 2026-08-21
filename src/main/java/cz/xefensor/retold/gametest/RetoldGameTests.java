package cz.xefensor.retold.gametest;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.aender.portal.RetoldAenderGameTests;
import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.control.RetoldAiControlMode;
import cz.xefensor.retold.behavior.control.RetoldAiControlOwner;
import cz.xefensor.retold.behavior.control.RetoldControlledCombatEvents;
import cz.xefensor.retold.behavior.control.RetoldTamedDefenderGameTests;
import cz.xefensor.retold.behavior.breeding.RetoldAnimalBreedingGameTests;
import cz.xefensor.retold.behavior.core.RetoldBehaviorMovement;
import cz.xefensor.retold.behavior.ecology.RetoldUnloadedEcosystemGameTests;
import cz.xefensor.retold.behavior.flee.RetoldCreeperAwareness;
import cz.xefensor.retold.behavior.flee.RetoldDamageFleeGameTests;
import cz.xefensor.retold.behavior.food.RetoldFoodBehaviorEvents;
import cz.xefensor.retold.behavior.food.RetoldFeedingPose;
import cz.xefensor.retold.behavior.food.RetoldAnimalFeederGameTests;
import cz.xefensor.retold.behavior.food.RetoldFoodSearchGameTests;
import cz.xefensor.retold.behavior.food.RetoldHungerSurvivalGameTests;
import cz.xefensor.retold.behavior.food.RetoldNaturalFoodAcquisitionGameTests;
import cz.xefensor.retold.behavior.food.RetoldStarvationGameTests;
import cz.xefensor.retold.behavior.food.RetoldWeakBarrierGameTests;
import cz.xefensor.retold.behavior.performance.RetoldAiSightCacheGameTests;
import cz.xefensor.retold.behavior.performance.RetoldAiPerformanceGameTests;
import cz.xefensor.retold.behavior.performance.RetoldPerMobTpsGameTests;
import cz.xefensor.retold.behavior.pack.RetoldWolfPackHungerGameTests;
import cz.xefensor.retold.behavior.profiles.RetoldMobProfile;
import cz.xefensor.retold.behavior.profiles.RetoldMobProfileType;
import cz.xefensor.retold.behavior.profiles.RetoldMobProfiles;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;
import cz.xefensor.retold.behavior.profiles.RetoldMobState;
import cz.xefensor.retold.behavior.profiles.RetoldMobStates;
import cz.xefensor.retold.behavior.species.RetoldCommanderSupportGameTests;
import cz.xefensor.retold.behavior.species.RetoldAxolotlGuardianGameTests;
import cz.xefensor.retold.behavior.species.RetoldAquaticEcologyGameTests;
import cz.xefensor.retold.behavior.species.RetoldBatColonyGameTests;
import cz.xefensor.retold.behavior.species.RetoldDolphinPodGameTests;
import cz.xefensor.retold.behavior.species.RetoldHerdSchoolGameTests;
import cz.xefensor.retold.behavior.species.RetoldHiveColonyGameTests;
import cz.xefensor.retold.behavior.species.RetoldUndeadTargetParityGameTests;
import cz.xefensor.retold.behavior.species.RetoldUndeadMountGameTests;
import cz.xefensor.retold.behavior.species.RetoldWitherThreatGameTests;
import cz.xefensor.retold.behavior.species.RetoldPandaBambooGameTests;
import cz.xefensor.retold.behavior.species.RetoldParrotForagerGameTests;
import cz.xefensor.retold.behavior.species.RetoldPhantomStalkerGameTests;
import cz.xefensor.retold.behavior.species.RetoldPolarBearWarningGameTests;
import cz.xefensor.retold.behavior.species.RetoldSpiderEcologyGameTests;
import cz.xefensor.retold.behavior.species.RetoldSpiderLairGameTests;
import cz.xefensor.retold.behavior.species.RetoldSlimeMergeGameTests;
import cz.xefensor.retold.behavior.species.RetoldSwarmScavengerEvents;
import cz.xefensor.retold.combat.RetoldFactionTargetMemory;
import cz.xefensor.retold.combat.RetoldCombatTargets;
import cz.xefensor.retold.combat.RetoldTargetSource;
import cz.xefensor.retold.compatibility.RetoldWorldProtectionGameTests;
import cz.xefensor.retold.enderman.RetoldEndermanDefense;
import cz.xefensor.retold.enchanting.RetoldEnchantingGameTests;
import cz.xefensor.retold.faction.RetoldFaction;
import cz.xefensor.retold.faction.RetoldFactionMembers;
import cz.xefensor.retold.faction.RetoldFactionRelations;
import cz.xefensor.retold.event.RetoldPlayerSyncEvents;
import cz.xefensor.retold.event.RetoldElderGuardianEvents;
import cz.xefensor.retold.event.RetoldEndProgressionEvents;
import cz.xefensor.retold.event.RetoldSnowballGameTests;
import cz.xefensor.retold.event.RetoldVexGameTests;
import cz.xefensor.retold.progression.RetoldToolProgressionGameTests;
import cz.xefensor.retold.recipe.RetoldRecipeCompatibilityGameTests;
import cz.xefensor.retold.progression.RetoldProgressionAcquisitionGameTests;
import cz.xefensor.retold.registry.RetoldBlocks;
import cz.xefensor.retold.registry.RetoldEntityTypes;
import cz.xefensor.retold.registry.RetoldTags;
import cz.xefensor.retold.stage.RetoldRitualOffering;
import cz.xefensor.retold.stage.RetoldRaidProgression;
import cz.xefensor.retold.stage.RetoldStageManager;
import cz.xefensor.retold.stage.RetoldStageRuntime;
import cz.xefensor.retold.stage.RetoldWorldData;
import cz.xefensor.retold.stage.RetoldWorldStage;
import cz.xefensor.retold.territory.RetoldTerritoryGameTests;
import cz.xefensor.retold.villager.RetoldVillagerTradeRefreshGameTests;
import cz.xefensor.retold.villager.RetoldVillagerTeachingGameTests;
import cz.xefensor.retold.villager.RetoldVillagerCommunalFoodGameTests;
import cz.xefensor.retold.villager.RetoldVillagerGolemConstructionGameTests;
import cz.xefensor.retold.villager.RetoldVillagerTorchRelightingGameTests;
import cz.xefensor.retold.villager.RetoldVillageContainerOwnershipGameTests;
import cz.xefensor.retold.villager.RetoldVillageCropReputationGameTests;
import cz.xefensor.retold.villager.RetoldVillageAnimalReputationGameTests;
import cz.xefensor.retold.worldgen.RetoldNetherMobSpawnGameTests;
import cz.xefensor.retold.worldgen.RetoldRuinedPortalGameTests;
import cz.xefensor.retold.worldgen.RetoldStructureRemovalGameTests;
import cz.xefensor.retold.worldgen.air.RetoldAirTempleDiscoveryGameTests;
import cz.xefensor.retold.worldgen.air.RetoldGaleCoreGameTests;
import cz.xefensor.retold.worldgen.fire.RetoldWildfireGameTests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.BuiltinTestFunctions;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class RetoldGameTests {
    private static final Identifier EMPTY_STRUCTURE =
            Identifier.withDefaultNamespace("empty");

    private RetoldGameTests() {
    }

    public static void register(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment =
                event.registerEnvironment(
                        id("default"),
                        new TestEnvironmentDefinition.AllOf()
                );

        registerTest(
                event,
                environment,
                "stage_transition_updates_saved_and_runtime_state",
                RetoldGameTests::stageTransitionUpdatesSavedAndRuntimeState
        );
        registerTest(
                event,
                environment,
                "player_sync_skips_clientless_gametest_players",
                RetoldGameTests::playerSyncSkipsClientlessGameTestPlayers
        );
        registerTest(
                event,
                environment,
                "raids_cannot_start_before_stage_three",
                RetoldGameTests::raidsCannotStartBeforeStageThree
        );
        registerTest(
                event,
                environment,
                "world_data_tracks_ritual_progress",
                RetoldGameTests::worldDataTracksRitualProgress
        );
        registerTest(
                event,
                environment,
                "dragon_egg_accepts_final_and_legacy_offerings",
                RetoldGameTests::dragonEggAcceptsFinalAndLegacyOfferings
        );
        registerTest(
                event,
                environment,
                "elder_guardian_drops_heart_of_the_sea",
                RetoldGameTests::elderGuardianDropsHeartOfTheSea
        );
        registerTest(
                event,
                environment,
                "mob_profiles_load_from_datapack",
                RetoldGameTests::mobProfilesLoadFromDatapack
        );
        registerTest(
                event,
                environment,
                "spider_retaliates_against_attacker",
                RetoldGameTests::spiderRetaliatesAgainstAttacker
        );
        registerTest(
                event,
                environment,
                "spider_targets_player_in_darkness",
                RetoldGameTests::spiderTargetsPlayerInDarkness
        );
        registerTest(
                event,
                environment,
                "guardian_ignores_non_player_damage_for_defense_assist",
                RetoldGameTests::guardianIgnoresNonPlayerDamageForDefenseAssist
        );
        registerTest(
                event,
                environment,
                "mobs_cannot_target_or_melee_creepers",
                RetoldGameTests::mobsCannotTargetOrMeleeCreepers
        );
        registerTest(
                event,
                environment,
                "indiscriminate_factions_follow_living_target_rules",
                RetoldGameTests::indiscriminateFactionsFollowLivingTargetRules
        );
        registerTest(
                event,
                environment,
                "faction_tags_preserve_defaults_and_standard_undead",
                RetoldGameTests::factionTagsPreserveDefaultsAndStandardUndead
        );
        registerTest(
                event,
                environment,
                "faction_tags_drive_targeting_and_retaliation",
                RetoldGameTests::factionTagsDriveTargetingAndRetaliation
        );
        registerTest(
                event,
                environment,
                "silverfish_and_endermites_are_unrelated",
                RetoldGameTests::silverfishAndEndermitesAreUnrelated
        );
        registerTest(
                event,
                environment,
                "endermen_only_coordinate_shared_defense_in_stage_3",
                RetoldGameTests::endermenOnlyCoordinateSharedDefenseInStage3
        );
        registerTest(
                event,
                environment,
                "ignited_creeper_causes_delayed_flight_except_zombies",
                RetoldGameTests::ignitedCreeperCausesDelayedFlightExceptZombies
        );
        registerTest(
                event,
                environment,
                "cats_retreat_from_unignited_creepers",
                RetoldGameTests::catsRetreatFromUnignitedCreepers
        );
        registerTest(
                event,
                environment,
                "animal_feeding_respects_mob_griefing",
                RetoldGameTests::animalFeedingRespectsMobGriefing
        );
        registerTest(
                event,
                environment,
                "dropped_food_interrupts_hunting_but_not_retaliation",
                RetoldGameTests::droppedFoodInterruptsHuntingButNotRetaliation
        );
        registerTest(
                event,
                environment,
                "cube_mob_hops_toward_dropped_item",
                RetoldGameTests::cubeMobHopsTowardDroppedItem
        );
        registerTest(
                event,
                environment,
                "creeper_explosion_respects_mob_griefing",
                RetoldGameTests::creeperExplosionRespectsMobGriefing
        );
        registerTest(
                event,
                environment,
                "compatibility_block_tags_preserve_existing_defaults",
                RetoldGameTests::compatibilityBlockTagsPreserveExistingDefaults
        );
        registerTest(
                event,
                environment,
                "compatibility_item_tags_preserve_existing_defaults",
                RetoldGameTests::compatibilityItemTagsPreserveExistingDefaults
        );
        registerTest(
                event,
                environment,
                "compatibility_food_and_forage_preserve_existing_behavior",
                RetoldGameTests::compatibilityFoodAndForagePreserveExistingBehavior
        );
        registerTest(
                event,
                environment,
                "extinguished_torches_drop_matching_lit_items",
                RetoldGameTests::extinguishedTorchesDropMatchingLitItems
        );

        RetoldAenderGameTests.register(event, environment);
        RetoldWorldProtectionGameTests.register(event, environment);
        RetoldMobAvailabilityGameTests.register(event, environment);
        RetoldNetherMobSpawnGameTests.register(event);
        RetoldAiPerformanceGameTests.register(event);
        RetoldPerMobTpsGameTests.register(event);
        RetoldAiSightCacheGameTests.register(event, environment);
        RetoldDamageFleeGameTests.register(event, environment);
        RetoldWeakBarrierGameTests.register(event, environment);
        RetoldFoodSearchGameTests.register(event);
        RetoldAnimalFeederGameTests.register(event);
        RetoldAnimalBreedingGameTests.register(event);
        RetoldStarvationGameTests.register(event);
        RetoldUnloadedEcosystemGameTests.register(event);
        RetoldHungerSurvivalGameTests.register(event);
        RetoldNaturalFoodAcquisitionGameTests.register(event);
        RetoldCommanderSupportGameTests.register(event, environment);
        RetoldAquaticEcologyGameTests.register(event);
        RetoldAxolotlGuardianGameTests.register(event);
        RetoldBatColonyGameTests.register(event);
        RetoldDolphinPodGameTests.register(event);
        RetoldHerdSchoolGameTests.register(event);
        RetoldHiveColonyGameTests.register(event);
        RetoldUndeadTargetParityGameTests.register(event);
        RetoldUndeadMountGameTests.register(event);
        RetoldWitherThreatGameTests.register(event);
        RetoldPandaBambooGameTests.register(event);
        RetoldParrotForagerGameTests.register(event);
        RetoldPhantomStalkerGameTests.register(event);
        RetoldPolarBearWarningGameTests.register(event, environment);
        RetoldWolfPackHungerGameTests.register(event, environment);
        RetoldTamedDefenderGameTests.register(event, environment);
        RetoldSpiderEcologyGameTests.register(event);
        RetoldSpiderLairGameTests.register(event);
        RetoldSlimeMergeGameTests.register(event);
        RetoldVexGameTests.register(event, environment);
        RetoldSnowballGameTests.register(event, environment);
        RetoldEnchantingGameTests.register(event, environment);
        RetoldToolProgressionGameTests.register(event, environment);
        RetoldProgressionAcquisitionGameTests.register(event, environment);
        RetoldRecipeCompatibilityGameTests.register(event, environment);
        RetoldTerritoryGameTests.register(event, environment);
        RetoldVillagerCommunalFoodGameTests.register(event);
        RetoldVillagerGolemConstructionGameTests.register(event);
        RetoldVillagerTorchRelightingGameTests.register(event);
        RetoldVillageContainerOwnershipGameTests.register(event);
        RetoldVillageCropReputationGameTests.register(event);
        RetoldVillageAnimalReputationGameTests.register(event);
        RetoldVillagerTradeRefreshGameTests.register(event, environment);
        RetoldVillagerTeachingGameTests.register(event, environment);
        RetoldAirTempleDiscoveryGameTests.register(event, environment);
        RetoldGaleCoreGameTests.register(event, environment);
        RetoldWildfireGameTests.register(event, environment);
        RetoldRuinedPortalGameTests.register(event, environment);
        RetoldStructureRemovalGameTests.register(event, environment);
    }

    private static void registerTest(
            RegisterGameTestsEvent event,
            Holder<TestEnvironmentDefinition<?>> environment,
            String name,
            Consumer<GameTestHelper> test
    ) {
        TestData<Holder<TestEnvironmentDefinition<?>>> testData =
                new TestData<>(
                        environment,
                        EMPTY_STRUCTURE,
                        40,
                        0,
                        true
                );

        event.registerTest(id(name), new InlineGameTest(testData, test));
    }

    private static void stageTransitionUpdatesSavedAndRuntimeState(
            GameTestHelper helper
    ) {
        ServerLevel level = helper.getLevel();
        RetoldWorldData data = RetoldWorldData.get(level);
        RetoldWorldStage originalSavedStage = data.getStage();
        RetoldWorldStage originalRuntimeStage =
                RetoldStageRuntime.getOverworldStage();

        try {
            data.setStage(RetoldWorldStage.STAGE_1);
            RetoldStageRuntime.setOverworldStage(RetoldWorldStage.STAGE_1);

            helper.assertTrue(
                    RetoldStageManager.setStage(
                            level,
                            RetoldWorldStage.STAGE_2
                    ),
                    "A changed stage must report a transition"
            );
            helper.assertValueEqual(
                    data.getStage(),
                    RetoldWorldStage.STAGE_2,
                    "The new stage must be stored in world data"
            );
            helper.assertValueEqual(
                    RetoldStageRuntime.getOverworldStage(),
                    RetoldWorldStage.STAGE_2,
                    "The runtime stage must follow world data"
            );
            helper.assertFalse(
                    RetoldStageManager.setStage(
                            level,
                            RetoldWorldStage.STAGE_2
                    ),
                    "Setting the current stage must be a no-op"
            );

            helper.succeed();
        } finally {
            data.setStage(originalSavedStage);
            RetoldStageRuntime.setOverworldStage(originalRuntimeStage);
        }
    }

    private static void playerSyncSkipsClientlessGameTestPlayers(
            GameTestHelper helper
    ) {
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(
                GameType.SURVIVAL
        );

        try {
            helper.assertTrue(
                    player.connection == null,
                    "The regression fixture must remain a clientless GameTest player"
            );
            RetoldPlayerSyncEvents.onPlayerLoggedIn(
                    new PlayerEvent.PlayerLoggedInEvent(player)
            );
            helper.succeed();
        } finally {
            player.discard();
        }
    }

    private static void raidsCannotStartBeforeStageThree(
            GameTestHelper helper
    ) {
        ServerLevel level = helper.getLevel();
        RetoldWorldData data = RetoldWorldData.get(level);
        RetoldWorldStage originalStage = data.getStage();
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(
                GameType.SURVIVAL
        );

        try {
            data.setStage(RetoldWorldStage.STAGE_2);

            helper.assertFalse(
                    RetoldRaidProgression.canStartRaid(level),
                    "The authoritative world stage must reject raids before Stage 3"
            );
            helper.assertTrue(
                    level.getRaids().createOrExtendRaid(
                            player,
                            helper.absolutePos(BlockPos.ZERO)
                    ) == null,
                    "The vanilla raid creation path must be blocked before Stage 3"
            );
            data.setStage(RetoldWorldStage.STAGE_3);
            helper.assertTrue(
                    RetoldRaidProgression.canStartRaid(level),
                    "Stage 3 must allow the vanilla raid creation path to proceed"
            );

            helper.succeed();
        } finally {
            player.discard();
            data.setStage(originalStage);
        }
    }

    private static void worldDataTracksRitualProgress(GameTestHelper helper) {
        RetoldWorldData data = RetoldWorldData.get(helper.getLevel());
        EnumSet<RetoldRitualOffering> originalOfferings =
                EnumSet.noneOf(RetoldRitualOffering.class);
        for (RetoldRitualOffering offering : RetoldRitualOffering.values()) {
            if (data.hasOffering(offering)) {
                originalOfferings.add(offering);
            }
        }
        BlockPos originalEggPos = data.getDragonEggPos();

        try {
            data.clearOfferings();
            data.clearDragonEggPos();

            helper.assertValueEqual(
                    RetoldRitualOffering.WATER.mask(),
                    1,
                    "Water must retain its original saved bit"
            );
            helper.assertValueEqual(
                    RetoldRitualOffering.FIRE.mask(),
                    2,
                    "Fire must retain its original saved bit"
            );
            helper.assertValueEqual(
                    RetoldRitualOffering.EARTH.mask(),
                    4,
                    "Earth must retain its original saved bit"
            );
            helper.assertValueEqual(
                    RetoldRitualOffering.AIR.mask(),
                    8,
                    "Air must retain its original saved bit"
            );

            helper.assertTrue(
                    data.offer(RetoldRitualOffering.FIRE),
                    "A new ritual offering must be accepted"
            );
            helper.assertFalse(
                    data.offer(RetoldRitualOffering.FIRE),
                    "A duplicate ritual offering must be rejected"
            );
            data.offer(RetoldRitualOffering.EARTH);
            data.offer(RetoldRitualOffering.LIFE);
            data.offer(RetoldRitualOffering.DEATH);
            helper.assertValueEqual(
                    data.offeredOfferingCount(),
                    4,
                    "Fire, Earth, Life, and Death must retain distinct save bits"
            );
            helper.assertValueEqual(
                    data.offeredRequiredOfferingCount(),
                    2,
                    "Life and Death must count toward the temporary hatch threshold"
            );
            helper.assertValueEqual(
                    data.requiredOfferingCount(),
                    4,
                    "The temporary hatch threshold must require four offerings"
            );
            helper.assertFalse(
                    data.hasAllRequiredOfferings(),
                    "Life and Death alone must not complete the current ritual"
            );

            data.offer(RetoldRitualOffering.AIR);
            helper.assertFalse(
                    data.hasAllRequiredOfferings(),
                    "Air alone must not complete the current ritual"
            );
            data.offer(RetoldRitualOffering.WATER);
            helper.assertTrue(
                    data.hasAllRequiredOfferings(),
                    "Water, Air, Life, and Death must complete the current ritual"
            );
            helper.assertValueEqual(
                    data.offeredRequiredOfferingCount(),
                    data.requiredOfferingCount(),
                    "All currently required offerings must be counted"
            );
            helper.assertValueEqual(
                    data.offeredOfferingCount(),
                    RetoldRitualOffering.values().length,
                    "The saved ritual model must represent all six offerings"
            );

            BlockPos.MutableBlockPos mutableEggPos =
                    new BlockPos.MutableBlockPos(3, 5, 7);
            data.setDragonEggPos(mutableEggPos);
            mutableEggPos.set(9, 9, 9);
            helper.assertValueEqual(
                    data.getDragonEggPos(),
                    new BlockPos(3, 5, 7),
                    "World data must keep an immutable egg position"
            );

            helper.succeed();
        } finally {
            data.clearOfferings();
            for (RetoldRitualOffering offering : originalOfferings) {
                data.offer(offering);
            }

            if (originalEggPos == null) {
                data.clearDragonEggPos();
            } else {
                data.setDragonEggPos(originalEggPos);
            }
        }
    }

    private static void dragonEggAcceptsFinalAndLegacyOfferings(
            GameTestHelper helper
    ) {
        ServerLevel level = helper.getLevel();
        RetoldWorldData data = RetoldWorldData.get(level);
        RetoldWorldStage originalStage = data.getStage();
        EnumSet<RetoldRitualOffering> originalOfferings =
                EnumSet.noneOf(RetoldRitualOffering.class);
        for (RetoldRitualOffering offering : RetoldRitualOffering.values()) {
            if (data.hasOffering(offering)) {
                originalOfferings.add(offering);
            }
        }
        BlockPos originalEggPos = data.getDragonEggPos();
        BlockPos relativeEggPos = new BlockPos(1, 2, 1);
        BlockPos eggPos = helper.absolutePos(relativeEggPos);
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(
                GameType.SURVIVAL
        );

        try {
            data.clearOfferings();
            data.clearDragonEggPos();
            data.setStage(RetoldWorldStage.STAGE_2);
            helper.setBlock(relativeEggPos, Blocks.DRAGON_EGG);

            ItemStack heavyCore = new ItemStack(Items.HEAVY_CORE);
            PlayerInteractEvent.RightClickBlock heavyCoreUse = useOnEgg(
                    player,
                    heavyCore,
                    eggPos
            );
            RetoldEndProgressionEvents.onDragonEggRightClick(heavyCoreUse);
            helper.assertTrue(
                    heavyCoreUse.isCanceled()
                            && heavyCoreUse.getCancellationResult()
                            == InteractionResult.SUCCESS,
                    "A Heavy Core must be accepted as the Air offering"
            );
            helper.assertTrue(
                    heavyCore.isEmpty()
                            && data.hasOffering(RetoldRitualOffering.AIR),
                    "The accepted Heavy Core must be consumed and record Air"
            );

            ItemStack legacyAir = RetoldBlocks.AIR_ELEMENT.toStack();
            PlayerInteractEvent.RightClickBlock duplicateAirUse = useOnEgg(
                    player,
                    legacyAir,
                    eggPos
            );
            RetoldEndProgressionEvents.onDragonEggRightClick(duplicateAirUse);
            helper.assertTrue(
                    duplicateAirUse.getCancellationResult()
                            == InteractionResult.FAIL
                            && legacyAir.getCount() == 1,
                    "A legacy Air Element must map to Air without consuming a duplicate"
            );

            data.clearOfferings();
            data.clearDragonEggPos();
            ItemStack heart = new ItemStack(Items.HEART_OF_THE_SEA);
            PlayerInteractEvent.RightClickBlock heartUse = useOnEgg(
                    player,
                    heart,
                    eggPos
            );
            RetoldEndProgressionEvents.onDragonEggRightClick(heartUse);
            helper.assertTrue(
                    heart.isEmpty()
                            && data.hasOffering(RetoldRitualOffering.WATER),
                    "A Heart of the Sea must be consumed and record Water"
            );

            ItemStack legacyWater = RetoldBlocks.WATER_ELEMENT.toStack();
            PlayerInteractEvent.RightClickBlock duplicateWaterUse = useOnEgg(
                    player,
                    legacyWater,
                    eggPos
            );
            RetoldEndProgressionEvents.onDragonEggRightClick(
                    duplicateWaterUse
            );
            helper.assertTrue(
                    duplicateWaterUse.getCancellationResult()
                            == InteractionResult.FAIL
                            && legacyWater.getCount() == 1,
                    "A legacy Water Element must map to Water without consuming a duplicate"
            );
            helper.assertTrue(
                    data.getStage() == RetoldWorldStage.STAGE_2
                            && level.getBlockState(eggPos).is(Blocks.DRAGON_EGG),
                    "One current offering must not hatch the egg"
            );

            data.clearOfferings();
            data.clearDragonEggPos();
            ItemStack totem = new ItemStack(Items.TOTEM_OF_UNDYING);
            PlayerInteractEvent.RightClickBlock totemUse = useOnEgg(
                    player,
                    totem,
                    eggPos
            );
            RetoldEndProgressionEvents.onDragonEggRightClick(totemUse);
            helper.assertTrue(
                    totem.isEmpty()
                            && data.hasOffering(RetoldRitualOffering.LIFE),
                    "A Totem of Undying must be consumed and record Life"
            );

            ItemStack netherStar = new ItemStack(Items.NETHER_STAR);
            PlayerInteractEvent.RightClickBlock netherStarUse = useOnEgg(
                    player,
                    netherStar,
                    eggPos
            );
            RetoldEndProgressionEvents.onDragonEggRightClick(netherStarUse);
            helper.assertTrue(
                    netherStar.isEmpty()
                            && data.hasOffering(RetoldRitualOffering.DEATH),
                    "A Nether Star must be consumed and record Death"
            );
            helper.assertTrue(
                    data.getStage() == RetoldWorldStage.STAGE_2
                            && level.getBlockState(eggPos).is(Blocks.DRAGON_EGG),
                    "Life and Death without Air and Water must not hatch the egg"
            );

            ItemStack reactorCore = RetoldBlocks.NETHER_REACTOR_CORE.toStack();
            PlayerInteractEvent.RightClickBlock reactorCoreUse = useOnEgg(
                    player,
                    reactorCore,
                    eggPos
            );
            RetoldEndProgressionEvents.onDragonEggRightClick(reactorCoreUse);
            helper.assertTrue(
                    reactorCore.isEmpty()
                            && data.hasOffering(RetoldRitualOffering.FIRE),
                    "A Nether Reactor Core must be consumed and record Fire"
            );
            helper.assertTrue(
                    data.getStage() == RetoldWorldStage.STAGE_2
                            && level.getBlockState(eggPos).is(Blocks.DRAGON_EGG),
                    "Fire must remain outside the hatch threshold until Earth exists"
            );
            helper.succeed();
        } finally {
            player.discard();
            helper.setBlock(relativeEggPos, Blocks.AIR);
            data.clearOfferings();
            for (RetoldRitualOffering offering : originalOfferings) {
                data.offer(offering);
            }
            if (originalEggPos == null) {
                data.clearDragonEggPos();
            } else {
                data.setDragonEggPos(originalEggPos);
            }
            data.setStage(originalStage);
        }
    }

    private static PlayerInteractEvent.RightClickBlock useOnEgg(
            ServerPlayer player,
            ItemStack stack,
            BlockPos eggPos
    ) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return new PlayerInteractEvent.RightClickBlock(
                player,
                InteractionHand.MAIN_HAND,
                eggPos,
                new BlockHitResult(
                        Vec3.atCenterOf(eggPos),
                        Direction.UP,
                        eggPos,
                        false
                )
        );
    }

    private static void elderGuardianDropsHeartOfTheSea(
            GameTestHelper helper
    ) {
        ElderGuardian guardian = helper.spawn(
                EntityTypes.ELDER_GUARDIAN,
                1,
                2,
                1
        );
        Collection<ItemEntity> drops = new ArrayList<>();
        LivingDropsEvent dropsEvent = new LivingDropsEvent(
                guardian,
                helper.getLevel().damageSources().generic(),
                drops,
                true
        );

        try {
            RetoldElderGuardianEvents.onLivingDrops(dropsEvent);
            RetoldElderGuardianEvents.onLivingDrops(dropsEvent);
            helper.assertValueEqual(
                    drops.stream()
                            .filter(drop -> drop.getItem().is(
                                    Items.HEART_OF_THE_SEA
                            ))
                            .count(),
                    1L,
                    "An Elder Guardian must guarantee exactly one Heart of the Sea"
            );
            helper.assertFalse(
                    drops.stream().anyMatch(drop -> drop.getItem().is(
                            RetoldBlocks.WATER_ELEMENT
                    )),
                    "An Elder Guardian must no longer drop the legacy Water Element"
            );
            helper.succeed();
        } finally {
            guardian.discard();
        }
    }

    private static void mobProfilesLoadFromDatapack(GameTestHelper helper) {
        helper.assertValueEqual(
                RetoldMobProfiles.loadedProfileCount(),
                83,
                "Every bundled mob profile must load"
        );

        RetoldMobProfile wildfire = RetoldMobProfiles.get(
                RetoldEntityTypes.WILDFIRE.get()
        );
        helper.assertValueEqual(
                wildfire.type(),
                RetoldMobProfileType.SPECIAL_VANILLA,
                "Wildfire must preserve Blaze movement and combat through its special profile"
        );

        RetoldMobProfile wolf = RetoldMobProfiles.get("minecraft:wolf");
        helper.assertValueEqual(
                wolf.type(),
                RetoldMobProfileType.PACK_PREDATOR,
                "Wolf must use its datapack profile"
        );
        helper.assertTrue(wolf.managed(), "Wolf profile must remain managed");
        helper.assertTrue(wolf.predator(), "Wolf profile must remain predatory");
        helper.assertTrue(wolf.packSocial(), "Wolf profile must remain pack-social");
        helper.assertTrue(
                RetoldMobProfiles.get(EntityTypes.WOLF) == wolf,
                "Entity-type profile lookup must use the loaded direct index"
        );
        helper.assertValueEqual(
                wolf.hungerIntervalTicks(),
                460,
                "Wolf hunger timing must preserve the previous balance"
        );

        RetoldMobProfile skeleton = RetoldMobProfiles.get("skeleton");
        helper.assertValueEqual(
                skeleton.type(),
                RetoldMobProfileType.UNDEAD_TOLERANT,
                "Unqualified lookups must default to the Minecraft namespace"
        );
        helper.assertValueEqual(
                skeleton.eatThreshold(),
                101,
                "Disabled hunger thresholds must survive data loading"
        );

        RetoldMobProfile bat = RetoldMobProfiles.get("bat");
        helper.assertValueEqual(
                bat.type(),
                RetoldMobProfileType.BAT_COLONY,
                "Bat must use its colony ecology profile"
        );
        helper.assertTrue(
                bat.managed() && bat.predator() && bat.packSocial(),
                "Bat colony behavior must retain managed hunger, hunting, and social flags"
        );

        RetoldMobProfile cod = RetoldMobProfiles.get("cod");
        helper.assertValueEqual(
                cod.type(),
                RetoldMobProfileType.AQUATIC_SCHOOL,
                "Cod must use the species-specific aquatic school profile"
        );
        RetoldMobProfile squid = RetoldMobProfiles.get("squid");
        helper.assertValueEqual(
                squid.type(),
                RetoldMobProfileType.LOOSE_AQUATIC_GROUP,
                "Squid must use the loose same-species aquatic group profile"
        );

        RetoldMobProfile villager = RetoldMobProfiles.get("villager");
        helper.assertValueEqual(
                villager.type(),
                RetoldMobProfileType.VILLAGER_COMMUNAL,
                "Villagers must use their communal food society profile"
        );

        helper.assertValueEqual(
                RetoldMobProfiles.get("minecraft:not_a_real_mob").type(),
                RetoldMobProfileType.NONE,
                "Unknown entities must retain the safe fallback profile"
        );
        helper.succeed();
    }

    private static void spiderRetaliatesAgainstAttacker(GameTestHelper helper) {
        Spider spider = helper.spawn(EntityTypes.SPIDER, 1, 2, 1);
        Zombie attacker = helper.spawn(EntityTypes.ZOMBIE, 3, 2, 1);

        spider.setLastHurtByMob(attacker);

        helper.succeedWhen(() -> {
            helper.assertTrue(
                    spider.getTarget() == attacker,
                    "A managed spider must target the entity that attacked it"
            );
            helper.assertTrue(
                    RetoldAiControl.isControlledAs(
                            spider,
                            RetoldAiControlMode.ATTACK
                    ),
                    "Spider retaliation must own ATTACK control"
            );
            helper.assertTrue(
                    RetoldFactionTargetMemory.isOwnedByAny(
                            spider,
                            attacker,
                            RetoldTargetSource.RETALIATION
                    ),
                    "Spider retaliation must use the RETALIATION target source"
            );
        });
    }

    private static void spiderTargetsPlayerInDarkness(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        buildDarkSpiderRoom(helper);

        Spider spider = spawnSightedTestSpider(helper);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Vec3 playerPosition = helper.absoluteVec(new Vec3(3.5D, 1.0D, 1.5D));

        player.snapTo(
                playerPosition.x(),
                playerPosition.y(),
                playerPosition.z(),
                0.0F,
                0.0F
        );
        level.addFreshEntity(player);

        helper.succeedWhen(() -> {
            helper.assertTrue(
                    spider.getLightLevelDependentMagicValue() < 0.5F,
                    "The spider player-target test must run in vanilla attack darkness"
            );

            RetoldControlledCombatEvents.tickControlledCombat(
                    level,
                    spider,
                    level.getGameTime()
            );

            helper.assertTrue(
                    spider.getTarget() == player,
                    "A managed spider must acquire a survival player in darkness"
            );
            helper.assertTrue(
                    RetoldAiControl.isControlledAs(
                            spider,
                            RetoldAiControlMode.ATTACK
                    ),
                    "Darkness-based player aggression must own ATTACK control"
            );
            helper.assertTrue(
                    RetoldFactionTargetMemory.isOwnedByAny(
                            spider,
                            player,
                            RetoldTargetSource.BEHAVIOR_COMBAT
                    ),
                    "Darkness-based player aggression must use behavior combat ownership"
            );
            player.discard();
        });
    }

    private static void guardianIgnoresNonPlayerDamageForDefenseAssist(
            GameTestHelper helper
    ) {
        ServerLevel level = helper.getLevel();
        Guardian guardian = helper.spawn(EntityTypes.GUARDIAN, 1, 2, 1);
        Drowned drowned = helper.spawn(EntityTypes.DROWNED, 3, 2, 1);
        float healthBeforeDamage = guardian.getHealth();

        guardian.hurtServer(
                level,
                guardian.damageSources().mobAttack(drowned),
                1.0F
        );

        helper.assertTrue(
                guardian.getHealth() < healthBeforeDamage,
                "Non-player damage must pass without crashing guardian defense assist"
        );
        helper.succeed();
    }

    private static void mobsCannotTargetOrMeleeCreepers(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Zombie zombie = helper.spawn(EntityTypes.ZOMBIE, 1, 2, 1);
        var creeper = helper.spawn(EntityTypes.CREEPER, 3, 2, 1);
        float healthBeforeAttack = creeper.getHealth();

        zombie.setTarget(creeper);
        helper.assertTrue(
                zombie.getTarget() == null,
                "Vanilla target assignment must not let a mob target a creeper"
        );

        helper.assertFalse(
                RetoldCombatTargets.applyAttackTarget(
                        zombie,
                        creeper,
                        RetoldTargetSource.FACTION_COMBAT
                ),
                "Retold-owned target assignment must not let a mob target a creeper"
        );
        helper.assertFalse(
                zombie.doHurtTarget(level, creeper),
                "Direct mob melee must not damage a creeper"
        );
        helper.assertValueEqual(
                creeper.getHealth(),
                healthBeforeAttack,
                "Blocked mob melee must leave creeper health unchanged"
        );
        helper.succeed();
    }

    private static void indiscriminateFactionsFollowLivingTargetRules(
            GameTestHelper helper
    ) {
        Zombie zombie = helper.spawn(EntityTypes.ZOMBIE, 1, 2, 1);
        var wither = helper.spawn(EntityTypes.WITHER, 2, 2, 1);
        var slime = helper.spawn(EntityTypes.SLIME, 3, 2, 1);
        var magmaCube = helper.spawn(EntityTypes.MAGMA_CUBE, 4, 2, 1);
        Guardian guardian = helper.spawn(EntityTypes.GUARDIAN, 1, 2, 3);
        var elderGuardian = helper.spawn(EntityTypes.ELDER_GUARDIAN, 2, 2, 3);
        var cow = helper.spawn(EntityTypes.COW, 3, 2, 3);
        var creeper = helper.spawn(EntityTypes.CREEPER, 4, 2, 3);

        RetoldMobStates.getOrCreate(
                slime,
                helper.getLevel().getGameTime()
        ).setHunger(RetoldMobRules.huntThreshold(slime));

        helper.assertTrue(
                RetoldFactionRelations.shouldAttack(zombie, cow),
                "Undead must consider unfactioned living animals hostile"
        );
        helper.assertFalse(
                RetoldFactionRelations.shouldAttack(zombie, wither),
                "Undead must tolerate the Wither as another undead creature"
        );
        helper.assertTrue(
                RetoldFactionRelations.shouldAttack(wither, cow),
                "The Wither must use Undead hostility toward living creatures"
        );
        helper.assertTrue(
                RetoldFactionRelations.shouldAttack(slime, cow),
                "Slimes must consider other living creatures valid targets"
        );
        helper.assertFalse(
                RetoldFactionRelations.shouldAttack(slime, magmaCube),
                "Slimes and magma cubes must tolerate one another"
        );
        helper.assertTrue(
                RetoldFactionRelations.shouldAttack(guardian, cow),
                "Guardians must consider non-guardian living creatures hostile"
        );
        helper.assertFalse(
                RetoldFactionRelations.shouldAttack(guardian, elderGuardian),
                "Guardians must tolerate other monument guardians"
        );
        helper.assertFalse(
                RetoldFactionRelations.shouldAttack(zombie, creeper),
                "Even indiscriminate Undead must not deliberately attack creepers"
        );
        slime.setTarget(magmaCube);
        guardian.setTarget(elderGuardian);
        helper.assertTrue(
                slime.getTarget() == null && guardian.getTarget() == null,
                "Vanilla target writes must preserve Cube Mob and monument-Guardian tolerance"
        );
        slime.setTarget(cow);
        guardian.setTarget(cow);
        helper.assertTrue(
                slime.getTarget() == cow && guardian.getTarget() == cow,
                "Internal tolerance must not block valid outsider targets"
        );
        RetoldCombatTargets.clearTargetReferencesAndAggression(
                slime,
                cow,
                false
        );
        RetoldCombatTargets.clearTargetReferencesAndAggression(
                guardian,
                cow,
                false
        );
        helper.assertTrue(
                RetoldCombatTargets.applyAttackTarget(
                        slime,
                        magmaCube,
                        RetoldTargetSource.RETALIATION
                )
                        && RetoldCombatTargets.applyAttackTarget(
                        guardian,
                        elderGuardian,
                        RetoldTargetSource.RETALIATION
                ),
                "Explicit Retold retaliation must remain available across tolerant families"
        );
        helper.succeed();
    }

    private static void factionTagsPreserveDefaultsAndStandardUndead(
            GameTestHelper helper
    ) {
        Map<EntityType<?>, RetoldFaction> expectedFactions = Map.ofEntries(
                Map.entry(EntityTypes.PIGLIN, RetoldFaction.NETHER_REMNANTS),
                Map.entry(EntityTypes.PIGLIN_BRUTE, RetoldFaction.NETHER_REMNANTS),
                Map.entry(EntityTypes.BLAZE, RetoldFaction.NETHER_REMNANTS),
                Map.entry(RetoldEntityTypes.WILDFIRE.get(), RetoldFaction.NETHER_REMNANTS),
                Map.entry(EntityTypes.PILLAGER, RetoldFaction.ILLAGERS),
                Map.entry(EntityTypes.VINDICATOR, RetoldFaction.ILLAGERS),
                Map.entry(EntityTypes.EVOKER, RetoldFaction.ILLAGERS),
                Map.entry(EntityTypes.ILLUSIONER, RetoldFaction.ILLAGERS),
                Map.entry(EntityTypes.RAVAGER, RetoldFaction.ILLAGERS),
                Map.entry(EntityTypes.VEX, RetoldFaction.ILLAGERS),
                Map.entry(EntityTypes.ZOMBIE, RetoldFaction.UNDEAD),
                Map.entry(EntityTypes.ZOMBIE_VILLAGER, RetoldFaction.UNDEAD),
                Map.entry(EntityTypes.HUSK, RetoldFaction.UNDEAD),
                Map.entry(EntityTypes.DROWNED, RetoldFaction.UNDEAD),
                Map.entry(EntityTypes.ZOMBIE_HORSE, RetoldFaction.UNDEAD),
                Map.entry(EntityTypes.CAMEL_HUSK, RetoldFaction.UNDEAD),
                Map.entry(EntityTypes.ZOMBIFIED_PIGLIN, RetoldFaction.UNDEAD),
                Map.entry(EntityTypes.ZOGLIN, RetoldFaction.UNDEAD),
                Map.entry(EntityTypes.ZOMBIE_NAUTILUS, RetoldFaction.UNDEAD),
                Map.entry(EntityTypes.SKELETON, RetoldFaction.UNDEAD),
                Map.entry(EntityTypes.STRAY, RetoldFaction.UNDEAD),
                Map.entry(EntityTypes.WITHER_SKELETON, RetoldFaction.UNDEAD),
                Map.entry(EntityTypes.SKELETON_HORSE, RetoldFaction.UNDEAD),
                Map.entry(EntityTypes.BOGGED, RetoldFaction.UNDEAD),
                Map.entry(EntityTypes.PARCHED, RetoldFaction.UNDEAD),
                Map.entry(EntityTypes.WITHER, RetoldFaction.UNDEAD),
                Map.entry(EntityTypes.PHANTOM, RetoldFaction.UNDEAD),
                Map.entry(EntityTypes.GHAST, RetoldFaction.UNDEAD),
                Map.entry(EntityTypes.SLIME, RetoldFaction.SLIMES),
                Map.entry(EntityTypes.MAGMA_CUBE, RetoldFaction.SLIMES),
                Map.entry(EntityTypes.GUARDIAN, RetoldFaction.AQUATIC_HOSTILES),
                Map.entry(EntityTypes.ELDER_GUARDIAN, RetoldFaction.AQUATIC_HOSTILES),
                Map.entry(EntityTypes.CREEPER, RetoldFaction.CREEPERS),
                Map.entry(EntityTypes.SPIDER, RetoldFaction.ARTHROPODS),
                Map.entry(EntityTypes.CAVE_SPIDER, RetoldFaction.ARTHROPODS),
                Map.entry(EntityTypes.SILVERFISH, RetoldFaction.SILVERFISH),
                Map.entry(EntityTypes.ENDERMITE, RetoldFaction.ENDERMITES),
                Map.entry(EntityTypes.HOGLIN, RetoldFaction.NETHER_BEASTS),
                Map.entry(EntityTypes.BREEZE, RetoldFaction.BREEZES),
                Map.entry(EntityTypes.WARDEN, RetoldFaction.WARDENS),
                Map.entry(EntityTypes.ENDER_DRAGON, RetoldFaction.BOSSES),
                Map.entry(EntityTypes.CREAKING, RetoldFaction.CREAKINGS),
                Map.entry(EntityTypes.IRON_GOLEM, RetoldFaction.VILLAGE_DEFENDERS),
                Map.entry(EntityTypes.SNOW_GOLEM, RetoldFaction.VILLAGE_DEFENDERS),
                Map.entry(EntityTypes.ENDERMAN, RetoldFaction.ENDERS),
                Map.entry(EntityTypes.SHULKER, RetoldFaction.ENDERS)
        );

        for (EntityType<?> entityType : BuiltInRegistries.ENTITY_TYPE) {
            RetoldFaction expected = expectedFactions.get(entityType);
            RetoldFaction actual = RetoldFactionMembers.getFaction(entityType);

            helper.assertTrue(
                    actual == expected,
                    BuiltInRegistries.ENTITY_TYPE.getKey(entityType)
                            + " must retain faction " + expected + ", got " + actual
            );
            helper.assertFalse(
                    RetoldFactionMembers.hasConflictingFactionTags(entityType),
                    BuiltInRegistries.ENTITY_TYPE.getKey(entityType)
                            + " must not have conflicting default faction tags"
            );

            if (expected != null) {
                helper.assertTrue(
                        entityType.builtInRegistryHolder().is(
                                RetoldFactionMembers.getFactionTag(expected)
                        ),
                        BuiltInRegistries.ENTITY_TYPE.getKey(entityType)
                                + " must be supplied by its Retold faction tag"
                );
            }
        }

        var witch = helper.spawn(EntityTypes.WITCH, 1, 2, 1);
        helper.assertTrue(
                RetoldFactionMembers.getFaction(witch) == null
                        && RetoldFactionMembers.isLooseAllyOf(
                        witch,
                        RetoldFaction.ILLAGERS
                ),
                "Witches must remain loose Illager allies rather than full members"
        );
        helper.succeed();
    }

    private static void factionTagsDriveTargetingAndRetaliation(
            GameTestHelper helper
    ) {
        var undeadMount = helper.spawn(EntityTypes.SKELETON_HORSE, 1, 2, 1);
        var cow = helper.spawn(EntityTypes.COW, 3, 2, 1);
        var defender = helper.spawn(EntityTypes.IRON_GOLEM, 5, 2, 1);
        var owner = helper.makeMockPlayer(GameType.SURVIVAL);

        undeadMount.setTamed(false);
        int untamedTargetGoalCount = undeadMount.targetSelector
                .getAvailableGoals()
                .size();
        helper.assertValueEqual(
                RetoldFactionMembers.getFaction(undeadMount),
                RetoldFaction.UNDEAD,
                "An untamed standard-tagged undead mount must join the Undead faction"
        );
        helper.assertTrue(
                RetoldFactionRelations.shouldAttack(undeadMount, cow),
                "A standard-tagged Undead member must inherit Undead targeting"
        );
        helper.assertTrue(
                RetoldCombatTargets.applyAttackTarget(
                        undeadMount,
                        cow,
                        RetoldTargetSource.FACTION_COMBAT
                ),
                "Faction targeting must accept a standard-tagged Undead attacker"
        );
        helper.assertTrue(
                RetoldFactionTargetMemory.isOwnedByAny(
                        undeadMount,
                        cow,
                        RetoldTargetSource.FACTION_COMBAT
                ),
                "Faction targeting must retain FACTION_COMBAT ownership"
        );
        helper.assertTrue(
                RetoldCombatTargets.applyAttackTarget(
                        defender,
                        undeadMount,
                        RetoldTargetSource.RETALIATION
                ),
                "A tagged Village Defender must accept an immediate retaliation target"
        );
        helper.assertTrue(
                RetoldFactionTargetMemory.isOwnedByAny(
                        defender,
                        undeadMount,
                        RetoldTargetSource.RETALIATION
                ),
                "Defender retaliation must retain RETALIATION ownership"
        );

        RetoldCombatTargets.clearTargetReferencesAndAggression(
                undeadMount,
                cow,
                true
        );
        undeadMount.setTamed(true);
        undeadMount.setOwner(owner);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(
                    RetoldFactionMembers.getFaction(undeadMount) == null,
                    "A tamed undead mount must not retain indiscriminate Undead hostility"
            );
            helper.assertFalse(
                    RetoldFactionRelations.shouldAttack(undeadMount, cow),
                    "A tamed undead mount must not inherit generic Undead targeting"
            );
            helper.assertValueEqual(
                    undeadMount.targetSelector.getAvailableGoals().size(),
                    untamedTargetGoalCount - 2,
                    "Loaded mobs must remove their faction target and retaliation goals "
                            + "when effective membership changes"
            );
            helper.succeed();
        });
    }

    private static void ignitedCreeperCausesDelayedFlightExceptZombies(
            GameTestHelper helper
    ) {
        ServerLevel level = helper.getLevel();
        long gameTime = level.getGameTime();
        var creeper = helper.spawn(EntityTypes.CREEPER, 7, 2, 1);
        var cow = helper.spawn(EntityTypes.COW, 1, 2, 1);
        var ironGolem = helper.spawn(EntityTypes.IRON_GOLEM, 1, 2, 3);
        Zombie zombie = helper.spawn(EntityTypes.ZOMBIE, 2, 2, 3);
        var ghast = helper.spawn(EntityTypes.GHAST, 1, 4, 5);

        creeper.ignite();
        ironGolem.setTarget(zombie);

        RetoldCreeperAwareness.tick(level, cow, gameTime, true);
        helper.assertFalse(
                RetoldCreeperAwareness.isReacting(cow),
                "A slow animal must have a short reaction delay after noticing a fuse"
        );

        RetoldCreeperAwareness.tick(level, cow, gameTime + 10L, false);
        RetoldCreeperAwareness.tick(level, ironGolem, gameTime, true);
        RetoldCreeperAwareness.tick(level, ironGolem, gameTime + 3L, false);
        RetoldCreeperAwareness.tick(level, zombie, gameTime, true);
        RetoldCreeperAwareness.tick(level, zombie, gameTime + 20L, true);
        RetoldCreeperAwareness.tick(level, ghast, gameTime, true);
        RetoldCreeperAwareness.tick(level, ghast, gameTime + 8L, false);

        helper.assertTrue(
                RetoldCreeperAwareness.isReacting(cow),
                "A mobile animal must flee after its creeper reaction delay"
        );
        helper.assertTrue(
                RetoldCreeperAwareness.isReacting(ironGolem),
                "A village defender must abandon combat and flee an ignited creeper"
        );
        helper.assertTrue(
                ironGolem.getTarget() == null,
                "Creeper flight must clear a village defender's previous combat target"
        );
        helper.assertFalse(
                RetoldCreeperAwareness.isReacting(zombie),
                "Zombie-family mobs must hold their ground instead of fleeing creepers"
        );
        helper.assertTrue(
                RetoldCreeperAwareness.isReacting(ghast),
                "Mobile flying mobs without Pathfinder navigation must also flee creepers"
        );
        helper.succeed();
    }

    private static void silverfishAndEndermitesAreUnrelated(
            GameTestHelper helper
    ) {
        var firstSilverfish = helper.spawn(EntityTypes.SILVERFISH, 1, 2, 1);
        var secondSilverfish = helper.spawn(EntityTypes.SILVERFISH, 2, 2, 1);
        var endermite = helper.spawn(EntityTypes.ENDERMITE, 3, 2, 1);
        var ironGolem = helper.spawn(EntityTypes.IRON_GOLEM, 4, 2, 1);

        helper.assertValueEqual(
                RetoldFactionMembers.getFaction(firstSilverfish),
                RetoldFaction.SILVERFISH,
                "Silverfish must have their own faction identity"
        );
        helper.assertValueEqual(
                RetoldFactionMembers.getFaction(endermite),
                RetoldFaction.ENDERMITES,
                "Endermites must have their own faction identity"
        );
        helper.assertFalse(
                RetoldFactionRelations.areEnemyFactions(
                        RetoldFaction.SILVERFISH,
                        RetoldFaction.ENDERMITES
                ),
                "Silverfish must remain neutral toward Endermites"
        );
        helper.assertFalse(
                RetoldFactionRelations.areEnemyFactions(
                        RetoldFaction.ENDERMITES,
                        RetoldFaction.SILVERFISH
                ),
                "Endermites must remain neutral toward Silverfish"
        );
        helper.assertTrue(
                RetoldSwarmScavengerEvents.canShareSmallArthropodSwarm(
                        firstSilverfish,
                        secondSilverfish
                ),
                "Silverfish must still coordinate with their own species"
        );
        helper.assertFalse(
                RetoldSwarmScavengerEvents.canShareSmallArthropodSwarm(
                        firstSilverfish,
                        endermite
                ),
                "Silverfish and Endermites must never share swarm coordination"
        );
        helper.assertTrue(
                RetoldFactionRelations.shouldAttack(ironGolem, firstSilverfish),
                "Village defenders must retain hostility toward Silverfish"
        );
        helper.assertTrue(
                RetoldFactionRelations.shouldAttack(ironGolem, endermite),
                "Village defenders must retain hostility toward Endermites"
        );
        helper.succeed();
    }

    private static void catsRetreatFromUnignitedCreepers(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        long gameTime = level.getGameTime();
        var cat = helper.spawn(EntityTypes.CAT, 1, 2, 1);
        var cow = helper.spawn(EntityTypes.COW, 1, 2, 3);

        helper.spawn(EntityTypes.CREEPER, 4, 2, 1);

        RetoldCreeperAwareness.tick(level, cat, gameTime, true);
        RetoldCreeperAwareness.tick(level, cow, gameTime, true);

        helper.assertTrue(
                RetoldCreeperAwareness.isReacting(cat),
                "Cats must hiss and retreat from a nearby creeper before it ignites"
        );
        helper.assertFalse(
                RetoldCreeperAwareness.isReacting(cow),
                "Ordinary animals must not flee a creeper before its fuse becomes dangerous"
        );
        helper.succeed();
    }

    private static void endermenOnlyCoordinateSharedDefenseInStage3(
            GameTestHelper helper
    ) {
        ServerLevel level = helper.getLevel();
        var victim = helper.spawn(EntityTypes.ENDERMAN, 1, 2, 1);
        var ally = helper.spawn(EntityTypes.ENDERMAN, 3, 2, 1);
        var attacker = helper.spawn(EntityTypes.ZOMBIE, 2, 2, 3);

        try {
            int stageTwoResponders = RetoldEndermanDefense.onEndermanAttacked(
                    level,
                    victim,
                    attacker,
                    RetoldWorldStage.STAGE_2
            );

            helper.assertValueEqual(
                    stageTwoResponders,
                    1,
                    "Before Stage 3 only the attacked Enderman may retaliate"
            );
            helper.assertTrue(
                    victim.getTarget() == attacker,
                    "An attacked Enderman must retaliate against its attacker"
            );
            helper.assertTrue(
                    ally.getTarget() == null,
                    "Nearby Endermen must not join another Enderman's defense before Stage 3"
            );
            helper.assertTrue(
                    RetoldFactionTargetMemory.isOwnedByAny(
                            victim,
                            attacker,
                            RetoldTargetSource.RETALIATION
                    ),
                    "The attacked Enderman's target must use retaliation ownership"
            );

            RetoldCombatTargets.clearTargetReferencesAndAggression(
                    victim,
                    attacker,
                    true
            );

            int stageThreeResponders = RetoldEndermanDefense.onEndermanAttacked(
                    level,
                    victim,
                    attacker,
                    RetoldWorldStage.STAGE_3
            );

            helper.assertTrue(
                    stageThreeResponders >= 2,
                    "Stage 3 defense must recruit nearby Endermen"
            );
            helper.assertTrue(
                    ally.getTarget() == attacker,
                    "A nearby Enderman must join shared defense in Stage 3"
            );
            helper.assertTrue(
                    RetoldFactionTargetMemory.isOwnedByAny(
                            ally,
                            attacker,
                            RetoldTargetSource.FACTION_ASSIST
                    ),
                    "A recruited Enderman's target must use assist ownership"
            );
            helper.succeed();
        } finally {
            RetoldCombatTargets.clearTargetReferencesAndAggression(
                    victim,
                    attacker,
                    true
            );
            RetoldCombatTargets.clearTargetReferencesAndAggression(
                    ally,
                    attacker,
                    true
            );
            victim.discard();
            ally.discard();
            attacker.discard();
        }
    }

    private static void animalFeedingRespectsMobGriefing(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        boolean originalMobGriefing = level.getGameRules().get(GameRules.MOB_GRIEFING);
        long gameTime = level.getGameTime();
        var cow = helper.spawn(EntityTypes.COW, 1, 2, 1);
        BlockPos forageRelativePos = new BlockPos(1, 1, 1);
        BlockPos foragePos = helper.absolutePos(forageRelativePos);
        ItemEntity droppedWheat = new ItemEntity(
                level,
                cow.getX(),
                cow.getY(),
                cow.getZ(),
                new ItemStack(Items.WHEAT)
        );
        RetoldMobState state = RetoldMobStates.getOrCreate(cow, gameTime);

        try {
            state.setHunger(80);
            helper.setBlock(forageRelativePos, Blocks.GRASS_BLOCK);
            helper.assertTrue(
                    level.addFreshEntity(droppedWheat),
                    "Dropped wheat must enter the test level"
            );
            level.getGameRules().set(
                    GameRules.MOB_GRIEFING,
                    false,
                    level.getServer()
            );

            helper.assertFalse(
                    RetoldFoodBehaviorEvents.tryConsumeForageBlock(
                            level,
                            cow,
                            foragePos,
                            gameTime
                    ),
                    "mobGriefing=false must prevent a cow from consuming a forage block"
            );
            helper.assertBlockPresent(Blocks.GRASS_BLOCK, forageRelativePos);
            helper.assertValueEqual(
                    state.hunger(),
                    80,
                    "Blocked forage consumption must not relieve hunger"
            );

            helper.assertTrue(
                    RetoldFoodBehaviorEvents.tryConsumeDroppedFood(
                            cow,
                            droppedWheat,
                            gameTime
                    ),
                    "Dropped food consumption must remain available when mobGriefing=false"
            );
            helper.assertTrue(
                    state.hunger() < 80,
                    "Eating dropped wheat must still relieve hunger"
            );
            helper.assertTrue(
                    droppedWheat.isRemoved(),
                    "The consumed dropped-food entity must be removed"
            );
            helper.succeed();
        } finally {
            level.getGameRules().set(
                    GameRules.MOB_GRIEFING,
                    originalMobGriefing,
                    level.getServer()
            );
            droppedWheat.discard();
            RetoldMobStates.remove(cow);
        }
    }

    private static void droppedFoodInterruptsHuntingButNotRetaliation(
            GameTestHelper helper
    ) {
        ServerLevel level = helper.getLevel();
        long gameTime = level.getGameTime();
        var wolf = helper.spawn(EntityTypes.WOLF, 2, 2, 2);
        var chicken = helper.spawn(EntityTypes.CHICKEN, 3, 2, 2);
        var zombie = helper.spawn(EntityTypes.ZOMBIE, 4, 2, 2);
        var slime = helper.spawn(EntityTypes.SLIME, 7, 2, 2);
        var cow = helper.spawn(EntityTypes.COW, 8, 2, 2);
        ItemEntity beef = new ItemEntity(
                level,
                wolf.getX(),
                wolf.getY(),
                wolf.getZ(),
                new ItemStack(Items.BEEF)
        );
        ItemEntity secondBeef = new ItemEntity(
                level,
                wolf.getX(),
                wolf.getY(),
                wolf.getZ(),
                new ItemStack(Items.BEEF)
        );
        ItemEntity slimeFood = new ItemEntity(
                level,
                slime.getX(),
                slime.getY(),
                slime.getZ(),
                new ItemStack(Items.COBBLESTONE)
        );
        ItemEntity fedSlimeFood = new ItemEntity(
                level,
                slime.getX(),
                slime.getY(),
                slime.getZ(),
                new ItemStack(Items.DIRT)
        );
        RetoldMobState wolfState = RetoldMobStates.getOrCreate(wolf, gameTime);
        RetoldMobState slimeState = RetoldMobStates.getOrCreate(slime, gameTime);

        slime.setSize(2, true);

        try {
            wolfState.setHunger(80);
            slimeState.setHunger(0);

            helper.assertTrue(
                    RetoldMobRules.wantsDroppedFood(slime, slimeState),
                    "A completely fed Slime must still want dropped items"
            );
            helper.assertTrue(
                    RetoldFoodBehaviorEvents.tryConsumeDroppedFood(
                            slime,
                            fedSlimeFood,
                            gameTime
                    ),
                    "A completely fed Slime must still consume a dropped item"
            );
            helper.assertFalse(
                    RetoldCombatTargets.applyAttackTarget(
                            slime,
                            cow,
                            RetoldTargetSource.FACTION_COMBAT
                    ),
                    "A fed Slime must still reject a living hunt target"
            );
            RetoldFeedingPose.tick(
                    slime,
                    gameTime + RetoldFeedingPose.DURATION_TICKS + 1L
            );

            helper.assertTrue(
                    RetoldCombatTargets.applyAttackTarget(
                            wolf,
                            chicken,
                            RetoldTargetSource.BEHAVIOR_COMBAT
                    ),
                    "The hungry Wolf must begin with ordinary prey"
            );
            helper.assertTrue(
                    RetoldAiControl.tryClaim(
                            wolf,
                            RetoldAiControlMode.HUNT,
                            RetoldAiControlOwner.SOLO_HUNTING,
                            gameTime,
                            80
                    ),
                    "The Wolf must begin under hunt control"
            );
            wolf.setSprinting(true);

            helper.assertTrue(
                    RetoldFoodBehaviorEvents.tryPreferDroppedFoodOverHunt(
                            wolf,
                            beef,
                            gameTime
                    ),
                    "Edible dropped meat must interrupt ordinary prey hunting"
            );
            helper.assertTrue(
                    wolf.getTarget() == null,
                    "Food preference must clear the previous prey target"
            );
            helper.assertValueEqual(
                    RetoldAiControl.getMode(wolf),
                    RetoldAiControlMode.FEED,
                    "Dropped food must take FEED control from HUNT"
            );
            helper.assertValueEqual(
                    RetoldAiControl.getOwner(wolf),
                    RetoldAiControlOwner.FOOD,
                    "The shared food system must own the preference"
            );
            helper.assertFalse(
                    wolf.isSprinting(),
                    "A mob leaving a hunt for food must stop chase sprinting"
            );

            RetoldAiControl.clear(wolf);
            helper.assertTrue(
                    RetoldCombatTargets.applyAttackTarget(
                            wolf,
                            zombie,
                            RetoldTargetSource.RETALIATION
                    ),
                    "The Wolf must accept its attacker as a retaliation target"
            );
            helper.assertTrue(
                    RetoldAiControl.tryClaim(
                            wolf,
                            RetoldAiControlMode.HUNT,
                            RetoldAiControlOwner.SOLO_HUNTING,
                            gameTime,
                            80
                    ),
                    "The regression setup must retain a stale hunt control state"
            );
            helper.assertFalse(
                    RetoldFoodBehaviorEvents.tryPreferDroppedFoodOverHunt(
                            wolf,
                            secondBeef,
                            gameTime
                    ),
                    "Dropped food must not interrupt urgent retaliation"
            );
            helper.assertTrue(
                    wolf.getTarget() == zombie,
                    "The retaliation target must remain active"
            );

            slimeState.setHunger(RetoldMobRules.huntThreshold(slime));
            helper.assertTrue(
                    RetoldCombatTargets.applyAttackTarget(
                            slime,
                            cow,
                            RetoldTargetSource.FACTION_COMBAT
                    ),
                    "The hungry Slime must begin with an ordinary living target"
            );
            helper.assertTrue(
                    RetoldAiControl.tryClaim(
                            slime,
                            RetoldAiControlMode.HUNT,
                            RetoldAiControlOwner.SWARM,
                            gameTime,
                            80
                    ),
                    "The Slime must begin under swarm hunt control"
            );
            helper.assertTrue(
                    RetoldFoodBehaviorEvents.tryPreferDroppedFoodOverHunt(
                            slime,
                            slimeFood,
                            gameTime
                    ),
                    "The shared preference must also cover non-predator hunt profiles"
            );
            helper.assertTrue(
                    slime.getTarget() == null,
                    "The Slime must choose its dropped item over living prey"
            );
            helper.succeed();
        } finally {
            RetoldCombatTargets.clearTargetReferencesAndAggression(
                    wolf,
                    chicken,
                    true
            );
            RetoldCombatTargets.clearTargetReferencesAndAggression(
                    wolf,
                    zombie,
                    true
            );
            RetoldCombatTargets.clearTargetReferencesAndAggression(
                    slime,
                    cow,
                    true
            );
            RetoldAiControl.clear(wolf);
            RetoldAiControl.clear(slime);
            RetoldMobStates.remove(wolf);
            RetoldMobStates.remove(slime);
            beef.discard();
            secondBeef.discard();
            slimeFood.discard();
            fedSlimeFood.discard();
            wolf.discard();
            chicken.discard();
            zombie.discard();
            slime.discard();
            cow.discard();
        }
    }

    private static void cubeMobHopsTowardDroppedItem(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        long gameTime = level.getGameTime();
        BlockPos floorMin = new BlockPos(0, 1, 0);
        BlockPos floorMax = new BlockPos(8, 1, 4);

        for (BlockPos floorPos : BlockPos.betweenClosed(floorMin, floorMax)) {
            helper.setBlock(floorPos, Blocks.STONE);
        }

        var slime = helper.spawn(EntityTypes.SLIME, 1, 2, 2);
        Vec3 itemPosition = helper.absoluteVec(new Vec3(7.5D, 2.0D, 2.5D));
        ItemEntity droppedItem = new ItemEntity(
                level,
                itemPosition.x(),
                itemPosition.y(),
                itemPosition.z(),
                new ItemStack(Items.COBBLESTONE)
        );
        double startingX = slime.getX();

        slime.setSize(3, true);
        droppedItem.setNoGravity(true);
        level.addFreshEntity(droppedItem);

        helper.assertTrue(
                RetoldAiControl.tryClaim(
                        slime,
                        RetoldAiControlMode.FEED,
                        RetoldAiControlOwner.SYSTEM,
                        1_000,
                        "cube_mob_movement_test",
                        gameTime,
                        80
                ),
                "The movement regression setup must obtain FEED control"
        );

        RetoldBehaviorMovement.throttledMoveTo(
                slime,
                droppedItem,
                0.72D,
                gameTime,
                8,
                1.5D * 1.5D
        );

        helper.startSequence()
                .thenIdle(30)
                .thenExecute(() -> {
                    try {
                        helper.assertTrue(
                                slime.getX() > startingX + 0.5D,
                                "A Cube Mob under FEED control must visibly hop toward its dropped item; startX="
                                        + startingX
                                        + ", currentX="
                                        + slime.getX()
                                        + ", alive="
                                        + slime.isAlive()
                                        + ", y="
                                        + slime.getY()
                                        + ", onGround="
                                        + slime.onGround()
                                        + ", noAi="
                                        + slime.isNoAi()
                                        + ", speed="
                                        + slime.getSpeed()
                                        + ", velocity="
                                        + slime.getDeltaMovement()
                        );
                    } finally {
                        RetoldAiControl.clear(slime);
                        RetoldMobStates.remove(slime);
                        slime.discard();
                        droppedItem.discard();

                        for (BlockPos floorPos : BlockPos.betweenClosed(
                                floorMin,
                                floorMax
                        )) {
                            helper.setBlock(floorPos, Blocks.AIR);
                        }
                    }
                })
                .thenSucceed();
    }

    private static void creeperExplosionRespectsMobGriefing(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        boolean originalMobGriefing = level.getGameRules().get(GameRules.MOB_GRIEFING);
        BlockPos relativePos = new BlockPos(2, 2, 2);
        BlockPos blockPos = helper.absolutePos(relativePos);
        var creeper = helper.spawn(EntityTypes.CREEPER, 2, 2, 1);

        try {
            helper.setBlock(relativePos, Blocks.COBBLESTONE);
            level.getGameRules().set(
                    GameRules.MOB_GRIEFING,
                    false,
                    level.getServer()
            );
            level.explode(
                    creeper,
                    blockPos.getX() + 0.5D,
                    blockPos.getY() + 0.5D,
                    blockPos.getZ() + 0.5D,
                    4.0F,
                    Level.ExplosionInteraction.MOB
            );

            helper.assertBlockPresent(Blocks.COBBLESTONE, relativePos);
            helper.succeed();
        } finally {
            level.getGameRules().set(
                    GameRules.MOB_GRIEFING,
                    originalMobGriefing,
                    level.getServer()
            );
            creeper.discard();
        }
    }

    private static void extinguishedTorchesDropMatchingLitItems(
            GameTestHelper helper
    ) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));

        assertOnlyDrop(
                helper,
                level,
                pos,
                RetoldBlocks.EXTINGUISHED_TORCH.get(),
                Items.TORCH,
                "An extinguished torch must drop a normal torch"
        );
        assertOnlyDrop(
                helper,
                level,
                pos,
                RetoldBlocks.EXTINGUISHED_WALL_TORCH.get(),
                Items.TORCH,
                "An extinguished wall torch must drop a normal torch"
        );
        assertOnlyDrop(
                helper,
                level,
                pos,
                RetoldBlocks.EXTINGUISHED_SOUL_TORCH.get(),
                Items.SOUL_TORCH,
                "An extinguished soul torch must drop a soul torch"
        );
        assertOnlyDrop(
                helper,
                level,
                pos,
                RetoldBlocks.EXTINGUISHED_SOUL_WALL_TORCH.get(),
                Items.SOUL_TORCH,
                "An extinguished soul wall torch must drop a soul torch"
        );
        assertOnlyDrop(
                helper,
                level,
                pos,
                RetoldBlocks.EXTINGUISHED_COPPER_TORCH.get(),
                Items.COPPER_TORCH,
                "An extinguished copper torch must drop a copper torch"
        );
        assertOnlyDrop(
                helper,
                level,
                pos,
                RetoldBlocks.EXTINGUISHED_COPPER_WALL_TORCH.get(),
                Items.COPPER_TORCH,
                "An extinguished copper wall torch must drop a copper torch"
        );

        helper.succeed();
    }

    private static void compatibilityBlockTagsPreserveExistingDefaults(
            GameTestHelper helper
    ) {
        assertTaggedDefaults(
                helper,
                RetoldTags.ARMADILLO_GRUB_SOILS,
                Blocks.GRASS_BLOCK,
                Blocks.DIRT,
                Blocks.COARSE_DIRT,
                Blocks.ROOTED_DIRT,
                Blocks.PODZOL,
                Blocks.RED_SAND,
                Blocks.TERRACOTTA,
                Blocks.MUD,
                Blocks.MUDDY_MANGROVE_ROOTS
        );
        assertTaggedDefaults(
                helper,
                RetoldTags.ARMADILLO_SCRUB_RANGE_BLOCKS,
                Blocks.GRASS_BLOCK,
                Blocks.DIRT,
                Blocks.COARSE_DIRT,
                Blocks.ROOTED_DIRT,
                Blocks.PODZOL,
                Blocks.SAND,
                Blocks.RED_SAND,
                Blocks.TERRACOTTA
        );
        assertTaggedDefaults(
                helper,
                RetoldTags.PANDA_BAMBOO_BLOCKS,
                Blocks.BAMBOO,
                Blocks.BAMBOO_SAPLING
        );
        assertTaggedDefaults(
                helper,
                RetoldTags.TURTLE_BEACH_BLOCKS,
                Blocks.SAND,
                Blocks.RED_SAND
        );
        assertTaggedDefaults(
                helper,
                RetoldTags.DESERT_BROWSE_BLOCKS,
                Blocks.DEAD_BUSH
        );
        assertTaggedDefaults(
                helper,
                RetoldTags.GOAT_SCRAPE_BLOCKS,
                Blocks.STONE,
                Blocks.SNOW_BLOCK,
                Blocks.PACKED_ICE,
                Blocks.GRAVEL
        );
        assertTaggedDefaults(
                helper,
                RetoldTags.MOOSHROOM_GRAZING_BLOCKS,
                Blocks.MYCELIUM
        );
        assertTaggedDefaults(
                helper,
                RetoldTags.FORAGE_CROPS,
                Blocks.WHEAT,
                Blocks.CARROTS,
                Blocks.POTATOES,
                Blocks.BEETROOTS,
                Blocks.MELON_STEM,
                Blocks.PUMPKIN_STEM,
                Blocks.ATTACHED_MELON_STEM,
                Blocks.ATTACHED_PUMPKIN_STEM
        );
        assertTaggedDefaults(
                helper,
                RetoldTags.FORAGE_FLOWERS,
                Blocks.POPPY,
                Blocks.DANDELION,
                Blocks.BLUE_ORCHID,
                Blocks.ALLIUM,
                Blocks.AZURE_BLUET,
                Blocks.RED_TULIP,
                Blocks.ORANGE_TULIP,
                Blocks.WHITE_TULIP,
                Blocks.PINK_TULIP,
                Blocks.OXEYE_DAISY,
                Blocks.CORNFLOWER,
                Blocks.LILY_OF_THE_VALLEY,
                Blocks.SUNFLOWER,
                Blocks.LILAC,
                Blocks.ROSE_BUSH,
                Blocks.PEONY
        );
        assertTaggedDefaults(
                helper,
                RetoldTags.GRAZER_FORAGE_PLANTS,
                Blocks.GRASS_BLOCK,
                Blocks.SHORT_GRASS,
                Blocks.TALL_GRASS,
                Blocks.FERN,
                Blocks.LARGE_FERN
        );
        assertTaggedDefaults(
                helper,
                RetoldTags.SMALL_PASSIVE_FORAGE_PLANTS,
                Blocks.SHORT_GRASS,
                Blocks.TALL_GRASS
        );
        assertTaggedDefaults(
                helper,
                RetoldTags.TURTLE_FORAGE_BLOCKS,
                Blocks.SEAGRASS
        );
        assertTaggedDefaults(
                helper,
                RetoldTags.HOGLIN_FORAGE_BLOCKS,
                Blocks.CRIMSON_FUNGUS
        );
        assertTaggedDefaults(
                helper,
                RetoldTags.PIGLIN_FORAGE_BLOCKS,
                Blocks.CRIMSON_FUNGUS,
                Blocks.RED_MUSHROOM,
                Blocks.BROWN_MUSHROOM
        );
        assertTaggedDefaults(
                helper,
                RetoldTags.STRIDER_FORAGE_BLOCKS,
                Blocks.WARPED_FUNGUS
        );
        assertTaggedDefaults(
                helper,
                RetoldTags.SPIDER_LAIR_WEB_BLOCKS,
                Blocks.COBWEB
        );
        assertTaggedDefaults(
                helper,
                RetoldTags.ILLAGER_VILLAGE_SIGNAL_BLOCKS,
                Blocks.BELL
        );
        assertTaggedDefaults(
                helper,
                RetoldTags.NETHER_REMNANT_GUARD_ANCHOR_BLOCKS,
                Blocks.NETHER_BRICKS,
                Blocks.NETHER_BRICK_FENCE,
                Blocks.NETHER_BRICK_STAIRS,
                Blocks.NETHER_BRICK_SLAB,
                Blocks.CRACKED_NETHER_BRICKS,
                Blocks.CHISELED_NETHER_BRICKS,
                Blocks.RED_NETHER_BRICKS,
                Blocks.RED_NETHER_BRICK_STAIRS,
                Blocks.RED_NETHER_BRICK_SLAB,
                Blocks.RED_NETHER_BRICK_WALL,
                Blocks.NETHER_BRICK_WALL
        );
        assertTaggedDefaults(
                helper,
                RetoldTags.OCEAN_MONUMENT_GUARD_ANCHOR_BLOCKS,
                Blocks.PRISMARINE,
                Blocks.PRISMARINE_BRICKS,
                Blocks.DARK_PRISMARINE,
                Blocks.SEA_LANTERN,
                Blocks.WET_SPONGE,
                Blocks.PRISMARINE_STAIRS,
                Blocks.PRISMARINE_SLAB,
                Blocks.PRISMARINE_WALL,
                Blocks.PRISMARINE_BRICK_STAIRS,
                Blocks.PRISMARINE_BRICK_SLAB,
                Blocks.DARK_PRISMARINE_STAIRS,
                Blocks.DARK_PRISMARINE_SLAB
        );
        assertTaggedDefaults(
                helper,
                RetoldTags.OCEAN_MONUMENT_PROTECTED_BLOCKS,
                Blocks.PRISMARINE,
                Blocks.PRISMARINE_BRICKS,
                Blocks.DARK_PRISMARINE,
                Blocks.SEA_LANTERN,
                Blocks.WET_SPONGE
        );
        helper.succeed();
    }

    private static void compatibilityItemTagsPreserveExistingDefaults(
            GameTestHelper helper
    ) {
        assertTaggedItemDefaults(
                helper,
                RetoldTags.CAMPFIRE_CONSUMABLE_IGNITERS,
                Items.FLINT
        );
        assertTaggedItemDefaults(
                helper,
                RetoldTags.LEAF_PRESERVING_TOOLS,
                Items.SHEARS
        );
        assertTaggedItemDefaults(
                helper,
                RetoldTags.MEAT_FOODS,
                Items.BEEF,
                Items.COOKED_BEEF,
                Items.PORKCHOP,
                Items.COOKED_PORKCHOP,
                Items.MUTTON,
                Items.COOKED_MUTTON,
                Items.CHICKEN,
                Items.COOKED_CHICKEN,
                Items.RABBIT,
                Items.COOKED_RABBIT,
                Items.ROTTEN_FLESH
        );
        assertTaggedItemDefaults(
                helper,
                RetoldTags.FISH_FOODS,
                Items.COD,
                Items.COOKED_COD,
                Items.SALMON,
                Items.COOKED_SALMON,
                Items.TROPICAL_FISH,
                Items.PUFFERFISH
        );
        assertTaggedItemDefaults(
                helper,
                RetoldTags.BERRY_FOODS,
                Items.SWEET_BERRIES,
                Items.GLOW_BERRIES
        );
        assertTaggedItemDefaults(
                helper,
                RetoldTags.GRAZER_FOODS,
                Items.WHEAT,
                Items.HAY_BLOCK,
                Items.APPLE,
                Items.CARROT,
                Items.POTATO,
                Items.BEETROOT,
                Items.SHORT_GRASS,
                Items.TALL_GRASS,
                Items.FERN,
                Items.LARGE_FERN
        );
        assertTaggedItemDefaults(
                helper,
                RetoldTags.SMALL_PASSIVE_FOODS,
                Items.WHEAT_SEEDS,
                Items.BEETROOT_SEEDS,
                Items.MELON_SEEDS,
                Items.PUMPKIN_SEEDS,
                Items.CARROT,
                Items.POTATO,
                Items.BEETROOT,
                Items.DANDELION
        );
        assertTaggedItemDefaults(
                helper,
                RetoldTags.FLOWER_FOODS,
                Items.POPPY,
                Items.DANDELION,
                Items.BLUE_ORCHID,
                Items.ALLIUM,
                Items.AZURE_BLUET,
                Items.RED_TULIP,
                Items.ORANGE_TULIP,
                Items.WHITE_TULIP,
                Items.PINK_TULIP,
                Items.OXEYE_DAISY,
                Items.CORNFLOWER,
                Items.LILY_OF_THE_VALLEY,
                Items.SUNFLOWER,
                Items.LILAC,
                Items.ROSE_BUSH,
                Items.PEONY
        );
        assertTaggedItemDefaults(
                helper,
                RetoldTags.NETHER_FUNGUS_FOODS,
                Items.CRIMSON_FUNGUS,
                Items.WARPED_FUNGUS,
                Items.RED_MUSHROOM,
                Items.BROWN_MUSHROOM
        );
        assertTaggedItemDefaults(
                helper,
                RetoldTags.BAT_FOODS,
                Items.SPIDER_EYE
        );
        assertTaggedItemDefaults(
                helper,
                RetoldTags.FELINE_SCAVENGE_FOODS,
                Items.PHANTOM_MEMBRANE
        );
        helper.succeed();
    }

    private static void compatibilityFoodAndForagePreserveExistingBehavior(
            GameTestHelper helper
    ) {
        var wolf = helper.spawn(EntityTypes.WOLF, 1, 2, 1);
        var fox = helper.spawn(EntityTypes.FOX, 1, 2, 1);
        var cat = helper.spawn(EntityTypes.CAT, 1, 2, 1);
        var cow = helper.spawn(EntityTypes.COW, 1, 2, 1);
        var chicken = helper.spawn(EntityTypes.CHICKEN, 1, 2, 1);
        var bee = helper.spawn(EntityTypes.BEE, 1, 2, 1);
        var turtle = helper.spawn(EntityTypes.TURTLE, 1, 2, 1);
        var armadillo = helper.spawn(EntityTypes.ARMADILLO, 1, 2, 1);
        var panda = helper.spawn(EntityTypes.PANDA, 1, 2, 1);
        var bat = helper.spawn(EntityTypes.BAT, 1, 2, 1);
        var hoglin = helper.spawn(EntityTypes.HOGLIN, 1, 2, 1);
        var piglin = helper.spawn(EntityTypes.PIGLIN, 1, 2, 1);
        var strider = helper.spawn(EntityTypes.STRIDER, 1, 2, 1);
        var guardian = helper.spawn(EntityTypes.GUARDIAN, 1, 2, 1);

        helper.assertTrue(
                RetoldMobRules.canEatDroppedItem(
                        wolf,
                        new ItemStack(Items.BEEF)
                )
                        && RetoldMobRules.canEatDroppedItem(
                        wolf,
                        new ItemStack(Items.COD)
                )
                        && !RetoldMobRules.canEatDroppedItem(
                        wolf,
                        new ItemStack(Items.WHEAT)
                ),
                "Predator meat/fish eligibility must remain unchanged"
        );
        helper.assertTrue(
                RetoldMobRules.canEatDroppedItem(
                        fox,
                        new ItemStack(Items.SWEET_BERRIES)
                )
                        && RetoldMobRules.canEatDroppedItem(
                        cat,
                        new ItemStack(Items.PHANTOM_MEMBRANE)
                ),
                "Fox berry and feline scavenging foods must remain eligible"
        );
        helper.assertTrue(
                RetoldMobRules.canEatDroppedItem(
                        cow,
                        new ItemStack(Items.WHEAT)
                )
                        && RetoldMobRules.canEatDroppedItem(
                        chicken,
                        new ItemStack(Items.WHEAT_SEEDS)
                )
                        && RetoldMobRules.canEatDroppedItem(
                        bee,
                        new ItemStack(Items.POPPY)
                ),
                "Herbivore and Bee dropped-food eligibility must remain unchanged"
        );
        helper.assertTrue(
                RetoldMobRules.canEatDroppedItem(
                        turtle,
                        new ItemStack(Items.SEAGRASS)
                )
                        && RetoldMobRules.canEatDroppedItem(
                        armadillo,
                        new ItemStack(Items.SPIDER_EYE)
                )
                        && RetoldMobRules.canEatDroppedItem(
                        panda,
                        new ItemStack(Items.BAMBOO)
                )
                        && RetoldMobRules.canEatDroppedItem(
                        bat,
                        new ItemStack(Items.SPIDER_EYE)
                ),
                "Species-specific dropped foods must remain eligible"
        );
        helper.assertTrue(
                RetoldMobRules.canEatDroppedItem(
                        hoglin,
                        new ItemStack(Items.CRIMSON_FUNGUS)
                )
                        && RetoldMobRules.canEatDroppedItem(
                        piglin,
                        new ItemStack(Items.BROWN_MUSHROOM)
                )
                        && RetoldMobRules.canEatDroppedItem(
                        strider,
                        new ItemStack(Items.WARPED_FUNGUS)
                )
                        && RetoldMobRules.canEatDroppedItem(
                        guardian,
                        new ItemStack(Items.SALMON)
                ),
                "Nether and aquatic dropped foods must remain eligible"
        );
        helper.assertTrue(
                RetoldMobRules.canForageBlock(
                        cow,
                        Blocks.GRASS_BLOCK.defaultBlockState()
                )
                        && RetoldMobRules.canForageBlock(
                        cow,
                        Blocks.WHEAT.defaultBlockState()
                )
                        && RetoldMobRules.canForageBlock(
                        cow,
                        Blocks.POPPY.defaultBlockState()
                )
                        && RetoldMobRules.canForageBlock(
                        chicken,
                        Blocks.SHORT_GRASS.defaultBlockState()
                ),
                "Ordinary grazer and small-passive forage must remain eligible"
        );
        helper.assertTrue(
                RetoldMobRules.canForageBlock(
                        turtle,
                        Blocks.SEAGRASS.defaultBlockState()
                )
                        && RetoldMobRules.canForageBlock(
                        hoglin,
                        Blocks.CRIMSON_FUNGUS.defaultBlockState()
                )
                        && RetoldMobRules.canForageBlock(
                        piglin,
                        Blocks.BROWN_MUSHROOM.defaultBlockState()
                )
                        && RetoldMobRules.canForageBlock(
                        strider,
                        Blocks.WARPED_FUNGUS.defaultBlockState()
                ),
                "Turtle and Nether forage must remain eligible"
        );
        helper.assertTrue(
                RetoldMobRules.isFlowerBlock(
                        Blocks.POPPY.defaultBlockState()
                )
                        && !RetoldMobRules.isFlowerBlock(
                        Blocks.STONE.defaultBlockState()
                ),
                "Bee flower classification must remain unchanged"
        );
        helper.assertValueEqual(
                RetoldMobRules.foodRelief(wolf, new ItemStack(Items.BEEF)),
                28,
                "Predator meat relief must remain unchanged"
        );
        helper.assertValueEqual(
                RetoldMobRules.foodRelief(cow, new ItemStack(Items.WHEAT)),
                28,
                "High-value grazer food relief must remain unchanged"
        );
        helper.assertValueEqual(
                RetoldMobRules.forageRelief(
                        cow,
                        Blocks.WHEAT.defaultBlockState()
                ),
                24,
                "Grazer crop relief must remain unchanged"
        );

        helper.succeed();
    }

    private static void assertTaggedDefaults(
            GameTestHelper helper,
            TagKey<Block> tag,
            Block... blocks
    ) {
        for (Block block : blocks) {
            helper.assertTrue(
                    block.defaultBlockState().is(tag),
                    block + " must remain in " + tag.location()
            );
        }
    }

    private static void assertTaggedItemDefaults(
            GameTestHelper helper,
            TagKey<Item> tag,
            Item... items
    ) {
        for (Item item : items) {
            helper.assertTrue(
                    item.getDefaultInstance().is(tag),
                    item + " must remain in " + tag.location()
            );
        }
    }

    private static void assertOnlyDrop(
            GameTestHelper helper,
            ServerLevel level,
            BlockPos pos,
            Block block,
            Item expectedItem,
            String message
    ) {
        List<ItemStack> drops = Block.getDrops(
                block.defaultBlockState(),
                level,
                pos,
                null
        );
        helper.assertTrue(
                drops.size() == 1
                        && drops.getFirst().is(expectedItem)
                        && drops.getFirst().getCount() == 1,
                message
        );
    }

    private static Spider spawnSightedTestSpider(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Spider spider = new Spider(EntityTypes.SPIDER, level) {
            @Override
            public boolean hasLineOfSight(Entity target) {
                return true;
            }
        };
        Vec3 spiderPosition = helper.absoluteVec(new Vec3(1.5D, 1.0D, 1.5D));

        spider.snapTo(
                spiderPosition.x(),
                spiderPosition.y(),
                spiderPosition.z(),
                0.0F,
                0.0F
        );
        level.addFreshEntity(spider);
        return spider;
    }

    private static void buildDarkSpiderRoom(GameTestHelper helper) {
        for (int x = 0; x <= 4; x++) {
            for (int z = 0; z <= 2; z++) {
                helper.setBlock(new BlockPos(x, 3, z), Blocks.STONE);
            }
        }

        for (int y = 0; y <= 3; y++) {
            for (int x = 0; x <= 4; x++) {
                helper.setBlock(new BlockPos(x, y, 0), Blocks.STONE);
                helper.setBlock(new BlockPos(x, y, 2), Blocks.STONE);
            }

            helper.setBlock(new BlockPos(0, y, 1), Blocks.STONE);
            helper.setBlock(new BlockPos(4, y, 1), Blocks.STONE);
        }
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Retold.MODID, path);
    }

    private static final class InlineGameTest
            extends FunctionGameTestInstance {
        private final Consumer<GameTestHelper> test;

        private InlineGameTest(
                TestData<Holder<TestEnvironmentDefinition<?>>> testData,
                Consumer<GameTestHelper> test
        ) {
            super(BuiltinTestFunctions.ALWAYS_PASS, testData);
            this.test = test;
        }

        @Override
        public void run(GameTestHelper helper) {
            test.accept(helper);
        }
    }
}
