package cz.xefensor.retold.worldgen.air;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.registry.RetoldEntityTypes;
import cz.xefensor.retold.worldgen.air.wind.AirTempleWindSource;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.BuiltinTestFunctions;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public final class RetoldGaleCoreGameTests {
    private static final Identifier EMPTY_STRUCTURE =
            Identifier.withDefaultNamespace("empty");
    private static final int TEST_PADDING = 48;

    private RetoldGaleCoreGameTests() {
    }

    public static void register(
            RegisterGameTestsEvent event,
            Holder<TestEnvironmentDefinition<?>> environment
    ) {
        registerTest(
                event,
                environment,
                "gale_core_activation_respects_player_state",
                RetoldGaleCoreGameTests::activationRespectsPlayerState
        );
        registerTest(
                event,
                environment,
                "gale_core_player_damage_activates_outside_proximity",
                RetoldGaleCoreGameTests::playerDamageActivatesOutsideProximity
        );
        registerTest(
                event,
                environment,
                "gale_core_phase_two_transition_is_one_way",
                RetoldGaleCoreGameTests::phaseTwoTransitionIsOneWay
        );
        registerTest(
                event,
                environment,
                "gale_core_disengagement_clears_combat_state",
                RetoldGaleCoreGameTests::disengagementClearsCombatState
        );
        registerTest(
                event,
                environment,
                "gale_core_state_round_trip_preserves_phase_and_home",
                RetoldGaleCoreGameTests::stateRoundTripPreservesPhaseAndHome
        );
        registerTest(
                event,
                environment,
                "gale_core_spawner_repairs_duplicates",
                RetoldGaleCoreGameTests::spawnerRepairsDuplicates
        );
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
                        100,
                        0,
                        true,
                        Rotation.NONE,
                        false,
                        1,
                        1,
                        false,
                        TEST_PADDING
                );

        event.registerTest(id(name), new InlineGameTest(testData, test));
    }

    private static void activationRespectsPlayerState(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Vec3 home = helper.absoluteVec(new Vec3(4.5D, 4.0D, 4.5D));
        GaleCore boss = createBoss(level, home, boundsAround(home, 32.0D));
        ServerPlayer creativePlayer = addMockServerPlayer(helper, GameType.CREATIVE);
        ServerPlayer survivalPlayer = null;

        try {
            movePlayer(creativePlayer, home);
            boss.tickBossState(level);

            helper.assertTrue(
                    boss.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).isEmpty(),
                    "A creative player must not activate the Gale Core"
            );

            removePlayer(level, creativePlayer);
            creativePlayer = null;
            survivalPlayer = addMockServerPlayer(helper, GameType.SURVIVAL);
            movePlayer(survivalPlayer, home);
            boss.tickBossState(level);

            assertAttackTarget(helper, boss, survivalPlayer);
            helper.assertFalse(
                    boss.isNoGravity(),
                    "An activated full-health Gale Core must enter grounded Phase 1"
            );
            helper.succeed();
        } finally {
            if (creativePlayer != null) {
                removePlayer(level, creativePlayer);
            }
            if (survivalPlayer != null) {
                removePlayer(level, survivalPlayer);
            }
            boss.discard();
        }
    }

    private static void playerDamageActivatesOutsideProximity(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Vec3 home = helper.absoluteVec(new Vec3(4.5D, 4.0D, 4.5D));
        GaleCore boss = createBoss(level, home, boundsAround(home, 40.0D));
        ServerPlayer player = addMockServerPlayer(helper, GameType.SURVIVAL);

        try {
            movePlayer(player, home.add(22.0D, 0.0D, 0.0D));
            boss.tickBossState(level);

            helper.assertTrue(
                    boss.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).isEmpty(),
                    "A player beyond the proximity radius must not activate the Gale Core"
            );
            helper.assertTrue(
                    boss.hurtServer(level, level.damageSources().playerAttack(player), 1.0F),
                    "The in-bounds player attack must damage the Gale Core"
            );

            assertAttackTarget(helper, boss, player);
            helper.assertFalse(
                    boss.isNoGravity(),
                    "Damage activation at full health must enter grounded Phase 1"
            );
            helper.succeed();
        } finally {
            removePlayer(level, player);
            boss.discard();
        }
    }

    private static void phaseTwoTransitionIsOneWay(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Vec3 home = helper.absoluteVec(new Vec3(4.5D, 4.0D, 4.5D));
        GaleCore boss = createBoss(level, home, boundsAround(home, 32.0D));
        ServerPlayer player = addMockServerPlayer(helper, GameType.SURVIVAL);

        try {
            movePlayer(player, home);
            boss.tickBossState(level);
            Projectile projectile = new Snowball(
                    level,
                    boss,
                    Items.SNOWBALL.getDefaultInstance()
            );
            float phaseThreshold = boss.getMaxHealth() * 0.5F;

            boss.setHealth(Math.nextUp(phaseThreshold));
            boss.tickBossState(level);
            helper.assertFalse(
                    boss.isNoGravity(),
                    "Health above half must remain in grounded Phase 1"
            );
            helper.assertFalse(
                    boss.deflection(projectile) == ProjectileDeflection.NONE,
                    "Phase 1 must retain Breeze-style projectile deflection"
            );

            boss.setHealth(phaseThreshold);
            boss.tickBossState(level);
            assertPhaseTwo(helper, boss, projectile);

            boss.setHealth(boss.getMaxHealth());
            boss.tickBossState(level);
            assertPhaseTwo(helper, boss, projectile);
            helper.succeed();
        } finally {
            removePlayer(level, player);
            boss.discard();
        }
    }

    private static void disengagementClearsCombatState(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Vec3 home = helper.absoluteVec(new Vec3(4.5D, 4.0D, 4.5D));
        AABB bounds = boundsAround(home, 24.0D);
        GaleCore boss = createBoss(level, home, bounds);
        ServerPlayer player = addMockServerPlayer(helper, GameType.SURVIVAL);

        try {
            movePlayer(player, home);
            boss.tickBossState(level);
            boss.setHealth(boss.getMaxHealth() * 0.5F);
            boss.tickBossState(level);

            movePlayer(player, new Vec3(bounds.maxX + 8.0D, home.y, home.z));
            boss.tickBossState(level);

            helper.assertTrue(
                    boss.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).isEmpty(),
                    "Disengagement must clear the Gale Core attack memory"
            );
            helper.assertTrue(
                    boss.getBrain().getMemory(MemoryModuleType.WALK_TARGET).isEmpty(),
                    "Disengagement must clear the Gale Core walk memory"
            );
            helper.assertTrue(
                    boss.getTarget() == null,
                    "Disengagement must clear normal mob targeting"
            );

            Projectile projectile = new Snowball(
                    level,
                    boss,
                    Items.SNOWBALL.getDefaultInstance()
            );
            assertPhaseTwo(helper, boss, projectile);

            movePlayer(player, home);
            boss.tickBossState(level);
            assertAttackTarget(helper, boss, player);
            assertPhaseTwo(helper, boss, projectile);
            helper.succeed();
        } finally {
            removePlayer(level, player);
            boss.discard();
        }
    }

    private static void stateRoundTripPreservesPhaseAndHome(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Vec3 home = helper.absoluteVec(new Vec3(4.5D, 4.0D, 4.5D));
        AABB bounds = boundsAround(home, 32.0D);
        GaleCore original = createBoss(level, home, bounds);
        GaleCore restored = createUnconfiguredBoss(level, home.add(20.0D, 0.0D, 0.0D));
        ServerPlayer player = addMockServerPlayer(helper, GameType.SURVIVAL);

        try {
            movePlayer(player, home);
            original.tickBossState(level);
            original.setHealth(original.getMaxHealth() * 0.5F);
            original.tickBossState(level);

            CompoundTag saved = saveAdditionalState(level, original);
            restored.readAdditionalSaveData(TagValueInput.create(
                    ProblemReporter.DISCARDING,
                    level.registryAccess(),
                    saved
            ));
            CompoundTag resaved = saveAdditionalState(level, restored);

            helper.assertValueEqual(
                    resaved.get("retold_home"),
                    saved.get("retold_home"),
                    "Gale Core serialization must preserve its home position"
            );
            helper.assertValueEqual(
                    resaved.get("retold_combat_bounds"),
                    saved.get("retold_combat_bounds"),
                    "Gale Core serialization must preserve its combat bounds"
            );
            helper.assertValueEqual(
                    resaved.get("retold_phase_two"),
                    saved.get("retold_phase_two"),
                    "Gale Core serialization must preserve Phase 2"
            );
            helper.assertTrue(
                    restored.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).isEmpty(),
                    "A restored Gale Core must not inherit a stale active target"
            );

            Projectile projectile = new Snowball(
                    level,
                    restored,
                    Items.SNOWBALL.getDefaultInstance()
            );
            assertPhaseTwo(helper, restored, projectile);
            restored.tickBossState(level);
            assertAttackTarget(helper, restored, player);
            assertPhaseTwo(helper, restored, projectile);
            helper.succeed();
        } finally {
            removePlayer(level, player);
            original.discard();
            restored.discard();
        }
    }

    private static void spawnerRepairsDuplicates(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Vec3 expectedHome = helper.absoluteVec(new Vec3(8.5D, 35.0D, 8.5D));
        AirTempleWindSource source = AirTempleWindSource.fromTemple(
                (int) Math.floor(expectedHome.x),
                (int) Math.floor(expectedHome.z) + 4,
                (int) Math.floor(expectedHome.y) - 31
        );
        AABB wrongBounds = boundsAround(expectedHome.add(400.0D, 0.0D, 0.0D), 8.0D);
        GaleCore keeper = createBoss(level, expectedHome.add(1.0D, 0.0D, 0.0D), wrongBounds);
        GaleCore duplicate = createBoss(level, expectedHome.add(4.0D, 0.0D, 0.0D), wrongBounds);
        ServerPlayer player = null;

        try {
            keeper.setHomePosition(expectedHome.add(200.0D, 0.0D, 0.0D));
            duplicate.setHomePosition(expectedHome.add(200.0D, 0.0D, 0.0D));
            helper.assertTrue(level.addFreshEntity(keeper), "The keeper Gale Core must enter the test level");
            helper.assertTrue(level.addFreshEntity(duplicate), "The duplicate Gale Core must enter the test level");

            helper.assertTrue(
                    GaleCoreSpawner.spawnIfNeeded(level, source),
                    "An existing Gale Core must satisfy the temple spawn request"
            );
            helper.assertFalse(keeper.isRemoved(), "The nearest Gale Core must be retained");
            helper.assertTrue(duplicate.isRemoved(), "Additional Gale Cores must be discarded");

            List<GaleCore> remaining = level.getEntities(
                    (Entity) null,
                    source.bounds().inflate(96.0D),
                    entity -> entity instanceof GaleCore
            ).stream().map(GaleCore.class::cast).toList();
            helper.assertValueEqual(
                    remaining.size(),
                    1,
                    "Duplicate repair must leave exactly one Gale Core near the temple"
            );

            player = addMockServerPlayer(helper, GameType.SURVIVAL);
            movePlayer(player, expectedHome);
            keeper.tickBossState(level);
            assertAttackTarget(helper, keeper, player);
            helper.succeed();
        } finally {
            if (player != null) {
                removePlayer(level, player);
            }
            keeper.discard();
            duplicate.discard();
        }
    }

    private static GaleCore createBoss(ServerLevel level, Vec3 home, AABB bounds) {
        GaleCore boss = createUnconfiguredBoss(level, home);
        boss.setHomePosition(home);
        boss.setCombatBounds(bounds);
        return boss;
    }

    private static GaleCore createUnconfiguredBoss(ServerLevel level, Vec3 position) {
        GaleCore boss = Objects.requireNonNull(
                RetoldEntityTypes.GALE_CORE.get().create(level, EntitySpawnReason.STRUCTURE),
                "Gale Core entity type must create an entity"
        );
        boss.snapTo(position.x, position.y, position.z, 0.0F, 0.0F);
        return boss;
    }

    private static AABB boundsAround(Vec3 center, double radius) {
        return new AABB(
                center.x - radius,
                center.y - radius,
                center.z - radius,
                center.x + radius,
                center.y + radius,
                center.z + radius
        );
    }

    private static void movePlayer(ServerPlayer player, Vec3 position) {
        player.snapTo(position.x, position.y, position.z, 0.0F, 0.0F);
    }

    private static ServerPlayer addMockServerPlayer(GameTestHelper helper, GameType gameType) {
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(gameType);
        // GameTest's mock ServerPlayer has no network connection. Keep it in
        // the authoritative level player view without firing login or chunk-tracking packets.
        helper.getLevel().players().add(player);
        return player;
    }

    private static void removePlayer(ServerLevel level, ServerPlayer player) {
        level.players().remove(player);
        player.discard();
    }

    private static CompoundTag saveAdditionalState(ServerLevel level, GaleCore boss) {
        TagValueOutput output = TagValueOutput.createWithContext(
                ProblemReporter.DISCARDING,
                level.registryAccess()
        );
        boss.addAdditionalSaveData(output);
        return output.buildResult();
    }

    private static void assertAttackTarget(
            GameTestHelper helper,
            GaleCore boss,
            ServerPlayer player
    ) {
        helper.assertTrue(
                boss.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null) == player,
                "The Gale Core must retain the expected attack target"
        );
    }

    private static void assertPhaseTwo(
            GameTestHelper helper,
            GaleCore boss,
            Projectile projectile
    ) {
        helper.assertTrue(boss.isNoGravity(), "Phase 2 must use aerial no-gravity movement");
        helper.assertTrue(
                boss.deflection(projectile) == ProjectileDeflection.NONE,
                "Phase 2 must not deflect projectiles"
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
