package cz.xefensor.retold.worldgen.air;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

final class AirTempleTowerGenerator {
    private static final int FLOOR_RADIUS = 8;
    private static final int[] FLOOR_OFFSETS = {2, 9, 16, 23, 30};
    private static final int[][] CORNER_PILLARS = {
            {-7, -7}, {-7, 7}, {7, -7}, {7, 7}
    };
    private static final int[][] SIDE_PILLARS = {
            {-8, 0}, {8, 0}, {0, -8}, {0, 8}
    };

    private AirTempleTowerGenerator() {
    }

    static void generate(WorldGenLevel level, BoundingBox chunkBB, int centerX, int centerZ, int islandY) {
        BlockState floor = Blocks.TUFF_BRICKS.defaultBlockState();
        BlockState underside = Blocks.POLISHED_TUFF.defaultBlockState();
        BlockState pillar = Blocks.POLISHED_TUFF.defaultBlockState();
        BlockState chiseled = Blocks.CHISELED_TUFF.defaultBlockState();
        BlockState chiseledBricks = Blocks.CHISELED_TUFF_BRICKS.defaultBlockState();
        BlockState grate = AirTempleBlocks.copperGrate(WeatheringCopper.WeatherState.EXPOSED);
        BlockState bulb = AirTempleBlocks.copperBulb(WeatheringCopper.WeatherState.UNAFFECTED);
        BlockState accent = AirTempleBlocks.cutCopper(WeatheringCopper.WeatherState.EXPOSED);

        generateBase(level, chunkBB, centerX, centerZ, islandY, floor, pillar, accent);

        for (int floorOffset : FLOOR_OFFSETS) {
            int y = islandY + floorOffset;
            generateFloor(level, chunkBB, centerX, centerZ, y, floor, underside, grate, accent);
            generateFloorDetails(level, chunkBB, centerX, centerZ, y, chiseledBricks, bulb);
        }

        generatePillars(level, chunkBB, centerX, centerZ, islandY + 2, AirTempleDimensions.towerTopY(islandY), pillar, chiseled);
        generateBraces(level, chunkBB, centerX, centerZ, islandY, pillar);
        generateTopCrown(level, chunkBB, centerX, centerZ, AirTempleDimensions.towerTopY(islandY), pillar, accent, bulb);
    }

