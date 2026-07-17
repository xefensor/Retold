package cz.xefensor.retold.behavior.species;

import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.control.RetoldAiControlMode;
import cz.xefensor.retold.behavior.control.RetoldAiControlOwner;
import cz.xefensor.retold.behavior.control.RetoldAiPriorities;
import cz.xefensor.retold.behavior.performance.RetoldAiScanCache;
import cz.xefensor.retold.behavior.performance.RetoldAiSightCache;
import cz.xefensor.retold.behavior.core.RetoldBehaviorCoordinator;
import cz.xefensor.retold.behavior.core.RetoldBehaviorMovement;
import cz.xefensor.retold.behavior.core.RetoldBehaviorTiming;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;

import cz.xefensor.retold.combat.RetoldCombatTargets;
import cz.xefensor.retold.combat.RetoldTargetSource;
import cz.xefensor.retold.faction.RetoldFaction;
import cz.xefensor.retold.faction.RetoldFactionMembers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.List;

public final class RetoldCommanderSupportEvents {
    private static final int THINK_INTERVAL_TICKS = 12;
    private static final int SUPPORT_SCAN_CACHE_TICKS = 6;
    private static final int SUPPORT_PATH_INTERVAL_TICKS = 8;
    private static final int SUPPORT_CONTROL_TICKS = 20 * 3;
    private static final int SUPPORT_PRIORITY = RetoldAiPriorities.above(RetoldAiPriorities.SUPPORT, 3);

    private static final double ALLY_SEARCH_RADIUS_BLOCKS = 24.0D;
    private static final double ALLY_SEARCH_RADIUS_SQUARED =
            ALLY_SEARCH_RADIUS_BLOCKS * ALLY_SEARCH_RADIUS_BLOCKS;

    private static final double SUPPORT_BACK_DISTANCE_BLOCKS = 7.5D;
    private static final double SUPPORT_SIDE_DISTANCE_BLOCKS = 3.0D;

    private static final double SUPPORT_REPOSITION_DISTANCE_BLOCKS = 5.0D;
    private static final double SUPPORT_REPOSITION_DISTANCE_SQUARED =
            SUPPORT_REPOSITION_DISTANCE_BLOCKS * SUPPORT_REPOSITION_DISTANCE_BLOCKS;

    private static final double SUPPORT_TOO_CLOSE_TO_TARGET_BLOCKS = 7.0D;
    private static final double SUPPORT_TOO_CLOSE_TO_TARGET_SQUARED =
            SUPPORT_TOO_CLOSE_TO_TARGET_BLOCKS * SUPPORT_TOO_CLOSE_TO_TARGET_BLOCKS;

    private static final double SUPPORT_SPEED = 0.82D;

