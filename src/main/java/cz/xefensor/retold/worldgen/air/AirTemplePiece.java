package cz.xefensor.retold.worldgen.air;

import cz.xefensor.retold.worldgen.RetoldWorldgenRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

public class AirTemplePiece extends StructurePiece {
    private static final int HORIZONTAL_RADIUS = 40;
    private static final int MAX_ISLAND_HEIGHT = 24;

    private final int centerX;
    private final int centerZ;
    private final int groundY;
    private final int islandY;

    public AirTemplePiece(int centerX, int centerZ, int groundY, int islandY) {
        super(
                RetoldWorldgenRegistries.AIR_TEMPLE_PIECE.get(),
                0,
                new BoundingBox(
                        centerX - HORIZONTAL_RADIUS,
                        Math.max(-64, groundY - 6),
                        centerZ - HORIZONTAL_RADIUS,
                        centerX + HORIZONTAL_RADIUS,
                        islandY + MAX_ISLAND_HEIGHT,
                        centerZ + HORIZONTAL_RADIUS
                )
        );

        this.centerX = centerX;
        this.centerZ = centerZ;
        this.groundY = groundY;
        this.islandY = islandY;
    }

    public AirTemplePiece(CompoundTag tag) {
        super(RetoldWorldgenRegistries.AIR_TEMPLE_PIECE.get(), tag);

        this.centerX = tag.getIntOr("CenterX", this.boundingBox.getCenter().getX());
        this.centerZ = tag.getIntOr("CenterZ", this.boundingBox.getCenter().getZ());
        this.groundY = tag.getIntOr("GroundY", this.boundingBox.minY() + 6);
        this.islandY = tag.getIntOr("IslandY", this.boundingBox.maxY() - MAX_ISLAND_HEIGHT);
    }

    @Override
    protected void addAdditionalSaveData(
            StructurePieceSerializationContext context,
            CompoundTag tag
    ) {
        tag.putInt("CenterX", centerX);
        tag.putInt("CenterZ", centerZ);
        tag.putInt("GroundY", groundY);
        tag.putInt("IslandY", islandY);
    }

    @Override
    public void postProcess(
            WorldGenLevel level,
            StructureManager structureManager,
            ChunkGenerator generator,
            RandomSource random,
            BoundingBox chunkBB,
            ChunkPos chunkPos,
            BlockPos referencePos
    ) {
        AirTempleWindEvents.rememberGeneratedTemple(centerX, centerZ, islandY);

        generateCrater(level, chunkBB);
        generateMainIsland(level, chunkBB);
        generateSatellite(level, chunkBB, centerX + 28, centerZ + 6, islandY - 2, 7);
        generateSatellite(level, chunkBB, centerX - 24, centerZ + 22, islandY + 1, 6);
        generateSatellite(level, chunkBB, centerX + 10, centerZ - 30, islandY + 3, 5);
        generateTemplePlaceholder(level, chunkBB);
    }

