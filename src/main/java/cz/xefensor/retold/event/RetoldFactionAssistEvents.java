package cz.xefensor.retold.event;

import cz.xefensor.retold.combat.RetoldAiTargets;
import cz.xefensor.retold.combat.RetoldCombatTargets;
import cz.xefensor.retold.faction.RetoldFaction;
import cz.xefensor.retold.faction.RetoldFactionMembers;
import cz.xefensor.retold.faction.RetoldFactionRelations;
import cz.xefensor.retold.combat.RetoldTargetSource;
import cz.xefensor.retold.territory.RetoldTerritoryEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class RetoldFactionAssistEvents {
    private static final int ASSIST_RADIUS_BLOCKS = 32;
    private static final int ENEMY_FACTION_DETECT_RADIUS_BLOCKS = 40;

    private static final int HELP_CALL_COOLDOWN_TICKS = 40;

    private static final Map<Entity, Long> LAST_HELP_CALL_AT = new WeakHashMap<>();
    private static final Map<Entity, LivingEntity> LAST_ANNOUNCED_TARGETS = new WeakHashMap<>();
    private static final Map<Entity, RetoldFaction> LAST_ANNOUNCED_TARGET_FACTIONS = new WeakHashMap<>();

    private RetoldFactionAssistEvents() {
    }

    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();

        if (victim.level().isClientSide()) {
            return;
        }

        if (!(victim.level() instanceof ServerLevel)) {
            return;
        }

        ServerLevel level = (ServerLevel) victim.level();
        LivingEntity attacker = getLivingAttacker(event.getSource());

        if (attacker == null) {
            return;
        }

        if (!RetoldAiTargets.isAliveInSameLevel(victim, attacker)) {
            return;
        }

        RetoldFaction victimFaction = RetoldFactionMembers.getFactionOrLooseAllyFaction(victim);
        RetoldFaction attackerFaction = RetoldFactionMembers.getFactionOrLooseAllyFaction(attacker);

        if (victimFaction != null && victimFaction != attackerFaction) {
            if (attackerFaction != null) {
                callForFactionHelpAgainstFaction(level, victim, attackerFaction, victimFaction);
            } else {
                callForFactionHelp(level, victim, attacker, victimFaction);
            }
        }

        if (attackerFaction != null && attackerFaction != victimFaction) {
            if (victimFaction != null) {
                callForFactionHelpAgainstFaction(level, attacker, victimFaction, attackerFaction);
            } else {
                callForFactionHelp(level, attacker, victim, attackerFaction);
            }
        }
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
        RetoldFaction mobFaction = RetoldFactionMembers.getFactionOrLooseAllyFaction(mob);

        if (mobFaction == null) {
            clearAnnouncements(mob);
            return;
        }

        LivingEntity target = mob.getTarget();

        if (target == null || !RetoldAiTargets.isAliveInSameLevel(mob, target)) {
            clearAnnouncements(mob);
            return;
        }

        RetoldFaction targetFaction = RetoldFactionMembers.getFactionOrLooseAllyFaction(target);

        if (targetFaction == mobFaction) {
            clearAnnouncements(mob);
            return;
        }

        if (targetFaction != null) {
            RetoldFaction lastAnnouncedFaction = LAST_ANNOUNCED_TARGET_FACTIONS.get(mob);

            if (lastAnnouncedFaction == targetFaction) {
                return;
            }

            LAST_ANNOUNCED_TARGET_FACTIONS.put(mob, targetFaction);
            LAST_ANNOUNCED_TARGETS.put(mob, target);

            callForFactionHelpAgainstFaction(level, mob, targetFaction, mobFaction);
            callForFactionHelpAgainstFaction(level, target, mobFaction, targetFaction);
            return;
        }

        LivingEntity lastAnnouncedTarget = LAST_ANNOUNCED_TARGETS.get(mob);

        if (lastAnnouncedTarget == target) {
            return;
        }

        LAST_ANNOUNCED_TARGETS.put(mob, target);
        LAST_ANNOUNCED_TARGET_FACTIONS.remove(mob);

        callForFactionHelp(level, mob, target, mobFaction);
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

        if (!RetoldFactionMembers.isAlignedWith(caller, callerFaction)) {
            return;
        }

        RetoldFaction targetFaction = RetoldFactionMembers.getFactionOrLooseAllyFaction(target);

        if (targetFaction != null && targetFaction != callerFaction) {
            callForFactionHelpAgainstFaction(level, caller, targetFaction, callerFaction);
            return;
        }

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

    public static void callForFactionHelpAgainstFaction(
            ServerLevel level,
            LivingEntity caller,
            RetoldFaction enemyFaction,
            RetoldFaction callerFaction
    ) {
        if (!caller.isAlive()) {
            return;
        }

        if (!RetoldFactionMembers.isAlignedWith(caller, callerFaction)) {
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

    private static void alertFactionAlliesAgainstSpecificTarget(
            ServerLevel level,
            LivingEntity caller,
            LivingEntity target,
            RetoldFaction callerFaction
    ) {
        AABB area = caller.getBoundingBox().inflate(ASSIST_RADIUS_BLOCKS);

        for (PathfinderMob ally : level.getEntitiesOfClass(
                PathfinderMob.class,
                area,
                mob -> isValidFactionAlly(mob, caller, callerFaction)
        )) {
            if (ally.getTarget() == null || ally.getTarget() == target) {
                makeAllyAttack(ally, target);
            }
        }
    }

    private static void alertFactionAlliesAgainstEnemyFaction(
            ServerLevel level,
            LivingEntity caller,
            RetoldFaction callerFaction,
            RetoldFaction enemyFaction
    ) {
        AABB allyArea = caller.getBoundingBox().inflate(ASSIST_RADIUS_BLOCKS);

        List<PathfinderMob> allies = level.getEntitiesOfClass(
                PathfinderMob.class,
                allyArea,
                mob -> isValidFactionAlly(mob, caller, callerFaction)
        );

        AABB enemyArea = caller.getBoundingBox().inflate(
                ASSIST_RADIUS_BLOCKS + ENEMY_FACTION_DETECT_RADIUS_BLOCKS
        );

        List<LivingEntity> enemies = level.getEntitiesOfClass(
                LivingEntity.class,
                enemyArea,
                enemy -> isValidEnemyFactionMember(enemy, caller, enemyFaction)
        );

        if (enemies.isEmpty()) {
            return;
        }

        Map<LivingEntity, Integer> assignments = new HashMap<>();

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

            makeAllyAttack(ally, chosenTarget);
            assignments.put(chosenTarget, assignments.getOrDefault(chosenTarget, 0) + 1);
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
        return RetoldAiTargets.isAliveInSameLevel(caller, ally)
                && RetoldFactionMembers.isAlignedWith(ally, callerFaction);
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
                && RetoldFactionMembers.isAlignedWith(enemy, enemyFaction);
    }

    private static boolean canDetectEnemy(PathfinderMob ally, LivingEntity enemy) {
        if (ally.distanceToSqr(enemy)
                > ENEMY_FACTION_DETECT_RADIUS_BLOCKS * ENEMY_FACTION_DETECT_RADIUS_BLOCKS) {
            return false;
        }

        return RetoldAiTargets.isVisibleTo(ally, enemy);
    }

    private static void makeAllyAttack(PathfinderMob ally, LivingEntity target) {
        if (RetoldTerritoryEvents.shouldBlockTargetDuringWarning(ally, target)) {
            return;
        }

        boolean applied = RetoldCombatTargets.applyAttackTarget(
                ally,
                target,
                RetoldTargetSource.FACTION_ASSIST
        );

        if (!applied) {
            return;
        }
    }

    private static void clearAnnouncements(Entity entity) {
        LAST_ANNOUNCED_TARGETS.remove(entity);
        LAST_ANNOUNCED_TARGET_FACTIONS.remove(entity);
    }
}
