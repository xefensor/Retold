package cz.xefensor.retold.behavior.species;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.food.RetoldFoodBehaviorEvents;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;
import cz.xefensor.retold.behavior.profiles.RetoldMobState;
import cz.xefensor.retold.behavior.profiles.RetoldMobStates;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.BuiltinTestFunctions;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.function.Consumer;

public final class RetoldAquaticEcologyGameTests {
    private static final Identifier EMPTY_STRUCTURE =
            Identifier.withDefaultNamespace("empty");

    private RetoldAquaticEcologyGameTests() {
    }

    public static void register(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment =
                event.registerEnvironment(
                        id("isolated_aquatic_ecology"),
                        new TestEnvironmentDefinition.AllOf()
                );

        registerTest(
                event,
                environment,
                "aquatic_school_fish_graze_tagged_plants",
                RetoldAquaticEcologyGameTests::schoolFishGrazeTaggedPlants
        );
        registerTest(
                event,
                environment,
                "aquatic_squid_consume_only_dropped_raw_fish",
                RetoldAquaticEcologyGameTests::squidConsumeOnlyDroppedRawFish
        );
    }

    private static void schoolFishGrazeTaggedPlants(GameTestHelper helper) {
        buildWaterArena(helper);
        ServerLevel level = helper.getLevel();
        boolean originalMobGriefing = level.getGameRules().get(GameRules.MOB_GRIEFING);
        PathfinderMob[] fish = {
                helper.spawn(EntityTypes.COD, 2, 3, 2),
                helper.spawn(EntityTypes.SALMON, 4, 3, 2),
                helper.spawn(EntityTypes.TROPICAL_FISH, 6, 3, 2),
                helper.spawn(EntityTypes.PUFFERFISH, 8, 3, 2)
        };

        try {
            level.getGameRules().set(
                    GameRules.MOB_GRIEFING,
                    true,
                    level.getServer()
            );

            for (int index = 0; index < fish.length; index++) {
                PathfinderMob subject = fish[index];
                subject.setNoAi(true);
                RetoldMobState state = RetoldMobStates.getOrCreate(
                        subject,
                        level.getGameTime()
                );
                state.setHunger(50);

                helper.assertTrue(
                        RetoldMobRules.hungerInterval(subject) == 520
                                && RetoldMobRules.hasEatDrive(subject, state),
                        "Every school-fish profile must use the loaded passive hunger cycle"
                );
                helper.assertTrue(
                        RetoldMobRules.canForageBlock(
                                subject,
                                Blocks.SEAGRASS.defaultBlockState()
                        )
                                && RetoldMobRules.canForageBlock(
                                subject,
                                Blocks.KELP.defaultBlockState()
                        ),
                        "Every school fish must accept the default seagrass and kelp forage tag"
                );

                BlockPos relativeForage = new BlockPos(2 + index * 2, 3, 2);
                helper.setBlock(relativeForage, Blocks.SEAGRASS);
                boolean consumed = RetoldFoodBehaviorEvents.tryConsumeForageBlock(
                        level,
                        subject,
                        helper.absolutePos(relativeForage),
                        level.getGameTime()
                );

                helper.assertTrue(
                        consumed && state.hunger() < 50,
                        "Each school fish must receive a meal from reached tagged aquatic forage"
                );
                helper.assertFalse(
                        level.getBlockState(helper.absolutePos(relativeForage))
                                .is(Blocks.SEAGRASS),
                        "Consumed aquatic forage must be removed without drops"
                );
            }

            PathfinderMob protectedCod = helper.spawn(EntityTypes.COD, 2, 3, 5);
            protectedCod.setNoAi(true);
            RetoldMobState protectedState = RetoldMobStates.getOrCreate(
                    protectedCod,
                    level.getGameTime()
            );
            protectedState.setHunger(50);
            BlockPos protectedForage = new BlockPos(2, 3, 5);
            helper.setBlock(protectedForage, Blocks.SEAGRASS);
            level.getGameRules().set(
                    GameRules.MOB_GRIEFING,
                    false,
                    level.getServer()
            );

            helper.assertFalse(
                    RetoldFoodBehaviorEvents.tryConsumeForageBlock(
                            level,
                            protectedCod,
                            helper.absolutePos(protectedForage),
                            level.getGameTime()
                    ),
                    "mobGriefing=false must block destructive fish grazing"
            );
            helper.assertTrue(
                    protectedState.hunger() == 50
                            && level.getBlockState(helper.absolutePos(protectedForage))
                            .is(Blocks.SEAGRASS),
                    "Blocked grazing must preserve both hunger and the aquatic plant"
            );
            cleanup(protectedCod);
            helper.succeed();
        } finally {
            level.getGameRules().set(
                    GameRules.MOB_GRIEFING,
                    originalMobGriefing,
                    level.getServer()
            );
            cleanup(fish);
        }
    }

