package cz.xefensor.retold.villager;

import cz.xefensor.retold.behavior.core.RetoldBehaviorMovement;
import cz.xefensor.retold.behavior.core.RetoldMobGriefing;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

/** Bounded real-crop production for Farmers returning after unloaded time. */
public final class RetoldUnloadedFarmerProduction {
    public static final int MAX_FARMERS_PER_TICK = 1;

    private static final int MAX_PENDING_TASKS = 4_096;
    private static final int CROP_HORIZONTAL_RADIUS = 16;
    private static final int CROP_VERTICAL_RADIUS = 4;
    private static final int MAX_CROP_CANDIDATES = 7;
    private static final int STORAGE_CACHE_TICKS = 100;

    private static final Queue<ProductionTask> PENDING = new ArrayDeque<>();
    private static final Set<UUID> QUEUED_FARMERS = new HashSet<>();

    private RetoldUnloadedFarmerProduction() {
    }

    public static synchronized void enqueue(
            ServerLevel level,
            Villager farmer,
            int unloadedDays
    ) {
        if (!isEligible(level, farmer)
                || unloadedDays <= 0
                || PENDING.size() >= MAX_PENDING_TASKS
                || !QUEUED_FARMERS.add(farmer.getUUID())) {
            return;
        }

        PENDING.add(new ProductionTask(
                level,
                farmer,
                Math.min(7, unloadedDays)
        ));
    }

    public static synchronized int processPending(int maximumFarmers) {
        int processed = 0;
        int limit = Math.max(0, maximumFarmers);

        while (processed < limit && !PENDING.isEmpty()) {
            ProductionTask task = PENDING.remove();
            QUEUED_FARMERS.remove(task.farmer().getUUID());
            processed++;

            if (!isEligible(task.level(), task.farmer())) {
                continue;
            }

            ProductionResult result = produceOneCycle(task);

            if (result == ProductionResult.DEFERRED) {
                requeue(task);
            } else if (result == ProductionResult.PRODUCED
                    && task.remainingCycles() > 1) {
                requeue(new ProductionTask(
                        task.level(),
                        task.farmer(),
                        task.remainingCycles() - 1
                ));
            }
        }

        return processed;
    }

    public static synchronized int pendingCount() {
        return PENDING.size();
    }

    public static synchronized void clear() {
        PENDING.clear();
        QUEUED_FARMERS.clear();
    }

    private static ProductionResult produceOneCycle(ProductionTask task) {
        ServerLevel level = task.level();
        Villager farmer = task.farmer();
        long gameTime = level.getGameTime();
        List<BlockPos> crops = RetoldVillageCropOwnership.ownedCropsNear(
                level,
                farmer.blockPosition(),
                CROP_HORIZONTAL_RADIUS,
                CROP_VERTICAL_RADIUS,
                MAX_CROP_CANDIDATES
        );

        if (crops.isEmpty()) {
            return ProductionResult.UNAVAILABLE;
        }

        for (BlockPos cropPos : crops) {
            BlockState current = level.getBlockState(cropPos);

            if (!(current.getBlock() instanceof CropBlock crop)) {
                RetoldVillageCropOwnership.clear(level, cropPos);
                continue;
            }

            if (!RetoldMobGriefing.canBreakBlock(level, farmer, cropPos)
                    || !RetoldMobGriefing.canPlaceBlock(level, farmer, cropPos)) {
                continue;
            }

            RetoldBehaviorMovement.ReachabilityResult cropReachability =
                    RetoldBehaviorMovement.probeReachability(
                            farmer,
                            findCropAccessPos(level, farmer, cropPos),
                            gameTime
                    );

            if (cropReachability
                    == RetoldBehaviorMovement.ReachabilityResult.DEFERRED) {
                return ProductionResult.DEFERRED;
            }

            if (cropReachability
                    != RetoldBehaviorMovement.ReachabilityResult.REACHABLE) {
                continue;
            }

            HarvestPlan harvest = prepareHarvest(
                    level,
                    farmer,
                    cropPos,
                    crop
            );

            if (harvest == null) {
                continue;
            }

            BlockPos storagePos = RetoldVillagerCommunalFoodSearch.findForDeposit(
                    level,
                    farmer,
                    harvest.offered(),
                    gameTime,
                    STORAGE_CACHE_TICKS
            );

            if (storagePos == null) {
                return RetoldVillagerCommunalFoodSearch
                        .isDepositSearchDeferred(farmer)
                        ? ProductionResult.DEFERRED
                        : ProductionResult.UNAVAILABLE;
            }

            BlockPos accessPos = RetoldVillagerCommunalFood.findAccessPos(
                    level,
                    farmer,
                    storagePos
            );
            RetoldBehaviorMovement.ReachabilityResult storageReachability =
                    RetoldBehaviorMovement.probeReachability(
                            farmer,
                            accessPos,
                            gameTime
                    );

            if (storageReachability
                    == RetoldBehaviorMovement.ReachabilityResult.DEFERRED) {
                return ProductionResult.DEFERRED;
            }

            if (storageReachability
                    != RetoldBehaviorMovement.ReachabilityResult.REACHABLE) {
                return ProductionResult.UNAVAILABLE;
            }

            return applyHarvest(
                    level,
                    farmer,
                    cropPos,
                    harvest,
                    storagePos
            );
        }

        return ProductionResult.UNAVAILABLE;
    }

