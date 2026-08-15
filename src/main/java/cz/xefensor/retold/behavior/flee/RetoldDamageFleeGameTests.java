package cz.xefensor.retold.behavior.flee;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.control.RetoldAiControlMode;
import cz.xefensor.retold.behavior.control.RetoldAiControlOwner;
import cz.xefensor.retold.behavior.control.RetoldAiPriorities;
import cz.xefensor.retold.behavior.profiles.RetoldMobStates;
import cz.xefensor.retold.combat.RetoldCombatTargets;
import cz.xefensor.retold.combat.RetoldFactionTargetMemory;
import cz.xefensor.retold.combat.RetoldTargetSource;

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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.List;
import java.util.function.Consumer;

public final class RetoldDamageFleeGameTests {
    private static final Identifier EMPTY_STRUCTURE =
            Identifier.withDefaultNamespace("empty");

    private RetoldDamageFleeGameTests() {
    }

    public static void register(
            RegisterGameTestsEvent event,
            Holder<TestEnvironmentDefinition<?>> environment
    ) {
        TestData<Holder<TestEnvironmentDefinition<?>>> testData =
                new TestData<>(
                        environment,
                        EMPTY_STRUCTURE,
                        260,
                        0,
                        true
                );

        event.registerTest(
                id("passive_mobs_flee_every_successful_damage_source"),
                new InlineGameTest(
                        testData,
                        RetoldDamageFleeGameTests::passiveMobsFleeEverySuccessfulDamageSource
                )
        );
        event.registerTest(
                id("badly_wounded_wild_predators_flee_attackers"),
                new InlineGameTest(
                        testData,
                        RetoldDamageFleeGameTests::badlyWoundedWildPredatorsFleeAttackers
                )
        );
        event.registerTest(
                id("wounded_predator_flee_respects_threshold_and_exemptions"),
                new InlineGameTest(
                        testData,
                        RetoldDamageFleeGameTests::woundedPredatorFleeRespectsThresholdAndExemptions
                )
        );
        event.registerTest(
                id("wounded_predator_flee_lasts_ten_seconds"),
                new InlineGameTest(
                        testData,
                        RetoldDamageFleeGameTests::woundedPredatorFleeLastsTenSeconds
                )
        );
    }

    private static void passiveMobsFleeEverySuccessfulDamageSource(
            GameTestHelper helper
    ) {
        ServerLevel level = helper.getLevel();
        var sheep = helper.spawn(EntityTypes.SHEEP, 2, 2, 2);
        var cow = helper.spawn(EntityTypes.COW, 2, 2, 4);
        var pig = helper.spawn(EntityTypes.PIG, 2, 2, 6);
        var salmon = helper.spawn(EntityTypes.SALMON, 2, 2, 8);
        var wolf = helper.spawn(EntityTypes.WOLF, 5, 2, 6);
        var zombie = helper.spawn(EntityTypes.ZOMBIE, 5, 2, 2);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Vec3 playerPosition = helper.absoluteVec(new Vec3(5.5D, 2.0D, 4.5D));

        player.snapTo(
                playerPosition.x(),
                playerPosition.y(),
                playerPosition.z(),
                0.0F,
                0.0F
        );
        level.addFreshEntity(player);
        helper.setBlock(new BlockPos(2, 2, 8), Blocks.WATER);

        try {
            helper.assertTrue(
                    sheep.hurtServer(
                            level,
                            level.damageSources().mobAttack(zombie),
                            1.0F
                    ),
                    "The Zombie must deal real damage to the Sheep"
            );
            assertFleeing(helper, sheep, "A Sheep must flee a damaging Zombie");

            helper.assertTrue(
                    cow.hurtServer(
                            level,
                            level.damageSources().playerAttack(player),
                            1.0F
                    ),
                    "The Player must deal real damage to the Cow"
            );
            assertFleeing(helper, cow, "A Cow must flee a damaging Player");

            helper.assertTrue(
                    pig.hurtServer(
                            level,
                            level.damageSources().generic(),
                            1.0F
                    ),
                    "Source-less damage must reduce the Pig's health"
            );
            assertFleeing(helper, pig, "A Pig must panic after source-less damage");

            helper.assertTrue(
                    salmon.hurtServer(
                            level,
                            level.damageSources().generic(),
                            1.0F
                    ),
                    "Source-less damage must reduce the Salmon's health"
            );
            assertFleeing(helper, salmon, "An ordinary fish must panic after damage");

            helper.assertTrue(
                    wolf.hurtServer(
                            level,
                            level.damageSources().mobAttack(zombie),
                            1.0F
                    ),
                    "The Zombie must deal real damage to the Wolf"
            );
            helper.assertFalse(
                    RetoldAiControl.isControlledAs(wolf, RetoldAiControlMode.FLEE),
                    "A combat-capable predator must retain its retaliation behavior after an ordinary hit"
            );
        } finally {
            level.players().remove(player);
            player.discard();
            cleanup(sheep);
            cleanup(cow);
            cleanup(pig);
            cleanup(salmon);
            cleanup(wolf);
            cleanup(zombie);
        }

        helper.succeed();
    }

