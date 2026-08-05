package cz.xefensor.retold.behavior.food;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.core.RetoldWeakBarriers;
import cz.xefensor.retold.behavior.profiles.RetoldMobState;
import cz.xefensor.retold.behavior.profiles.RetoldMobStates;
import cz.xefensor.retold.combat.RetoldFactionTargetGuards;
import cz.xefensor.retold.registry.RetoldAenderWood;

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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.gamerules.GameRules;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.function.Consumer;

public final class RetoldWeakBarrierGameTests {
    private static final Identifier EMPTY_STRUCTURE =
            Identifier.withDefaultNamespace("empty");

    private RetoldWeakBarrierGameTests() {
    }

    public static void register(
            RegisterGameTestsEvent event,
            Holder<TestEnvironmentDefinition<?>> environment
    ) {
        TestData<Holder<TestEnvironmentDefinition<?>>> testData =
                new TestData<>(
                        environment,
                        EMPTY_STRUCTURE,
                        40,
                        0,
                        true
                );

        registerTest(
                event,
                testData,
                "weak_mob_barrier_tag_matches_only_closed_wooden_barriers",
                RetoldWeakBarrierGameTests::weakMobBarrierTagMatchesOnlyClosedWoodenBarriers
        );
        registerTest(
                event,
                testData,
                "desperate_animals_break_barriers_with_drops_and_cooldown",
                RetoldWeakBarrierGameTests::desperateAnimalsBreakBarriersWithDropsAndCooldown
        );
        registerTest(
                event,
                testData,
                "only_wild_hunting_predators_break_barriers_quickly",
                RetoldWeakBarrierGameTests::onlyWildHuntingPredatorsBreakBarriersQuickly
        );
    }

