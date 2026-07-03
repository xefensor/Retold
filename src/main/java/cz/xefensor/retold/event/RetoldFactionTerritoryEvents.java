package cz.xefensor.retold.event;

import cz.xefensor.retold.faction.RetoldFaction;
import cz.xefensor.retold.faction.RetoldFactionMembers;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class RetoldFactionTerritoryEvents {
    private static final TagKey<Structure> NETHER_REMNANT_TERRITORY = TagKey.create(
            Registries.STRUCTURE,
            Identifier.fromNamespaceAndPath("retold", "nether_remnant_territory")
    );

    private static final int MOB_DECISION_INTERVAL_TICKS = 20;
    private static final int WARNING_NAVIGATION_REFRESH_TICKS = 10;

    private static final int WARNING_PULSE_INTERVAL_TICKS = 80;
    private static final int WARNING_PULSES_BEFORE_ATTACK = 5;

    private static final int STRUCTURE_SEARCH_RADIUS_CHUNKS = 6;
    private static final int TERRITORY_RADIUS_BLOCKS = 48;

    private static final int NOTICE_MOB_RADIUS_BLOCKS = 48;
    private static final int WARNING_START_RADIUS_BLOCKS = 12;
    private static final int ATTACK_CHAIN_RADIUS_BLOCKS = 48;

    private static final double ATTACK_TARGET_RELEASE_DISTANCE_SQUARED = 40.0D * 40.0D;

    private static final int BASE_WARNING_PARTICLE_COUNT = 2;
    private static final int WARNING_PARTICLE_COUNT_PER_PULSE = 2;

    private static final float BASE_WARNING_SOUND_VOLUME = 0.35F;
    private static final float WARNING_SOUND_VOLUME_PER_PULSE = 0.12F;
    private static final float WARNING_SOUND_PITCH = 0.85F;

    private static final double WARNING_APPROACH_SPEED = 0.85D;
    private static final double WARNING_APPROACH_STOP_DISTANCE_SQUARED = 5.0D * 5.0D;

    private static final int ATTACK_REFRESH_INTERVAL_TICKS = 100;

    private static final int TERRITORY_CACHE_TICKS = 100;
    private static final int MAX_TERRITORY_CACHE_SIZE = 4096;

    private static final Map<Entity, TerritoryMobState> MOB_STATES = new WeakHashMap<>();
    private static final Map<Long, TerritoryCacheEntry> TERRITORY_CACHE = new HashMap<>();

    private RetoldFactionTerritoryEvents() {
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
            clearMobState(mob);
            return;
        }

        ServerLevel level = (ServerLevel) mob.level();

        if (level.dimension() != Level.NETHER) {
            clearMobState(mob);
            return;
        }

        if (!isAngerableNetherRemnant(mob)) {
            clearMobState(mob);
            return;
        }

        long gameTime = level.getGameTime();
        TerritoryMobState state = MOB_STATES.computeIfAbsent(
                mob,
                ignored -> new TerritoryMobState(gameTime, mob.getId())
        );

        maintainContinuousBehavior(mob, state, gameTime);

        if (gameTime < state.nextDecisionAt) {
            return;
        }

        state.nextDecisionAt = gameTime + MOB_DECISION_INTERVAL_TICKS + Math.abs(mob.getId() % 5);

        updateMobTerritoryLogic(level, mob, state, gameTime);
    }

    private static void maintainContinuousBehavior(PathfinderMob mob, TerritoryMobState state, long gameTime) {
        if (state.hasStartedAttack) {
            return;
        }

        LivingEntity warningTarget = state.warningTarget;

        if (warningTarget == null || !isValidWarningTarget(mob, warningTarget)) {
            return;
        }

        stareAt(mob, warningTarget);

        if (gameTime >= state.nextNavigationRefreshAt) {
            approachWithoutAttacking(mob, warningTarget);
            state.nextNavigationRefreshAt = gameTime + WARNING_NAVIGATION_REFRESH_TICKS;
        }
    }

    private static void updateMobTerritoryLogic(
            ServerLevel level,
            PathfinderMob mob,
            TerritoryMobState state,
            long gameTime
    ) {
        if (!isNearNetherRemnantTerritory(level, mob, gameTime)) {
            clearMobState(mob);
            return;
        }

        if (state.hasStartedAttack) {
            updateAttackState(level, mob, state, gameTime);
            return;
        }

        LivingEntity currentWarningTarget = state.warningTarget;

        if (currentWarningTarget == null || !isValidWarningTarget(mob, currentWarningTarget)) {
            currentWarningTarget = findNearestIntruder(level, mob, gameTime);

            if (currentWarningTarget == null) {
                resetWarningState(state, gameTime);
                return;
            }

            state.warningTarget = currentWarningTarget;
            state.warningPulses = 0;
            state.nextWarningPulseAt = gameTime;
            state.warnedIntruders.clear();
        }

        if (!isCloseEnoughToCountWarning(mob, currentWarningTarget)) {
            state.warningPulses = 0;
            state.nextWarningPulseAt = gameTime;
            state.warnedIntruders.clear();
            return;
        }

        if (gameTime >= state.nextWarningPulseAt) {
            playWarningEffects(level, mob, state.warningPulses);

            state.warningPulses++;
            state.nextWarningPulseAt = gameTime + WARNING_PULSE_INTERVAL_TICKS;

            if (state.warningPulses >= WARNING_PULSES_BEFORE_ATTACK) {
                markNearbyIntrudersAsWarned(level, mob, state, gameTime);

                LivingEntity attackTarget = findNearestWarnedIntruderForAttack(level, mob, state, gameTime);

                if (attackTarget == null) {
                    resetWarningState(state, gameTime);
                    return;
                }

                state.hasStartedAttack = true;
                state.attackTarget = attackTarget;
                state.warningTarget = attackTarget;

                applyAttackTarget(mob, attackTarget, gameTime);
            }
        }
    }

    private static void updateAttackState(
            ServerLevel level,
            PathfinderMob mob,
            TerritoryMobState state,
            long gameTime
    ) {
        LivingEntity attackTarget = state.attackTarget;

        if (attackTarget == null || !isValidWarnedAttackTarget(level, mob, state, attackTarget, gameTime)) {
            attackTarget = findNearestWarnedIntruderForAttack(level, mob, state, gameTime);

            if (attackTarget == null) {
                // No already-warned enemies remain.
                // New arrivals must be warned first.
                returnToWarningMode(mob, state, gameTime);
                return;
            }

            state.attackTarget = attackTarget;
            state.warningTarget = attackTarget;

            applyAttackTarget(mob, attackTarget, gameTime);
            return;
        }

        if (gameTime - state.lastAttackRefreshAt >= ATTACK_REFRESH_INTERVAL_TICKS) {
            applyAttackTarget(mob, attackTarget, gameTime);
        }
    }

    private static LivingEntity findNearestIntruder(
            ServerLevel level,
            PathfinderMob mob,
            long gameTime
    ) {
        AABB area = mob.getBoundingBox().inflate(NOTICE_MOB_RADIUS_BLOCKS);

        List<LivingEntity> candidates = level.getEntitiesOfClass(
                LivingEntity.class,
                area,
                target -> isPossibleIntruder(level, mob, target, gameTime)
        );

        LivingEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (LivingEntity candidate : candidates) {
            double distance = mob.distanceToSqr(candidate);

            if (distance < nearestDistance) {
                nearest = candidate;
                nearestDistance = distance;
            }
        }

        return nearest;
    }

    private static void markNearbyIntrudersAsWarned(
            ServerLevel level,
            PathfinderMob mob,
            TerritoryMobState state,
            long gameTime
    ) {
        AABB area = mob.getBoundingBox().inflate(WARNING_START_RADIUS_BLOCKS);

        List<LivingEntity> candidates = level.getEntitiesOfClass(
                LivingEntity.class,
                area,
                target -> isPossibleIntruder(level, mob, target, gameTime)
        );

        state.warnedIntruders.clear();

        for (LivingEntity candidate : candidates) {
            state.warnedIntruders.add(candidate);
        }

        if (state.warningTarget != null
                && isPossibleIntruder(level, mob, state.warningTarget, gameTime)
                && isCloseEnoughToCountWarning(mob, state.warningTarget)) {
            state.warnedIntruders.add(state.warningTarget);
        }
    }

    private static LivingEntity findNearestWarnedIntruderForAttack(
            ServerLevel level,
            PathfinderMob mob,
            TerritoryMobState state,
            long gameTime
    ) {
        purgeInvalidWarnedIntruders(level, mob, state, gameTime);

        LivingEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (LivingEntity candidate : state.warnedIntruders) {
            if (!isValidWarnedAttackTarget(level, mob, state, candidate, gameTime)) {
                continue;
            }

            if (mob.distanceToSqr(candidate) > ATTACK_CHAIN_RADIUS_BLOCKS * ATTACK_CHAIN_RADIUS_BLOCKS) {
                continue;
            }

            double distance = mob.distanceToSqr(candidate);

            if (distance < nearestDistance) {
                nearest = candidate;
                nearestDistance = distance;
            }
        }

        return nearest;
    }

    private static void purgeInvalidWarnedIntruders(
            ServerLevel level,
            PathfinderMob mob,
            TerritoryMobState state,
            long gameTime
    ) {
        state.warnedIntruders.removeIf(
                target -> !isValidWarnedAttackTarget(level, mob, state, target, gameTime)
        );
    }

    private static boolean isValidWarnedAttackTarget(
            ServerLevel level,
            PathfinderMob mob,
            TerritoryMobState state,
            LivingEntity target,
            long gameTime
    ) {
        return state.warnedIntruders.contains(target)
                && isValidAttackTarget(mob, target)
                && isNearNetherRemnantTerritory(level, target, gameTime);
    }

    private static boolean isPossibleIntruder(
            ServerLevel level,
            PathfinderMob mob,
            LivingEntity target,
            long gameTime
    ) {
        if (target == mob) {
            return false;
        }

        if (!canTriggerTerritoryAnger(target)) {
            return false;
        }

        return isNearNetherRemnantTerritory(level, target, gameTime);
    }

    private static boolean canTriggerTerritoryAnger(LivingEntity intruder) {
        if (!intruder.isAlive()) {
            return false;
        }

        if (intruder instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer) intruder;

            if (player.isCreative() || player.isSpectator()) {
                return false;
            }
        }

        return !RetoldFactionMembers.isMemberOf(intruder, RetoldFaction.NETHER_REMNANTS);
    }

    private static boolean isValidWarningTarget(PathfinderMob mob, LivingEntity target) {
        if (!target.isAlive()) {
            return false;
        }

        if (mob.level() != target.level()) {
            return false;
        }

        if (!canTriggerTerritoryAnger(target)) {
            return false;
        }

        return mob.distanceToSqr(target) <= NOTICE_MOB_RADIUS_BLOCKS * NOTICE_MOB_RADIUS_BLOCKS;
    }

    private static boolean isValidAttackTarget(PathfinderMob mob, LivingEntity target) {
        if (!target.isAlive()) {
            return false;
        }

        if (mob.level() != target.level()) {
            return false;
        }

        if (!canTriggerTerritoryAnger(target)) {
            return false;
        }

        return mob.distanceToSqr(target) <= ATTACK_TARGET_RELEASE_DISTANCE_SQUARED;
    }

    private static boolean isCloseEnoughToCountWarning(PathfinderMob mob, LivingEntity target) {
        return mob.distanceToSqr(target) <= WARNING_START_RADIUS_BLOCKS * WARNING_START_RADIUS_BLOCKS;
    }

    private static boolean isNearNetherRemnantTerritory(ServerLevel level, Entity entity, long gameTime) {
        long chunkKey = chunkKey(entity);
        TerritoryCacheEntry cached = TERRITORY_CACHE.get(chunkKey);

        if (cached != null && gameTime < cached.expiresAt) {
            return cached.isNearTerritory;
        }

        boolean isNearTerritory = computeIsNearNetherRemnantTerritory(level, entity);

        TERRITORY_CACHE.put(
                chunkKey,
                new TerritoryCacheEntry(isNearTerritory, gameTime + TERRITORY_CACHE_TICKS)
        );

        if (TERRITORY_CACHE.size() > MAX_TERRITORY_CACHE_SIZE) {
            clearExpiredTerritoryCache(gameTime);
        }

        return isNearTerritory;
    }

    private static boolean computeIsNearNetherRemnantTerritory(ServerLevel level, Entity entity) {
        if (isInsideNetherRemnantStructure(level, entity)) {
            return true;
        }

        var structurePos = level.findNearestMapStructure(
                NETHER_REMNANT_TERRITORY,
                entity.blockPosition(),
                STRUCTURE_SEARCH_RADIUS_CHUNKS,
                false
        );

        if (structurePos == null) {
            return false;
        }

        return isWithinHorizontalDistance(
                entity.blockPosition().getX(),
                entity.blockPosition().getZ(),
                structurePos.getX(),
                structurePos.getZ(),
                TERRITORY_RADIUS_BLOCKS
        );
    }

    private static boolean isInsideNetherRemnantStructure(ServerLevel level, Entity entity) {
        StructureStart structureStart = level.structureManager().getStructureWithPieceAt(
                entity.blockPosition(),
                NETHER_REMNANT_TERRITORY
        );

        return structureStart != null && structureStart.isValid();
    }

    private static void clearExpiredTerritoryCache(long gameTime) {
        TERRITORY_CACHE.entrySet().removeIf(entry -> gameTime >= entry.getValue().expiresAt);
    }

    private static long chunkKey(Entity entity) {
        int chunkX = entity.blockPosition().getX() >> 4;
        int chunkZ = entity.blockPosition().getZ() >> 4;

        return ((long) chunkX & 4294967295L)
                | (((long) chunkZ & 4294967295L) << 32);
    }

    private static void playWarningEffects(ServerLevel level, PathfinderMob mob, int warningPulses) {
        int particleCount = BASE_WARNING_PARTICLE_COUNT
                + warningPulses * WARNING_PARTICLE_COUNT_PER_PULSE;

        float volume = BASE_WARNING_SOUND_VOLUME
                + warningPulses * WARNING_SOUND_VOLUME_PER_PULSE;

        spawnAngryParticles(level, mob, particleCount);
        playWarningSound(level, mob, volume);
    }

    private static void stareAt(PathfinderMob mob, LivingEntity target) {
        mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
    }

    private static void approachWithoutAttacking(PathfinderMob mob, LivingEntity target) {
        double distanceSquared = mob.distanceToSqr(target);

        if (distanceSquared <= WARNING_APPROACH_STOP_DISTANCE_SQUARED) {
            mob.getNavigation().stop();
            return;
        }

        mob.getNavigation().moveTo(target, WARNING_APPROACH_SPEED);
    }

    private static void applyAttackTarget(PathfinderMob mob, LivingEntity target, long gameTime) {
        mob.setTarget(target);
        mob.setAggressive(true);

        if (mob instanceof AbstractPiglin) {
            AbstractPiglin piglin = (AbstractPiglin) mob;

            piglin.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, target);
            piglin.getBrain().setMemory(MemoryModuleType.ANGRY_AT, target.getUUID());
        }

        TerritoryMobState state = MOB_STATES.get(mob);

        if (state != null) {
            state.lastAttackRefreshAt = gameTime;
        }
    }

    private static boolean isAngerableNetherRemnant(PathfinderMob mob) {
        return mob.isAlive()
                && RetoldFactionMembers.isMemberOf(mob, RetoldFaction.NETHER_REMNANTS);
    }

    private static void spawnAngryParticles(ServerLevel level, LivingEntity entity, int count) {
        level.sendParticles(
                ParticleTypes.ANGRY_VILLAGER,
                entity.getX(),
                entity.getY() + entity.getBbHeight() + 0.25D,
                entity.getZ(),
                count,
                0.35D,
                0.18D,
                0.35D,
                0.0D
        );
    }

    private static void playWarningSound(ServerLevel level, PathfinderMob mob, float volume) {
        level.playSound(
                null,
                mob.getX(),
                mob.getY(),
                mob.getZ(),
                SoundEvents.PIGLIN_ANGRY,
                SoundSource.HOSTILE,
                volume,
                WARNING_SOUND_PITCH
        );
    }

    private static void resetWarningState(TerritoryMobState state, long gameTime) {
        state.warningTarget = null;
        state.attackTarget = null;
        state.warningPulses = 0;
        state.hasStartedAttack = false;
        state.nextWarningPulseAt = gameTime;
        state.warnedIntruders.clear();
    }

    private static void returnToWarningMode(PathfinderMob mob, TerritoryMobState state, long gameTime) {
        LivingEntity oldAttackTarget = state.attackTarget;

        if (oldAttackTarget != null && mob.getTarget() == oldAttackTarget) {
            mob.setTarget(null);
            mob.setAggressive(false);

            if (mob instanceof AbstractPiglin) {
                AbstractPiglin piglin = (AbstractPiglin) mob;

                piglin.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
                piglin.getBrain().eraseMemory(MemoryModuleType.ANGRY_AT);
            }
        }

        resetWarningState(state, gameTime);
    }

    private static void clearMobState(PathfinderMob mob) {
        MOB_STATES.remove(mob);
    }

    private static boolean isWithinHorizontalDistance(
            int firstX,
            int firstZ,
            int secondX,
            int secondZ,
            int maxDistance
    ) {
        long dx = firstX - secondX;
        long dz = firstZ - secondZ;
        long maxDistanceSquared = (long) maxDistance * maxDistance;

        return dx * dx + dz * dz <= maxDistanceSquared;
    }

    private static final class TerritoryMobState {
        private LivingEntity warningTarget;
        private LivingEntity attackTarget;
        private int warningPulses;
        private boolean hasStartedAttack;
        private long nextDecisionAt;
        private long nextWarningPulseAt;
        private long nextNavigationRefreshAt;
        private long lastAttackRefreshAt;

        private final Set<LivingEntity> warnedIntruders = Collections.newSetFromMap(
                new WeakHashMap<LivingEntity, Boolean>()
        );

        private TerritoryMobState(long gameTime, int entityId) {
            this.warningTarget = null;
            this.attackTarget = null;
            this.warningPulses = 0;
            this.hasStartedAttack = false;
            this.nextDecisionAt = gameTime + Math.abs(entityId % 20);
            this.nextWarningPulseAt = gameTime;
            this.nextNavigationRefreshAt = gameTime;
            this.lastAttackRefreshAt = Long.MIN_VALUE / 2;
        }
    }

    private static final class TerritoryCacheEntry {
        private final boolean isNearTerritory;
        private final long expiresAt;

        private TerritoryCacheEntry(boolean isNearTerritory, long expiresAt) {
            this.isNearTerritory = isNearTerritory;
            this.expiresAt = expiresAt;
        }
    }
}