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
import cz.xefensor.retold.behavior.core.RetoldBehaviorTargets;
import cz.xefensor.retold.behavior.core.RetoldBehaviorTiming;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;
import cz.xefensor.retold.behavior.profiles.RetoldMobState;
import cz.xefensor.retold.behavior.profiles.RetoldMobStates;
import cz.xefensor.retold.behavior.hunting.RetoldPreyTargeting;

import cz.xefensor.retold.combat.RetoldTargetSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.List;

public final class RetoldSwarmScavengerEvents {
    private static final int THINK_INTERVAL_TICKS = 12;
    private static final int SWARM_SCAN_CACHE_TICKS = 6;
    private static final int SWARM_PATH_INTERVAL_TICKS = 6;

    private static final int SPIDER_SWARM_CONTROL_TICKS = 20 * 4;
    private static final int SLIME_SWARM_CONTROL_TICKS = 20 * 4;
    private static final int SMALL_ARTHROPOD_SWARM_CONTROL_TICKS = 20 * 4;
    private static final int SCAVENGE_CONTROL_TICKS = 20 * 5;

    private static final int SPIDER_SWARM_PRIORITY = RetoldAiPriorities.below(RetoldAiPriorities.HUNT, 1);
    private static final int SLIME_SWARM_PRIORITY = RetoldAiPriorities.below(RetoldAiPriorities.FEED, 3);
    private static final int SMALL_ARTHROPOD_SWARM_PRIORITY = RetoldAiPriorities.above(RetoldAiPriorities.HUNT, 3);
    private static final int SCAVENGE_PRIORITY = RetoldAiPriorities.above(RetoldAiPriorities.HUNT, 5);

    private static final double SPIDER_SWARM_RADIUS_BLOCKS = 18.0D;
    private static final double SPIDER_SWARM_RADIUS_SQUARED =
            SPIDER_SWARM_RADIUS_BLOCKS * SPIDER_SWARM_RADIUS_BLOCKS;

    private static final double SLIME_SWARM_RADIUS_BLOCKS = 14.0D;
    private static final double SLIME_SWARM_RADIUS_SQUARED =
            SLIME_SWARM_RADIUS_BLOCKS * SLIME_SWARM_RADIUS_BLOCKS;

    private static final double SMALL_ARTHROPOD_SWARM_RADIUS_BLOCKS = 10.0D;
    private static final double SMALL_ARTHROPOD_SWARM_RADIUS_SQUARED =
            SMALL_ARTHROPOD_SWARM_RADIUS_BLOCKS * SMALL_ARTHROPOD_SWARM_RADIUS_BLOCKS;

    private static final double SCAVENGE_RADIUS_BLOCKS = 14.0D;
    private static final double SCAVENGE_RADIUS_SQUARED =
            SCAVENGE_RADIUS_BLOCKS * SCAVENGE_RADIUS_BLOCKS;

    private static final double SPIDER_SWARM_SPEED = 1.12D;
    private static final double SLIME_SWARM_SPEED = 0.92D;
    private static final double SMALL_ARTHROPOD_SWARM_SPEED = 1.08D;
    private static final double SCAVENGE_SPEED = 0.72D;

