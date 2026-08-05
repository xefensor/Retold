package cz.xefensor.retold.behavior.species;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.control.RetoldAiControlMode;
import cz.xefensor.retold.behavior.control.RetoldAiControlOwner;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.function.Consumer;

public final class RetoldPolarBearWarningGameTests {
    private static final Identifier EMPTY_STRUCTURE =
            Identifier.withDefaultNamespace("empty");

    private RetoldPolarBearWarningGameTests() {
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

        event.registerTest(
                id("polar_bear_warns_before_defending_cub"),
                new InlineGameTest(
                        testData,
                        RetoldPolarBearWarningGameTests::polarBearWarnsBeforeDefendingCub
                )
        );
        event.registerTest(
                id("polar_bear_immediately_defends_attacked_cub"),
                new InlineGameTest(
                        testData,
                        RetoldPolarBearWarningGameTests::polarBearImmediatelyDefendsAttackedCub
                )
        );
    }

    private static void polarBearWarnsBeforeDefendingCub(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var protector = helper.spawn(EntityTypes.POLAR_BEAR, 1, 2, 1);
        var cub = helper.spawn(EntityTypes.POLAR_BEAR, 2, 2, 1);
        Player intruder = spawnPlayer(helper, new Vec3(4.5D, 2.0D, 1.5D));
        long startTime = level.getGameTime() + 1_000L;

        cub.setBaby(true);

        try {
            helper.assertTrue(
                    RetoldMobRules.isProtectiveNeutral(protector),
                    "The Polar Bear profile must be loaded before warning behavior is tested"
            );

            protector.setTarget(intruder);
            helper.assertTrue(
                    protector.getTarget() == null,
                    "Vanilla cub-proximity targeting must not bypass the warning"
            );

            RetoldNeutralWildlifeEvents.tickProtectiveNeutral(
                    level,
                    protector,
                    startTime
            );
            assertWarning(helper, protector, intruder);

            movePlayer(helper, intruder, new Vec3(30.5D, 2.0D, 1.5D));
            RetoldNeutralWildlifeEvents.tickProtectiveNeutral(
                    level,
                    protector,
                    startTime + 10L
            );
            helper.assertTrue(
                    protector.getTarget() == null
                            && !protector.isStanding()
                            && !RetoldAiControl.isControlledBy(
                            protector,
                            RetoldAiControlOwner.NEUTRAL_WILDLIFE
                    ),
                    "Withdrawing from the cub must cancel the warning without combat"
            );

            movePlayer(helper, intruder, new Vec3(4.5D, 2.0D, 1.5D));
            RetoldNeutralWildlifeEvents.tickProtectiveNeutral(
                    level,
                    protector,
                    startTime + 20L
            );
            RetoldNeutralWildlifeEvents.tickProtectiveNeutral(
                    level,
                    protector,
                    startTime + 59L
            );
            assertWarning(helper, protector, intruder);

            RetoldNeutralWildlifeEvents.tickProtectiveNeutral(
                    level,
                    protector,
                    startTime + 60L
            );
            helper.assertTrue(
                    protector.getTarget() == intruder
                            && RetoldAiControl.isControlledAsBy(
                            protector,
                            RetoldAiControlMode.ATTACK,
                            RetoldAiControlOwner.NEUTRAL_WILDLIFE
                    ),
                    "Remaining beside the cub through the warning must escalate to defense"
            );
            helper.assertTrue(
                    RetoldFactionTargetMemory.isOwnedByAny(
                            protector,
                            intruder,
                            RetoldTargetSource.BEHAVIOR_COMBAT
                    ),
                    "Escalated cub defense must use Retold target ownership"
            );
        } finally {
            cleanup(level, protector, cub, intruder);
        }

        helper.succeed();
    }

    private static void polarBearImmediatelyDefendsAttackedCub(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var protector = helper.spawn(EntityTypes.POLAR_BEAR, 1, 2, 1);
        var cub = helper.spawn(EntityTypes.POLAR_BEAR, 2, 2, 1);
        Player attacker = spawnPlayer(helper, new Vec3(4.5D, 2.0D, 1.5D));

        cub.setBaby(true);
        cub.setLastHurtByMob(attacker);

        try {
            RetoldNeutralWildlifeEvents.tickProtectiveNeutral(
                    level,
                    protector,
                    level.getGameTime() + 2_000L
            );
            helper.assertTrue(
                    protector.getTarget() == attacker
                            && RetoldAiControl.isControlledAsBy(
                            protector,
                            RetoldAiControlMode.ATTACK,
                            RetoldAiControlOwner.NEUTRAL_WILDLIFE
                    )
                            && !protector.isStanding(),
                    "Attacking the cub must bypass the proximity warning and trigger defense"
            );
        } finally {
            cleanup(level, protector, cub, attacker);
        }

        helper.succeed();
    }

    private static void assertWarning(
            GameTestHelper helper,
            net.minecraft.world.entity.animal.polarbear.PolarBear protector,
            Player intruder
    ) {
        helper.assertTrue(
                protector.getTarget() == null
                        && protector.isStanding()
                        && RetoldAiControl.isControlledAsBy(
                        protector,
                        RetoldAiControlMode.REGROUP,
                        RetoldAiControlOwner.NEUTRAL_WILDLIFE
                )
                        && "warn_cub_intruder".equals(
                        RetoldAiControl.getReason(protector)
                ),
                "The bear must stand, warn, and withhold its attack target while the intruder can retreat: "
                        + intruder.getScoreboardName()
        );
    }

    private static Player spawnPlayer(
            GameTestHelper helper,
            Vec3 relativePosition
    ) {
        ServerLevel level = helper.getLevel();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        movePlayer(helper, player, relativePosition);
        level.addFreshEntity(player);
        return player;
    }

    private static void movePlayer(
            GameTestHelper helper,
            Player player,
            Vec3 relativePosition
    ) {
        Vec3 position = helper.absoluteVec(relativePosition);
        player.snapTo(position.x(), position.y(), position.z(), 0.0F, 0.0F);
    }

    private static void cleanup(
            ServerLevel level,
            net.minecraft.world.entity.animal.polarbear.PolarBear protector,
            net.minecraft.world.entity.animal.polarbear.PolarBear cub,
            Player player
    ) {
        RetoldAiControl.clear(protector);
        level.players().remove(player);
        player.discard();
        protector.discard();
        cub.discard();
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
