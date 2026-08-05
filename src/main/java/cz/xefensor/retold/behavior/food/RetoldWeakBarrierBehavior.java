package cz.xefensor.retold.behavior.food;

import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.control.RetoldAiControlMode;
import cz.xefensor.retold.behavior.control.RetoldAiControlOwner;
import cz.xefensor.retold.behavior.control.RetoldAiPriorities;
import cz.xefensor.retold.behavior.core.RetoldBehaviorCoordinator;
import cz.xefensor.retold.behavior.core.RetoldBehaviorMovement;
import cz.xefensor.retold.behavior.core.RetoldMobGriefing;
import cz.xefensor.retold.behavior.core.RetoldWeakBarriers;
import cz.xefensor.retold.behavior.performance.RetoldBlockTargetSearch;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;
import cz.xefensor.retold.behavior.profiles.RetoldMobState;
import cz.xefensor.retold.behavior.profiles.RetoldMobStates;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.phys.Vec3;

import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

public final class RetoldWeakBarrierBehavior {
    private static final RetoldAiControlOwner CONTROL_OWNER =
            RetoldAiControlOwner.BARRIER_BREACH;

    private static final int SEARCH_HORIZONTAL_RADIUS = 4;
    private static final int SEARCH_VERTICAL_RADIUS = 2;
    private static final int SEARCH_CACHE_TICKS = 30;
    private static final int CONTROL_TICKS = 20;
    private static final int PREDATOR_BREAK_TICKS = 20 * 3;
    private static final int DESPERATE_ANIMAL_BREAK_TICKS = 20 * 6;
    private static final int SUCCESS_COOLDOWN_TICKS = 20 * 10;
    private static final int INACTIVE_STATE_TICKS = 20 * 20;
    private static final int BREACH_PRIORITY =
            RetoldAiPriorities.below(RetoldAiPriorities.FEED, 1);

    private static final double REACH_DISTANCE = 2.4D;
    private static final double REACH_DISTANCE_SQUARED =
            REACH_DISTANCE * REACH_DISTANCE;
    private static final double PASSIVE_SPEED = 0.70D;
    private static final double PREDATOR_SPEED = 0.85D;

    private static final Map<PathfinderMob, BreachState> BREACHES =
            new WeakHashMap<>();

    private RetoldWeakBarrierBehavior() {
    }

    public static void tick(
            ServerLevel level,
            PathfinderMob mob,
            long gameTime
    ) {
        BreachKind kind = breachKind(mob);

        if (level == null
                || kind == null
                || !RetoldMobGriefing.canModifyBlocks(level, mob)) {
            cancel(level, mob, false);
            return;
        }

        BreachState breach = BREACHES.computeIfAbsent(
                mob,
                ignored -> new BreachState()
        );
        breach.lastSeenAt = gameTime;

        if (gameTime < breach.cooldownUntil) {
            clearProgress(level, mob, breach);
            RetoldAiControl.clearIfOwnedBy(mob, CONTROL_OWNER);
            return;
        }

        if (!canAttemptControl(mob)) {
            clearProgress(level, mob, breach);
            return;
        }

        BlockPos target = breach.target;

        if (target == null
                || !isNearby(mob, target)
                || !RetoldWeakBarriers.isBreakable(level.getBlockState(target))) {
            clearProgress(level, mob, breach);
            target = RetoldBlockTargetSearch.findWeakBarrier(
                    level,
                    mob,
                    SEARCH_HORIZONTAL_RADIUS,
                    SEARCH_VERTICAL_RADIUS,
                    gameTime,
                    SEARCH_CACHE_TICKS
            );
            breach.target = target;
        }

        if (target == null) {
            RetoldAiControl.clearIfOwnedBy(mob, CONTROL_OWNER);
            return;
        }

        if (!claimControl(mob, gameTime)) {
            clearProgress(level, mob, breach);
            return;
        }

        if (!isWithinReach(mob, target)) {
            clearProgress(level, mob, breach);
            RetoldBehaviorMovement.throttledMoveTo(
                    mob,
                    target,
                    kind == BreachKind.PREDATOR ? PREDATOR_SPEED : PASSIVE_SPEED,
                    gameTime,
                    8,
                    1.0D
            );
            return;
        }

        mob.setSprinting(false);
        mob.getNavigation().stop();
        mob.lookAt(
                net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,
                Vec3.atCenterOf(target)
        );

        if (breach.startedAt == Long.MIN_VALUE) {
            breach.startedAt = gameTime;
            breach.lastCrackStage = -1;
        }

        int breakTicks = kind == BreachKind.PREDATOR
                ? PREDATOR_BREAK_TICKS
                : DESPERATE_ANIMAL_BREAK_TICKS;
        long elapsed = Math.max(0L, gameTime - breach.startedAt);

        if (elapsed >= breakTicks) {
            finishBreak(level, mob, breach, target, gameTime);
            return;
        }

        int crackStage = Math.min(
                9,
                (int) (elapsed * 10L / breakTicks)
        );

        if (crackStage != breach.lastCrackStage) {
            breach.lastCrackStage = crackStage;
            level.destroyBlockProgress(mob.getId(), target, crackStage);
            mob.swing(InteractionHand.MAIN_HAND);
        }
    }

