package cz.xefensor.retold.behavior.performance;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class RetoldAiScanCache {
    private static final double MAX_CENTER_DRIFT_SQUARED = 3.5D * 3.5D;
    private static final double SHARED_SCAN_BUCKET_SIZE_BLOCKS = 16.0D;
    private static final double SHARED_SCAN_PADDING_BLOCKS = SHARED_SCAN_BUCKET_SIZE_BLOCKS;
    private static final double SHARED_RADIUS_BUCKET_BLOCKS = 4.0D;
    private static final int MIN_SHARED_SCAN_CACHE_TICKS = 20;
    private static final int MIN_POSITION_SCAN_CACHE_TICKS = 30;
    private static final int STALE_SHARED_SCAN_GRACE_TICKS = 40;
    private static final int SCAN_CLEANUP_INTERVAL_TICKS = 20;

    private static final Map<Entity, List<ScanEntry>> SCANS = new WeakHashMap<>();
    private static final Map<ServerLevel, Map<SharedScanKey, SharedScanEntry>> SHARED_SCANS = new WeakHashMap<>();
    private static final Map<ServerLevel, Long> SHARED_SCAN_CLEANUP_AT = new WeakHashMap<>();
    private static final Map<ServerLevel, List<PositionScanEntry>> POSITION_SCANS = new WeakHashMap<>();
    private static final Map<ServerLevel, Long> POSITION_SCAN_CLEANUP_AT = new WeakHashMap<>();

    private RetoldAiScanCache() {
    }

    public static synchronized <T extends Entity> List<T> nearby(
            ServerLevel level,
            Entity center,
            Class<T> type,
            double radius,
            long gameTime,
            int cacheTicks
    ) {
        if (level == null || center == null || type == null || radius <= 0.0D) {
            return List.of();
        }

        AABB area = center.getBoundingBox().inflate(radius);
        List<ScanEntry> entries = SCANS.computeIfAbsent(
                center,
                ignored -> new ArrayList<>()
        );

        long radiusKey = Double.doubleToLongBits(radius);

        for (ScanEntry entry : entries) {
            if (
                    entry.type == type
                            && entry.radiusKey == radiusKey
                            && gameTime < entry.expiresAt
                            && center.distanceToSqr(entry.centerX, entry.centerY, entry.centerZ) <= MAX_CENTER_DRIFT_SQUARED
            ) {
                RetoldBehaviorPerf.recordAiScanCache(true);
                return cast(entry.results);
            }
        }

        int adjustedCacheTicks = RetoldAiLod.cacheTicks(center, cacheTicks);
        List<T> results = scanNearbyShared(
                level,
                center,
                type,
                radius,
                area,
                gameTime,
                adjustedCacheTicks
        );

        ScanEntry entry = new ScanEntry(
                type,
                radiusKey,
                Math.max(gameTime, gameTime + Math.max(1, adjustedCacheTicks)),
                center.getX(),
                center.getY(),
                center.getZ(),
                List.copyOf(results)
        );

        entries.removeIf(existing -> existing.type == type && existing.radiusKey == radiusKey);
        entries.add(entry);

        return results;
    }

    private static <T extends Entity> List<T> scanNearbyShared(
            ServerLevel level,
            Entity center,
            Class<T> type,
            double radius,
            AABB area,
            long gameTime,
            int cacheTicks
    ) {
        Map<SharedScanKey, SharedScanEntry> sharedEntries = SHARED_SCANS.computeIfAbsent(
                level,
                ignored -> new HashMap<>()
        );

        double sharedRadius = sharedRadius(radius);
        SharedScanKey key = new SharedScanKey(
                type,
                Double.doubleToLongBits(sharedRadius),
                bucket(center.getX()),
                bucket(center.getY()),
                bucket(center.getZ())
        );

        SharedScanEntry sharedEntry = sharedEntries.get(key);

        if (sharedEntry != null && gameTime < sharedEntry.expiresAt) {
            RetoldBehaviorPerf.recordAiScanCache(true);
            return filterToArea(sharedEntry.results, type, area);
        }

        if (!RetoldAiWorkBudget.tryUseEntityScan(gameTime)) {
            RetoldBehaviorPerf.recordAiScanCache(false);
            RetoldBehaviorPerf.recordAiScanBudgetSkip();

            if (sharedEntry != null && gameTime < sharedEntry.staleExpiresAt()) {
                return filterToArea(sharedEntry.results, type, area);
            }

            return List.of();
        }

        cleanupSharedScansIfNeeded(
                level,
                sharedEntries,
                gameTime
        );

        RetoldBehaviorPerf.recordAiScanCache(false);

        AABB broadArea = sharedScanArea(
                key.bucketX,
                key.bucketY,
                key.bucketZ,
                sharedRadius
        );
        List<T> broadResults = level.getEntitiesOfClass(
                type,
                broadArea
        );

            sharedEntries.put(
                    key,
                    new SharedScanEntry(
                        gameTime + Math.max(MIN_SHARED_SCAN_CACHE_TICKS, cacheTicks),
                        List.copyOf(broadResults)
                )
        );

        return filterToArea(
                broadResults,
                type,
                area
        );
    }

    private static int bucket(double coordinate) {
        return (int) Math.floor(coordinate / SHARED_SCAN_BUCKET_SIZE_BLOCKS);
    }

    private static double sharedRadius(double radius) {
        return Math.ceil(radius / SHARED_RADIUS_BUCKET_BLOCKS) * SHARED_RADIUS_BUCKET_BLOCKS;
    }

    private static AABB sharedScanArea(
            int bucketX,
            int bucketY,
            int bucketZ,
            double radius
    ) {
        double minX = bucketX * SHARED_SCAN_BUCKET_SIZE_BLOCKS;
        double minY = bucketY * SHARED_SCAN_BUCKET_SIZE_BLOCKS;
        double minZ = bucketZ * SHARED_SCAN_BUCKET_SIZE_BLOCKS;
        double maxX = minX + SHARED_SCAN_BUCKET_SIZE_BLOCKS;
        double maxY = minY + SHARED_SCAN_BUCKET_SIZE_BLOCKS;
        double maxZ = minZ + SHARED_SCAN_BUCKET_SIZE_BLOCKS;

        return new AABB(
                minX,
                minY,
                minZ,
                maxX,
                maxY,
                maxZ
        ).inflate(radius + SHARED_SCAN_PADDING_BLOCKS);
    }

    private static <T extends Entity> List<T> filterToArea(
            List<? extends Entity> entities,
            Class<T> type,
            AABB area
    ) {
        if (entities.isEmpty()) {
            return List.of();
        }

        List<T> filtered = new ArrayList<>();

        for (Entity entity : entities) {
            if (
                    type.isInstance(entity)
                            && entity.isAlive()
                            && !entity.isRemoved()
                            && entity.getBoundingBox().intersects(area)
            ) {
                filtered.add(type.cast(entity));
            }
        }

        return filtered;
    }

    private static void cleanupSharedScansIfNeeded(
            ServerLevel level,
            Map<SharedScanKey, SharedScanEntry> entries,
            long gameTime
    ) {
        long nextCleanupAt = SHARED_SCAN_CLEANUP_AT.getOrDefault(level, 0L);

        if (gameTime < nextCleanupAt) {
            return;
        }

        SHARED_SCAN_CLEANUP_AT.put(
                level,
                gameTime + SCAN_CLEANUP_INTERVAL_TICKS
        );
        entries.values().removeIf(entry -> gameTime >= entry.staleExpiresAt());
    }

    private static void cleanupPositionScansIfNeeded(
            ServerLevel level,
            List<PositionScanEntry> entries,
            long gameTime
    ) {
        long nextCleanupAt = POSITION_SCAN_CLEANUP_AT.getOrDefault(level, 0L);

        if (gameTime < nextCleanupAt) {
            return;
        }

        POSITION_SCAN_CLEANUP_AT.put(
                level,
                gameTime + SCAN_CLEANUP_INTERVAL_TICKS
        );
        entries.removeIf(entry -> gameTime >= entry.expiresAt);
    }

    public static synchronized <T extends Entity> List<T> nearbyAt(
            ServerLevel level,
            BlockPos center,
            Class<T> type,
            double radius,
            long gameTime,
            int cacheTicks
    ) {
        if (level == null || center == null || type == null || radius <= 0.0D) {
            return List.of();
        }

        BlockPos immutableCenter = center.immutable();
        long radiusKey = Double.doubleToLongBits(radius);
        List<PositionScanEntry> entries = POSITION_SCANS.computeIfAbsent(
                level,
                ignored -> new ArrayList<>()
        );

        cleanupPositionScansIfNeeded(level, entries, gameTime);

        PositionScanEntry staleEntry = null;

        for (PositionScanEntry entry : entries) {
            if (
                    entry.type == type
                            && entry.radiusKey == radiusKey
                            && entry.center.equals(immutableCenter)
            ) {
                staleEntry = entry;

                if (gameTime >= entry.expiresAt) {
                    continue;
                }

                RetoldBehaviorPerf.recordAiPositionScanCache(true);
                return cast(entry.results);
            }
        }

        if (!RetoldAiWorkBudget.tryUsePositionScan(gameTime)) {
            RetoldBehaviorPerf.recordAiPositionScanCache(false);
            RetoldBehaviorPerf.recordAiPositionScanBudgetSkip();
            return staleEntry != null ? cast(staleEntry.results) : List.of();
        }

        RetoldBehaviorPerf.recordAiPositionScanCache(false);

        AABB area = new AABB(immutableCenter).inflate(radius);
        List<T> results = level.getEntitiesOfClass(
                type,
                area
        );

        entries.add(new PositionScanEntry(
                type,
                radiusKey,
                gameTime + Math.max(MIN_POSITION_SCAN_CACHE_TICKS, cacheTicks),
                immutableCenter,
                List.copyOf(results)
        ));

        return results;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Entity> List<T> cast(List<? extends Entity> entities) {
        return (List<T>) entities;
    }

    private record ScanEntry(
            Class<? extends Entity> type,
            long radiusKey,
            long expiresAt,
            double centerX,
            double centerY,
            double centerZ,
            List<? extends Entity> results
    ) {
    }

    private record SharedScanKey(
            Class<? extends Entity> type,
            long radiusKey,
            int bucketX,
            int bucketY,
            int bucketZ
    ) {
    }

    private record SharedScanEntry(
            long expiresAt,
            List<? extends Entity> results
    ) {
        private long staleExpiresAt() {
            return expiresAt + STALE_SHARED_SCAN_GRACE_TICKS;
        }
    }

    private record PositionScanEntry(
            Class<? extends Entity> type,
            long radiusKey,
            long expiresAt,
            BlockPos center,
            List<? extends Entity> results
    ) {
    }
}
