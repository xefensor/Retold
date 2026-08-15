package cz.xefensor.retold.behavior.food;

import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.control.RetoldAiControlMode;
import cz.xefensor.retold.behavior.control.RetoldAiControlOwner;
import cz.xefensor.retold.behavior.control.RetoldAiPriorities;
import cz.xefensor.retold.behavior.ecology.RetoldUnloadedEcosystemCatchUp;
import cz.xefensor.retold.behavior.performance.RetoldAiScanCache;
import cz.xefensor.retold.behavior.performance.RetoldAiSightCache;
import cz.xefensor.retold.behavior.core.RetoldBehaviorCoordinator;
import cz.xefensor.retold.behavior.core.RetoldBehaviorMovement;
import cz.xefensor.retold.behavior.core.RetoldBehaviorTiming;
import cz.xefensor.retold.behavior.core.RetoldMobGriefing;
import cz.xefensor.retold.behavior.core.RetoldBehaviorTargets;
import cz.xefensor.retold.behavior.hunting.RetoldPredatorStrike;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;
import cz.xefensor.retold.behavior.profiles.RetoldMobState;
import cz.xefensor.retold.behavior.profiles.RetoldMobStates;
import cz.xefensor.retold.behavior.species.RetoldSlimeItemStorage;
import cz.xefensor.retold.behavior.species.RetoldSlimeStarvationBehavior;
import cz.xefensor.retold.behavior.species.RetoldStriderLavaSustenance;
import cz.xefensor.retold.behavior.species.RetoldTraderLlamaSustenance;
import cz.xefensor.retold.combat.RetoldFactionTargetMemory;
import cz.xefensor.retold.combat.RetoldTargetSource;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class RetoldFoodBehaviorEvents {
    private static final RetoldAiControlOwner CONTROL_OWNER = RetoldAiControlOwner.FOOD;

    private static final int THINK_INTERVAL_TICKS = 20;
    private static final int FOOD_SCAN_CACHE_TICKS = 8;
    private static final int FOOD_BLOCK_SEARCH_CACHE_TICKS = 30;
    private static final int FOOD_PATH_INTERVAL_TICKS = 8;
    private static final int FOOD_SEARCH_PATH_INTERVAL_TICKS = 10;
    private static final int CLEANUP_INTERVAL_TICKS = 20 * 10;
    private static final int FEED_CONTROL_TICKS = 20 * 4;
    private static final int SEARCH_CONTROL_TICKS = 20 * 5;
    private static final int SEARCH_POINT_LIFE_TICKS = 20 * 5;
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
    private static final double FOOD_SEARCH_SPEED = 0.72D;
    private static final double SEARCH_POINT_REACHED_DISTANCE_SQUARED = 2.5D * 2.5D;

    private static final int FOOD_SEARCH_HORIZONTAL_RADIUS = 12;
    private static final int FOOD_SEARCH_VERTICAL_RADIUS = 2;
    private static final int FOOD_SEARCH_DESTINATION_ATTEMPTS = 2;

    private static final Map<PathfinderMob, FoodSearchMemory> FOOD_SEARCH_MEMORIES =
            new WeakHashMap<>();

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

        if (!tickHunger(
                mob,
                state,
                gameTime
        )) {
            return;
        }

        if (RetoldStriderLavaSustenance.isSustainedByLava(level, mob)) {
            return;
        }

        if (RetoldTraderLlamaSustenance.tick(level, mob, state, gameTime)) {
            return;
        }

        if (RetoldMobRules.wantsDroppedFood(mob, state)
                && canConsiderDroppedFood(mob)) {
            ItemEntity droppedFood = findBestDroppedFood(
                    level,
                    mob
            );

            if (droppedFood != null
                    && prepareForDroppedFood(mob, droppedFood, gameTime)) {
                handleDroppedFood(
                        mob,
                        state,
                        droppedFood,
                        gameTime
                );
                return;
            }
        }

        if (!shouldSeekFood(mob, state)) {
            stopOwnedFoodSearch(mob);
            return;
        }

        if (RetoldAnimalFeederBehavior.tryUse(
                level,
                mob,
                state,
                gameTime
        )) {
            return;
        }

        BlockPos foragePos = findBestForageBlock(level, mob);

        if (foragePos != null
                && !RetoldMobGriefing.canModifyBlocks(level, mob)
                && !RetoldMobRules.isRenewableEnvironmentalForage(
                mob,
                level.getBlockState(foragePos)
        )) {
            foragePos = null;
        }

        if (foragePos != null) {
            handleForageBlock(
                    level,
                    mob,
                    state,
                    foragePos,
                    gameTime
            );
            return;
        }

        if (RetoldForageBlockSearch.isSearchDeferred(mob)) {
            stopOwnedFoodSearch(mob);
            return;
        }

        tryStartOrContinueFoodSearch(level, mob, state, gameTime);
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
        RetoldWeakBarrierBehavior.cleanup(gameTime);
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

    private static boolean tickHunger(
            PathfinderMob mob,
            RetoldMobState state,
            long gameTime
    ) {
        int interval = RetoldMobRules.hungerInterval(mob);

        if (interval <= 0) {
            return true;
        }

        if (gameTime - state.lastHungerTickAt() < interval) {
            return true;
        }

        if (RetoldUnloadedEcosystemCatchUp.deferLongGap(
                (ServerLevel) mob.level(),
                mob,
                state,
                gameTime,
                interval
        )) {
            return false;
        }

        state.addHunger(RetoldSlimeStarvationBehavior.hungerGain(mob));
        state.markHungerTick(gameTime);

        return RetoldStarvationBehavior.applyCriticalHunger(
                (ServerLevel) mob.level(),
                mob,
                state,
                gameTime
        );
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

        if (mode != RetoldAiControlMode.NONE
                && mode != RetoldAiControlMode.FEED
                && !RetoldAiControl.isControlledAsBy(
                mob,
                RetoldAiControlMode.SEARCH,
                CONTROL_OWNER
        )) {
            return false;
        }

        return RetoldMobRules.hasEatDrive(
                mob,
                state
        );
    }

    static boolean tryStartOrContinueFoodSearch(
            ServerLevel level,
            PathfinderMob mob,
            RetoldMobState state,
            long gameTime
    ) {
        if (level == null
                || mob == null
                || state == null
                || mob.level() != level
                || !RetoldMobRules.hasActiveSearchDrive(state)
                || RetoldMobRules.canUseNaturalPreyHuntingSystems(mob)
                || RetoldMobRules.isAquaticSchool(mob)
                || RetoldMobRules.isSnifferForager(mob)
                || RetoldMobRules.isHiveColony(mob)) {
            stopOwnedFoodSearch(mob);
            return false;
        }

        RetoldAiControlMode mode = RetoldAiControl.getMode(mob);

        if (mode != RetoldAiControlMode.NONE
                && !RetoldAiControl.isControlledAsBy(
                mob,
                RetoldAiControlMode.SEARCH,
                CONTROL_OWNER
        )) {
            FOOD_SEARCH_MEMORIES.remove(mob);
            return false;
        }

        FoodSearchMemory memory = FOOD_SEARCH_MEMORIES.get(mob);

        if (memory == null
                || gameTime > memory.expiresAt()
                || mob.getNavigation().isDone()
                || mob.distanceToSqr(Vec3.atCenterOf(memory.pos()))
                <= SEARCH_POINT_REACHED_DISTANCE_SQUARED) {
            memory = null;
        }

        if (!RetoldAiControl.tryClaim(
                mob,
                RetoldAiControlMode.SEARCH,
                CONTROL_OWNER,
                RetoldAiPriorities.SEARCH,
                "search_for_food",
                gameTime,
                SEARCH_CONTROL_TICKS
        )) {
            FOOD_SEARCH_MEMORIES.remove(mob);
            return false;
        }

        mob.setSprinting(false);

        if (memory != null && RetoldBehaviorMovement.throttledMoveTo(
                mob,
                memory.pos(),
                FOOD_SEARCH_SPEED,
                gameTime,
                FOOD_SEARCH_PATH_INTERVAL_TICKS,
                2.0D * 2.0D
        )) {
            return true;
        }

        for (int attempt = 0; attempt < FOOD_SEARCH_DESTINATION_ATTEMPTS; attempt++) {
            Vec3 searchPosition = LandRandomPos.getPos(
                    mob,
                    FOOD_SEARCH_HORIZONTAL_RADIUS,
                    FOOD_SEARCH_VERTICAL_RADIUS
            );

            if (searchPosition == null) {
                continue;
            }

            FoodSearchMemory candidate = new FoodSearchMemory(
                    BlockPos.containing(
                            searchPosition.x(),
                            mob.getY(),
                            searchPosition.z()
                    ),
                    gameTime + SEARCH_POINT_LIFE_TICKS
            );

            if (!RetoldBehaviorMovement.throttledMoveTo(
                    mob,
                    candidate.pos(),
                    FOOD_SEARCH_SPEED,
                    gameTime,
                    FOOD_SEARCH_PATH_INTERVAL_TICKS,
                    2.0D * 2.0D
            )) {
                continue;
            }

            FOOD_SEARCH_MEMORIES.put(mob, candidate);
            return true;
        }

        stopOwnedFoodSearch(mob);
        return false;
    }

    static BlockPos foodSearchTarget(PathfinderMob mob) {
        FoodSearchMemory memory = FOOD_SEARCH_MEMORIES.get(mob);
        return memory == null ? null : memory.pos();
    }

    private static void stopOwnedFoodSearch(PathfinderMob mob) {
        if (mob == null) {
            return;
        }

        FOOD_SEARCH_MEMORIES.remove(mob);

        if (RetoldAiControl.isControlledAsBy(
                mob,
                RetoldAiControlMode.SEARCH,
                CONTROL_OWNER
        )) {
            RetoldAiControl.clearIfControlledAsByAny(
                    mob,
                    CONTROL_OWNER,
                    RetoldAiControlMode.SEARCH
            );
            RetoldBehaviorMovement.stopOwnedMovement(
                    mob,
                    CONTROL_OWNER
            );
        }
    }

    private static boolean canConsiderDroppedFood(PathfinderMob mob) {
        return RetoldBehaviorCoordinator.canFeedNow(mob)
                || RetoldAiControl.getMode(mob) == RetoldAiControlMode.HUNT;
    }

    private static boolean prepareForDroppedFood(
            PathfinderMob mob,
            ItemEntity food,
            long gameTime
    ) {
        if (RetoldBehaviorCoordinator.canFeedNow(mob)) {
            return true;
        }

        return tryPreferDroppedFoodOverHunt(
                mob,
                food,
                gameTime
        );
    }

    public static boolean tryPreferDroppedFoodOverHunt(
            PathfinderMob mob,
            ItemEntity food,
            long gameTime
    ) {
        if (mob == null || food == null) {
            return false;
        }

        RetoldMobState state = RetoldMobStates.getOrCreate(mob, gameTime);

        if (!RetoldMobRules.wantsDroppedFood(mob, state)
                || RetoldAiControl.getMode(mob) != RetoldAiControlMode.HUNT
                || !isValidDroppedFood(mob, food)) {
            return false;
        }

        var target = mob.getTarget();

        if (target != null && RetoldFactionTargetMemory.isOwnedByAny(
                mob,
                target,
                RetoldTargetSource.OWNER_DEFENSE,
                RetoldTargetSource.RETALIATION,
                RetoldTargetSource.TERRITORY_ATTACK
        )) {
            return false;
        }

        if (!claimFoodControl(mob, gameTime)) {
            return false;
        }

        if (target != null) {
            RetoldBehaviorTargets.clearTargetAndAggression(
                    mob,
                    target,
                    true
            );
        }

        RetoldPredatorStrike.clear(mob);
        mob.setSprinting(false);
        return true;
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
            tryConsumeDroppedFood(
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

    public static boolean tryConsumeDroppedFood(
            PathfinderMob mob,
            ItemEntity food,
            long gameTime
    ) {
        if (mob == null || food == null) {
            return false;
        }

        return tryConsumeDroppedFood(
                mob,
                RetoldMobStates.getOrCreate(mob, gameTime),
                food,
                gameTime
        );
    }

    private static boolean tryConsumeDroppedFood(
            PathfinderMob mob,
            RetoldMobState state,
            ItemEntity food,
            long gameTime
    ) {
        if (!RetoldBehaviorCoordinator.canCompleteMeal(mob)
                || mob.distanceToSqr(food) > EAT_ITEM_DISTANCE_SQUARED) {
            return false;
        }

        Vec3 foodSource = food.position();
        ItemStack stack = food.getItem();

        if (stack.isEmpty()) {
            return false;
        }

        if (!RetoldMobRules.canEatDroppedItem(mob, stack)) {
            return false;
        }

        boolean swallowedStack = mob instanceof AbstractCubeMob;

        if (swallowedStack
                && !RetoldSlimeItemStorage.swallow((AbstractCubeMob) mob, stack)) {
            return false;
        }

        state.addHunger(
                -RetoldMobRules.foodRelief(
                        mob,
                        stack
                )
        );

        state.markFed(gameTime);

        RetoldFeedingAnimations.play(mob);

        if (swallowedStack) {
            food.discard();
        } else {
            stack.shrink(1);

            if (stack.isEmpty()) {
                food.discard();
            } else {
                food.setItem(stack);
            }
        }

        mob.getNavigation().stop();
        RetoldAiControl.clear(mob);
        RetoldFeedingPose.begin(mob, foodSource, gameTime);
        return true;
    }

    private static BlockPos findBestForageBlock(
            ServerLevel level,
            PathfinderMob mob
    ) {
        double maxDistanceSquared = RetoldMobRules.usesRenewableEnvironmentalForage(mob)
                ? FORAGE_HORIZONTAL_RADIUS * FORAGE_HORIZONTAL_RADIUS
                : 16.0D;

        return RetoldForageBlockSearch.findOrdinaryForageBlock(
                level,
                mob,
                FORAGE_HORIZONTAL_RADIUS,
                FORAGE_VERTICAL_RADIUS,
                maxDistanceSquared,
                level.getGameTime(),
                FOOD_BLOCK_SEARCH_CACHE_TICKS
        );
    }

    public static CatchUpForageResult findCatchUpForage(
            ServerLevel level,
            PathfinderMob mob,
            long gameTime,
            int maximumTargets
    ) {
        if (level == null
                || mob == null
                || mob.level() != level
                || maximumTargets <= 0) {
            return CatchUpForageResult.none();
        }

        double maxDistanceSquared = RetoldMobRules
                .usesRenewableEnvironmentalForage(mob)
                ? FORAGE_HORIZONTAL_RADIUS * FORAGE_HORIZONTAL_RADIUS
                : 16.0D;
        RetoldForageBlockSearch.CatchUpFindResult result =
                RetoldForageBlockSearch.findCatchUpForageBlocks(
                        level,
                        mob,
                        FORAGE_HORIZONTAL_RADIUS,
                        FORAGE_VERTICAL_RADIUS,
                        maxDistanceSquared,
                        gameTime,
                        maximumTargets
                );

        return new CatchUpForageResult(
                result.targets(),
                result.deferred()
        );
    }

    /**
     * Applies only the real forage mutation and reports its relief. Catch-up
     * state and meal timestamps remain owned by the reconciliation queue.
     */
    public static CatchUpForageMeal consumeCatchUpForage(
            ServerLevel level,
            PathfinderMob mob,
            BlockPos foragePos
    ) {
        if (level == null
                || mob == null
                || foragePos == null
                || mob.level() != level) {
            return CatchUpForageMeal.none();
        }

        BlockState blockState = level.getBlockState(foragePos);
        boolean renewable = RetoldMobRules.isRenewableEnvironmentalForage(
                mob,
                blockState
        );

        if (!RetoldMobRules.canForageBlock(mob, blockState)
                || !renewable
                && !RetoldMobGriefing.canBreakBlock(level, mob, foragePos)) {
            return CatchUpForageMeal.none();
        }

        int relief = RetoldMobRules.forageRelief(mob, blockState);

        if (!renewable) {
            destroyForageBlock(
                    level,
                    foragePos,
                    RetoldMobRules.getBlockPath(blockState)
            );
        }

        return new CatchUpForageMeal(relief, renewable);
    }

    private static void handleForageBlock(
            ServerLevel level,
            PathfinderMob mob,
            RetoldMobState state,
            BlockPos foragePos,
            long gameTime
    ) {
        if (mob.blockPosition().distSqr(foragePos) <= FORAGE_EAT_DISTANCE_SQUARED) {
            tryConsumeForageBlock(
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

        BlockPos movementTarget = foragePos;

        if (foragePos.getY() >= mob.blockPosition().getY()
                && !level.getBlockState(foragePos)
                .getCollisionShape(level, foragePos)
                .isEmpty()) {
            movementTarget = findForageAccessPos(
                    level,
                    mob,
                    foragePos
            );
        }

        if (movementTarget == null
                || !RetoldBehaviorMovement.throttledMoveToExact(
                mob,
                movementTarget,
                getFoodSpeed(mob),
                gameTime,
                FOOD_PATH_INTERVAL_TICKS,
                1.5D * 1.5D
        )) {
            RetoldBehaviorMovement.stopOwnedMovement(
                    mob,
                    RetoldAiControlOwner.FOOD
            );
        }
    }

    private static BlockPos findForageAccessPos(
            ServerLevel level,
            PathfinderMob mob,
            BlockPos foragePos
    ) {
        int accessDistance = mob.getBbWidth() > 1.0F ? 2 : 1;
        BlockPos best = null;
        double bestDistanceSquared = Double.MAX_VALUE;

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos candidate = foragePos.relative(direction, accessDistance);

            if (!mob.getNavigation().isStableDestination(candidate)
                    || !level.getBlockState(candidate)
                    .getCollisionShape(level, candidate)
                    .isEmpty()
                    || !level.getBlockState(candidate.above())
                    .getCollisionShape(level, candidate.above())
                    .isEmpty()) {
                continue;
            }

            double distanceSquared = mob.distanceToSqr(
                    Vec3.atBottomCenterOf(candidate)
            );

            if (distanceSquared < bestDistanceSquared) {
                bestDistanceSquared = distanceSquared;
                best = candidate;
            }
        }

        return best;
    }

    public static boolean tryConsumeForageBlock(
            ServerLevel level,
            PathfinderMob mob,
            BlockPos foragePos,
            long gameTime
    ) {
        if (level == null || mob == null || foragePos == null) {
            return false;
        }

        return tryConsumeForageBlock(
                level,
                mob,
                RetoldMobStates.getOrCreate(mob, gameTime),
                foragePos,
                gameTime
        );
    }

    private static boolean tryConsumeForageBlock(
            ServerLevel level,
            PathfinderMob mob,
            RetoldMobState state,
            BlockPos foragePos,
            long gameTime
    ) {
        if (!RetoldBehaviorCoordinator.canCompleteMeal(mob)
                || mob.blockPosition().distSqr(foragePos) > FORAGE_EAT_DISTANCE_SQUARED) {
            return false;
        }

        BlockState blockState = level.getBlockState(foragePos);
        boolean renewableEnvironmentalForage =
                RetoldMobRules.isRenewableEnvironmentalForage(mob, blockState);

        if (!renewableEnvironmentalForage
                && !RetoldMobGriefing.canBreakBlock(level, mob, foragePos)) {
            return false;
        }

        if (renewableEnvironmentalForage
                && state.lastAteAt() > 0L
                && gameTime - state.lastAteAt() < 20 * 30) {
            return false;
        }

        if (!RetoldMobRules.canForageBlock(mob, blockState)) {
            return false;
        }

        String blockPath = RetoldMobRules.getBlockPath(blockState);

        state.addHunger(
                -RetoldMobRules.forageRelief(
                        mob,
                        blockState
                )
        );

        state.markFed(gameTime);

        RetoldFeedingAnimations.play(mob);

        if (!renewableEnvironmentalForage) {
            destroyForageBlock(
                    level,
                    foragePos,
                    blockPath
            );
        }

        mob.getNavigation().stop();
        RetoldAiControl.clear(mob);
        RetoldFeedingPose.begin(
                mob,
                Vec3.atCenterOf(foragePos),
                gameTime
        );
        return true;
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
        return claimFoodControl(mob, gameTime, "seek_food");
    }

    static boolean claimFoodControl(
            PathfinderMob mob,
            long gameTime,
            String reason
    ) {
        boolean claimed = RetoldAiControl.tryClaim(
                mob,
                RetoldAiControlMode.FEED,
                CONTROL_OWNER,
                FEED_PRIORITY,
                reason,
                gameTime,
                FEED_CONTROL_TICKS
        );

        if (claimed) {
            FOOD_SEARCH_MEMORIES.remove(mob);
        }

        return claimed;
    }

    private record FoodSearchMemory(
            BlockPos pos,
            long expiresAt
    ) {
    }

    public record CatchUpForageResult(
            List<BlockPos> targets,
            boolean deferred
    ) {
        private static CatchUpForageResult none() {
            return new CatchUpForageResult(List.of(), false);
        }
    }

    public record CatchUpForageMeal(
            int relief,
            boolean reusable
    ) {
        private static CatchUpForageMeal none() {
            return new CatchUpForageMeal(0, false);
        }
    }
}
