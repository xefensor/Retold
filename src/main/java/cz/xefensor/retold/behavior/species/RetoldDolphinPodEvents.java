package cz.xefensor.retold.behavior.species;

import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.control.RetoldAiControlMode;
import cz.xefensor.retold.behavior.control.RetoldAiControlOwner;
import cz.xefensor.retold.behavior.control.RetoldAiPriorities;
import cz.xefensor.retold.behavior.core.RetoldBehaviorCombat;
import cz.xefensor.retold.behavior.performance.RetoldAiScanCache;
import cz.xefensor.retold.behavior.performance.RetoldAiSightCache;
import cz.xefensor.retold.behavior.core.RetoldBehaviorCoordinator;
import cz.xefensor.retold.behavior.core.RetoldBehaviorMovement;
import cz.xefensor.retold.behavior.core.RetoldBehaviorTargets;
import cz.xefensor.retold.behavior.core.RetoldBehaviorTiming;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;
import cz.xefensor.retold.behavior.profiles.RetoldMobState;
import cz.xefensor.retold.behavior.profiles.RetoldMobStates;
import cz.xefensor.retold.behavior.hunting.RetoldPreyTargeting;
import cz.xefensor.retold.combat.RetoldCombatTargets;
import cz.xefensor.retold.combat.RetoldFactionTargetMemory;
import cz.xefensor.retold.combat.RetoldTargetSource;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.List;

public final class RetoldDolphinPodEvents {
    private static final int THINK_INTERVAL_TICKS = 10;
    private static final int POD_SCAN_CACHE_TICKS = 5;
    private static final int POD_PATH_INTERVAL_TICKS = 6;
    private static final int POD_HUNT_CONTROL_TICKS = 20 * 4;
    private static final int POD_DEFENSE_CONTROL_TICKS = 20 * 5;
    private static final int POD_HUNT_PRIORITY = RetoldAiPriorities.above(RetoldAiPriorities.HUNT, 1);
    private static final int POD_DEFENSE_PRIORITY = RetoldAiPriorities.DEFENSE;

    private static final double POD_SHARE_RADIUS_BLOCKS = 28.0D;
    private static final double POD_SHARE_RADIUS_SQUARED =
            POD_SHARE_RADIUS_BLOCKS * POD_SHARE_RADIUS_BLOCKS;
    private static final double POD_CLOSE_WITNESS_RADIUS_SQUARED = 6.0D * 6.0D;
    private static final double POD_DEFENSE_KEEP_RADIUS_SQUARED = 36.0D * 36.0D;

    private static final double POD_HUNT_SPEED = 1.34D;
    private static final double POD_DEFENSE_SPEED = 1.28D;

