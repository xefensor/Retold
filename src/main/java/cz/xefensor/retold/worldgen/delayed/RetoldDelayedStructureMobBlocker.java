package cz.xefensor.retold.worldgen.delayed;

import cz.xefensor.retold.Retold;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class RetoldDelayedStructureMobBlocker {
    private static final Set<Long> DEFERRED_OUTPOST_CHUNKS =
            ConcurrentHashMap.newKeySet();

    // Outpost spawn area can extend outside the exact start chunk.
    // 6 chunks = 96 blocks radius, intentionally a bit generous.
    private static final int OUTPOST_BLOCK_RADIUS_CHUNKS = 6;

    private RetoldDelayedStructureMobBlocker() {
    }

    public static void rememberDeferredStructure(
            String structureId,
            ChunkPos pos
    ) {
        if (!RetoldDelayedStructureIds.PILLAGER_OUTPOST.equals(structureId)) {
            return;
        }

        rememberDeferredOutpostChunk(pos);
    }

    public static void rememberDeferredOutpostChunk(ChunkPos pos) {
        DEFERRED_OUTPOST_CHUNKS.add(packChunk(pos.x(), pos.z()));
    }

    public static void forgetDeferredStructure(
            String structureId,
            ChunkPos pos
    ) {
        if (!RetoldDelayedStructureIds.PILLAGER_OUTPOST.equals(structureId)) {
            return;
        }

        DEFERRED_OUTPOST_CHUNKS.remove(packChunk(pos.x(), pos.z()));
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        if (level.dimension() != Level.OVERWORLD) {
            return;
        }

        ChunkAccess chunk = event.getChunk();

        RetoldChunkStructureData data =
                chunk.getData(RetoldAttachments.CHUNK_STRUCTURE_DATA.get());

        if (data.hasDeferred(RetoldDelayedStructureIds.PILLAGER_OUTPOST)
                || data.hasMobSuppressed(RetoldDelayedStructureIds.PILLAGER_OUTPOST)) {
            rememberDeferredOutpostChunk(chunk.getPos());
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

        if (!isPillager(entity)) {
            return;
        }

        int entityChunkX = ((int) Math.floor(entity.getX())) >> 4;
        int entityChunkZ = ((int) Math.floor(entity.getZ())) >> 4;

        if (!isNearDeferredOutpost(entityChunkX, entityChunkZ)) {
            return;
        }

        event.setCanceled(true);

        Retold.LOGGER.debug(
                "Blocked Stage 1 pillager spawn near deferred outpost at chunk [{}, {}]",
                entityChunkX,
                entityChunkZ
        );
    }

    private static boolean isPillager(Entity entity) {
        return "entity.minecraft.pillager".equals(entity.getType().getDescriptionId());
    }

    private static boolean isNearDeferredOutpost(
            int chunkX,
            int chunkZ
    ) {
        for (long packed : DEFERRED_OUTPOST_CHUNKS) {
            int outpostChunkX = unpackChunkX(packed);
            int outpostChunkZ = unpackChunkZ(packed);

            if (Math.abs(chunkX - outpostChunkX) <= OUTPOST_BLOCK_RADIUS_CHUNKS
                    && Math.abs(chunkZ - outpostChunkZ) <= OUTPOST_BLOCK_RADIUS_CHUNKS) {
                return true;
            }
        }

        return false;
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
}