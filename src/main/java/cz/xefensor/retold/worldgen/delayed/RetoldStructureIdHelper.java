package cz.xefensor.retold.worldgen.delayed;

import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.structure.Structure;

public final class RetoldStructureIdHelper {
    private RetoldStructureIdHelper() {
    }

    public static String getStructureId(Holder<Structure> structureHolder) {
        return structureHolder.getRegisteredName();
    }
}