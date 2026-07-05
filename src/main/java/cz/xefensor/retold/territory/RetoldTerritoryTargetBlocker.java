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

        if (!RetoldTerritoryRules.canUseTerritoryBehavior(level, mob, config)) {
            return false;
        }

        long gameTime = level.getGameTime();

        if (!RetoldTerritoryDetector.isNearTerritory(level, mob, config, gameTime)) {
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

        RetoldTerritoryContext territoryContext = state == null
                ? RetoldTerritoryDetector.getContextAt(level, mob.blockPosition())
                : state.territoryContext;

        if (territoryContext == null || territoryContext.faction() != config.faction) {
            territoryContext = RetoldTerritoryDetector.getContextAt(level, mob.blockPosition());
        }

        if (territoryContext == null || territoryContext.faction() != config.faction) {
            return true;
        }

        return !RetoldTerritoryReputation.shouldAttack(territoryContext, target);
    }
}