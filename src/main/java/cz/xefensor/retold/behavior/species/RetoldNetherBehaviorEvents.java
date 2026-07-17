package cz.xefensor.retold.behavior.species;

import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.control.RetoldAiControlMode;
import cz.xefensor.retold.behavior.control.RetoldAiControlOwner;
import cz.xefensor.retold.behavior.control.RetoldAiPriorities;
import cz.xefensor.retold.behavior.performance.RetoldAiScanCache;
import cz.xefensor.retold.behavior.performance.RetoldAiSightCache;
import cz.xefensor.retold.behavior.core.RetoldBehaviorCombat;
import cz.xefensor.retold.behavior.core.RetoldBehaviorCoordinator;
import cz.xefensor.retold.behavior.core.RetoldBehaviorMovement;
import cz.xefensor.retold.behavior.core.RetoldBehaviorTiming;
import cz.xefensor.retold.behavior.food.RetoldForageBlockSearch;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;
import cz.xefensor.retold.behavior.profiles.RetoldMobState;
import cz.xefensor.retold.behavior.profiles.RetoldMobStates;

import cz.xefensor.retold.combat.RetoldTargetSource;
import cz.xefensor.retold.faction.RetoldFaction;
import cz.xefensor.retold.faction.RetoldFactionMembers;
import cz.xefensor.retold.faction.RetoldFactionRelations;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.List;

public final class RetoldNetherBehaviorEvents {
    private static final int THINK_INTERVAL_TICKS = 14;
    private static final int NETHER_SCAN_CACHE_TICKS = 7;
    private static final int NETHER_BLOCK_SEARCH_CACHE_TICKS = 28;
    private static final int NETHER_PATH_INTERVAL_TICKS = 7;

    private static final int REMNANT_ATTACK_CONTROL_TICKS = 20 * 4;
    private static final int REMNANT_ATTACK_PRIORITY = RetoldAiPriorities.below(RetoldAiPriorities.FLEE, 3);

    private static final double UNDEAD_PRESSURE_RADIUS_BLOCKS = 26.0D;
    private static final double UNDEAD_PRESSURE_RADIUS_SQUARED =
            UNDEAD_PRESSURE_RADIUS_BLOCKS * UNDEAD_PRESSURE_RADIUS_BLOCKS;

    private static final double FOOD_SEARCH_RADIUS_BLOCKS = 18.0D;
    private static final double FOOD_SEARCH_RADIUS_SQUARED =
            FOOD_SEARCH_RADIUS_BLOCKS * FOOD_SEARCH_RADIUS_BLOCKS;

    private static final int FORAGE_SEARCH_HORIZONTAL_RADIUS = 14;
    private static final int FORAGE_SEARCH_VERTICAL_RADIUS = 4;

    private static final int FOOD_SEARCH_CONTROL_TICKS = 20 * 5;
    private static final int FOOD_SEARCH_PRIORITY = RetoldAiPriorities.below(RetoldAiPriorities.FEED, 3);

    private static final double PIGLIN_FOOD_SEARCH_SPEED = 0.74D;
    private static final double HOGLIN_FOOD_SEARCH_SPEED = 0.78D;
    private static final double REMNANT_ATTACK_SPEED = 1.12D;

