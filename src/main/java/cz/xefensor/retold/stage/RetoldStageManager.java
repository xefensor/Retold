package cz.xefensor.retold.stage;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import cz.xefensor.retold.network.RetoldStageSyncPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

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
            playStage2TransitionSound(server);
        }
    }

    private static void playStage2TransitionSound(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!(player.level() instanceof ServerLevel level)) {
                continue;
            }

            level.playSound(
                    null,
                    player.blockPosition(),
                    SoundEvents.END_PORTAL_SPAWN,
                    SoundSource.MASTER,
                    1.2F,
                    0.55F
            );

            level.playSound(
                    null,
                    player.blockPosition(),
                    SoundEvents.WITHER_SPAWN,
                    SoundSource.MASTER,
                    0.5F,
                    0.65F
            );
        }
    }
}