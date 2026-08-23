package cz.xefensor.retold.worldgen.earth;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.ArrayList;
import java.util.List;

/** Chunk-bounded placement of the initial carved-tunnel Earth Labyrinth. */
final class EarthLabyrinthGenerator {
    private static final int CHAMBER_RADIUS = 2;
    private static final int GUARDIAN_CHAMBER_RADIUS = 3;
    private static final int INTERIOR_RADIUS = 1;

    private EarthLabyrinthGenerator() {
    }

    static void generate(
            WorldGenLevel level,
            BoundingBox chunkBounds,
            int centerX,
            int centerZ,
            int pyramidBaseY,
            BlockPos connection,
            Direction egressDirection,
            long layoutSeed
    ) {
        EarthLabyrinthLayout layout = EarthLabyrinthPlanner.generate(layoutSeed);
        Placement placement = Placement.centered(
                layout,
                centerX,
                centerZ,
                pyramidBaseY,
                layoutSeed
        );

        generateHorizontalPassageShells(level, chunkBounds, layoutSeed, layout, placement);
        generateCaveNodes(level, chunkBounds, layoutSeed, placement);
        carveHorizontalPassages(level, chunkBounds, layoutSeed, layout, placement);
        generateVerticalConnections(level, chunkBounds, layoutSeed, layout, placement);
        generatePuzzleRoomShaft(
                level,
                chunkBounds,
                layoutSeed,
                connection,
                egressDirection
        );
        generateEntranceStaircase(
                level,
                chunkBounds,
                layoutSeed,
                entranceStaircaseStart(connection),
                egressDirection,
                placement.cellCenter(layout.entrance())
        );
    }

    private static void generateHorizontalPassageShells(
            WorldGenLevel level,
            BoundingBox chunkBounds,
            long layoutSeed,
            EarthLabyrinthLayout layout,
            Placement placement
    ) {
        for (EarthLabyrinthLayout.Passage passage : layout.passages()) {
            if (passage.isVertical()) {
                continue;
            }

            List<BlockPos> path = placement.tunnelPath(passage);

            for (int index = 0; index < path.size(); index++) {
                BlockPos center = path.get(index);
                Direction direction = tunnelDirection(path, index);

                for (int yOffset = 0;
                     yOffset < EarthLabyrinthDimensions.CHAMBER_HEIGHT;
                     yOffset++) {
                    for (int width = -CHAMBER_RADIUS; width <= CHAMBER_RADIUS; width++) {
                        BlockPos target = passageCrossSectionPosition(
                                center,
                                direction,
                                width,
                                yOffset
                        );
                        setBlock(level, chunkBounds, target, wallState(layoutSeed, target));
                    }
                }
            }
        }
    }

    private static void generateCaveNodes(
            WorldGenLevel level,
            BoundingBox chunkBounds,
            long layoutSeed,
            Placement placement
    ) {
        for (EarthLabyrinthLayout.Cell cell : placement.layout().cells()) {
            BlockPos center = placement.cellCenter(cell);
            boolean guardianChamber = placement.layout().kindAt(cell)
                    == EarthLabyrinthLayout.CellKind.GUARDIAN_CHAMBER;
            int radius = guardianChamber ? GUARDIAN_CHAMBER_RADIUS : CHAMBER_RADIUS;

            for (int yOffset = 0;
                 yOffset < EarthLabyrinthDimensions.CHAMBER_HEIGHT;
                 yOffset++) {
                for (int zOffset = -radius;
                     zOffset <= radius;
                     zOffset++) {
                    for (int xOffset = -radius;
                         xOffset <= radius;
                         xOffset++) {
                        BlockPos target = center.offset(xOffset, yOffset, zOffset);
                        setBlock(
                                level,
                                chunkBounds,
                                target,
                                isCaveNodeInterior(
                                        layoutSeed,
                                        target,
                                        xOffset,
                                        yOffset,
                                        zOffset,
                                        guardianChamber
                                )
                                        ? Blocks.CAVE_AIR.defaultBlockState()
                                        : wallState(layoutSeed, target)
                        );
                    }
                }
            }
        }
    }

