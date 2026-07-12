package cz.xefensor.retold.territory;

import cz.xefensor.retold.behavior.RetoldAiScanCache;
import cz.xefensor.retold.combat.RetoldAiTargets;
import cz.xefensor.retold.faction.RetoldFaction;
import cz.xefensor.retold.faction.RetoldFactionMembers;
import cz.xefensor.retold.faction.RetoldFactionRelations;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;

import java.util.List;
import java.util.Map;

public final class RetoldTerritoryTargetSelector {
    private RetoldTerritoryTargetSelector() {
    }

    public static LivingEntity findBestWarningTarget(
            ServerLevel level,
            PathfinderMob mob,
            RetoldTerritoryConfig config,
            RetoldTerritoryMobState state,
            Map<PathfinderMob, RetoldTerritoryMobState> mobStates,
            long gameTime
    ) {
        List<LivingEntity> candidates = RetoldAiScanCache.nearby(
                level,
                mob,
                LivingEntity.class,
                RetoldTerritoryConstants.NOTICE_MOB_RADIUS_BLOCKS,
                gameTime,
                RetoldTerritoryConstants.WARNING_NEARBY_INTRUDER_SCAN_CACHE_TICKS
        );

        LivingEntity bestTarget = null;
        double bestScore = Double.MAX_VALUE;

        for (LivingEntity candidate : candidates) {
            if (
                    !isPossibleIntruder(level, mob, candidate, config, gameTime)
                            || !canSeeTarget(mob, candidate)
            ) {
                continue;
            }

            int focusCount = RetoldWarningMovement.countNearbyFactionMobsFocusedOn(
                    level,
                    mob,
                    config,
                    state,
                    candidate,
                    mobStates,
                    gameTime
            );

            double distanceScore = mob.distanceToSqr(candidate) * 0.01D;
            double tieBreaker = getStableTieBreaker(mob, candidate);

            double score = focusCount * 1000.0D + distanceScore + tieBreaker;

            if (score < bestScore) {
                bestScore = score;
                bestTarget = candidate;
            }
        }

        return bestTarget;
    }

    public static LivingEntity findBestAvailableAttackTarget(
            ServerLevel level,
            PathfinderMob mob,
            RetoldTerritoryMobState state,
            RetoldTerritoryConfig config,
            Map<PathfinderMob, RetoldTerritoryMobState> mobStates,
            long gameTime
    ) {
        List<LivingEntity> candidates = RetoldAiScanCache.nearby(
                level,
                mob,
                LivingEntity.class,
                RetoldTerritoryConstants.NOTICE_MOB_RADIUS_BLOCKS,
                gameTime,
                RetoldTerritoryConstants.WARNING_NEARBY_INTRUDER_SCAN_CACHE_TICKS
        );

        LivingEntity bestTarget = null;
        double bestScore = Double.MAX_VALUE;

        for (LivingEntity candidate : candidates) {
            if (!isSelectableTerritoryAttackTarget(level, mob, state, config, candidate, gameTime)) {
                continue;
            }

            int focusCount = RetoldWarningMovement.countNearbyFactionMobsFocusedOn(
                    level,
                    mob,
                    config,
                    state,
                    candidate,
                    mobStates,
                    gameTime
            );

            double distanceScore = mob.distanceToSqr(candidate) * 0.01D;
            double tieBreaker = getStableTieBreaker(mob, candidate);

            double score = focusCount * 900.0D + distanceScore + tieBreaker;

            if (candidate == mob.getLastHurtByMob()) {
                score -= 500.0D;
            }

            if (candidate == state.attackTarget) {
                score -= 120.0D;
            }

            if (score < bestScore) {
                bestScore = score;
                bestTarget = candidate;
            }
        }

        return bestTarget;
    }

