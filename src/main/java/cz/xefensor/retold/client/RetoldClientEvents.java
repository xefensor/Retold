package cz.xefensor.retold.client;

import cz.xefensor.retold.client.render.RetoldEndermanEyesLayer;
import net.minecraft.client.renderer.entity.EndermanRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public final class RetoldClientEvents {
    private RetoldClientEvents() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(RetoldClientEvents::addEntityRenderLayers);
    }

    private static void addEntityRenderLayers(EntityRenderersEvent.AddLayers event) {
        EntityRenderer<?, ?> renderer = event.getRenderer(EntityTypes.ENDERMAN);

        if (renderer instanceof EndermanRenderer endermanRenderer) {
            endermanRenderer.addLayer(new RetoldEndermanEyesLayer(endermanRenderer));
        }
    }
}