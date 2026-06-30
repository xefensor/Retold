package cz.xefensor.retold.registry;

import cz.xefensor.retold.Retold;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class RetoldBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(Retold.MODID);

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(Retold.MODID);

    public static final DeferredBlock<Block> AENDER_STABILIZER =
            BLOCKS.register(
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

    public static final DeferredItem<BlockItem> AENDER_STABILIZER_ITEM =
            ITEMS.registerSimpleBlockItem(
                    AENDER_STABILIZER,
                    properties -> properties
                            .setId(ResourceKey.create(
                                    Registries.ITEM,
                                    AENDER_STABILIZER.getId()
                            ))
            );

    private RetoldBlocks() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
    }
}