package cz.xefensor.retold.behavior;

import cz.xefensor.retold.combat.RetoldTargetSource;
import cz.xefensor.retold.faction.RetoldFaction;
import cz.xefensor.retold.faction.RetoldFactionMembers;
import cz.xefensor.retold.faction.RetoldFactionRelations;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.List;

public final class RetoldNetherBehaviorEvents {
    private static final int THINK_INTERVAL_TICKS = 14;

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

        long gameTime = level.getGameTime();

        if (!shouldThink(mob, gameTime)) {
            return;
        }

        if (RetoldFactionMembers.isNetherRemnant(mob)) {
            handleRemnantUndeadPressure(
                    level,
                    mob,
                    gameTime
            );
        }

        if (isNetherHungryLife(mob)) {
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

        RetoldAiControl.withNavigationBypass(() -> {
            remnant.getNavigation().moveTo(
                    target,
                    REMNANT_ATTACK_SPEED
            );
        });
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
        AABB area = remnant.getBoundingBox().inflate(UNDEAD_PRESSURE_RADIUS_BLOCKS);

        List<LivingEntity> candidates = level.getEntitiesOfClass(
                LivingEntity.class,
                area,
                candidate -> isValidUndeadPressureTarget(
                        remnant,
                        candidate
                )
        );

        LivingEntity bestTarget = null;
        double bestScore = Double.MAX_VALUE;

        for (LivingEntity candidate : candidates) {
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

            if (remnant.hasLineOfSight(candidate)) {
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

        return remnant.hasLineOfSight(candidate)
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
        AABB area = mob.getBoundingBox().inflate(FOOD_SEARCH_RADIUS_BLOCKS);

        List<ItemEntity> items = level.getEntitiesOfClass(
                ItemEntity.class,
                area,
                item -> isValidDroppedFood(
                        mob,
                        item
                )
        );

        ItemEntity best = null;
        double bestScore = Double.MAX_VALUE;

        for (ItemEntity item : items) {
            double distanceSquared = mob.distanceToSqr(item);

            if (distanceSquared > FOOD_SEARCH_RADIUS_SQUARED) {
                continue;
            }

            double score = distanceSquared;

            if (mob.hasLineOfSight(item)) {
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

        if (!mob.hasLineOfSight(item) && mob.distanceToSqr(item) > 64.0D) {
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
        BlockPos center = mob.blockPosition();
        BlockPos best = null;
        double bestScore = Double.MAX_VALUE;

        for (int dx = -FORAGE_SEARCH_HORIZONTAL_RADIUS; dx <= FORAGE_SEARCH_HORIZONTAL_RADIUS; dx++) {
            for (int dz = -FORAGE_SEARCH_HORIZONTAL_RADIUS; dz <= FORAGE_SEARCH_HORIZONTAL_RADIUS; dz++) {
                for (int dy = -FORAGE_SEARCH_VERTICAL_RADIUS; dy <= FORAGE_SEARCH_VERTICAL_RADIUS; dy++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(pos);

                    if (!isNetherForageBlock(mob, state)) {
                        continue;
                    }

                    double distanceSquared = center.distSqr(pos);

                    if (distanceSquared > FOOD_SEARCH_RADIUS_SQUARED) {
                        continue;
                    }

                    double score = distanceSquared;

                    if (state.is(Blocks.CRIMSON_FUNGUS)) {
                        score -= 12.0D;
                    }

                    if (score < bestScore) {
                        bestScore = score;
                        best = pos.immutable();
                    }
                }
            }
        }

        return best;
    }

    private static boolean isNetherForageBlock(
            PathfinderMob mob,
            BlockState state
    ) {
        if (state == null) {
            return false;
        }

        return RetoldMobRules.canForageBlock(
                mob,
                state
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

        RetoldAiControl.withNavigationBypass(() -> {
            mob.getNavigation().moveTo(
                    food,
                    foodSearchSpeed(mob)
            );
        });
    }

    private static void moveTowardForage(
            PathfinderMob mob,
            BlockPos pos,
            long gameTime
    ) {
        if (!claimFoodSearch(mob, gameTime)) {
            return;
        }

        RetoldAiControl.withNavigationBypass(() -> {
            mob.getNavigation().moveTo(
                    pos.getX() + 0.5D,
                    pos.getY(),
                    pos.getZ() + 0.5D,
                    foodSearchSpeed(mob)
            );
        });
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
