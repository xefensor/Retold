package cz.xefensor.retold.worldgen;

import cz.xefensor.retold.Retold;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.Structure;

public final class RetoldStructureTags {
    public static final TagKey<Structure> DELAYED_UNTIL_STAGE_2 =
            TagKey.create(
                    Registries.STRUCTURE,
                    Identifier.fromNamespaceAndPath(Retold.MODID, "delayed_until_stage_2")
            );

    private RetoldStructureTags() {
    }
}