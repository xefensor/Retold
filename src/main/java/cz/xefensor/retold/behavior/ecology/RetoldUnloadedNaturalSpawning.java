package cz.xefensor.retold.behavior.ecology;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.gamerules.GameRules;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/** Bounded vanilla-compatible spawn attempts for returning chunks. */
public final class RetoldUnloadedNaturalSpawning {
    public static final int MAX_CHUNKS_PER_TICK = 1;

    private static final int MAX_PENDING_CHUNKS = 4_096;
    private static final int MAX_ATTEMPTS_PER_CHUNK = 7;

    private static final Queue<SpawnKey> PENDING = new ArrayDeque<>();
    private static final Map<SpawnKey, SpawnTask> TASKS = new HashMap<>();

    private RetoldUnloadedNaturalSpawning() {
    }

    public static synchronized void enqueue(
            ServerLevel level,
            BlockPos returningPos,
            int unloadedDays
    ) {
        if (level == null || returningPos == null || unloadedDays <= 0) {
            return;
        }

        SpawnKey key = new SpawnKey(
                level,
                ChunkPos.pack(returningPos)
        );
        int attempts = Math.min(MAX_ATTEMPTS_PER_CHUNK, unloadedDays);
        SpawnTask existing = TASKS.get(key);

        if (existing != null) {
            if (attempts > existing.remainingAttempts()) {
                TASKS.put(key, new SpawnTask(key, attempts));
            }

            return;
        }

        if (TASKS.size() >= MAX_PENDING_CHUNKS) {
            return;
        }

        TASKS.put(key, new SpawnTask(key, attempts));
        PENDING.add(key);
    }

    public static synchronized int processPending(int maximumChunks) {
        int processed = 0;
        int limit = Math.max(0, maximumChunks);

        while (processed < limit && !PENDING.isEmpty()) {
            SpawnKey key = PENDING.remove();
            SpawnTask task = TASKS.remove(key);

            if (task == null) {
                continue;
            }

            processed++;
            AttemptResult result = attempt(task);

            if (result == AttemptResult.DEFERRED) {
                requeue(task);
            } else if (result == AttemptResult.ATTEMPTED
                    && task.remainingAttempts() > 1) {
                requeue(new SpawnTask(
                        key,
                        task.remainingAttempts() - 1
                ));
            }
        }

        return processed;
    }

    public static synchronized int pendingCount() {
        return TASKS.size();
    }

    public static synchronized int pendingAttemptCount() {
        return TASKS.values().stream()
                .mapToInt(SpawnTask::remainingAttempts)
                .sum();
    }

    public static synchronized void clear() {
        PENDING.clear();
        TASKS.clear();
    }

    private static AttemptResult attempt(SpawnTask task) {
        ServerLevel level = task.key().level();

        if (!level.getGameRules().get(GameRules.SPAWN_MOBS)) {
            return AttemptResult.FINISHED;
        }

        ChunkPos chunkPos = ChunkPos.unpack(task.key().chunk());
        LevelChunk chunk = level.getChunkSource().getChunkNow(
                chunkPos.x(),
                chunkPos.z()
        );

        if (chunk == null) {
            return AttemptResult.FINISHED;
        }

        NaturalSpawner.SpawnState spawnState = level.getChunkSource()
                .getLastSpawnState();

        if (spawnState == null) {
            return AttemptResult.DEFERRED;
        }

        if (!hasLoadedSpawnNeighborhood(level, chunkPos)) {
            return AttemptResult.ATTEMPTED;
        }

        // Each queued entry is already the single daily pass, so include
        // persistent creature categories instead of applying vanilla's
        // loaded-world one-in-400-tick scheduling gate a second time.
        List<MobCategory> categories =
                NaturalSpawner.getFilteredSpawningCategories(
                        spawnState,
                        level.isSpawningMonsters(),
                        true
                );

        if (level.canSpawnEntitiesInChunk(chunkPos)
                && !categories.isEmpty()) {
            NaturalSpawner.spawnForChunk(
                    level,
                    chunk,
                    spawnState,
                    categories
            );
        }

        return AttemptResult.ATTEMPTED;
    }

    private static boolean hasLoadedSpawnNeighborhood(
            ServerLevel level,
            ChunkPos center
    ) {
        for (int offsetX = -1; offsetX <= 1; offsetX++) {
            for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
                if (level.getChunkSource().getChunkNow(
                        center.x() + offsetX,
                        center.z() + offsetZ
                ) == null) {
                    return false;
                }
            }
        }

        return true;
    }

    private static void requeue(SpawnTask task) {
        if (TASKS.size() >= MAX_PENDING_CHUNKS
                || TASKS.putIfAbsent(task.key(), task) != null) {
            return;
        }

        PENDING.add(task.key());
    }

    private record SpawnKey(ServerLevel level, long chunk) {
    }

    private record SpawnTask(SpawnKey key, int remainingAttempts) {
    }

    private enum AttemptResult {
        ATTEMPTED,
        FINISHED,
        DEFERRED
    }
}
