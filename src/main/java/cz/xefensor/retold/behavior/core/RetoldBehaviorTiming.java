package cz.xefensor.retold.behavior.core;

import cz.xefensor.retold.behavior.performance.RetoldAiLod;
import cz.xefensor.retold.behavior.performance.RetoldAiLodLevel;
import cz.xefensor.retold.behavior.performance.RetoldBehaviorPerf;

import net.minecraft.world.entity.Entity;

import java.util.Map;
import java.util.WeakHashMap;

public final class RetoldBehaviorTiming {
    private static final Map<Entity, TimingCache> TIMING = new WeakHashMap<>();

    private RetoldBehaviorTiming() {
    }

    public static boolean shouldThink(
            Entity entity,
            long gameTime,
            int intervalTicks
    ) {
        if (entity == null || intervalTicks <= 0) {
            RetoldBehaviorPerf.recordTiming(false);
            return false;
        }

        RetoldAiLodLevel lod = RetoldAiLod.levelFor(entity);
        RetoldBehaviorPerf.recordLod(lod);
        int adjustedIntervalTicks = RetoldAiLod.timingInterval(entity, intervalTicks, lod);

        TimingCache timing = TIMING.computeIfAbsent(
                entity,
                ignored -> new TimingCache()
        );

        if (timing.gameTime != gameTime) {
            timing.reset(gameTime);
        }

        Boolean cached = timing.get(adjustedIntervalTicks);
        if (cached != null) {
            RetoldBehaviorPerf.recordTimingCacheHit();
            RetoldBehaviorPerf.recordTiming(cached);
            return cached;
        }

        int offset = Math.floorMod(
                entity.getId(),
                adjustedIntervalTicks
        );

        boolean passed = (gameTime + offset) % adjustedIntervalTicks == 0L;
        timing.put(
                adjustedIntervalTicks,
                passed
        );
        RetoldBehaviorPerf.recordTiming(passed);
        return passed;
    }

    private static final class TimingCache {
        private static final int MAX_ENTRIES = 8;

        private final int[] intervals = new int[MAX_ENTRIES];
        private final boolean[] passes = new boolean[MAX_ENTRIES];
        private long gameTime = Long.MIN_VALUE;
        private int size;

        private void reset(long refreshedGameTime) {
            gameTime = refreshedGameTime;
            size = 0;
        }

        private Boolean get(int intervalTicks) {
            for (int i = 0; i < size; i++) {
                if (intervals[i] == intervalTicks) {
                    return passes[i];
                }
            }

            return null;
        }

        private void put(
                int intervalTicks,
                boolean passed
        ) {
            if (size < MAX_ENTRIES) {
                intervals[size] = intervalTicks;
                passes[size] = passed;
                size++;
                return;
            }

            int slot = Math.floorMod(
                    intervalTicks,
                    MAX_ENTRIES
            );
            intervals[slot] = intervalTicks;
            passes[slot] = passed;
        }
    }
}
