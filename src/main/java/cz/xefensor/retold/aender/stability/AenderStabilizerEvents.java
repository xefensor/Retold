package cz.xefensor.retold.aender.stability;

import cz.xefensor.retold.aender.RetoldAenderDimensions;
import cz.xefensor.retold.registry.RetoldBlocks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.minecraft.core.BlockPos;

public final class AenderStabilizerEvents {
    private AenderStabilizerEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.isCanceled()) {
            return;
        }

        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        if (level.dimension() != RetoldAenderDimensions.AENDER) {
            return;
        }

        if (!event.getPlacedBlock().is(RetoldBlocks.AENDER_STABILIZER)) {
            return;
        }

        AenderStabilityData.get(level).addStabilizer(chunkOf(event.getPos()));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBreak(BreakBlockEvent event) {
        if (event.isCanceled()) {
            return;
        }

        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        if (level.dimension() != RetoldAenderDimensions.AENDER) {
            return;
        }

        if (!level.getBlockState(event.getPos()).is(RetoldBlocks.AENDER_STABILIZER)) {
            return;
        }

        AenderStabilityData.get(level).removeStabilizer(chunkOf(event.getPos()));
    }

    private static ChunkPos chunkOf(BlockPos pos) {
        return new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4);
    }
}