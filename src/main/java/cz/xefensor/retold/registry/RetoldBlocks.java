package cz.xefensor.retold.registry;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.item.WaterElementItem;
import cz.xefensor.retold.block.ExtinguishedTorchBlock;
import cz.xefensor.retold.block.ExtinguishedWallTorchBlock;
import cz.xefensor.retold.block.AenderChronolithBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class RetoldBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Retold.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Retold.MODID);

    public static final DeferredItem<WaterElementItem> WATER_ELEMENT = ITEMS.registerItem(
            "water_element",
            WaterElementItem::new,
            properties -> properties
                    .stacksTo(1)
                    .rarity(Rarity.EPIC)
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