    private RetoldCommanderSupportEvents() {
    }

    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof PathfinderMob support)) {
            return;
        }

        if (!(support.level() instanceof ServerLevel level)) {
            return;
        }

        if (!RetoldMobRules.isCommanderSupport(support)) {
            return;
        }

        long gameTime = level.getGameTime();

        if (!shouldThink(support, gameTime)) {
            return;
        }

        if (!canSupportNow(support)) {
            stopSupportIfOwned(support);
            return;
        }

        SupportContext context = findBestSupportContext(
                level,
                support
        );

        if (context == null) {
            stopSupportIfOwned(support);
            return;
        }

        adoptAllyTargetIfIdle(
                support,
                context.target()
        );

        repositionIfNeeded(
                support,
                context,
                gameTime
        );
    }

    private static boolean shouldThink(
            PathfinderMob support,
            long gameTime
    ) {
        return RetoldBehaviorTiming.shouldThink(
                support,
                gameTime,
                THINK_INTERVAL_TICKS
        );
    }

    private static boolean canSupportNow(PathfinderMob support) {
        if (support == null || !support.isAlive() || support.isRemoved()) {
            return false;
        }

        RetoldAiControlMode mode = RetoldAiControl.getMode(support);

        return mode == RetoldAiControlMode.NONE
                || RetoldAiControl.isControlledAsBy(
                support,
                RetoldAiControlMode.SUPPORT,
                RetoldAiControlOwner.COMMANDER_SUPPORT
        );
    }

    private static SupportContext findBestSupportContext(
            ServerLevel level,
            PathfinderMob support
    ) {
        List<PathfinderMob> allies = RetoldAiScanCache.nearby(
                level,
                support,
                PathfinderMob.class,
                ALLY_SEARCH_RADIUS_BLOCKS,
                level.getGameTime(),
                SUPPORT_SCAN_CACHE_TICKS
        );

        SupportContext best = null;
        double bestScore = Double.MAX_VALUE;

        for (PathfinderMob ally : allies) {
            if (!isValidSupportAlly(support, ally)) {
                continue;
            }

            LivingEntity target = ally.getTarget();

            if (!isValidSupportTarget(support, target)) {
                continue;
            }

            double distanceSquared = support.distanceToSqr(ally);

            if (distanceSquared > ALLY_SEARCH_RADIUS_SQUARED) {
                continue;
            }

            double score = distanceSquared;

            if (RetoldMobRules.isIllagerRaider(ally)) {
                score -= 28.0D;
            }

            if (RetoldAiSightCache.canSee(ally, target, level.getGameTime())) {
                score -= 12.0D;
            }

            if (score < bestScore) {
                best = new SupportContext(
                        ally,
                        target
                );
                bestScore = score;
            }
        }

        return best;
    }

    private static boolean isValidSupportAlly(
            PathfinderMob support,
            PathfinderMob ally
    ) {
        if (ally == null || ally == support) {
            return false;
        }

        if (!RetoldBehaviorCoordinator.isAliveInSameLevel(support, ally)) {
            return false;
        }

        if (support.distanceToSqr(ally) > ALLY_SEARCH_RADIUS_SQUARED) {
            return false;
        }

        return RetoldFactionMembers.isIllagerAligned(ally);
    }

    private static boolean isValidSupportTarget(
            PathfinderMob support,
            LivingEntity target
    ) {
        if (target == null || target == support) {
            return false;
        }

        if (!RetoldBehaviorCoordinator.isValidAssignmentTarget(support, target)) {
            return false;
        }

        return !RetoldFactionMembers.isIllagerAligned(target);
    }

    private static void adoptAllyTargetIfIdle(
            PathfinderMob support,
            LivingEntity target
    ) {
        if (support.getTarget() != null) {
            return;
        }

        RetoldCombatTargets.applyOwnedTarget(
                support,
                target,
                RetoldTargetSource.FACTION_ASSIST,
                false,
                false
        );
    }

    private static void repositionIfNeeded(
            PathfinderMob support,
            SupportContext context,
            long gameTime
    ) {
        Vec3 desired = desiredSupportPosition(
                support,
                context.ally(),
                context.target()
        );

        double distanceToDesired = support.position().distanceToSqr(desired);
        boolean tooCloseToTarget = support.distanceToSqr(context.target()) <= SUPPORT_TOO_CLOSE_TO_TARGET_SQUARED;
        boolean isInFront = isCloserToTargetThanAlly(
                support,
                context
        );

        if (
                distanceToDesired <= SUPPORT_REPOSITION_DISTANCE_SQUARED
                        && !tooCloseToTarget
                        && !isInFront
        ) {
            stopSupportIfOwned(support);
            return;
        }

        if (!RetoldAiControl.tryClaim(
                support,
                RetoldAiControlMode.SUPPORT,
                RetoldAiControlOwner.COMMANDER_SUPPORT,
                SUPPORT_PRIORITY,
                "support_ally",
                gameTime,
                SUPPORT_CONTROL_TICKS
        )) {
            return;
        }

        support.setSprinting(false);
        support.getLookControl().setLookAt(
                context.target(),
                30.0F,
                30.0F
        );

        RetoldBehaviorMovement.throttledMoveTo(
                support,
                desired.x,
                desired.y,
                desired.z,
                SUPPORT_SPEED,
                gameTime,
                SUPPORT_PATH_INTERVAL_TICKS,
                2.0D * 2.0D
        );
    }

    private static Vec3 desiredSupportPosition(
            PathfinderMob support,
            PathfinderMob ally,
            LivingEntity target
    ) {
        Vec3 awayFromTarget = ally.position()
                .subtract(target.position());

        awayFromTarget = new Vec3(
                awayFromTarget.x,
                0.0D,
                awayFromTarget.z
        );

        if (awayFromTarget.lengthSqr() <= 0.0001D) {
            awayFromTarget = support.position()
                    .subtract(target.position());
            awayFromTarget = new Vec3(
                    awayFromTarget.x,
                    0.0D,
                    awayFromTarget.z
            );
        }

        if (awayFromTarget.lengthSqr() <= 0.0001D) {
            awayFromTarget = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            awayFromTarget = awayFromTarget.normalize();
        }

        Vec3 side = new Vec3(
                -awayFromTarget.z,
                0.0D,
                awayFromTarget.x
        );

        double sideSign = Math.floorMod(support.getId(), 2) == 0
                ? 1.0D
                : -1.0D;

        return ally.position()
                .add(awayFromTarget.scale(SUPPORT_BACK_DISTANCE_BLOCKS))
                .add(side.scale(SUPPORT_SIDE_DISTANCE_BLOCKS * sideSign));
    }

    private static boolean isCloserToTargetThanAlly(
            PathfinderMob support,
            SupportContext context
    ) {
        double supportDistance = support.distanceToSqr(context.target());
        double allyDistance = context.ally().distanceToSqr(context.target());

        return supportDistance + 4.0D < allyDistance;
    }

    private static void stopSupportIfOwned(PathfinderMob support) {
        if (
                RetoldAiControl.isControlledAsBy(
                        support,
                        RetoldAiControlMode.SUPPORT,
                        RetoldAiControlOwner.COMMANDER_SUPPORT
                )
        ) {
            RetoldAiControl.clearIfOwnedBy(
                    support,
                    RetoldAiControlOwner.COMMANDER_SUPPORT
            );
            support.getNavigation().stop();
        }
    }

    private record SupportContext(
            PathfinderMob ally,
            LivingEntity target
    ) {
    }
}
