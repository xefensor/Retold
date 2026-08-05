package cz.xefensor.retold.villager;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

public final class RetoldVillageReputationEvents {
    private RetoldVillageReputationEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBreakVillageProperty(BreakBlockEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)
                || !(event.getLevel() instanceof ServerLevel level)
                || event.isCanceled()) {
            return;
        }

        var block = event.getState().getBlock();

        if (block instanceof CropBlock) {
            RetoldVillageCropOwnership.handlePlayerBreak(
                    level,
                    event.getPos(),
                    event.getState(),
                    player
            );
            return;
        }

        if (block instanceof ChestBlock || block instanceof BarrelBlock) {
            RetoldVillageContainerOwnership.handleProtectedContainerBreak(
                    level,
                    event.getPos(),
                    player
            );
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerPlaceCrop(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || event.isCanceled()) {
            return;
        }

        RetoldVillageCropOwnership.handlePlayerPlacement(
                level,
                event.getPos(),
                event.getPlacedBlock(),
                event.getEntity()
        );
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onFarmlandTrample(
            BlockEvent.FarmlandTrampleEvent event
    ) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || event.isCanceled()) {
            return;
        }

        RetoldVillageCropOwnership.handleFarmlandTrample(
                level,
                event.getPos(),
                event.getEntity()
        );
    }
}
