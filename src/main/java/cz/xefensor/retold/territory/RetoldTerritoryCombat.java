package cz.xefensor.retold.territory;

import cz.xefensor.retold.combat.RetoldAiTargets;
import cz.xefensor.retold.combat.RetoldCombatTargets;
import cz.xefensor.retold.combat.RetoldFactionTargetMemory;
import cz.xefensor.retold.combat.RetoldTargetSource;
import cz.xefensor.retold.faction.RetoldFactionMembers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;

import java.util.List;
import java.util.Map;

public final class RetoldTerritoryCombat {
    private static final int ATTACK_REFRESH_INTERVAL_TICKS = 100;

    private RetoldTerritoryCombat() {
    }

    public static boolean tryAdoptRetaliationTarget(
            ServerLevel level,
            PathfinderMob mob,
            RetoldTerritoryMobState state,
            RetoldTerritoryConfig config,
            Map<PathfinderMob, RetoldTerritoryMobState> mobStates,
            long gameTime
    ) {
        LivingEntity attacker = mob.getLastHurtByMob();

        if (attacker == null) {
            return false;
        }

        if (!RetoldTerritoryTargetSelector.isPossibleIntruder(level, mob, attacker, config, gameTime)) {
            return false;
        }

        if (!RetoldTerritoryTargetSelector.isValidAttackTarget(mob, attacker)) {
            return false;
        }

        startAttackOnTarget(
                level,
                mob,
                state,
                config,
                attacker,
                gameTime,
                RetoldTargetSource.RETALIATION
        );

        return true;
    }

    public static boolean tryAdoptOwnedCombatTarget(
            ServerLevel level,
            PathfinderMob mob,
            RetoldTerritoryMobState state,
            RetoldTerritoryConfig config,
            Map<PathfinderMob, RetoldTerritoryMobState> mobStates,
            long gameTime
    ) {
        LivingEntity existingTarget = getCurrentCombatTarget(mob);

        if (existingTarget == null) {
            return false;
        }

        if (!RetoldFactionTargetMemory.isOwnedByAny(
                mob,
                existingTarget,
                RetoldTargetSource.FACTION_ASSIST,
                RetoldTargetSource.FACTION_COMBAT,
                RetoldTargetSource.TERRITORY_ATTACK,
                RetoldTargetSource.RETALIATION
        )) {
            return false;
        }

        if (!RetoldTerritoryTargetSelector.isPossibleIntruder(level, mob, existingTarget, config, gameTime)) {
            return false;
        }

        if (!RetoldTerritoryTargetSelector.isValidAttackTarget(mob, existingTarget)) {
            return false;
        }

        if (!RetoldTerritoryTargetSelector.canAttackByTerritoryReputation(state, existingTarget)) {
            return false;
        }

        startAttackOnTarget(
                level,
                mob,
                state,
                config,
                existingTarget,
                gameTime,
                RetoldTargetSource.TERRITORY_ATTACK
        );

        return true;
    }

    public static boolean tryAdoptExistingAttackTarget(
            ServerLevel level,
            PathfinderMob mob,
            RetoldTerritoryMobState state,
            RetoldTerritoryConfig config,
            Map<PathfinderMob, RetoldTerritoryMobState> mobStates,
            long gameTime
    ) {
        LivingEntity existingTarget = getCurrentCombatTarget(mob);

        if (existingTarget == null) {
            return false;
        }

        if (existingTarget == mob.getLastHurtByMob()) {
            return false;
        }

        if (!RetoldTerritoryTargetSelector.isPossibleIntruder(level, mob, existingTarget, config, gameTime)) {
            return false;
        }

        if (!RetoldTerritoryTargetSelector.isValidAttackTarget(mob, existingTarget)) {
            return false;
        }

        if (!RetoldTerritoryTargetSelector.canAttackByTerritoryReputation(state, existingTarget)) {
            return false;
        }

        startAttackOnTarget(
                level,
                mob,
                state,
                config,
                existingTarget,
                gameTime,
                RetoldTargetSource.TERRITORY_ATTACK
        );

        return true;
    }

    public static boolean suppressExistingTargetDuringWarning(
            ServerLevel level,
            PathfinderMob mob,
            RetoldTerritoryConfig config,
            Map<PathfinderMob, RetoldTerritoryMobState> mobStates,
            long gameTime
    ) {
        LivingEntity existingTarget = getCurrentCombatTarget(mob);

        if (existingTarget == null) {
            return false;
        }

        if (RetoldFactionTargetMemory.isOwnedByAny(
                mob,
                existingTarget,
                RetoldTargetSource.TERRITORY_ATTACK,
                RetoldTargetSource.RETALIATION
        )) {
            return false;
        }

        if (existingTarget == mob.getLastHurtByMob()) {
            return false;
        }

        if (!RetoldTerritoryTargetSelector.isPossibleIntruder(level, mob, existingTarget, config, gameTime)) {
            return false;
        }

        RetoldTerritoryMobState state = mobStates.get(mob);

        if (state != null && RetoldTerritoryTargetSelector.canAttackByTerritoryReputation(state, existingTarget)) {
            return false;
        }

        clearUnauthorizedCombatTarget(mob, existingTarget);
        return true;
    }

