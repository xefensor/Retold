package cz.xefensor.retold.behavior.species;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.faction.RetoldFactionMembers;
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
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.function.Consumer;

public final class RetoldUndeadTargetParityGameTests {
    private static final Identifier EMPTY_STRUCTURE =
            Identifier.withDefaultNamespace("empty");

    private RetoldUndeadTargetParityGameTests() {
    }

    public static void register(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
                id("isolated_undead_target_parity"),
                new TestEnvironmentDefinition.AllOf()
        );

        registerTest(
                event,
                environment,
                "undead_targeting_does_not_prioritize_players",
                RetoldUndeadTargetParityGameTests::undeadTargetingDoesNotPrioritizePlayers
        );
    }

    private static void undeadTargetingDoesNotPrioritizePlayers(GameTestHelper helper) {
        assertAdditionalUndeadMemberships(helper);
        assertZombieHordeParity(helper);
        assertSkeletonRangedParity(helper);
        assertGhastArtilleryParity(helper);
        // The fourth production scan belongs in a fresh shared-work-budget tick.
        helper.runAfterDelay(1, () -> {
            assertZoglinRampageParity(helper);
            helper.succeed();
        });
    }

    private static void assertAdditionalUndeadMemberships(GameTestHelper helper) {
        var zoglin = helper.spawn(EntityTypes.ZOGLIN, 5, 3, 5);
        var zombieNautilus = helper.spawn(EntityTypes.ZOMBIE_NAUTILUS, 7, 3, 5);
        var ordinaryPrey = helper.spawn(EntityTypes.COW, 9, 3, 5);

        try {
            helper.assertTrue(
                    RetoldFactionMembers.isUndead(zoglin)
                            && RetoldFactionMembers.isUndead(zombieNautilus),
                    "Zoglins and Zombie Nautiluses must belong to Retold's Undead faction"
            );
            helper.assertTrue(
                    RetoldFactionRelations.shouldAttack(zoglin, ordinaryPrey)
                            && RetoldFactionRelations.shouldAttack(zombieNautilus, ordinaryPrey)
                            && !RetoldFactionRelations.shouldAttack(zoglin, zombieNautilus),
                    "Additional Undead members must attack ordinary prey while tolerating each other"
            );
        } finally {
            zoglin.discard();
            zombieNautilus.discard();
            ordinaryPrey.discard();
        }
    }

    private static void assertZombieHordeParity(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var zombie = helper.spawn(EntityTypes.ZOMBIE, 5, 3, 5);
        var nearerPrey = helper.spawn(EntityTypes.COW, 9, 3, 5);
        var undeadNeighbor = helper.spawn(EntityTypes.SKELETON, 6, 3, 5);
        ServerPlayer player = mockPlayer(helper, 10.5D, 3.0D, 5.5D);

        try {
            helper.assertTrue(
                    RetoldUndeadHordeEvents.findHungryTarget(level, zombie) == nearerPrey,
                    "A hungry Zombie must choose nearer ordinary prey without player favoritism"
            );
        } finally {
            player.discard();
            zombie.discard();
            nearerPrey.discard();
            undeadNeighbor.discard();
        }
    }

    private static void assertSkeletonRangedParity(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var skeleton = helper.spawn(EntityTypes.SKELETON, 5, 3, 5);
        var betterRangedTarget = helper.spawn(EntityTypes.COW, 18, 3, 5);
        var undeadNeighbor = helper.spawn(EntityTypes.ZOMBIE, 6, 3, 5);
        ServerPlayer player = mockPlayer(helper, 18.2D, 3.0D, 5.5D);

        try {
            helper.assertTrue(
                    RetoldSkeletonRangedEvents.findVisibleEnemy(level, skeleton) == betterRangedTarget,
                    "A Skeleton must choose the better ranged target without player favoritism"
            );
        } finally {
            player.discard();
            skeleton.discard();
            betterRangedTarget.discard();
            undeadNeighbor.discard();
        }
    }

    private static void assertGhastArtilleryParity(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var ghast = helper.spawn(EntityTypes.GHAST, 5, 10, 5);
        var nearerPrey = helper.spawn(EntityTypes.COW, 13, 10, 5);
        var undeadNeighbor = helper.spawn(EntityTypes.ZOMBIE, 6, 10, 5);
        ServerPlayer player = mockPlayer(helper, 17.5D, 10.0D, 5.5D);

        try {
            helper.assertTrue(
                    RetoldGhastArtilleryEvents.findBestArtilleryTarget(level, ghast) == nearerPrey,
                    "A Ghast must choose nearer visible prey without player favoritism"
            );
        } finally {
            player.discard();
            ghast.discard();
            nearerPrey.discard();
            undeadNeighbor.discard();
        }
    }

    private static void assertZoglinRampageParity(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        // Keep this fixture in a separate shared-scan bucket from the Skeleton case.
        var zoglin = helper.spawn(EntityTypes.ZOGLIN, 37, 3, 5);
        var nearerPrey = helper.spawn(EntityTypes.COW, 41, 3, 5);
        var undeadNeighbor = helper.spawn(EntityTypes.ZOMBIE, 38, 3, 5);
        ServerPlayer player = mockPlayer(helper, 42.5D, 3.0D, 5.5D);

        try {
            var selected = RetoldZoglinRampagerEvents.findBestRampageTarget(level, zoglin);
            helper.assertTrue(
                    selected == nearerPrey,
                    "A Zoglin must choose nearer visible prey without player favoritism; selected "
                            + (selected == null ? "none" : selected.getType().toString())
            );
        } finally {
            player.discard();
            zoglin.discard();
            nearerPrey.discard();
            undeadNeighbor.discard();
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
