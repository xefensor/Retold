package cz.xefensor.retold.territory;

import cz.xefensor.retold.faction.RetoldFaction;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.HashMap;
import java.util.Map;

public final class RetoldTerritoryDetector {
    private static final int STRUCTURE_SEARCH_RADIUS_CHUNKS = 12;
    private static final double TERRITORY_RADIUS_BLOCKS = 64.0D;

    private static final int TERRITORY_PIECE_PADDING_CHUNKS = 2;
    private static final int TERRITORY_PIECE_SCAN_STEP_BLOCKS = 8;
    private static final int TERRITORY_PIECE_VERTICAL_SCAN_STEP_BLOCKS = 8;
    private static final int TERRITORY_PIECE_VERTICAL_SCAN_RANGE_BLOCKS = 24;

    private static final int TERRITORY_CACHE_TICKS = 100;
    private static final int MAX_TERRITORY_CACHE_SIZE = 4096;

    private static final Map<TerritoryCacheKey, TerritoryCacheEntry> TERRITORY_CACHE =
            new HashMap<>();

    private RetoldTerritoryDetector() {
    }

    public static RetoldTerritoryContext getContextAt(ServerLevel level, BlockPos pos) {
        long gameTime = level.getGameTime();

        for (RetoldTerritoryConfig config : RetoldTerritoryConfigs.all()) {
            if (!isInAllowedDimension(level, config)) {
                continue;
            }

            if (!isNearTerritory(level, pos, config, gameTime)) {
                continue;
            }

            BlockPos structurePos = findTerritoryAnchor(level, pos, config);

            if (structurePos == null) {
                continue;
            }

            return new RetoldTerritoryContext(
                    config.faction,
                    getDimensionId(level),
                    structurePos.getX(),
                    structurePos.getZ()
            );
        }

        return null;
    }

    public static RetoldFaction getFactionAt(ServerLevel level, BlockPos pos) {
        RetoldTerritoryContext context = getContextAt(level, pos);
        return context == null ? null : context.faction();
    }

    public static boolean isInAllowedDimension(
            ServerLevel level,
            RetoldTerritoryConfig config
    ) {
        return config.requiredDimension == null || level.dimension() == config.requiredDimension;
    }

    public static boolean isNearTerritory(
            ServerLevel level,
            Entity entity,
            RetoldTerritoryConfig config,
            long gameTime
    ) {
        return isNearTerritory(level, entity.blockPosition(), config, gameTime);
    }

    public static boolean isNearTerritory(
            ServerLevel level,
            BlockPos pos,
            RetoldTerritoryConfig config,
            long gameTime
    ) {
        long chunkKey = chunkKey(pos);

        TerritoryCacheKey cacheKey = new TerritoryCacheKey(
                config.faction,
                level.dimension(),
                chunkKey
        );

        TerritoryCacheEntry cached = TERRITORY_CACHE.get(cacheKey);

        if (cached != null && cached.expiresAt >= gameTime) {
            return cached.nearTerritory;
        }

        boolean nearTerritory = computeNearTerritory(level, pos, config);

        if (TERRITORY_CACHE.size() > MAX_TERRITORY_CACHE_SIZE) {
            TERRITORY_CACHE.clear();
        }

        TERRITORY_CACHE.put(
                cacheKey,
                new TerritoryCacheEntry(nearTerritory, gameTime + TERRITORY_CACHE_TICKS)
        );

        return nearTerritory;
    }

    public static BlockPos findTerritoryAnchor(
            ServerLevel level,
            BlockPos pos,
            RetoldTerritoryConfig config
    ) {
        BlockPos structurePos = level.findNearestMapStructure(
                config.territoryTag,
                pos,
                STRUCTURE_SEARCH_RADIUS_CHUNKS,
                false
        );

        if (structurePos != null) {
            return structurePos;
        }

        BlockPos nearbyPiecePos = findNearbyTerritoryPiece(level, pos, config);

        if (nearbyPiecePos != null) {
            return new BlockPos(
                    nearbyPiecePos.getX() >> 4 << 4,
                    nearbyPiecePos.getY(),
                    nearbyPiecePos.getZ() >> 4 << 4
            );
        }

        return null;
    }

    private static boolean computeNearTerritory(
            ServerLevel level,
            BlockPos pos,
            RetoldTerritoryConfig config
    ) {
        if (findNearbyTerritoryPiece(level, pos, config) != null) {
            return true;
        }

        BlockPos structurePos = level.findNearestMapStructure(
                config.territoryTag,
                pos,
                STRUCTURE_SEARCH_RADIUS_CHUNKS,
                false
        );

        if (structurePos == null) {
            return false;
        }

        double dx = structurePos.getX() + 0.5D - pos.getX();
        double dz = structurePos.getZ() + 0.5D - pos.getZ();

        return dx * dx + dz * dz <= TERRITORY_RADIUS_BLOCKS * TERRITORY_RADIUS_BLOCKS;
    }

    private static BlockPos findNearbyTerritoryPiece(
            ServerLevel level,
            BlockPos pos,
            RetoldTerritoryConfig config
    ) {
        int paddingBlocks = TERRITORY_PIECE_PADDING_CHUNKS * 16;

        for (int dx = -paddingBlocks; dx <= paddingBlocks; dx += TERRITORY_PIECE_SCAN_STEP_BLOCKS) {
            for (int dz = -paddingBlocks; dz <= paddingBlocks; dz += TERRITORY_PIECE_SCAN_STEP_BLOCKS) {
                for (
                        int dy = -TERRITORY_PIECE_VERTICAL_SCAN_RANGE_BLOCKS;
                        dy <= TERRITORY_PIECE_VERTICAL_SCAN_RANGE_BLOCKS;
                        dy += TERRITORY_PIECE_VERTICAL_SCAN_STEP_BLOCKS
                ) {
                    BlockPos samplePos = pos.offset(dx, dy, dz);

                    StructureStart structureStart = level.structureManager()
                            .getStructureWithPieceAt(samplePos, config.territoryTag);

                    if (structureStart != null && structureStart.isValid()) {
                        return samplePos;
                    }
                }
            }
        }

        return null;
    }

    private static String getDimensionId(ServerLevel level) {
        return level.dimension().toString();
    }

    private static long chunkKey(BlockPos pos) {
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;

        return ((long) chunkX & 4294967295L) | (((long) chunkZ & 4294967295L) << 32);
    }

    private record TerritoryCacheKey(
            RetoldFaction faction,
            ResourceKey<Level> dimension,
            long chunkKey
    ) {
    }

    private record TerritoryCacheEntry(
            boolean nearTerritory,
            long expiresAt
    ) {
    }
}