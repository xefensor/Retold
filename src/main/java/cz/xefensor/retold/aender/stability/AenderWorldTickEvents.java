package cz.xefensor.retold.aender.stability;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.aender.RetoldAenderDimensions;
import cz.xefensor.retold.aender.generation.AenderVolatility;
import cz.xefensor.retold.aender.portal.AenderPortalWarmup;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

public final class AenderWorldTickEvents {
    private static boolean hadPlayers = false;

    private AenderWorldTickEvents() {
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        if (level.dimension() != RetoldAenderDimensions.AENDER) {
            return;
        }

        boolean hasPlayers = !level.players().isEmpty();

        if (hadPlayers && !hasPlayers) {
            resetVolatileTerrain();
        }

        hadPlayers = hasPlayers;
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getTo() == RetoldAenderDimensions.AENDER) {
            if (event.getEntity() instanceof ServerPlayer player) {
                /*
                 * Portal travel normally prepares this view before the transition.
                 * Keeping the same synchronous preparation here also covers commands
                 * and other mods that place a player directly into the Aender.
                 */
                AenderRealityTickEvents.prepareArrivalView(player.level(), player.blockPosition());
            }

            hadPlayers = true;
            return;
        }

        if (event.getFrom() != RetoldAenderDimensions.AENDER) {
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ServerLevel aender = player.level().getServer().getLevel(RetoldAenderDimensions.AENDER);

        if (!hadPlayers || aender == null || !aender.players().isEmpty()) {
            return;
        }

        resetVolatileTerrain();
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        AenderPortalWarmup.clear();
        AenderRealityTickEvents.clearPendingRegeneration();
        AenderVolatility.clearForgottenWorld();
        hadPlayers = false;
    }

    private static void resetVolatileTerrain() {
        AenderPortalWarmup.clear();
        AenderRealityTickEvents.clearPendingRegeneration();
        AenderVolatility.clearForgottenWorld();
        hadPlayers = false;
        Retold.LOGGER.debug("Cleared volatile Aender terrain because the dimension became empty");
    }
}
