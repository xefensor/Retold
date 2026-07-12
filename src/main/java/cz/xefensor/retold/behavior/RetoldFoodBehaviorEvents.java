package cz.xefensor.retold.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;

public final class RetoldFoodBehaviorEvents {
    private static final RetoldAiControlOwner CONTROL_OWNER = RetoldAiControlOwner.FOOD;

    private static final int THINK_INTERVAL_TICKS = 20;
    private static final int FOOD_SCAN_CACHE_TICKS = 8;
    private static final int FOOD_BLOCK_SEARCH_CACHE_TICKS = 30;
    private static final int FOOD_PATH_INTERVAL_TICKS = 8;
    private static final int CLEANUP_INTERVAL_TICKS = 20 * 10;
    private static final int FEED_CONTROL_TICKS = 20 * 4;
    private static final int FEED_PRIORITY = RetoldAiPriorities.above(RetoldAiPriorities.FEED, 1);

    private static final double DROPPED_FOOD_RADIUS = 8.0D;
    private static final double DROPPED_FOOD_RADIUS_SQUARED =
            DROPPED_FOOD_RADIUS * DROPPED_FOOD_RADIUS;

    private static final double EAT_ITEM_DISTANCE = 2.35D;
    private static final double EAT_ITEM_DISTANCE_SQUARED =
            EAT_ITEM_DISTANCE * EAT_ITEM_DISTANCE;

    private static final int FORAGE_HORIZONTAL_RADIUS = 6;
    private static final int FORAGE_VERTICAL_RADIUS = 2;

    private static final double FORAGE_EAT_DISTANCE = 2.15D;
    private static final double FORAGE_EAT_DISTANCE_SQUARED =
            FORAGE_EAT_DISTANCE * FORAGE_EAT_DISTANCE;

    private static final double PASSIVE_FOOD_SPEED = 0.65D;
    private static final double PREDATOR_FOOD_SPEED = 0.82D;

    private static long nextCleanupAt;

