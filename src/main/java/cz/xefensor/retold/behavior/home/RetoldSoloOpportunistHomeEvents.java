package cz.xefensor.retold.behavior.home;

import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.control.RetoldAiControlMode;
import cz.xefensor.retold.behavior.control.RetoldAiControlOwner;
import cz.xefensor.retold.behavior.control.RetoldAiPriorities;
import cz.xefensor.retold.behavior.core.RetoldBehaviorCoordinator;
import cz.xefensor.retold.behavior.core.RetoldBehaviorMovement;
import cz.xefensor.retold.behavior.core.RetoldBehaviorTiming;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;
import cz.xefensor.retold.behavior.profiles.RetoldMobState;
import cz.xefensor.retold.behavior.profiles.RetoldMobStates;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.List;

public final class RetoldSoloOpportunistHomeEvents {
    private static final RetoldAiControlOwner CONTROL_OWNER = RetoldAiControlOwner.REGROUP;
    private static final String REASON_RETURN_HOME = "return_solo_home";
    private static final String REASON_RETURN_CACHE = "return_solo_cache";
    private static final String REASON_RETURN_AFTER_HUNT = "return_after_hunt";
    private static final String REASON_SOLO_IDLE = "solo_home_idle";

    private static final int THINK_INTERVAL_TICKS = 40;
    private static final int SOLO_HOME_PATH_INTERVAL_TICKS = 12;
    private static final int HOME_RETURN_CONTROL_TICKS = 20 * 5;
    private static final int HOME_RETURN_PRIORITY = RetoldAiPriorities.above(RetoldAiPriorities.REST, 3);
    private static final int CACHE_RETURN_PRIORITY = RetoldAiPriorities.REGROUP;
    private static final int AFTER_HUNT_RETURN_PRIORITY = RetoldAiPriorities.above(RetoldAiPriorities.REST, 4);
    private static final int HOME_IDLE_CONTROL_TICKS = 20 * 5;
    private static final int HOME_IDLE_PRIORITY = RetoldAiPriorities.HOME_IDLE;
    private static final int HOME_IDLE_MOVE_INTERVAL_TICKS = 20 * 22;
    private static final int PANIC_RECOVERY_TICKS = 20 * 18;
    private static final int AFTER_HUNT_RETURN_TICKS = 20 * 45;

    private static final double HOME_RETURN_START_BLOCKS = 24.0D;
    private static final double HOME_RETURN_START_SQUARED =
            HOME_RETURN_START_BLOCKS * HOME_RETURN_START_BLOCKS;

    private static final double PANIC_RECOVERY_RETURN_START_BLOCKS = 9.0D;
    private static final double PANIC_RECOVERY_RETURN_START_SQUARED =
            PANIC_RECOVERY_RETURN_START_BLOCKS * PANIC_RECOVERY_RETURN_START_BLOCKS;

    private static final double HOME_RETURN_STOP_BLOCKS = 6.0D;
    private static final double HOME_RETURN_STOP_SQUARED =
            HOME_RETURN_STOP_BLOCKS * HOME_RETURN_STOP_BLOCKS;

    private static final double HOME_IDLE_RADIUS_BLOCKS = 10.0D;
    private static final double HOME_IDLE_RADIUS_SQUARED =
            HOME_IDLE_RADIUS_BLOCKS * HOME_IDLE_RADIUS_BLOCKS;

    private static final double HOME_IDLE_CLOSE_BLOCKS = 4.5D;
    private static final double HOME_IDLE_CLOSE_SQUARED =
            HOME_IDLE_CLOSE_BLOCKS * HOME_IDLE_CLOSE_BLOCKS;

    private static final double HOME_IDLE_MIN_STROLL_BLOCKS = 1.5D;
    private static final double HOME_IDLE_EXTRA_STROLL_BLOCKS = 3.5D;

    private static final double HOME_RETURN_SPEED = 0.74D;
    private static final double HOME_IDLE_STROLL_SPEED = 0.44D;

