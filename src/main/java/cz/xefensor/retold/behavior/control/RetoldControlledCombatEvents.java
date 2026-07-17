package cz.xefensor.retold.behavior.control;

import cz.xefensor.retold.behavior.performance.RetoldAiScanCache;
import cz.xefensor.retold.behavior.performance.RetoldAiSightCache;
import cz.xefensor.retold.behavior.core.RetoldBehaviorCoordinator;
import cz.xefensor.retold.behavior.core.RetoldBehaviorMovement;
import cz.xefensor.retold.behavior.core.RetoldBehaviorTargets;
import cz.xefensor.retold.behavior.core.RetoldBehaviorTiming;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.List;

public final class RetoldControlledCombatEvents {
    private static final int COMBAT_THINK_INTERVAL_TICKS = 10;
    private static final int COMBAT_SCAN_CACHE_TICKS = 5;
    private static final int COMBAT_PATH_INTERVAL_TICKS = 6;
    private static final int ATTACK_CONTROL_TICKS = 20 * 4;

    private static final double OWNER_THREAT_RADIUS_BLOCKS = 28.0D;
    private static final double OWNER_THREAT_RADIUS_SQUARED =
            OWNER_THREAT_RADIUS_BLOCKS * OWNER_THREAT_RADIUS_BLOCKS;

    private static final double WOLF_ENEMY_SEARCH_RADIUS_BLOCKS = 16.0D;
    private static final double WOLF_ENEMY_SEARCH_RADIUS_SQUARED =
            WOLF_ENEMY_SEARCH_RADIUS_BLOCKS * WOLF_ENEMY_SEARCH_RADIUS_BLOCKS;

    private static final double ATTACK_KEEP_RADIUS_BLOCKS = 36.0D;
    private static final double ATTACK_KEEP_RADIUS_SQUARED =
            ATTACK_KEEP_RADIUS_BLOCKS * ATTACK_KEEP_RADIUS_BLOCKS;

    private static final double OWNER_DEFENSE_SPEED = 1.12D;
    private static final double WOLF_SKELETON_ATTACK_SPEED = 1.05D;

