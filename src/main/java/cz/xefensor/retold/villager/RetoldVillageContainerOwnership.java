package cz.xefensor.retold.villager;

import cz.xefensor.retold.combat.RetoldAiTargets;
import cz.xefensor.retold.mixin.CompoundContainerAccessor;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tracks village-origin quantities without modifying vanilla ItemStacks.
 * Player additions remain unprotected even when they merge with protected
 * items of the same type and components.
 */
public final class RetoldVillageContainerOwnership {
    private static final String VILLAGE_LOOT_PREFIX = "chests/village/";

    private static final ThreadLocal<Map<RandomizableContainer, PendingLoot>>
            PENDING_LOOT = ThreadLocal.withInitial(IdentityHashMap::new);

    private RetoldVillageContainerOwnership() {
    }

    public static void beforeLootUnpack(RandomizableContainer container) {
        if (container == null) {
            return;
        }

        Map<RandomizableContainer, PendingLoot> pending = PENDING_LOOT.get();
        PendingLoot existing = pending.get(container);

        if (existing != null) {
            pending.put(container, existing.withDepth(existing.depth() + 1));
            return;
        }

        ResourceKey<LootTable> lootTable = container.getLootTable();

        if (isVillageLootTable(lootTable)) {
            pending.put(container, new PendingLoot(1));
        }
    }

    public static void afterLootUnpack(RandomizableContainer container) {
        if (container == null) {
            return;
        }

        Map<RandomizableContainer, PendingLoot> pending = PENDING_LOOT.get();
        PendingLoot remembered = pending.get(container);

        if (remembered == null) {
            return;
        }

        if (remembered.depth() > 1) {
            pending.put(
                    container,
                    remembered.withDepth(remembered.depth() - 1)
            );
            return;
        }

        pending.remove(container);

        if (pending.isEmpty()) {
            PENDING_LOOT.remove();
        }

        if (container.getLootTable() != null
                || !(container instanceof BaseContainerBlockEntity storage)
                || !(storage.getLevel() instanceof ServerLevel level)
                || !isVillageStorage(storage)) {
            return;
        }

        RetoldVillageContainerOwnershipData.get(level).replace(
                level,
                storage.getBlockPos(),
                snapshot(storage).items()
        );
        RetoldVillageStorageKnowledge.observe(
                level,
                storage.getBlockPos(),
                storage
        );
    }

    /**
     * Keeps already-known or village-local storage knowledge current when
     * vanilla, players, hoppers, or another mod changes a chest or barrel.
     */
    public static void onContainerChanged(BlockEntity blockEntity) {
        if (!(blockEntity instanceof BaseContainerBlockEntity storage)
                || !(storage.getLevel() instanceof ServerLevel level)
                || !isVillageStorage(storage)) {
            return;
        }

        BlockPos pos = storage.getBlockPos();
        RetoldVillageStorageKnowledge knowledge =
                RetoldVillageStorageKnowledge.get(level);

        if (knowledge.knowsStorage(level, pos)
                || RetoldVillageContainerOwnershipData.get(level)
                .totalOwned(level, pos) > 0
                || level.isCloseToVillage(pos, 1)) {
            RetoldVillageStorageKnowledge.observe(level, pos, storage);
        }
    }

    public static PlayerTransaction beginPlayerTransaction(
            Player player,
            List<Slot> slots
    ) {
        if (!(player instanceof ServerPlayer serverPlayer)
                || !(player.level() instanceof ServerLevel level)
                || RetoldAiTargets.isInvalidPlayerTarget(player)) {
            return null;
        }

        Map<BaseContainerBlockEntity, InventorySnapshot> before =
                new IdentityHashMap<>();

        for (BaseContainerBlockEntity storage : storagesFromSlots(slots)) {
            InventorySnapshot contents = snapshot(storage);
            reconcile(level, storage.getBlockPos(), contents);
            before.put(storage, contents);
        }

        if (before.isEmpty()) {
            return null;
        }

        return new PlayerTransaction(serverPlayer, before);
    }

    public static void finishPlayerTransaction(PlayerTransaction transaction) {
        if (transaction == null
                || !(transaction.player().level() instanceof ServerLevel level)) {
            return;
        }

        int stolen = 0;

        for (Map.Entry<BaseContainerBlockEntity, InventorySnapshot> entry
                : transaction.before().entrySet()) {
            BaseContainerBlockEntity storage = entry.getKey();

            if (storage.isRemoved() || storage.getLevel() != level) {
                continue;
            }

            InventorySnapshot after = snapshot(storage);
            stolen += applyPlayerChanges(
                    level,
                    storage.getBlockPos(),
                    entry.getValue(),
                    after
            );
            RetoldVillageStorageKnowledge.observe(
                    level,
                    storage.getBlockPos(),
                    storage
            );
        }

        if (stolen > 0) {
            RetoldVillageWitnessReputation.report(
                    level,
                    transaction.player(),
                    transaction.actionPosition(),
                    RetoldVillageWitnessReputation.Offense.STORAGE_THEFT
            );
        }
    }

