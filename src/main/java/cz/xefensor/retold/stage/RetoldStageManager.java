package cz.xefensor.retold.stage;

import cz.xefensor.retold.aender.RetoldAenderAccess;
import cz.xefensor.retold.network.RetoldStageSyncPayload;
import cz.xefensor.retold.worldgen.delayed.RetoldDelayedStructureRetrogen;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.PacketDistributor;

public final class RetoldStageManager {
    private RetoldStageManager() {
    }

    public static boolean setStage(
            ServerLevel level,
            RetoldWorldStage newStage
    ) {
        RetoldWorldData data = RetoldWorldData.get(level);
        RetoldWorldStage oldStage = data.getStage();

        if (oldStage == newStage) {
            return false;
        }

        data.setStage(newStage);
        RetoldStageRuntime.setOverworldStage(newStage);

        if (newStage.getId() >= RetoldWorldStage.STAGE_2.getId()) {
            RetoldDelayedStructureRetrogen.queueKnownDeferredChunksForStage(level);
        }

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

        if (oldStage != RetoldWorldStage.STAGE_3
                && newStage == RetoldWorldStage.STAGE_3) {
            RetoldAenderAccess.ejectPlayersFromVanillaEnd(server);
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