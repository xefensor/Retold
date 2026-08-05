package cz.xefensor.retold.villager;

import cz.xefensor.retold.behavior.breeding.RetoldAnimalBreeding;
import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.control.RetoldAiControlMode;
import cz.xefensor.retold.behavior.control.RetoldAiControlOwner;
import cz.xefensor.retold.behavior.control.RetoldAiPriorities;
import cz.xefensor.retold.behavior.core.RetoldBehaviorCoordinator;
import cz.xefensor.retold.behavior.core.RetoldBehaviorMovement;
import cz.xefensor.retold.behavior.performance.RetoldAiLod;
import cz.xefensor.retold.behavior.performance.RetoldAiScanCache;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;
import cz.xefensor.retold.behavior.profiles.RetoldMobStates;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Low-priority livestock tending by Shepherds, Leatherworkers, and Butchers.
 * Food is physically collected from village storage before either parent is
 * fed; the shared hunger-satisfaction owner decides later reproduction.
 */
public final class RetoldVillagerAnimalTending {
    private static final double ANIMAL_SEARCH_RADIUS = 12.0D;
    private static final double VILLAGE_RADIUS_SQUARED = 32.0D * 32.0D;
    private static final double MAX_PAIR_DISTANCE_SQUARED = 5.0D * 5.0D;
    private static final double FEED_DISTANCE_SQUARED = 3.5D * 3.5D;
    private static final double STORAGE_ACCESS_DISTANCE_SQUARED =
            1.25D * 1.25D;
    private static final int FOOD_REQUIRED = 2;
    private static final int ANIMAL_CACHE_TICKS = 80;
    private static final int STORAGE_CACHE_TICKS = 100;
    private static final int EMPTY_SEARCH_COOLDOWN_TICKS = 200;
    private static final int SUCCESS_COOLDOWN_TICKS = 1200;
    private static final int TASK_TIMEOUT_TICKS = 400;
    private static final int CONTROL_TICKS = 100;
    private static final int PATH_INTERVAL_TICKS = 8;
    private static final int CONTROL_PRIORITY = RetoldAiPriorities.below(
            RetoldAiPriorities.SEARCH,
            2
    );
    private static final double MOVEMENT_SPEED = 0.5D;
    private static final String CONTROL_REASON = "tend_village_animals";

    private static final Map<Villager, TendTask> TASKS = new WeakHashMap<>();
    private static final Map<Villager, Long> NEXT_SEARCH_AT =
            new WeakHashMap<>();

    private RetoldVillagerAnimalTending() {
    }

    public static void tick(
            ServerLevel level,
            Villager villager,
            long gameTime
    ) {
        if (!isUsable(level, villager, gameTime)) {
            cancel(villager);
            return;
        }

        TendTask task = TASKS.get(villager);

        if (task != null) {
            continueTask(level, villager, task, gameTime);
            return;
        }

        if (gameTime < NEXT_SEARCH_AT.getOrDefault(villager, 0L)
                || RetoldAiControl.getMode(villager)
                != RetoldAiControlMode.NONE) {
            return;
        }

        AnimalPair pair = findPair(level, villager, gameTime);

        if (pair == null) {
            scheduleNextSearch(
                    villager,
                    gameTime,
                    EMPTY_SEARCH_COOLDOWN_TICKS
            );
            return;
        }

        if (beginTending(level, villager, pair.first(), pair.second(), gameTime)) {
            continueTask(level, villager, TASKS.get(villager), gameTime);
        }
    }

    static boolean beginTending(
            ServerLevel level,
            Villager villager,
            Animal first,
            Animal second,
            long gameTime
    ) {
        if (level == null
                || villager == null
                || first == null
                || second == null
                || first.level() != level
                || second.level() != level
                || !isInsideVillage(level, villager, first, second)
                || first.distanceToSqr(second)
                > MAX_PAIR_DISTANCE_SQUARED
                || !isValidPair(villager, first, second)) {
            return false;
        }

        ItemStack food = RetoldVillageAnimalOwnership.tendingFood(first);
        TASKS.put(
                villager,
                new TendTask(
                        first.getUUID(),
                        second.getUUID(),
                        food.copyWithCount(1),
                        0,
                        gameTime + TASK_TIMEOUT_TICKS
                )
        );
        return true;
    }

