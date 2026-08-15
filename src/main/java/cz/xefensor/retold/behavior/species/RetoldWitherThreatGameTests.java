package cz.xefensor.retold.behavior.species;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.combat.RetoldFactionTargetMemory;
import cz.xefensor.retold.combat.RetoldTargetSource;
import cz.xefensor.retold.faction.RetoldFactionRelations;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.BuiltinTestFunctions;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.function.Consumer;

public final class RetoldWitherThreatGameTests {
    private static final Identifier EMPTY_STRUCTURE =
            Identifier.withDefaultNamespace("empty");

    private RetoldWitherThreatGameTests() {
    }

    public static void register(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
                id("isolated_wither_threat_behavior"),
                new TestEnvironmentDefinition.AllOf()
        );

        registerTest(
                event,
                environment,
                "wither_prioritizes_serious_threats",
                RetoldWitherThreatGameTests::witherPrioritizesSeriousThreats
        );
    }

    private static void witherPrioritizesSeriousThreats(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        WitherBoss wither = helper.spawn(EntityTypes.WITHER, 5, 8, 5);
        var nearbyPrey = helper.spawn(EntityTypes.COW, 8, 8, 5);
        var activeThreat = helper.spawn(EntityTypes.IRON_GOLEM, 14, 8, 5);
        var ghastAlly = helper.spawn(EntityTypes.GHAST, 6, 8, 5);
        var zoglinAlly = helper.spawn(EntityTypes.ZOGLIN, 7, 8, 5);
        var nautilusAlly = helper.spawn(EntityTypes.ZOMBIE_NAUTILUS, 8, 8, 6);
        ServerPlayer player = mockPlayer(helper, 9.5D, 8.0D, 5.5D);

        try {
            wither.setInvulnerableTicks(0);
            activeThreat.setTarget(wither);

            helper.assertTrue(
                    !RetoldFactionRelations.shouldAttack(wither, ghastAlly)
                            && !RetoldFactionRelations.shouldAttack(wither, zoglinAlly)
                            && !RetoldFactionRelations.shouldAttack(wither, nautilusAlly),
                    "Every Retold Undead addition must be safe from all Wither heads"
            );

            wither.setTarget(null);
            wither.setTarget(ghastAlly);
            helper.assertTrue(
                    wither.getTarget() == null,
                    "The Wither's primary head must reject a Retold Undead target"
            );

            wither.setAlternativeTarget(1, ghastAlly.getId());
            wither.setAlternativeTarget(2, zoglinAlly.getId());
            RetoldWitherThreatEvents.clearInvalidAlternativeTargets(level, wither);
            helper.assertTrue(
                    wither.getAlternativeTarget(1) == 0
                            && wither.getAlternativeTarget(2) == 0,
                    "Wither side heads must clear Retold Undead targets"
            );

            wither.setAlternativeTarget(1, nautilusAlly.getId());
            RetoldWitherThreatEvents.clearInvalidAlternativeTargets(level, wither);
            helper.assertValueEqual(
                    wither.getAlternativeTarget(1),
                    0,
                    "A wild Zombie Nautilus must be safe from Wither side heads"
            );

            nautilusAlly.setTame(true, true);
            wither.setTarget(nautilusAlly);
            wither.setAlternativeTarget(1, nautilusAlly.getId());
            RetoldWitherThreatEvents.clearInvalidAlternativeTargets(level, wither);
            helper.assertTrue(
                    RetoldFactionRelations.shouldAttack(wither, nautilusAlly)
                            && wither.getTarget() == nautilusAlly
                            && wither.getAlternativeTarget(1) == nautilusAlly.getId(),
                    "Taming must remove Zombie Nautilus Undead protection dynamically"
            );

            var selected = RetoldWitherThreatEvents.findBestThreat(level, wither);

            helper.assertTrue(
                    selected == activeThreat
                            && selected != nearbyPrey
                            && selected != player,
                    "The Wither must prioritize an active serious threat without player favoritism"
            );

            RetoldWitherThreatEvents.tickThreatTargeting(level, wither);

            helper.assertTrue(
                    wither.getTarget() == activeThreat
                            && RetoldFactionTargetMemory.getSource(wither, activeThreat)
                            == RetoldTargetSource.FACTION_COMBAT,
                    "Wither threat selection must use Retold's source-aware target owner"
            );

            wither.setTarget(nautilusAlly);
            nautilusAlly.setTame(false, true);
            RetoldWitherThreatEvents.clearInvalidPrimaryTarget(wither);
            RetoldWitherThreatEvents.clearInvalidAlternativeTargets(level, wither);
            helper.assertTrue(
                    wither.getTarget() == null
                            && wither.getAlternativeTarget(1) == 0,
                    "Every retained head target must clear when it becomes an Undead ally"
            );
            helper.succeed();
        } finally {
            player.discard();
            wither.discard();
            nearbyPrey.discard();
            activeThreat.discard();
            ghastAlly.discard();
            zoglinAlly.discard();
            nautilusAlly.discard();
        }
    }

    private static ServerPlayer mockPlayer(
            GameTestHelper helper,
            double x,
            double y,
            double z
    ) {
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        Vec3 position = helper.absoluteVec(new Vec3(x, y, z));
        player.snapTo(position.x(), position.y(), position.z(), 0.0F, 0.0F);
        return player;
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
                        new TestData<>(environment, EMPTY_STRUCTURE, 120, 0, true),
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
