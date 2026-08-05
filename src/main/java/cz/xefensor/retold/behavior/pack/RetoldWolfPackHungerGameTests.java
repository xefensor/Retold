package cz.xefensor.retold.behavior.pack;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.control.RetoldAiControlMode;
import cz.xefensor.retold.behavior.control.RetoldAiControlOwner;
import cz.xefensor.retold.behavior.hunting.RetoldControlledHuntingEvents;
import cz.xefensor.retold.behavior.profiles.RetoldMobStates;
import cz.xefensor.retold.combat.RetoldCombatTargets;
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
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.List;
import java.util.function.Consumer;

public final class RetoldWolfPackHungerGameTests {
    private static final Identifier EMPTY_STRUCTURE =
            Identifier.withDefaultNamespace("empty");

    private RetoldWolfPackHungerGameTests() {
    }

    public static void register(
            RegisterGameTestsEvent event,
            Holder<TestEnvironmentDefinition<?>> environment
    ) {
        registerTest(
                event,
                environment,
                "satisfied_predator_releases_food_hunt",
                RetoldWolfPackHungerGameTests::satisfiedPredatorReleasesFoodHunt
        );
        registerTest(
                event,
                environment,
                "hungry_wolf_takes_over_satisfied_pack",
                RetoldWolfPackHungerGameTests::hungryWolfTakesOverSatisfiedPack
        );
    }

    private static void satisfiedPredatorReleasesFoodHunt(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Wolf wolf = helper.spawn(EntityTypes.WOLF, 1, 2, 1);
        Sheep sheep = helper.spawn(EntityTypes.SHEEP, 4, 2, 1);
        Zombie attacker = helper.spawn(EntityTypes.ZOMBIE, 6, 2, 1);
        long gameTime = level.getGameTime() + 3_000L;

        try {
            RetoldMobStates.getOrCreate(wolf, gameTime).setHunger(100);
            helper.assertTrue(
                    RetoldControlledHuntingEvents.tryStartHunt(
                            level,
                            wolf,
                            gameTime
                    ),
                    "A hungry Wolf must begin an ordinary food hunt"
            );
            helper.assertTrue(
                    wolf.getTarget() == sheep
                            && RetoldAiControl.isControlledAs(
                            wolf,
                            RetoldAiControlMode.HUNT
                    ),
                    "The Wolf hunt setup must retain its prey and hunt control"
            );

            RetoldMobStates.getOrCreate(wolf, gameTime).setHunger(0);
            helper.assertTrue(
                    RetoldControlledHuntingEvents.endSatisfiedHuntIfNeeded(
                            wolf,
                            gameTime + 1L
                    ),
                    "A predator below its hunt threshold must release an active food hunt"
            );
            helper.assertTrue(
                    wolf.getTarget() == null
                            && !wolf.isSprinting()
                            && !RetoldAiControl.isControlledAs(
                            wolf,
                            RetoldAiControlMode.HUNT
                    ),
                    "Releasing a satisfied hunt must clear prey, sprinting, and hunt control"
            );

            helper.assertTrue(
                    RetoldCombatTargets.applyAttackTarget(
                            wolf,
                            attacker,
                            RetoldTargetSource.RETALIATION
                    ),
                    "The satisfied Wolf must accept a retaliation target"
            );
            helper.assertTrue(
                    RetoldAiControl.tryClaim(
                            wolf,
                            RetoldAiControlMode.HUNT,
                            RetoldAiControlOwner.SOLO_HUNTING,
                            gameTime + 2L,
                            80
                    ),
                    "The safety regression setup must retain stale hunt control"
            );
            helper.assertFalse(
                    RetoldControlledHuntingEvents.endSatisfiedHuntIfNeeded(
                            wolf,
                            gameTime + 3L
                    ),
                    "Satisfied-hunt cleanup must not erase urgent retaliation"
            );
            helper.assertTrue(
                    wolf.getTarget() == attacker,
                    "The retaliation target must survive satisfied-hunt cleanup"
            );
        } finally {
            cleanup(wolf);
            cleanup(sheep);
            cleanup(attacker);
        }

        helper.succeed();
    }

    private static void hungryWolfTakesOverSatisfiedPack(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Wolf satisfiedLeader = helper.spawn(EntityTypes.WOLF, 1, 2, 1);
        Wolf hungryMember = helper.spawn(EntityTypes.WOLF, 2, 2, 1);
        long gameTime = level.getGameTime() + 3_000L;
        RetoldPackParty party = RetoldPackParties.createParty(
                satisfiedLeader,
                List.of(hungryMember),
                satisfiedLeader.blockPosition(),
                gameTime
        );
        Sheep sheep = null;

        try {
            RetoldMobStates.getOrCreate(satisfiedLeader, gameTime).setHunger(0);
            RetoldMobStates.getOrCreate(hungryMember, gameTime).setHunger(100);
            helper.assertTrue(
                    RetoldPackControl.claim(
                            satisfiedLeader,
                            RetoldAiControlMode.HUNT,
                            gameTime,
                            100
                    ),
                    "The pack regression setup must begin with the satisfied Wolf as leader"
            );

            RetoldPackUpdates.updateLeaderParty(
                    level,
                    satisfiedLeader,
                    party,
                    gameTime + 1L
            );
            helper.assertTrue(
                    RetoldPackParties.leaderOf(hungryMember) == hungryMember
                            && RetoldPackParties.leaderOf(satisfiedLeader) == hungryMember
                            && RetoldPackParties.partyOf(hungryMember) == party,
                    "The hungry Wolf must take leadership when the previous leader is satisfied"
            );
            helper.assertTrue(
                    RetoldAiControl.isControlledAsBy(
                            hungryMember,
                            RetoldAiControlMode.SEARCH,
                            RetoldAiControlOwner.PACK_HUNTING
                    )
                            && !satisfiedLeader.isSprinting(),
                    "Leadership transfer must start a hungry search and stop the satisfied leader"
            );

            sheep = helper.spawn(EntityTypes.SHEEP, 4, 2, 1);
            helper.assertFalse(
                    RetoldPackSenses.isValidPartyPrey(
                            satisfiedLeader,
                            sheep,
                            gameTime + 2L
                    ),
                    "A satisfied pack member must not validate food prey"
            );
            helper.assertTrue(
                    RetoldPackSenses.isValidPartyPrey(
                            hungryMember,
                            sheep,
                            gameTime + 2L
                    ),
                    "A hungry pack member must continue validating suitable food prey"
            );
        } finally {
            PathfinderMob currentLeader = RetoldPackParties.leaderOf(hungryMember);

            if (currentLeader != null) {
                RetoldPackLifecycle.dissolveParty(
                        currentLeader,
                        party,
                        true
                );
            } else {
                RetoldPackParties.clearMappings(
                        satisfiedLeader,
                        party
                );
            }

            cleanup(satisfiedLeader);
            cleanup(hungryMember);
            cleanup(sheep);
        }

        helper.succeed();
    }

    private static void cleanup(PathfinderMob mob) {
        if (mob == null) {
            return;
        }

        RetoldAiControl.clear(mob);
        RetoldMobStates.remove(mob);
        mob.discard();
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Retold.MODID, path);
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
                        40,
                        0,
                        true
                );

        event.registerTest(
                id(name),
                new InlineGameTest(testData, test)
        );
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
