package cz.xefensor.retold.behavior.performance;

import java.util.Locale;
import java.util.concurrent.atomic.LongAdder;

public final class RetoldBehaviorPerf {
    private static final LongAdder TIMING_CHECKS = new LongAdder();
    private static final LongAdder TIMING_PASSES = new LongAdder();
    private static final LongAdder TIMING_CACHE_HITS = new LongAdder();
    private static final LongAdder LOD_FULL = new LongAdder();
    private static final LongAdder LOD_NEAR = new LongAdder();
    private static final LongAdder LOD_FAR = new LongAdder();
    private static final LongAdder LOD_BACKGROUND = new LongAdder();
    private static final LongAdder TERRITORY_NEARBY_CHECKS = new LongAdder();
    private static final LongAdder TERRITORY_NEARBY_PASSES = new LongAdder();
    private static final LongAdder TERRITORY_CONTEXT_LOOKUPS = new LongAdder();
    private static final LongAdder TERRITORY_CONTEXT_HITS = new LongAdder();
    private static final LongAdder TERRITORY_FACTION_MOB_REQUESTS = new LongAdder();
    private static final LongAdder TERRITORY_FACTION_MOB_CACHE_HITS = new LongAdder();
    private static final LongAdder AI_SCAN_REQUESTS = new LongAdder();
    private static final LongAdder AI_SCAN_CACHE_HITS = new LongAdder();
    private static final LongAdder AI_SCAN_BUDGET_SKIPS = new LongAdder();
    private static final LongAdder AI_POSITION_SCAN_REQUESTS = new LongAdder();
    private static final LongAdder AI_POSITION_SCAN_CACHE_HITS = new LongAdder();
    private static final LongAdder AI_POSITION_SCAN_BUDGET_SKIPS = new LongAdder();
    private static final LongAdder PATH_REQUESTS = new LongAdder();
    private static final LongAdder PATH_SKIPS = new LongAdder();
    private static final LongAdder SIGHT_REQUESTS = new LongAdder();
    private static final LongAdder SIGHT_CACHE_HITS = new LongAdder();
    private static final LongAdder SIGHT_BUDGET_SKIPS = new LongAdder();
    private static final LongAdder BLOCK_SEARCH_REQUESTS = new LongAdder();
    private static final LongAdder BLOCK_SEARCH_CACHE_HITS = new LongAdder();
    private static final LongAdder BLOCK_SEARCH_BUDGET_SKIPS = new LongAdder();

    private RetoldBehaviorPerf() {
    }

    public static void recordTiming(boolean passed) {
        TIMING_CHECKS.increment();

        if (passed) {
            TIMING_PASSES.increment();
        }
    }

    public static void recordTimingCacheHit() {
        TIMING_CACHE_HITS.increment();
    }

    public static void recordLod(RetoldAiLodLevel level) {
        if (level == null) {
            return;
        }

        switch (level) {
            case FULL -> LOD_FULL.increment();
            case NEAR -> LOD_NEAR.increment();
            case FAR -> LOD_FAR.increment();
            case BACKGROUND -> LOD_BACKGROUND.increment();
        }
    }

    public static void recordTerritoryNearby(boolean passed) {
        TERRITORY_NEARBY_CHECKS.increment();

        if (passed) {
            TERRITORY_NEARBY_PASSES.increment();
        }
    }

    public static void recordTerritoryContext(boolean hit) {
        TERRITORY_CONTEXT_LOOKUPS.increment();

        if (hit) {
            TERRITORY_CONTEXT_HITS.increment();
        }
    }

    public static void recordTerritoryFactionMobCache(boolean cacheHit) {
        TERRITORY_FACTION_MOB_REQUESTS.increment();

        if (cacheHit) {
            TERRITORY_FACTION_MOB_CACHE_HITS.increment();
        }
    }

    public static void recordAiScanCache(boolean cacheHit) {
        AI_SCAN_REQUESTS.increment();

        if (cacheHit) {
            AI_SCAN_CACHE_HITS.increment();
        }
    }

    public static void recordAiScanBudgetSkip() {
        AI_SCAN_BUDGET_SKIPS.increment();
    }

    public static void recordAiPositionScanCache(boolean cacheHit) {
        AI_POSITION_SCAN_REQUESTS.increment();

        if (cacheHit) {
            AI_POSITION_SCAN_CACHE_HITS.increment();
        }
    }

    public static void recordAiPositionScanBudgetSkip() {
        AI_POSITION_SCAN_BUDGET_SKIPS.increment();
    }

    public static void recordPathRequest(boolean skipped) {
        PATH_REQUESTS.increment();

        if (skipped) {
            PATH_SKIPS.increment();
        }
    }

    public static void recordSightCache(boolean cacheHit) {
        SIGHT_REQUESTS.increment();

        if (cacheHit) {
            SIGHT_CACHE_HITS.increment();
        }
    }

    public static void recordSightBudgetSkip() {
        SIGHT_BUDGET_SKIPS.increment();
    }

    public static void recordBlockSearchCache(boolean cacheHit) {
        BLOCK_SEARCH_REQUESTS.increment();

        if (cacheHit) {
            BLOCK_SEARCH_CACHE_HITS.increment();
        }
    }

    public static void recordBlockSearchBudgetSkip() {
        BLOCK_SEARCH_BUDGET_SKIPS.increment();
    }

