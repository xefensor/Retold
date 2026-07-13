package cz.xefensor.retold.worldgen.air;

import cz.xefensor.retold.worldgen.RetoldWorldgenRegistries;
import cz.xefensor.retold.worldgen.air.wind.AirTempleWindEvents;
import cz.xefensor.retold.worldgen.air.wind.AirTempleWindSource;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

public class AirTemplePiece extends StructurePiece {
    private final int centerX;
    private final int centerZ;
    private final int groundY;
    private final int islandY;
    private final AirTemplePaletteKind paletteKind;

    public AirTemplePiece(
            int centerX,
            int centerZ,
            int groundY,
            int islandY,
            AirTemplePaletteKind paletteKind
    ) {
        super(
                RetoldWorldgenRegistries.AIR_TEMPLE_PIECE.get(),
                0,
                createBoundingBox(centerX, centerZ, groundY, islandY)
        );

        this.centerX = centerX;
        this.centerZ = centerZ;
        this.groundY = groundY;
        this.islandY = islandY;
        this.paletteKind = paletteKind;
    }

    public AirTemplePiece(CompoundTag tag) {
        super(RetoldWorldgenRegistries.AIR_TEMPLE_PIECE.get(), tag);

        this.centerX = tag.getIntOr("CenterX", this.boundingBox.getCenter().getX());
        this.centerZ = tag.getIntOr("CenterZ", this.boundingBox.getCenter().getZ());
        this.groundY = tag.getIntOr("GroundY", this.boundingBox.minY() + AirTempleDimensions.MAX_ISLAND_DEPTH);
        this.islandY = tag.getIntOr("IslandY", this.boundingBox.maxY() - AirTempleDimensions.TOWER_HEIGHT);
        this.paletteKind = AirTemplePaletteKind.bySerializedId(tag.getIntOr("PaletteKind", 0));
    }

    private static BoundingBox createBoundingBox(int centerX, int centerZ, int groundY, int islandY) {
        return new BoundingBox(
                centerX - AirTempleDimensions.HORIZONTAL_RADIUS,
                Math.min(Math.max(-64, groundY - AirTempleDimensions.CRATER_MAX_DEPTH - 3), AirTempleDimensions.windMinY(islandY)),
                centerZ - AirTempleDimensions.HORIZONTAL_RADIUS,
                centerX + AirTempleDimensions.HORIZONTAL_RADIUS,
                AirTempleDimensions.windMaxY(islandY),
                centerZ + AirTempleDimensions.HORIZONTAL_RADIUS
        );
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("CenterX", centerX);
        tag.putInt("CenterZ", centerZ);
        tag.putInt("GroundY", groundY);
        tag.putInt("IslandY", islandY);
        tag.putInt("PaletteKind", paletteKind.ordinal());
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
        AirTemplePalette palette = new AirTemplePalette(paletteKind);

        AirTempleIslandGenerator.generate(level, chunkBB, centerX, centerZ, groundY, islandY, palette);
        AirTempleTowerGenerator.generate(level, chunkBB, centerX, centerZ, islandY);
        AirTempleWindEvents.rememberGeneratedTemple(
                AirTempleWindSource.fromTemple(centerX, centerZ, islandY)
        );
    }
}
