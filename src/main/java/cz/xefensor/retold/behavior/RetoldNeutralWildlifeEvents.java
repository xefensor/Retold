package cz.xefensor.retold.behavior;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.List;

public final class RetoldNeutralWildlifeEvents {
    private static final int THINK_INTERVAL_TICKS = 10;
    private static final int DEFENSE_CONTROL_TICKS = 20 * 4;
    private static final int DEFENSE_PRIORITY = RetoldAiPriorities.DEFENSE;

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

        handleProtectiveNeutral(
                level,
                mob,
                gameTime
        );
    }

    private static void handleProtectiveNeutral(
            ServerLevel level,
            PathfinderMob protector,
            long gameTime
    ) {
        if (!isAdultPolarBear(protector)) {
            return;
        }

        LivingEntity target = protector.getTarget();

        if (isValidDefenseTarget(protector, target) && hasNearbyCub(level, protector)) {
            continueDefense(
                    protector,
                    target,
                    gameTime
            );
            return;
        }

        if (RetoldAiControl.isControlledBy(protector, RetoldAiControlOwner.NEUTRAL_WILDLIFE)) {
            stopDefense(protector);
        }

        LivingEntity threat = findBestCubThreat(
                level,
                protector
        );

        if (threat == null) {
            return;
        }

        beginDefense(
                protector,
                threat,
                gameTime
        );
    }

    private static LivingEntity findBestCubThreat(
            ServerLevel level,
            PathfinderMob protector
    ) {
        AABB cubArea = protector.getBoundingBox().inflate(CUB_SCAN_RADIUS_BLOCKS);

        List<PathfinderMob> cubs = level.getEntitiesOfClass(
                PathfinderMob.class,
                cubArea,
                candidate -> isNearbyPolarBearCub(
                        protector,
                        candidate
                )
        );

        LivingEntity bestThreat = null;
        double bestScore = Double.MAX_VALUE;

        LivingEntity protectorAttacker = protector.getLastHurtByMob();

        for (PathfinderMob cub : cubs) {
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

            for (LivingEntity candidate : level.getEntitiesOfClass(
                    LivingEntity.class,
                    cub.getBoundingBox().inflate(CUB_THREAT_RADIUS_BLOCKS),
                    candidate -> isValidCubProximityThreat(
                            protector,
                            cub,
                            candidate
                    )
            )) {
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

        if (cub.distanceToSqr(candidate) > CUB_THREAT_RADIUS_SQUARED) {
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
            return protector.hasLineOfSight(candidate)
                    || cub.hasLineOfSight(candidate)
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
        AABB area = protector.getBoundingBox().inflate(CUB_SCAN_RADIUS_BLOCKS);

        return !level.getEntitiesOfClass(
                PathfinderMob.class,
                area,
                candidate -> isNearbyPolarBearCub(
                        protector,
                        candidate
                )
        ).isEmpty();
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
                target
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
                target
        );
    }

    private static void moveToDefenseTarget(
            PathfinderMob protector,
            LivingEntity target
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

        RetoldAiControl.withNavigationBypass(() -> {
            protector.getNavigation().moveTo(
                    target,
                    POLAR_BEAR_DEFENSE_SPEED
            );
        });
    }

    private static void stopDefense(PathfinderMob protector) {
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

        if (protector.hasLineOfSight(candidate)) {
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
}