    private static void continueTask(
            ServerLevel level,
            Villager villager,
            TendTask task,
            long gameTime
    ) {
        AnimalPair pair = resolvePair(level, villager, task);

        if (gameTime > task.expiresAt()
                || pair == null
                || !canUseControl(villager)) {
            cancel(villager);
            scheduleNextSearch(
                    villager,
                    gameTime,
                    EMPTY_SEARCH_COOLDOWN_TICKS
            );
            return;
        }

        if (!claimControl(villager, gameTime)) {
            return;
        }

        if (task.reservedFood() < FOOD_REQUIRED) {
            collectFood(level, villager, task, gameTime);
            return;
        }

        Vec3 feedingPosition = midpoint(pair.first(), pair.second());

        if (villager.distanceToSqr(pair.first()) > FEED_DISTANCE_SQUARED
                || villager.distanceToSqr(pair.second())
                > FEED_DISTANCE_SQUARED) {
            if (!RetoldBehaviorMovement.throttledMoveTo(
                    villager,
                    feedingPosition.x,
                    feedingPosition.y,
                    feedingPosition.z,
                    MOVEMENT_SPEED,
                    gameTime,
                    PATH_INTERVAL_TICKS,
                    1.5D * 1.5D
            )) {
                cancel(villager);
            }
            return;
        }

        feedPair(villager, pair, task, gameTime);
    }

    private static void collectFood(
            ServerLevel level,
            Villager villager,
            TendTask task,
            long gameTime
    ) {
        int missing = FOOD_REQUIRED - task.reservedFood();
        BlockPos storagePos = RetoldVillagerCommunalFoodSearch
                .findWithItemCount(
                        level,
                        villager,
                        task.food(),
                        missing,
                        gameTime,
                        STORAGE_CACHE_TICKS
                );

        if (storagePos == null) {
            return;
        }

        BlockPos accessPos = RetoldVillagerCommunalFood.findAccessPos(
                level,
                villager,
                storagePos
        );

        if (accessPos == null) {
            RetoldVillagerCommunalFoodSearch.forget(villager);
            return;
        }

        if (villager.distanceToSqr(Vec3.atBottomCenterOf(accessPos))
                > STORAGE_ACCESS_DISTANCE_SQUARED) {
            RetoldBehaviorMovement.throttledMoveToExact(
                    villager,
                    accessPos,
                    MOVEMENT_SPEED,
                    gameTime,
                    PATH_INTERVAL_TICKS,
                    1.5D * 1.5D
            );
            return;
        }

        int collected = takeIntoInventory(
                level,
                villager,
                storagePos,
                task.food(),
                missing
        );

        if (collected <= 0) {
            RetoldVillagerCommunalFoodSearch.forget(villager);
            return;
        }

        villager.getNavigation().stop();
        villager.getLookControl().setLookAt(Vec3.atCenterOf(storagePos));
        villager.swing(InteractionHand.MAIN_HAND);
        TendTask updated = task.withReservedFood(
                task.reservedFood() + collected
        );
        TASKS.put(villager, updated);
        RetoldVillagerCommunalFoodSearch.forget(villager);
    }

    static int takeIntoInventory(
            ServerLevel level,
            Villager villager,
            BlockPos storagePos,
            ItemStack wanted,
            int requested
    ) {
        if (level == null
                || villager == null
                || storagePos == null
                || wanted == null
                || wanted.isEmpty()) {
            return 0;
        }

        SimpleContainer inventory = villager.getInventory();
        int moved = 0;

        while (moved < Math.max(0, requested)
                && inventory.canAddItem(wanted.copyWithCount(1))) {
            int taken = RetoldVillagerCommunalFoodSearch.takeOne(
                    level,
                    villager,
                    storagePos,
                    wanted
            );

            if (taken <= 0) {
                break;
            }

            ItemStack remainder = inventory.addItem(
                    wanted.copyWithCount(1)
            );

            if (!remainder.isEmpty()) {
                break;
            }

            moved++;
        }

        if (moved > 0) {
            inventory.setChanged();
        }

        return moved;
    }

    private static void feedPair(
            Villager villager,
            AnimalPair pair,
            TendTask task,
            long gameTime
    ) {
        if (!performFeeding(
                villager,
                pair.first(),
                pair.second(),
                task.food(),
                gameTime
        )) {
            cancel(villager);
            scheduleNextSearch(
                    villager,
                    gameTime,
                    EMPTY_SEARCH_COOLDOWN_TICKS
            );
            return;
        }

        villager.getNavigation().stop();
        villager.getLookControl().setLookAt(midpoint(
                pair.first(),
                pair.second()
        ));
        villager.swing(InteractionHand.MAIN_HAND);
        TASKS.remove(villager);
        clearOwnedMovement(villager);
        scheduleNextSearch(villager, gameTime, SUCCESS_COOLDOWN_TICKS);
    }

