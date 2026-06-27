package cz.xefensor.retold.stage;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import cz.xefensor.retold.network.RetoldStageSyncPayload;
import net.neoforged.neoforge.network.PacketDistributor;

public final class RetoldStageManager {
    private RetoldStageManager() {
    }

    public static boolean setStage(ServerLevel level, RetoldWorldStage newStage) {
        RetoldWorldData data = RetoldWorldData.get(level);
        RetoldWorldStage oldStage = data.getStage();

        if (oldStage == newStage) {
            return false;
        }

        data.setStage(newStage);

        PacketDistributor.sendToAllPlayers(
                new RetoldStageSyncPayload(newStage.getId())
        );

        onStageChanged(level.getServer(), oldStage, newStage);

        return true;
    }

    private static void onStageChanged(
            MinecraftServer server,
            RetoldWorldStage oldStage,
            RetoldWorldStage newStage
    ) {
        if (newStage == RetoldWorldStage.STAGE_2) {
            server.getPlayerList().broadcastSystemMessage(
                    Component.literal("The Ender Dragon has fallen. The world has entered Stage 2."),
                    false
            );
        }

        if (newStage == RetoldWorldStage.STAGE_3) {
            server.getPlayerList().broadcastSystemMessage(
                    Component.literal("The world has been restored. The undead begin to fade."),
                    false
            );
        }
    }
}