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
            id("vex")
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

    private static final Set<Identifier> MONSTERS = Set.of(
            id("creeper"),
            id("slime"),
            id("magma_cube"),
            id("guardian"),
            id("elder_guardian"),
            id("witch"),
            id("breeze"),
            id("ravager"),
            id("hoglin"),
            id("zoglin"),
            id("shulker"),
            id("warden"),
            id("wither"),
            id("ender_dragon"),
            id("creaking")
    );

    private static final Set<Identifier> ARTHROPODS = Set.of(
            id("spider"),
            id("cave_spider"),
            id("silverfish"),
            id("endermite")
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

        if (MONSTERS.contains(id)) {
            return RetoldFaction.MONSTERS;
        }

        if (ARTHROPODS.contains(id)) {
            return RetoldFaction.ARTHROPODS;
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