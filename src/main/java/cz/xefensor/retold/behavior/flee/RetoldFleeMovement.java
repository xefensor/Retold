package cz.xefensor.retold.behavior.flee;

import cz.xefensor.retold.behavior.core.RetoldBehaviorMovement;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;

/** Shared destination and route safety for urgent Retold flight. */
final class RetoldFleeMovement {
    private static final int MAX_VERTICAL_SEARCH_BLOCKS = 5;
    private static final double MINIMUM_LAND_SEARCH_BLOCKS = 4.0D;

    private RetoldFleeMovement() {
    }

    static BlockPos chooseDestination(
            PathfinderMob mob,
            Vec3 dangerPos,
            Vec3 directDestination,
            double fleeDistance
    ) {
        BlockPos fallback = BlockPos.containing(directDestination);

        if (!(mob.getNavigation() instanceof GroundPathNavigation)
                || mob.isInWater()
                || dangerPos == null) {
            return fallback;
        }

        Vec3 dryLand = LandRandomPos.getPosAway(
                mob,
                Math.min(MINIMUM_LAND_SEARCH_BLOCKS, fleeDistance * 0.4D),
                Math.max(MINIMUM_LAND_SEARCH_BLOCKS, fleeDistance),
                MAX_VERTICAL_SEARCH_BLOCKS,
                dangerPos
        );

        if (dryLand != null) {
            return BlockPos.containing(dryLand);
        }

        return mob.level().getFluidState(fallback).is(FluidTags.WATER)
                ? mob.blockPosition()
                : fallback;
    }

    static boolean moveTo(
            PathfinderMob mob,
            BlockPos target,
            double speed,
            long gameTime,
            int pathIntervalTicks,
            double repathDistanceSquared
    ) {
        if (mob.getNavigation() instanceof GroundPathNavigation && !mob.isInWater()) {
            return RetoldBehaviorMovement.throttledMoveToAvoidingWater(
                    mob,
                    target,
                    speed,
                    gameTime,
                    pathIntervalTicks,
                    repathDistanceSquared
            );
        }

        return RetoldBehaviorMovement.throttledMoveTo(
                mob,
                target,
                speed,
                gameTime,
                pathIntervalTicks,
                repathDistanceSquared
        );
    }
}
