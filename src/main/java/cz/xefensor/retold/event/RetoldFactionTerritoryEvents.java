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

    private static final int STRUCTURE_SEARCH_RADIUS_CHUNKS = 6;
    private static final double TERRITORY_RADIUS_BLOCKS = 48.0D;

    private static final double NOTICE_MOB_RADIUS_BLOCKS = 48.0D;
    private static final double ATTACK_CHAIN_RADIUS_BLOCKS = 48.0D;
    private static final double ATTACK_TARGET_RELEASE_DISTANCE_SQUARED = 40.0D * 40.0D;

    private static final int WARNING_PULSES_BEFORE_ATTACK = 5;
    private static final int BASE_WARNING_PARTICLE_COUNT = 2;
    private static final int WARNING_PARTICLE_COUNT_PER_PULSE = 2;

    private static final float BASE_WARNING_SOUND_VOLUME = 0.35F;
    private static final float WARNING_SOUND_VOLUME_PER_PULSE = 0.12F;
    private static final float WARNING_SOUND_PITCH = 0.85F;

    private static final int BASE_WARNING_PULSE_INTERVAL_TICKS = 80;
    private static final int TARGET_RECHECK_INTERVAL_TICKS = 35;
    private static final int ATTACK_REFRESH_INTERVAL_TICKS = 100;

    private static final int WARNING_CROSSBOW_CHARGE_TICKS = 28;

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

        if (state != null && state.hasStartedAttack) {
            return false;
        }

        return true;
    }

    private static void updateMobTerritoryLogic(
            ServerLevel level,
            PathfinderMob mob,
            TerritoryMobState state,
            TerritoryConfig config,
            long gameTime
    ) {
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

        maintainContinuousBehavior(level, mob, state, config, gameTime);

        if (!canCountWarningPulse(level, mob, warningTarget, config, gameTime)) {
            state.nextWarningPulseAt = Math.max(state.nextWarningPulseAt, gameTime + 10L);
            return;
        }

        if (gameTime < state.nextWarningPulseAt) {
            return;
        }

        markVisibleWarnedIntruders(level, mob, state, config, gameTime);
        playWarningEffects(level, mob, config, state.warningPulses);

        state.warningPulses++;
        state.nextWarningPulseAt = gameTime + getWarningPulseInterval(mob);

        if (state.warningPulses >= WARNING_PULSES_BEFORE_ATTACK) {
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

        LivingEntity warningTarget = state.warningTarget;

        if (warningTarget == null || !isValidWarningTarget(level, mob, warningTarget, config, gameTime)) {
            stopWarningPose(mob);
            return;
        }

        updateWarningPose(mob, warningTarget);
        faceTargetSmoothly(mob, warningTarget);

        int focusCount = countNearbyFactionMobsFocusedOn(level, mob, config, warningTarget);
        WarningMovementProfile profile = getWarningMovementProfile(mob, warningTarget, focusCount);

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

        if (currentTarget == null || !isValidWarningTarget(level, mob, currentTarget, config, gameTime)) {
            LivingEntity bestTarget = findBestWarningTarget(level, mob, config, gameTime);
            setWarningTarget(state, mob, bestTarget, gameTime);
            return;
        }

        if (gameTime < state.nextTargetRecheckAt) {
            return;
        }

        state.nextTargetRecheckAt = gameTime + TARGET_RECHECK_INTERVAL_TICKS + Math.floorMod(mob.getId(), 12);

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
        state.warningPulses = 0;
        state.nextWarningPulseAt = gameTime;
        state.nextTargetRecheckAt = gameTime + TARGET_RECHECK_INTERVAL_TICKS + Math.floorMod(mob.getId(), 12);
        state.nextWarningRepositionAt = gameTime;

        if (target != null) {
            state.warningMoveSide = getStableSide(mob, target);
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
            TerritoryMobState otherState = MOB_STATES.get(other);

            if (otherState != null) {
                if (otherState.warningTarget == intruder || otherState.attackTarget == intruder) {
                    count++;
                    continue;
                }
            }

            if (other.getTarget() == intruder) {
                count++;
            }
        }

        return count;
    }

    private static void updateWarningMovement(
            PathfinderMob mob,
            LivingEntity target,
            TerritoryMobState state,
            WarningMovementProfile profile,
            long gameTime
    ) {
        double distanceSqr = mob.distanceToSqr(target);
        double minDistanceSqr = profile.minDistance * profile.minDistance;
        double maxDistanceSqr = profile.maxDistance * profile.maxDistance;

        boolean tooClose = distanceSqr < minDistanceSqr;
        boolean tooFar = distanceSqr > maxDistanceSqr;
        boolean shouldReposition = gameTime >= state.nextWarningRepositionAt;

        if (!tooClose && !tooFar && !shouldReposition) {
            return;
        }

        moveToWarningRingPosition(mob, target, state, profile, gameTime, tooClose || tooFar);
    }

    private static void moveToWarningRingPosition(
            PathfinderMob mob,
            LivingEntity target,
            TerritoryMobState state,
            WarningMovementProfile profile,
            long gameTime,
            boolean urgent
    ) {
        double dx = mob.getX() - target.getX();
        double dz = mob.getZ() - target.getZ();

        if (dx * dx + dz * dz < 0.0001D) {
            dx = 1.0D;
            dz = 0.0D;
        }

        double baseAngle = Math.atan2(dz, dx);
        double phase = ((gameTime / Math.max(1, profile.repositionInterval)) + Math.floorMod(mob.getId(), 4)) % 4;
        double phaseOffset = (phase - 1.5D) * 0.25D;
        double sideOffset = state.warningMoveSide * profile.sideAngle * (0.65D + phaseOffset);

        double targetAngle = baseAngle + sideOffset;

        double moveX = target.getX() + Math.cos(targetAngle) * profile.desiredDistance;
        double moveZ = target.getZ() + Math.sin(targetAngle) * profile.desiredDistance;

        mob.getNavigation().moveTo(
                moveX,
                target.getY(),
                moveZ,
                urgent ? profile.urgentSpeed : profile.speed
        );

        state.nextWarningRepositionAt = gameTime + profile.repositionInterval;
    }

    private static WarningMovementProfile getWarningMovementProfile(
            PathfinderMob mob,
            LivingEntity target,
            int focusCount
    ) {
        boolean ranged = isRangedWarningMob(mob);

        double mobWidth = Math.max(0.6D, mob.getBbWidth());
        double targetWidth = Math.max(0.6D, target.getBbWidth());

        if (ranged) {
            double desiredDistance = clamp(
                    9.5D + mobWidth + targetWidth + focusCount * 0.6D,
                    9.0D,
                    16.0D
            );

            double band = clamp(2.25D + focusCount * 0.15D, 2.25D, 4.0D);
            double sideAngle = clamp(0.35D + focusCount * 0.04D, 0.35D, 0.7D);
            int repositionInterval = 34 + Math.floorMod(mob.getId(), 16);

            return new WarningMovementProfile(
                    desiredDistance,
                    Math.max(6.5D, desiredDistance - band),
                    desiredDistance + band,
                    0.72D,
                    0.92D,
                    sideAngle,
                    repositionInterval
            );
        }

        double desiredDistance = clamp(
                3.5D + mobWidth + targetWidth + focusCount * 0.25D,
                4.25D,
                7.5D
        );

        double band = clamp(1.1D + focusCount * 0.08D, 1.1D, 1.9D);
        double sideAngle = clamp(0.55D + focusCount * 0.05D, 0.55D, 0.9D);
        int repositionInterval = 24 + Math.floorMod(mob.getId(), 14);

        return new WarningMovementProfile(
                desiredDistance,
                Math.max(2.75D, desiredDistance - band),
                desiredDistance + band,
                0.86D,
                1.02D,
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
            long gameTime
    ) {
        if (!isValidWarningTarget(level, mob, target, config, gameTime)) {
            return false;
        }

        int focusCount = countNearbyFactionMobsFocusedOn(level, mob, config, target);
        WarningMovementProfile profile = getWarningMovementProfile(mob, target, focusCount);

        double countDistance = profile.maxDistance + 1.5D;
        return mob.distanceToSqr(target) <= countDistance * countDistance;
    }

    private static int getWarningPulseInterval(PathfinderMob mob) {
        return BASE_WARNING_PULSE_INTERVAL_TICKS + Math.floorMod(mob.getId(), 16);
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
                        && canCountWarningPulse(level, mob, target, config, gameTime)
        );

        for (LivingEntity intruder : nearbyIntruders) {
            state.warnedIntruders.add(intruder.getUUID());
        }
    }

    private static void playWarningEffects(
            ServerLevel level,
            PathfinderMob mob,
            TerritoryConfig config,
            int warningPulses
    ) {
        int particleCount = BASE_WARNING_PARTICLE_COUNT + warningPulses * WARNING_PARTICLE_COUNT_PER_PULSE;
        float volume = BASE_WARNING_SOUND_VOLUME + warningPulses * WARNING_SOUND_VOLUME_PER_PULSE;

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

    private static void updateWarningPose(PathfinderMob mob, LivingEntity warningTarget) {
        RetoldFactionTargetGuards.setAggressiveIgnoringGuard(mob, true);

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
        state.warningPulses = WARNING_PULSES_BEFORE_ATTACK;
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
        state.attackTarget = null;
        state.warningTarget = null;
        state.warningPulses = 0;
        state.nextWarningPulseAt = gameTime;
        state.nextWarningRepositionAt = gameTime;
        state.nextTargetRecheckAt = gameTime;
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
                && isPossibleIntruder(level, mob, target, config, gameTime)
                && canSeeTarget(mob, target);
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

        return RetoldFactionRelations.areEnemyFactions(config.faction, intruderFaction);
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

        // Players should be warned in faction territory even if normal open-world
        // faction combat does not custom-target players.
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
        BlockPos pos = entity.blockPosition();
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
        StructureStart currentStructure = level.structureManager()
                .getStructureWithPieceAt(pos, config.territoryTag);

        if (currentStructure != null && currentStructure.isValid()) {
            return true;
        }

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
        private LivingEntity warningTarget;
        private LivingEntity attackTarget;

        private boolean hasStartedAttack;
        private boolean firedPreparedWarningShot;
        private int warningPulses;

        private long nextWarningPulseAt;
        private long nextWarningRepositionAt;
        private long nextTargetRecheckAt;
        private long nextAttackRefreshAt;

        private int warningMoveSide;

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