    public static void startAttackOnTarget(
            ServerLevel level,
            PathfinderMob mob,
            RetoldTerritoryMobState state,
            RetoldTerritoryConfig config,
            LivingEntity target,
            long gameTime,
            RetoldTargetSource source
    ) {
        if (target == null) {
            return;
        }

        RetoldWarningPose.stopWarningPose(mob);

        state.hasStartedAttack = true;
        state.attackTarget = target;
        state.warningTarget = target;
        state.warningPulses = 0;
        state.nextAttackRefreshAt = gameTime + 20L;
        state.warnedIntruders.add(target.getUUID());

        applyAttackTarget(mob, target, source);
    }

    public static void updateAttackState(
            ServerLevel level,
            PathfinderMob mob,
            RetoldTerritoryMobState state,
            RetoldTerritoryConfig config,
            Map<PathfinderMob, RetoldTerritoryMobState> mobStates,
            long gameTime
    ) {
        LivingEntity attackTarget = state.attackTarget;

        if (
                attackTarget == null
                        || !RetoldTerritoryTargetSelector.isSelectableTerritoryAttackTarget(
                        level,
                        mob,
                        state,
                        config,
                        attackTarget,
                        gameTime
                )
        ) {
            LivingEntity replacement = RetoldTerritoryTargetSelector.findBestAvailableAttackTarget(
                    level,
                    mob,
                    state,
                    config,
                    mobStates,
                    gameTime
            );

            if (replacement == null) {
                returnToWarningMode(mob, state, gameTime);
                return;
            }

            startAttackOnTarget(
                    level,
                    mob,
                    state,
                    config,
                    replacement,
                    gameTime,
                    RetoldTargetSource.TERRITORY_ATTACK
            );

            return;
        }

        if (gameTime >= state.nextAttackRefreshAt) {
            LivingEntity betterTarget = RetoldTerritoryTargetSelector.findBestAvailableAttackTarget(
                    level,
                    mob,
                    state,
                    config,
                    mobStates,
                    gameTime
            );

            if (
                    betterTarget != null
                            && betterTarget != attackTarget
                            && RetoldTerritoryTargetSelector.shouldSwitchAttackTarget(
                            level,
                            mob,
                            state,
                            config,
                            attackTarget,
                            betterTarget,
                            mobStates
                    )
            ) {
                attackTarget = betterTarget;
                state.attackTarget = betterTarget;
                state.warningTarget = betterTarget;
                state.warnedIntruders.add(betterTarget.getUUID());
            }

            RetoldTargetSource source = mob.getLastHurtByMob() == attackTarget
                    ? RetoldTargetSource.RETALIATION
                    : RetoldTargetSource.TERRITORY_ATTACK;

            applyAttackTarget(mob, attackTarget, source);
            state.nextAttackRefreshAt = gameTime + ATTACK_REFRESH_INTERVAL_TICKS;
        }
    }

    public static void signalNearbyWarnedGuardsToAttack(
            ServerLevel level,
            PathfinderMob caller,
            RetoldTerritoryConfig config,
            Map<PathfinderMob, RetoldTerritoryMobState> mobStates,
            long gameTime
    ) {
        List<PathfinderMob> nearbyGuards = level.getEntitiesOfClass(
                PathfinderMob.class,
                caller.getBoundingBox().inflate(RetoldTerritoryConstants.ATTACK_CHAIN_RADIUS_BLOCKS),
                other -> other != caller
                        && other.isAlive()
                        && RetoldFactionMembers.isMemberOf(other, config.faction)
        );

        for (PathfinderMob guard : nearbyGuards) {
            RetoldTerritoryMobState guardState = mobStates.computeIfAbsent(
                    guard,
                    ignored -> new RetoldTerritoryMobState()
            );

            guardState.territoryContext = RetoldTerritoryDetector.getContextAt(
                    level,
                    guard.blockPosition()
            );

            if (guardState.territoryContext == null || guardState.territoryContext.faction() != config.faction) {
                continue;
            }

            LivingEntity attackTarget = RetoldTerritoryTargetSelector.findBestAvailableAttackTarget(
                    level,
                    guard,
                    guardState,
                    config,
                    mobStates,
                    gameTime
            );

            if (attackTarget == null) {
                continue;
            }

            startAttackOnTarget(
                    level,
                    guard,
                    guardState,
                    config,
                    attackTarget,
                    gameTime,
                    RetoldTargetSource.TERRITORY_ATTACK
            );
        }
    }

    private static void applyAttackTarget(
            PathfinderMob mob,
            LivingEntity target,
            RetoldTargetSource source
    ) {
        RetoldCombatTargets.applyAttackTarget(mob, target, source);
    }

    private static LivingEntity getCurrentCombatTarget(PathfinderMob mob) {
        LivingEntity target = mob.getTarget();

        if (target != null) {
            return target;
        }

        return RetoldAiTargets.getBrainAttackTargetSafely(mob);
    }

    private static void clearUnauthorizedCombatTarget(
            PathfinderMob mob,
            LivingEntity target
    ) {
        if (target == null) {
            return;
        }

        RetoldCombatTargets.clearTargetReferencesAndAggression(mob, target, true);
    }

    private static void returnToWarningMode(
            PathfinderMob mob,
            RetoldTerritoryMobState state,
            long gameTime
    ) {
        LivingEntity oldAttackTarget = state.attackTarget;

        if (oldAttackTarget != null) {
            RetoldFactionTargetMemory.clearTargetIfOwnedByAny(
                    mob,
                    oldAttackTarget,
                    RetoldTargetSource.TERRITORY_ATTACK,
                    RetoldTargetSource.RETALIATION
            );
        }

        RetoldTerritoryController.resetWarningState(state, gameTime);
    }
}
