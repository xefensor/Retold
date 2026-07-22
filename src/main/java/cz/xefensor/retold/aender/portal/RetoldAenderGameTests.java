package cz.xefensor.retold.aender.portal;

import com.mojang.serialization.Codec;
import cz.xefensor.retold.Retold;
import cz.xefensor.retold.aender.RetoldAenderDimensions;
import cz.xefensor.retold.aender.generation.AenderAttachments;
import cz.xefensor.retold.aender.generation.AenderChunkRealityData;
import cz.xefensor.retold.aender.generation.AenderLoadedChunkReplacement;
import cz.xefensor.retold.aender.generation.AenderRealityData;
import cz.xefensor.retold.aender.generation.AenderVolatility;
import cz.xefensor.retold.aender.stability.AenderRealityTickEvents;
import cz.xefensor.retold.aender.stability.AenderStabilityData;
import cz.xefensor.retold.block.AenderGrassBlock;
import cz.xefensor.retold.block.AenderLeavesBlock;
import cz.xefensor.retold.registry.RetoldAenderWood;
import cz.xefensor.retold.registry.RetoldBlocks;
import cz.xefensor.retold.registry.RetoldEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.BuiltinTestFunctions;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.HangingSignBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;

import java.util.List;
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
                "aender_reality_data_round_trip_preserves_seed_and_version",
                RetoldAenderGameTests::realityDataRoundTripPreservesSeedAndVersion
        );
        registerTest(
                event,
                environment,
                "aender_reality_regenerates_only_volatile_chunks",
                RetoldAenderGameTests::realityRegeneratesOnlyVolatileChunks
        );
        registerTest(
                event,
                environment,
                "aender_terrain_blocks_have_survival_data",
                RetoldAenderGameTests::terrainBlocksHaveSurvivalData
        );
        registerTest(
                event,
                environment,
                "aender_wood_family_has_survival_data",
                RetoldAenderGameTests::woodFamilyHasSurvivalData
        );
        registerTest(
                event,
                environment,
                "aender_content_uses_vanilla_creative_tabs",
                RetoldAenderGameTests::contentUsesVanillaCreativeTabs
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

    private static void realityDataRoundTripPreservesSeedAndVersion(GameTestHelper helper) {
        AenderRealityData original = AenderRealityData.get(helper.getLevel());
        original.advanceRegion(14, -9);
        original.advanceRegion(14, -9);

        Codec<AenderRealityData> codec = AenderRealityData.TYPE.codecFactory().create(null);
        Tag encoded = codec.encodeStart(NbtOps.INSTANCE, original)
                .getOrThrow(IllegalStateException::new);
        AenderRealityData decoded = codec.parse(NbtOps.INSTANCE, encoded)
                .getOrThrow(IllegalStateException::new);

        helper.assertValueEqual(decoded.seed(), original.seed(), "Reality seed must survive serialization");
        helper.assertValueEqual(decoded.epoch(), original.epoch(), "Reality epoch must survive serialization");
        helper.assertValueEqual(
                decoded.generatorVersion(),
                original.generatorVersion(),
                "Generator version must survive serialization"
        );
        helper.assertValueEqual(
                decoded.regionEpoch(14, -9),
                2L,
                "Regional reality epochs must survive serialization"
        );

        CompoundTag legacyTag = new CompoundTag();
        legacyTag.putLong("seed", 123L);
        AenderRealityData legacy = codec.parse(NbtOps.INSTANCE, legacyTag)
                .getOrThrow(IllegalStateException::new);
        helper.assertValueEqual(
                legacy.generatorVersion(),
                AenderRealityData.LEGACY_GENERATOR_VERSION,
                "Reality data without a generator version must preserve legacy generation"
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

        stability.addStabilizer(stablePos);
        AenderVolatility.retainForChunk(stableChunk);
        AenderVolatility.retainForChunk(volatileChunk);
        AenderVolatility.markGenerated(stableChunk);
        AenderVolatility.markGenerated(volatileChunk);
        level.setBlock(stableSentinel, Blocks.DIAMOND_BLOCK.defaultBlockState(), 3);
        level.setBlock(volatileSentinel, Blocks.DIAMOND_BLOCK.defaultBlockState(), 3);
        ItemEntity volatileEntity = new ItemEntity(
                level,
                volatileSentinel.getX() + 0.5D,
                volatileSentinel.getY() + 1.0D,
                volatileSentinel.getZ() + 0.5D,
                new ItemStack(Items.DIAMOND)
        );
        level.addFreshEntity(volatileEntity);

        // Let the server index the newly spawned entity before querying the chunk bounds.
        helper.runAfterDelay(1, () -> {
            try {
                AenderVolatility.advanceTransientRealityForTest();

                helper.assertFalse(
                        AenderRealityTickEvents.shouldRegenerate(stability, stableChunk),
                        "A stabilized stale chunk must be excluded from regeneration"
                );
                helper.assertTrue(
                        AenderRealityTickEvents.shouldRegenerate(stability, volatileChunk),
                        "A volatile stale chunk must be selected for regeneration"
                );
                AenderLoadedChunkReplacement.blankForProgressiveRegeneration(level, volatileChunk);

                helper.assertTrue(
                        volatileChunk.getData(AenderAttachments.CHUNK_REALITY).stale(),
                        "A blank transition chunk must persist an explicit stale marker"
                );
                helper.assertFalse(
                        level.getBlockState(volatileSentinel).is(Blocks.DIAMOND_BLOCK),
                        "A volatile stale chunk must be blanked before progressive regeneration"
                );
                helper.assertTrue(
                        volatileEntity.isRemoved(),
                        "Blanking a volatile chunk must remove its non-player entities immediately"
                );
                helper.assertTrue(
                        AenderVolatility.needsRegeneration(volatileChunk),
                        "A blank transition chunk must remain queued for regeneration"
                );
                AenderLoadedChunkReplacement.regenerate(level, volatileChunk);

                AenderChunkRealityData regeneratedReality =
                        volatileChunk.getData(AenderAttachments.CHUNK_REALITY);
                helper.assertFalse(
                        regeneratedReality.stale(),
                        "A regenerated chunk must persist its current reality signature"
                );

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

                int cachedSignatures = AenderVolatility.cachedChunkSignatureCount();
                AenderVolatility.releaseForChunk(volatileChunk);
                helper.assertValueEqual(
                        AenderVolatility.cachedChunkSignatureCount(),
                        cachedSignatures - 1,
                        "Unloading a chunk must evict its runtime generation signature"
                );
                helper.assertValueEqual(
                        volatileChunk.getData(AenderAttachments.CHUNK_REALITY),
                        regeneratedReality,
                        "Runtime eviction must preserve the chunk's persistent reality attachment"
                );

                AenderVolatility.retainForChunk(volatileChunk);
                helper.assertFalse(
                        AenderVolatility.needsRegeneration(volatileChunk),
                        "Reloading must restore the signature without regenerating unchanged terrain"
                );

                helper.succeed();
            } finally {
                AenderVolatility.markGenerated(stableChunk);
                stability.removeStabilizer(stablePos);
                level.setBlock(stableSentinel, Blocks.AIR.defaultBlockState(), 3);
                level.setBlock(volatileSentinel, Blocks.AIR.defaultBlockState(), 3);
                AenderRealityTickEvents.clearPendingRegeneration();
                AenderVolatility.advanceTransientRealityForTest();
            }
        });
    }

    private static void terrainBlocksHaveSurvivalData(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos groundPos = helper.absolutePos(new BlockPos(2, 2, 2));
        BlockPos plantPos = groundPos.above();
        BlockPos leafPos = helper.absolutePos(new BlockPos(5, 3, 5));
        BlockPos leafLogPos = leafPos.below();
        BlockState grass = RetoldBlocks.AENDER_GRASS_BLOCK.get().defaultBlockState();
        BlockState soil = RetoldBlocks.AENDER_SOIL.get().defaultBlockState();
        BlockState stone = RetoldBlocks.AENDER_STONE.get().defaultBlockState();
        BlockState log = RetoldBlocks.AENDER_LOG.get().defaultBlockState();
        BlockState leaves = RetoldBlocks.AENDER_LEAVES.get().defaultBlockState();

        try {
            helper.assertTrue(grass.is(BlockTags.SUPPORTS_VEGETATION), "Aender grass must support vegetation");
            helper.assertTrue(soil.is(BlockTags.SUPPORTS_VEGETATION), "Aender soil must support vegetation");
            helper.assertTrue(grass.is(BlockTags.MINEABLE_WITH_SHOVEL), "Aender grass must be shovel-mineable");
            helper.assertTrue(soil.is(BlockTags.MINEABLE_WITH_SHOVEL), "Aender soil must be shovel-mineable");
            helper.assertTrue(stone.is(BlockTags.MINEABLE_WITH_PICKAXE), "Aender stone must be pickaxe-mineable");
            helper.assertTrue(log.is(BlockTags.MINEABLE_WITH_AXE), "Aender logs must be axe-mineable");
            helper.assertTrue(log.is(BlockTags.LOGS), "Aender logs must participate in the vanilla logs tag");
            helper.assertTrue(
                    new ItemStack(RetoldBlocks.AENDER_LOG_ITEM.get()).is(ItemTags.LOGS),
                    "Aender log items must participate in the vanilla logs item tag"
            );
            helper.assertTrue(leaves.is(BlockTags.MINEABLE_WITH_HOE), "Aender leaves must be hoe-mineable");
            helper.assertTrue(leaves.is(BlockTags.LEAVES), "Aender leaves must participate in the vanilla leaves tag");
            helper.assertTrue(
                    new ItemStack(RetoldBlocks.AENDER_LEAVES_ITEM.get()).is(ItemTags.LEAVES),
                    "Aender leaf items must participate in the vanilla leaves item tag"
            );
            helper.assertTrue(grass.getBlock() instanceof AenderGrassBlock, "Aender grass must use living grass behavior");
            helper.assertTrue(leaves.getBlock() instanceof AenderLeavesBlock, "Aender leaves must use living leaf behavior");
            helper.assertTrue(
                    leaves.getValue(LeavesBlock.PERSISTENT),
                    "Legacy and player-placed Aender leaves must default to persistent"
            );

            level.setBlock(groundPos, grass, 3);
            helper.assertTrue(
                    Blocks.SHORT_GRASS.defaultBlockState().canSurvive(level, plantPos),
                    "Vanilla vegetation must survive on Aender grass"
            );

            level.setBlock(plantPos, Blocks.STONE.defaultBlockState(), 3);
            helper.randomTick(new BlockPos(2, 2, 2));
            helper.assertTrue(
                    level.getBlockState(groundPos).is(RetoldBlocks.AENDER_SOIL),
                    "Covered Aender grass must revert to Aender soil"
            );

            level.setBlock(leafLogPos, log, 3);
            level.setBlock(leafPos, leaves.setValue(LeavesBlock.PERSISTENT, false), 3);
            helper.tickBlock(new BlockPos(5, 3, 5));
            helper.assertValueEqual(
                    level.getBlockState(leafPos).getValue(LeavesBlock.DISTANCE),
                    1,
                    "Natural Aender leaves must detect a neighboring Aender log"
            );
            level.setBlock(leafLogPos, Blocks.AIR.defaultBlockState(), 3);
            helper.tickBlock(new BlockPos(5, 3, 5));
            helper.randomTick(new BlockPos(5, 3, 5));
            helper.assertTrue(level.getBlockState(leafPos).isAir(), "Disconnected natural Aender leaves must decay");

            helper.assertTrue(
                    isOnlyDrop(Block.getDrops(grass, level, groundPos, null), RetoldBlocks.AENDER_SOIL_ITEM.get()),
                    "Aender grass without Silk Touch must drop Aender soil"
            );
            helper.assertTrue(
                    isOnlyDrop(Block.getDrops(soil, level, groundPos, null), RetoldBlocks.AENDER_SOIL_ITEM.get()),
                    "Aender soil must drop itself"
            );
            helper.assertTrue(
                    isOnlyDrop(Block.getDrops(stone, level, groundPos, null), RetoldBlocks.AENDER_STONE_ITEM.get()),
                    "Aender stone must drop itself"
            );
            helper.assertTrue(
                    isOnlyDrop(Block.getDrops(log, level, groundPos, null), RetoldBlocks.AENDER_LOG_ITEM.get()),
                    "Aender logs must drop themselves"
            );
            helper.assertTrue(
                    Block.getDrops(leaves, level, groundPos, null).stream().allMatch(
                            stack -> stack.is(RetoldAenderWood.AENDER_SAPLING_ITEM.get())
                                    || stack.is(Items.STICK)
                    ),
                    "Naturally broken Aender leaves may only drop their sapling or sticks"
            );
            helper.assertTrue(
                    isOnlyDrop(
                            Block.getDrops(leaves, level, groundPos, null, null, new ItemStack(Items.SHEARS)),
                            RetoldBlocks.AENDER_LEAVES_ITEM.get()
                    ),
                    "Shears must harvest Aender leaves"
            );

            helper.succeed();
        } finally {
            level.setBlock(plantPos, Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(groundPos, Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(leafPos, Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(leafLogPos, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    @SuppressWarnings("deprecation")
    private static void woodFamilyHasSurvivalData(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        BlockPos saplingPos = helper.absolutePos(new BlockPos(8, 3, 8));
        BlockPos signPos = helper.absolutePos(new BlockPos(11, 3, 8));
        BlockPos hangingSignPos = helper.absolutePos(new BlockPos(14, 3, 8));
        BlockState planks = RetoldAenderWood.AENDER_PLANKS.get().defaultBlockState();
        BlockState strippedLog = RetoldAenderWood.STRIPPED_AENDER_LOG.get().defaultBlockState();
        BlockState strippedFromLog = AxeItem.getAxeStrippingState(
                RetoldBlocks.AENDER_LOG.get().defaultBlockState()
        );
        BlockState strippedFromWood = AxeItem.getAxeStrippingState(
                RetoldAenderWood.AENDER_WOOD.get().defaultBlockState()
        );

        helper.assertTrue(planks.is(BlockTags.PLANKS), "Aender planks must participate in the planks tag");
        helper.assertTrue(
                RetoldAenderWood.AENDER_STAIRS.get().defaultBlockState().is(BlockTags.WOODEN_STAIRS),
                "Aender stairs must participate in the wooden stairs tag"
        );
        helper.assertTrue(
                RetoldAenderWood.AENDER_SLAB.get().defaultBlockState().is(BlockTags.WOODEN_SLABS),
                "Aender slabs must participate in the wooden slabs tag"
        );
        helper.assertTrue(
                RetoldAenderWood.AENDER_FENCE.get().defaultBlockState().is(BlockTags.WOODEN_FENCES),
                "Aender fences must participate in the wooden fences tag"
        );
        helper.assertTrue(
                RetoldAenderWood.AENDER_DOOR.get().defaultBlockState().is(BlockTags.WOODEN_DOORS),
                "Aender doors must participate in the wooden doors tag"
        );
        helper.assertTrue(
                RetoldAenderWood.AENDER_TRAPDOOR.get().defaultBlockState().is(BlockTags.WOODEN_TRAPDOORS),
                "Aender trapdoors must participate in the wooden trapdoors tag"
        );
        helper.assertTrue(
                new ItemStack(RetoldAenderWood.AENDER_SAPLING_ITEM.get()).is(ItemTags.SAPLINGS),
                "Aender saplings must participate in the saplings item tag"
        );
        helper.assertTrue(
                new ItemStack(RetoldAenderWood.AENDER_BOAT_ITEM.get()).is(ItemTags.BOATS),
                "Aender boats must participate in the boats item tag"
        );
        helper.assertTrue(
                new ItemStack(RetoldAenderWood.AENDER_CHEST_BOAT_ITEM.get()).is(ItemTags.CHEST_BOATS),
                "Aender chest boats must participate in the chest boats item tag"
        );
        helper.assertTrue(
                RetoldAenderWood.AENDER_SAPLING_ITEM.get()
                        .builtInRegistryHolder()
                        .getData(NeoForgeDataMaps.COMPOSTABLES) != null,
                "Aender saplings must be compostable"
        );
        helper.assertTrue(
                strippedFromLog != null && strippedFromLog.is(RetoldAenderWood.STRIPPED_AENDER_LOG),
                "An axe must strip Aender logs"
        );
        helper.assertTrue(
                strippedFromWood != null && strippedFromWood.is(RetoldAenderWood.STRIPPED_AENDER_WOOD),
                "An axe must strip Aender wood"
        );
        helper.assertTrue(
                isOnlyDrop(
                        Block.getDrops(planks, level, pos, null),
                        RetoldAenderWood.AENDER_PLANKS_ITEM.get()
                ),
                "Aender planks must drop themselves"
        );
        helper.assertTrue(
                isOnlyDrop(
                        Block.getDrops(strippedLog, level, pos, null),
                        RetoldAenderWood.STRIPPED_AENDER_LOG_ITEM.get()
                ),
                "Stripped Aender logs must drop themselves"
        );
        helper.assertTrue(
                isOnlyDrop(
                        Block.getDrops(
                                RetoldAenderWood.AENDER_WALL_SIGN.get().defaultBlockState(),
                                level,
                                pos,
                                null
                        ),
                        RetoldAenderWood.AENDER_SIGN_ITEM.get()
                ),
                "Aender wall signs must drop the matching Aender sign item"
        );

        level.setBlock(signPos, RetoldAenderWood.AENDER_SIGN.get().defaultBlockState(), 3);
        helper.assertTrue(
                level.getBlockEntity(signPos) instanceof SignBlockEntity,
                "Aender signs must create a functional sign block entity"
        );

        level.setBlock(hangingSignPos.above(), planks, 3);
        level.setBlock(
                hangingSignPos,
                RetoldAenderWood.AENDER_HANGING_SIGN.get().defaultBlockState(),
                3
        );
        helper.assertTrue(
                level.getBlockEntity(hangingSignPos) instanceof HangingSignBlockEntity,
                "Aender hanging signs must create a functional hanging-sign block entity"
        );

        level.setBlock(saplingPos.below(), RetoldBlocks.AENDER_SOIL.get().defaultBlockState(), 3);
        level.setBlock(saplingPos, RetoldAenderWood.AENDER_SAPLING.get().defaultBlockState(), 3);
        RetoldAenderWood.AENDER_SAPLING.get().advanceTree(
                level,
                saplingPos,
                level.getBlockState(saplingPos),
                level.getRandom()
        );
        RetoldAenderWood.AENDER_SAPLING.get().advanceTree(
                level,
                saplingPos,
                level.getBlockState(saplingPos),
                level.getRandom()
        );
        helper.assertTrue(
                level.getBlockState(saplingPos).is(RetoldBlocks.AENDER_LOG),
                "Aender saplings must grow a renewable Aender tree"
        );

        helper.succeed();
    }

    private static boolean isOnlyDrop(List<ItemStack> drops, Item item) {
        return drops.size() == 1 && drops.getFirst().is(item) && drops.getFirst().getCount() == 1;
    }

    private static void contentUsesVanillaCreativeTabs(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        CreativeModeTabs.tryRebuildTabContents(
                level.enabledFeatures(),
                true,
                level.registryAccess()
        );

        assertConsecutiveItems(
                helper,
                CreativeModeTabs.BUILDING_BLOCKS,
                Items.WARPED_BUTTON,
                RetoldBlocks.AENDER_LOG_ITEM.get(),
                RetoldAenderWood.AENDER_WOOD_ITEM.get(),
                RetoldAenderWood.STRIPPED_AENDER_LOG_ITEM.get(),
                RetoldAenderWood.STRIPPED_AENDER_WOOD_ITEM.get(),
                RetoldAenderWood.AENDER_PLANKS_ITEM.get(),
                RetoldAenderWood.AENDER_STAIRS_ITEM.get(),
                RetoldAenderWood.AENDER_SLAB_ITEM.get(),
                RetoldAenderWood.AENDER_FENCE_ITEM.get(),
                RetoldAenderWood.AENDER_FENCE_GATE_ITEM.get(),
                RetoldAenderWood.AENDER_DOOR_ITEM.get(),
                RetoldAenderWood.AENDER_TRAPDOOR_ITEM.get(),
                RetoldAenderWood.AENDER_PRESSURE_PLATE_ITEM.get(),
                RetoldAenderWood.AENDER_BUTTON_ITEM.get()
        );
        assertConsecutiveItems(
                helper,
                CreativeModeTabs.NATURAL_BLOCKS,
                Items.DIRT,
                RetoldBlocks.AENDER_GRASS_BLOCK_ITEM.get(),
                RetoldBlocks.AENDER_SOIL_ITEM.get()
        );
        assertConsecutiveItems(
                helper,
                CreativeModeTabs.NATURAL_BLOCKS,
                Items.END_STONE,
                RetoldBlocks.AENDER_STONE_ITEM.get()
        );
        assertConsecutiveItems(
                helper,
                CreativeModeTabs.NATURAL_BLOCKS,
                Items.PALE_OAK_LOG,
                RetoldBlocks.AENDER_LOG_ITEM.get()
        );
        assertConsecutiveItems(
                helper,
                CreativeModeTabs.NATURAL_BLOCKS,
                Items.PALE_OAK_LEAVES,
                RetoldBlocks.AENDER_LEAVES_ITEM.get()
        );
        assertConsecutiveItems(
                helper,
                CreativeModeTabs.NATURAL_BLOCKS,
                Items.PALE_OAK_SAPLING,
                RetoldAenderWood.AENDER_SAPLING_ITEM.get()
        );
        assertConsecutiveItems(
                helper,
                CreativeModeTabs.FUNCTIONAL_BLOCKS,
                Items.WARPED_HANGING_SIGN,
                RetoldAenderWood.AENDER_SIGN_ITEM.get(),
                RetoldAenderWood.AENDER_HANGING_SIGN_ITEM.get()
        );
        assertConsecutiveItems(
                helper,
                CreativeModeTabs.FUNCTIONAL_BLOCKS,
                Items.CONDUIT,
                RetoldBlocks.AENDER_STABILIZER_ITEM.get(),
                RetoldBlocks.DEV_TIME_ACCELERATOR_ITEM.get()
        );
        assertConsecutiveItems(
                helper,
                CreativeModeTabs.TOOLS_AND_UTILITIES,
                Items.PALE_OAK_CHEST_BOAT,
                RetoldAenderWood.AENDER_BOAT_ITEM.get(),
                RetoldAenderWood.AENDER_CHEST_BOAT_ITEM.get()
        );
        assertConsecutiveItems(
                helper,
                CreativeModeTabs.INGREDIENTS,
                Items.NETHER_STAR,
                RetoldBlocks.WATER_ELEMENT.get(),
                RetoldBlocks.AIR_ELEMENT.get()
        );
        assertConsecutiveItems(
                helper,
                CreativeModeTabs.SPAWN_EGGS,
                Items.BREEZE_SPAWN_EGG,
                RetoldBlocks.GALE_CORE_SPAWN_EGG.get()
        );
        assertConsecutiveItems(
                helper,
                CreativeModeTabs.SPAWN_EGGS,
                Items.ENDERMAN_SPAWN_EGG,
                RetoldBlocks.AENDER_EYE_SPAWN_EGG.get()
        );
        assertCreativeTabContains(
                helper,
                CreativeModeTabs.OP_BLOCKS,
                RetoldBlocks.DEV_AENDER_PORTAL_FRAME_ITEM.get()
        );
        helper.assertTrue(
                SpawnEggItem.spawnsEntity(
                        new ItemStack(RetoldBlocks.AENDER_EYE_SPAWN_EGG.get()),
                        RetoldEntityTypes.AENDER_EYE.get()
                ),
                "The Aender Eye spawn egg must spawn Aender Eyes"
        );
        helper.assertTrue(
                SpawnEggItem.spawnsEntity(
                        new ItemStack(RetoldBlocks.GALE_CORE_SPAWN_EGG.get()),
                        RetoldEntityTypes.GALE_CORE.get()
                ),
                "The Gale Core spawn egg must spawn Gale Cores"
        );

        helper.succeed();
    }

    private static void assertCreativeTabContains(
            GameTestHelper helper,
            ResourceKey<CreativeModeTab> tabKey,
            ItemLike... expectedItems
    ) {
        CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.getValueOrThrow(tabKey);

        for (ItemLike expected : expectedItems) {
            helper.assertTrue(
                    creativeTabIndex(tab, expected) >= 0,
                    expected.asItem() + " must appear in its vanilla creative tab"
            );
        }
    }

    private static void assertConsecutiveItems(
            GameTestHelper helper,
            ResourceKey<CreativeModeTab> tabKey,
            ItemLike anchor,
            ItemLike... expectedItems
    ) {
        CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.getValueOrThrow(tabKey);
        int previousIndex = creativeTabIndex(tab, anchor);
        helper.assertTrue(previousIndex >= 0, "Creative tab anchor is missing: " + anchor.asItem());

        for (ItemLike expected : expectedItems) {
            int expectedIndex = creativeTabIndex(tab, expected);
            helper.assertTrue(
                    expectedIndex == previousIndex + 1,
                    expected.asItem() + " must follow its vanilla creative-tab neighbor"
            );
            previousIndex = expectedIndex;
        }
    }

    private static int creativeTabIndex(CreativeModeTab tab, ItemLike target) {
        int index = 0;

        for (ItemStack stack : tab.getDisplayItems()) {
            if (stack.is(target.asItem())) {
                return index;
            }
            index++;
        }

        return -1;
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
