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

        ChunkAccess chunk = level.getChunk(pos);

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

        ChunkAccess chunk = level.getChunk(pos);

        RetoldAenderChunkData oldData =
                chunk.getData(RetoldAenderAttachments.CHUNK_DATA.get());

        RetoldAenderChunkData newData = oldData.withAddedStabilizer();

        if (newData == oldData) {
            return;
        }

        chunk.setData(RetoldAenderAttachments.CHUNK_DATA.get(), newData);

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

        ChunkAccess chunk = level.getChunk(pos);

        RetoldAenderChunkData oldData =
                chunk.getData(RetoldAenderAttachments.CHUNK_DATA.get());

        RetoldAenderChunkData newData = oldData.withRemovedStabilizer();

        if (newData == oldData) {
            return;
        }

        chunk.setData(RetoldAenderAttachments.CHUNK_DATA.get(), newData);

        Retold.LOGGER.debug(
                "Aender chunk [{}, {}] stabilizer count changed to {}",
                chunk.getPos().x(),
                chunk.getPos().z(),
                newData.stabilizerCount()
        );
    }

    public static void scheduleRegenerationOnNextLoad(
            ServerLevel level,
            ChunkAccess chunk,
            long salt,
            int targetRegionVersion
    ) {
        if (!isAender(level)) {
            return;
        }

        RetoldAenderChunkData oldData =
                chunk.getData(RetoldAenderAttachments.CHUNK_DATA.get());

        if (oldData.isStabilized()) {
            return;
        }

        RetoldAenderChunkData newData =
                oldData.withRegenerationScheduled(
                        salt,
                        targetRegionVersion
                );

        if (newData == oldData) {
            return;
        }

        chunk.setData(RetoldAenderAttachments.CHUNK_DATA.get(), newData);

        Retold.LOGGER.debug(
                "Scheduled Aender chunk [{}, {}] for region version {}",
                chunk.getPos().x(),
                chunk.getPos().z(),
                targetRegionVersion
        );
    }

    public static void markRegenerationFinished(
            ServerLevel level,
            ChunkAccess chunk
    ) {
        if (!isAender(level)) {
            return;
        }

        RetoldAenderChunkData oldData =
                chunk.getData(RetoldAenderAttachments.CHUNK_DATA.get());

        RetoldAenderChunkData newData =
                oldData.withRegenerationFinished();

        if (newData == oldData) {
            return;
        }

        chunk.setData(RetoldAenderAttachments.CHUNK_DATA.get(), newData);
    }
}