    private RetoldDolphinPodEvents() {
    }

    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof PathfinderMob dolphin)) {
            return;
        }

        if (!(dolphin.level() instanceof ServerLevel level)) {
            return;
        }

        if (!isDolphin(dolphin)) {
            return;
        }

        tick(level, dolphin, level.getGameTime());
    }

    public static void onLivingDamage(LivingDamageEvent.Post event) {
        if (event.getHealthDamage() <= 0.0F
                || !(event.getEntity() instanceof PathfinderMob victim)
                || !isDolphin(victim)
                || !(victim.level() instanceof ServerLevel level)
                || !(event.getSource().getEntity() instanceof LivingEntity attacker)) {
            return;
        }

        beginCollectiveDefense(
                level,
                victim,
                attacker,
                level.getGameTime()
        );
    }

    public static void tick(
            ServerLevel level,
            PathfinderMob dolphin,
            long gameTime
    ) {
        if (level == null
                || dolphin == null
                || dolphin.level() != level
                || !isDolphin(dolphin)) {
            return;
        }

        if (!shouldThink(dolphin, gameTime)) {
            return;
        }

        LivingEntity target = dolphin.getTarget();

        if (isOwnedPodDefense(dolphin, target)) {
            continuePodDefense(dolphin, target, gameTime);
            return;
        }

        if (RetoldAiControl.isControlledAsBy(
                dolphin,
                RetoldAiControlMode.ATTACK,
                RetoldAiControlOwner.AQUATIC_POD
        )) {
            stopPodDefense(dolphin, target);
        }

        if (isValidPodPrey(dolphin, target, gameTime)) {
            sharePodTarget(
                    level,
                    dolphin,
                    target,
                    gameTime
            );
            return;
        }

        LivingEntity sharedTarget = findSharedPodTarget(
                level,
                dolphin,
                gameTime
        );

        if (sharedTarget == null || !canJoinPodHunt(dolphin, gameTime)) {
            return;
        }

        joinPodHunt(
                dolphin,
                sharedTarget,
                gameTime
        );
    }

    static void beginCollectiveDefense(
            ServerLevel level,
            PathfinderMob victim,
            LivingEntity attacker,
            long gameTime
    ) {
        if (level == null
                || victim == null
                || victim.level() != level
                || !isDolphin(victim)
                || !isValidDefenseTarget(victim, attacker)) {
            return;
        }

        defendAgainst(
                victim,
                attacker,
                RetoldTargetSource.RETALIATION,
                gameTime
        );

        for (PathfinderMob recruit : RetoldAiScanCache.nearby(
                level,
                victim,
                PathfinderMob.class,
                POD_SHARE_RADIUS_BLOCKS,
                gameTime,
                POD_SCAN_CACHE_TICKS
        )) {
            if (!canJoinPodDefense(victim, recruit, attacker, gameTime)) {
                continue;
            }

            defendAgainst(
                    recruit,
                    attacker,
                    RetoldTargetSource.FACTION_ASSIST,
                    gameTime
            );
        }
    }

    private static boolean canJoinPodDefense(
            PathfinderMob victim,
            PathfinderMob recruit,
            LivingEntity attacker,
            long gameTime
    ) {
        if (recruit == null
                || recruit == victim
                || !isDolphin(recruit)
                || !RetoldBehaviorCoordinator.isAliveInSameLevel(victim, recruit)
                || victim.distanceToSqr(recruit) > POD_SHARE_RADIUS_SQUARED
                || !isValidDefenseTarget(recruit, attacker)) {
            return false;
        }

        LivingEntity currentTarget = recruit.getTarget();

        if (currentTarget != null && currentTarget != attacker) {
            return false;
        }

        return recruit.distanceToSqr(victim) <= POD_CLOSE_WITNESS_RADIUS_SQUARED
                || RetoldAiSightCache.canSee(recruit, attacker, gameTime);
    }

    private static boolean isValidDefenseTarget(
            PathfinderMob dolphin,
            LivingEntity attacker
    ) {
        return attacker != null
                && !isDolphinEntity(attacker)
                && RetoldBehaviorCoordinator.isValidAssignmentTarget(dolphin, attacker);
    }

    private static boolean isDolphinEntity(LivingEntity entity) {
        return entity instanceof PathfinderMob mob && isDolphin(mob);
    }

    private static boolean defendAgainst(
            PathfinderMob dolphin,
            LivingEntity attacker,
            RetoldTargetSource source,
            long gameTime
    ) {
        if ((source != RetoldTargetSource.RETALIATION
                && source != RetoldTargetSource.FACTION_ASSIST)
                || !isValidDefenseTarget(dolphin, attacker)) {
            return false;
        }

        int priority = source == RetoldTargetSource.RETALIATION
                ? RetoldAiPriorities.ATTACK
                : POD_DEFENSE_PRIORITY;

        if (!RetoldBehaviorCombat.claimAttackControl(
                dolphin,
                RetoldAiControlOwner.AQUATIC_POD,
                priority,
                source == RetoldTargetSource.RETALIATION
                        ? "dolphin_pod_retaliation"
                        : "dolphin_pod_defense",
                gameTime,
                POD_DEFENSE_CONTROL_TICKS
        )) {
            return false;
        }

        if (!RetoldBehaviorCombat.applyAttackTargetOrClearOwner(
                dolphin,
                attacker,
                source,
                RetoldAiControlOwner.AQUATIC_POD
        )) {
            return false;
        }

        RetoldMobState state = RetoldMobStates.getOrCreate(dolphin, gameTime);
        state.markDanger(gameTime);
        state.addStress(source == RetoldTargetSource.RETALIATION ? 5 : 3);

        RetoldBehaviorMovement.throttledMoveTo(
                dolphin,
                attacker,
                POD_DEFENSE_SPEED,
                gameTime,
                POD_PATH_INTERVAL_TICKS,
                2.0D * 2.0D
        );
        return true;
    }

    private static boolean isOwnedPodDefense(
            PathfinderMob dolphin,
            LivingEntity target
    ) {
        return target != null
                && RetoldAiControl.isControlledAsBy(
                dolphin,
                RetoldAiControlMode.ATTACK,
                RetoldAiControlOwner.AQUATIC_POD
        )
                && RetoldFactionTargetMemory.isOwnedByAny(
                dolphin,
                target,
                RetoldTargetSource.RETALIATION,
                RetoldTargetSource.FACTION_ASSIST
        );
    }

    private static void continuePodDefense(
            PathfinderMob dolphin,
            LivingEntity target,
            long gameTime
    ) {
        if (!isValidDefenseTarget(dolphin, target)
                || dolphin.distanceToSqr(target) > POD_DEFENSE_KEEP_RADIUS_SQUARED) {
            stopPodDefense(dolphin, target);
            return;
        }

        RetoldAiControl.refreshIfOwnedBy(
                dolphin,
                RetoldAiControlMode.ATTACK,
                RetoldAiControlOwner.AQUATIC_POD,
                gameTime,
                POD_DEFENSE_CONTROL_TICKS
        );
        RetoldBehaviorMovement.throttledMoveTo(
                dolphin,
                target,
                POD_DEFENSE_SPEED,
                gameTime,
                POD_PATH_INTERVAL_TICKS,
                2.0D * 2.0D
        );
    }

    private static void stopPodDefense(
            PathfinderMob dolphin,
            LivingEntity target
    ) {
        if (target != null && RetoldFactionTargetMemory.isOwnedByAny(
                dolphin,
                target,
                RetoldTargetSource.RETALIATION,
                RetoldTargetSource.FACTION_ASSIST
        )) {
            RetoldCombatTargets.clearTargetReferencesAndAggression(
                    dolphin,
                    target,
                    true
            );
        }

        RetoldAiControl.clearIfOwnedBy(
                dolphin,
                RetoldAiControlOwner.AQUATIC_POD
        );
    }

    private static boolean isDolphin(PathfinderMob mob) {
        return RetoldMobRules.isDolphin(mob);
    }

    private static boolean shouldThink(
            PathfinderMob dolphin,
            long gameTime
    ) {
        return RetoldBehaviorTiming.shouldThink(
                dolphin,
                gameTime,
                THINK_INTERVAL_TICKS
        );
    }

    private static LivingEntity findSharedPodTarget(
            ServerLevel level,
            PathfinderMob dolphin,
            long gameTime
    ) {
        List<PathfinderMob> sources = RetoldAiScanCache.nearby(
                level,
                dolphin,
                PathfinderMob.class,
                POD_SHARE_RADIUS_BLOCKS,
                gameTime,
                POD_SCAN_CACHE_TICKS
        );

        LivingEntity bestTarget = null;
        double bestScore = Double.MAX_VALUE;

        for (PathfinderMob source : sources) {
            if (!isValidPodSource(dolphin, source, gameTime)) {
                continue;
            }

            LivingEntity target = source.getTarget();

            if (!isValidPodPrey(dolphin, target, gameTime)) {
                continue;
            }

            double score = dolphin.distanceToSqr(source);

            if (RetoldAiSightCache.canSee(source, target, gameTime)) {
                score -= 20.0D;
            }

            if (RetoldAiSightCache.canSee(dolphin, source, gameTime)) {
                score -= 8.0D;
            }

            if (score < bestScore) {
                bestScore = score;
                bestTarget = target;
            }
        }

        return bestTarget;
    }

    private static void sharePodTarget(
            ServerLevel level,
            PathfinderMob source,
            LivingEntity target,
            long gameTime
    ) {
        for (PathfinderMob recruit : RetoldAiScanCache.nearby(
                level,
                source,
                PathfinderMob.class,
                POD_SHARE_RADIUS_BLOCKS,
                gameTime,
                POD_SCAN_CACHE_TICKS
        )) {
            if (!isValidPodRecruit(source, recruit, gameTime)) {
                continue;
            }

            joinPodHunt(
                    recruit,
                    target,
                    gameTime
            );
        }
    }

    private static boolean isValidPodSource(
            PathfinderMob dolphin,
            PathfinderMob source,
            long gameTime
    ) {
        if (source == null || source == dolphin) {
            return false;
        }

        if (!isDolphin(source)) {
            return false;
        }

        if (!RetoldBehaviorCoordinator.isAliveInSameLevel(dolphin, source)) {
            return false;
        }

        if (dolphin.distanceToSqr(source) > POD_SHARE_RADIUS_SQUARED) {
            return false;
        }

        return isValidPodPrey(
                dolphin,
                source.getTarget(),
                gameTime
        );
    }

    private static boolean isValidPodRecruit(
            PathfinderMob source,
            PathfinderMob recruit,
            long gameTime
    ) {
        if (recruit == null || recruit == source) {
            return false;
        }

        if (!isDolphin(recruit)) {
            return false;
        }

        if (!RetoldBehaviorCoordinator.isAliveInSameLevel(source, recruit)) {
            return false;
        }

        if (source.distanceToSqr(recruit) > POD_SHARE_RADIUS_SQUARED) {
            return false;
        }

        return canJoinPodHunt(
                recruit,
                gameTime
        );
    }

    private static boolean canJoinPodHunt(
            PathfinderMob dolphin,
            long gameTime
    ) {
        if (RetoldBehaviorCoordinator.hasLiveTarget(dolphin)) {
            return false;
        }

        RetoldAiControlMode mode = RetoldAiControl.getMode(dolphin);

        if (
                mode != RetoldAiControlMode.NONE
                        && !RetoldAiControl.isControlledAsBy(
                        dolphin,
                        RetoldAiControlMode.HUNT,
                        RetoldAiControlOwner.AQUATIC_POD
                )
        ) {
            return false;
        }

        RetoldMobState state = RetoldMobStates.getOrCreate(
                dolphin,
                gameTime
        );

        return RetoldMobRules.hasHuntDrive(
                dolphin,
                state
        );
    }

    private static boolean isValidPodPrey(
            PathfinderMob dolphin,
            LivingEntity target,
            long gameTime
    ) {
        return RetoldPreyTargeting.isValidMobRulePrey(
                dolphin,
                target,
                gameTime
        );
    }

    private static void joinPodHunt(
            PathfinderMob dolphin,
            LivingEntity target,
            long gameTime
    ) {
        if (!RetoldAiControl.tryClaim(
                dolphin,
                RetoldAiControlMode.HUNT,
                RetoldAiControlOwner.AQUATIC_POD,
                POD_HUNT_PRIORITY,
                "dolphin_pod_hunt",
                gameTime,
                POD_HUNT_CONTROL_TICKS
        )) {
            return;
        }

        if (!RetoldBehaviorTargets.setAttackTargetOrClearOwner(
                dolphin,
                target,
                RetoldAiControlOwner.AQUATIC_POD
        )) {
            return;
        }

        RetoldBehaviorMovement.throttledMoveTo(
                dolphin,
                target,
                POD_HUNT_SPEED,
                gameTime,
                POD_PATH_INTERVAL_TICKS,
                2.0D * 2.0D
        );
    }
}
