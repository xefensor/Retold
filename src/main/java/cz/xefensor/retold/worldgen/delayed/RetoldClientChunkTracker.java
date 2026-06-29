package cz.xefensor.retold.worldgen.delayed;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;

import java.util.HashMap;
import java.util.Map;

public final class RetoldClientChunkTracker {
    private static final Map<Long, Integer> SENT_CHUNK_WATCHERS = new HashMap<>();

    private RetoldClientChunkTracker() {
    }

    @SubscribeEvent
    public static void onChunkSent(ChunkWatchEvent.Sent event) {
        ServerLevel level = event.getLevel();

        if (level.dimension() != Level.OVERWORLD) {
            return;
        }

        ChunkPos pos = event.getPos();
        long key = packChunk(pos.x(), pos.z());

        SENT_CHUNK_WATCHERS.merge(key, 1, Integer::sum);
    }

    @SubscribeEvent
    public static void onChunkUnwatch(ChunkWatchEvent.UnWatch event) {
        ServerLevel level = event.getLevel();

        if (level.dimension() != Level.OVERWORLD) {
            return;
        }

        ChunkPos pos = event.getPos();
        long key = packChunk(pos.x(), pos.z());

        int watchers = SENT_CHUNK_WATCHERS.getOrDefault(key, 0) - 1;

        if (watchers <= 0) {
            SENT_CHUNK_WATCHERS.remove(key);
        } else {
            SENT_CHUNK_WATCHERS.put(key, watchers);
        }

        // Important:
        // When the player stops watching the chunk, this is the best moment
        // to retry retrogen without visible pop-in.
        RetoldDelayedStructureRetrogen.enqueueForPossibleRetrogen(level, pos);
    }

    public static boolean isSentToAnyPlayer(ChunkPos pos) {
        return SENT_CHUNK_WATCHERS.containsKey(packChunk(pos.x(), pos.z()));
    }

    private static long packChunk(int chunkX, int chunkZ) {
        return ((long) chunkX & 4294967295L)
                | (((long) chunkZ & 4294967295L) << 32);
    }
}