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
import java.util.concurrent.ThreadLocalRandom;

public final class RetoldAenderUnstableChunkEvents {
    private static final Queue<ChunkPos> REGENERATION_QUEUE =
            new ArrayDeque<>();

    private static final Set<Long> QUEUED_CHUNKS =
            new HashSet<>();

    private static final Map<Long, Integer> WATCHERS =
            new HashMap<>();

    private static final Map<Long, Integer> REGENERATION_COOLDOWN_UNTIL_TICK =
            new HashMap<>();

    private static final int CHUNKS_PER_TICK = 1;
    private static final int MAX_REGENERATION_QUEUE_SIZE = 2048;
    private static final int REGENERATION_COOLDOWN_TICKS = 100;
    private static final int COOLDOWN_CLEANUP_INTERVAL_TICKS = 200;

    private static int currentTick = 0;

    private RetoldAenderUnstableChunkEvents() {
    }

    @SubscribeEvent
    public static void onChunkSent(ChunkWatchEvent.Sent event) {
        ServerLevel level = event.getLevel();

        if (!RetoldAenderChunkStability.isAender(level)) {
            return;
        }

        ChunkPos pos = event.getPos();
        long key = packChunk(pos.x(), pos.z());

        WATCHERS.merge(key, 1, Integer::sum);
    }

    @SubscribeEvent
    public static void onChunkUnwatch(ChunkWatchEvent.UnWatch event) {
        ServerLevel level = event.getLevel();

        if (!RetoldAenderChunkStability.isAender(level)) {
            return;
        }

        ChunkPos pos = event.getPos();
        long key = packChunk(pos.x(), pos.z());

        int watchers = WATCHERS.getOrDefault(key, 0) - 1;

        if (watchers <= 0) {
            WATCHERS.remove(key);
            scheduleUnwatchedChunkRegeneration(level, pos);
        } else {
            WATCHERS.put(key, watchers);
        }
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        if (!RetoldAenderChunkStability.isAender(level)) {
            return;
        }

        ChunkAccess chunk = event.getChunk();

        RetoldAenderChunkData data =
                chunk.getData(RetoldAenderAttachments.CHUNK_DATA.get());

        if (data.shouldGenerateInitialTerrain()) {
            generateInitialTerrain(level, chunk);
            return;
        }

        if (!data.shouldRegenerateOnNextLoad()) {
            return;
        }

        enqueue(chunk.getPos());
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        currentTick++;

        if (currentTick % COOLDOWN_CLEANUP_INTERVAL_TICKS == 0) {
            cleanupExpiredCooldowns();
        }

        ServerLevel aenderLevel =
                event.getServer().getLevel(RetoldAenderDimensions.AENDER);

        if (aenderLevel == null) {
            return;
        }

        for (int i = 0; i < CHUNKS_PER_TICK && !REGENERATION_QUEUE.isEmpty(); i++) {
            ChunkPos pos = REGENERATION_QUEUE.poll();
            long key = packChunk(pos.x(), pos.z());

            QUEUED_CHUNKS.remove(key);

            if (!aenderLevel.hasChunk(pos.x(), pos.z())) {
                continue;
            }

            if (isInRegenerationCooldown(pos)) {
                continue;
            }

            if (isWatched(pos)) {
                continue;
            }

            if (hasPlayerInsideChunk(aenderLevel, pos)) {
                continue;
            }

            regenerateIfStillUnstable(aenderLevel, pos);
        }
    }

    private static void generateInitialTerrain(
            ServerLevel level,
            ChunkAccess chunk
    ) {
        RetoldAenderChunkData data =
                chunk.getData(RetoldAenderAttachments.CHUNK_DATA.get());

        if (!data.shouldGenerateInitialTerrain()) {
            return;
        }

        long salt = initialSaltForChunk(level, chunk.getPos());

        RetoldAenderTerrainBuilder.generateFloatingIslands(
                level,
                chunk.getPos(),
                salt
        );

        RetoldAenderChunkData newData =
                data.withInitialTerrainGenerated(salt);

        chunk.setData(RetoldAenderAttachments.CHUNK_DATA.get(), newData);

        Retold.LOGGER.debug(
                "Generated initial Aender island terrain in chunk [{}, {}]",
                chunk.getPos().x(),
                chunk.getPos().z()
        );
    }

