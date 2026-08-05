package cz.xefensor.retold.behavior.species;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.control.RetoldAiControlMode;
import cz.xefensor.retold.behavior.control.RetoldAiControlOwner;
import cz.xefensor.retold.behavior.core.RetoldBehaviorMovement;
import cz.xefensor.retold.behavior.food.RetoldFeedingPose;
import cz.xefensor.retold.behavior.home.RetoldAnimalHomeMemory;
import cz.xefensor.retold.behavior.home.RetoldAnimalHomeType;
import cz.xefensor.retold.behavior.home.RetoldAnimalHomes;
import cz.xefensor.retold.behavior.home.RetoldAnimalDailyRhythm;
import cz.xefensor.retold.behavior.performance.RetoldBlockTargetSearch;
import cz.xefensor.retold.behavior.performance.RetoldBehaviorPerf;
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
import net.minecraft.server.level.TicketType;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public final class RetoldBatColonyGameTests {
    private static final Identifier EMPTY_STRUCTURE =
            Identifier.withDefaultNamespace("empty");
    private static final Identifier TALL_CAVE_TEST_STRUCTURE =
            Identifier.withDefaultNamespace("woodland_mansion/2x2_a1");
    private static final int BAT_TPS_SAMPLE_SIZE = 64;
    private static final int BAT_TPS_OBSERVATION_TICKS = 200;
    private static final int DAYTIME_SETTLE_SAMPLE_SIZE = 8;
    private static final int DAYTIME_SETTLE_OBSERVATION_TICKS = 360;
    private static final int DISTURBED_BAT_TEST_TICKS = 20 * 12;
    private static final int DISTURBED_BAT_RETENTION_TICKS = 20 * 6;
    private static final int DISTURBED_BAT_RELEASE_TICKS = 20 * 11;
    private static final double MAX_SERVER_TICK_MILLIS = 50.0D;

    private RetoldBatColonyGameTests() {
    }

    public static void register(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> colonyEnvironment =
                event.registerEnvironment(
                        id("isolated_bat_clock"),
                        new TestEnvironmentDefinition.AllOf()
                );
        Holder<TestEnvironmentDefinition<?>> tallCaveEnvironment =
                event.registerEnvironment(
                        id("isolated_bat_tall_cave"),
                        new TestEnvironmentDefinition.AllOf()
                );
        Holder<TestEnvironmentDefinition<?>> tpsEnvironment =
                event.registerEnvironment(
                        id("isolated_bat_tps"),
                        new TestEnvironmentDefinition.AllOf()
                );
        TestData<Holder<TestEnvironmentDefinition<?>>> colonyTestData =
                new TestData<>(
                        colonyEnvironment,
                        EMPTY_STRUCTURE,
                        80,
                        0,
                        true
                );
        TestData<Holder<TestEnvironmentDefinition<?>>> tallCaveTestData =
                new TestData<>(
                        tallCaveEnvironment,
                        TALL_CAVE_TEST_STRUCTURE,
                        80,
                        0,
                        true
                );
        TestData<Holder<TestEnvironmentDefinition<?>>> tpsTestData =
                new TestData<>(
                        tpsEnvironment,
                        TALL_CAVE_TEST_STRUCTURE,
                        BAT_TPS_OBSERVATION_TICKS + 40,
                        0,
                        true
                );
        TestData<Holder<TestEnvironmentDefinition<?>>> daytimeSettleTestData =
                new TestData<>(
                        tallCaveEnvironment,
                        TALL_CAVE_TEST_STRUCTURE,
                        DAYTIME_SETTLE_OBSERVATION_TICKS + 40,
                        0,
                        true
                );
        TestData<Holder<TestEnvironmentDefinition<?>>> disturbedBatTestData =
                new TestData<>(
                        tallCaveEnvironment,
                        TALL_CAVE_TEST_STRUCTURE,
                        DISTURBED_BAT_TEST_TICKS + 40,
                        0,
                        true
                );

        event.registerTest(
                id("bats_share_dark_roosts_hunt_at_night_and_share_panic"),
                new InlineGameTest(
                        colonyTestData,
                        RetoldBatColonyGameTests::batsShareDarkRoostsHuntAtNightAndSharePanic
                )
        );
        event.registerTest(
                id("bats_find_high_ceiling_and_search_in_five_member_parties"),
                new InlineGameTest(
                        tallCaveTestData,
                        RetoldBatColonyGameTests::batsFindHighCeilingAndSearchInFiveMemberParties
                )
        );
        event.registerTest(
                id("large_bat_colony_sustains_twenty_tps"),
                new InlineGameTest(
                        tpsTestData,
                        RetoldBatColonyGameTests::largeBatColonySustainsTwentyTps
                )
        );
        event.registerTest(
                id("awake_bats_settle_at_daytime_roosts"),
                new InlineGameTest(
                        daytimeSettleTestData,
                        RetoldBatColonyGameTests::awakeBatsSettleAtDaytimeRoosts
                )
        );
        event.registerTest(
                id("disturbed_bat_stays_panicked_before_roosting"),
                new InlineGameTest(
                        disturbedBatTestData,
                        RetoldBatColonyGameTests::disturbedBatStaysPanickedBeforeRoosting
                )
        );
    }

    private static void disturbedBatStaysPanickedBeforeRoosting(
            GameTestHelper helper
    ) {
        buildTallDarkCave(helper);
        helper.runAfterDelay(5, () -> startDisturbedBatObservation(helper));
    }

    private static void startDisturbedBatObservation(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Holder<WorldClock> clock = level.dimensionType().defaultClock().orElseThrow();
        long originalClockTime = level.getServer().clockManager().getTotalTicks(clock);
        Vec3 roostPosition = helper.absoluteVec(
                new Vec3(7.5D, 21.1D, 7.5D)
        );
        Bat bat = helper.spawn(EntityTypes.BAT, 7, 21, 7);

        setClockTime(level, clock, 6_000L);
        bat.snapTo(
                roostPosition.x(),
                roostPosition.y(),
                roostPosition.z(),
                0.0F,
                0.0F
        );
        bat.setPersistenceRequired();
        bat.setResting(true);
        bat.setResting(false);
        RetoldBatColonyEvents.onVanillaRoostDisturbed(level, bat);

        helper.runAfterDelay(
                10,
                () -> verifyDisturbedBatPanicStarted(
                        helper,
                        bat,
                        clock,
                        originalClockTime
                )
        );
    }

    private static void verifyDisturbedBatPanicStarted(
            GameTestHelper helper,
            Bat bat,
            Holder<WorldClock> clock,
            long originalClockTime
    ) {
        helper.assertTrue(
                !bat.isResting()
                        && RetoldAiControl.isControlledAsBy(
                        bat,
                        RetoldAiControlMode.FLEE,
                        RetoldAiControlOwner.BAT_COLONY
                ),
                "A vanilla roost disturbance must start owned panic flight"
        );

        helper.runAfterDelay(
                DISTURBED_BAT_RETENTION_TICKS,
                () -> helper.assertTrue(
                        !bat.isResting()
                                && RetoldAiControl.isControlledAsBy(
                                bat,
                                RetoldAiControlMode.FLEE,
                                RetoldAiControlOwner.BAT_COLONY
                        ),
                        "A disturbed Bat must remain awake and panicked for longer than the old three-second window"
                )
        );
        helper.runAfterDelay(
                DISTURBED_BAT_RELEASE_TICKS,
                () -> finishDisturbedBatObservation(
                        helper,
                        bat,
                        clock,
                        originalClockTime
                )
        );
    }

    private static void finishDisturbedBatObservation(
            GameTestHelper helper,
            Bat bat,
            Holder<WorldClock> clock,
            long originalClockTime
    ) {
        try {
            helper.assertTrue(
                    !RetoldAiControl.isControlledAsBy(
                            bat,
                            RetoldAiControlMode.FLEE,
                            RetoldAiControlOwner.BAT_COLONY
                    ),
                    "A disturbed Bat must leave panic after its ten-second recovery window"
            );
            helper.succeed();
        } finally {
            setClockTime(helper.getLevel(), clock, originalClockTime);
            cleanup(bat);
            clearTallDarkCave(helper);
        }
    }

    private static void awakeBatsSettleAtDaytimeRoosts(
            GameTestHelper helper
    ) {
        buildTallDarkCave(helper);
        helper.runAfterDelay(5, () -> startDaytimeSettleObservation(helper));
    }

    private static void startDaytimeSettleObservation(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Holder<WorldClock> clock = level.dimensionType().defaultClock().orElseThrow();
        long originalClockTime = level.getServer().clockManager().getTotalTicks(clock);
        Bat[] bats = new Bat[DAYTIME_SETTLE_SAMPLE_SIZE];

        setClockTime(level, clock, 6_000L);

        for (int index = 0; index < bats.length; index++) {
            Bat bat = helper.spawn(EntityTypes.BAT, 7, 3, 3 + index);

            bat.setPersistenceRequired();
            bat.setResting(false);
            bats[index] = bat;
        }

        helper.runAfterDelay(
                DAYTIME_SETTLE_OBSERVATION_TICKS,
                () -> finishDaytimeSettleObservation(
                        helper,
                        bats,
                        clock,
                        originalClockTime
                )
        );
    }

    private static void finishDaytimeSettleObservation(
            GameTestHelper helper,
            Bat[] bats,
            Holder<WorldClock> clock,
            long originalClockTime
    ) {
        ServerLevel level = helper.getLevel();
        int restingBats = 0;
        int validRoosts = 0;
        Set<BlockPos> occupiedRoosts = new HashSet<>();
        StringBuilder batStates = new StringBuilder();

        for (int index = 0; index < bats.length; index++) {
            Bat bat = bats[index];
            Vec3 destination = bat == null
                    ? null
                    : RetoldBatColonyEvents.daytimeRoostDestination(bat);

            if (bat != null && bat.isResting()) {
                restingBats++;
                occupiedRoosts.add(bat.blockPosition());
            }

            if (bat != null && RetoldBlockTargetSearch.isBatRoostAt(
                    level,
                    bat.blockPosition()
            )) {
                validRoosts++;
            }

            batStates.append(" [")
                    .append(index)
                    .append(": pos=")
                    .append(bat == null ? "none" : bat.position())
                    .append(", blockPos=")
                    .append(bat == null ? "none" : bat.blockPosition())
                    .append(", maxY=")
                    .append(bat == null ? "none" : bat.getBoundingBox().maxY)
                    .append(", mode=")
                    .append(bat == null ? "none" : RetoldAiControl.getMode(bat))
                    .append(", home=")
                    .append(bat == null ? "none" : RetoldAnimalHomes.get(bat))
                    .append(", destination=")
                    .append(destination)
                    .append(", destinationClear=")
                    .append(bat == null || destination == null
                            ? "none"
                            : level.noCollision(
                            bat,
                            bat.getBoundingBox().move(
                                    destination.subtract(bat.position())
                            )
                    ))
                    .append(']');
        }

        try {
            helper.assertTrue(
                    restingBats == DAYTIME_SETTLE_SAMPLE_SIZE,
                    "Every awake Bat must finish its daytime return by sleeping; resting="
                            + restingBats
                            + batStates
            );
            helper.assertTrue(
                    validRoosts == DAYTIME_SETTLE_SAMPLE_SIZE,
                    "Every sleeping Bat must occupy a valid supported ceiling cell; valid="
                            + validRoosts
                            + batStates
            );
            helper.assertTrue(
                    occupiedRoosts.size() == DAYTIME_SETTLE_SAMPLE_SIZE,
                    "Every sleeping Bat must occupy its own ceiling cell; occupied="
                            + occupiedRoosts.size()
                            + ", roosts="
                            + occupiedRoosts
                            + batStates
            );
            helper.succeed();
        } finally {
            setClockTime(level, clock, originalClockTime);

            for (Bat bat : bats) {
                cleanup(bat);
            }

            clearTallDarkCave(helper);
        }
    }

    private static void largeBatColonySustainsTwentyTps(
            GameTestHelper helper
    ) {
        buildTallDarkCave(helper);
        helper.runAfterDelay(5, () -> startBatTpsObservation(helper));
    }

    private static void startBatTpsObservation(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Holder<WorldClock> clock = level.dimensionType().defaultClock().orElseThrow();
        long originalClockTime = level.getServer().clockManager().getTotalTicks(clock);
        Bat[] bats = new Bat[BAT_TPS_SAMPLE_SIZE];

        setClockTime(level, clock, 6_000L);

        for (int index = 0; index < bats.length; index++) {
            int x = 2 + index % 8;
            int z = 2 + index / 8;
            Bat bat = helper.spawn(EntityTypes.BAT, x, 3, z);

            bat.setPersistenceRequired();
            bat.setResting(false);
            RetoldMobStates.getOrCreate(
                    bat,
                    level.getGameTime()
            ).setHunger(100);
            bats[index] = bat;
        }

        RetoldBehaviorPerf.reset();
        long startedAtNanos = System.nanoTime();
        int startedAtServerTick = level.getServer().getTickCount();

        helper.runAtTickTime(
                105,
                () -> setClockTime(level, clock, 18_000L)
        );
        helper.runAtTickTime(
                205,
                () -> finishBatTpsObservation(
                        helper,
                        bats,
                        clock,
                        originalClockTime,
                        startedAtNanos,
                        startedAtServerTick
                )
        );
    }

    private static void finishBatTpsObservation(
            GameTestHelper helper,
            Bat[] bats,
            Holder<WorldClock> clock,
            long originalClockTime,
            long startedAtNanos,
            int startedAtServerTick
    ) {
        ServerLevel level = helper.getLevel();
        int observedServerTicks = Math.max(
                1,
                level.getServer().getTickCount() - startedAtServerTick
        );
        double elapsedMillis = (System.nanoTime() - startedAtNanos)
                / 1_000_000.0D;
        double averageServerTickMillis = elapsedMillis / observedServerTicks;
        double sustainableTps = Math.min(
                20.0D,
                1_000.0D / averageServerTickMillis
        );
        int aliveBats = 0;
        int ceilingHomes = 0;

        for (Bat bat : bats) {
            if (bat != null && bat.isAlive()) {
                aliveBats++;
            }

            RetoldAnimalHomeMemory home = RetoldAnimalHomes.get(bat);

            if (home != null
                    && RetoldBlockTargetSearch.isBatRoostAt(level, home.pos())) {
                ceilingHomes++;
            }
        }

        try {
            helper.assertValueEqual(
                    aliveBats,
                    BAT_TPS_SAMPLE_SIZE,
                    "The Bat TPS sample must remain alive"
            );
            helper.assertValueEqual(
                    ceilingHomes,
                    BAT_TPS_SAMPLE_SIZE,
                    "Every Bat in the TPS sample must retain a real ceiling home"
            );
            helper.assertTrue(
                    observedServerTicks >= BAT_TPS_OBSERVATION_TICKS,
                    "The Bat TPS test must observe 200 real server ticks; observed="
                            + observedServerTicks
            );
            helper.assertTrue(
                    averageServerTickMillis < MAX_SERVER_TICK_MILLIS,
                    "A 64-Bat day/night colony must sustain 20 TPS; averageTickMs="
                            + averageServerTickMillis
                            + ", sustainableTps="
                            + sustainableTps
            );

            Retold.LOGGER.info(
                    "Bat colony TPS performance: bats={}, serverTicks={}, avgServerTickMs={}, sustainableTps={}",
                    BAT_TPS_SAMPLE_SIZE,
                    observedServerTicks,
                    String.format(
                            java.util.Locale.ROOT,
                            "%.3f",
                            averageServerTickMillis
                    ),
                    String.format(
                            java.util.Locale.ROOT,
                            "%.2f",
                            sustainableTps
                    )
            );
            helper.succeed();
        } finally {
            setClockTime(level, clock, originalClockTime);

            for (Bat bat : bats) {
                cleanup(bat);
            }

            clearTallDarkCave(helper);
            RetoldBehaviorPerf.reset();
        }
    }

    private static void batsFindHighCeilingAndSearchInFiveMemberParties(
            GameTestHelper helper
    ) {
        buildTallDarkCave(helper);
        helper.runAfterDelay(
                5,
                () -> runTallCaveAndHuntingPartyAssertions(helper)
        );
    }

    private static void runTallCaveAndHuntingPartyAssertions(
            GameTestHelper helper
    ) {
        ServerLevel level = helper.getLevel();
        Holder<WorldClock> clock = level.dimensionType().defaultClock().orElseThrow();
        long originalClockTime = level.getServer().clockManager().getTotalTicks(clock);
        long gameTime = level.getGameTime() + 6_000L;
        Bat[] bats = new Bat[6];

        try {
            setClockTime(level, clock, 6_000L);

            for (int index = 0; index < bats.length; index++) {
                bats[index] = helper.spawn(
                        EntityTypes.BAT,
                        7,
                        3,
                        7 + index
                );
                bats[index].setResting(false);
                bats[index].setYRot(-90.0F);
            }

            BlockPos legacyGroundHome = helper.absolutePos(
                    new BlockPos(7, 2, 7)
            );
            RetoldAnimalHomes.rememberSingleHome(
                    level,
                    bats[0],
                    legacyGroundHome,
                    gameTime - 1L
            );

            long checkedBefore = RetoldBehaviorPerf.blockTargetPositionsChecked();
            RetoldBatColonyEvents.tick(level, bats[0], gameTime);
            RetoldBatColonyEvents.tick(level, bats[1], gameTime + 1L);
            long ceilingPositionsChecked =
                    RetoldBehaviorPerf.blockTargetPositionsChecked()
                            - checkedBefore;

            RetoldAnimalHomeMemory roost = RetoldAnimalHomes.get(bats[0]);
            Vec3 firstSlot = RetoldBatColonyEvents.daytimeRoostDestination(bats[0]);
            Vec3 secondSlot = RetoldBatColonyEvents.daytimeRoostDestination(bats[1]);

            helper.assertTrue(
                    roost != null
                            && !roost.pos().equals(legacyGroundHome)
                            && roost.pos().getY() >= helper.absolutePos(
                            new BlockPos(0, 20, 0)
                    ).getY()
                            && RetoldBlockTargetSearch.isBatRoostAt(level, roost.pos())
                            && firstSlot != null
                            && secondSlot != null
                            && firstSlot.y() >= helper.absolutePos(
                            new BlockPos(0, 20, 0)
                    ).getY()
                            && secondSlot.y() >= helper.absolutePos(
                            new BlockPos(0, 20, 0)
                    ).getY()
                            && firstSlot.distanceToSqr(secondSlot) > 0.25D,
                    "Ground-level Bats in a tall cave must discover distributed supported ceiling slots; roost="
                            + roost
                            + ", roostPos="
                            + (roost == null ? "none" : roost.pos())
                            + ", roostValid="
                            + (roost != null && RetoldBlockTargetSearch.isBatRoostAt(
                            level,
                            roost.pos()
                    ))
                            + ", firstSlot="
                            + firstSlot
                            + ", secondSlot="
                            + secondSlot
                            + ", firstPos="
                            + bats[0].blockPosition()
                            + ", secondPos="
                            + bats[1].blockPosition()
                            + ", firstMode="
                            + RetoldAiControl.getMode(bats[0])
                            + ", secondMode="
                            + RetoldAiControl.getMode(bats[1])
            );
            helper.assertTrue(
                    ceilingPositionsChecked <= 4_096L,
                    "Tall-cave Bat roost acquisition must stop early instead of exhaustively scanning the volume; checked="
                            + ceilingPositionsChecked
            );

            bats[0].snapTo(
                    firstSlot.x(),
                    firstSlot.y(),
                    firstSlot.z(),
                    0.0F,
                    0.0F
            );
            RetoldAiControl.clear(bats[0]);
            RetoldBehaviorMovement.clearFlyingPath(bats[0]);

            for (int elapsed = 2; elapsed <= 70 && !bats[0].isResting(); elapsed++) {
                RetoldBatColonyEvents.tick(
                        level,
                        bats[0],
                        gameTime + elapsed
                );
            }

            helper.assertTrue(
                    bats[0].isResting(),
                    "A Bat that reaches its high-ceiling slot must settle there during daylight"
            );

            bats[1].snapTo(
                    firstSlot.x(),
                    firstSlot.y(),
                    firstSlot.z(),
                    0.0F,
                    0.0F
            );
            bats[1].setResting(true);
            bats[1].setDeltaMovement(Vec3.ZERO);
            RetoldAiControl.clear(bats[1]);
            RetoldBehaviorMovement.clearFlyingPath(bats[1]);
            RetoldBatColonyEvents.tick(level, bats[1], gameTime + 71L);

            Vec3 reassignedSlot = RetoldBatColonyEvents.daytimeRoostDestination(
                    bats[1]
            );
            helper.assertTrue(
                    !bats[1].isResting()
                            && reassignedSlot != null
                            && reassignedSlot.distanceToSqr(firstSlot) > 0.25D,
                    "A Bat sharing an occupied sleeping cell must wake and move to a distinct ceiling slot; reassigned="
                            + reassignedSlot
            );

            openTallCaveNightExit(helper);
            setClockTime(level, clock, 18_000L);

            for (int index = 0; index < bats.length; index++) {
                Vec3 position = helper.absoluteVec(
                        new Vec3(2.5D, 10.0D, 4.5D + index)
                );
                bats[index].snapTo(
                        position.x(),
                        position.y(),
                        position.z(),
                        -90.0F,
                        0.0F
                );
                bats[index].setYRot(-90.0F);
                bats[index].setResting(false);
                bats[index].setDeltaMovement(Vec3.ZERO);
                RetoldAiControl.clear(bats[index]);
                RetoldBehaviorMovement.clearFlyingPath(bats[index]);
                RetoldMobStates.getOrCreate(
                        bats[index],
                        gameTime
                ).setHunger(100);
            }

            long nightTime = gameTime + 100L;
            RetoldBatColonyEvents.tick(level, bats[0], nightTime);
            long firstPartyThinkAt = RetoldBatColonyEvents.huntingPartyNextThinkAt(
                    bats[0]
            );
            RetoldBatColonyEvents.tick(level, bats[1], nightTime + 1L);
            helper.assertTrue(
                    RetoldBatColonyEvents.huntingPartyNextThinkAt(bats[0])
                            == firstPartyThinkAt,
                    "A second party member must reuse the shared decision instead of repeating party-wide work"
            );
            RetoldBatColonyEvents.tick(level, bats[5], nightTime + 2L);

            Vec3 partyDirection = RetoldBatColonyEvents.huntingPartyDirection(bats[0]);

            helper.assertTrue(
                    RetoldBatColonyEvents.huntingPartySize(bats[0]) == 5
                            && RetoldBatColonyEvents.huntingPartySize(bats[4]) == 5
                            && RetoldBatColonyEvents.huntingPartySize(bats[5]) == 1
                            && partyDirection != null,
                    "Nearby hungry Bats must split into directional hunting parties capped at five"
            );

            for (int index = 0; index < 5; index++) {
                Vec3 destination = RetoldBatColonyEvents.flightDestination(bats[index]);

                helper.assertTrue(
                        destination != null
                                && RetoldBatColonyEvents.huntingPartyDirection(
                                bats[index]
                        ).dot(partyDirection) > 0.999D
                                && destination.subtract(
                                bats[index].position()
                        ).dot(partyDirection) > 0.0D,
                        "Every member of a five-Bat party must search forward along its shared heading"
                );
            }

            RetoldBatColonyEvents.tick(level, bats[0], nightTime + 200L);
            Vec3 retainedDirection = RetoldBatColonyEvents.huntingPartyDirection(bats[0]);
            helper.assertTrue(
                    retainedDirection != null
                            && retainedDirection.dot(partyDirection) > 0.999D,
                    "A Bat hunting party must keep trying one direction for a meaningful search period"
            );

            RetoldBatColonyEvents.tick(level, bats[0], nightTime + 500L);
            Vec3 changedDirection = RetoldBatColonyEvents.huntingPartyDirection(bats[0]);
            helper.assertTrue(
                    changedDirection != null
                            && changedDirection.dot(partyDirection) < 0.5D,
                    "After its search period, a Bat hunting party must choose a substantially different direction"
            );
            helper.succeed();
        } finally {
            setClockTime(level, clock, originalClockTime);

            for (Bat bat : bats) {
                cleanup(bat);
            }

            clearTallDarkCave(helper);
        }
    }

    private static void batsShareDarkRoostsHuntAtNightAndSharePanic(
            GameTestHelper helper
    ) {
        ServerLevel level = helper.getLevel();
        BlockPos roomCenter = helper.absolutePos(
                new BlockPos(22, 3, 12)
        );
        ChunkPos roomTicketCenter = new ChunkPos(
                roomCenter.getX() >> 4,
                roomCenter.getZ() >> 4
        );
        level.getChunkSource().addTicketWithRadius(
                TicketType.FORCED,
                roomTicketCenter,
                3
        );
        buildDarkRoostRoom(helper);

        Bat founder = helper.spawn(EntityTypes.BAT, 3, 4, 3);
        Bat colonyMember = helper.spawn(EntityTypes.BAT, 4, 4, 3);
        Silverfish prey = helper.spawn(EntityTypes.SILVERFISH, 7, 3, 3);
        Mob attacker = helper.spawn(EntityTypes.ZOMBIE, 8, 2, 6);
        Vec3 foodPosition = helper.absoluteVec(new Vec3(5.5D, 3.0D, 6.5D));
        ItemEntity droppedFood = new ItemEntity(
                level,
                foodPosition.x(),
                foodPosition.y(),
                foodPosition.z(),
                new ItemStack(Items.SPIDER_EYE)
        );
        Holder<WorldClock> clock = level.dimensionType().defaultClock().orElseThrow();
        long originalClockTime = level.getServer().clockManager().getTotalTicks(clock);

        prey.setNoAi(true);
        attacker.setNoAi(true);
        level.addFreshEntity(droppedFood);

        helper.runAfterDelay(
                5,
                () -> runAssertions(
                        helper,
                        founder,
                        colonyMember,
                        prey,
                        attacker,
                        droppedFood,
                        clock,
                        originalClockTime,
                        roomTicketCenter
                )
        );
    }

    private static void runAssertions(
            GameTestHelper helper,
            Bat founder,
            Bat colonyMember,
            Silverfish prey,
            Mob attacker,
            ItemEntity droppedFood,
            Holder<WorldClock> clock,
            long originalClockTime,
            ChunkPos roomTicketCenter
    ) {
        ServerLevel level = helper.getLevel();
        long gameTime = level.getGameTime() + 3_000L;
        Spider combatSpider = null;
        Bat searcher = null;

        try {
            RetoldAiControl.clear(founder);
            RetoldAiControl.clear(colonyMember);
            RetoldAnimalHomes.remove(founder);
            RetoldAnimalHomes.remove(colonyMember);
            founder.setResting(false);
            colonyMember.setResting(false);
            Vec3 founderStart = helper.absoluteVec(
                    new Vec3(3.5D, 4.0D, 3.5D)
            );
            Vec3 memberStart = helper.absoluteVec(
                    new Vec3(4.5D, 4.0D, 3.5D)
            );
            founder.snapTo(
                    founderStart.x(),
                    founderStart.y(),
                    founderStart.z(),
                    0.0F,
                    0.0F
            );
            colonyMember.snapTo(
                    memberStart.x(),
                    memberStart.y(),
                    memberStart.z(),
                    0.0F,
                    0.0F
            );
            setClockTime(level, clock, 6_000L);
            RetoldBatColonyEvents.tick(level, founder, gameTime);
            RetoldBatColonyEvents.tick(level, colonyMember, gameTime + 1L);

            RetoldAnimalHomeMemory founderRoost = RetoldAnimalHomes.get(founder);
            RetoldAnimalHomeMemory memberRoost = RetoldAnimalHomes.get(colonyMember);

            if (founderRoost == null
                    || memberRoost == null
                    || !memberRoost.pos().equals(founderRoost.pos())) {
                RetoldBatColonyEvents.tick(level, founder, gameTime + 1L);
                founderRoost = RetoldAnimalHomes.get(founder);
                memberRoost = RetoldAnimalHomes.get(colonyMember);
            }

            helper.assertTrue(
                    founderRoost != null
                            && founderRoost.type() == RetoldAnimalHomeType.BAT_ROOST
                            && memberRoost != null
                            && memberRoost.pos().equals(founderRoost.pos()),
                    "Nearby Bats must share the same broad roost zone"
            );
            helper.assertTrue(
                    RetoldBlockTargetSearch.isBatRoostAt(
                            level,
                            founderRoost.pos()
                    ),
                    "The shared Bat roost must remain a genuinely dark supported air cell"
            );
            helper.assertFalse(
                    RetoldBlockTargetSearch.isBatRoostAt(
                            level,
                            helper.absolutePos(new BlockPos(47, 4, 3))
                    ),
                    "An open-sky position must not qualify as a Bat roost"
            );

            Vec3 daylightDestination =
                    RetoldBatColonyEvents.daytimeRoostDestination(founder);

            if (daylightDestination != null) {
                founder.snapTo(
                        daylightDestination.x(),
                        daylightDestination.y(),
                        daylightDestination.z(),
                        0.0F,
                        0.0F
                );
                RetoldAiControl.clear(founder);
                RetoldBehaviorMovement.clearFlyingPath(founder);
            }

            for (int elapsed = 1; elapsed <= 60 && !founder.isResting(); elapsed++) {
                RetoldBatColonyEvents.tick(
                        level,
                        founder,
                        gameTime + elapsed
                );
            }
            helper.assertTrue(
                    founder.isResting()
                            && RetoldBlockTargetSearch.isBatRoostAt(
                            level,
                            founder.blockPosition()
                    ),
                    "A daytime Bat beneath a valid dark ceiling must settle after a short individual delay"
            );
            founder.setResting(false);
            founder.setDeltaMovement(Vec3.ZERO);

            level.setBlockAndUpdate(
                    founderRoost.pos().above(),
                    Blocks.AIR.defaultBlockState()
            );
            helper.assertFalse(
                    RetoldBlockTargetSearch.isBatRoostAt(
                            level,
                            founderRoost.pos()
                    ),
                    "Breaking one ceiling block must invalidate only that hanging spot"
            );

            RetoldMobStates.getOrCreate(founder, gameTime).setHunger(100);
            RetoldAiControl.clear(founder);
            founder.setResting(false);
            founder.setDeltaMovement(Vec3.ZERO);
            Vec3 farPosition = helper.absoluteVec(
                    new Vec3(26.5D, 3.0D, 6.5D)
            );
            founder.snapTo(
                    farPosition.x(),
                    farPosition.y(),
                    farPosition.z(),
                    0.0F,
                    0.0F
            );
            RetoldAnimalHomeMemory repairedRoost = null;

            /*
             * A full-suite tick can exhaust a shared block-search or path-start
             * budget before this test executes. The gameplay contract is that
             * the Bat retries and obtains a real supported-ceiling route, not
             * that one particular budget window must admit the work.
             */
            for (int attempt = 0; attempt < 5; attempt++) {
                RetoldBatColonyEvents.tick(
                        level,
                        founder,
                        gameTime + 10L + attempt
                );
                repairedRoost = RetoldAnimalHomes.get(founder);

                if (repairedRoost != null
                        && !repairedRoost.pos().equals(founderRoost.pos())
                        && RetoldBlockTargetSearch.isBatRoostAt(
                        level,
                        repairedRoost.pos()
                )
                        && RetoldAiControl.isControlledAsBy(
                        founder,
                        RetoldAiControlMode.SHELTER,
                        RetoldAiControlOwner.BAT_COLONY
                )
                        && RetoldBehaviorMovement.hasFlyingPath(founder)) {
                    break;
                }
            }

            helper.assertTrue(
                    !RetoldAnimalDailyRhythm.isNight(level)
                            && repairedRoost != null
                            && !repairedRoost.pos().equals(founderRoost.pos())
                            && RetoldBlockTargetSearch.isBatRoostAt(
                            level,
                            repairedRoost.pos()
                    )
                            && founder.getTarget() == null
                            && RetoldAiControl.isControlledAsBy(
                            founder,
                            RetoldAiControlMode.SHELTER,
                            RetoldAiControlOwner.BAT_COLONY
                    )
                            && RetoldBehaviorMovement.hasFlyingPath(founder)
                            && RetoldBatColonyEvents.applyOwnedFlightStep(
                            level,
                            founder
                    ),
                    "A Bat whose ceiling was broken must replace the stale home with an owned supported-ceiling route; mode="
                            + RetoldAiControl.getMode(founder)
                            + ", target="
                            + founder.getTarget()
                            + ", resting="
                            + founder.isResting()
                            + ", batPos="
                            + founder.blockPosition()
                            + ", oldRoostPos="
                            + founderRoost.pos()
                            + ", repairedRoostPos="
                            + (repairedRoost == null ? "none" : repairedRoost.pos())
                            + ", insideZone="
                            + RetoldBatColonyEvents.isInsideRoostZone(
                            founder.blockPosition(),
                            repairedRoost == null
                                    ? founderRoost.pos()
                                    : repairedRoost.pos()
                    )
                            + ", clearRoute="
                            + RetoldBatColonyEvents.hasClearRoostBiasPath(
                            level,
                            founder,
                            Vec3.atCenterOf(
                                    repairedRoost == null
                                            ? founderRoost.pos()
                                            : repairedRoost.pos()
                            )
                    )
            );
            helper.assertTrue(
                    founder.getDeltaMovement().lengthSqr() > 0.0D,
                    "An owned daytime return must actively fly the Bat along its next safe path waypoint"
            );

            RetoldAiControl.clear(founder);
            founder.setDeltaMovement(Vec3.ZERO);
            buildReturnBlockingWall(helper, Blocks.STONE);
            RetoldBehaviorMovement.clearFlyingPath(founder);
            RetoldBatColonyEvents.tick(level, founder, gameTime + 12L);
            helper.assertTrue(
                    RetoldAiControl.isControlledAsBy(
                            founder,
                            RetoldAiControlMode.SHELTER,
                            RetoldAiControlOwner.BAT_COLONY
                    )
                            && RetoldBehaviorMovement.hasFlyingPath(founder),
                    "A wall between a Bat and its colony must produce an obstacle-aware daytime route"
            );
            helper.assertTrue(
                    RetoldBatColonyEvents.applyOwnedFlightStep(
                            level,
                            founder
                    )
                            && founder.getDeltaMovement().lengthSqr() > 0.0D,
                    "The owned daytime return must follow the detour instead of steering into the wall"
            );
            buildReturnBlockingWall(helper, Blocks.AIR);

            Vec3 insideZonePosition = helper.absoluteVec(
                    new Vec3(13.5D, 3.0D, 6.5D)
            );
            founder.snapTo(
                    insideZonePosition.x(),
                    insideZonePosition.y(),
                    insideZonePosition.z(),
                    0.0F,
                    0.0F
            );
            founder.setDeltaMovement(Vec3.ZERO);

            setClockTime(level, clock, 18_000L);

            searcher = helper.spawn(EntityTypes.BAT, 36, 3, 12);
            searcher.setResting(false);
            RetoldMobStates.getOrCreate(searcher, gameTime).setHunger(100);
            RetoldBatColonyEvents.tick(level, searcher, gameTime + 13L);
            helper.assertTrue(
                    searcher.getTarget() == null
                            && RetoldAiControl.isControlledAsBy(
                            searcher,
                            RetoldAiControlMode.SEARCH,
                            RetoldAiControlOwner.BAT_COLONY
                    )
                            && RetoldBehaviorMovement.hasFlyingPath(searcher),
                    "A hungry nighttime Bat without nearby food must actively search along a flying path; mode="
                            + RetoldAiControl.getMode(searcher)
                            + ", target="
                            + searcher.getTarget()
                            + ", hasPath="
                            + RetoldBehaviorMovement.hasFlyingPath(searcher)
            );
            searcher.setDeltaMovement(Vec3.ZERO);
            helper.assertTrue(
                    RetoldBatColonyEvents.applyOwnedFlightStep(level, searcher)
                            && searcher.getDeltaMovement().lengthSqr() > 0.0D,
                    "Night food search must follow a path waypoint instead of waiting in place"
            );
            cleanup(searcher);
            searcher = null;

            RetoldAiControl.clear(founder);
            RetoldBatColonyEvents.tick(level, founder, gameTime + 13L);

            helper.assertTrue(
                    founder.getTarget() == null
                            && RetoldAiControl.isControlledAsBy(
                            founder,
                            RetoldAiControlMode.FEED,
                            RetoldAiControlOwner.BAT_COLONY
                    ),
                    "A hungry Bat must prefer suitable dropped food over living arthropod prey"
            );
            founder.setDeltaMovement(Vec3.ZERO);
            Vec3 feedingDestination =
                    RetoldBatColonyEvents.flightDestination(founder);
            helper.assertTrue(
                    feedingDestination != null
                            && feedingDestination.distanceToSqr(
                            droppedFood.position()
                    ) < 0.01D
                            && RetoldBatColonyEvents.applyOwnedFlightStep(
                            level,
                            founder
                    )
                            && founder.getDeltaMovement().lengthSqr() > 0.0D,
                    "Owned feeding flight must follow a path toward the dropped Spider Eye"
            );

            int hungerBeforeDroppedFood = RetoldMobStates.get(founder).hunger();
            Vec3 foodPosition = droppedFood.position();
            founder.snapTo(
                    foodPosition.x(),
                    foodPosition.y(),
                    foodPosition.z(),
                    0.0F,
                    0.0F
            );
            RetoldBatColonyEvents.tick(level, founder, gameTime + 14L);
            helper.assertTrue(
                    droppedFood.isRemoved()
                            && RetoldMobStates.get(founder).hunger()
                            < hungerBeforeDroppedFood,
                    "A Bat must consume one suitable dropped Spider Eye and relieve hunger"
            );
            helper.assertTrue(
                    RetoldAiControl.isControlledAsByWithReason(
                            founder,
                            RetoldAiControlMode.FEED,
                            RetoldAiControlOwner.FOOD,
                            "feeding_pose"
                    ),
                    "A Bat must hold the shared stationary feeding pose after consuming easy food"
            );
            RetoldFeedingPose.tick(
                    founder,
                    gameTime + RetoldFeedingPose.DURATION_TICKS + 15L
            );

            Vec3 routedHuntStart = helper.absoluteVec(
                    new Vec3(18.5D, 3.0D, 3.5D)
            );
            founder.snapTo(
                    routedHuntStart.x(),
                    routedHuntStart.y(),
                    routedHuntStart.z(),
                    0.0F,
                    0.0F
            );
            founder.setDeltaMovement(Vec3.ZERO);
            buildHuntDetourWall(helper, Blocks.STONE);
            RetoldBatColonyEvents.tick(level, founder, gameTime + 15L);

            if (founder.getTarget() != prey) {
                RetoldBatColonyEvents.tick(level, founder, gameTime + 16L);
            }

            helper.assertTrue(
                    founder.getTarget() == prey
                            && RetoldAiControl.isControlledAsBy(
                            founder,
                            RetoldAiControlMode.HUNT,
                            RetoldAiControlOwner.BAT_COLONY
                    )
                            && RetoldBehaviorMovement.hasFlyingPath(founder),
                    "After easy food is gone, a still-hungry Bat must pathfind around obstacles to hunt arthropods at night; mode="
                            + RetoldAiControl.getMode(founder)
                            + ", target="
                            + founder.getTarget()
                            + ", hasPath="
                            + RetoldBehaviorMovement.hasFlyingPath(founder)
            );
            founder.setDeltaMovement(Vec3.ZERO);
            RetoldBatColonyEvents.applyOwnedFlightStep(level, founder);
            helper.assertTrue(
                    founder.getDeltaMovement().dot(
                            prey.position().subtract(founder.position())
                    ) > 0.0D,
                    "Owned hunting flight must steer toward the current arthropod prey"
            );

            int hungerBeforeBite = RetoldMobStates.get(founder).hunger();
            prey.setHealth(1.0F);
            Vec3 preyPosition = prey.position();
            founder.snapTo(
                    preyPosition.x() - 0.5D,
                    preyPosition.y(),
                    preyPosition.z(),
                    0.0F,
                    0.0F
            );
            RetoldBatColonyEvents.tick(level, founder, gameTime + 31L);

            helper.assertTrue(
                    !prey.isAlive()
                            && RetoldMobStates.get(founder).hunger() < hungerBeforeBite
                            && founder.getTarget() == null,
                    "A close Bat bite must damage, consume, and release defeated arthropod prey"
            );

            combatSpider = helper.spawn(EntityTypes.SPIDER, 7, 3, 6);
            combatSpider.setNoAi(true);
            RetoldMobStates.getOrCreate(founder, gameTime).setHunger(100);
            RetoldMobStates.getOrCreate(colonyMember, gameTime).setHunger(100);
            RetoldAiControl.clear(founder);
            RetoldAiControl.clear(colonyMember);
            RetoldBehaviorMovement.clearFlyingPath(founder);
            RetoldBehaviorMovement.clearFlyingPath(colonyMember);
            Vec3 sharedAttackPosition = helper.absoluteVec(
                    new Vec3(5.5D, 3.5D, 5.5D)
            );
            founder.snapTo(
                    sharedAttackPosition.x(),
                    sharedAttackPosition.y(),
                    sharedAttackPosition.z(),
                    0.0F,
                    0.0F
            );
            colonyMember.snapTo(
                    sharedAttackPosition.x(),
                    sharedAttackPosition.y(),
                    sharedAttackPosition.z(),
                    0.0F,
                    0.0F
            );
            RetoldBatColonyEvents.tick(level, founder, gameTime + 41L);
            RetoldBatColonyEvents.tick(level, colonyMember, gameTime + 41L);
            founder.setDeltaMovement(Vec3.ZERO);
            colonyMember.setDeltaMovement(Vec3.ZERO);
            Vec3 founderApproach = RetoldBatColonyEvents.flightDestination(
                    founder
            );
            Vec3 memberApproach = RetoldBatColonyEvents.flightDestination(
                    colonyMember
            );

            helper.assertTrue(
                    founder.getTarget() == combatSpider
                            && colonyMember.getTarget() == combatSpider
                            && founderApproach != null
                            && memberApproach != null
                            && founderApproach.distanceToSqr(memberApproach)
                            > 0.0001D,
                    "Bats attacking one arthropod must use separated approach paths instead of overlapping"
            );

            founder.invulnerableTime = 0;
            helper.assertTrue(
                    founder.hurtServer(
                            level,
                            level.damageSources().mobAttack(combatSpider),
                            1.0F
                    ),
                    "The fearless combat setup must let the Spider bite one attacking Bat"
            );
            helper.assertTrue(
                    founder.getTarget() == combatSpider
                            && RetoldAiControl.isControlledAsBy(
                            founder,
                            RetoldAiControlMode.HUNT,
                            RetoldAiControlOwner.BAT_COLONY
                    )
                            && !RetoldAiControl.isControlledAsBy(
                            colonyMember,
                            RetoldAiControlMode.FLEE,
                            RetoldAiControlOwner.BAT_COLONY
                    ),
                    "An arthropod bite must make only the struck Bat dodge without routing the hunt"
            );

            colonyMember.invulnerableTime = 0;
            helper.assertTrue(
                    colonyMember.hurtServer(
                            level,
                            level.damageSources().mobAttack(attacker),
                            1.0F
                    ),
                    "The unrelated-danger setup must inflict real damage on one Bat"
            );
            helper.assertTrue(
                    RetoldAiControl.isControlledAsBy(
                            colonyMember,
                            RetoldAiControlMode.FLEE,
                            RetoldAiControlOwner.BAT_COLONY
                    )
                            && !RetoldAiControl.isControlledAsBy(
                            founder,
                            RetoldAiControlMode.FLEE,
                            RetoldAiControlOwner.BAT_COLONY
                    )
                            && !colonyMember.isResting(),
                    "Unrelated danger must panic the victim immediately without synchronously routing every Bat"
            );
            helper.succeed();
        } finally {
            setClockTime(level, clock, originalClockTime);
            cleanup(founder);
            cleanup(colonyMember);
            cleanup(prey);
            cleanup(attacker);
            cleanup(combatSpider);
            cleanup(searcher);
            droppedFood.discard();
            clearDarkRoostRoom(helper);
            level.getChunkSource().removeTicketWithRadius(
                    TicketType.FORCED,
                    roomTicketCenter,
                    3
            );
        }
    }

    private static void buildDarkRoostRoom(GameTestHelper helper) {
        for (int x = 0; x <= 45; x++) {
            for (int z = 0; z <= 25; z++) {
                helper.setBlock(x, 1, z, Blocks.STONE);
                helper.setBlock(x, 5, z, Blocks.STONE);
            }
        }

        for (int y = 2; y <= 4; y++) {
            for (int x = 0; x <= 45; x++) {
                helper.setBlock(x, y, 0, Blocks.STONE);
                helper.setBlock(x, y, 25, Blocks.STONE);
            }

            for (int z = 0; z <= 25; z++) {
                helper.setBlock(0, y, z, Blocks.STONE);
                helper.setBlock(45, y, z, Blocks.STONE);
            }
        }
    }

    private static void buildTallDarkCave(GameTestHelper helper) {
        clearTallDarkCave(helper);

        for (int x = 0; x <= 14; x++) {
            for (int z = 0; z <= 14; z++) {
                helper.setBlock(x, 1, z, Blocks.STONE);
                helper.setBlock(x, 22, z, Blocks.STONE);
            }
        }

        for (int y = 2; y <= 21; y++) {
            for (int horizontal = 0; horizontal <= 14; horizontal++) {
                helper.setBlock(0, y, horizontal, Blocks.STONE);
                helper.setBlock(14, y, horizontal, Blocks.STONE);
                helper.setBlock(horizontal, y, 0, Blocks.STONE);
                helper.setBlock(horizontal, y, 14, Blocks.STONE);
            }
        }
    }

    private static void clearDarkRoostRoom(GameTestHelper helper) {
        for (int x = 0; x <= 45; x++) {
            for (int y = 1; y <= 5; y++) {
                for (int z = 0; z <= 25; z++) {
                    helper.setBlock(x, y, z, Blocks.AIR);
                }
            }
        }
    }

    private static void clearTallDarkCave(GameTestHelper helper) {
        for (int x = 0; x <= 14; x++) {
            for (int y = 1; y <= 22; y++) {
                for (int z = 0; z <= 14; z++) {
                    helper.setBlock(x, y, z, Blocks.AIR);
                }
            }
        }
    }

    private static void openTallCaveNightExit(GameTestHelper helper) {
        for (int y = 2; y <= 21; y++) {
            for (int z = 1; z < 14; z++) {
                helper.setBlock(14, y, z, Blocks.AIR);
            }
        }
    }

    private static void buildReturnBlockingWall(
            GameTestHelper helper,
            Block block
    ) {
        for (int y = 2; y <= 4; y++) {
            for (int z = 1; z <= 6; z++) {
                helper.setBlock(20, y, z, block);
            }
        }
    }

    private static void buildHuntDetourWall(
            GameTestHelper helper,
            Block block
    ) {
        for (int y = 2; y <= 4; y++) {
            for (int z = 1; z <= 5; z++) {
                helper.setBlock(14, y, z, block);
            }
        }
    }

    private static void setClockTime(
            ServerLevel level,
            Holder<WorldClock> clock,
            long clockTime
    ) {
        level.getServer().clockManager().setTotalTicks(clock, clockTime);
    }

    private static void cleanup(Mob mob) {
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
