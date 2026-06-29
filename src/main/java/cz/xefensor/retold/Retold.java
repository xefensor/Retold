package cz.xefensor.retold;

import cz.xefensor.retold.worldgen.delayed.RetoldAttachments;
import cz.xefensor.retold.worldgen.delayed.RetoldChunkEditEvents;
import cz.xefensor.retold.worldgen.delayed.RetoldDelayedStructureRetrogen;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import cz.xefensor.retold.event.RetoldGameEvents;
import cz.xefensor.retold.network.RetoldNetworking;
import cz.xefensor.retold.client.RetoldClientEvents;
import net.neoforged.fml.loading.FMLEnvironment;
import cz.xefensor.retold.worldgen.RetoldWorldgenRegistries;
import cz.xefensor.retold.recipe.RetoldRecipeBookEvents;
import cz.xefensor.retold.worldgen.RetoldWorldSpawnCache;
import cz.xefensor.retold.villager.RetoldVillagerTeachingReloadListener;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import cz.xefensor.retold.worldgen.delayed.RetoldDelayedStructureMobBlocker;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(Retold.MODID)
public class Retold {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "retold";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under the "retold" namespace
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    // Create a Deferred Register to hold Items which will all be registered under the "retold" namespace
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "retold" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public Retold(IEventBus modEventBus, ModContainer modContainer) {

        RetoldWorldgenRegistries.register(modEventBus);
        RetoldAttachments.register(modEventBus);
        modEventBus.addListener(RetoldNetworking::registerPayloads);
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so tabs get registered
        CREATIVE_MODE_TABS.register(modEventBus);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (Retold) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            RetoldClientEvents.register(modEventBus);
        }
        NeoForge.EVENT_BUS.register(RetoldGameEvents.class);
        NeoForge.EVENT_BUS.register(RetoldRecipeBookEvents.class);
        NeoForge.EVENT_BUS.register(RetoldWorldSpawnCache.class);
        NeoForge.EVENT_BUS.addListener(this::addServerReloadListeners);
        NeoForge.EVENT_BUS.register(RetoldChunkEditEvents.class);
        NeoForge.EVENT_BUS.register(RetoldDelayedStructureRetrogen.class);
        NeoForge.EVENT_BUS.register(RetoldDelayedStructureMobBlocker.class);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");

        if (Config.LOG_DIRT_BLOCK.getAsBoolean()) {
            LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));
        }

        LOGGER.info("{}{}", Config.MAGIC_NUMBER_INTRODUCTION.get(), Config.MAGIC_NUMBER.getAsInt());

        Config.ITEM_STRINGS.get().forEach((item) -> LOGGER.info("ITEM >> {}", item));
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

    private void addServerReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(
                RetoldVillagerTeachingReloadListener.ID,
                new RetoldVillagerTeachingReloadListener()
        );
    }
}
