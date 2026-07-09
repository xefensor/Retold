package cz.xefensor.retold.client;

import cz.xefensor.retold.client.render.RetoldEndermanEyesLayer;
import cz.xefensor.retold.client.render.RetoldChronolithBeamClient;
import cz.xefensor.retold.client.sky.RetoldClientEndSky;
import cz.xefensor.retold.client.sky.RetoldEndSkyPatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EndermanRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class RetoldClientEvents {
    private RetoldClientEvents() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(RetoldClientEvents::addEntityRenderLayers);
        RetoldChronolithBeamClient.register(modEventBus);
        NeoForge.EVENT_BUS.addListener(RetoldClientEvents::onClientTick);
    }

    private static void addEntityRenderLayers(EntityRenderersEvent.AddLayers event) {
        EntityRenderer renderer = event.getRenderer(EntityTypes.ENDERMAN);

        if (renderer instanceof EndermanRenderer endermanRenderer) {
            endermanRenderer.addLayer(new RetoldEndermanEyesLayer(endermanRenderer));
        }
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null) {
            return;
        }

        if (minecraft.level.dimension() != Level.END) {
            return;
        }

        RetoldClientEndSky.getTexture();
        RetoldEndSkyPatcher.patchSkyRendererTexture(RetoldClientEndSky.getSeed());
    }
}
