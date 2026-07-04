package cz.xefensor.retold.faction;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

public final class RetoldFactionRelations {
    private static final Map<RetoldFaction, Set<RetoldFaction>> ENEMY_FACTIONS =
            new EnumMap<>(RetoldFaction.class);

    /*
     * This is only for special entities that are not in a RetoldFaction,
     * but should still be treated as enemies of a faction.
     *
     * Most normal hostile mobs should be handled through faction membership now.
     */
    private static final Map<RetoldFaction, Set<Identifier>> DIRECT_ENEMIES =
            new EnumMap<>(RetoldFaction.class);

    static {
        /*
         * Players are enemies of the factions whose territory/combat should react to them.
         * Do not make PLAYER enemy of Village Defenders unless you want golems to fight players.
         */
        ENEMY_FACTIONS.put(
                RetoldFaction.PLAYER,
                Set.of(
                        RetoldFaction.NETHER_REMNANTS,
                        RetoldFaction.ILLAGERS
                )
        );

        /*
         * Piglins/brutes/blazes hate most dangerous factions.
         * They do not hate Village Defenders by default.
         */
        ENEMY_FACTIONS.put(
                RetoldFaction.NETHER_REMNANTS,
                Set.of(
                        RetoldFaction.PLAYER,
                        RetoldFaction.ILLAGERS,
                        RetoldFaction.UNDEAD,
                        RetoldFaction.MONSTERS,
                        RetoldFaction.ARTHROPODS,
                        RetoldFaction.ENDERS
                )
        );

        /*
         * Illagers are hostile to players, piglin-side factions, undead rivals,
         * monsters that threaten them, and village defenders.
         */
        ENEMY_FACTIONS.put(
                RetoldFaction.ILLAGERS,
                Set.of(
                        RetoldFaction.PLAYER,
                        RetoldFaction.NETHER_REMNANTS,
                        RetoldFaction.UNDEAD,
                        RetoldFaction.MONSTERS,
                        RetoldFaction.VILLAGE_DEFENDERS
                )
        );

        /*
         * Undead are a broad hostile faction.
         * Includes ghasts in your current grouping.
         */
        ENEMY_FACTIONS.put(
                RetoldFaction.UNDEAD,
                Set.of(
                        RetoldFaction.NETHER_REMNANTS,
                        RetoldFaction.ILLAGERS,
                        RetoldFaction.VILLAGE_DEFENDERS
                )
        );

        /*
         * Generic monsters fight organized factions, but not necessarily every other monster group.
         * This prevents constant monster-vs-monster chaos.
         */
        ENEMY_FACTIONS.put(
                RetoldFaction.MONSTERS,
                Set.of(
                        RetoldFaction.NETHER_REMNANTS,
                        RetoldFaction.ILLAGERS,
                        RetoldFaction.VILLAGE_DEFENDERS
                )
        );

        /*
         * Arthropods are their own hostile creature faction.
         * They mainly oppose Nether Remnants and Village Defenders.
         */
        ENEMY_FACTIONS.put(
                RetoldFaction.ARTHROPODS,
                Set.of(
                        RetoldFaction.NETHER_REMNANTS,
                        RetoldFaction.VILLAGE_DEFENDERS
                )
        );

        /*
         * Village defenders protect against classic hostile groups and illagers.
         * They are not enemies of players.
         */
        ENEMY_FACTIONS.put(
                RetoldFaction.VILLAGE_DEFENDERS,
                Set.of(
                        RetoldFaction.ILLAGERS,
                        RetoldFaction.UNDEAD,
                        RetoldFaction.MONSTERS,
                        RetoldFaction.ARTHROPODS
                )
        );

        /*
         * End-related mobs are strange/territorial.
         * Keep this narrower so they do not fight everyone all the time.
         */
        ENEMY_FACTIONS.put(
                RetoldFaction.ENDERS,
                Set.of(
                        RetoldFaction.NETHER_REMNANTS
                )
        );

        /*
         * Direct non-faction enemies.
         * Useful for mobs/entities you do not want to put into a faction,
         * but still want to trigger faction hostility.
         */
        DIRECT_ENEMIES.put(
                RetoldFaction.NETHER_REMNANTS,
                Set.of(
                        id("warden"),
                        id("wither"),
                        id("ender_dragon"),
                        id("creaking")
                )
        );

        DIRECT_ENEMIES.put(
                RetoldFaction.ILLAGERS,
                Set.of(
                        id("warden"),
                        id("wither"),
                        id("ender_dragon"),
                        id("creaking")
                )
        );

        DIRECT_ENEMIES.put(
                RetoldFaction.VILLAGE_DEFENDERS,
                Set.of(
                        id("warden"),
                        id("wither"),
                        id("creaking")
                )
        );
    }

    private RetoldFactionRelations() {
    }

    public static boolean shouldAttack(Entity attacker, LivingEntity target) {
        if (attacker == target) {
            return false;
        }

        if (!target.isAlive()) {
            return false;
        }

        RetoldFaction targetFaction = RetoldFactionMembers.getFaction(target);

        if (targetFaction == null) {
            return false;
        }

        return shouldAttackFaction(attacker, targetFaction);
    }

    public static boolean shouldAttackFaction(Entity attacker, RetoldFaction targetFaction) {
        RetoldFaction attackerFaction = RetoldFactionMembers.getFaction(attacker);

        if (attackerFaction != null) {
            return areEnemyFactions(attackerFaction, targetFaction);
        }

        return isDirectEnemyOfFaction(attacker, targetFaction);
    }

    public static boolean hasPotentialFactionTarget(Entity attacker) {
        for (RetoldFaction faction : RetoldFaction.values()) {
            if (shouldAttackFaction(attacker, faction)) {
                return true;
            }
        }

        return false;
    }

    public static boolean areEnemyFactions(RetoldFaction first, RetoldFaction second) {
        if (first == null || second == null) {
            return false;
        }

        if (first == second) {
            return false;
        }

        return enemiesOf(first).contains(second)
                || enemiesOf(second).contains(first);
    }

    private static Set<RetoldFaction> enemiesOf(RetoldFaction faction) {
        return ENEMY_FACTIONS.getOrDefault(faction, Set.of());
    }

    private static boolean isDirectEnemyOfFaction(Entity entity, RetoldFaction targetFaction) {
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());

        return DIRECT_ENEMIES
                .getOrDefault(targetFaction, Set.of())
                .contains(id);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("minecraft", path);
    }
}