package cz.xefensor.retold.aender.generation;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class AenderChunkGenerator extends ChunkGenerator {
    public static final MapCodec<AenderChunkGenerator> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    BiomeSource.CODEC
                            .fieldOf("biome_source")
                            .forGetter(generator -> generator.biomeSource)
            ).apply(instance, AenderChunkGenerator::new));

    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final BlockState END_STONE = Blocks.END_STONE.defaultBlockState();

    public AenderChunkGenerator(BiomeSource biomeSource) {
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
        AenderVolatility.retainForChunk(chunk);
        generateChunk(chunk);
        return CompletableFuture.completedFuture(chunk);
    }

    public static void generateChunk(ChunkAccess chunk) {
        generateChunk(chunk, false);
    }

    public static void regenerateLoadedChunk(ChunkAccess chunk) {
        generateChunk(chunk, true);
    }

    private static void generateChunk(ChunkAccess chunk, boolean clearFirst) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        int chunkMinX = chunk.getPos().getMinBlockX();
        int chunkMaxX = chunkMinX + 15;
        int chunkMinZ = chunk.getPos().getMinBlockZ();
        int chunkMaxZ = chunkMinZ + 15;

        if (clearFirst) {
            for (int localX = 0; localX < 16; localX++) {
                for (int localZ = 0; localZ < 16; localZ++) {
                    int x = chunkMinX + localX;
                    int z = chunkMinZ + localZ;

                    for (int y = AenderIslandSampler.MIN_Y; y < AenderIslandSampler.MAX_Y; y++) {
                        pos.set(x, y, z);

                        if (!chunk.getBlockState(pos).isAir()) {
                            chunk.setBlockState(pos, AIR, 0);
                        }
                    }
                }
            }
        }

        List<AenderIslandSampler.Island> islands = AenderIslandSampler.islandsForChunk(chunk);

        for (AenderIslandSampler.Island island : islands) {
            int minX = Math.max(chunkMinX, island.minX());
            int maxX = Math.min(chunkMaxX, island.maxX());
            int minY = island.minY();
            int maxY = island.maxY();
            int minZ = Math.max(chunkMinZ, island.minZ());
            int maxZ = Math.min(chunkMaxZ, island.maxZ());

            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    AenderIslandSampler.Island.Column column = island.columnAt(x, z);

                    if (column.empty()) {
                        continue;
                    }

                    int columnMinY = Math.max(minY, column.minY());
                    int columnMaxY = Math.min(maxY, column.maxY());

                    for (int y = columnMinY; y <= columnMaxY; y++) {
                        pos.set(x, y, z);
                        chunk.setBlockState(pos, END_STONE, 0);
                    }
                }
            }
        }

        Heightmap.primeHeightmaps(
                chunk,
                EnumSet.of(
                        Heightmap.Types.WORLD_SURFACE_WG,
                        Heightmap.Types.OCEAN_FLOOR_WG,
                        Heightmap.Types.MOTION_BLOCKING
                )
        );

        AenderVolatility.markGenerated(chunk);
    }

    @Override
    public int getBaseHeight(
            int x,
            int z,
            Heightmap.Types type,
            LevelHeightAccessor level,
            RandomState random
    ) {
        for (int y = AenderIslandSampler.MAX_Y - 1; y >= AenderIslandSampler.MIN_Y; y -= 4) {
            if (AenderIslandSampler.density(x, y, z) > 0.0D) {
                return Math.min(y + 5, AenderIslandSampler.MAX_Y);
            }
        }

        return AenderIslandSampler.MIN_Y;
    }

    @Override
    public NoiseColumn getBaseColumn(
            int x,
            int z,
            LevelHeightAccessor level,
            RandomState random
    ) {
        BlockState[] states = new BlockState[AenderIslandSampler.HEIGHT];

        for (int i = 0; i < states.length; i++) {
            int y = AenderIslandSampler.MIN_Y + i;
            states[i] = AenderIslandSampler.density(x, y, z) > 0.0D ? END_STONE : AIR;
        }

        return new NoiseColumn(AenderIslandSampler.MIN_Y, states);
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
        return AenderIslandSampler.MIN_Y;
    }

    @Override
    public int getGenDepth() {
        return AenderIslandSampler.HEIGHT;
    }

    @Override
    public int getSeaLevel() {
        return 0;
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState random, BlockPos pos) {
        info.add("Retold Aender volatile terrain");
        info.add("Aender retained chunks: " + AenderVolatility.retainedChunkCount());
        info.add("Aender active regions: " + AenderVolatility.activeRegionCount());
        info.add("Aender generated chunks: " + AenderVolatility.generatedThisSessionCount());
    }
}