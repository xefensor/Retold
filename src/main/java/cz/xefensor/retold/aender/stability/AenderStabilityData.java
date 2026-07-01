package cz.xefensor.retold.aender.stability;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import cz.xefensor.retold.Retold;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.Map;

public final class AenderStabilityData extends SavedData {
    public static final SavedDataType<AenderStabilityData> TYPE =
            new SavedDataType<>(
                    Identifier.fromNamespaceAndPath(Retold.MODID, "aender_stability"),
                    AenderStabilityData::new,
                    RecordCodecBuilder.create(instance -> instance.group(
                            Codec.unboundedMap(Codec.STRING, Codec.INT)
                                    .optionalFieldOf("chunks", Map.of())
                                    .forGetter(data -> data.encodeChunks())
                    ).apply(instance, AenderStabilityData::new))
            );

    private final Map<Long, Integer> stableCounts = new HashMap<>();

    public AenderStabilityData() {
    }

    private AenderStabilityData(Map<String, Integer> encoded) {
        encoded.forEach((key, value) -> {
            try {
                long packed = Long.parseLong(key);
                if (value > 0) {
                    stableCounts.put(packed, value);
                }
            } catch (NumberFormatException ignored) {
            }
        });
    }

    public static AenderStabilityData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public boolean isStable(ChunkPos pos) {
        return stableCounts.getOrDefault(pack(pos), 0) > 0;
    }

    public void addStabilizer(ChunkPos center) {
        changeHalo(center, 1);
    }

    public void removeStabilizer(ChunkPos center) {
        changeHalo(center, -1);
    }

    private void changeHalo(ChunkPos center, int delta) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int chunkX = center.x() + dx;
                int chunkZ = center.z() + dz;

                long key = pack(chunkX, chunkZ);

                int next = stableCounts.getOrDefault(key, 0) + delta;
                if (next <= 0) {
                    stableCounts.remove(key);
                } else {
                    stableCounts.put(key, next);
                }
            }
        }

        setDirty();
    }

    private Map<String, Integer> encodeChunks() {
        Map<String, Integer> encoded = new HashMap<>();
        stableCounts.forEach((key, value) -> {
            if (value > 0) {
                encoded.put(Long.toString(key), value);
            }
        });
        return encoded;
    }

    private static long pack(ChunkPos pos) {
        return pack(pos.x(), pos.z());
    }

    private static long pack(int x, int z) {
        return ((long) x & 0xffffffffL) | (((long) z & 0xffffffffL) << 32);
    }
}