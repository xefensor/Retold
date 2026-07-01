package cz.xefensor.retold.aender;

import cz.xefensor.retold.Retold;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public final class RetoldAenderUnstableChunkEvents {
    private static final Map<Long, ChunkPos> LOADED_CHUNKS =
            new HashMap<>();

    private static final Queue<ChunkPos> DIRTY_CHUNKS =
            new ArrayDeque<>();

    private static final Set<Long> DIRTY_CHUNK_KEYS =
            new HashSet<>();

    private static final Map<Long, Integer> DIRTY_RETRY_TICK =
            new HashMap<>();

    private static final Queue<RegenerationTask> REGENERATION_QUEUE =
            new ArrayDeque<>();

    private static final Set<Long> QUEUED_CHUNKS =
            new HashSet<>();

    private static final int DIRTY_CHUNKS_PER_TICK = 64;
    private static final int CHUNKS_PER_TICK = 1;
    private static final int MAX_REGENERATION_QUEUE_SIZE = 4096;

    /*
     * This only protects chunks already loaded while the player is nearby.
     * Chunks loading from disk regenerate immediately in onChunkLoad,
     * before being watched/sent.
     */
    private static final int PLAYER_SAFE_RADIUS_CHUNKS = 8;

    private static final int DIRTY_RETRY_DELAY_TICKS = 40;
    private static final int GLOBAL_MUTATION_COOLDOWN_TICKS = 120;

    private static int currentTick = 0;
    private static int nextGlobalMutationTick = 0;
    private static long mutationSequence = 0L;

    private RetoldAenderUnstableChunkEvents() {
    }

    @SubscribeEvent
    public static void onChunkSent(ChunkWatchEvent.Sent event) {
        /*
         * Intentionally unused.
         *
         * Aender instability does not depend on watcher counts.
         * Watcher counts were the source of missed rows/lines before.
         */
    }

    @SubscribeEvent
    public static void onChunkUnwatch(ChunkWatchEvent.UnWatch event) {
        ServerLevel level =
                event.getLevel();

        if (!RetoldAenderChunkStability.isAender(level)) {
            return;
        }

        requestGlobalTerrainMutation(level);
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        if (!RetoldAenderChunkStability.isAender(level)) {
            return;
        }

        ChunkAccess chunk =
                event.getChunk();

        ChunkPos pos =
                chunk.getPos();

        LOADED_CHUNKS.put(
                packChunk(pos.x(), pos.z()),
                pos
        );

        /*
         * Important:
         * If this chunk was unloaded while the Aender changed,
         * regenerate it immediately on load.
         *
         * This is the cleanest version of the rule:
         * "If you leave unstable Aender terrain and come back,
         * it is different."
         */
        regenerateLoadedChunkImmediatelyIfOutdated(
                level,
                chunk
        );

        enqueueDirtyChunk(pos, 0);
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        if (!RetoldAenderChunkStability.isAender(level)) {
            return;
        }

        ChunkAccess chunk =
                event.getChunk();

        ChunkPos pos =
                chunk.getPos();

        LOADED_CHUNKS.remove(
                packChunk(pos.x(), pos.z())
        );

        RetoldAenderChunkData data =
                chunk.getData(RetoldAenderAttachments.CHUNK_DATA.get());

        if (!data.isStabilized()) {
            requestGlobalTerrainMutation(level);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        currentTick++;

        ServerLevel aenderLevel =
                event.getServer().getLevel(RetoldAenderDimensions.AENDER);

        if (aenderLevel == null) {
            return;
        }

        processDirtyChunks(aenderLevel);
        processRegenerationQueue(aenderLevel);
    }

    private static void requestGlobalTerrainMutation(ServerLevel level) {
        if (currentTick < nextGlobalMutationTick) {
            enqueueAllLoadedChunksDirty();
            return;
        }

        RetoldAenderTerrainData terrainData =
                RetoldAenderTerrainData.get(level);

        long salt =
                createMutationSalt(level);

        long revision =
                terrainData.bumpTerrain(salt);

        nextGlobalMutationTick =
                currentTick + GLOBAL_MUTATION_COOLDOWN_TICKS;

        enqueueAllLoadedChunksDirty();

        Retold.LOGGER.info(
                "Mutated Aender global terrain to revision {}",
                revision
        );
    }

    private static void enqueueAllLoadedChunksDirty() {
        for (ChunkPos pos : Set.copyOf(LOADED_CHUNKS.values())) {
            enqueueDirtyChunk(pos, 0);
        }
    }

    private static void enqueueDirtyChunk(
            ChunkPos pos,
            int delayTicks
    ) {
        long key =
                packChunk(pos.x(), pos.z());

        int retryTick =
                currentTick + delayTicks;

        DIRTY_RETRY_TICK.merge(
                key,
                retryTick,
                Math::min
        );

        if (DIRTY_CHUNK_KEYS.add(key)) {
            DIRTY_CHUNKS.add(pos);
        }
    }

    private static void processDirtyChunks(ServerLevel level) {
        for (int i = 0;
             i < DIRTY_CHUNKS_PER_TICK && !DIRTY_CHUNKS.isEmpty();
             i++) {
            ChunkPos pos =
                    DIRTY_CHUNKS.poll();

            long key =
                    packChunk(pos.x(), pos.z());

            int retryTick =
                    DIRTY_RETRY_TICK.getOrDefault(
                            key,
                            currentTick
                    );

            if (retryTick > currentTick) {
                DIRTY_CHUNKS.add(pos);
                continue;
            }

            DIRTY_CHUNK_KEYS.remove(key);
            DIRTY_RETRY_TICK.remove(key);

            if (!level.hasChunk(pos.x(), pos.z())) {
                continue;
            }

            if (!canScheduleChunk(level, pos)) {
                enqueueDirtyChunk(
                        pos,
                        DIRTY_RETRY_DELAY_TICKS
                );
                continue;
            }

            scheduleChunkIfOutdated(level, pos);
        }
    }

    private static boolean canScheduleChunk(
            ServerLevel level,
            ChunkPos pos
    ) {
        if (hasPlayerNearChunk(
                level,
                pos,
                PLAYER_SAFE_RADIUS_CHUNKS
        )) {
            return false;
        }

        ChunkAccess chunk =
                level.getChunk(pos.x(), pos.z());

        RetoldAenderChunkData data =
                chunk.getData(RetoldAenderAttachments.CHUNK_DATA.get());

        return !data.isStabilized();
    }

    private static void scheduleChunkIfOutdated(
            ServerLevel level,
            ChunkPos pos
    ) {
        RetoldAenderTerrainData terrainData =
                RetoldAenderTerrainData.get(level);

        long targetRevision =
                terrainData.getRevision();

        if (targetRevision <= 0L) {
            return;
        }

        ChunkAccess chunk =
                level.getChunk(pos.x(), pos.z());

        RetoldAenderChunkData data =
                chunk.getData(RetoldAenderAttachments.CHUNK_DATA.get());

        if (data.appliedTerrainRevision() == targetRevision) {
            return;
        }

        long key =
                packChunk(pos.x(), pos.z());

        if (QUEUED_CHUNKS.contains(key)) {
            return;
        }

        if (REGENERATION_QUEUE.size() >= MAX_REGENERATION_QUEUE_SIZE) {
            enqueueDirtyChunk(
                    pos,
                    DIRTY_RETRY_DELAY_TICKS
            );
            return;
        }

        if (!RetoldAenderChunkStability.scheduleTerrainRegeneration(
                level,
                chunk,
                targetRevision
        )) {
            return;
        }

        QUEUED_CHUNKS.add(key);

        REGENERATION_QUEUE.add(
                new RegenerationTask(
                        pos,
                        targetRevision
                )
        );
    }

    private static void processRegenerationQueue(ServerLevel level) {
        for (int i = 0;
             i < CHUNKS_PER_TICK && !REGENERATION_QUEUE.isEmpty();
             i++) {
            RegenerationTask task =
                    REGENERATION_QUEUE.poll();

            ChunkPos pos =
                    task.pos();

            QUEUED_CHUNKS.remove(
                    packChunk(pos.x(), pos.z())
            );

            if (!level.hasChunk(pos.x(), pos.z())) {
                continue;
            }

            if (hasPlayerNearChunk(
                    level,
                    pos,
                    PLAYER_SAFE_RADIUS_CHUNKS
            )) {
                enqueueDirtyChunk(
                        pos,
                        DIRTY_RETRY_DELAY_TICKS
                );
                continue;
            }

            regenerateIfStillOutdated(level, task);
        }
    }

    private static void regenerateLoadedChunkImmediatelyIfOutdated(
            ServerLevel level,
            ChunkAccess chunk
    ) {
        RetoldAenderTerrainData terrainData =
                RetoldAenderTerrainData.get(level);

        long targetRevision =
                terrainData.getRevision();

        if (targetRevision <= 0L) {
            return;
        }

        RetoldAenderChunkData data =
                chunk.getData(RetoldAenderAttachments.CHUNK_DATA.get());

        if (data.isStabilized()) {
            return;
        }

        if (data.appliedTerrainRevision() == targetRevision) {
            return;
        }

        if (!RetoldAenderChunkStability.scheduleTerrainRegeneration(
                level,
                chunk,
                targetRevision
        )) {
            return;
        }

        RetoldAenderTerrainBuilder.regenerateFloatingIslands(
                chunk,
                level.getSeed(),
                terrainData
        );

        RetoldAenderChunkStability.markTerrainRegenerationFinished(
                level,
                chunk
        );

        Retold.LOGGER.info(
                "Immediately regenerated loaded Aender chunk [{}, {}] to terrain revision {}",
                chunk.getPos().x(),
                chunk.getPos().z(),
                targetRevision
        );
    }

    private static void regenerateIfStillOutdated(
            ServerLevel level,
            RegenerationTask task
    ) {
        ChunkPos pos =
                task.pos();

        ChunkAccess chunk =
                level.getChunk(pos.x(), pos.z());

        RetoldAenderChunkData data =
                chunk.getData(RetoldAenderAttachments.CHUNK_DATA.get());

        if (data.isStabilized()) {
            return;
        }

        if (!data.shouldRegenerateOnNextLoad()) {
            return;
        }

        RetoldAenderTerrainData terrainData =
                RetoldAenderTerrainData.get(level);

        if (terrainData.getRevision() != task.targetRevision()) {
            enqueueDirtyChunk(pos, 0);
            return;
        }

        if (data.targetTerrainRevision() != task.targetRevision()) {
            enqueueDirtyChunk(pos, 0);
            return;
        }

        RetoldAenderTerrainBuilder.regenerateFloatingIslands(
                chunk,
                level.getSeed(),
                terrainData
        );

        RetoldAenderChunkStability.markTerrainRegenerationFinished(
                level,
                chunk
        );

        Retold.LOGGER.info(
                "Regenerated Aender chunk [{}, {}] to terrain revision {}",
                pos.x(),
                pos.z(),
                task.targetRevision()
        );
    }

    private static boolean hasPlayerNearChunk(
            ServerLevel level,
            ChunkPos pos,
            int radiusChunks
    ) {
        for (var player : level.players()) {
            int playerChunkX =
                    ((int) Math.floor(player.getX())) >> 4;

            int playerChunkZ =
                    ((int) Math.floor(player.getZ())) >> 4;

            if (Math.abs(playerChunkX - pos.x()) <= radiusChunks
                    && Math.abs(playerChunkZ - pos.z()) <= radiusChunks) {
                return true;
            }
        }

        return false;
    }

    private static long createMutationSalt(ServerLevel level) {
        mutationSequence++;

        long value =
                level.getSeed();

        value ^= mutationSequence * 0x9E3779B97F4A7C15L;
        value ^= (long) currentTick * 0xD1B54A32D192ED03L;
        value ^= 0xA3D1E41F29B7C53DL;

        long salt =
                mix64(value);

        if (salt == 0L) {
            return 1L;
        }

        return salt;
    }

    private static long mix64(long value) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= value >>> 33;

        return value;
    }

    private static long packChunk(
            int chunkX,
            int chunkZ
    ) {
        return ((long) chunkX & 4294967295L)
                | (((long) chunkZ & 4294967295L) << 32);
    }

    private record RegenerationTask(
            ChunkPos pos,
            long targetRevision
    ) {
    }
}