    private static BlockPos findCropAccessPos(
            ServerLevel level,
            Villager farmer,
            BlockPos cropPos
    ) {
        BlockPos best = null;
        double bestDistanceSquared = Double.MAX_VALUE;

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos candidate = cropPos.relative(direction);

            if (!farmer.getNavigation().isStableDestination(candidate)
                    || !level.getBlockState(candidate)
                    .getCollisionShape(level, candidate)
                    .isEmpty()
                    || !level.getBlockState(candidate.above())
                    .getCollisionShape(level, candidate.above())
                    .isEmpty()) {
                continue;
            }

            double distanceSquared = farmer.distanceToSqr(
                    Vec3.atBottomCenterOf(candidate)
            );

            if (distanceSquared < bestDistanceSquared) {
                bestDistanceSquared = distanceSquared;
                best = candidate;
            }
        }

        return best;
    }

    private static HarvestPlan prepareHarvest(
            ServerLevel level,
            Villager farmer,
            BlockPos cropPos,
            CropBlock crop
    ) {
        BlockState mature = crop.getStateForAge(crop.getMaxAge());
        List<ItemStack> output = mutableCopies(Block.getDrops(
                mature,
                level,
                cropPos,
                null,
                farmer,
                ItemStack.EMPTY
        ));

        if (!consumeReplantItem(output, crop.asItem())) {
            return null;
        }

        SimpleContainer staged = copyInventory(farmer.getInventory());

        if (!insertAll(staged, output) || !craftBread(staged)) {
            return null;
        }

        BlockState replanted = crop.getStateForAge(0);

        if (!replanted.canSurvive(level, cropPos)) {
            return null;
        }

        ItemStack offered = RetoldVillagerCommunalSupply.firstSurplus(staged);

        if (offered.isEmpty()) {
            offered = productionFood(crop);
        }

        return offered.isEmpty()
                ? null
                : new HarvestPlan(replanted, List.copyOf(output), offered);
    }

    private static ProductionResult applyHarvest(
            ServerLevel level,
            Villager farmer,
            BlockPos cropPos,
            HarvestPlan harvest,
            BlockPos storagePos
    ) {
        boolean changed = level.setBlock(
                cropPos,
                harvest.replanted(),
                Block.UPDATE_ALL
        );

        if (!changed
                && !level.getBlockState(cropPos).equals(harvest.replanted())) {
            return ProductionResult.APPLY_FAILED;
        }

        if (!insertAll(farmer.getInventory(), harvest.output())
                || !craftBread(farmer.getInventory())) {
            return ProductionResult.APPLY_FAILED;
        }

        farmer.getInventory().setChanged();
        RetoldVillageCropOwnership.mark(level, cropPos);
        RetoldVillagerCommunalSupply.depositCatchUp(
                level,
                farmer,
                storagePos
        );
        return ProductionResult.PRODUCED;
    }

    private static ItemStack productionFood(CropBlock crop) {
        if (crop == Blocks.WHEAT) {
            return new ItemStack(Items.BREAD);
        }

        if (crop == Blocks.CARROTS
                || crop == Blocks.POTATOES
                || crop == Blocks.BEETROOTS) {
            Item item = crop.asItem();
            return new ItemStack(item);
        }

        return ItemStack.EMPTY;
    }

    private static List<ItemStack> mutableCopies(List<ItemStack> stacks) {
        List<ItemStack> copies = new ArrayList<>(stacks.size());

        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) {
                copies.add(stack.copy());
            }
        }

        return copies;
    }

    private static boolean consumeReplantItem(
            List<ItemStack> output,
            Item seed
    ) {
        for (ItemStack stack : output) {
            if (stack.is(seed)) {
                stack.shrink(1);
                return true;
            }
        }

        return false;
    }

    private static SimpleContainer copyInventory(SimpleContainer inventory) {
        SimpleContainer copy = new SimpleContainer(inventory.getContainerSize());

        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            copy.setItem(slot, inventory.getItem(slot).copy());
        }

        return copy;
    }

    private static boolean insertAll(
            SimpleContainer inventory,
            List<ItemStack> output
    ) {
        for (ItemStack stack : output) {
            if (!stack.isEmpty()
                    && !inventory.addItem(stack.copy()).isEmpty()) {
                return false;
            }
        }

        return true;
    }

    private static boolean craftBread(SimpleContainer inventory) {
        int breadCount = inventory.countItem(Items.WHEAT) / 3;

        if (breadCount <= 0) {
            return true;
        }

        ItemStack removed = inventory.removeItemType(
                Items.WHEAT,
                breadCount * 3
        );

        if (removed.getCount() != breadCount * 3) {
            return false;
        }

        return inventory.addItem(
                new ItemStack(Items.BREAD, breadCount)
        ).isEmpty();
    }

    private static boolean isEligible(
            ServerLevel level,
            Villager farmer
    ) {
        return level != null
                && farmer != null
                && farmer.level() == level
                && farmer.isAlive()
                && !farmer.isRemoved()
                && !farmer.isBaby()
                && farmer.getVillagerData()
                .profession()
                .is(VillagerProfession.FARMER);
    }

    private static void requeue(ProductionTask task) {
        UUID farmerId = task.farmer().getUUID();

        if (PENDING.size() >= MAX_PENDING_TASKS
                || !QUEUED_FARMERS.add(farmerId)) {
            return;
        }

        PENDING.add(task);
    }

    private record ProductionTask(
            ServerLevel level,
            Villager farmer,
            int remainingCycles
    ) {
    }

    private record HarvestPlan(
            BlockState replanted,
            List<ItemStack> output,
            ItemStack offered
    ) {
    }

    private enum ProductionResult {
        PRODUCED,
        UNAVAILABLE,
        APPLY_FAILED,
        DEFERRED
    }
}
