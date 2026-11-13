package cz.xefensor.retold.worldgen.earth;

import cz.xefensor.retold.behavior.core.RetoldBehaviorMovement;
import cz.xefensor.retold.behavior.core.RetoldMobGriefing;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/** Short, loaded-only walking corridors. The controller batches these one-edit steps. */
final class EarthGuardianRoute {
    private final EarthGuardian guardian;
    private @Nullable BlockPos start;
    private @Nullable BlockPos clue;
    private @Nullable Direction rampDirection;
    private Direction direction = Direction.NORTH;
    private int rampTargetY;
    private int rise;
    private boolean ready;
    private @Nullable BlockPos blockedAt;
    private boolean backfilled;
    private int removed;

    EarthGuardianRoute(EarthGuardian guardian) {
        this.guardian = guardian;
    }

    boolean active() {
        return start != null;
    }

    Direction direction() {
        return direction;
    }

    boolean reserves(BlockPos pos) {
        if (start == null) {
            return false;
        }
        Direction side = direction.getClockWise();
        int dx = pos.getX() - start.getX();
        int dz = pos.getZ() - start.getZ();
        int distance = dx * direction.getStepX() + dz * direction.getStepZ();
        int width = dx * side.getStepX() + dz * side.getStepZ();
        if (distance < 0 || distance > 4 || Math.abs(width) > 1) {
            return false;
        }
        int feetY = strip(start, direction, rise, distance).getY();
        int clearance = (int) Math.ceil(guardian.getBbHeight()) + (rise == 0 ? 0 : 1);
        return pos.getY() >= feetY - 1 && pos.getY() < feetY + clearance;
    }

    @Nullable BlockPos destination() {
        return start != null && ready ? strip(start, direction, rise, 3) : null;
    }

    void clear() {
        start = null;
        clue = null;
        rampDirection = null;
        ready = false;
    }

    void tick(ServerLevel level, BlockPos target) {
        // Footsteps move the remembered clue while this short segment is being built/walked.
        // Finish the committed landing before steering toward the latest clue; restarting on
        // each step repeatedly stops navigation at the same tunnel entrance.
        clue = target.immutable();
        if (start != null && ready) {
            BlockPos end = strip(start, direction, rise, 3);
            Vec3 center = Vec3.atBottomCenterOf(end);
            if (guardian.position().distanceToSqr(center) > 0.8D
                    || Math.abs(guardian.getY() - end.getY()) > 0.3D) {
                if (!guardian.getNavigation().isDone()) {
                    return;
                }
                // Player edits and environmental attacks can invalidate a prepared segment.
                ready = false;
            } else {
                start = null;
            }
        }
        if (start == null) {
            // Large-mob path nodes end on integer centers. Using floor(position) would shift
            // the corridor sideways by one block after every segment instead of following it.
            start = BlockPos.containing(guardian.getX() - 0.5D, guardian.getY(), guardian.getZ() - 0.5D);
            rise = Integer.compare(target.getY(), start.getY());
            if (rise == 0) {
                rampDirection = null;
            } else if (rampDirection == null || rampTargetY != target.getY()) {
                rampDirection = toward(start, target);
                rampTargetY = target.getY();
            }
            // Keep a ramp's heading until it reaches the clue's level; turning back each step
            // toward a clue directly overhead would excavate the ramp we are standing on.
            direction = rampDirection == null ? toward(start, target) : rampDirection;
            // Only align sideways. Along the travel axis the next raised support must remain
            // ahead of the entire body, otherwise occupancy protection correctly refuses it.
            BlockPos occupied = guardian.blockPosition();
            start = direction.getAxis() == Direction.Axis.X
                    ? new BlockPos(occupied.getX(), start.getY(), start.getZ())
                    : new BlockPos(start.getX(), start.getY(), occupied.getZ());
            ready = false;
            guardian.getNavigation().stop();
        }
        int clearance = (int) Math.ceil(guardian.getBbHeight()) + (rise == 0 ? 0 : 1);
        Direction side = direction.getClockWise();
        // Aim beyond the first lower step: navigation may stop up to half a body-width short,
        // which would otherwise leave this wide mob supported by the upper ledge forever.
        for (int distance = 0; distance <= 4; distance++) {
            BlockPos feet = strip(start, direction, rise, distance);
            for (int height = 0; height < clearance; height++) {
                for (int width = -1; width <= 1; width++) {
                    BlockPos pos = feet.relative(side, width).above(height);
                    if (!available(level, pos)) {
                        return;
                    }
                    if (!level.getBlockState(pos).isAir()) {
                        blockedAt = pos;
                        if (guardian.tryExcavateBlock(level, pos)) {
                            removed++;
                            RetoldBehaviorMovement.clearGroundPathState(guardian);
                        } else if (guardian.isTerrainReserveFull()) {
                            backfilled = guardian.placeBarrierBehind(level, direction);
                        }
                        return;
                    }
                }
            }
        }
        for (int distance = 0; distance <= 4; distance++) {
            BlockPos feet = strip(start, direction, rise, distance);
            for (int width = -1; width <= 1; width++) {
                BlockPos support = feet.relative(side, width).below();
                if (!available(level, support)) {
                    return;
                }
                BlockState state = level.getBlockState(support);
                if (state.isCollisionShapeFullBlock(level, support)) {
                    continue;
                }
                blockedAt = support;
                if (state.isAir() && new AABB(support).intersects(guardian.getBoundingBox())) {
                    // Stopping navigation does not remove momentum. If the body drifted into
                    // the next raised support while we worked, move the next stair ahead of
                    // its current feet instead of repeatedly attempting an occupied placement.
                    start = null;
                    ready = false;
                    guardian.getNavigation().stop();
                    return;
                }
                if (!state.isAir()) {
                    if (!guardian.tryExcavateBlock(level, support) && guardian.isTerrainReserveFull()) {
                        guardian.placeBarrierBehind(level, direction);
                    }
                } else if (RetoldMobGriefing.canPlaceBlock(level, guardian, support)) {
                    if (guardian.hasRouteSupport(level, support)) {
                        guardian.tryPlaceRouteSupport(level, support);
                    } else {
                        gatherSupport(level, side);
                    }
                }
                return;
            }
        }
        ready = true;
        RetoldBehaviorMovement.clearGroundPathState(guardian);
    }

