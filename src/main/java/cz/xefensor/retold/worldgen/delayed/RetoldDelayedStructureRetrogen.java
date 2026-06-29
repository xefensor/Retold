package cz.xefensor.retold.worldgen.delayed;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.stage.RetoldStageRuntime;
import cz.xefensor.retold.stage.RetoldWorldStage;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

public final class RetoldDelayedStructureRetrogen {
    private static final Queue<ChunkPos> QUEUE = new ArrayDeque<>();
    private static final Set<Long> QUEUED = new HashSet<>();

    private static final int CHUNKS_PER_TICK = 2;

    private RetoldDelayedStructureRetrogen() {
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        if (level.dimension() != Level.OVERWORLD) {
            return;
        }

        if (!RetoldStageRuntime.isAtLeast(RetoldWorldStage.STAGE_2)) {
            return;
        }

        ChunkAccess chunk = event.getChunk();

        RetoldChunkStructureData data =
                chunk.getData(RetoldAttachments.CHUNK_STRUCTURE_DATA.get());

        if (!data.hasAnyDeferredStructures()) {
            return;
        }

        enqueue(chunk.getPos());
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ServerLevel overworld = event.getServer().getLevel(Level.OVERWORLD);

        if (overworld == null) {
            return;
        }

        if (!RetoldStageRuntime.isAtLeast(RetoldWorldStage.STAGE_2)) {
            return;
        }

        for (int i = 0; i < CHUNKS_PER_TICK && !QUEUE.isEmpty(); i++) {
            ChunkPos pos = QUEUE.poll();
            QUEUED.remove(pos.getWorldPosition().asLong());

            processChunk(overworld, pos);
        }
    }

    private static void enqueue(ChunkPos pos) {
        long key = pos.getWorldPosition().asLong();

        if (QUEUED.add(key)) {
            QUEUE.add(pos);
        }
    }

    private static void processChunk(ServerLevel level, ChunkPos pos) {
        if (!level.hasChunk(pos.x(), pos.z())) {
            return;
        }

        ChunkAccess chunk = level.getChunk(pos.x(), pos.z());

        RetoldChunkStructureData data =
                chunk.getData(RetoldAttachments.CHUNK_STRUCTURE_DATA.get());

        if (!data.hasAnyDeferredStructures()) {
            return;
        }

        RetoldChunkStructureData newData = data;

        if (data.isEditedByPlayer()) {
            for (String structureId : data.deferredStructures()) {
                newData = newData.withChecked(structureId);
                newData = newData.withoutDeferred(structureId);

                Retold.LOGGER.info(
                        "Skipped deferred structure {} at chunk [{}, {}] because chunk is edited",
                        structureId,
                        pos.x(),
                        pos.z()
                );
            }

            chunk.setData(RetoldAttachments.CHUNK_STRUCTURE_DATA.get(), newData);
            return;
        }

        for (String structureId : data.deferredStructures()) {
            if (newData.hasChecked(structureId)) {
                newData = newData.withoutDeferred(structureId);
                continue;
            }

            RetrogenResult result = tryRetrogenStructure(level, pos, structureId);

            if (result == RetrogenResult.SUCCESS || result == RetrogenResult.PERMANENT_SKIP) {
                newData = newData.withChecked(structureId);
                newData = newData.withoutDeferred(structureId);
            }
        }

        if (newData != data) {
            chunk.setData(RetoldAttachments.CHUNK_STRUCTURE_DATA.get(), newData);
        }
    }

    private static RetrogenResult tryRetrogenStructure(
            ServerLevel level,
            ChunkPos pos,
            String structureId
    ) {
        // Next phase:
        // Actually place/generate the structure here.
        //
        // For now, keep the deferred candidate saved by returning TRY_LATER.
        // This prevents your test world from permanently losing candidates.

        Retold.LOGGER.info(
                "Deferred structure {} is waiting for real placement at chunk [{}, {}]",
                structureId,
                pos.x(),
                pos.z()
        );

        return RetrogenResult.TRY_LATER;
    }

    private enum RetrogenResult {
        SUCCESS,
        PERMANENT_SKIP,
        TRY_LATER
    }
}