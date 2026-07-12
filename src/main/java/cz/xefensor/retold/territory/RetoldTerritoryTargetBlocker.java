package cz.xefensor.retold.territory;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;

public final class RetoldTerritoryTargetBlocker {
    private RetoldTerritoryTargetBlocker() {
    }

    public static boolean shouldBlockTargetDuringWarning(
            PathfinderMob mob,
            LivingEntity target
    ) {
        if (target == null) {
            return false;
        }

        if (mob.level().isClientSide()) {
            return false;
        }

        if (!(mob.level() instanceof ServerLevel level)) {
            return false;
        }

        RetoldTerritoryConfig config = RetoldTerritoryConfigs.getForEntity(mob);

        if (config == null) {
            return false;
        }

        long gameTime = level.getGameTime();

        if (!RetoldTerritoryRules.canUseNearbyTerritoryBehavior(
                level,
                mob,
                config,
                gameTime
        )) {
            return false;
        }

        if (!RetoldTerritoryTargetSelector.isPossibleIntruder(level, mob, target, config, gameTime)) {
            return false;
        }

        if (target == mob.getLastHurtByMob()) {
            return false;
        }

        if (!RetoldTerritoryTargetSelector.isReputationGatedIntruder(target)) {
            return false;
        }

        RetoldTerritoryMobState state = RetoldTerritoryMobStates.get(mob);

        RetoldTerritoryContext territoryContext = RetoldTerritoryRules.refreshCachedMatchingContext(
                state,
                level,
                mob,
                config,
                gameTime
        );

        if (territoryContext == null) {
            return true;
        }

        if (RetoldTerritoryReputation.shouldAttack(territoryContext, target)) {
            return false;
        }

        RetoldTerritoryMobState warningState = RetoldTerritoryMobStates.getOrCreate(mob);
        warningState.territoryContext = territoryContext;
        RetoldTerritoryController.setWarningTarget(
                warningState,
                mob,
                target,
                gameTime
        );

        return true;
    }
}
