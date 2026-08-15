package cz.xefensor.retold.villager;

import cz.xefensor.retold.behavior.performance.RetoldAiLod;
import cz.xefensor.retold.behavior.performance.RetoldAiWorkBudget;
import cz.xefensor.retold.behavior.performance.RetoldBehaviorPerf;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

final class RetoldVillagerCommunalFoodSearch {
    static final int HORIZONTAL_RADIUS = 16;
    static final int VERTICAL_RADIUS = 4;

    private static final int VILLAGE_RADIUS = 32;
    private static final int MIN_CACHE_TICKS = 80;
    private static final double MAX_CENTER_DRIFT_SQUARED = 5.0D * 5.0D;
    private static final Map<Villager, StorageTarget> FOOD_TARGETS =
            new WeakHashMap<>();
    private static final Map<Villager, Boolean> DEFERRED_FOOD_SEARCHES =
            new WeakHashMap<>();
    private static final Map<Villager, DepositTarget> DEPOSIT_TARGETS =
            new WeakHashMap<>();
    private static final Map<Villager, Boolean> DEFERRED_DEPOSIT_SEARCHES =
            new WeakHashMap<>();
    private static final Map<Villager, ItemTarget> ITEM_TARGETS =
            new WeakHashMap<>();

    private RetoldVillagerCommunalFoodSearch() {
    }

    static synchronized BlockPos find(
            ServerLevel level,
            Villager villager,
            long gameTime,
            int cacheTicks
    ) {
        VillageContext context = villageContext(level, villager);

        if (context == null) {
            FOOD_TARGETS.remove(villager);
            DEFERRED_FOOD_SEARCHES.remove(villager);
            return null;
        }

        BlockPos center = villager.blockPosition();
        StorageTarget cached = FOOD_TARGETS.get(villager);

        if (cached != null
                && gameTime < cached.expiresAt()
                && center.distSqr(cached.center()) <= MAX_CENTER_DRIFT_SQUARED
                && context.anchor().equals(cached.villageAnchor())
                && isValidForFood(level, villager, context, cached.target())) {
            DEFERRED_FOOD_SEARCHES.remove(villager);
            RetoldBehaviorPerf.recordBlockSearchCache(true);
            return cached.target();
        }

        if (!RetoldAiWorkBudget.tryUseBlockSearch(gameTime)) {
            DEFERRED_FOOD_SEARCHES.put(villager, Boolean.TRUE);
            RetoldBehaviorPerf.recordBlockSearchCache(false);
            RetoldBehaviorPerf.recordBlockSearchBudgetSkip();
            return null;
        }

        DEFERRED_FOOD_SEARCHES.remove(villager);
        RetoldBehaviorPerf.recordBlockSearchCache(false);
        BlockPos target = scan(level, villager, context, center, null);

        FOOD_TARGETS.put(
                villager,
                new StorageTarget(
                        center.immutable(),
                        context.anchor(),
                        gameTime + Math.max(
                                MIN_CACHE_TICKS,
                                RetoldAiLod.cacheTicks(villager, cacheTicks)
                        ),
                        target
                )
        );
        return target;
    }

    static synchronized boolean isFoodSearchDeferred(Villager villager) {
        return villager != null && DEFERRED_FOOD_SEARCHES.containsKey(villager);
    }

    static synchronized BlockPos findForDeposit(
            ServerLevel level,
            Villager villager,
            ItemStack offered,
            long gameTime,
            int cacheTicks
    ) {
        if (offered == null || offered.isEmpty()) {
            DEPOSIT_TARGETS.remove(villager);
            DEFERRED_DEPOSIT_SEARCHES.remove(villager);
            return null;
        }

        VillageContext context = villageContext(level, villager);

        if (context == null) {
            DEPOSIT_TARGETS.remove(villager);
            DEFERRED_DEPOSIT_SEARCHES.remove(villager);
            return null;
        }

        BlockPos center = villager.blockPosition();
        DepositTarget cached = DEPOSIT_TARGETS.get(villager);

        if (cached != null
                && ItemStack.isSameItemSameComponents(cached.offered(), offered)
                && gameTime < cached.expiresAt()
                && center.distSqr(cached.center()) <= MAX_CENTER_DRIFT_SQUARED
                && context.anchor().equals(cached.villageAnchor())
                && isValidForDeposit(
                level,
                villager,
                context,
                cached.target(),
                offered
        )) {
            DEFERRED_DEPOSIT_SEARCHES.remove(villager);
            RetoldBehaviorPerf.recordBlockSearchCache(true);
            return cached.target();
        }

        if (!RetoldAiWorkBudget.tryUseBlockSearch(gameTime)) {
            DEFERRED_DEPOSIT_SEARCHES.put(villager, Boolean.TRUE);
            RetoldBehaviorPerf.recordBlockSearchCache(false);
            RetoldBehaviorPerf.recordBlockSearchBudgetSkip();
            return null;
        }

        DEFERRED_DEPOSIT_SEARCHES.remove(villager);
        RetoldBehaviorPerf.recordBlockSearchCache(false);
        BlockPos target = scan(level, villager, context, center, offered);

        DEPOSIT_TARGETS.put(
                villager,
                new DepositTarget(
                        center.immutable(),
                        context.anchor(),
                        gameTime + Math.max(
                                MIN_CACHE_TICKS,
                                RetoldAiLod.cacheTicks(villager, cacheTicks)
                        ),
                        target,
                        offered.copyWithCount(1)
                )
        );
        return target;
    }

