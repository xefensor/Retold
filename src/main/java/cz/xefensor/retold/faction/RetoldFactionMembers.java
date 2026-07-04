package cz.xefensor.retold.faction;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.Set;

public final class RetoldFactionMembers {
    private static final Set<Identifier> NETHER_REMNANTS = Set.of(
            id("piglin"),
            id("piglin_brute"),
            id("blaze")
    );

    private static final Set<Identifier> ILLAGERS = Set.of(
            id("pillager"),
            id("vindicator"),
            id("evoker"),
            id("illusioner"),
            id("ravager"),
            id("vex"),
            id("witch")
    );

    private static final Set<Identifier> UNDEAD = Set.of(
            id("zombie"),
            id("zombie_villager"),
            id("husk"),
            id("drowned"),
            id("skeleton"),
            id("stray"),
            id("bogged"),
            id("wither_skeleton"),
            id("zombified_piglin"),
            id("phantom"),
            id("ghast")
    );

    private static final Set<Identifier> SLIMES = Set.of(
            id("slime"),
            id("magma_cube")
    );

    private static final Set<Identifier> AQUATIC_HOSTILES = Set.of(
            id("guardian"),
            id("elder_guardian")
    );

    private static final Set<Identifier> CREEPERS = Set.of(
            id("creeper")
    );

    private static final Set<Identifier> ARTHROPODS = Set.of(
            id("spider"),
            id("cave_spider"),
            id("silverfish"),
            id("endermite")
    );

    private static final Set<Identifier> NETHER_BEASTS = Set.of(
            id("hoglin"),
            id("zoglin")
    );

    private static final Set<Identifier> BREEZES = Set.of(
            id("breeze")
    );

    private static final Set<Identifier> WARDENS = Set.of(
            id("warden")
    );

    private static final Set<Identifier> BOSSES = Set.of(
            id("wither"),
            id("ender_dragon")
    );

    private static final Set<Identifier> CREAKINGS = Set.of(
            id("creaking")
    );

    private static final Set<Identifier> VILLAGE_DEFENDERS = Set.of(
            id("iron_golem"),
            id("snow_golem")
    );

    private static final Set<Identifier> ENDERS = Set.of(
            id("enderman"),
            id("shulker")
    );

    private RetoldFactionMembers() {
    }

    public static RetoldFaction getFaction(Entity entity) {
        if (entity instanceof Player) {
            return RetoldFaction.PLAYER;
        }

        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());

        if (NETHER_REMNANTS.contains(id)) {
            return RetoldFaction.NETHER_REMNANTS;
        }

        if (ILLAGERS.contains(id)) {
            return RetoldFaction.ILLAGERS;
        }

        if (UNDEAD.contains(id)) {
            return RetoldFaction.UNDEAD;
        }

        if (SLIMES.contains(id)) {
            return RetoldFaction.SLIMES;
        }

        if (AQUATIC_HOSTILES.contains(id)) {
            return RetoldFaction.AQUATIC_HOSTILES;
        }

        if (CREEPERS.contains(id)) {
            return RetoldFaction.CREEPERS;
        }

        if (ARTHROPODS.contains(id)) {
            return RetoldFaction.ARTHROPODS;
        }

        if (NETHER_BEASTS.contains(id)) {
            return RetoldFaction.NETHER_BEASTS;
        }

        if (BREEZES.contains(id)) {
            return RetoldFaction.BREEZES;
        }

        if (WARDENS.contains(id)) {
            return RetoldFaction.WARDENS;
        }

        if (BOSSES.contains(id)) {
            return RetoldFaction.BOSSES;
        }

        if (CREAKINGS.contains(id)) {
            return RetoldFaction.CREAKINGS;
        }

        if (VILLAGE_DEFENDERS.contains(id)) {
            return RetoldFaction.VILLAGE_DEFENDERS;
        }

        if (ENDERS.contains(id)) {
            return RetoldFaction.ENDERS;
        }

        return null;
    }

    public static boolean isMemberOf(Entity entity, RetoldFaction faction) {
        return getFaction(entity) == faction;
    }

    public static boolean isTargetableMemberOf(LivingEntity entity, RetoldFaction faction) {
        return isMemberOf(entity, faction);
    }

    public static boolean isNetherRemnant(Entity entity) {
        return isMemberOf(entity, RetoldFaction.NETHER_REMNANTS);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("minecraft", path);
    }
}