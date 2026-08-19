package cz.xefensor.retold.network;

import cz.xefensor.retold.client.RetoldTeachingPreviewClient;
import cz.xefensor.retold.client.enchanting.RetoldClientEnchantmentCatalog;
import cz.xefensor.retold.client.enchanting.RetoldClientEnchantmentKnowledge;
import cz.xefensor.retold.client.enchanting.RetoldEnchantingScreenFeedback;
import cz.xefensor.retold.client.recipe.RetoldClientRecipeKnowledge;
import cz.xefensor.retold.client.render.RetoldChronolithBeamClient;
import cz.xefensor.retold.client.render.RetoldHorizonAmbientClient;
import cz.xefensor.retold.client.stage.RetoldClientStage;
import cz.xefensor.retold.enchanting.RetoldEnchantingMenuActions;
import cz.xefensor.retold.stage.RetoldWorldStage;
import cz.xefensor.retold.villager.RetoldVillagerTeaching;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class RetoldNetworking {
    private RetoldNetworking() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("2");

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

        registrar.playToServer(
                RetoldRequestTeachingPreviewPayload.TYPE,
                RetoldRequestTeachingPreviewPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player) {
                        RetoldVillagerTeaching.sendPreviewToClient(player);
                    }
                }
        );

        registrar.playToServer(
                RetoldEnchantingCastPayload.TYPE,
                RetoldEnchantingCastPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player) {
                        boolean succeeded = RetoldEnchantingMenuActions.tryCast(
                                player,
                                payload.containerId(),
                                payload.word(),
                                payload.level()
                        );
                        PacketDistributor.sendToPlayer(
                                player,
                                new RetoldEnchantingCastResultPayload(
                                        payload.containerId(),
                                        succeeded
                                )
                        );
                    }
                }
        );

        registrar.playToClient(
                RetoldEnchantingCastResultPayload.TYPE,
                RetoldEnchantingCastResultPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (Minecraft.getInstance().gui.screen()
                            instanceof RetoldEnchantingScreenFeedback feedback) {
                        feedback.retold$castFinished(
                                payload.containerId(),
                                payload.success()
                        );
                    }
                }
        );

        registrar.playToClient(
                RetoldEnchantmentCatalogSyncPayload.TYPE,
                RetoldEnchantmentCatalogSyncPayload.STREAM_CODEC,
                (payload, context) -> RetoldClientEnchantmentCatalog.replace(
                        payload.definitions()
                )
        );

        registrar.playToClient(
                RetoldEnchantmentKnowledgeSyncPayload.TYPE,
                RetoldEnchantmentKnowledgeSyncPayload.STREAM_CODEC,
                (payload, context) -> RetoldClientEnchantmentKnowledge.replace(
                        payload.knownEnchantments()
                )
        );

        registrar.playToClient(
                RetoldRecipeKnowledgeSyncPayload.TYPE,
                RetoldRecipeKnowledgeSyncPayload.STREAM_CODEC,
                (payload, context) -> RetoldClientRecipeKnowledge.replace(
                        payload.knownRecipes()
                )
        );

        registrar.playToClient(
                RetoldChronolithBeamPayload.TYPE,
                RetoldChronolithBeamPayload.STREAM_CODEC,
                (payload, context) -> RetoldChronolithBeamClient.handleSync(payload)
        );

        registrar.playToClient(
                RetoldHorizonCuePayload.TYPE,
                RetoldHorizonCuePayload.STREAM_CODEC,
                (payload, context) -> RetoldHorizonAmbientClient.handleCue(payload)
        );

        registrar.playToClient(
                RetoldTeachingPreviewPayload.TYPE,
                RetoldTeachingPreviewPayload.STREAM_CODEC,
                (payload, context) -> {
                    RetoldTeachingPreviewClient.set(
                            payload.active(),
                            payload.buttonLabel(),
                            payload.status(),
                            payload.cost(),
                            payload.tooltip(),
                            payload.feedback()
                    );
                }
        );
    }
}
