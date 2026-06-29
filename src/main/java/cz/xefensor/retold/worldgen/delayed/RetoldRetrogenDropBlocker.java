package cz.xefensor.retold.worldgen.delayed;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class RetoldRetrogenDropBlocker {
    private static final Map<Long, Integer> ACTIVE_CHUNKS =
            new ConcurrentHashMap<>();

    private RetoldRetrogenDropBlocker() {
    }

    public static void beginChunk(ChunkPos pos) {
        long key = packChunk(pos.x(), pos.z());
        ACTIVE_CHUNKS.merge(key, 1, Integer::sum);
    }

    public static void endChunk(ChunkPos pos) {
        long key = packChunk(pos.x(), pos.z());

        int count = ACTIVE_CHUNKS.getOrDefault(key, 0) - 1;

        if (count <= 0) {
            ACTIVE_CHUNKS.remove(key);
        } else {
            ACTIVE_CHUNKS.put(key, count);
        }
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        if (level.dimension() != Level.OVERWORLD) {
            return;
        }

        Entity entity = event.getEntity();

        if (!isItemEntity(entity)) {
            return;
        }

        int chunkX = ((int) Math.floor(entity.getX())) >> 4;
        int chunkZ = ((int) Math.floor(entity.getZ())) >> 4;

        long key = packChunk(chunkX, chunkZ);

        if (!ACTIVE_CHUNKS.containsKey(key)) {
            return;
        }

        event.setCanceled(true);
    }

    private static boolean isItemEntity(Entity entity) {
        return "entity.minecraft.item".equals(entity.getType().getDescriptionId());
    }

    private static long packChunk(int chunkX, int chunkZ) {
        return ((long) chunkX & 4294967295L)
                | (((long) chunkZ & 4294967295L) << 32);
    }
}