    private RetoldSoloOpportunistHomeEvents() {
    }

    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof PathfinderMob animal)) {
            return;
        }

        if (!(animal.level() instanceof ServerLevel level)) {
            return;
        }

        if (!isSoloOpportunist(animal)) {
            return;
        }

        long gameTime = level.getGameTime();

        if (!RetoldBehaviorTiming.shouldThink(animal, gameTime, THINK_INTERVAL_TICKS)) {
            return;
        }

        RetoldAnimalHomeMemory home = RetoldAnimalHomes.get(animal);

        if (!RetoldAnimalHomes.isValidFor(level, animal, home)) {
            home = tryCreateSoloHome(
                    level,
                    animal,
                    gameTime
            );

            if (!RetoldAnimalHomes.isValidFor(level, animal, home)) {
                return;
            }
        }

        updateHomeReturn(
                level,
                animal,
                home,
                gameTime
        );
    }

    private static RetoldAnimalHomeMemory tryCreateSoloHome(
            ServerLevel level,
            PathfinderMob animal,
            long gameTime
    ) {
        if (!canCreateHome(animal, gameTime)) {
            return null;
        }

        return RetoldAnimalHomes.getOrCreatePackHome(
                level,
                animal,
                List.of(),
                animal.blockPosition().immutable(),
                gameTime
        );
    }

    private static boolean canCreateHome(
            PathfinderMob animal,
            long gameTime
    ) {
        if (!RetoldBehaviorCoordinator.canStartLowPriorityHomeBehavior(animal)) {
            return false;
        }

        RetoldMobState state = RetoldMobStates.getOrCreate(
                animal,
                gameTime
        );

        return !RetoldMobRules.hasHuntDrive(animal, state);
    }

    private static void updateHomeReturn(
            ServerLevel level,
            PathfinderMob animal,
            RetoldAnimalHomeMemory home,
            long gameTime
    ) {
        RetoldAiControlMode mode = RetoldAiControl.getMode(animal);
        RetoldAiControlOwner owner = RetoldAiControl.getOwner(animal);

        if (!canUseHomeReturn(animal, mode)) {
            RetoldHomeRestAnimations.stopResting(animal);
            return;
        }

        double distanceSquared = animal.blockPosition().distSqr(home.pos());
        boolean recoveringFromPanic = isRecoveringFromPanic(
                animal,
                gameTime
        );
        boolean shouldCacheHeldFood = shouldReturnToCacheHeldFood(animal);
        boolean shouldReturnAfterHunt = shouldReturnAfterHunt(
                animal,
                gameTime
        );

        if (
                !shouldCacheHeldFood
                        && !shouldReturnAfterHunt
                        && RetoldAnimalDailyRhythm.isActive(level, animal)
                        && !shouldRestAtHome(level, animal)
                        && distanceSquared < getReturnStartDistanceSquared(recoveringFromPanic)
        ) {
            releaseSoloIdleIfOwned(animal);
            RetoldHomeRestAnimations.stopResting(animal);
            RetoldAnimalHomes.markUsed(animal, gameTime);
            return;
        }

        if (distanceSquared <= HOME_RETURN_STOP_SQUARED) {
            if (tryCacheHeldFoodAtHome(level, animal, gameTime)) {
                return;
            }

            if (
                    RetoldAnimalDailyRhythm.isActive(level, animal)
                            && !shouldCacheHeldFood
                            && !shouldReturnAfterHunt
            ) {
                releaseSoloIdleIfOwned(animal);
                RetoldHomeRestAnimations.stopResting(animal);
                RetoldAnimalHomes.markUsed(animal, gameTime);
                return;
            }

            if (
                    RetoldAnimalHomeIdle.shouldIdleAtHome(
                            animal,
                            home,
                            mode,
                            owner,
                            CONTROL_OWNER,
                            REASON_SOLO_IDLE,
                            distanceSquared,
                            HOME_IDLE_RADIUS_SQUARED,
                            gameTime
                    )
            ) {
                RetoldAnimalHomeIdle.idleAtHome(
                        level,
                        animal,
                        home,
                        gameTime,
                        REASON_SOLO_IDLE,
                        HOME_IDLE_PRIORITY,
                        HOME_IDLE_CONTROL_TICKS,
                        HOME_IDLE_CLOSE_SQUARED,
                        HOME_RETURN_SPEED,
                        HOME_IDLE_STROLL_SPEED,
                        HOME_IDLE_MIN_STROLL_BLOCKS,
                        HOME_IDLE_EXTRA_STROLL_BLOCKS,
                        HOME_IDLE_MOVE_INTERVAL_TICKS,
                        shouldRestAtHome(level, animal)
                );
                return;
            }

            if (RetoldAiControl.isControlledAsByWithAnyReason(
                    animal,
                    RetoldAiControlMode.REGROUP,
                    CONTROL_OWNER,
                    REASON_RETURN_HOME,
                    REASON_RETURN_CACHE,
                    REASON_RETURN_AFTER_HUNT
            )) {
                RetoldAiControl.clear(animal);
                animal.getNavigation().stop();
            }

            RetoldHomeRestAnimations.stopResting(animal);
            RetoldAnimalHomes.markUsed(animal, gameTime);
            return;
        }

        if (
                !shouldCacheHeldFood
                        && !shouldReturnAfterHunt
                        && mode == RetoldAiControlMode.NONE
                        && distanceSquared < getReturnStartDistanceSquared(recoveringFromPanic)
        ) {
            return;
        }

        if (!RetoldAiControl.tryClaim(
                animal,
                RetoldAiControlMode.REGROUP,
                CONTROL_OWNER,
                returnPriority(shouldCacheHeldFood, shouldReturnAfterHunt),
                returnReason(shouldCacheHeldFood, shouldReturnAfterHunt),
                gameTime,
                HOME_RETURN_CONTROL_TICKS
        )) {
            return;
        }

        RetoldHomeRestAnimations.stopResting(animal);
        animal.setSprinting(false);
        RetoldAnimalHomes.markUsed(animal, gameTime);

        RetoldBehaviorMovement.throttledMoveTo(
                animal,
                home.pos(),
                HOME_RETURN_SPEED,
                gameTime,
                SOLO_HOME_PATH_INTERVAL_TICKS,
                2.0D * 2.0D
        );
    }

    private static int returnPriority(
            boolean cacheHeldFood,
            boolean returnAfterHunt
    ) {
        if (cacheHeldFood) {
            return CACHE_RETURN_PRIORITY;
        }

        if (returnAfterHunt) {
            return AFTER_HUNT_RETURN_PRIORITY;
        }

        return HOME_RETURN_PRIORITY;
    }

    private static String returnReason(
            boolean cacheHeldFood,
            boolean returnAfterHunt
    ) {
        if (cacheHeldFood) {
            return REASON_RETURN_CACHE;
        }

        if (returnAfterHunt) {
            return REASON_RETURN_AFTER_HUNT;
        }

        return REASON_RETURN_HOME;
    }

    private static double getReturnStartDistanceSquared(boolean recoveringFromPanic) {
        return recoveringFromPanic
                ? PANIC_RECOVERY_RETURN_START_SQUARED
                : HOME_RETURN_START_SQUARED;
    }

    private static boolean isRecoveringFromPanic(
            PathfinderMob animal,
            long gameTime
    ) {
        RetoldMobState state = RetoldMobStates.get(animal);

        return state != null
                && gameTime - state.lastFleeEndedAt() <= PANIC_RECOVERY_TICKS;
    }

    private static boolean shouldReturnToCacheHeldFood(PathfinderMob animal) {
        if (!RetoldMobRules.isFox(animal)) {
            return false;
        }

        return hasEdibleHeldFood(animal);
    }

    private static boolean shouldReturnAfterHunt(
            PathfinderMob animal,
            long gameTime
    ) {
        if (!RetoldMobRules.isFoxCatOrOcelot(animal)) {
            return false;
        }

        RetoldMobState state = RetoldMobStates.get(animal);

        return state != null
                && gameTime - state.lastSuccessfulHuntAt() <= AFTER_HUNT_RETURN_TICKS;
    }

    private static boolean tryCacheHeldFoodAtHome(
            ServerLevel level,
            PathfinderMob animal,
            long gameTime
    ) {
        if (!RetoldMobRules.isFox(animal)) {
            return false;
        }

        RetoldMobState state = RetoldMobStates.getOrCreate(
                animal,
                gameTime
        );

        if (RetoldMobRules.hasEatDrive(
                animal,
                state
        )) {
            return false;
        }

        if (tryDropCachedFood(level, animal, EquipmentSlot.MAINHAND)) {
            RetoldAnimalHomes.markUsed(animal, gameTime);
            return true;
        }

        if (tryDropCachedFood(level, animal, EquipmentSlot.OFFHAND)) {
            RetoldAnimalHomes.markUsed(animal, gameTime);
            return true;
        }

        return false;
    }

    private static boolean hasEdibleHeldFood(PathfinderMob animal) {
        return isEdibleHeldFood(animal, EquipmentSlot.MAINHAND)
                || isEdibleHeldFood(animal, EquipmentSlot.OFFHAND);
    }

    private static boolean isEdibleHeldFood(
            PathfinderMob animal,
            EquipmentSlot slot
    ) {
        ItemStack stack = animal.getItemBySlot(slot);

        return !stack.isEmpty()
                && RetoldMobRules.canEatDroppedItem(animal, stack);
    }

    private static boolean tryDropCachedFood(
            ServerLevel level,
            PathfinderMob animal,
            EquipmentSlot slot
    ) {
        ItemStack stack = animal.getItemBySlot(slot);

        if (stack.isEmpty() || !RetoldMobRules.canEatDroppedItem(animal, stack)) {
            return false;
        }

        ItemStack cached = stack.split(1);

        if (stack.isEmpty()) {
            animal.setItemSlot(slot, ItemStack.EMPTY);
        } else {
            animal.setItemSlot(slot, stack);
        }

        ItemEntity item = new ItemEntity(
                level,
                animal.getX(),
                animal.getY() + 0.2D,
                animal.getZ(),
                cached
        );
        item.setPickUpDelay(20 * 10);
        level.addFreshEntity(item);

        animal.setSprinting(false);
        animal.getNavigation().stop();

        return true;
    }

    private static boolean shouldRestAtHome(
            ServerLevel level,
            PathfinderMob animal
    ) {
        return RetoldAnimalDailyRhythm.shouldRestAtHome(level, animal);
    }

    private static boolean canUseHomeReturn(
            PathfinderMob animal,
            RetoldAiControlMode mode
    ) {
        return RetoldBehaviorCoordinator.canUseOwnedModeWithoutLiveTarget(
                animal,
                mode,
                RetoldAiControlMode.REGROUP,
                CONTROL_OWNER
        );
    }

    private static void releaseSoloIdleIfOwned(PathfinderMob animal) {
        if (RetoldAiControl.clearIfControlledAsByWithReason(
                animal,
                RetoldAiControlMode.REGROUP,
                CONTROL_OWNER,
                REASON_SOLO_IDLE
        )) {
            animal.getNavigation().stop();
        }
    }

    private static boolean isSoloOpportunist(PathfinderMob mob) {
        return mob != null
                && mob.isAlive()
                && !mob.isRemoved()
                && RetoldMobRules.canUseOrdinaryLifeSystems(mob)
                && RetoldMobRules.isSoloOpportunist(mob);
    }
}
