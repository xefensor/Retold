package cz.xefensor.retold.network;

import cz.xefensor.retold.client.stage.RetoldClientStage;
import cz.xefensor.retold.stage.RetoldWorldStage;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import cz.xefensor.retold.villager.RetoldVillagerTeaching;
import net.minecraft.server.level.ServerPlayer;

public final class RetoldNetworking {
    private RetoldNetworking() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToClient(
                RetoldStageSyncPayload.TYPE,
                RetoldStageSyncPayload.STREAM_CODEC,
                (payload, context) -> {
                    RetoldWorldStage stage = RetoldWorldStage.getStageFromId(payload.stageId());
                    RetoldClientStage.setStage(stage);
                }
        );

        registrar.playToClient(
                RetoldEndSkySeedSyncPayload.TYPE,
                RetoldEndSkySeedSyncPayload.STREAM_CODEC,
                (payload, context) -> {
                    cz.xefensor.retold.client.sky.RetoldClientEndSky.setSeed(payload.seed());
                }
        );

        registrar.playToServer(
                RetoldLearnRecipePayload.TYPE,
                RetoldLearnRecipePayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player) {
                        RetoldVillagerTeaching.tryTeachHeldItemRecipe(player);
                    }
                }
        );
    }
}