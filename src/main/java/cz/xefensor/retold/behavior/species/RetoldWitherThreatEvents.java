package cz.xefensor.retold.behavior.species;

import cz.xefensor.retold.behavior.performance.RetoldAiScanCache;
import cz.xefensor.retold.behavior.performance.RetoldAiSightCache;
import cz.xefensor.retold.behavior.core.RetoldBehaviorTiming;
import cz.xefensor.retold.combat.RetoldAiTargets;
import cz.xefensor.retold.combat.RetoldCombatTargets;
import cz.xefensor.retold.combat.RetoldMobTargetPolicy;
import cz.xefensor.retold.combat.RetoldTargetSource;
import cz.xefensor.retold.faction.RetoldFactionRelations;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.List;

public final class RetoldWitherThreatEvents {
    private static final int THREAT_SCAN_CACHE_TICKS = 10;
    private static final double THREAT_SEARCH_RADIUS_BLOCKS = 40.0D;
    private static final double THREAT_SEARCH_RADIUS_SQUARED =
            THREAT_SEARCH_RADIUS_BLOCKS * THREAT_SEARCH_RADIUS_BLOCKS;

    private static final double CURRENT_TARGET_INERTIA = 144.0D;
    private static final double ACTIVE_THREAT_PRIORITY = 900.0D;
    private static final double RECENT_ATTACKER_PRIORITY = 1_600.0D;
    private static final int RECENT_ATTACK_TICKS = 20 * 5;

    private RetoldWitherThreatEvents() {
    }

    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof WitherBoss wither)) {
            return;
        }

        if (!(wither.level() instanceof ServerLevel level)) {
            return;
        }

        clearInvalidPrimaryTarget(wither);
        clearInvalidAlternativeTargets(level, wither);

        if (!RetoldBehaviorTiming.shouldThink(
                wither,
                level.getGameTime(),
                10
        )) {
            return;
        }

        tickThreatTargeting(level, wither);
    }

    static void clearInvalidPrimaryTarget(WitherBoss wither) {
        LivingEntity target = wither.getTarget();

        if (target == null || isPermittedHeadTarget(wither, target)) {
            return;
        }

        RetoldCombatTargets.clearTargetReferencesAndAggression(
                wither,
                target,
                false
        );
    }

    static void clearInvalidAlternativeTargets(
            ServerLevel level,
            WitherBoss wither
    ) {
        /*
         * Vanilla side heads store raw entity ids outside Mob#setTarget, so the primary-target
         * mixin cannot protect them. The heads acquire before they fire on a later update; this
         * post-tick check removes invalid dynamic faction targets during that intervening tick.
         */
        for (int headIndex = 1; headIndex <= 2; headIndex++) {
            int targetId = wither.getAlternativeTarget(headIndex);

            if (targetId <= 0) {
                continue;
            }

            Entity target = level.getEntity(targetId);

            if (!(target instanceof LivingEntity livingTarget)
                    || !isPermittedHeadTarget(wither, livingTarget)) {
                wither.setAlternativeTarget(headIndex, 0);
            }
        }
    }

    static void tickThreatTargeting(
            ServerLevel level,
            WitherBoss wither
    ) {
        if (!wither.isAlive()
                || wither.isRemoved()
                || wither.getInvulnerableTicks() > 0) {
            return;
        }

        LivingEntity bestThreat = findBestThreat(level, wither);

        if (bestThreat == null || wither.getTarget() == bestThreat) {
            return;
        }

        RetoldCombatTargets.applyAttackTarget(
                wither,
                bestThreat,
                RetoldTargetSource.FACTION_COMBAT
        );
    }

    static LivingEntity findBestThreat(
            ServerLevel level,
            WitherBoss wither
    ) {
        long gameTime = level.getGameTime();
        List<LivingEntity> candidates = RetoldAiScanCache.nearby(
                level,
                wither,
                LivingEntity.class,
                THREAT_SEARCH_RADIUS_BLOCKS,
                gameTime,
                THREAT_SCAN_CACHE_TICKS
        );

        LivingEntity bestThreat = null;
        double bestScore = Double.MAX_VALUE;

        for (LivingEntity candidate : candidates) {
            if (!isValidThreat(wither, candidate, gameTime)) {
                continue;
            }

            double score = threatScore(wither, candidate);

            if (score < bestScore) {
                bestThreat = candidate;
                bestScore = score;
            }
        }

        return bestThreat;
    }

    private static boolean isValidThreat(
            WitherBoss wither,
            LivingEntity candidate,
            long gameTime
    ) {
        if (candidate == wither
                || !RetoldAiTargets.isValidAssignmentTarget(wither, candidate)
                || wither.distanceToSqr(candidate) > THREAT_SEARCH_RADIUS_SQUARED
                || !RetoldFactionRelations.shouldAttack(wither, candidate)
                || !wither.canAttack(candidate)) {
            return false;
        }

        return RetoldAiSightCache.canSee(wither, candidate, gameTime);
    }

    private static boolean isPermittedHeadTarget(
            WitherBoss wither,
            LivingEntity target
    ) {
        return RetoldAiTargets.isValidAssignmentTarget(wither, target)
                && !RetoldMobTargetPolicy.shouldBlockDeliberateHostility(wither, target)
                && RetoldFactionRelations.shouldAttack(wither, target);
    }

    private static double threatScore(
            WitherBoss wither,
            LivingEntity candidate
    ) {
        double score = wither.distanceToSqr(candidate);

        if (candidate == wither.getTarget()) {
            score -= CURRENT_TARGET_INERTIA;
        }

        if (isActivelyThreatening(candidate, wither)) {
            score -= ACTIVE_THREAT_PRIORITY;
        }

        if (isRecentAttacker(candidate, wither)) {
            score -= RECENT_ATTACKER_PRIORITY;
        }

        return score;
    }

    private static boolean isActivelyThreatening(
            LivingEntity candidate,
            WitherBoss wither
    ) {
        if (!(candidate instanceof Mob mob)) {
            return false;
        }

        return mob.getTarget() == wither
                || RetoldAiTargets.getBrainAttackTargetSafely(mob) == wither;
    }

    private static boolean isRecentAttacker(
            LivingEntity candidate,
            WitherBoss wither
    ) {
        if (wither.getLastHurtByMob() == candidate) {
            return true;
        }

        return candidate.getLastHurtMob() == wither
                && candidate.tickCount - candidate.getLastHurtMobTimestamp()
                <= RECENT_ATTACK_TICKS;
    }
}
