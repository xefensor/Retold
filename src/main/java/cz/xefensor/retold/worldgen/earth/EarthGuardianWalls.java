package cz.xefensor.retold.worldgen.earth;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

/** Small, supported trailing walls made only from conserved full-block material. */
final class EarthGuardianWalls {
    static final int WALL_BLOCKS = 9;
    private static final int MINIMUM_RESERVE = 18;

    private EarthGuardianWalls() {
    }

    static int buildBehind(ServerLevel level, EarthGuardian guardian, Direction travel) {
        if (guardian.terrainReserveSize() < MINIMUM_RESERVE) {
            return 0;
        }
        Direction side = travel.getClockWise();
        BlockPos origin = guardian.blockPosition();
        for (int distance = 3; distance <= 6; distance++) {
            for (int height = -2; height <= 2; height++) {
                BlockPos base = origin.relative(travel.getOpposite(), distance).above(height);
                if (!hasSupportedBase(level, base, side)) {
                    continue;
                }
                int placed = 0;
                for (int y = 0; y < 3; y++) {
                    for (int width = -1; width <= 1; width++) {
                        BlockPos pos = base.relative(side, width).above(y);
                        if (!guardian.isActiveRoutePosition(pos) && guardian.tryPlaceRouteSupport(level, pos)) {
                            placed++;
                        }
                    }
                }
                if (placed > 0) {
                    return placed;
                }
            }
        }
        return 0;
    }

    private static boolean hasSupportedBase(ServerLevel level, BlockPos base, Direction side) {
        for (int width = -1; width <= 1; width++) {
            BlockPos foot = base.relative(side, width);
            if (!level.hasChunkAt(foot) || level.isOutsideBuildHeight(foot.below())
                    || level.isOutsideBuildHeight(foot.above(2))
                    || !level.getBlockState(foot.below()).isCollisionShapeFullBlock(level, foot.below())) {
                return false;
            }
        }
        return true;
    }
}
