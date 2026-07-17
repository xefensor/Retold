package cz.xefensor.retold.behavior.profiles;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum RetoldMobProfileType {
    NONE,
    HUNGRY_GRAZER,
    SMALL_FORAGER,
    PACK_PREDATOR,
    SOLO_OPPORTUNIST,
    AQUATIC_PREDATOR,
    HUNGRY_SWARM_PREDATOR,
    HIVE_COLONY,
    NETHER_HUNGRY,
    UNDEAD_HUNGRY,
    UNDEAD_TOLERANT,
    PHANTOM_STALKER,
    GHAST_ARTILLERY,
    ZOGLIN_RAMPAGER,
    SLIME_HUNGRY,
    SMALL_ARTHROPOD_SWARM,
    PROTECTIVE_NEUTRAL,
    PANDA_BAMBOO,
    SNIFFER_FORAGER,
    ARMADILLO_DEFENSIVE,
    TURTLE_BEACH,
    AMPHIBIAN_FORAGER,
    AQUATIC_HELPER_PREDATOR,
    AQUATIC_TERRITORY_GUARD,
    TERRITORY_GUARD,
    COMMANDER_SUPPORT,
    ILLAGER_RAIDER,
    SPECIAL_VANILLA,
    APEX_OR_BOSS;

    public static final Codec<RetoldMobProfileType> CODEC = Codec.STRING.comapFlatMap(
            serializedName -> fromSerializedName(serializedName)
                    .map(DataResult::success)
                    .orElseGet(() -> DataResult.error(
                            () -> "Unknown mob profile type: " + serializedName
                    )),
            RetoldMobProfileType::serializedName
    );

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Optional<RetoldMobProfileType> fromSerializedName(String serializedName) {
        if (serializedName == null) {
            return Optional.empty();
        }

        return Arrays.stream(values())
                .filter(type -> type.serializedName().equals(serializedName))
                .findFirst();
    }
}
