package cz.xefensor.retold.worldgen.delayed;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.stage.RetoldStageRuntime;
import cz.xefensor.retold.stage.RetoldWorldStage;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.*;

public final class RetoldDelayedStructureRetrogen {
    private static final Queue<ChunkPos> QUEUE = new ArrayDeque<>();
    private static final Set<Long> QUEUED = new HashSet<>();

    // Chunks where Stage 1 definitely deferred some delayed structure.
    // This fixes already-loaded chunks after stage switching.
    private static final Set<Long> KNOWN_DEFERRED_CHUNKS = new HashSet<>();
    private static final Set<String> SUPPRESSED_STRUCTURE_STARTS = new HashSet<>();

    private static final Map<Long, ChunkPos> RETRY_POSITIONS = new HashMap<>();
    private static final Map<Long, Integer> RETRY_TICKS = new HashMap<>();

    private static final int CHUNKS_PER_TICK = 2;
    private static final int RETRY_DELAY_TICKS = 40;

    private static int currentTick = 0;

    private RetoldDelayedStructureRetrogen() {
    }

    public static void rememberDeferredChunk(ChunkPos pos) {
        KNOWN_DEFERRED_CHUNKS.add(packChunk(pos.x(), pos.z()));
    }

    public static void queueKnownDeferredChunksForStage(ServerLevel level) {
        if (level.dimension() != Level.OVERWORLD) {
            return;
        }

        for (long packed : Set.copyOf(KNOWN_DEFERRED_CHUNKS)) {
            int chunkX = unpackChunkX(packed);
            int chunkZ = unpackChunkZ(packed);

            if (!level.hasChunk(chunkX, chunkZ)) {
                continue;
            }

            enqueue(new ChunkPos(chunkX, chunkZ));
        }
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

        ChunkAccess loadedChunk = event.getChunk();
        ChunkPos pos = loadedChunk.getPos();

        RetoldChunkStructureData data =
                loadedChunk.getData(RetoldAttachments.CHUNK_STRUCTURE_DATA.get());

        if (data.hasAnyDeferredStructures()) {
            rememberDeferredChunk(pos);
        }

        boolean retryLater = processChunk(level, pos);

        if (retryLater) {
            scheduleRetry(pos);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        currentTick++;

        ServerLevel overworld = event.getServer().getLevel(Level.OVERWORLD);

        if (overworld == null) {
            return;
        }

        if (!RetoldStageRuntime.isAtLeast(RetoldWorldStage.STAGE_2)) {
            return;
        }

        enqueueDueRetries();

        for (int i = 0; i < CHUNKS_PER_TICK && !QUEUE.isEmpty(); i++) {
            ChunkPos pos = QUEUE.poll();
            QUEUED.remove(packChunk(pos.x(), pos.z()));

            boolean retryLater = processChunk(overworld, pos);

            if (retryLater) {
                scheduleRetry(pos);
            }
        }
    }

    private static void enqueueDueRetries() {
        for (long key : Set.copyOf(RETRY_TICKS.keySet())) {
            int retryTick = RETRY_TICKS.getOrDefault(key, Integer.MAX_VALUE);

            if (retryTick > currentTick) {
                continue;
            }

            ChunkPos pos = RETRY_POSITIONS.remove(key);
            RETRY_TICKS.remove(key);

            if (pos != null) {
                enqueue(pos);
            }
        }
    }

    private static void scheduleRetry(ChunkPos pos) {
        long key = packChunk(pos.x(), pos.z());

        RETRY_POSITIONS.put(key, pos);
        RETRY_TICKS.put(key, currentTick + RETRY_DELAY_TICKS);
    }

    public static void enqueueForPossibleRetrogen(
            ServerLevel level,
            ChunkPos pos
    ) {
        if (level.dimension() != Level.OVERWORLD) {
            return;
        }

        if (!RetoldStageRuntime.isAtLeast(RetoldWorldStage.STAGE_2)) {
            return;
        }

        if (!level.hasChunk(pos.x(), pos.z())) {
            return;
        }

        boolean retryLater = processChunk(level, pos);

        if (retryLater) {
            scheduleRetry(pos);
        }
    }

    private static void enqueue(ChunkPos pos) {
        long key = packChunk(pos.x(), pos.z());

        if (QUEUED.add(key)) {
            QUEUE.add(pos);
        }
    }

    private static boolean processChunk(ServerLevel level, ChunkPos pos) {
        if (!level.hasChunk(pos.x(), pos.z())) {
            return false;
        }

        ChunkAccess chunk = level.getChunk(pos.x(), pos.z());

        RetoldChunkStructureData data =
                chunk.getData(RetoldAttachments.CHUNK_STRUCTURE_DATA.get());

        RetoldChunkStructureData newData = data;
        boolean retryLater = false;

        for (String structureId : RetoldDelayedStructureIds.ALL) {
            if (!RetoldStageRuntime.isAtLeast(RetoldDelayedStructureIds.requiredStage(structureId))) {
                continue;
            }

            if (newData.hasChecked(structureId)) {
                newData = newData.withoutDeferred(structureId);
                continue;
            }

            List<StructureStart> starts =
                    findVanillaStartsForStructure(level, pos, structureId);

            boolean hasDeferred = newData.hasDeferred(structureId);

            if (!hasDeferred && starts.isEmpty()) {
                continue;
            }

            RetrogenResult result =
                    tryRetrogenStructure(level, pos, structureId, starts, hasDeferred);

            if (result == RetrogenResult.TRY_LATER) {
                retryLater = true;
                continue;
            }

            if (result == RetrogenResult.SUCCESS) {
                RetoldDelayedStructureMobBlocker.forgetDeferredStructure(structureId, pos);

                newData = newData.withChecked(structureId);
                newData = newData.withoutDeferred(structureId);
                newData = newData.withoutMobSuppressed(structureId);

                KNOWN_DEFERRED_CHUNKS.remove(packChunk(pos.x(), pos.z()));
            }

            if (result == RetrogenResult.PERMANENT_SKIP) {
                if (RetoldDelayedStructureIds.PILLAGER_OUTPOST.equals(structureId)) {
                    RetoldDelayedStructureMobBlocker.forgetDeferredStructure(structureId, pos);
                    RetoldDelayedStructureMobBlocker.rememberSuppressedStructure(structureId, pos);
                    newData = newData.withMobSuppressed(structureId);
                }

                newData = newData.withChecked(structureId);
                newData = newData.withoutDeferred(structureId);

                KNOWN_DEFERRED_CHUNKS.remove(packChunk(pos.x(), pos.z()));
            }
        }

        if (newData != data) {
            chunk.setData(RetoldAttachments.CHUNK_STRUCTURE_DATA.get(), newData);
        }

        return retryLater;
    }

    private static RetrogenResult tryRetrogenStructure(
            ServerLevel level,
            ChunkPos pos,
            String structureId,
            List<StructureStart> starts,
            boolean hasDeferred
    ) {
        ChunkAccess chunk = level.getChunk(pos.x(), pos.z());

        RetoldChunkStructureData chunkData =
                chunk.getData(RetoldAttachments.CHUNK_STRUCTURE_DATA.get());

        if (starts.isEmpty()) {
            if (hasDeferred) {
                Retold.LOGGER.debug(
                        "No vanilla StructureStart found yet for deferred {} in chunk [{}, {}]",
                        structureId,
                        pos.x(),
                        pos.z()
                );

                return RetrogenResult.TRY_LATER;
            }

            return RetrogenResult.NO_ACTION;
        }

        boolean placedAny = false;

        for (StructureStart start : starts) {
            if (!start.isValid()) {
                continue;
            }

            String startKey = getStructureStartKey(structureId, start);

            if (SUPPRESSED_STRUCTURE_STARTS.contains(startKey)) {
                markLoadedTouchedChunksHandled(level, start, structureId, true);

                Retold.LOGGER.debug(
                        "Skipping {} in chunk [{}, {}] because whole structure start is suppressed",
                        structureId,
                        pos.x(),
                        pos.z()
                );

                return RetrogenResult.PERMANENT_SKIP;
            }

            if (hasAnyEditedLoadedTouchedChunk(level, start)) {
                SUPPRESSED_STRUCTURE_STARTS.add(startKey);
                markLoadedTouchedChunksHandled(level, start, structureId, true);

                Retold.LOGGER.info(
                        "Suppressing whole {} because one touched chunk is edited. Current chunk [{}, {}]",
                        structureId,
                        pos.x(),
                        pos.z()
                );

                return RetrogenResult.PERMANENT_SKIP;
            }

            if (RetoldClientChunkTracker.isSentToAnyPlayer(pos)) {
                return RetrogenResult.TRY_LATER;
            }

            placeVanillaStructureStartInChunk(level, start, pos);
            placedAny = true;
        }

        if (!placedAny) {
            return RetrogenResult.TRY_LATER;
        }

        Retold.LOGGER.info(
                "Vanilla-retrogen placed chunk part of {} in chunk [{}, {}]",
                structureId,
                pos.x(),
                pos.z()
        );

        return RetrogenResult.SUCCESS;
    }

    private static boolean hasAnyEditedLoadedTouchedChunk(
            ServerLevel level,
            StructureStart start
    ) {
        for (ChunkPos touchedChunkPos : getTouchedChunks(start)) {
            if (!level.hasChunk(touchedChunkPos.x(), touchedChunkPos.z())) {
                continue;
            }

            ChunkAccess touchedChunk =
                    level.getChunk(touchedChunkPos.x(), touchedChunkPos.z());

            RetoldChunkStructureData data =
                    touchedChunk.getData(RetoldAttachments.CHUNK_STRUCTURE_DATA.get());

            if (data.isEditedByPlayer()) {
                Retold.LOGGER.info(
                        "Touched chunk [{}, {}] is edited, whole delayed structure will be skipped",
                        touchedChunkPos.x(),
                        touchedChunkPos.z()
                );

                return true;
            }
        }

        return false;
    }

    private static void markLoadedTouchedChunksHandled(
            ServerLevel level,
            StructureStart start,
            String structureId,
            boolean permanentlySkipped
    ) {
        for (ChunkPos touchedChunkPos : getTouchedChunks(start)) {
            if (!level.hasChunk(touchedChunkPos.x(), touchedChunkPos.z())) {
                continue;
            }

            ChunkAccess touchedChunk =
                    level.getChunk(touchedChunkPos.x(), touchedChunkPos.z());

            RetoldChunkStructureData oldData =
                    touchedChunk.getData(RetoldAttachments.CHUNK_STRUCTURE_DATA.get());

            RetoldChunkStructureData newData = oldData
                    .withChecked(structureId)
                    .withoutDeferred(structureId);

            if (permanentlySkipped
                    && RetoldDelayedStructureIds.PILLAGER_OUTPOST.equals(structureId)) {
                newData = newData.withMobSuppressed(structureId);

                RetoldDelayedStructureMobBlocker.forgetDeferredStructure(
                        structureId,
                        touchedChunkPos
                );

                RetoldDelayedStructureMobBlocker.rememberSuppressedStructure(
                        structureId,
                        touchedChunkPos
                );
            } else {
                newData = newData.withoutMobSuppressed(structureId);

                RetoldDelayedStructureMobBlocker.forgetDeferredStructure(
                        structureId,
                        touchedChunkPos
                );
            }

            if (newData != oldData) {
                touchedChunk.setData(
                        RetoldAttachments.CHUNK_STRUCTURE_DATA.get(),
                        newData
                );
            }

            KNOWN_DEFERRED_CHUNKS.remove(packChunk(touchedChunkPos.x(), touchedChunkPos.z()));
        }
    }

    private static List<ChunkPos> getTouchedChunks(StructureStart start) {
        List<ChunkPos> result = new ArrayList<>();

        Iterable<ChunkPos> touchedChunks =
                start.getBoundingBox().intersectingChunks()::iterator;

        for (ChunkPos touchedChunkPos : touchedChunks) {
            result.add(touchedChunkPos);
        }

        return result;
    }

    private static String getStructureStartKey(
            String structureId,
            StructureStart start
    ) {
        return structureId + "|" + start.getBoundingBox();
    }

    private static List<StructureStart> findVanillaStartsForStructure(
            ServerLevel level,
            ChunkPos pos,
            String structureId
    ) {
        StructureManager structureManager = level.structureManager();

        List<StructureStart> result = new ArrayList<>();

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

        result.addAll(starts);

        return result;
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

        RetoldRetrogenDropBlocker.beginChunk(chunkPos);

        try {
            start.placeInChunk(
                    level,
                    level.structureManager(),
                    level.getChunkSource().getGenerator(),
                    RandomSource.create(randomSeed),
                    writableArea,
                    chunkPos
            );
        } finally {
            RetoldRetrogenDropBlocker.endChunk(chunkPos);
        }
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

    private static long packChunk(int chunkX, int chunkZ) {
        return ((long) chunkX & 4294967295L)
                | (((long) chunkZ & 4294967295L) << 32);
    }

    private static int unpackChunkX(long packed) {
        return (int) packed;
    }

    private static int unpackChunkZ(long packed) {
        return (int) (packed >> 32);
    }

    private enum RetrogenResult {
        SUCCESS,
        PERMANENT_SKIP,
        TRY_LATER,
        NO_ACTION
    }
}