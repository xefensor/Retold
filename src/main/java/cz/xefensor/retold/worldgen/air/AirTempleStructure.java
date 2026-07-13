package cz.xefensor.retold.worldgen.air;

import com.mojang.serialization.MapCodec;
import cz.xefensor.retold.worldgen.RetoldWorldgenRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

import java.util.Optional;

public class AirTempleStructure extends Structure {
    public static final MapCodec<AirTempleStructure> CODEC = simpleCodec(AirTempleStructure::new);

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
        Holder<Biome> centerBiome = context.biomeSource().getNoiseBiome(
                QuartPos.fromBlock(centerX),
                QuartPos.fromBlock(groundY),
                QuartPos.fromBlock(centerZ),
                context.randomState().sampler()
        );

        if (!context.validBiome().test(centerBiome)) {
            return Optional.empty();
        }

        int maxIslandY = context.heightAccessor().getMaxY()
                - AirTempleDimensions.TOWER_HEIGHT
                - AirTempleDimensions.WIND_ABOVE_TOWER
                - AirTempleDimensions.TOP_CLEARANCE;
        int islandY = Math.min(
                maxIslandY,
                Math.max(AirTempleDimensions.MIN_ISLAND_Y, groundY + AirTempleDimensions.SURFACE_CLEARANCE)
        );

        if (islandY <= groundY + 32) {
            return Optional.empty();
        }

        AirTemplePaletteKind paletteKind = AirTemplePaletteKind.fromCenterBiome(centerBiome);
        BlockPos startPos = new BlockPos(centerX, islandY, centerZ);

        return Optional.of(new GenerationStub(
                startPos,
                builder -> generatePieces(builder, centerX, centerZ, groundY, islandY, paletteKind)
        ));
    }

    private static void generatePieces(
            StructurePiecesBuilder builder,
            int centerX,
            int centerZ,
            int groundY,
            int islandY,
            AirTemplePaletteKind paletteKind
    ) {
        builder.addPiece(new AirTemplePiece(centerX, centerZ, groundY, islandY, paletteKind));
    }

    @Override
    public StructureType<?> type() {
        return RetoldWorldgenRegistries.AIR_TEMPLE_STRUCTURE.get();
    }
}
