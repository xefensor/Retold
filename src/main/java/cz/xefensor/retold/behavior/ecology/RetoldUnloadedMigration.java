package cz.xefensor.retold.behavior.ecology;

import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.control.RetoldAiControlMode;
import cz.xefensor.retold.behavior.control.RetoldAiControlOwner;
import cz.xefensor.retold.behavior.control.RetoldAiPriorities;
import cz.xefensor.retold.behavior.core.RetoldBehaviorCoordinator;
import cz.xefensor.retold.behavior.core.RetoldBehaviorMovement;
import cz.xefensor.retold.behavior.food.RetoldAnimalFeederBehavior;
import cz.xefensor.retold.behavior.food.RetoldRangeForage;
import cz.xefensor.retold.behavior.home.RetoldAnimalHomeMemory;
import cz.xefensor.retold.behavior.home.RetoldAnimalHomeType;
import cz.xefensor.retold.behavior.home.RetoldAnimalHomes;
import cz.xefensor.retold.behavior.home.RetoldAnimalSocialGroups;
import cz.xefensor.retold.behavior.performance.RetoldAiScanCache;
import cz.xefensor.retold.behavior.performance.RetoldAiWorkBudget;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;
import cz.xefensor.retold.behavior.profiles.RetoldMobState;
import cz.xefensor.retold.behavior.profiles.RetoldMobStates;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.AABB;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

/**
 * Bounded physical migration for real herds and schools returning after a
 * long unloaded gap. No entity moves until the complete group has distinct,
 * collision-free landings and a reachable path to each one.
 */
final class RetoldUnloadedMigration {
    static final long MINIMUM_UNLOADED_TICKS = 24_000L;
    static final int MAX_GROUPS_PER_TICK = 1;

    private static final int MAX_PENDING_TASKS = 4_096;
    private static final int LANDING_SEARCH_RADIUS = 4;
    private static final int[] LANDING_Y_OFFSETS = {0, 1, -1, 2, -2};
    private static final int CONTROL_TICKS = 20 * 5;
    private static final int CONTROL_PRIORITY = RetoldAiPriorities.above(
            RetoldAiPriorities.FEED,
            1
    );
    private static final String CONTROL_REASON = "unloaded_migration";

    private static final Queue<MigrationTask> PENDING = new ArrayDeque<>();
    private static final Set<UUID> QUEUED_MOBS = new HashSet<>();

    private RetoldUnloadedMigration() {
    }

    static synchronized void enqueueIfEligible(
            ServerLevel level,
            PathfinderMob mob,
            RetoldMobState state,
            RetoldUnloadedCatchUpPlan.Plan plan,
            int mealsConsumed
    ) {
        MigrationPolicy policy = MigrationPolicy.forMob(mob);
        RetoldAnimalHomeMemory home = RetoldAnimalHomes.get(mob);

        if (level == null
                || mob == null
                || state == null
                || plan == null
                || policy == null
                || mealsConsumed > 0
                || plan.simulatedTicks() < MINIMUM_UNLOADED_TICKS
                || state.hunger() < policy.migrationHunger()
                || !RetoldAnimalHomes.isValidFor(level, mob, home)
                || home.type() != policy.homeType()) {
            return;
        }

        UUID mobId = mob.getUUID();

        if (PENDING.size() >= MAX_PENDING_TASKS
                || !QUEUED_MOBS.add(mobId)) {
            return;
        }

        if (!claimMigrationControl(mob, level.getGameTime())) {
            QUEUED_MOBS.remove(mobId);
            return;
        }

        PENDING.add(new MigrationTask(level, mob, home));
    }

