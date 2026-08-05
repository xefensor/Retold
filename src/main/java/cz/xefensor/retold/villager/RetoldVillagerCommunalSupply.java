package cz.xefensor.retold.villager;

import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.control.RetoldAiControlMode;
import cz.xefensor.retold.behavior.control.RetoldAiControlOwner;
import cz.xefensor.retold.behavior.control.RetoldAiPriorities;
import cz.xefensor.retold.behavior.core.RetoldBehaviorCoordinator;
import cz.xefensor.retold.behavior.core.RetoldBehaviorMovement;
import cz.xefensor.retold.behavior.food.RetoldFeedingPose;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;
import cz.xefensor.retold.behavior.profiles.RetoldMobState;
import cz.xefensor.retold.behavior.profiles.RetoldMobStates;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public final class RetoldVillagerCommunalSupply {
    static final int PERSONAL_FOOD_RESERVE = 24;

    private static final int CACHE_TICKS = 100;
    private static final int PATH_INTERVAL_TICKS = 8;
    private static final int CONTROL_TICKS = 20 * 5;
    private static final int CONTROL_PRIORITY = RetoldAiPriorities.below(
            RetoldAiPriorities.SEARCH,
            1
    );
    private static final double MOVEMENT_SPEED = 0.5D;
    private static final double ACCESS_DISTANCE_SQUARED = 1.25D * 1.25D;
    private static final String CONTROL_REASON = "stock_village_food_storage";

    private RetoldVillagerCommunalSupply() {
    }

    public static void tick(
            ServerLevel level,
            Villager villager,
            long gameTime
    ) {
        if (!isUsable(level, villager)
                || RetoldFeedingPose.tick(villager, gameTime)) {
            clearOwnedMovement(villager);
            return;
        }

        RetoldMobState state = RetoldMobStates.getOrCreate(villager, gameTime);

        if (RetoldMobRules.hasEatDrive(villager, state)
                || RetoldVillagerCommunalFood.hasUrgentVanillaActivity(villager)
                || !canUseCurrentActivity(villager)
                || !canUseCommunalControl(villager)) {
            clearOwnedMovement(villager);
            return;
        }

        ItemStack surplus = firstSurplus(villager);

        if (surplus.isEmpty()) {
            clearOwnedMovement(villager);
            return;
        }

        BlockPos storagePos = RetoldVillagerCommunalFoodSearch.findForDeposit(
                level,
                villager,
                surplus,
                gameTime,
                CACHE_TICKS
        );

        if (storagePos == null) {
            clearOwnedMovement(villager);
            return;
        }

        BlockPos accessPos = RetoldVillagerCommunalFood.findAccessPos(
                level,
                villager,
                storagePos
        );

        if (accessPos == null) {
            RetoldVillagerCommunalFoodSearch.forget(villager);
            clearOwnedMovement(villager);
            return;
        }

        if (villager.distanceToSqr(Vec3.atBottomCenterOf(accessPos))
                <= ACCESS_DISTANCE_SQUARED) {
            tryDeposit(level, villager, storagePos);
            return;
        }

        if (!RetoldAiControl.tryClaim(
                villager,
                RetoldAiControlMode.SEARCH,
                RetoldAiControlOwner.VILLAGER_COMMUNAL,
                CONTROL_PRIORITY,
                CONTROL_REASON,
                gameTime,
                CONTROL_TICKS
        )) {
            return;
        }

        if (!RetoldBehaviorMovement.throttledMoveToExact(
                villager,
                accessPos,
                MOVEMENT_SPEED,
                gameTime,
                PATH_INTERVAL_TICKS,
                1.5D * 1.5D
        )) {
            clearOwnedMovement(villager);
        }
    }

    static int tryDeposit(
            ServerLevel level,
            Villager villager,
            BlockPos storagePos
    ) {
        if (!isUsable(level, villager)) {
            return 0;
        }

        ItemStack surplus = firstSurplus(villager);

        if (surplus.isEmpty()
                || !RetoldVillagerCommunalFoodSearch.isVillageStorageWithSpace(
                level,
                villager,
                storagePos,
                surplus
        )) {
            return 0;
        }

        BlockPos accessPos = RetoldVillagerCommunalFood.findAccessPos(
                level,
                villager,
                storagePos
        );

        if (accessPos == null
                || villager.distanceToSqr(Vec3.atBottomCenterOf(accessPos))
                > ACCESS_DISTANCE_SQUARED) {
            return 0;
        }

        Container storage = RetoldVillagerCommunalFoodSearch.containerAt(
                level,
                storagePos
        );
        RetoldVillageContainerOwnership.SystemMutation ownershipMutation =
                RetoldVillageContainerOwnership.beginSystemMutation(storage);
        int moved = moveSurplus(villager.getInventory(), storage);
        RetoldVillageContainerOwnership.finishSystemMutation(
                level,
                ownershipMutation,
                true
        );

        if (moved <= 0) {
            RetoldVillagerCommunalFoodSearch.forget(villager);
            return 0;
        }

        villager.getNavigation().stop();
        clearOwnedMovement(villager);
        RetoldVillagerCommunalFoodSearch.forget(villager);
        villager.getLookControl().setLookAt(Vec3.atCenterOf(storagePos));
        villager.swing(InteractionHand.MAIN_HAND);
        return moved;
    }

    static int foodPointsInInventory(Villager villager) {
        if (villager == null) {
            return 0;
        }

        int points = 0;
        SimpleContainer inventory = villager.getInventory();

        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            int itemPoints = stack.isEmpty()
                    ? 0
                    : Villager.FOOD_POINTS.getOrDefault(stack.getItem(), 0);
            points += itemPoints * stack.getCount();
        }

        return points;
    }

    private static int moveSurplus(
            SimpleContainer inventory,
            Container storage
    ) {
        if (inventory == null || storage == null) {
            return 0;
        }

        int foodPoints = foodPointsInInventory(inventory);
        int movedTotal = 0;

        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack source = inventory.getItem(slot);
            int itemPoints = source.isEmpty()
                    ? 0
                    : Villager.FOOD_POINTS.getOrDefault(source.getItem(), 0);

            if (itemPoints <= 0) {
                continue;
            }

            int allowed = Math.min(
                    source.getCount(),
                    Math.max(
                            0,
                            (foodPoints - PERSONAL_FOOD_RESERVE) / itemPoints
                    )
            );
            int moved = insert(storage, source, allowed);

            if (moved <= 0) {
                continue;
            }

            movedTotal += moved;
            foodPoints -= moved * itemPoints;

            if (source.isEmpty()) {
                inventory.setItem(slot, ItemStack.EMPTY);
            }
        }

        if (movedTotal > 0) {
            inventory.setChanged();
            storage.setChanged();
        }

        return movedTotal;
    }

    private static int foodPointsInInventory(SimpleContainer inventory) {
        int points = 0;

        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            int itemPoints = stack.isEmpty()
                    ? 0
                    : Villager.FOOD_POINTS.getOrDefault(stack.getItem(), 0);
            points += itemPoints * stack.getCount();
        }

        return points;
    }

    private static int insert(
            Container storage,
            ItemStack source,
            int limit
    ) {
        int requested = Math.min(source.getCount(), Math.max(0, limit));
        int remaining = requested;

        for (int slot = 0;
             slot < storage.getContainerSize() && remaining > 0;
             slot++) {
            ItemStack stored = storage.getItem(slot);

            if (stored.isEmpty()
                    || !ItemStack.isSameItemSameComponents(stored, source)
                    || !storage.canPlaceItem(slot, source)) {
                continue;
            }

            int capacity = Math.min(
                    stored.getMaxStackSize(),
                    storage.getMaxStackSize(stored)
            ) - stored.getCount();
            int moved = Math.min(remaining, Math.max(0, capacity));

            if (moved > 0) {
                stored.grow(moved);
                source.shrink(moved);
                remaining -= moved;
            }
        }

        for (int slot = 0;
             slot < storage.getContainerSize() && remaining > 0;
             slot++) {
            if (!storage.getItem(slot).isEmpty()
                    || !storage.canPlaceItem(slot, source)) {
                continue;
            }

            int moved = Math.min(
                    remaining,
                    Math.min(
                            source.getMaxStackSize(),
                            storage.getMaxStackSize(source)
                    )
            );

            if (moved > 0) {
                storage.setItem(slot, source.copyWithCount(moved));
                source.shrink(moved);
                remaining -= moved;
            }
        }

        return requested - remaining;
    }

    private static ItemStack firstSurplus(Villager villager) {
        int foodPoints = foodPointsInInventory(villager);

        if (foodPoints <= PERSONAL_FOOD_RESERVE) {
            return ItemStack.EMPTY;
        }

        SimpleContainer inventory = villager.getInventory();

        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            int itemPoints = stack.isEmpty()
                    ? 0
                    : Villager.FOOD_POINTS.getOrDefault(stack.getItem(), 0);

            if (itemPoints > 0
                    && foodPoints - itemPoints >= PERSONAL_FOOD_RESERVE) {
                return stack.copyWithCount(1);
            }
        }

        return ItemStack.EMPTY;
    }

    private static boolean isUsable(ServerLevel level, Villager villager) {
        return level != null
                && villager != null
                && villager.level() == level
                && villager.isAlive()
                && !villager.isRemoved()
                && !villager.isNoAi()
                && !villager.isBaby()
                && !villager.isSleeping()
                && villager.getTradingPlayer() == null
                && villager.getVillagerData()
                .profession()
                .is(VillagerProfession.FARMER);
    }

    private static boolean canUseCurrentActivity(Villager villager) {
        var activity = villager.getBrain().getActiveNonCoreActivity();

        return activity.isEmpty()
                || activity.get() == Activity.IDLE
                || activity.get() == Activity.MEET
                || activity.get() == Activity.WORK;
    }

    private static boolean canUseCommunalControl(Villager villager) {
        if (!RetoldBehaviorCoordinator.isUsableMob(villager)
                || RetoldBehaviorCoordinator.hasLiveTarget(villager)) {
            return false;
        }

        RetoldAiControlMode mode = RetoldAiControl.getMode(villager);
        return mode == RetoldAiControlMode.NONE
                || RetoldAiControl.isControlledAsBy(
                villager,
                RetoldAiControlMode.SEARCH,
                RetoldAiControlOwner.VILLAGER_COMMUNAL
        );
    }

    private static void clearOwnedMovement(Villager villager) {
        if (villager == null || !RetoldAiControl.isControlledBy(
                villager,
                RetoldAiControlOwner.VILLAGER_COMMUNAL
        )) {
            return;
        }

        RetoldAiControl.clearIfControlledAsByAny(
                villager,
                RetoldAiControlOwner.VILLAGER_COMMUNAL,
                RetoldAiControlMode.SEARCH
        );
        RetoldBehaviorMovement.stopOwnedMovement(
                villager,
                RetoldAiControlOwner.VILLAGER_COMMUNAL
        );
    }
}
