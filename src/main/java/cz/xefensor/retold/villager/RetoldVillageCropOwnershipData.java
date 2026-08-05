package cz.xefensor.retold.villager;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import cz.xefensor.retold.Retold;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Persistent positions of crops planted or replanted by Farmers. */
final class RetoldVillageCropOwnershipData extends SavedData {
    private static final int SAVE_VERSION = 1;

    private static final Codec<OwnedCrop> CROP_CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Identifier.CODEC.fieldOf("dimension").forGetter(
                            OwnedCrop::dimension
                    ),
                    Codec.LONG.fieldOf("position").forGetter(
                            OwnedCrop::position
                    )
            ).apply(instance, OwnedCrop::new));

    private static final Codec<SerializedState> RAW_CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.INT.fieldOf("version").forGetter(
                            SerializedState::version
                    ),
                    CROP_CODEC.listOf().fieldOf("crops").forGetter(
                            SerializedState::crops
                    )
            ).apply(instance, SerializedState::new));

    private static final Codec<RetoldVillageCropOwnershipData> CODEC =
            RAW_CODEC.flatXmap(
                    RetoldVillageCropOwnershipData::decode,
                    RetoldVillageCropOwnershipData::encode
            );

    static final SavedDataType<RetoldVillageCropOwnershipData> TYPE =
            new SavedDataType<>(
                    Identifier.fromNamespaceAndPath(
                            Retold.MODID,
                            "village_crop_ownership"
                    ),
                    RetoldVillageCropOwnershipData::new,
                    CODEC
            );

    private final Set<CropKey> crops;

    RetoldVillageCropOwnershipData() {
        crops = new HashSet<>();
    }

    private RetoldVillageCropOwnershipData(SerializedState state) {
        crops = new HashSet<>();

        for (OwnedCrop crop : state.crops()) {
            crops.add(new CropKey(crop.dimension(), crop.position()));
        }
    }

    static RetoldVillageCropOwnershipData get(ServerLevel level) {
        return level.getServer().getDataStorage().computeIfAbsent(TYPE);
    }

    synchronized boolean isOwned(ServerLevel level, BlockPos pos) {
        return crops.contains(key(level, pos));
    }

    synchronized void mark(ServerLevel level, BlockPos pos) {
        if (crops.add(key(level, pos))) {
            setDirty();
        }
    }

    synchronized void clear(ServerLevel level, BlockPos pos) {
        if (crops.remove(key(level, pos))) {
            setDirty();
        }
    }

    synchronized SerializedState serializeState() {
        List<OwnedCrop> serialized = crops.stream()
                .map(crop -> new OwnedCrop(
                        crop.dimension(),
                        crop.position()
                ))
                .toList();
        return new SerializedState(SAVE_VERSION, serialized);
    }

    static RetoldVillageCropOwnershipData fromSerializedState(
            SerializedState state
    ) {
        validate(state);
        return new RetoldVillageCropOwnershipData(state);
    }

    private static DataResult<RetoldVillageCropOwnershipData> decode(
            SerializedState state
    ) {
        try {
            return DataResult.success(fromSerializedState(state));
        } catch (IllegalArgumentException exception) {
            return DataResult.error(exception::getMessage);
        }
    }

    private static DataResult<SerializedState> encode(
            RetoldVillageCropOwnershipData data
    ) {
        return DataResult.success(data.serializeState());
    }

    private static void validate(SerializedState state) {
        if (state == null || state.version() != SAVE_VERSION) {
            throw new IllegalArgumentException(
                    "Unsupported village crop ownership version"
            );
        }

        if (state.crops() == null) {
            throw new IllegalArgumentException(
                    "Village crop ownership has no crop list"
            );
        }

        Set<CropKey> seen = new HashSet<>();

        for (OwnedCrop crop : state.crops()) {
            if (crop == null || crop.dimension() == null) {
                throw new IllegalArgumentException(
                        "Invalid village crop ownership entry"
                );
            }

            CropKey key = new CropKey(crop.dimension(), crop.position());

            if (!seen.add(key)) {
                throw new IllegalArgumentException(
                        "Duplicate village crop ownership entry"
                );
            }
        }
    }

    private static CropKey key(ServerLevel level, BlockPos pos) {
        return new CropKey(
                level.dimension().identifier(),
                pos.asLong()
        );
    }

    record SerializedState(int version, List<OwnedCrop> crops) {
    }

    record OwnedCrop(Identifier dimension, long position) {
    }

    private record CropKey(Identifier dimension, long position) {
    }
}
