package cz.xefensor.retold.worldgen.delayed;

import cz.xefensor.retold.worldgen.RetoldStructureTags;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.Optional;

public final class RetoldDelayedStructureHelper {
    private RetoldDelayedStructureHelper() {
    }

    public static boolean isDelayedStructure(
            RegistryAccess registryAccess,
            Structure structure
    ) {
        return findStructureHolder(registryAccess, structure)
                .map(holder -> holder.is(RetoldStructureTags.DELAYED_UNTIL_STAGE_2))
                .orElse(false);
    }

    public static String getStructureId(
            RegistryAccess registryAccess,
            Structure structure
    ) {
        return findStructureHolder(registryAccess, structure)
                .map(Holder::getRegisteredName)
                .orElse("unknown");
    }

    private static Optional<Holder.Reference<Structure>> findStructureHolder(
            RegistryAccess registryAccess,
            Structure structure
    ) {
        HolderLookup.RegistryLookup<Structure> lookup =
                registryAccess.lookupOrThrow(Registries.STRUCTURE);

        return lookup
                .listElements()
                .filter(holder -> holder.value() == structure)
                .findFirst();
    }
}