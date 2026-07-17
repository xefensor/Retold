package cz.xefensor.retold.behavior.profiles;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.HashMap;
import java.util.Map;

public final class RetoldMobProfiles {
    private static final RetoldMobProfile NONE = new RetoldMobProfile(
            RetoldMobProfileType.NONE,
            false,
            false,
            false,
            false,
            460,
            18,
            36
    );

    private static volatile Map<Identifier, RetoldMobProfile> profiles = Map.of();

    private RetoldMobProfiles() {
    }

    public static RetoldMobProfile get(Entity entity) {
        if (entity == null) {
            return NONE;
        }

        return get(entity.getType());
    }

    public static RetoldMobProfile get(EntityType<?> entityType) {
        if (entityType == null) {
            return NONE;
        }

        return get(BuiltInRegistries.ENTITY_TYPE.getKey(entityType));
    }

    public static RetoldMobProfile get(Identifier entityId) {
        if (entityId == null) {
            return NONE;
        }

        return profiles.getOrDefault(entityId, NONE);
    }

    public static RetoldMobProfile get(String entityPath) {
        if (entityPath == null || entityPath.isBlank()) {
            return NONE;
        }

        Identifier entityId = entityPath.indexOf(':') >= 0
                ? Identifier.parse(entityPath)
                : Identifier.withDefaultNamespace(entityPath);

        return get(entityId);
    }

    public static boolean isManaged(String entityPath) {
        return get(entityPath).managed();
    }

    public static boolean isPredator(String entityPath) {
        return get(entityPath).predator();
    }

    public static boolean isPackSocial(String entityPath) {
        return get(entityPath).packSocial();
    }

    public static boolean isType(String entityPath, RetoldMobProfileType type) {
        return get(entityPath).is(type);
    }

    public static int loadedProfileCount() {
        return profiles.size();
    }

    static int replace(Map<Identifier, RetoldMobProfileDefinition> definitions) {
        if (definitions.isEmpty()) {
            throw new IllegalArgumentException("No mob profile definitions were loaded");
        }

        Map<Identifier, RetoldMobProfile> nextProfiles = new HashMap<>();
        Map<Identifier, Identifier> sourcesByEntity = new HashMap<>();

        for (Map.Entry<Identifier, RetoldMobProfileDefinition> entry : definitions.entrySet()) {
            Identifier sourceId = entry.getKey();
            RetoldMobProfileDefinition definition = entry.getValue();
            Identifier entityId = definition.entity();

            if (!BuiltInRegistries.ENTITY_TYPE.containsKey(entityId)) {
                throw new IllegalArgumentException(
                        "Unknown entity " + entityId + " in " + sourceId
                );
            }

            Identifier previousSource = sourcesByEntity.putIfAbsent(entityId, sourceId);

            if (previousSource != null) {
                throw new IllegalArgumentException(
                        "Duplicate profile for " + entityId + " in " + previousSource + " and " + sourceId
                );
            }

            nextProfiles.put(entityId, definition.profile());
        }

        profiles = Map.copyOf(nextProfiles);
        return profiles.size();
    }
}
