package cz.xefensor.retold.worldgen.fire;

import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.control.RetoldAiControlMode;
import cz.xefensor.retold.behavior.control.RetoldAiControlOwner;
import cz.xefensor.retold.behavior.control.RetoldAiPriorities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

/** Controls the Wildfire's lava depth and movement during its ranged attack cycle. */
final class WildfireMovement {
    private static final int COMBAT_CONTROL_TICKS = 25;
    private static final int REPOSITION_INTERVAL_TICKS = 40;
    private static final double COMBAT_MOVEMENT_SPEED = 1.2D;
    private static final double IDEAL_COMBAT_RANGE = 12.0D;
    private static final double MIN_COMBAT_RANGE = 7.0D;
    private static final double MAX_COMBAT_RANGE = 20.0D;
    private static final double DESTINATION_TOLERANCE_SQUARED = 2.0D * 2.0D;
    private static final double COMBAT_HEIGHT_ABOVE_TARGET = 3.0D;
    private static final double MAX_COMBAT_HEIGHT_CHANGE = 2.0D;
    private static final double SURFACE_LIFT = 0.10D;
    private static final double SUBMERGED_LIFT = 0.18D;
    private static final double RECOVERY_DIVE_SPEED = -0.14D;

    private WildfireMovement() {
    }

    static Goal createCombatGoal(Wildfire wildfire) {
        return new CombatRepositionGoal(wildfire);
    }

    static void moveTowardRecoverySource(
            Wildfire wildfire,
            Vec3 destination,
            double speed
    ) {
        Vec3 offset = destination.subtract(wildfire.position());

        if (offset.lengthSqr() <= 0.25D) {
            return;
        }

        Vec3 step = offset.normalize().scale(0.075D * speed);
        // Blaze movement control is tied to vanilla goals and may discard a Retold-owned request.
        // A small collision-aware flight step keeps the shelter route authoritative without
        // reserving Goal.MOVE and starving ordinary patrol movement while recovery is inactive.
        wildfire.move(MoverType.SELF, step);
    }

    static void tickLavaMovement(
            Wildfire wildfire,
            boolean recovering,
            boolean recoverySubmerged
    ) {
        if (!isTouchingLava(wildfire)) {
            return;
        }

        Vec3 movement = wildfire.getDeltaMovement();

        if (recovering) {
            wildfire.getMoveControl().setWait();
            wildfire.setDeltaMovement(
                    0.0D,
                    recoverySubmerged
                            ? 0.0D
                            : RECOVERY_DIVE_SPEED,
                    0.0D
            );
            return;
        }

        double lift = wildfire.isEyeInFluid(FluidTags.LAVA)
                ? SUBMERGED_LIFT
                : SURFACE_LIFT;

        if (movement.y < lift) {
            wildfire.setDeltaMovement(movement.x, lift, movement.z);
        }
    }

    static boolean isTouchingLava(Wildfire wildfire) {
        return wildfire.isInLava()
                || wildfire.level().getFluidState(wildfire.blockPosition()).is(FluidTags.LAVA);
    }

    private static boolean canOwnCombatMovement(Wildfire wildfire) {
        return RetoldAiControl.isControlledBy(
                wildfire,
                RetoldAiControlOwner.WILDFIRE_COMBAT
        ) || RetoldAiControl.getPriority(wildfire) <= RetoldAiPriorities.SPECIAL_RANGED;
    }

    private static boolean claimCombatMovement(Wildfire wildfire) {
        return RetoldAiControl.tryClaim(
                wildfire,
                RetoldAiControlMode.ATTACK,
                RetoldAiControlOwner.WILDFIRE_COMBAT,
                RetoldAiPriorities.SPECIAL_RANGED,
                "wildfire_reposition",
                wildfire.level().getGameTime(),
                COMBAT_CONTROL_TICKS
        );
    }

    private static void releaseCombatMovement(Wildfire wildfire) {
        if (!RetoldAiControl.isControlledBy(
                wildfire,
                RetoldAiControlOwner.WILDFIRE_COMBAT
        )) {
            return;
        }

        wildfire.getMoveControl().setWait();
        RetoldAiControl.clearIfOwnedBy(
                wildfire,
                RetoldAiControlOwner.WILDFIRE_COMBAT
        );
    }

