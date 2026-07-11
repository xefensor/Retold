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

        if (!RetoldTerritoryRules.canUseNearbyTerritoryBehavior(
                level,
                mob,
                config,
                gameTime
        )) {
            RetoldTerritoryMobStates.clearMobState(mob);
            return;
        }

        RetoldTerritoryMobState state = RetoldTerritoryMobStates.getOrCreate(mob);

        RetoldTerritoryCombat.suppressExistingTargetDuringWarning(
                level,
                mob,
                config,
                RetoldTerritoryMobStates.states(),
                gameTime
        );

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