    public static void cleanup(long gameTime) {
        Iterator<Map.Entry<PathfinderMob, BreachState>> iterator =
                BREACHES.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<PathfinderMob, BreachState> entry = iterator.next();
            PathfinderMob mob = entry.getKey();
            BreachState state = entry.getValue();

            if (mob != null
                    && state != null
                    && mob.isAlive()
                    && !mob.isRemoved()
                    && gameTime - state.lastSeenAt <= INACTIVE_STATE_TICKS) {
                continue;
            }

            if (mob != null && mob.level() instanceof ServerLevel level) {
                clearProgress(level, mob, state);
                RetoldAiControl.clearIfOwnedBy(mob, CONTROL_OWNER);
            }

            iterator.remove();
        }
    }

    static void forget(PathfinderMob mob) {
        if (mob == null) {
            return;
        }

        if (mob.level() instanceof ServerLevel level) {
            cancel(level, mob, true);
        } else {
            BREACHES.remove(mob);
        }
    }

    static int breakTicks(PathfinderMob mob) {
        return breachKind(mob) == BreachKind.PREDATOR
                ? PREDATOR_BREAK_TICKS
                : DESPERATE_ANIMAL_BREAK_TICKS;
    }

    private static BreachKind breachKind(PathfinderMob mob) {
        if (!RetoldBehaviorCoordinator.isUsableMob(mob)
                || !RetoldMobRules.canUseOrdinaryLifeSystems(mob)
                || mob.isPassenger()
                || mob instanceof Leashable leashable && leashable.isLeashed()
                || !usesGroundNavigation(mob)) {
            return null;
        }

        RetoldMobState state = RetoldMobStates.getOrCreate(
                mob,
                mob.level().getGameTime()
        );

        if (RetoldMobRules.canUseOrdinaryPredatorSystems(mob)) {
            if (mob instanceof TamableAnimal tamable && tamable.isTame()) {
                return null;
            }

            return RetoldMobRules.hasHuntDrive(mob, state)
                    && RetoldBehaviorCoordinator.hasLiveTarget(mob)
                    ? BreachKind.PREDATOR
                    : null;
        }

        return RetoldMobRules.hungerInterval(mob) > 0
                && RetoldMobRules.hasDesperateFoodDrive(state)
                && !RetoldBehaviorCoordinator.hasLiveTarget(mob)
                ? BreachKind.DESPERATE_ANIMAL
                : null;
    }

    private static boolean usesGroundNavigation(PathfinderMob mob) {
        return mob.getNavigation() instanceof GroundPathNavigation
                || mob.getNavigation() instanceof AmphibiousPathNavigation;
    }

    private static boolean claimControl(
            PathfinderMob mob,
            long gameTime
    ) {
        return RetoldAiControl.tryClaim(
                mob,
                RetoldAiControlMode.SEARCH,
                CONTROL_OWNER,
                BREACH_PRIORITY,
                "breach_weak_barrier",
                gameTime,
                CONTROL_TICKS
        );
    }

    private static boolean canAttemptControl(PathfinderMob mob) {
        return RetoldAiControl.isControlledBy(mob, CONTROL_OWNER)
                || BREACH_PRIORITY > RetoldAiControl.getPriority(mob);
    }

    private static boolean isNearby(
            PathfinderMob mob,
            BlockPos pos
    ) {
        BlockPos center = mob.blockPosition();

        return Math.abs(pos.getX() - center.getX()) <= SEARCH_HORIZONTAL_RADIUS
                && Math.abs(pos.getY() - center.getY()) <= SEARCH_VERTICAL_RADIUS
                && Math.abs(pos.getZ() - center.getZ()) <= SEARCH_HORIZONTAL_RADIUS;
    }

    private static boolean isWithinReach(
            PathfinderMob mob,
            BlockPos pos
    ) {
        return mob.distanceToSqr(Vec3.atCenterOf(pos)) <= REACH_DISTANCE_SQUARED;
    }

    private static void finishBreak(
            ServerLevel level,
            PathfinderMob mob,
            BreachState breach,
            BlockPos target,
            long gameTime
    ) {
        clearProgress(level, mob, breach);

        if (!RetoldMobGriefing.canModifyBlocks(level, mob)
                || !RetoldWeakBarriers.isBreakable(level.getBlockState(target))) {
            breach.target = null;
            return;
        }

        if (level.destroyBlock(target, true, mob)) {
            breach.cooldownUntil = gameTime + SUCCESS_COOLDOWN_TICKS;
        }

        breach.target = null;
        mob.getNavigation().stop();
        RetoldAiControl.clearIfOwnedBy(mob, CONTROL_OWNER);
    }

    private static void cancel(
            ServerLevel level,
            PathfinderMob mob,
            boolean removeState
    ) {
        if (mob == null) {
            return;
        }

        BreachState breach = removeState
                ? BREACHES.remove(mob)
                : BREACHES.get(mob);
        clearProgress(level, mob, breach);
        RetoldAiControl.clearIfOwnedBy(mob, CONTROL_OWNER);
    }

    private static void clearProgress(
            ServerLevel level,
            PathfinderMob mob,
            BreachState breach
    ) {
        if (breach == null) {
            return;
        }

        if (level != null && mob != null && breach.target != null && breach.startedAt != Long.MIN_VALUE) {
            level.destroyBlockProgress(mob.getId(), breach.target, -1);
        }

        breach.startedAt = Long.MIN_VALUE;
        breach.lastCrackStage = -1;
    }

    private enum BreachKind {
        PREDATOR,
        DESPERATE_ANIMAL
    }

    private static final class BreachState {
        private BlockPos target;
        private long startedAt = Long.MIN_VALUE;
        private long cooldownUntil;
        private long lastSeenAt;
        private int lastCrackStage = -1;
    }
}
