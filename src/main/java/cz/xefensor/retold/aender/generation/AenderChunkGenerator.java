package cz.xefensor.retold.aender.generation;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import cz.xefensor.retold.registry.RetoldBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
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
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.EnumSet;
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

    public static void regenerateLoadedChunk(ChunkAccess chunk) {
        generateChunk(chunk, true);
    }

    public static void regenerateLoadedChunk(ServerLevel level, ChunkAccess chunk) {
        ChunkPos chunkPos = chunk.getPos();
        List<Entity> entities = entitiesInRegenerationBounds(level, chunkPos);
        generateChunk(chunk, true);
        reconcileEntitiesAfterRegeneration(level, chunkPos, entities);
    }

    private static void generateChunk(ChunkAccess chunk, boolean clearFirst) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        int chunkMinX = chunk.getPos().getMinBlockX();
        int chunkMaxX = chunkMinX + 15;
        int chunkMinZ = chunk.getPos().getMinBlockZ();
        int chunkMaxZ = chunkMinZ + 15;
        TerrainBlocks terrainBlocks = TerrainBlocks.create();

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
        List<CachedIsland> cachedIslands = new ArrayList<>(islands.size());

        for (AenderIslandSampler.Island island : islands) {
            cachedIslands.add(new CachedIsland(island, chunkMinX, chunkMinZ));
        }

        for (CachedIsland island : cachedIslands) {
            generateIslandTerrain(chunk, island, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ, terrainBlocks, pos);
        }

        for (CachedIsland island : cachedIslands) {
            decorateIsland(chunk, island, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ, pos);
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

    private static void decorateIsland(
            ChunkAccess chunk,
            CachedIsland island,
            int chunkMinX,
            int chunkMaxX,
            int chunkMinZ,
            int chunkMaxZ,
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

                LakeColumn lake = lakeColumnAt(island, x, z, surfaceY);

                if (lake != null && carveLakeColumn(chunk, column, x, z, surfaceY, lake, pos)) {
                    continue;
                }

                placeUndersideSpur(chunk, column, x, z, island.seed(), pos);
                placeUndersideGrowth(chunk, column, x, z, island.seed(), pos);

                pos.set(x, surfaceY, z);

                if (!chunk.getBlockState(pos).is(RetoldBlocks.AENDER_GRASS_BLOCK)) {
                    continue;
                }

                pos.set(x, surfaceY + 1, z);

                if (!chunk.getBlockState(pos).isAir()) {
                    continue;
                }

                if (isBoulderOrigin(x, z, island.seed())) {
                    placeBoulder(chunk, x, surfaceY, z, island.seed());
                    continue;
                }

                if (isTreeOrigin(x, z, island.seed())
                        && hasTreeRoom(chunk, x, surfaceY, z)
                        && hasGentleGround(island, x, surfaceY, z)) {
                    placeTree(chunk, x, surfaceY, z, island.seed());
                    continue;
                }

                placeGroundDecoration(chunk, x, surfaceY, z, island.seed(), pos);
            }
        }
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
        TerrainBlocks terrainBlocks = TerrainBlocks.create();
        BlockState[] states = new BlockState[AenderIslandSampler.HEIGHT];

        for (int i = 0; i < states.length; i++) {
            int y = AenderIslandSampler.MIN_Y + i;
            int surfaceDepth = AenderIslandSampler.surfaceDepthAt(x, y, z);
            states[i] = surfaceDepth >= 0 ? terrainBlockForDepth(terrainBlocks, 0L, x, z, surfaceDepth) : AIR;
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

    private static List<Entity> entitiesInRegenerationBounds(ServerLevel level, ChunkPos chunkPos) {
        AABB chunkBounds = new AABB(
                chunkPos.getMinBlockX(),
                AenderIslandSampler.MIN_Y,
                chunkPos.getMinBlockZ(),
                chunkPos.getMaxBlockX() + 1,
                AenderIslandSampler.MAX_Y,
                chunkPos.getMaxBlockZ() + 1
        );

        return level.getEntities(
                (Entity) null,
                chunkBounds,
                entity -> !(entity instanceof Player)
        );
    }

    private static void reconcileEntitiesAfterRegeneration(
            ServerLevel level,
            ChunkPos chunkPos,
            List<Entity> entities
    ) {
        for (Entity entity : entities) {
            if (entity.isRemoved() || entity instanceof Player) {
                continue;
            }

            if (isEntityValidAfterRegeneration(level, entity)) {
                continue;
            }

            if (shouldTryPreserveRegeneratedEntity(entity)
                    && moveEntityToRegeneratedSafeSpot(level, chunkPos, entity)) {
                continue;
            }

            entity.discard();
        }
    }

    private static boolean isEntityValidAfterRegeneration(ServerLevel level, Entity entity) {
        return entity.getY() >= AenderIslandSampler.MIN_Y
                && entity.getY() < AenderIslandSampler.MAX_Y
                && level.noCollision(entity, entity.getBoundingBox());
    }

    private static boolean shouldTryPreserveRegeneratedEntity(Entity entity) {
        if (entity.hasCustomName() || entity.isVehicle() || entity.isPassenger()) {
            return true;
        }

        return entity instanceof Mob mob && mob.isPersistenceRequired();
    }

    private static boolean moveEntityToRegeneratedSafeSpot(ServerLevel level, ChunkPos chunkPos, Entity entity) {
        BlockPos origin = entity.blockPosition();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int radius = 0; radius <= 4; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) != radius && Math.abs(dz) != radius) {
                        continue;
                    }

                    int x = origin.getX() + dx;
                    int z = origin.getZ() + dz;

                    if (!isInsideChunk(chunkPos, x, z)) {
                        continue;
                    }

                    int surfaceY = AenderIslandSampler.highestBlockYAt(x, z);

                    if (surfaceY < AenderIslandSampler.MIN_Y) {
                        continue;
                    }

                    int minY = Math.max(AenderIslandSampler.MIN_Y, surfaceY + 1);
                    int maxY = Math.min(AenderIslandSampler.MAX_Y - 1, surfaceY + 8);

                    for (int y = minY; y <= maxY; y++) {
                        pos.set(x, y, z);

                        if (!level.isEmptyBlock(pos) || !level.isEmptyBlock(pos.above())) {
                            continue;
                        }

                        entity.snapTo(
                                x + 0.5D,
                                y,
                                z + 0.5D,
                                entity.getYRot(),
                                entity.getXRot()
                        );

                        if (isEntityValidAfterRegeneration(level, entity)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private static boolean isInsideChunk(ChunkPos chunkPos, int x, int z) {
        return x >= chunkPos.getMinBlockX()
                && x <= chunkPos.getMaxBlockX()
                && z >= chunkPos.getMinBlockZ()
                && z <= chunkPos.getMaxBlockZ();
    }

    private static BlockState terrainBlockForDepth(TerrainBlocks terrainBlocks, long seed, int x, int z, int surfaceDepth) {
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
        int cellX = Math.floorDiv(x, 64);
        int cellZ = Math.floorDiv(z, 64);

        for (int cx = cellX - 1; cx <= cellX + 1; cx++) {
            for (int cz = cellZ - 1; cz <= cellZ + 1; cz++) {
                long lakeSeed = mix64(island.seed()
                        ^ (long) cx * 0xD1342543DE82EF95L
                        ^ (long) cz * 0xC6BC279692B5CC83L
                        ^ 0x1A4E5EADL);

                if (unit(lakeSeed) >= 0.24D) {
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
            int x,
            int surfaceY,
            int z,
            long seed,
            BlockPos.MutableBlockPos pos
    ) {
        double roll = unit(seed
                ^ (long) x * 0xD1342543DE82EF95L
                ^ (long) z * 0xC6BC279692B5CC83L
                ^ 0x61746C696665L);

        if (roll >= 0.34D) {
            return;
        }

        BlockState decoration;

        if (roll < 0.030D) {
            decoration = RetoldBlocks.AENDER_LEAVES.get().defaultBlockState();
        } else if (roll < 0.070D) {
            int flowerIndex = (int) (unit(seed ^ (long) x * 0x9E3779B97F4A7C15L ^ (long) z) * FLOWERS.length);
            decoration = FLOWERS[Math.min(flowerIndex, FLOWERS.length - 1)];
        } else if (roll < 0.130D) {
            decoration = FERN;
        } else {
            decoration = SHORT_GRASS;
        }

        pos.set(x, surfaceY + 1, z);
        chunk.setBlockState(pos, decoration, 0);
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

    private static void placeBoulder(ChunkAccess chunk, int x, int surfaceY, int z, long seed) {
        long boulderSeed = mix64(seed
                ^ (long) x * 0xD1342543DE82EF95L
                ^ (long) z * 0xC6BC279692B5CC83L
                ^ 0xB075D32L);
        int radius = unit(boulderSeed ^ 0x51L) < 0.55D ? 1 : 2;
        int height = radius + 1;
        BlockState stone = RetoldBlocks.AENDER_STONE.get().defaultBlockState();
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

    private static void placeUndersideSpur(
            ChunkAccess chunk,
            AenderIslandSampler.Island.Column column,
            int x,
            int z,
            long seed,
            BlockPos.MutableBlockPos pos
    ) {
        long spurSeed = mix64(seed
                ^ (long) x * 0xA24BAED4963EE407L
                ^ (long) z * 0x9FB21C651E98DF25L
                ^ 0x5F013E5DL);

        if (unit(spurSeed) >= 0.045D || column.minY() <= AenderIslandSampler.MIN_Y + 4) {
            return;
        }

        int length = 2 + (int) (unit(spurSeed ^ 0x61L) * 7.0D);
        BlockState stone = RetoldBlocks.AENDER_STONE.get().defaultBlockState();
        BlockState soil = RetoldBlocks.AENDER_SOIL.get().defaultBlockState();

        for (int dy = 1; dy <= length; dy++) {
            int y = column.minY() - dy;

            if (y < AenderIslandSampler.MIN_Y) {
                break;
            }

            pos.set(x, y, z);
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
            int x,
            int z,
            long seed,
            BlockPos.MutableBlockPos pos
    ) {
        long growthSeed = mix64(seed
                ^ (long) x * 0xD6E8FEB86659FD93L
                ^ (long) z * 0xA5A3564E27F886BFL
                ^ 0xA11FEAFL);

        if (unit(growthSeed) >= 0.025D || column.minY() <= AenderIslandSampler.MIN_Y + 6) {
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

        double grove = unit(mix64(seed
                ^ (long) cellX * 0x8CB92BA72F3D8DD7L
                ^ (long) cellZ * 0xB5AD4ECEDA1CE2A9L
                ^ 0x6500D5L));

        if (unit(cellSeed) >= (grove < 0.38D ? 0.62D : 0.32D)) {
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
        BlockState leaves = RetoldBlocks.AENDER_LEAVES.get().defaultBlockState();
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
            chunk.setBlockState(pos, log, 0);
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
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        return pos.getX() >= minX
                && pos.getX() <= minX + 15
                && pos.getZ() >= minZ
                && pos.getZ() <= minZ + 15
                && pos.getY() >= AenderIslandSampler.MIN_Y
                && pos.getY() < AenderIslandSampler.MAX_Y;
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

    private record TerrainBlocks(BlockState grass, BlockState soil, BlockState stone) {
        private static TerrainBlocks create() {
            return new TerrainBlocks(
                    RetoldBlocks.AENDER_GRASS_BLOCK.get().defaultBlockState(),
                    RetoldBlocks.AENDER_SOIL.get().defaultBlockState(),
                    RetoldBlocks.AENDER_STONE.get().defaultBlockState()
            );
        }
    }

    private record LakeColumn(int waterY, double distance) {
    }
}
