package cz.xefensor.retold.event;

import cz.xefensor.retold.faction.RetoldFaction;
import cz.xefensor.retold.faction.RetoldFactionMembers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.Map;
import java.util.WeakHashMap;

public final class RetoldFactionAssistEvents {
    private static final int ASSIST_RADIUS_BLOCKS = 32;

    // Per caller cooldown. Prevents every target refresh from spamming assists.
    private static final int HELP_CALL_COOLDOWN_TICKS = 40;

    private static final Map<Entity, Long> LAST_HELP_CALL_AT = new WeakHashMap<>();

    // Tracks what target a faction mob has already announced.
    // If the target changes, it can call help again.
    private static final Map<Entity, LivingEntity> LAST_ANNOUNCED_TARGETS = new WeakHashMap<>();

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

        if (!attacker.isAlive()) {
            return;
        }

        if (attacker.level() != victim.level()) {
            return;
        }

        RetoldFaction victimFaction = getFaction(victim);
        RetoldFaction attackerFaction = getFaction(attacker);

        // A faction member was attacked.
        if (victimFaction != null && !RetoldFactionMembers.isMemberOf(attacker, victimFaction)) {
            callForFactionHelp(level, victim, attacker, victimFaction);
        }

        // A faction member hit someone.
        if (attackerFaction != null && !RetoldFactionMembers.isMemberOf(victim, attackerFaction)) {
            callForFactionHelp(level, attacker, victim, attackerFaction);
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
            LAST_ANNOUNCED_TARGETS.remove(mob);
            return;
        }

        ServerLevel level = (ServerLevel) mob.level();
        RetoldFaction faction = getFaction(mob);

        if (faction == null) {
            LAST_ANNOUNCED_TARGETS.remove(mob);
            return;
        }

        LivingEntity target = mob.getTarget();

        if (target == null || !target.isAlive()) {
            LAST_ANNOUNCED_TARGETS.remove(mob);
            return;
        }

        if (target.level() != mob.level()) {
            LAST_ANNOUNCED_TARGETS.remove(mob);
            return;
        }

        if (RetoldFactionMembers.isMemberOf(target, faction)) {
            LAST_ANNOUNCED_TARGETS.remove(mob);
            return;
        }

        LivingEntity lastAnnouncedTarget = LAST_ANNOUNCED_TARGETS.get(mob);

        if (lastAnnouncedTarget == target) {
            return;
        }

        LAST_ANNOUNCED_TARGETS.put(mob, target);
        callForFactionHelp(level, mob, target, faction);
    }

    public static void callForFactionHelp(
            ServerLevel level,
            LivingEntity caller,
            LivingEntity target,
            RetoldFaction faction
    ) {
        if (!caller.isAlive()) {
            return;
        }

        if (!target.isAlive()) {
            return;
        }

        if (caller.level() != target.level()) {
            return;
        }

        if (!RetoldFactionMembers.isMemberOf(caller, faction)) {
            return;
        }

        if (RetoldFactionMembers.isMemberOf(target, faction)) {
            return;
        }

        long gameTime = level.getGameTime();

        if (!canCallForHelp(caller, gameTime)) {
            return;
        }

        LAST_HELP_CALL_AT.put(caller, gameTime);
        alertFactionAllies(level, caller, target, faction);
    }

    private static RetoldFaction getFaction(LivingEntity entity) {
        if (RetoldFactionMembers.isMemberOf(entity, RetoldFaction.NETHER_REMNANTS)) {
            return RetoldFaction.NETHER_REMNANTS;
        }

        return null;
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

    private static void alertFactionAllies(
            ServerLevel level,
            LivingEntity caller,
            LivingEntity target,
            RetoldFaction faction
    ) {
        AABB area = caller.getBoundingBox().inflate(ASSIST_RADIUS_BLOCKS);

        for (PathfinderMob ally : level.getEntitiesOfClass(
                PathfinderMob.class,
                area,
                mob -> isValidAlly(mob, caller, target, faction)
        )) {
            makeAllyAttack(ally, target);
        }
    }

    private static boolean isValidAlly(
            PathfinderMob ally,
            LivingEntity caller,
            LivingEntity target,
            RetoldFaction faction
    ) {
        if (!ally.isAlive()) {
            return false;
        }

        if (ally == caller) {
            return false;
        }

        if (ally == target) {
            return false;
        }

        if (ally.level() != caller.level()) {
            return false;
        }

        if (!RetoldFactionMembers.isMemberOf(ally, faction)) {
            return false;
        }

        return ally.getTarget() == null || ally.getTarget() == target;
    }

    private static void makeAllyAttack(PathfinderMob ally, LivingEntity target) {
        ally.setTarget(target);
        ally.setAggressive(true);
        ally.getLookControl().setLookAt(target, 30.0F, 30.0F);

        if (ally instanceof AbstractPiglin) {
            AbstractPiglin piglin = (AbstractPiglin) ally;

            piglin.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, target);
            piglin.getBrain().setMemory(MemoryModuleType.ANGRY_AT, target.getUUID());
        }
    }
}