    private void gatherSupport(ServerLevel level, Direction side) {
        if (guardian.isTerrainReserveFull()) {
            guardian.placeBarrierBehind(level, direction);
            return;
        }
        // Donors are outside the three-wide route, never its own floor. No material is created.
        for (int distance = 0; distance <= 3; distance++) {
            for (int width : new int[]{-2, 2}) {
                BlockPos donor = start.relative(direction, distance).relative(side, width).below();
                if (available(level, donor) && level.getBlockState(donor).isCollisionShapeFullBlock(level, donor)
                        && guardian.tryExcavateBlock(level, donor)) {
                    return;
                }
            }
        }
    }

    private static boolean available(ServerLevel level, BlockPos pos) {
        return !level.isOutsideBuildHeight(pos) && level.hasChunkAt(pos);
    }

    String describe() {
        return "start=" + start + ", direction=" + direction + ", rise=" + rise
                + ", ready=" + ready + ", blockedAt=" + blockedAt
                + ", blockedState=" + (blockedAt == null ? null : guardian.level().getBlockState(blockedAt))
                + ", backfilled=" + backfilled + ", removed=" + removed + ", clue=" + clue;
    }

    static BlockPos strip(BlockPos start, Direction direction, int rise, int distance) {
        return start.relative(direction, distance).above(distance < 2 ? 0 : rise);
    }

    private static Direction toward(BlockPos start, BlockPos target) {
        int dx = target.getX() - start.getX();
        int dz = target.getZ() - start.getZ();
        if (dx == 0 && dz == 0) {
            return Direction.NORTH;
        }
        return Math.abs(dx) >= Math.abs(dz) ? (dx >= 0 ? Direction.EAST : Direction.WEST)
                : (dz >= 0 ? Direction.SOUTH : Direction.NORTH);
    }

    void save(ValueOutput output) {
        if (rampDirection != null) {
            output.putInt("retold_ramp_direction", rampDirection.get2DDataValue());
            output.putInt("retold_ramp_target_y", rampTargetY);
        }
    }

    void load(ValueInput input) {
        clear();
        int savedDirection = input.getIntOr("retold_ramp_direction", -1);
        if (savedDirection >= 0 && savedDirection < 4) {
            rampDirection = Direction.from2DDataValue(savedDirection);
            rampTargetY = input.getIntOr("retold_ramp_target_y", guardian.blockPosition().getY());
        }
    }
}