    private static void squidConsumeOnlyDroppedRawFish(GameTestHelper helper) {
        buildWaterArena(helper);
        ServerLevel level = helper.getLevel();
        boolean originalMobGriefing = level.getGameRules().get(GameRules.MOB_GRIEFING);
        PathfinderMob[] squid = {
                helper.spawn(EntityTypes.SQUID, 3, 3, 3),
                helper.spawn(EntityTypes.GLOW_SQUID, 6, 3, 3)
        };

        try {
            level.getGameRules().set(
                    GameRules.MOB_GRIEFING,
                    false,
                    level.getServer()
            );

            for (int index = 0; index < squid.length; index++) {
                PathfinderMob subject = squid[index];
                subject.setNoAi(true);
                RetoldMobState state = RetoldMobStates.getOrCreate(
                        subject,
                        level.getGameTime()
                );
                state.setHunger(50);

                helper.assertTrue(
                        RetoldMobRules.hungerInterval(subject) == 520
                                && RetoldMobRules.hasEatDrive(subject, state),
                        "Squid and Glow Squid must use the loaded passive hunger cycle"
                );
                helper.assertTrue(
                        RetoldMobRules.canEatDroppedItem(
                                subject,
                                new ItemStack(Items.COD)
                        )
                                && RetoldMobRules.canEatDroppedItem(
                                subject,
                                new ItemStack(Items.PUFFERFISH)
                        ),
                        "The default Squid diet must accept dropped raw fish"
                );
                helper.assertFalse(
                        RetoldMobRules.canEatDroppedItem(
                                subject,
                                new ItemStack(Items.COOKED_COD)
                        )
                                || RetoldMobRules.canEatDroppedItem(
                                subject,
                                new ItemStack(Items.SEAGRASS)
                        )
                                || RetoldMobRules.canUseNaturalPreyHuntingSystems(subject),
                        "Squid must not inherit cooked fish, plant grazing, or living-prey hunting"
                );

                ItemStack stack = new ItemStack(
                        index == 0 ? Items.COD : Items.SALMON,
                        2
                );
                ItemEntity food = new ItemEntity(
                        level,
                        subject.getX(),
                        subject.getY(),
                        subject.getZ(),
                        stack
                );
                helper.assertTrue(
                        level.addFreshEntity(food),
                        "The dropped raw-fish fixture must enter the test level"
                );

                boolean consumed = RetoldFoodBehaviorEvents.tryConsumeDroppedFood(
                        subject,
                        food,
                        level.getGameTime()
                );
                helper.assertTrue(
                        consumed
                                && state.hunger() < 50
                                && food.isAlive()
                                && food.getItem().getCount() == 1,
                        "Each Squid species must consume one reached raw fish and preserve the stack remainder"
                );
                food.discard();
            }

            helper.succeed();
        } finally {
            level.getGameRules().set(
                    GameRules.MOB_GRIEFING,
                    originalMobGriefing,
                    level.getServer()
            );
            cleanup(squid);
        }
    }

    private static void buildWaterArena(GameTestHelper helper) {
        for (int x = 0; x <= 10; x++) {
            for (int z = 0; z <= 7; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.GLASS);

                for (int y = 2; y <= 5; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.WATER);
                }
            }
        }
    }

    private static void cleanup(PathfinderMob... mobs) {
        for (PathfinderMob mob : mobs) {
            RetoldAiControl.clear(mob);
            RetoldMobStates.remove(mob);
            mob.discard();
        }
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
                        100,
                        0,
                        true
                );

        event.registerTest(
                id(name),
                new InlineGameTest(testData, test)
        );
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Retold.MODID, path);
    }

    private static final class InlineGameTest extends FunctionGameTestInstance {
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