    private static void scheduleUnwatchedChunkRegeneration(
            ServerLevel level,
            ChunkPos pos
    ) {
        if (!level.hasChunk(pos.x(), pos.z())) {
            return;
        }

        if (isInRegenerationCooldown(pos)) {
            return;
        }

        ChunkAccess chunk = level.getChunk(pos.x(), pos.z());

        RetoldAenderChunkData data =
                chunk.getData(RetoldAenderAttachments.CHUNK_DATA.get());

        if (data.isStabilized()) {
            return;
        }

        RetoldAenderChunkStability.scheduleRegenerationOnNextLoad(
                level,
                chunk,
                ThreadLocalRandom.current().nextLong()
        );

        enqueue(pos);
    }

    private static void enqueue(ChunkPos pos) {
        if (REGENERATION_QUEUE.size() >= MAX_REGENERATION_QUEUE_SIZE) {
            Retold.LOGGER.warn(
                    "Skipped queueing Aender chunk [{}, {}] because regeneration queue is full",
                    pos.x(),
                    pos.z()
            );
            return;
        }

        long key = packChunk(pos.x(), pos.z());

        if (QUEUED_CHUNKS.add(key)) {
            REGENERATION_QUEUE.add(pos);
        }
    }

    private static boolean isWatched(ChunkPos pos) {
        return WATCHERS.containsKey(packChunk(pos.x(), pos.z()));
    }

    private static boolean hasPlayerInsideChunk(
            ServerLevel level,
            ChunkPos pos
    ) {
        for (var player : level.players()) {
            int playerChunkX = ((int) Math.floor(player.getX())) >> 4;
            int playerChunkZ = ((int) Math.floor(player.getZ())) >> 4;

            if (playerChunkX == pos.x() && playerChunkZ == pos.z()) {
                return true;
            }
        }

        return false;
    }

    private static boolean isInRegenerationCooldown(ChunkPos pos) {
        long key = packChunk(pos.x(), pos.z());
        int cooldownUntilTick =
                REGENERATION_COOLDOWN_UNTIL_TICK.getOrDefault(key, -1);

        return cooldownUntilTick > currentTick;
    }

    private static void startRegenerationCooldown(ChunkPos pos) {
        long key = packChunk(pos.x(), pos.z());

        REGENERATION_COOLDOWN_UNTIL_TICK.put(
                key,
                currentTick + REGENERATION_COOLDOWN_TICKS
        );
    }

    private static void cleanupExpiredCooldowns() {
        for (long key : Set.copyOf(REGENERATION_COOLDOWN_UNTIL_TICK.keySet())) {
            int cooldownUntilTick =
                    REGENERATION_COOLDOWN_UNTIL_TICK.getOrDefault(key, -1);

            if (cooldownUntilTick <= currentTick) {
                REGENERATION_COOLDOWN_UNTIL_TICK.remove(key);
            }
        }
    }

    private static void regenerateIfStillUnstable(
            ServerLevel level,
            ChunkPos pos
    ) {
        ChunkAccess chunk = level.getChunk(pos.x(), pos.z());

        RetoldAenderChunkData data =
                chunk.getData(RetoldAenderAttachments.CHUNK_DATA.get());

        if (!data.shouldRegenerateOnNextLoad()) {
            return;
        }

        if (data.isStabilized()) {
            return;
        }

        RetoldAenderTerrainBuilder.generateFloatingIslands(
                level,
                pos,
                data.regenerationSalt()
        );

        RetoldAenderChunkStability.markRegenerationFinished(level, chunk);
        startRegenerationCooldown(pos);

        Retold.LOGGER.info(
                "Regenerated unwatched unstable Aender island chunk [{}, {}]",
                pos.x(),
                pos.z()
        );
    }

    private static long initialSaltForChunk(
            ServerLevel level,
            ChunkPos pos
    ) {
        return level.getSeed()
                ^ packChunk(pos.x(), pos.z())
                ^ 0x4E2A19D7C05B8F33L;
    }

    private static long packChunk(int chunkX, int chunkZ) {
        return ((long) chunkX & 4294967295L)
                | (((long) chunkZ & 4294967295L) << 32);
    }
}