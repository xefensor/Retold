package cz.xefensor.retold.behavior.control;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.behavior.core.RetoldBehaviorTargets;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.function.Consumer;

public final class RetoldTamedDefenderGameTests {
    private static final Identifier EMPTY_STRUCTURE =
            Identifier.withDefaultNamespace("empty");

    private RetoldTamedDefenderGameTests() {
    }

    public static void register(
            RegisterGameTestsEvent event,
            Holder<TestEnvironmentDefinition<?>> environment
    ) {
        TestData<Holder<TestEnvironmentDefinition<?>>> testData = new TestData<>(
                environment,
                EMPTY_STRUCTURE,
                100,
                0,
                true
        );
        Identifier testId = Identifier.fromNamespaceAndPath(
                Retold.MODID,
                "tamed_wolf_defends_owner_and_attacks_owner_target"
        );

        event.registerTest(testId, new InlineGameTest(
                testData,
                RetoldTamedDefenderGameTests::tamedWolfDefendsOwnerAndAttacksOwnerTarget
        ));
    }

    private static void tamedWolfDefendsOwnerAndAttacksOwnerTarget(
            GameTestHelper helper
    ) {
        ServerLevel level = helper.getLevel();
        Player owner = helper.makeMockPlayer(
                GameType.SURVIVAL
        );
        Wolf wolf = helper.spawn(EntityTypes.WOLF, 2, 2, 2);
        Zombie ownerAttacker = helper.spawn(EntityTypes.ZOMBIE, 3, 2, 2);
        Drowned ownerTarget = helper.spawn(EntityTypes.DROWNED, 4, 2, 2);
        Skeleton previousTarget = helper.spawn(EntityTypes.SKELETON, 5, 2, 2);

        try {
            wolf.setTame(true, true);
            wolf.setOwner(owner);
            wolf.setOrderedToSit(false);

            helper.hurt(
                    owner,
                    level.damageSources().mobAttack(ownerAttacker),
                    1.0F
            );
            helper.assertTrue(
                    owner.getLastHurtByMob() == ownerAttacker,
                    "The real mob hit must update the owner's attacker memory"
            );

            RetoldControlledCombatEvents.tickControlledCombat(
                    level,
                    wolf,
                    level.getGameTime()
            );
            assertOwnerDefenseTarget(
                    helper,
                    wolf,
                    ownerAttacker,
                    "A standing tamed Wolf must defend its owner"
            );

            RetoldBehaviorTargets.clearTargetAndAggression(
                    wolf,
                    ownerAttacker,
                    true
            );
            RetoldAiControl.clear(wolf);
            ownerAttacker.discard();

            helper.assertTrue(
                    RetoldCombatTargets.applyAttackTarget(
                            wolf,
                            previousTarget,
                            RetoldTargetSource.BEHAVIOR_COMBAT
                    ),
                    "The regression setup must begin with an ordinary combat target"
            );
            RetoldAiControl.claim(
                    wolf,
                    RetoldAiControlMode.ATTACK,
                    level.getGameTime(),
                    80
            );
            owner.setLastHurtMob(ownerTarget);

            RetoldControlledCombatEvents.tickControlledCombat(
                    level,
                    wolf,
                    level.getGameTime()
            );
            assertOwnerDefenseTarget(
                    helper,
                    wolf,
                    ownerTarget,
                    "A standing tamed Wolf must prioritize the mob its owner attacked"
            );

            helper.succeed();
        } finally {
            RetoldAiControl.clear(wolf);
            wolf.discard();
            ownerAttacker.discard();
            ownerTarget.discard();
            previousTarget.discard();
            owner.discard();
        }
    }

    private static void assertOwnerDefenseTarget(
            GameTestHelper helper,
            Wolf wolf,
            LivingEntity expectedTarget,
            String message
    ) {
        helper.assertTrue(wolf.getTarget() == expectedTarget, message);
        helper.assertTrue(
                RetoldAiControl.isControlledAs(
                        wolf,
                        RetoldAiControlMode.ATTACK
                ),
                message + " under ATTACK control"
        );
        helper.assertTrue(
                RetoldFactionTargetMemory.isOwnedByAny(
                        wolf,
                        expectedTarget,
                        RetoldTargetSource.OWNER_DEFENSE
                ),
                message + " with owner-defense target ownership"
        );
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
