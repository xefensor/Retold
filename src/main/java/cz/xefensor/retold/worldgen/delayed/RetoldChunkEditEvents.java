package cz.xefensor.retold.worldgen.delayed;

import cz.xefensor.retold.Retold;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

public final class RetoldChunkEditEvents {
    private RetoldChunkEditEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBlockBreak(BreakBlockEvent event) {
        if (event.isCanceled()) {
            return;
        }

        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        if (event.getPlayer() == null) {
            return;
        }

        countPlayerEdit(level, event.getPos());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.isCanceled()) {
            return;
        }

        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        Entity entity = event.getEntity();

        if (!(entity instanceof Player)) {
            return;
        }

        countPlayerEdit(level, event.getPos());
    }

    private static void countPlayerEdit(ServerLevel level, BlockPos pos) {
        ChunkAccess chunk = level.getChunk(pos);

        RetoldChunkStructureData oldData =
                chunk.getData(RetoldAttachments.CHUNK_STRUCTURE_DATA.get());

        RetoldChunkStructureData newData = oldData.withPlayerEdit();

        if (newData != oldData) {
            Retold.LOGGER.info(
                    "Player edit counted in chunk [{}, {}], now {} / {}",
                    chunk.getPos().x(),
                    chunk.getPos().z(),
                    newData.playerEditCount(),
                    RetoldChunkStructureData.EDITED_THRESHOLD
            );
            chunk.setData(RetoldAttachments.CHUNK_STRUCTURE_DATA.get(), newData);
        }
    }
}