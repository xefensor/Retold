package cz.xefensor.retold.behavior.performance;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.Deque;

import net.minecraft.world.entity.Entity;

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
    private static final int FAIR_BLOCK_SEARCH_WAIT_TICKS = 40;
    private static final Deque<FairBlockSearchWaiter> FAIR_BLOCK_SEARCH_WAITERS =
            new ArrayDeque<>();

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

    public static synchronized boolean tryUseBlockSearch(long gameTime) {
        cleanFairBlockSearchWaiters(gameTime);
        if (blockSearchTick != gameTime) {
            blockSearchTick = gameTime;
            blockSearchesThisTick = 0;
        }

        int limit = FAIR_BLOCK_SEARCH_WAITERS.isEmpty()
                ? MAX_BLOCK_SEARCHES_PER_TICK
                : MAX_BLOCK_SEARCHES_PER_TICK - 1;

        if (blockSearchesThisTick >= limit) {
            return false;
        }

        blockSearchesThisTick++;
        return true;
    }

    public static synchronized boolean tryUseFairBlockSearch(
            Object claimant,
            long gameTime
    ) {
        if (claimant == null) {
            return tryUseBlockSearch(gameTime);
        }

        cleanFairBlockSearchWaiters(gameTime);

        if (blockSearchTick != gameTime) {
            blockSearchTick = gameTime;
            blockSearchesThisTick = 0;
        }

        Object first = firstFairBlockSearchWaiter();

        if (first != null && first != claimant) {
            enqueueFairBlockSearchWaiter(claimant, gameTime);
            return false;
        }

        if (blockSearchesThisTick >= MAX_BLOCK_SEARCHES_PER_TICK) {
            enqueueFairBlockSearchWaiter(claimant, gameTime);
            return false;
        }

        if (first == claimant) {
            FAIR_BLOCK_SEARCH_WAITERS.removeFirst();
        }

        blockSearchesThisTick++;
        return true;
    }

    private static Object firstFairBlockSearchWaiter() {
        FairBlockSearchWaiter first = FAIR_BLOCK_SEARCH_WAITERS.peekFirst();
        return first == null ? null : first.claimant.get();
    }

    private static void enqueueFairBlockSearchWaiter(
            Object claimant,
            long gameTime
    ) {
        for (FairBlockSearchWaiter waiter : FAIR_BLOCK_SEARCH_WAITERS) {
            if (waiter.claimant.get() == claimant) {
                waiter.expiresAt = gameTime + FAIR_BLOCK_SEARCH_WAIT_TICKS;
                return;
            }
        }

        FAIR_BLOCK_SEARCH_WAITERS.addLast(new FairBlockSearchWaiter(
                claimant,
                gameTime + FAIR_BLOCK_SEARCH_WAIT_TICKS
        ));
    }

    private static void cleanFairBlockSearchWaiters(long gameTime) {
        FAIR_BLOCK_SEARCH_WAITERS.removeIf(waiter -> {
            Object claimant = waiter.claimant.get();

            return claimant == null
                    || gameTime > waiter.expiresAt
                    || claimant instanceof Entity entity
                    && (!entity.isAlive() || entity.isRemoved());
        });
    }

    private static final class FairBlockSearchWaiter {
        private final WeakReference<Object> claimant;
        private long expiresAt;

        private FairBlockSearchWaiter(Object claimant, long expiresAt) {
            this.claimant = new WeakReference<>(claimant);
            this.expiresAt = expiresAt;
        }
    }

    static long maximumEntityScansOver(int ticks) {
        return maximumWorkOver(MAX_ENTITY_SCANS_PER_TICK, ticks);
    }

    static long maximumPositionScansOver(int ticks) {
        return maximumWorkOver(MAX_POSITION_SCANS_PER_TICK, ticks);
    }

    static long maximumSightRaycastsOver(int ticks) {
        return maximumWorkOver(MAX_SIGHT_RAYCASTS_PER_TICK, ticks);
    }

    static long maximumBlockSearchesOver(int ticks) {
        return maximumWorkOver(MAX_BLOCK_SEARCHES_PER_TICK, ticks);
    }

    private static long maximumWorkOver(
            int maximumPerTick,
            int ticks
    ) {
        return (long) maximumPerTick * Math.max(0, ticks);
    }

}