    private static void weakMobBarrierTagMatchesOnlyClosedWoodenBarriers(
            GameTestHelper helper
    ) {
        helper.assertTrue(
                RetoldWeakBarriers.isBreakable(Blocks.OAK_FENCE.defaultBlockState()),
                "Wooden fences must be weak mob barriers"
        );
        helper.assertTrue(
                RetoldWeakBarriers.isBreakable(Blocks.OAK_FENCE_GATE.defaultBlockState()),
                "Closed wooden fence gates must be weak mob barriers"
        );
        helper.assertFalse(
                RetoldWeakBarriers.isBreakable(
                        Blocks.OAK_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.OPEN, true)
                ),
                "Open fence gates must not be attacked"
        );
        helper.assertTrue(
                RetoldWeakBarriers.isBreakable(
                        RetoldAenderWood.AENDER_FENCE.get().defaultBlockState()
                ),
                "Aender fences must inherit the weak-barrier rule"
        );
        helper.assertTrue(
                RetoldWeakBarriers.isBreakable(
                        RetoldAenderWood.AENDER_FENCE_GATE.get().defaultBlockState()
                ),
                "Closed Aender fence gates must inherit the weak-barrier rule"
        );
        helper.assertFalse(
                RetoldWeakBarriers.isBreakable(Blocks.OAK_DOOR.defaultBlockState()),
                "Wooden doors must remain outside the weak-barrier tag"
        );
        helper.assertFalse(
                RetoldWeakBarriers.isBreakable(Blocks.COBBLESTONE_WALL.defaultBlockState()),
                "Walls must remain outside the weak-barrier tag"
        );
        helper.succeed();
    }

    private static void desperateAnimalsBreakBarriersWithDropsAndCooldown(
            GameTestHelper helper
    ) {
        ServerLevel level = helper.getLevel();
        boolean originalMobGriefing = level.getGameRules().get(GameRules.MOB_GRIEFING);
        long startTime = level.getGameTime();
        BlockPos firstBarrier = new BlockPos(2, 2, 1);
        BlockPos secondBarrier = new BlockPos(1, 2, 2);
        BlockPos protectedBarrier = new BlockPos(5, 2, 1);
        var cow = helper.spawn(EntityTypes.COW, 1, 2, 1);
        var protectedCow = helper.spawn(EntityTypes.COW, 4, 2, 1);

        try {
            level.getGameRules().set(
                    GameRules.MOB_GRIEFING,
                    true,
                    level.getServer()
            );
            helper.setBlock(firstBarrier, Blocks.OAK_FENCE);
            helper.setBlock(protectedBarrier, Blocks.OAK_FENCE);

            RetoldMobState cowState = RetoldMobStates.getOrCreate(cow, startTime);
            cowState.setHunger(100);
            RetoldAiControl.clear(cow);

            RetoldWeakBarrierBehavior.tick(level, cow, startTime);
            RetoldWeakBarrierBehavior.tick(
                    level,
                    cow,
                    startTime + RetoldWeakBarrierBehavior.breakTicks(cow) - 1L
            );
            helper.assertBlockPresent(Blocks.OAK_FENCE, firstBarrier);

            RetoldWeakBarrierBehavior.tick(
                    level,
                    cow,
                    startTime + RetoldWeakBarrierBehavior.breakTicks(cow)
            );
            helper.assertBlockNotPresent(Blocks.OAK_FENCE, firstBarrier);
            helper.assertItemEntityPresent(
                    Blocks.OAK_FENCE.asItem(),
                    firstBarrier,
                    2.0D
            );

            helper.setBlock(secondBarrier, Blocks.OAK_FENCE);
            RetoldWeakBarrierBehavior.tick(
                    level,
                    cow,
                    startTime + RetoldWeakBarrierBehavior.breakTicks(cow) + 1L
            );
            helper.assertBlockPresent(Blocks.OAK_FENCE, secondBarrier);

            RetoldMobState protectedState = RetoldMobStates.getOrCreate(
                    protectedCow,
                    startTime
            );
            protectedState.setHunger(100);
            RetoldAiControl.clear(protectedCow);
            level.getGameRules().set(
                    GameRules.MOB_GRIEFING,
                    false,
                    level.getServer()
            );

            RetoldWeakBarrierBehavior.tick(level, protectedCow, startTime);
            RetoldWeakBarrierBehavior.tick(level, protectedCow, startTime + 200L);
            helper.assertBlockPresent(Blocks.OAK_FENCE, protectedBarrier);
            helper.succeed();
        } finally {
            level.getGameRules().set(
                    GameRules.MOB_GRIEFING,
                    originalMobGriefing,
                    level.getServer()
            );
            RetoldWeakBarrierBehavior.forget(cow);
            RetoldWeakBarrierBehavior.forget(protectedCow);
            RetoldMobStates.remove(cow);
            RetoldMobStates.remove(protectedCow);
            cow.discard();
            protectedCow.discard();
        }
    }

    private static void onlyWildHuntingPredatorsBreakBarriersQuickly(
            GameTestHelper helper
    ) {
        ServerLevel level = helper.getLevel();
        boolean originalMobGriefing = level.getGameRules().get(GameRules.MOB_GRIEFING);
        long startTime = level.getGameTime();
        BlockPos barrier = new BlockPos(2, 2, 1);
        var wildWolf = helper.spawn(EntityTypes.WOLF, 1, 2, 1);
        var prey = helper.spawn(EntityTypes.COW, 4, 2, 1);

        try {
            level.getGameRules().set(
                    GameRules.MOB_GRIEFING,
                    true,
                    level.getServer()
            );
            helper.setBlock(barrier, Blocks.OAK_FENCE);
            RetoldMobStates.getOrCreate(wildWolf, startTime).setHunger(100);
            RetoldAiControl.clear(wildWolf);
            RetoldFactionTargetGuards.setTargetIgnoringGuard(wildWolf, prey);

            RetoldWeakBarrierBehavior.tick(level, wildWolf, startTime);
            RetoldWeakBarrierBehavior.tick(
                    level,
                    wildWolf,
                    startTime + RetoldWeakBarrierBehavior.breakTicks(wildWolf) - 1L
            );
            helper.assertBlockPresent(Blocks.OAK_FENCE, barrier);
            RetoldWeakBarrierBehavior.tick(
                    level,
                    wildWolf,
                    startTime + RetoldWeakBarrierBehavior.breakTicks(wildWolf)
            );
            helper.assertBlockNotPresent(Blocks.OAK_FENCE, barrier);

            RetoldWeakBarrierBehavior.forget(wildWolf);
            RetoldMobStates.remove(wildWolf);
            wildWolf.discard();
            prey.discard();

            helper.setBlock(barrier, Blocks.OAK_FENCE);
            var tamedWolf = helper.spawn(EntityTypes.WOLF, 1, 2, 1);
            var secondPrey = helper.spawn(EntityTypes.COW, 4, 2, 1);
            tamedWolf.setTame(true, true);
            RetoldMobStates.getOrCreate(tamedWolf, startTime).setHunger(100);
            RetoldAiControl.clear(tamedWolf);
            RetoldFactionTargetGuards.setTargetIgnoringGuard(tamedWolf, secondPrey);

            RetoldWeakBarrierBehavior.tick(level, tamedWolf, startTime);
            RetoldWeakBarrierBehavior.tick(level, tamedWolf, startTime + 200L);
            helper.assertBlockPresent(Blocks.OAK_FENCE, barrier);

            RetoldWeakBarrierBehavior.forget(tamedWolf);
            RetoldMobStates.remove(tamedWolf);
            tamedWolf.discard();
            secondPrey.discard();
            helper.succeed();
        } finally {
            level.getGameRules().set(
                    GameRules.MOB_GRIEFING,
                    originalMobGriefing,
                    level.getServer()
            );
            RetoldWeakBarrierBehavior.forget(wildWolf);
            RetoldMobStates.remove(wildWolf);
            wildWolf.discard();
            prey.discard();
        }
    }

    private static void registerTest(
            RegisterGameTestsEvent event,
            TestData<Holder<TestEnvironmentDefinition<?>>> testData,
            String name,
            Consumer<GameTestHelper> test
    ) {
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