    private RetoldNetherBehaviorEvents() {
    }

    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof PathfinderMob mob)) {
            return;
        }

        if (!(mob.level() instanceof ServerLevel level)) {
            return;
        }

        boolean isNetherRemnant = RetoldFactionMembers.isNetherRemnant(mob);
        boolean isNetherHungryLife = isNetherHungryLife(mob);

        if (!isNetherRemnant && !isNetherHungryLife) {
            return;
        }

        long gameTime = level.getGameTime();

        if (!shouldThink(mob, gameTime)) {
            return;
        }

        if (isNetherRemnant) {
            handleRemnantUndeadPressure(
                    level,
                    mob,
                    gameTime
            );
        }

        if (isNetherHungryLife) {
            handleNetherFoodSearch(
                    level,
                    mob,
                    gameTime
            );
        }
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

    private static void handleRemnantUndeadPressure(
            ServerLevel level,
            PathfinderMob remnant,
            long gameTime
    ) {
        if (!canPressureUndead(remnant)) {
            return;
        }

        LivingEntity target = findBestUndeadPressureTarget(
                level,
                remnant
        );

        if (target == null) {
            return;
        }

        if (!RetoldBehaviorCombat.claimAttackControl(
                remnant,
                RetoldAiControlOwner.COMBAT,
                REMNANT_ATTACK_PRIORITY,
                "nether_remnant_undead_pressure",
                gameTime,
                REMNANT_ATTACK_CONTROL_TICKS
        )) {
            return;
        }

        if (!RetoldBehaviorCombat.applyAttackTargetOrClearOwner(
                remnant,
                target,
                RetoldTargetSource.FACTION_COMBAT,
                RetoldAiControlOwner.COMBAT
        )) {
            return;
        }

        RetoldBehaviorMovement.throttledMoveTo(
                remnant,
                target,
                REMNANT_ATTACK_SPEED,
                gameTime,
                NETHER_PATH_INTERVAL_TICKS,
                2.0D * 2.0D
        );
    }

    private static boolean canPressureUndead(PathfinderMob remnant) {
        if (remnant == null || !remnant.isAlive() || remnant.isRemoved()) {
            return false;
        }

        if (RetoldBehaviorCoordinator.hasLiveTarget(remnant)) {
            return false;
        }

        RetoldAiControlMode mode = RetoldAiControl.getMode(remnant);

        return mode == RetoldAiControlMode.NONE
                || RetoldAiControl.isControlledAsBy(
                remnant,
                RetoldAiControlMode.ATTACK,
                RetoldAiControlOwner.COMBAT
        );
    }

    private static LivingEntity findBestUndeadPressureTarget(
            ServerLevel level,
            PathfinderMob remnant
    ) {
        List<LivingEntity> candidates = RetoldAiScanCache.nearby(
                level,
                remnant,
                LivingEntity.class,
                UNDEAD_PRESSURE_RADIUS_BLOCKS,
                level.getGameTime(),
                NETHER_SCAN_CACHE_TICKS
        );

        LivingEntity bestTarget = null;
        double bestScore = Double.MAX_VALUE;

        for (LivingEntity candidate : candidates) {
            if (!isValidUndeadPressureTarget(remnant, candidate)) {
                continue;
            }

            double distanceSquared = remnant.distanceToSqr(candidate);

            if (distanceSquared > UNDEAD_PRESSURE_RADIUS_SQUARED) {
                continue;
            }

            double score = distanceSquared;
            String path = RetoldMobRules.getEntityTypePath(candidate.getType());

            if (path.equals("zombified_piglin")) {
                score -= 160.0D;
            }

            if (path.equals("wither_skeleton")) {
                score -= 60.0D;
            }

            if (RetoldAiSightCache.canSee(remnant, candidate, level.getGameTime())) {
                score -= 30.0D;
            }

            if (score < bestScore) {
                bestScore = score;
                bestTarget = candidate;
            }
        }

        return bestTarget;
    }

    private static boolean isValidUndeadPressureTarget(
            PathfinderMob remnant,
            LivingEntity candidate
    ) {
        if (candidate == null || candidate == remnant) {
            return false;
        }

        if (!RetoldBehaviorCoordinator.isAliveInSameLevel(remnant, candidate)) {
            return false;
        }

        if (RetoldBehaviorCoordinator.isInvalidPlayerTarget(candidate)) {
            return false;
        }

        if (remnant.distanceToSqr(candidate) > UNDEAD_PRESSURE_RADIUS_SQUARED) {
            return false;
        }

        if (!RetoldFactionMembers.isUndead(candidate)) {
            return false;
        }

        if (!RetoldFactionRelations.shouldAttack(remnant, candidate)) {
            return false;
        }

        return RetoldAiSightCache.canSee(remnant, candidate, remnant.level().getGameTime())
                || remnant.distanceToSqr(candidate) <= 64.0D;
    }

    private static boolean isNetherHungryLife(PathfinderMob mob) {
        return RetoldMobRules.canUseOrdinaryLifeSystems(mob)
                && RetoldMobRules.isNetherHungry(mob);
    }

    private static void handleNetherFoodSearch(
            ServerLevel level,
            PathfinderMob mob,
            long gameTime
    ) {
        if (!canSearchForNetherFood(mob, gameTime)) {
            return;
        }

        ItemEntity droppedFood = findBestDroppedFood(
                level,
                mob
        );

        if (droppedFood != null) {
            moveTowardFood(
                    mob,
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
            moveTowardForage(
                    mob,
                    foragePos,
                    gameTime
            );
        }
    }

    private static boolean canSearchForNetherFood(
            PathfinderMob mob,
            long gameTime
    ) {
        if (mob == null || !mob.isAlive() || mob.isRemoved()) {
            return false;
        }

        if (RetoldBehaviorCoordinator.hasLiveTarget(mob)) {
            return false;
        }

        RetoldAiControlMode mode = RetoldAiControl.getMode(mob);

        if (
                mode != RetoldAiControlMode.NONE
                        && !RetoldAiControl.isControlledAsBy(
                        mob,
                        RetoldAiControlMode.FEED,
                        RetoldAiControlOwner.FOOD
                )
        ) {
            return false;
        }

        RetoldMobState state = RetoldMobStates.getOrCreate(
                mob,
                gameTime
        );

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
                FOOD_SEARCH_RADIUS_BLOCKS,
                level.getGameTime(),
                NETHER_SCAN_CACHE_TICKS
        );

        ItemEntity best = null;
        double bestScore = Double.MAX_VALUE;

        for (ItemEntity item : items) {
            if (!isValidDroppedFood(mob, item)) {
                continue;
            }

            double distanceSquared = mob.distanceToSqr(item);

            if (distanceSquared > FOOD_SEARCH_RADIUS_SQUARED) {
                continue;
            }

            double score = distanceSquared;

            if (RetoldAiSightCache.canSee(mob, item, level.getGameTime())) {
                score -= 16.0D;
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
        if (item == null || !item.isAlive() || item.isRemoved()) {
            return false;
        }

        if (mob.distanceToSqr(item) > FOOD_SEARCH_RADIUS_SQUARED) {
            return false;
        }

        if (
                !RetoldAiSightCache.canSee(mob, item, mob.level().getGameTime())
                        && mob.distanceToSqr(item) > 64.0D
        ) {
            return false;
        }

        return RetoldMobRules.canEatDroppedItem(
                mob,
                item.getItem()
        );
    }

    private static BlockPos findBestForageBlock(
            ServerLevel level,
            PathfinderMob mob
    ) {
        return RetoldForageBlockSearch.findNetherForageBlock(
                level,
                mob,
                FORAGE_SEARCH_HORIZONTAL_RADIUS,
                FORAGE_SEARCH_VERTICAL_RADIUS,
                FOOD_SEARCH_RADIUS_SQUARED,
                level.getGameTime(),
                NETHER_BLOCK_SEARCH_CACHE_TICKS
        );
    }

    private static void moveTowardFood(
            PathfinderMob mob,
            ItemEntity food,
            long gameTime
    ) {
        if (!claimFoodSearch(mob, gameTime)) {
            return;
        }

        RetoldBehaviorMovement.throttledMoveTo(
                mob,
                food,
                foodSearchSpeed(mob),
                gameTime,
                NETHER_PATH_INTERVAL_TICKS,
                1.5D * 1.5D
        );
    }

    private static void moveTowardForage(
            PathfinderMob mob,
            BlockPos pos,
            long gameTime
    ) {
        if (!claimFoodSearch(mob, gameTime)) {
            return;
        }

        RetoldBehaviorMovement.throttledMoveTo(
                mob,
                pos,
                foodSearchSpeed(mob),
                gameTime,
                NETHER_PATH_INTERVAL_TICKS,
                1.5D * 1.5D
        );
    }

    private static boolean claimFoodSearch(
            PathfinderMob mob,
            long gameTime
    ) {
        return RetoldAiControl.tryClaim(
                mob,
                RetoldAiControlMode.FEED,
                RetoldAiControlOwner.FOOD,
                FOOD_SEARCH_PRIORITY,
                "nether_food_search",
                gameTime,
                FOOD_SEARCH_CONTROL_TICKS
        );
    }

    private static double foodSearchSpeed(PathfinderMob mob) {
        return RetoldMobRules.isHoglin(mob)
                ? HOGLIN_FOOD_SEARCH_SPEED
                : PIGLIN_FOOD_SEARCH_SPEED;
    }
}
