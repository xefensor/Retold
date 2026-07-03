package cz.xefensor.retold.faction;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;

import java.util.Set;

public final class RetoldFactionRelations {
    private static final Set<Identifier> HOSTILE_TO_NETHER_REMNANTS = Set.of(
            // Blazes are intentionally excluded because they are part of Nether Remnants.

            id("bogged"),
            id("breeze"),
            id("cave_spider"),
            id("creeper"),
            id("drowned"),
            id("elder_guardian"),
            id("enderman"),
            id("endermite"),
            id("evoker"),
            id("ghast"),
            id("guardian"),
            id("hoglin"),
            id("husk"),
            id("illusioner"),
            id("magma_cube"),
            id("phantom"),
            id("pillager"),
            id("ravager"),
            id("shulker"),
            id("silverfish"),
            id("skeleton"),
            id("slime"),
            id("spider"),
            id("stray"),
            id("vex"),
            id("vindicator"),
            id("witch"),
            id("wither_skeleton"),
            id("zoglin"),
            id("zombie"),
            id("zombie_villager"),

            // Special / boss mobs.
            id("creaking"),
            id("warden"),
            id("wither"),
            id("ender_dragon")
    );

    private RetoldFactionRelations() {
    }

    public static boolean shouldAttackFaction(Entity attacker, RetoldFaction targetFaction) {
        return switch (targetFaction) {
            case NETHER_REMNANTS -> isHostileToNetherRemnants(attacker);
        };
    }

    private static boolean isHostileToNetherRemnants(Entity entity) {
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return HOSTILE_TO_NETHER_REMNANTS.contains(id);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("minecraft", path);
    }
}