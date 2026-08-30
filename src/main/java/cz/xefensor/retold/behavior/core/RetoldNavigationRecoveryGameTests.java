package cz.xefensor.retold.behavior.core;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.control.RetoldAiControlMode;
import cz.xefensor.retold.behavior.control.RetoldControlledCombatEvents;
import cz.xefensor.retold.combat.RetoldCombatTargets;
import cz.xefensor.retold.combat.RetoldFactionTargetMemory;
import cz.xefensor.retold.combat.RetoldTargetSource;

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
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.function.Consumer;

public final class RetoldNavigationRecoveryGameTests {
    private static final Identifier EMPTY_STRUCTURE =
            Identifier.withDefaultNamespace("empty");

    private RetoldNavigationRecoveryGameTests() {
    }

    public static void register(
            RegisterGameTestsEvent event,
            Holder<TestEnvironmentDefinition<?>> environment
    ) {
        registerTest(
                event,
                environment,
                "ground_navigation_detects_no_progress_and_recovers_combat",
                RetoldNavigationRecoveryGameTests::groundNavigationDetectsNoProgressAndRecoversCombat
        );
        registerTest(
                event,
                environment,
                "path_budget_deferral_does_not_mark_route_unreachable",
                RetoldNavigationRecoveryGameTests::pathBudgetDeferralDoesNotMarkRouteUnreachable
        );
        registerTest(
                event,
                environment,
                "urgent_navigation_recovery_retries_retaliation_memory",
                RetoldNavigationRecoveryGameTests::urgentNavigationRecoveryRetriesRetaliationMemory
        );
    }

    private static void groundNavigationDetectsNoProgressAndRecoversCombat(
            GameTestHelper helper
    ) {
        buildFloor(helper);

        ServerLevel level = helper.getLevel();
        Wolf wolf = helper.spawn(EntityTypes.WOLF, 2, 2, 2);
        Skeleton target = helper.spawnWithNoFreeWill(EntityTypes.SKELETON, 8, 2, 2);
        Skeleton alternateTarget = helper.spawnWithNoFreeWill(EntityTypes.SKELETON, 8, 2, 5);
        long gameTime = level.getGameTime() + 20_000L;

        try {
            RetoldAiControl.claim(wolf, RetoldAiControlMode.ATTACK, gameTime, 200);
            helper.assertTrue(
                    RetoldCombatTargets.applyAttackTarget(
                            wolf,
                            target,
                            RetoldTargetSource.BEHAVIOR_COMBAT
                    ),
                    "The fixture must establish an ordinary Retold combat target"
            );
            wolf.setOnGround(true);
            RetoldBehaviorMovement.MovementOutcome started =
                    RetoldBehaviorMovement.throttledMoveToWithOutcome(
                            wolf,
                            target,
                            1.05D,
                            gameTime,
                            6,
                            2.0D * 2.0D
                    );
            helper.assertTrue(
                    started == RetoldBehaviorMovement.MovementOutcome.MOVING,
                    "The no-progress fixture must begin with a real reachable path; outcome="
                            + started
            );

            for (int elapsed = 10; elapsed <= 40; elapsed += 10) {
                RetoldControlledCombatEvents.tickControlledCombat(
                        level,
                        wolf,
                        gameTime + elapsed
                );
            }

            helper.assertTrue(
                    wolf.getTarget() == null
                            && !RetoldAiControl.isControlledAs(
                            wolf,
                            RetoldAiControlMode.ATTACK
                    ),
                    "A ground attacker that makes no progress for 40 ticks must release "
                            + "ordinary combat instead of endlessly restarting the same path"
            );

            RetoldControlledCombatEvents.tickControlledCombat(
                    level,
                    wolf,
                    gameTime + 50L
            );
            helper.assertTrue(
                    wolf.getTarget() == alternateTarget,
                    "Ordinary recovery must suppress only the unreachable target so another eligible "
                            + "nearby enemy can be considered"
            );

            helper.succeed();
        } finally {
            RetoldBehaviorMovement.clearGroundPathState(wolf);
            RetoldAiControl.clear(wolf);
            wolf.discard();
            target.discard();
            alternateTarget.discard();
        }
    }

