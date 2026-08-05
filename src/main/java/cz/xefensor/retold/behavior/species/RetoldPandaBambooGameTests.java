package cz.xefensor.retold.behavior.species;

import cz.xefensor.retold.Retold;
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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.panda.Panda;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.function.Consumer;

public final class RetoldPandaBambooGameTests {
    private static final Identifier EMPTY_STRUCTURE =
            Identifier.withDefaultNamespace("empty");
    private static final int TEST_TIMEOUT_TICKS = 240;
    private static final int INITIAL_HUNGER = 99;

    private RetoldPandaBambooGameTests() {
    }

    public static void register(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> naturalEnvironment =
                event.registerEnvironment(
                        id("isolated_panda_bamboo_natural"),
                        new TestEnvironmentDefinition.AllOf()
                );
        Holder<TestEnvironmentDefinition<?>> griefingEnvironment =
                event.registerEnvironment(
                        id("isolated_panda_bamboo_griefing"),
                        new TestEnvironmentDefinition.AllOf()
                );

        registerTest(
                event,
                naturalEnvironment,
                "panda_bamboo_naturally_eats_and_breaks_block",
                RetoldPandaBambooGameTests::pandaNaturallyEatsAndBreaksBlock
        );
        registerTest(
                event,
                griefingEnvironment,
                "panda_bamboo_consumption_respects_mob_griefing",
                RetoldPandaBambooGameTests::pandaBambooConsumptionRespectsMobGriefing
        );
    }

    private static void pandaNaturallyEatsAndBreaksBlock(
            GameTestHelper helper
    ) {
        ServerLevel level = helper.getLevel();
        BlockPos bambooPos = new BlockPos(5, 2, 5);
        buildHabitat(helper);
        helper.setBlock(bambooPos, Blocks.BAMBOO);

        Panda panda = helper.spawn(EntityTypes.PANDA, 3, 2, 5);
        ServerPlayer observer = makeLoadedWorldObserver(helper);
        RetoldMobState state = RetoldMobStates.getOrCreate(
                panda,
                level.getGameTime()
        );
        state.setHunger(INITIAL_HUNGER);
        state.markHungerTick(level.getGameTime());

        helper.succeedWhen(() -> {
            helper.assertTrue(
                    panda.isAlive() && !panda.isRemoved(),
                    "A hungry Panda must remain alive while approaching bamboo"
            );
            helper.assertTrue(
                    level.getBlockState(helper.absolutePos(bambooPos)).isAir(),
                    "A Panda meal must consume the targeted bamboo block"
            );
            helper.assertTrue(
                    state.hunger() < INITIAL_HUNGER,
                    "A Panda must receive hunger relief only after consuming bamboo; hunger="
                            + state.hunger()
            );
            cleanup(panda, observer);
        });
    }

    private static void pandaBambooConsumptionRespectsMobGriefing(
            GameTestHelper helper
    ) {
        ServerLevel level = helper.getLevel();
        BlockPos bambooPos = new BlockPos(5, 2, 5);
        buildHabitat(helper);
        helper.setBlock(bambooPos, Blocks.BAMBOO);

        Panda panda = helper.spawn(EntityTypes.PANDA, 4, 2, 5);
        RetoldMobState state = RetoldMobStates.getOrCreate(
                panda,
                level.getGameTime()
        );
        state.setHunger(INITIAL_HUNGER);
        boolean previousMobGriefing = level.getGameRules().get(GameRules.MOB_GRIEFING);

        try {
            level.getGameRules().set(
                    GameRules.MOB_GRIEFING,
                    false,
                    level.getServer()
            );
            boolean consumed = RetoldPandaBambooEvents.tryNibbleBamboo(
                    level,
                    panda,
                    helper.absolutePos(bambooPos),
                    level.getGameTime()
            );

            helper.assertFalse(
                    consumed,
                    "mobGriefing=false must prevent a Panda from consuming a bamboo block"
            );
            helper.assertTrue(
                    level.getBlockState(helper.absolutePos(bambooPos)).is(Blocks.BAMBOO),
                    "mobGriefing=false must preserve the bamboo block"
            );
            helper.assertTrue(
                    state.hunger() == INITIAL_HUNGER,
                    "A Panda must not gain hunger relief when bamboo consumption is denied"
            );
            helper.succeed();
        } finally {
            level.getGameRules().set(
                    GameRules.MOB_GRIEFING,
                    previousMobGriefing,
                    level.getServer()
            );
            cleanup(panda, null);
        }
    }

    private static void buildHabitat(GameTestHelper helper) {
        for (int x = 1; x <= 8; x++) {
            for (int z = 1; z <= 8; z++) {
                helper.setBlock(x, 1, z, Blocks.DIRT);
                helper.setBlock(x, 2, z, Blocks.AIR);
                helper.setBlock(x, 3, z, Blocks.AIR);
            }
        }
    }

    private static ServerPlayer makeLoadedWorldObserver(GameTestHelper helper) {
        ServerPlayer observer = (ServerPlayer) helper.makeMockServerPlayer(
                GameType.CREATIVE
        );
        BlockPos position = helper.absolutePos(new BlockPos(2, 2, 2));
        observer.snapTo(
                position.getX() + 0.5D,
                position.getY(),
                position.getZ() + 0.5D,
                0.0F,
                0.0F
        );
        return observer;
    }

    private static void cleanup(
            Panda panda,
            ServerPlayer observer
    ) {
        RetoldMobStates.remove(panda);
        panda.discard();

        if (observer != null) {
            panda.level().players().remove(observer);
            observer.discard();
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
                        TEST_TIMEOUT_TICKS,
                        0,
                        true
                );

        event.registerTest(id(name), new InlineGameTest(testData, test));
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
