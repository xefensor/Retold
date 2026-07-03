package cz.xefensor.retold.faction;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class RetoldFactionMembers {
    private static final Identifier PIGLIN = id("piglin");
    private static final Identifier PIGLIN_BRUTE = id("piglin_brute");
    private static final Identifier BLAZE = id("blaze");

    private RetoldFactionMembers() {
    }

    public static boolean isMemberOf(Entity entity, RetoldFaction faction) {
        return switch (faction) {
            case NETHER_REMNANTS -> isNetherRemnant(entity);
        };
    }

    public static boolean isTargetableMemberOf(LivingEntity entity, RetoldFaction faction) {
        return switch (faction) {
            case NETHER_REMNANTS -> isNetherRemnant(entity);
        };
    }

    public static boolean isNetherRemnant(Entity entity) {
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());

        return PIGLIN.equals(id)
                || PIGLIN_BRUTE.equals(id)
                || BLAZE.equals(id);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("minecraft", path);
    }
}