    static synchronized int processPending(int maximumGroups) {
        int processed = 0;
        int limit = Math.max(0, maximumGroups);

        while (processed < limit && !PENDING.isEmpty()) {
            MigrationTask task = PENDING.remove();
            QUEUED_MOBS.remove(task.mob().getUUID());
            processed++;

            MigrationResult result = migrate(task);

            if (result == MigrationResult.DEFERRED) {
                if (claimMigrationControl(
                        task.mob(),
                        task.level().getGameTime()
                )) {
                    requeue(task);
                } else {
                    clearMigrationControl(task.mob());
                }
            } else {
                clearMigrationControl(task.mob());
            }
        }

        return processed;
    }

    static synchronized int pendingCount() {
        return PENDING.size();
    }

    static synchronized void clear() {
        for (MigrationTask task : PENDING) {
            clearMigrationControl(task.mob());
        }

        PENDING.clear();
        QUEUED_MOBS.clear();
    }

    private static boolean claimMigrationControl(
            PathfinderMob mob,
            long gameTime
    ) {
        return RetoldAiControl.tryClaim(
                mob,
                RetoldAiControlMode.REGROUP,
                RetoldAiControlOwner.UNLOADED_ECOLOGY,
                CONTROL_PRIORITY,
                CONTROL_REASON,
                gameTime,
                CONTROL_TICKS
        );
    }

    private static void clearMigrationControl(PathfinderMob mob) {
        RetoldAiControl.clearIfControlledAsByWithReason(
                mob,
                RetoldAiControlMode.REGROUP,
                RetoldAiControlOwner.UNLOADED_ECOLOGY,
                CONTROL_REASON
        );
    }

    private static MigrationResult migrate(MigrationTask task) {
        PathfinderMob mob = task.mob();
        ServerLevel level = task.level();
        MigrationPolicy policy = MigrationPolicy.forMob(mob);

        if (!task.isStillValid()
                || policy == null
                || task.expectedHome().type() != policy.homeType()
                || !canRelocate(mob)) {
            return MigrationResult.FINISHED;
        }

        RetoldMobState state = RetoldMobStates.get(mob);

        if (state == null || state.hunger() < policy.migrationHunger()) {
            return MigrationResult.FINISHED;
        }

        long gameTime = level.getGameTime();

        if (policy.usesFeeder()
                && RetoldAnimalFeederBehavior.hasUsableFoodNearby(
                level,
                mob,
                task.expectedHome().pos(),
                gameTime
        )) {
            return MigrationResult.FINISHED;
        }

        RetoldAiScanCache.FreshScanResult<PathfinderMob> scan =
                RetoldAiScanCache.freshNearbyAt(
                        level,
                        task.expectedHome().pos(),
                        PathfinderMob.class,
                        RetoldAnimalSocialGroups.homeSeparationBlocks(mob),
                        gameTime
                );

        if (scan.deferred()) {
            return MigrationResult.DEFERRED;
        }

        List<PathfinderMob> group = currentGroup(task, policy, scan.entities());

        if (group.isEmpty()
                || group.getFirst() != mob
                || group.stream().anyMatch(member -> !canRelocate(member))) {
            return MigrationResult.FINISHED;
        }

        if (group.stream().anyMatch(
                member -> !isReadyForPathProbe(member, policy.aquatic())
        )) {
            return MigrationResult.DEFERRED;
        }

        if (!RetoldAiWorkBudget.tryUseFairBlockSearch(mob, gameTime)) {
            return MigrationResult.DEFERRED;
        }

        int currentScore = RetoldRangeForage.forageScore(
                level,
                mob,
                task.expectedHome().pos(),
                policy.horizontalRadius(),
                policy.verticalRadius(),
                gameTime,
                policy.cacheTicks()
        );

        if (currentScore > policy.depletedScore()) {
            return MigrationResult.FINISHED;
        }

        BlockPos newCenter = RetoldRangeForage.findBetterForageCenter(
                level,
                mob,
                task.expectedHome().pos(),
                policy.horizontalRadius(),
                policy.verticalRadius(),
                currentScore,
                policy.targetScore(),
                gameTime,
                policy.cacheTicks()
        );

        if (newCenter == null) {
            return MigrationResult.FINISHED;
        }

        List<Landing> landings = findLandings(
                level,
                group,
                newCenter,
                policy.aquatic()
        );

        if (landings.size() != group.size()) {
            return MigrationResult.FINISHED;
        }

        for (Landing landing : landings) {
            RetoldBehaviorMovement.ReachabilityResult reachability =
                    RetoldBehaviorMovement.probeMigrationReachability(
                            landing.mob(),
                            landing.pos(),
                            gameTime
                    );

            if (reachability == RetoldBehaviorMovement.ReachabilityResult.DEFERRED) {
                return MigrationResult.DEFERRED;
            }

            if (reachability != RetoldBehaviorMovement.ReachabilityResult.REACHABLE) {
                return MigrationResult.FINISHED;
            }
        }

        for (Landing landing : landings) {
            PathfinderMob member = landing.mob();
            BlockPos pos = landing.pos();
            member.getNavigation().stop();
            RetoldAiControl.clear(member);
            member.setSprinting(false);
            member.snapTo(
                    pos.getX() + 0.5D,
                    policy.aquatic() ? pos.getY() + 0.5D : pos.getY(),
                    pos.getZ() + 0.5D,
                    member.getYRot(),
                    member.getXRot()
            );
        }

        RetoldAnimalHomes.replacePackHome(
                level,
                mob,
                group.subList(1, group.size()),
                newCenter,
                gameTime
        );
        return MigrationResult.FINISHED;
    }