    private RetoldFoodBehaviorEvents() {
    }

    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof PathfinderMob mob)) {
            return;
        }

        if (!(mob.level() instanceof ServerLevel level)) {
            return;
        }

        if (!RetoldMobRules.canUseOrdinaryLifeSystems(mob)) {
            return;
        }

        long gameTime = level.getGameTime();

        if (!shouldThink(mob, gameTime)) {
            return;
        }

        RetoldMobState state = RetoldMobStates.getOrCreate(
                mob,
                gameTime
        );

        tickHunger(
                mob,
                state,
                gameTime
        );

        if (!shouldSeekFood(mob, state)) {
            return;
        }

        ItemEntity droppedFood = findBestDroppedFood(
                level,
                mob
        );

        if (droppedFood != null) {
            handleDroppedFood(
                    mob,
                    state,
                    droppedFood,
                    gameTime
            );
            return;
        }

        BlockPos foragePos = findBestForageBlock(
                level,
                mob
        );

        if (foragePos != null) {
            handleForageBlock(
                    level,
                    mob,
                    state,
                    foragePos,
                    gameTime
            );
        }
    }

    @SubscribeEvent
    public static void onServerTickPost(ServerTickEvent.Post event) {
        long gameTime = event.getServer().overworld().getGameTime();

        if (gameTime < nextCleanupAt) {
            return;
        }

        nextCleanupAt = gameTime + CLEANUP_INTERVAL_TICKS;

        RetoldMobStates.cleanup(gameTime);
        RetoldAiControl.cleanup(gameTime);
    }

    private static boolean shouldThink(
            PathfinderMob mob,
            long gameTime
    ) {
        return RetoldBehaviorTiming.shouldThink(
                mob,
                gameTime,
                THINK_INTERVAL_TICKS
        );
    }

    private static void tickHunger(
            PathfinderMob mob,
            RetoldMobState state,
            long gameTime
    ) {
        int interval = RetoldMobRules.hungerInterval(mob);

        if (interval <= 0) {
            return;
        }

        if (gameTime - state.lastHungerTickAt() < interval) {
            return;
        }

        state.addHunger(1);
        state.markHungerTick(gameTime);
    }

    private static boolean shouldSeekFood(
            PathfinderMob mob,
            RetoldMobState state
    ) {
        if (state == null) {
            return false;
        }

        if (!RetoldBehaviorCoordinator.canFeedNow(mob)) {
            return false;
        }

        RetoldAiControlMode mode = RetoldAiControl.getMode(mob);

        if (mode != RetoldAiControlMode.NONE && mode != RetoldAiControlMode.FEED) {
            return false;
        }

        return RetoldMobRules.hasEatDrive(
                mob,
                state
        );
    }

    private static ItemEntity findBestDroppedFood(
            ServerLevel level,
            PathfinderMob mob
    ) {
        List<ItemEntity> items = RetoldAiScanCache.nearby(
                level,
                mob,
                ItemEntity.class,
                DROPPED_FOOD_RADIUS,
                level.getGameTime(),
                FOOD_SCAN_CACHE_TICKS
        );

        ItemEntity best = null;
        double bestScore = Double.MAX_VALUE;

        for (ItemEntity item : items) {
            if (!isValidDroppedFood(mob, item)) {
                continue;
            }

            double distanceSquared = mob.distanceToSqr(item);

            if (distanceSquared > DROPPED_FOOD_RADIUS_SQUARED) {
                continue;
            }

            double score = distanceSquared;

            if (RetoldAiSightCache.canSee(mob, item, level.getGameTime())) {
                score -= 8.0D;
            }

            if (score < bestScore) {
                bestScore = score;
                best = item;
            }
        }

        return best;
    }

    private static boolean isValidDroppedFood(
            PathfinderMob mob,
            ItemEntity item
    ) {
        if (mob == null || item == null) {
            return false;
        }

        if (!item.isAlive() || item.isRemoved()) {
            return false;
        }

        ItemStack stack = item.getItem();

        if (stack.isEmpty()) {
            return false;
        }

        if (mob.distanceToSqr(item) > DROPPED_FOOD_RADIUS_SQUARED) {
            return false;
        }

        if (
                !RetoldAiSightCache.canSee(mob, item, mob.level().getGameTime())
                        && mob.distanceToSqr(item) > 16.0D
        ) {
            return false;
        }

        return RetoldMobRules.canEatDroppedItem(
                mob,
                stack
        );
    }

    private static void handleDroppedFood(
            PathfinderMob mob,
            RetoldMobState state,
            ItemEntity food,
            long gameTime
    ) {
        if (mob.distanceToSqr(food) <= EAT_ITEM_DISTANCE_SQUARED) {
            consumeDroppedFood(
                    mob,
                    state,
                    food,
                    gameTime
            );
            return;
        }

        if (!claimFoodControl(mob, gameTime)) {
            return;
        }

        RetoldBehaviorMovement.throttledMoveTo(
                mob,
                food,
                getFoodSpeed(mob),
                gameTime,
                FOOD_PATH_INTERVAL_TICKS,
                1.5D * 1.5D
        );
    }

    private static void consumeDroppedFood(
            PathfinderMob mob,
            RetoldMobState state,
            ItemEntity food,
            long gameTime
    ) {
        ItemStack stack = food.getItem();

        if (stack.isEmpty()) {
            return;
        }

        if (!RetoldMobRules.canEatDroppedItem(mob, stack)) {
            return;
        }

        String itemPath = RetoldMobRules.getItemPath(stack);

        state.addHunger(
                -RetoldMobRules.foodRelief(
                        mob,
                        itemPath
                )
        );

        state.markFed(gameTime);

        RetoldFeedingAnimations.play(mob);

        stack.shrink(1);

        if (stack.isEmpty()) {
            food.discard();
        } else {
            food.setItem(stack);
        }

        mob.getNavigation().stop();
        RetoldAiControl.clear(mob);
    }

    private static BlockPos findBestForageBlock(
            ServerLevel level,
            PathfinderMob mob
    ) {
        return RetoldForageBlockSearch.findOrdinaryForageBlock(
                level,
                mob,
                FORAGE_HORIZONTAL_RADIUS,
                FORAGE_VERTICAL_RADIUS,
                16.0D,
                level.getGameTime(),
                FOOD_BLOCK_SEARCH_CACHE_TICKS
        );
    }

    private static void handleForageBlock(
            ServerLevel level,
            PathfinderMob mob,
            RetoldMobState state,
            BlockPos foragePos,
            long gameTime
    ) {
        if (mob.blockPosition().distSqr(foragePos) <= FORAGE_EAT_DISTANCE_SQUARED) {
            consumeForageBlock(
                    level,
                    mob,
                    state,
                    foragePos,
                    gameTime
            );
            return;
        }

        if (!claimFoodControl(mob, gameTime)) {
            return;
        }

        RetoldBehaviorMovement.throttledMoveTo(
                mob,
                foragePos,
                getFoodSpeed(mob),
                gameTime,
                FOOD_PATH_INTERVAL_TICKS,
                1.5D * 1.5D
        );
    }

    private static void consumeForageBlock(
            ServerLevel level,
            PathfinderMob mob,
            RetoldMobState state,
            BlockPos foragePos,
            long gameTime
    ) {
        BlockState blockState = level.getBlockState(foragePos);

        if (!RetoldMobRules.canForageBlock(mob, blockState)) {
            return;
        }

        String blockPath = RetoldMobRules.getBlockPath(blockState);

        state.addHunger(
                -RetoldMobRules.forageRelief(
                        mob,
                        blockPath
                )
        );

        state.markFed(gameTime);

        RetoldFeedingAnimations.play(mob);

        destroyForageBlock(
                level,
                foragePos,
                blockPath
        );

        mob.getNavigation().stop();
        RetoldAiControl.clear(mob);
    }

    private static void destroyForageBlock(
            ServerLevel level,
            BlockPos pos,
            String blockPath
    ) {
        if (blockPath.equals("grass_block")) {
            level.setBlock(
                    pos,
                    Blocks.DIRT.defaultBlockState(),
                    3
            );
            return;
        }

        level.destroyBlock(
                pos,
                false
        );
    }

    private static double getFoodSpeed(PathfinderMob mob) {
        if (RetoldMobRules.canUseOrdinaryPredatorSystems(mob)) {
            return PREDATOR_FOOD_SPEED;
        }

        return PASSIVE_FOOD_SPEED;
    }

    private static boolean claimFoodControl(
            PathfinderMob mob,
            long gameTime
    ) {
        return RetoldAiControl.tryClaim(
                mob,
                RetoldAiControlMode.FEED,
                CONTROL_OWNER,
                FEED_PRIORITY,
                "seek_food",
                gameTime,
                FEED_CONTROL_TICKS
        );
    }
}
