package cz.xefensor.retold.worldgen.fire;

import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.control.RetoldAiControlMode;
import cz.xefensor.retold.behavior.control.RetoldAiControlOwner;
import cz.xefensor.retold.behavior.control.RetoldAiPriorities;
import cz.xefensor.retold.behavior.core.RetoldBehaviorMovement;
import cz.xefensor.retold.combat.RetoldFactionTargetMemory;
import cz.xefensor.retold.combat.RetoldTargetSource;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/** Owns patrol and combat-follow movement for Wildfire escort Blazes. */
final class WildfireFormation {
    private static final int GOAL_PRIORITY = 2;
    private static final int CONTROL_TICKS = 25;
    private static final int FAILED_PATROL_RETRY_TICKS = 20;
    private static final int PATROL_SEGMENT_TICKS = 300;
    private static final int FLYING_PATH_REFRESH_TICKS = 10;
    private static final double PATROL_DISTANCE = 14.0D;
    private static final double[] PATROL_TURN_OFFSETS = {
            0.0D,
            Math.PI / 4.0D,
            -Math.PI / 4.0D,
            Math.PI / 2.0D,
            -Math.PI / 2.0D,
            Math.PI
    };
    private static final double[] PATROL_CLIMB_OFFSETS = {
            0.0D,
            2.0D,
            4.0D,
            6.0D
    };
    private static final double PATROL_SPEED = 1.0D;
    private static final double PATROL_CLIMB_SPEED = 0.16D;
    private static final double ESCORT_SPEED = 1.15D;
    private static final double ESCORT_COMBAT_SPEED = 1.3D;
    private static final double ESCORT_SPACING = 2.25D;
    private static final double LEADER_PATH_DESTINATION_TOLERANCE_SQUARED = 1.0D;
    private static final double ESCORT_PATH_DESTINATION_TOLERANCE_SQUARED = 2.0D * 2.0D;
    private static final double ESCORT_POSITION_TOLERANCE_SQUARED = 0.8D * 0.8D;
    private static final double ESCORT_COMBAT_POSITION_TOLERANCE_SQUARED = 1.5D * 1.5D;
    private static final Map<Blaze, EscortGoal> INSTALLED_ESCORT_GOALS = new WeakHashMap<>();
    private static final Map<Blaze, EscortCombatGoal> INSTALLED_COMBAT_GOALS =
            new WeakHashMap<>();

    private WildfireFormation() {
    }

    static Goal createPatrolGoal(Wildfire wildfire) {
        return new PatrolGoal(wildfire);
    }

    static void installEscort(Blaze blaze, UUID leaderId) {
        WildfireEncounterTargets.markEscort(blaze);
        EscortGoal installed = INSTALLED_ESCORT_GOALS.get(blaze);
        EscortCombatGoal installedCombat = INSTALLED_COMBAT_GOALS.get(blaze);

        if (installed != null
                && installed.hasLeader(leaderId)
                && installedCombat != null
                && installedCombat.hasLeader(leaderId)) {
            return;
        }

        if (installed != null) {
            blaze.getGoalSelector().removeGoal(installed);
        }

        if (installedCombat != null) {
            blaze.getGoalSelector().removeGoal(installedCombat);
        }

        EscortGoal replacement = new EscortGoal(blaze, leaderId);
        EscortCombatGoal combatReplacement = new EscortCombatGoal(blaze, leaderId);
        blaze.getGoalSelector().addGoal(GOAL_PRIORITY, replacement);
        blaze.getGoalSelector().addGoal(GOAL_PRIORITY, combatReplacement);
        INSTALLED_ESCORT_GOALS.put(blaze, replacement);
        INSTALLED_COMBAT_GOALS.put(blaze, combatReplacement);
    }

    static Vec3 followPosition(Wildfire leader, int slot) {
        return followPosition(leader.position(), leader.formationDirection(), slot);
    }

