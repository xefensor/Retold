package cz.xefensor.retold.behavior.core;

import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.control.RetoldAiControlMode;
import cz.xefensor.retold.behavior.control.RetoldAiControlOwner;
import cz.xefensor.retold.behavior.performance.RetoldAiLod;
import cz.xefensor.retold.behavior.performance.RetoldBehaviorPerf;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;

public final class RetoldBehaviorMovement {
    private static final int MAX_PATH_STARTS_PER_TICK = 16;
    private static final int MAX_RECONCILIATION_PATH_PROBES_PER_TICK = 8;
    private static final int MAX_MIGRATION_PATH_PROBES_PER_TICK = 10;
    private static final int MAX_MIGRATION_PATH_LENGTH = 64;
    private static final int MAX_FLYING_PATH_LENGTH = 64;
    private static final int GROUND_PATH_FAILURES_BEFORE_UNREACHABLE = 3;
    private static final int GROUND_PATH_STUCK_TICKS = 40;
    private static final int GROUND_PATH_FAILURE_BACKOFF_TICKS = 10;
    private static final int MAX_GROUND_PATH_FAILURE_BACKOFF_TICKS = 80;
    private static final double GROUND_PATH_PROGRESS_DISTANCE_SQUARED = 0.5D * 0.5D;
    private static final double UNCHANGED_DESTINATION_DISTANCE_SQUARED = 0.01D * 0.01D;
    private static final double FLYING_WAYPOINT_REACHED_SQUARED = 0.85D * 0.85D;

    private static final Map<PathfinderMob, PathMemory> PATH_MEMORIES = new WeakHashMap<>();
    private static final Map<PathfinderMob, GroundPathRecoveryMemory> GROUND_PATH_RECOVERY_MEMORIES =
            new WeakHashMap<>();
    private static final Map<Mob, FlyingPathMemory> FLYING_PATH_MEMORIES = new WeakHashMap<>();
    private static long pathBudgetTick = Long.MIN_VALUE;
    private static int pathStartsThisTick;
    private static long reconciliationPathBudgetTick = Long.MIN_VALUE;
    private static int reconciliationPathProbesThisTick;
    private static long migrationPathBudgetTick = Long.MIN_VALUE;
    private static int migrationPathProbesThisTick;

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

