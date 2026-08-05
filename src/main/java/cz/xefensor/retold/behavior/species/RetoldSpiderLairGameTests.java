package cz.xefensor.retold.behavior.species;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.control.RetoldAiControlMode;
import cz.xefensor.retold.behavior.control.RetoldAiControlOwner;
import cz.xefensor.retold.behavior.home.RetoldAnimalHomeMemory;
import cz.xefensor.retold.behavior.home.RetoldAnimalHomeType;
import cz.xefensor.retold.behavior.home.RetoldAnimalHomes;
import cz.xefensor.retold.behavior.profiles.RetoldMobStates;
import cz.xefensor.retold.combat.RetoldCombatTargets;
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
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.function.Consumer;

public final class RetoldSpiderLairGameTests {
    private static final Identifier EMPTY_STRUCTURE =
            Identifier.withDefaultNamespace("empty");
    private static final int EXPECTED_WEB_CAP = 50;

    private RetoldSpiderLairGameTests() {
    }

    public static void register(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment =
                event.registerEnvironment(
                        id("isolated_spider_lair"),
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
                id("spiders_build_share_and_repair_dark_lairs"),
                new InlineGameTest(
                        testData,
                        RetoldSpiderLairGameTests::spidersBuildShareAndRepairDarkLairs
                )
        );
    }

    private static void spidersBuildShareAndRepairDarkLairs(
            GameTestHelper helper
    ) {
        ServerLevel level = helper.getLevel();
        long gameTime = level.getGameTime() + 3_000L;
        boolean originalMobGriefing = level.getGameRules().get(GameRules.MOB_GRIEFING);
        Holder<WorldClock> clock = level.dimensionType().defaultClock().orElseThrow();
        long originalClockTime = level.getServer().clockManager().getTotalTicks(clock);

        buildDarkSupportedArea(helper, 0, 9, 0, 6);
        buildDarkSupportedArea(helper, 29, 36, 0, 6);
        buildOpenSupportedArea(helper, 20, 28, 0, 8);

        BrightSpider brightSpider = spawnSpider(
                helper,
                new BrightSpider(level),
                new Vec3(2.5D, 2.0D, 2.5D)
        );
        DarkSpider builder = spawnSpider(
                helper,
                new DarkSpider(level),
                new Vec3(4.5D, 2.0D, 2.5D)
        );
        PathfinderMob recruit = helper.spawn(EntityTypes.CAVE_SPIDER, 8, 2, 2);
        DarkSpider griefingDisabledSpider = spawnSpider(
                helper,
                new DarkSpider(level),
                new Vec3(32.5D, 2.0D, 2.5D)
        );
        DarkSpider openSkySpider = spawnSpider(
                helper,
                new DarkSpider(level),
                new Vec3(24.5D, 2.0D, 4.5D)
        );
        PathfinderMob retaliationTarget = helper.spawn(EntityTypes.ZOMBIE, 18, 2, 2);

        LairTestContext context = new LairTestContext(
                level,
                gameTime,
                originalMobGriefing,
                clock,
                originalClockTime,
                brightSpider,
                builder,
                recruit,
                griefingDisabledSpider,
                openSkySpider,
                retaliationTarget
        );
        helper.runAfterDelay(
                5,
                () -> runLairAssertions(helper, context)
        );
    }

