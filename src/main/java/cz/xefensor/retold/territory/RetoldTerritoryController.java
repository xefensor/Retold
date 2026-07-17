package cz.xefensor.retold.territory;

import cz.xefensor.retold.behavior.performance.RetoldAiScanCache;

import cz.xefensor.retold.combat.RetoldAiTargets;
import cz.xefensor.retold.combat.RetoldTargetSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;

import java.util.List;
import java.util.Map;

public final class RetoldTerritoryController {
    private RetoldTerritoryController() {
    }

    public static void updateMobTerritoryLogic(
            ServerLevel level,
            PathfinderMob mob,
            RetoldTerritoryMobState state,
            RetoldTerritoryConfig config,
            Map<PathfinderMob, RetoldTerritoryMobState> mobStates,
            long gameTime
    ) {
        RetoldTerritoryStateMachine.tick(
                level,
                mob,
                state,
                config,
                mobStates,
                gameTime,
                true
        );
    }

    public static void maintainContinuousBehavior(
            ServerLevel level,
            PathfinderMob mob,
            RetoldTerritoryMobState state,
            RetoldTerritoryConfig config,
            Map<PathfinderMob, RetoldTerritoryMobState> mobStates,
            long gameTime
    ) {
        RetoldTerritoryStateMachine.tick(
                level,
                mob,
                state,
                config,
                mobStates,
                gameTime,
                false
        );
    }

    static void tickWarningState(RetoldTerritoryStateContext context) {
        maintainWarningBehavior(context);

        if (context.decisionTick()) {
            tickWarningPulse(context);
        }
    }

    private static void maintainWarningBehavior(RetoldTerritoryStateContext context) {
        RetoldTerritoryMobState state = context.state();
        PathfinderMob mob = context.mob();
        LivingEntity warningTarget = state.warningTarget;

        if (
                warningTarget == null
                        || !canMaintainWarningAwareness(
                        context.level(),
                        mob,
                        state,
                        context.config(),
                        warningTarget,
                        context.gameTime()
                )
        ) {
            RetoldWarningPose.stopWarningPose(mob);
            return;
        }

        RetoldWarningLevel warningLevel = RetoldTerritoryReputation.getWarningLevel(
                state.territoryContext,
                warningTarget
        );
        RetoldWarningPose.updateWarningPose(mob, warningTarget, warningLevel);

        if (canSeeTarget(mob, warningTarget)) {
            RetoldTerritoryReputation.markSeen(state.territoryContext, warningTarget, context.gameTime());
            faceTargetSmoothly(mob, warningTarget);
        } else {
            faceLastKnownWarningPosition(mob, state);
        }

        if (warningLevel == RetoldWarningLevel.NONE) {
            mob.getNavigation().stop();
            return;
        }

        RetoldWarningMovement.tickWarningMovement(
                context.level(),
                mob,
                context.config(),
                state,
                warningTarget,
                warningLevel,
                context.mobStates(),
                context.gameTime()
        );
    }

    private static void tickWarningPulse(RetoldTerritoryStateContext context) {
        RetoldTerritoryMobState state = context.state();
        PathfinderMob mob = context.mob();
        LivingEntity warningTarget = state.warningTarget;

        if (warningTarget == null) {
            RetoldTerritoryStateMachine.deactivate(mob, state, context.gameTime());
            return;
        }

        if (!canCountWarningPulse(
                context.level(),
                mob,
                warningTarget,
                context.config(),
                state.territoryContext,
                context.gameTime()
        )) {
            state.nextWarningPulseAt = Math.max(state.nextWarningPulseAt, context.gameTime() + 10L);
            return;
        }

        if (context.gameTime() < state.nextWarningPulseAt) {
            return;
        }

        markVisibleWarnedIntruders(
                context.level(),
                mob,
                state,
                context.config(),
                context.mobStates(),
                context.gameTime()
        );

        RetoldWarningLevel beforeGain = RetoldTerritoryReputation.getWarningLevel(
                state.territoryContext,
                warningTarget
        );
        RetoldWarningEffects.playWarningEffects(context.level(), mob, context.config(), beforeGain);

        int suspicionGain = getWarningSuspicionGain(beforeGain);

        if (suspicionGain > 0) {
            RetoldTerritoryReputation.addVisibleWarningSuspicion(
                    state.territoryContext,
                    warningTarget,
                    suspicionGain,
                    context.gameTime()
            );
        }

        if (isTooCloseDuringWarning(mob, warningTarget)) {
            RetoldTerritoryReputation.addTooCloseSuspicion(
                    state.territoryContext,
                    warningTarget,
                    context.gameTime()
            );
        }

        RetoldWarningLevel afterGain = RetoldTerritoryReputation.getWarningLevel(
                state.territoryContext,
                warningTarget
        );
        state.warningPulses++;
        state.nextWarningPulseAt = context.gameTime() + getWarningPulseInterval(mob, afterGain);
        RetoldTerritoryStateMachine.reconcileWarningState(mob, state, context.gameTime());

        if (!canStartTerritoryAttack(state, afterGain, context.gameTime())) {
            return;
        }

        RetoldTerritoryCombat.startAttackOnTarget(
                context.level(),
                mob,
                state,
                context.config(),
                warningTarget,
                context.gameTime(),
                RetoldTargetSource.TERRITORY_ATTACK
        );
        RetoldTerritoryCombat.signalNearbyWarnedGuardsToAttack(
                context.level(),
                mob,
                context.config(),
                context.mobStates(),
                context.gameTime()
        );
    }