    static Vec3 followPosition(Vec3 leaderPosition, Vec3 direction, int slot) {
        Vec3 horizontal = new Vec3(direction.x, 0.0D, direction.z).normalize();
        return leaderPosition.subtract(horizontal.scale(ESCORT_SPACING * (slot + 1)));
    }

    static Vec3 combatFollowPosition(Wildfire leader, int slot) {
        LivingEntity target = leader.getTarget();
        Vec3 forward = target == null
                ? leader.formationDirection()
                : target.position().subtract(leader.position());
        Vec3 horizontal = new Vec3(forward.x, 0.0D, forward.z);

        if (horizontal.lengthSqr() < 0.01D) {
            horizontal = leader.formationDirection();
        }

        horizontal = horizontal.normalize();
        Vec3 lateralDirection = new Vec3(-horizontal.z, 0.0D, horizontal.x);
        int row = slot / 2;
        double side = slot % 2 == 0 ? -1.0D : 1.0D;
        double distanceBehind = row * 0.5D;
        double lateralDistance = side * (2.0D + row * 0.75D);

        return leader.position()
                .subtract(horizontal.scale(distanceBehind))
                .add(lateralDirection.scale(lateralDistance));
    }

    private static boolean canOwnFormationMovement(Mob mob) {
        return RetoldAiControl.getMode(mob) == RetoldAiControlMode.NONE
                || RetoldAiControl.isControlledAsBy(
                mob,
                RetoldAiControlMode.REGROUP,
                RetoldAiControlOwner.WILDFIRE_FORMATION
        );
    }

    private static boolean claimFormationMovement(Mob mob, long gameTime) {
        return RetoldAiControl.tryClaim(
                mob,
                RetoldAiControlMode.REGROUP,
                RetoldAiControlOwner.WILDFIRE_FORMATION,
                gameTime,
                CONTROL_TICKS
        );
    }

    private static void releaseFormationMovement(Mob mob) {
        if (!RetoldAiControl.isControlledBy(
                mob,
                RetoldAiControlOwner.WILDFIRE_FORMATION
        )) {
            return;
        }

        mob.getNavigation().stop();
        mob.getMoveControl().setWantedPosition(
                mob.getX(),
                mob.getY(),
                mob.getZ(),
                0.0D
        );
        RetoldBehaviorMovement.clearFlyingPath(mob);
        RetoldAiControl.clearIfOwnedBy(mob, RetoldAiControlOwner.WILDFIRE_FORMATION);
    }

    private static boolean canOwnEscortCombatMovement(Mob mob) {
        return RetoldAiControl.isControlledBy(
                mob,
                RetoldAiControlOwner.WILDFIRE_ESCORT_COMBAT
        ) || RetoldAiControl.getPriority(mob) <= RetoldAiPriorities.SPECIAL_RANGED;
    }

    private static boolean claimEscortCombatMovement(Mob mob, long gameTime) {
        return RetoldAiControl.tryClaim(
                mob,
                RetoldAiControlMode.ATTACK,
                RetoldAiControlOwner.WILDFIRE_ESCORT_COMBAT,
                RetoldAiPriorities.SPECIAL_RANGED,
                "wildfire_escort_combat_follow",
                gameTime,
                CONTROL_TICKS
        );
    }

    private static void releaseEscortCombatMovement(Mob mob) {
        if (!RetoldAiControl.isControlledBy(
                mob,
                RetoldAiControlOwner.WILDFIRE_ESCORT_COMBAT
        )) {
            return;
        }

        mob.getMoveControl().setWait();
        RetoldBehaviorMovement.clearFlyingPath(mob);
        RetoldAiControl.clearIfOwnedBy(
                mob,
                RetoldAiControlOwner.WILDFIRE_ESCORT_COMBAT
        );
    }

