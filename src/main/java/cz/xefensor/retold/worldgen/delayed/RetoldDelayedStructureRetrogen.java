package cz.xefensor.retold.worldgen.delayed;

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

        enqueue(event.getChunk().getPos());
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

        if (data.isEditedByPlayer()) {
            markAllDelayedStructuresChecked(chunk, data);
            return;
        }

        RetoldChunkStructureData newData = data;

        for (String structureId : RetoldDelayedStructureIds.ALL) {
            if (newData.hasChecked(structureId)) {
                continue;
            }

            boolean generated = tryRetrogenStructure(level, pos, structureId);

            // Important:
            // Mark checked even if generated == false.
            // Otherwise this same chunk gets retried forever.
            newData = newData.withChecked(structureId);
        }

        if (newData != data) {
            chunk.setData(RetoldAttachments.CHUNK_STRUCTURE_DATA.get(), newData);
        }
    }

    private static void markAllDelayedStructuresChecked(
            ChunkAccess chunk,
            RetoldChunkStructureData data
    ) {
        RetoldChunkStructureData newData = data;

        for (String structureId : RetoldDelayedStructureIds.ALL) {
            if (!newData.hasChecked(structureId)) {
                newData = newData.withChecked(structureId);
            }
        }

        if (newData != data) {
            chunk.setData(RetoldAttachments.CHUNK_STRUCTURE_DATA.get(), newData);
        }
    }

    private static boolean tryRetrogenStructure(
            ServerLevel level,
            ChunkPos pos,
            String structureId
    ) {
        // Phase 8 only tracks that the chunk was checked.
        // Actual mansion/outpost placement comes later.

        cz.xefensor.retold.Retold.LOGGER.debug(
                "Retold delayed structure retrogen check: {} at chunk [{}, {}]",
                structureId,
                pos.x(),
                pos.z()
        );

        return false;
    }
}