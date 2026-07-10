package cz.xefensor.retold.behavior;

import com.mojang.serialization.Codec;

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
    OCELOT_TERRITORY;

    public static final Codec<RetoldAnimalHomeType> CODEC =
            Codec.STRING.xmap(
                    RetoldAnimalHomeType::fromSerializedName,
                    RetoldAnimalHomeType::serializedName
            );

    public String serializedName() {
        return name().toLowerCase();
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
