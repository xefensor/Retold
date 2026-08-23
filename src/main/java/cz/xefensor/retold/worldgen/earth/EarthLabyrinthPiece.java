package cz.xefensor.retold.worldgen.earth;

import cz.xefensor.retold.worldgen.RetoldWorldgenRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

public final class EarthLabyrinthPiece extends StructurePiece {
    private final int centerX;
    private final int centerZ;
    private final int pyramidBaseY;
    private final BlockPos connection;
    private final Direction egressDirection;
    private final long layoutSeed;

    EarthLabyrinthPiece(
            int centerX,
            int centerZ,
            int pyramidBaseY,
            BlockPos connection,
            Direction egressDirection,
            long layoutSeed
    ) {
        super(
                RetoldWorldgenRegistries.EARTH_LABYRINTH_PIECE.get(),
                0,
                createBoundingBox(centerX, centerZ, pyramidBaseY, connection, layoutSeed)
        );
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.pyramidBaseY = pyramidBaseY;
        this.connection = connection.immutable();
        this.egressDirection = egressDirection;
        this.layoutSeed = layoutSeed;
    }

    public EarthLabyrinthPiece(CompoundTag tag) {
        super(RetoldWorldgenRegistries.EARTH_LABYRINTH_PIECE.get(), tag);
        this.centerX = tag.getIntOr("CenterX", this.boundingBox.getCenter().getX());
        this.centerZ = tag.getIntOr("CenterZ", this.boundingBox.getCenter().getZ());
        this.pyramidBaseY = tag.getIntOr(
                "PyramidBaseY",
                this.boundingBox.maxY() - EarthLabyrinthDimensions.CONNECTION_LOCAL_Y
        );
        this.connection = new BlockPos(
                tag.getIntOr("ConnectionX", centerX),
                tag.getIntOr("ConnectionY", this.boundingBox.maxY()),
                tag.getIntOr("ConnectionZ", centerZ)
        );
        this.egressDirection = Direction.from2DDataValue(tag.getIntOr("EgressDirection", 0));
        this.layoutSeed = tag.getLongOr("LayoutSeed", 0L);
    }

    private static BoundingBox createBoundingBox(
            int centerX,
            int centerZ,
            int pyramidBaseY,
            BlockPos connection,
            long layoutSeed
    ) {
        EarthLabyrinthLayout layout = EarthLabyrinthPlanner.generate(layoutSeed);
        int width = layout.footprintWidthBlocks();
        int minX = centerX - width / 2;
        int minZ = centerZ - width / 2;
        int minY = EarthLabyrinthDimensions.floorY(
                pyramidBaseY,
                EarthLabyrinthLayout.LEVEL_COUNT - 1
        );
        int maxY = Math.max(
                connection.getY() + 3,
                EarthLabyrinthDimensions.upperFloorY(pyramidBaseY)
                        + EarthLabyrinthDimensions.CHAMBER_HEIGHT - 1
        );
        return new BoundingBox(
                minX,
                minY,
                minZ,
                minX + width - 1,
                maxY,
                minZ + width - 1
        );
    }

    @Override
    protected void addAdditionalSaveData(
            StructurePieceSerializationContext context,
            CompoundTag tag
    ) {
        tag.putInt("CenterX", centerX);
        tag.putInt("CenterZ", centerZ);
        tag.putInt("PyramidBaseY", pyramidBaseY);
        tag.putInt("ConnectionX", connection.getX());
        tag.putInt("ConnectionY", connection.getY());
        tag.putInt("ConnectionZ", connection.getZ());
        tag.putInt("EgressDirection", egressDirection.get2DDataValue());
        tag.putLong("LayoutSeed", layoutSeed);
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
        EarthLabyrinthGenerator.generate(
                level,
                chunkBB,
                centerX,
                centerZ,
                pyramidBaseY,
                connection,
                egressDirection,
                layoutSeed
        );
    }

    long layoutSeed() {
        return layoutSeed;
    }
}
