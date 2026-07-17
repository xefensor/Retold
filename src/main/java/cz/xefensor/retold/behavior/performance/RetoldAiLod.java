package cz.xefensor.retold.behavior.performance;

import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.profiles.RetoldMobState;
import cz.xefensor.retold.behavior.profiles.RetoldMobStates;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;

import java.util.Map;
import java.util.WeakHashMap;

public final class RetoldAiLod {
    private static final double FULL_RADIUS_SQUARED = 48.0D * 48.0D;
    private static final double NEAR_RADIUS_SQUARED = 96.0D * 96.0D;
    private static final double FAR_RADIUS_SQUARED = 160.0D * 160.0D;
    private static final long RECENT_DANGER_FULL_TICKS = 20L * 20L;
    private static final int LOD_CACHE_TICKS = 10;

    private static final Map<Mob, LodMemory> LOD_MEMORY = new WeakHashMap<>();

    private RetoldAiLod() {
    }

    public static RetoldAiLodLevel levelFor(Entity entity) {
        if (!(entity instanceof Mob mob)) {
            return RetoldAiLodLevel.FULL;
        }

        if (!(mob.level() instanceof ServerLevel level)) {
            return RetoldAiLodLevel.FULL;
        }

        if (isImportantNow(mob, level.getGameTime())) {
            return RetoldAiLodLevel.FULL;
        }

        long gameTime = level.getGameTime();
        LodMemory memory = LOD_MEMORY.get(mob);

        if (memory != null && gameTime < memory.expiresAt) {
            return memory.level;
        }

        double nearestPlayerDistance = nearestPlayerDistanceSquared(level, mob);
        RetoldAiLodLevel lod;

        if (nearestPlayerDistance <= FULL_RADIUS_SQUARED) {
            lod = RetoldAiLodLevel.FULL;
        } else if (nearestPlayerDistance <= NEAR_RADIUS_SQUARED) {
            lod = RetoldAiLodLevel.NEAR;
        } else if (nearestPlayerDistance <= FAR_RADIUS_SQUARED) {
            lod = RetoldAiLodLevel.FAR;
        } else {
            lod = RetoldAiLodLevel.BACKGROUND;
        }

        LOD_MEMORY.put(
                mob,
                new LodMemory(
                        lod,
                        gameTime + LOD_CACHE_TICKS
                )
        );

        return lod;
    }

    public static int timingInterval(
            Entity entity,
            int baseIntervalTicks
    ) {
        return timingInterval(
                entity,
                baseIntervalTicks,
                levelFor(entity)
        );
    }

    public static int timingInterval(
            Entity entity,
            int baseIntervalTicks,
            RetoldAiLodLevel lod
    ) {
        int safeBase = Math.max(1, baseIntervalTicks);

        return switch (lod == null ? RetoldAiLodLevel.FULL : lod) {
            case FULL -> safeBase;
            case NEAR -> Math.max(safeBase, safeBase * 2);
            case FAR -> Math.max(safeBase, safeBase * 4);
            case BACKGROUND -> Math.max(safeBase, safeBase * 8);
        };
    }

    public static int cacheTicks(
            Entity entity,
            int baseCacheTicks
    ) {
        int safeBase = Math.max(1, baseCacheTicks);

        return switch (levelFor(entity)) {
            case FULL -> safeBase;
            case NEAR -> Math.max(safeBase, safeBase * 2);
            case FAR -> Math.max(safeBase, safeBase * 4);
            case BACKGROUND -> Math.max(safeBase, safeBase * 8);
        };
    }

    public static boolean canStartPath(
            PathfinderMob mob,
            long gameTime
    ) {
        RetoldAiLodLevel lod = levelFor(mob);

        return switch (lod) {
            case FULL -> true;
            case NEAR -> shouldRunOnCadence(mob, gameTime, 2);
            case FAR -> shouldRunOnCadence(mob, gameTime, 5);
            case BACKGROUND -> shouldRunOnCadence(mob, gameTime, 12);
        };
    }

    private static boolean isImportantNow(
            Mob mob,
            long gameTime
    ) {
        if (mob.getTarget() != null || mob.getLastHurtByMob() != null) {
            return true;
        }

        if (RetoldAiControl.isControlled(mob)) {
            return true;
        }

        if (mob instanceof PathfinderMob pathfinderMob) {
            RetoldMobState state = RetoldMobStates.get(pathfinderMob);

            return state != null
                    && gameTime - state.lastDangerAt() <= RECENT_DANGER_FULL_TICKS;
        }

        return false;
    }

    private static boolean shouldRunOnCadence(
            Entity entity,
            long gameTime,
            int cadenceTicks
    ) {
        int cadence = Math.max(1, cadenceTicks);

        return Math.floorMod(
                gameTime + entity.getId(),
                cadence
        ) == 0;
    }

    private static double nearestPlayerDistanceSquared(
            ServerLevel level,
            Entity entity
    ) {
        double best = Double.MAX_VALUE;

        for (ServerPlayer player : level.players()) {
            if (player.isSpectator()) {
                continue;
            }

            double distance = entity.distanceToSqr(player);

            if (distance < best) {
                best = distance;
            }
        }

        return best;
    }

    private record LodMemory(
            RetoldAiLodLevel level,
            long expiresAt
    ) {
    }
}