    private static void badlyWoundedWildPredatorsFleeAttackers(
            GameTestHelper helper
    ) {
        ServerLevel level = helper.getLevel();
        var attacker = helper.spawn(EntityTypes.ZOMBIE, 7, 2, 7);
        List<PathfinderMob> predators = List.of(
                helper.spawn(EntityTypes.WOLF, 2, 2, 2),
                helper.spawn(EntityTypes.FOX, 2, 2, 4),
                helper.spawn(EntityTypes.CAT, 2, 2, 6),
                helper.spawn(EntityTypes.OCELOT, 2, 2, 8),
                helper.spawn(EntityTypes.DOLPHIN, 4, 2, 2),
                helper.spawn(EntityTypes.SPIDER, 4, 2, 4),
                helper.spawn(EntityTypes.CAVE_SPIDER, 4, 2, 6)
        );

        attacker.setNoAi(true);

        try {
            Wolf wolf = (Wolf) predators.getFirst();

            helper.assertTrue(
                    RetoldCombatTargets.applyAttackTarget(
                            wolf,
                            attacker,
                            RetoldTargetSource.RETALIATION
                    ),
                    "The regression setup must give the Wolf an owned retaliation target"
            );
            RetoldAiControl.claim(
                    wolf,
                    RetoldAiControlMode.ATTACK,
                    level.getGameTime(),
                    80
            );

            for (PathfinderMob predator : predators) {
                woundBelowThreshold(helper, level, predator, attacker);
                assertWoundedPredatorFleeing(
                        helper,
                        predator,
                        "A badly wounded wild " + predator.getType() + " must flee its attacker"
                );
            }

            helper.assertTrue(
                    wolf.getTarget() == null,
                    "Wounded flight must clear the Wolf's ordinary retaliation target"
            );
            helper.assertFalse(
                    RetoldFactionTargetMemory.isOwnedByAny(
                            wolf,
                            attacker,
                            RetoldTargetSource.RETALIATION
                    ),
                    "Wounded flight must release Retold retaliation ownership"
            );
        } finally {
            cleanup(attacker);
            predators.forEach(RetoldDamageFleeGameTests::cleanup);
        }

        helper.succeed();
    }

    private static void woundedPredatorFleeRespectsThresholdAndExemptions(
            GameTestHelper helper
    ) {
        ServerLevel level = helper.getLevel();
        var attacker = helper.spawn(EntityTypes.ZOMBIE, 7, 2, 7);
        Wolf boundaryWolf = helper.spawn(EntityTypes.WOLF, 2, 2, 2);
        Wolf tamedWolf = helper.spawn(EntityTypes.WOLF, 2, 2, 4);
        Wolf territoryWolf = helper.spawn(EntityTypes.WOLF, 2, 2, 6);
        var undead = helper.spawn(EntityTypes.ZOMBIE, 4, 2, 2);
        var boss = helper.spawn(EntityTypes.WITHER, 4, 4, 5);

        attacker.setNoAi(true);
        undead.setNoAi(true);
        boss.setNoAi(true);
        boss.setInvulnerableTicks(0);
        tamedWolf.setTame(true, true);

        try {
            setHealthForExactThresholdAfterHit(boundaryWolf, 1.0F);
            damage(helper, level, boundaryWolf, attacker, 1.0F);
            assertNotWoundedPredatorFleeing(
                    helper,
                    boundaryWolf,
                    "A wild predator at exactly 25% health must not enter wounded flight"
            );

            setHealthForBelowThresholdAfterHit(tamedWolf, 1.0F);
            damage(helper, level, tamedWolf, attacker, 1.0F);
            assertNotWoundedPredatorFleeing(
                    helper,
                    tamedWolf,
                    "A tamed defender must remain exempt from wounded-predator flight"
            );

            helper.assertTrue(
                    RetoldAiControl.tryClaim(
                            territoryWolf,
                            RetoldAiControlMode.TERRITORY,
                            RetoldAiControlOwner.TERRITORY,
                            RetoldAiPriorities.TERRITORY,
                            "wounded_flee_test",
                            level.getGameTime(),
                            80
                    ),
                    "The regression setup must establish active territory duty"
            );
            setHealthForBelowThresholdAfterHit(territoryWolf, 1.0F);
            damage(helper, level, territoryWolf, attacker, 1.0F);
            assertNotWoundedPredatorFleeing(
                    helper,
                    territoryWolf,
                    "Active territory duty must remain exempt from wounded flight"
            );
            helper.assertTrue(
                    RetoldAiControl.isControlledAsBy(
                            territoryWolf,
                            RetoldAiControlMode.TERRITORY,
                            RetoldAiControlOwner.TERRITORY
                    ),
                    "Wounded-flight evaluation must preserve territory ownership"
            );

            setHealthForBelowThresholdAfterHit(undead, 1.0F);
            damage(helper, level, undead, attacker, 1.0F);
            assertNotWoundedPredatorFleeing(
                    helper,
                    undead,
                    "Mindless Undead must keep fighting instead of using wounded flight"
            );

            setHealthForBelowThresholdAfterHit(boss, 1.0F);
            damage(helper, level, boss, boundaryWolf, 1.0F);
            assertNotWoundedPredatorFleeing(
                    helper,
                    boss,
                    "Bosses must remain exempt from ordinary wounded-predator flight"
            );
        } finally {
            cleanup(attacker);
            cleanup(boundaryWolf);
            cleanup(tamedWolf);
            cleanup(territoryWolf);
            cleanup(undead);
            cleanup(boss);
        }

        helper.succeed();
    }