    private static boolean isCaveNodeInterior(
            long layoutSeed,
            BlockPos target,
            int xOffset,
            int yOffset,
            int zOffset,
            boolean guardianChamber
    ) {
        if (yOffset < 1 || yOffset > 3) {
            return false;
        }

        if (guardianChamber) {
            int distanceSquared = xOffset * xOffset + zOffset * zOffset;
            return distanceSquared < 4
                    || distanceSquared <= 5 && roughValue(layoutSeed, target, 3L) != 0L;
        }

        if (Math.abs(xOffset) > INTERIOR_RADIUS || Math.abs(zOffset) > INTERIOR_RADIUS) {
            return false;
        }

        boolean corner = Math.abs(xOffset) == INTERIOR_RADIUS
                && Math.abs(zOffset) == INTERIOR_RADIUS;
        return !corner || roughValue(layoutSeed, target, 3L) != 0L;
    }

    private static void carveHorizontalPassages(
            WorldGenLevel level,
            BoundingBox chunkBounds,
            long layoutSeed,
            EarthLabyrinthLayout layout,
            Placement placement
    ) {
        for (EarthLabyrinthLayout.Passage passage : layout.passages()) {
            if (passage.isVertical()) {
                continue;
            }

            List<BlockPos> path = placement.tunnelPath(passage);

            for (int index = 0; index < path.size(); index++) {
                BlockPos center = path.get(index);
                Direction direction = tunnelDirection(path, index);

                for (int height = 1; height <= 3; height++) {
                    int widthRadius = height == 3
                            && roughValue(layoutSeed, center, 4L) == 0L
                            ? 0
                            : INTERIOR_RADIUS;

                    for (int width = -widthRadius; width <= widthRadius; width++) {
                        BlockPos target = passageCrossSectionPosition(
                                center,
                                direction,
                                width,
                                height
                        );
                        setBlock(level, chunkBounds, target, Blocks.CAVE_AIR.defaultBlockState());
                    }
                }
            }
        }
    }

    private static BlockPos passageCrossSectionPosition(
            BlockPos center,
            Direction direction,
            int width,
            int height
    ) {
        return direction.getAxis() == Direction.Axis.X
                ? center.offset(0, height, width)
                : center.offset(width, height, 0);
    }

    private static Direction tunnelDirection(List<BlockPos> path, int index) {
        return index + 1 < path.size()
                ? directionBetween(path.get(index), path.get(index + 1))
                : directionBetween(path.get(index - 1), path.get(index));
    }

    static BlockPos cellCenter(
            EarthLabyrinthLayout layout,
            EarthLabyrinthLayout.Cell cell,
            int centerX,
            int centerZ,
            int pyramidBaseY,
            long layoutSeed
    ) {
        return Placement.centered(
                layout,
                centerX,
                centerZ,
                pyramidBaseY,
                layoutSeed
        ).cellCenter(cell);
    }

    static List<BlockPos> tunnelPath(
            EarthLabyrinthLayout layout,
            EarthLabyrinthLayout.Passage passage,
            int centerX,
            int centerZ,
            int pyramidBaseY,
            long layoutSeed
    ) {
        return Placement.centered(
                layout,
                centerX,
                centerZ,
                pyramidBaseY,
                layoutSeed
        ).tunnelPath(passage);
    }

    private static void generateVerticalConnections(
            WorldGenLevel level,
            BoundingBox chunkBounds,
            long layoutSeed,
            EarthLabyrinthLayout layout,
            Placement placement
    ) {
        BlockState ladder = Blocks.LADDER.defaultBlockState()
                .setValue(LadderBlock.FACING, Direction.SOUTH);

        for (EarthLabyrinthLayout.Passage passage : layout.passages()) {
            if (!passage.isVertical()) {
                continue;
            }

            EarthLabyrinthLayout.Cell upperCell = passage.first().level() == 0
                    ? passage.first()
                    : passage.second();
            EarthLabyrinthLayout.Cell lowerCell = passage.first().level() == 1
                    ? passage.first()
                    : passage.second();
            BlockPos upper = placement.cellCenter(upperCell);
            BlockPos lower = placement.cellCenter(lowerCell);
            BlockPos supportBase = new BlockPos(upper.getX(), lower.getY(), upper.getZ());
            BlockPos ladderBase = supportBase.south();

            for (int y = lower.getY();
                 y <= upper.getY() + EarthLabyrinthDimensions.CHAMBER_HEIGHT - 2;
                 y++) {
                BlockPos support = new BlockPos(supportBase.getX(), y, supportBase.getZ());
                BlockPos ladderPos = new BlockPos(ladderBase.getX(), y, ladderBase.getZ());
                setBlock(level, chunkBounds, support, wallState(layoutSeed, support));

                if (y > lower.getY()) {
                    setBlock(level, chunkBounds, ladderPos, ladder);
                }
            }
        }
    }

