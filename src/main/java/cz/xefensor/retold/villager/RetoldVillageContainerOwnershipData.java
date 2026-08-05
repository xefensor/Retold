package cz.xefensor.retold.villager;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import cz.xefensor.retold.Retold;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Persistent quantities that originated in generated village loot or from a
 * Villager deposit. Physical container stacks remain vanilla-owned.
 */
final class RetoldVillageContainerOwnershipData extends SavedData {
    private static final int SAVE_VERSION = 1;

    private static final Codec<OwnedStack> OWNED_STACK_CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    ItemStack.CODEC.fieldOf("item").forGetter(
                            OwnedStack::item
                    ),
                    Codec.INT.fieldOf("count").forGetter(OwnedStack::count)
            ).apply(instance, OwnedStack::new));

    private static final Codec<SerializedContainer> CONTAINER_CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Identifier.CODEC.fieldOf("dimension").forGetter(
                            SerializedContainer::dimension
                    ),
                    Codec.LONG.fieldOf("position").forGetter(
                            SerializedContainer::position
                    ),
                    OWNED_STACK_CODEC.listOf().fieldOf("items").forGetter(
                            SerializedContainer::items
                    )
            ).apply(instance, SerializedContainer::new));

    private static final Codec<SerializedState> RAW_CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.INT.fieldOf("version").forGetter(
                            SerializedState::version
                    ),
                    CONTAINER_CODEC.listOf().fieldOf("containers").forGetter(
                            SerializedState::containers
                    )
            ).apply(instance, SerializedState::new));

    private static final Codec<RetoldVillageContainerOwnershipData> CODEC =
            RAW_CODEC.flatXmap(
                    RetoldVillageContainerOwnershipData::decode,
                    RetoldVillageContainerOwnershipData::encode
            );

    static final SavedDataType<RetoldVillageContainerOwnershipData> TYPE =
            new SavedDataType<>(
                    Identifier.fromNamespaceAndPath(
                            Retold.MODID,
                            "village_container_ownership"
                    ),
                    RetoldVillageContainerOwnershipData::new,
                    CODEC
            );

    private final Map<ContainerKey, List<OwnedItem>> containers;

    RetoldVillageContainerOwnershipData() {
        containers = new HashMap<>();
    }

    private RetoldVillageContainerOwnershipData(SerializedState state) {
        containers = new HashMap<>();

        for (SerializedContainer serialized : state.containers()) {
            ContainerKey key = new ContainerKey(
                    serialized.dimension(),
                    serialized.position()
            );
            List<OwnedItem> owned = new ArrayList<>();

            for (OwnedStack stack : serialized.items()) {
                addToList(owned, stack.item(), stack.count());
            }

            if (!owned.isEmpty()) {
                containers.put(key, owned);
            }
        }
    }

    static RetoldVillageContainerOwnershipData get(ServerLevel level) {
        return level.getServer().getDataStorage().computeIfAbsent(TYPE);
    }

    synchronized int ownedCount(
            ServerLevel level,
            BlockPos pos,
            ItemStack item
    ) {
        if (item == null || item.isEmpty()) {
            return 0;
        }

        List<OwnedItem> owned = containers.get(key(level, pos));
        return countInList(owned, item);
    }

    synchronized int totalOwned(ServerLevel level, BlockPos pos) {
        List<OwnedItem> owned = containers.get(key(level, pos));

        if (owned == null) {
            return 0;
        }

        int total = 0;

        for (OwnedItem item : owned) {
            total += item.count();
        }

        return total;
    }

    synchronized void replace(
            ServerLevel level,
            BlockPos pos,
            List<ItemStack> items
    ) {
        ContainerKey key = key(level, pos);
        List<OwnedItem> replacement = new ArrayList<>();

        if (items != null) {
            for (ItemStack item : items) {
                if (item != null && !item.isEmpty()) {
                    addToList(replacement, item, item.getCount());
                }
            }
        }

        if (replacement.isEmpty()) {
            containers.remove(key);
        } else {
            containers.put(key, replacement);
        }

        setDirty();
    }

    synchronized void add(
            ServerLevel level,
            BlockPos pos,
            ItemStack item,
            int count
    ) {
        if (item == null || item.isEmpty() || count <= 0) {
            return;
        }

        List<OwnedItem> owned = containers.computeIfAbsent(
                key(level, pos),
                ignored -> new ArrayList<>()
        );
        addToList(owned, item, count);
        setDirty();
    }

    synchronized int remove(
            ServerLevel level,
            BlockPos pos,
            ItemStack item,
            int count
    ) {
        if (item == null || item.isEmpty() || count <= 0) {
            return 0;
        }

        ContainerKey key = key(level, pos);
        List<OwnedItem> owned = containers.get(key);

        if (owned == null) {
            return 0;
        }

        int removed = removeFromList(owned, item, count);

        if (owned.isEmpty()) {
            containers.remove(key);
        }

        if (removed > 0) {
            setDirty();
        }

        return removed;
    }

    synchronized void reconcile(
            ServerLevel level,
            BlockPos pos,
            List<ItemStack> actualItems
    ) {
        ContainerKey key = key(level, pos);
        List<OwnedItem> owned = containers.get(key);

        if (owned == null) {
            return;
        }

        boolean changed = false;

        for (int index = owned.size() - 1; index >= 0; index--) {
            OwnedItem item = owned.get(index);
            int actual = countActual(actualItems, item.sample());

            if (actual <= 0) {
                owned.remove(index);
                changed = true;
            } else if (item.count() > actual) {
                owned.set(index, new OwnedItem(item.sample(), actual));
                changed = true;
            }
        }

        if (owned.isEmpty()) {
            containers.remove(key);
        }

        if (changed) {
            setDirty();
        }
    }

    synchronized void clear(ServerLevel level, BlockPos pos) {
        if (containers.remove(key(level, pos)) != null) {
            setDirty();
        }
    }

    synchronized SerializedState serializeState() {
        List<SerializedContainer> serialized = new ArrayList<>();

        for (Map.Entry<ContainerKey, List<OwnedItem>> entry
                : containers.entrySet()) {
            List<OwnedStack> items = new ArrayList<>();

            for (OwnedItem item : entry.getValue()) {
                items.add(new OwnedStack(
                        item.sample().copyWithCount(1),
                        item.count()
                ));
            }

            if (!items.isEmpty()) {
                serialized.add(new SerializedContainer(
                        entry.getKey().dimension(),
                        entry.getKey().position(),
                        List.copyOf(items)
                ));
            }
        }

        return new SerializedState(SAVE_VERSION, List.copyOf(serialized));
    }

    static RetoldVillageContainerOwnershipData fromSerializedState(
            SerializedState state
    ) {
        validate(state);
        return new RetoldVillageContainerOwnershipData(state);
    }

    private static DataResult<RetoldVillageContainerOwnershipData> decode(
            SerializedState state
    ) {
        try {
            return DataResult.success(fromSerializedState(state));
        } catch (IllegalArgumentException exception) {
            return DataResult.error(exception::getMessage);
        }
    }

    private static DataResult<SerializedState> encode(
            RetoldVillageContainerOwnershipData data
    ) {
        return DataResult.success(data.serializeState());
    }

    private static void validate(SerializedState state) {
        if (state == null || state.version() != SAVE_VERSION) {
            throw new IllegalArgumentException(
                    "Unsupported village container ownership version"
            );
        }

        if (state.containers() == null) {
            throw new IllegalArgumentException(
                    "Village container ownership has no container list"
            );
        }

        Set<ContainerKey> seen = new HashSet<>();

        for (SerializedContainer container : state.containers()) {
            if (container == null
                    || container.dimension() == null
                    || container.items() == null) {
                throw new IllegalArgumentException(
                        "Invalid village container ownership entry"
                );
            }

            ContainerKey key = new ContainerKey(
                    container.dimension(),
                    container.position()
            );

            if (!seen.add(key)) {
                throw new IllegalArgumentException(
                        "Duplicate village container ownership entry"
                );
            }

            for (OwnedStack item : container.items()) {
                if (item == null
                        || item.item() == null
                        || item.item().isEmpty()
                        || item.count() <= 0) {
                    throw new IllegalArgumentException(
                            "Invalid village-owned item entry"
                    );
                }
            }
        }
    }

    private static ContainerKey key(ServerLevel level, BlockPos pos) {
        return new ContainerKey(
                level.dimension().identifier(),
                pos.asLong()
        );
    }

    private static int countInList(
            List<OwnedItem> items,
            ItemStack wanted
    ) {
        if (items == null) {
            return 0;
        }

        for (OwnedItem item : items) {
            if (ItemStack.isSameItemSameComponents(item.sample(), wanted)) {
                return item.count();
            }
        }

        return 0;
    }

    private static int countActual(
            List<ItemStack> items,
            ItemStack wanted
    ) {
        if (items == null) {
            return 0;
        }

        for (ItemStack item : items) {
            if (ItemStack.isSameItemSameComponents(item, wanted)) {
                return item.getCount();
            }
        }

        return 0;
    }

    private static void addToList(
            List<OwnedItem> items,
            ItemStack item,
            int count
    ) {
        if (count <= 0) {
            return;
        }

        for (int index = 0; index < items.size(); index++) {
            OwnedItem owned = items.get(index);

            if (ItemStack.isSameItemSameComponents(owned.sample(), item)) {
                items.set(
                        index,
                        new OwnedItem(owned.sample(), owned.count() + count)
                );
                return;
            }
        }

        items.add(new OwnedItem(item.copyWithCount(1), count));
    }

    private static int removeFromList(
            List<OwnedItem> items,
            ItemStack item,
            int count
    ) {
        for (int index = 0; index < items.size(); index++) {
            OwnedItem owned = items.get(index);

            if (!ItemStack.isSameItemSameComponents(owned.sample(), item)) {
                continue;
            }

            int removed = Math.min(count, owned.count());
            int remaining = owned.count() - removed;

            if (remaining <= 0) {
                items.remove(index);
            } else {
                items.set(index, new OwnedItem(owned.sample(), remaining));
            }

            return removed;
        }

        return 0;
    }

    record SerializedState(int version, List<SerializedContainer> containers) {
    }

    record SerializedContainer(
            Identifier dimension,
            long position,
            List<OwnedStack> items
    ) {
    }

    record OwnedStack(ItemStack item, int count) {
    }

    private record ContainerKey(Identifier dimension, long position) {
    }

    private record OwnedItem(ItemStack sample, int count) {
    }
}
