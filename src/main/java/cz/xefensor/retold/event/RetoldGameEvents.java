package cz.xefensor.retold.event;

import cz.xefensor.retold.command.RetoldCommands;
import cz.xefensor.retold.stage.RetoldWorldData;
import cz.xefensor.retold.stage.RetoldWorldStage;
import cz.xefensor.retold.undead.RetoldUndead;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class RetoldGameEvents {
    private RetoldGameEvents() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        RetoldCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();

        if (entity.level().isClientSide()) {
            return;
        }

        if (!RetoldUndead.isUndead(entity)) {
            return;
        }

        if (!(entity.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        RetoldWorldStage stage = RetoldWorldData.get(serverLevel).getStage();

        if (stage == RetoldWorldStage.STAGE_2) {
            entity.clearFire();
        }
    }

    @SubscribeEvent
    public static void onServerTickPost(ServerTickEvent.Post event) {
        ServerLevel endLevel = event.getServer().getLevel(Level.END);

        if (endLevel == null) {
            return;
        }

        EnderDragonFight dragonFight = endLevel.getDragonFight();

        if (dragonFight == null) {
            return;
        }

        if (!dragonFight.hasPreviouslyKilledDragon()) {
            return;
        }

        RetoldWorldData data = RetoldWorldData.get(endLevel);

        if (data.getStage() != RetoldWorldStage.STAGE_1) {
            return;
        }

        data.setStage(RetoldWorldStage.STAGE_2);

        event.getServer().getPlayerList().broadcastSystemMessage(
                Component.literal("The Ender Dragon has fallen. The world has entered Stage 2."),
                false
        );
    }
}