    private RetoldSwarmScavengerEvents() {
    }

    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof PathfinderMob mob)) {
            return;
        }

        if (!(mob.level() instanceof ServerLevel level)) {
            return;
        }

        if (!isSwarmHandledHere(mob)) {
            return;
        }

        long gameTime = level.getGameTime();

        if (!shouldThink(mob, gameTime)) {
            return;
        }

        if (isSpiderSwarmPredator(mob)) {
            handleSpiderSwarm(
                    level,
                    mob,
                    gameTime
            );
            return;
        }

        if (isSlimeScavenger(mob)) {
            handleSlimeScavenger(
                    level,
                    mob,
                    gameTime
            );
            return;
        }

        if (isSmallArthropodSwarm(mob)) {
            handleSmallArthropodSwarm(
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

    private static boolean isSpiderSwarmPredator(PathfinderMob mob) {
        return RetoldMobRules.canUseOrdinaryLifeSystems(mob)
                && RetoldMobRules.isHungrySwarmPredator(mob);
    }

    private static boolean isSlimeScavenger(PathfinderMob mob) {
        return RetoldMobRules.canUseOrdinaryLifeSystems(mob)
                && RetoldMobRules.isSlimeHungry(mob);
    }

    private static boolean isSmallArthropodSwarm(PathfinderMob mob) {
        return RetoldMobRules.isSmallArthropodSwarm(mob);
    }

    private static boolean isSwarmHandledHere(PathfinderMob mob) {
        return isSpiderSwarmPredator(mob)
                || isSlimeScavenger(mob)
                || isSmallArthropodSwarm(mob);
    }

    private static void handleSpiderSwarm(
            ServerLevel level,
            PathfinderMob spider,
            long gameTime
    ) {
        LivingEntity target = spider.getTarget();

        if (!isValidSpiderFoodTarget(spider, target, gameTime)) {
            target = findSharedSpiderTarget(
                    level,
                    spider,
                    gameTime
            );
        } else {
            spreadSpiderTarget(
                    level,
                    spider,
                    target,
                    gameTime
            );
            return;
        }

        if (target == null || !canJoinSpiderSwarm(spider, gameTime)) {
            return;
        }

        joinSpiderHunt(
                spider,
                target,
                gameTime
        );
    }

    private static LivingEntity findSharedSpiderTarget(
            ServerLevel level,
            PathfinderMob spider,
            long gameTime
    ) {
        List<PathfinderMob> sources = RetoldAiScanCache.nearby(
                level,
                spider,
                PathfinderMob.class,
                SPIDER_SWARM_RADIUS_BLOCKS,
                gameTime,
                SWARM_SCAN_CACHE_TICKS
        );

        LivingEntity bestTarget = null;
        double bestScore = Double.MAX_VALUE;

        for (PathfinderMob source : sources) {
            if (!isValidSpiderSwarmSource(spider, source, gameTime)) {
                continue;
            }

            LivingEntity target = source.getTarget();

            if (!isValidSpiderFoodTarget(spider, target, gameTime)) {
                continue;
            }

            double score = spider.distanceToSqr(source);

            if (RetoldAiSightCache.canSee(source, target, gameTime)) {
                score -= 20.0D;
            }

            if (RetoldAiSightCache.canSee(spider, source, gameTime)) {
                score -= 8.0D;
            }

            if (score < bestScore) {
                bestScore = score;
                bestTarget = target;
            }
        }

        return bestTarget;
    }

    private static void spreadSpiderTarget(
            ServerLevel level,
            PathfinderMob source,
            LivingEntity target,
            long gameTime
    ) {
        for (PathfinderMob recruit : RetoldAiScanCache.nearby(
                level,
                source,
                PathfinderMob.class,
                SPIDER_SWARM_RADIUS_BLOCKS,
                gameTime,
                SWARM_SCAN_CACHE_TICKS
        )) {
            if (!isValidSpiderRecruit(source, recruit, gameTime)) {
                continue;
            }

            joinSpiderHunt(
                    recruit,
                    target,
                    gameTime
            );
        }
    }

    private static boolean isValidSpiderSwarmSource(
            PathfinderMob spider,
            PathfinderMob source,
            long gameTime
    ) {
        if (source == null || source == spider) {
            return false;
        }

        if (!isSpiderSwarmPredator(source)) {
            return false;
        }

        if (!RetoldBehaviorCoordinator.isAliveInSameLevel(spider, source)) {
            return false;
        }

        if (spider.distanceToSqr(source) > SPIDER_SWARM_RADIUS_SQUARED) {
            return false;
        }

        return isValidSpiderFoodTarget(
                spider,
                source.getTarget(),
                gameTime
        );
    }

    private static boolean isValidSpiderRecruit(
            PathfinderMob source,
            PathfinderMob recruit,
            long gameTime
    ) {
        if (recruit == null || recruit == source) {
            return false;
        }

        if (!isSpiderSwarmPredator(recruit)) {
            return false;
        }

        if (!RetoldBehaviorCoordinator.isAliveInSameLevel(source, recruit)) {
            return false;
        }

        if (source.distanceToSqr(recruit) > SPIDER_SWARM_RADIUS_SQUARED) {
            return false;
        }

        return canJoinSpiderSwarm(
                recruit,
                gameTime
        );
    }

    private static boolean canJoinSpiderSwarm(
            PathfinderMob spider,
            long gameTime
    ) {
        if (RetoldBehaviorCoordinator.hasLiveTarget(spider)) {
            return false;
        }

        RetoldAiControlMode mode = RetoldAiControl.getMode(spider);

        if (
                mode != RetoldAiControlMode.NONE
                        && !RetoldAiControl.isControlledAsBy(
                        spider,
                        RetoldAiControlMode.HUNT,
                        RetoldAiControlOwner.SWARM
                )
        ) {
            return false;
        }

        RetoldMobState state = RetoldMobStates.getOrCreate(
                spider,
                gameTime
        );

        return RetoldMobRules.hasHuntDrive(
                spider,
                state
        );
    }

    private static boolean isValidSpiderFoodTarget(
            PathfinderMob spider,
            LivingEntity target,
            long gameTime
    ) {
        if (target == null || target == spider) {
            return false;
        }

        return RetoldPreyTargeting.isValidMobRulePrey(
                spider,
                target,
                gameTime
        );
    }

    private static void joinSpiderHunt(
            PathfinderMob spider,
            LivingEntity target,
            long gameTime
    ) {
        if (!RetoldAiControl.tryClaim(
                spider,
                RetoldAiControlMode.HUNT,
                RetoldAiControlOwner.SWARM,
                SPIDER_SWARM_PRIORITY,
                "spider_swarm",
                gameTime,
                SPIDER_SWARM_CONTROL_TICKS
        )) {
            return;
        }

        if (!RetoldBehaviorTargets.setAttackTargetOrClearOwner(
                spider,
                target,
                RetoldAiControlOwner.SWARM
        )) {
            return;
        }

        spider.setSprinting(true);

        RetoldBehaviorMovement.throttledMoveTo(
                spider,
                target,
                SPIDER_SWARM_SPEED,
                gameTime,
                SWARM_PATH_INTERVAL_TICKS,
                2.0D * 2.0D
        );
    }

    private static void handleSlimeScavenger(
            ServerLevel level,
            PathfinderMob slime,
            long gameTime
    ) {
        LivingEntity target = slime.getTarget();

        if (isValidSlimeTarget(slime, target)) {
            spreadSlimeTarget(
                    level,
                    slime,
                    target,
                    gameTime
            );
            return;
        }

        LivingEntity sharedTarget = findSharedSlimeTarget(
                level,
                slime
        );

        if (sharedTarget != null && canJoinSlimeSwarm(slime)) {
            joinSlimeAttack(
                    slime,
                    sharedTarget,
                    gameTime
            );
            return;
        }

        ItemEntity food = findBestOrganicScrap(
                level,
                slime
        );

        if (food != null && canScavenge(slime, gameTime)) {
            moveToOrganicScrap(
                    slime,
                    food,
                    gameTime
            );
        }
    }

    private static LivingEntity findSharedSlimeTarget(
            ServerLevel level,
            PathfinderMob slime
    ) {
        List<PathfinderMob> sources = RetoldAiScanCache.nearby(
                level,
                slime,
                PathfinderMob.class,
                SLIME_SWARM_RADIUS_BLOCKS,
                level.getGameTime(),
                SWARM_SCAN_CACHE_TICKS
        );

        LivingEntity bestTarget = null;
        double bestScore = Double.MAX_VALUE;

        for (PathfinderMob source : sources) {
            if (!isValidSlimeSource(slime, source)) {
                continue;
            }

            LivingEntity target = source.getTarget();

            if (!isValidSlimeTarget(slime, target)) {
                continue;
            }

            double score = slime.distanceToSqr(source);

            if (RetoldAiSightCache.canSee(source, target, level.getGameTime())) {
                score -= 14.0D;
            }

            if (score < bestScore) {
                bestScore = score;
                bestTarget = target;
            }
        }

        return bestTarget;
    }

    private static void spreadSlimeTarget(
            ServerLevel level,
            PathfinderMob source,
            LivingEntity target,
            long gameTime
    ) {
        for (PathfinderMob recruit : RetoldAiScanCache.nearby(
                level,
                source,
                PathfinderMob.class,
                SLIME_SWARM_RADIUS_BLOCKS,
                gameTime,
                SWARM_SCAN_CACHE_TICKS
        )) {
            if (!isValidSlimeRecruit(source, recruit)) {
                continue;
            }

            joinSlimeAttack(
                    recruit,
                    target,
                    gameTime
            );
        }
    }

    private static boolean isValidSlimeSource(
            PathfinderMob slime,
            PathfinderMob source
    ) {
        if (source == null || source == slime) {
            return false;
        }

        if (!isSlimeScavenger(source)) {
            return false;
        }

        if (!RetoldBehaviorCoordinator.isAliveInSameLevel(slime, source)) {
            return false;
        }

        if (slime.distanceToSqr(source) > SLIME_SWARM_RADIUS_SQUARED) {
            return false;
        }

        return isValidSlimeTarget(
                slime,
                source.getTarget()
        );
    }

    private static boolean isValidSlimeRecruit(
            PathfinderMob source,
            PathfinderMob recruit
    ) {
        if (recruit == null || recruit == source) {
            return false;
        }

        if (!isSlimeScavenger(recruit)) {
            return false;
        }

        if (!RetoldBehaviorCoordinator.isAliveInSameLevel(source, recruit)) {
            return false;
        }

        if (source.distanceToSqr(recruit) > SLIME_SWARM_RADIUS_SQUARED) {
            return false;
        }

        return canJoinSlimeSwarm(recruit);
    }

    private static boolean canJoinSlimeSwarm(PathfinderMob slime) {
        if (RetoldBehaviorCoordinator.hasLiveTarget(slime)) {
            return false;
        }

        return RetoldBehaviorCombat.canUseAttackControl(
                slime,
                RetoldAiControlOwner.SWARM
        );
    }

    private static boolean isValidSlimeTarget(
            PathfinderMob slime,
            LivingEntity target
    ) {
        return RetoldBehaviorCombat.isValidEnemyTarget(
                slime,
                target,
                Double.MAX_VALUE,
                false
        );
    }

    private static void joinSlimeAttack(
            PathfinderMob slime,
            LivingEntity target,
            long gameTime
    ) {
        if (!RetoldBehaviorCombat.claimAttackControl(
                slime,
                RetoldAiControlOwner.SWARM,
                SLIME_SWARM_PRIORITY,
                "slime_swarm",
                gameTime,
                SLIME_SWARM_CONTROL_TICKS
        )) {
            return;
        }

        if (!RetoldBehaviorCombat.applyAttackTargetOrClearOwner(
                slime,
                target,
                RetoldTargetSource.FACTION_ASSIST,
                RetoldAiControlOwner.SWARM
        )) {
            return;
        }

        RetoldBehaviorMovement.throttledMoveTo(
                slime,
                target,
                SLIME_SWARM_SPEED,
                gameTime,
                SWARM_PATH_INTERVAL_TICKS,
                2.0D * 2.0D
        );
    }

    private static ItemEntity findBestOrganicScrap(
            ServerLevel level,
            PathfinderMob slime
    ) {
        List<ItemEntity> items = RetoldAiScanCache.nearby(
                level,
                slime,
                ItemEntity.class,
                SCAVENGE_RADIUS_BLOCKS,
                level.getGameTime(),
                SWARM_SCAN_CACHE_TICKS
        );

        ItemEntity best = null;
        double bestScore = Double.MAX_VALUE;

        for (ItemEntity item : items) {
            if (!isValidOrganicScrap(slime, item)) {
                continue;
            }

            double distanceSquared = slime.distanceToSqr(item);

            if (distanceSquared > SCAVENGE_RADIUS_SQUARED) {
                continue;
            }

            double score = distanceSquared;

            if (RetoldAiSightCache.canSee(slime, item, level.getGameTime())) {
                score -= 10.0D;
            }

            if (score < bestScore) {
                bestScore = score;
                best = item;
            }
        }

        return best;
    }

    private static boolean isValidOrganicScrap(
            PathfinderMob slime,
            ItemEntity item
    ) {
        if (item == null || !item.isAlive() || item.isRemoved()) {
            return false;
        }

        if (slime.distanceToSqr(item) > SCAVENGE_RADIUS_SQUARED) {
            return false;
        }

        if (
                !RetoldAiSightCache.canSee(slime, item, slime.level().getGameTime())
                        && slime.distanceToSqr(item) > 49.0D
        ) {
            return false;
        }

        return RetoldMobRules.canEatDroppedItem(
                slime,
                item.getItem()
        );
    }

    private static boolean canScavenge(
            PathfinderMob slime,
            long gameTime
    ) {
        if (RetoldBehaviorCoordinator.hasLiveTarget(slime)) {
            return false;
        }

        RetoldAiControlMode mode = RetoldAiControl.getMode(slime);

        if (
                mode != RetoldAiControlMode.NONE
                        && !RetoldAiControl.isControlledAsBy(
                        slime,
                        RetoldAiControlMode.FEED,
                        RetoldAiControlOwner.SCAVENGER
                )
        ) {
            return false;
        }

        RetoldMobState state = RetoldMobStates.getOrCreate(
                slime,
                gameTime
        );

        return RetoldMobRules.hasEatDrive(
                slime,
                state
        );
    }

    private static void moveToOrganicScrap(
            PathfinderMob slime,
            ItemEntity food,
            long gameTime
    ) {
        if (!RetoldAiControl.tryClaim(
                slime,
                RetoldAiControlMode.FEED,
                RetoldAiControlOwner.SCAVENGER,
                SCAVENGE_PRIORITY,
                "slime_scavenge",
                gameTime,
                SCAVENGE_CONTROL_TICKS
        )) {
            return;
        }

        RetoldBehaviorMovement.throttledMoveTo(
                slime,
                food,
                SCAVENGE_SPEED,
                gameTime,
                SWARM_PATH_INTERVAL_TICKS,
                1.5D * 1.5D
        );
    }

    private static void handleSmallArthropodSwarm(
            ServerLevel level,
            PathfinderMob arthropod,
            long gameTime
    ) {
        LivingEntity target = arthropod.getTarget();

        if (isValidSmallArthropodTarget(arthropod, target)) {
            spreadSmallArthropodTarget(
                    level,
                    arthropod,
                    target,
                    gameTime
            );
            return;
        }

        LivingEntity sharedTarget = findSharedSmallArthropodTarget(
                level,
                arthropod
        );

        if (sharedTarget != null && canJoinSmallArthropodSwarm(arthropod)) {
            joinSmallArthropodAttack(
                    arthropod,
                    sharedTarget,
                    gameTime
            );
        }
    }

    private static LivingEntity findSharedSmallArthropodTarget(
            ServerLevel level,
            PathfinderMob arthropod
    ) {
        List<PathfinderMob> sources = RetoldAiScanCache.nearby(
                level,
                arthropod,
                PathfinderMob.class,
                SMALL_ARTHROPOD_SWARM_RADIUS_BLOCKS,
                level.getGameTime(),
                SWARM_SCAN_CACHE_TICKS
        );

        LivingEntity bestTarget = null;
        double bestScore = Double.MAX_VALUE;

        for (PathfinderMob source : sources) {
            if (!isValidSmallArthropodSource(arthropod, source)) {
                continue;
            }

            LivingEntity target = source.getTarget();

            if (!isValidSmallArthropodTarget(arthropod, target)) {
                continue;
            }

            double score = arthropod.distanceToSqr(source);

            if (RetoldAiSightCache.canSee(source, target, level.getGameTime())) {
                score -= 12.0D;
            }

            if (score < bestScore) {
                bestScore = score;
                bestTarget = target;
            }
        }

        return bestTarget;
    }

    private static void spreadSmallArthropodTarget(
            ServerLevel level,
            PathfinderMob source,
            LivingEntity target,
            long gameTime
    ) {
        for (PathfinderMob recruit : RetoldAiScanCache.nearby(
                level,
                source,
                PathfinderMob.class,
                SMALL_ARTHROPOD_SWARM_RADIUS_BLOCKS,
                gameTime,
                SWARM_SCAN_CACHE_TICKS
        )) {
            if (!isValidSmallArthropodRecruit(source, recruit)) {
                continue;
            }

            joinSmallArthropodAttack(
                    recruit,
                    target,
                    gameTime
            );
        }
    }

    private static boolean isValidSmallArthropodSource(
            PathfinderMob arthropod,
            PathfinderMob source
    ) {
        if (source == null || source == arthropod) {
            return false;
        }

        if (!isSmallArthropodSwarm(source)) {
            return false;
        }

        if (!RetoldBehaviorCoordinator.isAliveInSameLevel(arthropod, source)) {
            return false;
        }

        if (arthropod.distanceToSqr(source) > SMALL_ARTHROPOD_SWARM_RADIUS_SQUARED) {
            return false;
        }

        return isValidSmallArthropodTarget(
                arthropod,
                source.getTarget()
        );
    }

    private static boolean isValidSmallArthropodRecruit(
            PathfinderMob source,
            PathfinderMob recruit
    ) {
        if (recruit == null || recruit == source) {
            return false;
        }

        if (!isSmallArthropodSwarm(recruit)) {
            return false;
        }

        if (!RetoldBehaviorCoordinator.isAliveInSameLevel(source, recruit)) {
            return false;
        }

        if (source.distanceToSqr(recruit) > SMALL_ARTHROPOD_SWARM_RADIUS_SQUARED) {
            return false;
        }

        return canJoinSmallArthropodSwarm(recruit);
    }

    private static boolean canJoinSmallArthropodSwarm(PathfinderMob arthropod) {
        if (RetoldBehaviorCoordinator.hasLiveTarget(arthropod)) {
            return false;
        }

        return RetoldBehaviorCombat.canUseAttackControl(
                arthropod,
                RetoldAiControlOwner.SWARM
        );
    }

    private static boolean isValidSmallArthropodTarget(
            PathfinderMob arthropod,
            LivingEntity target
    ) {
        return RetoldBehaviorCombat.isValidEnemyTarget(
                arthropod,
                target,
                Double.MAX_VALUE,
                false
        );
    }

    private static void joinSmallArthropodAttack(
            PathfinderMob arthropod,
            LivingEntity target,
            long gameTime
    ) {
        if (!RetoldBehaviorCombat.claimAttackControl(
                arthropod,
                RetoldAiControlOwner.SWARM,
                SMALL_ARTHROPOD_SWARM_PRIORITY,
                "small_arthropod_swarm",
                gameTime,
                SMALL_ARTHROPOD_SWARM_CONTROL_TICKS
        )) {
            return;
        }

        if (!RetoldBehaviorCombat.applyAttackTargetOrClearOwner(
                arthropod,
                target,
                RetoldTargetSource.FACTION_ASSIST,
                RetoldAiControlOwner.SWARM
        )) {
            return;
        }

        RetoldBehaviorMovement.throttledMoveTo(
                arthropod,
                target,
                SMALL_ARTHROPOD_SWARM_SPEED,
                gameTime,
                SWARM_PATH_INTERVAL_TICKS,
                2.0D * 2.0D
        );
    }
}
