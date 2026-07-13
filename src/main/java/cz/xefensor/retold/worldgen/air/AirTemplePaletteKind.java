package cz.xefensor.retold.worldgen.air;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

enum AirTemplePaletteKind {
    FROZEN,
    STONY;

    static AirTemplePaletteKind fromCenterBiome(Holder<Biome> biome) {
        if (biome.is(Biomes.STONY_PEAKS)) {
            return STONY;
        }

        return FROZEN;
    }

    static AirTemplePaletteKind bySerializedId(int id) {
        AirTemplePaletteKind[] values = values();

        if (id < 0 || id >= values.length) {
            return FROZEN;
        }

        return values[id];
    }
}