    public static void reset() {
        TIMING_CHECKS.reset();
        TIMING_PASSES.reset();
        TIMING_CACHE_HITS.reset();
        LOD_FULL.reset();
        LOD_NEAR.reset();
        LOD_FAR.reset();
        LOD_BACKGROUND.reset();
        TERRITORY_NEARBY_CHECKS.reset();
        TERRITORY_NEARBY_PASSES.reset();
        TERRITORY_CONTEXT_LOOKUPS.reset();
        TERRITORY_CONTEXT_HITS.reset();
        TERRITORY_FACTION_MOB_REQUESTS.reset();
        TERRITORY_FACTION_MOB_CACHE_HITS.reset();
        AI_SCAN_REQUESTS.reset();
        AI_SCAN_CACHE_HITS.reset();
        AI_SCAN_BUDGET_SKIPS.reset();
        AI_POSITION_SCAN_REQUESTS.reset();
        AI_POSITION_SCAN_CACHE_HITS.reset();
        AI_POSITION_SCAN_BUDGET_SKIPS.reset();
        PATH_REQUESTS.reset();
        PATH_SKIPS.reset();
        SIGHT_REQUESTS.reset();
        SIGHT_CACHE_HITS.reset();
        SIGHT_BUDGET_SKIPS.reset();
        BLOCK_SEARCH_REQUESTS.reset();
        BLOCK_SEARCH_CACHE_HITS.reset();
        BLOCK_SEARCH_BUDGET_SKIPS.reset();
    }

    public static String debugText() {
        long timingChecks = TIMING_CHECKS.sum();
        long timingPasses = TIMING_PASSES.sum();
        long timingCacheHits = TIMING_CACHE_HITS.sum();
        long lodFull = LOD_FULL.sum();
        long lodNear = LOD_NEAR.sum();
        long lodFar = LOD_FAR.sum();
        long lodBackground = LOD_BACKGROUND.sum();
        long nearbyChecks = TERRITORY_NEARBY_CHECKS.sum();
        long nearbyPasses = TERRITORY_NEARBY_PASSES.sum();
        long contextLookups = TERRITORY_CONTEXT_LOOKUPS.sum();
        long contextHits = TERRITORY_CONTEXT_HITS.sum();
        long factionMobRequests = TERRITORY_FACTION_MOB_REQUESTS.sum();
        long factionMobCacheHits = TERRITORY_FACTION_MOB_CACHE_HITS.sum();
        long aiScanRequests = AI_SCAN_REQUESTS.sum();
        long aiScanCacheHits = AI_SCAN_CACHE_HITS.sum();
        long aiScanBudgetSkips = AI_SCAN_BUDGET_SKIPS.sum();
        long aiPositionScanRequests = AI_POSITION_SCAN_REQUESTS.sum();
        long aiPositionScanCacheHits = AI_POSITION_SCAN_CACHE_HITS.sum();
        long aiPositionScanBudgetSkips = AI_POSITION_SCAN_BUDGET_SKIPS.sum();
        long pathRequests = PATH_REQUESTS.sum();
        long pathSkips = PATH_SKIPS.sum();
        long sightRequests = SIGHT_REQUESTS.sum();
        long sightCacheHits = SIGHT_CACHE_HITS.sum();
        long sightBudgetSkips = SIGHT_BUDGET_SKIPS.sum();
        long blockSearchRequests = BLOCK_SEARCH_REQUESTS.sum();
        long blockSearchCacheHits = BLOCK_SEARCH_CACHE_HITS.sum();
        long blockSearchBudgetSkips = BLOCK_SEARCH_BUDGET_SKIPS.sum();

        return "Retold behavior perf"
                + "\nTiming checks: " + timingChecks
                + "\nTiming passes: " + timingPasses
                + " (" + percentText(timingPasses, timingChecks) + ")"
                + "\nTiming cache hits: " + timingCacheHits
                + " (" + percentText(timingCacheHits, timingChecks) + ")"
                + "\nLOD full/near/far/background: "
                + lodFull + "/" + lodNear + "/" + lodFar + "/" + lodBackground
                + "\nTerritory nearby checks: " + nearbyChecks
                + "\nTerritory nearby passes: " + nearbyPasses
                + " (" + percentText(nearbyPasses, nearbyChecks) + ")"
                + "\nTerritory context lookups: " + contextLookups
                + "\nTerritory context hits: " + contextHits
                + " (" + percentText(contextHits, contextLookups) + ")"
                + "\nTerritory faction mob requests: " + factionMobRequests
                + "\nTerritory faction mob cache hits: " + factionMobCacheHits
                + " (" + percentText(factionMobCacheHits, factionMobRequests) + ")"
                + "\nAI scan requests: " + aiScanRequests
                + "\nAI scan cache hits: " + aiScanCacheHits
                + " (" + percentText(aiScanCacheHits, aiScanRequests) + ")"
                + "\nAI scan budget skips: " + aiScanBudgetSkips
                + "\nAI position scan requests: " + aiPositionScanRequests
                + "\nAI position scan cache hits: " + aiPositionScanCacheHits
                + " (" + percentText(aiPositionScanCacheHits, aiPositionScanRequests) + ")"
                + "\nAI position scan budget skips: " + aiPositionScanBudgetSkips
                + "\nPath requests: " + pathRequests
                + "\nPath skips: " + pathSkips
                + " (" + percentText(pathSkips, pathRequests) + ")"
                + "\nSight requests: " + sightRequests
                + "\nSight cache hits: " + sightCacheHits
                + " (" + percentText(sightCacheHits, sightRequests) + ")"
                + "\nSight budget skips: " + sightBudgetSkips
                + "\nBlock search requests: " + blockSearchRequests
                + "\nBlock search cache hits: " + blockSearchCacheHits
                + " (" + percentText(blockSearchCacheHits, blockSearchRequests) + ")"
                + "\nBlock search budget skips: " + blockSearchBudgetSkips;
    }

    private static String percentText(
            long value,
            long total
    ) {
        if (total <= 0L) {
            return "0.0%";
        }

        return String.format(
                Locale.ROOT,
                "%.1f%%",
                value * 100.0D / total
        );
    }
}
