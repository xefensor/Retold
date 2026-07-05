package cz.xefensor.retold.event;

import cz.xefensor.retold.faction.RetoldFaction;
import cz.xefensor.retold.faction.RetoldFactionMembers;
import cz.xefensor.retold.faction.RetoldFactionRelations;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.CrossbowAttackMob;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

public final class RetoldFactionTerritoryEvents {
    private static final int MOB_DECISION_INTERVAL_TICKS = 20;

    private static final int STRUCTURE_SEARCH_RADIUS_CHUNKS = 12;
    private static final double TERRITORY_RADIUS_BLOCKS = 64.0D;

    private static final int TERRITORY_PIECE_PADDING_CHUNKS = 2;
    private static final int TERRITORY_PIECE_SCAN_STEP_BLOCKS = 8;
    private static final int TERRITORY_PIECE_VERTICAL_SCAN_STEP_BLOCKS = 8;
    private static final int TERRITORY_PIECE_VERTICAL_SCAN_RANGE_BLOCKS = 24;

    private static final double NOTICE_MOB_RADIUS_BLOCKS = 48.0D;
    private static final double ATTACK_CHAIN_RADIUS_BLOCKS = 48.0D;
    private static final double ATTACK_TARGET_RELEASE_DISTANCE_SQUARED = 40.0D * 40.0D;

    private static final int BASE_WARNING_PARTICLE_COUNT = 2;
    private static final int WARNING_PARTICLE_COUNT_PER_PULSE = 2;

    private static final float BASE_WARNING_SOUND_VOLUME = 0.35F;
    private static final float WARNING_SOUND_VOLUME_PER_PULSE = 0.12F;
    private static final float WARNING_SOUND_PITCH = 0.85F;

    private static final int TARGET_RECHECK_INTERVAL_TICKS = 35;
    private static final int ATTACK_REFRESH_INTERVAL_TICKS = 100;

    private static final int WARNING_CROSSBOW_CHARGE_TICKS = 28;
    private static final int WARNING_LOST_SIGHT_MEMORY_TICKS = 100;
    private static final int WARNING_FORMATION_RECHECK_INTERVAL_TICKS = 45;
    private static final int MIN_FINAL_WARNING_TICKS_BEFORE_ATTACK = 40;

    private static final double WARNING_POSITION_REPATH_DISTANCE_SQUARED = 2.5D * 2.5D;
    private static final double WARNING_TARGET_DRIFT_REPATH_DISTANCE_SQUARED = 4.0D * 4.0D;
    private static final double WARNING_POSITION_STOP_DISTANCE_SQUARED = 1.75D * 1.75D;
    private static final int WARNING_MIN_REPATH_INTERVAL_TICKS = 30;

    private static final int TERRITORY_CACHE_TICKS = 100;
    private static final int MAX_TERRITORY_CACHE_SIZE = 4096;

    private static final Set<Identifier> RANGED_WARNING_ENTITY_IDS = Set.of(
            id("pillager"),
            id("blaze"),
            id("evoker"),
            id("illusioner"),
            id("witch")
    );

    private static final Map<RetoldFaction, TerritoryConfig> TERRITORY_CONFIGS =
            new EnumMap<>(RetoldFaction.class);

    private static final Map<PathfinderMob, TerritoryMobState> MOB_STATES =
            Collections.synchronizedMap(new WeakHashMap<>());

    private static final Map<TerritoryCacheKey, TerritoryCacheEntry> TERRITORY_CACHE =
            new HashMap<>();

    static {
        registerTerritory(
                RetoldFaction.NETHER_REMNANTS,
                "nether_remnant_territory",
                SoundEvents.PIGLIN_ANGRY,
                Level.NETHER
        );

        registerTerritory(
                RetoldFaction.ILLAGERS,
                "illager_territory",
                SoundEvents.PILLAGER_AMBIENT,
                null
        );
    }

    private RetoldFactionTerritoryEvents() {
    }

    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();

        if (!(entity instanceof PathfinderMob mob)) {
            return;
        }

        if (!(mob.level() instanceof ServerLevel level)) {
            return;
        }

        TerritoryConfig config = getTerritoryConfigForMob(mob);

        if (config == null) {
            MOB_STATES.remove(mob);
            return;
        }

        long gameTime = level.getGameTime();

        RetoldFactionTargetMemory.cleanupTargetState(mob);

        if (!isInAllowedDimension(level, config)) {
            clearMobState(mob);
            return;
        }

        if (config.faction == RetoldFaction.ILLAGERS && isInRaid(level, mob)) {
            clearMobState(mob);
            return;
        }

        if (!isNearFactionTerritory(level, mob, config, gameTime)) {
            clearMobState(mob);
            return;
        }

        if (gameTime % MOB_DECISION_INTERVAL_TICKS != Math.floorMod(mob.getId(), MOB_DECISION_INTERVAL_TICKS)) {
            TerritoryMobState state = MOB_STATES.get(mob);

            if (state != null) {
                maintainContinuousBehavior(level, mob, state, config, gameTime);
            }

            return;
        }

        TerritoryMobState state = MOB_STATES.computeIfAbsent(
                mob,
                ignored -> new TerritoryMobState(mob.getId())
        );

