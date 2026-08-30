package cz.xefensor.retold.combat;

import cz.xefensor.retold.Retold;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.BuiltinTestFunctions;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityTypes;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.function.Consumer;

public final class RetoldThreatRetargetGameTests {
    private static final Identifier EMPTY_STRUCTURE =
            Identifier.withDefaultNamespace("empty");

    private RetoldThreatRetargetGameTests() {
    }

    public static void register(
            RegisterGameTestsEvent event,
            Holder<TestEnvironmentDefinition<?>> environment
    ) {
        registerTest(
                event,
                environment,
                "mobs_retarget_to_materially_more_dangerous_attacker",
                RetoldThreatRetargetGameTests::mobsRetargetToMoreDangerousAttacker
        );
    }

    private static void mobsRetargetToMoreDangerousAttacker(
            GameTestHelper helper
    ) {
        ServerLevel level = helper.getLevel();
        var retargetingMob = helper.spawn(EntityTypes.ZOMBIE, 2, 2, 2);
        var creeperSafeMob = helper.spawn(EntityTypes.ZOMBIE, 2, 2, 6);
        var currentThreat = helper.spawn(EntityTypes.COW, 5, 2, 2);
        var creeperSafeTarget = helper.spawn(EntityTypes.COW, 5, 2, 6);
        var dangerousAttacker = helper.spawn(EntityTypes.IRON_GOLEM, 8, 2, 4);
        var creeper = helper.spawn(EntityTypes.CREEPER, 8, 2, 7);

        try {
            helper.assertTrue(
                    RetoldCombatTargets.applyAttackTarget(
                            retargetingMob,
                            currentThreat,
                            RetoldTargetSource.FACTION_COMBAT
                    ),
                    "The retargeting fixture must begin with an ordinary combat target"
            );
            helper.assertTrue(
                    retargetingMob.hurtServer(
                            level,
                            level.damageSources().mobAttack(currentThreat),
                            6.0F
                    ),
                    "The current target must deal real damage for its threat score"
            );

            retargetingMob.invulnerableTime = 0;

            helper.assertTrue(
                    retargetingMob.hurtServer(
                            level,
                            level.damageSources().mobAttack(dangerousAttacker),
                            8.0F
                    ),
                    "The challenger must deal real damage for retargeting"
            );
            helper.assertTrue(
                    retargetingMob.getTarget() == dangerousAttacker
                            && RetoldFactionTargetMemory.getSource(
                            retargetingMob,
                            dangerousAttacker
                    ) == RetoldTargetSource.RETALIATION,
                    "A materially more damaging attacker must replace ordinary combat through retaliation ownership"
            );

            helper.assertTrue(
                    RetoldCombatTargets.applyAttackTarget(
                            creeperSafeMob,
                            creeperSafeTarget,
                            RetoldTargetSource.FACTION_COMBAT
                    ),
                    "The Creeper-safety fixture must begin with an ordinary combat target"
            );
            helper.assertTrue(
                    creeperSafeMob.hurtServer(
                            level,
                            level.damageSources().mobAttack(creeper),
                            8.0F
                    ),
                    "The Creeper must deal real damage before its target exclusion is tested"
            );
            helper.assertTrue(
                    creeperSafeMob.getTarget() == creeperSafeTarget
                            && RetoldFactionTargetMemory.getSource(
                            creeperSafeMob,
                            creeperSafeTarget
                    ) == RetoldTargetSource.FACTION_COMBAT,
                    "Threat scoring must not bypass the global Creeper target exclusion"
            );

            helper.succeed();
        } finally {
            retargetingMob.discard();
            creeperSafeMob.discard();
            currentThreat.discard();
            creeperSafeTarget.discard();
            dangerousAttacker.discard();
            creeper.discard();
        }
    }

    private static void registerTest(
            RegisterGameTestsEvent event,
            Holder<TestEnvironmentDefinition<?>> environment,
            String path,
            Consumer<GameTestHelper> test
    ) {
        event.registerTest(
                id(path),
                new InlineGameTest(
                        new TestData<>(environment, EMPTY_STRUCTURE, 40, 0, true),
                        test
                )
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