    private static final class CombatRepositionGoal extends Goal {
        private final Wildfire wildfire;
        private Vec3 destination;
        private int nextRepositionTick;
        private double orbitDirection;

        private CombatRepositionGoal(Wildfire wildfire) {
            this.wildfire = wildfire;
            // BlazeAttackGoal privately owns MOVE and LOOK even though it never supplies movement.
            // Keeping this goal flagless lets it reposition without replacing the vanilla fireball cycle.
        }

        @Override
        public boolean canUse() {
            return hasValidTarget() && canOwnCombatMovement(wildfire);
        }

        @Override
        public boolean canContinueToUse() {
            return hasValidTarget() && canOwnCombatMovement(wildfire);
        }

        @Override
        public void start() {
            orbitDirection = wildfire.getRandom().nextBoolean() ? 1.0D : -1.0D;
            nextRepositionTick = 0;
            destination = null;
            claimCombatMovement(wildfire);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = wildfire.getTarget();

            if (target == null || !claimCombatMovement(wildfire)) {
                return;
            }

            if (destination == null
                    || wildfire.tickCount >= nextRepositionTick
                    || wildfire.distanceToSqr(destination) <= DESTINATION_TOLERANCE_SQUARED
                    || !isDestinationClear(destination)) {
                destination = findDestination(target);
                nextRepositionTick = wildfire.tickCount + REPOSITION_INTERVAL_TICKS;
            }

            if (destination != null) {
                wildfire.getMoveControl().setWantedPosition(
                        destination.x,
                        destination.y,
                        destination.z,
                        COMBAT_MOVEMENT_SPEED
                );
            }
        }

        @Override
        public void stop() {
            destination = null;
            releaseCombatMovement(wildfire);
        }

        private boolean hasValidTarget() {
            LivingEntity target = wildfire.getTarget();
            return wildfire.level() instanceof ServerLevel
                    && wildfire.isAlive()
                    && target != null
                    && target.isAlive()
                    && wildfire.canAttack(target);
        }

        private Vec3 findDestination(LivingEntity target) {
            Vec3 relative = wildfire.position().subtract(target.position());
            Vec3 horizontal = new Vec3(relative.x, 0.0D, relative.z);

            if (horizontal.horizontalDistanceSqr() < 0.01D) {
                double randomAngle = wildfire.getRandom().nextDouble() * Math.PI * 2.0D;
                horizontal = new Vec3(Math.cos(randomAngle), 0.0D, Math.sin(randomAngle));
            }

            double currentRange = Math.sqrt(horizontal.horizontalDistanceSqr());
            double radius = currentRange < MIN_COMBAT_RANGE || currentRange > MAX_COMBAT_RANGE
                    ? IDEAL_COMBAT_RANGE
                    : Mth.clamp(currentRange, MIN_COMBAT_RANGE, MAX_COMBAT_RANGE);
            double baseAngle = Math.atan2(horizontal.z, horizontal.x);
            double wantedY = Mth.clamp(
                    target.getY() + COMBAT_HEIGHT_ABOVE_TARGET,
                    wildfire.getY() - MAX_COMBAT_HEIGHT_CHANGE,
                    wildfire.getY() + MAX_COMBAT_HEIGHT_CHANGE
            );

            for (int attempt = 0; attempt < 6; attempt++) {
                double angle = baseAngle + orbitDirection
                        * (Math.PI / 3.0D + attempt * Math.PI / 6.0D);
                Vec3 candidate = new Vec3(
                        target.getX() + Math.cos(angle) * radius,
                        wantedY,
                        target.getZ() + Math.sin(angle) * radius
                );

                if (isDestinationClear(candidate)) {
                    if (attempt >= 3) {
                        orbitDirection = -orbitDirection;
                    }

                    return candidate;
                }
            }

            return null;
        }

        private boolean isDestinationClear(Vec3 candidate) {
            ServerLevel level = (ServerLevel) wildfire.level();
            BlockPos pos = BlockPos.containing(candidate);

            return level.hasChunkAt(pos)
                    && !level.getFluidState(pos).is(FluidTags.LAVA)
                    && level.noCollision(
                            wildfire,
                            wildfire.getBoundingBox().move(
                                    candidate.subtract(wildfire.position())
                            )
                    );
        }
    }
}
