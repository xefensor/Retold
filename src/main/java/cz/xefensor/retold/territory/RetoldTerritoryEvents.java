package cz.xefensor.retold.territory;

import cz.xefensor.retold.faction.RetoldFaction;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public final class RetoldTerritoryEvents {
    private RetoldTerritoryEvents() {
    }

    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        RetoldTerritoryTick.tickEntity(event.getEntity());
    }

    public static boolean shouldBlockTargetDuringWarning(
            PathfinderMob mob,
            LivingEntity target
    ) {
        return RetoldTerritoryTargetBlocker.shouldBlockTargetDuringWarning(
                mob,
                target
        );
    }

    public static RetoldTerritoryContext getTerritoryContextAt(
            ServerLevel level,
            BlockPos pos
    ) {
        return RetoldTerritoryDetector.getContextAt(level, pos);
    }

    public static RetoldFaction getTerritoryFactionAt(
            ServerLevel level,
            BlockPos pos
    ) {
        return RetoldTerritoryDetector.getFactionAt(level, pos);
    }

    public static boolean hasWitnessForIllegalAction(
            ServerLevel level,
            RetoldFaction faction,
            ServerPlayer player,
            BlockPos actionPos
    ) {
        return RetoldTerritoryWitnesses.hasWitnessForIllegalAction(
                level,
                faction,
                player,
                actionPos,
                RetoldTerritoryConstants.ILLEGAL_ACTION_WITNESS_RADIUS_BLOCKS
        );
    }

    public static void alertWitnessesAfterIllegalAction(
            ServerLevel level,
            RetoldFaction faction,
            ServerPlayer player,
            BlockPos actionPos
    ) {
        RetoldTerritoryWitnesses.alertWitnessesAfterIllegalAction(
                level,
                faction,
                player,
                actionPos,
                RetoldTerritoryMobStates.states(),
                RetoldTerritoryConstants.ILLEGAL_ACTION_WITNESS_RADIUS_BLOCKS
        );
    }
}