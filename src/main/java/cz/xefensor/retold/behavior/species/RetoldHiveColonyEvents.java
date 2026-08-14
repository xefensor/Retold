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
import cz.xefensor.retold.behavior.performance.RetoldBlockTargetSearch;
import cz.xefensor.retold.behavior.food.RetoldFeedingAnimations;
import cz.xefensor.retold.behavior.food.RetoldFeedingPose;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;
import cz.xefensor.retold.behavior.profiles.RetoldMobState;
import cz.xefensor.retold.behavior.profiles.RetoldMobStates;
import cz.xefensor.retold.combat.RetoldCombatTargets;
import cz.xefensor.retold.combat.RetoldFactionTargetMemory;
import cz.xefensor.retold.combat.RetoldTargetSource;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class RetoldHiveColonyEvents {
    private static final Map<PathfinderMob, FlowerMemory> FLOWER_MEMORIES = new WeakHashMap<>();

    private static final int THINK_INTERVAL_TICKS = 18;
    private static final int HIVE_SCAN_CACHE_TICKS = 8;
    private static final int FLOWER_BLOCK_SEARCH_CACHE_TICKS = 30;
    private static final int HIVE_PATH_INTERVAL_TICKS = 8;
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
    private static final double DEFENSE_KEEP_RADIUS_BLOCKS = 36.0D;
    private static final double DEFENSE_KEEP_RADIUS_SQUARED =
            DEFENSE_KEEP_RADIUS_BLOCKS * DEFENSE_KEEP_RADIUS_BLOCKS;

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

        tick(level, bee, level.getGameTime());
    }

    public static void onLivingDamage(LivingDamageEvent.Post event) {
        if (event.getHealthDamage() <= 0.0F
                || !(event.getEntity() instanceof PathfinderMob victim)
                || !isHiveBee(victim)
                || !(victim.level() instanceof ServerLevel level)
                || !(event.getSource().getEntity() instanceof LivingEntity attacker)) {
            return;
        }

        beginCollectiveDefense(
                level,
                victim.blockPosition(),
                victim,
                attacker,
                level.getGameTime()
        );
    }

    public static void onHiveHarvest(UseItemOnBlockEvent event) {
        if (event.isCanceled()
                || event.getUsePhase() != UseItemOnBlockEvent.UsePhase.BLOCK
                || !(event.getLevel() instanceof ServerLevel level)
                || event.getPlayer() == null) {
            return;
        }

        BlockPos hivePos = event.getPos();
        BlockState hiveState = level.getBlockState(hivePos);

        if (!isUnsmokedHoneyHarvest(
                level,
                hivePos,
                hiveState,
                event.getItemStack()
        )) {
            return;
        }

        beginCollectiveDefense(
                level,
                hivePos,
                null,
                event.getPlayer(),
                level.getGameTime()
        );
    }

    public static void onHiveBreak(BreakBlockEvent event) {
        if (event.isCanceled()
                || !(event.getLevel() instanceof ServerLevel level)
                || !event.getState().is(BlockTags.BEEHIVES)) {
            return;
        }

        beginCollectiveDefense(
                level,
                event.getPos(),
                null,
                event.getPlayer(),
                level.getGameTime()
        );
    }

    public static void tick(
            ServerLevel level,
            PathfinderMob bee,
            long gameTime
    ) {
        if (level == null
                || bee == null
                || bee.level() != level
                || !isHiveBee(bee)) {
            return;
        }

        LivingEntity target = bee.getTarget();

        if ((isHiveDefenseTargetOwned(bee, target) || ownsHiveAttackControl(bee))
                && !isValidDefenseTarget(bee, target)) {
            stopDefense(bee, target);
            return;
        }

        if (!shouldThink(bee, gameTime)) {
            return;
        }

        if (isOwnedHiveDefense(bee, target)) {
            if (!continueDefense(bee, target, gameTime)) {
                stopDefense(bee, target);
                return;
            }

            shareDefenseTarget(level, bee, target, gameTime);
            return;
        }

        if (RetoldAiControl.isControlledAsBy(
                bee,
                RetoldAiControlMode.ATTACK,
                RetoldAiControlOwner.HIVE_COLONY
        )) {
            stopDefense(bee, target);
            target = bee.getTarget();
        }

        LivingEntity sharedTarget = findSharedDefenseTarget(
                level,
                bee
        );

        if (sharedTarget != null && canDefend(bee)) {
            defendAgainst(
                    bee,
                    sharedTarget,
                    RetoldTargetSource.FACTION_ASSIST,
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

    private static boolean isUnsmokedHoneyHarvest(
            ServerLevel level,
            BlockPos hivePos,
            BlockState hiveState,
            ItemStack stack
    ) {
        if (!hiveState.is(BlockTags.BEEHIVES)
                || !hiveState.hasProperty(BeehiveBlock.HONEY_LEVEL)
                || hiveState.getValue(BeehiveBlock.HONEY_LEVEL) < BeehiveBlock.MAX_HONEY_LEVELS
                || CampfireBlock.isSmokeyPos(level, hivePos)) {
            return false;
        }

        return stack.is(Items.GLASS_BOTTLE)
                || stack.canPerformAction(ItemAbilities.SHEARS_HARVEST);
    }

    static void beginCollectiveDefense(
            ServerLevel level,
            BlockPos incidentPos,
            PathfinderMob victim,
            LivingEntity attacker,
            long gameTime
    ) {
        if (level == null
                || incidentPos == null
                || attacker == null) {
            return;
        }

        if (victim != null) {
            if (victim.level() != level
                    || !isHiveBee(victim)
                    || !isValidDefenseTarget(victim, attacker)) {
                return;
            }

            defendAgainst(
                    victim,
                    attacker,
                    RetoldTargetSource.RETALIATION,
                    gameTime
            );
        }

        for (PathfinderMob recruit : RetoldAiScanCache.nearbyAt(
                level,
                incidentPos,
                PathfinderMob.class,
                DEFENSE_SHARE_RADIUS_BLOCKS,
                gameTime,
                HIVE_SCAN_CACHE_TICKS
        )) {
            if (recruit == victim
                    || !isHiveBee(recruit)
                    || !RetoldBehaviorCoordinator.isUsableEntity(recruit)
                    || !isValidDefenseTarget(recruit, attacker)
                    || !canDefend(recruit)) {
                continue;
            }

            defendAgainst(
                    recruit,
                    attacker,
                    RetoldTargetSource.FACTION_ASSIST,
                    gameTime
            );
        }
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
        List<PathfinderMob> sources = RetoldAiScanCache.nearby(
                level,
                bee,
                PathfinderMob.class,
                DEFENSE_SHARE_RADIUS_BLOCKS,
                level.getGameTime(),
                HIVE_SCAN_CACHE_TICKS
        );

        LivingEntity bestTarget = null;
        double bestScore = Double.MAX_VALUE;

        for (PathfinderMob source : sources) {
            if (!isValidDefenseSource(bee, source)) {
                continue;
            }

            LivingEntity target = source.getTarget();

            if (!isValidDefenseTarget(bee, target)) {
                continue;
            }

            double score = bee.distanceToSqr(source);

            if (RetoldAiSightCache.canSee(source, target, level.getGameTime())) {
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

        LivingEntity target = source.getTarget();

        return isValidDefenseTarget(bee, target)
                && isOwnedHiveDefense(source, target);
    }

    private static void shareDefenseTarget(
            ServerLevel level,
            PathfinderMob source,
            LivingEntity target,
            long gameTime
    ) {
        for (PathfinderMob recruit : RetoldAiScanCache.nearby(
                level,
                source,
                PathfinderMob.class,
                DEFENSE_SHARE_RADIUS_BLOCKS,
                gameTime,
                HIVE_SCAN_CACHE_TICKS
        )) {
            if (!isValidDefenseRecruit(source, recruit)) {
                continue;
            }

            defendAgainst(
                    recruit,
                    target,
                    RetoldTargetSource.FACTION_ASSIST,
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

    private static boolean defendAgainst(
            PathfinderMob bee,
            LivingEntity target,
            RetoldTargetSource source,
            long gameTime
    ) {
        if ((source != RetoldTargetSource.RETALIATION
                && source != RetoldTargetSource.FACTION_ASSIST)
                || !isValidDefenseTarget(bee, target)) {
            return false;
        }

        int priority = source == RetoldTargetSource.RETALIATION
                ? RetoldAiPriorities.ATTACK
                : DEFENSE_PRIORITY;

        if (!RetoldBehaviorCombat.claimAttackControl(
                bee,
                RetoldAiControlOwner.HIVE_COLONY,
                priority,
                source == RetoldTargetSource.RETALIATION
                        ? "hive_retaliation"
                        : "hive_collective_defense",
                gameTime,
                DEFENSE_CONTROL_TICKS
        )) {
            return false;
        }

        if (!RetoldBehaviorCombat.applyAttackTargetOrClearOwner(
                bee,
                target,
                source,
                RetoldAiControlOwner.HIVE_COLONY
        )) {
            return false;
        }

        RetoldMobState state = RetoldMobStates.getOrCreate(bee, gameTime);
        state.markDanger(gameTime);
        state.addStress(source == RetoldTargetSource.RETALIATION ? 5 : 3);
        return true;
    }

    private static boolean isOwnedHiveDefense(
            PathfinderMob bee,
            LivingEntity target
    ) {
        return target != null
                && ownsHiveAttackControl(bee)
                && isHiveDefenseTargetOwned(bee, target);
    }

    private static boolean ownsHiveAttackControl(PathfinderMob bee) {
        return RetoldAiControl.isControlledAsBy(
                bee,
                RetoldAiControlMode.ATTACK,
                RetoldAiControlOwner.HIVE_COLONY
        );
    }

    private static boolean isHiveDefenseTargetOwned(
            PathfinderMob bee,
            LivingEntity target
    ) {
        return target != null
                && RetoldFactionTargetMemory.isOwnedByAny(
                bee,
                target,
                RetoldTargetSource.RETALIATION,
                RetoldTargetSource.FACTION_ASSIST
        );
    }

    private static boolean continueDefense(
            PathfinderMob bee,
            LivingEntity target,
            long gameTime
    ) {
        if (!isValidDefenseTarget(bee, target)
                || bee.distanceToSqr(target) > DEFENSE_KEEP_RADIUS_SQUARED) {
            return false;
        }

        RetoldAiControl.refreshIfOwnedBy(
                bee,
                RetoldAiControlMode.ATTACK,
                RetoldAiControlOwner.HIVE_COLONY,
                gameTime,
                DEFENSE_CONTROL_TICKS
        );
        RetoldBehaviorMovement.throttledMoveTo(
                bee,
                target,
                DEFENSE_SPEED,
                gameTime,
                HIVE_PATH_INTERVAL_TICKS,
                2.0D * 2.0D
        );
        return true;
    }

    private static void stopDefense(
            PathfinderMob bee,
            LivingEntity target
    ) {
        boolean ownsHiveAttack = ownsHiveAttackControl(bee);
        boolean ownsTarget = target != null && RetoldFactionTargetMemory.isOwnedByAny(
                bee,
                target,
                RetoldTargetSource.RETALIATION,
                RetoldTargetSource.FACTION_ASSIST
        );

        if (target != null && (ownsTarget || ownsHiveAttack)) {
            RetoldCombatTargets.clearTargetReferencesAndAggression(
                    bee,
                    target,
                    true
            );
        }

        RetoldAiControl.clearIfOwnedBy(
                bee,
                RetoldAiControlOwner.HIVE_COLONY
        );
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
        return RetoldBlockTargetSearch.findFlower(
                level,
                bee,
                FLOWER_SEARCH_HORIZONTAL_RADIUS,
                FLOWER_SEARCH_VERTICAL_RADIUS,
                FLOWER_SEARCH_RADIUS_SQUARED,
                level.getGameTime(),
                FLOWER_BLOCK_SEARCH_CACHE_TICKS
        );
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
        if (!RetoldBehaviorCoordinator.canCompleteMeal(bee)) {
            return;
        }

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
        RetoldFeedingPose.begin(
                bee,
                Vec3.atCenterOf(flowerPos),
                gameTime
        );
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

        RetoldBehaviorMovement.throttledMoveTo(
                bee,
                flowerPos.getX() + 0.5D,
                flowerPos.getY() + 0.5D,
                flowerPos.getZ() + 0.5D,
                FLOWER_SPEED,
                gameTime,
                HIVE_PATH_INTERVAL_TICKS,
                1.5D * 1.5D
        );
    }

    private record FlowerMemory(
            BlockPos pos,
            long foundAt
    ) {
    }
}
