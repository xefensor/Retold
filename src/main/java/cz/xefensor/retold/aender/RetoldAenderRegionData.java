package cz.xefensor.retold.aender;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import cz.xefensor.retold.Retold;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RetoldAenderRegionData extends SavedData {
    public static final SavedDataType<RetoldAenderRegionData> TYPE =
            new SavedDataType<>(
                    Identifier.fromNamespaceAndPath(
                            Retold.MODID,
                            "aender_region_data"
                    ),
                    RetoldAenderRegionData::new,
                    RecordCodecBuilder.create(instance -> instance.group(
                            RegionEntry.CODEC
                                    .listOf()
                                    .optionalFieldOf("regions", List.of())
                                    .forGetter(RetoldAenderRegionData::entries)
                    ).apply(instance, RetoldAenderRegionData::new))
            );

    private final Map<Long, RegionState> regions =
            new HashMap<>();

    public RetoldAenderRegionData() {
    }

    private RetoldAenderRegionData(List<RegionEntry> entries) {
        for (RegionEntry entry : entries) {
            regions.put(
                    entry.key(),
                    new RegionState(
                            Math.max(0, entry.version()),
                            entry.salt()
                    )
            );
        }
    }

    public int getVersion(int regionX, int regionZ) {
        return getState(regionX, regionZ).version();
    }

    public long getSalt(int regionX, int regionZ) {
        return getState(regionX, regionZ).salt();
    }

    public int bumpRegionToSalt(
            int regionX,
            int regionZ,
            long salt
    ) {
        long key = packRegion(regionX, regionZ);
        RegionState oldState =
                regions.getOrDefault(key, RegionState.EMPTY);

        int newVersion = oldState.version() + 1;

        regions.put(
                key,
                new RegionState(
                        newVersion,
                        salt == 0L ? 1L : salt
                )
        );

        setDirty();

        return newVersion;
    }

    public static RetoldAenderRegionData get(ServerLevel level) {
        return level.getServer().getDataStorage().computeIfAbsent(TYPE);
    }

    private RegionState getState(int regionX, int regionZ) {
        return regions.getOrDefault(
                packRegion(regionX, regionZ),
                RegionState.EMPTY
        );
    }

    private List<RegionEntry> entries() {
        return regions
                .entrySet()
                .stream()
                .map(entry -> new RegionEntry(
                        entry.getKey(),
                        entry.getValue().version(),
                        entry.getValue().salt()
                ))
                .toList();
    }

    private static long packRegion(int regionX, int regionZ) {
        return ((long) regionX & 4294967295L)
                | (((long) regionZ & 4294967295L) << 32);
    }

    private record RegionState(
            int version,
            long salt
    ) {
        private static final RegionState EMPTY =
                new RegionState(0, 0L);
    }

    private record RegionEntry(
            long key,
            int version,
            long salt
    ) {
        private static final Codec<RegionEntry> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        Codec.LONG
                                .fieldOf("key")
                                .forGetter(RegionEntry::key),

                        Codec.INT
                                .fieldOf("version")
                                .forGetter(RegionEntry::version),

                        Codec.LONG
                                .fieldOf("salt")
                                .forGetter(RegionEntry::salt)
                ).apply(instance, RegionEntry::new));
    }
}