package cz.xefensor.retold.event;

import cz.xefensor.retold.command.RetoldCommands;
import cz.xefensor.retold.command.showcase.RetoldShowcaseEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.MobDespawnEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public final class RetoldCommandEvents {
    private RetoldCommandEvents() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        RetoldCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onShowcaseEntityTickPre(EntityTickEvent.Pre event) {
        Entity entity = event.getEntity();
        if (entity.level() instanceof ServerLevel
                && entity.entityTags().contains(RetoldShowcaseEntities.EXHIBIT_TAG)) {
            // NoAI does not stop Gale's direct flight, Guardian encounter ticks, or Retold Post
            // dispatchers. Only these explicit developer exhibits skip their server tick and Post.
            entity.setDeltaMovement(Vec3.ZERO);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onShowcaseMobDespawn(MobDespawnEvent event) {
        if (event.getEntity().entityTags().contains(RetoldShowcaseEntities.EXHIBIT_TAG)) {
            // checkDespawn runs before Pre, including the Peaceful-mode hostile removal.
            event.setResult(MobDespawnEvent.Result.DENY);
        }
    }
}
