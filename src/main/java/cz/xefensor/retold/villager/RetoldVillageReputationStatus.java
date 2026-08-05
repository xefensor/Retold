package cz.xefensor.retold.villager;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Produces a one-off summary of the vanilla gossip reputation held by the
 * loaded village-context Villagers around a player.
 */
public final class RetoldVillageReputationStatus {
    public static final int QUERY_RADIUS = 32;
    private static final double QUERY_RADIUS_SQUARED =
            QUERY_RADIUS * QUERY_RADIUS;
    private static final int GOLEM_HOSTILITY_REPUTATION = -100;

    private RetoldVillageReputationStatus() {
    }

    public static Snapshot inspect(
            ServerLevel level,
            ServerPlayer player
    ) {
        AABB bounds = player.getBoundingBox().inflate(QUERY_RADIUS);
        BlockPos playerPos = player.blockPosition();
        List<Villager> villagers = level.getEntitiesOfClass(
                Villager.class,
                bounds,
                villager -> isInQueriedVillage(
                        level,
                        villager,
                        playerPos
                )
        );

        if (villagers.isEmpty()) {
            return Snapshot.empty();
        }

        int total = 0;
        int worst = Integer.MAX_VALUE;
        int best = Integer.MIN_VALUE;
        int negative = 0;
        int hostile = 0;

        for (Villager villager : villagers) {
            int reputation = villager.getPlayerReputation(player);
            total += reputation;
            worst = Math.min(worst, reputation);
            best = Math.max(best, reputation);

            if (reputation < 0) {
                negative++;
            }

            if (reputation <= GOLEM_HOSTILITY_REPUTATION) {
                hostile++;
            }
        }

        int average = Math.round((float) total / villagers.size());
        return new Snapshot(
                villagers.size(),
                average,
                worst,
                best,
                negative,
                hostile,
                standing(average, hostile)
        );
    }

    private static boolean isInQueriedVillage(
            ServerLevel level,
            Villager villager,
            BlockPos playerPos
    ) {
        if (!villager.isAlive()) {
            return false;
        }

        BlockPos villageAnchor = RetoldVillagerCommunalFoodSearch
                .villageAnchor(level, villager);
        return villageAnchor != null
                && villageAnchor.distSqr(playerPos)
                <= QUERY_RADIUS_SQUARED;
    }

    private static Standing standing(int average, int hostile) {
        if (hostile > 0) {
            return Standing.HOSTILE;
        }

        if (average <= -25) {
            return Standing.DISTRUSTFUL;
        }

        if (average < 0) {
            return Standing.WARY;
        }

        if (average > 0) {
            return Standing.FRIENDLY;
        }

        return Standing.NEUTRAL;
    }

    public enum Standing {
        HOSTILE("hostile"),
        DISTRUSTFUL("distrustful"),
        WARY("wary"),
        NEUTRAL("neutral"),
        FRIENDLY("friendly");

        private final String displayName;

        Standing(String displayName) {
            this.displayName = displayName;
        }

        public String displayName() {
            return displayName;
        }
    }

    public record Snapshot(
            int villagerCount,
            int averageReputation,
            int worstReputation,
            int bestReputation,
            int negativeVillagers,
            int hostileVillagers,
            Standing standing
    ) {
        private static Snapshot empty() {
            return new Snapshot(
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    Standing.NEUTRAL
            );
        }

        public boolean hasVillage() {
            return villagerCount > 0;
        }

        public boolean hasGolemHostilityRisk() {
            return hostileVillagers > 0;
        }
    }
}
