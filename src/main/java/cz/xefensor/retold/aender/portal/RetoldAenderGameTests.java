package cz.xefensor.retold.aender.portal;

import com.mojang.serialization.Codec;
import cz.xefensor.retold.Retold;
import cz.xefensor.retold.aender.RetoldAenderDimensions;
import cz.xefensor.retold.aender.generation.AenderChunkGenerator;
import cz.xefensor.retold.aender.generation.AenderVolatility;
import cz.xefensor.retold.aender.stability.AenderRealityTickEvents;
import cz.xefensor.retold.aender.stability.AenderStabilityData;
import cz.xefensor.retold.registry.RetoldBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.BuiltinTestFunctions;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.function.Consumer;

public final class RetoldAenderGameTests {
    private static final Identifier EMPTY_STRUCTURE =
            Identifier.withDefaultNamespace("empty");
    private static final int TEST_PADDING = 32;
    private static final BlockPos COUNTERPART_TARGET =
            new BlockPos(2048, 201, 2048);

    private RetoldAenderGameTests() {
    }

    public static void register(
            RegisterGameTestsEvent event,
            Holder<TestEnvironmentDefinition<?>> environment
    ) {
        registerTest(
                event,
                environment,
                "aender_portal_shape_activation_and_invalidation",
                RetoldAenderGameTests::portalShapeActivationAndInvalidation
        );
        registerTest(
                event,
                environment,
                "aender_coordinates_scale_both_directions",
                RetoldAenderGameTests::coordinatesScaleBothDirections
        );
        registerTest(
                event,
                environment,
                "aender_counterpart_portal_is_created_and_indexed",
                RetoldAenderGameTests::counterpartPortalIsCreatedAndIndexed
        );
        registerTest(
                event,
                environment,
                "aender_stability_round_trip_preserves_overlap_counts",
                RetoldAenderGameTests::stabilityRoundTripPreservesOverlapCounts
        );
        registerTest(
                event,
                environment,
                "aender_reality_regenerates_only_volatile_chunks",
                RetoldAenderGameTests::realityRegeneratesOnlyVolatileChunks
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

    private static void portalShapeActivationAndInvalidation(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        AenderPortalShape valid = new AenderPortalShape(
                helper.absolutePos(new BlockPos(1, 2, 1)),
                3,
                3
        );
        AenderPortalShape tooSmall = new AenderPortalShape(
                helper.absolutePos(new BlockPos(8, 2, 1)),
                2,
                3
        );
        AenderPortalShape tooLarge = new AenderPortalShape(
                helper.absolutePos(new BlockPos(-26, 2, 8)),
                22,
                3
        );
        BlockPos finalFrame = valid.minCorner().offset(-1, 0, -1);

        try {
            placeFrame(level, valid, finalFrame);
            helper.assertTrue(
                    AenderPortalShape.findComplete(level, valid.minCorner()).isEmpty(),
                    "An incomplete Aender portal frame must not be detected as complete"
            );

            level.setBlock(
                    finalFrame,
                    RetoldBlocks.DEV_AENDER_PORTAL_FRAME.get().defaultBlockState(),
                    3
            );

            helper.assertValueEqual(
                    AenderPortalShape.findComplete(level, valid.centerBlock()).orElse(null),
                    valid,
                    "Placing the final frame block must activate the 3x3 portal"
            );
            assertPortalInterior(helper, level, valid);

            placeFrame(level, tooSmall, null);
            helper.assertTrue(
                    AenderPortalShape.findEmpty(level, tooSmall.minCorner()).isEmpty(),
                    "A 2x3 Aender portal interior must be rejected"
            );

            placeFrame(level, tooLarge, null);
            helper.assertTrue(
                    AenderPortalShape.findEmpty(level, tooLarge.minCorner()).isEmpty(),
                    "A 22x3 Aender portal interior must be rejected"
            );

            level.setBlock(finalFrame, Blocks.AIR.defaultBlockState(), 3);
            helper.assertTrue(
                    AenderPortalShape.findComplete(level, valid.centerBlock()).isEmpty(),
                    "Breaking an Aender portal frame must invalidate the portal"
            );
            valid.forEachInterior(pos -> helper.assertFalse(
                    level.getBlockState(pos).is(RetoldBlocks.AENDER_PORTAL),
                    "Invalidated portal interiors must not retain portal blocks"
            ));

            helper.succeed();
        } finally {
            AenderPortalData.get(level).remove(level, valid);
            clearShape(level, valid);
            clearShape(level, tooSmall);
            clearShape(level, tooLarge);
        }
    }

    private static void coordinatesScaleBothDirections(GameTestHelper helper) {
        WorldBorder border = new WorldBorder();

        helper.assertValueEqual(
                AenderPortalCoordinates.scaleAndClamp(
                        Level.OVERWORLD,
                        border,
                        new Vec3(12.75D, 64.0D, -3.125D)
                ),
                new BlockPos(102, 64, -25),
                "Overworld coordinates must scale by eight when entering the Aender"
        );
        helper.assertValueEqual(
                AenderPortalCoordinates.scaleAndClamp(
                        RetoldAenderDimensions.AENDER,
                        border,
                        new Vec3(-84.0D, 70.0D, 44.0D)
                ),
                new BlockPos(-11, 70, 5),
                "Aender coordinates must scale by one eighth when returning to the Overworld"
        );

        border.setCenter(0.0D, 0.0D);
        border.setSize(100.0D);
        BlockPos clamped = AenderPortalCoordinates.scaleAndClamp(
                Level.OVERWORLD,
                border,
                new Vec3(20.0D, 80.0D, -20.0D)
        );
        helper.assertTrue(
                border.isWithinBounds(clamped),
                "Scaled Aender portal coordinates must remain inside the destination world border"
        );

        helper.succeed();
    }

    private static void counterpartPortalIsCreatedAndIndexed(GameTestHelper helper) {
        ServerLevel overworld = helper.getLevel();
        AenderPortalShape createdPortal = null;

        try {
            prepareCounterpartArea(overworld);
            createdPortal = AenderPortalLogic.createExitPortal(overworld, COUNTERPART_TARGET);

            helper.assertTrue(
                    createdPortal.isComplete(overworld),
                    "Counterpart creation must produce a complete portal"
            );
            helper.assertValueEqual(createdPortal.width(), 3, "Created counterparts must have a 3-block interior width");
            helper.assertValueEqual(createdPortal.depth(), 3, "Created counterparts must have a 3-block interior depth");
            helper.assertTrue(
                    AenderPortalData.get(overworld)
                            .findNear(overworld, COUNTERPART_TARGET, 16)
                            .contains(createdPortal),
                    "A created counterpart portal must be indexed near its requested destination"
            );
            assertPortalFrameAndSupport(helper, overworld, createdPortal);

            helper.succeed();
        } finally {
            if (createdPortal != null) {
                AenderPortalData.get(overworld).remove(overworld, createdPortal);
            }
            clearCounterpartArea(overworld);
        }
    }

    private static void stabilityRoundTripPreservesOverlapCounts(GameTestHelper helper) {
        AenderStabilityData original = new AenderStabilityData();
        ChunkPos firstCenter = new ChunkPos(0, 0);
        ChunkPos secondCenter = new ChunkPos(2, 0);
        original.addStabilizer(firstCenter);
        original.addStabilizer(secondCenter);

        Codec<AenderStabilityData> codec = AenderStabilityData.TYPE.codecFactory().create(null);
        Tag encoded = codec.encodeStart(NbtOps.INSTANCE, original)
                .getOrThrow(IllegalStateException::new);
        AenderStabilityData decoded = codec.parse(NbtOps.INSTANCE, encoded)
                .getOrThrow(IllegalStateException::new);

        decoded.removeStabilizer(firstCenter);

        helper.assertFalse(
                decoded.isStable(new ChunkPos(-1, 0)),
                "A non-overlapping chunk must be released after its decoded stabilizer is removed"
        );
        helper.assertTrue(
                decoded.isStable(new ChunkPos(1, 0)),
                "An overlapping chunk must retain the second decoded stabilizer count"
        );
        helper.assertTrue(
                decoded.isStable(new ChunkPos(3, 0)),
                "The second decoded stabilizer halo must remain stable"
        );

        helper.succeed();
    }

    private static void realityRegeneratesOnlyVolatileChunks(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos anchor = helper.absolutePos(BlockPos.ZERO);
        ChunkPos stablePos = new ChunkPos((anchor.getX() >> 4) + 64, anchor.getZ() >> 4);
        ChunkPos volatilePos = new ChunkPos(stablePos.x() + 4, stablePos.z());
        ChunkAccess stableChunk = level.getChunk(stablePos.x(), stablePos.z());
        ChunkAccess volatileChunk = level.getChunk(volatilePos.x(), volatilePos.z());
        BlockPos stableSentinel = sentinelPos(level, stablePos);
        BlockPos volatileSentinel = sentinelPos(level, volatilePos);
        AenderStabilityData stability = new AenderStabilityData();

        try {
            stability.addStabilizer(stablePos);
            AenderVolatility.retainForChunk(stableChunk);
            AenderVolatility.retainForChunk(volatileChunk);
            AenderVolatility.markGenerated(stableChunk);
            AenderVolatility.markGenerated(volatileChunk);
            level.setBlock(stableSentinel, Blocks.DIAMOND_BLOCK.defaultBlockState(), 3);
            level.setBlock(volatileSentinel, Blocks.DIAMOND_BLOCK.defaultBlockState(), 3);

            AenderVolatility.clearForgottenWorld();

            helper.assertFalse(
                    AenderRealityTickEvents.shouldRegenerate(stability, stableChunk),
                    "A stabilized stale chunk must be excluded from regeneration"
            );
            helper.assertTrue(
                    AenderRealityTickEvents.shouldRegenerate(stability, volatileChunk),
                    "A volatile stale chunk must be selected for regeneration"
            );
            AenderChunkGenerator.regenerateLoadedChunk(volatileChunk);

            helper.assertTrue(
                    level.getBlockState(stableSentinel).is(Blocks.DIAMOND_BLOCK),
                    "A stabilized chunk must preserve its blocks across a reality reset"
            );
            helper.assertFalse(
                    level.getBlockState(volatileSentinel).is(Blocks.DIAMOND_BLOCK),
                    "A volatile chunk must replace stale blocks during regeneration"
            );
            helper.assertFalse(
                    AenderVolatility.needsRegeneration(volatileChunk),
                    "A regenerated volatile chunk must receive the current reality signature"
            );

            helper.succeed();
        } finally {
            AenderVolatility.markGenerated(stableChunk);
            stability.removeStabilizer(stablePos);
            level.setBlock(stableSentinel, Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(volatileSentinel, Blocks.AIR.defaultBlockState(), 3);
            AenderRealityTickEvents.clearPendingRegeneration();
            AenderVolatility.clearForgottenWorld();
        }
    }

    private static void placeFrame(
            ServerLevel level,
            AenderPortalShape shape,
            BlockPos skipped
    ) {
        BlockState frame = RetoldBlocks.DEV_AENDER_PORTAL_FRAME.get().defaultBlockState();

        for (int x = -1; x <= shape.width(); x++) {
            for (int z = -1; z <= shape.depth(); z++) {
                if (x != -1 && x != shape.width() && z != -1 && z != shape.depth()) {
                    continue;
                }

                BlockPos pos = shape.minCorner().offset(x, 0, z);
                if (!pos.equals(skipped)) {
                    level.setBlock(pos, frame, 18);
                }
            }
        }
    }

    private static void clearShape(ServerLevel level, AenderPortalShape shape) {
        for (int x = -1; x <= shape.width(); x++) {
            for (int z = -1; z <= shape.depth(); z++) {
                level.setBlock(
                        shape.minCorner().offset(x, 0, z),
                        Blocks.AIR.defaultBlockState(),
                        18
                );
            }
        }
    }

    private static void assertPortalInterior(
            GameTestHelper helper,
            ServerLevel level,
            AenderPortalShape shape
    ) {
        shape.forEachInterior(pos -> helper.assertTrue(
                level.getBlockState(pos).is(RetoldBlocks.AENDER_PORTAL),
                "Activated portal interiors must contain Aender portal blocks"
        ));
    }

    private static void prepareCounterpartArea(ServerLevel level) {
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                level.setBlock(
                        COUNTERPART_TARGET.offset(x, -1, z),
                        Blocks.STONE.defaultBlockState(),
                        18
                );

                for (int y = 0; y <= 4; y++) {
                    level.setBlock(
                            COUNTERPART_TARGET.offset(x, y, z),
                            Blocks.AIR.defaultBlockState(),
                            18
                    );
                }
            }
        }
    }

    private static void clearCounterpartArea(ServerLevel level) {
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                for (int y = -1; y <= 4; y++) {
                    level.setBlock(
                            COUNTERPART_TARGET.offset(x, y, z),
                            Blocks.AIR.defaultBlockState(),
                            18
                    );
                }
            }
        }
    }

    private static void assertPortalFrameAndSupport(
            GameTestHelper helper,
            ServerLevel level,
            AenderPortalShape shape
    ) {
        for (int x = -1; x <= shape.width(); x++) {
            for (int z = -1; z <= shape.depth(); z++) {
                BlockPos planePos = shape.minCorner().offset(x, 0, z);
                boolean frame = x == -1 || x == shape.width() || z == -1 || z == shape.depth();

                helper.assertTrue(
                        level.getBlockState(planePos).is(
                                frame
                                        ? RetoldBlocks.DEV_AENDER_PORTAL_FRAME
                                        : RetoldBlocks.AENDER_PORTAL
                        ),
                        "Created counterparts must contain the expected frame and portal field"
                );

                BlockPos supportPos = planePos.below();
                helper.assertTrue(
                        level.getBlockState(supportPos).isFaceSturdy(level, supportPos, Direction.UP),
                        "Created counterpart portals must have sturdy support"
                );
            }
        }
    }

    private static BlockPos sentinelPos(ServerLevel level, ChunkPos chunkPos) {
        return new BlockPos(
                chunkPos.getMinBlockX() + 8,
                level.getMaxY() - 8,
                chunkPos.getMinBlockZ() + 8
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
