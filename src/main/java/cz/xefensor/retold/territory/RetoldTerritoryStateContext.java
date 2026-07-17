package cz.xefensor.retold.territory;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;

import java.util.Map;

record RetoldTerritoryStateContext(
        ServerLevel level,
        PathfinderMob mob,
        RetoldTerritoryMobState state,
        RetoldTerritoryConfig config,
        Map<PathfinderMob, RetoldTerritoryMobState> mobStates,
        long gameTime,
        boolean decisionTick
) {
}
