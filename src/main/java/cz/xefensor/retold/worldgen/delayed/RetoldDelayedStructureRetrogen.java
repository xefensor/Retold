package cz.xefensor.retold.worldgen.delayed;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.stage.RetoldStageRuntime;
import cz.xefensor.retold.stage.RetoldWorldStage;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.List;

public final class RetoldDelayedStructureRetrogen {
    private static final Queue<ChunkPos> QUEUE = new ArrayDeque<>();
    private static final Set<Long> QUEUED = new HashSet<>();

    private static final int CHUNKS_PER_TICK = 2;

    private static final int PLAYER_SCAN_CHUNK_RADIUS = 12;
    private static final int PLAYER_SCAN_INTERVAL_TICKS = 40;

    private static int ticksUntilPlayerScan = 0;

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

        ticksUntilPlayerScan--;

        if (ticksUntilPlayerScan <= 0) {
            ticksUntilPlayerScan = PLAYER_SCAN_INTERVAL_TICKS;
            enqueueDeferredChunksAroundPlayers(overworld);
        }

        for (int i = 0; i < CHUNKS_PER_TICK && !QUEUE.isEmpty(); i++) {
            ChunkPos pos = QUEUE.poll();
            QUEUED.remove(pos.getWorldPosition().asLong());

            processChunk(overworld, pos);
        }
    }

    public static void enqueueDeferredChunksAroundPlayers(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            ChunkPos playerChunk = player.chunkPosition();

            for (int dx = -PLAYER_SCAN_CHUNK_RADIUS; dx <= PLAYER_SCAN_CHUNK_RADIUS; dx++) {
                for (int dz = -PLAYER_SCAN_CHUNK_RADIUS; dz <= PLAYER_SCAN_CHUNK_RADIUS; dz++) {
                    int chunkX = playerChunk.x() + dx;
                    int chunkZ = playerChunk.z() + dz;

                    if (!level.hasChunk(chunkX, chunkZ)) {
                        continue;
                    }

                    ChunkAccess chunk = level.getChunk(chunkX, chunkZ);

                    RetoldChunkStructureData data =
                            chunk.getData(RetoldAttachments.CHUNK_STRUCTURE_DATA.get());

                    if (!data.hasAnyDeferredStructures()) {
                        continue;
                    }

                    enqueue(chunk.getPos());
                }
            }
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
            for (String structureId : Set.copyOf(data.deferredStructures())) {
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

        for (String structureId : Set.copyOf(data.deferredStructures())) {
            if (!RetoldStageRuntime.isAtLeast(RetoldDelayedStructureIds.requiredStage(structureId))) {
                continue;
            }

            if (newData.hasChecked(structureId)) {
                newData = newData.withoutDeferred(structureId);
                continue;
            }

            RetrogenResult result = tryRetrogenStructure(level, pos, structureId);

            if (result == RetrogenResult.SUCCESS) {
                RetoldDelayedStructureMobBlocker.forgetDeferredStructure(structureId, pos);

                newData = newData.withChecked(structureId);
                newData = newData.withoutDeferred(structureId);
                newData = newData.withoutMobSuppressed(structureId);
            }

            if (result == RetrogenResult.PERMANENT_SKIP) {
                if (RetoldDelayedStructureIds.PILLAGER_OUTPOST.equals(structureId)) {
                    RetoldDelayedStructureMobBlocker.forgetDeferredStructure(structureId, pos);
                    RetoldDelayedStructureMobBlocker.rememberSuppressedStructure(structureId, pos);
                    newData = newData.withMobSuppressed(structureId);
                }

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
        ChunkAccess chunk = level.getChunk(pos.x(), pos.z());

        RetoldChunkStructureData chunkData =
                chunk.getData(RetoldAttachments.CHUNK_STRUCTURE_DATA.get());

        if (!chunkData.hasDeferred(structureId)) {
            return RetrogenResult.PERMANENT_SKIP;
        }

        StructureManager structureManager = level.structureManager();

        List<StructureStart> starts = structureManager.startsForStructure(
                pos,
                structure -> {
                    if (!RetoldDelayedStructureHelper.isDelayedStructure(
                            level.registryAccess(),
                            structure
                    )) {
                        return false;
                    }

                    String id = RetoldDelayedStructureHelper.getStructureId(
                            level.registryAccess(),
                            structure
                    );

                    return structureId.equals(id);
                }
        );

        if (starts.isEmpty()) {
            Retold.LOGGER.warn(
                    "No vanilla StructureStart found for deferred {} in chunk [{}, {}]",
                    structureId,
                    pos.x(),
                    pos.z()
            );

            return RetrogenResult.TRY_LATER;
        }

        boolean placedAny = false;

        for (StructureStart start : starts) {
            if (!start.isValid()) {
                continue;
            }

            RetrogenResult safety = checkWholeStructureAreaSafe(level, start);

            if (safety != RetrogenResult.SUCCESS) {
                return safety;
            }

            placeVanillaStructureStartInChunk(level, start, pos);
            placedAny = true;
        }

        if (!placedAny) {
            return RetrogenResult.TRY_LATER;
        }

        Retold.LOGGER.info(
                "Vanilla-retrogen placed {} in chunk [{}, {}]",
                structureId,
                pos.x(),
                pos.z()
        );

        return RetrogenResult.SUCCESS;
    }

    private static void placeVanillaStructureStartInChunk(
            ServerLevel level,
            StructureStart start,
            ChunkPos chunkPos
    ) {
        BoundingBox writableArea = getChunkWritableArea(level, chunkPos);

        long chunkLong =
                ((long) chunkPos.x() & 4294967295L)
                        | (((long) chunkPos.z() & 4294967295L) << 32);

        long randomSeed = level.getSeed() ^ chunkLong;

        start.placeInChunk(
                level,
                level.structureManager(),
                level.getChunkSource().getGenerator(),
                RandomSource.create(randomSeed),
                writableArea,
                chunkPos
        );
    }

    private static BoundingBox getChunkWritableArea(
            ServerLevel level,
            ChunkPos chunkPos
    ) {
        int minX = chunkPos.getMinBlockX();
        int minZ = chunkPos.getMinBlockZ();
        int maxX = chunkPos.getMaxBlockX();
        int maxZ = chunkPos.getMaxBlockZ();

        return new BoundingBox(
                minX,
                level.getMinY(),
                minZ,
                maxX,
                level.getMaxY() - 1,
                maxZ
        );
    }

    private static RetrogenResult checkWholeStructureAreaSafe(
            ServerLevel level,
            StructureStart start
    ) {
        BoundingBox box = start.getBoundingBox();

        Iterable<ChunkPos> touchedChunks =
                box.intersectingChunks()::iterator;

        for (ChunkPos touchedChunkPos : touchedChunks) {
            if (!level.hasChunk(touchedChunkPos.x(), touchedChunkPos.z())) {
                return RetrogenResult.TRY_LATER;
            }

            ChunkAccess touchedChunk =
                    level.getChunk(touchedChunkPos.x(), touchedChunkPos.z());

            RetoldChunkStructureData data =
                    touchedChunk.getData(RetoldAttachments.CHUNK_STRUCTURE_DATA.get());

            if (data.isEditedByPlayer()) {
                Retold.LOGGER.info(
                        "Skipping vanilla retrogen because touched chunk [{}, {}] is edited",
                        touchedChunkPos.x(),
                        touchedChunkPos.z()
                );

                return RetrogenResult.PERMANENT_SKIP;
            }
        }

        return RetrogenResult.SUCCESS;
    }

    private enum RetrogenResult {
        SUCCESS,
        PERMANENT_SKIP,
        TRY_LATER
    }
}