    static SystemMutation beginSystemMutation(Container container) {
        Map<BaseContainerBlockEntity, InventorySnapshot> before =
                new IdentityHashMap<>();

        for (BaseContainerBlockEntity storage : storages(container)) {
            before.put(storage, snapshot(storage));
        }

        return new SystemMutation(before);
    }

    static void finishSystemMutation(
            ServerLevel level,
            SystemMutation mutation,
            boolean additionsAreVillageOwned
    ) {
        if (level == null || mutation == null) {
            return;
        }

        for (Map.Entry<BaseContainerBlockEntity, InventorySnapshot> entry
                : mutation.before().entrySet()) {
            BaseContainerBlockEntity storage = entry.getKey();

            if (storage.isRemoved() || storage.getLevel() != level) {
                continue;
            }

            InventorySnapshot after = snapshot(storage);
            applySystemChanges(
                    level,
                    storage.getBlockPos(),
                    entry.getValue(),
                    after,
                    additionsAreVillageOwned
            );
            RetoldVillageStorageKnowledge.observe(
                    level,
                    storage.getBlockPos(),
                    storage
            );
        }
    }

    static int handleProtectedContainerBreak(
            ServerLevel level,
            BlockPos pos,
            ServerPlayer player
    ) {
        if (level == null || pos == null) {
            return 0;
        }

        RetoldVillageContainerOwnershipData data =
                RetoldVillageContainerOwnershipData.get(level);
        RetoldVillageStorageKnowledge.forget(level, pos);
        if (level.getBlockEntity(pos)
                instanceof BaseContainerBlockEntity storage
                && isVillageStorage(storage)) {
            reconcile(level, pos, snapshot(storage));
        }
        int protectedItems = data.totalOwned(level, pos);

        if (protectedItems <= 0) {
            return 0;
        }

        data.clear(level, pos);

        if (player != null && !RetoldAiTargets.isInvalidPlayerTarget(player)) {
            RetoldVillageWitnessReputation.report(
                    level,
                    player,
                    pos,
                    RetoldVillageWitnessReputation.Offense.STORAGE_BREAK
            );
        }

        return protectedItems;
    }

    public static int ownedCount(
            ServerLevel level,
            BlockPos pos,
            ItemStack item
    ) {
        return RetoldVillageContainerOwnershipData.get(level).ownedCount(
                level,
                pos,
                item
        );
    }

    static int totalOwned(ServerLevel level, BlockPos pos) {
        return RetoldVillageContainerOwnershipData.get(level).totalOwned(
                level,
                pos
        );
    }

    static void markVillageOwned(
            ServerLevel level,
            BlockPos pos,
            ItemStack item,
            int count
    ) {
        RetoldVillageContainerOwnershipData.get(level).add(
                level,
                pos,
                item,
                count
        );
    }

    static void clear(ServerLevel level, BlockPos pos) {
        RetoldVillageContainerOwnershipData.get(level).clear(level, pos);
    }

    private static int applyPlayerChanges(
            ServerLevel level,
            BlockPos pos,
            InventorySnapshot before,
            InventorySnapshot after
    ) {
        RetoldVillageContainerOwnershipData data =
                RetoldVillageContainerOwnershipData.get(level);
        int stolen = 0;

        for (ItemStack item : combinedSamples(before, after)) {
            int beforeCount = before.count(item);
            int afterCount = after.count(item);

            if (afterCount >= beforeCount) {
                continue;
            }

            int owned = data.ownedCount(level, pos, item);
            int unowned = Math.max(0, beforeCount - owned);
            int removed = beforeCount - afterCount;
            int stolenForItem = Math.max(0, removed - unowned);

            if (stolenForItem > 0) {
                stolen += data.remove(level, pos, item, stolenForItem);
            }
        }

        reconcile(level, pos, after);
        return stolen;
    }

    private static void applySystemChanges(
            ServerLevel level,
            BlockPos pos,
            InventorySnapshot before,
            InventorySnapshot after,
            boolean additionsAreVillageOwned
    ) {
        RetoldVillageContainerOwnershipData data =
                RetoldVillageContainerOwnershipData.get(level);
        reconcile(level, pos, before);

        for (ItemStack item : combinedSamples(before, after)) {
            int change = after.count(item) - before.count(item);

            if (change > 0 && additionsAreVillageOwned) {
                data.add(level, pos, item, change);
            } else if (change < 0) {
                data.remove(level, pos, item, -change);
            }
        }

        reconcile(level, pos, after);
    }

