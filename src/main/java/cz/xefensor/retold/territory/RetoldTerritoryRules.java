package cz.xefensor.retold.territory;

import cz.xefensor.retold.faction.RetoldFaction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.raid.Raid;

public final class RetoldTerritoryRules {
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
        return canUseTerritoryBehavior(
                level,
                entity,
                config
        ) && RetoldTerritoryDetector.isNearTerritory(
                level,
                entity,
                config,
                gameTime
        );
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
            return null;
        }

        return context;
    }

    public static RetoldTerritoryContext refreshMatchingContext(
            RetoldTerritoryContext current,
            ServerLevel level,
            Entity entity,
            RetoldTerritoryConfig config
    ) {
        if (current != null && config != null && current.faction() == config.faction) {
            return current;
        }

        return getMatchingContext(
                level,
                entity,
                config
        );
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