        return throttledMoveToWithOutcome(
                mob,
                target.getX(),
                target.getY(),
                target.getZ(),
                speed,
                gameTime,
                minIntervalTicks,
                repathDistanceSquared
        ).isMoving();
    }

    /**
     * Performs a bounded path probe without starting movement. This is for
     * episodic reconciliation that must prove a target was accessible before
     * mutating real world state.
     */
    public static ReachabilityResult probeReachability(
            PathfinderMob mob,
            Entity target,
            long gameTime
    ) {
        if (mob == null
                || target == null
                || mob.level() != target.level()
                || !mob.isAlive()
                || !target.isAlive()) {
            return ReachabilityResult.UNREACHABLE;
        }

        if (!tryUseReconciliationPathBudget(gameTime)) {
            RetoldBehaviorPerf.recordPathRequest(true);
            return ReachabilityResult.DEFERRED;
        }

        RetoldBehaviorPerf.recordPathRequest(false);
        Path path = mob.getNavigation().createPath(target, 1);
        return path != null && path.canReach()
                ? ReachabilityResult.REACHABLE
                : ReachabilityResult.UNREACHABLE;
    }

    /** Proves a block target is reachable without starting movement. */
    public static ReachabilityResult probeReachability(
            PathfinderMob mob,
            BlockPos target,
            long gameTime
    ) {
        if (mob == null
                || target == null
                || !mob.isAlive()
                || mob.isRemoved()) {
            return ReachabilityResult.UNREACHABLE;
        }

        if (!tryUseReconciliationPathBudget(gameTime)) {
            RetoldBehaviorPerf.recordPathRequest(true);
            return ReachabilityResult.DEFERRED;
        }

        RetoldBehaviorPerf.recordPathRequest(false);
        Path path = mob.getNavigation().createPath(target, 1);
        return path != null
                && (path.canReach()
                || path.getEndNode() != null
                && path.getEndNode().distanceTo(target) <= 1.5F)
                ? ReachabilityResult.REACHABLE
                : ReachabilityResult.UNREACHABLE;
    }

    /**
     * Proves that an unloaded-migration landing is reachable without starting
     * movement. This budget is separate from predation reconciliation so one
     * returning maximum-size herd cannot starve unrelated kill validation.
     */
    public static ReachabilityResult probeMigrationReachability(
            PathfinderMob mob,
            BlockPos target,
            long gameTime
    ) {
        if (mob == null
                || target == null
                || !mob.isAlive()
                || mob.isRemoved()) {
            return ReachabilityResult.UNREACHABLE;
        }

        if (!tryUseMigrationPathBudget(gameTime)) {
            RetoldBehaviorPerf.recordPathRequest(true);
            return ReachabilityResult.DEFERRED;
        }

        RetoldBehaviorPerf.recordPathRequest(false);
        Path path = mob.getNavigation().createPath(
                target,
                1,
                MAX_MIGRATION_PATH_LENGTH
        );
        boolean reachesLanding = path != null
                && (path.canReach()
                || path.getEndNode() != null
                && path.getEndNode().distanceTo(target) <= 1.5F);
        return reachesLanding
                ? ReachabilityResult.REACHABLE
                : ReachabilityResult.UNREACHABLE;
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

        return throttledMoveToWithOutcome(
                mob,
                target.getX(),
                target.getY(),
                target.getZ(),
                speed,
                gameTime,
                minIntervalTicks,
                repathDistanceSquared
        ).isMoving();
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

        return throttledMoveToWithOutcome(
                mob,
                target.getX() + 0.5D,
                target.getY(),
                target.getZ() + 0.5D,
                speed,
                gameTime,
                minIntervalTicks,
                repathDistanceSquared
        ).isMoving();
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

        return throttledMoveToWithOutcome(
                mob,
                target.getX() + 0.5D,
                target.getY(),
                target.getZ() + 0.5D,
                speed,
                gameTime,
                minIntervalTicks,
                repathDistanceSquared,
                0,
                false
        ).isMoving();
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
        return throttledMoveToWithOutcome(
                mob,
                x,
                y,
                z,
                speed,
                gameTime,
                minIntervalTicks,
                repathDistanceSquared,
                1,
                false
        ).isMoving();
    }

    /**
     * Starts ordinary ground navigation only when the resulting route stays out of water.
     * Land mobs use this for urgent Retold flight so a direct danger vector cannot make them
     * choose drowning as the apparently shortest escape.
     */
    public static boolean throttledMoveToAvoidingWater(
            PathfinderMob mob,
            BlockPos target,
            double speed,
            long gameTime,
            int minIntervalTicks,
            double repathDistanceSquared
    ) {
        return throttledMoveToAvoidingWaterWithOutcome(
                mob,
                target,
                speed,
                gameTime,
                minIntervalTicks,
                repathDistanceSquared
        ).isMoving();
    }

    /**
     * Starts water-avoiding ground navigation while preserving a real route failure separately
     * from an LOD or work-budget deferral. Urgent flight uses this to try a bounded side detour only
     * when the straight escape corridor cannot produce a usable path.
     */
    public static MovementOutcome throttledMoveToAvoidingWaterWithOutcome(
            PathfinderMob mob,
            BlockPos target,
            double speed,
            long gameTime,
            int minIntervalTicks,
            double repathDistanceSquared
    ) {
        if (target == null) {
            return MovementOutcome.UNSUPPORTED;
        }

        return throttledMoveToWithOutcome(
                mob,
                target.getX() + 0.5D,
                target.getY(),
                target.getZ() + 0.5D,
                speed,
                gameTime,
                minIntervalTicks,
                repathDistanceSquared,
                1,
                true
        );
    }

    /**
     * Requests ordinary movement while exposing why no new route was started.
     * Budget and LOD deferrals are deliberately separate from genuine ground-path
     * failures so behavior owners never abandon a target merely because another
     * mob consumed this tick's bounded navigation work.
     */
    public static MovementOutcome throttledMoveToWithOutcome(
            PathfinderMob mob,
            LivingEntity target,
            double speed,
            long gameTime,
            int minIntervalTicks,
            double repathDistanceSquared
    ) {
        if (target == null) {
            return MovementOutcome.UNSUPPORTED;
        }

        return throttledMoveToWithOutcome(
                mob,
                target.getX(),
                target.getY(),
                target.getZ(),
                speed,
                gameTime,
                minIntervalTicks,
                repathDistanceSquared,
                1,
                false
        );
    }

    public static MovementOutcome throttledMoveToWithOutcome(
            PathfinderMob mob,
            double x,
            double y,
            double z,
            double speed,
            long gameTime,
            int minIntervalTicks,
            double repathDistanceSquared
    ) {
        return throttledMoveToWithOutcome(
                mob,
                x,
                y,
                z,
                speed,
                gameTime,
                minIntervalTicks,
                repathDistanceSquared,
                1,
                false
        );
    }

    private static MovementOutcome throttledMoveToWithOutcome(
            PathfinderMob mob,
            double x,
            double y,
            double z,
            double speed,
            long gameTime,
            int minIntervalTicks,
            double repathDistanceSquared,
            int reachRange,
            boolean avoidWater
    ) {
        if (mob == null) {
            return MovementOutcome.UNSUPPORTED;
        }

        PathMemory memory = PATH_MEMORIES.get(mob);
        GroundPathRecoveryMemory recovery = groundRecoveryMemory(
                mob,
                x,
                y,
                z,
                repathDistanceSquared,
                gameTime
        );

        if (recovery != null) {
            MovementOutcome progressOutcome = observeGroundPathProgress(
                    mob,
                    recovery,
                    x,
                    y,
                    z,
                    reachRange,
                    gameTime
            );

            if (progressOutcome != null) {
                return progressOutcome;
            }
        }

        boolean matchesRememberedDestination = memory != null
                && distanceSquared(x, y, z, memory.x, memory.y, memory.z)
                <= UNCHANGED_DESTINATION_DISTANCE_SQUARED
                && memory.reachRange == reachRange
                && memory.avoidWater == avoidWater;
        boolean matchesActiveNavigationTarget = matchesNavigationTarget(
                mob,
                x,
                y,
                z
        );

        if (!mob.getNavigation().isDone()
                && (matchesRememberedDestination || matchesActiveNavigationTarget)) {
            mob.getNavigation().setSpeedModifier(speed);
            PATH_MEMORIES.put(
                    mob,
                    new PathMemory(
                            x,
                            y,
                            z,
                            speed,
                            reachRange,
                            avoidWater,
                            gameTime + Math.max(1, minIntervalTicks)
                    )
            );
            RetoldBehaviorPerf.recordPathRequest(true);
            return MovementOutcome.MOVING;
        }

        if (
                memory != null
                        && gameTime < memory.nextPathAt
                        && !mob.getNavigation().isDone()
                        && distanceSquared(x, y, z, memory.x, memory.y, memory.z) <= repathDistanceSquared
                        && Math.abs(speed - memory.speed) < 0.001D
                        && memory.reachRange == reachRange
                        && memory.avoidWater == avoidWater
        ) {
            RetoldBehaviorPerf.recordPathRequest(true);
            return MovementOutcome.MOVING;
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
                                avoidWater,
                                gameTime + Math.max(1, minIntervalTicks)
                        )
                );
                return MovementOutcome.MOVING;
            }
            return MovementOutcome.UNSUPPORTED;
        }

        if (recovery != null && gameTime < recovery.nextRetryAt) {
            RetoldBehaviorPerf.recordPathRequest(true);
            RetoldBehaviorPerf.recordGroundPathBackoff();
            return recovery.isUnreachable()
                    ? MovementOutcome.UNREACHABLE
                    : MovementOutcome.RETRYING;
        }

        if (!RetoldAiLod.canStartPath(mob, gameTime)) {
            RetoldBehaviorPerf.recordPathRequest(true);
            return MovementOutcome.DEFERRED;
        }

        if (!tryUsePathBudget(gameTime)) {
            RetoldBehaviorPerf.recordPathRequest(true);
            return MovementOutcome.DEFERRED;
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

        if (started[0]
                && avoidWater
                && pathUsesWater(mob, mob.getNavigation().getPath())) {
            RetoldAiControl.withNavigationBypass(mob.getNavigation()::stop);
            started[0] = false;
        }

        if (started[0]) {
            PATH_MEMORIES.put(
                    mob,
                    new PathMemory(
                            x,
                            y,
                            z,
                            speed,
                            reachRange,
                            avoidWater,
                            gameTime + Math.max(1, minIntervalTicks)
                    )
            );
            if (recovery != null) {
                recovery.markAttempted(gameTime, mob.position());
            }
        } else {
            PATH_MEMORIES.remove(mob);

            if (recovery != null) {
                return recordGroundPathFailure(recovery, gameTime, false);
            }
        }

        return started[0]
                ? MovementOutcome.MOVING
                : MovementOutcome.UNSUPPORTED;
    }

    private static GroundPathRecoveryMemory groundRecoveryMemory(
            PathfinderMob mob,
            double x,
            double y,
            double z,
            double repathDistanceSquared,
            long gameTime
    ) {
        if (!(mob.getNavigation() instanceof GroundPathNavigation)) {
            return null;
        }

        GroundPathRecoveryMemory recovery = GROUND_PATH_RECOVERY_MEMORIES.get(mob);

        if (recovery == null
                || distanceSquared(x, y, z, recovery.x, recovery.y, recovery.z)
                > repathDistanceSquared) {
            recovery = new GroundPathRecoveryMemory(x, y, z, gameTime, mob.position());
            GROUND_PATH_RECOVERY_MEMORIES.put(mob, recovery);
        } else {
            recovery.updateDestination(x, y, z);
        }

        return recovery;
    }

    private static boolean matchesNavigationTarget(
            PathfinderMob mob,
            double x,
            double y,
            double z
    ) {
        BlockPos navigationTarget = mob.getNavigation().getTargetPos();
        Path activePath = mob.getNavigation().getPath();
        BlockPos requestedTarget = BlockPos.containing(x, y, z);

        return requestedTarget.equals(navigationTarget)
                || (activePath != null && requestedTarget.equals(activePath.getTarget()));
    }

    private static MovementOutcome observeGroundPathProgress(
            PathfinderMob mob,
            GroundPathRecoveryMemory recovery,
            double x,
            double y,
            double z,
            int reachRange,
            long gameTime
    ) {
        double acceptableDistance = reachRange + 1.5D;

        if (distanceSquared(mob.getX(), mob.getY(), mob.getZ(), x, y, z)
                <= acceptableDistance * acceptableDistance) {
            recovery.markReached(gameTime, mob.position());
            return null;
        }

        if (mob.position().distanceToSqr(recovery.lastProgressPosition)
                >= GROUND_PATH_PROGRESS_DISTANCE_SQUARED) {
            recovery.markProgress(gameTime, mob.position());
            return null;
        }

        if (recovery.attempted
                && gameTime - recovery.lastProgressAt >= GROUND_PATH_STUCK_TICKS) {
            RetoldAiControl.withNavigationBypass(mob.getNavigation()::stop);
            PATH_MEMORIES.remove(mob);
            return recordGroundPathFailure(recovery, gameTime, true);
        }

        return null;
    }

    private static MovementOutcome recordGroundPathFailure(
            GroundPathRecoveryMemory recovery,
            long gameTime,
            boolean stuck
    ) {
        if (stuck) {
            recovery.failures = GROUND_PATH_FAILURES_BEFORE_UNREACHABLE;
            RetoldBehaviorPerf.recordGroundPathStuck();
        } else {
            recovery.failures++;
            RetoldBehaviorPerf.recordGroundPathFailure();
        }

        int backoffMultiplier = 1 << Math.min(3, Math.max(0, recovery.failures - 1));
        recovery.nextRetryAt = gameTime + Math.min(
                MAX_GROUND_PATH_FAILURE_BACKOFF_TICKS,
                GROUND_PATH_FAILURE_BACKOFF_TICKS * backoffMultiplier
        );
        /*
         * A rejected route has no active path whose progress can be observed.
         * Once its bounded backoff expires it must be allowed to try again in
         * case doors, blocks, or the destination changed in the meantime.
         */
        recovery.attempted = false;

        return recovery.isUnreachable()
                ? MovementOutcome.UNREACHABLE
                : MovementOutcome.RETRYING;
    }

    private static boolean pathUsesWater(PathfinderMob mob, Path path) {
        if (mob == null || path == null) {
            return false;
        }

        for (int index = path.getNextNodeIndex(); index < path.getNodeCount(); index++) {
            BlockPos nodePos = path.getNodePos(index);

            if (mob.level().getFluidState(nodePos).is(FluidTags.WATER)
                    || mob.level().getFluidState(nodePos.below()).is(FluidTags.WATER)) {
                return true;
            }
        }

        return false;
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

    /**
     * Claims block-directed movement while preserving the path outcome for owners that can choose
     * another world target after a proven route failure.
     */
    public static MovementOutcome claimAndMoveToBlockWithOutcome(
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
            return MovementOutcome.UNSUPPORTED;
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
            return MovementOutcome.UNSUPPORTED;
        }

        mob.setSprinting(sprinting);
        return throttledMoveToWithOutcome(
                mob,
                target.getX() + 0.5D,
                target.getY(),
                target.getZ() + 0.5D,
                speed,
                gameTime,
                8,
                1.0D
        );
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
        clearGroundPathState(mob);

        RetoldAiControl.clearIfOwnedBy(
                mob,
                owner
        );
    }

    /** Clears route reuse and failure history when a behavior intentionally ends movement. */
    public static void clearGroundPathState(PathfinderMob mob) {
        if (mob == null) {
            return;
        }

        PATH_MEMORIES.remove(mob);
        GROUND_PATH_RECOVERY_MEMORIES.remove(mob);
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

    private static boolean tryUseReconciliationPathBudget(long gameTime) {
        if (reconciliationPathBudgetTick != gameTime) {
            reconciliationPathBudgetTick = gameTime;
            reconciliationPathProbesThisTick = 0;
        }

        if (reconciliationPathProbesThisTick
                >= MAX_RECONCILIATION_PATH_PROBES_PER_TICK) {
            return false;
        }

        reconciliationPathProbesThisTick++;
        return true;
    }

    private static boolean tryUseMigrationPathBudget(long gameTime) {
        if (migrationPathBudgetTick != gameTime) {
            migrationPathBudgetTick = gameTime;
            migrationPathProbesThisTick = 0;
        }

        if (migrationPathProbesThisTick
                >= MAX_MIGRATION_PATH_PROBES_PER_TICK) {
            return false;
        }

        migrationPathProbesThisTick++;
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
            boolean avoidWater,
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

    private static final class GroundPathRecoveryMemory {
        private double x;
        private double y;
        private double z;
        private Vec3 lastProgressPosition;
        private long lastProgressAt;
        private long nextRetryAt;
        private int failures;
        private boolean attempted;

        private GroundPathRecoveryMemory(
                double x,
                double y,
                double z,
                long gameTime,
                Vec3 position
        ) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.lastProgressPosition = position;
            this.lastProgressAt = gameTime;
        }

        private void updateDestination(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        private void markAttempted(long gameTime, Vec3 position) {
            if (!attempted) {
                lastProgressPosition = position;
                lastProgressAt = gameTime;
            }

            attempted = true;
        }

        private void markProgress(long gameTime, Vec3 position) {
            lastProgressPosition = position;
            lastProgressAt = gameTime;
            nextRetryAt = Long.MIN_VALUE;
            failures = 0;
            attempted = true;
        }

        private void markReached(long gameTime, Vec3 position) {
            markProgress(gameTime, position);
            attempted = false;
        }

        private boolean isUnreachable() {
            return failures >= GROUND_PATH_FAILURES_BEFORE_UNREACHABLE;
        }
    }

    public enum MovementOutcome {
        MOVING,
        DEFERRED,
        RETRYING,
        UNREACHABLE,
        UNSUPPORTED;

        public boolean isMoving() {
            return this == MOVING;
        }

        public boolean shouldRecover() {
            return this == UNREACHABLE;
        }
    }

    public enum ReachabilityResult {
        REACHABLE,
        UNREACHABLE,
        DEFERRED
    }
}
