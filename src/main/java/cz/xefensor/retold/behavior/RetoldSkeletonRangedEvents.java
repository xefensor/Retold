package cz.xefensor.retold.behavior;

import cz.xefensor.retold.combat.RetoldTargetSource;
import cz.xefensor.retold.faction.RetoldFaction;
import cz.xefensor.retold.faction.RetoldFactionMembers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.List;

public final class RetoldSkeletonRangedEvents {
    private static final int THINK_INTERVAL_TICKS = 12;
    private static final int RANGED_CONTROL_TICKS = 20 * 4;
    private static final int RANGED_PRIORITY = RetoldAiPriorities.below(RetoldAiPriorities.FEED, 1);

    private static final double SHARE_RADIUS_BLOCKS = 24.0D;
    private static final double SHARE_RADIUS_SQUARED =
            SHARE_RADIUS_BLOCKS * SHARE_RADIUS_BLOCKS;

    private static final double NOTICE_RADIUS_BLOCKS = 22.0D;
    private static final double NOTICE_RADIUS_SQUARED =
            NOTICE_RADIUS_BLOCKS * NOTICE_RADIUS_BLOCKS;

    private static final double TOO_CLOSE_BLOCKS = 7.0D;
    private static final double TOO_CLOSE_SQUARED =
            TOO_CLOSE_BLOCKS * TOO_CLOSE_BLOCKS;

    private static final double IDEAL_RANGE_BLOCKS = 14.0D;
    private static final double IDEAL_RANGE_SQUARED =
            IDEAL_RANGE_BLOCKS * IDEAL_RANGE_BLOCKS;

    private static final double TOO_FAR_BLOCKS = 26.0D;
    private static final double TOO_FAR_SQUARED =
            TOO_FAR_BLOCKS * TOO_FAR_BLOCKS;

    private static final double RANGED_MOVE_SPEED = 0.92D;
    private static final double BACKPEDAL_BLOCKS = 8.0D;