    static boolean performFeeding(
            Villager villager,
            Animal first,
            Animal second,
            ItemStack food,
            long gameTime
    ) {
        if (!isValidPair(villager, first, second)
                || !removeItems(
                villager.getInventory(),
                food,
                FOOD_REQUIRED
        )) {
            return false;
        }

        RetoldVillageAnimalOwnership.markVillageOwned(first);
        RetoldVillageAnimalOwnership.markVillageOwned(second);
        return RetoldAnimalBreeding.feed(first, food, gameTime)
                && RetoldAnimalBreeding.feed(second, food, gameTime);
    }

    static AnimalPair findPair(
            ServerLevel level,
            Villager villager,
            long gameTime
    ) {
        BlockPos anchor = RetoldVillagerCommunalFoodSearch.villageAnchor(
                level,
                villager
        );

        if (anchor == null) {
            return null;
        }

        List<Animal> nearby = RetoldAiScanCache.nearby(
                level,
                villager,
                Animal.class,
                ANIMAL_SEARCH_RADIUS,
                gameTime,
                ANIMAL_CACHE_TICKS
        );
        AnimalPair best = null;
        double bestScore = Double.MAX_VALUE;

        for (int firstIndex = 0;
             firstIndex < nearby.size();
             firstIndex++) {
            Animal first = nearby.get(firstIndex);

            if (first.blockPosition().distSqr(anchor)
                    > VILLAGE_RADIUS_SQUARED) {
                continue;
            }

            for (int secondIndex = firstIndex + 1;
                 secondIndex < nearby.size();
                 secondIndex++) {
                Animal second = nearby.get(secondIndex);

                if (second.blockPosition().distSqr(anchor)
                        > VILLAGE_RADIUS_SQUARED
                        || first.distanceToSqr(second)
                        > MAX_PAIR_DISTANCE_SQUARED
                        || !isValidPair(villager, first, second)) {
                    continue;
                }

                double score = villager.distanceToSqr(first)
                        + villager.distanceToSqr(second);

                if (score < bestScore) {
                    bestScore = score;
                    best = new AnimalPair(
                            first,
                            second,
                            RetoldVillageAnimalOwnership.tendingFood(first)
                    );
                }
            }
        }

        return best;
    }

    private static AnimalPair resolvePair(
            ServerLevel level,
            Villager villager,
            TendTask task
    ) {
        Entity firstEntity = level.getEntityInAnyDimension(task.first());
        Entity secondEntity = level.getEntityInAnyDimension(task.second());

        if (!(firstEntity instanceof Animal first)
                || !(secondEntity instanceof Animal second)
                || first.level() != level
                || second.level() != level
                || !isInsideVillage(level, villager, first, second)
                || first.distanceToSqr(second)
                > MAX_PAIR_DISTANCE_SQUARED
                || !isValidPair(villager, first, second)) {
            return null;
        }

        return new AnimalPair(first, second, task.food());
    }

    private static boolean isInsideVillage(
            ServerLevel level,
            Villager villager,
            Animal first,
            Animal second
    ) {
        BlockPos anchor = RetoldVillagerCommunalFoodSearch.villageAnchor(
                level,
                villager
        );
        return anchor != null
                && first.blockPosition().distSqr(anchor)
                <= VILLAGE_RADIUS_SQUARED
                && second.blockPosition().distSqr(anchor)
                <= VILLAGE_RADIUS_SQUARED;
    }

    private static boolean isValidPair(
            Villager villager,
            Animal first,
            Animal second
    ) {
        return first != null
                && second != null
                && first != second
                && first.isAlive()
                && second.isAlive()
                && !first.isRemoved()
                && !second.isRemoved()
                && first.getType() == second.getType()
                && RetoldAnimalBreeding.needsTendingFood(first)
                && RetoldAnimalBreeding.needsTendingFood(second)
                && RetoldVillageAnimalOwnership.canTend(villager, first)
                && RetoldVillageAnimalOwnership.canTend(villager, second)
                && !RetoldVillageAnimalOwnership.tendingFood(first).isEmpty();
    }