    private static void runLairAssertions(
            GameTestHelper helper,
            LairTestContext context
    ) {
        ServerLevel level = context.level();
        long gameTime = context.gameTime();
        boolean originalMobGriefing = context.originalMobGriefing();
        Holder<WorldClock> clock = context.clock();
        long originalClockTime = context.originalClockTime();
        BrightSpider brightSpider = context.brightSpider();
        DarkSpider builder = context.builder();
        PathfinderMob recruit = context.recruit();
        DarkSpider griefingDisabledSpider = context.griefingDisabledSpider();
        DarkSpider openSkySpider = context.openSkySpider();
        PathfinderMob retaliationTarget = context.retaliationTarget();

        try {
            setClockTime(level, clock, 18_000L);
            RetoldMobStates.getOrCreate(brightSpider, gameTime).markFed(gameTime);
            helper.assertFalse(
                    RetoldSpiderLairEvents.tryBuildOrRepairLair(
                            level,
                            brightSpider,
                            gameTime
                    ),
                    "A recently fed Spider must not establish a lair in bright conditions"
            );
            helper.assertTrue(
                    RetoldAnimalHomes.get(brightSpider) == null,
                    "Bright-condition rejection must not assign a Spider lair home"
            );
            brightSpider.discard();
            RetoldAnimalHomes.remove(brightSpider);
            RetoldMobStates.remove(brightSpider);

            RetoldMobStates.getOrCreate(openSkySpider, gameTime).markFed(gameTime);
            helper.assertFalse(
                    RetoldSpiderLairEvents.tryBuildOrRepairLair(
                            level,
                            openSkySpider,
                            gameTime
                    ),
                    "Outdoor nighttime darkness must not establish a Spider lair under open sky"
            );
            helper.assertTrue(
                    RetoldAnimalHomes.get(openSkySpider) == null,
                    "Open-sky rejection must not assign a Spider lair home"
            );
            openSkySpider.discard();
            RetoldAnimalHomes.remove(openSkySpider);
            RetoldMobStates.remove(openSkySpider);

            RetoldMobStates.getOrCreate(builder, gameTime).markFed(gameTime);
            helper.assertTrue(
                    RetoldSpiderLairEvents.tryBuildOrRepairLair(
                            level,
                            builder,
                            gameTime
                    ),
                    "A recently fed Spider must establish its first supported cobweb in darkness"
            );

            RetoldAnimalHomeMemory builderHome = RetoldAnimalHomes.get(builder);
            RetoldAnimalHomeMemory recruitHome = RetoldAnimalHomes.get(recruit);

            helper.assertTrue(
                    builderHome != null
                            && builderHome.type() == RetoldAnimalHomeType.SPIDER_LAIR,
                    "The builder must persist the cobweb as a Spider lair home"
            );
            helper.assertTrue(
                    recruitHome != null
                            && recruitHome.type() == RetoldAnimalHomeType.SPIDER_LAIR
                            && recruitHome.dimension().equals(builderHome.dimension())
                            && recruitHome.pos().equals(builderHome.pos()),
                    "A nearby Cave Spider must share the new Spider lair"
            );
            helper.assertValueEqual(
                    RetoldSpiderLairEvents.countCobwebs(level, builderHome.pos()),
                    1,
                    "A new lair must begin with exactly one cobweb"
            );

            for (int index = 1; index < EXPECTED_WEB_CAP; index++) {
                helper.assertTrue(
                        RetoldSpiderLairEvents.tryBuildOrRepairLair(
                                level,
                                builder,
                                gameTime + index * 1_000L
                        ),
                        "A dark lair must expand one cobweb at a time up to its cap"
                );
            }

            helper.assertValueEqual(
                    RetoldSpiderLairEvents.countCobwebs(level, builderHome.pos()),
                    EXPECTED_WEB_CAP,
                    "A Spider lair must stop at 50 nearby cobwebs"
            );
            helper.assertFalse(
                    RetoldSpiderLairEvents.tryBuildOrRepairLair(
                            level,
                            builder,
                            gameTime + EXPECTED_WEB_CAP * 1_000L
                    ),
                    "A complete Spider lair must not exceed 50 cobwebs"
            );

            BlockPos removedWeb = findCobweb(level, builderHome.pos());
            helper.assertTrue(
                    removedWeb != null,
                    "The complete lair must contain a cobweb that can be removed"
            );
            level.setBlock(
                    removedWeb,
                    Blocks.AIR.defaultBlockState(),
                    Block.UPDATE_ALL
            );
            helper.assertTrue(
                    RetoldSpiderLairEvents.tryBuildOrRepairLair(
                            level,
                            builder,
                            gameTime + (EXPECTED_WEB_CAP + 1L) * 1_000L
                    ),
                    "A Spider at its dark lair must repair a removed cobweb"
            );
            helper.assertValueEqual(
                    RetoldSpiderLairEvents.countCobwebs(level, builderHome.pos()),
                    EXPECTED_WEB_CAP,
                    "Repair must restore the lair without exceeding its cap"
            );

            RetoldMobStates.getOrCreate(
                    griefingDisabledSpider,
                    gameTime
            ).markFed(gameTime);
            level.getGameRules().set(
                    GameRules.MOB_GRIEFING,
                    false,
                    level.getServer()
            );
            helper.assertFalse(
                    RetoldSpiderLairEvents.tryBuildOrRepairLair(
                            level,
                            griefingDisabledSpider,
                            gameTime + (EXPECTED_WEB_CAP + 2L) * 1_000L
                    ),
                    "mobGriefing=false must prevent a dark fed Spider from placing cobwebs"
            );
            helper.assertTrue(
                    RetoldAnimalHomes.get(griefingDisabledSpider) == null,
                    "Blocked cobweb placement must not leave a lair home"
            );
            level.getGameRules().set(
                    GameRules.MOB_GRIEFING,
                    true,
                    level.getServer()
            );

            builder.snapTo(
                    helper.absoluteVec(new Vec3(16.5D, 2.0D, 2.5D))
            );
            setClockTime(level, clock, 6_000L);
            RetoldSpiderLairEvents.tick(
                    level,
                    builder,
                    gameTime + (EXPECTED_WEB_CAP + 3L) * 1_000L
            );
            helper.assertTrue(
                    RetoldAiControl.isControlledAsBy(
                            builder,
                            RetoldAiControlMode.REGROUP,
                            RetoldAiControlOwner.SPIDER_LAIR
                    ),
                    "A Spider away from its lair in daylight must begin returning home"
            );

            helper.assertTrue(
                    RetoldCombatTargets.applyAttackTarget(
                            builder,
                            retaliationTarget,
                            RetoldTargetSource.RETALIATION
                    ),
                    "The return-home regression setup must assign a retaliation target"
            );
            RetoldSpiderLairEvents.tick(
                    level,
                    builder,
                    gameTime + (EXPECTED_WEB_CAP + 3L) * 1_000L + 1L
            );
            helper.assertTrue(
                    builder.getTarget() == retaliationTarget
                            && !RetoldAiControl.isControlledBy(
                            builder,
                            RetoldAiControlOwner.SPIDER_LAIR
                    ),
                    "Retaliation must immediately interrupt daylight lair return"
            );

            RetoldCombatTargets.clearTargetReferencesAndAggression(
                    builder,
                    retaliationTarget,
                    true
            );
            RetoldSpiderLairEvents.tick(
                    level,
                    builder,
                    gameTime + (EXPECTED_WEB_CAP + 3L) * 1_000L + 2L
            );
            helper.assertTrue(
                    RetoldAiControl.isControlledBy(
                            builder,
                            RetoldAiControlOwner.SPIDER_LAIR
                    ),
                    "The Spider must resume its daylight return after danger clears"
            );

            setClockTime(level, clock, 18_000L);
            RetoldSpiderLairEvents.tick(
                    level,
                    builder,
                    gameTime + (EXPECTED_WEB_CAP + 3L) * 1_000L + 3L
            );
            helper.assertFalse(
                    RetoldAiControl.isControlledBy(
                            builder,
                            RetoldAiControlOwner.SPIDER_LAIR
                    ),
                    "Nightfall must release lair-return movement so the Spider can hunt"
            );
        } finally {
            setClockTime(level, clock, originalClockTime);
            level.getGameRules().set(
                    GameRules.MOB_GRIEFING,
                    originalMobGriefing,
                    level.getServer()
            );
            RetoldCombatTargets.clearTargetReferencesAndAggression(
                    builder,
                    retaliationTarget,
                    true
            );
            cleanup(brightSpider);
            cleanup(builder);
            cleanup(recruit);
            cleanup(griefingDisabledSpider);
            cleanup(openSkySpider);
            cleanup(retaliationTarget);
        }

        helper.succeed();
    }

