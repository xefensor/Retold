package cz.xefensor.retold.worldgen.earth;

import com.mojang.serialization.Codec;
import cz.xefensor.retold.Retold;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.Map;

public final class EarthGuardianEncounterData extends SavedData {
    public static final SavedDataType<EarthGuardianEncounterData> TYPE =
            new SavedDataType<>(
                    Identifier.fromNamespaceAndPath(Retold.MODID, "earth_guardian_encounters"),
                    EarthGuardianEncounterData::new,
                    Codec.unboundedMap(Codec.STRING, Codec.INT)
                            .xmap(
                                    EarthGuardianEncounterData::new,
                                    EarthGuardianEncounterData::encodeLifecycles
                            )
            );

    private final Map<Long, EarthGuardianLifecycle> lifecycles = new HashMap<>();

    public EarthGuardianEncounterData() {
    }

    private EarthGuardianEncounterData(Map<String, Integer> encoded) {
        encoded.forEach((key, lifecycleId) -> {
            try {
                lifecycles.put(
                        Long.parseLong(key),
                        EarthGuardianLifecycle.bySerializedId(lifecycleId)
                );
            } catch (NumberFormatException ignored) {
                // Ignore a single malformed encounter key while retaining every valid entry.
            }
        });
    }

    public static EarthGuardianEncounterData get(ServerLevel level) {
        return level.getServer().getDataStorage().computeIfAbsent(TYPE);
    }

    public EarthGuardianLifecycle lifecycle(long labyrinthKey) {
        return lifecycles.getOrDefault(labyrinthKey, EarthGuardianLifecycle.DORMANT);
    }

    public void setLifecycle(long labyrinthKey, EarthGuardianLifecycle lifecycle) {
        EarthGuardianLifecycle previous = lifecycles.put(labyrinthKey, lifecycle);

        if (previous != lifecycle) {
            setDirty();
        }
    }

    private Map<String, Integer> encodeLifecycles() {
        Map<String, Integer> encoded = new HashMap<>();
        lifecycles.forEach((key, lifecycle) -> encoded.put(
                Long.toString(key),
                lifecycle.ordinal()
        ));
        return encoded;
    }
}
