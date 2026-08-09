package cz.xefensor.retold.gametest;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.aender.portal.RetoldAenderGameTests;
import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.control.RetoldAiControlMode;
import cz.xefensor.retold.behavior.control.RetoldAiControlOwner;
import cz.xefensor.retold.behavior.control.RetoldControlledCombatEvents;
import cz.xefensor.retold.behavior.breeding.RetoldAnimalBreedingGameTests;
import cz.xefensor.retold.behavior.core.RetoldBehaviorMovement;
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
import cz.xefensor.retold.behavior.species.RetoldBatColonyGameTests;
import cz.xefensor.retold.behavior.species.RetoldHerdSchoolGameTests;
import cz.xefensor.retold.behavior.species.RetoldPandaBambooGameTests;
import cz.xefensor.retold.behavior.species.RetoldPolarBearWarningGameTests;
import cz.xefensor.retold.behavior.species.RetoldSpiderEcologyGameTests;
import cz.xefensor.retold.behavior.species.RetoldSpiderLairGameTests;
import cz.xefensor.retold.behavior.species.RetoldSlimeMergeGameTests;
import cz.xefensor.retold.behavior.species.RetoldSwarmScavengerEvents;
import cz.xefensor.retold.combat.RetoldFactionTargetMemory;
import cz.xefensor.retold.combat.RetoldCombatTargets;
import cz.xefensor.retold.combat.RetoldTargetSource;
import cz.xefensor.retold.enderman.RetoldEndermanDefense;
import cz.xefensor.retold.faction.RetoldFaction;
import cz.xefensor.retold.faction.RetoldFactionMembers;
import cz.xefensor.retold.faction.RetoldFactionRelations;
import cz.xefensor.retold.event.RetoldPlayerSyncEvents;
import cz.xefensor.retold.event.RetoldSnowballGameTests;
import cz.xefensor.retold.event.RetoldVexGameTests;
import cz.xefensor.retold.registry.RetoldBlocks;
import cz.xefensor.retold.stage.RetoldElementType;
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
import cz.xefensor.retold.worldgen.air.RetoldAirTempleDiscoveryGameTests;
import cz.xefensor.retold.worldgen.air.RetoldGaleCoreGameTests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.BuiltinTestFunctions;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
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
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.EnumSet;
import java.util.List;
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
                "extinguished_torches_drop_matching_lit_items",
                RetoldGameTests::extinguishedTorchesDropMatchingLitItems
        );

        RetoldAenderGameTests.register(event, environment);
        RetoldMobAvailabilityGameTests.register(event, environment);
        RetoldAiPerformanceGameTests.register(event);
        RetoldPerMobTpsGameTests.register(event);
        RetoldAiSightCacheGameTests.register(event, environment);
        RetoldDamageFleeGameTests.register(event, environment);
        RetoldWeakBarrierGameTests.register(event, environment);
        RetoldFoodSearchGameTests.register(event);
        RetoldAnimalFeederGameTests.register(event);
        RetoldAnimalBreedingGameTests.register(event);
        RetoldStarvationGameTests.register(event);
        RetoldHungerSurvivalGameTests.register(event);
        RetoldNaturalFoodAcquisitionGameTests.register(event);
        RetoldCommanderSupportGameTests.register(event, environment);
        RetoldAxolotlGuardianGameTests.register(event);
        RetoldBatColonyGameTests.register(event);
        RetoldHerdSchoolGameTests.register(event);
        RetoldPandaBambooGameTests.register(event);
        RetoldPolarBearWarningGameTests.register(event, environment);
        RetoldWolfPackHungerGameTests.register(event, environment);
        RetoldSpiderEcologyGameTests.register(event);
        RetoldSpiderLairGameTests.register(event);
        RetoldSlimeMergeGameTests.register(event);
        RetoldVexGameTests.register(event, environment);
        RetoldSnowballGameTests.register(event, environment);
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
        EnumSet<RetoldElementType> originalElements =
                EnumSet.noneOf(RetoldElementType.class);
        for (RetoldElementType element : RetoldElementType.values()) {
            if (data.hasElementOffered(element)) {
                originalElements.add(element);
            }
        }
        BlockPos originalEggPos = data.getDragonEggPos();

        try {
            data.clearOfferedElements();
            data.clearDragonEggPos();

            helper.assertTrue(
                    data.offerElement(RetoldElementType.WATER),
                    "A new element must be accepted"
            );
            helper.assertFalse(
                    data.offerElement(RetoldElementType.WATER),
                    "A duplicate element must be rejected"
            );
            helper.assertFalse(
                    data.hasAllElements(),
                    "One required element must not complete the ritual"
            );

            data.offerElement(RetoldElementType.AIR);
            helper.assertTrue(
                    data.hasAllElements(),
                    "Water and air must complete the current ritual"
            );
            helper.assertValueEqual(
                    data.offeredRequiredElementCount(),
                    data.requiredElementCount(),
                    "All required elements must be counted"
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
            data.clearOfferedElements();
            for (RetoldElementType element : originalElements) {
                data.offerElement(element);
            }

            if (originalEggPos == null) {
                data.clearDragonEggPos();
            } else {
                data.setDragonEggPos(originalEggPos);
            }
        }
    }

    private static void mobProfilesLoadFromDatapack(GameTestHelper helper) {
        helper.assertValueEqual(
                RetoldMobProfiles.loadedProfileCount(),
                77,
                "Every bundled mob profile must load"
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
        helper.succeed();
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
