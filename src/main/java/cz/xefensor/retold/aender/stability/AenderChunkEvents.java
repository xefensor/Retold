package cz.xefensor.retold.aender.stability;

import cz.xefensor.retold.aender.RetoldAenderDimensions;
import cz.xefensor.retold.aender.generation.AenderChunkGenerator;
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
         * Important:
         * Even stable chunks must be retained and marked as current.
         *
         * Otherwise this happens:
         * 1. Place stabilizer.
         * 2. Save and quit.
         * 3. Load world again.
         * 4. Stable chunk loads, but gets no AenderVolatility signature.
         * 5. Break stabilizer.
         * 6. Chunk becomes unstable.
         * 7. needsRegeneration() sees no signature and instantly regenerates it.
         */
        AenderVolatility.retainForChunk(chunk);

        if (AenderStabilityData.get(level).isStable(pos)) {
            AenderVolatility.markGenerated(chunk);
            return;
        }

        if (AenderVolatility.needsRegeneration(chunk)) {
            System.out.println("[Aender] regenerating loaded unstable chunk " + chunk.getPos());
            AenderChunkGenerator.regenerateLoadedChunk(chunk);
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
