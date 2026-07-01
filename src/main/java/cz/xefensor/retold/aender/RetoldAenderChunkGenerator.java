package cz.xefensor.retold.aender;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class RetoldAenderChunkGenerator extends ChunkGenerator {
    public static final MapCodec<RetoldAenderChunkGenerator> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    BiomeSource.CODEC
                            .fieldOf("biome_source")
                            .forGetter(generator -> generator.biomeSource)
            ).apply(instance, RetoldAenderChunkGenerator::new));

    public RetoldAenderChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(
            Blender blender,
            RandomState randomState,
            StructureManager structureManager,
            ChunkAccess chunk
    ) {
        RetoldAenderTerrainBuilder.generateInitialFloatingIslands(
                chunk,
                0L
        );

        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public int getBaseHeight(
            int x,
            int z,
            Heightmap.Types type,
            LevelHeightAccessor level,
            RandomState random
    ) {
        for (int y = RetoldAenderTerrainBuilder.MAX_Y - 1;
             y >= RetoldAenderTerrainBuilder.MIN_Y;
             y--) {
            BlockState state =
                    RetoldAenderTerrainBuilder.getBaseBlockStateAt(
                            x,
                            y,
                            z,
                            0L
                    );

            if (!state.isAir()) {
                return y + 1;
            }
        }

        return RetoldAenderTerrainBuilder.MIN_Y;
    }

    @Override
    public NoiseColumn getBaseColumn(
            int x,
            int z,
            LevelHeightAccessor height,
            RandomState random
    ) {
        BlockState[] states =
                new BlockState[RetoldAenderTerrainBuilder.HEIGHT];

        for (int index = 0; index < states.length; index++) {
            int y = RetoldAenderTerrainBuilder.MIN_Y + index;

            states[index] =
                    RetoldAenderTerrainBuilder.getBaseBlockStateAt(
                            x,
                            y,
                            z,
                            0L
                    );
        }

        return new NoiseColumn(
                RetoldAenderTerrainBuilder.MIN_Y,
                states
        );
    }

    @Override
    public void buildSurface(
            WorldGenRegion level,
            StructureManager structureManager,
            RandomState random,
            ChunkAccess chunk
    ) {
    }

    @Override
    public void applyCarvers(
            WorldGenRegion level,
            long seed,
            RandomState random,
            BiomeManager biomeManager,
            StructureManager structureManager,
            ChunkAccess chunk
    ) {
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion level) {
    }

    @Override
    public int getSpawnHeight(LevelHeightAccessor level) {
        return 96;
    }

    @Override
    public int getMinY() {
        return RetoldAenderTerrainBuilder.MIN_Y;
    }

    @Override
    public int getGenDepth() {
        return RetoldAenderTerrainBuilder.HEIGHT;
    }

    @Override
    public int getSeaLevel() {
        return 0;
    }

    @Override
    public void addDebugScreenInfo(
            List<String> info,
            RandomState random,
            BlockPos pos
    ) {
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;

        info.add("Retold Aender island chunk: "
                + chunkX
                + ", "
                + chunkZ);
    }
}