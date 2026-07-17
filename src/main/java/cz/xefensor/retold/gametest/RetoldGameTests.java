package cz.xefensor.retold.gametest;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.control.RetoldAiControlMode;
import cz.xefensor.retold.behavior.control.RetoldControlledCombatEvents;
import cz.xefensor.retold.behavior.profiles.RetoldMobProfile;
import cz.xefensor.retold.behavior.profiles.RetoldMobProfileType;
import cz.xefensor.retold.behavior.profiles.RetoldMobProfiles;
import cz.xefensor.retold.combat.RetoldFactionTargetMemory;
import cz.xefensor.retold.combat.RetoldTargetSource;
import cz.xefensor.retold.stage.RetoldElementType;
import cz.xefensor.retold.stage.RetoldStageManager;
import cz.xefensor.retold.stage.RetoldStageRuntime;
import cz.xefensor.retold.stage.RetoldWorldData;
import cz.xefensor.retold.stage.RetoldWorldStage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.BuiltinTestFunctions;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.EnumSet;
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
                67,
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