    public static boolean shouldSwitchAttackTarget(
            ServerLevel level,
            PathfinderMob mob,
            RetoldTerritoryMobState state,
            RetoldTerritoryConfig config,
            LivingEntity currentTarget,
            LivingEntity betterTarget,
            Map<PathfinderMob, RetoldTerritoryMobState> mobStates,
            long gameTime
    ) {
        if (betterTarget == mob.getLastHurtByMob()) {
            return true;
        }

        if (currentTarget == null || !currentTarget.isAlive()) {
            return true;
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

        int betterFocus = RetoldWarningMovement.countNearbyFactionMobsFocusedOn(
                level,
                mob,
                config,
                state,
                betterTarget,
                mobStates,
                gameTime
        );

        if (currentFocus > betterFocus + 1) {
            return true;
        }

        double currentDistance = mob.distanceToSqr(currentTarget);
        double betterDistance = mob.distanceToSqr(betterTarget);

        return betterDistance + 8.0D * 8.0D < currentDistance;
    }

    public static boolean isSelectableTerritoryAttackTarget(
            ServerLevel level,
            PathfinderMob mob,
            RetoldTerritoryMobState state,
            RetoldTerritoryConfig config,
            LivingEntity target,
            long gameTime
    ) {
        if (!isPossibleIntruder(level, mob, target, config, gameTime)) {
            return false;
        }

        if (!isValidAttackTarget(mob, target)) {
            return false;
        }

        return canAttackByTerritoryReputation(state, target);
    }

    public static boolean isValidWarningTarget(
            ServerLevel level,
            PathfinderMob mob,
            LivingEntity target,
            RetoldTerritoryConfig config,
            long gameTime
    ) {
        return target != null
                && RetoldAiTargets.isAliveInSameLevel(mob, target)
                && isPossibleIntruder(level, mob, target, config, gameTime);
    }

    public static boolean isValidAttackTarget(PathfinderMob mob, LivingEntity target) {
        return RetoldAiTargets.isValidAssignmentTarget(mob, target)
                && mob.distanceToSqr(target) <= RetoldTerritoryConstants.ATTACK_TARGET_RELEASE_DISTANCE_SQUARED;
    }

    public static boolean isPossibleIntruder(
            ServerLevel level,
            PathfinderMob mob,
            LivingEntity intruder,
            RetoldTerritoryConfig config,
            long gameTime
    ) {
        if (intruder == mob) {
            return false;
        }

        if (!RetoldAiTargets.isAliveInSameLevel(mob, intruder)) {
            return false;
        }

        if (mob.distanceToSqr(intruder) > RetoldTerritoryConstants.NOTICE_MOB_RADIUS_BLOCKS * RetoldTerritoryConstants.NOTICE_MOB_RADIUS_BLOCKS) {
            return false;
        }

        if (RetoldAiTargets.isInvalidPlayerTarget(intruder)) {
            return false;
        }

        RetoldFaction intruderFaction = RetoldFactionMembers.getFaction(intruder);

        if (intruderFaction == null) {
            return false;
        }

        return canTriggerTerritoryWarning(config.faction, intruderFaction);
    }

    public static boolean canAttackByTerritoryReputation(
            RetoldTerritoryMobState state,
            LivingEntity target
    ) {
        if (!isReputationGatedIntruder(target)) {
            return true;
        }

        if (state.territoryContext == null || target == null) {
            return false;
        }

        return RetoldTerritoryReputation.shouldAttack(
                state.territoryContext,
                target
        );
    }

    public static boolean isReputationGatedIntruder(LivingEntity target) {
        if (target instanceof ServerPlayer) {
            return true;
        }

        return RetoldFactionMembers.isPlayer(target);
    }

    private static boolean canTriggerTerritoryWarning(
            RetoldFaction territoryFaction,
            RetoldFaction intruderFaction
    ) {
        if (territoryFaction == null || intruderFaction == null) {
            return false;
        }

        if (territoryFaction == intruderFaction) {
            return false;
        }

        if (intruderFaction == RetoldFaction.PLAYER) {
            return territoryFaction == RetoldFaction.NETHER_REMNANTS
                    || territoryFaction == RetoldFaction.ILLAGERS;
        }

        return RetoldFactionRelations.areEnemyFactions(territoryFaction, intruderFaction);
    }

    private static boolean canSeeTarget(PathfinderMob mob, LivingEntity target) {
        return RetoldAiTargets.isVisibleTo(mob, target);
    }

    private static double getStableTieBreaker(Entity first, Entity second) {
        int value = Math.floorMod(first.getId() * 7349 + second.getId() * 9151, 1000);
        return value / 100000.0D;
    }
}