    public static void setWarningTarget(
            RetoldTerritoryMobState state,
            PathfinderMob mob,
            LivingEntity target,
            long gameTime
    ) {
        if (state.flowState == RetoldTerritoryFlowState.ATTACKING
                || state.flowState == RetoldTerritoryFlowState.COOLDOWN) {
            return;
        }

        if (state.warningTarget == target) {
            if (target != null) {
                RetoldTerritoryStateMachine.reconcileWarningState(mob, state, gameTime);
            }
            return;
        }

        state.warningTarget = target;
        state.attackTarget = null;
        state.finalWarningStartedAt = -1L;
        state.warningPulses = 0;
        state.nextWarningPulseAt = gameTime;
        state.nextTargetRecheckAt = gameTime
                + RetoldTerritoryConstants.WARNING_TARGET_RECHECK_INTERVAL_TICKS
                + Math.floorMod(mob.getId(), 12);
        state.hasWarningMoveTarget = false;
        state.warningMoveTargetSlot = Integer.MIN_VALUE;
        state.nextWarningPathRefreshAt = gameTime;

        if (target != null) {
            state.warningAnchorAngle = RetoldWarningMovement.getAngleFromTargetToMob(mob, target);
            state.warningFormationSlot = 0;
            state.nextFormationRecheckAt = gameTime;

            rememberWarningTargetPosition(state, target, gameTime);

            if (state.territoryContext != null) {
                RetoldTerritoryReputation.addTrespassSuspicion(
                        state.territoryContext,
                        target,
                        gameTime
                );
            }

            RetoldTerritoryStateMachine.reconcileWarningState(mob, state, gameTime);
            return;
        }

        RetoldTerritoryStateMachine.deactivate(mob, state, gameTime);
    }

    public static void resetWarningState(
            PathfinderMob mob,
            RetoldTerritoryMobState state,
            long gameTime
    ) {
        RetoldTerritoryStateMachine.deactivate(mob, state, gameTime);
    }

    static void clearWarningData(
            PathfinderMob mob,
            RetoldTerritoryMobState state,
            long gameTime
    ) {
        RetoldWarningPose.stopWarningPose(mob);
        state.finalWarningStartedAt = -1L;
        state.attackTarget = null;
        state.warningTarget = null;
        state.warningPulses = 0;
        state.nextWarningPulseAt = gameTime;
        state.nextTargetRecheckAt = gameTime;
        state.nextFormationRecheckAt = gameTime;
        state.hasWarningMoveTarget = false;
        state.warningMoveTargetSlot = Integer.MIN_VALUE;
        state.nextWarningPathRefreshAt = gameTime;
        state.warnedIntruders.clear();
    }

