package cz.xefensor.retold.villager;

import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.control.RetoldAiControlMode;
import cz.xefensor.retold.behavior.control.RetoldAiControlOwner;
import cz.xefensor.retold.behavior.control.RetoldAiPriorities;
import cz.xefensor.retold.behavior.core.RetoldBehaviorCoordinator;
import cz.xefensor.retold.behavior.core.RetoldBehaviorMovement;
import cz.xefensor.retold.behavior.food.RetoldFeedingPose;
import cz.xefensor.retold.behavior.food.RetoldStarvationBehavior;
import cz.xefensor.retold.behavior.profiles.RetoldMobProfileType;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;
import cz.xefensor.retold.behavior.profiles.RetoldMobState;
import cz.xefensor.retold.behavior.profiles.RetoldMobStates;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public final class RetoldVillagerCommunalFood {
    static final int PERSONAL_FOOD_STOCK_POINTS = 12;

    private static final int CACHE_TICKS = 100;
    private static final int PATH_INTERVAL_TICKS = 8;
    private static final int CONTROL_TICKS = 20 * 5;
    private static final int CONTROL_PRIORITY = RetoldAiPriorities.above(
            RetoldAiPriorities.FEED,
            1
    );
    private static final double MOVEMENT_SPEED = 0.55D;
    private static final double ACCESS_DISTANCE_SQUARED = 1.25D * 1.25D;
    private static final String CONTROL_REASON = "use_village_food_storage";

    private RetoldVillagerCommunalFood() {
    }

    public static void tick(
            ServerLevel level,
            Villager villager,
            long gameTime
    ) {
        if (!isUsable(level, villager)) {
            clearOwnedMovement(villager);
            return;
        }

        RetoldMobState state = RetoldMobStates.getOrCreate(villager, gameTime);
        if (!tickHunger(level, villager, state, gameTime)) {
            return;
        }

        if (RetoldFeedingPose.tick(villager, gameTime)) {
            return;
        }

        if (!RetoldMobRules.hasEatDrive(villager, state)
                || hasUrgentVanillaActivity(villager)
                || !RetoldBehaviorCoordinator.canFeedNow(villager)) {
            clearOwnedMovement(villager);
            return;
        }

        if (consumePersonalFood(villager, state, gameTime)) {
            return;
        }

        BlockPos storagePos = RetoldVillagerCommunalFoodSearch.find(
                level,
                villager,
                gameTime,
                CACHE_TICKS
        );

        if (storagePos == null) {
            clearOwnedMovement(villager);
            return;
        }

        BlockPos accessPos = findAccessPos(level, villager, storagePos);

        if (accessPos == null) {
            RetoldVillagerCommunalFoodSearch.forget(villager);
            clearOwnedMovement(villager);
            return;
        }

        if (villager.distanceToSqr(Vec3.atBottomCenterOf(accessPos))
                <= ACCESS_DISTANCE_SQUARED) {
            tryConsume(level, villager, state, storagePos, gameTime);
            return;
        }

        if (!claimFoodControl(villager, gameTime)) {
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

    public static boolean tryConsume(
            ServerLevel level,
            Villager villager,
            RetoldMobState state,
            BlockPos storagePos,
            long gameTime
    ) {
        if (!isUsable(level, villager)
                || state == null
                || storagePos == null
                || !RetoldMobRules.hasEatDrive(villager, state)
                || hasUrgentVanillaActivity(villager)
                || !RetoldBehaviorCoordinator.canFeedNow(villager)) {
            return false;
        }

        BlockPos accessPos = findAccessPos(level, villager, storagePos);

        if (accessPos == null
                || villager.distanceToSqr(Vec3.atBottomCenterOf(accessPos))
                > ACCESS_DISTANCE_SQUARED) {
            return false;
        }

        int stocked = RetoldVillagerCommunalFoodSearch.stockPersonalFood(
                level,
                villager,
                storagePos,
                PERSONAL_FOOD_STOCK_POINTS
        );

        if (stocked <= 0) {
            return false;
        }

        return consumePersonalFood(
                villager,
                state,
                gameTime,
                Vec3.atCenterOf(storagePos)
        );
    }

    private static boolean consumePersonalFood(
            Villager villager,
            RetoldMobState state,
            long gameTime
    ) {
        return consumePersonalFood(
                villager,
                state,
                gameTime,
                carriedFoodSource(villager)
        );
    }

    private static boolean consumePersonalFood(
            Villager villager,
            RetoldMobState state,
            long gameTime,
            Vec3 foodSource
    ) {
        var inventory = villager.getInventory();
        int bestSlot = -1;
        int bestPoints = 0;

        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            int points = stack.isEmpty()
                    ? 0
                    : Villager.FOOD_POINTS.getOrDefault(stack.getItem(), 0);

            if (points > bestPoints) {
                bestPoints = points;
                bestSlot = slot;
            }
        }

        if (bestSlot < 0) {
            return false;
        }

        ItemStack consumed = inventory.removeItem(bestSlot, 1);

        if (consumed.isEmpty()) {
            return false;
        }

        inventory.setChanged();
        int foodPoints = Villager.FOOD_POINTS.getOrDefault(consumed.getItem(), 0);
        state.addHunger(-Math.max(12, foodPoints * 6));
        state.markFed(gameTime);
        villager.playSound(SoundEvents.GENERIC_EAT.value(), 1.0F, 1.0F);
        villager.getNavigation().stop();
        clearOwnedMovement(villager);
        RetoldVillagerCommunalFoodSearch.forget(villager);
        RetoldFeedingPose.begin(
                villager,
                foodSource,
                gameTime
        );
        return true;
    }

    private static Vec3 carriedFoodSource(Villager villager) {
        return villager.getEyePosition().add(
                villager.getLookAngle().scale(0.75D)
        );
    }

    static BlockPos findAccessPos(
            ServerLevel level,
            Villager villager,
            BlockPos storagePos
    ) {
        BlockPos best = null;
        double bestDistanceSquared = Double.MAX_VALUE;

        for (var direction : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            BlockPos candidate = storagePos.relative(direction);

            if (!villager.getNavigation().isStableDestination(candidate)
                    || !level.getBlockState(candidate)
                    .getCollisionShape(level, candidate)
                    .isEmpty()
                    || !level.getBlockState(candidate.above())
                    .getCollisionShape(level, candidate.above())
                    .isEmpty()) {
                continue;
            }

            double distanceSquared = villager.distanceToSqr(
                    Vec3.atBottomCenterOf(candidate)
            );

            if (distanceSquared < bestDistanceSquared) {
                bestDistanceSquared = distanceSquared;
                best = candidate;
            }
        }

        return best;
    }

    private static boolean isUsable(ServerLevel level, Villager villager) {
        return level != null
                && villager != null
                && villager.level() == level
                && villager.isAlive()
                && !villager.isRemoved()
                && !villager.isNoAi()
                && !villager.isSleeping()
                && villager.getTradingPlayer() == null
                && RetoldMobRules.profileType(villager)
                == RetoldMobProfileType.VILLAGER_COMMUNAL;
    }

    static boolean hasUrgentVanillaActivity(Villager villager) {
        var brain = villager.getBrain();

        return villager.getLastHurtByMob() != null
                || brain.hasMemoryValue(MemoryModuleType.HURT_BY)
                || brain.hasMemoryValue(MemoryModuleType.HURT_BY_ENTITY)
                || brain.hasMemoryValue(MemoryModuleType.AVOID_TARGET)
                || brain.hasMemoryValue(MemoryModuleType.NEAREST_HOSTILE);
    }

    private static boolean tickHunger(
            ServerLevel level,
            Villager villager,
            RetoldMobState state,
            long gameTime
    ) {
        int interval = RetoldMobRules.hungerInterval(villager);

        if (interval <= 0
                || gameTime - state.lastHungerTickAt() < interval) {
            return true;
        }

        state.addHunger(1);
        state.markHungerTick(gameTime);
        return RetoldStarvationBehavior.applyCriticalHunger(
                level,
                villager,
                state,
                gameTime
        );
    }

    private static boolean claimFoodControl(Villager villager, long gameTime) {
        return RetoldAiControl.tryClaim(
                villager,
                RetoldAiControlMode.FEED,
                RetoldAiControlOwner.FOOD,
                CONTROL_PRIORITY,
                CONTROL_REASON,
                gameTime,
                CONTROL_TICKS
        );
    }

    private static void clearOwnedMovement(Villager villager) {
        if (villager == null || !RetoldAiControl.isControlledBy(
                villager,
                RetoldAiControlOwner.FOOD
        )) {
            return;
        }

        RetoldAiControl.clearIfControlledAsByAny(
                villager,
                RetoldAiControlOwner.FOOD,
                RetoldAiControlMode.FEED,
                RetoldAiControlMode.SEARCH
        );
        RetoldBehaviorMovement.stopOwnedMovement(
                villager,
                RetoldAiControlOwner.FOOD
        );
    }
}
