package cz.xefensor.retold.villager;

import cz.xefensor.retold.Retold;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
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
import net.minecraft.world.entity.ai.behavior.HarvestFarmland;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.function.Consumer;
import java.util.List;

public final class RetoldVillageCropReputationGameTests {
    private static final Identifier EMPTY_STRUCTURE =
            Identifier.withDefaultNamespace("empty");

    private RetoldVillageCropReputationGameTests() {
    }

    public static void register(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment =
                event.registerEnvironment(
                        id("isolated_village_crop_reputation"),
                        new TestEnvironmentDefinition.AllOf()
                );
        registerTest(
                event,
                environment,
                "village_crop_reputation_tracks_farmer_and_player_crops",
                RetoldVillageCropReputationGameTests::tracksFarmerAndPlayerCrops
        );
        registerTest(
                event,
                environment,
                "village_crop_reputation_distinguishes_harvest_and_vandalism",
                RetoldVillageCropReputationGameTests::distinguishesHarvestAndVandalism
        );
        registerTest(
                event,
                environment,
                "village_crop_reputation_handles_farmland_trampling",
                RetoldVillageCropReputationGameTests::handlesFarmlandTrampling
        );
        registerTest(
                event,
                environment,
                "village_crop_reputation_uses_vanilla_farmer_hook",
                RetoldVillageCropReputationGameTests::usesVanillaFarmerHook
        );
    }

    private static void tracksFarmerAndPlayerCrops(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos relativeCrop = new BlockPos(3, 2, 2);
        BlockPos cropPos = helper.absolutePos(relativeCrop);
        placeFarmland(helper, relativeCrop.below());
        BlockState young = Blocks.WHEAT.defaultBlockState();
        helper.setBlock(relativeCrop, young);
        RetoldVillageCropOwnership.clear(level, cropPos);
        RetoldVillageCropOwnership.afterFarmerWork(
                level,
                cropPos,
                Blocks.AIR.defaultBlockState(),
                young
        );
        helper.assertTrue(
                RetoldVillageCropOwnership.isOwned(level, cropPos),
                "A newly Farmer-planted crop must become village-owned"
        );

        CropBlock wheat = (CropBlock) Blocks.WHEAT;
        BlockState mature = wheat.getStateForAge(wheat.getMaxAge());
        helper.setBlock(relativeCrop, mature);
        helper.assertTrue(
                RetoldVillageCropOwnership.isOwned(level, cropPos),
                "Natural growth must retain Farmer-crop ownership"
        );

        RetoldVillageCropOwnershipData restored =
                RetoldVillageCropOwnershipData.fromSerializedState(
                        RetoldVillageCropOwnershipData.get(level)
                                .serializeState()
                );
        helper.assertTrue(
                restored.isOwned(level, cropPos),
                "Farmer-crop ownership must survive SavedData serialization"
        );

        ServerPlayer player = makePlayer(helper, GameType.SURVIVAL);
        helper.setBlock(relativeCrop, young);
        RetoldVillageCropOwnership.handlePlayerPlacement(
                level,
                cropPos,
                young,
                player
        );
        helper.assertTrue(
                !RetoldVillageCropOwnership.isOwned(level, cropPos),
                "A player-planted crop must remain player-owned"
        );

        RetoldVillageCropOwnership.afterFarmerWork(
                level,
                cropPos,
                young,
                young
        );
        helper.assertTrue(
                !RetoldVillageCropOwnership.isOwned(level, cropPos),
                "Unchanged player crops must not be claimed by Farmer observation"
        );
        removePlayer(level, player);
        helper.succeed();
    }

    private static void distinguishesHarvestAndVandalism(
            GameTestHelper helper
    ) {
        ServerLevel level = helper.getLevel();
        placeFloor(helper);
        BlockPos matureRelative = new BlockPos(3, 2, 2);
        BlockPos youngRelative = new BlockPos(5, 2, 2);
        BlockPos maturePos = helper.absolutePos(matureRelative);
        BlockPos youngPos = helper.absolutePos(youngRelative);
        CropBlock wheat = (CropBlock) Blocks.WHEAT;
        BlockState mature = wheat.getStateForAge(wheat.getMaxAge());
        BlockState young = wheat.getStateForAge(1);
        helper.setBlock(matureRelative, mature);
        helper.setBlock(youngRelative, young);
        RetoldVillageCropOwnership.mark(level, maturePos);
        RetoldVillageCropOwnership.mark(level, youngPos);

        ServerPlayer player = makePlayer(helper, GameType.SURVIVAL);
        Villager witness = helper.spawn(EntityTypes.VILLAGER, 4, 2, 3);
        setVillageHome(helper, witness, new BlockPos(4, 2, 3));
        helper.assertTrue(
                RetoldVillageCropOwnership.handlePlayerBreak(
                        level,
                        maturePos,
                        mature,
                        player
                ),
                "Harvesting a mature Farmer crop must be recognized"
        );
        helper.assertValueEqual(
                witness.getPlayerReputation(player),
                -25,
                "A witnessed mature harvest must be minor theft"
        );
        helper.assertTrue(
                RetoldVillageCropOwnership.handlePlayerBreak(
                        level,
                        youngPos,
                        young,
                        player
                ),
                "Breaking an immature Farmer crop must be recognized"
        );
        helper.assertValueEqual(
                witness.getPlayerReputation(player),
                -75,
                "Immature crop vandalism must add a stronger -50 penalty"
        );

        BlockPos creativeRelative = new BlockPos(7, 2, 2);
        BlockPos creativePos = helper.absolutePos(creativeRelative);
        helper.setBlock(creativeRelative, young);
        RetoldVillageCropOwnership.mark(level, creativePos);
        ServerPlayer creative = makePlayer(helper, GameType.CREATIVE);
        RetoldVillageCropOwnership.handlePlayerBreak(
                level,
                creativePos,
                young,
                creative
        );
        helper.assertValueEqual(
                witness.getPlayerReputation(creative),
                0,
                "Creative crop changes must not damage village reputation"
        );

        removePlayer(level, creative);
        removePlayer(level, player);
        witness.discard();
        helper.succeed();
    }

