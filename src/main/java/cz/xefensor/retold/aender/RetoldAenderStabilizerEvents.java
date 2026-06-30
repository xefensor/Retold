package cz.xefensor.retold.aender;

import cz.xefensor.retold.registry.RetoldBlocks;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

public final class RetoldAenderStabilizerEvents {
    private RetoldAenderStabilizerEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.isCanceled()) {
            return;
        }

        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        if (!RetoldAenderChunkStability.isAender(level)) {
            return;
        }

        if (!event.getPlacedBlock().is(RetoldBlocks.AENDER_STABILIZER)) {
            return;
        }

        RetoldAenderChunkStability.addStabilizer(level, event.getPos());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBlockBreak(BreakBlockEvent event) {
        if (event.isCanceled()) {
            return;
        }

        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        if (!RetoldAenderChunkStability.isAender(level)) {
            return;
        }

        if (!level.getBlockState(event.getPos()).is(RetoldBlocks.AENDER_STABILIZER)) {
            return;
        }

        RetoldAenderChunkStability.removeStabilizer(level, event.getPos());
    }
}