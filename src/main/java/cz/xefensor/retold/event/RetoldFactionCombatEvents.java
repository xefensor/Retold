package cz.xefensor.retold.event;

import cz.xefensor.retold.combat.RetoldAiTargets;
import cz.xefensor.retold.combat.RetoldCombatTargets;
import cz.xefensor.retold.combat.RetoldFactionTargetMemory;
import cz.xefensor.retold.combat.RetoldTargetSource;
import cz.xefensor.retold.faction.RetoldFaction;
import cz.xefensor.retold.faction.RetoldFactionMembers;
import cz.xefensor.retold.faction.RetoldFactionRelations;
import cz.xefensor.retold.worldgen.fire.Wildfire;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public final class RetoldFactionCombatEvents {
    private static final Map<UUID, WeakReference<Goal>> FACTION_TARGET_GOALS = new HashMap<>();
    private static final Map<UUID, WeakReference<Goal>> RETALIATION_GOALS = new HashMap<>();

    private static final int FORCED_TARGET_CHECK_INTERVAL_TICKS = 10;
    private static final int FORCED_TARGET_REFRESH_INTERVAL_TICKS = 20;

    private static final int FORCED_TARGET_RADIUS_BLOCKS = 40;
    private static final double FORCED_TARGET_RELEASE_DISTANCE_SQUARED = 48.0D * 48.0D;
    private static final int WILDFIRE_GHAST_TARGET_RADIUS_BLOCKS = 64;
    private static final double WILDFIRE_GHAST_TARGET_DISTANCE_SQUARED =
            WILDFIRE_GHAST_TARGET_RADIUS_BLOCKS * WILDFIRE_GHAST_TARGET_RADIUS_BLOCKS;

    private static final Map<Entity, LivingEntity> FORCED_TARGETS = new WeakHashMap<>();
    private static final Map<Entity, Long> NEXT_FORCED_TARGET_CHECK_AT = new WeakHashMap<>();
    private static final Map<Entity, Long> LAST_FORCED_TARGET_REFRESH_AT = new WeakHashMap<>();

    private RetoldFactionCombatEvents() {
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();

        if (entity.level().isClientSide()) {
            return;
        }

        if (entity instanceof Mob mob) {
            updateFactionGoals(mob);
        }
    }

    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        if (event.getEntity() instanceof Mob mob) {
            removeFactionGoals(mob);
        }
    }

    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();

        if (!(entity instanceof Mob)) {
            return;
        }

        Mob mob = (Mob) entity;

        if (mob.level().isClientSide()) {
            return;
        }

        if (!(mob.level() instanceof ServerLevel)) {
            clearForcedTarget(mob);
            return;
        }

        ServerLevel level = (ServerLevel) mob.level();

        updateFactionGoals(mob);
        RetoldFactionTargetMemory.cleanupTargetState(mob);

        if (!RetoldFactionRelations.hasPotentialFactionTarget(mob)
                || usesSpecializedFactionTargeting(mob)) {
            clearForcedTarget(mob);
            return;
        }

        long gameTime = level.getGameTime();

        Long nextCheckAt = NEXT_FORCED_TARGET_CHECK_AT.get(mob);

        if (nextCheckAt != null && gameTime < nextCheckAt) {
            return;
        }

        NEXT_FORCED_TARGET_CHECK_AT.put(
                mob,
                gameTime + FORCED_TARGET_CHECK_INTERVAL_TICKS + Math.abs(mob.getId() % 10)
        );

        updateForcedTarget(level, mob, gameTime);
    }

    private static void updateFactionGoals(Mob mob) {
        boolean needsFactionTargetGoal = RetoldFactionRelations.hasPotentialFactionTarget(mob)
                && !usesSpecializedFactionTargeting(mob);
        UUID entityId = mob.getUUID();
        Goal factionTargetGoal = getGoal(FACTION_TARGET_GOALS.get(entityId));

        if (needsFactionTargetGoal && factionTargetGoal == null) {
            factionTargetGoal = new NearestAttackableTargetGoal<>(
                    mob,
                    LivingEntity.class,
                    10,
                    true,
                    false,
                    (target, level) -> isValidFactionTarget(mob, target)
            );
            FACTION_TARGET_GOALS.put(entityId, new WeakReference<>(factionTargetGoal));
            mob.targetSelector.addGoal(2, factionTargetGoal);
        } else if (!needsFactionTargetGoal && factionTargetGoal != null) {
            mob.targetSelector.removeGoal(factionTargetGoal);
            FACTION_TARGET_GOALS.remove(entityId);
        }

        if (!(mob instanceof PathfinderMob pathfinderMob)) {
            return;
        }

        boolean needsRetaliationGoal = RetoldFactionMembers.hasFaction(pathfinderMob);
        Goal retaliationGoal = getGoal(RETALIATION_GOALS.get(entityId));

        if (needsRetaliationGoal && retaliationGoal == null) {
            retaliationGoal = new HurtByTargetGoal(pathfinderMob);
            RETALIATION_GOALS.put(entityId, new WeakReference<>(retaliationGoal));
            pathfinderMob.targetSelector.addGoal(1, retaliationGoal);
        } else if (!needsRetaliationGoal && retaliationGoal != null) {
            pathfinderMob.targetSelector.removeGoal(retaliationGoal);
            RETALIATION_GOALS.remove(entityId);
        }
    }

    private static void removeFactionGoals(Mob mob) {
        UUID entityId = mob.getUUID();
        Goal factionTargetGoal = getGoal(FACTION_TARGET_GOALS.remove(entityId));

        if (factionTargetGoal != null) {
            mob.targetSelector.removeGoal(factionTargetGoal);
        }

        if (!(mob instanceof PathfinderMob pathfinderMob)) {
            return;
        }

        Goal retaliationGoal = getGoal(RETALIATION_GOALS.remove(entityId));

        if (retaliationGoal != null) {
            pathfinderMob.targetSelector.removeGoal(retaliationGoal);
        }
    }

    private static Goal getGoal(WeakReference<Goal> reference) {
        return reference == null ? null : reference.get();
    }

    private static boolean usesSpecializedFactionTargeting(Mob mob) {
        return mob.getType() == EntityTypes.WITHER;
    }

    private static boolean isValidFactionTarget(Mob mob, LivingEntity target) {
        if (target == mob) {
            return false;
        }

        if (!RetoldAiTargets.isAliveInSameLevel(mob, target)) {
            return false;
        }

        if (mob.distanceToSqr(target) > acquisitionDistanceSquared(mob, target)) {
            return false;
        }

        RetoldFaction targetFaction = RetoldFactionMembers.getFaction(target);

        if (targetFaction == RetoldFaction.PLAYER) {
            return false;
        }

        RetoldFaction attackerFaction = RetoldFactionMembers.getFaction(mob);

        if (attackerFaction == RetoldFaction.ILLAGERS) {
            if (!(mob.level() instanceof ServerLevel)) {
                return false;
            }

            ServerLevel level = (ServerLevel) mob.level();

            if (!isInRaid(level, mob)) {
                return false;
            }
        }

        if (!RetoldFactionRelations.shouldAttack(mob, target)) {
            return false;
        }

        return mob.getSensing().hasLineOfSight(target);
    }

    private static void updateForcedTarget(ServerLevel level, Mob mob, long gameTime) {
        LivingEntity currentForcedTarget = FORCED_TARGETS.get(mob);

        if (currentForcedTarget != null && !isValidForcedTarget(mob, currentForcedTarget)) {
            clearForcedTarget(mob);
            currentForcedTarget = null;
        }

        LivingEntity currentMobTarget = mob.getTarget();

        if (currentForcedTarget == null) {
            if (currentMobTarget != null && isValidForcedTarget(mob, currentMobTarget)) {
                currentForcedTarget = currentMobTarget;
            } else {
                currentForcedTarget = findNearestFactionTarget(level, mob);
            }

            if (currentForcedTarget == null) {
                return;
            }

            FORCED_TARGETS.put(mob, currentForcedTarget);
        }

        Long lastRefreshAt = LAST_FORCED_TARGET_REFRESH_AT.get(mob);

        boolean targetMissing = mob.getTarget() == null;
        boolean targetChanged = mob.getTarget() != currentForcedTarget;
        boolean shouldRefresh = lastRefreshAt == null
                || gameTime - lastRefreshAt >= FORCED_TARGET_REFRESH_INTERVAL_TICKS;

        if (!targetMissing && !targetChanged && !shouldRefresh) {
            return;
        }

        forceTarget(mob, currentForcedTarget, gameTime);
    }

    private static LivingEntity findNearestFactionTarget(ServerLevel level, Mob mob) {
        AABB area = mob.getBoundingBox().inflate(targetSearchRadius(mob));

        LivingEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (LivingEntity candidate : level.getEntitiesOfClass(
                LivingEntity.class,
                area,
                target -> isValidFactionTarget(mob, target)
        )) {
            double distance = mob.distanceToSqr(candidate);

            if (distance < nearestDistance) {
                nearest = candidate;
                nearestDistance = distance;
            }
        }

        return nearest;
    }

    private static boolean isValidForcedTarget(Mob mob, LivingEntity target) {
        if (!RetoldAiTargets.isAliveInSameLevel(mob, target)) {
            return false;
        }

        RetoldFaction targetFaction = RetoldFactionMembers.getFaction(target);

        if (targetFaction == RetoldFaction.PLAYER) {
            return false;
        }

        if (!RetoldFactionRelations.shouldAttack(mob, target)) {
            return false;
        }

        return mob.distanceToSqr(target) <= releaseDistanceSquared(mob, target);
    }

    private static int targetSearchRadius(Mob mob) {
        return mob instanceof Wildfire
                ? WILDFIRE_GHAST_TARGET_RADIUS_BLOCKS
                : FORCED_TARGET_RADIUS_BLOCKS;
    }

    private static double acquisitionDistanceSquared(Mob mob, LivingEntity target) {
        return isWildfireGhastEngagement(mob, target)
                ? WILDFIRE_GHAST_TARGET_DISTANCE_SQUARED
                : FORCED_TARGET_RADIUS_BLOCKS * FORCED_TARGET_RADIUS_BLOCKS;
    }

    private static double releaseDistanceSquared(Mob mob, LivingEntity target) {
        return isWildfireGhastEngagement(mob, target)
                ? WILDFIRE_GHAST_TARGET_DISTANCE_SQUARED
                : FORCED_TARGET_RELEASE_DISTANCE_SQUARED;
    }

    private static boolean isWildfireGhastEngagement(Mob mob, LivingEntity target) {
        return mob instanceof Wildfire && target.getType() == EntityTypes.GHAST;
    }

    private static void forceTarget(Mob mob, LivingEntity target, long gameTime) {
        boolean applied = RetoldCombatTargets.applyAttackTarget(
                mob,
                target,
                RetoldTargetSource.FACTION_COMBAT
        );

        if (!applied) {
            return;
        }

        LAST_FORCED_TARGET_REFRESH_AT.put(mob, gameTime);
    }

    private static void clearForcedTarget(Mob mob) {
        LivingEntity forcedTarget = FORCED_TARGETS.remove(mob);

        NEXT_FORCED_TARGET_CHECK_AT.remove(mob);
        LAST_FORCED_TARGET_REFRESH_AT.remove(mob);

        if (forcedTarget != null) {
            RetoldFactionTargetMemory.clearTargetIfOwnedBy(
                    mob,
                    forcedTarget,
                    RetoldTargetSource.FACTION_COMBAT
            );
        }
    }

    private static boolean isInRaid(ServerLevel level, Entity entity) {
        Raid raid = level.getRaidAt(entity.blockPosition());

        return raid != null && raid.isActive();
    }
}
