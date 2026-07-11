package cz.xefensor.retold.behavior;

import cz.xefensor.retold.combat.RetoldTargetSource;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.AABB;

import java.util.List;

final class RetoldWolfDenDefense {
    private static final String REASON_RETURN_HOME = "return_home";
    private static final String REASON_DEN_IDLE = "den_idle";
    private static final String REASON_DEN_DEFENSE = "den_defense";

    private static final int DEN_DEFENSE_CONTROL_TICKS = 20 * 4;
    private static final int DEN_DEFENSE_PRIORITY = 86;

    private static final double WOLF_DEN_DEFENSE_RADIUS_BLOCKS = 20.0D;
    private static final double WOLF_DEN_DEFENSE_RADIUS_SQUARED =
            WOLF_DEN_DEFENSE_RADIUS_BLOCKS * WOLF_DEN_DEFENSE_RADIUS_BLOCKS;

    private static final double WOLF_DEN_DEFENSE_JOIN_RADIUS_BLOCKS = 28.0D;
    private static final double WOLF_DEN_DEFENSE_JOIN_RADIUS_SQUARED =
            WOLF_DEN_DEFENSE_JOIN_RADIUS_BLOCKS * WOLF_DEN_DEFENSE_JOIN_RADIUS_BLOCKS;

    private static final double WOLF_DEN_DEFENSE_SPEED = 1.10D;

    private RetoldWolfDenDefense() {
    }

    static boolean tryDefendHome(
            ServerLevel level,
            PathfinderMob mob,
            RetoldAnimalHomeMemory home,
            RetoldAiControlMode mode,
            RetoldAiControlOwner owner,
            long gameTime
    ) {
        if (home.type() != RetoldAnimalHomeType.WOLF_DEN) {
            return false;
        }

        if (!canStartDenDefense(mob, mode, owner)) {
            return false;
        }

        if (mob.blockPosition().distSqr(home.pos()) > WOLF_DEN_DEFENSE_JOIN_RADIUS_SQUARED) {
            return false;
        }

        LivingEntity enemy = findDenEnemy(
                level,
                mob,
                home.pos()
        );

        if (enemy == null) {
            return false;
        }

        if (!RetoldBehaviorCombat.claimAttackControl(
                mob,
                RetoldAiControlOwner.COMBAT,
                DEN_DEFENSE_PRIORITY,
                REASON_DEN_DEFENSE,
                gameTime,
                DEN_DEFENSE_CONTROL_TICKS
        )) {
            return false;
        }

        RetoldBehaviorCombat.applyAttackTarget(
                mob,
                enemy,
                RetoldTargetSource.FACTION_COMBAT
        );

        mob.getLookControl().setLookAt(
                enemy,
                30.0F,
                30.0F
        );

        RetoldAiControl.withNavigationBypass(() -> {
            mob.getNavigation().moveTo(
                    enemy,
                    WOLF_DEN_DEFENSE_SPEED
            );
        });

        return true;
    }

    private static boolean canStartDenDefense(
            PathfinderMob mob,
            RetoldAiControlMode mode,
            RetoldAiControlOwner owner
    ) {
        if (RetoldBehaviorCoordinator.hasLiveTarget(mob)) {
            return false;
        }

        if (mode == RetoldAiControlMode.NONE) {
            return true;
        }

        if (mode != RetoldAiControlMode.REGROUP) {
            return false;
        }

        if (owner == RetoldAiControlOwner.SYSTEM) {
            return true;
        }

        return RetoldAiControl.isControlledAsByWithAnyReason(
                mob,
                mode,
                RetoldAiControlOwner.REGROUP,
                REASON_DEN_IDLE,
                REASON_RETURN_HOME
        );
    }

    private static LivingEntity findDenEnemy(
            ServerLevel level,
            PathfinderMob wolf,
            BlockPos homePos
    ) {
        AABB area = new AABB(
                homePos
        ).inflate(WOLF_DEN_DEFENSE_RADIUS_BLOCKS);

        List<LivingEntity> candidates = level.getEntitiesOfClass(
                LivingEntity.class,
                area,
                candidate -> isValidDenEnemy(
                        wolf,
                        homePos,
                        candidate
                )
        );

        LivingEntity bestEnemy = null;
        double bestScore = Double.MAX_VALUE;

        for (LivingEntity candidate : candidates) {
            double distanceFromHome = candidate.blockPosition().distSqr(homePos);

            if (distanceFromHome > WOLF_DEN_DEFENSE_RADIUS_SQUARED) {
                continue;
            }

            double score = distanceFromHome;

            if (wolf.hasLineOfSight(candidate)) {
                score -= 12.0D;
            }

            if (score < bestScore) {
                bestScore = score;
                bestEnemy = candidate;
            }
        }

        return bestEnemy;
    }

    private static boolean isValidDenEnemy(
            PathfinderMob wolf,
            BlockPos homePos,
            LivingEntity candidate
    ) {
        if (wolf == null || homePos == null || candidate == null) {
            return false;
        }

        if (wolf == candidate) {
            return false;
        }

        if (!RetoldBehaviorCoordinator.isAliveInSameLevel(wolf, candidate)) {
            return false;
        }

        if (RetoldBehaviorCoordinator.isInvalidPlayerTarget(candidate)) {
            return false;
        }

        if (candidate.blockPosition().distSqr(homePos) > WOLF_DEN_DEFENSE_RADIUS_SQUARED) {
            return false;
        }

        if (!RetoldMobRules.isWolfEnemyButNotFood(candidate)) {
            return false;
        }

        return wolf.hasLineOfSight(candidate)
                || wolf.distanceToSqr(candidate) <= 36.0D
                || candidate.blockPosition().distSqr(homePos) <= 64.0D;
    }
}
