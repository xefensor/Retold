package cz.xefensor.retold.event;

import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.control.RetoldAiControlMode;
import cz.xefensor.retold.behavior.control.RetoldControlledCombatEvents;
import cz.xefensor.retold.behavior.flee.RetoldControlledFleeEvents;
import cz.xefensor.retold.behavior.performance.RetoldAiScanCache;
import cz.xefensor.retold.behavior.performance.RetoldAiSightCache;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;
import cz.xefensor.retold.combat.RetoldAiTargets;
import cz.xefensor.retold.combat.RetoldCombatTargets;
import cz.xefensor.retold.combat.RetoldFactionTargetMemory;
import cz.xefensor.retold.combat.RetoldMobTargetPolicy;
import cz.xefensor.retold.combat.RetoldTargetSource;
import cz.xefensor.retold.faction.RetoldFaction;
import cz.xefensor.retold.faction.RetoldFactionMembers;
import cz.xefensor.retold.faction.RetoldFactionRelations;
import cz.xefensor.retold.territory.RetoldTerritoryEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class RetoldFactionAssistEvents {
    private static final int ASSIST_RADIUS_BLOCKS = 32;
    private static final int ENEMY_FACTION_DETECT_RADIUS_BLOCKS = 40;
    private static final int ASSIST_SCAN_CACHE_TICKS = 5;
    private static final int MAX_ASSIST_RESPONDERS = 8;
    private static final double CLOSE_HELP_CALL_RADIUS_SQUARED = 8.0D * 8.0D;

    private static final int HELP_CALL_COOLDOWN_TICKS = 40;
    private static final int ATTACK_INTENT_RECHECK_TICKS = 5;
    private static final double VISIBLE_ATTACK_INTENT_RADIUS_SQUARED = 24.0D * 24.0D;

    private static final Map<Entity, Long> LAST_HELP_CALL_AT = new WeakHashMap<>();
    private static final Map<Entity, LivingEntity> LAST_ANNOUNCED_TARGETS = new WeakHashMap<>();
    private static final Map<Entity, RetoldFaction> LAST_ANNOUNCED_TARGET_FACTIONS = new WeakHashMap<>();
    private static final Map<Entity, LivingEntity> LAST_PERCEIVED_ATTACK_TARGETS =
            new WeakHashMap<>();
    private static final Map<Entity, LivingEntity> LAST_CHECKED_ATTACK_TARGETS =
            new WeakHashMap<>();
    private static final Map<Entity, Long> NEXT_ATTACK_INTENT_CHECK_AT = new WeakHashMap<>();

    private RetoldFactionAssistEvents() {
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Post event) {
        LivingEntity victim = event.getEntity();

        if (event.getHealthDamage() <= 0.0F
                || !(victim.level() instanceof ServerLevel level)) {
            return;
        }

        LivingEntity attacker = getLivingAttacker(event.getSource());

        handleSuccessfulDamage(level, victim, attacker);
    }

    static int handleSuccessfulDamage(
            ServerLevel level,
            LivingEntity victim,
            LivingEntity attacker
    ) {
        if (level == null
                || victim == null
                || attacker == null
                || victim.level() != level
                || !RetoldAiTargets.isAliveInSameLevel(victim, attacker)) {
            return 0;
        }

        boolean sameSpeciesDefense = RetoldMobRules.isSharedDefenseSpecies(victim)
                && victim.getType() != attacker.getType();
        RetoldFaction victimFaction = RetoldFactionMembers.getActiveCombatFaction(victim);
        RetoldFaction attackerFaction = RetoldFactionMembers.getActiveCombatFaction(attacker);
        boolean factionDefense = RetoldFactionRelations.supportsSharedDefense(victimFaction)
                && victimFaction != attackerFaction;

        if ((!sameSpeciesDefense && !factionDefense)
                || !canCallForHelp(victim, level.getGameTime())) {
            return 0;
        }

        LAST_HELP_CALL_AT.put(victim, level.getGameTime());

        if (sameSpeciesDefense) {
            return alertSameSpeciesAgainstSpecificTarget(
                    level,
                    victim,
                    attacker
            );
        }

        return alertFactionAlliesAgainstSpecificTarget(
                level,
                victim,
                attacker,
                victimFaction
        );
    }

    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();

        if (!(entity instanceof PathfinderMob)) {
            return;
        }

        PathfinderMob mob = (PathfinderMob) entity;

        if (mob.level().isClientSide()) {
            return;
        }

        if (!(mob.level() instanceof ServerLevel)) {
            clearAnnouncements(mob);
            return;
        }

        ServerLevel level = (ServerLevel) mob.level();
        RetoldFaction mobFaction = RetoldFactionMembers.getActiveCombatFaction(mob);

        LivingEntity target = mob.getTarget();

        if (target == null || !RetoldAiTargets.isAliveInSameLevel(mob, target)) {
            clearAnnouncements(mob);
            clearVisibleAttackIntent(mob);
            return;
        }

        handleVisibleAttackIntent(level, mob, target);

        RetoldFaction targetFaction = RetoldFactionMembers.getActiveCombatFaction(target);

        if (mobFaction == null
                || targetFaction == mobFaction
                || !RetoldFactionRelations.supportsSharedDefense(targetFaction)
                || RetoldFactionTargetMemory.isOwnedByAny(
                mob,
                target,
                RetoldTargetSource.THREAT_RESPONSE
        )) {
            clearAnnouncements(mob);
            return;
        }

        RetoldFaction lastAnnouncedFaction = LAST_ANNOUNCED_TARGET_FACTIONS.get(mob);
        LivingEntity lastAnnouncedTarget = LAST_ANNOUNCED_TARGETS.get(mob);

        if (lastAnnouncedFaction == targetFaction
                && lastAnnouncedTarget == target) {
            return;
        }

        LAST_ANNOUNCED_TARGET_FACTIONS.put(mob, targetFaction);
        LAST_ANNOUNCED_TARGETS.put(mob, target);

        callForFactionDefenseAgainstSpecificTarget(
                level,
                target,
                mob,
                targetFaction,
                mobFaction
        );
    }

    static boolean handleVisibleAttackIntent(
            ServerLevel level,
            PathfinderMob attacker,
            LivingEntity intendedVictim
    ) {
        if (level == null
                || attacker == null
                || intendedVictim == null
                || attacker.level() != level
                || attacker.getTarget() != intendedVictim
                || !RetoldAiTargets.isAliveInSameLevel(attacker, intendedVictim)) {
            return false;
        }

        LivingEntity lastPerceivedTarget = LAST_PERCEIVED_ATTACK_TARGETS.get(attacker);

        if (lastPerceivedTarget == intendedVictim) {
            return true;
        }

        LivingEntity lastCheckedTarget = LAST_CHECKED_ATTACK_TARGETS.get(attacker);

        if (lastCheckedTarget != intendedVictim) {
            LAST_PERCEIVED_ATTACK_TARGETS.remove(attacker);
            NEXT_ATTACK_INTENT_CHECK_AT.remove(attacker);
            LAST_CHECKED_ATTACK_TARGETS.put(attacker, intendedVictim);
        }

        long gameTime = level.getGameTime();
        Long nextCheckAt = NEXT_ATTACK_INTENT_CHECK_AT.get(attacker);

        if (nextCheckAt != null && gameTime < nextCheckAt) {
            return false;
        }

        NEXT_ATTACK_INTENT_CHECK_AT.put(
                attacker,
                gameTime + ATTACK_INTENT_RECHECK_TICKS
        );

        if (!(intendedVictim instanceof PathfinderMob victim)) {
            LAST_PERCEIVED_ATTACK_TARGETS.put(attacker, intendedVictim);
            return false;
        }

        RetoldTargetSource attackSource = RetoldFactionTargetMemory.getSource(
                attacker,
                intendedVictim
        );

        if (RetoldMobTargetPolicy.shouldBlockDeliberateHostility(
                attacker,
                intendedVictim,
                attackSource
        )
                || victim.distanceToSqr(attacker) > VISIBLE_ATTACK_INTENT_RADIUS_SQUARED
                || !RetoldAiSightCache.canSee(victim, attacker, gameTime)) {
            return false;
        }

        LAST_PERCEIVED_ATTACK_TARGETS.put(attacker, intendedVictim);
        NEXT_ATTACK_INTENT_CHECK_AT.remove(attacker);

        boolean reacted = makeIntendedVictimReact(
                victim,
                attacker,
                gameTime
        );

        if (RetoldMobRules.isSharedDefenseSpecies(victim)
                && victim.getType() != attacker.getType()
                && canCallForHelp(victim, gameTime)) {
            LAST_HELP_CALL_AT.put(victim, gameTime);
            reacted |= alertSameSpeciesAgainstSpecificTarget(
                    level,
                    victim,
                    attacker
            ) > 0;
        }

        return reacted;
    }

    private static boolean makeIntendedVictimReact(
            PathfinderMob victim,
            PathfinderMob attacker,
            long gameTime
    ) {
        if (victim.getTarget() == attacker) {
            return true;
        }

        if (RetoldControlledFleeEvents.usesSharedFleeBehavior(victim)) {
            return RetoldControlledFleeEvents.beginVisibleThreatFlee(
                    victim,
                    attacker,
                    gameTime
            );
        }

        if (RetoldMobRules.canUseOrdinaryPredatorSystems(victim)) {
            return RetoldControlledCombatEvents.beginVisibleThreatDefense(
                    victim,
                    attacker,
                    gameTime
            );
        }

        RetoldFaction victimFaction = RetoldFactionMembers.getActiveCombatFaction(victim);
        RetoldFaction attackerFaction = RetoldFactionMembers.getActiveCombatFaction(attacker);

        if (victimFaction == null
                || victimFaction == RetoldFaction.CREEPERS
                || victimFaction == RetoldFaction.WARDENS
                || victimFaction == RetoldFaction.BOSSES
                || victimFaction == attackerFaction
                || (!victim.canAttack(attacker)
                && !RetoldMobTargetPolicy.shouldAllowFactionGhastTarget(victim, attacker))) {
            return false;
        }

        LivingEntity currentTarget = victim.getTarget();

        if (currentTarget != null
                && currentTarget != attacker
                && RetoldAiTargets.isAliveInSameLevel(victim, currentTarget)) {
            return false;
        }

        if (RetoldAiControl.isControlled(victim)
                && (!RetoldAiControl.isControlledAs(
                victim,
                RetoldAiControlMode.ATTACK
        ) || currentTarget != attacker)) {
            return false;
        }

        return RetoldCombatTargets.applyAttackTarget(
                victim,
                attacker,
                RetoldTargetSource.THREAT_RESPONSE
        );
    }

    public static void callForFactionHelp(
            ServerLevel level,
            LivingEntity caller,
            LivingEntity target,
            RetoldFaction callerFaction
    ) {
        if (!caller.isAlive()) {
            return;
        }

        if (!RetoldAiTargets.isAliveInSameLevel(caller, target)) {
            return;
        }

        if (!RetoldFactionMembers.isCombatAlignedWith(caller, callerFaction)) {
            return;
        }

        if (!RetoldFactionRelations.supportsSharedDefense(callerFaction)) {
            return;
        }

        RetoldFaction targetFaction = RetoldFactionMembers.getActiveCombatFaction(target);

        if (targetFaction != null && targetFaction != callerFaction) {
            callForFactionHelpAgainstFaction(level, caller, targetFaction, callerFaction);
            return;
        }

        /* Inactive loose allies remain neutral even though they cannot answer help calls. */
        if (RetoldFactionMembers.isAlignedWith(target, callerFaction)) {
            return;
        }

        long gameTime = level.getGameTime();

        if (!canCallForHelp(caller, gameTime)) {
            return;
        }

        LAST_HELP_CALL_AT.put(caller, gameTime);
        alertFactionAlliesAgainstSpecificTarget(level, caller, target, callerFaction);
    }

    private static void callForFactionDefenseAgainstSpecificTarget(
            ServerLevel level,
            LivingEntity caller,
            LivingEntity attacker,
            RetoldFaction callerFaction,
            RetoldFaction attackerFaction
    ) {
        if (!caller.isAlive()
                || !RetoldAiTargets.isAliveInSameLevel(caller, attacker)
                || !RetoldFactionMembers.isCombatAlignedWith(caller, callerFaction)
                || !RetoldFactionRelations.supportsSharedDefense(callerFaction)
                || !RetoldFactionRelations.areEnemyFactions(
                callerFaction,
                attackerFaction
        )) {
            return;
        }

        long gameTime = level.getGameTime();

        if (!canCallForHelp(caller, gameTime)) {
            return;
        }

        LAST_HELP_CALL_AT.put(caller, gameTime);
        alertFactionAlliesAgainstSpecificTarget(
                level,
                caller,
                attacker,
                callerFaction
        );
    }

    public static void callForFactionHelpAgainstFaction(
            ServerLevel level,
            LivingEntity caller,
            RetoldFaction enemyFaction,
            RetoldFaction callerFaction
    ) {
        if (!caller.isAlive()) {
            return;
        }

        if (!RetoldFactionMembers.isCombatAlignedWith(caller, callerFaction)) {
            return;
        }

        if (!RetoldFactionRelations.supportsSharedDefense(callerFaction)) {
            return;
        }

        if (enemyFaction == callerFaction) {
            return;
        }

        if (!RetoldFactionRelations.areEnemyFactions(callerFaction, enemyFaction)) {
            return;
        }

        long gameTime = level.getGameTime();

        if (!canCallForHelp(caller, gameTime)) {
            return;
        }

        LAST_HELP_CALL_AT.put(caller, gameTime);
        alertFactionAlliesAgainstEnemyFaction(level, caller, callerFaction, enemyFaction);
    }

    private static LivingEntity getLivingAttacker(DamageSource source) {
        Entity attacker = source.getEntity();

        if (attacker instanceof LivingEntity) {
            return (LivingEntity) attacker;
        }

        return null;
    }

    private static boolean canCallForHelp(LivingEntity caller, long gameTime) {
        Long lastHelpCallAt = LAST_HELP_CALL_AT.get(caller);

        return lastHelpCallAt == null
                || gameTime - lastHelpCallAt >= HELP_CALL_COOLDOWN_TICKS;
    }

    private static int alertSameSpeciesAgainstSpecificTarget(
            ServerLevel level,
            LivingEntity caller,
            LivingEntity target
    ) {
        int responders = 0;

        for (PathfinderMob ally : RetoldAiScanCache.nearby(
                level,
                caller,
                PathfinderMob.class,
                ASSIST_RADIUS_BLOCKS,
                level.getGameTime(),
                ASSIST_SCAN_CACHE_TICKS
        )) {
            if (!isValidSameSpeciesAlly(ally, caller, target)
                    || !canPerceiveHelpCall(ally, caller, target)) {
                continue;
            }

            if (makeAllyAttack(ally, target, true)) {
                responders++;

                if (responders >= MAX_ASSIST_RESPONDERS) {
                    break;
                }
            }
        }

        return responders;
    }

    private static int alertFactionAlliesAgainstSpecificTarget(
            ServerLevel level,
            LivingEntity caller,
            LivingEntity target,
            RetoldFaction callerFaction
    ) {
        int responders = 0;

        for (PathfinderMob ally : RetoldAiScanCache.nearby(
                level,
                caller,
                PathfinderMob.class,
                ASSIST_RADIUS_BLOCKS,
                level.getGameTime(),
                ASSIST_SCAN_CACHE_TICKS
        )) {
            if (!isValidFactionAlly(ally, caller, callerFaction)
                    || !canAnswerHelpCall(ally, target)
                    || !canPerceiveHelpCall(ally, caller, target)) {
                continue;
            }

            if (makeAllyAttack(ally, target, false)) {
                responders++;

                if (responders >= MAX_ASSIST_RESPONDERS) {
                    break;
                }
            }
        }

        return responders;
    }

    private static void alertFactionAlliesAgainstEnemyFaction(
            ServerLevel level,
            LivingEntity caller,
            RetoldFaction callerFaction,
            RetoldFaction enemyFaction
    ) {
        List<PathfinderMob> allies = new ArrayList<>();

        for (PathfinderMob candidate : RetoldAiScanCache.nearby(
                level,
                caller,
                PathfinderMob.class,
                ASSIST_RADIUS_BLOCKS,
                level.getGameTime(),
                ASSIST_SCAN_CACHE_TICKS
        )) {
            if (isValidFactionAlly(candidate, caller, callerFaction)
                    && canAnswerFactionCall(candidate, enemyFaction)) {
                allies.add(candidate);
            }
        }

        List<LivingEntity> enemies = new ArrayList<>();

        for (LivingEntity candidate : RetoldAiScanCache.nearby(
                level,
                caller,
                LivingEntity.class,
                ASSIST_RADIUS_BLOCKS + ENEMY_FACTION_DETECT_RADIUS_BLOCKS,
                level.getGameTime(),
                ASSIST_SCAN_CACHE_TICKS
        )) {
            if (isValidEnemyFactionMember(candidate, caller, enemyFaction)) {
                enemies.add(candidate);
            }
        }

        if (enemies.isEmpty()) {
            return;
        }

        Map<LivingEntity, Integer> assignments = new HashMap<>();

        int responders = 0;

        for (PathfinderMob ally : allies) {
            LivingEntity currentTarget = ally.getTarget();

            if (currentTarget != null && isValidEnemyFactionMember(currentTarget, ally, enemyFaction)) {
                assignments.put(currentTarget, assignments.getOrDefault(currentTarget, 0) + 1);
            }
        }

        for (PathfinderMob ally : allies) {
            LivingEntity currentTarget = ally.getTarget();

            if (currentTarget != null
                    && isValidEnemyFactionMember(currentTarget, ally, enemyFaction)
                    && canDetectEnemy(ally, currentTarget)) {
                continue;
            }

            LivingEntity chosenTarget = chooseEnemyFactionTargetForAlly(ally, enemies, assignments);

            if (chosenTarget == null) {
                continue;
            }

            if (makeAllyAttack(ally, chosenTarget, false)) {
                assignments.put(chosenTarget, assignments.getOrDefault(chosenTarget, 0) + 1);
                responders++;

                if (responders >= MAX_ASSIST_RESPONDERS) {
                    break;
                }
            }
        }
    }

    private static LivingEntity chooseEnemyFactionTargetForAlly(
            PathfinderMob ally,
            List<LivingEntity> enemies,
            Map<LivingEntity, Integer> assignments
    ) {
        LivingEntity bestTarget = null;
        int bestAssignmentCount = Integer.MAX_VALUE;
        double bestDistance = Double.MAX_VALUE;

        for (LivingEntity enemy : enemies) {
            if (!canDetectEnemy(ally, enemy)) {
                continue;
            }

            int assignmentCount = assignments.getOrDefault(enemy, 0);
            double distance = ally.distanceToSqr(enemy);

            if (assignmentCount < bestAssignmentCount
                    || assignmentCount == bestAssignmentCount && distance < bestDistance) {
                bestTarget = enemy;
                bestAssignmentCount = assignmentCount;
                bestDistance = distance;
            }
        }

        return bestTarget;
    }

    private static boolean isValidFactionAlly(
            PathfinderMob ally,
            LivingEntity caller,
            RetoldFaction callerFaction
    ) {
        return ally != caller
                && RetoldAiTargets.isAliveInSameLevel(caller, ally)
                && RetoldFactionMembers.areCooperatingAllies(
                caller,
                ally,
                callerFaction
        );
    }

    private static boolean isValidSameSpeciesAlly(
            PathfinderMob ally,
            LivingEntity caller,
            LivingEntity target
    ) {
        if (ally == caller
                || ally.getType() != caller.getType()
                || !RetoldMobRules.isSharedDefenseSpecies(ally)
                || !RetoldAiTargets.isAliveInSameLevel(caller, ally)
                || !canAnswerHelpCall(ally, target)
                || ally instanceof AgeableMob ageableMob && ageableMob.isBaby()) {
            return false;
        }

        boolean callerIsTamable = caller instanceof TamableAnimal;
        boolean allyIsTamable = ally instanceof TamableAnimal;

        if (!callerIsTamable || !allyIsTamable) {
            return !callerIsTamable && !allyIsTamable;
        }

        TamableAnimal callerTamable = (TamableAnimal) caller;
        TamableAnimal allyTamable = (TamableAnimal) ally;

        if (callerTamable.isTame() != allyTamable.isTame()) {
            return false;
        }

        if (!callerTamable.isTame()) {
            return true;
        }

        return !allyTamable.isOrderedToSit()
                && callerTamable.getOwnerReference() != null
                && callerTamable.getOwnerReference().equals(
                allyTamable.getOwnerReference()
        );
    }

    private static boolean canAnswerFactionCall(
            PathfinderMob ally,
            RetoldFaction enemyFaction
    ) {
        LivingEntity currentTarget = ally.getTarget();

        if (currentTarget != null
                && RetoldAiTargets.isAliveInSameLevel(ally, currentTarget)
                && !isValidEnemyFactionMember(currentTarget, ally, enemyFaction)) {
            return false;
        }

        return canAnswerHelpCall(ally, currentTarget);
    }

    private static boolean canAnswerHelpCall(
            PathfinderMob ally,
            LivingEntity target
    ) {
        if (ally instanceof AgeableMob ageableMob && ageableMob.isBaby()) {
            return false;
        }

        if (ally instanceof TamableAnimal tamableAnimal
                && tamableAnimal.isOrderedToSit()) {
            return false;
        }

        LivingEntity currentTarget = ally.getTarget();

        if (currentTarget != null
                && currentTarget != target
                && RetoldAiTargets.isAliveInSameLevel(ally, currentTarget)) {
            return false;
        }

        return !RetoldAiControl.isControlled(ally)
                || RetoldAiControl.isControlledAs(ally, RetoldAiControlMode.ATTACK)
                && currentTarget == target;
    }

    private static boolean canPerceiveHelpCall(
            PathfinderMob ally,
            LivingEntity caller,
            LivingEntity target
    ) {
        return ally.distanceToSqr(caller) <= CLOSE_HELP_CALL_RADIUS_SQUARED
                || RetoldAiSightCache.canSee(
                ally,
                target,
                ally.level().getGameTime()
        );
    }

    private static boolean isValidEnemyFactionMember(
            LivingEntity enemy,
            LivingEntity observer,
            RetoldFaction enemyFaction
    ) {
        if (enemy == observer) {
            return false;
        }

        return RetoldAiTargets.isAliveInSameLevel(observer, enemy)
                && RetoldFactionMembers.isCombatAlignedWith(enemy, enemyFaction);
    }

    private static boolean canDetectEnemy(PathfinderMob ally, LivingEntity enemy) {
        if (ally.distanceToSqr(enemy)
                > ENEMY_FACTION_DETECT_RADIUS_BLOCKS * ENEMY_FACTION_DETECT_RADIUS_BLOCKS) {
            return false;
        }

        return RetoldAiTargets.isVisibleTo(ally, enemy);
    }

    private static boolean makeAllyAttack(
            PathfinderMob ally,
            LivingEntity target,
            boolean useControlledCombat
    ) {
        if (RetoldTerritoryEvents.shouldBlockTargetDuringWarning(ally, target)) {
            return false;
        }

        if (useControlledCombat
                && RetoldMobRules.canUseOrdinaryPredatorSystems(ally)) {
            return RetoldControlledCombatEvents.beginSharedDefense(
                    ally,
                    target,
                    ally.level().getGameTime()
            );
        }

        return RetoldCombatTargets.applyAttackTarget(
                ally,
                target,
                RetoldTargetSource.FACTION_ASSIST
        );
    }

    private static void clearAnnouncements(Entity entity) {
        LAST_ANNOUNCED_TARGETS.remove(entity);
        LAST_ANNOUNCED_TARGET_FACTIONS.remove(entity);
    }

    private static void clearVisibleAttackIntent(Entity entity) {
        LAST_PERCEIVED_ATTACK_TARGETS.remove(entity);
        LAST_CHECKED_ATTACK_TARGETS.remove(entity);
        NEXT_ATTACK_INTENT_CHECK_AT.remove(entity);
    }
}
