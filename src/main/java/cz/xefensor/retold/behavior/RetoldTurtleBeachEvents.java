package cz.xefensor.retold.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.List;

public final class RetoldTurtleBeachEvents {
    private static final int THINK_INTERVAL_TICKS = 20;
    private static final int TURTLE_SCAN_CACHE_TICKS = 10;
    private static final int TURTLE_BLOCK_SEARCH_CACHE_TICKS = 35;
    private static final int CONTROL_TICKS = 20 * 5;

    private static final int FLEE_PRIORITY = RetoldAiPriorities.below(RetoldAiPriorities.FLEE, 1);
    private static final int RETURN_PRIORITY = RetoldAiPriorities.above(RetoldAiPriorities.REGROUP, 4);

    private static final int BEACH_SEARCH_HORIZONTAL_RADIUS = 12;
    private static final int BEACH_SEARCH_VERTICAL_RADIUS = 4;
    private static final double BEACH_MEMBER_SEARCH_RADIUS_BLOCKS = 16.0D;
    private static final int WATER_SEARCH_HORIZONTAL_RADIUS = 18;
    private static final int WATER_SEARCH_VERTICAL_RADIUS = 5;

    private static final int RECENT_DANGER_RETURN_TICKS = 20 * 45;

    private static final double CLOSE_PLAYER_THREAT_BLOCKS = 5.5D;
    private static final double CLOSE_PLAYER_THREAT_SQUARED =
            CLOSE_PLAYER_THREAT_BLOCKS * CLOSE_PLAYER_THREAT_BLOCKS;

    private static final double THREAT_SCAN_RADIUS_BLOCKS = 12.0D;

    private static final double FAR_FROM_BEACH_BLOCKS = 28.0D;
    private static final double FAR_FROM_BEACH_SQUARED =
            FAR_FROM_BEACH_BLOCKS * FAR_FROM_BEACH_BLOCKS;

    private static final double BEACH_REACHED_BLOCKS = 4.0D;
    private static final double BEACH_REACHED_SQUARED =
            BEACH_REACHED_BLOCKS * BEACH_REACHED_BLOCKS;

    private static final double WATER_REACHED_BLOCKS = 2.5D;
    private static final double WATER_REACHED_SQUARED =
            WATER_REACHED_BLOCKS * WATER_REACHED_BLOCKS;

    private static final double TURTLE_WATER_FLEE_SPEED = 1.08D;
    private static final double TURTLE_BEACH_RETURN_SPEED = 0.82D;

