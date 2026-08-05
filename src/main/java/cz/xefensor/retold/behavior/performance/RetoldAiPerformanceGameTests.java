package cz.xefensor.retold.behavior.performance;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.behavior.profiles.RetoldMobStates;

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
import net.minecraft.world.entity.animal.armadillo.Armadillo;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.panda.Panda;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class RetoldAiPerformanceGameTests {
    private static final Identifier TEST_STRUCTURE =
            Identifier.withDefaultNamespace("woodland_mansion/2x2_a1");

    private static final int GRID_WIDTH = 16;
    private static final int MOB_COUNT = GRID_WIDTH * GRID_WIDTH;
    private static final int OBSERVATION_TICKS = 200;
    private static final int ARENA_MAX = 14;
    private static final double GRID_SPACING = 0.8D;

    private RetoldAiPerformanceGameTests() {
    }

    public static void register(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment =
                event.registerEnvironment(
                        id("isolated_ai_performance"),
                        new TestEnvironmentDefinition.AllOf()
                );
        TestData<Holder<TestEnvironmentDefinition<?>>> testData =
                new TestData<>(
                        environment,
                        TEST_STRUCTURE,
                        OBSERVATION_TICKS + 40,
                        0,
                        true,
                        Rotation.NONE,
                        false,
                        1,
                        1,
                        true,
                        40
                );

        event.registerTest(
                id("loaded_mob_ai_work_remains_bounded"),
                new InlineGameTest(
                        testData,
                        RetoldAiPerformanceGameTests::loadedMobAiWorkRemainsBounded
                )
        );
    }

    private static void loadedMobAiWorkRemainsBounded(GameTestHelper helper) {
        buildArena(helper);

        List<PathfinderMob> mobs = new ArrayList<>(MOB_COUNT);

        for (int index = 0; index < MOB_COUNT; index++) {
            double x = 1.5D + index % GRID_WIDTH * GRID_SPACING;
            double z = 1.5D + index / GRID_WIDTH * GRID_SPACING;
            PathfinderMob mob = spawnManagedMob(
                    helper,
                    index / 32,
                    x,
                    z
            );
            mob.setPersistenceRequired();
            mob.setInvulnerable(true);
            RetoldMobStates.getOrCreate(
                    mob,
                    helper.getLevel().getGameTime()
            ).markDanger(helper.getLevel().getGameTime());
            mobs.add(mob);
        }

        RetoldBehaviorPerf.reset();
        long startedAtNanos = System.nanoTime();
        int startedAtServerTick = helper.getLevel().getServer().getTickCount();

        helper.runAtTickTime(
                OBSERVATION_TICKS,
                () -> finishObservation(
                        helper,
                        mobs,
                        startedAtNanos,
                        startedAtServerTick
                )
        );
    }

    private static void finishObservation(
            GameTestHelper helper,
            List<PathfinderMob> mobs,
            long startedAtNanos,
            int startedAtServerTick
    ) {
        long finishedAtNanos = System.nanoTime();
        int finishedAtServerTick = helper.getLevel().getServer().getTickCount();
        RetoldBehaviorPerf.AiWorkSnapshot snapshot =
                RetoldBehaviorPerf.aiWorkSnapshot();
        long aliveMobs = mobs.stream()
                .filter(PathfinderMob::isAlive)
                .count();
        int observedServerTicks = Math.max(
                1,
                finishedAtServerTick - startedAtServerTick
        );
        double averageServerTickMillis = (finishedAtNanos - startedAtNanos)
                / 1_000_000.0D
                / observedServerTicks;

        try {
            helper.assertValueEqual(
                    aliveMobs,
                    (long) MOB_COUNT,
                    "The loaded-mob sample must remain intact for the observation window"
            );
            helper.assertTrue(
                    snapshot.timingChecks() > 0L,
                    "The loaded population must exercise Retold timing"
            );
            helper.assertTrue(
                    snapshot.lodSamples() > 0L
                            && snapshot.lodFullSamples() == snapshot.lodSamples(),
                    "Recent danger markers must keep every observed LOD sample at full detail"
            );
            helper.assertTrue(
                    snapshot.entityScanRequests() > 0L,
                    "The mixed loaded population must exercise entity scans"
            );
            helper.assertTrue(
                    snapshot.entityScanCacheHits() * 2L
                            >= snapshot.entityScanRequests(),
                    "At least half of loaded-mob entity scan requests must hit a cache"
            );
            assertWorkWithinBudgets(
                    helper,
                    snapshot,
                    observedServerTicks
            );

            Retold.LOGGER.info(
                    "Loaded-mob AI performance: mobs={}, testSteps={}, serverTicks={}, "
                            + "avgServerTickMs={}, "
                            + "timingChecks={}, lodFull/total={}/{}, scans={}/{}/{} "
                            + "positionScans={}/{}/{} paths={}/{} sight={}/{}/{} "
                            + "blockSearches={}/{}/{}",
                    MOB_COUNT,
                    OBSERVATION_TICKS,
                    observedServerTicks,
                    String.format(
                            java.util.Locale.ROOT,
                            "%.3f",
                            averageServerTickMillis
                    ),
                    snapshot.timingChecks(),
                    snapshot.lodFullSamples(),
                    snapshot.lodSamples(),
                    snapshot.entityScanRequests(),
                    snapshot.entityScanCacheHits(),
                    snapshot.entityScanBudgetSkips(),
                    snapshot.positionScanRequests(),
                    snapshot.positionScanCacheHits(),
                    snapshot.positionScanBudgetSkips(),
                    snapshot.pathRequests(),
                    snapshot.pathSkips(),
                    snapshot.sightRequests(),
                    snapshot.sightCacheHits(),
                    snapshot.sightBudgetSkips(),
                    snapshot.blockSearchRequests(),
                    snapshot.blockSearchCacheHits(),
                    snapshot.blockSearchBudgetSkips()
            );

            helper.succeed();
        } finally {
            for (PathfinderMob mob : mobs) {
                RetoldMobStates.remove(mob);
                mob.discard();
            }

            clearArena(helper);
            RetoldBehaviorPerf.reset();
        }
    }

    private static void assertWorkWithinBudgets(
            GameTestHelper helper,
            RetoldBehaviorPerf.AiWorkSnapshot snapshot,
            int observedServerTicks
    ) {
        helper.assertTrue(
                snapshot.successfulEntityScans()
                        <= RetoldAiWorkBudget.maximumEntityScansOver(observedServerTicks),
                "Entity scans must remain inside the global per-tick work budget"
        );
        helper.assertTrue(
                snapshot.successfulPositionScans()
                        <= RetoldAiWorkBudget.maximumPositionScansOver(observedServerTicks),
                "Position scans must remain inside the global per-tick work budget"
        );
        helper.assertTrue(
                snapshot.successfulSightRaycasts()
                        <= RetoldAiWorkBudget.maximumSightRaycastsOver(observedServerTicks),
                "Sight raycasts must remain inside the global per-tick work budget"
        );
        helper.assertTrue(
                snapshot.successfulBlockSearches()
                        <= RetoldAiWorkBudget.maximumBlockSearchesOver(observedServerTicks),
                "Block searches must remain inside the global per-tick work budget"
        );
    }

    private static PathfinderMob spawnManagedMob(
            GameTestHelper helper,
            int group,
            double x,
            double z
    ) {
        ServerLevel level = helper.getLevel();
        PathfinderMob mob = switch (group) {
            case 0 -> new AlwaysTickingCow(level);
            case 1 -> new AlwaysTickingPig(level);
            case 2 -> new AlwaysTickingSheep(level);
            case 3 -> new AlwaysTickingRabbit(level);
            case 4 -> new AlwaysTickingGoat(level);
            case 5 -> new AlwaysTickingPanda(level);
            case 6 -> new AlwaysTickingFrog(level);
            default -> new AlwaysTickingArmadillo(level);
        };
        Vec3 position = helper.absoluteVec(new Vec3(x, 2.0D, z));

        mob.snapTo(
                position.x(),
                position.y(),
                position.z(),
                0.0F,
                0.0F
        );
        level.addFreshEntity(mob);
        return mob;
    }

    private static void buildArena(GameTestHelper helper) {
        clearArena(helper);

        for (int x = 0; x <= ARENA_MAX; x++) {
            for (int z = 0; z <= ARENA_MAX; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
            }
        }

        for (int offset = 0; offset <= ARENA_MAX; offset++) {
            for (int y = 2; y <= 3; y++) {
                helper.setBlock(new BlockPos(0, y, offset), Blocks.STONE);
                helper.setBlock(new BlockPos(ARENA_MAX, y, offset), Blocks.STONE);
                helper.setBlock(new BlockPos(offset, y, 0), Blocks.STONE);
                helper.setBlock(new BlockPos(offset, y, ARENA_MAX), Blocks.STONE);
            }
        }
    }

    private static void clearArena(GameTestHelper helper) {
        for (int x = 0; x <= ARENA_MAX; x++) {
            for (int y = 0; y <= 8; y++) {
                for (int z = 0; z <= ARENA_MAX; z++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
                }
            }
        }
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Retold.MODID, path);
    }

    private static final class AlwaysTickingCow extends Cow {
        private AlwaysTickingCow(ServerLevel level) {
            super(EntityTypes.COW, level);
        }

        @Override
        public boolean isAlwaysTicking() {
            return true;
        }
    }

    private static final class AlwaysTickingPig extends Pig {
        private AlwaysTickingPig(ServerLevel level) {
            super(EntityTypes.PIG, level);
        }

        @Override
        public boolean isAlwaysTicking() {
            return true;
        }
    }

    private static final class AlwaysTickingSheep extends Sheep {
        private AlwaysTickingSheep(ServerLevel level) {
            super(EntityTypes.SHEEP, level);
        }

        @Override
        public boolean isAlwaysTicking() {
            return true;
        }
    }

    private static final class AlwaysTickingRabbit extends Rabbit {
        private AlwaysTickingRabbit(ServerLevel level) {
            super(EntityTypes.RABBIT, level);
        }

        @Override
        public boolean isAlwaysTicking() {
            return true;
        }
    }

    private static final class AlwaysTickingGoat extends Goat {
        private AlwaysTickingGoat(ServerLevel level) {
            super(EntityTypes.GOAT, level);
        }

        @Override
        public boolean isAlwaysTicking() {
            return true;
        }
    }

    private static final class AlwaysTickingPanda extends Panda {
        private AlwaysTickingPanda(ServerLevel level) {
            super(EntityTypes.PANDA, level);
        }

        @Override
        public boolean isAlwaysTicking() {
            return true;
        }
    }

    private static final class AlwaysTickingFrog extends Frog {
        private AlwaysTickingFrog(ServerLevel level) {
            super(EntityTypes.FROG, level);
        }

        @Override
        public boolean isAlwaysTicking() {
            return true;
        }
    }

    private static final class AlwaysTickingArmadillo extends Armadillo {
        private AlwaysTickingArmadillo(ServerLevel level) {
            super(EntityTypes.ARMADILLO, level);
        }

        @Override
        public boolean isAlwaysTicking() {
            return true;
        }
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
