package cz.xefensor.retold.aender;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.stage.RetoldWorldData;
import cz.xefensor.retold.stage.RetoldWorldStage;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class RetoldAenderAccess {
    private static final Vec3 AENDER_ENTRY_POSITION =
            RetoldAenderEntryPlatform.ENTRY_POSITION;

    private RetoldAenderAccess() {
    }

    public static void ejectPlayersFromVanillaEnd(MinecraftServer server) {
        ServerLevel endLevel = server.getLevel(Level.END);

        if (endLevel == null) {
            return;
        }

        for (ServerPlayer player : List.copyOf(endLevel.players())) {
            ejectPlayerToSpawn(player);
        }
    }

    public static TeleportTransition getAenderPortalDestination(
            ServerLevel sourceLevel,
            Entity entity
    ) {
        if (sourceLevel.dimension() != Level.OVERWORLD) {
            return null;
        }

        if (!(entity instanceof ServerPlayer)) {
            return null;
        }

        RetoldWorldStage stage =
                RetoldWorldData.get(sourceLevel).getStage();

        if (!(stage.getId() >= RetoldWorldStage.STAGE_3.getId())) {
            return null;
        }

        ServerLevel aenderLevel =
                sourceLevel.getServer().getLevel(RetoldAenderDimensions.AENDER);

        if (aenderLevel == null) {
            Retold.LOGGER.warn(
                    "Could not redirect End portal to Aender because retold:aender is not loaded"
            );

            return null;
        }

        RetoldAenderEntryPlatform.createInLevel(aenderLevel);

        return new TeleportTransition(
                aenderLevel,
                AENDER_ENTRY_POSITION,
                Vec3.ZERO,
                entity.getYRot(),
                entity.getXRot(),
                TeleportTransition.PLAY_PORTAL_SOUND
        );
    }

    private static void ejectPlayerToSpawn(ServerPlayer player) {
        TeleportTransition transition =
                player.findRespawnPositionAndUseSpawnBlock(
                        false,
                        TeleportTransition.DO_NOTHING
                );

        player.teleport(transition);

        player.sendSystemMessage(
                Component.literal("The End is no longer")
        );
    }
}
