package cz.xefensor.retold.worldgen.earth;

import com.mojang.serialization.MapCodec;
import cz.xefensor.retold.worldgen.RetoldWorldgenRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

import java.util.Optional;

/**
 * Composite replacement for newly generated Jungle Pyramids. The registry id remains
 * {@code minecraft:jungle_pyramid}, so existing saved starts retain their vanilla pieces.
 */
public final class EarthJungleTempleStructure extends Structure {
    public static final MapCodec<EarthJungleTempleStructure> CODEC =
            simpleCodec(EarthJungleTempleStructure::new);

    public EarthJungleTempleStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        if (getLowestY(
                context,
                EarthLabyrinthDimensions.PYRAMID_WIDTH,
                EarthLabyrinthDimensions.PYRAMID_DEPTH
        ) < context.chunkGenerator().getSeaLevel()) {
            return Optional.empty();
        }

        return onTopOfChunkCenter(
                context,
                Heightmap.Types.WORLD_SURFACE_WG,
                builder -> generatePieces(builder, context)
        );
    }

    private static void generatePieces(
            StructurePiecesBuilder builder,
            GenerationContext context
    ) {
        ChunkPos chunkPos = context.chunkPos();
        EarthJungleTemplePiece temple = new EarthJungleTemplePiece(
                context.random(),
                chunkPos.getMinBlockX(),
                chunkPos.getMinBlockZ()
        );
        int pyramidBaseY = averageGroundHeight(context, temple.getBoundingBox());
        temple.moveToGround(pyramidBaseY);

        long layoutSeed = EarthLabyrinthPlanner.seedForPyramid(
                context.seed(),
                chunkPos.x(),
                chunkPos.z()
        );
        BlockPos connection = temple.labyrinthConnectionPosition();
        var egressDirection = temple.labyrinthEgressDirection();

        builder.addPiece(temple);
        builder.addPiece(new EarthLabyrinthPiece(
                temple.getBoundingBox().getCenter().getX(),
                temple.getBoundingBox().getCenter().getZ(),
                pyramidBaseY,
                connection,
                egressDirection,
                layoutSeed
        ));
    }

    private static int averageGroundHeight(
            GenerationContext context,
            BoundingBox pyramidBounds
    ) {
        long total = 0L;
        int count = 0;

        for (int z = pyramidBounds.minZ(); z <= pyramidBounds.maxZ(); z++) {
            for (int x = pyramidBounds.minX(); x <= pyramidBounds.maxX(); x++) {
                total += context.chunkGenerator().getFirstOccupiedHeight(
                        x,
                        z,
                        Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        context.heightAccessor(),
                        context.randomState()
                );
                count++;
            }
        }

        return Math.toIntExact(total / count);
    }

    @Override
    public StructureType<?> type() {
        return RetoldWorldgenRegistries.EARTH_JUNGLE_TEMPLE_STRUCTURE.get();
    }
}