    private static void generateEntranceStaircase(
            WorldGenLevel level,
            BoundingBox chunkBounds,
            long layoutSeed,
            BlockPos connection,
            Direction egressDirection,
            BlockPos entrance
    ) {
        List<BlockPos> path = entrancePath(connection, egressDirection, entrance);

        for (int index = 3; index < path.size(); index++) {
            BlockPos node = path.get(index);

            for (int yOffset = 0; yOffset <= 4; yOffset++) {
                for (int zOffset = -2; zOffset <= 2; zOffset++) {
                    for (int xOffset = -2; xOffset <= 2; xOffset++) {
                        boolean shell = yOffset == 0
                                || yOffset == 4
                                || Math.abs(xOffset) == 2
                                || Math.abs(zOffset) == 2;

                        BlockPos target = node.offset(xOffset, yOffset, zOffset);

                        if (shell
                                && !isInsideEntranceChamber(target, entrance)
                                && !isInsideEntranceShaft(
                                        target,
                                        connection,
                                        egressDirection
                                )) {
                            setBlock(level, chunkBounds, target, wallState(layoutSeed, target));
                        }
                    }
                }
            }
        }

        for (int index = 1; index < path.size(); index++) {
            BlockPos node = path.get(index);
            int radius = index <= 3 ? 0 : INTERIOR_RADIUS;

            for (int yOffset = 1; yOffset <= 3; yOffset++) {
                for (int zOffset = -radius;
                     zOffset <= radius;
                     zOffset++) {
                    for (int xOffset = -radius;
                         xOffset <= radius;
                         xOffset++) {
                        BlockPos target = node.offset(xOffset, yOffset, zOffset);

                        if (!isInsideEntranceShaft(target, connection, egressDirection)) {
                            setBlock(
                                    level,
                                    chunkBounds,
                                    target,
                                    Blocks.CAVE_AIR.defaultBlockState()
                            );
                        }
                    }
                }
            }
        }

        for (int index = 1; index < path.size(); index++) {
            BlockPos previous = path.get(index - 1);
            BlockPos node = path.get(index);

            if (node.getY() >= previous.getY()) {
                continue;
            }

            Direction travel = directionBetween(previous, node);
            BlockState stair = Blocks.MOSSY_COBBLESTONE_STAIRS.defaultBlockState()
                    .setValue(StairBlock.FACING, travel.getOpposite());
            setBlock(level, chunkBounds, node, stair);
        }
    }

    /**
     * Drops below the complete vanilla puzzle footprint before any horizontal carving begins.
     * The opening is the unused center of the revealed chamber at local (9, -3, 9); its
     * neighboring redstone, pistons, repeater, hidden chest, and their supports remain untouched.
     */
    private static void generatePuzzleRoomShaft(
            WorldGenLevel level,
            BoundingBox chunkBounds,
            long layoutSeed,
            BlockPos connection,
            Direction egressDirection
    ) {
        BlockState ladder = Blocks.LADDER.defaultBlockState()
                .setValue(LadderBlock.FACING, egressDirection);
        Direction supportDirection = egressDirection.getOpposite();

        setBlock(
                level,
                chunkBounds,
                connection.above(),
                Blocks.CAVE_AIR.defaultBlockState()
        );

        for (int depth = 0;
             depth <= EarthLabyrinthDimensions.ENTRANCE_SHAFT_DEPTH;
             depth++) {
            BlockPos ladderPos = connection.below(depth);
            BlockPos support = ladderPos.relative(supportDirection);
            setBlock(level, chunkBounds, support, wallState(layoutSeed, support));
            setBlock(level, chunkBounds, ladderPos, ladder);
        }
    }

