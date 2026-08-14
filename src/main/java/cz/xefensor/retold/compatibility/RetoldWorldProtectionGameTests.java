package cz.xefensor.retold.compatibility;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.api.world.RetoldWorldMutationType;
import cz.xefensor.retold.api.world.RetoldWorldProtection;
import cz.xefensor.retold.behavior.food.RetoldFoodBehaviorEvents;
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
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.function.Consumer;

public final class RetoldWorldProtectionGameTests {
    private static final Identifier EMPTY_STRUCTURE =
            Identifier.withDefaultNamespace("empty");
    private static final Identifier TEST_INTEGRATION =
            Identifier.fromNamespaceAndPath("examplemod", "test_claims");

    private RetoldWorldProtectionGameTests() {
    }

    public static void register(
            RegisterGameTestsEvent event,
            Holder<TestEnvironmentDefinition<?>> environment
    ) {
        TestData<Holder<TestEnvironmentDefinition<?>>> testData =
                new TestData<>(environment, EMPTY_STRUCTURE, 40, 0, true);

        event.registerTest(
                Identifier.fromNamespaceAndPath(
                        Retold.MODID,
                        "world_protection_rules_preserve_defaults_and_block_mutations"
                ),
                new InlineGameTest(
                        testData,
                        RetoldWorldProtectionGameTests::rulesPreserveDefaultsAndBlockMutations
                )
        );
    }

    private static void rulesPreserveDefaultsAndBlockMutations(
            GameTestHelper helper
    ) {
        ServerLevel level = helper.getLevel();
        long gameTime = level.getGameTime();
        Cow cow = helper.spawn(EntityTypes.COW, 4, 2, 4);
        BlockPos relativeForage = new BlockPos(5, 2, 4);
        BlockPos forage = helper.absolutePos(relativeForage);
        RetoldMobState state = RetoldMobStates.getOrCreate(cow, gameTime);
        state.setHunger(100);
        helper.setBlock(relativeForage, Blocks.GRASS_BLOCK);

        helper.assertTrue(
                RetoldWorldProtection.canMobBreak(level, forage, cow),
                "No installed protection rules must preserve Retold's default allow behavior"
        );

        RetoldWorldProtection.Registration registration =
                RetoldWorldProtection.register(
                        TEST_INTEGRATION,
                        context -> context.level() != level
                                || !context.pos().equals(forage)
                                || context.type() != RetoldWorldMutationType.MOB_BREAK
                );

        try {
            helper.assertFalse(
                    RetoldFoodBehaviorEvents.tryConsumeForageBlock(
                            level,
                            cow,
                            forage,
                            gameTime
                    ),
                    "A denied mob-break mutation must not consume forage"
            );
            helper.assertTrue(
                    level.getBlockState(forage).is(Blocks.GRASS_BLOCK),
                    "Denied forage must leave the world unchanged"
            );
            helper.assertTrue(
                    state.hunger() == 100,
                    "Denied forage must not grant food before the world mutation succeeds"
            );
            helper.assertTrue(
                    RetoldWorldProtection.canWorldModify(level, forage, null),
                    "Rules must be able to distinguish mutation categories"
            );

            boolean duplicateRejected = false;

            try {
                RetoldWorldProtection.register(TEST_INTEGRATION, ignored -> true);
            } catch (IllegalStateException expected) {
                duplicateRejected = true;
            }

            helper.assertTrue(
                    duplicateRejected,
                    "Duplicate integration identifiers must not replace an existing rule silently"
            );
        } finally {
            registration.close();
        }

        helper.assertTrue(
                RetoldFoodBehaviorEvents.tryConsumeForageBlock(
                        level,
                        cow,
                        forage,
                        gameTime
                ),
                "Closing a rule registration must restore standalone behavior"
        );
        helper.assertTrue(
                level.getBlockState(forage).is(Blocks.DIRT),
                "The existing grass-forage mutation must remain unchanged when allowed"
        );
        helper.assertTrue(
                state.hunger() < 100,
                "Allowed forage must retain its existing hunger relief"
        );
        helper.succeed();
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
