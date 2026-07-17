package cz.xefensor.retold.behavior.species;

import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.control.RetoldAiControlMode;
import cz.xefensor.retold.behavior.control.RetoldAiControlOwner;
import cz.xefensor.retold.behavior.control.RetoldAiPriorities;
import cz.xefensor.retold.behavior.performance.RetoldAiScanCache;
import cz.xefensor.retold.behavior.performance.RetoldAiSightCache;
import cz.xefensor.retold.behavior.home.RetoldAnimalDailyRhythm;
import cz.xefensor.retold.behavior.home.RetoldAnimalHomeMemory;
import cz.xefensor.retold.behavior.home.RetoldAnimalHomes;
import cz.xefensor.retold.behavior.home.RetoldAnimalSocialGroups;
import cz.xefensor.retold.behavior.core.RetoldBehaviorCoordinator;
import cz.xefensor.retold.behavior.core.RetoldBehaviorMovement;
import cz.xefensor.retold.behavior.core.RetoldBehaviorTiming;
import cz.xefensor.retold.behavior.performance.RetoldBlockTargetSearch;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;
import cz.xefensor.retold.behavior.profiles.RetoldMobState;
import cz.xefensor.retold.behavior.profiles.RetoldMobStates;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.List;

public final class RetoldArmadilloDefenseEvents {
    private static final int THINK_INTERVAL_TICKS = 8;
    private static final int ARMADILLO_SCAN_CACHE_TICKS = 6;
    private static final int SCRUB_BLOCK_SEARCH_CACHE_TICKS = 24;
    private static final int SHELTER_CONTROL_TICKS = 20 * 4;
    private static final int RETREAT_CONTROL_TICKS = 20 * 5;
    private static final int RETURN_CONTROL_TICKS = 20 * 5;

    private static final int SHELTER_PRIORITY = RetoldAiPriorities.above(RetoldAiPriorities.SHELTER, 2);
    private static final int RETREAT_PRIORITY = RetoldAiPriorities.below(RetoldAiPriorities.FLEE, 3);
    private static final int RETURN_PRIORITY = RetoldAiPriorities.above(RetoldAiPriorities.REGROUP, 4);

    private static final int SCRUB_SEARCH_HORIZONTAL_RADIUS = 14;
    private static final int SCRUB_SEARCH_VERTICAL_RADIUS = 4;
    private static final double RANGE_MEMBER_SEARCH_RADIUS_BLOCKS = 12.0D;

    private static final int RECENT_DANGER_RETURN_TICKS = 20 * 35;

    private static final double THREAT_SCAN_RADIUS_BLOCKS = 13.0D;
    private static final double THREAT_SCAN_RADIUS_SQUARED =
            THREAT_SCAN_RADIUS_BLOCKS * THREAT_SCAN_RADIUS_BLOCKS;

    private static final double CLOSE_THREAT_BLOCKS = 5.0D;
    private static final double CLOSE_THREAT_SQUARED =
            CLOSE_THREAT_BLOCKS * CLOSE_THREAT_BLOCKS;

    private static final double FAR_FROM_RANGE_BLOCKS = 22.0D;
    private static final double FAR_FROM_RANGE_SQUARED =
            FAR_FROM_RANGE_BLOCKS * FAR_FROM_RANGE_BLOCKS;

    private static final double RANGE_REACHED_BLOCKS = 5.0D;
    private static final double RANGE_REACHED_SQUARED =
            RANGE_REACHED_BLOCKS * RANGE_REACHED_BLOCKS;

    private static final double ARMADILLO_RETREAT_SPEED = 1.08D;
    private static final double ARMADILLO_RETURN_SPEED = 0.72D;

