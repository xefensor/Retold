package cz.xefensor.retold.villager;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import cz.xefensor.retold.Retold;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
    private final Map<CropChunkKey, Set<Long>> cropsByChunk;

    RetoldVillageCropOwnershipData() {
        crops = new HashSet<>();
        cropsByChunk = new HashMap<>();
    }

    private RetoldVillageCropOwnershipData(SerializedState state) {
        crops = new HashSet<>();
        cropsByChunk = new HashMap<>();

        for (OwnedCrop crop : state.crops()) {
            CropKey key = new CropKey(crop.dimension(), crop.position());
            crops.add(key);
            index(key);
        }
    }

    static RetoldVillageCropOwnershipData get(ServerLevel level) {
        return level.getServer().getDataStorage().computeIfAbsent(TYPE);
    }

    synchronized boolean isOwned(ServerLevel level, BlockPos pos) {
        return crops.contains(key(level, pos));
    }

    synchronized void mark(ServerLevel level, BlockPos pos) {
        CropKey key = key(level, pos);

        if (crops.add(key)) {
            index(key);
            setDirty();
        }
    }

    synchronized void clear(ServerLevel level, BlockPos pos) {
        CropKey key = key(level, pos);

        if (crops.remove(key)) {
            unindex(key);
            setDirty();
        }
    }

    synchronized List<BlockPos> nearby(
            ServerLevel level,
            BlockPos center,
            int horizontalRadius,
            int verticalRadius,
            int limit
    ) {
        if (level == null || center == null || limit <= 0) {
            return List.of();
        }

        Identifier dimension = level.dimension().identifier();
        int radius = Math.max(0, horizontalRadius);
        int yRadius = Math.max(0, verticalRadius);
        int minChunkX = Math.floorDiv(center.getX() - radius, 16);
        int maxChunkX = Math.floorDiv(center.getX() + radius, 16);
        int minChunkZ = Math.floorDiv(center.getZ() - radius, 16);
        int maxChunkZ = Math.floorDiv(center.getZ() + radius, 16);
        List<BlockPos> found = new ArrayList<>();

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                Set<Long> indexed = cropsByChunk.get(new CropChunkKey(
                        dimension,
                        ChunkPos.pack(chunkX, chunkZ)
                ));

                if (indexed == null) {
                    continue;
                }

                for (long packedPos : indexed) {
                    BlockPos pos = BlockPos.of(packedPos);
                    int dx = pos.getX() - center.getX();
                    int dz = pos.getZ() - center.getZ();

                    if (Math.abs(pos.getY() - center.getY()) <= yRadius
                            && dx * dx + dz * dz <= radius * radius
                            && level.hasChunkAt(pos)) {
                        found.add(pos);
                    }
                }
            }
        }

        found.sort(Comparator
                .comparingDouble((BlockPos pos) -> center.distSqr(pos))
                .thenComparingLong(BlockPos::asLong));
        return found.size() <= limit
                ? List.copyOf(found)
                : List.copyOf(found.subList(0, limit));
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

    private void index(CropKey crop) {
        BlockPos pos = BlockPos.of(crop.position());
        cropsByChunk.computeIfAbsent(
                new CropChunkKey(crop.dimension(), ChunkPos.pack(pos)),
                ignored -> new HashSet<>()
        ).add(crop.position());
    }

    private void unindex(CropKey crop) {
        BlockPos pos = BlockPos.of(crop.position());
        CropChunkKey chunk = new CropChunkKey(
                crop.dimension(),
                ChunkPos.pack(pos)
        );
        Set<Long> indexed = cropsByChunk.get(chunk);

        if (indexed == null) {
            return;
        }

        indexed.remove(crop.position());

        if (indexed.isEmpty()) {
            cropsByChunk.remove(chunk);
        }
    }

    record SerializedState(int version, List<OwnedCrop> crops) {
    }

    record OwnedCrop(Identifier dimension, long position) {
    }

    private record CropKey(Identifier dimension, long position) {
    }

    private record CropChunkKey(Identifier dimension, long chunk) {
    }
}
