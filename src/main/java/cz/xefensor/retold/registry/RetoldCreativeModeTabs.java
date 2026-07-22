package cz.xefensor.retold.registry;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

/** Places Aender content alongside equivalent vanilla content without adding a Retold tab. */
public final class RetoldCreativeModeTabs {
    private RetoldCreativeModeTabs() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(RetoldCreativeModeTabs::buildContents);
    }

    private static void buildContents(BuildCreativeModeTabContentsEvent event) {
        if (CreativeModeTabs.BUILDING_BLOCKS.equals(event.getTabKey())) {
            addBuildingBlocks(event);
        } else if (CreativeModeTabs.NATURAL_BLOCKS.equals(event.getTabKey())) {
            addNaturalBlocks(event);
        } else if (CreativeModeTabs.FUNCTIONAL_BLOCKS.equals(event.getTabKey())) {
            addFunctionalBlocks(event);
        } else if (CreativeModeTabs.TOOLS_AND_UTILITIES.equals(event.getTabKey())) {
            addToolsAndUtilities(event);
        } else if (CreativeModeTabs.INGREDIENTS.equals(event.getTabKey())) {
            addIngredients(event);
        } else if (CreativeModeTabs.SPAWN_EGGS.equals(event.getTabKey())) {
            addSpawnEggs(event);
        } else if (CreativeModeTabs.OP_BLOCKS.equals(event.getTabKey()) && event.hasPermissions()) {
            addOperatorUtilities(event);
        }
    }

    private static void addBuildingBlocks(BuildCreativeModeTabContentsEvent event) {
        insertAfter(
                event,
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
    }

    private static void addNaturalBlocks(BuildCreativeModeTabContentsEvent event) {
        insertAfter(
                event,
                Items.DIRT,
                RetoldBlocks.AENDER_GRASS_BLOCK_ITEM.get(),
                RetoldBlocks.AENDER_SOIL_ITEM.get()
        );
        insertAfter(
                event,
                Items.END_STONE,
                RetoldBlocks.AENDER_STONE_ITEM.get(),
                RetoldBlocks.AENDER_SAND_ITEM.get(),
                RetoldBlocks.AENDER_SANDSTONE_ITEM.get(),
                RetoldBlocks.AENDER_CACTUS_ITEM.get()
        );
        insertAfter(event, Items.PALE_OAK_LOG, RetoldBlocks.AENDER_LOG_ITEM.get());
        insertAfter(event, Items.PALE_OAK_LEAVES, RetoldBlocks.AENDER_LEAVES_ITEM.get());
        insertAfter(event, Items.PALE_OAK_SAPLING, RetoldAenderWood.AENDER_SAPLING_ITEM.get());
    }

    private static void addFunctionalBlocks(BuildCreativeModeTabContentsEvent event) {
        insertAfter(
                event,
                Items.WARPED_HANGING_SIGN,
                RetoldAenderWood.AENDER_SIGN_ITEM.get(),
                RetoldAenderWood.AENDER_HANGING_SIGN_ITEM.get()
        );
        insertAfter(
                event,
                Items.CONDUIT,
                RetoldBlocks.AENDER_STABILIZER_ITEM.get(),
                RetoldBlocks.DEV_TIME_ACCELERATOR_ITEM.get()
        );
    }

    private static void addToolsAndUtilities(BuildCreativeModeTabContentsEvent event) {
        insertAfter(
                event,
                Items.PALE_OAK_CHEST_BOAT,
                RetoldAenderWood.AENDER_BOAT_ITEM.get(),
                RetoldAenderWood.AENDER_CHEST_BOAT_ITEM.get()
        );
    }

    private static void addIngredients(BuildCreativeModeTabContentsEvent event) {
        insertAfter(
                event,
                Items.NETHER_STAR,
                RetoldBlocks.WATER_ELEMENT.get(),
                RetoldBlocks.AIR_ELEMENT.get()
        );
    }

    private static void addSpawnEggs(BuildCreativeModeTabContentsEvent event) {
        insertAfter(event, Items.BREEZE_SPAWN_EGG, RetoldBlocks.GALE_CORE_SPAWN_EGG.get());
        insertAfter(event, Items.ENDERMAN_SPAWN_EGG, RetoldBlocks.AENDER_EYE_SPAWN_EGG.get());
    }

    private static void addOperatorUtilities(BuildCreativeModeTabContentsEvent event) {
        event.accept(RetoldBlocks.DEV_AENDER_PORTAL_FRAME_ITEM.get());
    }

    private static void insertAfter(
            BuildCreativeModeTabContentsEvent event,
            ItemLike anchor,
            ItemLike... entries
    ) {
        ItemStack previous = new ItemStack(anchor);

        for (ItemLike entry : entries) {
            ItemStack next = new ItemStack(entry);
            event.insertAfter(
                    previous,
                    next,
                    CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS
            );
            previous = next;
        }
    }
}