    static synchronized boolean isDepositSearchDeferred(Villager villager) {
        return villager != null
                && DEFERRED_DEPOSIT_SEARCHES.containsKey(villager);
    }

    static synchronized void forget(Villager villager) {
        if (villager != null) {
            FOOD_TARGETS.remove(villager);
            DEFERRED_FOOD_SEARCHES.remove(villager);
            DEPOSIT_TARGETS.remove(villager);
            DEFERRED_DEPOSIT_SEARCHES.remove(villager);
            ITEM_TARGETS.remove(villager);
        }
    }

    static BlockPos villageAnchor(
            ServerLevel level,
            Villager villager
    ) {
        VillageContext context = villageContext(level, villager);
        return context == null ? null : context.anchor();
    }

    static synchronized BlockPos findWithItem(
            ServerLevel level,
            Villager villager,
            ItemStack wanted,
            long gameTime,
            int cacheTicks
    ) {
        return findWithItemCount(
                level,
                villager,
                wanted,
                1,
                gameTime,
                cacheTicks
        );
    }

    static synchronized BlockPos findWithItemCount(
            ServerLevel level,
            Villager villager,
            ItemStack wanted,
            int minimumCount,
            long gameTime,
            int cacheTicks
    ) {
        if (wanted == null || wanted.isEmpty()) {
            ITEM_TARGETS.remove(villager);
            return null;
        }

        int required = Math.max(1, minimumCount);

        VillageContext context = villageContext(level, villager);

        if (context == null) {
            ITEM_TARGETS.remove(villager);
            return null;
        }

        BlockPos center = villager.blockPosition();
        ItemTarget cached = ITEM_TARGETS.get(villager);

        if (cached != null
                && ItemStack.isSameItemSameComponents(cached.wanted(), wanted)
                && cached.minimumCount() == required
                && gameTime < cached.expiresAt()
                && center.distSqr(cached.center()) <= MAX_CENTER_DRIFT_SQUARED
                && context.anchor().equals(cached.villageAnchor())
                && isValidForItem(
                level,
                villager,
                context,
                cached.target(),
                wanted,
                required
        )) {
            RetoldBehaviorPerf.recordBlockSearchCache(true);
            return cached.target();
        }

        if (!RetoldAiWorkBudget.tryUseBlockSearch(gameTime)) {
            RetoldBehaviorPerf.recordBlockSearchCache(false);
            RetoldBehaviorPerf.recordBlockSearchBudgetSkip();
            return null;
        }

        RetoldBehaviorPerf.recordBlockSearchCache(false);
        BlockPos target = scanForItem(
                level,
                villager,
                context,
                center,
                wanted,
                required
        );

        ITEM_TARGETS.put(
                villager,
                new ItemTarget(
                        center.immutable(),
                        context.anchor(),
                        gameTime + Math.max(
                                MIN_CACHE_TICKS,
                                RetoldAiLod.cacheTicks(villager, cacheTicks)
                        ),
                        target,
                        wanted.copyWithCount(1),
                        required
                )
        );
        return target;
    }

