package cz.xefensor.retold.territory;

import cz.xefensor.retold.behavior.RetoldBehaviorPerf;
import cz.xefensor.retold.faction.RetoldFaction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.raid.Raid;

public final class RetoldTerritoryRules {
    private static final int CONTEXT_RECHECK_TICKS = 20;

    private RetoldTerritoryRules() {
    }

    public static boolean canUseTerritoryBehavior(
            ServerLevel level,
            Entity entity,
            RetoldTerritoryConfig config
    ) {
        if (level == null || entity == null || config == null) {
            return false;
        }

        if (!RetoldTerritoryDetector.isInAllowedDimension(level, config)) {
            return false;
        }

        if (config.faction == RetoldFaction.ILLAGERS && isInActiveRaid(level, entity)) {
            return false;
        }

        return true;
    }

    public static boolean canUseNearbyTerritoryBehavior(
            ServerLevel level,
            Entity entity,
            RetoldTerritoryConfig config,
            long gameTime
    ) {
        boolean passed = canUseTerritoryBehavior(
                level,
                entity,
                config
        ) && RetoldTerritoryDetector.isNearTerritory(
                level,
                entity,
                config,
                gameTime
        );

        RetoldBehaviorPerf.recordTerritoryNearby(passed);
        return passed;
    }

    public static RetoldTerritoryContext getMatchingContext(
            ServerLevel level,
            Entity entity,
            RetoldTerritoryConfig config
    ) {
        if (level == null || entity == null || config == null) {
            return null;
        }

        RetoldTerritoryContext context = RetoldTerritoryDetector.getContextAt(
                level,
                entity.blockPosition()
        );

        if (context == null || context.faction() != config.faction) {
            RetoldBehaviorPerf.recordTerritoryContext(false);
            return null;
        }

        RetoldBehaviorPerf.recordTerritoryContext(true);
        return context;
    }

    public static RetoldTerritoryContext refreshCachedMatchingContext(
            RetoldTerritoryMobState state,
            ServerLevel level,
            Entity entity,
            RetoldTerritoryConfig config,
            long gameTime
    ) {
        if (state == null) {
            return getMatchingContext(
                    level,
                    entity,
                    config
            );
        }

        if (
                state.territoryContext != null
                        && config != null
                        && state.territoryContext.faction() == config.faction
                        && gameTime < state.nextTerritoryContextRecheckAt
        ) {
            return state.territoryContext;
        }

        state.territoryContext = getMatchingContext(
                level,
                entity,
                config
        );
        state.nextTerritoryContextRecheckAt = gameTime + CONTEXT_RECHECK_TICKS;

        return state.territoryContext;
    }

    public static boolean isInActiveRaid(
            ServerLevel level,
            Entity entity
    ) {
        if (level == null || entity == null) {
            return false;
        }

        Raid raid = level.getRaidAt(entity.blockPosition());
        return raid != null && raid.isActive();
    }
}
