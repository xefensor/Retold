package cz.xefensor.retold.faction;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

public final class RetoldFactionRelations {
    private static final Map<RetoldFaction, Set<RetoldFaction>> TARGET_FACTIONS =
            new EnumMap<>(RetoldFaction.class);

    static {
        TARGET_FACTIONS.put(
                RetoldFaction.PLAYER,
                Set.of(
                        RetoldFaction.NETHER_REMNANTS,
                        RetoldFaction.ILLAGERS
                )
        );

        TARGET_FACTIONS.put(
                RetoldFaction.NETHER_REMNANTS,
                Set.of(
                        RetoldFaction.PLAYER,
                        RetoldFaction.ILLAGERS,
                        RetoldFaction.UNDEAD,
                        RetoldFaction.SLIMES,
                        RetoldFaction.AQUATIC_HOSTILES,
                        RetoldFaction.ARTHROPODS,
                        RetoldFaction.NETHER_BEASTS,
                        RetoldFaction.BREEZES,
                        RetoldFaction.WARDENS,
                        RetoldFaction.BOSSES,
                        RetoldFaction.CREAKINGS
                )
        );

        TARGET_FACTIONS.put(
                RetoldFaction.ILLAGERS,
                Set.of(
                        RetoldFaction.PLAYER,
                        RetoldFaction.NETHER_REMNANTS,
                        RetoldFaction.UNDEAD,
                        RetoldFaction.SLIMES,
                        RetoldFaction.AQUATIC_HOSTILES,
                        RetoldFaction.ARTHROPODS,
                        RetoldFaction.NETHER_BEASTS,
                        RetoldFaction.BREEZES,
                        RetoldFaction.WARDENS,
                        RetoldFaction.BOSSES,
                        RetoldFaction.CREAKINGS,
                        RetoldFaction.VILLAGE_DEFENDERS
                )
        );

        /*
         * Undead attack everyone except other undead.
         *
         * This includes:
         * - players
         * - piglins/brutes/blazes
         * - illagers
         * - slimes/magma cubes
         * - guardians
         * - creepers
         * - arthropods
         * - nether beasts
         * - breezes
         * - wardens
         * - bosses
         * - creakings
         * - village defenders
         * - enders
         */
        TARGET_FACTIONS.put(
                RetoldFaction.UNDEAD,
                Set.of(
                        RetoldFaction.PLAYER,
                        RetoldFaction.NETHER_REMNANTS,
                        RetoldFaction.ILLAGERS,
                        RetoldFaction.SLIMES,
                        RetoldFaction.AQUATIC_HOSTILES,
                        RetoldFaction.ARTHROPODS,
                        RetoldFaction.NETHER_BEASTS,
                        RetoldFaction.BREEZES,
                        RetoldFaction.WARDENS,
                        RetoldFaction.BOSSES,
                        RetoldFaction.CREAKINGS,
                        RetoldFaction.VILLAGE_DEFENDERS,
                        RetoldFaction.ENDERS
                )
        );

        TARGET_FACTIONS.put(
                RetoldFaction.SLIMES,
                Set.of(
                        RetoldFaction.NETHER_REMNANTS,
                        RetoldFaction.ILLAGERS,
                        RetoldFaction.VILLAGE_DEFENDERS
                )
        );

        TARGET_FACTIONS.put(
                RetoldFaction.AQUATIC_HOSTILES,
                Set.of(
                        RetoldFaction.NETHER_REMNANTS,
                        RetoldFaction.ILLAGERS,
                        RetoldFaction.VILLAGE_DEFENDERS
                )
        );

        /*
         * Creepers keep mostly vanilla behavior.
         * They only target players.
         *
         * Note: generic faction combat currently blocks custom player targeting,
         * so actual player targeting still mostly comes from vanilla creeper AI.
         */
        TARGET_FACTIONS.put(
                RetoldFaction.CREEPERS,
                Set.of(
                        RetoldFaction.PLAYER
                )
        );

        TARGET_FACTIONS.put(
                RetoldFaction.ARTHROPODS,
                Set.of(
                        RetoldFaction.NETHER_REMNANTS,
                        RetoldFaction.ILLAGERS,
                        RetoldFaction.VILLAGE_DEFENDERS
                )
        );

        TARGET_FACTIONS.put(
                RetoldFaction.NETHER_BEASTS,
                Set.of(
                        RetoldFaction.NETHER_REMNANTS,
                        RetoldFaction.ILLAGERS,
                        RetoldFaction.VILLAGE_DEFENDERS
                )
        );

        TARGET_FACTIONS.put(
                RetoldFaction.BREEZES,
                Set.of(
                        RetoldFaction.NETHER_REMNANTS,
                        RetoldFaction.ILLAGERS,
                        RetoldFaction.VILLAGE_DEFENDERS
                )
        );

        TARGET_FACTIONS.put(
                RetoldFaction.WARDENS,
                Set.of(
                        RetoldFaction.NETHER_REMNANTS,
                        RetoldFaction.ILLAGERS,
                        RetoldFaction.VILLAGE_DEFENDERS
                )
        );

        TARGET_FACTIONS.put(
                RetoldFaction.BOSSES,
                Set.of(
                        RetoldFaction.NETHER_REMNANTS,
                        RetoldFaction.ILLAGERS,
                        RetoldFaction.VILLAGE_DEFENDERS
                )
        );

        TARGET_FACTIONS.put(
                RetoldFaction.CREAKINGS,
                Set.of(
                        RetoldFaction.NETHER_REMNANTS,
                        RetoldFaction.ILLAGERS,
                        RetoldFaction.VILLAGE_DEFENDERS
                )
        );

        /*
         * Village Defenders target hostile/monstrous factions except:
         * - Players
         * - Nether Remnants
         * - Enders
         */
        TARGET_FACTIONS.put(
                RetoldFaction.VILLAGE_DEFENDERS,
                Set.of(
                        RetoldFaction.ILLAGERS,
                        RetoldFaction.UNDEAD,
                        RetoldFaction.SLIMES,
                        RetoldFaction.AQUATIC_HOSTILES,
                        RetoldFaction.ARTHROPODS,
                        RetoldFaction.NETHER_BEASTS,
                        RetoldFaction.BREEZES,
                        RetoldFaction.WARDENS,
                        RetoldFaction.BOSSES,
                        RetoldFaction.CREAKINGS
                )
        );

        /*
         * Enders keep vanilla behavior.
         * They do not custom-target anyone.
         */
        TARGET_FACTIONS.put(
                RetoldFaction.ENDERS,
                Set.of()
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

        if (attackerFaction == null) {
            return false;
        }

        return areEnemyFactions(attackerFaction, targetFaction);
    }

    public static boolean hasPotentialFactionTarget(Entity attacker) {
        for (RetoldFaction faction : RetoldFaction.values()) {
            if (shouldAttackFaction(attacker, faction)) {
                return true;
            }
        }

        return false;
    }

    /*
     * Directional relation:
     * first = attacker faction
     * second = target faction
     */
    public static boolean areEnemyFactions(RetoldFaction first, RetoldFaction second) {
        if (first == null || second == null) {
            return false;
        }

        if (first == second) {
            return false;
        }

        return targetsOf(first).contains(second);
    }

    private static Set<RetoldFaction> targetsOf(RetoldFaction faction) {
        return TARGET_FACTIONS.getOrDefault(faction, Set.of());
    }
}