    static BlockPos entranceStaircaseStart(BlockPos connection) {
        return connection.below(EarthLabyrinthDimensions.ENTRANCE_SHAFT_DEPTH);
    }

    private static boolean isInsideEntranceShaft(
            BlockPos target,
            BlockPos staircaseStart,
            Direction egressDirection
    ) {
        if (target.getY() < staircaseStart.getY()) {
            return false;
        }

        BlockPos supportStart = staircaseStart.relative(egressDirection.getOpposite());
        return (target.getX() == staircaseStart.getX()
                && target.getZ() == staircaseStart.getZ())
                || (target.getX() == supportStart.getX()
                && target.getZ() == supportStart.getZ());
    }

    private static boolean isInsideEntranceChamber(BlockPos target, BlockPos entrance) {
        return Math.abs(target.getX() - entrance.getX()) <= CHAMBER_RADIUS
                && Math.abs(target.getZ() - entrance.getZ()) <= CHAMBER_RADIUS
                && target.getY() >= entrance.getY()
                && target.getY() < entrance.getY() + EarthLabyrinthDimensions.CHAMBER_HEIGHT;
    }

    static List<BlockPos> entrancePath(
            BlockPos connection,
            Direction egressDirection,
            BlockPos entrance
    ) {
        List<BlockPos> path = new ArrayList<>();
        BlockPos.MutableBlockPos cursor = connection.mutable();
        path.add(cursor.immutable());

        for (int step = 0; step < 3; step++) {
            addStairPathStep(path, cursor, egressDirection, entrance.getY());
        }

        int directDistance = horizontalDistance(cursor, entrance);
        int remainingDrop = cursor.getY() - entrance.getY();
        int extraHorizontalSteps = Math.max(0, remainingDrop - directDistance);
        boolean alignXFirst = true;

        if (extraHorizontalSteps > 0) {
            Direction detourDirection = detourDirection(cursor, entrance);
            int detourAxisDistance = detourDirection.getAxis() == Direction.Axis.X
                    ? Math.abs(cursor.getX() - entrance.getX())
                    : Math.abs(cursor.getZ() - entrance.getZ());
            int detourDistance = detourAxisDistance + (extraHorizontalSteps + 1) / 2;
            alignXFirst = detourDirection.getAxis() == Direction.Axis.Z;

            for (int step = 0; step < detourDistance; step++) {
                addStairPathStep(path, cursor, detourDirection, entrance.getY());
            }
        }

        while (cursor.getX() != entrance.getX() || cursor.getZ() != entrance.getZ()) {
            Direction direction;

            if (alignXFirst && cursor.getX() != entrance.getX()) {
                direction = cursor.getX() < entrance.getX() ? Direction.EAST : Direction.WEST;
            } else if (!alignXFirst && cursor.getZ() != entrance.getZ()) {
                direction = cursor.getZ() < entrance.getZ() ? Direction.SOUTH : Direction.NORTH;
            } else if (cursor.getX() != entrance.getX()) {
                direction = cursor.getX() < entrance.getX() ? Direction.EAST : Direction.WEST;
            } else {
                direction = cursor.getZ() < entrance.getZ() ? Direction.SOUTH : Direction.NORTH;
            }

            addStairPathStep(path, cursor, direction, entrance.getY());
        }

        if (cursor.getY() != entrance.getY()) {
            throw new IllegalStateException("Earth Labyrinth staircase did not reach entrance depth");
        }

        return List.copyOf(path);
    }

    private static void addStairPathStep(
            List<BlockPos> path,
            BlockPos.MutableBlockPos cursor,
            Direction direction,
            int targetY
    ) {
        cursor.move(direction);

        if (cursor.getY() > targetY) {
            cursor.move(Direction.DOWN);
        }

        path.add(cursor.immutable());
    }

    private static int horizontalDistance(BlockPos first, BlockPos second) {
        return Math.abs(first.getX() - second.getX())
                + Math.abs(first.getZ() - second.getZ());
    }

