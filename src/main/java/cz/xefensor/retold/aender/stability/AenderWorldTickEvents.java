package cz.xefensor.retold.aender.stability;

import cz.xefensor.retold.aender.RetoldAenderDimensions;
import cz.xefensor.retold.aender.generation.AenderVolatility;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

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
            AenderVolatility.clearForgottenWorld();
            System.out.println("[Aender] cleared volatile terrain because dimension is empty");
        }

        hadPlayers = hasPlayers;
    }
}