    private static void woundedPredatorFleeLastsTenSeconds(
            GameTestHelper helper
    ) {
        ServerLevel level = helper.getLevel();
        Wolf wolf = helper.spawn(EntityTypes.WOLF, 2, 2, 2);
        var attacker = helper.spawn(EntityTypes.ZOMBIE, 6, 2, 2);

        attacker.setNoAi(true);
        woundBelowThreshold(helper, level, wolf, attacker);
        assertWoundedPredatorFleeing(
                helper,
                wolf,
                "Wounded flight must begin immediately after the threshold-crossing hit"
        );
        wolf.setNoAi(true);

        helper.runAfterDelay(195, () -> assertWoundedPredatorFleeing(
                helper,
                wolf,
                "Wounded flight must remain active just before ten seconds"
        ));
        helper.runAfterDelay(210, () -> {
            try {
                assertNotWoundedPredatorFleeing(
                        helper,
                        wolf,
                        "Wounded flight must end after its ten-second memory"
                );
                helper.assertFalse(
                        RetoldAiControl.isControlledAs(wolf, RetoldAiControlMode.FLEE),
                        "Expired wounded flight must release FLEE control"
                );
                helper.succeed();
            } finally {
                cleanup(wolf);
                cleanup(attacker);
            }
        });
    }

    private static void woundBelowThreshold(
            GameTestHelper helper,
            ServerLevel level,
            PathfinderMob predator,
            Mob attacker
    ) {
        setHealthForBelowThresholdAfterHit(predator, 1.0F);
        damage(helper, level, predator, attacker, 1.0F);
    }

    private static void setHealthForBelowThresholdAfterHit(
            PathfinderMob mob,
            float damage
    ) {
        mob.setHealth(mob.getMaxHealth() * 0.25F + damage * 0.5F);
    }

    private static void setHealthForExactThresholdAfterHit(
            PathfinderMob mob,
            float damage
    ) {
        mob.setHealth(mob.getMaxHealth() * 0.25F + damage);
    }

    private static void damage(
            GameTestHelper helper,
            ServerLevel level,
            PathfinderMob victim,
            Mob attacker,
            float amount
    ) {
        helper.assertTrue(
                victim.hurtServer(
                        level,
                        level.damageSources().mobAttack(attacker),
                        amount
                ),
                "The threshold test must deal real health damage to " + victim.getType()
        );
    }

    private static void assertWoundedPredatorFleeing(
            GameTestHelper helper,
            PathfinderMob predator,
            String message
    ) {
        helper.assertTrue(
                RetoldControlledFleeEvents.isWoundedPredatorFleeing(predator)
                        && RetoldAiControl.isControlledAsBy(
                        predator,
                        RetoldAiControlMode.FLEE,
                        RetoldAiControlOwner.FLEEING
                )
                        && predator.isSprinting(),
                message
                        + " (memory="
                        + RetoldControlledFleeEvents.isWoundedPredatorFleeing(predator)
                        + ", control=" + RetoldAiControl.getMode(predator)
                        + ", owner=" + RetoldAiControl.getOwner(predator)
                        + ", removed=" + predator.isRemoved()
                        + ", health=" + predator.getHealth() + ")"
        );
    }

    private static void assertNotWoundedPredatorFleeing(
            GameTestHelper helper,
            PathfinderMob predator,
            String message
    ) {
        helper.assertFalse(
                RetoldControlledFleeEvents.isWoundedPredatorFleeing(predator),
                message
        );
    }

    private static void assertFleeing(
            GameTestHelper helper,
            PathfinderMob mob,
            String message
    ) {
        helper.assertTrue(
                RetoldAiControl.isControlledAs(mob, RetoldAiControlMode.FLEE)
                        && mob.isSprinting(),
                message
        );
    }

    private static void cleanup(PathfinderMob mob) {
        RetoldAiControl.clear(mob);
        RetoldMobStates.remove(mob);
        mob.discard();
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