    private static void buildDarkSupportedArea(
            GameTestHelper helper,
            int minX,
            int maxX,
            int minZ,
            int maxZ
    ) {
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
                helper.setBlock(new BlockPos(x, 4, z), Blocks.STONE);

                if (x == minX || x == maxX || z == minZ || z == maxZ) {
                    helper.setBlock(new BlockPos(x, 2, z), Blocks.STONE);
                    helper.setBlock(new BlockPos(x, 3, z), Blocks.STONE);
                }
            }
        }
    }

    private static void buildOpenSupportedArea(
            GameTestHelper helper,
            int minX,
            int maxX,
            int minZ,
            int maxZ
    ) {
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
            }
        }
    }

    private static <T extends Spider> T spawnSpider(
            GameTestHelper helper,
            T spider,
            Vec3 relativePosition
    ) {
        Vec3 position = helper.absoluteVec(relativePosition);

        spider.snapTo(position.x(), position.y(), position.z(), 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(spider);
        return spider;
    }

    private static BlockPos findCobweb(
            ServerLevel level,
            BlockPos center
    ) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -3; dz <= 3; dz++) {
                    mutable.set(
                            center.getX() + dx,
                            center.getY() + dy,
                            center.getZ() + dz
                    );

                    if (level.getBlockState(mutable).is(Blocks.COBWEB)) {
                        return mutable.immutable();
                    }
                }
            }
        }

        return null;
    }

    private static void setClockTime(
            ServerLevel level,
            Holder<WorldClock> clock,
            long clockTime
    ) {
        level.getServer().clockManager().setTotalTicks(clock, clockTime);
    }

    private static void cleanup(PathfinderMob mob) {
        if (mob == null) {
            return;
        }

        RetoldAiControl.clear(mob);
        RetoldAnimalHomes.remove(mob);
        RetoldMobStates.remove(mob);
        mob.discard();
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Retold.MODID, path);
    }

    private record LairTestContext(
            ServerLevel level,
            long gameTime,
            boolean originalMobGriefing,
            Holder<WorldClock> clock,
            long originalClockTime,
            BrightSpider brightSpider,
            DarkSpider builder,
            PathfinderMob recruit,
            DarkSpider griefingDisabledSpider,
            DarkSpider openSkySpider,
            PathfinderMob retaliationTarget
    ) {
    }

    private static final class BrightSpider extends Spider {
        private BrightSpider(ServerLevel level) {
            super(EntityTypes.SPIDER, level);
        }

        @Override
        public float getLightLevelDependentMagicValue() {
            return 1.0F;
        }
    }

    private static final class DarkSpider extends Spider {
        private DarkSpider(ServerLevel level) {
            super(EntityTypes.SPIDER, level);
        }

        @Override
        public float getLightLevelDependentMagicValue() {
            return 0.0F;
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
