package cz.xefensor.retold.behavior.core;

import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.control.RetoldAiControlMode;
import cz.xefensor.retold.behavior.control.RetoldAiControlOwner;
import cz.xefensor.retold.behavior.performance.RetoldAiLod;
import cz.xefensor.retold.behavior.performance.RetoldBehaviorPerf;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;

public final class RetoldBehaviorMovement {
    private static final int MAX_PATH_STARTS_PER_TICK = 16;
    private static final int MAX_FLYING_PATH_LENGTH = 64;
    private static final double FLYING_WAYPOINT_REACHED_SQUARED = 0.85D * 0.85D;

    private static final Map<PathfinderMob, PathMemory> PATH_MEMORIES = new WeakHashMap<>();
    private static final Map<Mob, FlyingPathMemory> FLYING_PATH_MEMORIES = new WeakHashMap<>();
    private static long pathBudgetTick = Long.MIN_VALUE;
    private static int pathStartsThisTick;

    private RetoldBehaviorMovement() {
    }

    public static boolean throttledMoveTo(
            PathfinderMob mob,
            LivingEntity target,
            double speed,
            long gameTime,
            int minIntervalTicks,
            double repathDistanceSquared
    ) {
        if (target == null) {
            return false;
        }

        return throttledMoveTo(
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

    public static boolean throttledMoveTo(
            PathfinderMob mob,
            Entity target,
            double speed,
            long gameTime,
            int minIntervalTicks,
            double repathDistanceSquared
    ) {
        if (target == null) {
            return false;
        }

        return throttledMoveTo(
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

    public static boolean throttledMoveTo(
            PathfinderMob mob,
            BlockPos target,
            double speed,
            long gameTime,
            int minIntervalTicks,
            double repathDistanceSquared
    ) {
        if (target == null) {
            return false;
        }

        return throttledMoveTo(
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

    public static boolean throttledMoveToExact(
            PathfinderMob mob,
            BlockPos target,
            double speed,
            long gameTime,
            int minIntervalTicks,
            double repathDistanceSquared
    ) {
        if (target == null) {
            return false;
        }

        return throttledMoveTo(
                mob,
                target.getX() + 0.5D,
                target.getY(),
                target.getZ() + 0.5D,
                speed,
                gameTime,
                minIntervalTicks,
                repathDistanceSquared,
                0
        );
    }

    public static boolean throttledMoveTo(
            PathfinderMob mob,
            double x,
            double y,
            double z,
            double speed,
            long gameTime,
            int minIntervalTicks,
            double repathDistanceSquared
    ) {
        return throttledMoveTo(
                mob,
                x,
                y,
                z,
                speed,
                gameTime,
                minIntervalTicks,
                repathDistanceSquared,
                1
        );
    }

    private static boolean throttledMoveTo(
            PathfinderMob mob,
            double x,
            double y,
            double z,
            double speed,
            long gameTime,
            int minIntervalTicks,
            double repathDistanceSquared,
            int reachRange
    ) {
        if (mob == null) {
            return false;
        }

        PathMemory memory = PATH_MEMORIES.get(mob);

        if (
                memory != null
                        && gameTime < memory.nextPathAt
                        && !mob.getNavigation().isDone()
                        && distanceSquared(x, y, z, memory.x, memory.y, memory.z) <= repathDistanceSquared
                        && Math.abs(speed - memory.speed) < 0.001D
                        && memory.reachRange == reachRange
        ) {
            RetoldBehaviorPerf.recordPathRequest(true);
            return true;
        }

        if (mob instanceof AbstractCubeMob cubeMob) {
            if (RetoldCubeMobMovement.moveToward(cubeMob, x, z, speed)) {
                PATH_MEMORIES.put(
                        mob,
                        new PathMemory(
                                x,
                                y,
                                z,
                                speed,
                                reachRange,
                                gameTime + Math.max(1, minIntervalTicks)
                        )
                );
                return true;
            }
            return false;
        }

        if (!RetoldAiLod.canStartPath(mob, gameTime)) {
            RetoldBehaviorPerf.recordPathRequest(true);
            return false;
        }

        if (!tryUsePathBudget(gameTime)) {
            RetoldBehaviorPerf.recordPathRequest(true);
            return false;
        }

        RetoldBehaviorPerf.recordPathRequest(false);

        boolean[] started = {false};

        RetoldAiControl.withNavigationBypass(() -> {
            started[0] = mob.getNavigation().moveTo(
                    x,
                    y,
                    z,
                    reachRange,
                    speed
            );
        });

        if (started[0]) {
            PATH_MEMORIES.put(
                    mob,
                    new PathMemory(
                            x,
                            y,
                            z,
                            speed,
                            reachRange,
                            gameTime + Math.max(1, minIntervalTicks)
                    )
            );
        } else {
            PATH_MEMORIES.remove(mob);
        }

        return started[0];
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

    public static boolean requestFlyingPath(
            Mob mob,
            Vec3 destination,
            long gameTime,
            int minIntervalTicks,
            double repathDistanceSquared
    ) {
        if (mob == null || destination == null) {
            return false;
        }

        FlyingPathMemory memory = FLYING_PATH_MEMORIES.get(mob);
        boolean canReuse = memory != null
                && memory.level == mob.level()
                && !memory.path.isDone();
        boolean destinationMatches = canReuse
                && distanceSquared(
                destination.x(),
                destination.y(),
                destination.z(),
                memory.x,
                memory.y,
                memory.z
        ) <= repathDistanceSquared;

        if (canReuse
                && gameTime < memory.nextPathAt
                && destinationMatches) {
            RetoldBehaviorPerf.recordPathRequest(true);
            return true;
        }

        if (!RetoldAiLod.canStartPath(mob, gameTime)
                || !tryUsePathBudget(gameTime)) {
            RetoldBehaviorPerf.recordPathRequest(true);
            return destinationMatches;
        }

        RetoldBehaviorPerf.recordPathRequest(false);

        FlyingPathNavigation pathfinder = memory != null
                && memory.level == mob.level()
                ? memory.pathfinder
                : new FlyingPathNavigation(mob, mob.level());
        pathfinder.setRequiredPathLength(MAX_FLYING_PATH_LENGTH);
        Path path = pathfinder.createPath(
                BlockPos.containing(destination),
                0,
                MAX_FLYING_PATH_LENGTH
        );

        if (path == null || path.getNodeCount() <= 0 || !path.canReach()) {
            if (!destinationMatches) {
                FLYING_PATH_MEMORIES.remove(mob);
            }
            return destinationMatches;
        }

        FLYING_PATH_MEMORIES.put(
                mob,
                new FlyingPathMemory(
                        mob.level(),
                        pathfinder,
                        path,
                        destination.x(),
                        destination.y(),
                        destination.z(),
                        gameTime + Math.max(1, minIntervalTicks)
                )
        );
        return true;
    }

    public static Vec3 nextFlyingWaypoint(Mob mob) {
        if (mob == null) {
            return null;
        }

        FlyingPathMemory memory = FLYING_PATH_MEMORIES.get(mob);

        if (memory == null || memory.level != mob.level()) {
            FLYING_PATH_MEMORIES.remove(mob);
            return null;
        }

        while (!memory.path.isDone()
                && mob.position().distanceToSqr(
                memory.path.getNextEntityPos(mob)
        ) <= FLYING_WAYPOINT_REACHED_SQUARED) {
            memory.path.advance();
        }

        if (memory.path.isDone()) {
            return null;
        }

        return memory.path.getNextEntityPos(mob);
    }

    public static boolean hasFlyingPath(Mob mob) {
        FlyingPathMemory memory = mob == null
                ? null
                : FLYING_PATH_MEMORIES.get(mob);

        return memory != null
                && memory.level == mob.level()
                && !memory.path.isDone();
    }

    public static void clearFlyingPath(Mob mob) {
        if (mob != null) {
            FLYING_PATH_MEMORIES.remove(mob);
        }
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
            int reachRange,
            long nextPathAt
    ) {
    }

    private record FlyingPathMemory(
            Level level,
            FlyingPathNavigation pathfinder,
            Path path,
            double x,
            double y,
            double z,
            long nextPathAt
    ) {
    }
}