    private RetoldTurtleBeachEvents() {
    }

    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof PathfinderMob turtle)) {
            return;
        }

        if (!(turtle.level() instanceof ServerLevel level)) {
            return;
        }

        if (!RetoldMobRules.isTurtleBeach(turtle)) {
            return;
        }

        long gameTime = level.getGameTime();

        if (!RetoldBehaviorTiming.shouldThink(
                turtle,
                gameTime,
                THINK_INTERVAL_TICKS
        )) {
            return;
        }

        handleTurtle(
                level,
                turtle,
                gameTime
        );
    }

    private static void handleTurtle(
            ServerLevel level,
            PathfinderMob turtle,
            long gameTime
    ) {
        RetoldAnimalHomeMemory beach = getOrCreateBeachHome(
                level,
                turtle,
                gameTime
        );

        LivingEntity threat = findThreat(
                level,
                turtle
        );

        if (threat != null) {
            rememberDanger(
                    turtle,
                    gameTime
            );

            BlockPos water = findNearestWater(
                    level,
                    turtle
            );

            if (water != null) {
                fleeToWater(
                        turtle,
                        water,
                        gameTime
                );
            }

            return;
        }

        if (RetoldAiControl.isControlledAsBy(
                turtle,
                RetoldAiControlMode.FLEE,
                RetoldAiControlOwner.NEUTRAL_WILDLIFE
        )) {
            if (isInWater(turtle) || turtle.getNavigation().isDone()) {
                stopTurtleControl(turtle, gameTime);
            }
            return;
        }

        if (beach == null) {
            return;
        }

        if (shouldReturnToBeach(level, turtle, beach, gameTime)) {
            returnToBeach(
                    turtle,
                    beach.pos(),
                    gameTime
            );
            return;
        }

        if (
                RetoldAiControl.isControlledAsBy(
                        turtle,
                        RetoldAiControlMode.REGROUP,
                        RetoldAiControlOwner.NEUTRAL_WILDLIFE
                )
                        && turtle.blockPosition().distSqr(beach.pos()) <= BEACH_REACHED_SQUARED
        ) {
            stopTurtleControl(turtle, gameTime);
            RetoldAnimalHomes.markUsed(
                    turtle,
                    gameTime
            );
        }
    }

    private static RetoldAnimalHomeMemory getOrCreateBeachHome(
            ServerLevel level,
            PathfinderMob turtle,
            long gameTime
    ) {
        RetoldAnimalHomeMemory existing = RetoldAnimalHomes.get(turtle);

        if (RetoldAnimalHomes.isValidFor(level, turtle, existing) && isBeachSand(level, existing.pos())) {
            RetoldAnimalHomes.markUsed(
                    turtle,
                    gameTime
            );
            return existing;
        }

        BlockPos beachPos = findNearestBeachSand(
                level,
                turtle
        );

        if (beachPos == null) {
            return null;
        }

        return RetoldAnimalHomes.getOrCreatePackHome(
                level,
                turtle,
                findNearbyBeachMembers(
                        level,
                        turtle
                ),
                beachPos,
                gameTime
        );
    }

    private static List<PathfinderMob> findNearbyBeachMembers(
            ServerLevel level,
            PathfinderMob turtle
    ) {
        return RetoldAiScanCache.nearby(
                level,
                turtle,
                PathfinderMob.class,
                BEACH_MEMBER_SEARCH_RADIUS_BLOCKS,
                level.getGameTime(),
                TURTLE_SCAN_CACHE_TICKS
        ).stream()
                .filter(
                candidate -> candidate != turtle
                        && RetoldAnimalSocialGroups.canShareHomeOrRange(
                        turtle,
                        candidate
                )
        ).toList();
    }

    private static LivingEntity findThreat(
            ServerLevel level,
            PathfinderMob turtle
    ) {
        LivingEntity recentAttacker = turtle.getLastHurtByMob();

        if (isValidThreat(turtle, recentAttacker)) {
            return recentAttacker;
        }

        List<LivingEntity> candidates = RetoldAiScanCache.nearby(
                level,
                turtle,
                LivingEntity.class,
                THREAT_SCAN_RADIUS_BLOCKS,
                level.getGameTime(),
                TURTLE_SCAN_CACHE_TICKS
        );

        LivingEntity bestThreat = null;
        double bestScore = Double.MAX_VALUE;

        for (LivingEntity candidate : candidates) {
            if (!isValidThreat(turtle, candidate)) {
                continue;
            }

            double score = turtle.distanceToSqr(candidate);

            if (candidate instanceof PathfinderMob mob && mob.getTarget() == turtle) {
                score -= 80.0D;
            }

            if (candidate == recentAttacker) {
                score -= 60.0D;
            }

            if (score < bestScore) {
                bestScore = score;
                bestThreat = candidate;
            }
        }

        return bestThreat;
    }

    private static boolean isValidThreat(
            PathfinderMob turtle,
            LivingEntity candidate
    ) {
        if (turtle == null || candidate == null || candidate == turtle) {
            return false;
        }

        if (!RetoldBehaviorCoordinator.isAliveInSameLevel(turtle, candidate)) {
            return false;
        }

        if (RetoldBehaviorCoordinator.isInvalidPlayerTarget(candidate)) {
            return false;
        }

        if (candidate instanceof Player player) {
            return turtle.distanceToSqr(player) <= CLOSE_PLAYER_THREAT_SQUARED
                    && RetoldAiSightCache.canSee(turtle, player, turtle.level().getGameTime());
        }

        if (candidate == turtle.getLastHurtByMob()) {
            return true;
        }

        return candidate instanceof PathfinderMob mob
                && mob.getTarget() == turtle;
    }

    private static void rememberDanger(
            PathfinderMob turtle,
            long gameTime
    ) {
        RetoldMobState state = RetoldMobStates.getOrCreate(
                turtle,
                gameTime
        );

        state.markDanger(gameTime);
        state.addStress(2);
        state.addConfidence(-1);
    }

    private static void fleeToWater(
            PathfinderMob turtle,
            BlockPos water,
            long gameTime
    ) {
        RetoldBehaviorMovement.claimAndMoveToBlock(
                turtle,
                water,
                RetoldAiControlMode.FLEE,
                RetoldAiControlOwner.NEUTRAL_WILDLIFE,
                FLEE_PRIORITY,
                "turtle_water_flee",
                gameTime,
                CONTROL_TICKS,
                TURTLE_WATER_FLEE_SPEED,
                true
        );
    }

    private static boolean shouldReturnToBeach(
            ServerLevel level,
            PathfinderMob turtle,
            RetoldAnimalHomeMemory beach,
            long gameTime
    ) {
        if (beach == null) {
            return false;
        }

        if (turtle.blockPosition().distSqr(beach.pos()) <= BEACH_REACHED_SQUARED) {
            return false;
        }

        RetoldMobState state = RetoldMobStates.getOrCreate(
                turtle,
                gameTime
        );

        boolean recentDanger = state.lastDangerAt() > 0L
                && gameTime - state.lastDangerAt() <= RECENT_DANGER_RETURN_TICKS;

        boolean farFromBeach = turtle.blockPosition().distSqr(beach.pos()) >= FAR_FROM_BEACH_SQUARED;
        boolean restTime = RetoldAnimalDailyRhythm.isNight(level)
                || level.isRainingAt(turtle.blockPosition());

        return recentDanger || (farFromBeach && restTime);
    }

    private static void returnToBeach(
            PathfinderMob turtle,
            BlockPos beach,
            long gameTime
    ) {
        RetoldBehaviorMovement.claimAndMoveToBlock(
                turtle,
                beach,
                RetoldAiControlMode.REGROUP,
                RetoldAiControlOwner.NEUTRAL_WILDLIFE,
                RETURN_PRIORITY,
                "turtle_beach_return",
                gameTime,
                CONTROL_TICKS,
                TURTLE_BEACH_RETURN_SPEED,
                false
        );
    }

    private static void stopTurtleControl(
            PathfinderMob turtle,
            long gameTime
    ) {
        RetoldBehaviorMovement.stopOwnedMovement(
                turtle,
                RetoldAiControlOwner.NEUTRAL_WILDLIFE
        );

        RetoldMobStates.getOrCreate(
                turtle,
                gameTime
        ).markFleeEnded(gameTime);
    }

    private static BlockPos findNearestBeachSand(
            ServerLevel level,
            PathfinderMob turtle
    ) {
        return RetoldBlockTargetSearch.findBeachSand(
                level,
                turtle,
                BEACH_SEARCH_HORIZONTAL_RADIUS,
                BEACH_SEARCH_VERTICAL_RADIUS,
                level.getGameTime(),
                TURTLE_BLOCK_SEARCH_CACHE_TICKS
        );
    }

    private static boolean isBeachSand(
            ServerLevel level,
            BlockPos pos
    ) {
        if (level == null || pos == null) {
            return false;
        }

        if (!isSand(level, pos)) {
            return false;
        }

        if (!level.getBlockState(pos.above()).isAir()) {
            return false;
        }

        return hasNearbyWater(
                level,
                pos
        );
    }

    private static boolean hasNearbyWater(
            ServerLevel level,
            BlockPos pos
    ) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int dx = -4; dx <= 4; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -4; dz <= 4; dz++) {
                    mutable.set(
                            pos.getX() + dx,
                            pos.getY() + dy,
                            pos.getZ() + dz
                    );

                    if (isWater(level, mutable)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static BlockPos findNearestWater(
            ServerLevel level,
            PathfinderMob turtle
    ) {
        return RetoldBlockTargetSearch.findWater(
                level,
                turtle,
                WATER_SEARCH_HORIZONTAL_RADIUS,
                WATER_SEARCH_VERTICAL_RADIUS,
                level.getGameTime(),
                TURTLE_BLOCK_SEARCH_CACHE_TICKS
        );
    }

    private static boolean isInWater(PathfinderMob turtle) {
        return turtle.isInWater();
    }

    private static boolean isSand(
            ServerLevel level,
            BlockPos pos
    ) {
        return level.getBlockState(pos).is(Blocks.SAND)
                || level.getBlockState(pos).is(Blocks.RED_SAND);
    }

    private static boolean isWater(
            ServerLevel level,
            BlockPos pos
    ) {
        return level.getFluidState(pos).is(FluidTags.WATER);
    }
}
