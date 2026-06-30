package cz.xefensor.retold.event;

import cz.xefensor.retold.network.RetoldEndSkySeedSyncPayload;
import cz.xefensor.retold.network.RetoldStageSyncPayload;
import cz.xefensor.retold.sky.RetoldEndSkyData;
import cz.xefensor.retold.stage.RetoldWorldData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class RetoldPlayerSyncEvents {
    private RetoldPlayerSyncEvents() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        ServerLevel level = (ServerLevel) serverPlayer.level();

        RetoldWorldData worldData = RetoldWorldData.get(level);
        RetoldEndSkyData endSkyData = RetoldEndSkyData.get(level);

        PacketDistributor.sendToPlayer(
                serverPlayer,
                new RetoldStageSyncPayload(worldData.getStage().getId())
        );

        PacketDistributor.sendToPlayer(
                serverPlayer,
                new RetoldEndSkySeedSyncPayload(endSkyData.getSeed())
        );
    }
}