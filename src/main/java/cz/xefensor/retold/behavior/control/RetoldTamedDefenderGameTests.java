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
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.List;
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
        event.registerTest(
                Identifier.fromNamespaceAndPath(
                        Retold.MODID,
                        "ordinary_predators_defend_themselves_after_damage"
                ),
                new InlineGameTest(
                        testData,
                        RetoldTamedDefenderGameTests::ordinaryPredatorsDefendThemselvesAfterDamage
                )
        );
    }

    private static void ordinaryPredatorsDefendThemselvesAfterDamage(
            GameTestHelper helper
    ) {
        ServerLevel level = helper.getLevel();
        Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
        Zombie attacker = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, 8, 2, 5);
        Wolf tamedWolf = helper.spawnWithNoFreeWill(EntityTypes.WOLF, 2, 2, 3);
        Wolf ownerSafeWolf = helper.spawnWithNoFreeWill(EntityTypes.WOLF, 6, 2, 5);
        List<PathfinderMob> predators = List.of(
                helper.spawnWithNoFreeWill(EntityTypes.WOLF, 2, 2, 1),
                tamedWolf,
                helper.spawnWithNoFreeWill(EntityTypes.FOX, 2, 2, 5),
                helper.spawnWithNoFreeWill(EntityTypes.CAT, 4, 2, 1),
                helper.spawnWithNoFreeWill(EntityTypes.OCELOT, 4, 2, 3),
                helper.spawnWithNoFreeWill(EntityTypes.DOLPHIN, 4, 2, 5),
                helper.spawnWithNoFreeWill(EntityTypes.SPIDER, 6, 2, 1),
                helper.spawnWithNoFreeWill(EntityTypes.CAVE_SPIDER, 6, 2, 3)
        );

        tamedWolf.setTame(true, true);
        tamedWolf.setOwner(owner);
        tamedWolf.setOrderedToSit(false);
        ownerSafeWolf.setTame(true, true);
        ownerSafeWolf.setOwner(owner);
        ownerSafeWolf.setOrderedToSit(false);

        try {
            for (PathfinderMob predator : predators) {
                helper.assertTrue(
                        predator.hurtServer(
                                level,
                                level.damageSources().mobAttack(attacker),
                                1.0F
                        ),
                        "The attacker must deal real damage to " + predator.getType()
                );
                helper.assertTrue(
                        predator.getLastHurtByMob() == attacker,
                        "Real damage must record the attacker for " + predator.getType()
                );

                RetoldControlledCombatEvents.tickControlledCombat(
                        level,
                        predator,
                        level.getGameTime()
                );

                helper.assertTrue(
                        predator.getTarget() == attacker,
                        "A healthy combat-capable " + predator.getType()
                                + " must defend itself after damage"
                );
                helper.assertTrue(
                        RetoldAiControl.isControlledAs(
                                predator,
                                RetoldAiControlMode.ATTACK
                        ),
                        "Self-defense must own ATTACK control for " + predator.getType()
                );
                helper.assertTrue(
                        RetoldFactionTargetMemory.isOwnedByAny(
                                predator,
                                attacker,
                                RetoldTargetSource.RETALIATION
                        ),
                        "Self-defense must use retaliation ownership for "
                                + predator.getType()
                );
            }

            PathfinderMob continuingWolf = predators.getFirst();
            continuingWolf.setLastHurtByMob(null);
            RetoldControlledCombatEvents.tickControlledCombat(
                    level,
                    continuingWolf,
                    level.getGameTime() + 10L
            );
            helper.assertTrue(
                    continuingWolf.getTarget() == attacker
                            && RetoldFactionTargetMemory.isOwnedByAny(
                                    continuingWolf,
                                    attacker,
                                    RetoldTargetSource.RETALIATION
                            ),
                    "Owned self-defense must continue after one-time damage memory clears"
            );

            helper.assertTrue(
                    ownerSafeWolf.hurtServer(
                            level,
                            level.damageSources().playerAttack(owner),
                            1.0F
                    ),
                    "The owner must be able to deal real damage for the tame-safety boundary"
            );
            RetoldControlledCombatEvents.tickControlledCombat(
                    level,
                    ownerSafeWolf,
                    level.getGameTime()
            );
            helper.assertTrue(
                    ownerSafeWolf.getTarget() == null,
                    "A tamed Wolf must never retaliate against its own owner"
            );

            helper.succeed();
        } finally {
            for (PathfinderMob predator : predators) {
                RetoldAiControl.clear(predator);
                predator.discard();
            }

            RetoldAiControl.clear(ownerSafeWolf);
            ownerSafeWolf.discard();
            attacker.discard();
            owner.discard();
        }
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
