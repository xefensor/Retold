package cz.xefensor.retold.behavior.species;

import cz.xefensor.retold.Retold;

import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.BuiltinTestFunctions;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerSpawnPhantomsEvent;

import java.util.function.Consumer;

public final class RetoldPhantomStalkerGameTests {
    private static final Identifier EMPTY_STRUCTURE =
            Identifier.withDefaultNamespace("empty");

    private RetoldPhantomStalkerGameTests() {
    }

    public static void register(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
                id("isolated_phantom_stalker_behavior"),
                new TestEnvironmentDefinition.AllOf()
        );

        registerTest(
                event,
                environment,
                "phantom_spawn_pressure_is_insomnia_independent",
                RetoldPhantomStalkerGameTests::spawnPressureIsInsomniaIndependent
        );
        registerTest(
                event,
                environment,
                "phantoms_do_not_prioritize_players_over_nearer_prey",
                RetoldPhantomStalkerGameTests::phantomsDoNotPrioritizePlayers
        );
        registerTest(
                event,
                environment,
                "phantom_stalker_completes_swoop_attack",
                240,
                RetoldPhantomStalkerGameTests::phantomStalkerCompletesSwoopAttack
        );
    }

    private static void spawnPressureIsInsomniaIndependent(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        Vec3 playerPos = helper.absoluteVec(new Vec3(3.5D, 3.0D, 3.5D));
        player.snapTo(playerPos.x(), playerPos.y(), playerPos.z(), 0.0F, 0.0F);
        player.getStats().setValue(
                player,
                Stats.CUSTOM.get(Stats.TIME_SINCE_REST),
                0
        );

        try {
            helper.assertValueEqual(
                    RetoldPhantomStalkerEvents.SPAWN_RARITY_ATTEMPTS,
                    16,
                    "Phantom pressure must retain the rare one-in-sixteen attempt gate"
            );

            helper.setTime(6_000L);
            PlayerSpawnPhantomsEvent daytime = spawnEvent(player);
            RetoldPhantomStalkerEvents.applySpawnPolicy(
                    daytime,
                    level,
                    player.blockPosition(),
                    0,
                    0.0F
            );
            helper.assertValueEqual(
                    daytime.getResult(),
                    PlayerSpawnPhantomsEvent.Result.DENY,
                    "Ordinary daytime must not produce Phantom pressure"
            );

            helper.setTime(18_000L);
            PlayerSpawnPhantomsEvent commonAttempt = spawnEvent(player);
            RetoldPhantomStalkerEvents.applySpawnPolicy(
                    commonAttempt,
                    level,
                    player.blockPosition(),
                    1,
                    0.0F
            );
            helper.assertValueEqual(
                    commonAttempt.getResult(),
                    PlayerSpawnPhantomsEvent.Result.DENY,
                    "The Retold rarity gate must reject ordinary eligible attempts"
            );

            PlayerSpawnPhantomsEvent rareAttempt = spawnEvent(player, 4);
            RetoldPhantomStalkerEvents.applySpawnPolicy(
                    rareAttempt,
                    level,
                    player.blockPosition(),
                    0,
                    0.0F
            );
            helper.assertTrue(
                    rareAttempt.getResult() == PlayerSpawnPhantomsEvent.Result.ALLOW
                            && rareAttempt.getPhantomsToSpawn() == 1
                            && player.getStats().getValue(
                            Stats.CUSTOM.get(Stats.TIME_SINCE_REST)
                    ) == 0,
                    "A rare open-sky night attempt must allow exactly one Phantom without insomnia"
            );

            helper.assertValueEqual(
                    RetoldPhantomStalkerEvents.resolveSpawnResult(
                            true,
                            false,
                            true,
                            0,
                            true
                    ),
                    PlayerSpawnPhantomsEvent.Result.DENY,
                    "A covered player must not receive Phantom pressure"
            );
            helper.assertValueEqual(
                    RetoldPhantomStalkerEvents.resolveSpawnResult(
                            true,
                            true,
                            true,
                            0,
                            false
                    ),
                    PlayerSpawnPhantomsEvent.Result.DENY,
                    "An eligible attempt must retain the local-difficulty gate"
            );

            PlayerSpawnPhantomsEvent externalDecision = spawnEvent(player, 4);
            externalDecision.setResult(PlayerSpawnPhantomsEvent.Result.ALLOW);
            RetoldPhantomStalkerEvents.applySpawnPolicy(
                    externalDecision,
                    level,
                    player.blockPosition(),
                    1,
                    3.0F
            );
            helper.assertValueEqual(
                    externalDecision.getResult(),
                    PlayerSpawnPhantomsEvent.Result.ALLOW,
                    "Retold must preserve another mod's explicit Phantom spawn decision"
            );
            helper.assertValueEqual(
                    externalDecision.getPhantomsToSpawn(),
                    4,
                    "Retold must preserve another mod's explicit Phantom spawn count"
            );

            externalDecision.setResult(PlayerSpawnPhantomsEvent.Result.DENY);
            RetoldPhantomStalkerEvents.applySpawnPolicy(
                    externalDecision,
                    level,
                    player.blockPosition(),
                    0,
                    0.0F
            );
            helper.assertValueEqual(
                    externalDecision.getResult(),
                    PlayerSpawnPhantomsEvent.Result.DENY,
                    "Retold must preserve another mod's explicit Phantom spawn denial"
            );
            helper.succeed();
        } finally {
            player.discard();
        }
    }

    private static void phantomsDoNotPrioritizePlayers(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Phantom phantom = helper.spawn(EntityTypes.PHANTOM, 5, 6, 5);
        Cow nearerPrey = helper.spawn(EntityTypes.COW, 6, 3, 5);
        Zombie undeadNeighbor = helper.spawn(EntityTypes.ZOMBIE, 5, 3, 6);
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        Vec3 playerPos = helper.absoluteVec(new Vec3(8.0D, 3.0D, 5.0D));
        player.snapTo(playerPos.x(), playerPos.y(), playerPos.z(), 0.0F, 0.0F);

        try {
            helper.setTime(18_000L);
            LivingEntity selected = RetoldPhantomStalkerEvents.findBestStalkTarget(
                    level,
                    phantom
            );
            helper.assertTrue(
                    selected == nearerPrey && selected != undeadNeighbor,
                    "A Phantom must choose nearer valid prey without player favoritism or Undead hostility"
            );
            helper.succeed();
        } finally {
            player.discard();
            phantom.discard();
            nearerPrey.discard();
            undeadNeighbor.discard();
        }
    }

    private static void phantomStalkerCompletesSwoopAttack(GameTestHelper helper) {
        Phantom phantom = helper.spawn(EntityTypes.PHANTOM, 0, 10, 0);
        Cow prey = helper.spawn(EntityTypes.COW, 0, 2, 0);
        ChunkPos ticketCenter = phantom.chunkPosition();
        float startingHealth = prey.getHealth();
        int[] strikeTimestamp = {Integer.MIN_VALUE};

        setForcedChunks(helper.getLevel(), ticketCenter, true);
        prey.setNoAi(true);
        prey.setNoGravity(true);
        helper.setTime(18_000L);
        RetoldPhantomStalkerEvents.keepTarget(
                phantom,
                prey,
                helper.getLevel().getGameTime()
        );

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        prey.getHealth() < startingHealth,
                        "A Phantom stalker must turn its production-selected target into a completed swoop attack"
                                + "; target=" + (phantom.getTarget() == prey)
                                + ", tickCount=" + phantom.tickCount
                                + ", distance=" + phantom.distanceTo(prey)
                                + ", phantomPos=" + phantom.position()
                                + ", preyPos=" + prey.position()
                ))
                .thenExecute(() -> strikeTimestamp[0] = prey.getLastHurtByMobTimestamp())
                .thenIdle(40)
                .thenExecute(() -> helper.assertTrue(
                        prey.getLastHurtByMobTimestamp() == strikeTimestamp[0]
                                && phantom.getTarget() == null,
                        "A completed Phantom strike must release its target and enter recovery instead of dealing repeated contact damage"
                                + "; strikeTimestamp=" + strikeTimestamp[0]
                                + ", currentTimestamp=" + prey.getLastHurtByMobTimestamp()
                                + ", target=" + phantom.getTarget()
                ))
                .thenExecute(() -> setForcedChunks(helper.getLevel(), ticketCenter, false))
                .thenSucceed();
    }

    private static void setForcedChunks(
            ServerLevel level,
            ChunkPos center,
            boolean forced
    ) {
        for (int chunkX = center.x() - 1; chunkX <= center.x() + 1; chunkX++) {
            for (int chunkZ = center.z() - 1; chunkZ <= center.z() + 1; chunkZ++) {
                level.setChunkForced(chunkX, chunkZ, forced);
            }
        }
    }

    private static PlayerSpawnPhantomsEvent spawnEvent(ServerPlayer player) {
        return new PlayerSpawnPhantomsEvent(player, 1);
    }

    private static PlayerSpawnPhantomsEvent spawnEvent(
            ServerPlayer player,
            int phantomsToSpawn
    ) {
        return new PlayerSpawnPhantomsEvent(player, phantomsToSpawn);
    }

    private static void registerTest(
            RegisterGameTestsEvent event,
            Holder<TestEnvironmentDefinition<?>> environment,
            String path,
            Consumer<GameTestHelper> test
    ) {
        registerTest(event, environment, path, 120, test);
    }

    private static void registerTest(
            RegisterGameTestsEvent event,
            Holder<TestEnvironmentDefinition<?>> environment,
            String path,
            int timeoutTicks,
            Consumer<GameTestHelper> test
    ) {
        event.registerTest(
                id(path),
                new InlineGameTest(
                        new TestData<>(environment, EMPTY_STRUCTURE, timeoutTicks, 0, true),
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
