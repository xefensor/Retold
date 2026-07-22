package cz.xefensor.retold.aender.generation;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import cz.xefensor.retold.aender.RetoldAenderEntryPlatform;
import cz.xefensor.retold.registry.RetoldBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class AenderChunkGenerator extends ChunkGenerator {
    public static final MapCodec<AenderChunkGenerator> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    BiomeSource.CODEC
                            .fieldOf("biome_source")
                            .forGetter(generator -> generator.biomeSource)
            ).apply(instance, AenderChunkGenerator::new));

    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final BlockState WATER = Blocks.WATER.defaultBlockState();
    private static final BlockState SHORT_GRASS = Blocks.SHORT_GRASS.defaultBlockState();
    private static final BlockState FERN = Blocks.FERN.defaultBlockState();
    private static final BlockState[] FLOWERS = {
            Blocks.ALLIUM.defaultBlockState(),
            Blocks.PINK_PETALS.defaultBlockState()
    };

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

    public static void regenerateLoadedChunk(ServerLevel level, ChunkAccess chunk) {
        chunk.fillBiomesFromNoise(
                level.getChunkSource().getGenerator().getBiomeSource(),
                level.getChunkSource().randomState().sampler()
        );
        generateChunk(chunk, true);
    }

    private static void generateChunk(ChunkAccess chunk, boolean clearFirst) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        int chunkMinX = chunk.getPos().getMinBlockX();
        int chunkMaxX = chunkMinX + 15;
        int chunkMinZ = chunk.getPos().getMinBlockZ();
        int chunkMaxZ = chunkMinZ + 15;
        TerrainBlocks terrainBlocks = TerrainBlocks.create();

        if (clearFirst) {
            AenderChunkSectionEditor.clear(chunk);
        }

        List<AenderIslandSampler.Island> islands = AenderIslandSampler.islandsForChunk(chunk);
        List<CachedIsland> cachedIslands = new ArrayList<>(islands.size());

        for (AenderIslandSampler.Island island : islands) {
            cachedIslands.add(new CachedIsland(island, chunkMinX, chunkMinZ));
        }

        boolean mergedGeneration = AenderVolatility.currentGeneratorVersion()
                >= AenderRealityData.MERGED_TERRAIN_GENERATOR_VERSION;

        if (mergedGeneration) {
            generateMergedTerrain(chunk, cachedIslands, terrainBlocks, pos);
        } else {
            for (CachedIsland island : cachedIslands) {
                generateIslandTerrain(chunk, island, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ, terrainBlocks, pos);
            }
        }

        placePortalFrameDeposits(chunk, cachedIslands);

        for (CachedIsland island : cachedIslands) {
            decorateIsland(
                    chunk,
                    island,
                    cachedIslands,
                    chunkMinX,
                    chunkMaxX,
                    chunkMinZ,
                    chunkMaxZ,
                    mergedGeneration,
                    pos
            );
        }

        if (mergedGeneration) {
            placeCrossChunkLargeDecorations(chunk, cachedIslands);
        }

        RetoldAenderEntryPlatform.generateInChunk(chunk);

        AenderChunkSectionEditor.primeFreshHeightmaps(chunk);

        AenderVolatility.markGenerated(chunk);
    }

    private static void generateIslandTerrain(
            ChunkAccess chunk,
            CachedIsland island,
            int chunkMinX,
            int chunkMaxX,
            int chunkMinZ,
            int chunkMaxZ,
            TerrainBlocks terrainBlocks,
            BlockPos.MutableBlockPos pos
    ) {
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
                    chunk.setBlockState(
                            pos,
                            terrainBlockForDepth(terrainBlocks, island.seed(), x, z, columnMaxY - y),
                            0
                    );
                }
            }
        }
    }

    private static void generateMergedTerrain(
            ChunkAccess chunk,
            List<CachedIsland> islands,
            TerrainBlocks terrainBlocks,
            BlockPos.MutableBlockPos pos
    ) {
        int[] intervalMins = new int[islands.size()];
        int[] intervalMaxes = new int[islands.size()];
        long[] intervalSeeds = new long[islands.size()];
        int chunkMinX = chunk.getPos().getMinBlockX();
        int chunkMinZ = chunk.getPos().getMinBlockZ();

        for (int localX = 0; localX < 16; localX++) {
            int x = chunkMinX + localX;

            for (int localZ = 0; localZ < 16; localZ++) {
                int z = chunkMinZ + localZ;
                int intervalCount = collectTerrainIntervals(
                        islands,
                        x,
                        z,
                        intervalMins,
                        intervalMaxes,
                        intervalSeeds
                );
                int mergedCount = AenderTerrainIntervals.merge(
                        intervalMins,
                        intervalMaxes,
                        intervalSeeds,
                        intervalCount
                );

                for (int intervalIndex = 0; intervalIndex < mergedCount; intervalIndex++) {
                    int mergedMin = intervalMins[intervalIndex];
                    int mergedMax = intervalMaxes[intervalIndex];
                    long surfaceSeed = intervalSeeds[intervalIndex];

                    for (int y = mergedMin; y <= mergedMax; y++) {
                        pos.set(x, y, z);
                        chunk.setBlockState(
                                pos,
                                terrainBlockForDepth(terrainBlocks, surfaceSeed, x, z, mergedMax - y),
                                0
                        );
                    }
                }
            }
        }
    }

    private static int collectTerrainIntervals(
            List<CachedIsland> islands,
            int x,
            int z,
            int[] intervalMins,
            int[] intervalMaxes,
            long[] intervalSeeds
    ) {
        int count = 0;

        for (CachedIsland island : islands) {
            AenderIslandSampler.Island.Column column = island.columnAt(x, z);

            if (column.empty()) {
                continue;
            }

            intervalMins[count] = column.minY();
            intervalMaxes[count] = column.maxY();
            intervalSeeds[count] = island.seed();
            count++;
        }

        return count;
    }

    private static void placePortalFrameDeposits(ChunkAccess chunk, List<CachedIsland> islands) {
        final int cellSize = 32;
        final int clusterRadius = 2;
        int chunkMinX = chunk.getPos().getMinBlockX();
        int chunkMaxX = chunkMinX + 15;
        int chunkMinZ = chunk.getPos().getMinBlockZ();
        int chunkMaxZ = chunkMinZ + 15;
        int minCellX = Math.floorDiv(chunkMinX - clusterRadius, cellSize);
        int maxCellX = Math.floorDiv(chunkMaxX + clusterRadius, cellSize);
        int minCellZ = Math.floorDiv(chunkMinZ - clusterRadius, cellSize);
        int maxCellZ = Math.floorDiv(chunkMaxZ + clusterRadius, cellSize);
        BlockState frame = RetoldBlocks.DEV_AENDER_PORTAL_FRAME.get().defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (CachedIsland island : islands) {
            for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
                for (int cellZ = minCellZ; cellZ <= maxCellZ; cellZ++) {
                    long depositSeed = mix64(
                            island.seed()
                                    ^ (long) cellX * 0xD1342543DE82EF95L
                                    ^ (long) cellZ * 0xC6BC279692B5CC83L
                                    ^ 0x504F5254414C4652L
                    );

                    if (unit(depositSeed) >= 0.52D) {
                        continue;
                    }

                    int centerX = cellX * cellSize + 5 + (int) (unit(depositSeed ^ 0x11L) * 22.0D);
                    int centerZ = cellZ * cellSize + 5 + (int) (unit(depositSeed ^ 0x12L) * 22.0D);
                    AenderIslandSampler.Island.Column column = island.columnAt(centerX, centerZ);

                    if (column.empty() || column.maxY() - column.minY() < 12) {
                        continue;
                    }

                    int verticalRange = column.maxY() - column.minY() - 9;
                    int centerY = column.minY() + 4
                            + (int) (unit(depositSeed ^ 0x13L) * Math.max(1, verticalRange));

                    for (int dx = -clusterRadius; dx <= clusterRadius; dx++) {
                        for (int dy = -clusterRadius; dy <= clusterRadius; dy++) {
                            for (int dz = -clusterRadius; dz <= clusterRadius; dz++) {
                                int taxicabDistance = Math.abs(dx) + Math.abs(dy) + Math.abs(dz);

                                if (taxicabDistance > clusterRadius) {
                                    continue;
                                }

                                long blockSeed = depositSeed
                                        ^ (long) dx * 0x9E3779B97F4A7C15L
                                        ^ (long) dy * 0xC2B2AE3D27D4EB4FL
                                        ^ (long) dz * 0x632BE59BD9B4E019L;

                                if (taxicabDistance > 0 && unit(blockSeed) < 0.18D) {
                                    continue;
                                }

                                int x = centerX + dx;
                                int y = centerY + dy;
                                int z = centerZ + dz;

                                if (x < chunkMinX || x > chunkMaxX || z < chunkMinZ || z > chunkMaxZ) {
                                    continue;
                                }

                                pos.set(x, y, z);

                                if (chunk.getBlockState(pos).is(RetoldBlocks.AENDER_STONE)) {
                                    chunk.setBlockState(pos, frame, 0);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static void decorateIsland(
            ChunkAccess chunk,
            CachedIsland island,
            List<CachedIsland> islands,
            int chunkMinX,
            int chunkMaxX,
            int chunkMinZ,
            int chunkMaxZ,
            boolean mergedGeneration,
            BlockPos.MutableBlockPos pos
    ) {
        int minX = Math.max(chunkMinX, island.minX());
        int maxX = Math.min(chunkMaxX, island.maxX());
        int minZ = Math.max(chunkMinZ, island.minZ());
        int maxZ = Math.min(chunkMaxZ, island.maxZ());

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                AenderIslandSampler.Island.Column column = island.columnAt(x, z);

                if (column.empty()) {
                    continue;
                }

                int surfaceY = column.maxY();

                if (surfaceY + 1 >= AenderIslandSampler.MAX_Y) {
                    continue;
                }

                if (mergedGeneration
                        && !isExposedSurfaceOwner(islands, island, x, z, surfaceY)) {
                    continue;
                }

                pos.set(x, surfaceY, z);

                if (mergedGeneration
                        && (!isSurfaceBlock(chunk.getBlockState(pos), island.biome())
                        || !chunk.getBlockState(pos.above()).isAir())) {
                    continue;
                }

                LakeColumn lake = lakeColumnAt(island, x, z, surfaceY);

                if (lake != null && carveLakeColumn(chunk, column, x, z, surfaceY, lake, pos)) {
                    continue;
                }

                placeUndersideSpur(chunk, column, island, x, z, mergedGeneration, pos);
                placeUndersideGrowth(chunk, column, island, x, z, pos);

                pos.set(x, surfaceY, z);

                if (!isSurfaceBlock(chunk.getBlockState(pos), island.biome())) {
                    continue;
                }

                pos.set(x, surfaceY + 1, z);

                if (!chunk.getBlockState(pos).isAir()) {
                    continue;
                }

                if (formationContaining(island, x, z) != null) {
                    continue;
                }

                if (!mergedGeneration && isBoulderOrigin(x, z, island.seed())) {
                    placeBoulder(chunk, x, surfaceY, z, island.seed(), island.biome());
                    continue;
                }

                if (!mergedGeneration
                        && island.biome() == AenderBiomeKind.PLAINS
                        && isTreeOrigin(x, z, island.seed())
                        && hasTreeRoom(chunk, x, surfaceY, z)
                        && hasGentleGround(island, x, surfaceY, z)) {
                    placeTree(chunk, x, surfaceY, z, island.seed());
                    continue;
                }

                if (mergedGeneration
                        && (isBoulderOrigin(x, z, island.seed())
                        || isTreePlacement(islands, island, x, surfaceY, z))) {
                    continue;
                }

                placeGroundDecoration(chunk, island, x, surfaceY, z, pos);
            }
        }
    }

    private static void placeCrossChunkLargeDecorations(
            ChunkAccess chunk,
            List<CachedIsland> islands
    ) {
        placeCrossChunkTerrainFormations(chunk, islands);

        final int featureHalo = 5;
        int minX = chunk.getPos().getMinBlockX() - featureHalo;
        int maxX = chunk.getPos().getMaxBlockX() + featureHalo;
        int minZ = chunk.getPos().getMinBlockZ() - featureHalo;
        int maxZ = chunk.getPos().getMaxBlockZ() + featureHalo;

        for (CachedIsland island : islands) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    AenderIslandSampler.Island.Column column = island.columnAt(x, z);

                    if (column.empty()) {
                        continue;
                    }

                    int surfaceY = column.maxY();

                    if (!isExposedSurfaceOwner(islands, island, x, z, surfaceY)) {
                        continue;
                    }

                    if (formationContaining(island, x, z) != null
                            || isStoneOutcrop(island.seed(), x, z)
                            || lakeColumnAt(island, x, z, surfaceY) != null) {
                        continue;
                    }

                    if (isBoulderOrigin(x, z, island.seed())) {
                        placeBoulder(chunk, x, surfaceY, z, island.seed(), island.biome());
                        continue;
                    }

                    if (isTreePlacement(islands, island, x, surfaceY, z)) {
                        placeTree(chunk, x, surfaceY, z, island.seed());
                    }
                }
            }
        }
    }

    private static void placeCrossChunkTerrainFormations(
            ChunkAccess chunk,
            List<CachedIsland> islands
    ) {
        int minX = chunk.getPos().getMinBlockX();
        int maxX = chunk.getPos().getMaxBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        int maxZ = chunk.getPos().getMaxBlockZ();

        for (CachedIsland island : islands) {
            List<AenderDecorationPlanner.Formation> formations =
                    AenderDecorationPlanner.formationsIntersecting(island.seed(), minX, maxX, minZ, maxZ);

            for (AenderDecorationPlanner.Formation formation : formations) {
                AenderIslandSampler.Island.Column column = island.columnAt(formation.x(), formation.z());

                if (column.empty()
                        || !isExposedSurfaceOwner(islands, island, formation.x(), formation.z(), column.maxY())) {
                    continue;
                }

                placeTerrainFormation(chunk, island, islands, formation, column.maxY());
            }
        }
    }

    private static boolean isTreePlacement(
            List<CachedIsland> islands,
            CachedIsland island,
            int x,
            int surfaceY,
            int z
    ) {
        return island.biome() == AenderBiomeKind.PLAINS
                && surfaceY + 8 < AenderIslandSampler.MAX_Y
                && isTreeOrigin(x, z, island.seed())
                && !isBoulderOrigin(x, z, island.seed())
                && !isStoneOutcrop(island.seed(), x, z)
                && lakeColumnAt(island, x, z, surfaceY) == null
                && isExposedSurfaceOwner(islands, island, x, z, surfaceY)
                && hasGentleGround(island, x, surfaceY, z);
    }

    private static boolean isExposedSurfaceOwner(
            List<CachedIsland> islands,
            CachedIsland candidate,
            int x,
            int z,
            int surfaceY
    ) {
        for (CachedIsland island : islands) {
            if (island == candidate) {
                continue;
            }

            AenderIslandSampler.Island.Column other = island.columnAt(x, z);

            if (other.empty()) {
                continue;
            }

            if (other.minY() <= surfaceY + 1 && other.maxY() >= surfaceY + 1) {
                return false;
            }

            if (other.maxY() == surfaceY
                    && Long.compareUnsigned(island.seed(), candidate.seed()) < 0) {
                return false;
            }
        }

        return true;
    }

    @Override
    public int getBaseHeight(
            int x,
            int z,
            Heightmap.Types type,
            LevelHeightAccessor level,
            RandomState random
    ) {
        return Math.min(AenderIslandSampler.highestBlockYAt(x, z) + 1, AenderIslandSampler.MAX_Y);
    }

    @Override
    public NoiseColumn getBaseColumn(
            int x,
            int z,
            LevelHeightAccessor level,
            RandomState random
    ) {
        if (AenderVolatility.currentGeneratorVersion() >= AenderRealityData.MERGED_TERRAIN_GENERATOR_VERSION) {
            return getMergedBaseColumn(x, z);
        }

        TerrainBlocks terrainBlocks = TerrainBlocks.create();
        BlockState[] states = new BlockState[AenderIslandSampler.HEIGHT];

        for (int i = 0; i < states.length; i++) {
            int y = AenderIslandSampler.MIN_Y + i;
            int surfaceDepth = AenderIslandSampler.surfaceDepthAt(x, y, z);
            states[i] = surfaceDepth >= 0 ? terrainBlockForDepth(terrainBlocks, 0L, x, z, surfaceDepth) : AIR;
        }

        return new NoiseColumn(AenderIslandSampler.MIN_Y, states);
    }

    private static NoiseColumn getMergedBaseColumn(int x, int z) {
        List<AenderIslandSampler.Island> islands = AenderIslandSampler.islandsAtColumn(x, z);
        int[] mins = new int[islands.size()];
        int[] maxes = new int[islands.size()];
        long[] seeds = new long[islands.size()];

        for (int index = 0; index < islands.size(); index++) {
            AenderIslandSampler.Island island = islands.get(index);
            AenderIslandSampler.Island.Column column = island.columnAt(x, z);
            mins[index] = column.minY();
            maxes[index] = column.maxY();
            seeds[index] = island.seed();
        }

        int intervalCount = AenderTerrainIntervals.merge(mins, maxes, seeds, islands.size());
        TerrainBlocks terrainBlocks = TerrainBlocks.create();
        BlockState[] states = new BlockState[AenderIslandSampler.HEIGHT];
        Arrays.fill(states, AIR);

        for (int intervalIndex = 0; intervalIndex < intervalCount; intervalIndex++) {
            for (int y = mins[intervalIndex]; y <= maxes[intervalIndex]; y++) {
                int surfaceDepth = maxes[intervalIndex] - y;
                states[y - AenderIslandSampler.MIN_Y] = terrainBlockForDepth(
                        terrainBlocks,
                        seeds[intervalIndex],
                        x,
                        z,
                        surfaceDepth
                );
            }
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
        info.add("Aender cached chunk signatures: " + AenderVolatility.cachedChunkSignatureCount());
    }

    private static BlockState terrainBlockForDepth(TerrainBlocks terrainBlocks, long seed, int x, int z, int surfaceDepth) {
        if (AenderBiomeKind.fromIslandSeed(seed) == AenderBiomeKind.DESERT) {
            if (surfaceDepth <= 2 && isStoneOutcrop(seed, x, z)) {
                return terrainBlocks.sandstone();
            }

            if (surfaceDepth <= 3) {
                return terrainBlocks.sand();
            }

            if (surfaceDepth <= 10) {
                return terrainBlocks.sandstone();
            }

            return terrainBlocks.stone();
        }

        if (seed != 0L && surfaceDepth <= 2 && isStoneOutcrop(seed, x, z)) {
            return terrainBlocks.stone();
        }

        if (surfaceDepth == 0) {
            return terrainBlocks.grass();
        }

        if (surfaceDepth <= 4) {
            return terrainBlocks.soil();
        }

        return terrainBlocks.stone();
    }

    private static boolean isStoneOutcrop(long seed, int x, int z) {
        int cellX = Math.floorDiv(x, 13);
        int cellZ = Math.floorDiv(z, 13);
        long cellSeed = mix64(seed
                ^ (long) cellX * 0x9E3779B97F4A7C15L
                ^ (long) cellZ * 0xC2B2AE3D27D4EB4FL
                ^ 0x5EED5EEDL);

        if (unit(cellSeed) >= 0.28D) {
            return false;
        }

        int centerX = cellX * 13 + 2 + (int) (unit(cellSeed ^ 0x31L) * 9.0D);
        int centerZ = cellZ * 13 + 2 + (int) (unit(cellSeed ^ 0x32L) * 9.0D);
        int distance = Math.abs(x - centerX) + Math.abs(z - centerZ);
        return distance <= 1 || (distance == 2 && unit(cellSeed ^ x ^ ((long) z << 32)) < 0.45D);
    }

    private static LakeColumn lakeColumnAt(CachedIsland island, int x, int z, int surfaceY) {
        if (island.biome() == AenderBiomeKind.DESERT) {
            return null;
        }

        double lakeChance = switch (island.archetype()) {
            case ROUND -> 0.28D;
            case ELONGATED -> 0.24D;
            case TWIN -> 0.18D;
            case CRESCENT, SPLIT -> 0.12D;
            default -> 0.0D;
        };

        int cellX = Math.floorDiv(x, 64);
        int cellZ = Math.floorDiv(z, 64);

        for (int cx = cellX - 1; cx <= cellX + 1; cx++) {
            for (int cz = cellZ - 1; cz <= cellZ + 1; cz++) {
                long lakeSeed = mix64(island.seed()
                        ^ (long) cx * 0xD1342543DE82EF95L
                        ^ (long) cz * 0xC6BC279692B5CC83L
                        ^ 0x1A4E5EADL);

                if (unit(lakeSeed) >= lakeChance) {
                    continue;
                }

                int centerX = cx * 64 + 18 + (int) (unit(lakeSeed ^ 0x11L) * 28.0D);
                int centerZ = cz * 64 + 18 + (int) (unit(lakeSeed ^ 0x12L) * 28.0D);
                double radiusX = 5.0D + unit(lakeSeed ^ 0x21L) * 8.0D;
                double radiusZ = 5.0D + unit(lakeSeed ^ 0x22L) * 8.0D;

                double dx = (x - centerX) / radiusX;
                double dz = (z - centerZ) / radiusZ;
                double distance = dx * dx + dz * dz;

                if (distance > 1.0D) {
                    continue;
                }

                AenderIslandSampler.Island.Column centerColumn = island.columnAt(centerX, centerZ);

                if (centerColumn.empty()) {
                    continue;
                }

                int waterY = centerColumn.maxY() - 1;

                if (surfaceY < waterY || surfaceY > waterY + 4) {
                    continue;
                }

                return new LakeColumn(waterY, distance);
            }
        }

        return null;
    }

    private static boolean carveLakeColumn(
            ChunkAccess chunk,
            AenderIslandSampler.Island.Column column,
            int x,
            int z,
            int surfaceY,
            LakeColumn lake,
            BlockPos.MutableBlockPos pos
    ) {
        if (lake.distance() > 0.78D) {
            pos.set(x, surfaceY, z);

            if (chunk.getBlockState(pos).is(RetoldBlocks.AENDER_GRASS_BLOCK)) {
                chunk.setBlockState(pos, RetoldBlocks.AENDER_SOIL.get().defaultBlockState(), 0);
            }

            return true;
        }

        int waterY = lake.waterY();

        for (int y = waterY + 1; y <= surfaceY; y++) {
            pos.set(x, y, z);

            if (isInsideChunk(chunk, pos)) {
                chunk.setBlockState(pos, AIR, 0);
            }
        }

        pos.set(x, waterY, z);

        if (isInsideChunk(chunk, pos)) {
            chunk.setBlockState(pos, WATER, 0);
            chunk.markPosForPostProcessing(pos);
        }

        int floorY = waterY - 1;

        if (floorY >= column.minY()) {
            pos.set(x, floorY, z);

            if (isInsideChunk(chunk, pos)) {
                chunk.setBlockState(pos, RetoldBlocks.AENDER_SOIL.get().defaultBlockState(), 0);
            }
        }

        return true;
    }

    private static void placeGroundDecoration(
            ChunkAccess chunk,
            CachedIsland island,
            int x,
            int surfaceY,
            int z,
            BlockPos.MutableBlockPos pos
    ) {
        long seed = island.seed();

        if (island.biome() == AenderBiomeKind.DESERT) {
            if (isCactusOrigin(x, z, seed)) {
                placeCactus(chunk, island, x, surfaceY, z, seed);
            }

            return;
        }

        double vegetation = AenderDecorationPlanner.plainsVegetationStrength(seed, x, z);

        if (vegetation <= 0.02D) {
            return;
        }

        double roll = unit(seed
                ^ (long) x * 0xD1342543DE82EF95L
                ^ (long) z * 0xC6BC279692B5CC83L
                ^ 0x61746C696665L);

        if (roll >= 0.12D + vegetation * 0.52D) {
            return;
        }

        BlockState decoration;
        double flowerPatch = AenderDecorationPlanner.flowerPatchStrength(seed, x, z);
        double detailRoll = unit(seed
                ^ (long) x * 0x9E3779B97F4A7C15L
                ^ (long) z * 0xC2B2AE3D27D4EB4FL
                ^ 0xDEC02A7EL);

        if (detailRoll < 0.035D) {
            decoration = RetoldBlocks.AENDER_LEAVES.get().defaultBlockState();
        } else if (flowerPatch > 0.10D && detailRoll < 0.16D + flowerPatch * 0.42D) {
            int flowerIndex = (int) (unit(seed ^ (long) x * 0x9E3779B97F4A7C15L ^ (long) z) * FLOWERS.length);
            decoration = FLOWERS[Math.min(flowerIndex, FLOWERS.length - 1)];
        } else if (detailRoll < 0.30D) {
            decoration = FERN;
        } else {
            decoration = SHORT_GRASS;
        }

        pos.set(x, surfaceY + 1, z);
        chunk.setBlockState(pos, decoration, 0);
    }

    private static boolean isSurfaceBlock(BlockState state, AenderBiomeKind biome) {
        return biome == AenderBiomeKind.DESERT
                ? state.is(RetoldBlocks.AENDER_SAND)
                : state.is(RetoldBlocks.AENDER_GRASS_BLOCK);
    }

    private static boolean isCactusOrigin(int x, int z, long seed) {
        int cellX = Math.floorDiv(x, 13);
        int cellZ = Math.floorDiv(z, 13);
        long cellSeed = mix64(seed
                ^ (long) cellX * 0x9E3779B97F4A7C15L
                ^ (long) cellZ * 0xC2B2AE3D27D4EB4FL
                ^ 0xCA67C5L);

        double patch = AenderDecorationPlanner.cactusPatchStrength(seed, x, z);
        double chance = patch > 0.0D ? 0.36D + patch * 0.48D : 0.08D;

        if (unit(cellSeed) >= chance) {
            return false;
        }

        int originX = cellX * 13 + 3 + (int) (unit(cellSeed ^ 0x31L) * 7.0D);
        int originZ = cellZ * 13 + 3 + (int) (unit(cellSeed ^ 0x32L) * 7.0D);
        return x == originX && z == originZ;
    }

    private static void placeCactus(
            ChunkAccess chunk,
            CachedIsland island,
            int x,
            int surfaceY,
            int z,
            long seed
    ) {
        if (surfaceDelta(island, x + 1, z, surfaceY) > 0
                || surfaceDelta(island, x - 1, z, surfaceY) > 0
                || surfaceDelta(island, x, z + 1, surfaceY) > 0
                || surfaceDelta(island, x, z - 1, surfaceY) > 0) {
            return;
        }

        long cactusSeed = mix64(seed
                ^ (long) x * 0xD1342543DE82EF95L
                ^ (long) z * 0xC6BC279692B5CC83L
                ^ 0xA3C7C5L);
        int height = 1 + (int) (unit(cactusSeed) * 3.0D);
        BlockState cactus = RetoldBlocks.AENDER_CACTUS.get().defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int dy = 1; dy <= height; dy++) {
            pos.set(x, surfaceY + dy, z);

            if (!isInsideChunk(chunk, pos) || !chunk.getBlockState(pos).isAir()) {
                return;
            }

            chunk.setBlockState(pos, cactus, 0);
        }
    }

    private static boolean isBoulderOrigin(int x, int z, long seed) {
        int cellX = Math.floorDiv(x, 24);
        int cellZ = Math.floorDiv(z, 24);
        long cellSeed = mix64(seed
                ^ (long) cellX * 0xDB4F0B9175AE2165L
                ^ (long) cellZ * 0xBBE0563303A4615FL
                ^ 0xB011D3A5L);

        if (unit(cellSeed) >= 0.22D) {
            return false;
        }

        int originX = cellX * 24 + 5 + (int) (unit(cellSeed ^ 0x41L) * 14.0D);
        int originZ = cellZ * 24 + 5 + (int) (unit(cellSeed ^ 0x42L) * 14.0D);
        return x == originX && z == originZ;
    }

    private static void placeBoulder(
            ChunkAccess chunk,
            int x,
            int surfaceY,
            int z,
            long seed,
            AenderBiomeKind biome
    ) {
        long boulderSeed = mix64(seed
                ^ (long) x * 0xD1342543DE82EF95L
                ^ (long) z * 0xC6BC279692B5CC83L
                ^ 0xB075D32L);
        int radius = unit(boulderSeed ^ 0x51L) < 0.55D ? 1 : 2;
        int height = radius + 1;
        BlockState stone = biome == AenderBiomeKind.DESERT
                ? RetoldBlocks.AENDER_SANDSTONE.get().defaultBlockState()
                : RetoldBlocks.AENDER_STONE.get().defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int dy = 1; dy <= height; dy++) {
            int layerRadius = Math.max(0, radius - dy / 2);

            for (int dx = -layerRadius; dx <= layerRadius; dx++) {
                for (int dz = -layerRadius; dz <= layerRadius; dz++) {
                    int distance = Math.abs(dx) + Math.abs(dz);

                    if (distance > layerRadius + (dy == 1 ? 1 : 0)) {
                        continue;
                    }

                    if (unit(boulderSeed ^ dx * 31L ^ dz * 17L ^ dy * 13L) < 0.12D) {
                        continue;
                    }

                    pos.set(x + dx, surfaceY + dy, z + dz);

                    if (isInsideChunk(chunk, pos) && chunk.getBlockState(pos).isAir()) {
                        chunk.setBlockState(pos, stone, 0);
                    }
                }
            }
        }
    }

    private static AenderDecorationPlanner.Formation formationContaining(
            CachedIsland island,
            int x,
            int z
    ) {
        AenderDecorationPlanner.Formation formation = AenderDecorationPlanner.formationContaining(
                island.seed(),
                x,
                z
        );

        if (formation == null || island.columnAt(formation.x(), formation.z()).empty()) {
            return null;
        }

        return formation;
    }

    private static void placeTerrainFormation(
            ChunkAccess chunk,
            CachedIsland island,
            List<CachedIsland> islands,
            AenderDecorationPlanner.Formation formation,
            int surfaceY
    ) {
        BlockState stone = island.biome() == AenderBiomeKind.DESERT
                ? RetoldBlocks.AENDER_SANDSTONE.get().defaultBlockState()
                : RetoldBlocks.AENDER_STONE.get().defaultBlockState();

        switch (formation.kind()) {
            case SPIRE -> placeStoneSpire(chunk, formation, surfaceY, stone);
            case CRATER -> carveEnergyCrater(chunk, island, islands, formation, stone);
        }
    }

    private static void placeStoneSpire(
            ChunkAccess chunk,
            AenderDecorationPlanner.Formation formation,
            int surfaceY,
            BlockState stone
    ) {
        int height = 8 + (int) (unit(formation.seed() ^ 0x51L) * 12.0D);
        int baseRadius = unit(formation.seed() ^ 0x52L) < 0.72D ? 2 : 3;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int dy = 1; dy <= height; dy++) {
            double taper = 1.0D - dy / (double) (height + 1);
            int radius = Math.max(0, (int) Math.floor(baseRadius * taper));

            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dz * dz > radius * radius + (dy <= 2 ? 1 : 0)) {
                        continue;
                    }

                    if (dy > 2 && unit(formation.seed() ^ dx * 31L ^ dz * 17L ^ dy * 13L) < 0.08D) {
                        continue;
                    }

                    pos.set(formation.x() + dx, surfaceY + dy, formation.z() + dz);

                    if (isInsideChunk(chunk, pos) && chunk.getBlockState(pos).isAir()) {
                        chunk.setBlockState(pos, stone, 0);
                    }
                }
            }
        }
    }

    private static void carveEnergyCrater(
            ChunkAccess chunk,
            CachedIsland island,
            List<CachedIsland> islands,
            AenderDecorationPlanner.Formation formation,
            BlockState stone
    ) {
        double radiusX = 6.0D + unit(formation.seed() ^ 0x71L) * 3.0D;
        double radiusZ = 6.0D + unit(formation.seed() ^ 0x72L) * 3.0D;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int dx = -(int) Math.ceil(radiusX); dx <= Math.ceil(radiusX); dx++) {
            for (int dz = -(int) Math.ceil(radiusZ); dz <= Math.ceil(radiusZ); dz++) {
                double normalizedX = dx / radiusX;
                double normalizedZ = dz / radiusZ;
                double distance = Math.sqrt(normalizedX * normalizedX + normalizedZ * normalizedZ);

                if (distance > 1.0D) {
                    continue;
                }

                int x = formation.x() + dx;
                int z = formation.z() + dz;

                if (!isInsideChunkColumn(chunk, x, z)) {
                    continue;
                }

                AenderIslandSampler.Island.Column column = island.columnAt(x, z);

                if (column.empty()) {
                    continue;
                }

                if (!isExposedSurfaceOwner(islands, island, x, z, column.maxY())) {
                    continue;
                }

                int depth = 1 + (int) Math.floor((1.0D - distance) * 5.0D);
                int floorY = Math.max(column.minY(), column.maxY() - depth);

                for (int y = floorY + 1; y <= column.maxY() + 8; y++) {
                    pos.set(x, y, z);

                    if (isInsideChunk(chunk, pos)
                            && !chunk.getBlockState(pos).is(RetoldBlocks.DEV_AENDER_PORTAL_FRAME)) {
                        chunk.setBlockState(pos, AIR, 0);
                    }
                }

                pos.set(x, floorY, z);

                if (isInsideChunk(chunk, pos)
                        && !chunk.getBlockState(pos).is(RetoldBlocks.DEV_AENDER_PORTAL_FRAME)) {
                    chunk.setBlockState(pos, stone, 0);
                }
            }
        }
    }

    private static void placeUndersideSpur(
            ChunkAccess chunk,
            AenderIslandSampler.Island.Column column,
            CachedIsland island,
            int x,
            int z,
            boolean stopAtOccupiedBlock,
            BlockPos.MutableBlockPos pos
    ) {
        long seed = island.seed();
        long spurSeed = mix64(seed
                ^ (long) x * 0xA24BAED4963EE407L
                ^ (long) z * 0x9FB21C651E98DF25L
                ^ 0x5F013E5DL);

        double chance = switch (island.underside()) {
            case TAPERED -> 0.050D;
            case ROOTED -> 0.055D;
            case FRACTURED -> 0.078D;
            case TERRACED -> 0.026D;
        };

        if (unit(spurSeed) >= chance || column.minY() <= AenderIslandSampler.MIN_Y + 4) {
            return;
        }

        int extraLength = switch (island.underside()) {
            case TAPERED -> 6;
            case ROOTED -> 4;
            case FRACTURED -> 5;
            case TERRACED -> 2;
        };
        int length = 2 + (int) (unit(spurSeed ^ 0x61L) * extraLength);
        AenderBiomeKind biome = island.biome();
        BlockState stone = biome == AenderBiomeKind.DESERT
                ? RetoldBlocks.AENDER_SANDSTONE.get().defaultBlockState()
                : RetoldBlocks.AENDER_STONE.get().defaultBlockState();
        BlockState soil = biome == AenderBiomeKind.DESERT
                ? stone
                : RetoldBlocks.AENDER_SOIL.get().defaultBlockState();

        for (int dy = 1; dy <= length; dy++) {
            int y = column.minY() - dy;

            if (y < AenderIslandSampler.MIN_Y) {
                break;
            }

            pos.set(x, y, z);

            if (stopAtOccupiedBlock && !chunk.getBlockState(pos).isAir()) {
                break;
            }

            chunk.setBlockState(pos, dy < 3 && unit(spurSeed ^ dy) < 0.35D ? soil : stone, 0);

            if (dy <= 2 && unit(spurSeed ^ 0x71L ^ dy) < 0.55D) {
                placeSpurShoulder(chunk, x + 1, y, z, stone, pos);
                placeSpurShoulder(chunk, x - 1, y, z, stone, pos);
                placeSpurShoulder(chunk, x, y, z + 1, stone, pos);
                placeSpurShoulder(chunk, x, y, z - 1, stone, pos);
            }
        }
    }

    private static void placeSpurShoulder(
            ChunkAccess chunk,
            int x,
            int y,
            int z,
            BlockState state,
            BlockPos.MutableBlockPos pos
    ) {
        pos.set(x, y, z);

        if (isInsideChunk(chunk, pos) && chunk.getBlockState(pos).isAir()) {
            chunk.setBlockState(pos, state, 0);
        }
    }

    private static void placeUndersideGrowth(
            ChunkAccess chunk,
            AenderIslandSampler.Island.Column column,
            CachedIsland island,
            int x,
            int z,
            BlockPos.MutableBlockPos pos
    ) {
        if (island.biome() == AenderBiomeKind.DESERT) {
            return;
        }

        long seed = island.seed();
        long growthSeed = mix64(seed
                ^ (long) x * 0xD6E8FEB86659FD93L
                ^ (long) z * 0xA5A3564E27F886BFL
                ^ 0xA11FEAFL);

        double chance = island.underside() == AenderUndersideProfile.ROOTED ? 0.052D : 0.017D;

        if (unit(growthSeed) >= chance || column.minY() <= AenderIslandSampler.MIN_Y + 6) {
            return;
        }

        int length = 2 + (int) (unit(growthSeed ^ 0x11L) * 5.0D);
        BlockState leaves = RetoldBlocks.AENDER_LEAVES.get().defaultBlockState();
        BlockState log = RetoldBlocks.AENDER_LOG.get().defaultBlockState();

        for (int dy = 1; dy <= length; dy++) {
            int y = column.minY() - dy;

            if (y < AenderIslandSampler.MIN_Y) {
                break;
            }

            pos.set(x, y, z);

            if (!isInsideChunk(chunk, pos) || !chunk.getBlockState(pos).isAir()) {
                break;
            }

            chunk.setBlockState(pos, dy == 1 && unit(growthSeed ^ 0x21L) < 0.35D ? log : leaves, 0);

            if (dy <= 2 && unit(growthSeed ^ dy * 0x632BE59BD9B4E019L) < 0.45D) {
                placeHangingLeaf(chunk, x + 1, y, z, leaves, pos);
                placeHangingLeaf(chunk, x - 1, y, z, leaves, pos);
                placeHangingLeaf(chunk, x, y, z + 1, leaves, pos);
                placeHangingLeaf(chunk, x, y, z - 1, leaves, pos);
            }
        }
    }

    private static void placeHangingLeaf(
            ChunkAccess chunk,
            int x,
            int y,
            int z,
            BlockState leaves,
            BlockPos.MutableBlockPos pos
    ) {
        pos.set(x, y, z);

        if (isInsideChunk(chunk, pos) && chunk.getBlockState(pos).isAir()) {
            chunk.setBlockState(pos, leaves, 0);
        }
    }

    private static boolean isTreeOrigin(int x, int z, long seed) {
        int cellX = Math.floorDiv(x, 20);
        int cellZ = Math.floorDiv(z, 20);
        long cellSeed = mix64(seed
                ^ (long) cellX * 0x632BE59BD9B4E019L
                ^ (long) cellZ * 0x85157AF5C91D1B35L
                ^ 0x7472656573L);

        double grove = AenderDecorationPlanner.groveStrength(seed, x, z);
        double chance = grove > 0.0D ? 0.38D + grove * 0.42D : 0.07D;

        if (unit(cellSeed) >= chance) {
            return false;
        }

        int originX = cellX * 20 + 4 + (int) (unit(cellSeed ^ 0xA1L) * 12.0D);
        int originZ = cellZ * 20 + 4 + (int) (unit(cellSeed ^ 0xA2L) * 12.0D);
        return x == originX && z == originZ;
    }

    private static boolean hasTreeRoom(ChunkAccess chunk, int x, int surfaceY, int z) {
        int minX = chunk.getPos().getMinBlockX() + 3;
        int maxX = chunk.getPos().getMinBlockX() + 12;
        int minZ = chunk.getPos().getMinBlockZ() + 3;
        int maxZ = chunk.getPos().getMinBlockZ() + 12;
        return x >= minX
                && x <= maxX
                && z >= minZ
                && z <= maxZ
                && surfaceY + 8 < AenderIslandSampler.MAX_Y;
    }

    private static boolean hasGentleGround(CachedIsland island, int x, int surfaceY, int z) {
        return surfaceDelta(island, x + 2, z, surfaceY) <= 3
                && surfaceDelta(island, x - 2, z, surfaceY) <= 3
                && surfaceDelta(island, x, z + 2, surfaceY) <= 3
                && surfaceDelta(island, x, z - 2, surfaceY) <= 3;
    }

    private static int surfaceDelta(CachedIsland island, int x, int z, int surfaceY) {
        AenderIslandSampler.Island.Column column = island.columnAt(x, z);
        return column.empty() ? 99 : Math.abs(column.maxY() - surfaceY);
    }

    private static void placeTree(ChunkAccess chunk, int x, int surfaceY, int z, long seed) {
        long treeSeed = mix64(seed
                ^ (long) x * 0xD1342543DE82EF95L
                ^ (long) z * 0xC6BC279692B5CC83L
                ^ 0xA3ADE7L);
        int height = 5 + (int) (unit(treeSeed ^ 0x11L) * 4.0D);
        int leanX = unit(treeSeed ^ 0x12L) < 0.5D ? -1 : 1;
        int leanZ = unit(treeSeed ^ 0x13L) < 0.5D ? -1 : 1;
        boolean leanAlongX = unit(treeSeed ^ 0x14L) < 0.5D;

        BlockState log = RetoldBlocks.AENDER_LOG.get().defaultBlockState();
        BlockState leaves = RetoldBlocks.AENDER_LEAVES.get()
                .defaultBlockState()
                .setValue(LeavesBlock.PERSISTENT, false);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        int[] trunkX = new int[height + 1];
        int[] trunkZ = new int[height + 1];

        for (int dy = 1; dy <= height; dy++) {
            double curve = dy / (double) height;
            int offsetX = leanAlongX && curve > 0.45D ? leanX : 0;
            int offsetZ = !leanAlongX && curve > 0.45D ? leanZ : 0;

            if (curve > 0.72D) {
                offsetX += leanAlongX ? 0 : leanX;
                offsetZ += leanAlongX ? leanZ : 0;
            }

            trunkX[dy] = x + offsetX;
            trunkZ[dy] = z + offsetZ;

            pos.set(trunkX[dy], surfaceY + dy, trunkZ[dy]);

            if (isInsideChunk(chunk, pos) && chunk.getBlockState(pos).isAir()) {
                chunk.setBlockState(pos, log, 0);
            }
        }

        placeTreeRoots(chunk, x, surfaceY, z, treeSeed, log, pos);

        int crownX = trunkX[height];
        int crownZ = trunkZ[height];
        int canopyBase = surfaceY + height - 3;
        int canopyTop = surfaceY + height + 2;

        for (int y = canopyBase; y <= canopyTop; y++) {
            int dy = y - (surfaceY + height);
            int radius = dy >= 1 ? 1 : 2 + (unit(treeSeed ^ y * 0x632BE59BD9B4E019L) < 0.35D ? 1 : 0);

            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int stretchX = leanAlongX ? leanX : 0;
                    int stretchZ = leanAlongX ? 0 : leanZ;
                    int localX = dx - stretchX;
                    int localZ = dz - stretchZ;

                    if (Math.abs(localX) + Math.abs(localZ) + Math.max(0, dy) > radius + 1) {
                        continue;
                    }

                    if (crownX + dx == trunkX[Math.min(height, Math.max(1, y - surfaceY))]
                            && crownZ + dz == trunkZ[Math.min(height, Math.max(1, y - surfaceY))]
                            && y <= surfaceY + height) {
                        continue;
                    }

                    if (unit(treeSeed ^ dx * 31L ^ dz * 17L ^ y * 13L) < 0.08D + Math.max(0, dy) * 0.08D) {
                        continue;
                    }

                    pos.set(crownX + dx, y, crownZ + dz);

                    if (isInsideChunk(chunk, pos) && chunk.getBlockState(pos).isAir()) {
                        chunk.setBlockState(pos, leaves, 0);

                        if (dy <= 0
                                && Math.abs(localX) + Math.abs(localZ) >= radius
                                && unit(treeSeed ^ dx * 97L ^ dz * 53L ^ y) < 0.20D) {
                            placeLeafTendril(chunk, crownX + dx, y - 1, crownZ + dz, treeSeed, leaves);
                        }
                    }
                }
            }
        }
    }

    private static void placeTreeRoots(
            ChunkAccess chunk,
            int x,
            int surfaceY,
            int z,
            long treeSeed,
            BlockState log,
            BlockPos.MutableBlockPos pos
    ) {
        placeRoot(chunk, x + 1, surfaceY + 1, z, treeSeed ^ 0x21L, log, pos);
        placeRoot(chunk, x - 1, surfaceY + 1, z, treeSeed ^ 0x22L, log, pos);
        placeRoot(chunk, x, surfaceY + 1, z + 1, treeSeed ^ 0x23L, log, pos);
        placeRoot(chunk, x, surfaceY + 1, z - 1, treeSeed ^ 0x24L, log, pos);
    }

    private static void placeRoot(
            ChunkAccess chunk,
            int x,
            int y,
            int z,
            long seed,
            BlockState log,
            BlockPos.MutableBlockPos pos
    ) {
        if (unit(seed) > 0.58D) {
            return;
        }

        pos.set(x, y, z);

        if (isInsideChunk(chunk, pos) && chunk.getBlockState(pos).isAir()) {
            chunk.setBlockState(pos, log, 0);
        }
    }

    private static void placeLeafTendril(
            ChunkAccess chunk,
            int x,
            int y,
            int z,
            long seed,
            BlockState leaves
    ) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int length = 1 + (int) (unit(seed ^ (long) x * 0x632BE59BD9B4E019L ^ (long) z) * 3.0D);

        for (int i = 0; i < length; i++) {
            pos.set(x, y - i, z);

            if (!isInsideChunk(chunk, pos) || !chunk.getBlockState(pos).isAir()) {
                return;
            }

            chunk.setBlockState(pos, leaves, 0);
        }
    }

    private static double unit(long seed) {
        long value = mix64(seed);
        return (value >>> 11) * 0x1.0p-53;
    }

    private static long mix64(long value) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= value >>> 33;
        return value;
    }

    private static boolean isInsideChunk(ChunkAccess chunk, BlockPos pos) {
        return isInsideChunkColumn(chunk, pos.getX(), pos.getZ())
                && pos.getY() >= AenderIslandSampler.MIN_Y
                && pos.getY() < AenderIslandSampler.MAX_Y;
    }

    private static boolean isInsideChunkColumn(ChunkAccess chunk, int x, int z) {
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        return x >= minX
                && x <= minX + 15
                && z >= minZ
                && z <= minZ + 15;
    }

    private static final class CachedIsland {
        private final AenderIslandSampler.Island island;
        private final int chunkMinX;
        private final int chunkMinZ;
        private final AenderIslandSampler.Island.Column[] chunkColumns = new AenderIslandSampler.Island.Column[16 * 16];
        private final Map<Long, AenderIslandSampler.Island.Column> extraColumns = new HashMap<>();

        private CachedIsland(AenderIslandSampler.Island island, int chunkMinX, int chunkMinZ) {
            this.island = island;
            this.chunkMinX = chunkMinX;
            this.chunkMinZ = chunkMinZ;
        }

        private int minX() {
            return island.minX();
        }

        private int maxX() {
            return island.maxX();
        }

        private int minY() {
            return island.minY();
        }

        private int maxY() {
            return island.maxY();
        }

        private int minZ() {
            return island.minZ();
        }

        private int maxZ() {
            return island.maxZ();
        }

        private long seed() {
            return island.seed();
        }

        private AenderBiomeKind biome() {
            return island.biome();
        }

        private AenderIslandArchetype archetype() {
            return island.archetype();
        }

        private AenderUndersideProfile underside() {
            return island.underside();
        }

        private AenderIslandSampler.Island.Column columnAt(int x, int z) {
            int localX = x - chunkMinX;
            int localZ = z - chunkMinZ;

            if (localX >= 0 && localX < 16 && localZ >= 0 && localZ < 16) {
                int index = localX + localZ * 16;
                AenderIslandSampler.Island.Column column = chunkColumns[index];

                if (column == null) {
                    column = island.columnAt(x, z);
                    chunkColumns[index] = column;
                }

                return column;
            }

            return extraColumns.computeIfAbsent(columnKey(x, z), key -> island.columnAt(x, z));
        }
    }

    private static long columnKey(int x, int z) {
        return ((long) x & 0xffffffffL) | (((long) z & 0xffffffffL) << 32);
    }

    private record TerrainBlocks(
            BlockState grass,
            BlockState soil,
            BlockState stone,
            BlockState sand,
            BlockState sandstone
    ) {
        private static TerrainBlocks create() {
            return new TerrainBlocks(
                    RetoldBlocks.AENDER_GRASS_BLOCK.get().defaultBlockState(),
                    RetoldBlocks.AENDER_SOIL.get().defaultBlockState(),
                    RetoldBlocks.AENDER_STONE.get().defaultBlockState(),
                    RetoldBlocks.AENDER_SAND.get().defaultBlockState(),
                    RetoldBlocks.AENDER_SANDSTONE.get().defaultBlockState()
            );
        }
    }

    private record LakeColumn(int waterY, double distance) {
    }
}
