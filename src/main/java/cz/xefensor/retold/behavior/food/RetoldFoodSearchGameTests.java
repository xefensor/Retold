package cz.xefensor.retold.behavior.food;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.control.RetoldAiControlMode;
import cz.xefensor.retold.behavior.control.RetoldAiControlOwner;
import cz.xefensor.retold.behavior.profiles.RetoldMobState;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;
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
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.Path;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.function.Consumer;

public final class RetoldFoodSearchGameTests {
    private static final Identifier EMPTY_STRUCTURE =
            Identifier.withDefaultNamespace("empty");

    private RetoldFoodSearchGameTests() {
    }

    public static void register(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment =
                event.registerEnvironment(
                        id("isolated_food_search"),
                        new TestEnvironmentDefinition.AllOf()
                );

        registerTest(
                event,
                environment,
                "hungry_forager_searches_for_food_with_navigation",
                RetoldFoodSearchGameTests::hungryForagerSearchesForFoodWithNavigation
        );
    }

    private static void hungryForagerSearchesForFoodWithNavigation(
            GameTestHelper helper
    ) {
        buildFloor(helper);

        Cow cow = helper.spawn(EntityTypes.COW, 8, 2, 8);

        helper.startSequence()
                .thenWaitUntil(() -> assertFoodSearchStarted(helper, cow))
                .thenExecute(() -> assertSatisfiedCowStopsSearching(helper, cow))
                .thenExecute(() -> cleanup(cow))
                .thenSucceed();
    }

    private static void assertFoodSearchStarted(
            GameTestHelper helper,
            Cow cow
    ) {
        ServerLevel level = helper.getLevel();
        long gameTime = level.getGameTime() + 3_000L;
        RetoldMobState state = RetoldMobStates.getOrCreate(cow, gameTime);

        state.setHunger(50);
        RetoldAiControl.clear(cow);
        cow.getNavigation().stop();
        cow.setOnGround(true);
        boolean started = RetoldFoodBehaviorEvents.tryStartOrContinueFoodSearch(
                level,
                cow,
                state,
                gameTime
        );
        helper.assertTrue(
                started,
                "A hungry non-predator must start an active food search; activeDrive="
                        + RetoldMobRules.hasActiveSearchDrive(state)
                        + ", ordinaryPredator="
                        + RetoldMobRules.canUseOrdinaryPredatorSystems(cow)
                        + ", mode="
                        + RetoldAiControl.getMode(cow)
                        + ", owner="
                        + RetoldAiControl.getOwner(cow)
                        + ", onGround="
                        + cow.onGround()
                        + ", noAi="
                        + cow.isNoAi()
                        + ", pos="
                        + cow.blockPosition()
        );

        BlockPos searchTarget = RetoldFoodBehaviorEvents.foodSearchTarget(cow);
        Path path = cow.getNavigation().getPath();

        helper.assertTrue(
                searchTarget != null
                        && RetoldAiControl.isControlledAsBy(
                        cow,
                        RetoldAiControlMode.SEARCH,
                        RetoldAiControlOwner.FOOD
                )
                        && path != null
                        && path.canReach(),
                "Food search must own SEARCH control and start a reachable navigation path; mode="
                        + RetoldAiControl.getMode(cow)
                        + ", owner="
                        + RetoldAiControl.getOwner(cow)
                        + ", target="
                        + searchTarget
                        + ", cowPos="
                        + cow.blockPosition()
                        + ", targetBlock="
                        + (searchTarget == null
                        ? "none"
                        : level.getBlockState(searchTarget))
                        + ", targetBelow="
                        + (searchTarget == null
                        ? "none"
                        : level.getBlockState(searchTarget.below()))
                        + ", path="
                        + path
                        + ", canReach="
                        + (path != null && path.canReach())
        );
    }

    private static void assertSatisfiedCowStopsSearching(
            GameTestHelper helper,
            Cow cow
    ) {
        ServerLevel level = helper.getLevel();
        long gameTime = level.getGameTime() + 3_000L;
        RetoldMobState state = RetoldMobStates.getOrCreate(cow, gameTime);

        state.setHunger(0);
        helper.assertFalse(
                RetoldFoodBehaviorEvents.tryStartOrContinueFoodSearch(
                        level,
                        cow,
                        state,
                        gameTime + 1L
                ),
                "A satisfied forager must stop actively searching for food"
        );
        helper.assertFalse(
                RetoldAiControl.isControlledAsBy(
                        cow,
                        RetoldAiControlMode.SEARCH,
                        RetoldAiControlOwner.FOOD
                ),
                "Stopping the food search must release its control ownership"
        );
    }

    private static void cleanup(Cow cow) {
        RetoldAiControl.clear(cow);
        RetoldMobStates.remove(cow);
        cow.discard();
    }

    private static void buildFloor(GameTestHelper helper) {
        for (int x = 0; x <= 15; x++) {
            for (int z = 0; z <= 15; z++) {
                helper.setBlock(x, 1, z, Blocks.STONE);
            }
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
                        80,
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
