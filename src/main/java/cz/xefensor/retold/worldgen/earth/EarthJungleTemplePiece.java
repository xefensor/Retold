package cz.xefensor.retold.worldgen.earth;

import cz.xefensor.retold.worldgen.RetoldWorldgenRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.structure.structures.JungleTemplePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

/** Vanilla Jungle Temple generation with a fixed precomputed ground height and Retold piece id. */
public final class EarthJungleTemplePiece extends JungleTemplePiece {
    EarthJungleTemplePiece(RandomSource random, int west, int north) {
        super(random, west, north);
    }

    public EarthJungleTemplePiece(CompoundTag tag) {
        super(tag);
    }

    void moveToGround(int groundY) {
        this.boundingBox = this.boundingBox.moved(0, groundY - this.boundingBox.minY(), 0);
        this.heightPosition = groundY;
    }

    BlockPos labyrinthConnectionPosition() {
        return this.getWorldPos(
                EarthLabyrinthDimensions.CONNECTION_LOCAL_X,
                EarthLabyrinthDimensions.CONNECTION_LOCAL_Y,
                EarthLabyrinthDimensions.CONNECTION_LOCAL_Z
        ).immutable();
    }

    Direction labyrinthEgressDirection() {
        BlockPos connection = labyrinthConnectionPosition();
        BlockPos next = this.getWorldPos(
                EarthLabyrinthDimensions.CONNECTION_LOCAL_X + 1,
                EarthLabyrinthDimensions.CONNECTION_LOCAL_Y,
                EarthLabyrinthDimensions.CONNECTION_LOCAL_Z
        );
        int deltaX = next.getX() - connection.getX();
        int deltaZ = next.getZ() - connection.getZ();

        if (deltaX != 0) {
            return deltaX > 0 ? Direction.EAST : Direction.WEST;
        }

        return deltaZ > 0 ? Direction.SOUTH : Direction.NORTH;
    }

    @Override
    public StructurePieceType getType() {
        return RetoldWorldgenRegistries.EARTH_JUNGLE_TEMPLE_PIECE.get();
    }
}
