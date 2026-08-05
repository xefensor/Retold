package cz.xefensor.retold.behavior.species;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;
import cz.xefensor.retold.combat.RetoldCombatTargets;
import cz.xefensor.retold.combat.RetoldFactionTargetMemory;
import cz.xefensor.retold.combat.RetoldTargetSource;
import cz.xefensor.retold.territory.RetoldTerritoryBrainGuards;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.BuiltinTestFunctions;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.function.Consumer;

public final class RetoldAxolotlGuardianGameTests {
    private static final Identifier EMPTY_STRUCTURE =
            Identifier.withDefaultNamespace("empty");

    private RetoldAxolotlGuardianGameTests() {
    }

    public static void register(
            RegisterGameTestsEvent event
    ) {
        Holder<TestEnvironmentDefinition<?>> environment =
                event.registerEnvironment(
                        id("isolated_axolotl_guardian_defense"),
                        new TestEnvironmentDefinition.AllOf()
                );
        TestData<Holder<TestEnvironmentDefinition<?>>> testData =
                new TestData<>(
                        environment,
                        EMPTY_STRUCTURE,
                        40,
                        0,
                        true
                );

        event.registerTest(
                id("axolotls_only_fight_guardians_in_defense"),
                new InlineGameTest(
                        testData,
                        RetoldAxolotlGuardianGameTests::axolotlsOnlyFightGuardiansInDefense
                )
        );
    }

    private static void axolotlsOnlyFightGuardiansInDefense(
            GameTestHelper helper
    ) {
        ServerLevel level = helper.getLevel();
        var victim = helper.spawn(EntityTypes.AXOLOTL, 1, 2, 1);
        var ally = helper.spawn(EntityTypes.AXOLOTL, 2, 2, 1);
        var guardian = helper.spawn(EntityTypes.GUARDIAN, 3, 2, 1);

        victim.setTarget(guardian);
        helper.assertTrue(
                victim.getTarget() == null
                        && victim.getTargetUnchecked() == null,
                "Vanilla target assignment must not make a Guardian random Axolotl prey"
        );

        RetoldTerritoryBrainGuards.pushCurrentMob(victim);
        try {
            victim.getBrain().setMemory(
                    MemoryModuleType.ATTACK_TARGET,
                    guardian
            );
        } finally {
            RetoldTerritoryBrainGuards.popCurrentMob(victim);
        }

        helper.assertTrue(
                victim.getBrain().getMemory(
                        MemoryModuleType.ATTACK_TARGET
                ).isEmpty(),
                "The Axolotl brain must not turn a sensed Guardian into random prey"
        );
        helper.assertFalse(
                RetoldCombatTargets.applyAttackTarget(
                        victim,
                        guardian,
                        RetoldTargetSource.BEHAVIOR_COMBAT
                ),
                "Ordinary Retold hunting must not target a Guardian"
        );
        helper.assertTrue(
                RetoldMobRules.isAquaticHelperPredator(victim),
                "The Axolotl profile must be loaded before defense is tested"
        );
        helper.assertTrue(
                victim.canAttack(guardian),
                "Minecraft's entity-level attack check must accept the Guardian"
        );
        helper.assertTrue(
                RetoldCombatTargets.applyAttackTarget(
                        victim,
                        guardian,
                        RetoldTargetSource.RETALIATION
                ),
                "The explicit retaliation target must be accepted; target="
                        + victim.getTarget()
                        + ", owner="
                        + RetoldFactionTargetMemory.debugOwnershipText(victim)
        );
        RetoldCombatTargets.clearTargetReferencesAndAggression(
                victim,
                guardian,
                true
        );

        float guardianHealthBeforeDefense = guardian.getHealth();
        helper.assertTrue(
                victim.hurtServer(
                        level,
                        level.damageSources().mobAttack(guardian),
                        1.0F
                ),
                "The Guardian attack must reach the Axolotl"
        );
        helper.assertTrue(
                victim.getTarget() == guardian
                        && RetoldFactionTargetMemory.isOwnedByAny(
                        victim,
                        guardian,
                        RetoldTargetSource.RETALIATION
                ),
                "The attacked Axolotl must retaliate against the Guardian; target="
                        + victim.getTarget()
                        + ", owner="
                        + RetoldFactionTargetMemory.debugOwnershipText(victim)
                        + ", control="
                        + RetoldAiControl.getMode(victim)
        );
        helper.assertTrue(
                ally.getTarget() == guardian
                        && RetoldFactionTargetMemory.isOwnedByAny(
                        ally,
                        guardian,
                        RetoldTargetSource.FACTION_ASSIST
                ),
                "A nearby Axolotl that witnesses the attack must assist"
        );

        helper.startSequence()
                .thenIdle(1)
                .thenWaitUntil(() -> helper.assertTrue(
                        guardian.getHealth() < guardianHealthBeforeDefense,
                        "Defensive Axolotls must follow through and damage the Guardian"
                ))
                .thenExecute(() -> {
                    victim.discard();
                    ally.discard();
                    guardian.discard();
                })
                .thenSucceed();
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
