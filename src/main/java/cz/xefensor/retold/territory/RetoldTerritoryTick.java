package cz.xefensor.retold.territory;

import cz.xefensor.retold.combat.RetoldFactionTargetMemory;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;

public final class RetoldTerritoryTick {
    private static final int MOB_DECISION_INTERVAL_TICKS = 20;

    private RetoldTerritoryTick() {
    }

    public static void tickEntity(Entity entity) {
        if (!(entity instanceof PathfinderMob mob)) {
            return;
        }

        if (!(mob.level() instanceof ServerLevel level)) {
            return;
        }

        RetoldTerritoryConfig config = RetoldTerritoryConfigs.getForEntity(mob);

        if (config == null) {
            RetoldTerritoryMobStates.remove(mob);
            return;
        }

        long gameTime = level.getGameTime();

        RetoldFactionTargetMemory.cleanupTargetState(mob);

        if (!RetoldTerritoryRules.canUseTerritoryBehavior(level, mob, config)) {
            RetoldTerritoryMobStates.clearMobState(mob);
            return;
        }

        RetoldTerritoryMobState state = RetoldTerritoryMobStates.get(mob);

        if (!RetoldTerritoryRules.canUseNearbyTerritoryBehavior(
                level,
                mob,
                config,
                gameTime
        )) {
            if (state != null && state.flowState.continuesOutsideTerritory()) {
                tickStateMachine(level, mob, state, config, gameTime);
                return;
            }

            RetoldTerritoryMobStates.clearMobState(mob);
            return;
        }

        if (state == null) {
            state = RetoldTerritoryMobStates.getOrCreate(mob);
        }

        tickStateMachine(level, mob, state, config, gameTime);
    }

    private static void tickStateMachine(
            ServerLevel level,
            PathfinderMob mob,
            RetoldTerritoryMobState state,
            RetoldTerritoryConfig config,
            long gameTime
    ) {
        if (gameTime % MOB_DECISION_INTERVAL_TICKS != Math.floorMod(mob.getId(), MOB_DECISION_INTERVAL_TICKS)) {
            RetoldTerritoryController.maintainContinuousBehavior(
                    level,
                    mob,
                    state,
                    config,
                    RetoldTerritoryMobStates.states(),
                    gameTime
            );

            return;
        }

        RetoldTerritoryController.updateMobTerritoryLogic(
                level,
                mob,
                state,
                config,
                RetoldTerritoryMobStates.states(),
                gameTime
        );
    }
}
