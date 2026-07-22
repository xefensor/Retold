package cz.xefensor.retold.aender.stability;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.aender.RetoldAenderDimensions;
import cz.xefensor.retold.aender.generation.AenderIslandSampler;
import cz.xefensor.retold.aender.generation.AenderLoadedChunkReplacement;
import cz.xefensor.retold.aender.generation.AenderVolatility;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public final class AenderRealityTickEvents {
    private static final int EXTRA_REGEN_RADIUS_CHUNKS = 2;
    private static final int SAFE_ARRIVAL_RADIUS_CHUNKS = 2;
    private static final int MAX_REGEN_CHUNKS_PER_TICK = 16;
    private static final int MAX_BLANK_CHUNKS_PER_TICK = 8;
    private static final int OVERLOADED_RETRY_TICKS = 5;
    private static final int MAX_QUEUE_SIZE = 8192;
    private static final long MAX_REGEN_NANOS_PER_TICK = 12_000_000L;
    private static final long MAX_BLANK_NANOS_PER_TICK = 4_000_000L;

    private static final Queue<Long> REGEN_QUEUE = new ArrayDeque<>();
    private static final Set<Long> QUEUED = new HashSet<>();
    private static final Queue<Long> BLANK_QUEUE = new ArrayDeque<>();
    private static final Set<Long> BLANK_QUEUED = new HashSet<>();
    private static final AenderActiveRegionTracker ACTIVE_REGIONS = new AenderActiveRegionTracker();
    private static long nextRegenGameTime;
    private static long nextBlankGameTime;
    private static long estimatedRegenNanos = 2_000_000L;

    private AenderRealityTickEvents() {
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        if (level.dimension() != RetoldAenderDimensions.AENDER) {
            return;
        }

        boolean scanTick = (level.getGameTime() & 15L) == 0L;

        if (scanTick) {
            updatePlayerLoadedRegions(level);
        }

        int blanked = processBlankQueue(level);

        if (blanked > 0 || !BLANK_QUEUE.isEmpty()) {
            return;
        }

        if (scanTick) {
            queueNearbyStaleChunks(level);
        }

        processRegenQueue(level);
    }

    public static void enqueueIfNeeded(ServerLevel level, ChunkAccess chunk) {
        if (level.dimension() != RetoldAenderDimensions.AENDER) {
            return;
        }

        if (AenderStabilityData.get(level).isStable(chunk.getPos())) {
            return;
        }

        AenderVolatility.retainForChunk(chunk);

        if (AenderVolatility.needsRegeneration(chunk)) {
            enqueue(chunk.getPos().x(), chunk.getPos().z());
        }
    }

    public static void clearPendingRegeneration() {
        REGEN_QUEUE.clear();
        QUEUED.clear();
        BLANK_QUEUE.clear();
        BLANK_QUEUED.clear();
        ACTIVE_REGIONS.clear();
        nextRegenGameTime = 0L;
        nextBlankGameTime = 0L;
        estimatedRegenNanos = 2_000_000L;
    }

    /**
     * Schedules removal of the previous reality from every still-loaded volatile
     * chunk before any of those chunks are regenerated.
     *
     * <p>This deliberately uses two TPS-paced phases: first all volatile chunks
     * become empty, then player-centered arrival preparation and nearby-chunk
     * scans regenerate them from the player outward. Stabilized chunks retain
     * their blocks and are marked current in the new runtime reality.</p>
     */
    public static void beginRealityTransition(ServerLevel level, List<ChunkPos> previouslyLoaded) {
        if (level.dimension() != RetoldAenderDimensions.AENDER) {
            return;
        }

        clearPendingRegeneration();
        AenderStabilityData stability = AenderStabilityData.get(level);
        int queuedForBlanking = 0;
        int preserved = 0;

        for (ChunkPos pos : previouslyLoaded) {
            ChunkAccess chunk = getLoadedChunk(level, pos.x(), pos.z());

            if (chunk == null) {
                continue;
            }

            AenderVolatility.retainForChunk(chunk);

            if (stability.isStable(pos)) {
                AenderVolatility.markGenerated(chunk);
                preserved++;
                continue;
            }

            if (enqueueBlank(pos.x(), pos.z())) {
                queuedForBlanking++;
            }
        }

        if (queuedForBlanking > 0 || preserved > 0) {
            Retold.LOGGER.debug(
                    "Started Aender reality transition with {} volatile chunks queued for blanking and {} preserved stable chunks",
                    queuedForBlanking,
                    preserved
            );
        }
    }

    /**
     * Synchronously guarantees only the 5x5 collision-safe arrival core.
     *
     * <p>The portal charge asynchronously prepares the wider client view for at
     * most five seconds. Keeping the final synchronous fallback fixed at this
     * radius prevents configured view distance from turning a teleport into an
     * unbounded main-thread generation stall.</p>
     */
    public static void prepareArrivalCore(ServerLevel level, BlockPos center) {
        if (level.dimension() != RetoldAenderDimensions.AENDER) {
            return;
        }

        int centerChunkX = center.getX() >> 4;
        int centerChunkZ = center.getZ() >> 4;
        int radius = SAFE_ARRIVAL_RADIUS_CHUNKS;
        int loaded = 0;
        int regenerated = 0;
        long startedAtNanos = System.nanoTime();
        AenderStabilityData stability = AenderStabilityData.get(level);

        // Work from the center out so the essential arrival chunks are prepared first.
        for (int ring = 0; ring <= radius; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) {
                        continue;
                    }

                    int chunkX = centerChunkX + dx;
                    int chunkZ = centerChunkZ + dz;
                    ChunkAccess chunk = getLoadedChunk(level, chunkX, chunkZ);

                    if (chunk == null) {
                        chunk = level.getChunk(chunkX, chunkZ);
                        loaded++;
                    }

                    if (stability.isStable(chunk.getPos())) {
                        continue;
                    }

                    AenderVolatility.retainForChunk(chunk);

                    if (AenderVolatility.needsRegeneration(chunk)) {
                        AenderLoadedChunkReplacement.regenerate(level, chunk);
                        regenerated++;
                    }
                }
            }
        }

        if (loaded > 0 || regenerated > 0) {
            Retold.LOGGER.debug(
                    "Prepared Aender arrival core around {}, {} in {} ms: loaded {} chunks and regenerated {} chunks",
                    centerChunkX,
                    centerChunkZ,
                    (System.nanoTime() - startedAtNanos) / 1_000_000L,
                    loaded,
                    regenerated
            );
        }
    }

    /**
     * Refreshes one chunk only if Minecraft's asynchronous ticket pipeline has
     * already made it available. This never blocks waiting for generation and is
     * therefore safe to call in small batches during a portal charge.
     */
    public static boolean prepareLoadedArrivalChunk(ServerLevel level, int chunkX, int chunkZ) {
        if (level.dimension() != RetoldAenderDimensions.AENDER) {
            return false;
        }

        ChunkAccess chunk = getLoadedChunk(level, chunkX, chunkZ);

        if (chunk == null) {
            return false;
        }

        AenderStabilityData stability = AenderStabilityData.get(level);

        if (stability.isStable(chunk.getPos())) {
            return true;
        }

        AenderVolatility.retainForChunk(chunk);

        if (shouldRegenerate(stability, chunk)) {
            AenderLoadedChunkReplacement.regenerate(level, chunk);
        }

        return true;
    }

    /**
     * Makes an already-loaded destination chunk safe to reveal without doing
     * the expensive regeneration step. Unloaded chunks are also safe because
     * their load callback applies the same blank-before-queue rule before they
     * can be tracked by a client.
     */
    public static boolean prepareArrivalVisibilityChunk(ServerLevel level, int chunkX, int chunkZ) {
        if (level.dimension() != RetoldAenderDimensions.AENDER) {
            return false;
        }

        ChunkAccess chunk = getLoadedChunk(level, chunkX, chunkZ);

        if (chunk == null) {
            return true;
        }

        AenderStabilityData stability = AenderStabilityData.get(level);

        if (stability.isStable(chunk.getPos())) {
            return true;
        }

        AenderVolatility.retainForChunk(chunk);

        if (AenderVolatility.needsRegeneration(chunk)) {
            blankLoadedStaleChunk(level, chunk);
        }

        return true;
    }

    public static void blankLoadedStaleChunk(ServerLevel level, ChunkAccess chunk) {
        AenderLoadedChunkReplacement.blankForProgressiveRegeneration(level, chunk);
        BLANK_QUEUED.remove(pack(chunk.getPos().x(), chunk.getPos().z()));
        enqueueIfNeeded(level, chunk);
    }

    /**
     * Keeps the stability exclusion explicit and independently testable from
     * the asynchronous arrival-ticket lifecycle.
     */
    public static boolean shouldRegenerate(
            AenderStabilityData stability,
            ChunkAccess chunk
    ) {
        return !stability.isStable(chunk.getPos())
                && AenderVolatility.needsRegeneration(chunk);
    }

    public static long adaptiveRegenerationBudget(ServerLevel level, long maximumBudgetNanos) {
        return AenderRegenerationBudget.calculate(
                level.getServer().getAverageTickTimeNanos(),
                level.getServer().tickRateManager().nanosecondsPerTick(),
                maximumBudgetNanos
        );
    }

    private static void queueNearbyStaleChunks(ServerLevel level) {
        AenderStabilityData stability = AenderStabilityData.get(level);
        int radius = regenerationRadius(level);
        Set<Long> visited = new HashSet<>();

        for (ServerPlayer player : level.players()) {
            BlockPos playerPos = player.blockPosition();

            int centerChunkX = playerPos.getX() >> 4;
            int centerChunkZ = playerPos.getZ() >> 4;

            for (int ring = 0; ring <= radius; ring++) {
                for (int dx = -ring; dx <= ring; dx++) {
                    for (int dz = -ring; dz <= ring; dz++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) {
                            continue;
                        }

                        int chunkX = centerChunkX + dx;
                        int chunkZ = centerChunkZ + dz;
                        long key = pack(chunkX, chunkZ);

                        if (!visited.add(key)) {
                            continue;
                        }

                        ChunkAccess chunk = getLoadedChunk(level, chunkX, chunkZ);

                        if (chunk == null) {
                            continue;
                        }

                        if (stability.isStable(chunk.getPos())) {
                            continue;
                        }

                        AenderVolatility.retainForChunk(chunk);

                        if (!AenderVolatility.needsRegeneration(chunk)) {
                            continue;
                        }

                        enqueue(chunkX, chunkZ);
                    }
                }
            }
        }
    }

    private static void updatePlayerLoadedRegions(ServerLevel level) {
        boolean hasPlayers = !level.players().isEmpty();
        Set<Long> currentlyActive = hasPlayers ? collectPlayerLoadedRegions(level) : Set.of();
        Set<Long> departed = ACTIVE_REGIONS.update(currentlyActive, hasPlayers);

        if (departed.isEmpty()) {
            return;
        }

        for (long key : departed) {
            AenderVolatility.advanceRegion(level, unpackX(key), unpackZ(key));
        }

        int queuedForBlanking = queueUnwatchedStaleChunksForBlanking(level);
        Retold.LOGGER.debug(
                "Advanced {} unattended Aender region columns and queued {} loaded stale chunks for blanking",
                departed.size(),
                queuedForBlanking
        );
    }

    private static Set<Long> collectPlayerLoadedRegions(ServerLevel level) {
        Set<Long> result = new HashSet<>();

        for (ChunkPos pos : AenderVolatility.retainedChunkPositions()) {
            if (level.getChunkSource().chunkMap.getPlayers(pos, false).isEmpty()) {
                continue;
            }

            int minRegionX = Math.floorDiv(pos.getMinBlockX(), AenderIslandSampler.REGION_SIZE) - 1;
            int maxRegionX = Math.floorDiv(pos.getMaxBlockX(), AenderIslandSampler.REGION_SIZE) + 1;
            int minRegionZ = Math.floorDiv(pos.getMinBlockZ(), AenderIslandSampler.REGION_SIZE) - 1;
            int maxRegionZ = Math.floorDiv(pos.getMaxBlockZ(), AenderIslandSampler.REGION_SIZE) + 1;

            for (int regionX = minRegionX; regionX <= maxRegionX; regionX++) {
                for (int regionZ = minRegionZ; regionZ <= maxRegionZ; regionZ++) {
                    result.add(pack(regionX, regionZ));
                }
            }
        }

        return result;
    }

    private static int queueUnwatchedStaleChunksForBlanking(ServerLevel level) {
        AenderStabilityData stability = AenderStabilityData.get(level);
        int queued = 0;

        for (ChunkPos pos : AenderVolatility.retainedChunkPositions()) {
            if (!level.getChunkSource().chunkMap.getPlayers(pos, false).isEmpty() || stability.isStable(pos)) {
                continue;
            }

            ChunkAccess chunk = getLoadedChunk(level, pos.x(), pos.z());

            if (chunk == null || !AenderVolatility.needsRegeneration(chunk)) {
                continue;
            }

            if (enqueueBlank(pos.x(), pos.z())) {
                queued++;
            }
        }

        return queued;
    }

    private static int processBlankQueue(ServerLevel level) {
        long gameTime = level.getGameTime();

        if (gameTime < nextBlankGameTime || BLANK_QUEUE.isEmpty()) {
            return 0;
        }

        long workBudgetNanos = adaptiveRegenerationBudget(level, MAX_BLANK_NANOS_PER_TICK);

        if (workBudgetNanos <= 0L) {
            nextBlankGameTime = gameTime + OVERLOADED_RETRY_TICKS;
            return 0;
        }

        int blanked = 0;
        long startedAt = System.nanoTime();
        AenderStabilityData stability = AenderStabilityData.get(level);

        while (blanked < MAX_BLANK_CHUNKS_PER_TICK && !BLANK_QUEUE.isEmpty()) {
            if (System.nanoTime() - startedAt >= workBudgetNanos) {
                break;
            }

            long key = BLANK_QUEUE.poll();

            if (!BLANK_QUEUED.remove(key)) {
                continue;
            }

            ChunkAccess chunk = getLoadedChunk(level, unpackX(key), unpackZ(key));

            if (chunk == null || stability.isStable(chunk.getPos())) {
                continue;
            }

            AenderVolatility.retainForChunk(chunk);

            if (!AenderVolatility.needsRegeneration(chunk)) {
                continue;
            }

            AenderLoadedChunkReplacement.blankForProgressiveRegeneration(level, chunk);
            blanked++;
        }

        if (blanked > 0) {
            nextBlankGameTime = gameTime + 1L;
        }

        return blanked;
    }

    private static void processRegenQueue(ServerLevel level) {
        long gameTime = level.getGameTime();

        if (gameTime < nextRegenGameTime) {
            return;
        }

        long workBudgetNanos = adaptiveRegenerationBudget(level, MAX_REGEN_NANOS_PER_TICK);

        if (workBudgetNanos <= 0L) {
            nextRegenGameTime = gameTime + OVERLOADED_RETRY_TICKS;
            return;
        }

        int regenerated = 0;
        long startNanos = System.nanoTime();

        while (regenerated < MAX_REGEN_CHUNKS_PER_TICK && !REGEN_QUEUE.isEmpty()) {
            long elapsedNanos = System.nanoTime() - startNanos;

            if (elapsedNanos >= workBudgetNanos
                    || regenerated > 0 && elapsedNanos + estimatedRegenNanos > workBudgetNanos) {
                break;
            }

            long key = REGEN_QUEUE.poll();
            QUEUED.remove(key);

            int chunkX = unpackX(key);
            int chunkZ = unpackZ(key);

            ChunkAccess chunk = getLoadedChunk(level, chunkX, chunkZ);

            if (chunk == null) {
                continue;
            }

            if (AenderStabilityData.get(level).isStable(chunk.getPos())) {
                continue;
            }

            if (!AenderVolatility.needsRegeneration(chunk)) {
                continue;
            }

            long chunkStartedAt = System.nanoTime();
            AenderLoadedChunkReplacement.regenerate(level, chunk);
            long chunkNanos = System.nanoTime() - chunkStartedAt;
            estimatedRegenNanos = (estimatedRegenNanos * 3L + chunkNanos) / 4L;
            regenerated++;
        }

        if (regenerated > 0) {
            nextRegenGameTime = gameTime + 1L;
        }
    }

    private static ChunkAccess getLoadedChunk(ServerLevel level, int chunkX, int chunkZ) {
        return level.getChunkSource().getChunkNow(chunkX, chunkZ);
    }

    private static void enqueue(int chunkX, int chunkZ) {
        if (REGEN_QUEUE.size() >= MAX_QUEUE_SIZE) {
            return;
        }

        long key = pack(chunkX, chunkZ);

        if (QUEUED.add(key)) {
            REGEN_QUEUE.add(key);
        }
    }

    private static boolean enqueueBlank(int chunkX, int chunkZ) {
        if (BLANK_QUEUE.size() >= MAX_QUEUE_SIZE) {
            return false;
        }

        long key = pack(chunkX, chunkZ);

        if (!BLANK_QUEUED.add(key)) {
            return false;
        }

        BLANK_QUEUE.add(key);
        return true;
    }

    private static int regenerationRadius(ServerLevel level) {
        return level.getServer().getPlayerList().getViewDistance() + EXTRA_REGEN_RADIUS_CHUNKS;
    }

    public static int arrivalPreparationRadius(ServerLevel level) {
        return level.getServer().getPlayerList().getViewDistance();
    }

    private static long pack(int x, int z) {
        return ((long) x & 0xffffffffL) | (((long) z & 0xffffffffL) << 32);
    }

    private static int unpackX(long key) {
        return (int) key;
    }

    private static int unpackZ(long key) {
        return (int) (key >> 32);
    }
}
