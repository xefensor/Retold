package cz.xefensor.retold.worldgen.air.wind;

import com.mojang.serialization.Codec;
import cz.xefensor.retold.Retold;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class AirTempleWindData extends SavedData {
    public static final SavedDataType<AirTempleWindData> TYPE =
            new SavedDataType<>(
                    Identifier.fromNamespaceAndPath(Retold.MODID, "air_temple_wind"),
                    AirTempleWindData::new,
                    Codec.unboundedMap(Codec.STRING, Codec.INT.listOf())
                            .xmap(AirTempleWindData::new, AirTempleWindData::encodeSources)
            );

    private final Map<Long, AirTempleWindSource> sources = new HashMap<>();

    public AirTempleWindData() {
    }

    private AirTempleWindData(Map<String, List<Integer>> encoded) {
        encoded.forEach((key, values) -> {
            if (values.size() < 3) {
                return;
            }

            try {
                long packedKey = Long.parseLong(key);
                AirTempleWindSource source = AirTempleWindSource.fromTemple(
                        values.get(0),
                        values.get(1),
                        values.get(2)
                );
                sources.put(packedKey, source);
            } catch (NumberFormatException ignored) {
            }
        });
    }

    public static AirTempleWindData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public Map<Long, AirTempleWindSource> sources() {
        return new HashMap<>(sources);
    }

    public void remember(AirTempleWindSource source) {
        AirTempleWindSource previous = sources.put(source.key(), source);

        if (!source.equals(previous)) {
            setDirty();
        }
    }

    public void rememberAll(Iterable<AirTempleWindSource> newSources) {
        for (AirTempleWindSource source : newSources) {
            remember(source);
        }
    }

    public void forget(long key) {
        if (sources.remove(key) != null) {
            setDirty();
        }
    }

    private Map<String, List<Integer>> encodeSources() {
        Map<String, List<Integer>> encoded = new HashMap<>();

        sources.forEach((key, source) -> encoded.put(
                Long.toString(key),
                List.of(source.centerX(), source.centerZ(), source.islandY())
        ));

        return encoded;
    }
}