    private void generateCrater(WorldGenLevel level, BoundingBox chunkBB) {
        int radius = 18;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double distance = Math.sqrt(dx * dx + dz * dz);

                if (distance > radius) {
                    continue;
                }

                int x = centerX + dx;
                int z = centerZ + dz;
                int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;

                if (y < level.getMinY()) {
                    continue;
                }

                BlockState state;

                if (distance > 12) {
                    state = Blocks.COARSE_DIRT.defaultBlockState();
                } else if (distance > 7) {
                    state = Blocks.GRAVEL.defaultBlockState();
                } else {
                    state = Blocks.STONE.defaultBlockState();
                }

                place(level, chunkBB, new BlockPos(x, y, z), state);
            }
        }
    }

    private void generateMainIsland(WorldGenLevel level, BoundingBox chunkBB) {
        int radius = 18;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double normalized = Math.sqrt(dx * dx + dz * dz) / radius;

                if (normalized > 1.0D) {
                    continue;
                }

                int depth = Math.max(1, (int) Math.round((1.0D - normalized) * 7.0D));
                int x = centerX + dx;
                int z = centerZ + dz;

                for (int y = islandY - depth; y <= islandY; y++) {
                    BlockState state = y == islandY
                            ? Blocks.STONE_BRICKS.defaultBlockState()
                            : Blocks.STONE.defaultBlockState();

                    place(level, chunkBB, new BlockPos(x, y, z), state);
                }
            }
        }
    }

    private void generateSatellite(
            WorldGenLevel level,
            BoundingBox chunkBB,
            int satelliteX,
            int satelliteZ,
            int satelliteY,
            int radius
    ) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double normalized = Math.sqrt(dx * dx + dz * dz) / radius;

                if (normalized > 1.0D) {
                    continue;
                }

                int depth = Math.max(1, (int) Math.round((1.0D - normalized) * 4.0D));
                int x = satelliteX + dx;
                int z = satelliteZ + dz;

                for (int y = satelliteY - depth; y <= satelliteY; y++) {
                    BlockState state = y == satelliteY
                            ? cutCopper(WeatheringCopper.WeatherState.UNAFFECTED)
                            : Blocks.STONE.defaultBlockState();

                    place(level, chunkBB, new BlockPos(x, y, z), state);
                }
            }
        }
    }

    private void generateTemplePlaceholder(WorldGenLevel level, BoundingBox chunkBB) {
        BlockState wall = Blocks.STONE_BRICKS.defaultBlockState();
        BlockState accent = copperBlock(WeatheringCopper.WeatherState.OXIDIZED);
        BlockState floor = cutCopper(WeatheringCopper.WeatherState.UNAFFECTED);

        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                place(level, chunkBB, new BlockPos(centerX + dx, islandY + 1, centerZ + dz), floor);
            }
        }

        for (int y = islandY + 2; y <= islandY + 7; y++) {
            for (int d = -6; d <= 6; d++) {
                BlockState northSouth = Math.abs(d) == 6 || y == islandY + 7 ? accent : wall;
                BlockState eastWest = Math.abs(d) == 6 || y == islandY + 7 ? accent : wall;

                place(level, chunkBB, new BlockPos(centerX + d, y, centerZ - 6), northSouth);
                place(level, chunkBB, new BlockPos(centerX + d, y, centerZ + 6), northSouth);
                place(level, chunkBB, new BlockPos(centerX - 6, y, centerZ + d), eastWest);
                place(level, chunkBB, new BlockPos(centerX + 6, y, centerZ + d), eastWest);
            }
        }

        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                place(level, chunkBB, new BlockPos(centerX + dx, islandY + 7, centerZ + dz), wall);
            }
        }

        for (int y = islandY + 2; y <= islandY + 4; y++) {
            place(level, chunkBB, new BlockPos(centerX, y, centerZ - 6), Blocks.AIR.defaultBlockState());
            place(level, chunkBB, new BlockPos(centerX + 1, y, centerZ - 6), Blocks.AIR.defaultBlockState());
        }

        place(level, chunkBB, new BlockPos(centerX, islandY + 2, centerZ), lightningRod());
    }

    private static BlockState copperBlock(WeatheringCopper.WeatherState state) {
        return Blocks.COPPER_BLOCK.weathering().pick(state).defaultBlockState();
    }

    private static BlockState cutCopper(WeatheringCopper.WeatherState state) {
        return Blocks.CUT_COPPER.weathering().pick(state).defaultBlockState();
    }

    private static BlockState lightningRod() {
        return Blocks.LIGHTNING_ROD.weathering()
                .pick(WeatheringCopper.WeatherState.UNAFFECTED)
                .defaultBlockState();
    }

    private static void place(
            WorldGenLevel level,
            BoundingBox chunkBB,
            BlockPos pos,
            BlockState state
    ) {
        if (chunkBB.isInside(pos)) {
            level.setBlock(pos, state, 2);
        }
    }
}