    private static void followFlyingPath(
            Mob mob,
            Vec3 destination,
            double speed,
            long gameTime,
            double destinationToleranceSquared
    ) {
        double remainingClimb = destination.y - mob.getY();

        if (remainingClimb > 0.5D
                && mob.level().noCollision(
                mob,
                mob.getBoundingBox().move(
                        0.0D,
                        Math.min(1.0D, remainingClimb),
                        0.0D
                )
        )) {
            RetoldBehaviorMovement.clearFlyingPath(mob);
            mob.getMoveControl().setWantedPosition(
                    mob.getX(),
                    destination.y,
                    mob.getZ(),
                    speed
            );
            Vec3 movement = mob.getDeltaMovement();
            mob.setDeltaMovement(
                    movement.x * 0.5D,
                    Math.max(movement.y, PATROL_CLIMB_SPEED),
                    movement.z * 0.5D
            );
            return;
        }

        if (!RetoldBehaviorMovement.requestFlyingPath(
                mob,
                destination,
                gameTime,
                FLYING_PATH_REFRESH_TICKS,
                destinationToleranceSquared
        )) {
            mob.getMoveControl().setWait();
            return;
        }

        Vec3 waypoint = RetoldBehaviorMovement.nextFlyingWaypoint(mob);

        if (waypoint == null) {
            mob.getMoveControl().setWait();
            return;
        }

        mob.getMoveControl().setWantedPosition(
                waypoint.x,
                waypoint.y,
                waypoint.z,
                speed
        );
    }

    private static boolean climbBlockedCombatCorridor(
            Mob mob,
            Vec3 destination,
            double speed
    ) {
        Vec3 horizontalOffset = new Vec3(
                destination.x - mob.getX(),
                0.0D,
                destination.z - mob.getZ()
        );

        if (horizontalOffset.lengthSqr() < 0.25D) {
            return false;
        }

        Vec3 forwardStep = horizontalOffset.normalize().scale(
                Math.min(1.0D, Math.sqrt(horizontalOffset.lengthSqr()))
        );

        if (mob.level().noCollision(
                mob,
                mob.getBoundingBox().move(forwardStep)
        ) || !mob.level().noCollision(
                mob,
                mob.getBoundingBox().move(0.0D, 1.0D, 0.0D)
        )) {
            return false;
        }

        RetoldBehaviorMovement.clearFlyingPath(mob);
        mob.getMoveControl().setWantedPosition(
                mob.getX(),
                mob.getY() + 2.0D,
                mob.getZ(),
                speed
        );
        Vec3 movement = mob.getDeltaMovement();
        mob.setDeltaMovement(
                movement.x * 0.35D,
                Math.max(movement.y, PATROL_CLIMB_SPEED),
                movement.z * 0.35D
        );
        return true;
    }

    private static final class PatrolGoal extends Goal {
        private final Wildfire wildfire;
        private Vec3 destination;
        private int nextAttemptTick;
        private int segmentTicks;

        private PatrolGoal(Wildfire wildfire) {
            this.wildfire = wildfire;
            // Run above vanilla Blaze wandering so a stuck roam cannot retain MOVE indefinitely.
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!isAvailable() || wildfire.tickCount < nextAttemptTick) {
                return false;
            }

            destination = findAirborneDestination();

            if (destination == null) {
                nextAttemptTick = wildfire.tickCount + FAILED_PATROL_RETRY_TICKS;
                return false;
            }

            wildfire.setFormationDirection(destination.subtract(wildfire.position()));
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return isAvailable()
                    && destination != null
                    && segmentTicks > 0
                    && wildfire.distanceToSqr(destination) > 2.0D;
        }

        @Override
        public void start() {
            if (!claimFormationMovement(wildfire, wildfire.level().getGameTime())) {
                destination = null;
                return;
            }

            segmentTicks = PATROL_SEGMENT_TICKS;
            followPatrolRoute();
        }

        @Override
        public void tick() {
            segmentTicks--;
            RetoldAiControl.refreshIfOwnedBy(
                    wildfire,
                    RetoldAiControlMode.REGROUP,
                    RetoldAiControlOwner.WILDFIRE_FORMATION,
                    wildfire.level().getGameTime(),
                    CONTROL_TICKS
            );
            followPatrolRoute();
        }

