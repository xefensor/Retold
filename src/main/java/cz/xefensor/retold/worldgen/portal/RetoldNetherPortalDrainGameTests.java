package cz.xefensor.retold.worldgen.portal;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.api.world.RetoldWorldMutationType;
import cz.xefensor.retold.api.world.RetoldWorldProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.BuiltinTestFunctions;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public final class RetoldNetherPortalDrainGameTests {
    private static final Identifier EMPTY_STRUCTURE =
            Identifier.withDefaultNamespace("empty");
    private static final Identifier TEST_PROTECTION =
            Identifier.fromNamespaceAndPath(
                    Retold.MODID,
                    "test_nether_portal_drain_protection"
            );

    private RetoldNetherPortalDrainGameTests() {
    }

    public static void register(
            RegisterGameTestsEvent event,
            Holder<TestEnvironmentDefinition<?>> environment
    ) {
        TestData<Holder<TestEnvironmentDefinition<?>>> testData =
                new TestData<>(environment, EMPTY_STRUCTURE, 80, 0, true);

        registerTest(
                event,
                testData,
                "nether_portal_drain_maps_materials_and_protects_valuable_blocks",
                RetoldNetherPortalDrainGameTests::mapsMaterialsAndProtectsValuables
        );
        registerTest(
                event,
                testData,
                "nether_portal_drain_uses_explicit_palette",
                RetoldNetherPortalDrainGameTests::usesExplicitPalette
        );
        registerTest(
                event,
                testData,
                "nether_portal_lava_sources_linearly_resist_spread",
                RetoldNetherPortalDrainGameTests::lavaSourcesLinearlyResistSpread
        );
        registerTest(
                event,
                testData,
                "nether_portal_travel_pulses_are_bounded",
                RetoldNetherPortalDrainGameTests::travelPulsesAreBounded
        );
        registerTest(
                event,
                testData,
                "nether_portal_drain_advances_outward",
                RetoldNetherPortalDrainGameTests::drainAdvancesOutward
        );
        registerTest(
                event,
                testData,
                "nether_portal_drain_scales_with_portal_area",
                RetoldNetherPortalDrainGameTests::drainScalesWithPortalArea
        );
    }

    private static void mapsMaterialsAndProtectsValuables(
            GameTestHelper helper
    ) {
        ServerLevel level = helper.getLevel();
        BlockPos center = helper.absolutePos(new BlockPos(8, 4, 8));
        BlockPos outerGround = center.offset(12, 0, 0);
        BlockPos outerLeaves = center.offset(-12, 0, 0);
        BlockPos outerStone = center.offset(0, 0, 12);
        BlockPos innerSoil = center.east();
        BlockPos innerStone = center.east(2);
        BlockPos innerStairs = center.east(3);
        BlockPos innerLog = center.east(4);
        BlockPos innerWater = center.east(5);
        BlockPos missedInnerSoil = center.south(2);
        BlockPos protectedOre = center.west();
        BlockPos protectedStorage = center.west(2);
        BlockPos protectedContainer = center.west(3);
        BlockPos protectedObsidian = center.west(4);
        BlockPos denied = center.north(2);

        BlockState stoneBrickStairs = Blocks.STONE_BRICK_STAIRS
                .defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST)
                .setValue(BlockStateProperties.HALF, Half.TOP);
        BlockState horizontalLog = Blocks.OAK_LOG.defaultBlockState()
                .setValue(BlockStateProperties.AXIS, Direction.Axis.X);

        level.setBlock(outerGround, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
        level.setBlock(outerLeaves, Blocks.OAK_LEAVES.defaultBlockState(), 3);
        level.setBlock(outerStone, Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(innerSoil, Blocks.DIRT.defaultBlockState(), 3);
        level.setBlock(innerStone, Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(innerStairs, stoneBrickStairs, 3);
        level.setBlock(innerLog, horizontalLog, 3);
        level.setBlock(innerWater, Blocks.WATER.defaultBlockState(), 3);
        level.setBlock(missedInnerSoil, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
        level.setBlock(protectedOre, Blocks.DIAMOND_ORE.defaultBlockState(), 3);
        level.setBlock(protectedStorage, Blocks.DIAMOND_BLOCK.defaultBlockState(), 3);
        level.setBlock(protectedContainer, Blocks.CHEST.defaultBlockState(), 3);
        level.setBlock(protectedObsidian, Blocks.OBSIDIAN.defaultBlockState(), 3);
        level.setBlock(denied, Blocks.DIRT.defaultBlockState(), 3);

        drain(helper, level, center, outerGround);
        drain(helper, level, center, outerLeaves);
        helper.assertFalse(
                RetoldNetherPortalDrainEvents.drainAt(
                        level,
                        center,
                        outerStone
                ),
                "Outer drain must leave non-living stone unchanged"
        );
        drain(helper, level, center, innerSoil);
        helper.assertTrue(
                level.getBlockState(innerSoil).is(Blocks.COARSE_DIRT),
                "Inner living ground must die before Nether corruption"
        );
        helper.assertFalse(
                RetoldNetherPortalDrainEvents.drainAt(
                        level,
                        center,
                        innerStone
                ),
                "The death wave must not directly corrupt stone"
        );
        corrupt(helper, level, center, innerSoil);
        corrupt(helper, level, center, innerStone);
        corrupt(helper, level, center, innerStairs);
        corrupt(helper, level, center, innerLog);
        corrupt(helper, level, center, innerWater);
        corrupt(helper, level, center, missedInnerSoil);
        helper.assertTrue(
                level.getBlockState(missedInnerSoil).is(Blocks.COARSE_DIRT),
                "Corruption must not bypass death for newly loaded living ground"
        );
        corrupt(helper, level, center, missedInnerSoil);

        helper.assertTrue(
                level.getBlockState(outerGround).is(Blocks.COARSE_DIRT),
                "Outer living ground must drain into coarse dirt"
        );
        helper.assertTrue(
                level.getBlockState(outerLeaves).isAir(),
                "Outer vegetation must wither away"
        );
        helper.assertTrue(
                level.getBlockState(innerSoil).is(Blocks.NETHERRACK),
                "Inner soil must corrupt into netherrack"
        );
        helper.assertTrue(
                level.getBlockState(innerStone).is(Blocks.BLACKSTONE),
                "Inner stone must corrupt into blackstone"
        );

        BlockState transformedStairs = level.getBlockState(innerStairs);
        helper.assertTrue(
                transformedStairs.is(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS)
                        && transformedStairs.getValue(
                        BlockStateProperties.HORIZONTAL_FACING
                ) == Direction.EAST
                        && transformedStairs.getValue(
                        BlockStateProperties.HALF
                ) == Half.TOP,
                "Masonry stairs must retain their compatible shape state"
        );

        BlockState transformedLog = level.getBlockState(innerLog);
        helper.assertTrue(
                transformedLog.is(Blocks.CRIMSON_STEM)
                        && transformedLog.getValue(BlockStateProperties.AXIS)
                        == Direction.Axis.X,
                "Wood must become matching crimson wood without losing its axis"
        );
        helper.assertTrue(
                level.getBlockState(innerWater).isAir(),
                "Inner-zone water must evaporate permanently"
        );
        helper.assertTrue(
                level.getBlockState(missedInnerSoil).is(Blocks.NETHERRACK),
                "Dead ground may become netherrack on a later corruption visit"
        );

        for (BlockPos protectedPos : List.of(
                protectedOre,
                protectedStorage,
                protectedContainer,
                protectedObsidian
        )) {
            helper.assertFalse(
                    RetoldNetherPortalDrainEvents.drainAt(
                            level,
                            center,
                            protectedPos
                    ),
                    "Protected block must reject the death wave at "
                            + protectedPos
            );
            helper.assertFalse(
                    RetoldNetherPortalDrainEvents.corruptAt(
                            level,
                            center,
                            protectedPos
                    ),
                    "Protected block must reject Nether corruption at "
                            + protectedPos
            );
        }

        RetoldWorldProtection.Registration registration =
                RetoldWorldProtection.register(
                        TEST_PROTECTION,
                        context -> context.type()
                                != RetoldWorldMutationType.NETHER_PORTAL_DRAIN
                                || !context.pos().equals(denied)
                );
        try {
            helper.assertFalse(
                    RetoldNetherPortalDrainEvents.drainAt(
                            level,
                            center,
                            denied
                    ),
                    "A protection rule must be able to deny portal drain"
            );
            helper.assertTrue(
                    level.getBlockState(denied).is(Blocks.DIRT),
                    "Denied portal drain must leave the original block intact"
            );
        } finally {
            registration.close();
        }

        helper.succeed();
    }

    private static void lavaSourcesLinearlyResistSpread(
            GameTestHelper helper
    ) {
        ServerLevel level = helper.getLevel();
        BlockPos center = helper.absolutePos(new BlockPos(8, 4, 8));
        int radius = RetoldNetherPortalDrainEvents.OUTER_RADIUS;

        level.setBlock(
                center.offset(radius, 0, 0),
                Blocks.LAVA.defaultBlockState(),
                3
        );
        level.setBlock(
                center.east(),
                Blocks.LAVA.defaultBlockState().setValue(LiquidBlock.LEVEL, 1),
                3
        );
        level.setBlock(
                center.offset(radius + 1, 0, 0),
                Blocks.LAVA.defaultBlockState(),
                3
        );
        level.setBlock(center.south(), Blocks.WATER.defaultBlockState(), 3);

        helper.assertValueEqual(
                RetoldNetherPortalDrainEvents
                        .lavaSourcesInAffectedAreaForTest(
                                level,
                                center,
                                radius
                        ),
                1,
                "Only lava sources inside the full affected sphere must count"
        );
        helper.assertValueEqual(
                RetoldNetherPortalDrainEvents.allowedSpreadAttemptsForTest(
                        6,
                        0,
                        12
                ),
                12,
                "A portal without lava must retain full spread speed"
        );
        helper.assertValueEqual(
                RetoldNetherPortalDrainEvents.allowedSpreadAttemptsForTest(
                        6,
                        1,
                        12
                ),
                10,
                "One source around a six-block portal must leave five-sixths speed"
        );
        helper.assertValueEqual(
                RetoldNetherPortalDrainEvents.allowedSpreadAttemptsForTest(
                        6,
                        3,
                        12
                ),
                6,
                "Three sources around a six-block portal must halve spread speed"
        );
        helper.assertValueEqual(
                RetoldNetherPortalDrainEvents.allowedSpreadAttemptsForTest(
                        6,
                        5,
                        12
                ),
                2,
                "Five sources must leave one-sixth spread speed"
        );
        helper.assertValueEqual(
                RetoldNetherPortalDrainEvents.allowedSpreadAttemptsForTest(
                        6,
                        6,
                        12
                ),
                0,
                "Matching the portal area in lava sources must stop spread"
        );
        helper.assertValueEqual(
                RetoldNetherPortalDrainEvents.allowedSpreadAttemptsForTest(
                        6,
                        7,
                        12
                ),
                0,
                "Extra lava sources must keep spread stopped"
        );

        helper.succeed();
    }

    private static void usesExplicitPalette(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos center = helper.absolutePos(new BlockPos(8, 4, 8));
        BlockPos target = center.east();

        for (BlockState stable : List.of(
                Blocks.NETHERRACK.defaultBlockState(),
                Blocks.BLACKSTONE.defaultBlockState(),
                Blocks.WARPED_PLANKS.defaultBlockState(),
                Blocks.SOUL_SOIL.defaultBlockState(),
                Blocks.GRAVEL.defaultBlockState()
        )) {
            level.setBlock(target, stable, 3);
            helper.assertFalse(
                    RetoldNetherPortalDrainEvents.drainAt(
                            level,
                            center,
                            target
                    ),
                    stable.getBlock() + " must also resist the death front"
            );
            helper.assertFalse(
                    RetoldNetherPortalDrainEvents.corruptAt(
                            level,
                            center,
                            target
                    ),
                    stable.getBlock() + " must be a stable portal-drain result"
            );
            helper.assertTrue(
                    level.getBlockState(target).is(stable.getBlock()),
                    stable.getBlock() + " must remain unchanged"
            );
        }

        for (BlockState unchanged : List.of(
                Blocks.CONCRETE.white().defaultBlockState(),
                Blocks.CONCRETE_POWDER.white().defaultBlockState(),
                Blocks.TERRACOTTA.defaultBlockState(),
                Blocks.GLASS.defaultBlockState(),
                Blocks.WOOL.white().defaultBlockState(),
                Blocks.RAIL.defaultBlockState(),
                Blocks.HAY_BLOCK.defaultBlockState(),
                Blocks.PUMPKIN.defaultBlockState(),
                Blocks.RED_MUSHROOM_BLOCK.defaultBlockState(),
                Blocks.BOOKSHELF.defaultBlockState()
        )) {
            level.setBlock(target, unchanged, 3);
            helper.assertFalse(
                    RetoldNetherPortalDrainEvents.corruptAt(
                            level,
                            center,
                            target
                    ),
                    unchanged.getBlock()
                            + " has no agreed Nether equivalent and must be ignored"
            );
            helper.assertTrue(
                    level.getBlockState(target).is(unchanged.getBlock()),
                    unchanged.getBlock() + " must remain unchanged"
            );
        }

        assertCorruptsTo(helper, level, center, target, Blocks.SAND, Blocks.GRAVEL);
        assertCorruptsTo(helper, level, center, target, Blocks.CLAY, Blocks.NETHERRACK);
        assertCorruptsTo(
                helper,
                level,
                center,
                target,
                Blocks.SANDSTONE_STAIRS,
                Blocks.BLACKSTONE_STAIRS
        );
        assertCorruptsTo(
                helper,
                level,
                center,
                target,
                Blocks.BRICKS,
                Blocks.POLISHED_BLACKSTONE_BRICKS
        );

        level.setBlock(target, Blocks.ICE.defaultBlockState(), 3);
        drain(helper, level, center, target);
        helper.assertTrue(
                level.getBlockState(target).isAir(),
                "Ice must melt during the death front"
        );

        level.setBlock(target.below(), Blocks.COARSE_DIRT.defaultBlockState(), 3);
        level.setBlock(target, Blocks.DANDELION.defaultBlockState(), 3);
        drain(helper, level, center, target);
        helper.assertTrue(
                level.getBlockState(target).is(Blocks.DEAD_BUSH),
                "A supported small plant must become a dead bush"
        );

        level.setBlock(target, Blocks.CACTUS.defaultBlockState(), 3);
        drain(helper, level, center, target);
        helper.assertTrue(
                level.getBlockState(target).isAir(),
                "Cactus must disappear during the death front"
        );

        level.setBlock(target.below(), Blocks.STONE.defaultBlockState(), 3);
        BlockState wetCoral = Blocks.TUBE_CORAL_FAN.defaultBlockState()
                .setValue(BlockStateProperties.WATERLOGGED, true);
        level.setBlock(target, wetCoral, 3);
        drain(helper, level, center, target);
        helper.assertTrue(
                level.getBlockState(target).is(Blocks.DEAD_TUBE_CORAL_FAN)
                        && level.getBlockState(target).getValue(
                        BlockStateProperties.WATERLOGGED
                ),
                "The death front must kill coral without evaporating its water"
        );

        level.setBlock(target, wetCoral, 3);
        corrupt(helper, level, center, target);
        helper.assertTrue(
                level.getBlockState(target).is(Blocks.DEAD_TUBE_CORAL_FAN)
                        && !level.getBlockState(target).getValue(
                        BlockStateProperties.WATERLOGGED
                ),
                "The corruption front must kill coral and evaporate water"
        );

        BlockState waterloggedStairs = Blocks.STONE_BRICK_STAIRS
                .defaultBlockState()
                .setValue(BlockStateProperties.WATERLOGGED, true);
        level.setBlock(target, waterloggedStairs, 3);
        corrupt(helper, level, center, target);
        helper.assertTrue(
                level.getBlockState(target).is(
                        Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS
                ) && !level.getBlockState(target).getValue(
                        BlockStateProperties.WATERLOGGED
                ),
                "Waterlogged masonry must transform while only its water evaporates"
        );

        helper.succeed();
    }

    private static void assertCorruptsTo(
            GameTestHelper helper,
            ServerLevel level,
            BlockPos center,
            BlockPos target,
            net.minecraft.world.level.block.Block source,
            net.minecraft.world.level.block.Block expected
    ) {
        level.setBlock(target, source.defaultBlockState(), 3);
        corrupt(helper, level, center, target);
        helper.assertTrue(
                level.getBlockState(target).is(expected),
                source + " must corrupt into " + expected
        );
    }

    private static void travelPulsesAreBounded(GameTestHelper helper) {
        RetoldNetherPortalDrainEvents.clearForTests();
        ServerLevel overworld = helper.getLevel();
        ServerLevel nether = Objects.requireNonNull(
                overworld.getServer().getLevel(Level.NETHER),
                "GameTest server must provide the Nether"
        );
        BlockPos portal = helper.absolutePos(new BlockPos(4, 3, 4));

        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 2; x++) {
                overworld.setBlock(
                        portal.offset(x, y, 0),
                        Blocks.NETHER_PORTAL.defaultBlockState().setValue(
                                NetherPortalBlock.AXIS,
                                Direction.Axis.X
                        ),
                        3
                );
            }
        }

        ArmorStand traveler = helper.spawn(EntityTypes.ARMOR_STAND, 4, 3, 4);
        TeleportTransition baseTransition = new TeleportTransition(
                nether,
                Vec3.atBottomCenterOf(portal),
                Vec3.ZERO,
                0.0F,
                0.0F,
                TeleportTransition.DO_NOTHING
        );
        TeleportTransition wrapped =
                RetoldNetherPortalDrainTravel.attachSuccessfulTravelPulse(
                        overworld,
                        portal,
                        baseTransition
                );
        wrapped.postTeleportTransition().onTransition(traveler);

        helper.assertValueEqual(
                RetoldNetherPortalDrainEvents.pendingPulseWorkForTest(
                        overworld,
                        portal
                ),
                RetoldNetherPortalDrainEvents.PULSE_CHANGES_PER_TRAVEL,
                "Every successful traveler must add one bounded drain pulse"
        );

        for (int travel = 0; travel < 20; travel++) {
            wrapped.postTeleportTransition().onTransition(traveler);
        }

        helper.assertValueEqual(
                RetoldNetherPortalDrainEvents.pendingPulseWorkForTest(
                        overworld,
                        portal
                ),
                RetoldNetherPortalDrainEvents.MAX_QUEUED_PULSE_CHANGES,
                "Rapid portal traffic must coalesce at the per-portal work cap"
        );
        RetoldNetherPortalDrainEvents.clearForTests();
        helper.succeed();
    }

    private static void drainAdvancesOutward(GameTestHelper helper) {
        int offsetCount = RetoldNetherPortalDrainEvents
                .offsetCountForRadiusForTest(
                        RetoldNetherPortalDrainEvents.OUTER_RADIUS
                );
        helper.assertTrue(offsetCount > 0, "Drain order must not be empty");

        int previousDistanceSquared = 0;
        for (int index = 0; index < offsetCount; index++) {
            int distanceSquared = RetoldNetherPortalDrainEvents
                    .offsetDistanceSquaredForTest(index);
            helper.assertTrue(
                    distanceSquared >= previousDistanceSquared,
                    "Portal drain candidates must never move back toward the portal"
            );
            previousDistanceSquared = distanceSquared;
        }

        helper.assertValueEqual(
                RetoldNetherPortalDrainEvents
                        .offsetDistanceSquaredForTest(0),
                1,
                "Portal drain must begin immediately beside the portal"
        );
        helper.assertValueEqual(
                RetoldNetherPortalDrainEvents
                        .offsetDistanceSquaredForTest(offsetCount - 1),
                RetoldNetherPortalDrainEvents.OUTER_RADIUS
                        * RetoldNetherPortalDrainEvents.OUTER_RADIUS,
                "Portal drain must finish at the configured outer radius"
        );
        helper.assertFalse(
                RetoldNetherPortalDrainEvents
                        .corruptionFitsBehindDeathFrontForTest(1, 1),
                "Nether corruption must not begin alongside the death front"
        );
        helper.assertTrue(
                RetoldNetherPortalDrainEvents
                        .corruptionFitsBehindDeathFrontForTest(4, 1),
                "Corruption may begin once the death radius is twice as large"
        );
        helper.assertFalse(
                RetoldNetherPortalDrainEvents
                        .corruptionFitsBehindDeathFrontForTest(255, 64),
                "The final corruption shell must wait for the final death shell"
        );
        helper.assertTrue(
                RetoldNetherPortalDrainEvents
                        .corruptionFitsBehindDeathFrontForTest(256, 64),
                "The final 8-block corruption shell must fit inside the 16-block death front"
        );
        helper.succeed();
    }

    private static void drainScalesWithPortalArea(GameTestHelper helper) {
        helper.assertValueEqual(
                RetoldNetherPortalDrainEvents.outerRadiusForPortalForTest(2, 3),
                16,
                "A standard six-block portal must keep the original radius"
        );
        helper.assertValueEqual(
                RetoldNetherPortalDrainEvents.outerRadiusForPortalForTest(3, 3),
                19,
                "Every portal block beyond the standard area must add one radius block"
        );
        helper.assertValueEqual(
                RetoldNetherPortalDrainEvents.outerRadiusForPortalForTest(4, 5),
                30,
                "Both portal width and height must contribute through total area"
        );
        helper.assertValueEqual(
                RetoldNetherPortalDrainEvents.outerRadiusForPortalForTest(7, 7),
                RetoldNetherPortalDrainEvents.MAX_OUTER_RADIUS,
                "Large portals must stop at the 48-block cap"
        );

        int maximumOffsetCount = RetoldNetherPortalDrainEvents
                .offsetCountForRadiusForTest(
                        RetoldNetherPortalDrainEvents.MAX_OUTER_RADIUS
                );
        helper.assertValueEqual(
                RetoldNetherPortalDrainEvents.offsetDistanceSquaredForTest(
                        maximumOffsetCount - 1
                ),
                RetoldNetherPortalDrainEvents.MAX_OUTER_RADIUS
                        * RetoldNetherPortalDrainEvents.MAX_OUTER_RADIUS,
                "The bounded offset index must cover the complete maximum radius"
        );
        helper.succeed();
    }

    private static void drain(
            GameTestHelper helper,
            ServerLevel level,
            BlockPos center,
            BlockPos target
    ) {
        helper.assertTrue(
                RetoldNetherPortalDrainEvents.drainAt(
                        level,
                        center,
                        target
                ),
                "Expected portal death-wave transformation at " + target
        );
    }

    private static void corrupt(
            GameTestHelper helper,
            ServerLevel level,
            BlockPos center,
            BlockPos target
    ) {
        helper.assertTrue(
                RetoldNetherPortalDrainEvents.corruptAt(
                        level,
                        center,
                        target
                ),
                "Expected portal Nether-corruption transformation at " + target
        );
    }

    private static void registerTest(
            RegisterGameTestsEvent event,
            TestData<Holder<TestEnvironmentDefinition<?>>> testData,
            String path,
            Consumer<GameTestHelper> test
    ) {
        event.registerTest(
                Identifier.fromNamespaceAndPath(Retold.MODID, path),
                new InlineGameTest(testData, test)
        );
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
