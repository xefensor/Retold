package cz.xefensor.retold.behavior;

import cz.xefensor.retold.combat.RetoldFactionTargetGuards;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.List;

public final class RetoldControlledCombatEvents {
    private static final int COMBAT_THINK_INTERVAL_TICKS = 10;
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

        if (!RetoldMobRules.isManagedPredator(mob)) {
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

        if (killer.getTarget() == killed) {
            RetoldFactionTargetGuards.setTargetIgnoringGuard(
                    killer,
                    null
            );
        }

        RetoldFactionTargetGuards.setAggressiveIgnoringGuard(
                killer,
                false
        );

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

        if (!defender.isAlive() || defender.isRemoved()) {
            return false;
        }

        if (!owner.isAlive() || owner.isRemoved()) {
            return false;
        }

        if (!threat.isAlive() || threat.isRemoved()) {
            return false;
        }

        if (threat == owner || threat == defender) {
            return false;
        }

        if (defender.level() != owner.level() || defender.level() != threat.level()) {
            return false;
        }

        if (threat instanceof Player player && (player.isCreative() || player.isSpectator())) {
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

        AABB area = mob.getBoundingBox().inflate(WOLF_ENEMY_SEARCH_RADIUS_BLOCKS);

        List<LivingEntity> candidates = level.getEntitiesOfClass(
                LivingEntity.class,
                area,
                candidate -> isValidWolfEnemy(
                        mob,
                        candidate
                )
        );

        LivingEntity bestEnemy = null;
        double bestScore = Double.MAX_VALUE;

        for (LivingEntity candidate : candidates) {
            double distanceSquared = mob.distanceToSqr(candidate);

            if (distanceSquared > WOLF_ENEMY_SEARCH_RADIUS_SQUARED) {
                continue;
            }

            double score = distanceSquared;

            if (mob.hasLineOfSight(candidate)) {
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

        if (!candidate.isAlive() || candidate.isRemoved()) {
            return false;
        }

        if (wolf.level() != candidate.level()) {
            return false;
        }

        if (candidate instanceof Player player && (player.isCreative() || player.isSpectator())) {
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
        return wolf.hasLineOfSight(candidate)
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

        RetoldFactionTargetGuards.setTargetIgnoringGuard(
                attacker,
                target
        );

        RetoldFactionTargetGuards.setAggressiveIgnoringGuard(
                attacker,
                true
        );

        attacker.getLookControl().setLookAt(
                target,
                30.0F,
                30.0F
        );

        RetoldAiControl.withNavigationBypass(() -> {
            attacker.getNavigation().moveTo(
                    target,
                    speed
            );
        });
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

        RetoldFactionTargetGuards.setAggressiveIgnoringGuard(
                attacker,
                true
        );

        attacker.getLookControl().setLookAt(
                target,
                30.0F,
                30.0F
        );

        RetoldAiControl.withNavigationBypass(() -> {
            attacker.getNavigation().moveTo(
                    target,
                    getEnemyAttackSpeed(attacker, target)
            );
        });
    }

    private static boolean isStillValidAttackTarget(
            PathfinderMob attacker,
            LivingEntity target
    ) {
        if (attacker == null || target == null) {
            return false;
        }

        if (!attacker.isAlive() || attacker.isRemoved()) {
            return false;
        }

        if (!target.isAlive() || target.isRemoved()) {
            return false;
        }

        if (attacker.level() != target.level()) {
            return false;
        }

        if (target instanceof Player player && (player.isCreative() || player.isSpectator())) {
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
        RetoldFactionTargetGuards.setTargetIgnoringGuard(
                attacker,
                null
        );

        RetoldFactionTargetGuards.setAggressiveIgnoringGuard(
                attacker,
                false
        );

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
        return RetoldMobRules.isEntityPath(
                mob,
                "wolf"
        );
    }
}