    private RetoldControlledCombatEvents() {
    }

    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof PathfinderMob mob)) {
            return;
        }

        if (!(mob.level() instanceof ServerLevel level)) {
            return;
        }

        if (!RetoldMobRules.canUseOrdinaryPredatorSystems(mob)) {
            return;
        }

        long gameTime = level.getGameTime();

        if (!shouldThink(mob, gameTime)) {
            return;
        }

        if (RetoldAiControl.isControlledAs(mob, RetoldAiControlMode.ATTACK)) {
            continueAttack(
                    mob,
                    gameTime
            );
            return;
        }

        LivingEntity ownerThreat = findOwnerThreat(mob);

        if (ownerThreat != null) {
            beginAttack(
                    mob,
                    ownerThreat,
                    OWNER_DEFENSE_SPEED,
                    gameTime
            );
            return;
        }

        /*
         * Do not let autonomous enemy combat interrupt eating or hunting.
         * Owner defense above is allowed to override because it is higher priority.
         */
        if (RetoldAiControl.isControlled(mob)) {
            return;
        }

        LivingEntity enemy = findAutonomousEnemy(
                level,
                mob
        );

        if (enemy == null) {
            return;
        }

        beginAttack(
                mob,
                enemy,
                getEnemyAttackSpeed(mob, enemy),
                gameTime
        );
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity killed = event.getEntity();

        PathfinderMob killer = findResponsibleKiller(
                event,
                killed
        );

        if (killer == null) {
            return;
        }

        if (!RetoldAiControl.isControlledAs(killer, RetoldAiControlMode.ATTACK)) {
            return;
        }

        RetoldBehaviorTargets.clearTargetAndAggression(killer, killed, false);

        killer.getNavigation().stop();
        RetoldAiControl.clear(killer);

        /*
         * Important:
         * ATTACK kills do not affect hunger.
         * Food hunger changes only happen through eating actual items/blocks.
         */
    }

    private static PathfinderMob findResponsibleKiller(
            LivingDeathEvent event,
            LivingEntity killed
    ) {
        Entity sourceEntity = event.getSource().getEntity();

        if (sourceEntity instanceof PathfinderMob mob) {
            return mob;
        }

        if (killed.getLastHurtByMob() instanceof PathfinderMob mob) {
            return mob;
        }

        return null;
    }

    private static boolean shouldThink(
            PathfinderMob mob,
            long gameTime
    ) {
        return RetoldBehaviorTiming.shouldThink(
                mob,
                gameTime,
                COMBAT_THINK_INTERVAL_TICKS
        );
    }

    private static LivingEntity findOwnerThreat(PathfinderMob defender) {
        if (!(defender instanceof TamableAnimal tamableAnimal)) {
            return null;
        }

        if (!tamableAnimal.isTame()) {
            return null;
        }

        LivingEntity owner = tamableAnimal.getOwner();

        if (owner == null || !owner.isAlive() || owner.isRemoved()) {
            return null;
        }

        LivingEntity attacker = owner.getLastHurtByMob();

        if (!isValidOwnerThreat(defender, owner, attacker)) {
            return null;
        }

        return attacker;
    }

    private static boolean isValidOwnerThreat(
            PathfinderMob defender,
            LivingEntity owner,
            LivingEntity threat
    ) {
        if (defender == null || owner == null || threat == null) {
            return false;
        }

        if (!RetoldBehaviorCoordinator.isUsableEntity(defender)) {
            return false;
        }

        if (!RetoldBehaviorCoordinator.isAliveInSameLevel(defender, owner)) {
            return false;
        }

        if (!RetoldBehaviorCoordinator.isValidAssignmentTarget(defender, threat)) {
            return false;
        }

        if (threat == owner || threat == defender) {
            return false;
        }

        if (
                threat instanceof TamableAnimal threatTamable
                        && threatTamable.isTame()
                        && threatTamable.getOwner() == owner
        ) {
            return false;
        }

        return defender.distanceToSqr(threat) <= OWNER_THREAT_RADIUS_SQUARED
                || owner.distanceToSqr(threat) <= OWNER_THREAT_RADIUS_SQUARED;
    }

    private static LivingEntity findAutonomousEnemy(
            ServerLevel level,
            PathfinderMob mob
    ) {
        if (!isWolf(mob)) {
            return null;
        }

        List<LivingEntity> candidates = RetoldAiScanCache.nearby(
                level,
                mob,
                LivingEntity.class,
                WOLF_ENEMY_SEARCH_RADIUS_BLOCKS,
                level.getGameTime(),
                COMBAT_SCAN_CACHE_TICKS
        );

        LivingEntity bestEnemy = null;
        double bestScore = Double.MAX_VALUE;

        for (LivingEntity candidate : candidates) {
            if (!isValidWolfEnemy(mob, candidate)) {
                continue;
            }

            double distanceSquared = mob.distanceToSqr(candidate);

            if (distanceSquared > WOLF_ENEMY_SEARCH_RADIUS_SQUARED) {
                continue;
            }

            double score = distanceSquared;

            if (RetoldAiSightCache.canSee(mob, candidate, level.getGameTime())) {
                score -= 16.0D;
            }

            if (score < bestScore) {
                bestScore = score;
                bestEnemy = candidate;
            }
        }

        return bestEnemy;
    }

    private static boolean isValidWolfEnemy(
            PathfinderMob wolf,
            LivingEntity candidate
    ) {
        if (wolf == null || candidate == null) {
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

        if (wolf.distanceToSqr(candidate) > WOLF_ENEMY_SEARCH_RADIUS_SQUARED) {
            return false;
        }

        if (!RetoldMobRules.isWolfEnemyButNotFood(candidate)) {
            return false;
        }

        /*
         * Sight or close smell/hearing range.
         */
        return RetoldAiSightCache.canSee(wolf, candidate, wolf.level().getGameTime())
                || wolf.distanceToSqr(candidate) <= 16.0D;
    }

    private static void beginAttack(
            PathfinderMob attacker,
            LivingEntity target,
            double speed,
            long gameTime
    ) {
        if (attacker == null || target == null) {
            return;
        }

        RetoldAiControl.claim(
                attacker,
                RetoldAiControlMode.ATTACK,
                gameTime,
                ATTACK_CONTROL_TICKS
        );

        if (!RetoldBehaviorTargets.setAttackTargetOrClearMode(
                attacker,
                target,
                RetoldAiControlMode.ATTACK
        )) {
            return;
        }

        attacker.getLookControl().setLookAt(
                target,
                30.0F,
                30.0F
        );

        RetoldBehaviorMovement.throttledMoveTo(
                attacker,
                target,
                speed,
                gameTime,
                COMBAT_PATH_INTERVAL_TICKS,
                2.0D * 2.0D
        );
    }

    private static void continueAttack(
            PathfinderMob attacker,
            long gameTime
    ) {
        LivingEntity target = attacker.getTarget();

        if (!isStillValidAttackTarget(attacker, target)) {
            stopAttack(attacker);
            return;
        }

        RetoldAiControl.refresh(
                attacker,
                RetoldAiControlMode.ATTACK,
                gameTime,
                ATTACK_CONTROL_TICKS
        );

        RetoldBehaviorTargets.setAggression(attacker, true);

        attacker.getLookControl().setLookAt(
                target,
                30.0F,
                30.0F
        );

        RetoldBehaviorMovement.throttledMoveTo(
                attacker,
                target,
                getEnemyAttackSpeed(attacker, target),
                gameTime,
                COMBAT_PATH_INTERVAL_TICKS,
                2.0D * 2.0D
        );
    }

    private static boolean isStillValidAttackTarget(
            PathfinderMob attacker,
            LivingEntity target
    ) {
        if (attacker == null || target == null) {
            return false;
        }

        if (!RetoldBehaviorCoordinator.isUsableEntity(attacker)) {
            return false;
        }

        if (!RetoldBehaviorCoordinator.isValidAssignmentTarget(attacker, target)) {
            return false;
        }

        if (attacker.distanceToSqr(target) > ATTACK_KEEP_RADIUS_SQUARED) {
            return false;
        }

        if (isWolf(attacker) && RetoldMobRules.isWolfEnemyButNotFood(target)) {
            return true;
        }

        if (attacker instanceof TamableAnimal tamableAnimal && tamableAnimal.isTame()) {
            LivingEntity owner = tamableAnimal.getOwner();

            if (owner != null) {
                return isValidOwnerThreat(
                        attacker,
                        owner,
                        target
                );
            }
        }

        return false;
    }

    private static void stopAttack(PathfinderMob attacker) {
        RetoldBehaviorTargets.setTargetAndAggression(attacker, null, false);

        attacker.getNavigation().stop();

        RetoldAiControl.clear(attacker);
    }

    private static double getEnemyAttackSpeed(
            PathfinderMob attacker,
            LivingEntity target
    ) {
        if (isWolf(attacker) && RetoldMobRules.isWolfEnemyButNotFood(target)) {
            return WOLF_SKELETON_ATTACK_SPEED;
        }

        return OWNER_DEFENSE_SPEED;
    }

    private static boolean isWolf(PathfinderMob mob) {
        return RetoldMobRules.isWolf(mob);
    }
}
