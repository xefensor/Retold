package cz.xefensor.retold.aender.stability;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.aender.RetoldAenderDimensions;
import cz.xefensor.retold.aender.generation.AenderVolatility;
import cz.xefensor.retold.aender.portal.AenderPortalWarmup;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import java.util.List;

public final class AenderWorldTickEvents {
    private static boolean hadPlayers = false;

    private AenderWorldTickEvents() {
    }

    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level
                && level.dimension() == RetoldAenderDimensions.AENDER) {
            AenderVolatility.initializeReality(level);
        }
    }

    @SubscribeEvent
    public static void onCreateSpawnPosition(LevelEvent.CreateSpawnPosition event) {
        if (event.getLevel() instanceof ServerLevel level
                && level.dimension() == Level.OVERWORLD) {
            /*
             * This event only fires while a brand-new save chooses its first
             * spawn. Worlds from older Retold releases never receive it, so a
             * missing Aender reality record safely defaults to generator V1.
             */
            AenderVolatility.enableCurrentGeneratorForFreshWorld(level);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        if (level.dimension() != RetoldAenderDimensions.AENDER) {
            return;
        }

        if (!level.players().isEmpty()) {
            hadPlayers = true;
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getTo() == RetoldAenderDimensions.AENDER) {
            if (event.getEntity() instanceof ServerPlayer player) {
                /*
                 * Portal travel normally prepares this bounded core before the
                 * transition. Doing the same here covers commands and other mods
                 * that place a player directly into the Aender.
                 */
                AenderRealityTickEvents.prepareArrivalCore(player.level(), player.blockPosition());
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

        resetVolatileTerrain(aender);
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        AenderPortalWarmup.clear();
        AenderRealityTickEvents.clearPendingRegeneration();
        AenderVolatility.clearRuntime();
        hadPlayers = false;
    }

    private static void resetVolatileTerrain(ServerLevel level) {
        List<ChunkPos> loadedChunks = AenderVolatility.retainedChunkPositions();
        AenderPortalWarmup.clear();
        AenderVolatility.advanceReality(level);
        AenderRealityTickEvents.beginRealityTransition(level, loadedChunks);
        hadPlayers = false;
        Retold.LOGGER.debug("Cleared volatile Aender terrain after the last player travelled out");
    }
}
