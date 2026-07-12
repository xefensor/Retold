package cz.xefensor.retold.worldgen.air;

import com.mojang.serialization.MapCodec;
import cz.xefensor.retold.worldgen.RetoldWorldgenRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

import java.util.Optional;

public class AirTempleStructure extends Structure {
    public static final MapCodec<AirTempleStructure> CODEC =
            simpleCodec(AirTempleStructure::new);

    private static final int MIN_ISLAND_Y = 224;
    private static final int SURFACE_CLEARANCE = 96;
    private static final int TOP_CLEARANCE = 32;

    public AirTempleStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunkPos = context.chunkPos();
        int centerX = chunkPos.getMiddleBlockX();
        int centerZ = chunkPos.getMiddleBlockZ();
        int groundY = context.chunkGenerator().getFirstOccupiedHeight(
                centerX,
                centerZ,
                Heightmap.Types.WORLD_SURFACE_WG,
                context.heightAccessor(),
                context.randomState()
        );
        int maxIslandY = context.heightAccessor().getMaxY() - TOP_CLEARANCE;
        int islandY = Math.min(
                maxIslandY,
                Math.max(MIN_ISLAND_Y, groundY + SURFACE_CLEARANCE)
        );

        if (islandY <= groundY + 32) {
            return Optional.empty();
        }

        BlockPos startPos = new BlockPos(centerX, islandY, centerZ);

        return Optional.of(new GenerationStub(
                startPos,
                builder -> generatePieces(builder, centerX, centerZ, groundY, islandY)
        ));
    }

    private static void generatePieces(
            StructurePiecesBuilder builder,
            int centerX,
            int centerZ,
            int groundY,
            int islandY
    ) {
        builder.addPiece(new AirTemplePiece(centerX, centerZ, groundY, islandY));
    }

    @Override
    public StructureType<?> type() {
        return RetoldWorldgenRegistries.AIR_TEMPLE_STRUCTURE.get();
    }
}