    private static Direction detourDirection(BlockPos cursor, BlockPos entrance) {
        int xDistance = Math.abs(cursor.getX() - entrance.getX());
        int zDistance = Math.abs(cursor.getZ() - entrance.getZ());

        if (xDistance <= zDistance) {
            return cursor.getX() >= entrance.getX() ? Direction.WEST : Direction.EAST;
        }

        return cursor.getZ() >= entrance.getZ() ? Direction.NORTH : Direction.SOUTH;
    }

    private static Direction directionBetween(BlockPos first, BlockPos second) {
        int deltaX = second.getX() - first.getX();

        if (deltaX != 0) {
            return deltaX > 0 ? Direction.EAST : Direction.WEST;
        }

        int deltaZ = second.getZ() - first.getZ();
        return deltaZ > 0 ? Direction.SOUTH : Direction.NORTH;
    }

    private static BlockState wallState(long layoutSeed, BlockPos pos) {
        long choice = roughValue(layoutSeed, pos, 16L);

        if (pos.getY() < 0) {
            if (choice < 9L) {
                return Blocks.DEEPSLATE.defaultBlockState();
            }
            if (choice < 12L) {
                return Blocks.TUFF.defaultBlockState();
            }
            if (choice < 14L) {
                return Blocks.COBBLED_DEEPSLATE.defaultBlockState();
            }
            return choice == 14L
                    ? Blocks.MOSSY_COBBLESTONE.defaultBlockState()
                    : Blocks.ANDESITE.defaultBlockState();
        }

        if (choice < 9L) {
            return Blocks.STONE.defaultBlockState();
        }
        if (choice < 12L) {
            return Blocks.ANDESITE.defaultBlockState();
        }
        if (choice < 14L) {
            return Blocks.TUFF.defaultBlockState();
        }
        return choice == 14L
                ? Blocks.COBBLESTONE.defaultBlockState()
                : Blocks.MOSSY_COBBLESTONE.defaultBlockState();
    }

    private static long roughValue(long layoutSeed, BlockPos pos, long bound) {
        long value = layoutSeed
                ^ (long) pos.getX() * 0x9E3779B185EBCA87L
                ^ (long) pos.getY() * 0xC2B2AE3D27D4EB4FL
                ^ (long) pos.getZ() * 0x165667B19E3779F9L;
        value ^= value >>> 29;
        return Long.remainderUnsigned(value, bound);
    }

    private static void setBlock(
            WorldGenLevel level,
            BoundingBox chunkBounds,
            BlockPos pos,
            BlockState state
    ) {
        if (!chunkBounds.isInside(pos) || level.isOutsideBuildHeight(pos)) {
            return;
        }

        if (level.getBlockEntity(pos) != null) {
            return;
        }

        level.setBlock(pos, state, 2);

        if (state.is(Blocks.LADDER) && level.getChunk(pos) instanceof ProtoChunk protoChunk) {
            protoChunk.markPosForPostProcessing(pos);
        }
    }

