package cz.xefensor.retold;

import com.mojang.logging.LogUtils;
import cz.xefensor.retold.aender.stability.AenderWorldTickEvents;
import cz.xefensor.retold.client.RetoldClientEvents;
import cz.xefensor.retold.event.*;
import cz.xefensor.retold.network.RetoldNetworking;
import cz.xefensor.retold.recipe.RetoldRecipeBookEvents;
import cz.xefensor.retold.villager.RetoldVillagerTeachingReloadListener;
import cz.xefensor.retold.worldgen.RetoldWorldSpawnCache;
import cz.xefensor.retold.worldgen.RetoldWorldgenRegistries;
import cz.xefensor.retold.worldgen.delayed.*;
import cz.xefensor.retold.registry.RetoldBlocks;
import cz.xefensor.retold.aender.RetoldAenderRegistries;
import cz.xefensor.retold.aender.stability.AenderChunkEvents;
import cz.xefensor.retold.aender.stability.AenderStabilizerEvents;
import cz.xefensor.retold.registry.RetoldBlocks;
import cz.xefensor.retold.event.RetoldFactionTerritoryEvents;
import cz.xefensor.retold.event.RetoldFactionCombatEvents;
import cz.xefensor.retold.event.RetoldFactionAssistEvents;
import cz.xefensor.retold.aender.stability.AenderRealityTickEvents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import org.slf4j.Logger;


@Mod(Retold.MODID)
public final class Retold {
    public static final String MODID = "retold";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Retold(IEventBus modEventBus) {
        RetoldBlocks.register(modEventBus);
        RetoldWorldgenRegistries.register(modEventBus);
        RetoldAttachments.register(modEventBus);
        RetoldAenderRegistries.register(modEventBus);

        modEventBus.addListener(RetoldNetworking::registerPayloads);

        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            RetoldClientEvents.register(modEventBus);
        }

        NeoForge.EVENT_BUS.register(RetoldCommandEvents.class);
        NeoForge.EVENT_BUS.register(RetoldStageRuntimeEvents.class);
        NeoForge.EVENT_BUS.register(RetoldUndeadEvents.class);
        NeoForge.EVENT_BUS.register(RetoldPiglinEvents.class);
        NeoForge.EVENT_BUS.register(RetoldEndProgressionEvents.class);
        NeoForge.EVENT_BUS.register(RetoldGolemEvents.class);
        NeoForge.EVENT_BUS.register(RetoldEndermanEvents.class);
        NeoForge.EVENT_BUS.register(RetoldPlayerSyncEvents.class);
        NeoForge.EVENT_BUS.register(RetoldRecipeBookEvents.class);
        NeoForge.EVENT_BUS.register(RetoldWorldSpawnCache.class);

        NeoForge.EVENT_BUS.register(RetoldChunkEditEvents.class);
        NeoForge.EVENT_BUS.register(RetoldDelayedStructureRetrogen.class);
        NeoForge.EVENT_BUS.register(RetoldDelayedStructureMobBlocker.class);
        NeoForge.EVENT_BUS.register(RetoldClientChunkTracker.class);
        NeoForge.EVENT_BUS.register(RetoldRetrogenDropBlocker.class);
        NeoForge.EVENT_BUS.register(RetoldPatrolStageEvents.class);
        NeoForge.EVENT_BUS.register(AenderChunkEvents.class);
        NeoForge.EVENT_BUS.register(AenderStabilizerEvents.class);
        NeoForge.EVENT_BUS.register(AenderWorldTickEvents.class);
        NeoForge.EVENT_BUS.register(AenderRealityTickEvents.class);
        NeoForge.EVENT_BUS.register(RetoldFactionCombatEvents.class);
        NeoForge.EVENT_BUS.register(RetoldFactionTerritoryEvents.class);
        NeoForge.EVENT_BUS.register(RetoldFactionAssistEvents.class);

        NeoForge.EVENT_BUS.addListener(this::addServerReloadListeners);
    }

    private void addServerReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(
                RetoldVillagerTeachingReloadListener.ID,
                new RetoldVillagerTeachingReloadListener()
        );
    }
}