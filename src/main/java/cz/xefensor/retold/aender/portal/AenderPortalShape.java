package cz.xefensor.retold.aender.portal;

import cz.xefensor.retold.registry.RetoldBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

public record AenderPortalShape(BlockPos minCorner, int width, int depth) {
    public static final int MIN_SIZE = 3;
    public static final int MAX_SIZE = 21;

    public static Optional<AenderPortalShape> findEmpty(LevelReader level, BlockPos interiorPos) {
        return findFromInterior(level, interiorPos, false);
    }

    public static Optional<AenderPortalShape> findComplete(LevelReader level, BlockPos interiorPos) {
        return findFromInterior(level, interiorPos, true);
    }

    public static Optional<AenderPortalShape> findEmptyNextToFrame(LevelReader level, BlockPos framePos) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }

                Optional<AenderPortalShape> shape = findEmpty(level, framePos.offset(dx, 0, dz));

                if (shape.isPresent()) {
                    return shape;
                }
            }
        }

        return Optional.empty();
    }

    public static void invalidateNextToMissingFrame(LevelAccessor level, BlockPos formerFramePos) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }

                BlockPos portalPos = formerFramePos.offset(dx, 0, dz);

                if (!level.getBlockState(portalPos).is(RetoldBlocks.AENDER_PORTAL)) {
                    continue;
                }

                if (findComplete(level, portalPos).isEmpty()) {
                    level.setBlock(portalPos, Blocks.AIR.defaultBlockState(), 3);
                }

                return;
            }
        }
    }

    public void createPortalBlocks(LevelAccessor level) {
        BlockState portalState = RetoldBlocks.AENDER_PORTAL.get().defaultBlockState();
        forEachInterior(pos -> level.setBlock(pos, portalState, 18));
    }

    public boolean isComplete(LevelReader level) {
        return isValid(level, true);
    }

    public BlockPos centerBlock() {
        return minCorner.offset(width / 2, 0, depth / 2);
    }

    public void forEachInterior(PositionConsumer consumer) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                consumer.accept(pos.set(minCorner).move(x, 0, z).immutable());
            }
        }
    }

    private static Optional<AenderPortalShape> findFromInterior(
            LevelReader level,
            BlockPos interiorPos,
            boolean requireComplete
    ) {
        BlockPos west = findFrameBlock(level, interiorPos, Direction.WEST, requireComplete);
        BlockPos east = findFrameBlock(level, interiorPos, Direction.EAST, requireComplete);
        BlockPos north = findFrameBlock(level, interiorPos, Direction.NORTH, requireComplete);
        BlockPos south = findFrameBlock(level, interiorPos, Direction.SOUTH, requireComplete);

        if (west == null || east == null || north == null || south == null) {
            return Optional.empty();
        }

        int width = east.getX() - west.getX() - 1;
        int depth = south.getZ() - north.getZ() - 1;

        if (width < MIN_SIZE || width > MAX_SIZE || depth < MIN_SIZE || depth > MAX_SIZE) {
            return Optional.empty();
        }

        AenderPortalShape shape = new AenderPortalShape(
                new BlockPos(west.getX() + 1, interiorPos.getY(), north.getZ() + 1),
                width,
                depth
        );

        return shape.isValid(level, requireComplete) ? Optional.of(shape) : Optional.empty();
    }

    private static BlockPos findFrameBlock(
            LevelReader level,
            BlockPos origin,
            Direction direction,
            boolean requireComplete
    ) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int distance = 1; distance <= MAX_SIZE + 1; distance++) {
            cursor.set(origin).move(direction, distance);
            BlockState state = level.getBlockState(cursor);

            if (state.is(RetoldBlocks.DEV_AENDER_PORTAL_FRAME)) {
                return cursor.immutable();
            }

            if (requireComplete) {
                if (!state.is(RetoldBlocks.AENDER_PORTAL)) {
                    return null;
                }
            } else if (!isEmptyForPortal(state)) {
                return null;
            }
        }

        return null;
    }

    private boolean isValid(LevelReader level, boolean requireComplete) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = -1; x <= width; x++) {
            for (int z = -1; z <= depth; z++) {
                pos.set(minCorner).move(x, 0, z);
                boolean frame = x == -1 || x == width || z == -1 || z == depth;
                BlockState state = level.getBlockState(pos);

                if (frame) {
                    if (!state.is(RetoldBlocks.DEV_AENDER_PORTAL_FRAME)) {
                        return false;
                    }
                } else if (requireComplete) {
                    if (!state.is(RetoldBlocks.AENDER_PORTAL)) {
                        return false;
                    }
                } else if (!isEmptyForPortal(state)) {
                    return false;
                }
            }
        }

        return true;
    }

    private static boolean isEmptyForPortal(BlockState state) {
        return state.isAir() || state.is(Blocks.FIRE) || state.is(RetoldBlocks.AENDER_PORTAL);
    }

    @FunctionalInterface
    public interface PositionConsumer {
        void accept(BlockPos pos);
    }
}
