package cz.xefensor.retold.behavior.performance;

import cz.xefensor.retold.Retold;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.BuiltinTestFunctions;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.function.Consumer;

public final class RetoldAiSightCacheGameTests {
    private static final Identifier EMPTY_STRUCTURE =
            Identifier.withDefaultNamespace("empty");

    private RetoldAiSightCacheGameTests() {
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
                id("ai_sight_cache_cleanup_retains_fresh_result"),
                new InlineGameTest(
                        testData,
                        RetoldAiSightCacheGameTests::cleanupRetainsFreshResult
                )
        );
    }

    private static void cleanupRetainsFreshResult(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        CountingSpider expiredObserver = spawnObserver(
                helper,
                new Vec3(1.5D, 1.0D, 1.5D)
        );
        Zombie expiredTarget = spawnTarget(
                helper,
                new Vec3(3.5D, 1.0D, 1.5D)
        );
        CountingSpider currentObserver = spawnObserver(
                helper,
                new Vec3(1.5D, 1.0D, 4.5D)
        );
        Zombie currentTarget = spawnTarget(
                helper,
                new Vec3(3.5D, 1.0D, 4.5D)
        );

        RetoldAiSightCache.resetForTests();

        try {
            long initialGameTime = level.getGameTime();
            long cleanupGameTime = initialGameTime + 1_000L;

            helper.assertTrue(
                    RetoldAiSightCache.canSee(
                            expiredObserver,
                            expiredTarget,
                            initialGameTime
                    ),
                    "The initial line-of-sight result must be visible"
            );
            helper.assertValueEqual(
                    RetoldAiSightCache.cachedEntryCountForTests(expiredObserver),
                    1,
                    "The initial sight result must remain attached to its observer"
            );

            helper.assertTrue(
                    RetoldAiSightCache.canSee(
                            currentObserver,
                            currentTarget,
                            cleanupGameTime
                    ),
                    "The sight result calculated on a cleanup boundary must be visible"
            );
            helper.assertValueEqual(
                    RetoldAiSightCache.cachedEntryCountForTests(expiredObserver),
                    -1,
                    "Cleanup must remove an observer whose only entry expired"
            );
            helper.assertValueEqual(
                    RetoldAiSightCache.cachedEntryCountForTests(currentObserver),
                    1,
                    "Cleanup must not detach the current observer's fresh entry list"
            );

            helper.assertTrue(
                    RetoldAiSightCache.canSee(
                            currentObserver,
                            currentTarget,
                            cleanupGameTime
                    ),
                    "The immediate repeated lookup must reuse the visible result"
            );
            helper.assertValueEqual(
                    currentObserver.lineOfSightChecks(),
                    1,
                    "A repeated lookup must not perform a second raycast"
            );

            helper.succeed();
        } finally {
            RetoldAiSightCache.resetForTests();
            expiredObserver.discard();
            expiredTarget.discard();
            currentObserver.discard();
            currentTarget.discard();
        }
    }

    private static CountingSpider spawnObserver(
            GameTestHelper helper,
            Vec3 relativePosition
    ) {
        ServerLevel level = helper.getLevel();
        CountingSpider observer = new CountingSpider(level);
        Vec3 position = helper.absoluteVec(relativePosition);

        observer.snapTo(
                position.x(),
                position.y(),
                position.z(),
                0.0F,
                0.0F
        );
        level.addFreshEntity(observer);
        return observer;
    }

    private static Zombie spawnTarget(
            GameTestHelper helper,
            Vec3 relativePosition
    ) {
        ServerLevel level = helper.getLevel();
        Zombie target = new Zombie(EntityTypes.ZOMBIE, level);
        Vec3 position = helper.absoluteVec(relativePosition);

        target.snapTo(
                position.x(),
                position.y(),
                position.z(),
                0.0F,
                0.0F
        );
        level.addFreshEntity(target);
        return target;
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Retold.MODID, path);
    }

    private static final class CountingSpider extends Spider {
        private int lineOfSightChecks;

        private CountingSpider(ServerLevel level) {
            super(EntityTypes.SPIDER, level);
        }

        @Override
        public boolean hasLineOfSight(Entity target) {
            lineOfSightChecks++;
            return true;
        }

        private int lineOfSightChecks() {
            return lineOfSightChecks;
        }
    }

    private static final class InlineGameTest
            extends FunctionGameTestInstance {
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
