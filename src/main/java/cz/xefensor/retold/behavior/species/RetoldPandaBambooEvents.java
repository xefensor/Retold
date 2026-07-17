package cz.xefensor.retold.behavior.species;

import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.control.RetoldAiControlMode;
import cz.xefensor.retold.behavior.control.RetoldAiControlOwner;
import cz.xefensor.retold.behavior.control.RetoldAiPriorities;
import cz.xefensor.retold.behavior.performance.RetoldAiScanCache;
import cz.xefensor.retold.behavior.home.RetoldAnimalDailyRhythm;
import cz.xefensor.retold.behavior.home.RetoldAnimalHomeMemory;
import cz.xefensor.retold.behavior.home.RetoldAnimalHomes;
import cz.xefensor.retold.behavior.home.RetoldAnimalSocialGroups;
import cz.xefensor.retold.behavior.core.RetoldBehaviorCoordinator;
import cz.xefensor.retold.behavior.core.RetoldBehaviorMovement;
import cz.xefensor.retold.behavior.core.RetoldBehaviorTiming;
import cz.xefensor.retold.behavior.performance.RetoldBlockTargetSearch;
import cz.xefensor.retold.behavior.food.RetoldFeedingAnimations;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;
import cz.xefensor.retold.behavior.profiles.RetoldMobState;
import cz.xefensor.retold.behavior.profiles.RetoldMobStates;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.List;

public final class RetoldPandaBambooEvents {
    private static final int THINK_INTERVAL_TICKS = 20;
    private static final int PANDA_SCAN_CACHE_TICKS = 10;
    private static final int BAMBOO_BLOCK_SEARCH_CACHE_TICKS = 35;
    private static final int FEED_CONTROL_TICKS = 20 * 5;
    private static final int RETURN_CONTROL_TICKS = 20 * 6;

    private static final int BAMBOO_FEED_PRIORITY = RetoldAiPriorities.below(RetoldAiPriorities.FEED, 3);
    private static final int RETURN_PRIORITY = RetoldAiPriorities.above(RetoldAiPriorities.REGROUP, 3);

    private static final int BAMBOO_SEARCH_HORIZONTAL_RADIUS = 12;
    private static final int BAMBOO_SEARCH_VERTICAL_RADIUS = 4;
    private static final double GROVE_MEMBER_SEARCH_RADIUS_BLOCKS = 14.0D;

    private static final int RECENT_DANGER_RETURN_TICKS = 20 * 35;

    private static final double FAR_FROM_GROVE_BLOCKS = 24.0D;
    private static final double FAR_FROM_GROVE_SQUARED =
            FAR_FROM_GROVE_BLOCKS * FAR_FROM_GROVE_BLOCKS;

    private static final double GROVE_REST_DISTANCE_BLOCKS = 7.0D;
    private static final double GROVE_REST_DISTANCE_SQUARED =
            GROVE_REST_DISTANCE_BLOCKS * GROVE_REST_DISTANCE_BLOCKS;

    private static final double BAMBOO_EAT_DISTANCE_BLOCKS = 2.3D;
    private static final double BAMBOO_EAT_DISTANCE_SQUARED =
            BAMBOO_EAT_DISTANCE_BLOCKS * BAMBOO_EAT_DISTANCE_BLOCKS;

    private static final int BAMBOO_RELIEF = 24;

    private static final double PANDA_FEED_SPEED = 0.72D;
    private static final double PANDA_RETURN_SPEED = 0.66D;