    private static void generateBase(
            WorldGenLevel level,
            BoundingBox chunkBB,
            int centerX,
            int centerZ,
            int islandY,
            BlockState floor,
            BlockState pillar,
            BlockState accent
    ) {
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                if (Math.abs(dx) + Math.abs(dz) > 8) {
                    continue;
                }

                AirTempleBlocks.place(level, chunkBB, new BlockPos(centerX + dx, islandY + 1, centerZ + dz), floor);
            }
        }

        for (int[] pillarPos : CORNER_PILLARS) {
            AirTempleBlocks.place(level, chunkBB, new BlockPos(centerX + pillarPos[0], islandY + 1, centerZ + pillarPos[1]), accent);
            AirTempleBlocks.place(level, chunkBB, new BlockPos(centerX + pillarPos[0], islandY + 2, centerZ + pillarPos[1]), pillar);
        }
    }

    private static void generateFloor(
            WorldGenLevel level,
            BoundingBox chunkBB,
            int centerX,
            int centerZ,
            int y,
            BlockState floor,
            BlockState underside,
            BlockState grate,
            BlockState accent
    ) {
        for (int dx = -FLOOR_RADIUS; dx <= FLOOR_RADIUS; dx++) {
            for (int dz = -FLOOR_RADIUS; dz <= FLOOR_RADIUS; dz++) {
                int absX = Math.abs(dx);
                int absZ = Math.abs(dz);

                if (absX + absZ > 13 || absX > FLOOR_RADIUS || absZ > FLOOR_RADIUS) {
                    continue;
                }

                boolean edge = absX + absZ >= 12 || absX == FLOOR_RADIUS || absZ == FLOOR_RADIUS;
                boolean grateBand = !edge && (absX == 5 || absZ == 5) && absX + absZ > 6;
                BlockState state = edge ? accent : grateBand ? grate : floor;

                AirTempleBlocks.place(level, chunkBB, new BlockPos(centerX + dx, y, centerZ + dz), state);

                if (edge || (absX + absZ) % 4 == 0) {
                    AirTempleBlocks.place(level, chunkBB, new BlockPos(centerX + dx, y - 1, centerZ + dz), underside);
                }
            }
        }

        generateRails(level, chunkBB, centerX, centerZ, y + 1, grate, accent);
    }

    private static void generateRails(
            WorldGenLevel level,
            BoundingBox chunkBB,
            int centerX,
            int centerZ,
            int y,
            BlockState grate,
            BlockState accent
    ) {
        for (int d = -5; d <= 5; d++) {
            BlockState state = d % 3 == 0 ? accent : grate;
            AirTempleBlocks.place(level, chunkBB, new BlockPos(centerX + d, y, centerZ - 8), state);
            AirTempleBlocks.place(level, chunkBB, new BlockPos(centerX + d, y, centerZ + 8), state);
            AirTempleBlocks.place(level, chunkBB, new BlockPos(centerX - 8, y, centerZ + d), state);
            AirTempleBlocks.place(level, chunkBB, new BlockPos(centerX + 8, y, centerZ + d), state);
        }
    }

    private static void generatePillars(
            WorldGenLevel level,
            BoundingBox chunkBB,
            int centerX,
            int centerZ,
            int minY,
            int maxY,
            BlockState pillar,
            BlockState chiseled
    ) {
        for (int[] pillarPos : CORNER_PILLARS) {
            generateVerticalPillar(level, chunkBB, centerX + pillarPos[0], centerZ + pillarPos[1], minY, maxY, pillar, chiseled, 7);
        }

        for (int[] pillarPos : SIDE_PILLARS) {
            generateVerticalPillar(level, chunkBB, centerX + pillarPos[0], centerZ + pillarPos[1], minY + 1, maxY - 4, pillar, chiseled, 11);
        }

        generateVerticalPillar(level, chunkBB, centerX, centerZ, minY, maxY - 2, pillar, chiseled, 7);
    }

    private static void generateVerticalPillar(
            WorldGenLevel level,
            BoundingBox chunkBB,
            int x,
            int z,
            int minY,
            int maxY,
            BlockState pillar,
            BlockState chiseled,
            int detailInterval
    ) {
        for (int y = minY; y <= maxY; y++) {
            BlockState state = (y - minY) % detailInterval == 0 ? chiseled : pillar;
            AirTempleBlocks.place(level, chunkBB, new BlockPos(x, y, z), state);
        }
    }

    private static void generateBraces(
            WorldGenLevel level,
            BoundingBox chunkBB,
            int centerX,
            int centerZ,
            int islandY,
            BlockState pillar
    ) {
        for (int i = 0; i < FLOOR_OFFSETS.length - 1; i++) {
            int lowerY = islandY + FLOOR_OFFSETS[i] + 2;
            int upperY = islandY + FLOOR_OFFSETS[i + 1] - 1;
            int height = Math.max(1, upperY - lowerY);

            for (int step = 0; step <= height; step++) {
                int y = lowerY + step;
                int inset = Math.min(3, step / 2);

                AirTempleBlocks.place(level, chunkBB, new BlockPos(centerX - 7 + inset, y, centerZ - 7), pillar);
                AirTempleBlocks.place(level, chunkBB, new BlockPos(centerX + 7 - inset, y, centerZ + 7), pillar);
                AirTempleBlocks.place(level, chunkBB, new BlockPos(centerX - 7, y, centerZ + 7 - inset), pillar);
                AirTempleBlocks.place(level, chunkBB, new BlockPos(centerX + 7, y, centerZ - 7 + inset), pillar);
            }
        }
    }

    private static void generateFloorDetails(
            WorldGenLevel level,
            BoundingBox chunkBB,
            int centerX,
            int centerZ,
            int y,
            BlockState chiseledBricks,
            BlockState bulb
    ) {
        AirTempleBlocks.place(level, chunkBB, new BlockPos(centerX, y + 1, centerZ), chiseledBricks);

        for (int[] pos : SIDE_PILLARS) {
            AirTempleBlocks.place(level, chunkBB, new BlockPos(centerX + pos[0], y + 2, centerZ + pos[1]), bulb);
        }
    }

    private static void generateTopCrown(
            WorldGenLevel level,
            BoundingBox chunkBB,
            int centerX,
            int centerZ,
            int topY,
            BlockState pillar,
            BlockState accent,
            BlockState bulb
    ) {
        for (int d = -6; d <= 6; d++) {
            AirTempleBlocks.place(level, chunkBB, new BlockPos(centerX + d, topY, centerZ - 6), accent);
            AirTempleBlocks.place(level, chunkBB, new BlockPos(centerX + d, topY, centerZ + 6), accent);
            AirTempleBlocks.place(level, chunkBB, new BlockPos(centerX - 6, topY, centerZ + d), accent);
            AirTempleBlocks.place(level, chunkBB, new BlockPos(centerX + 6, topY, centerZ + d), accent);

            if (Math.abs(d) <= 4) {
                AirTempleBlocks.place(level, chunkBB, new BlockPos(centerX + d, topY + 3, centerZ - 4), pillar);
                AirTempleBlocks.place(level, chunkBB, new BlockPos(centerX + d, topY + 3, centerZ + 4), pillar);
                AirTempleBlocks.place(level, chunkBB, new BlockPos(centerX - 4, topY + 3, centerZ + d), pillar);
                AirTempleBlocks.place(level, chunkBB, new BlockPos(centerX + 4, topY + 3, centerZ + d), pillar);
            }
        }

        for (int[] pillarPos : CORNER_PILLARS) {
            for (int y = topY + 1; y <= topY + 3; y++) {
                AirTempleBlocks.place(level, chunkBB, new BlockPos(centerX + pillarPos[0] / 2, y, centerZ + pillarPos[1] / 2), pillar);
            }
        }

        AirTempleBlocks.place(level, chunkBB, new BlockPos(centerX, topY + 4, centerZ), bulb);
    }
}