    private static void reconcile(
            ServerLevel level,
            BlockPos pos,
            InventorySnapshot actual
    ) {
        RetoldVillageContainerOwnershipData.get(level).reconcile(
                level,
                pos,
                actual.items()
        );
    }

    private static Set<BaseContainerBlockEntity> storagesFromSlots(
            List<Slot> slots
    ) {
        Set<BaseContainerBlockEntity> found =
                Collections.newSetFromMap(new IdentityHashMap<>());

        if (slots == null) {
            return found;
        }

        for (Slot slot : slots) {
            if (slot != null) {
                found.addAll(storages(slot.container));
            }
        }

        return found;
    }

    private static Set<BaseContainerBlockEntity> storages(
            Container container
    ) {
        Set<BaseContainerBlockEntity> found =
                Collections.newSetFromMap(new IdentityHashMap<>());
        collectStorages(container, found);
        return found;
    }

    private static void collectStorages(
            Container container,
            Set<BaseContainerBlockEntity> found
    ) {
        if (container instanceof BaseContainerBlockEntity storage) {
            if (isVillageStorage(storage)) {
                found.add(storage);
            }
            return;
        }

        if (container instanceof CompoundContainer
                && container instanceof CompoundContainerAccessor accessor) {
            collectStorages(accessor.retold$getFirstContainer(), found);
            collectStorages(accessor.retold$getSecondContainer(), found);
        }
    }

    private static boolean isVillageStorage(
            BaseContainerBlockEntity storage
    ) {
        return storage.getBlockState().getBlock() instanceof ChestBlock
                || storage.getBlockState().getBlock() instanceof BarrelBlock;
    }

    private static boolean isVillageLootTable(
            ResourceKey<LootTable> lootTable
    ) {
        return lootTable != null
                && lootTable.identifier().getPath().startsWith(
                VILLAGE_LOOT_PREFIX
        );
    }

    private static InventorySnapshot snapshot(Container container) {
        List<ItemStack> items = new ArrayList<>();

        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);

            if (!stack.isEmpty()) {
                addCount(items, stack, stack.getCount());
            }
        }

        return new InventorySnapshot(List.copyOf(items));
    }

    private static List<ItemStack> combinedSamples(
            InventorySnapshot first,
            InventorySnapshot second
    ) {
        List<ItemStack> combined = new ArrayList<>();

        for (ItemStack item : first.items()) {
            addSample(combined, item);
        }

        for (ItemStack item : second.items()) {
            addSample(combined, item);
        }

        return combined;
    }

    private static void addSample(List<ItemStack> items, ItemStack sample) {
        for (ItemStack item : items) {
            if (ItemStack.isSameItemSameComponents(item, sample)) {
                return;
            }
        }

        items.add(sample.copyWithCount(1));
    }

    private static void addCount(
            List<ItemStack> items,
            ItemStack stack,
            int count
    ) {
        for (ItemStack item : items) {
            if (ItemStack.isSameItemSameComponents(item, stack)) {
                item.grow(count);
                return;
            }
        }

        items.add(stack.copyWithCount(count));
    }

    public static final class PlayerTransaction {
        private final ServerPlayer player;
        private final Map<BaseContainerBlockEntity, InventorySnapshot> before;
        private final BlockPos actionPosition;

        private PlayerTransaction(
                ServerPlayer player,
                Map<BaseContainerBlockEntity, InventorySnapshot> before
        ) {
            this.player = player;
            this.before = before;
            actionPosition = before.keySet().iterator().next()
                    .getBlockPos().immutable();
        }

        private ServerPlayer player() {
            return player;
        }

        private Map<BaseContainerBlockEntity, InventorySnapshot> before() {
            return before;
        }

        private BlockPos actionPosition() {
            return actionPosition;
        }
    }

    static final class SystemMutation {
        private final Map<BaseContainerBlockEntity, InventorySnapshot> before;

        private SystemMutation(
                Map<BaseContainerBlockEntity, InventorySnapshot> before
        ) {
            this.before = before;
        }

        private Map<BaseContainerBlockEntity, InventorySnapshot> before() {
            return before;
        }
    }

    private record PendingLoot(int depth) {
        private PendingLoot withDepth(int newDepth) {
            return new PendingLoot(newDepth);
        }
    }

    private record InventorySnapshot(List<ItemStack> items) {
        private int count(ItemStack wanted) {
            for (ItemStack item : items) {
                if (ItemStack.isSameItemSameComponents(item, wanted)) {
                    return item.getCount();
                }
            }

            return 0;
        }
    }
}
