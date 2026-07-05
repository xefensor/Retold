package cz.xefensor.retold.territory;

import cz.xefensor.retold.faction.RetoldFaction;
import cz.xefensor.retold.faction.RetoldFactionMembers;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public final class RetoldTerritoryConfigs {
    private static final Map<RetoldFaction, RetoldTerritoryConfig> CONFIGS =
            new EnumMap<>(RetoldFaction.class);

    static {
        registerTerritory(
                RetoldFaction.NETHER_REMNANTS,
                "nether_remnant_territory",
                SoundEvents.PIGLIN_ANGRY,
                Level.NETHER
        );

        registerTerritory(
                RetoldFaction.ILLAGERS,
                "illager_territory",
                SoundEvents.PILLAGER_AMBIENT,
                null
        );
    }

    private RetoldTerritoryConfigs() {
    }

    public static RetoldTerritoryConfig get(RetoldFaction faction) {
        if (faction == null) {
            return null;
        }

        return CONFIGS.get(faction);
    }

    public static RetoldTerritoryConfig getForEntity(Entity entity) {
        if (entity == null) {
            return null;
        }

        RetoldFaction faction = RetoldFactionMembers.getFaction(entity);

        if (faction == null) {
            return null;
        }

        return get(faction);
    }

    public static Collection<RetoldTerritoryConfig> all() {
        return Collections.unmodifiableCollection(CONFIGS.values());
    }

    private static void registerTerritory(
            RetoldFaction faction,
            String structureTagPath,
            SoundEvent warningSound,
            ResourceKey<Level> requiredDimension
    ) {
        CONFIGS.put(
                faction,
                new RetoldTerritoryConfig(
                        faction,
                        territoryTag(structureTagPath),
                        warningSound,
                        requiredDimension
                )
        );
    }

    private static TagKey<Structure> territoryTag(String path) {
        return TagKey.create(
                Registries.STRUCTURE,
                Identifier.fromNamespaceAndPath("retold", path)
        );
    }
}