    private static List<PathfinderMob> currentGroup(
            MigrationTask task,
            MigrationPolicy policy,
            List<PathfinderMob> scanned
    ) {
        List<PathfinderMob> group = new ArrayList<>();
        group.add(task.mob());

        for (PathfinderMob candidate : scanned) {
            if (candidate == task.mob()
                    || !policy.compatible(task.mob(), candidate)
                    || !RetoldAnimalHomes.hasSameValidHomeAs(
                    task.level(),
                    candidate,
                    task.expectedHome()
            )) {
                continue;
            }

            group.add(candidate);
        }

        group.sort(Comparator.comparingInt(PathfinderMob::getId));
        return List.copyOf(group);
    }

    private static List<Landing> findLandings(
            ServerLevel level,
            List<PathfinderMob> group,
            BlockPos center,
            boolean aquatic
    ) {
        List<Landing> landings = new ArrayList<>();
        List<AABB> occupied = new ArrayList<>();

        for (PathfinderMob member : group) {
            Landing landing = findLanding(
                    level,
                    member,
                    center,
                    aquatic,
                    occupied
            );

            if (landing == null) {
                return List.of();
            }

            landings.add(landing);
            occupied.add(landing.box());
        }

        return List.copyOf(landings);
    }

    private static Landing findLanding(
            ServerLevel level,
            PathfinderMob mob,
            BlockPos center,
            boolean aquatic,
            List<AABB> occupied
    ) {
        for (int radius = 0; radius <= LANDING_SEARCH_RADIUS; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                        continue;
                    }

                    for (int dy : LANDING_Y_OFFSETS) {
                        BlockPos pos = center.offset(dx, dy, dz);
                        Landing landing = validLanding(
                                level,
                                mob,
                                pos,
                                aquatic,
                                occupied
                        );

                        if (landing != null) {
                            return landing;
                        }
                    }
                }
            }
        }

        return null;
    }

    private static Landing validLanding(
            ServerLevel level,
            PathfinderMob mob,
            BlockPos pos,
            boolean aquatic,
            List<AABB> occupied
    ) {
        if (!level.hasChunkAt(pos)
                || level.isOutsideBuildHeight(pos)
                || !level.getWorldBorder().isWithinBounds(pos)) {
            return null;
        }

        if (aquatic) {
            if (!level.getFluidState(pos).is(FluidTags.WATER)) {
                return null;
            }
        } else if (!mob.getNavigation().isStableDestination(pos)) {
            return null;
        }

        double targetY = aquatic ? pos.getY() + 0.5D : pos.getY();
        AABB targetBox = mob.getBoundingBox().move(
                pos.getX() + 0.5D - mob.getX(),
                targetY - mob.getY(),
                pos.getZ() + 0.5D - mob.getZ()
        );

        if (!level.noCollision(mob, targetBox)
                || occupied.stream().anyMatch(targetBox::intersects)) {
            return null;
        }

        return new Landing(mob, pos.immutable(), targetBox);
    }

    private static boolean canRelocate(PathfinderMob mob) {
        if (mob == null) {
            return false;
        }

        if (!mob.isAlive()
                || mob.isRemoved()
                || mob.isNoAi()
                || mob.isLeashed()
                || mob.isPassenger()
                || mob.isVehicle()
                || RetoldBehaviorCoordinator.hasLiveTarget(mob)) {
            return false;
        }

        RetoldAiControlMode mode = RetoldAiControl.getMode(mob);
        return mode == RetoldAiControlMode.NONE
                || mode == RetoldAiControlMode.REGROUP;
    }

    private static boolean isReadyForPathProbe(
            PathfinderMob mob,
            boolean aquatic
    ) {
        return aquatic
                ? mob.isInWater()
                || mob.level().getFluidState(mob.blockPosition()).is(
                FluidTags.WATER
        )
                : mob.onGround()
                || mob.getNavigation().isStableDestination(
                mob.blockPosition()
        );
    }

    private static void requeue(MigrationTask task) {
        UUID mobId = task.mob().getUUID();

        if (PENDING.size() >= MAX_PENDING_TASKS
                || !QUEUED_MOBS.add(mobId)) {
            return;
        }

        PENDING.add(task);
    }

    private enum MigrationResult {
        FINISHED,
        DEFERRED
    }

    private record MigrationTask(
            ServerLevel level,
            PathfinderMob mob,
            RetoldAnimalHomeMemory expectedHome
    ) {
        private boolean isStillValid() {
            return mob.level() == level
                    && mob.isAlive()
                    && !mob.isRemoved()
                    && RetoldAnimalHomes.hasSameValidHomeAs(
                    level,
                    mob,
                    expectedHome
            );
        }
    }

    private record Landing(
            PathfinderMob mob,
            BlockPos pos,
            AABB box
    ) {
    }

    private record MigrationPolicy(
            RetoldAnimalHomeType homeType,
            int migrationHunger,
            int depletedScore,
            int targetScore,
            int horizontalRadius,
            int verticalRadius,
            int cacheTicks,
            boolean aquatic,
            boolean usesFeeder
    ) {
        private static MigrationPolicy forMob(PathfinderMob mob) {
            if (mob == null) {
                return null;
            }

            if (RetoldMobRules.isHungryGrazer(mob)) {
                return new MigrationPolicy(
                        RetoldAnimalHomeType.HERD_RANGE,
                        48,
                        8,
                        18,
                        10,
                        2,
                        80,
                        false,
                        true
                );
            }

            if (RetoldMobRules.isPig(mob)) {
                return new MigrationPolicy(
                        RetoldAnimalHomeType.FORAGING_RANGE,
                        45,
                        5,
                        12,
                        8,
                        2,
                        80,
                        false,
                        true
                );
            }

            if (RetoldMobRules.isAquaticSchool(mob)) {
                return new MigrationPolicy(
                        RetoldAnimalHomeType.AQUATIC_SCHOOL_RANGE,
                        48,
                        2,
                        8,
                        8,
                        3,
                        80,
                        true,
                        false
                );
            }

            return null;
        }

        private boolean compatible(
                PathfinderMob first,
                PathfinderMob second
        ) {
            return aquatic
                    ? RetoldAnimalSocialGroups.canSchoolWith(first, second)
                    : RetoldAnimalSocialGroups.canShareHomeOrRange(first, second);
        }
    }
}