        @Override
        public void stop() {
            destination = null;
            segmentTicks = 0;
            releaseFormationMovement(wildfire);
        }

        private boolean isAvailable() {
            return wildfire.level() instanceof ServerLevel level
                    && wildfire.isAlive()
                    && wildfire.getTarget() == null
                    && wildfire.hasLoadedFormationCompanion(level)
                    && canOwnFormationMovement(wildfire);
        }

        private void followPatrolRoute() {
            followFlyingPath(
                    wildfire,
                    destination,
                    PATROL_SPEED,
                    wildfire.level().getGameTime(),
                    LEADER_PATH_DESTINATION_TOLERANCE_SQUARED
            );
        }

        private Vec3 findAirborneDestination() {
            Vec3 direction = wildfire.formationDirection();
            double baseAngle = Math.atan2(direction.z, direction.x);
            ServerLevel level = (ServerLevel) wildfire.level();

            for (double turnOffset : PATROL_TURN_OFFSETS) {
                double angle = baseAngle + turnOffset;
                Vec3 horizontalOffset = new Vec3(
                        Math.cos(angle) * PATROL_DISTANCE,
                        0.0D,
                        Math.sin(angle) * PATROL_DISTANCE
                );

                for (double climbOffset : PATROL_CLIMB_OFFSETS) {
                    Vec3 candidate = wildfire.position()
                            .add(horizontalOffset)
                            .add(0.0D, climbOffset, 0.0D);

                    if (hasClearPatrolCorridor(
                            level,
                            horizontalOffset,
                            climbOffset,
                            candidate
                    )) {
                        return candidate;
                    }
                }
            }

            return null;
        }

