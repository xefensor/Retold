package cz.xefensor.retold.territory;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class RetoldTerritoryReputationDebugEvents {
    private RetoldTerritoryReputationDebugEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTickPost(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        long gameTime = level.getGameTime();

        RetoldTerritoryReputation.tickDecay(gameTime);

        if (!RetoldTerritoryConstants.DEBUG_REPUTATION) {
            return;
        }

        if (gameTime % RetoldTerritoryConstants.DEBUG_REPUTATION_INTERVAL_TICKS != 0L) {
            return;
        }

        RetoldTerritoryContext territoryContext = RetoldTerritoryDetector.getContextAt(
                level,
                player.blockPosition()
        );

        if (territoryContext != null) {
            String text = "Territory: "
                    + RetoldTerritoryReputation.getDebugText(
                    territoryContext,
                    player,
                    gameTime
            );

            player.sendSystemMessage(Component.literal(text), true);
            return;
        }

        String text = "Suspicion: "
                + RetoldTerritoryReputation.getMostSuspiciousDebugText(player, gameTime);

        if (!text.endsWith("No reputation")) {
            player.sendSystemMessage(Component.literal(text), true);
        }
    }
}