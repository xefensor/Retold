package cz.xefensor.retold.event;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class RetoldReputationDebugEvents {
    private static final boolean DEBUG_REPUTATION = false;
    private static final int DEBUG_INTERVAL_TICKS = 10;

    private RetoldReputationDebugEvents() {
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

        RetoldIntruderReputation.tickDecay(gameTime);

        if (!DEBUG_REPUTATION) {
            return;
        }

        if (gameTime % DEBUG_INTERVAL_TICKS != 0L) {
            return;
        }

        RetoldTerritoryContext territoryContext = RetoldFactionTerritoryEvents.getTerritoryContextAt(
                level,
                player.blockPosition()
        );

        if (territoryContext != null) {
            String text = "Territory: "
                    + RetoldIntruderReputation.getDebugText(territoryContext, player, gameTime);

            player.sendSystemMessage(Component.literal(text), true);
            return;
        }

        String text = "Suspicion: "
                + RetoldIntruderReputation.getMostSuspiciousDebugText(player, gameTime);

        if (!text.endsWith("No reputation")) {
            player.sendSystemMessage(Component.literal(text), true);
        }
    }
}