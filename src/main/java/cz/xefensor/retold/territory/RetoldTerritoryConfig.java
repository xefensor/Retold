package cz.xefensor.retold.territory;

import cz.xefensor.retold.faction.RetoldFaction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;

public final class RetoldTerritoryConfig {
    public final RetoldFaction faction;
    public final TagKey<Structure> territoryTag;
    public final SoundEvent warningSound;
    public final ResourceKey<Level> requiredDimension;

    public RetoldTerritoryConfig(
            RetoldFaction faction,
            TagKey<Structure> territoryTag,
            SoundEvent warningSound,
            ResourceKey<Level> requiredDimension
    ) {
        this.faction = faction;
        this.territoryTag = territoryTag;
        this.warningSound = warningSound;
        this.requiredDimension = requiredDimension;
    }
}