    private record Placement(
            EarthLabyrinthLayout layout,
            int minX,
            int minZ,
            int pyramidBaseY,
            long layoutSeed
    ) {
        private static Placement centered(
                EarthLabyrinthLayout layout,
                int centerX,
                int centerZ,
                int pyramidBaseY,
                long layoutSeed
        ) {
            return new Placement(
                    layout,
                    centerX - layout.footprintWidthBlocks() / 2,
                    centerZ - layout.footprintWidthBlocks() / 2,
                    pyramidBaseY,
                    layoutSeed
            );
        }

        private BlockPos cellCenter(EarthLabyrinthLayout.Cell cell) {
            return new BlockPos(
                    minX + CHAMBER_RADIUS
                            + cell.x() * EarthLabyrinthLayout.CELL_PITCH_BLOCKS
                            + cellJitter(cell, true),
                    EarthLabyrinthDimensions.floorY(pyramidBaseY, cell.level()),
                    minZ + CHAMBER_RADIUS
                            + cell.z() * EarthLabyrinthLayout.CELL_PITCH_BLOCKS
                            + cellJitter(cell, false)
            );
        }

        private int cellJitter(EarthLabyrinthLayout.Cell cell, boolean xAxis) {
            EarthLabyrinthLayout.CellKind kind = layout.kindAt(cell);

            if (kind == EarthLabyrinthLayout.CellKind.ENTRANCE
                    || kind == EarthLabyrinthLayout.CellKind.GUARDIAN_CHAMBER) {
                return 0;
            }

            BlockPos local = new BlockPos(cell.x(), 0, cell.z());
            long salt = xAxis ? 0x243F6A8885A308D3L : 0x13198A2E03707344L;
            int jitter = (int) roughValue(layoutSeed ^ salt, local, 3L) - 1;
            int coordinate = xAxis ? cell.x() : cell.z();

            if (coordinate == 0) {
                return Math.abs(jitter);
            }
            if (coordinate == layout.sideCells() - 1) {
                return -Math.abs(jitter);
            }
            return jitter;
        }

        private List<BlockPos> tunnelPath(EarthLabyrinthLayout.Passage passage) {
            BlockPos first = cellCenter(passage.first());
            BlockPos second = cellCenter(passage.second());
            List<BlockPos> path = new ArrayList<>();
            BlockPos.MutableBlockPos cursor = first.mutable();
            path.add(cursor.immutable());

            int xDistance = Math.abs(second.getX() - first.getX());
            int zDistance = Math.abs(second.getZ() - first.getZ());
            long curveSeed = layoutSeed
                    ^ (long) passage.first().hashCode() * 0x9E3779B97F4A7C15L
                    ^ (long) passage.second().hashCode() * 0xC2B2AE3D27D4EB4FL;
            int curveOffset = (int) Long.remainderUnsigned(curveSeed, 3L) - 1;
            int bendX = (first.getX() + second.getX()) / 2;
            int bendZ = (first.getZ() + second.getZ()) / 2;

            if (xDistance >= zDistance) {
                bendZ += curveOffset;
            } else {
                bendX += curveOffset;
            }

            appendMeanderingSteps(
                    path,
                    cursor,
                    clampTunnelX(bendX),
                    clampTunnelZ(bendZ),
                    curveSeed
            );
            appendMeanderingSteps(
                    path,
                    cursor,
                    second.getX(),
                    second.getZ(),
                    curveSeed ^ 0xA4093822299F31D0L
            );

            return List.copyOf(path);
        }

        private int clampTunnelX(int x) {
            return Math.max(
                    minX + CHAMBER_RADIUS,
                    Math.min(minX + layout.footprintWidthBlocks() - 1 - CHAMBER_RADIUS, x)
            );
        }

        private int clampTunnelZ(int z) {
            return Math.max(
                    minZ + CHAMBER_RADIUS,
                    Math.min(minZ + layout.footprintWidthBlocks() - 1 - CHAMBER_RADIUS, z)
            );
        }

        private static void appendMeanderingSteps(
                List<BlockPos> path,
                BlockPos.MutableBlockPos cursor,
                int targetX,
                int targetZ,
                long pathSeed
        ) {
            while (cursor.getX() != targetX || cursor.getZ() != targetZ) {
                int xDistance = Math.abs(targetX - cursor.getX());
                int zDistance = Math.abs(targetZ - cursor.getZ());
                long choice = roughValue(pathSeed, cursor, 4L);
                Direction direction;

                if (zDistance == 0) {
                    direction = cursor.getX() < targetX ? Direction.EAST : Direction.WEST;
                } else if (xDistance == 0) {
                    direction = cursor.getZ() < targetZ ? Direction.SOUTH : Direction.NORTH;
                } else if (xDistance > zDistance) {
                    direction = choice == 0L
                            ? cursor.getZ() < targetZ ? Direction.SOUTH : Direction.NORTH
                            : cursor.getX() < targetX ? Direction.EAST : Direction.WEST;
                } else if (zDistance > xDistance) {
                    direction = choice == 0L
                            ? cursor.getX() < targetX ? Direction.EAST : Direction.WEST
                            : cursor.getZ() < targetZ ? Direction.SOUTH : Direction.NORTH;
                } else if ((choice & 1L) == 0L) {
                    direction = cursor.getX() < targetX ? Direction.EAST : Direction.WEST;
                } else {
                    direction = cursor.getZ() < targetZ ? Direction.SOUTH : Direction.NORTH;
                }

                cursor.move(direction);
                path.add(cursor.immutable());
            }
        }
    }
}