    private RetoldPandaBambooEvents() {
    }

    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof PathfinderMob panda)) {
            return;
        }

        if (!(panda.level() instanceof ServerLevel level)) {
            return;
        }

        if (!RetoldMobRules.isPandaBamboo(panda)) {
            return;
        }

        long gameTime = level.getGameTime();

        if (!RetoldBehaviorTiming.shouldThink(
                panda,
                gameTime,
                THINK_INTERVAL_TICKS
        )) {
            return;
        }

        handlePanda(
                level,
                panda,
                gameTime
        );
    }

    private static void handlePanda(
            ServerLevel level,
            PathfinderMob panda,
            long gameTime
    ) {
        RetoldAnimalHomeMemory grove = getOrCreateGrove(
                level,
                panda,
                gameTime
        );

        LivingEntity threat = findThreat(panda);

        if (threat != null) {
            rememberDanger(
                    panda,
                    gameTime
            );

            if (grove != null) {
                returnToGrove(
                        panda,
                        grove.pos(),
                        gameTime,
                        "panda_danger_return"
                );
            }

            return;
        }

        if (RetoldAiControl.isControlledAsBy(
                panda,
                RetoldAiControlMode.FEED,
                RetoldAiControlOwner.PANDA_BAMBOO
        )) {
            continueBambooFeeding(
                    level,
                    panda,
                    gameTime
            );
            return;
        }

        if (shouldSeekBamboo(panda, gameTime)) {
            BlockPos bamboo = findNearestBamboo(
                    level,
                    panda
            );

            if (bamboo != null) {
                moveToBamboo(
                        panda,
                        bamboo,
                        gameTime
                );
                return;
            }
        }

        if (grove == null) {
            return;
        }

        if (shouldReturnToGrove(level, panda, grove, gameTime)) {
            returnToGrove(
                    panda,
                    grove.pos(),
                    gameTime,
                    "panda_grove_return"
            );
            return;
        }

        if (
                RetoldAiControl.isControlledAsBy(
                        panda,
                        RetoldAiControlMode.REGROUP,
                        RetoldAiControlOwner.PANDA_BAMBOO
                )
                        && panda.blockPosition().distSqr(grove.pos()) <= GROVE_REST_DISTANCE_SQUARED
        ) {
            stopControl(panda);
            RetoldAnimalHomes.markUsed(
                    panda,
                    gameTime
            );
        }
    }

    private static RetoldAnimalHomeMemory getOrCreateGrove(
            ServerLevel level,
            PathfinderMob panda,
            long gameTime
    ) {
        RetoldAnimalHomeMemory existing = RetoldAnimalHomes.get(panda);

        if (RetoldAnimalHomes.isValidFor(level, panda, existing)) {
            if (isBambooGrove(level, existing.pos())) {
                RetoldAnimalHomes.markUsed(
                        panda,
                        gameTime
                );
                return existing;
            }

            RetoldAnimalHomes.remove(panda);
        }

        BlockPos bamboo = findNearestBamboo(
                level,
                panda
        );

        if (bamboo == null) {
            return null;
        }

        return RetoldAnimalHomes.getOrCreatePackHome(
                level,
                panda,
                findNearbyGroveMembers(
                        level,
                        panda
                ),
                bamboo,
                gameTime
        );
    }

    private static List<PathfinderMob> findNearbyGroveMembers(
            ServerLevel level,
            PathfinderMob panda
    ) {
        return RetoldAiScanCache.nearby(
                level,
                panda,
                PathfinderMob.class,
                GROVE_MEMBER_SEARCH_RADIUS_BLOCKS,
                level.getGameTime(),
                PANDA_SCAN_CACHE_TICKS
        ).stream()
                .filter(
                candidate -> candidate != panda
                        && RetoldAnimalSocialGroups.canShareHomeOrRange(
                        panda,
                        candidate
                )
        ).toList();
    }

    private static LivingEntity findThreat(PathfinderMob panda) {
        LivingEntity attacker = panda.getLastHurtByMob();

        if (isValidThreat(panda, attacker)) {
            return attacker;
        }

        LivingEntity target = panda.getTarget();

        if (isValidThreat(panda, target)) {
            return target;
        }

        return null;
    }

    private static boolean isValidThreat(
            PathfinderMob panda,
            LivingEntity threat
    ) {
        if (panda == null || threat == null || threat == panda) {
            return false;
        }

        if (!RetoldBehaviorCoordinator.isAliveInSameLevel(panda, threat)) {
            return false;
        }

        return panda.distanceToSqr(threat) <= FAR_FROM_GROVE_SQUARED
                || threat == panda.getLastHurtByMob();
    }

    private static void rememberDanger(
            PathfinderMob panda,
            long gameTime
    ) {
        RetoldMobState state = RetoldMobStates.getOrCreate(
                panda,
                gameTime
        );

        state.markDanger(gameTime);
        state.addStress(1);
        state.addConfidence(-1);
    }

    private static boolean shouldSeekBamboo(
            PathfinderMob panda,
            long gameTime
    ) {
        if (RetoldAiControl.isControlled(panda)) {
            return false;
        }

        RetoldMobState state = RetoldMobStates.getOrCreate(
                panda,
                gameTime
        );

        return RetoldMobRules.hasEatDrive(
                panda,
                state
        );
    }

    private static void continueBambooFeeding(
            ServerLevel level,
            PathfinderMob panda,
            long gameTime
    ) {
        BlockPos bamboo = findNearestBamboo(
                level,
                panda
        );

        if (bamboo == null) {
            stopControl(panda);
            return;
        }

        if (panda.blockPosition().distSqr(bamboo) <= BAMBOO_EAT_DISTANCE_SQUARED) {
            nibbleBamboo(
                    panda,
                    gameTime
            );
            return;
        }

        moveToBamboo(
                panda,
                bamboo,
                gameTime
        );
    }

    private static void moveToBamboo(
            PathfinderMob panda,
            BlockPos bamboo,
            long gameTime
    ) {
        RetoldBehaviorMovement.claimAndMoveToBlock(
                panda,
                bamboo,
                RetoldAiControlMode.FEED,
                RetoldAiControlOwner.PANDA_BAMBOO,
                BAMBOO_FEED_PRIORITY,
                "panda_bamboo_feed",
                gameTime,
                FEED_CONTROL_TICKS,
                PANDA_FEED_SPEED,
                false
        );
    }

    private static void nibbleBamboo(
            PathfinderMob panda,
            long gameTime
    ) {
        RetoldMobState state = RetoldMobStates.getOrCreate(
                panda,
                gameTime
        );

        state.addHunger(-BAMBOO_RELIEF);
        state.markFed(gameTime);

        RetoldFeedingAnimations.play(panda);
        stopControl(panda);
    }

    private static boolean shouldReturnToGrove(
            ServerLevel level,
            PathfinderMob panda,
            RetoldAnimalHomeMemory grove,
            long gameTime
    ) {
        if (grove == null) {
            return false;
        }

        if (panda.blockPosition().distSqr(grove.pos()) <= GROVE_REST_DISTANCE_SQUARED) {
            return false;
        }

        RetoldMobState state = RetoldMobStates.getOrCreate(
                panda,
                gameTime
        );

        boolean recentDanger = state.lastDangerAt() > 0L
                && gameTime - state.lastDangerAt() <= RECENT_DANGER_RETURN_TICKS;

        boolean restTime = RetoldAnimalDailyRhythm.isNight(level)
                || level.isRainingAt(panda.blockPosition());

        boolean farFromGrove = panda.blockPosition().distSqr(grove.pos()) >= FAR_FROM_GROVE_SQUARED;

        return recentDanger || (farFromGrove && restTime);
    }

    private static void returnToGrove(
            PathfinderMob panda,
            BlockPos grove,
            long gameTime,
            String reason
    ) {
        RetoldBehaviorMovement.claimAndMoveToBlock(
                panda,
                grove,
                RetoldAiControlMode.REGROUP,
                RetoldAiControlOwner.PANDA_BAMBOO,
                RETURN_PRIORITY,
                reason,
                gameTime,
                RETURN_CONTROL_TICKS,
                PANDA_RETURN_SPEED,
                false
        );
    }

    private static void stopControl(PathfinderMob panda) {
        RetoldBehaviorMovement.stopOwnedMovement(
                panda,
                RetoldAiControlOwner.PANDA_BAMBOO
        );
    }

    private static BlockPos findNearestBamboo(
            ServerLevel level,
            PathfinderMob panda
    ) {
        return RetoldBlockTargetSearch.findBamboo(
                level,
                panda,
                BAMBOO_SEARCH_HORIZONTAL_RADIUS,
                BAMBOO_SEARCH_VERTICAL_RADIUS,
                level.getGameTime(),
                BAMBOO_BLOCK_SEARCH_CACHE_TICKS
        );
    }

    private static boolean isBambooGrove(
            ServerLevel level,
            BlockPos pos
    ) {
        if (isBamboo(level, pos)) {
            return true;
        }

        return hasNearbyBamboo(
                level,
                pos,
                4
        );
    }

    private static boolean hasNearbyBamboo(
            ServerLevel level,
            BlockPos pos,
            int radius
    ) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    mutable.set(
                            pos.getX() + dx,
                            pos.getY() + dy,
                            pos.getZ() + dz
                    );

                    if (isBamboo(level, mutable)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static boolean isBamboo(
            ServerLevel level,
            BlockPos pos
    ) {
        return level.getBlockState(pos).is(Blocks.BAMBOO)
                || level.getBlockState(pos).is(Blocks.BAMBOO_SAPLING);
    }
}
