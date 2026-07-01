package cz.xefensor.retold.aender;

import cz.xefensor.retold.Retold;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;

public final class RetoldAenderChunkStability {
    private RetoldAenderChunkStability() {
    }

    public static boolean isAender(ServerLevel level) {
        return level.dimension() == RetoldAenderDimensions.AENDER;
    }

    public static boolean isChunkStabilized(
            ServerLevel level,
            BlockPos pos
    ) {
        if (!isAender(level)) {
            return false;
        }

        ChunkAccess chunk =
                level.getChunk(pos);

        RetoldAenderChunkData data =
                chunk.getData(RetoldAenderAttachments.CHUNK_DATA.get());

        return data.isStabilized();
    }

    public static void addStabilizer(
            ServerLevel level,
            BlockPos pos
    ) {
        if (!isAender(level)) {
            return;
        }

        ChunkAccess chunk =
                level.getChunk(pos);

        RetoldAenderChunkData oldData =
                chunk.getData(RetoldAenderAttachments.CHUNK_DATA.get());

        RetoldAenderChunkData newData =
                oldData.withAddedStabilizer();

        if (newData == oldData) {
            return;
        }

        chunk.setData(
                RetoldAenderAttachments.CHUNK_DATA.get(),
                newData
        );

        Retold.LOGGER.debug(
                "Aender chunk [{}, {}] stabilizer count changed to {}",
                chunk.getPos().x(),
                chunk.getPos().z(),
                newData.stabilizerCount()
        );
    }

    public static void removeStabilizer(
            ServerLevel level,
            BlockPos pos
    ) {
        if (!isAender(level)) {
            return;
        }

        ChunkAccess chunk =
                level.getChunk(pos);

        RetoldAenderChunkData oldData =
                chunk.getData(RetoldAenderAttachments.CHUNK_DATA.get());

        RetoldAenderChunkData newData =
                oldData.withRemovedStabilizer();

        if (newData == oldData) {
            return;
        }

        chunk.setData(
                RetoldAenderAttachments.CHUNK_DATA.get(),
                newData
        );

        Retold.LOGGER.debug(
                "Aender chunk [{}, {}] stabilizer count changed to {}",
                chunk.getPos().x(),
                chunk.getPos().z(),
                newData.stabilizerCount()
        );
    }

    public static boolean scheduleTerrainRegeneration(
            ServerLevel level,
            ChunkAccess chunk,
            long targetRevision
    ) {
        if (!isAender(level)) {
            return false;
        }

        RetoldAenderChunkData oldData =
                chunk.getData(RetoldAenderAttachments.CHUNK_DATA.get());

        if (oldData.isStabilized()) {
            return false;
        }

        RetoldAenderChunkData newData =
                oldData.withTerrainRegenerationScheduled(targetRevision);

        if (newData == oldData) {
            return false;
        }

        chunk.setData(
                RetoldAenderAttachments.CHUNK_DATA.get(),
                newData
        );

        return true;
    }

    public static void markTerrainRegenerationFinished(
            ServerLevel level,
            ChunkAccess chunk
    ) {
        if (!isAender(level)) {
            return;
        }

        RetoldAenderChunkData oldData =
                chunk.getData(RetoldAenderAttachments.CHUNK_DATA.get());

        RetoldAenderChunkData newData =
                oldData.withTerrainRegenerationFinished();

        if (newData == oldData) {
            return;
        }

        chunk.setData(
                RetoldAenderAttachments.CHUNK_DATA.get(),
                newData
        );
    }
}