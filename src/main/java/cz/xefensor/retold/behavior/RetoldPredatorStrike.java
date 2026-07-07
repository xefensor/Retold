package cz.xefensor.retold.behavior;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public final class RetoldPredatorStrike {
    private static final Map<PathfinderMob, StrikeState> STRIKES = new WeakHashMap<>();

    private RetoldPredatorStrike() {
    }

    public static boolean tryStrike(
            ServerLevel level,
            PathfinderMob predator,
            LivingEntity prey,
            long gameTime
    ) {
        if (level == null || predator == null || prey == null) {
            return false;
        }

        if (!predator.isAlive() || predator.isRemoved()) {
            clear(predator);
            return false;
        }

        if (!prey.isAlive() || prey.isRemoved()) {
            clear(predator);
            return false;
        }

        StrikeState state = STRIKES.get(predator);

        if (state != null && state.recoveryUntil() > gameTime) {
            return false;
        }

        StrikeProfile profile = getProfile(predator);
        double distanceSquared = predator.distanceToSqr(prey);

        if (distanceSquared > profile.strikeStartRangeSquared()) {
            return false;
        }

        if (state != null && state.nextStrikeAt() > gameTime) {
            return false;
        }

        Vec3 predicted = predictPreyPosition(
                prey,
                profile
        );

        facePredictedPosition(
                predator,
                predicted
        );

        lungeTowardPrediction(
                predator,
                prey,
                predicted,
                profile
        );

        predator.swing(InteractionHand.MAIN_HAND);

        boolean hit = canHitPredictedTarget(
                predator,
                prey,
                predicted,
                profile
        );

        long nextStrikeAt = gameTime + profile.cooldownTicks();
        long recoveryUntil = gameTime + profile.missRecoveryTicks();

        if (hit) {
            predator.doHurtTarget(
                    level,
                    prey
            );

            recoveryUntil = gameTime + Math.max(
                    2,
                    profile.missRecoveryTicks() / 2
            );
        }

        STRIKES.put(
                predator,
                new StrikeState(
                        prey.getUUID(),
                        nextStrikeAt,
                        recoveryUntil
                )
        );

        return true;
    }

    public static double chaseSpeedMultiplier(
            PathfinderMob predator,
            long gameTime
    ) {
        StrikeState state = STRIKES.get(predator);

        if (state == null) {
            return 1.0D;
        }

        if (state.recoveryUntil() > gameTime) {
            return 0.76D;
        }

        return 1.0D;
    }

    public static boolean isRecovering(
            PathfinderMob predator,
            long gameTime
    ) {
        StrikeState state = STRIKES.get(predator);

        return state != null
                && state.recoveryUntil() > gameTime;
    }

    public static void clear(PathfinderMob predator) {
        if (predator == null) {
            return;
        }

        STRIKES.remove(predator);
    }

    private static Vec3 predictPreyPosition(
            LivingEntity prey,
            StrikeProfile profile
    ) {
        Vec3 movement = prey.getDeltaMovement();

        return prey.position().add(
                movement.x * profile.leadTicks(),
                movement.y * Math.min(
                        2.0D,
                        profile.leadTicks()
                ),
                movement.z * profile.leadTicks()
        );
    }

    private static void facePredictedPosition(
            PathfinderMob predator,
            Vec3 predicted
    ) {
        predator.getLookControl().setLookAt(
                predicted.x,
                predicted.y + predator.getBbHeight() * 0.35D,
                predicted.z,
                40.0F,
                40.0F
        );
    }

    private static void lungeTowardPrediction(
            PathfinderMob predator,
            LivingEntity prey,
            Vec3 predicted,
            StrikeProfile profile
    ) {
        Vec3 current = predator.getDeltaMovement();

        Vec3 toward = new Vec3(
                predicted.x - predator.getX(),
                0.0D,
                predicted.z - predator.getZ()
        );

        if (toward.lengthSqr() <= 0.0001D) {
            toward = new Vec3(
                    prey.getX() - predator.getX(),
                    0.0D,
                    prey.getZ() - predator.getZ()
            );
        }

        if (toward.lengthSqr() <= 0.0001D) {
            return;
        }

        Vec3 direction = toward.normalize();

        double preyHorizontalSpeed = horizontalSpeed(prey);
        double speedBonus = Math.min(
                0.18D,
                preyHorizontalSpeed * 0.35D
        );

        double lungeStrength = profile.lungeStrength() + speedBonus;

        double yBoost = profile.jumpLift();

        if (!predator.onGround()) {
            yBoost *= 0.35D;
        }

        predator.setDeltaMovement(
                current.x * 0.35D + direction.x * lungeStrength,
                Math.max(
                        current.y,
                        yBoost
                ),
                current.z * 0.35D + direction.z * lungeStrength
        );

        predator.setSprinting(true);
    }

    private static boolean canHitPredictedTarget(
            PathfinderMob predator,
            LivingEntity prey,
            Vec3 predicted,
            StrikeProfile profile
    ) {
        double preySpeed = horizontalSpeed(prey);

        /*
         * Fast prey gets a slightly wider intercept window,
         * because the predator is aiming where the prey will be.
         */
        double dynamicHitRange = profile.hitRange()
                + Math.min(
                profile.maxLeadBonus(),
                preySpeed * profile.leadTicks() * 0.45D
        );

        if (predator.distanceToSqr(prey) <= dynamicHitRange * dynamicHitRange) {
            return true;
        }

        /*
         * Intercept check:
         * predator may connect if it lunges into the predicted path,
         * but the real prey still needs to be near enough.
         */
        double predictedRange = dynamicHitRange + 0.55D;
        double realPreyLimit = dynamicHitRange + 0.85D;

        return predator.position().distanceToSqr(predicted) <= predictedRange * predictedRange
                && predator.distanceToSqr(prey) <= realPreyLimit * realPreyLimit;
    }

    private static double horizontalSpeed(LivingEntity entity) {
        Vec3 movement = entity.getDeltaMovement();

        return Math.sqrt(
                movement.x * movement.x + movement.z * movement.z
        );
    }

    private static StrikeProfile getProfile(PathfinderMob predator) {
        String path = RetoldMobRules.getEntityTypePath(
                predator.getType()
        );

        if (path.equals("cat") || path.equals("ocelot")) {
            return new StrikeProfile(
                    3.8D,
                    1.95D,
                    5.0D,
                    0.54D,
                    0.20D,
                    9,
                    5,
                    0.95D
            );
        }

        if (path.equals("fox")) {
            return new StrikeProfile(
                    3.6D,
                    2.05D,
                    4.5D,
                    0.48D,
                    0.18D,
                    11,
                    5,
                    0.9D
            );
        }

        if (path.equals("spider") || path.equals("cave_spider")) {
            return new StrikeProfile(
                    3.0D,
                    2.0D,
                    3.0D,
                    0.36D,
                    0.08D,
                    14,
                    7,
                    0.75D
            );
        }

        if (path.equals("dolphin")) {
            return new StrikeProfile(
                    4.0D,
                    2.15D,
                    4.0D,
                    0.58D,
                    0.0D,
                    10,
                    5,
                    1.0D
            );
        }

        /*
         * Wolf / default predator.
         */
        return new StrikeProfile(
                3.5D,
                2.1D,
                4.0D,
                0.46D,
                0.16D,
                12,
                6,
                0.9D
        );
    }

    private record StrikeProfile(
            double strikeStartRange,
            double hitRange,
            double leadTicks,
            double lungeStrength,
            double jumpLift,
            int cooldownTicks,
            int missRecoveryTicks,
            double maxLeadBonus
    ) {
        public double strikeStartRangeSquared() {
            return strikeStartRange * strikeStartRange;
        }
    }

    private record StrikeState(
            UUID preyUuid,
            long nextStrikeAt,
            long recoveryUntil
    ) {
    }
}