    static void updateWarningTarget(
            ServerLevel level,
            PathfinderMob mob,
            RetoldTerritoryMobState state,
            RetoldTerritoryConfig config,
            Map<PathfinderMob, RetoldTerritoryMobState> mobStates,
            long gameTime
    ) {
        LivingEntity currentTarget = state.warningTarget;

        if (
                currentTarget == null
                        || !canMaintainWarningAwareness(level, mob, state, config, currentTarget, gameTime)
        ) {
            LivingEntity bestTarget = RetoldTerritoryTargetSelector.findBestWarningTarget(
                    level,
                    mob,
                    config,
                    state,
                    mobStates,
                    gameTime
            );

            setWarningTarget(state, mob, bestTarget, gameTime);
            return;
        }

        if (gameTime < state.nextTargetRecheckAt) {
            return;
        }

        state.nextTargetRecheckAt = gameTime
                + RetoldTerritoryConstants.WARNING_TARGET_RECHECK_INTERVAL_TICKS
                + Math.floorMod(mob.getId(), 12);

        if (!canSeeTarget(mob, currentTarget)) {
            return;
        }

        LivingEntity bestTarget = RetoldTerritoryTargetSelector.findBestWarningTarget(
                level,
                mob,
                config,
                state,
                mobStates,
                gameTime
        );

        if (bestTarget == null || bestTarget == currentTarget) {
            return;
        }

        int currentFocus = RetoldWarningMovement.countNearbyFactionMobsFocusedOn(
                level,
                mob,
                config,
                state,
                currentTarget,
                mobStates,
                gameTime
        );

        int bestFocus = RetoldWarningMovement.countNearbyFactionMobsFocusedOn(
                level,
                mob,
                config,
                state,
                bestTarget,
                mobStates,
                gameTime
        );

        if (currentFocus > bestFocus + 1) {
            setWarningTarget(state, mob, bestTarget, gameTime);
        }
    }

    private static boolean canCountWarningPulse(
            ServerLevel level,
            PathfinderMob mob,
            LivingEntity target,
            RetoldTerritoryConfig config,
            RetoldTerritoryContext territoryContext,
            long gameTime
    ) {
        if (territoryContext == null) {
            return false;
        }

        if (!RetoldTerritoryTargetSelector.isValidWarningTarget(level, mob, target, config, gameTime)) {
            return false;
        }

        if (!canSeeTarget(mob, target)) {
            return false;
        }

        return true;
    }

    private static int getWarningPulseInterval(
            PathfinderMob mob,
            RetoldWarningLevel warningLevel
    ) {
        int baseInterval = switch (warningLevel) {
            case NONE -> RetoldTerritoryConstants.WARNING_NONE_PULSE_INTERVAL_TICKS;
            case NOTICED -> RetoldTerritoryConstants.WARNING_NOTICED_PULSE_INTERVAL_TICKS;
            case WARNING -> RetoldTerritoryConstants.WARNING_WARNING_PULSE_INTERVAL_TICKS;
            case FINAL_WARNING -> RetoldTerritoryConstants.WARNING_FINAL_WARNING_PULSE_INTERVAL_TICKS;
            case ATTACK -> RetoldTerritoryConstants.WARNING_ATTACK_PULSE_INTERVAL_TICKS;
        };

        return baseInterval + Math.floorMod(mob.getId(), 16);
    }

    private static int getWarningSuspicionGain(RetoldWarningLevel warningLevel) {
        return switch (warningLevel) {
            case NONE -> 0;
            case NOTICED -> RetoldTerritoryConstants.WARNING_NOTICED_SUSPICION_GAIN;
            case WARNING -> RetoldTerritoryConstants.WARNING_WARNING_SUSPICION_GAIN;
            case FINAL_WARNING -> RetoldTerritoryConstants.WARNING_FINAL_WARNING_SUSPICION_GAIN;
            case ATTACK -> 0;
        };
    }

    private static boolean isTooCloseDuringWarning(PathfinderMob mob, LivingEntity target) {
        return mob.distanceToSqr(target) <= RetoldTerritoryConstants.WARNING_TOO_CLOSE_DISTANCE_SQUARED;
    }

    private static boolean canStartTerritoryAttack(
            RetoldTerritoryMobState state,
            RetoldWarningLevel warningLevel,
            long gameTime
    ) {
        if (warningLevel != RetoldWarningLevel.ATTACK) {
            return false;
        }

        if (state.finalWarningStartedAt < 0L) {
            return false;
        }

        return gameTime - state.finalWarningStartedAt
                >= RetoldTerritoryConstants.WARNING_MIN_FINAL_WARNING_TICKS_BEFORE_ATTACK;
    }

