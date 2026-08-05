package cz.xefensor.retold.behavior.species;

import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.control.RetoldAiControlMode;
import cz.xefensor.retold.behavior.control.RetoldAiControlOwner;
import cz.xefensor.retold.behavior.control.RetoldAiPriorities;
import cz.xefensor.retold.behavior.performance.RetoldAiScanCache;
import cz.xefensor.retold.behavior.performance.RetoldAiSightCache;
import cz.xefensor.retold.behavior.core.RetoldBehaviorCoordinator;
import cz.xefensor.retold.behavior.core.RetoldBehaviorMovement;
import cz.xefensor.retold.behavior.core.RetoldBehaviorTargets;
import cz.xefensor.retold.behavior.core.RetoldBehaviorTiming;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.animal.polarbear.PolarBear;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class RetoldNeutralWildlifeEvents {
    private static final int THINK_INTERVAL_TICKS = 10;
    private static final int NEUTRAL_SCAN_CACHE_TICKS = 6;
    private static final int NEUTRAL_PATH_INTERVAL_TICKS = 6;
    private static final int DEFENSE_CONTROL_TICKS = 20 * 4;
    private static final int DEFENSE_PRIORITY = RetoldAiPriorities.DEFENSE;
    private static final int CUB_WARNING_DURATION_TICKS = 40;

    private static final double CUB_SCAN_RADIUS_BLOCKS = 16.0D;
    private static final double CUB_SCAN_RADIUS_SQUARED =
            CUB_SCAN_RADIUS_BLOCKS * CUB_SCAN_RADIUS_BLOCKS;

    private static final double CUB_THREAT_RADIUS_BLOCKS = 10.0D;
    private static final double CUB_THREAT_RADIUS_SQUARED =
            CUB_THREAT_RADIUS_BLOCKS * CUB_THREAT_RADIUS_BLOCKS;

    private static final double DEFENSE_KEEP_RADIUS_BLOCKS = 34.0D;
    private static final double DEFENSE_KEEP_RADIUS_SQUARED =
            DEFENSE_KEEP_RADIUS_BLOCKS * DEFENSE_KEEP_RADIUS_BLOCKS;

    private static final double POLAR_BEAR_DEFENSE_SPEED = 1.18D;

    private static final Map<PathfinderMob, CubWarningState> CUB_WARNINGS =
            new WeakHashMap<>();

    private RetoldNeutralWildlifeEvents() {
    }

    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof PathfinderMob mob)) {
            return;
        }

        if (!(mob.level() instanceof ServerLevel level)) {
            return;
        }

        if (!RetoldMobRules.isProtectiveNeutral(mob)) {
            return;
        }

        long gameTime = level.getGameTime();

        if (!RetoldBehaviorTiming.shouldThink(
                mob,
                gameTime,
                THINK_INTERVAL_TICKS
        )) {
            return;
        }

        tickProtectiveNeutral(
                level,
                mob,
                gameTime
        );
    }

    static void tickProtectiveNeutral(
            ServerLevel level,
            PathfinderMob protector,
            long gameTime
    ) {
        if (!isAdultPolarBear(protector)) {
            clearWarningState(protector, true);
            return;
        }

        LivingEntity target = protector.getTarget();

        if (RetoldAiControl.isControlledAsBy(
                protector,
                RetoldAiControlMode.ATTACK,
                RetoldAiControlOwner.NEUTRAL_WILDLIFE
        ) && isValidDefenseTarget(protector, target) && hasNearbyCub(level, protector)) {
            clearWarningState(protector, false);
            continueDefense(
                    protector,
                    target,
                    gameTime
            );
            return;
        }

        LivingEntity threat = findBestCubThreat(
                level,
                protector,
                gameTime
        );

        if (threat == null) {
            clearWarningState(protector, true);

            if (RetoldAiControl.isControlledBy(protector, RetoldAiControlOwner.NEUTRAL_WILDLIFE)) {
                stopDefense(protector);
            }
            return;
        }

        if (isImmediateCubThreat(level, protector, threat, gameTime)) {
            clearWarningState(protector, false);
            beginDefense(
                    protector,
                    threat,
                    gameTime
            );
            return;
        }

        CubWarningState warningState = CUB_WARNINGS.get(protector);

        if (warningState == null || warningState.target() != threat) {
            beginWarning(
                    protector,
                    threat,
                    gameTime
            );
            return;
        }

        if (gameTime - warningState.startedAt() < CUB_WARNING_DURATION_TICKS) {
            continueWarning(
                    protector,
                    threat,
                    gameTime
            );
            return;
        }

        clearWarningState(protector, false);
        beginDefense(
                protector,
                threat,
                gameTime
        );
    }

    private static void beginWarning(
            PathfinderMob protector,
            LivingEntity target,
            long gameTime
    ) {
        if (!RetoldAiControl.tryClaim(
                protector,
                RetoldAiControlMode.REGROUP,
                RetoldAiControlOwner.NEUTRAL_WILDLIFE,
                DEFENSE_PRIORITY,
                "warn_cub_intruder",
                gameTime,
                DEFENSE_CONTROL_TICKS
        )) {
            return;
        }

        RetoldBehaviorTargets.setTargetAndAggression(protector, null, false);
        protector.getNavigation().stop();
        CUB_WARNINGS.put(protector, new CubWarningState(target, gameTime));
        showWarning(protector, target, true);
    }

    private static void continueWarning(
            PathfinderMob protector,
            LivingEntity target,
            long gameTime
    ) {
        if (!RetoldAiControl.refreshIfOwnedBy(
                protector,
                RetoldAiControlMode.REGROUP,
                RetoldAiControlOwner.NEUTRAL_WILDLIFE,
                gameTime,
                DEFENSE_CONTROL_TICKS
        )) {
            clearWarningState(protector, false);
            return;
        }

        RetoldBehaviorTargets.setTargetAndAggression(protector, null, false);
        protector.getNavigation().stop();
        showWarning(protector, target, false);
    }

    private static void showWarning(
            PathfinderMob protector,
            LivingEntity target,
            boolean playSound
    ) {
        protector.getLookControl().setLookAt(target, 30.0F, 30.0F);

        if (protector instanceof PolarBear polarBear) {
            polarBear.setStanding(true);

            if (playSound) {
                polarBear.playSound(SoundEvents.POLAR_BEAR_WARNING, 1.0F, 1.0F);
            }
        }
    }

    private static void clearWarningState(
            PathfinderMob protector,
            boolean clearControl
    ) {
        CUB_WARNINGS.remove(protector);

        if (protector instanceof PolarBear polarBear) {
            polarBear.setStanding(false);
        }

        if (clearControl && RetoldAiControl.isControlledAsBy(
                protector,
                RetoldAiControlMode.REGROUP,
                RetoldAiControlOwner.NEUTRAL_WILDLIFE
        )) {
            RetoldAiControl.clearIfOwnedBy(
                    protector,
                    RetoldAiControlOwner.NEUTRAL_WILDLIFE
            );
        }
    }

    private static boolean isImmediateCubThreat(
            ServerLevel level,
            PathfinderMob protector,
            LivingEntity threat,
            long gameTime
    ) {
        if (threat == protector.getLastHurtByMob()) {
            return true;
        }

        for (PathfinderMob cub : RetoldAiScanCache.nearby(
                level,
                protector,
                PathfinderMob.class,
                CUB_SCAN_RADIUS_BLOCKS,
                gameTime,
                NEUTRAL_SCAN_CACHE_TICKS
        )) {
            if (!isNearbyPolarBearCub(protector, cub)) {
                continue;
            }

            if (threat == cub.getLastHurtByMob()) {
                return true;
            }

            if (threat instanceof PathfinderMob mob && mob.getTarget() == cub) {
                return true;
            }
        }

        return false;
    }

    private static LivingEntity findBestCubThreat(
            ServerLevel level,
            PathfinderMob protector,
            long gameTime
    ) {
        List<PathfinderMob> cubs = RetoldAiScanCache.nearby(
                level,
                protector,
                PathfinderMob.class,
                CUB_SCAN_RADIUS_BLOCKS,
                gameTime,
                NEUTRAL_SCAN_CACHE_TICKS
        );

        LivingEntity bestThreat = null;
        double bestScore = Double.MAX_VALUE;

        LivingEntity protectorAttacker = protector.getLastHurtByMob();

        for (PathfinderMob cub : cubs) {
            if (!isNearbyPolarBearCub(protector, cub)) {
                continue;
            }

            LivingEntity cubAttacker = cub.getLastHurtByMob();

            bestThreat = chooseBetterThreat(
                    protector,
                    cub,
                    bestThreat,
                    cubAttacker,
                    bestScore
            );

            if (bestThreat == cubAttacker) {
                bestScore = threatScore(protector, cub, cubAttacker);
            }

            bestThreat = chooseBetterThreat(
                    protector,
                    cub,
                    bestThreat,
                    protectorAttacker,
                    bestScore
            );

            if (bestThreat == protectorAttacker) {
                bestScore = threatScore(protector, cub, protectorAttacker);
            }

            for (LivingEntity candidate : RetoldAiScanCache.nearby(
                    level,
                    cub,
                    LivingEntity.class,
                    CUB_THREAT_RADIUS_BLOCKS,
                    gameTime,
                    NEUTRAL_SCAN_CACHE_TICKS
            )) {
                if (!isValidCubProximityThreat(protector, cub, candidate)) {
                    continue;
                }

                double score = threatScore(
                        protector,
                        cub,
                        candidate
                );

                if (score < bestScore) {
                    bestScore = score;
                    bestThreat = candidate;
                }
            }
        }

        return bestThreat;
    }

    private static LivingEntity chooseBetterThreat(
            PathfinderMob protector,
            PathfinderMob cub,
            LivingEntity current,
            LivingEntity candidate,
            double currentScore
    ) {
        if (!isValidDefenseTarget(protector, candidate)) {
            return current;
        }

        boolean actualAttacker = candidate == cub.getLastHurtByMob()
                || candidate == protector.getLastHurtByMob();
        double allowedDistanceSquared = actualAttacker
                ? DEFENSE_KEEP_RADIUS_SQUARED
                : CUB_THREAT_RADIUS_SQUARED;

        if (cub.distanceToSqr(candidate) > allowedDistanceSquared) {
            return current;
        }

        double score = threatScore(
                protector,
                cub,
                candidate
        );

        if (score >= currentScore) {
            return current;
        }

        return candidate;
    }

    private static boolean isValidCubProximityThreat(
            PathfinderMob protector,
            PathfinderMob cub,
            LivingEntity candidate
    ) {
        if (!isValidDefenseTarget(protector, candidate)) {
            return false;
        }

        if (cub.distanceToSqr(candidate) > CUB_THREAT_RADIUS_SQUARED) {
            return false;
        }

        if (candidate instanceof Player) {
            return RetoldAiSightCache.canSee(protector, candidate, protector.level().getGameTime())
                    || RetoldAiSightCache.canSee(cub, candidate, cub.level().getGameTime())
                    || cub.distanceToSqr(candidate) <= 16.0D;
        }

        if (candidate instanceof PathfinderMob mob && mob.getTarget() == cub) {
            return true;
        }

        return candidate == cub.getLastHurtByMob()
                || candidate == protector.getLastHurtByMob();
    }

    private static boolean hasNearbyCub(
            ServerLevel level,
            PathfinderMob protector
    ) {
        for (PathfinderMob candidate : RetoldAiScanCache.nearby(
                level,
                protector,
                PathfinderMob.class,
                CUB_SCAN_RADIUS_BLOCKS,
                level.getGameTime(),
                NEUTRAL_SCAN_CACHE_TICKS
        )) {
            if (isNearbyPolarBearCub(protector, candidate)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isNearbyPolarBearCub(
            PathfinderMob protector,
            PathfinderMob candidate
    ) {
        if (candidate == null || candidate == protector) {
            return false;
        }

        if (!isPolarBear(candidate)) {
            return false;
        }

        if (!(candidate instanceof AgeableMob ageableMob) || !ageableMob.isBaby()) {
            return false;
        }

        if (!RetoldBehaviorCoordinator.isAliveInSameLevel(protector, candidate)) {
            return false;
        }

        return protector.distanceToSqr(candidate) <= CUB_SCAN_RADIUS_SQUARED;
    }

    private static void beginDefense(
            PathfinderMob protector,
            LivingEntity target,
            long gameTime
    ) {
        clearWarningState(protector, false);

        if (!RetoldAiControl.tryClaim(
                protector,
                RetoldAiControlMode.ATTACK,
                RetoldAiControlOwner.NEUTRAL_WILDLIFE,
                DEFENSE_PRIORITY,
                "protect_cub",
                gameTime,
                DEFENSE_CONTROL_TICKS
        )) {
            return;
        }

        moveToDefenseTarget(
                protector,
                target,
                gameTime
        );
    }

    private static void continueDefense(
            PathfinderMob protector,
            LivingEntity target,
            long gameTime
    ) {
        if (!RetoldAiControl.refreshIfOwnedBy(
                protector,
                RetoldAiControlMode.ATTACK,
                RetoldAiControlOwner.NEUTRAL_WILDLIFE,
                gameTime,
                DEFENSE_CONTROL_TICKS
        )) {
            return;
        }

        moveToDefenseTarget(
                protector,
                target,
                gameTime
        );
    }

    private static void moveToDefenseTarget(
            PathfinderMob protector,
            LivingEntity target,
            long gameTime
    ) {
        if (!RetoldBehaviorTargets.setAttackTargetOrClearOwner(
                protector,
                target,
                RetoldAiControlOwner.NEUTRAL_WILDLIFE
        )) {
            return;
        }

        protector.getLookControl().setLookAt(
                target,
                30.0F,
                30.0F
        );

        RetoldBehaviorMovement.throttledMoveTo(
                protector,
                target,
                POLAR_BEAR_DEFENSE_SPEED,
                gameTime,
                NEUTRAL_PATH_INTERVAL_TICKS,
                2.0D * 2.0D
        );
    }

    private static void stopDefense(PathfinderMob protector) {
        clearWarningState(protector, false);
        RetoldBehaviorTargets.setTargetAndAggression(protector, null, false);

        protector.getNavigation().stop();
        RetoldAiControl.clearIfOwnedBy(
                protector,
                RetoldAiControlOwner.NEUTRAL_WILDLIFE
        );
    }

    private static boolean isValidDefenseTarget(
            PathfinderMob protector,
            LivingEntity target
    ) {
        if (target == null || target == protector) {
            return false;
        }

        if (!RetoldBehaviorCoordinator.isValidAssignmentTarget(protector, target)) {
            return false;
        }

        if (isPolarBear(target)) {
            return false;
        }

        return protector.distanceToSqr(target) <= DEFENSE_KEEP_RADIUS_SQUARED;
    }

    private static double threatScore(
            PathfinderMob protector,
            PathfinderMob cub,
            LivingEntity candidate
    ) {
        if (candidate == null) {
            return Double.MAX_VALUE;
        }

        double score = cub.distanceToSqr(candidate) + protector.distanceToSqr(candidate) * 0.35D;

        if (candidate == cub.getLastHurtByMob()) {
            score -= 80.0D;
        }

        if (candidate == protector.getLastHurtByMob()) {
            score -= 40.0D;
        }

        if (candidate instanceof PathfinderMob mob && mob.getTarget() == cub) {
            score -= 60.0D;
        }

        if (RetoldAiSightCache.canSee(protector, candidate, protector.level().getGameTime())) {
            score -= 10.0D;
        }

        return score;
    }

    private static boolean isAdultPolarBear(PathfinderMob mob) {
        return isPolarBear(mob)
                && (!(mob instanceof AgeableMob ageableMob) || !ageableMob.isBaby());
    }

    private static boolean isPolarBear(LivingEntity entity) {
        return entity != null
                && RetoldMobRules.isEntityPath(
                entity,
                "polar_bear"
        );
    }

    private record CubWarningState(
            LivingEntity target,
            long startedAt
    ) {
    }
}
