package cz.xefensor.retold.villager;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import cz.xefensor.retold.Retold;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
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
import java.util.function.Predicate;

/**
 * Persistent, shared knowledge of storage that villagers have observed.
 * Entries are indexed by chunk so a lookup never scans every known container.
 * The physical container remains authoritative and is revalidated before use.
 */
final class RetoldVillageStorageKnowledge extends SavedData {
    private static final int SAVE_VERSION = 1;

    private static final Codec<SerializedStorage> STORAGE_CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Identifier.CODEC.fieldOf("dimension").forGetter(
                            SerializedStorage::dimension
                    ),
                    Codec.LONG.fieldOf("position").forGetter(
                            SerializedStorage::position
                    ),
                    Codec.INT.fieldOf("slots").forGetter(
                            SerializedStorage::slots
                    ),
                    ItemStack.CODEC.listOf().fieldOf("items").forGetter(
                            SerializedStorage::items
                    )
            ).apply(instance, SerializedStorage::new));

    private static final Codec<SerializedState> RAW_CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.INT.fieldOf("version").forGetter(
                            SerializedState::version
                    ),
                    STORAGE_CODEC.listOf().fieldOf("storages").forGetter(
                            SerializedState::storages
                    )
            ).apply(instance, SerializedState::new));

    private static final Codec<RetoldVillageStorageKnowledge> CODEC =
            RAW_CODEC.flatXmap(
                    RetoldVillageStorageKnowledge::decode,
                    RetoldVillageStorageKnowledge::encode
            );

    private static final SavedDataType<RetoldVillageStorageKnowledge> TYPE =
            new SavedDataType<>(
                    Identifier.fromNamespaceAndPath(
                            Retold.MODID,
                            "village_storage_knowledge"
                    ),
                    RetoldVillageStorageKnowledge::new,
                    CODEC
            );

    private final Map<StorageKey, KnownStorage> storages = new HashMap<>();
    private final Map<ChunkKey, Set<StorageKey>> storagesByChunk =
            new HashMap<>();

    private RetoldVillageStorageKnowledge() {
    }

    private RetoldVillageStorageKnowledge(SerializedState state) {
        for (SerializedStorage serialized : state.storages()) {
            StorageKey key = new StorageKey(
                    serialized.dimension(),
                    serialized.position()
            );
            put(
                    key,
                    new KnownStorage(
                            serialized.slots(),
                            copyItems(serialized.items())
                    )
            );
        }
    }

    static RetoldVillageStorageKnowledge get(ServerLevel level) {
        return level.getServer().getDataStorage().computeIfAbsent(TYPE);
    }

    static void observe(
            ServerLevel level,
            BlockPos pos,
            Container container
    ) {
        if (level == null || pos == null || container == null) {
            return;
        }

        get(level).observeStorage(level, pos, container);
    }

    static void forget(ServerLevel level, BlockPos pos) {
        if (level != null && pos != null) {
            get(level).forgetStorage(level, pos);
        }
    }

    static List<BlockPos> foodCandidates(
            ServerLevel level,
            BlockPos center,
            BlockPos villageAnchor,
            int horizontalRadius,
            int verticalRadius,
            int villageRadius
    ) {
        return get(level).candidates(
                level,
                center,
                villageAnchor,
                horizontalRadius,
                verticalRadius,
                villageRadius,
                RetoldVillageStorageKnowledge::hasVillagerFood
        );
    }

    static List<BlockPos> itemCandidates(
            ServerLevel level,
            BlockPos center,
            BlockPos villageAnchor,
            int horizontalRadius,
            int verticalRadius,
            int villageRadius,
            ItemStack wanted,
            int minimumCount
    ) {
        return get(level).candidates(
                level,
                center,
                villageAnchor,
                horizontalRadius,
                verticalRadius,
                villageRadius,
                storage -> hasItem(storage, wanted, minimumCount)
        );
    }

    static List<BlockPos> depositCandidates(
            ServerLevel level,
            BlockPos center,
            BlockPos villageAnchor,
            int horizontalRadius,
            int verticalRadius,
            int villageRadius,
            ItemStack offered
    ) {
        return get(level).candidates(
                level,
                center,
                villageAnchor,
                horizontalRadius,
                verticalRadius,
                villageRadius,
                storage -> hasRoomFor(storage, offered)
        );
    }

    synchronized SerializedState serializeState() {
        List<SerializedStorage> serialized = new ArrayList<>();

        for (Map.Entry<StorageKey, KnownStorage> entry : storages.entrySet()) {
            serialized.add(new SerializedStorage(
                    entry.getKey().dimension(),
                    entry.getKey().position(),
                    entry.getValue().slots(),
                    copyItems(entry.getValue().items())
            ));
        }

        return new SerializedState(SAVE_VERSION, List.copyOf(serialized));
    }

    static RetoldVillageStorageKnowledge fromSerializedState(
            SerializedState state
    ) {
        validate(state);
        return new RetoldVillageStorageKnowledge(state);
    }

    synchronized boolean knowsItem(
            ServerLevel level,
            BlockPos pos,
            ItemStack wanted,
            int minimumCount
    ) {
        KnownStorage storage = storages.get(key(level, pos));
        return storage != null && hasItem(storage, wanted, minimumCount);
    }

    synchronized boolean knowsStorage(ServerLevel level, BlockPos pos) {
        return storages.containsKey(key(level, pos));
    }

    private synchronized void observeStorage(
            ServerLevel level,
            BlockPos pos,
            Container container
    ) {
        List<ItemStack> items = new ArrayList<>();

        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);

            if (!stack.isEmpty()) {
                items.add(stack.copy());
            }
        }

        put(
                key(level, pos),
                new KnownStorage(container.getContainerSize(), List.copyOf(items))
        );
        setDirty();
    }

    private synchronized void forgetStorage(
            ServerLevel level,
            BlockPos pos
    ) {
        StorageKey key = key(level, pos);

        if (storages.remove(key) == null) {
            return;
        }

        ChunkKey chunkKey = chunkKey(key);
        Set<StorageKey> chunkStorages = storagesByChunk.get(chunkKey);

        if (chunkStorages != null) {
            chunkStorages.remove(key);

            if (chunkStorages.isEmpty()) {
                storagesByChunk.remove(chunkKey);
            }
        }

        setDirty();
    }

    private synchronized List<BlockPos> candidates(
            ServerLevel level,
            BlockPos center,
            BlockPos villageAnchor,
            int horizontalRadius,
            int verticalRadius,
            int villageRadius,
            Predicate<KnownStorage> purpose
    ) {
        if (level == null
                || center == null
                || villageAnchor == null
                || purpose == null) {
            return List.of();
        }

        int minChunkX = Math.floorDiv(center.getX() - horizontalRadius, 16);
        int maxChunkX = Math.floorDiv(center.getX() + horizontalRadius, 16);
        int minChunkZ = Math.floorDiv(center.getZ() - horizontalRadius, 16);
        int maxChunkZ = Math.floorDiv(center.getZ() + horizontalRadius, 16);
        Identifier dimension = level.dimension().identifier();
        List<BlockPos> matches = new ArrayList<>();

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                if (!level.hasChunk(chunkX, chunkZ)) {
                    continue;
                }

                Set<StorageKey> chunkStorages = storagesByChunk.get(
                        new ChunkKey(
                                dimension,
                                ChunkPos.pack(chunkX, chunkZ)
                        )
                );

                if (chunkStorages == null) {
                    continue;
                }

                for (StorageKey storageKey : chunkStorages) {
                    BlockPos pos = BlockPos.of(storageKey.position());
                    KnownStorage storage = storages.get(storageKey);

                    if (storage != null
                            && isInsideSearch(
                            center,
                            pos,
                            horizontalRadius,
                            verticalRadius
                    )
                            && pos.distSqr(villageAnchor)
                            <= (double) villageRadius * villageRadius
                            && purpose.test(storage)) {
                        matches.add(pos);
                    }
                }
            }
        }

        matches.sort(
                Comparator.comparingDouble(
                        (BlockPos pos) -> center.distSqr(pos)
                ).thenComparingLong(BlockPos::asLong)
        );
        return List.copyOf(matches);
    }

    private void put(StorageKey key, KnownStorage storage) {
        storages.put(key, storage);
        storagesByChunk.computeIfAbsent(
                chunkKey(key),
                ignored -> new HashSet<>()
        ).add(key);
    }

    private static boolean hasVillagerFood(KnownStorage storage) {
        for (ItemStack stack : storage.items()) {
            if (Villager.FOOD_POINTS.getOrDefault(stack.getItem(), 0) > 0) {
                return true;
            }
        }

        return false;
    }

    private static boolean hasItem(
            KnownStorage storage,
            ItemStack wanted,
            int minimumCount
    ) {
        if (wanted == null || wanted.isEmpty()) {
            return false;
        }

        int found = 0;

        for (ItemStack stored : storage.items()) {
            if (ItemStack.isSameItemSameComponents(stored, wanted)) {
                found += stored.getCount();

                if (found >= Math.max(1, minimumCount)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean hasRoomFor(
            KnownStorage storage,
            ItemStack offered
    ) {
        if (offered == null || offered.isEmpty()) {
            return false;
        }

        if (storage.items().size() < storage.slots()) {
            return true;
        }

        for (ItemStack stored : storage.items()) {
            if (ItemStack.isSameItemSameComponents(stored, offered)
                    && stored.getCount() < stored.getMaxStackSize()) {
                return true;
            }
        }

        return false;
    }

    private static boolean isInsideSearch(
            BlockPos center,
            BlockPos pos,
            int horizontalRadius,
            int verticalRadius
    ) {
        int dx = pos.getX() - center.getX();
        int dy = Math.abs(pos.getY() - center.getY());
        int dz = pos.getZ() - center.getZ();
        return dy <= verticalRadius
                && dx * dx + dz * dz <= horizontalRadius * horizontalRadius;
    }

    private static StorageKey key(ServerLevel level, BlockPos pos) {
        return new StorageKey(
                level.dimension().identifier(),
                pos.asLong()
        );
    }

    private static ChunkKey chunkKey(StorageKey key) {
        BlockPos pos = BlockPos.of(key.position());
        return new ChunkKey(
                key.dimension(),
                ChunkPos.pack(pos.getX() >> 4, pos.getZ() >> 4)
        );
    }

    private static List<ItemStack> copyItems(List<ItemStack> items) {
        List<ItemStack> copies = new ArrayList<>();

        if (items != null) {
            for (ItemStack item : items) {
                if (item != null && !item.isEmpty()) {
                    copies.add(item.copy());
                }
            }
        }

        return List.copyOf(copies);
    }

    private static DataResult<RetoldVillageStorageKnowledge> decode(
            SerializedState state
    ) {
        try {
            return DataResult.success(fromSerializedState(state));
        } catch (IllegalArgumentException exception) {
            return DataResult.error(exception::getMessage);
        }
    }

    private static DataResult<SerializedState> encode(
            RetoldVillageStorageKnowledge data
    ) {
        return DataResult.success(data.serializeState());
    }

    private static void validate(SerializedState state) {
        if (state == null || state.version() != SAVE_VERSION) {
            throw new IllegalArgumentException(
                    "Unsupported village storage knowledge version"
            );
        }

        if (state.storages() == null) {
            throw new IllegalArgumentException(
                    "Village storage knowledge has no storage list"
            );
        }

        Set<StorageKey> seen = new HashSet<>();

        for (SerializedStorage storage : state.storages()) {
            if (storage == null
                    || storage.dimension() == null
                    || storage.slots() <= 0
                    || storage.items() == null) {
                throw new IllegalArgumentException(
                        "Invalid village storage knowledge entry"
                );
            }

            StorageKey key = new StorageKey(
                    storage.dimension(),
                    storage.position()
            );

            if (!seen.add(key)) {
                throw new IllegalArgumentException(
                        "Duplicate village storage knowledge entry"
                );
            }

            if (storage.items().size() > storage.slots()) {
                throw new IllegalArgumentException(
                        "Village storage knowledge contains too many stacks"
                );
            }

            for (ItemStack item : storage.items()) {
                if (item == null || item.isEmpty()) {
                    throw new IllegalArgumentException(
                            "Invalid known village storage item"
                    );
                }
            }
        }
    }

    record SerializedState(int version, List<SerializedStorage> storages) {
    }

    record SerializedStorage(
            Identifier dimension,
            long position,
            int slots,
            List<ItemStack> items
    ) {
    }

    private record StorageKey(Identifier dimension, long position) {
    }

    private record ChunkKey(Identifier dimension, long chunk) {
    }

    private record KnownStorage(int slots, List<ItemStack> items) {
    }
}
