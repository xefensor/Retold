package cz.xefensor.retold.aender.stability;

import cz.xefensor.retold.aender.RetoldAenderDimensions;
import cz.xefensor.retold.aender.generation.AenderVolatility;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;

public final class AenderChunkEvents {
    private AenderChunkEvents() {
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        if (level.dimension() != RetoldAenderDimensions.AENDER) {
            return;
        }

        ChunkAccess chunk = event.getChunk();
        ChunkPos pos = chunk.getPos();

        /*
         * Stable chunks are adopted into the current reality without replacing
         * their blocks. The signature is persisted with the chunk, so save/reload
         * does not turn missing runtime state into destructive regeneration.
         */
        AenderVolatility.retainForChunk(chunk);

        if (AenderStabilityData.get(level).isStable(pos)) {
            AenderVolatility.markGenerated(chunk);
            return;
        }

        if (AenderVolatility.needsRegeneration(chunk)) {
            /*
             * Never rebuild terrain synchronously from a chunk-load callback. A burst of
             * stale loads can otherwise put several complete chunk generations into one
             * server tick. Blank the old reality immediately, then let the adaptive queue
             * rebuild it without exposing an old/new terrain wall to clients.
             */
            AenderRealityTickEvents.blankLoadedStaleChunk(level, chunk);
        }
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        if (level.dimension() != RetoldAenderDimensions.AENDER) {
            return;
        }

        AenderVolatility.releaseForChunk(event.getChunk());
    }
}
