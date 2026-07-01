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
    private static final Queue<RegenerationTask> REGENERATION_QUEUE =
            new ArrayDeque<>();

    private static final Set<Long> QUEUED_CHUNKS =
            new HashSet<>();

    private static final Map<Long, Integer> CHUNK_WATCHERS =
            new HashMap<>();

    private static final Map<Long, Integer> REGION_WATCHERS =
            new HashMap<>();

    private static final int REGION_SIZE_CHUNKS = 8;
    private static final int CHUNKS_PER_TICK = 1;
    private static final int MAX_REGENERATION_QUEUE_SIZE = 2048;

    private static long regionSequence = 0L;

    private RetoldAenderUnstableChunkEvents() {
    }

    @SubscribeEvent
    public static void onChunkSent(ChunkWatchEvent.Sent event) {
        ServerLevel level = event.getLevel();

        if (!RetoldAenderChunkStability.isAender(level)) {
            return;
        }

        ChunkPos pos = event.getPos();

        CHUNK_WATCHERS.merge(
                packChunk(pos.x(), pos.z()),
                1,
                Integer::sum
        );

        REGION_WATCHERS.merge(
                getRegionKey(pos),
                1,
                Integer::sum
        );
    }

    @SubscribeEvent
    public static void onChunkUnwatch(ChunkWatchEvent.UnWatch event) {
        ServerLevel level = event.getLevel();

        if (!RetoldAenderChunkStability.isAender(level)) {
            return;
        }

        ChunkPos pos = event.getPos();

        decrementChunkWatchers(pos);

        long regionKey = getRegionKey(pos);
        int regionWatchers =
                REGION_WATCHERS.getOrDefault(regionKey, 0) - 1;

        if (regionWatchers <= 0) {
            REGION_WATCHERS.remove(regionKey);

            RegionPos regionPos =
                    getRegionPos(pos);

            scheduleRegionRegeneration(
                    level,
                    regionPos.regionX(),
                    regionPos.regionZ()
            );
        } else {
            REGION_WATCHERS.put(regionKey, regionWatchers);
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

        catchUpChunkToRegionVersion(level, chunk);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ServerLevel aenderLevel =
                event.getServer().getLevel(RetoldAenderDimensions.AENDER);

        if (aenderLevel == null) {
            return;
        }

        for (int i = 0; i < CHUNKS_PER_TICK && !REGENERATION_QUEUE.isEmpty(); i++) {
            RegenerationTask task = REGENERATION_QUEUE.poll();
            ChunkPos pos = task.pos();

            QUEUED_CHUNKS.remove(packChunk(pos.x(), pos.z()));

            if (!aenderLevel.hasChunk(pos.x(), pos.z())) {
                continue;
            }

            if (isChunkWatched(pos)) {
                continue;
            }

            if (hasPlayerInsideChunk(aenderLevel, pos)) {
                continue;
            }

            regenerateIfStillOutdated(aenderLevel, task);
        }
    }

    private static void decrementChunkWatchers(ChunkPos pos) {
        long key = packChunk(pos.x(), pos.z());
        int watchers = CHUNK_WATCHERS.getOrDefault(key, 0) - 1;

        if (watchers <= 0) {
            CHUNK_WATCHERS.remove(key);
        } else {
            CHUNK_WATCHERS.put(key, watchers);
        }
    }

    private static void scheduleRegionRegeneration(
            ServerLevel level,
            int regionX,
            int regionZ
    ) {
        long salt =
                createRegionMutationSalt(
                        level,
                        regionX,
                        regionZ
                );

        RetoldAenderRegionData regionData =
                RetoldAenderRegionData.get(level);

        int targetVersion =
                regionData.bumpRegionToSalt(
                        regionX,
                        regionZ,
                        salt
                );

        int minChunkX = regionX * REGION_SIZE_CHUNKS;
        int minChunkZ = regionZ * REGION_SIZE_CHUNKS;

        int scheduled = 0;

        for (int offsetX = 0; offsetX < REGION_SIZE_CHUNKS; offsetX++) {
            for (int offsetZ = 0; offsetZ < REGION_SIZE_CHUNKS; offsetZ++) {
                int chunkX = minChunkX + offsetX;
                int chunkZ = minChunkZ + offsetZ;

                if (!level.hasChunk(chunkX, chunkZ)) {
                    continue;
                }

                ChunkPos pos =
                        new ChunkPos(chunkX, chunkZ);

                ChunkAccess chunk =
                        level.getChunk(chunkX, chunkZ);

                if (scheduleChunkForRegionVersion(
                        level,
                        chunk,
                        salt,
                        targetVersion
                )) {
                    scheduled++;
                }
            }
        }

        if (scheduled > 0) {
            Retold.LOGGER.info(
                    "Scheduled Aender region [{}, {}] version {} with {} loaded chunks",
                    regionX,
                    regionZ,
                    targetVersion,
                    scheduled
            );
        }
    }

    private static void catchUpChunkToRegionVersion(
            ServerLevel level,
            ChunkAccess chunk
    ) {
        ChunkPos pos = chunk.getPos();
        RegionPos regionPos = getRegionPos(pos);

        RetoldAenderRegionData regionData =
                RetoldAenderRegionData.get(level);

        int targetVersion =
                regionData.getVersion(
                        regionPos.regionX(),
                        regionPos.regionZ()
                );

        if (targetVersion <= 0) {
            return;
        }

        long salt =
                regionData.getSalt(
                        regionPos.regionX(),
                        regionPos.regionZ()
                );

        scheduleChunkForRegionVersion(
                level,
                chunk,
                salt,
                targetVersion
        );
    }

    private static boolean scheduleChunkForRegionVersion(
            ServerLevel level,
            ChunkAccess chunk,
            long salt,
            int targetVersion
    ) {
        ChunkPos pos = chunk.getPos();

        if (isChunkWatched(pos)) {
            return false;
        }

        if (hasPlayerInsideChunk(level, pos)) {
            return false;
        }

        RetoldAenderChunkData data =
                chunk.getData(RetoldAenderAttachments.CHUNK_DATA.get());

        if (data.isStabilized()) {
            return false;
        }

        if (data.appliedRegionVersion() >= targetVersion) {
            return false;
        }

        long key = packChunk(pos.x(), pos.z());

        if (!QUEUED_CHUNKS.add(key)) {
            return false;
        }

        RetoldAenderChunkStability.scheduleRegenerationOnNextLoad(
                level,
                chunk,
                salt,
                targetVersion
        );

        RegionPos regionPos = getRegionPos(pos);

        REGENERATION_QUEUE.add(
                new RegenerationTask(
                        pos,
                        salt,
                        targetVersion,
                        regionPos.regionX(),
                        regionPos.regionZ()
                )
        );

        return true;
    }

    private static void regenerateIfStillOutdated(
            ServerLevel level,
            RegenerationTask task
    ) {
        ChunkPos pos = task.pos();

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

        if (data.regenerationRegionVersion() != task.targetVersion()) {
            return;
        }

        RetoldAenderTerrainBuilder.regenerateFloatingIslands(
                chunk,
                level.getSeed(),
                task.salt(),
                getTerrainConstraints(level, task)
        );

        RetoldAenderChunkStability.markRegenerationFinished(level, chunk);

        Retold.LOGGER.info(
                "Regenerated Aender chunk [{}, {}] to region version {}",
                pos.x(),
                pos.z(),
                task.targetVersion()
        );
    }

    private static RetoldAenderTerrainConstraints getTerrainConstraints(
            ServerLevel level,
            RegenerationTask task
    ) {
        ChunkPos pos = task.pos();

        return new RetoldAenderTerrainConstraints(
                shouldConstrainToNeighbor(
                        level,
                        task,
                        new ChunkPos(pos.x(), pos.z() - 1)
                ),
                shouldConstrainToNeighbor(
                        level,
                        task,
                        new ChunkPos(pos.x(), pos.z() + 1)
                ),
                shouldConstrainToNeighbor(
                        level,
                        task,
                        new ChunkPos(pos.x() - 1, pos.z())
                ),
                shouldConstrainToNeighbor(
                        level,
                        task,
                        new ChunkPos(pos.x() + 1, pos.z())
                )
        );
    }

    private static boolean shouldConstrainToNeighbor(
            ServerLevel level,
            RegenerationTask task,
            ChunkPos neighborPos
    ) {
        if (isChunkWatched(neighborPos)) {
            return true;
        }

        if (!level.hasChunk(neighborPos.x(), neighborPos.z())) {
            return false;
        }

        ChunkAccess neighborChunk =
                level.getChunk(neighborPos.x(), neighborPos.z());

        RetoldAenderChunkData neighborData =
                neighborChunk.getData(RetoldAenderAttachments.CHUNK_DATA.get());

        if (neighborData.isStabilized()) {
            return true;
        }

        RegionPos neighborRegion =
                getRegionPos(neighborPos);

        if (neighborRegion.regionX() == task.regionX()
                && neighborRegion.regionZ() == task.regionZ()) {
            return false;
        }

        return false;
    }

    private static boolean isChunkWatched(ChunkPos pos) {
        return CHUNK_WATCHERS.containsKey(
                packChunk(pos.x(), pos.z())
        );
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

    private static long createRegionMutationSalt(
            ServerLevel level,
            int regionX,
            int regionZ
    ) {
        regionSequence++;

        long value = level.getSeed();
        value ^= packChunk(regionX, regionZ);
        value ^= regionSequence * 0x9E3779B97F4A7C15L;
        value ^= 0xD71A8E5B4C2F9031L;

        long salt = mix64(value);

        if (salt == 0L) {
            return 1L;
        }

        return salt;
    }

    private static RegionPos getRegionPos(ChunkPos pos) {
        return new RegionPos(
                Math.floorDiv(pos.x(), REGION_SIZE_CHUNKS),
                Math.floorDiv(pos.z(), REGION_SIZE_CHUNKS)
        );
    }

    private static long getRegionKey(ChunkPos pos) {
        RegionPos regionPos =
                getRegionPos(pos);

        return packChunk(
                regionPos.regionX(),
                regionPos.regionZ()
        );
    }

    private static long mix64(long value) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= value >>> 33;

        return value;
    }

    private static long packChunk(int chunkX, int chunkZ) {
        return ((long) chunkX & 4294967295L)
                | (((long) chunkZ & 4294967295L) << 32);
    }

    private record RegionPos(
            int regionX,
            int regionZ
    ) {
    }

    private record RegenerationTask(
            ChunkPos pos,
            long salt,
            int targetVersion,
            int regionX,
            int regionZ
    ) {
    }
}