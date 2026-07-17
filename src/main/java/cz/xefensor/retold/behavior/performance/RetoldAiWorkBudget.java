package cz.xefensor.retold.behavior.performance;

public final class RetoldAiWorkBudget {
    private static final int MAX_ENTITY_SCANS_PER_TICK = 14;
    private static final int MAX_POSITION_SCANS_PER_TICK = 6;
    private static final int MAX_SIGHT_RAYCASTS_PER_TICK = 96;
    private static final int MAX_BLOCK_SEARCHES_PER_TICK = 8;

    private static long entityScanTick = Long.MIN_VALUE;
    private static int entityScansThisTick;
    private static long positionScanTick = Long.MIN_VALUE;
    private static int positionScansThisTick;
    private static long sightRaycastTick = Long.MIN_VALUE;
    private static int sightRaycastsThisTick;
    private static long blockSearchTick = Long.MIN_VALUE;
    private static int blockSearchesThisTick;

    private RetoldAiWorkBudget() {
    }

    public static boolean tryUseEntityScan(long gameTime) {
        if (entityScanTick != gameTime) {
            entityScanTick = gameTime;
            entityScansThisTick = 0;
        }

        if (entityScansThisTick >= MAX_ENTITY_SCANS_PER_TICK) {
            return false;
        }

        entityScansThisTick++;
        return true;
    }

    public static boolean tryUsePositionScan(long gameTime) {
        if (positionScanTick != gameTime) {
            positionScanTick = gameTime;
            positionScansThisTick = 0;
        }

        if (positionScansThisTick >= MAX_POSITION_SCANS_PER_TICK) {
            return false;
        }

        positionScansThisTick++;
        return true;
    }

    public static boolean tryUseSightRaycast(long gameTime) {
        if (sightRaycastTick != gameTime) {
            sightRaycastTick = gameTime;
            sightRaycastsThisTick = 0;
        }

        if (sightRaycastsThisTick >= MAX_SIGHT_RAYCASTS_PER_TICK) {
            return false;
        }

        sightRaycastsThisTick++;
        return true;
    }

    public static boolean tryUseBlockSearch(long gameTime) {
        if (blockSearchTick != gameTime) {
            blockSearchTick = gameTime;
            blockSearchesThisTick = 0;
        }

        if (blockSearchesThisTick >= MAX_BLOCK_SEARCHES_PER_TICK) {
            return false;
        }

        blockSearchesThisTick++;
        return true;
    }
}
