package cz.xefensor.retold.behavior.home;

import com.mojang.serialization.Codec;

import java.util.Locale;

public enum RetoldAnimalHomeType {
    NONE,
    WOLF_DEN,
    DOLPHIN_POD_RANGE,
    HERD_RANGE,
    FORAGING_RANGE,
    ROOST,
    WARREN,
    FOX_DEN,
    CAT_TERRITORY,
    OCELOT_TERRITORY,
    PANDA_BAMBOO_GROVE,
    SNIFFER_FORAGING_RANGE,
    ARMADILLO_SCRUB_RANGE,
    TURTLE_BEACH,
    AMPHIBIAN_WETLAND,
    AXOLOTL_WATER_RANGE;

    public static final Codec<RetoldAnimalHomeType> CODEC =
            Codec.STRING.xmap(
                    RetoldAnimalHomeType::fromSerializedName,
                    RetoldAnimalHomeType::serializedName
            );

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static RetoldAnimalHomeType fromSerializedName(String name) {
        if (name == null || name.isBlank()) {
            return NONE;
        }

        for (RetoldAnimalHomeType type : values()) {
            if (type.serializedName().equals(name) || type.name().equalsIgnoreCase(name)) {
                return type;
            }
        }

        return NONE;
    }
}
