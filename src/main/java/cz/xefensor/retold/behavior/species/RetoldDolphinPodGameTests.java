package cz.xefensor.retold.behavior.species;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.control.RetoldAiControlMode;
import cz.xefensor.retold.behavior.control.RetoldAiControlOwner;
import cz.xefensor.retold.behavior.control.RetoldAiPriorities;
import cz.xefensor.retold.behavior.profiles.RetoldMobState;
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
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.animal.dolphin.Dolphin;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.function.Consumer;

public final class RetoldDolphinPodGameTests {
    private static final Identifier EMPTY_STRUCTURE =
            Identifier.withDefaultNamespace("empty");

    private RetoldDolphinPodGameTests() {
    }

    public static void register(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
                id("isolated_dolphin_pod_defense"),
                new TestEnvironmentDefinition.AllOf()
        );
        TestData<Holder<TestEnvironmentDefinition<?>>> testData = new TestData<>(
                environment,
                EMPTY_STRUCTURE,
                120,
                0,
                true
        );

        event.registerTest(
                id("dolphins_collectively_defend_attacked_podmates"),
                new InlineGameTest(
                        testData,
                        RetoldDolphinPodGameTests::collectivelyDefendAttackedPodmates
                )
        );
    }

    private static void collectivelyDefendAttackedPodmates(GameTestHelper helper) {
        buildWaterArena(helper);
        ServerLevel level = helper.getLevel();
        Dolphin victim = helper.spawn(EntityTypes.DOLPHIN, 4, 3, 5);
        Dolphin recruit = helper.spawn(EntityTypes.DOLPHIN, 7, 3, 5);
        Dolphin busyPodmate = helper.spawn(EntityTypes.DOLPHIN, 8, 3, 7);
        Drowned attacker = helper.spawn(EntityTypes.DROWNED, 5, 3, 5);
        Drowned busyTarget = helper.spawn(EntityTypes.DROWNED, 9, 3, 7);
        long gameTime = level.getGameTime();

        try {
            RetoldMobState recruitState = RetoldMobStates.getOrCreate(recruit, gameTime);
            recruitState.setHunger(0);

            helper.assertTrue(
                    RetoldCombatTargets.applyAttackTarget(
                            busyPodmate,
                            busyTarget,
                            RetoldTargetSource.RETALIATION
                    ),
                    "The busy-podmate fixture must begin with an unrelated urgent target"
            );
            helper.assertTrue(
                    RetoldAiControl.tryClaim(
                            busyPodmate,
                            RetoldAiControlMode.ATTACK,
                            RetoldAiControlOwner.COMBAT,
                            RetoldAiPriorities.ATTACK,
                            "dolphin_test_existing_retaliation",
                            gameTime,
                            100
                    ),
                    "The busy-podmate fixture must own its existing retaliation"
            );

            float healthBeforeDamage = victim.getHealth();
            helper.assertTrue(
                    victim.hurtServer(
                            level,
                            level.damageSources().mobAttack(attacker),
                            1.0F
                    ),
                    "The Drowned must deal real damage to the pod member"
            );
            helper.assertTrue(
                    victim.getHealth() < healthBeforeDamage,
                    "Pod defense must be triggered only after successful health damage"
            );
            helper.assertTrue(
                    victim.getTarget() == attacker
                            && RetoldFactionTargetMemory.isOwnedByAny(
                            victim,
                            attacker,
                            RetoldTargetSource.RETALIATION
                    )
                            && RetoldAiControl.isControlledAsBy(
                            victim,
                            RetoldAiControlMode.ATTACK,
                            RetoldAiControlOwner.AQUATIC_POD
                    ),
                    "The damaged Dolphin must own direct retaliation against its attacker"
            );
            helper.assertTrue(
                    recruit.getTarget() == attacker
                            && RetoldFactionTargetMemory.isOwnedByAny(
                            recruit,
                            attacker,
                            RetoldTargetSource.FACTION_ASSIST
                    )
                            && RetoldAiControl.isControlledAsBy(
                            recruit,
                            RetoldAiControlMode.ATTACK,
                            RetoldAiControlOwner.AQUATIC_POD
                    ),
                    "A nearby available Dolphin must join collective pod defense"
            );
            helper.assertValueEqual(
                    recruitState.hunger(),
                    0,
                    "Collective defense must not require or consume hunger drive"
            );
            helper.assertTrue(
                    busyPodmate.getTarget() == busyTarget
                            && RetoldAiControl.isControlledBy(
                            busyPodmate,
                            RetoldAiControlOwner.COMBAT
                    ),
                    "A podmate with an unrelated urgent target must not be redirected"
            );

            attacker.discard();

            for (int offset = 1; offset <= 20; offset++) {
                RetoldDolphinPodEvents.tick(level, victim, gameTime + offset);
                RetoldDolphinPodEvents.tick(level, recruit, gameTime + offset);
            }

            helper.assertTrue(
                    victim.getTarget() == null
                            && recruit.getTarget() == null
                            && !RetoldAiControl.isControlledBy(
                            victim,
                            RetoldAiControlOwner.AQUATIC_POD
                    )
                            && !RetoldAiControl.isControlledBy(
                            recruit,
                            RetoldAiControlOwner.AQUATIC_POD
                    ),
                    "Pod-defense ownership and targets must clear after the attacker is gone"
            );
            helper.succeed();
        } finally {
            cleanup(victim, recruit, busyPodmate, attacker, busyTarget);
        }
    }

    private static void buildWaterArena(GameTestHelper helper) {
        for (int x = 0; x <= 14; x++) {
            for (int z = 0; z <= 14; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.GLASS);

                for (int y = 2; y <= 6; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.WATER);
                }
            }
        }
    }

    private static void cleanup(PathfinderMob... mobs) {
        for (PathfinderMob mob : mobs) {
            if (mob == null) {
                continue;
            }

            RetoldAiControl.clear(mob);
            RetoldMobStates.remove(mob);

            if (!mob.isRemoved()) {
                mob.discard();
            }
        }
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
