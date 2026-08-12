package cz.xefensor.retold.registry;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.item.AirElementItem;
import cz.xefensor.retold.item.WaterElementItem;
import cz.xefensor.retold.block.ExtinguishedTorchBlock;
import cz.xefensor.retold.block.ExtinguishedWallTorchBlock;
import cz.xefensor.retold.block.AenderChronolithBlock;
import cz.xefensor.retold.block.AenderCactusBlock;
import cz.xefensor.retold.block.AenderGrassBlock;
import cz.xefensor.retold.block.AenderLeavesBlock;
import cz.xefensor.retold.block.AenderPortalBlock;
import cz.xefensor.retold.block.AenderPortalFrameBlock;
import cz.xefensor.retold.block.AnimalFeederBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.ColorRGBA;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ColoredFallingBlock;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.util.valueproviders.UniformInt;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Map;

public final class RetoldBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Retold.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Retold.MODID);

    private static final ToolMaterial FLINT_MULTI_TOOL_MATERIAL =
            new ToolMaterial(
                    RetoldTags.INCORRECT_FOR_FLINT_MULTI_TOOL,
                    48,
                    2.0F,
                    0.0F,
                    5,
                    RetoldTags.FLINT_MULTI_TOOL_REPAIR_MATERIALS
            );
    private static final ToolMaterial STEEL_TOOL_MATERIAL =
            new ToolMaterial(
                    RetoldTags.INCORRECT_FOR_STEEL_TOOL,
                    750,
                    7.0F,
                    2.5F,
                    12,
                    RetoldTags.STEEL_REPAIR_MATERIALS
            );
    private static final ArmorMaterial STEEL_ARMOR_MATERIAL =
            new ArmorMaterial(
                    25,
                    Map.of(
                            ArmorType.BOOTS, 3,
                            ArmorType.LEGGINGS, 6,
                            ArmorType.CHESTPLATE, 7,
                            ArmorType.HELMET, 3,
                            ArmorType.BODY, 9
                    ),
                    12,
                    SoundEvents.ARMOR_EQUIP_IRON,
                    1.0F,
                    0.0F,
                    RetoldTags.STEEL_REPAIR_MATERIALS,
                    EquipmentAssets.IRON
            );

    public static final DeferredItem<Item> FLINT_MULTI_TOOL = ITEMS.registerItem(
            "flint_multi_tool",
            Item::new,
            properties -> properties.tool(
                    FLINT_MULTI_TOOL_MATERIAL,
                    RetoldTags.FLINT_MULTI_TOOL_MINEABLE,
                    1.0F,
                    -2.8F,
                    0.0F
            )
    );
    public static final DeferredItem<Item> FLINT_SPEAR = ITEMS.registerItem(
            "flint_spear",
            Item::new,
            properties -> properties.spear(
                    FLINT_MULTI_TOOL_MATERIAL,
                    0.75F,
                    0.82F,
                    0.70F,
                    4.5F,
                    13.0F,
                    9.0F,
                    5.1F,
                    13.75F,
                    4.6F
            )
    );
    public static final DeferredItem<Item> STEEL_INGOT = ITEMS.registerSimpleItem(
            "steel_ingot"
    );
    public static final DeferredItem<Item> STEEL_SWORD = ITEMS.registerItem(
            "steel_sword",
            Item::new,
            properties -> properties.sword(
                    STEEL_TOOL_MATERIAL,
                    3.0F,
                    -2.4F
            )
    );
    public static final DeferredItem<Item> STEEL_SPEAR = ITEMS.registerItem(
            "steel_spear",
            Item::new,
            properties -> properties.spear(
                    STEEL_TOOL_MATERIAL,
                    1.0F,
                    1.0F,
                    0.55F,
                    2.75F,
                    10.5F,
                    6.625F,
                    5.1F,
                    10.625F,
                    4.6F
            )
    );
    public static final DeferredItem<ShovelItem> STEEL_SHOVEL = ITEMS.registerItem(
            "steel_shovel",
            properties -> new ShovelItem(
                    STEEL_TOOL_MATERIAL,
                    1.5F,
                    -3.0F,
                    properties
            )
    );
    public static final DeferredItem<Item> STEEL_PICKAXE = ITEMS.registerItem(
            "steel_pickaxe",
            Item::new,
            properties -> properties.pickaxe(
                    STEEL_TOOL_MATERIAL,
                    1.0F,
                    -2.8F
            )
    );
    public static final DeferredItem<AxeItem> STEEL_AXE = ITEMS.registerItem(
            "steel_axe",
            properties -> new AxeItem(
                    STEEL_TOOL_MATERIAL,
                    5.5F,
                    -3.05F,
                    properties
            )
    );
    public static final DeferredItem<HoeItem> STEEL_HOE = ITEMS.registerItem(
            "steel_hoe",
            properties -> new HoeItem(
                    STEEL_TOOL_MATERIAL,
                    -2.5F,
                    -0.5F,
                    properties
            )
    );
    public static final DeferredItem<Item> STEEL_HELMET = ITEMS.registerItem(
            "steel_helmet",
            Item::new,
            properties -> properties.humanoidArmor(
                    STEEL_ARMOR_MATERIAL,
                    ArmorType.HELMET
            )
    );
    public static final DeferredItem<Item> STEEL_CHESTPLATE = ITEMS.registerItem(
            "steel_chestplate",
            Item::new,
            properties -> properties.humanoidArmor(
                    STEEL_ARMOR_MATERIAL,
                    ArmorType.CHESTPLATE
            )
    );
    public static final DeferredItem<Item> STEEL_LEGGINGS = ITEMS.registerItem(
            "steel_leggings",
            Item::new,
            properties -> properties.humanoidArmor(
                    STEEL_ARMOR_MATERIAL,
                    ArmorType.LEGGINGS
            )
    );
    public static final DeferredItem<Item> STEEL_BOOTS = ITEMS.registerItem(
            "steel_boots",
            Item::new,
            properties -> properties.humanoidArmor(
                    STEEL_ARMOR_MATERIAL,
                    ArmorType.BOOTS
            )
    );

    public static final DeferredItem<WaterElementItem> WATER_ELEMENT = ITEMS.registerItem(
            "water_element",
            WaterElementItem::new,
            properties -> properties
                    .stacksTo(1)
                    .rarity(Rarity.EPIC)
    );

    public static final DeferredItem<AirElementItem> AIR_ELEMENT = ITEMS.registerItem(
            "air_element",
            AirElementItem::new,
            properties -> properties
                    .stacksTo(1)
                    .rarity(Rarity.EPIC)
    );

    public static final DeferredItem<SpawnEggItem> AENDER_EYE_SPAWN_EGG = ITEMS.registerItem(
            "aender_eye_spawn_egg",
            properties -> new SpawnEggItem(properties.spawnEgg(RetoldEntityTypes.AENDER_EYE.get()))
    );

    public static final DeferredItem<SpawnEggItem> GALE_CORE_SPAWN_EGG = ITEMS.registerItem(
            "gale_core_spawn_egg",
            properties -> new SpawnEggItem(properties.spawnEgg(RetoldEntityTypes.GALE_CORE.get()))
    );

    public static final DeferredBlock<AnimalFeederBlock> ANIMAL_FEEDER = BLOCKS.register(
            "animal_feeder",
            registryName -> new AnimalFeederBlock(
                    BlockBehaviour.Properties
                            .ofFullCopy(Blocks.OAK_PLANKS)
                            .setId(ResourceKey.create(Registries.BLOCK, registryName))
                            .strength(2.0F, 3.0F)
                            .sound(SoundType.WOOD)
                            .noOcclusion()
            )
    );

    public static final DeferredItem<BlockItem> ANIMAL_FEEDER_ITEM = ITEMS.registerSimpleBlockItem(
            ANIMAL_FEEDER,
            properties -> properties.setId(ResourceKey.create(
                    Registries.ITEM,
                    ANIMAL_FEEDER.getId()
            ))
    );

    public static final DeferredBlock<AenderGrassBlock> AENDER_GRASS_BLOCK = BLOCKS.register(
            "aender_grass_block",
            registryName -> new AenderGrassBlock(
                    BlockBehaviour.Properties
                            .ofFullCopy(Blocks.GRASS_BLOCK)
                            .setId(ResourceKey.create(
                                    Registries.BLOCK,
                                    registryName
                            ))
                            .sound(SoundType.GRASS)
            )
    );

    public static final DeferredItem<BlockItem> AENDER_GRASS_BLOCK_ITEM = ITEMS.registerSimpleBlockItem(
            AENDER_GRASS_BLOCK,
            properties -> properties.setId(ResourceKey.create(
                    Registries.ITEM,
                    AENDER_GRASS_BLOCK.getId()
            ))
    );

    public static final DeferredBlock<Block> AENDER_SOIL = BLOCKS.register(
            "aender_soil",
            registryName -> new Block(
                    BlockBehaviour.Properties
                            .ofFullCopy(Blocks.DIRT)
                            .setId(ResourceKey.create(
                                    Registries.BLOCK,
                                    registryName
                            ))
                            .sound(SoundType.GRAVEL)
            )
    );

    public static final DeferredItem<BlockItem> AENDER_SOIL_ITEM = ITEMS.registerSimpleBlockItem(
            AENDER_SOIL,
            properties -> properties.setId(ResourceKey.create(
                    Registries.ITEM,
                    AENDER_SOIL.getId()
            ))
    );

    public static final DeferredBlock<Block> AENDER_STONE = BLOCKS.register(
            "aender_stone",
            registryName -> new Block(
                    BlockBehaviour.Properties
                            .ofFullCopy(Blocks.STONE)
                            .setId(ResourceKey.create(
                                    Registries.BLOCK,
                                    registryName
                            ))
                            .sound(SoundType.CALCITE)
            )
    );

    public static final DeferredItem<BlockItem> AENDER_STONE_ITEM = ITEMS.registerSimpleBlockItem(
            AENDER_STONE,
            properties -> properties.setId(ResourceKey.create(
                    Registries.ITEM,
                    AENDER_STONE.getId()
            ))
    );

    public static final DeferredBlock<DropExperienceBlock> AENDERITE_ORE = BLOCKS.register(
            "aenderite_ore",
            registryName -> new DropExperienceBlock(
                    UniformInt.of(3, 7),
                    BlockBehaviour.Properties
                            .ofFullCopy(Blocks.DIAMOND_ORE)
                            .setId(ResourceKey.create(Registries.BLOCK, registryName))
                            .strength(4.5F, 4.0F)
                            .sound(SoundType.CALCITE)
            )
    );

    public static final DeferredItem<BlockItem> AENDERITE_ORE_ITEM = ITEMS.registerSimpleBlockItem(
            AENDERITE_ORE,
            properties -> properties.setId(ResourceKey.create(Registries.ITEM, AENDERITE_ORE.getId()))
    );

    public static final DeferredItem<Item> RAW_AENDERITE = ITEMS.registerItem(
            "raw_aenderite",
            Item::new
    );

    public static final DeferredItem<Item> AENDERITE_INGOT = ITEMS.registerItem(
            "aenderite_ingot",
            Item::new
    );

    public static final DeferredBlock<ColoredFallingBlock> AENDER_SAND = BLOCKS.register(
            "aender_sand",
            registryName -> new ColoredFallingBlock(
                    new ColorRGBA(0xFF434F79),
                    BlockBehaviour.Properties
                            .ofFullCopy(Blocks.SAND)
                            .setId(ResourceKey.create(Registries.BLOCK, registryName))
                            .sound(SoundType.SAND)
            )
    );

    public static final DeferredItem<BlockItem> AENDER_SAND_ITEM = ITEMS.registerSimpleBlockItem(
            AENDER_SAND,
            properties -> properties.setId(ResourceKey.create(Registries.ITEM, AENDER_SAND.getId()))
    );

    public static final DeferredBlock<Block> AENDER_SANDSTONE = BLOCKS.register(
            "aender_sandstone",
            registryName -> new Block(
                    BlockBehaviour.Properties
                            .ofFullCopy(Blocks.SANDSTONE)
                            .setId(ResourceKey.create(Registries.BLOCK, registryName))
                            .sound(SoundType.STONE)
            )
    );

    public static final DeferredItem<BlockItem> AENDER_SANDSTONE_ITEM = ITEMS.registerSimpleBlockItem(
            AENDER_SANDSTONE,
            properties -> properties.setId(ResourceKey.create(Registries.ITEM, AENDER_SANDSTONE.getId()))
    );

    public static final DeferredBlock<AenderCactusBlock> AENDER_CACTUS = BLOCKS.register(
            "aender_cactus",
            registryName -> new AenderCactusBlock(
                    BlockBehaviour.Properties
                            .ofFullCopy(Blocks.CACTUS)
                            .setId(ResourceKey.create(Registries.BLOCK, registryName))
            )
    );

    public static final DeferredItem<BlockItem> AENDER_CACTUS_ITEM = ITEMS.registerSimpleBlockItem(
            AENDER_CACTUS,
            properties -> properties.setId(ResourceKey.create(Registries.ITEM, AENDER_CACTUS.getId()))
    );

    public static final DeferredBlock<RotatedPillarBlock> AENDER_LOG = BLOCKS.register(
            "aender_log",
            registryName -> new RotatedPillarBlock(
                    BlockBehaviour.Properties
                            .ofFullCopy(Blocks.OAK_LOG)
                            .setId(ResourceKey.create(
                                    Registries.BLOCK,
                                    registryName
                            ))
                            .sound(SoundType.WOOD)
            )
    );

    public static final DeferredItem<BlockItem> AENDER_LOG_ITEM = ITEMS.registerSimpleBlockItem(
            AENDER_LOG,
            properties -> properties.setId(ResourceKey.create(
                    Registries.ITEM,
                    AENDER_LOG.getId()
            ))
    );

    public static final DeferredBlock<AenderLeavesBlock> AENDER_LEAVES = BLOCKS.register(
            "aender_leaves",
            registryName -> new AenderLeavesBlock(
                    BlockBehaviour.Properties
                            .ofFullCopy(Blocks.OAK_LEAVES)
                            .setId(ResourceKey.create(
                                    Registries.BLOCK,
                                    registryName
                            ))
                            .sound(SoundType.AZALEA_LEAVES)
            )
    );

    public static final DeferredItem<BlockItem> AENDER_LEAVES_ITEM = ITEMS.registerSimpleBlockItem(
            AENDER_LEAVES,
            properties -> properties.setId(ResourceKey.create(
                    Registries.ITEM,
                    AENDER_LEAVES.getId()
            ))
    );

    public static final DeferredBlock<AenderPortalFrameBlock> DEV_AENDER_PORTAL_FRAME = BLOCKS.register(
            "dev_aender_portal_frame",
            registryName -> new AenderPortalFrameBlock(
                    BlockBehaviour.Properties
                            .ofFullCopy(Blocks.AMETHYST_BLOCK)
                            .setId(ResourceKey.create(Registries.BLOCK, registryName))
                            .strength(3.0F, 9.0F)
                            .sound(SoundType.AMETHYST)
                            .lightLevel(state -> 8)
            )
    );

    public static final DeferredItem<BlockItem> DEV_AENDER_PORTAL_FRAME_ITEM = ITEMS.registerSimpleBlockItem(
            DEV_AENDER_PORTAL_FRAME,
            properties -> properties.setId(ResourceKey.create(
                    Registries.ITEM,
                    DEV_AENDER_PORTAL_FRAME.getId()
            ))
    );

    public static final DeferredBlock<AenderPortalBlock> AENDER_PORTAL = BLOCKS.register(
            "aender_portal",
            registryName -> new AenderPortalBlock(
                    BlockBehaviour.Properties
                            .ofFullCopy(Blocks.END_PORTAL)
                            .setId(ResourceKey.create(Registries.BLOCK, registryName))
                            .noCollision()
                            .noOcclusion()
                            .noLootTable()
                            .lightLevel(state -> 11)
            )
    );

    public static final DeferredBlock<Block> AENDER_STABILIZER = BLOCKS.register(
            "aender_stabilizer",
            registryName -> new Block(
                    BlockBehaviour.Properties
                            .ofFullCopy(Blocks.OBSIDIAN)
                            .setId(ResourceKey.create(
                                    Registries.BLOCK,
                                    registryName
                            ))
                            .strength(12.0F, 1200.0F)
                            .sound(SoundType.AMETHYST)
                            .lightLevel(state -> 6)
            )
    );

    public static final DeferredItem<?> AENDER_STABILIZER_ITEM = ITEMS.registerSimpleBlockItem(
            AENDER_STABILIZER,
            properties -> properties
                    .setId(ResourceKey.create(
                            Registries.ITEM,
                            AENDER_STABILIZER.getId()
                    ))
    );

    public static final DeferredBlock<AenderChronolithBlock> DEV_TIME_ACCELERATOR = BLOCKS.register(
            "aender_chronolith",
            registryName -> new AenderChronolithBlock(
                    BlockBehaviour.Properties
                            .ofFullCopy(Blocks.OBSIDIAN)
                            .setId(ResourceKey.create(Registries.BLOCK, registryName))
                            .strength(12.0F, 1200.0F)
                            .sound(SoundType.AMETHYST)
                            .lightLevel(state -> state.getValue(AenderChronolithBlock.LIT) ? 12 : 0)
            )
    );

    public static final DeferredItem<BlockItem> DEV_TIME_ACCELERATOR_ITEM = ITEMS.registerSimpleBlockItem(
            DEV_TIME_ACCELERATOR,
            properties -> properties.setId(ResourceKey.create(
                    Registries.ITEM,
                    DEV_TIME_ACCELERATOR.getId()
            ))
    );

    public static final DeferredBlock<ExtinguishedTorchBlock> EXTINGUISHED_TORCH = BLOCKS.register(
            "extinguished_torch",
            registryName -> new ExtinguishedTorchBlock(
                    Blocks.TORCH,
                    Items.TORCH,
                    BlockBehaviour.Properties
                            .ofFullCopy(Blocks.TORCH)
                            .setId(ResourceKey.create(
                                    Registries.BLOCK,
                                    registryName
                            ))
                            .lightLevel(state -> 0)
            )
    );

    public static final DeferredBlock<ExtinguishedWallTorchBlock> EXTINGUISHED_WALL_TORCH = BLOCKS.register(
            "extinguished_wall_torch",
            registryName -> new ExtinguishedWallTorchBlock(
                    Blocks.WALL_TORCH,
                    Items.TORCH,
                    BlockBehaviour.Properties
                            .ofFullCopy(Blocks.WALL_TORCH)
                            .setId(ResourceKey.create(
                                    Registries.BLOCK,
                                    registryName
                            ))
                            .lightLevel(state -> 0)
            )
    );

    public static final DeferredBlock<ExtinguishedTorchBlock> EXTINGUISHED_SOUL_TORCH = BLOCKS.register(
            "extinguished_soul_torch",
            registryName -> new ExtinguishedTorchBlock(
                    Blocks.SOUL_TORCH,
                    Items.SOUL_TORCH,
                    BlockBehaviour.Properties
                            .ofFullCopy(Blocks.SOUL_TORCH)
                            .setId(ResourceKey.create(
                                    Registries.BLOCK,
                                    registryName
                            ))
                            .lightLevel(state -> 0)
            )
    );

    public static final DeferredBlock<ExtinguishedWallTorchBlock> EXTINGUISHED_SOUL_WALL_TORCH = BLOCKS.register(
            "extinguished_soul_wall_torch",
            registryName -> new ExtinguishedWallTorchBlock(
                    Blocks.SOUL_WALL_TORCH,
                    Items.SOUL_TORCH,
                    BlockBehaviour.Properties
                            .ofFullCopy(Blocks.SOUL_WALL_TORCH)
                            .setId(ResourceKey.create(
                                    Registries.BLOCK,
                                    registryName
                            ))
                            .lightLevel(state -> 0)
            )
    );

    public static final DeferredBlock<ExtinguishedTorchBlock> EXTINGUISHED_COPPER_TORCH = BLOCKS.register(
            "extinguished_copper_torch",
            registryName -> new ExtinguishedTorchBlock(
                    Blocks.COPPER_TORCH,
                    Items.COPPER_TORCH,
                    BlockBehaviour.Properties
                            .ofFullCopy(Blocks.COPPER_TORCH)
                            .setId(ResourceKey.create(
                                    Registries.BLOCK,
                                    registryName
                            ))
                            .lightLevel(state -> 0)
            )
    );

    public static final DeferredBlock<ExtinguishedWallTorchBlock> EXTINGUISHED_COPPER_WALL_TORCH = BLOCKS.register(
            "extinguished_copper_wall_torch",
            registryName -> new ExtinguishedWallTorchBlock(
                    Blocks.COPPER_WALL_TORCH,
                    Items.COPPER_TORCH,
                    BlockBehaviour.Properties
                            .ofFullCopy(Blocks.COPPER_WALL_TORCH)
                            .setId(ResourceKey.create(
                                    Registries.BLOCK,
                                    registryName
                            ))
                            .lightLevel(state -> 0)
            )
    );

    private RetoldBlocks() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
    }
}
