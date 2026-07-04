package cz.xefensor.retold.event;

import cz.xefensor.retold.faction.RetoldFaction;
import cz.xefensor.retold.faction.RetoldFactionMembers;
import cz.xefensor.retold.faction.RetoldFactionRelations;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class RetoldFactionCombatEvents {
    private static final Set<Entity> PATCHED_ENTITIES = Collections.newSetFromMap(new WeakHashMap<>());

    private static final int FORCED_TARGET_CHECK_INTERVAL_TICKS = 10;
    private static final int FORCED_TARGET_REFRESH_INTERVAL_TICKS = 20;

    private static final int FORCED_TARGET_RADIUS_BLOCKS = 40;
    private static final double FORCED_TARGET_RELEASE_DISTANCE_SQUARED = 48.0D * 48.0D;

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

        if (!PATCHED_ENTITIES.add(entity)) {
            return;
        }

        if (entity instanceof Mob) {
            Mob mob = (Mob) entity;

            if (RetoldFactionRelations.hasPotentialFactionTarget(mob)) {
                addFactionTargetGoal(mob);
            }
        }

        if (entity instanceof PathfinderMob) {
            PathfinderMob pathfinderMob = (PathfinderMob) entity;

            if (RetoldFactionMembers.getFaction(pathfinderMob) != null) {
                addRetaliationGoal(pathfinderMob);
            }
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

        if (!RetoldFactionRelations.hasPotentialFactionTarget(mob)) {
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

    private static void addFactionTargetGoal(Mob mob) {
        mob.targetSelector.addGoal(
                2,
                new NearestAttackableTargetGoal<>(
                        mob,
                        LivingEntity.class,
                        10,
                        true,
                        false,
                        (target, level) -> isValidFactionTarget(mob, target)
                )
        );
    }

    private static boolean isValidFactionTarget(Mob mob, LivingEntity target) {
        if (target == mob) {
            return false;
        }

        if (!target.isAlive()) {
            return false;
        }

        if (mob.level() != target.level()) {
            return false;
        }

        if (mob.distanceToSqr(target) > FORCED_TARGET_RADIUS_BLOCKS * FORCED_TARGET_RADIUS_BLOCKS) {
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

    private static void addRetaliationGoal(PathfinderMob mob) {
        mob.targetSelector.addGoal(1, new HurtByTargetGoal(mob));
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
        AABB area = mob.getBoundingBox().inflate(FORCED_TARGET_RADIUS_BLOCKS);

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
        if (!target.isAlive()) {
            return false;
        }

        if (mob.level() != target.level()) {
            return false;
        }

        RetoldFaction targetFaction = RetoldFactionMembers.getFaction(target);

        if (targetFaction == RetoldFaction.PLAYER) {
            return false;
        }

        if (!RetoldFactionRelations.shouldAttack(mob, target)) {
            return false;
        }

        return mob.distanceToSqr(target) <= FORCED_TARGET_RELEASE_DISTANCE_SQUARED;
    }

    private static void forceTarget(Mob mob, LivingEntity target, long gameTime) {
        mob.setTarget(target);
        mob.setAggressive(true);
        mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
        LAST_FORCED_TARGET_REFRESH_AT.put(mob, gameTime);
    }

    private static void clearForcedTarget(Mob mob) {
        LivingEntity forcedTarget = FORCED_TARGETS.remove(mob);

        NEXT_FORCED_TARGET_CHECK_AT.remove(mob);
        LAST_FORCED_TARGET_REFRESH_AT.remove(mob);

        if (forcedTarget != null && mob.getTarget() == forcedTarget) {
            mob.setTarget(null);
            mob.setAggressive(false);
        }
    }

    private static boolean isInRaid(ServerLevel level, Entity entity) {
        Raid raid = level.getRaidAt(entity.blockPosition());

        return raid != null && raid.isActive();
    }
}