        updateMobTerritoryLogic(level, mob, state, config, gameTime);
    }

    public static boolean shouldBlockTargetDuringWarning(PathfinderMob mob, LivingEntity target) {
        if (mob.level().isClientSide()) {
            return false;
        }

        if (!(mob.level() instanceof ServerLevel level)) {
            return false;
        }

        TerritoryConfig config = getTerritoryConfigForMob(mob);

        if (config == null) {
            return false;
        }

        if (!isInAllowedDimension(level, config)) {
            return false;
        }

        if (config.faction == RetoldFaction.ILLAGERS && isInRaid(level, mob)) {
            return false;
        }

        long gameTime = level.getGameTime();

        if (!isNearFactionTerritory(level, mob, config, gameTime)) {
            return false;
        }

        if (!isPossibleIntruder(level, mob, target, config, gameTime)) {
            return false;
        }

        TerritoryMobState state = MOB_STATES.get(mob);

        return state == null || !state.hasStartedAttack;
    }

    public static RetoldTerritoryContext getTerritoryContextAt(ServerLevel level, BlockPos pos) {
        long gameTime = level.getGameTime();

        for (TerritoryConfig config : TERRITORY_CONFIGS.values()) {
            if (!isInAllowedDimension(level, config)) {
                continue;
            }

            if (!isNearFactionTerritory(level, pos, config, gameTime)) {
                continue;
            }

            BlockPos structurePos = findFactionTerritoryAnchor(level, pos, config);

            if (structurePos == null) {
                continue;
            }

            return new RetoldTerritoryContext(
                    config.faction,
                    getDimensionId(level),
                    structurePos.getX(),
                    structurePos.getZ()
            );
        }

        return null;
    }

    private static String getDimensionId(ServerLevel level) {
        return level.dimension().toString();
    }

    public static RetoldFaction getTerritoryFactionAt(ServerLevel level, BlockPos pos) {
        RetoldTerritoryContext context = getTerritoryContextAt(level, pos);
        return context == null ? null : context.faction();
    }

    public static boolean hasWitnessForIllegalAction(
            ServerLevel level,
            RetoldFaction faction,
            ServerPlayer player,
            BlockPos actionPos
    ) {
        if (faction == null || player == null) {
            return false;
        }

        List<PathfinderMob> possibleWitnesses = level.getEntitiesOfClass(
                PathfinderMob.class,
                player.getBoundingBox().inflate(NOTICE_MOB_RADIUS_BLOCKS),
                mob -> mob.isAlive()
                        && RetoldFactionMembers.getFaction(mob) == faction
                        && mob.distanceToSqr(
                        actionPos.getX() + 0.5D,
                        actionPos.getY() + 0.5D,
                        actionPos.getZ() + 0.5D
                ) <= NOTICE_MOB_RADIUS_BLOCKS * NOTICE_MOB_RADIUS_BLOCKS
        );

        for (PathfinderMob witness : possibleWitnesses) {
            if (witness.getSensing().hasLineOfSight(player)) {
                return true;
            }
        }

        return false;
    }

    public static void alertWitnessesAfterIllegalAction(
            ServerLevel level,
            RetoldFaction faction,
            ServerPlayer player,
            BlockPos actionPos
    ) {
        if (faction == null || player == null) {
            return;
        }

        List<PathfinderMob> witnesses = level.getEntitiesOfClass(
                PathfinderMob.class,
                player.getBoundingBox().inflate(NOTICE_MOB_RADIUS_BLOCKS),
                mob -> mob.isAlive()
                        && RetoldFactionMembers.getFaction(mob) == faction
                        && mob.distanceToSqr(
                        actionPos.getX() + 0.5D,
                        actionPos.getY() + 0.5D,
                        actionPos.getZ() + 0.5D
                ) <= NOTICE_MOB_RADIUS_BLOCKS * NOTICE_MOB_RADIUS_BLOCKS
                        && mob.getSensing().hasLineOfSight(player)
        );

        long gameTime = level.getGameTime();

        for (PathfinderMob witness : witnesses) {
            TerritoryConfig config = getTerritoryConfigForMob(witness);

            if (config == null || config.faction != faction) {
                continue;
            }

            if (!isInAllowedDimension(level, config)) {
                continue;
            }

            if (!isNearFactionTerritory(level, witness, config, gameTime)) {
                continue;
            }

            TerritoryMobState state = MOB_STATES.computeIfAbsent(
                    witness,
                    ignored -> new TerritoryMobState(witness.getId())
            );

            state.territoryContext = getTerritoryContextAt(level, witness.blockPosition());

            if (state.territoryContext == null || state.territoryContext.faction() != faction) {
                continue;
            }

            if (state.hasStartedAttack) {
                continue;
            }

            setWarningTarget(state, witness, player, gameTime);

            if (RetoldIntruderReputation.shouldAttack(state.territoryContext, player)) {
                startAttackOnTarget(
                        level,
                        witness,
                        state,
                        config,
                        player,
                        gameTime,
                        RetoldTargetSource.TERRITORY_ATTACK
                );
            }
        }
    }

    private static void updateMobTerritoryLogic(
            ServerLevel level,
            PathfinderMob mob,
            TerritoryMobState state,
            TerritoryConfig config,
            long gameTime
    ) {
        RetoldTerritoryContext territoryContext = getTerritoryContextAt(level, mob.blockPosition());

        if (territoryContext == null || territoryContext.faction() != config.faction) {
            clearMobState(mob);
            return;
        }

        state.territoryContext = territoryContext;

        if (state.hasStartedAttack) {
            updateAttackState(level, mob, state, config, gameTime);
            return;
        }

        if (tryAdoptRetaliationTarget(level, mob, state, config, gameTime)) {
            updateAttackState(level, mob, state, config, gameTime);
            return;
        }

        if (tryAdoptOwnedCombatTarget(level, mob, state, config, gameTime)) {
            updateAttackState(level, mob, state, config, gameTime);
            return;
        }

        if (config.faction == RetoldFaction.ILLAGERS) {
            suppressExistingTargetDuringWarning(level, mob, config, gameTime);
        } else if (tryAdoptExistingAttackTarget(level, mob, state, config, gameTime)) {
            updateAttackState(level, mob, state, config, gameTime);
            return;
        }

        updateWarningTarget(level, mob, state, config, gameTime);

        LivingEntity warningTarget = state.warningTarget;

        if (warningTarget == null) {
            stopWarningPose(mob);
            resetWarningState(state, gameTime);
            return;
        }

        if (RetoldIntruderReputation.getWarningLevel(state.territoryContext, warningTarget) == RetoldWarningLevel.ATTACK) {
            startAttackOnTarget(
                    level,
                    mob,
                    state,
                    config,
                    warningTarget,
                    gameTime,
                    RetoldTargetSource.TERRITORY_ATTACK
            );

            signalNearbyWarnedGuardsToAttack(level, mob, config, gameTime);
            return;
        }

        maintainContinuousBehavior(level, mob, state, config, gameTime);

        if (!canCountWarningPulse(level, mob, warningTarget, config, state.territoryContext, gameTime)) {
            state.nextWarningPulseAt = Math.max(state.nextWarningPulseAt, gameTime + 10L);
            return;
        }

        if (gameTime < state.nextWarningPulseAt) {
            return;
        }

        markVisibleWarnedIntruders(level, mob, state, config, gameTime);

        RetoldWarningLevel warningLevelBeforeGain = RetoldIntruderReputation.getWarningLevel(
                state.territoryContext,
                warningTarget
        );

        updateFinalWarningTimer(state, warningLevelBeforeGain, gameTime);
        playWarningEffects(level, mob, config, warningLevelBeforeGain);

        int suspicionGain = getWarningSuspicionGain(warningLevelBeforeGain);

        if (suspicionGain > 0) {
            RetoldIntruderReputation.addVisibleWarningSuspicion(
                    state.territoryContext,
                    warningTarget,
                    suspicionGain,
                    gameTime
            );
        }

        if (isTooCloseDuringWarning(mob, warningTarget)) {
            RetoldIntruderReputation.addTooCloseSuspicion(
                    state.territoryContext,
                    warningTarget,
                    gameTime
            );
        }

        RetoldWarningLevel warningLevelAfterGain = RetoldIntruderReputation.getWarningLevel(
                state.territoryContext,
                warningTarget
        );

        updateFinalWarningTimer(state, warningLevelAfterGain, gameTime);

        state.warningPulses++;
        state.nextWarningPulseAt = gameTime + getWarningPulseInterval(mob, warningLevelAfterGain);

        if (canStartTerritoryAttack(state, warningLevelAfterGain, gameTime)) {
            startAttackOnTarget(
                    level,
                    mob,
                    state,
                    config,
                    warningTarget,
                    gameTime,
                    RetoldTargetSource.TERRITORY_ATTACK
            );

            signalNearbyWarnedGuardsToAttack(level, mob, config, gameTime);
        }
    }

    private static void maintainContinuousBehavior(
            ServerLevel level,
            PathfinderMob mob,
            TerritoryMobState state,
            TerritoryConfig config,
            long gameTime
    ) {
        if (state.hasStartedAttack) {
            return;
        }

        if (state.territoryContext == null || state.territoryContext.faction() != config.faction) {
            state.territoryContext = getTerritoryContextAt(level, mob.blockPosition());
        }

        if (state.territoryContext == null || state.territoryContext.faction() != config.faction) {
            stopWarningPose(mob);
            return;
        }

        LivingEntity warningTarget = state.warningTarget;

        if (
                warningTarget == null
                        || !canMaintainWarningAwareness(level, mob, state, config, warningTarget, gameTime)
        ) {
            stopWarningPose(mob);
            return;
        }

        RetoldWarningLevel warningLevel = RetoldIntruderReputation.getWarningLevel(
                state.territoryContext,
                warningTarget
        );

        updateWarningPose(mob, warningTarget, warningLevel);

        if (canSeeTarget(mob, warningTarget)) {
            RetoldIntruderReputation.markSeen(
                    state.territoryContext,
                    warningTarget,
                    gameTime
            );

            faceTargetSmoothly(mob, warningTarget);
        } else {
            faceLastKnownWarningPosition(mob, state);
        }

        if (warningLevel == RetoldWarningLevel.NONE) {
            mob.getNavigation().stop();
            return;
        }

        WarningFormationSlot formationSlot = getWarningFormationSlot(
                level,
                mob,
                config,
                warningTarget
        );

        if (gameTime >= state.nextFormationRecheckAt) {
            if (state.warningFormationSlot != formationSlot.slotOffset) {
                state.warningFormationSlot = formationSlot.slotOffset;
                state.hasWarningMoveTarget = false;
            }

            state.nextFormationRecheckAt = gameTime + WARNING_FORMATION_RECHECK_INTERVAL_TICKS;
        }

        int focusCount = Math.max(0, formationSlot.totalGuards - 1);

        WarningMovementProfile profile = getWarningMovementProfile(
                mob,
                warningTarget,
                focusCount,
                warningLevel
        );

        updateWarningMovement(mob, warningTarget, state, profile, gameTime);
    }

    private static void updateWarningTarget(
            ServerLevel level,
            PathfinderMob mob,
            TerritoryMobState state,
            TerritoryConfig config,
            long gameTime
    ) {
        LivingEntity currentTarget = state.warningTarget;

        if (
                currentTarget == null
                        || !canMaintainWarningAwareness(level, mob, state, config, currentTarget, gameTime)
        ) {
            LivingEntity bestTarget = findBestWarningTarget(level, mob, config, gameTime);
            setWarningTarget(state, mob, bestTarget, gameTime);
            return;
        }

        if (gameTime < state.nextTargetRecheckAt) {
            return;
        }

        state.nextTargetRecheckAt = gameTime + TARGET_RECHECK_INTERVAL_TICKS + Math.floorMod(mob.getId(), 12);

        if (!canSeeTarget(mob, currentTarget)) {
            return;
        }

        LivingEntity bestTarget = findBestWarningTarget(level, mob, config, gameTime);

        if (bestTarget == null || bestTarget == currentTarget) {
            return;
        }

        int currentFocus = countNearbyFactionMobsFocusedOn(level, mob, config, currentTarget);
        int bestFocus = countNearbyFactionMobsFocusedOn(level, mob, config, bestTarget);

        if (currentFocus > bestFocus + 1) {
            setWarningTarget(state, mob, bestTarget, gameTime);
        }
    }

    private static void setWarningTarget(
            TerritoryMobState state,
            PathfinderMob mob,
            LivingEntity target,
            long gameTime
    ) {
        if (state.warningTarget == target) {
            return;
        }

        state.warningTarget = target;
        state.attackTarget = null;
        state.hasStartedAttack = false;
        state.firedPreparedWarningShot = false;
        state.finalWarningStartedAt = -1L;
        state.warningPulses = 0;
        state.nextWarningPulseAt = gameTime;
        state.nextTargetRecheckAt = gameTime + TARGET_RECHECK_INTERVAL_TICKS + Math.floorMod(mob.getId(), 12);
        state.nextWarningRepositionAt = gameTime;
        state.hasWarningMoveTarget = false;
        state.warningMoveTargetSlot = Integer.MIN_VALUE;
        state.nextWarningPathRefreshAt = gameTime;

        if (target != null) {
            state.warningMoveSide = getStableSide(mob, target);
            state.warningAnchorAngle = getAngleFromTargetToMob(mob, target);
            state.warningFormationSlot = 0;
            state.nextFormationRecheckAt = gameTime;

            rememberWarningTargetPosition(state, target, gameTime);

            if (state.territoryContext != null) {
                RetoldIntruderReputation.addTrespassSuspicion(
                        state.territoryContext,
                        target,
                        gameTime
                );
            }
        }
    }

    private static LivingEntity findBestWarningTarget(
            ServerLevel level,
            PathfinderMob mob,
            TerritoryConfig config,
            long gameTime
    ) {
        List<LivingEntity> candidates = level.getEntitiesOfClass(
                LivingEntity.class,
                mob.getBoundingBox().inflate(NOTICE_MOB_RADIUS_BLOCKS),
                target -> isPossibleIntruder(level, mob, target, config, gameTime)
                        && canSeeTarget(mob, target)
        );

        LivingEntity bestTarget = null;
        double bestScore = Double.MAX_VALUE;

        for (LivingEntity candidate : candidates) {
            int focusCount = countNearbyFactionMobsFocusedOn(level, mob, config, candidate);
            double distanceScore = mob.distanceToSqr(candidate) * 0.01D;
            double tieBreaker = getStableTieBreaker(mob, candidate);

            double score = focusCount * 1000.0D + distanceScore + tieBreaker;

            if (score < bestScore) {
                bestScore = score;
                bestTarget = candidate;
            }
        }

        return bestTarget;
    }

    private static int countNearbyFactionMobsFocusedOn(
            ServerLevel level,
            PathfinderMob mob,
            TerritoryConfig config,
            LivingEntity intruder
    ) {
        List<PathfinderMob> nearbyMobs = level.getEntitiesOfClass(
                PathfinderMob.class,
                mob.getBoundingBox().inflate(NOTICE_MOB_RADIUS_BLOCKS),
                other -> other != mob
                        && other.isAlive()
                        && RetoldFactionMembers.getFaction(other) == config.faction
        );

        int count = 0;

        for (PathfinderMob other : nearbyMobs) {
            if (isFocusedOnIntruder(other, intruder)) {
                count++;
            }
        }

        return count;
    }

    private static WarningFormationSlot getWarningFormationSlot(
            ServerLevel level,
            PathfinderMob mob,
            TerritoryConfig config,
            LivingEntity intruder
    ) {
        List<PathfinderMob> focusedGuards = level.getEntitiesOfClass(
                PathfinderMob.class,
                mob.getBoundingBox().inflate(NOTICE_MOB_RADIUS_BLOCKS),
                other -> other.isAlive()
                        && RetoldFactionMembers.getFaction(other) == config.faction
                        && isFocusedOnIntruder(other, intruder)
        );

        if (!focusedGuards.contains(mob)) {
            focusedGuards.add(mob);
        }

        focusedGuards.sort((first, second) -> Integer.compare(first.getId(), second.getId()));

        int index = focusedGuards.indexOf(mob);

        if (index < 0) {
            index = 0;
        }

        return new WarningFormationSlot(
                toCenteredFormationSlot(index),
                focusedGuards.size()
        );
    }

    private static boolean isFocusedOnIntruder(PathfinderMob mob, LivingEntity intruder) {
        TerritoryMobState state = MOB_STATES.get(mob);

        if (state != null) {
            if (state.warningTarget == intruder || state.attackTarget == intruder) {
                return true;
            }
        }

        return mob.getTarget() == intruder;
    }

    private static int toCenteredFormationSlot(int index) {
        if (index == 0) {
            return 0;
        }

        int distanceFromCenter = (index + 1) / 2;

        return index % 2 == 1
                ? -distanceFromCenter
                : distanceFromCenter;
    }

    private static double getAngleFromTargetToMob(PathfinderMob mob, LivingEntity target) {
        double dx = mob.getX() - target.getX();
        double dz = mob.getZ() - target.getZ();

        if (dx * dx + dz * dz < 0.0001D) {
            double stableAngle = Math.floorMod(mob.getId() * 37 + target.getId() * 19, 360);
            return Math.toRadians(stableAngle);
        }

        return Math.atan2(dz, dx);
    }

    private static void updateWarningMovement(
            PathfinderMob mob,
            LivingEntity target,
            TerritoryMobState state,
            WarningMovementProfile profile,
            long gameTime
    ) {
        double distanceToTargetSqr = mob.distanceToSqr(target);
        double minDistanceSqr = profile.minDistance * profile.minDistance;
        double maxDistanceSqr = profile.maxDistance * profile.maxDistance;

        boolean tooCloseToIntruder = distanceToTargetSqr < minDistanceSqr;
        boolean tooFarFromIntruder = distanceToTargetSqr > maxDistanceSqr;

        boolean needsNewMoveTarget = !state.hasWarningMoveTarget
                || state.warningMoveTargetSlot != state.warningFormationSlot
                || hasWarningTargetDrifted(target, state)
                || tooCloseToIntruder
                || tooFarFromIntruder;

        if (needsNewMoveTarget && gameTime >= state.nextWarningPathRefreshAt) {
            setWarningMoveTarget(mob, target, state, profile, gameTime);
        }

        if (!state.hasWarningMoveTarget) {
            return;
        }

        double distanceToMoveTargetSqr = mob.distanceToSqr(
                state.warningMoveTargetX,
                state.warningMoveTargetY,
                state.warningMoveTargetZ
        );

        if (
                !tooCloseToIntruder
                        && !tooFarFromIntruder
                        && distanceToMoveTargetSqr <= WARNING_POSITION_STOP_DISTANCE_SQUARED
        ) {
            mob.getNavigation().stop();
            return;
        }

        if (distanceToMoveTargetSqr > WARNING_POSITION_REPATH_DISTANCE_SQUARED) {
            mob.getNavigation().moveTo(
                    state.warningMoveTargetX,
                    state.warningMoveTargetY,
                    state.warningMoveTargetZ,
                    tooCloseToIntruder || tooFarFromIntruder ? profile.urgentSpeed : profile.speed
            );
        }
    }

    private static void setWarningMoveTarget(
            PathfinderMob mob,
            LivingEntity target,
            TerritoryMobState state,
            WarningMovementProfile profile,
            long gameTime
    ) {
        double slotOffset = state.warningFormationSlot * profile.sideAngle;
        slotOffset = clamp(slotOffset, -1.2D, 1.2D);

        double targetAngle = state.warningAnchorAngle + slotOffset;

        state.warningMoveTargetX = target.getX() + Math.cos(targetAngle) * profile.desiredDistance;
        state.warningMoveTargetY = target.getY();
        state.warningMoveTargetZ = target.getZ() + Math.sin(targetAngle) * profile.desiredDistance;

        state.warningMoveTargetSourceX = target.getX();
        state.warningMoveTargetSourceZ = target.getZ();
        state.warningMoveTargetSlot = state.warningFormationSlot;
        state.hasWarningMoveTarget = true;

        state.nextWarningPathRefreshAt = gameTime + WARNING_MIN_REPATH_INTERVAL_TICKS;
    }

    private static boolean hasWarningTargetDrifted(
            LivingEntity target,
            TerritoryMobState state
    ) {
        if (!state.hasWarningMoveTarget) {
            return true;
        }

        double dx = target.getX() - state.warningMoveTargetSourceX;
        double dz = target.getZ() - state.warningMoveTargetSourceZ;

        return dx * dx + dz * dz >= WARNING_TARGET_DRIFT_REPATH_DISTANCE_SQUARED;
    }

    private static WarningMovementProfile getWarningMovementProfile(
            PathfinderMob mob,
            LivingEntity target,
            int focusCount,
            RetoldWarningLevel warningLevel
    ) {
        boolean ranged = isRangedWarningMob(mob);
        boolean noticed = warningLevel == RetoldWarningLevel.NOTICED;
        boolean finalWarning = warningLevel == RetoldWarningLevel.FINAL_WARNING
                || warningLevel == RetoldWarningLevel.ATTACK;

        double mobWidth = Math.max(0.6D, mob.getBbWidth());
        double targetWidth = Math.max(0.6D, target.getBbWidth());

        if (noticed) {
            if (ranged) {
                double desiredDistance = clamp(
                        14.0D + mobWidth + targetWidth + focusCount * 0.4D,
                        12.0D,
                        19.0D
                );

                return new WarningMovementProfile(
                        desiredDistance,
                        desiredDistance - 3.0D,
                        desiredDistance + 4.0D,
                        0.45D,
                        0.65D,
                        0.25D,
                        70 + Math.floorMod(mob.getId(), 20)
                );
            }

            double desiredDistance = clamp(
                    8.0D + mobWidth + targetWidth + focusCount * 0.25D,
                    7.0D,
                    11.0D
            );

            return new WarningMovementProfile(
                    desiredDistance,
                    desiredDistance - 2.0D,
                    desiredDistance + 3.0D,
                    0.55D,
                    0.75D,
                    0.35D,
                    60 + Math.floorMod(mob.getId(), 20)
            );
        }

        if (ranged) {
            double desiredDistance = finalWarning
                    ? clamp(10.0D + mobWidth + targetWidth + focusCount * 0.45D, 9.0D, 14.0D)
                    : clamp(12.0D + mobWidth + targetWidth + focusCount * 0.6D, 10.0D, 17.0D);

            double band = finalWarning
                    ? clamp(1.4D + focusCount * 0.1D, 1.4D, 2.6D)
                    : clamp(2.4D + focusCount * 0.15D, 2.4D, 4.0D);

            double sideAngle = finalWarning
                    ? clamp(0.25D + focusCount * 0.03D, 0.25D, 0.5D)
                    : clamp(0.4D + focusCount * 0.04D, 0.4D, 0.75D);

            int repositionInterval = finalWarning
                    ? 50 + Math.floorMod(mob.getId(), 18)
                    : 36 + Math.floorMod(mob.getId(), 16);

            return new WarningMovementProfile(
                    desiredDistance,
                    Math.max(7.0D, desiredDistance - band),
                    desiredDistance + band,
                    finalWarning ? 0.55D : 0.72D,
                    finalWarning ? 0.75D : 0.92D,
                    sideAngle,
                    repositionInterval
            );
        }

        double desiredDistance = finalWarning
                ? clamp(2.7D + mobWidth + targetWidth + focusCount * 0.15D, 3.25D, 5.5D)
                : clamp(4.3D + mobWidth + targetWidth + focusCount * 0.25D, 4.75D, 7.75D);

        double band = finalWarning
                ? clamp(0.75D + focusCount * 0.05D, 0.75D, 1.4D)
                : clamp(1.2D + focusCount * 0.08D, 1.2D, 2.0D);

        double sideAngle = finalWarning
                ? clamp(0.45D + focusCount * 0.04D, 0.45D, 0.75D)
                : clamp(0.6D + focusCount * 0.05D, 0.6D, 0.95D);

        int repositionInterval = finalWarning
                ? 38 + Math.floorMod(mob.getId(), 14)
                : 28 + Math.floorMod(mob.getId(), 14);

        return new WarningMovementProfile(
                desiredDistance,
                Math.max(2.5D, desiredDistance - band),
                desiredDistance + band,
                finalWarning ? 0.82D : 0.86D,
                finalWarning ? 1.05D : 1.02D,
                sideAngle,
                repositionInterval
        );
    }

    private static boolean isRangedWarningMob(PathfinderMob mob) {
        Identifier entityId = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());

        if (RANGED_WARNING_ENTITY_IDS.contains(entityId)) {
            return true;
        }

        return isProjectileWeapon(mob.getMainHandItem())
                || isProjectileWeapon(mob.getOffhandItem());
    }

    private static boolean canCountWarningPulse(
            ServerLevel level,
            PathfinderMob mob,
            LivingEntity target,
            TerritoryConfig config,
            RetoldTerritoryContext territoryContext,
            long gameTime
    ) {
        if (territoryContext == null) {
            return false;
        }

        if (!isValidWarningTarget(level, mob, target, config, gameTime)) {
            return false;
        }

        if (!canSeeTarget(mob, target)) {
            return false;
        }

        int focusCount = countNearbyFactionMobsFocusedOn(level, mob, config, target);

        RetoldWarningLevel warningLevel = RetoldIntruderReputation.getWarningLevel(
                territoryContext,
                target
        );

        WarningMovementProfile profile = getWarningMovementProfile(
                mob,
                target,
                focusCount,
                warningLevel
        );

        double countDistance = profile.maxDistance + 1.5D;
        return mob.distanceToSqr(target) <= countDistance * countDistance;
    }

    private static int getWarningPulseInterval(
            PathfinderMob mob,
            RetoldWarningLevel warningLevel
    ) {
        int baseInterval = switch (warningLevel) {
            case NONE -> 140;
            case NOTICED -> 130;
            case WARNING -> 95;
            case FINAL_WARNING -> 60;
            case ATTACK -> 40;
        };

        return baseInterval + Math.floorMod(mob.getId(), 16);
    }

    private static int getWarningSuspicionGain(RetoldWarningLevel warningLevel) {
        return switch (warningLevel) {
            case NONE -> 0;
            case NOTICED -> 3;
            case WARNING -> 8;
            case FINAL_WARNING -> 13;
            case ATTACK -> 0;
        };
    }

    private static boolean isTooCloseDuringWarning(PathfinderMob mob, LivingEntity target) {
        return mob.distanceToSqr(target) <= 4.0D * 4.0D;
    }

    private static void updateFinalWarningTimer(
            TerritoryMobState state,
            RetoldWarningLevel warningLevel,
            long gameTime
    ) {
        if (warningLevel == RetoldWarningLevel.FINAL_WARNING || warningLevel == RetoldWarningLevel.ATTACK) {
            if (state.finalWarningStartedAt < 0L) {
                state.finalWarningStartedAt = gameTime;
            }

            return;
        }

        state.finalWarningStartedAt = -1L;
    }

    private static boolean canStartTerritoryAttack(
            TerritoryMobState state,
            RetoldWarningLevel warningLevel,
            long gameTime
    ) {
        if (warningLevel != RetoldWarningLevel.ATTACK) {
            return false;
        }

        if (state.finalWarningStartedAt < 0L) {
            return false;
        }

        return gameTime - state.finalWarningStartedAt >= MIN_FINAL_WARNING_TICKS_BEFORE_ATTACK;
    }

    private static void markVisibleWarnedIntruders(
            ServerLevel level,
            PathfinderMob mob,
            TerritoryMobState state,
            TerritoryConfig config,
            long gameTime
    ) {
        LivingEntity warningTarget = state.warningTarget;

        if (warningTarget != null && isValidWarningTarget(level, mob, warningTarget, config, gameTime)) {
            state.warnedIntruders.add(warningTarget.getUUID());
        }

        List<LivingEntity> nearbyIntruders = level.getEntitiesOfClass(
                LivingEntity.class,
                mob.getBoundingBox().inflate(NOTICE_MOB_RADIUS_BLOCKS),
                target -> isPossibleIntruder(level, mob, target, config, gameTime)
                        && canSeeTarget(mob, target)
                        && canCountWarningPulse(level, mob, target, config, state.territoryContext, gameTime)
        );

        for (LivingEntity intruder : nearbyIntruders) {
            state.warnedIntruders.add(intruder.getUUID());
        }
    }

    private static void playWarningEffects(
            ServerLevel level,
            PathfinderMob mob,
            TerritoryConfig config,
            RetoldWarningLevel warningLevel
    ) {
        int intensity = getWarningIntensity(warningLevel);

        int particleCount = BASE_WARNING_PARTICLE_COUNT + intensity * WARNING_PARTICLE_COUNT_PER_PULSE;
        float volume = BASE_WARNING_SOUND_VOLUME + intensity * WARNING_SOUND_VOLUME_PER_PULSE;

        level.sendParticles(
                ParticleTypes.ANGRY_VILLAGER,
                mob.getX(),
                mob.getEyeY() + 0.25D,
                mob.getZ(),
                particleCount,
                0.35D,
                0.25D,
                0.35D,
                0.01D
        );

        level.playSound(
                null,
                mob.blockPosition(),
                config.warningSound,
                SoundSource.HOSTILE,
                volume,
                WARNING_SOUND_PITCH
        );
    }

    private static int getWarningIntensity(RetoldWarningLevel warningLevel) {
        return switch (warningLevel) {
            case NONE -> 0;
            case NOTICED -> 1;
            case WARNING -> 2;
            case FINAL_WARNING -> 4;
            case ATTACK -> 5;
        };
    }

    private static void updateWarningPose(
            PathfinderMob mob,
            LivingEntity warningTarget,
            RetoldWarningLevel warningLevel
    ) {
        if (warningLevel == RetoldWarningLevel.NONE || warningLevel == RetoldWarningLevel.NOTICED) {
            stopWarningPose(mob);
            return;
        }

        RetoldFactionTargetGuards.setAggressiveIgnoringGuard(mob, true);

        if (warningLevel != RetoldWarningLevel.FINAL_WARNING && warningLevel != RetoldWarningLevel.ATTACK) {
            return;
        }

        InteractionHand weaponHand = getProjectileWeaponHand(mob);

        if (weaponHand == null) {
            return;
        }

        ItemStack weaponStack = mob.getItemInHand(weaponHand);

        if (isCrossbowWeapon(weaponStack)) {
            updateWarningCrossbowPose(mob, weaponHand, weaponStack);
            return;
        }

        if (!mob.isUsingItem()) {
            mob.startUsingItem(weaponHand);
        }
    }

    private static void updateWarningCrossbowPose(
            PathfinderMob mob,
            InteractionHand weaponHand,
            ItemStack crossbowStack
    ) {
        if (isCrossbowCharged(crossbowStack)) {
            if (mob instanceof CrossbowAttackMob crossbowAttackMob) {
                crossbowAttackMob.setChargingCrossbow(false);
            }

            if (mob.isUsingItem()) {
                mob.stopUsingItem();
            }

            return;
        }

        if (!mob.isUsingItem()) {
            mob.startUsingItem(weaponHand);
        }

        if (mob instanceof CrossbowAttackMob crossbowAttackMob) {
            crossbowAttackMob.setChargingCrossbow(true);
        }

        if (mob.getTicksUsingItem() < WARNING_CROSSBOW_CHARGE_TICKS) {
            return;
        }

        forceLoadCrossbow(crossbowStack);

        if (mob instanceof CrossbowAttackMob crossbowAttackMob) {
            crossbowAttackMob.setChargingCrossbow(false);
        }

        if (mob.isUsingItem()) {
            mob.stopUsingItem();
        }
    }

    private static void stopWarningPose(PathfinderMob mob) {
        if (mob.getTarget() != null) {
            return;
        }

        RetoldFactionTargetGuards.setAggressiveIgnoringGuard(mob, false);

        if (mob instanceof CrossbowAttackMob crossbowAttackMob) {
            crossbowAttackMob.setChargingCrossbow(false);
        }

        if (mob.isUsingItem()) {
            mob.stopUsingItem();
        }
    }

    private static boolean tryFirePreparedWarningOpeningShot(
            ServerLevel level,
            PathfinderMob mob,
            LivingEntity target
    ) {
        InteractionHand crossbowHand = getCrossbowHand(mob);

        if (crossbowHand != null) {
            ItemStack crossbowStack = mob.getItemInHand(crossbowHand);

            if (!forceLoadCrossbow(crossbowStack)) {
                return false;
            }

            if (mob.isUsingItem()) {
                mob.stopUsingItem();
            }

            if (mob instanceof CrossbowAttackMob crossbowAttackMob) {
                crossbowAttackMob.setChargingCrossbow(false);
            }

            faceTargetForOpeningShot(mob, target);
            mob.swing(crossbowHand);

            if (crossbowStack.getItem() instanceof CrossbowItem crossbowItem) {
                crossbowItem.performShooting(
                        level,
                        mob,
                        crossbowHand,
                        crossbowStack,
                        1.6F,
                        8.0F,
                        target
                );
            }

            return true;
        }

        InteractionHand weaponHand = getProjectileWeaponHand(mob);

        if (weaponHand == null) {
            return false;
        }

        ItemStack weaponStack = mob.getItemInHand(weaponHand);

        if (!isProjectileWeapon(weaponStack)) {
            return false;
        }

        if (mob.isUsingItem()) {
            mob.stopUsingItem();
        }

        faceTargetForOpeningShot(mob, target);
        mob.swing(weaponHand);

        if (mob instanceof RangedAttackMob rangedAttackMob) {
            rangedAttackMob.performRangedAttack(target, 1.0F);
            return true;
        }

        return false;
    }

    private static boolean forceLoadCrossbow(ItemStack crossbowStack) {
        if (!isCrossbowWeapon(crossbowStack)) {
            return false;
        }

        if (isCrossbowCharged(crossbowStack)) {
            return true;
        }

        ItemStackTemplate arrowTemplate = ItemStackTemplate.fromNonEmptyStack(
                Items.ARROW.getDefaultInstance()
        );

        crossbowStack.set(
                DataComponents.CHARGED_PROJECTILES,
                ChargedProjectiles.of(arrowTemplate)
        );

        return true;
    }

    private static boolean isCrossbowWeapon(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof CrossbowItem;
    }

    private static boolean isCrossbowCharged(ItemStack stack) {
        if (!isCrossbowWeapon(stack)) {
            return false;
        }

        ChargedProjectiles chargedProjectiles = stack.get(DataComponents.CHARGED_PROJECTILES);

        return chargedProjectiles != null && !chargedProjectiles.isEmpty();
    }

    private static InteractionHand getCrossbowHand(PathfinderMob mob) {
        if (isCrossbowWeapon(mob.getMainHandItem())) {
            return InteractionHand.MAIN_HAND;
        }

        if (isCrossbowWeapon(mob.getOffhandItem())) {
            return InteractionHand.OFF_HAND;
        }

        return null;
    }

    private static InteractionHand getProjectileWeaponHand(PathfinderMob mob) {
        if (isProjectileWeapon(mob.getMainHandItem())) {
            return InteractionHand.MAIN_HAND;
        }

        if (isProjectileWeapon(mob.getOffhandItem())) {
            return InteractionHand.OFF_HAND;
        }

        return null;
    }

    private static boolean isProjectileWeapon(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ProjectileWeaponItem;
    }

    private static boolean canMaintainWarningAwareness(
            ServerLevel level,
            PathfinderMob mob,
            TerritoryMobState state,
            TerritoryConfig config,
            LivingEntity target,
            long gameTime
    ) {
        if (!isValidWarningTarget(level, mob, target, config, gameTime)) {
            return false;
        }

        if (canSeeTarget(mob, target)) {
            rememberWarningTargetPosition(state, target, gameTime);
            return true;
        }

        return gameTime - state.lastSawWarningTargetAt <= WARNING_LOST_SIGHT_MEMORY_TICKS;
    }

    private static void rememberWarningTargetPosition(
            TerritoryMobState state,
            LivingEntity target,
            long gameTime
    ) {
        state.lastSawWarningTargetAt = gameTime;
        state.lastKnownWarningTargetX = target.getX();
        state.lastKnownWarningTargetY = target.getY();
        state.lastKnownWarningTargetZ = target.getZ();
    }

    private static void faceTargetSmoothly(PathfinderMob mob, LivingEntity target) {
        double dx = target.getX() - mob.getX();
        double dz = target.getZ() - mob.getZ();

        if (dx * dx + dz * dz < 0.0001D) {
            return;
        }

        double dy = target.getEyeY() - mob.getEyeY();
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) (Math.atan2(dz, dx) * 57.2957763671875D) - 90.0F;
        float pitch = (float) (-(Math.atan2(dy, horizontalDistance) * 57.2957763671875D));

        mob.setYRot(rotateToward(mob.getYRot(), yaw, 10.0F));
        mob.setYHeadRot(rotateToward(mob.getYHeadRot(), yaw, 16.0F));
        mob.yBodyRot = rotateToward(mob.yBodyRot, yaw, 8.0F);
        mob.setXRot(rotateToward(mob.getXRot(), pitch, 3.5F));
    }

    private static void faceLastKnownWarningPosition(PathfinderMob mob, TerritoryMobState state) {
        double dx = state.lastKnownWarningTargetX - mob.getX();
        double dz = state.lastKnownWarningTargetZ - mob.getZ();

        if (dx * dx + dz * dz < 0.0001D) {
            return;
        }

        double dy = state.lastKnownWarningTargetY + 1.0D - mob.getEyeY();
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) (Math.atan2(dz, dx) * 57.2957763671875D) - 90.0F;
        float pitch = (float) (-(Math.atan2(dy, horizontalDistance) * 57.2957763671875D));

        mob.setYRot(rotateToward(mob.getYRot(), yaw, 10.0F));
        mob.setYHeadRot(rotateToward(mob.getYHeadRot(), yaw, 16.0F));
        mob.yBodyRot = rotateToward(mob.yBodyRot, yaw, 8.0F);
        mob.setXRot(rotateToward(mob.getXRot(), pitch, 3.5F));
    }

    private static void faceTargetForOpeningShot(PathfinderMob mob, LivingEntity target) {
        double dx = target.getX() - mob.getX();
        double dz = target.getZ() - mob.getZ();

        if (dx * dx + dz * dz < 0.0001D) {
            return;
        }

        double dy = target.getEyeY() - mob.getEyeY();
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) (Math.atan2(dz, dx) * 57.2957763671875D) - 90.0F;
        float pitch = (float) (-(Math.atan2(dy, horizontalDistance) * 57.2957763671875D));

        mob.setYRot(yaw);
        mob.setYHeadRot(yaw);
        mob.yBodyRot = yaw;
        mob.setXRot(pitch);
    }

    private static boolean tryAdoptRetaliationTarget(
            ServerLevel level,
            PathfinderMob mob,
            TerritoryMobState state,
            TerritoryConfig config,
            long gameTime
    ) {
        LivingEntity attacker = mob.getLastHurtByMob();

        if (attacker == null) {
            return false;
        }

        if (!isPossibleIntruder(level, mob, attacker, config, gameTime)) {
            return false;
        }

        if (!isValidAttackTarget(mob, attacker)) {
            return false;
        }

        startAttackOnTarget(
                level,
                mob,
                state,
                config,
                attacker,
                gameTime,
                RetoldTargetSource.RETALIATION
        );

        return true;
    }

    private static boolean tryAdoptOwnedCombatTarget(
            ServerLevel level,
            PathfinderMob mob,
            TerritoryMobState state,
            TerritoryConfig config,
            long gameTime
    ) {
        LivingEntity existingTarget = mob.getTarget();

        if (existingTarget == null) {
            return false;
        }

        if (!RetoldFactionTargetMemory.isOwnedByAny(
                mob,
                existingTarget,
                RetoldTargetSource.FACTION_ASSIST,
                RetoldTargetSource.FACTION_COMBAT,
                RetoldTargetSource.TERRITORY_ATTACK,
                RetoldTargetSource.RETALIATION
        )) {
            return false;
        }

        if (!isPossibleIntruder(level, mob, existingTarget, config, gameTime)) {
            return false;
        }

        if (!isValidAttackTarget(mob, existingTarget)) {
            return false;
        }

        startAttackOnTarget(
                level,
                mob,
                state,
                config,
                existingTarget,
                gameTime,
                RetoldTargetSource.TERRITORY_ATTACK
        );

        return true;
    }

    private static boolean tryAdoptExistingAttackTarget(
            ServerLevel level,
            PathfinderMob mob,
            TerritoryMobState state,
            TerritoryConfig config,
            long gameTime
    ) {
        LivingEntity existingTarget = mob.getTarget();

        if (existingTarget == null) {
            return false;
        }

        if (existingTarget == mob.getLastHurtByMob()) {
            return false;
        }

        if (!isPossibleIntruder(level, mob, existingTarget, config, gameTime)) {
            return false;
        }

        if (!isValidAttackTarget(mob, existingTarget)) {
            return false;
        }

        startAttackOnTarget(
                level,
                mob,
                state,
                config,
                existingTarget,
                gameTime,
                RetoldTargetSource.TERRITORY_ATTACK
        );

        return true;
    }

    private static void suppressExistingTargetDuringWarning(
            ServerLevel level,
            PathfinderMob mob,
            TerritoryConfig config,
            long gameTime
    ) {
        LivingEntity existingTarget = mob.getTarget();

        if (existingTarget == null) {
            return;
        }

        if (RetoldFactionTargetMemory.isOwnedByAny(
                mob,
                existingTarget,
                RetoldTargetSource.FACTION_ASSIST,
                RetoldTargetSource.FACTION_COMBAT,
                RetoldTargetSource.TERRITORY_ATTACK,
                RetoldTargetSource.RETALIATION
        )) {
            return;
        }

        if (existingTarget == mob.getLastHurtByMob()) {
            return;
        }

        if (!isPossibleIntruder(level, mob, existingTarget, config, gameTime)) {
            return;
        }

        mob.setTarget(null);
        mob.setAggressive(false);
    }

    private static void startAttackOnTarget(
            ServerLevel level,
            PathfinderMob mob,
            TerritoryMobState state,
            TerritoryConfig config,
            LivingEntity target,
            long gameTime,
            RetoldTargetSource source
    ) {
        if (target == null) {
            return;
        }

        if (!state.firedPreparedWarningShot) {
            state.firedPreparedWarningShot = tryFirePreparedWarningOpeningShot(level, mob, target);
        }

        stopWarningPose(mob);

        state.hasStartedAttack = true;
        state.attackTarget = target;
        state.warningTarget = target;
        state.warningPulses = 0;
        state.nextAttackRefreshAt = gameTime + 20L;
        state.warnedIntruders.add(target.getUUID());

        applyAttackTarget(level, mob, target, config, gameTime, source);
    }

    private static void applyAttackTarget(
            ServerLevel level,
            PathfinderMob mob,
            LivingEntity target,
            TerritoryConfig config,
            long gameTime,
            RetoldTargetSource source
    ) {
        boolean applied = RetoldFactionTargetMemory.trySetTarget(mob, target, source);

        if (!applied && mob.getTarget() != target) {
            return;
        }

        mob.setAggressive(true);
        mob.getLookControl().setLookAt(target, 30.0F, 30.0F);

        if (mob instanceof AbstractPiglin piglin) {
            if (piglin.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null) != target) {
                piglin.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, target);
            }

            piglin.getBrain().setMemory(MemoryModuleType.ANGRY_AT, target.getUUID());
        }
    }

    private static void updateAttackState(
            ServerLevel level,
            PathfinderMob mob,
            TerritoryMobState state,
            TerritoryConfig config,
            long gameTime
    ) {
        LivingEntity attackTarget = state.attackTarget;

        if (attackTarget == null || !isValidAttackTarget(mob, attackTarget)) {
            LivingEntity replacement = findBestWarnedAttackTarget(level, mob, state, config, gameTime);

            if (replacement == null) {
                returnToWarningMode(mob, state, gameTime);
                return;
            }

            startAttackOnTarget(
                    level,
                    mob,
                    state,
                    config,
                    replacement,
                    gameTime,
                    RetoldTargetSource.TERRITORY_ATTACK
            );

            return;
        }

        if (gameTime >= state.nextAttackRefreshAt) {
            RetoldTargetSource source = mob.getLastHurtByMob() == attackTarget
                    ? RetoldTargetSource.RETALIATION
                    : RetoldTargetSource.TERRITORY_ATTACK;

            applyAttackTarget(level, mob, attackTarget, config, gameTime, source);
            state.nextAttackRefreshAt = gameTime + ATTACK_REFRESH_INTERVAL_TICKS;
        }
    }

    private static LivingEntity findBestWarnedAttackTarget(
            ServerLevel level,
            PathfinderMob mob,
            TerritoryMobState state,
            TerritoryConfig config,
            long gameTime
    ) {
        if (
                state.warningTarget != null
                        && state.warnedIntruders.contains(state.warningTarget.getUUID())
                        && isPossibleIntruder(level, mob, state.warningTarget, config, gameTime)
                        && isValidAttackTarget(mob, state.warningTarget)
        ) {
            return state.warningTarget;
        }

        List<LivingEntity> candidates = level.getEntitiesOfClass(
                LivingEntity.class,
                mob.getBoundingBox().inflate(NOTICE_MOB_RADIUS_BLOCKS),
                target -> state.warnedIntruders.contains(target.getUUID())
                        && isPossibleIntruder(level, mob, target, config, gameTime)
                        && isValidAttackTarget(mob, target)
        );

        LivingEntity bestTarget = null;
        double bestScore = Double.MAX_VALUE;

        for (LivingEntity candidate : candidates) {
            int focusCount = countNearbyFactionMobsFocusedOn(level, mob, config, candidate);
            double score = focusCount * 1000.0D + mob.distanceToSqr(candidate) * 0.01D;

            if (score < bestScore) {
                bestScore = score;
                bestTarget = candidate;
            }
        }

        return bestTarget;
    }

    private static void signalNearbyWarnedGuardsToAttack(
            ServerLevel level,
            PathfinderMob caller,
            TerritoryConfig config,
            long gameTime
    ) {
        List<PathfinderMob> nearbyGuards = level.getEntitiesOfClass(
                PathfinderMob.class,
                caller.getBoundingBox().inflate(ATTACK_CHAIN_RADIUS_BLOCKS),
                other -> other != caller
                        && other.isAlive()
                        && RetoldFactionMembers.getFaction(other) == config.faction
        );

        for (PathfinderMob guard : nearbyGuards) {
            TerritoryMobState guardState = MOB_STATES.get(guard);

            if (guardState == null || guardState.hasStartedAttack) {
                continue;
            }

            guardState.territoryContext = getTerritoryContextAt(level, guard.blockPosition());

            if (guardState.territoryContext == null || guardState.territoryContext.faction() != config.faction) {
                continue;
            }

            LivingEntity attackTarget = findBestWarnedAttackTarget(level, guard, guardState, config, gameTime);

            if (attackTarget == null) {
                continue;
            }

            startAttackOnTarget(
                    level,
                    guard,
                    guardState,
                    config,
                    attackTarget,
                    gameTime,
                    RetoldTargetSource.TERRITORY_ATTACK
            );
        }
    }

    private static void returnToWarningMode(PathfinderMob mob, TerritoryMobState state, long gameTime) {
        LivingEntity oldAttackTarget = state.attackTarget;

        if (oldAttackTarget != null) {
            RetoldFactionTargetMemory.clearTargetIfOwnedByAny(
                    mob,
                    oldAttackTarget,
                    RetoldTargetSource.TERRITORY_ATTACK,
                    RetoldTargetSource.RETALIATION
            );
        }

        resetWarningState(state, gameTime);
    }

    private static void resetWarningState(TerritoryMobState state, long gameTime) {
        state.hasStartedAttack = false;
        state.firedPreparedWarningShot = false;
        state.finalWarningStartedAt = -1L;
        state.attackTarget = null;
        state.warningTarget = null;
        state.warningPulses = 0;
        state.nextWarningPulseAt = gameTime;
        state.nextWarningRepositionAt = gameTime;
        state.nextTargetRecheckAt = gameTime;
        state.nextFormationRecheckAt = gameTime;
        state.hasWarningMoveTarget = false;
        state.warningMoveTargetSlot = Integer.MIN_VALUE;
        state.nextWarningPathRefreshAt = gameTime;
        state.warnedIntruders.clear();
    }

    private static boolean isValidWarningTarget(
            ServerLevel level,
            PathfinderMob mob,
            LivingEntity target,
            TerritoryConfig config,
            long gameTime
    ) {
        return target != null
                && target.isAlive()
                && mob.level() == target.level()
                && isPossibleIntruder(level, mob, target, config, gameTime);
    }

    private static boolean isValidAttackTarget(PathfinderMob mob, LivingEntity target) {
        return target != null
                && target.isAlive()
                && mob.level() == target.level()
                && mob.distanceToSqr(target) <= ATTACK_TARGET_RELEASE_DISTANCE_SQUARED;
    }

    private static boolean isPossibleIntruder(
            ServerLevel level,
            PathfinderMob mob,
            LivingEntity intruder,
            TerritoryConfig config,
            long gameTime
    ) {
        if (intruder == mob) {
            return false;
        }

        if (!intruder.isAlive()) {
            return false;
        }

        if (mob.level() != intruder.level()) {
            return false;
        }

        if (mob.distanceToSqr(intruder) > NOTICE_MOB_RADIUS_BLOCKS * NOTICE_MOB_RADIUS_BLOCKS) {
            return false;
        }

        if (intruder instanceof ServerPlayer player) {
            if (player.isCreative() || player.isSpectator()) {
                return false;
            }
        }

        RetoldFaction intruderFaction = RetoldFactionMembers.getFaction(intruder);

        if (intruderFaction == null) {
            return false;
        }

        return canTriggerTerritoryWarning(config.faction, intruderFaction);
    }

    private static boolean canTriggerTerritoryWarning(
            RetoldFaction territoryFaction,
            RetoldFaction intruderFaction
    ) {
        if (territoryFaction == null || intruderFaction == null) {
            return false;
        }

        if (territoryFaction == intruderFaction) {
            return false;
        }

        if (intruderFaction == RetoldFaction.PLAYER) {
            return territoryFaction == RetoldFaction.NETHER_REMNANTS
                    || territoryFaction == RetoldFaction.ILLAGERS;
        }

        return RetoldFactionRelations.areEnemyFactions(territoryFaction, intruderFaction);
    }

    private static boolean canSeeTarget(PathfinderMob mob, LivingEntity target) {
        return mob.getSensing().hasLineOfSight(target);
    }

    private static TerritoryConfig getTerritoryConfigForMob(Entity entity) {
        RetoldFaction faction = RetoldFactionMembers.getFaction(entity);

        if (faction == null) {
            return null;
        }

        return TERRITORY_CONFIGS.get(faction);
    }

    private static boolean isInAllowedDimension(ServerLevel level, TerritoryConfig config) {
        return config.requiredDimension == null || level.dimension() == config.requiredDimension;
    }

    private static boolean isNearFactionTerritory(
            ServerLevel level,
            Entity entity,
            TerritoryConfig config,
            long gameTime
    ) {
        return isNearFactionTerritory(level, entity.blockPosition(), config, gameTime);
    }

    private static boolean isNearFactionTerritory(
            ServerLevel level,
            BlockPos pos,
            TerritoryConfig config,
            long gameTime
    ) {
        long chunkKey = chunkKey(pos);

        TerritoryCacheKey cacheKey = new TerritoryCacheKey(
                config.faction,
                level.dimension(),
                chunkKey
        );

        TerritoryCacheEntry cached = TERRITORY_CACHE.get(cacheKey);

        if (cached != null && cached.expiresAt >= gameTime) {
            return cached.nearTerritory;
        }

        boolean nearTerritory = computeNearFactionTerritory(level, pos, config);

        if (TERRITORY_CACHE.size() > MAX_TERRITORY_CACHE_SIZE) {
            TERRITORY_CACHE.clear();
        }

        TERRITORY_CACHE.put(
                cacheKey,
                new TerritoryCacheEntry(nearTerritory, gameTime + TERRITORY_CACHE_TICKS)
        );

        return nearTerritory;
    }

    private static boolean computeNearFactionTerritory(
            ServerLevel level,
            BlockPos pos,
            TerritoryConfig config
    ) {
        // Best detection:
        // inside the structure OR within 2 chunks of any real structure piece.
        if (findNearbyFactionTerritoryPiece(level, pos, config) != null) {
            return true;
        }

        // Fallback detection:
        // near the map structure start/anchor.
        BlockPos structurePos = level.findNearestMapStructure(
                config.territoryTag,
                pos,
                STRUCTURE_SEARCH_RADIUS_CHUNKS,
                false
        );

        if (structurePos == null) {
            return false;
        }

        double dx = structurePos.getX() + 0.5D - pos.getX();
        double dz = structurePos.getZ() + 0.5D - pos.getZ();

        return dx * dx + dz * dz <= TERRITORY_RADIUS_BLOCKS * TERRITORY_RADIUS_BLOCKS;
    }

    private static BlockPos findFactionTerritoryAnchor(
            ServerLevel level,
            BlockPos pos,
            TerritoryConfig config
    ) {
        // Prefer the real structure start as the reputation key anchor.
        // This keeps one fortress/bastion/outpost using one stable suspicion key.
        BlockPos structurePos = level.findNearestMapStructure(
                config.territoryTag,
                pos,
                STRUCTURE_SEARCH_RADIUS_CHUNKS,
                false
        );

        if (structurePos != null) {
            return structurePos;
        }

        // Fallback: if map-structure lookup fails but a structure piece is nearby,
        // use that piece chunk as the local reputation anchor.
        BlockPos nearbyPiecePos = findNearbyFactionTerritoryPiece(level, pos, config);

        if (nearbyPiecePos != null) {
            return new BlockPos(
                    nearbyPiecePos.getX() >> 4 << 4,
                    nearbyPiecePos.getY(),
                    nearbyPiecePos.getZ() >> 4 << 4
            );
        }

        return null;
    }

    private static BlockPos findNearbyFactionTerritoryPiece(
            ServerLevel level,
            BlockPos pos,
            TerritoryConfig config
    ) {
        int paddingBlocks = TERRITORY_PIECE_PADDING_CHUNKS * 16;

        for (int dx = -paddingBlocks; dx <= paddingBlocks; dx += TERRITORY_PIECE_SCAN_STEP_BLOCKS) {
            for (int dz = -paddingBlocks; dz <= paddingBlocks; dz += TERRITORY_PIECE_SCAN_STEP_BLOCKS) {
                for (
                        int dy = -TERRITORY_PIECE_VERTICAL_SCAN_RANGE_BLOCKS;
                        dy <= TERRITORY_PIECE_VERTICAL_SCAN_RANGE_BLOCKS;
                        dy += TERRITORY_PIECE_VERTICAL_SCAN_STEP_BLOCKS
                ) {
                    BlockPos samplePos = pos.offset(dx, dy, dz);

                    StructureStart structureStart = level.structureManager()
                            .getStructureWithPieceAt(samplePos, config.territoryTag);

                    if (structureStart != null && structureStart.isValid()) {
                        return samplePos;
                    }
                }
            }
        }

        return null;
    }

    private static boolean isInRaid(ServerLevel level, Entity entity) {
        Raid raid = level.getRaidAt(entity.blockPosition());
        return raid != null && raid.isActive();
    }

    private static void clearMobState(PathfinderMob mob) {
        stopWarningPose(mob);
        MOB_STATES.remove(mob);
    }

    private static void registerTerritory(
            RetoldFaction faction,
            String structureTagPath,
            SoundEvent warningSound,
            ResourceKey<Level> requiredDimension
    ) {
        TERRITORY_CONFIGS.put(
                faction,
                new TerritoryConfig(
                        faction,
                        territoryTag(structureTagPath),
                        warningSound,
                        requiredDimension
                )
        );
    }

    private static TagKey<Structure> territoryTag(String path) {
        return TagKey.create(
                Registries.STRUCTURE,
                Identifier.fromNamespaceAndPath("retold", path)
        );
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("minecraft", path);
    }

    private static long chunkKey(BlockPos pos) {
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;

        return ((long) chunkX & 4294967295L) | (((long) chunkZ & 4294967295L) << 32);
    }

    private static int getStableSide(Entity first, Entity second) {
        return Math.floorMod(first.getId() * 31 + second.getId() * 17, 2) == 0 ? 1 : -1;
    }

    private static double getStableTieBreaker(Entity first, Entity second) {
        int value = Math.floorMod(first.getId() * 7349 + second.getId() * 9151, 1000);
        return value / 100000.0D;
    }

    private static float rotateToward(float current, float target, float maxChange) {
        float difference = wrapDegrees(target - current);

        if (difference > maxChange) {
            difference = maxChange;
        }

        if (difference < -maxChange) {
            difference = -maxChange;
        }

        return current + difference;
    }

    private static float wrapDegrees(float degrees) {
        while (degrees >= 180.0F) {
            degrees -= 360.0F;
        }

        while (degrees < -180.0F) {
            degrees += 360.0F;
        }

        return degrees;
    }

    private static double clamp(double value, double min, double max) {
        if (value < min) {
            return min;
        }

        if (value > max) {
            return max;
        }

        return value;
    }

    private static final class TerritoryConfig {
        private final RetoldFaction faction;
        private final TagKey<Structure> territoryTag;
        private final SoundEvent warningSound;
        private final ResourceKey<Level> requiredDimension;

        private TerritoryConfig(
                RetoldFaction faction,
                TagKey<Structure> territoryTag,
                SoundEvent warningSound,
                ResourceKey<Level> requiredDimension
        ) {
            this.faction = faction;
            this.territoryTag = territoryTag;
            this.warningSound = warningSound;
            this.requiredDimension = requiredDimension;
        }
    }

    private static final class TerritoryMobState {
        private RetoldTerritoryContext territoryContext;

        private LivingEntity warningTarget;
        private LivingEntity attackTarget;

        private boolean hasStartedAttack;
        private boolean firedPreparedWarningShot;
        private int warningPulses;

        private long nextWarningPulseAt;
        private long nextWarningRepositionAt;
        private long nextTargetRecheckAt;
        private long nextFormationRecheckAt;
        private long nextAttackRefreshAt;

        private long finalWarningStartedAt = -1L;

        private long lastSawWarningTargetAt;
        private double lastKnownWarningTargetX;
        private double lastKnownWarningTargetY;
        private double lastKnownWarningTargetZ;

        private int warningMoveSide;
        private int warningFormationSlot;
        private double warningAnchorAngle;

        private boolean hasWarningMoveTarget;
        private double warningMoveTargetX;
        private double warningMoveTargetY;
        private double warningMoveTargetZ;
        private double warningMoveTargetSourceX;
        private double warningMoveTargetSourceZ;
        private int warningMoveTargetSlot = Integer.MIN_VALUE;
        private long nextWarningPathRefreshAt;

        private final Set<UUID> warnedIntruders = new HashSet<>();

        private TerritoryMobState(int mobId) {
            this.warningMoveSide = Math.floorMod(mobId, 2) == 0 ? 1 : -1;
        }
    }

    private static final class WarningMovementProfile {
        private final double desiredDistance;
        private final double minDistance;
        private final double maxDistance;
        private final double speed;
        private final double urgentSpeed;
        private final double sideAngle;
        private final int repositionInterval;

        private WarningMovementProfile(
                double desiredDistance,
                double minDistance,
                double maxDistance,
                double speed,
                double urgentSpeed,
                double sideAngle,
                int repositionInterval
        ) {
            this.desiredDistance = desiredDistance;
            this.minDistance = minDistance;
            this.maxDistance = maxDistance;
            this.speed = speed;
            this.urgentSpeed = urgentSpeed;
            this.sideAngle = sideAngle;
            this.repositionInterval = repositionInterval;
        }
    }

    private static final class WarningFormationSlot {
        private final int slotOffset;
        private final int totalGuards;

        private WarningFormationSlot(int slotOffset, int totalGuards) {
            this.slotOffset = slotOffset;
            this.totalGuards = totalGuards;
        }
    }

    private record TerritoryCacheKey(
            RetoldFaction faction,
            ResourceKey<Level> dimension,
            long chunkKey
    ) {
    }

    private record TerritoryCacheEntry(
            boolean nearTerritory,
            long expiresAt
    ) {
    }
}