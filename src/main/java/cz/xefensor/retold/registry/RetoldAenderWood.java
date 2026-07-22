package cz.xefensor.retold.registry;

import cz.xefensor.retold.Retold;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.HangingSignItem;
import net.minecraft.world.item.SignItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallHangingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Optional;

/** Complete renewable building family for the Aender tree. */
public final class RetoldAenderWood {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Retold.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Retold.MODID);

    public static final BlockSetType BLOCK_SET_TYPE = BlockSetType.register(
            new BlockSetType(Retold.MODID + ":aender")
    );
    public static final WoodType WOOD_TYPE = WoodType.register(
            new WoodType(Retold.MODID + ":aender", BLOCK_SET_TYPE)
    );

    private static final ResourceKey<ConfiguredFeature<?, ?>> AENDER_TREE_FEATURE = ResourceKey.create(
            Registries.CONFIGURED_FEATURE,
            Identifier.fromNamespaceAndPath(Retold.MODID, "aender_tree")
    );
    private static final TreeGrower AENDER_TREE_GROWER = new TreeGrower(
            Retold.MODID + ":aender",
            Optional.empty(),
            Optional.of(AENDER_TREE_FEATURE),
            Optional.empty()
    );

    public static final DeferredBlock<SaplingBlock> AENDER_SAPLING = BLOCKS.register(
            "aender_sapling",
            id -> new SaplingBlock(AENDER_TREE_GROWER, copy(Blocks.OAK_SAPLING, id))
    );
    public static final DeferredBlock<RotatedPillarBlock> AENDER_WOOD = pillar("aender_wood", Blocks.OAK_WOOD);
    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_AENDER_LOG = pillar(
            "stripped_aender_log",
            Blocks.STRIPPED_OAK_LOG
    );
    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_AENDER_WOOD = pillar(
            "stripped_aender_wood",
            Blocks.STRIPPED_OAK_WOOD
    );
    public static final DeferredBlock<Block> AENDER_PLANKS = BLOCKS.register(
            "aender_planks",
            id -> new Block(copy(Blocks.OAK_PLANKS, id))
    );
    public static final DeferredBlock<StairBlock> AENDER_STAIRS = BLOCKS.register(
            "aender_stairs",
            id -> new StairBlock(AENDER_PLANKS.get().defaultBlockState(), copy(Blocks.OAK_STAIRS, id))
    );
    public static final DeferredBlock<SlabBlock> AENDER_SLAB = BLOCKS.register(
            "aender_slab",
            id -> new SlabBlock(copy(Blocks.OAK_SLAB, id))
    );
    public static final DeferredBlock<FenceBlock> AENDER_FENCE = BLOCKS.register(
            "aender_fence",
            id -> new FenceBlock(copy(Blocks.OAK_FENCE, id))
    );
    public static final DeferredBlock<FenceGateBlock> AENDER_FENCE_GATE = BLOCKS.register(
            "aender_fence_gate",
            id -> new FenceGateBlock(WOOD_TYPE, copy(Blocks.OAK_FENCE_GATE, id))
    );
    public static final DeferredBlock<DoorBlock> AENDER_DOOR = BLOCKS.register(
            "aender_door",
            id -> new DoorBlock(BLOCK_SET_TYPE, copy(Blocks.OAK_DOOR, id))
    );
    public static final DeferredBlock<TrapDoorBlock> AENDER_TRAPDOOR = BLOCKS.register(
            "aender_trapdoor",
            id -> new TrapDoorBlock(BLOCK_SET_TYPE, copy(Blocks.OAK_TRAPDOOR, id))
    );
    public static final DeferredBlock<ButtonBlock> AENDER_BUTTON = BLOCKS.register(
            "aender_button",
            id -> new ButtonBlock(BLOCK_SET_TYPE, 30, copy(Blocks.OAK_BUTTON, id))
    );
    public static final DeferredBlock<PressurePlateBlock> AENDER_PRESSURE_PLATE = BLOCKS.register(
            "aender_pressure_plate",
            id -> new PressurePlateBlock(BLOCK_SET_TYPE, copy(Blocks.OAK_PRESSURE_PLATE, id))
    );
    public static final DeferredBlock<StandingSignBlock> AENDER_SIGN = BLOCKS.register(
            "aender_sign",
            id -> new StandingSignBlock(WOOD_TYPE, copy(Blocks.OAK_SIGN, id))
    );
    public static final DeferredBlock<WallSignBlock> AENDER_WALL_SIGN = BLOCKS.register(
            "aender_wall_sign",
            id -> new WallSignBlock(
                    WOOD_TYPE,
                    copy(Blocks.OAK_WALL_SIGN, id)
                            .overrideLootTable(AENDER_SIGN.get().getLootTable())
                            .overrideDescription(AENDER_SIGN.get().getDescriptionId())
            )
    );
    public static final DeferredBlock<CeilingHangingSignBlock> AENDER_HANGING_SIGN = BLOCKS.register(
            "aender_hanging_sign",
            id -> new CeilingHangingSignBlock(WOOD_TYPE, copy(Blocks.OAK_HANGING_SIGN, id))
    );
    public static final DeferredBlock<WallHangingSignBlock> AENDER_WALL_HANGING_SIGN = BLOCKS.register(
            "aender_wall_hanging_sign",
            id -> new WallHangingSignBlock(
                    WOOD_TYPE,
                    copy(Blocks.OAK_WALL_HANGING_SIGN, id)
                            .overrideLootTable(AENDER_HANGING_SIGN.get().getLootTable())
                            .overrideDescription(AENDER_HANGING_SIGN.get().getDescriptionId())
            )
    );

    public static final DeferredItem<BlockItem> AENDER_SAPLING_ITEM = ITEMS.registerSimpleBlockItem(AENDER_SAPLING);
    public static final DeferredItem<BlockItem> AENDER_WOOD_ITEM = ITEMS.registerSimpleBlockItem(AENDER_WOOD);
    public static final DeferredItem<BlockItem> STRIPPED_AENDER_LOG_ITEM = ITEMS.registerSimpleBlockItem(STRIPPED_AENDER_LOG);
    public static final DeferredItem<BlockItem> STRIPPED_AENDER_WOOD_ITEM = ITEMS.registerSimpleBlockItem(STRIPPED_AENDER_WOOD);
    public static final DeferredItem<BlockItem> AENDER_PLANKS_ITEM = ITEMS.registerSimpleBlockItem(AENDER_PLANKS);
    public static final DeferredItem<BlockItem> AENDER_STAIRS_ITEM = ITEMS.registerSimpleBlockItem(AENDER_STAIRS);
    public static final DeferredItem<BlockItem> AENDER_SLAB_ITEM = ITEMS.registerSimpleBlockItem(AENDER_SLAB);
    public static final DeferredItem<BlockItem> AENDER_FENCE_ITEM = ITEMS.registerSimpleBlockItem(AENDER_FENCE);
    public static final DeferredItem<BlockItem> AENDER_FENCE_GATE_ITEM = ITEMS.registerSimpleBlockItem(AENDER_FENCE_GATE);
    public static final DeferredItem<BlockItem> AENDER_DOOR_ITEM = ITEMS.registerSimpleBlockItem(AENDER_DOOR);
    public static final DeferredItem<BlockItem> AENDER_TRAPDOOR_ITEM = ITEMS.registerSimpleBlockItem(AENDER_TRAPDOOR);
    public static final DeferredItem<BlockItem> AENDER_BUTTON_ITEM = ITEMS.registerSimpleBlockItem(AENDER_BUTTON);
    public static final DeferredItem<BlockItem> AENDER_PRESSURE_PLATE_ITEM = ITEMS.registerSimpleBlockItem(AENDER_PRESSURE_PLATE);
    public static final DeferredItem<SignItem> AENDER_SIGN_ITEM = ITEMS.registerItem(
            "aender_sign",
            properties -> new SignItem(AENDER_SIGN.get(), AENDER_WALL_SIGN.get(), properties.stacksTo(16))
    );
    public static final DeferredItem<HangingSignItem> AENDER_HANGING_SIGN_ITEM = ITEMS.registerItem(
            "aender_hanging_sign",
            properties -> new HangingSignItem(
                    AENDER_HANGING_SIGN.get(),
                    AENDER_WALL_HANGING_SIGN.get(),
                    properties.stacksTo(16)
            )
    );
    public static final DeferredItem<BoatItem> AENDER_BOAT_ITEM = ITEMS.registerItem(
            "aender_boat",
            properties -> new BoatItem(RetoldEntityTypes.AENDER_BOAT.get(), properties.stacksTo(1))
    );
    public static final DeferredItem<BoatItem> AENDER_CHEST_BOAT_ITEM = ITEMS.registerItem(
            "aender_chest_boat",
            properties -> new BoatItem(RetoldEntityTypes.AENDER_CHEST_BOAT.get(), properties.stacksTo(1))
    );

    private RetoldAenderWood() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        modEventBus.addListener(RetoldAenderWood::addSignBlocksToVanillaTypes);
    }

    private static void addSignBlocksToVanillaTypes(BlockEntityTypeAddBlocksEvent event) {
        event.modify(BlockEntityTypes.SIGN, AENDER_SIGN.get(), AENDER_WALL_SIGN.get());
        event.modify(
                BlockEntityTypes.HANGING_SIGN,
                AENDER_HANGING_SIGN.get(),
                AENDER_WALL_HANGING_SIGN.get()
        );
    }

    private static DeferredBlock<RotatedPillarBlock> pillar(String name, Block vanillaBlock) {
        return BLOCKS.register(name, id -> new RotatedPillarBlock(copy(vanillaBlock, id)));
    }

    private static BlockBehaviour.Properties copy(Block vanillaBlock, Identifier id) {
        return BlockBehaviour.Properties
                .ofFullCopy(vanillaBlock)
                .setId(ResourceKey.create(Registries.BLOCK, id));
    }
}
