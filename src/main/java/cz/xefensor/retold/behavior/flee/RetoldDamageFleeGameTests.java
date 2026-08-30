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
import net.minecraft.tags.FluidTags;
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
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
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
                id("land_mob_flee_paths_do_not_enter_water"),
                new InlineGameTest(
                        testData,
                        RetoldDamageFleeGameTests::landMobFleePathsDoNotEnterWater
                )
        );
        event.registerTest(
                id("flee_routes_remain_committed_while_navigation_is_valid"),
                new InlineGameTest(
                        testData,
                        RetoldDamageFleeGameTests::fleeRoutesRemainCommittedWhileNavigationIsValid
                )
        );
        event.registerTest(
                id("flee_destinations_stay_in_escape_corridor"),
                new InlineGameTest(
                        testData,
                        RetoldDamageFleeGameTests::fleeDestinationsStayInEscapeCorridor
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

    private static void landMobFleePathsDoNotEnterWater(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        for (int x = 0; x <= 15; x++) {
            for (int z = 0; z <= 15; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
            }
        }

        var sheep = helper.spawn(EntityTypes.SHEEP, 8, 2, 8);

        for (int x = 3; x <= 7; x++) {
            for (int z = 7; z <= 9; z++) {
                helper.setBlock(new BlockPos(x, 2, z), Blocks.WATER);
            }
        }

        helper.runAfterDelay(2, () -> {
            Vec3 directDestination = helper.absoluteVec(new Vec3(2.5D, 2.0D, 8.5D));
            BlockPos destination = RetoldFleeMovement.chooseDestination(
                    sheep,
                    directDestination,
                    8.0D,
                    level.getGameTime()
            );

            helper.assertFalse(
                    level.getFluidState(destination).is(FluidTags.WATER),
                    "Land flight must replace a water destination with a dry alternative"
            );

            RetoldAiControl.claim(
                    sheep,
                    RetoldAiControlMode.FLEE,
                    level.getGameTime(),
                    40
            );
            boolean started = RetoldFleeMovement.moveTo(
                    sheep,
                    destination,
                    1.2D,
                    level.getGameTime(),
                    1,
                    1.0D
            );
            Path path = sheep.getNavigation().getPath();

            helper.assertTrue(
                    !started || path != null,
                    "A started land-flight route must expose its path for water validation"
            );

            if (!started) {
                helper.assertTrue(
                        sheep.getNavigation().isDone(),
                        "A rejected water-crossing route must leave navigation stopped"
                );
                helper.succeed();
                return;
            }

            for (int index = path.getNextNodeIndex(); index < path.getNodeCount(); index++) {
                BlockPos nodePos = path.getNodePos(index);
                helper.assertFalse(
                        level.getFluidState(nodePos).is(FluidTags.WATER)
                                || level.getFluidState(nodePos.below()).is(FluidTags.WATER),
                        "Land flight path node " + nodePos + " must not enter or cross water"
                );
            }

            helper.succeed();
        });
    }

    private static void fleeRoutesRemainCommittedWhileNavigationIsValid(
            GameTestHelper helper
    ) {
        ServerLevel level = helper.getLevel();

        for (int x = 0; x <= 20; x++) {
            for (int z = 0; z <= 20; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
            }
        }

        var sheep = helper.spawn(EntityTypes.SHEEP, 10, 2, 10);

        helper.runAfterDelay(2, () -> {
            try {
                long gameTime = level.getGameTime();
                Vec3 firstDirectDestination = helper.absoluteVec(
                        new Vec3(5.5D, 2.0D, 10.5D)
                );
                Vec3 slightlyAdjustedDestination = helper.absoluteVec(
                        new Vec3(5.5D, 2.0D, 11.5D)
                );
                BlockPos firstDestination = RetoldFleeMovement.chooseDestination(
                        sheep,
                        firstDirectDestination,
                        8.0D,
                        gameTime
                );

                RetoldAiControl.claim(
                        sheep,
                        RetoldAiControlMode.FLEE,
                        gameTime,
                        40
                );
                BlockPos start = sheep.blockPosition();
                Path firstPath = new Path(
                        List.of(
                                new Node(start.getX(), start.getY(), start.getZ()),
                                new Node(
                                        firstDestination.getX(),
                                        firstDestination.getY(),
                                        firstDestination.getZ()
                                )
                        ),
                        firstDestination,
                        true
                );

                RetoldAiControl.withNavigationBypass(
                        () -> sheep.getNavigation().moveTo(firstPath, 1.1D)
                );
                helper.assertTrue(
                        sheep.getNavigation().getPath() == firstPath,
                        "The flee-route fixture must begin its seeded navigation path"
                );

                BlockPos refreshedDestination = RetoldFleeMovement.chooseDestination(
                        sheep,
                        slightlyAdjustedDestination,
                        8.0D,
                        gameTime + 8
                );
                helper.assertValueEqual(
                        refreshedDestination,
                        firstDestination,
                        "A fleeing mob must retain its safe destination across ordinary think ticks"
                );

                helper.assertTrue(
                        RetoldFleeMovement.moveTo(
                                sheep,
                                refreshedDestination,
                                1.45D,
                                gameTime + 8,
                                1,
                                4.0D
                        ),
                        "A flee speed update must keep the existing route active"
                );
                helper.assertTrue(
                        sheep.getNavigation().getPath() == firstPath,
                        "Changing flee speed must not replace an unchanged navigation path"
                );

                helper.succeed();
            } finally {
                cleanup(sheep);
            }
        });
    }

    private static void fleeDestinationsStayInEscapeCorridor(
            GameTestHelper helper
    ) {
        ServerLevel level = helper.getLevel();

        for (int x = 0; x <= 20; x++) {
            for (int z = 0; z <= 20; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
            }
        }

        var sheep = helper.spawn(EntityTypes.SHEEP, 10, 2, 10);

        helper.runAfterDelay(2, () -> {
            try {
                long gameTime = level.getGameTime();
                Vec3 firstDirection = helper.absoluteVec(new Vec3(2.5D, 2.0D, 10.5D));
                Vec3 adjustedDirection = helper.absoluteVec(new Vec3(2.5D, 2.0D, 11.5D));
                BlockPos firstDestination = RetoldFleeMovement.chooseDestination(
                        sheep,
                        firstDirection,
                        8.0D,
                        gameTime
                );
                BlockPos refreshedDestination = RetoldFleeMovement.chooseDestination(
                        sheep,
                        adjustedDirection,
                        8.0D,
                        gameTime + 8
                );

                helper.assertValueEqual(
                        refreshedDestination,
                        firstDestination,
                        "Land flight must retain its corridor across ordinary think ticks"
                );
                Vec3 intendedDirection = firstDirection.subtract(sheep.position()).normalize();
                Vec3 chosenDirection = Vec3.atCenterOf(firstDestination)
                        .subtract(sheep.position());
                double lateralOffset = Math.abs(
                        intendedDirection.x * chosenDirection.z
                                - intendedDirection.z * chosenDirection.x
                );

                helper.assertTrue(
                        intendedDirection.dot(chosenDirection) > 3.0D,
                        "A clear flee destination must continue forward from the threat"
                );
                helper.assertTrue(
                        lateralOffset <= 1.0D,
                        "A clear flee destination must stay in a straight escape corridor"
                );
                helper.succeed();
            } finally {
                cleanup(sheep);
            }
        });
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
        RetoldFleeMovement.clearDestination(mob);
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