    private static void handlesFarmlandTrampling(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        placeFloor(helper);
        BlockPos farmlandRelative = new BlockPos(4, 1, 2);
        BlockPos cropRelative = farmlandRelative.above();
        BlockPos farmlandPos = helper.absolutePos(farmlandRelative);
        BlockPos cropPos = helper.absolutePos(cropRelative);
        helper.setBlock(farmlandRelative, Blocks.FARMLAND);
        helper.setBlock(cropRelative, Blocks.CARROTS);
        RetoldVillageCropOwnership.mark(level, cropPos);

        ServerPlayer player = makePlayer(helper, GameType.SURVIVAL);
        Villager witness = helper.spawn(EntityTypes.VILLAGER, 4, 2, 3);
        setVillageHome(helper, witness, new BlockPos(4, 2, 3));
        helper.assertTrue(
                RetoldVillageCropOwnership.handleFarmlandTrample(
                        level,
                        farmlandPos,
                        player
                ),
                "Trampling below a Farmer crop must be recognized"
        );
        helper.assertTrue(
                !RetoldVillageCropOwnership.isOwned(level, cropPos),
                "Trampling must clear the destroyed crop's ownership"
        );
        helper.assertValueEqual(
                witness.getPlayerReputation(player),
                -50,
                "Witnessed farmland trampling must be crop vandalism"
        );

        removePlayer(level, player);
        witness.discard();
        helper.succeed();
    }

    private static void usesVanillaFarmerHook(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos cropRelative = new BlockPos(4, 2, 2);
        BlockPos cropPos = helper.absolutePos(cropRelative);
        helper.setBlock(cropRelative.below(), Blocks.FARMLAND);
        helper.setBlock(cropRelative, Blocks.AIR);
        RetoldVillageCropOwnership.clear(level, cropPos);

        Villager farmer = helper.spawn(EntityTypes.VILLAGER, 4, 2, 2);
        farmer.setVillagerData(
                farmer.getVillagerData().withProfession(
                        level.registryAccess(),
                        VillagerProfession.FARMER
                )
        );
        farmer.getInventory().setItem(
                0,
                new ItemStack(Items.WHEAT_SEEDS)
        );
        farmer.getBrain().setMemory(
                MemoryModuleType.SECONDARY_JOB_SITE,
                List.of(GlobalPos.of(level.dimension(), cropPos.below()))
        );

        HarvestFarmland behavior = new HarvestFarmland();
        long gameTime = Math.max(1L, level.getGameTime());
        helper.assertTrue(
                behavior.tryStart(level, farmer, gameTime),
                "Vanilla HarvestFarmland must start on the empty fixture"
        );
        behavior.tickOrStop(level, farmer, gameTime + 1L);
        helper.assertTrue(
                level.getBlockState(cropPos).getBlock()
                        instanceof CropBlock,
                "Vanilla Farmer behavior must plant the fixture crop"
        );
        helper.assertTrue(
                RetoldVillageCropOwnership.isOwned(level, cropPos),
                "The HarvestFarmland mixin must mark the planted crop"
        );

        farmer.discard();
        helper.succeed();
    }

    private static ServerPlayer makePlayer(
            GameTestHelper helper,
            GameType gameType
    ) {
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(
                gameType
        );
        BlockPos position = helper.absolutePos(new BlockPos(4, 2, 2));
        player.snapTo(
                position.getX() + 0.5D,
                position.getY(),
                position.getZ() + 1.5D,
                0.0F,
                0.0F
        );
        return player;
    }

    private static void removePlayer(
            ServerLevel level,
            ServerPlayer player
    ) {
        level.players().remove(player);
        player.discard();
    }

    private static void setVillageHome(
            GameTestHelper helper,
            Villager villager,
            BlockPos relativePos
    ) {
        villager.getBrain().setMemory(
                MemoryModuleType.HOME,
                GlobalPos.of(
                        helper.getLevel().dimension(),
                        helper.absolutePos(relativePos)
                )
        );
    }

    private static void placeFarmland(
            GameTestHelper helper,
            BlockPos relativePos
    ) {
        helper.setBlock(relativePos, Blocks.FARMLAND);
    }

    private static void placeFloor(GameTestHelper helper) {
        for (int x = 1; x <= 8; x++) {
            for (int z = 1; z <= 4; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.FARMLAND);
            }
        }
    }

    private static void registerTest(
            RegisterGameTestsEvent event,
            Holder<TestEnvironmentDefinition<?>> environment,
            String name,
            Consumer<GameTestHelper> function
    ) {
        TestData<Holder<TestEnvironmentDefinition<?>>> testData =
                new TestData<>(environment, EMPTY_STRUCTURE, 80, 0, true);
        event.registerTest(id(name), new InlineGameTest(testData, function));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Retold.MODID, path);
    }

    private static final class InlineGameTest
            extends FunctionGameTestInstance {
        private final Consumer<GameTestHelper> function;

        private InlineGameTest(
                TestData<Holder<TestEnvironmentDefinition<?>>> testData,
                Consumer<GameTestHelper> function
        ) {
            super(BuiltinTestFunctions.ALWAYS_PASS, testData);
            this.function = function;
        }

        @Override
        public void run(GameTestHelper helper) {
            function.accept(helper);
        }
    }
}