        private boolean hasClearPatrolCorridor(
                ServerLevel level,
                Vec3 horizontalOffset,
                double climbOffset,
                Vec3 candidate
        ) {
            if (!level.hasChunkAt(BlockPos.containing(candidate))) {
                return false;
            }

            for (double rise = 1.0D; rise <= climbOffset; rise += 1.0D) {
                if (!level.noCollision(
                        wildfire,
                        wildfire.getBoundingBox().move(0.0D, rise, 0.0D)
                )) {
                    return false;
                }
            }

            int horizontalSteps = Math.max(1, (int) Math.ceil(horizontalOffset.length()));

            for (int step = 1; step <= horizontalSteps; step++) {
                double progress = (double) step / horizontalSteps;
                Vec3 offset = horizontalOffset.scale(progress).add(0.0D, climbOffset, 0.0D);
                BlockPos stepPosition = BlockPos.containing(wildfire.position().add(offset));

                if (!level.hasChunkAt(stepPosition)
                        || !level.noCollision(
                        wildfire,
                        wildfire.getBoundingBox().move(offset)
                )) {
                    return false;
                }
            }

            return true;
        }
    }

    private static final class EscortGoal extends Goal {
        private final Blaze blaze;
        private final UUID leaderId;

        private EscortGoal(Blaze blaze, UUID leaderId) {
            this.blaze = blaze;
            this.leaderId = leaderId;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return findLeader() != null && canOwnFormationMovement(blaze);
        }

        @Override
        public boolean canContinueToUse() {
            return findLeader() != null && canOwnFormationMovement(blaze);
        }

        @Override
        public void start() {
            claimFormationMovement(blaze, blaze.level().getGameTime());
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Wildfire leader = findLeader();

            if (leader == null || !claimFormationMovement(blaze, blaze.level().getGameTime())) {
                return;
            }

            int slot = leader.formationSlot(blaze.getUUID());
            Vec3 destination = followPosition(leader, slot);

            if (blaze.distanceToSqr(destination) <= ESCORT_POSITION_TOLERANCE_SQUARED) {
                RetoldBehaviorMovement.clearFlyingPath(blaze);
                blaze.getMoveControl().setWantedPosition(
                        blaze.getX(),
                        blaze.getY(),
                        blaze.getZ(),
                        0.0D
                );
                return;
            }

            followFlyingPath(
                    blaze,
                    destination,
                    ESCORT_SPEED,
                    blaze.level().getGameTime(),
                    ESCORT_PATH_DESTINATION_TOLERANCE_SQUARED
            );
        }

        @Override
        public void stop() {
            releaseFormationMovement(blaze);
        }

        private Wildfire findLeader() {
            if (!(blaze.level() instanceof ServerLevel level)
                    || !blaze.isAlive()
                    || blaze.getTarget() != null) {
                return null;
            }

            Entity entity = level.getEntity(leaderId);

            if (!(entity instanceof Wildfire leader)
                    || !leader.isAlive()
                    || leader.getTarget() != null
                    || leader.formationSlot(blaze.getUUID()) < 0) {
                return null;
            }

            return leader;
        }

        private boolean hasLeader(UUID candidateLeaderId) {
            return leaderId.equals(candidateLeaderId);
        }
    }

    private static final class EscortCombatGoal extends Goal {
        private final Blaze blaze;
        private final UUID leaderId;

        private EscortCombatGoal(Blaze blaze, UUID leaderId) {
            this.blaze = blaze;
            this.leaderId = leaderId;
            // BlazeAttackGoal owns MOVE and LOOK even though it supplies no useful pursuit.
            // Staying flagless lets combat-follow flight coexist with the vanilla fireball cycle.
        }

        @Override
        public boolean canUse() {
            return findCombatLeader() != null && canOwnEscortCombatMovement(blaze);
        }

        @Override
        public boolean canContinueToUse() {
            return findCombatLeader() != null && canOwnEscortCombatMovement(blaze);
        }

        @Override
        public void start() {
            claimEscortCombatMovement(blaze, blaze.level().getGameTime());
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Wildfire leader = findCombatLeader();

            if (leader == null
                    || !claimEscortCombatMovement(blaze, blaze.level().getGameTime())) {
                return;
            }

            LivingEntity leaderTarget = leader.getTarget();

            if (blaze.getTarget() == null && leaderTarget != null) {
                RetoldFactionTargetMemory.trySetTarget(
                        blaze,
                        leaderTarget,
                        RetoldTargetSource.FACTION_ASSIST
                );
            }

            int slot = leader.formationSlot(blaze.getUUID());
            Vec3 destination = combatFollowPosition(leader, slot);

            if (climbBlockedCombatCorridor(blaze, destination, ESCORT_COMBAT_SPEED)) {
                return;
            }

            if (blaze.distanceToSqr(destination)
                    <= ESCORT_COMBAT_POSITION_TOLERANCE_SQUARED) {
                RetoldBehaviorMovement.clearFlyingPath(blaze);
                blaze.getMoveControl().setWait();
                return;
            }

            followFlyingPath(
                    blaze,
                    destination,
                    ESCORT_COMBAT_SPEED,
                    blaze.level().getGameTime(),
                    ESCORT_PATH_DESTINATION_TOLERANCE_SQUARED
            );
        }

        @Override
        public void stop() {
            releaseEscortCombatMovement(blaze);
        }

        private Wildfire findCombatLeader() {
            if (!(blaze.level() instanceof ServerLevel level) || !blaze.isAlive()) {
                return null;
            }

            Entity entity = level.getEntity(leaderId);

            if (!(entity instanceof Wildfire leader)
                    || !leader.isAlive()
                    || leader.isLavaRecoveryActive()
                    || leader.getTarget() == null
                    || !leader.getTarget().isAlive()
                    || leader.formationSlot(blaze.getUUID()) < 0) {
                return null;
            }

            return leader;
        }

        private boolean hasLeader(UUID candidateLeaderId) {
            return leaderId.equals(candidateLeaderId);
        }
    }
}