    private RetoldSkeletonRangedEvents() {
    }

    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof PathfinderMob skeleton)) {
            return;
        }

        if (!(skeleton.level() instanceof ServerLevel level)) {
            return;
        }

        if (!isRangedUndead(skeleton)) {
            return;
        }

        long gameTime = level.getGameTime();

        if (!RetoldBehaviorTiming.shouldThink(
                skeleton,
                gameTime,
                THINK_INTERVAL_TICKS
        )) {
            return;
        }

        LivingEntity target = skeleton.getTarget();

        if (isValidRangedTarget(skeleton, target)) {
            spreadTargetToNearbySkeletons(
                    level,
                    skeleton,
                    target,
                    gameTime
            );
            maintainRange(
                    skeleton,
                    target,
                    gameTime
            );
            return;
        }

        if (!canAdoptTarget(skeleton)) {
            clearRangedControlIfOwned(skeleton);
            return;
        }

        LivingEntity sharedTarget = findSharedSkeletonTarget(
                level,
                skeleton
        );

        if (sharedTarget == null) {
            sharedTarget = findVisibleEnemy(
                    level,
                    skeleton
            );
        }

        if (sharedTarget == null) {
            clearRangedControlIfOwned(skeleton);
            return;
        }

        adoptTarget(
                skeleton,
                sharedTarget,
                gameTime
        );
    }

    private static LivingEntity findSharedSkeletonTarget(
            ServerLevel level,
            PathfinderMob skeleton
    ) {
        AABB area = skeleton.getBoundingBox().inflate(SHARE_RADIUS_BLOCKS);
        List<PathfinderMob> sources = level.getEntitiesOfClass(
                PathfinderMob.class,
                area,
                source -> isValidRangedSource(
                        skeleton,
                        source
                )
        );

        LivingEntity bestTarget = null;
        double bestScore = Double.MAX_VALUE;

        for (PathfinderMob source : sources) {
            LivingEntity target = source.getTarget();

            if (!isValidRangedTarget(skeleton, target)) {
                continue;
            }

            double score = skeleton.distanceToSqr(source);

            if (source.hasLineOfSight(target)) {
                score -= 20.0D;
            }

            if (skeleton.hasLineOfSight(source)) {
                score -= 8.0D;
            }

            if (score < bestScore) {
                bestScore = score;
                bestTarget = target;
            }
        }

        return bestTarget;
    }

    private static LivingEntity findVisibleEnemy(
            ServerLevel level,
            PathfinderMob skeleton
    ) {
        AABB area = skeleton.getBoundingBox().inflate(NOTICE_RADIUS_BLOCKS);
        List<LivingEntity> candidates = level.getEntitiesOfClass(
                LivingEntity.class,
                area,
                candidate -> isValidVisibleEnemy(
                        skeleton,
                        candidate
                )
        );

        LivingEntity bestTarget = null;
        double bestScore = Double.MAX_VALUE;

        for (LivingEntity candidate : candidates) {
            double distanceSquared = skeleton.distanceToSqr(candidate);

            if (distanceSquared > NOTICE_RADIUS_SQUARED) {
                continue;
            }

            double score = Math.abs(distanceSquared - IDEAL_RANGE_SQUARED);

            if (candidate instanceof Player) {
                score -= 10.0D;
            }

            if (score < bestScore) {
                bestScore = score;
                bestTarget = candidate;
            }
        }

        return bestTarget;
    }

    private static void spreadTargetToNearbySkeletons(
            ServerLevel level,
            PathfinderMob source,
            LivingEntity target,
            long gameTime
    ) {
        AABB area = source.getBoundingBox().inflate(SHARE_RADIUS_BLOCKS);

        for (PathfinderMob ally : level.getEntitiesOfClass(
                PathfinderMob.class,
                area,
                candidate -> isValidRangedRecruit(
                        source,
                        candidate
                )
        )) {
            adoptTarget(
                    ally,
                    target,
                    gameTime
            );
        }
    }

    private static void adoptTarget(
            PathfinderMob skeleton,
            LivingEntity target,
            long gameTime
    ) {
        if (!isValidRangedTarget(skeleton, target)) {
            return;
        }

        if (!RetoldBehaviorCombat.applyAttackTarget(
                skeleton,
                target,
                RetoldTargetSource.FACTION_ASSIST
        )) {
            return;
        }

        maintainRange(
                skeleton,
                target,
                gameTime
        );
    }

    private static void maintainRange(
            PathfinderMob skeleton,
            LivingEntity target,
            long gameTime
    ) {
        if (!RetoldBehaviorCombat.claimAttackControl(
                skeleton,
                RetoldAiControlOwner.UNDEAD_RANGED,
                RANGED_PRIORITY,
                "skeleton_ranged_line",
                gameTime,
                RANGED_CONTROL_TICKS
        )) {
            return;
        }

        skeleton.getLookControl().setLookAt(
                target,
                30.0F,
                30.0F
        );

        double distanceSquared = skeleton.distanceToSqr(target);

        if (distanceSquared <= TOO_CLOSE_SQUARED) {
            moveAwayFromTarget(
                    skeleton,
                    target
            );
            return;
        }

        if (distanceSquared >= TOO_FAR_SQUARED || !skeleton.hasLineOfSight(target)) {
            RetoldAiControl.withNavigationBypass(() -> {
                skeleton.getNavigation().moveTo(
                        target,
                        RANGED_MOVE_SPEED
                );
            });
            return;
        }

        if (distanceSquared <= IDEAL_RANGE_SQUARED && skeleton.hasLineOfSight(target)) {
            skeleton.getNavigation().stop();
        }
    }

    private static void moveAwayFromTarget(
            PathfinderMob skeleton,
            LivingEntity target
    ) {
        Vec3 away = skeleton.position().subtract(target.position());

        if (away.lengthSqr() < 0.001D) {
            away = new Vec3(
                    skeleton.getRandom().nextDouble() - 0.5D,
                    0.0D,
                    skeleton.getRandom().nextDouble() - 0.5D
            );
        }

        Vec3 destination = skeleton.position().add(
                away.normalize().scale(BACKPEDAL_BLOCKS)
        );

        RetoldAiControl.withNavigationBypass(() -> {
            skeleton.getNavigation().moveTo(
                    destination.x,
                    skeleton.getY(),
                    destination.z,
                    RANGED_MOVE_SPEED
            );
        });
    }

    private static boolean isValidRangedSource(
            PathfinderMob skeleton,
            PathfinderMob source
    ) {
        if (source == null || source == skeleton) {
            return false;
        }

        if (!isRangedUndead(source)) {
            return false;
        }

        if (!RetoldBehaviorCoordinator.isAliveInSameLevel(skeleton, source)) {
            return false;
        }

        if (skeleton.distanceToSqr(source) > SHARE_RADIUS_SQUARED) {
            return false;
        }

        return isValidRangedTarget(
                skeleton,
                source.getTarget()
        );
    }

    private static boolean isValidRangedRecruit(
            PathfinderMob source,
            PathfinderMob recruit
    ) {
        if (recruit == null || recruit == source) {
            return false;
        }

        if (!isRangedUndead(recruit)) {
            return false;
        }

        if (!RetoldBehaviorCoordinator.isAliveInSameLevel(source, recruit)) {
            return false;
        }

        if (source.distanceToSqr(recruit) > SHARE_RADIUS_SQUARED) {
            return false;
        }

        LivingEntity currentTarget = recruit.getTarget();

        return currentTarget == null
                || !currentTarget.isAlive()
                || currentTarget == source.getTarget();
    }

    private static boolean isValidVisibleEnemy(
            PathfinderMob skeleton,
            LivingEntity candidate
    ) {
        if (!isValidRangedTarget(skeleton, candidate)) {
            return false;
        }

        if (skeleton.distanceToSqr(candidate) > NOTICE_RADIUS_SQUARED) {
            return false;
        }

        return skeleton.hasLineOfSight(candidate);
    }

    private static boolean isValidRangedTarget(
            PathfinderMob skeleton,
            LivingEntity target
    ) {
        if (!RetoldBehaviorCombat.isValidEnemyTarget(
                skeleton,
                target,
                Double.MAX_VALUE,
                false
        )) {
            return false;
        }

        if (RetoldFactionMembers.isUndead(target)) {
            return false;
        }

        return true;
    }

    private static boolean canAdoptTarget(PathfinderMob skeleton) {
        return RetoldBehaviorCombat.canUseAttackControl(
                skeleton,
                RetoldAiControlOwner.UNDEAD_RANGED
        );
    }

    private static void clearRangedControlIfOwned(PathfinderMob skeleton) {
        RetoldBehaviorCombat.clearAttackControlIfOwned(
                skeleton,
                skeleton.getTarget(),
                RetoldAiControlOwner.UNDEAD_RANGED,
                RetoldTargetSource.FACTION_ASSIST
        );
    }

    private static boolean isRangedUndead(PathfinderMob mob) {
        String path = RetoldMobRules.getEntityTypePath(mob.getType());

        return path.equals("skeleton")
                || path.equals("stray")
                || path.equals("bogged");
    }
}
