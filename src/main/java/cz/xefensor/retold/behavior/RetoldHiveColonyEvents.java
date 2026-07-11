package cz.xefensor.retold.behavior;

import cz.xefensor.retold.combat.RetoldTargetSource;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class RetoldHiveColonyEvents {
    private static final Map<PathfinderMob, FlowerMemory> FLOWER_MEMORIES = new WeakHashMap<>();

    private static final int THINK_INTERVAL_TICKS = 18;
    private static final int FLOWER_CONTROL_TICKS = 20 * 5;
    private static final int DEFENSE_CONTROL_TICKS = 20 * 4;

    private static final int FLOWER_SEARCH_PRIORITY = RetoldAiPriorities.below(RetoldAiPriorities.FEED, 1);
    private static final int DEFENSE_PRIORITY = RetoldAiPriorities.DEFENSE;

    private static final double FLOWER_SEARCH_RADIUS_BLOCKS = 18.0D;
    private static final double FLOWER_SEARCH_RADIUS_SQUARED =
            FLOWER_SEARCH_RADIUS_BLOCKS * FLOWER_SEARCH_RADIUS_BLOCKS;

    private static final int FLOWER_SEARCH_HORIZONTAL_RADIUS = 16;
    private static final int FLOWER_SEARCH_VERTICAL_RADIUS = 5;

    private static final double FLOWER_FEED_DISTANCE_BLOCKS = 2.4D;
    private static final double FLOWER_FEED_DISTANCE_SQUARED =
            FLOWER_FEED_DISTANCE_BLOCKS * FLOWER_FEED_DISTANCE_BLOCKS;

    private static final double DEFENSE_SHARE_RADIUS_BLOCKS = 18.0D;
    private static final double DEFENSE_SHARE_RADIUS_SQUARED =
            DEFENSE_SHARE_RADIUS_BLOCKS * DEFENSE_SHARE_RADIUS_BLOCKS;

    private static final double FLOWER_SPEED = 0.78D;
    private static final double DEFENSE_SPEED = 1.18D;

    private RetoldHiveColonyEvents() {
    }

    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof PathfinderMob bee)) {
            return;
        }

        if (!(bee.level() instanceof ServerLevel level)) {
            return;
        }

        if (!isHiveBee(bee)) {
            FLOWER_MEMORIES.remove(bee);
            return;
        }

        long gameTime = level.getGameTime();

        if (!shouldThink(bee, gameTime)) {
            return;
        }

        LivingEntity target = bee.getTarget();

        if (isValidDefenseTarget(bee, target)) {
            shareDefenseTarget(
                    level,
                    bee,
                    target,
                    gameTime
            );
            return;
        }

        LivingEntity sharedTarget = findSharedDefenseTarget(
                level,
                bee
        );

        if (sharedTarget != null && canDefend(bee)) {
            defendAgainst(
                    bee,
                    sharedTarget,
                    gameTime
            );
            return;
        }

        handleFlowerForaging(
                level,
                bee,
                gameTime
        );
    }

    private static boolean isHiveBee(PathfinderMob mob) {
        return RetoldMobRules.canUseOrdinaryLifeSystems(mob)
                && RetoldMobRules.isHiveColony(mob);
    }

    private static boolean shouldThink(
            PathfinderMob bee,
            long gameTime
    ) {
        return RetoldBehaviorTiming.shouldThink(
                bee,
                gameTime,
                THINK_INTERVAL_TICKS
        );
    }

    private static LivingEntity findSharedDefenseTarget(
            ServerLevel level,
            PathfinderMob bee
    ) {
        AABB area = bee.getBoundingBox().inflate(DEFENSE_SHARE_RADIUS_BLOCKS);

        List<PathfinderMob> sources = level.getEntitiesOfClass(
                PathfinderMob.class,
                area,
                source -> isValidDefenseSource(
                        bee,
                        source
                )
        );

        LivingEntity bestTarget = null;
        double bestScore = Double.MAX_VALUE;

        for (PathfinderMob source : sources) {
            LivingEntity target = source.getTarget();

            if (!isValidDefenseTarget(bee, target)) {
                continue;
            }

            double score = bee.distanceToSqr(source);

            if (source.hasLineOfSight(target)) {
                score -= 18.0D;
            }

            if (score < bestScore) {
                bestScore = score;
                bestTarget = target;
            }
        }

        return bestTarget;
    }

    private static boolean isValidDefenseSource(
            PathfinderMob bee,
            PathfinderMob source
    ) {
        if (source == null || source == bee) {
            return false;
        }

        if (!isHiveBee(source)) {
            return false;
        }

        if (!RetoldBehaviorCoordinator.isAliveInSameLevel(bee, source)) {
            return false;
        }

        if (bee.distanceToSqr(source) > DEFENSE_SHARE_RADIUS_SQUARED) {
            return false;
        }

        return isValidDefenseTarget(
                bee,
                source.getTarget()
        );
    }

    private static void shareDefenseTarget(
            ServerLevel level,
            PathfinderMob source,
            LivingEntity target,
            long gameTime
    ) {
        AABB area = source.getBoundingBox().inflate(DEFENSE_SHARE_RADIUS_BLOCKS);

        for (PathfinderMob recruit : level.getEntitiesOfClass(
                PathfinderMob.class,
                area,
                candidate -> isValidDefenseRecruit(
                        source,
                        candidate
                )
        )) {
            defendAgainst(
                    recruit,
                    target,
                    gameTime
            );
        }
    }

    private static boolean isValidDefenseRecruit(
            PathfinderMob source,
            PathfinderMob recruit
    ) {
        if (recruit == null || recruit == source) {
            return false;
        }

        if (!isHiveBee(recruit)) {
            return false;
        }

        if (!RetoldBehaviorCoordinator.isAliveInSameLevel(source, recruit)) {
            return false;
        }

        if (source.distanceToSqr(recruit) > DEFENSE_SHARE_RADIUS_SQUARED) {
            return false;
        }

        return canDefend(recruit);
    }

    private static boolean canDefend(PathfinderMob bee) {
        if (RetoldBehaviorCoordinator.hasLiveTarget(bee)) {
            return false;
        }

        return RetoldBehaviorCombat.canUseAttackControl(
                bee,
                RetoldAiControlOwner.HIVE_COLONY
        );
    }

    private static boolean isValidDefenseTarget(
            PathfinderMob bee,
            LivingEntity target
    ) {
        if (!RetoldBehaviorCoordinator.isValidAssignmentTarget(bee, target) || target == bee) {
            return false;
        }

        return !isHiveBeeTarget(target);
    }

    private static boolean isHiveBeeTarget(LivingEntity target) {
        return target instanceof PathfinderMob mob && isHiveBee(mob);
    }

    private static void defendAgainst(
            PathfinderMob bee,
            LivingEntity target,
            long gameTime
    ) {
        if (!RetoldBehaviorCombat.claimAttackControl(
                bee,
                RetoldAiControlOwner.HIVE_COLONY,
                DEFENSE_PRIORITY,
                "hive_defense",
                gameTime,
                DEFENSE_CONTROL_TICKS
        )) {
            return;
        }

        if (!RetoldBehaviorCombat.applyAttackTargetOrClearOwner(
                bee,
                target,
                RetoldTargetSource.FACTION_ASSIST,
                RetoldAiControlOwner.HIVE_COLONY
        )) {
            return;
        }

        RetoldAiControl.withNavigationBypass(() -> {
            bee.getNavigation().moveTo(
                    target,
                    DEFENSE_SPEED
            );
        });
    }

    private static void handleFlowerForaging(
            ServerLevel level,
            PathfinderMob bee,
            long gameTime
    ) {
        if (!canForageFlowers(bee, gameTime)) {
            return;
        }

        FlowerMemory memory = FLOWER_MEMORIES.get(bee);

        if (memory == null || !isValidFlower(level, memory.pos())) {
            BlockPos flowerPos = findBestFlower(
                    level,
                    bee
            );

            if (flowerPos == null) {
                return;
            }

            memory = new FlowerMemory(
                    flowerPos,
                    gameTime
            );
            FLOWER_MEMORIES.put(
                    bee,
                    memory
            );
        }

        if (bee.blockPosition().distSqr(memory.pos()) <= FLOWER_FEED_DISTANCE_SQUARED) {
            feedFromFlower(
                    level,
                    bee,
                    memory.pos(),
                    gameTime
            );
            return;
        }

        moveTowardFlower(
                bee,
                memory.pos(),
                gameTime
        );
    }

    private static boolean canForageFlowers(
            PathfinderMob bee,
            long gameTime
    ) {
        if (RetoldBehaviorCoordinator.hasLiveTarget(bee)) {
            return false;
        }

        RetoldAiControlMode mode = RetoldAiControl.getMode(bee);

        if (
                mode != RetoldAiControlMode.NONE
                        && !RetoldAiControl.isControlledAsBy(
                        bee,
                        RetoldAiControlMode.FEED,
                        RetoldAiControlOwner.HIVE_COLONY
                )
        ) {
            return false;
        }

        RetoldMobState state = RetoldMobStates.getOrCreate(
                bee,
                gameTime
        );

        return RetoldMobRules.hasEatDrive(
                bee,
                state
        );
    }

    private static BlockPos findBestFlower(
            ServerLevel level,
            PathfinderMob bee
    ) {
        BlockPos center = bee.blockPosition();
        BlockPos best = null;
        double bestScore = Double.MAX_VALUE;

        for (int dx = -FLOWER_SEARCH_HORIZONTAL_RADIUS; dx <= FLOWER_SEARCH_HORIZONTAL_RADIUS; dx++) {
            for (int dz = -FLOWER_SEARCH_HORIZONTAL_RADIUS; dz <= FLOWER_SEARCH_HORIZONTAL_RADIUS; dz++) {
                for (int dy = -FLOWER_SEARCH_VERTICAL_RADIUS; dy <= FLOWER_SEARCH_VERTICAL_RADIUS; dy++) {
                    BlockPos pos = center.offset(dx, dy, dz);

                    if (!isValidFlower(level, pos)) {
                        continue;
                    }

                    double distanceSquared = center.distSqr(pos);

                    if (distanceSquared > FLOWER_SEARCH_RADIUS_SQUARED) {
                        continue;
                    }

                    double score = distanceSquared;

                    if (score < bestScore) {
                        bestScore = score;
                        best = pos.immutable();
                    }
                }
            }
        }

        return best;
    }

    private static boolean isValidFlower(
            ServerLevel level,
            BlockPos pos
    ) {
        BlockState state = level.getBlockState(pos);

        return RetoldMobRules.isFlowerBlock(state);
    }

    private static void feedFromFlower(
            ServerLevel level,
            PathfinderMob bee,
            BlockPos flowerPos,
            long gameTime
    ) {
        BlockState state = level.getBlockState(flowerPos);

        if (!RetoldMobRules.isFlowerBlock(state)) {
            FLOWER_MEMORIES.remove(bee);
            return;
        }

        RetoldMobState mobState = RetoldMobStates.getOrCreate(
                bee,
                gameTime
        );

        mobState.addHunger(
                -RetoldMobRules.forageRelief(
                        bee,
                        RetoldMobRules.getBlockPath(state)
                )
        );
        mobState.markFed(gameTime);

        RetoldFeedingAnimations.play(bee);

        RetoldAiControl.clearIfOwnedBy(
                bee,
                RetoldAiControlOwner.HIVE_COLONY
        );
        bee.getNavigation().stop();
    }

    private static void moveTowardFlower(
            PathfinderMob bee,
            BlockPos flowerPos,
            long gameTime
    ) {
        if (!RetoldAiControl.tryClaim(
                bee,
                RetoldAiControlMode.FEED,
                RetoldAiControlOwner.HIVE_COLONY,
                FLOWER_SEARCH_PRIORITY,
                "hive_flower_patch",
                gameTime,
                FLOWER_CONTROL_TICKS
        )) {
            return;
        }

        RetoldAiControl.withNavigationBypass(() -> {
            bee.getNavigation().moveTo(
                    flowerPos.getX() + 0.5D,
                    flowerPos.getY() + 0.5D,
                    flowerPos.getZ() + 0.5D,
                    FLOWER_SPEED
            );
        });
    }

    private record FlowerMemory(
            BlockPos pos,
            long foundAt
    ) {
    }
}