    private static boolean removeItems(
            SimpleContainer inventory,
            ItemStack wanted,
            int count
    ) {
        if (inventory == null
                || wanted == null
                || wanted.isEmpty()
                || count <= 0
                || countItems(inventory, wanted) < count) {
            return false;
        }

        int remaining = count;

        for (int slot = 0;
             slot < inventory.getContainerSize() && remaining > 0;
             slot++) {
            ItemStack stored = inventory.getItem(slot);

            if (stored.isEmpty()
                    || !ItemStack.isSameItemSameComponents(stored, wanted)) {
                continue;
            }

            int removed = Math.min(remaining, stored.getCount());
            stored.shrink(removed);
            remaining -= removed;

            if (stored.isEmpty()) {
                inventory.setItem(slot, ItemStack.EMPTY);
            }
        }

        inventory.setChanged();
        return remaining == 0;
    }

    private static int countItems(
            SimpleContainer inventory,
            ItemStack wanted
    ) {
        int count = 0;

        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stored = inventory.getItem(slot);

            if (!stored.isEmpty()
                    && ItemStack.isSameItemSameComponents(stored, wanted)) {
                count += stored.getCount();
            }
        }

        return count;
    }

    private static Vec3 midpoint(Animal first, Animal second) {
        return new Vec3(
                (first.getX() + second.getX()) * 0.5D,
                Math.min(first.getY(), second.getY()),
                (first.getZ() + second.getZ()) * 0.5D
        );
    }

    private static boolean isUsable(
            ServerLevel level,
            Villager villager,
            long gameTime
    ) {
        if (level == null
                || villager == null
                || villager.level() != level
                || !villager.isAlive()
                || villager.isRemoved()
                || villager.isNoAi()
                || villager.isBaby()
                || villager.isSleeping()
                || villager.getTradingPlayer() != null
                || RetoldBehaviorCoordinator.hasLiveTarget(villager)
                || RetoldMobRules.hasEatDrive(
                villager,
                RetoldMobStates.getOrCreate(villager, gameTime)
        )
                || RetoldVillagerCommunalFood.hasUrgentVanillaActivity(
                villager
        )) {
            return false;
        }

        var activity = villager.getBrain().getActiveNonCoreActivity();
        return activity.isEmpty()
                || activity.get() == Activity.IDLE
                || activity.get() == Activity.MEET
                || activity.get() == Activity.WORK;
    }

    private static boolean canUseControl(Villager villager) {
        RetoldAiControlMode mode = RetoldAiControl.getMode(villager);
        return mode == RetoldAiControlMode.NONE
                || RetoldAiControl.isControlledAsBy(
                villager,
                RetoldAiControlMode.SEARCH,
                RetoldAiControlOwner.VILLAGER_ANIMAL_TENDING
        );
    }

    private static boolean claimControl(Villager villager, long gameTime) {
        return RetoldAiControl.tryClaim(
                villager,
                RetoldAiControlMode.SEARCH,
                RetoldAiControlOwner.VILLAGER_ANIMAL_TENDING,
                CONTROL_PRIORITY,
                CONTROL_REASON,
                gameTime,
                CONTROL_TICKS
        );
    }

    private static void cancel(Villager villager) {
        if (villager == null) {
            return;
        }

        TASKS.remove(villager);
        RetoldVillagerCommunalFoodSearch.forget(villager);
        clearOwnedMovement(villager);
    }

    private static void clearOwnedMovement(Villager villager) {
        RetoldAiControl.clearIfOwnedBy(
                villager,
                RetoldAiControlOwner.VILLAGER_ANIMAL_TENDING
        );
        RetoldBehaviorMovement.stopOwnedMovement(
                villager,
                RetoldAiControlOwner.VILLAGER_ANIMAL_TENDING
        );
    }

    private static void scheduleNextSearch(
            Villager villager,
            long gameTime,
            int ticks
    ) {
        NEXT_SEARCH_AT.put(
                villager,
                gameTime + RetoldAiLod.cacheTicks(villager, ticks)
        );
    }

    record AnimalPair(Animal first, Animal second, ItemStack food) {
    }

    private record TendTask(
            UUID first,
            UUID second,
            ItemStack food,
            int reservedFood,
            long expiresAt
    ) {
        private TendTask withReservedFood(int newReservedFood) {
            return new TendTask(
                    first,
                    second,
                    food,
                    Math.min(FOOD_REQUIRED, Math.max(0, newReservedFood)),
                    expiresAt
            );
        }
    }
}
