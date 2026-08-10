package cz.xefensor.retold.worldgen;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;

import java.util.List;

public final class RetoldRuinedPortalChests {
    private static final BlockIgnoreProcessor CHEST_REMOVAL_PROCESSOR =
            new BlockIgnoreProcessor(List.of(Blocks.CHEST));

    private RetoldRuinedPortalChests() {
    }

    public static StructurePlaceSettings removeChests(
            StructurePlaceSettings settings
    ) {
        return settings.addProcessor(CHEST_REMOVAL_PROCESSOR);
    }
}
