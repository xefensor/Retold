package cz.xefensor.retold.aender.generation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import cz.xefensor.retold.Retold;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.SavedDataStorage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Persistent identity of the current Aender reality.
 *
 * <p>The terrain seed changes only after the last player travels out of the
 * dimension. Keeping it in SavedData makes disconnects, ordinary saves, clean
 * shutdowns, and crash recovery return to the same procedural reality instead
 * of silently deriving one from process timing.</p>
 */
public final class AenderRealityData extends SavedData {
    public static final int LEGACY_GENERATOR_VERSION = 1;
    public static final int MERGED_TERRAIN_GENERATOR_VERSION = 2;
    public static final int ISLAND_BIOMES_GENERATOR_VERSION = 3;
    public static final int CURRENT_GENERATOR_VERSION = ISLAND_BIOMES_GENERATOR_VERSION;

    private static final Codec<RegionEpochEntry> REGION_EPOCH_CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.INT.fieldOf("x").forGetter(RegionEpochEntry::x),
                    Codec.INT.fieldOf("z").forGetter(RegionEpochEntry::z),
                    Codec.LONG.fieldOf("epoch").forGetter(RegionEpochEntry::epoch)
            ).apply(instance, RegionEpochEntry::new));

    public static final SavedDataType<AenderRealityData> TYPE =
            new SavedDataType<>(
                    Identifier.fromNamespaceAndPath(Retold.MODID, "aender_reality"),
                    AenderRealityData::new,
                    RecordCodecBuilder.create(instance -> instance.group(
                            Codec.LONG.fieldOf("seed").forGetter(data -> data.seed),
                            Codec.LONG.optionalFieldOf("epoch", 0L).forGetter(data -> data.epoch),
                            Codec.INT
                                    .optionalFieldOf("generator_version", LEGACY_GENERATOR_VERSION)
                                    .forGetter(data -> data.generatorVersion),
                            REGION_EPOCH_CODEC
                                    .listOf()
                                    .optionalFieldOf("region_epochs", List.of())
                                    .forGetter(AenderRealityData::encodedRegionEpochs)
                    ).apply(instance, AenderRealityData::new))
            );

    private long seed;
    private long epoch;
    private int generatorVersion = LEGACY_GENERATOR_VERSION;
    private final Map<Long, Long> regionEpochs = new HashMap<>();

    public AenderRealityData() {
    }

    AenderRealityData(long seed, int generatorVersion) {
        this.seed = seed;
        this.generatorVersion = generatorVersion;
    }

    private AenderRealityData(
            long seed,
            long epoch,
            int generatorVersion,
            List<RegionEpochEntry> regionEpochs
    ) {
        this.seed = seed;
        this.epoch = epoch;
        this.generatorVersion = Math.max(LEGACY_GENERATOR_VERSION, generatorVersion);

        for (RegionEpochEntry entry : regionEpochs) {
            if (entry.epoch() > 0L) {
                this.regionEpochs.put(pack(entry.x(), entry.z()), entry.epoch());
            }
        }
    }

    public static AenderRealityData get(ServerLevel level) {
        SavedDataStorage storage = level.getServer().getDataStorage();
        AenderRealityData data = storage.get(TYPE);

        if (data == null) {
            data = new AenderRealityData();
            data.seed = initialSeed(level.getSeed());
            storage.set(TYPE, data);
        }

        return data;
    }

    public long seed() {
        return seed;
    }

    public long epoch() {
        return epoch;
    }

    public int generatorVersion() {
        return generatorVersion;
    }

    public long regionEpoch(int regionX, int regionZ) {
        return regionEpochs.getOrDefault(pack(regionX, regionZ), 0L);
    }

    public void enableCurrentGeneratorForFreshWorld() {
        if (generatorVersion != CURRENT_GENERATOR_VERSION) {
            generatorVersion = CURRENT_GENERATOR_VERSION;
            setDirty();
        }
    }

    public void advanceReality() {
        epoch++;
        seed = mix64(
                seed
                        ^ epoch * 0x9E3779B97F4A7C15L
                        ^ 0xA3D1E41F29B7C53DL
        );
        regionEpochs.clear();
        setDirty();
    }

    public void advanceRegion(int regionX, int regionZ) {
        regionEpochs.merge(pack(regionX, regionZ), 1L, Long::sum);
        setDirty();
    }

    private List<RegionEpochEntry> encodedRegionEpochs() {
        return regionEpochs.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new RegionEpochEntry(
                        unpackX(entry.getKey()),
                        unpackZ(entry.getKey()),
                        entry.getValue()
                ))
                .toList();
    }

    private static long initialSeed(long worldSeed) {
        return mix64(worldSeed ^ 0xA3D1E41F29B7C53DL);
    }

    private static long pack(int x, int z) {
        return ((long) x & 0xffffffffL) | (((long) z & 0xffffffffL) << 32);
    }

    private static int unpackX(long key) {
        return (int) key;
    }

    private static int unpackZ(long key) {
        return (int) (key >> 32);
    }

    private static long mix64(long value) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= value >>> 33;
        return value;
    }

    private record RegionEpochEntry(int x, int z, long epoch) {
    }
}
