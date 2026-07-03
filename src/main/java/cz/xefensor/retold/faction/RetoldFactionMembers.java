package cz.xefensor.retold.faction;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

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

    private RetoldFactionMembers() {
    }

    public static RetoldFaction getFaction(Entity entity) {
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());

        if (NETHER_REMNANTS.contains(id)) {
            return RetoldFaction.NETHER_REMNANTS;
        }

        if (ILLAGERS.contains(id)) {
            return RetoldFaction.ILLAGERS;
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