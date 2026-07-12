package cz.xefensor.retold.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;

import java.util.Map;
import java.util.WeakHashMap;

public final class RetoldBehaviorMovement {
    private static final int MAX_PATH_STARTS_PER_TICK = 16;

    private static final Map<PathfinderMob, PathMemory> PATH_MEMORIES = new WeakHashMap<>();
    private static long pathBudgetTick = Long.MIN_VALUE;
    private static int pathStartsThisTick;

    private RetoldBehaviorMovement() {
    }

    public static void throttledMoveTo(
            PathfinderMob mob,
            LivingEntity target,
            double speed,
            long gameTime,
            int minIntervalTicks,
            double repathDistanceSquared
    ) {
        if (target == null) {
            return;
        }

        throttledMoveTo(
                mob,
                target.getX(),
                target.getY(),
                target.getZ(),
                speed,
                gameTime,
                minIntervalTicks,
                repathDistanceSquared
        );
    }

    public static void throttledMoveTo(
            PathfinderMob mob,
            Entity target,
            double speed,
            long gameTime,
            int minIntervalTicks,
            double repathDistanceSquared
    ) {
        if (target == null) {
            return;
        }

        throttledMoveTo(
                mob,
                target.getX(),
                target.getY(),
                target.getZ(),
                speed,
                gameTime,
                minIntervalTicks,
                repathDistanceSquared
        );
    }

    public static void throttledMoveTo(
            PathfinderMob mob,
            BlockPos target,
            double speed,
            long gameTime,
            int minIntervalTicks,
            double repathDistanceSquared
    ) {
        if (target == null) {
            return;
        }

        throttledMoveTo(
                mob,
                target.getX() + 0.5D,
                target.getY(),
                target.getZ() + 0.5D,
                speed,
                gameTime,
                minIntervalTicks,
                repathDistanceSquared
        );
    }

    public static void throttledMoveTo(
            PathfinderMob mob,
            double x,
            double y,
            double z,
            double speed,
            long gameTime,
            int minIntervalTicks,
            double repathDistanceSquared
    ) {
        if (mob == null) {
            return;
        }

        PathMemory memory = PATH_MEMORIES.get(mob);

        if (
                memory != null
                        && gameTime < memory.nextPathAt
                        && distanceSquared(x, y, z, memory.x, memory.y, memory.z) <= repathDistanceSquared
                        && Math.abs(speed - memory.speed) < 0.001D
        ) {
            RetoldBehaviorPerf.recordPathRequest(true);
            return;
        }

        if (!RetoldAiLod.canStartPath(mob, gameTime)) {
            RetoldBehaviorPerf.recordPathRequest(true);
            return;
        }

        if (!tryUsePathBudget(gameTime)) {
            RetoldBehaviorPerf.recordPathRequest(true);
            return;
        }

        RetoldBehaviorPerf.recordPathRequest(false);

        RetoldAiControl.withNavigationBypass(() -> {
            mob.getNavigation().moveTo(
                    x,
                    y,
                    z,
                    speed
            );
        });

        PATH_MEMORIES.put(
                mob,
                new PathMemory(
                        x,
                        y,
                        z,
                        speed,
                        gameTime + Math.max(1, minIntervalTicks)
                )
        );
    }

    public static boolean claimAndMoveToBlock(
            PathfinderMob mob,
            BlockPos target,
            RetoldAiControlMode mode,
            RetoldAiControlOwner owner,
            int priority,
            String reason,
            long gameTime,
            int controlTicks,
            double speed,
            boolean sprinting
    ) {
        if (mob == null || target == null) {
            return false;
        }

        if (!RetoldAiControl.tryClaim(
                mob,
                mode,
                owner,
                priority,
                reason,
                gameTime,
                controlTicks
        )) {
            return false;
        }

        mob.setSprinting(sprinting);
        throttledMoveTo(
                mob,
                target,
                speed,
                gameTime,
                8,
                1.0D
        );

        return true;
    }

    public static void stopOwnedMovement(
            PathfinderMob mob,
            RetoldAiControlOwner owner
    ) {
        if (mob == null) {
            return;
        }

        mob.setSprinting(false);
        mob.getNavigation().stop();

        RetoldAiControl.clearIfOwnedBy(
                mob,
                owner
        );
    }

    private static boolean tryUsePathBudget(long gameTime) {
        if (pathBudgetTick != gameTime) {
            pathBudgetTick = gameTime;
            pathStartsThisTick = 0;
        }

        if (pathStartsThisTick >= MAX_PATH_STARTS_PER_TICK) {
            return false;
        }

        pathStartsThisTick++;
        return true;
    }

    private static double distanceSquared(
            double firstX,
            double firstY,
            double firstZ,
            double secondX,
            double secondY,
            double secondZ
    ) {
        double dx = firstX - secondX;
        double dy = firstY - secondY;
        double dz = firstZ - secondZ;

        return dx * dx + dy * dy + dz * dz;
    }

    private record PathMemory(
            double x,
            double y,
            double z,
            double speed,
            long nextPathAt
    ) {
    }
}
