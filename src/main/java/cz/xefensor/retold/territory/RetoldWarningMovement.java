package cz.xefensor.retold.territory;

import cz.xefensor.retold.behavior.performance.RetoldAiScanCache;
import cz.xefensor.retold.behavior.performance.RetoldBehaviorPerf;

import cz.xefensor.retold.faction.RetoldFactionMembers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class RetoldWarningMovement {
    private static final Set<Identifier> RANGED_WARNING_ENTITY_IDS = Set.of(
            id("pillager"),
            id("blaze"),
            id("evoker"),
            id("illusioner"),
            id("witch")
    );

    private RetoldWarningMovement() {
    }

    public static void tickWarningMovement(
            ServerLevel level,
            PathfinderMob mob,
            RetoldTerritoryConfig config,
            RetoldTerritoryMobState state,
            LivingEntity warningTarget,
            RetoldWarningLevel warningLevel,
            Map<PathfinderMob, RetoldTerritoryMobState> mobStates,
            long gameTime
    ) {
        WarningFormationSlot formationSlot = getWarningFormationSlot(
                level,
                mob,
                config,
                state,
                warningTarget,
                mobStates,
                gameTime
        );

        if (gameTime >= state.nextFormationRecheckAt) {
            if (state.warningFormationSlot != formationSlot.slotOffset) {
                state.warningFormationSlot = formationSlot.slotOffset;
                state.hasWarningMoveTarget = false;
            }

            state.nextFormationRecheckAt = gameTime
                    + RetoldTerritoryConstants.WARNING_FORMATION_RECHECK_INTERVAL_TICKS;
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

    public static int countNearbyFactionMobsFocusedOn(
            ServerLevel level,
            PathfinderMob mob,
            RetoldTerritoryConfig config,
            RetoldTerritoryMobState state,
            LivingEntity intruder,
            Map<PathfinderMob, RetoldTerritoryMobState> mobStates,
            long gameTime
    ) {
        List<PathfinderMob> nearbyMobs = getNearbyFactionMobs(
                level,
                mob,
                config,
                state,
                gameTime
        );

        int count = 0;

        for (PathfinderMob other : nearbyMobs) {
            if (isFocusedOnIntruder(other, intruder, mobStates)) {
                count++;
            }
        }

        return count;
    }

    public static List<PathfinderMob> getNearbyFactionMobs(
            ServerLevel level,
            PathfinderMob mob,
            RetoldTerritoryConfig config,
            RetoldTerritoryMobState state,
            long gameTime
    ) {
        if (level == null || mob == null || config == null) {
            return List.of();
        }

        if (
                state != null
                        && state.nearbyFactionMobsFaction == config.faction
                        && gameTime < state.nextNearbyFactionMobsRecheckAt
        ) {
            RetoldBehaviorPerf.recordTerritoryFactionMobCache(true);
            return state.nearbyFactionMobs;
        }

        RetoldBehaviorPerf.recordTerritoryFactionMobCache(false);

        List<PathfinderMob> nearbyMobs = RetoldAiScanCache.nearby(
                level,
                mob,
                PathfinderMob.class,
                RetoldTerritoryConstants.NOTICE_MOB_RADIUS_BLOCKS,
                gameTime,
                RetoldTerritoryConstants.WARNING_NEARBY_FACTION_MOB_CACHE_TICKS
        ).stream()
                .filter(other -> other != mob)
                .filter(PathfinderMob::isAlive)
                .filter(other -> RetoldFactionMembers.isMemberOf(other, config.faction))
                .toList();

        if (state != null) {
            state.nearbyFactionMobs = nearbyMobs;
            state.nearbyFactionMobsFaction = config.faction;
            state.nextNearbyFactionMobsRecheckAt = gameTime
                    + RetoldTerritoryConstants.WARNING_NEARBY_FACTION_MOB_CACHE_TICKS
                    + Math.floorMod(mob.getId(), 4);
        }

        return nearbyMobs;
    }

    public static double getAngleFromTargetToMob(PathfinderMob mob, LivingEntity target) {
        double dx = mob.getX() - target.getX();
        double dz = mob.getZ() - target.getZ();

        if (dx * dx + dz * dz < 0.0001D) {
            double stableAngle = Math.floorMod(mob.getId() * 37 + target.getId() * 19, 360);
            return Math.toRadians(stableAngle);
        }

        return Math.atan2(dz, dx);
    }

    private static WarningFormationSlot getWarningFormationSlot(
            ServerLevel level,
            PathfinderMob mob,
            RetoldTerritoryConfig config,
            RetoldTerritoryMobState state,
            LivingEntity intruder,
            Map<PathfinderMob, RetoldTerritoryMobState> mobStates,
            long gameTime
    ) {
        List<PathfinderMob> focusedGuards = getNearbyFactionMobs(
                level,
                mob,
                config,
                state,
                gameTime
        ).stream()
                .filter(other -> isFocusedOnIntruder(other, intruder, mobStates))
                .toList();

        if (!focusedGuards.contains(mob)) {
            focusedGuards = new java.util.ArrayList<>(focusedGuards);
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

    private static boolean isFocusedOnIntruder(
            PathfinderMob mob,
            LivingEntity intruder,
            Map<PathfinderMob, RetoldTerritoryMobState> mobStates
    ) {
        RetoldTerritoryMobState state = mobStates.get(mob);

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

    private static void updateWarningMovement(
            PathfinderMob mob,
            LivingEntity target,
            RetoldTerritoryMobState state,
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
                        && distanceToMoveTargetSqr <= RetoldTerritoryConstants.WARNING_POSITION_STOP_DISTANCE_SQUARED
        ) {
            mob.getNavigation().stop();
            return;
        }

        if (distanceToMoveTargetSqr > RetoldTerritoryConstants.WARNING_POSITION_REPATH_DISTANCE_SQUARED) {
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
            RetoldTerritoryMobState state,
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

        state.nextWarningPathRefreshAt = gameTime
                + RetoldTerritoryConstants.WARNING_MIN_REPATH_INTERVAL_TICKS;
    }

    private static boolean hasWarningTargetDrifted(
            LivingEntity target,
            RetoldTerritoryMobState state
    ) {
        if (!state.hasWarningMoveTarget) {
            return true;
        }

        double dx = target.getX() - state.warningMoveTargetSourceX;
        double dz = target.getZ() - state.warningMoveTargetSourceZ;

        return dx * dx + dz * dz >= RetoldTerritoryConstants.WARNING_TARGET_DRIFT_REPATH_DISTANCE_SQUARED;
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
                        0.25D
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
                    0.35D
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

            return new WarningMovementProfile(
                    desiredDistance,
                    Math.max(7.0D, desiredDistance - band),
                    desiredDistance + band,
                    finalWarning ? 0.55D : 0.72D,
                    finalWarning ? 0.75D : 0.92D,
                    sideAngle
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

        return new WarningMovementProfile(
                desiredDistance,
                Math.max(2.5D, desiredDistance - band),
                desiredDistance + band,
                finalWarning ? 0.82D : 0.86D,
                finalWarning ? 1.05D : 1.02D,
                sideAngle
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

    private static boolean isProjectileWeapon(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ProjectileWeaponItem;
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("minecraft", path);
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

    private static final class WarningMovementProfile {
        private final double desiredDistance;
        private final double minDistance;
        private final double maxDistance;
        private final double speed;
        private final double urgentSpeed;
        private final double sideAngle;

        private WarningMovementProfile(
                double desiredDistance,
                double minDistance,
                double maxDistance,
                double speed,
                double urgentSpeed,
                double sideAngle
        ) {
            this.desiredDistance = desiredDistance;
            this.minDistance = minDistance;
            this.maxDistance = maxDistance;
            this.speed = speed;
            this.urgentSpeed = urgentSpeed;
            this.sideAngle = sideAngle;
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
}
