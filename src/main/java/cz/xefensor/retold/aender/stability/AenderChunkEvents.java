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

        if (AenderStabilityData.get(level).isStable(pos)) {
            return;
        }

        AenderVolatility.retainForChunk(chunk);

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

        ChunkAccess chunk = event.getChunk();

        AenderVolatility.releaseForChunk(chunk);

        int blockX = chunk.getPos().getMinBlockX();
        int blockZ = chunk.getPos().getMinBlockZ();

        int regionX = AenderVolatility.regionXForBlock(blockX);
        int regionZ = AenderVolatility.regionZForBlock(blockZ);
    }
}