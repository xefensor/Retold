package cz.xefensor.retold.behavior.performance;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class RetoldAiSightCache {
    private static final int DEFAULT_CACHE_TICKS = 10;
    private static final int MIN_CACHE_TICKS = 10;
    private static final int CLEANUP_INTERVAL_TICKS = 20;

    private static final Map<Mob, List<SightEntry>> SIGHT = new WeakHashMap<>();
    private static long nextCleanupAt;

    private RetoldAiSightCache() {
    }

    public static synchronized boolean canSee(
            Mob observer,
            Entity target,
            long gameTime
    ) {
        return canSee(
                observer,
                target,
                gameTime,
                DEFAULT_CACHE_TICKS
        );
    }

    public static synchronized boolean canSee(
            Mob observer,
            Entity target,
            long gameTime,
            int cacheTicks
    ) {
        if (observer == null || target == null || observer.level() != target.level()) {
            return false;
        }

        List<SightEntry> entries = SIGHT.computeIfAbsent(
                observer,
                ignored -> new ArrayList<>()
        );

        cleanupIfNeeded(gameTime);

        SightEntry staleEntry = null;

        for (SightEntry entry : entries) {
            if (entry.target != target) {
                continue;
            }

            double observerDriftSquared = observer.distanceToSqr(
                    entry.observerX,
                    entry.observerY,
                    entry.observerZ
            );
            double targetDriftSquared = target.distanceToSqr(
                    entry.targetX,
                    entry.targetY,
                    entry.targetZ
            );

            if (
                    staleEntry == null
                            && RetoldAiSightCachePolicy.isStaleCandidate(
                                    observerDriftSquared,
                                    targetDriftSquared
                            )
            ) {
                staleEntry = entry;
            }

            if (RetoldAiSightCachePolicy.isNormalCacheHit(
                    gameTime,
                    entry.expiresAt,
                    observerDriftSquared,
                    targetDriftSquared
            )) {
                RetoldBehaviorPerf.recordSightCache(true);
                return entry.visible;
            }
        }

        if (!RetoldAiWorkBudget.tryUseSightRaycast(gameTime)) {
            RetoldBehaviorPerf.recordSightCache(false);
            RetoldBehaviorPerf.recordSightBudgetSkip();
            return staleEntry != null && staleEntry.visible;
        }

        RetoldBehaviorPerf.recordSightCache(false);

        boolean visible = observer.hasLineOfSight(target);

        entries.removeIf(entry -> entry.target == target);
        entries.add(new SightEntry(
                target,
                visible,
                gameTime + Math.max(MIN_CACHE_TICKS, RetoldAiLod.cacheTicks(observer, cacheTicks)),
                observer.getX(),
                observer.getY(),
                observer.getZ(),
                target.getX(),
                target.getY(),
                target.getZ()
        ));

        return visible;
    }

    private static void cleanupIfNeeded(long gameTime) {
        if (gameTime < nextCleanupAt) {
            return;
        }

        nextCleanupAt = gameTime + CLEANUP_INTERVAL_TICKS;
        SIGHT.values().removeIf(entries -> {
            entries.removeIf(entry -> gameTime >= entry.expiresAt || !entry.target.isAlive() || entry.target.isRemoved());
            return entries.isEmpty();
        });
    }

    private record SightEntry(
            Entity target,
            boolean visible,
            long expiresAt,
            double observerX,
            double observerY,
            double observerZ,
            double targetX,
            double targetY,
            double targetZ
    ) {
    }
}