    private static void markVisibleWarnedIntruders(
            ServerLevel level,
            PathfinderMob mob,
            RetoldTerritoryMobState state,
            RetoldTerritoryConfig config,
            Map<PathfinderMob, RetoldTerritoryMobState> mobStates,
            long gameTime
    ) {
        LivingEntity warningTarget = state.warningTarget;

        if (
                warningTarget != null
                        && RetoldTerritoryTargetSelector.isValidWarningTarget(level, mob, warningTarget, config, gameTime)
        ) {
            state.warnedIntruders.add(warningTarget.getUUID());
        }

        List<LivingEntity> nearbyIntruders = RetoldAiScanCache.nearby(
                level,
                mob,
                LivingEntity.class,
                RetoldTerritoryConstants.NOTICE_MOB_RADIUS_BLOCKS,
                gameTime,
                RetoldTerritoryConstants.WARNING_NEARBY_INTRUDER_SCAN_CACHE_TICKS
        );

        for (LivingEntity intruder : nearbyIntruders) {
            if (
                    !RetoldTerritoryTargetSelector.isPossibleIntruder(level, mob, intruder, config, gameTime)
                            || !canSeeTarget(mob, intruder)
                            || !canCountWarningPulse(level, mob, intruder, config, state.territoryContext, gameTime)
            ) {
                continue;
            }

            state.warnedIntruders.add(intruder.getUUID());
        }
    }

    private static boolean canMaintainWarningAwareness(
            ServerLevel level,
            PathfinderMob mob,
            RetoldTerritoryMobState state,
            RetoldTerritoryConfig config,
            LivingEntity target,
            long gameTime
    ) {
        if (!RetoldTerritoryTargetSelector.isValidWarningTarget(level, mob, target, config, gameTime)) {
            return false;
        }

        if (canSeeTarget(mob, target)) {
            rememberWarningTargetPosition(state, target, gameTime);
            return true;
        }

        return gameTime - state.lastSawWarningTargetAt
                <= RetoldTerritoryConstants.WARNING_LOST_SIGHT_MEMORY_TICKS;
    }

    private static void rememberWarningTargetPosition(
            RetoldTerritoryMobState state,
            LivingEntity target,
            long gameTime
    ) {
        state.lastSawWarningTargetAt = gameTime;
        state.lastKnownWarningTargetX = target.getX();
        state.lastKnownWarningTargetY = target.getY();
        state.lastKnownWarningTargetZ = target.getZ();
    }

    private static void faceTargetSmoothly(PathfinderMob mob, LivingEntity target) {
        double dx = target.getX() - mob.getX();
        double dz = target.getZ() - mob.getZ();

        if (dx * dx + dz * dz < 0.0001D) {
            return;
        }

        double dy = target.getEyeY() - mob.getEyeY();
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) (Math.atan2(dz, dx) * 57.2957763671875D) - 90.0F;
        float pitch = (float) (-(Math.atan2(dy, horizontalDistance) * 57.2957763671875D));

        mob.setYRot(rotateToward(mob.getYRot(), yaw, 10.0F));
        mob.setYHeadRot(rotateToward(mob.getYHeadRot(), yaw, 16.0F));
        mob.yBodyRot = rotateToward(mob.yBodyRot, yaw, 8.0F);
        mob.setXRot(rotateToward(mob.getXRot(), pitch, 3.5F));
    }

    private static void faceLastKnownWarningPosition(PathfinderMob mob, RetoldTerritoryMobState state) {
        double dx = state.lastKnownWarningTargetX - mob.getX();
        double dz = state.lastKnownWarningTargetZ - mob.getZ();

        if (dx * dx + dz * dz < 0.0001D) {
            return;
        }

        double dy = state.lastKnownWarningTargetY + 1.0D - mob.getEyeY();
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) (Math.atan2(dz, dx) * 57.2957763671875D) - 90.0F;
        float pitch = (float) (-(Math.atan2(dy, horizontalDistance) * 57.2957763671875D));

        mob.setYRot(rotateToward(mob.getYRot(), yaw, 10.0F));
        mob.setYHeadRot(rotateToward(mob.getYHeadRot(), yaw, 16.0F));
        mob.yBodyRot = rotateToward(mob.yBodyRot, yaw, 8.0F);
        mob.setXRot(rotateToward(mob.getXRot(), pitch, 3.5F));
    }

    private static boolean canSeeTarget(PathfinderMob mob, LivingEntity target) {
        return RetoldAiTargets.isVisibleTo(mob, target);
    }

    private static float rotateToward(float current, float target, float maxChange) {
        float difference = wrapDegrees(target - current);

        if (difference > maxChange) {
            difference = maxChange;
        }

        if (difference < -maxChange) {
            difference = -maxChange;
        }

        return current + difference;
    }

    private static float wrapDegrees(float degrees) {
        while (degrees >= 180.0F) {
            degrees -= 360.0F;
        }

        while (degrees < -180.0F) {
            degrees += 360.0F;
        }

        return degrees;
    }
}