    private static void urgentNavigationRecoveryRetriesRetaliationMemory(
            GameTestHelper helper
    ) {
        buildFloor(helper);

        ServerLevel level = helper.getLevel();
        Wolf wolf = helper.spawn(EntityTypes.WOLF, 2, 2, 2);
        Skeleton attacker = helper.spawnWithNoFreeWill(EntityTypes.SKELETON, 12, 2, 2);
        long gameTime = level.getGameTime() + 25_000L;

        try {
            helper.assertTrue(
                    wolf.hurtServer(
                            level,
                            level.damageSources().mobAttack(attacker),
                            1.0F
                    ),
                    "The retaliation fixture must record real attacker damage"
            );

            for (int elapsed = 0; elapsed <= 40; elapsed += 10) {
                RetoldControlledCombatEvents.tickControlledCombat(
                        level,
                        wolf,
                        gameTime + elapsed
                );
            }

            helper.assertTrue(
                    wolf.getTarget() == null,
                    "Stuck retaliation must release its unusable live route during the urgent pause"
            );

            RetoldControlledCombatEvents.tickControlledCombat(
                    level,
                    wolf,
                    gameTime + 50L
            );
            helper.assertTrue(
                    wolf.getTarget() == null,
                    "Urgent navigation recovery must not immediately restart the same stuck route"
            );

            RetoldControlledCombatEvents.tickControlledCombat(
                    level,
                    wolf,
                    gameTime + 80L
            );
            helper.assertTrue(
                    wolf.getTarget() == attacker
                            && RetoldFactionTargetMemory.isOwnedByAny(
                            wolf,
                            attacker,
                            RetoldTargetSource.RETALIATION
                    ),
                    "After the shorter two-second pause, valid retaliation memory must be retried "
                            + "with its original source ownership"
            );

            helper.succeed();
        } finally {
            RetoldBehaviorMovement.clearGroundPathState(wolf);
            RetoldAiControl.clear(wolf);
            wolf.discard();
            attacker.discard();
        }
    }

    private static void pathBudgetDeferralDoesNotMarkRouteUnreachable(
            GameTestHelper helper
    ) {
        buildFloor(helper);

        ServerLevel level = helper.getLevel();
        Cow mover = helper.spawnWithNoFreeWill(EntityTypes.COW, 2, 2, 8);
        long gameTime = level.getGameTime() + 30_000L;
        double destinationY = mover.getY();
        double destinationZ = mover.getZ() + 2.0D;
        double deferredDestinationX = mover.getX() + 8.0D;

        try {
            RetoldAiControl.claim(mover, RetoldAiControlMode.SEARCH, gameTime, 200);

            for (int index = 0; index < 16; index++) {
                RetoldBehaviorMovement.throttledMoveToWithOutcome(
                        mover,
                        mover.getX() + 2.0D + index * 0.25D,
                        destinationY,
                        destinationZ,
                        1.0D,
                        gameTime,
                        1,
                        0.0D
                );
            }

            RetoldBehaviorMovement.MovementOutcome deferred =
                    RetoldBehaviorMovement.throttledMoveToWithOutcome(
                            mover,
                            deferredDestinationX,
                            destinationY,
                            destinationZ,
                            1.0D,
                            gameTime,
                            1,
                            0.0D
                    );
            helper.assertTrue(
                    deferred == RetoldBehaviorMovement.MovementOutcome.DEFERRED,
                    "The seventeenth path request in one tick must be a neutral budget deferral; outcome="
                            + deferred
            );

            RetoldBehaviorMovement.MovementOutcome nextTick =
                    RetoldBehaviorMovement.throttledMoveToWithOutcome(
                            mover,
                            deferredDestinationX,
                            destinationY,
                            destinationZ,
                            1.0D,
                            gameTime + 1L,
                            1,
                            0.0D
                    );
            helper.assertTrue(
                    nextTick != RetoldBehaviorMovement.MovementOutcome.DEFERRED
                            && nextTick != RetoldBehaviorMovement.MovementOutcome.UNREACHABLE,
                    "A budget-deferred route must remain eligible for a first real attempt next tick; outcome="
                            + nextTick
            );

            helper.succeed();
        } finally {
            RetoldBehaviorMovement.clearGroundPathState(mover);
            RetoldAiControl.clear(mover);
            mover.discard();
        }
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
        TestData<Holder<TestEnvironmentDefinition<?>>> testData = new TestData<>(
                environment,
                EMPTY_STRUCTURE,
                100,
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
