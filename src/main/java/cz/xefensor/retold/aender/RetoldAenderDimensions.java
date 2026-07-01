package cz.xefensor.retold.aender;

import cz.xefensor.retold.Retold;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public final class RetoldAenderDimensions {
    public static final ResourceKey<Level> AENDER = ResourceKey.create(
            Registries.DIMENSION,
            Identifier.fromNamespaceAndPath(Retold.MODID, "aender")
    );

    private RetoldAenderDimensions() {
    }
}