    static int takeOne(
            ServerLevel level,
            Villager villager,
            BlockPos pos,
            ItemStack wanted
    ) {
        VillageContext context = villageContext(level, villager);

        if (context == null
                || !isValidForItem(level, villager, context, pos, wanted)) {
            return 0;
        }

        Container container = containerAt(level, pos);
        RetoldVillageContainerOwnership.SystemMutation ownershipMutation =
                RetoldVillageContainerOwnership.beginSystemMutation(container);

        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stored = container.getItem(slot);

            if (!stored.isEmpty()
                    && ItemStack.isSameItemSameComponents(stored, wanted)) {
                stored.shrink(1);

                if (stored.isEmpty()) {
                    container.setItem(slot, ItemStack.EMPTY);
                }

                container.setChanged();
                ITEM_TARGETS.remove(villager);
                RetoldVillageContainerOwnership.finishSystemMutation(
                        level,
                        ownershipMutation,
                        false
                );
                return 1;
            }
        }

        RetoldVillageContainerOwnership.finishSystemMutation(
                level,
                ownershipMutation,
                false
        );

        return 0;
    }

    static boolean isVillageStorageWithFood(
            ServerLevel level,
            Villager villager,
            BlockPos pos
    ) {
        VillageContext context = villageContext(level, villager);
        return context != null
                && isValidForFood(level, villager, context, pos);
    }

    static boolean isVillageStorageWithSpace(
            ServerLevel level,
            Villager villager,
            BlockPos pos,
            ItemStack offered
    ) {
        VillageContext context = villageContext(level, villager);
        return context != null
                && isValidForDeposit(
                level,
                villager,
                context,
                pos,
                offered
        );
    }

    static int stockPersonalFood(
            ServerLevel level,
            Villager villager,
            BlockPos pos,
            int targetFoodPoints
    ) {
        if (targetFoodPoints <= 0
                || !isVillageStorageWithFood(level, villager, pos)) {
            return 0;
        }

        Container container = containerAt(level, pos);

        if (container == null) {
            return 0;
        }

        SimpleContainer inventory = villager.getInventory();
        int foodPoints = foodPoints(inventory);

        if (foodPoints >= targetFoodPoints) {
            return 0;
        }

        RetoldVillageContainerOwnership.SystemMutation ownershipMutation =
                RetoldVillageContainerOwnership.beginSystemMutation(container);

        boolean[] skippedSlots = new boolean[container.getContainerSize()];
        int movedTotal = 0;

        while (foodPoints < targetFoodPoints) {
            int slot = bestFoodSlot(container, skippedSlots);

            if (slot < 0) {
                break;
            }

            ItemStack stored = container.getItem(slot);
            int itemPoints = Villager.FOOD_POINTS.getOrDefault(
                    stored.getItem(),
                    0
            );
            int needed = Math.max(
                    1,
                    (targetFoodPoints - foodPoints + itemPoints - 1)
                            / itemPoints
            );
            int offeredCount = Math.min(stored.getCount(), needed);
            ItemStack remaining = inventory.addItem(
                    stored.copyWithCount(offeredCount)
            );
            int accepted = offeredCount - remaining.getCount();

            if (accepted <= 0) {
                skippedSlots[slot] = true;
                continue;
            }

            stored.shrink(accepted);
            movedTotal += accepted;
            foodPoints += accepted * itemPoints;

            if (stored.isEmpty()) {
                container.setItem(slot, ItemStack.EMPTY);
            }
        }

        if (movedTotal > 0) {
            inventory.setChanged();
            container.setChanged();
        }

        RetoldVillageContainerOwnership.finishSystemMutation(
                level,
                ownershipMutation,
                false
        );

        return movedTotal;
    }

    private static BlockPos scan(
            ServerLevel level,
            Villager villager,
            VillageContext context,
            BlockPos center,
            ItemStack offered
    ) {
        int minChunkX = Math.floorDiv(center.getX() - HORIZONTAL_RADIUS, 16);
        int maxChunkX = Math.floorDiv(center.getX() + HORIZONTAL_RADIUS, 16);
        int minChunkZ = Math.floorDiv(center.getZ() - HORIZONTAL_RADIUS, 16);
        int maxChunkZ = Math.floorDiv(center.getZ() + HORIZONTAL_RADIUS, 16);
        BlockPos best = null;
        double bestScore = Double.MAX_VALUE;
        long positionsChecked = 0L;

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                if (!level.hasChunk(chunkX, chunkZ)) {
                    continue;
                }

                for (BlockPos pos : level.getChunk(chunkX, chunkZ).getBlockEntities().keySet()) {
                    positionsChecked++;

                    if (!isInsideSearch(center, pos)
                            || !isValidForPurpose(
                            level,
                            villager,
                            context,
                            pos,
                            offered
                    )) {
                        continue;
                    }

                    double score = center.distSqr(pos);

                    if (score < bestScore) {
                        bestScore = score;
                        best = pos.immutable();
                    }
                }
            }
        }

        RetoldBehaviorPerf.recordBlockTargetPositionsChecked(positionsChecked);
        return best;
    }

    private static BlockPos scanForItem(
            ServerLevel level,
            Villager villager,
            VillageContext context,
            BlockPos center,
            ItemStack wanted,
            int minimumCount
    ) {
        int minChunkX = Math.floorDiv(center.getX() - HORIZONTAL_RADIUS, 16);
        int maxChunkX = Math.floorDiv(center.getX() + HORIZONTAL_RADIUS, 16);
        int minChunkZ = Math.floorDiv(center.getZ() - HORIZONTAL_RADIUS, 16);
        int maxChunkZ = Math.floorDiv(center.getZ() + HORIZONTAL_RADIUS, 16);
        BlockPos best = null;
        double bestScore = Double.MAX_VALUE;
        long positionsChecked = 0L;

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                if (!level.hasChunk(chunkX, chunkZ)) {
                    continue;
                }

                for (BlockPos pos : level.getChunk(chunkX, chunkZ)
                        .getBlockEntities().keySet()) {
                    positionsChecked++;

                    if (!isInsideSearch(center, pos)
                            || !isValidForItem(
                            level,
                            villager,
                            context,
                            pos,
                            wanted,
                            minimumCount
                    )) {
                        continue;
                    }

                    double score = center.distSqr(pos);

                    if (score < bestScore) {
                        bestScore = score;
                        best = pos.immutable();
                    }
                }
            }
        }

        RetoldBehaviorPerf.recordBlockTargetPositionsChecked(positionsChecked);
        return best;
    }

    private static boolean isInsideSearch(BlockPos center, BlockPos pos) {
        int dx = pos.getX() - center.getX();
        int dy = Math.abs(pos.getY() - center.getY());
        int dz = pos.getZ() - center.getZ();

        return dy <= VERTICAL_RADIUS
                && dx * dx + dz * dz <= HORIZONTAL_RADIUS * HORIZONTAL_RADIUS;
    }

    private static boolean isValidForPurpose(
            ServerLevel level,
            Villager villager,
            VillageContext context,
            BlockPos pos,
            ItemStack offered
    ) {
        if (offered == null) {
            return isValidForFood(level, villager, context, pos);
        }

        return isValidForDeposit(
                level,
                villager,
                context,
                pos,
                offered
        );
    }

    private static boolean isValidForFood(
            ServerLevel level,
            Villager villager,
            VillageContext context,
            BlockPos pos
    ) {
        if (pos == null) {
            return true;
        }

        if (level.isOutsideBuildHeight(pos)
                || pos.distSqr(context.anchor()) > VILLAGE_RADIUS * VILLAGE_RADIUS
                || !isSupportedStorage(level, pos)
                || !hasVillagerFood(containerAt(level, pos))) {
            return false;
        }

        return RetoldVillagerCommunalFood.findAccessPos(level, villager, pos) != null;
    }

    private static boolean isValidForDeposit(
            ServerLevel level,
            Villager villager,
            VillageContext context,
            BlockPos pos,
            ItemStack offered
    ) {
        if (pos == null) {
            return true;
        }

        if (offered == null
                || offered.isEmpty()
                || level.isOutsideBuildHeight(pos)
                || pos.distSqr(context.anchor())
                > VILLAGE_RADIUS * VILLAGE_RADIUS
                || !isSupportedStorage(level, pos)
                || !hasRoomFor(containerAt(level, pos), offered)) {
            return false;
        }

        return RetoldVillagerCommunalFood.findAccessPos(
                level,
                villager,
                pos
        ) != null;
    }

    private static boolean isValidForItem(
            ServerLevel level,
            Villager villager,
            VillageContext context,
            BlockPos pos,
            ItemStack wanted
    ) {
        return isValidForItem(
                level,
                villager,
                context,
                pos,
                wanted,
                1
        );
    }

    private static boolean isValidForItem(
            ServerLevel level,
            Villager villager,
            VillageContext context,
            BlockPos pos,
            ItemStack wanted,
            int minimumCount
    ) {
        if (pos == null) {
            return true;
        }

        if (wanted == null
                || wanted.isEmpty()
                || level.isOutsideBuildHeight(pos)
                || pos.distSqr(context.anchor())
                > VILLAGE_RADIUS * VILLAGE_RADIUS
                || !isSupportedStorage(level, pos)
                || !hasItemCount(
                containerAt(level, pos),
                wanted,
                minimumCount
        )) {
            return false;
        }

        return RetoldVillagerCommunalFood.findAccessPos(
                level,
                villager,
                pos
        ) != null;
    }

    private static boolean isSupportedStorage(ServerLevel level, BlockPos pos) {
        var state = level.getBlockState(pos);

        if (state.getBlock() instanceof ChestBlock) {
            return !ChestBlock.isChestBlockedAt(level, pos);
        }

        return state.getBlock() instanceof BarrelBlock;
    }

    static Container containerAt(ServerLevel level, BlockPos pos) {
        var state = level.getBlockState(pos);

        if (state.getBlock() instanceof ChestBlock chest) {
            return ChestBlock.getContainer(
                    chest,
                    state,
                    level,
                    pos,
                    false
            );
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity instanceof Container container ? container : null;
    }

    private static boolean hasVillagerFood(Container container) {
        return container != null && bestFoodSlot(container) >= 0;
    }

    private static boolean hasItemCount(
            Container container,
            ItemStack wanted,
            int minimumCount
    ) {
        if (container == null || wanted == null || wanted.isEmpty()) {
            return false;
        }

        int found = 0;

        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stored = container.getItem(slot);

            if (!stored.isEmpty()
                    && ItemStack.isSameItemSameComponents(stored, wanted)) {
                found += stored.getCount();

                if (found >= Math.max(1, minimumCount)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static int bestFoodSlot(Container container) {
        return bestFoodSlot(
                container,
                new boolean[container.getContainerSize()]
        );
    }

    private static int bestFoodSlot(
            Container container,
            boolean[] skippedSlots
    ) {
        int bestSlot = -1;
        int bestPoints = 0;

        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (skippedSlots != null
                    && slot < skippedSlots.length
                    && skippedSlots[slot]) {
                continue;
            }

            ItemStack stack = container.getItem(slot);
            int points = stack.isEmpty()
                    ? 0
                    : Villager.FOOD_POINTS.getOrDefault(stack.getItem(), 0);

            if (points > bestPoints) {
                bestPoints = points;
                bestSlot = slot;
            }
        }

        return bestSlot;
    }

    private static int foodPoints(Container container) {
        int points = 0;

        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            int itemPoints = stack.isEmpty()
                    ? 0
                    : Villager.FOOD_POINTS.getOrDefault(stack.getItem(), 0);
            points += itemPoints * stack.getCount();
        }

        return points;
    }

    private static boolean hasRoomFor(
            Container container,
            ItemStack offered
    ) {
        if (container == null || offered == null || offered.isEmpty()) {
            return false;
        }

        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (!container.canPlaceItem(slot, offered)) {
                continue;
            }

            ItemStack stored = container.getItem(slot);

            if (stored.isEmpty()) {
                return true;
            }

            if (ItemStack.isSameItemSameComponents(stored, offered)
                    && stored.getCount() < Math.min(
                    stored.getMaxStackSize(),
                    container.getMaxStackSize(stored)
            )) {
                return true;
            }
        }

        return false;
    }

    private static VillageContext villageContext(
            ServerLevel level,
            Villager villager
    ) {
        if (level == null || villager == null || villager.level() != level) {
            return null;
        }

        for (MemoryModuleType<GlobalPos> memory : villageMemories()) {
            Optional<GlobalPos> remembered = villager.getBrain().getMemory(memory);

            if (remembered.isPresent()
                    && remembered.get().dimension().equals(level.dimension())) {
                return new VillageContext(remembered.get().pos().immutable());
            }
        }

        if (level.isCloseToVillage(villager.blockPosition(), 1)) {
            return new VillageContext(villager.blockPosition().immutable());
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private static MemoryModuleType<GlobalPos>[] villageMemories() {
        return new MemoryModuleType[]{
                MemoryModuleType.HOME,
                MemoryModuleType.MEETING_POINT,
                MemoryModuleType.JOB_SITE
        };
    }

    private record VillageContext(BlockPos anchor) {
    }

    private record StorageTarget(
            BlockPos center,
            BlockPos villageAnchor,
            long expiresAt,
            BlockPos target
    ) {
    }

    private record DepositTarget(
            BlockPos center,
            BlockPos villageAnchor,
            long expiresAt,
            BlockPos target,
            ItemStack offered
    ) {
    }

    private record ItemTarget(
            BlockPos center,
            BlockPos villageAnchor,
            long expiresAt,
            BlockPos target,
            ItemStack wanted,
            int minimumCount
    ) {
    }
}
