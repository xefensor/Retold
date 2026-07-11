package cz.xefensor.retold.territory;

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
        RetoldTerritoryContext territoryContext = RetoldTerritoryRules.getMatchingContext(
                level,
                mob,
                config
        );

        if (territoryContext == null) {
            clearStateOnly(mob, state, gameTime);
            return;
        }

        state.territoryContext = territoryContext;

        if (state.hasStartedAttack) {
            RetoldTerritoryCombat.updateAttackState(
                    level,
                    mob,
                    state,
                    config,
                    mobStates,
                    gameTime
            );
            return;
        }

        if (
                RetoldTerritoryCombat.tryAdoptRetaliationTarget(
                        level,
                        mob,
                        state,
                        config,
                        mobStates,
                        gameTime
                )
        ) {
            RetoldTerritoryCombat.updateAttackState(level, mob, state, config, mobStates, gameTime);
            return;
        }

        if (
                RetoldTerritoryCombat.tryAdoptOwnedCombatTarget(
                        level,
                        mob,
                        state,
                        config,
                        mobStates,
                        gameTime
                )
        ) {
            RetoldTerritoryCombat.updateAttackState(level, mob, state, config, mobStates, gameTime);
            return;
        }

        if (
                RetoldTerritoryCombat.tryAdoptExistingAttackTarget(
                        level,
                        mob,
                        state,
                        config,
                        mobStates,
                        gameTime
                )
        ) {
            RetoldTerritoryCombat.updateAttackState(level, mob, state, config, mobStates, gameTime);
            return;
        }

        if (
                RetoldTerritoryCombat.suppressExistingTargetDuringWarning(
                        level,
                        mob,
                        config,
                        mobStates,
                        gameTime
                )
        ) {
            RetoldWarningPose.stopWarningPose(mob);
            return;
        }

        updateWarningTarget(level, mob, state, config, mobStates, gameTime);

        LivingEntity warningTarget = state.warningTarget;

        if (warningTarget == null) {
            RetoldWarningPose.stopWarningPose(mob);
            resetWarningState(state, gameTime);
            return;
        }

        if (RetoldTerritoryReputation.getWarningLevel(state.territoryContext, warningTarget) == RetoldWarningLevel.ATTACK) {
            RetoldTerritoryCombat.startAttackOnTarget(
                    level,
                    mob,
                    state,
                    config,
                    warningTarget,
                    gameTime,
                    RetoldTargetSource.TERRITORY_ATTACK
            );

            RetoldTerritoryCombat.signalNearbyWarnedGuardsToAttack(
                    level,
                    mob,
                    config,
                    mobStates,
                    gameTime
            );
            return;
        }

        maintainContinuousBehavior(level, mob, state, config, mobStates, gameTime);

        if (!canCountWarningPulse(level, mob, warningTarget, config, state.territoryContext, gameTime)) {
            state.nextWarningPulseAt = Math.max(state.nextWarningPulseAt, gameTime + 10L);
            return;
        }

        if (gameTime < state.nextWarningPulseAt) {
            return;
        }

        markVisibleWarnedIntruders(level, mob, state, config, mobStates, gameTime);

        RetoldWarningLevel warningLevelBeforeGain = RetoldTerritoryReputation.getWarningLevel(
                state.territoryContext,
                warningTarget
        );

        updateFinalWarningTimer(state, warningLevelBeforeGain, gameTime);
        RetoldWarningEffects.playWarningEffects(level, mob, config, warningLevelBeforeGain);

        int suspicionGain = getWarningSuspicionGain(warningLevelBeforeGain);

        if (suspicionGain > 0) {
            RetoldTerritoryReputation.addVisibleWarningSuspicion(
                    state.territoryContext,
                    warningTarget,
                    suspicionGain,
                    gameTime
            );
        }

        if (isTooCloseDuringWarning(mob, warningTarget)) {
            RetoldTerritoryReputation.addTooCloseSuspicion(
                    state.territoryContext,
                    warningTarget,
                    gameTime
            );
        }

        RetoldWarningLevel warningLevelAfterGain = RetoldTerritoryReputation.getWarningLevel(
                state.territoryContext,
                warningTarget
        );

        updateFinalWarningTimer(state, warningLevelAfterGain, gameTime);

        state.warningPulses++;
        state.nextWarningPulseAt = gameTime + getWarningPulseInterval(mob, warningLevelAfterGain);

        if (canStartTerritoryAttack(state, warningLevelAfterGain, gameTime)) {
            RetoldTerritoryCombat.startAttackOnTarget(
                    level,
                    mob,
                    state,
                    config,
                    warningTarget,
                    gameTime,
                    RetoldTargetSource.TERRITORY_ATTACK
            );

            RetoldTerritoryCombat.signalNearbyWarnedGuardsToAttack(
                    level,
                    mob,
                    config,
                    mobStates,
                    gameTime
            );
        }
    }

    public static void maintainContinuousBehavior(
            ServerLevel level,
            PathfinderMob mob,
            RetoldTerritoryMobState state,
            RetoldTerritoryConfig config,
            Map<PathfinderMob, RetoldTerritoryMobState> mobStates,
            long gameTime
    ) {
        if (state.hasStartedAttack) {
            return;
        }

        state.territoryContext = RetoldTerritoryRules.refreshMatchingContext(
                state.territoryContext,
                level,
                mob,
                config
        );

        if (state.territoryContext == null) {
            RetoldWarningPose.stopWarningPose(mob);
            return;
        }

        LivingEntity warningTarget = state.warningTarget;

        if (
                warningTarget == null
                        || !canMaintainWarningAwareness(level, mob, state, config, warningTarget, gameTime)
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
            RetoldTerritoryReputation.markSeen(
                    state.territoryContext,
                    warningTarget,
                    gameTime
            );

            faceTargetSmoothly(mob, warningTarget);
        } else {
            faceLastKnownWarningPosition(mob, state);
        }

        if (warningLevel == RetoldWarningLevel.NONE) {
            mob.getNavigation().stop();
            return;
        }

        RetoldWarningMovement.tickWarningMovement(
                level,
                mob,
                config,
                state,
                warningTarget,
                warningLevel,
                mobStates,
                gameTime
        );
    }

    public static void setWarningTarget(
            RetoldTerritoryMobState state,
            PathfinderMob mob,
            LivingEntity target,
            long gameTime
    ) {
        if (state.warningTarget == target) {
            return;
        }

        state.warningTarget = target;
        state.attackTarget = null;
        state.hasStartedAttack = false;
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
        }
    }

    public static void resetWarningState(
            RetoldTerritoryMobState state,
            long gameTime
    ) {
        state.hasStartedAttack = false;
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

    private static void updateWarningTarget(
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
                currentTarget,
                mobStates
        );

        int bestFocus = RetoldWarningMovement.countNearbyFactionMobsFocusedOn(
                level,
                mob,
                config,
                bestTarget,
                mobStates
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

    private static void updateFinalWarningTimer(
            RetoldTerritoryMobState state,
            RetoldWarningLevel warningLevel,
            long gameTime
    ) {
        if (warningLevel == RetoldWarningLevel.FINAL_WARNING || warningLevel == RetoldWarningLevel.ATTACK) {
            if (state.finalWarningStartedAt < 0L) {
                state.finalWarningStartedAt = gameTime;
            }

            return;
        }

        state.finalWarningStartedAt = -1L;
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

        List<LivingEntity> nearbyIntruders = level.getEntitiesOfClass(
                LivingEntity.class,
                mob.getBoundingBox().inflate(RetoldTerritoryConstants.NOTICE_MOB_RADIUS_BLOCKS),
                target -> RetoldTerritoryTargetSelector.isPossibleIntruder(level, mob, target, config, gameTime)
                        && canSeeTarget(mob, target)
                        && canCountWarningPulse(level, mob, target, config, state.territoryContext, gameTime)
        );

        for (LivingEntity intruder : nearbyIntruders) {
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

    private static void clearStateOnly(
            PathfinderMob mob,
            RetoldTerritoryMobState state,
            long gameTime
    ) {
        RetoldWarningPose.stopWarningPose(mob);
        resetWarningState(state, gameTime);
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