    private RetoldArmadilloDefenseEvents() {
    }

    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof PathfinderMob armadillo)) {
            return;
        }

        if (!(armadillo.level() instanceof ServerLevel level)) {
            return;
        }

        if (!RetoldMobRules.isArmadilloDefensive(armadillo)) {
            return;
        }

        long gameTime = level.getGameTime();

        if (!RetoldBehaviorTiming.shouldThink(
                armadillo,
                gameTime,
                THINK_INTERVAL_TICKS
        )) {
            return;
        }

        handleArmadillo(
                level,
                armadillo,
                gameTime
        );
    }

    private static void handleArmadillo(
            ServerLevel level,
            PathfinderMob armadillo,
            long gameTime
    ) {
        RetoldAnimalHomeMemory range = getOrCreateRange(
                level,
                armadillo,
                gameTime
        );

        LivingEntity threat = findThreat(
                level,
                armadillo
        );

        if (threat != null) {
            rememberDanger(
                    armadillo,
                    gameTime
            );

            if (armadillo.distanceToSqr(threat) <= CLOSE_THREAT_SQUARED) {
                shelterInPlace(
                        armadillo,
                        gameTime
                );
                return;
            }

            if (range != null) {
                retreatToRange(
                        armadillo,
                        range.pos(),
                        gameTime
                );
                return;
            }

            shelterInPlace(
                    armadillo,
                    gameTime
            );
            return;
        }

        if (RetoldAiControl.isControlledBy(armadillo, RetoldAiControlOwner.ARMADILLO_DEFENSE)) {
            stopDefenseControl(armadillo);
        }

        if (range == null) {
            return;
        }

        if (shouldReturnToRange(level, armadillo, range, gameTime)) {
            returnToRange(
                    armadillo,
                    range.pos(),
                    gameTime
            );
            return;
        }

        if (
                RetoldAiControl.isControlledAsBy(
                        armadillo,
                        RetoldAiControlMode.REGROUP,
                        RetoldAiControlOwner.ARMADILLO_DEFENSE
                )
                        && armadillo.blockPosition().distSqr(range.pos()) <= RANGE_REACHED_SQUARED
        ) {
            stopDefenseControl(armadillo);
            RetoldAnimalHomes.markUsed(
                    armadillo,
                    gameTime
            );
        }
    }

    private static RetoldAnimalHomeMemory getOrCreateRange(
            ServerLevel level,
            PathfinderMob armadillo,
            long gameTime
    ) {
        RetoldAnimalHomeMemory existing = RetoldAnimalHomes.get(armadillo);

        if (RetoldAnimalHomes.isValidFor(level, armadillo, existing) && isScrubRange(level, existing.pos())) {
            RetoldAnimalHomes.markUsed(
                    armadillo,
                    gameTime
            );
            return existing;
        }

        BlockPos rangePos = findNearestScrubRange(
                level,
                armadillo
        );

        if (rangePos == null) {
            return null;
        }

        return RetoldAnimalHomes.getOrCreatePackHome(
                level,
                armadillo,
                findNearbyRangeMembers(
                        level,
                        armadillo
                ),
                rangePos,
                gameTime
        );
    }

    private static List<PathfinderMob> findNearbyRangeMembers(
            ServerLevel level,
            PathfinderMob armadillo
    ) {
        return RetoldAiScanCache.nearby(
                level,
                armadillo,
                PathfinderMob.class,
                RANGE_MEMBER_SEARCH_RADIUS_BLOCKS,
                level.getGameTime(),
                ARMADILLO_SCAN_CACHE_TICKS
        ).stream()
                .filter(
                candidate -> candidate != armadillo
                        && RetoldAnimalSocialGroups.canShareHomeOrRange(
                        armadillo,
                        candidate
                )
        ).toList();
    }

    private static LivingEntity findThreat(
            ServerLevel level,
            PathfinderMob armadillo
    ) {
        LivingEntity attacker = armadillo.getLastHurtByMob();

        if (isValidThreat(armadillo, attacker)) {
            return attacker;
        }

        LivingEntity bestThreat = null;
        double bestScore = Double.MAX_VALUE;

        for (LivingEntity candidate : RetoldAiScanCache.nearby(
                level,
                armadillo,
                LivingEntity.class,
                THREAT_SCAN_RADIUS_BLOCKS,
                level.getGameTime(),
                ARMADILLO_SCAN_CACHE_TICKS
        )) {
            if (!isValidThreat(armadillo, candidate)) {
                continue;
            }

            double distanceSquared = armadillo.distanceToSqr(candidate);

            if (distanceSquared > THREAT_SCAN_RADIUS_SQUARED) {
                continue;
            }

            double score = distanceSquared;

            if (candidate instanceof PathfinderMob mob && mob.getTarget() == armadillo) {
                score -= 80.0D;
            }

            if (isScaryMob(candidate)) {
                score -= 18.0D;
            }

            if (candidate instanceof Player player && player.isSprinting()) {
                score -= 14.0D;
            }

            if (score < bestScore) {
                bestScore = score;
                bestThreat = candidate;
            }
        }

        return bestThreat;
    }

    private static boolean isValidThreat(
            PathfinderMob armadillo,
            LivingEntity candidate
    ) {
        if (armadillo == null || candidate == null || candidate == armadillo) {
            return false;
        }

        if (!RetoldBehaviorCoordinator.isAliveInSameLevel(armadillo, candidate)) {
            return false;
        }

        if (RetoldBehaviorCoordinator.isInvalidPlayerTarget(candidate)) {
            return false;
        }

        if (candidate instanceof Player player) {
            return player.isSprinting()
                    && armadillo.distanceToSqr(player) <= THREAT_SCAN_RADIUS_SQUARED
                    && RetoldAiSightCache.canSee(armadillo, player, armadillo.level().getGameTime());
        }

        if (candidate == armadillo.getLastHurtByMob()) {
            return true;
        }

        if (candidate instanceof PathfinderMob mob && mob.getTarget() == armadillo) {
            return true;
        }

        return isScaryMob(candidate)
                && armadillo.distanceToSqr(candidate) <= THREAT_SCAN_RADIUS_SQUARED
                && (
                RetoldAiSightCache.canSee(armadillo, candidate, armadillo.level().getGameTime())
                        || armadillo.distanceToSqr(candidate) <= CLOSE_THREAT_SQUARED
        );
    }

    private static void rememberDanger(
            PathfinderMob armadillo,
            long gameTime
    ) {
        RetoldMobState state = RetoldMobStates.getOrCreate(
                armadillo,
                gameTime
        );

        state.markDanger(gameTime);
        state.addStress(3);
        state.addConfidence(-2);
    }

    private static void shelterInPlace(
            PathfinderMob armadillo,
            long gameTime
    ) {
        if (!RetoldAiControl.tryClaim(
                armadillo,
                RetoldAiControlMode.SHELTER,
                RetoldAiControlOwner.ARMADILLO_DEFENSE,
                SHELTER_PRIORITY,
                "armadillo_shelter",
                gameTime,
                SHELTER_CONTROL_TICKS
        )) {
            return;
        }

        armadillo.setSprinting(false);
        armadillo.getNavigation().stop();
    }

    private static void retreatToRange(
            PathfinderMob armadillo,
            BlockPos range,
            long gameTime
    ) {
        RetoldBehaviorMovement.claimAndMoveToBlock(
                armadillo,
                range,
                RetoldAiControlMode.FLEE,
                RetoldAiControlOwner.ARMADILLO_DEFENSE,
                RETREAT_PRIORITY,
                "armadillo_retreat",
                gameTime,
                RETREAT_CONTROL_TICKS,
                ARMADILLO_RETREAT_SPEED,
                true
        );
    }

    private static boolean shouldReturnToRange(
            ServerLevel level,
            PathfinderMob armadillo,
            RetoldAnimalHomeMemory range,
            long gameTime
    ) {
        if (range == null) {
            return false;
        }

        if (armadillo.blockPosition().distSqr(range.pos()) <= RANGE_REACHED_SQUARED) {
            return false;
        }

        RetoldMobState state = RetoldMobStates.getOrCreate(
                armadillo,
                gameTime
        );

        boolean recentDanger = state.lastDangerAt() > 0L
                && gameTime - state.lastDangerAt() <= RECENT_DANGER_RETURN_TICKS;

        boolean farFromRange = armadillo.blockPosition().distSqr(range.pos()) >= FAR_FROM_RANGE_SQUARED;
        boolean restTime = RetoldAnimalDailyRhythm.isNight(level)
                || level.isRainingAt(armadillo.blockPosition());

        return recentDanger || (farFromRange && restTime);
    }

    private static void returnToRange(
            PathfinderMob armadillo,
            BlockPos range,
            long gameTime
    ) {
        RetoldBehaviorMovement.claimAndMoveToBlock(
                armadillo,
                range,
                RetoldAiControlMode.REGROUP,
                RetoldAiControlOwner.ARMADILLO_DEFENSE,
                RETURN_PRIORITY,
                "armadillo_range_return",
                gameTime,
                RETURN_CONTROL_TICKS,
                ARMADILLO_RETURN_SPEED,
                false
        );
    }

    private static void stopDefenseControl(PathfinderMob armadillo) {
        armadillo.setSprinting(false);
        armadillo.getNavigation().stop();

        RetoldAiControl.clearIfOwnedBy(
                armadillo,
                RetoldAiControlOwner.ARMADILLO_DEFENSE
        );
    }

    private static BlockPos findNearestScrubRange(
            ServerLevel level,
            PathfinderMob armadillo
    ) {
        return RetoldBlockTargetSearch.findScrubRange(
                level,
                armadillo,
                SCRUB_SEARCH_HORIZONTAL_RADIUS,
                SCRUB_SEARCH_VERTICAL_RADIUS,
                level.getGameTime(),
                SCRUB_BLOCK_SEARCH_CACHE_TICKS
        );
    }

    private static boolean isScrubRange(
            ServerLevel level,
            BlockPos pos
    ) {
        if (level == null || pos == null) {
            return false;
        }

        if (!level.getBlockState(pos.above()).isAir()) {
            return false;
        }

        BlockState state = level.getBlockState(pos);

        return state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.DIRT)
                || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.ROOTED_DIRT)
                || state.is(Blocks.PODZOL)
                || state.is(Blocks.SAND)
                || state.is(Blocks.RED_SAND)
                || state.is(Blocks.TERRACOTTA);
    }

    private static boolean isScaryMob(LivingEntity entity) {
        String path = RetoldMobRules.getEntityTypePath(entity.getType());

        return path.equals("wolf")
                || path.equals("spider")
                || path.equals("cave_spider")
                || path.equals("zombie")
                || path.equals("zombie_villager")
                || path.equals("husk")
                || path.equals("drowned")
                || path.equals("zoglin");
    }
}
