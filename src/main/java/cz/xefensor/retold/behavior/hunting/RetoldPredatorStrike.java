package cz.xefensor.retold.behavior.hunting;

import cz.xefensor.retold.behavior.profiles.RetoldMobRules;

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

    private static final double EPSILON = 0.0001D;

    private RetoldPredatorStrike() {
    }

    public static boolean tryStrike(
            ServerLevel level,
            PathfinderMob predator,
            LivingEntity prey,
            long gameTime
    ) {
        if (
                level == null
                        || predator == null
                        || prey == null
                        || !predator.isAlive()
                        || predator.isRemoved()
                        || !prey.isAlive()
                        || prey.isRemoved()
                        || predator.level() != prey.level()
        ) {
            return false;
        }

        StrikeProfile profile = getProfile(
                predator
        );

        StrikeState state = STRIKES.get(predator);

        if (state != null) {
            /*
             * Recovery prevents instant repeated strike attempts,
             * but it does not slow movement anymore.
             */
            if (state.recoveryUntil > gameTime) {
                return false;
            }

            /*
             * Main anti-spam rule:
             * after a successful bite, the predator must chase/re-aim before biting again.
             */
            if (
                    state.preyId.equals(prey.getUUID())
                            && state.nextStrikeAt > gameTime
            ) {
                return false;
            }
        }

        double distanceSquared = predator.distanceToSqr(prey);

        if (distanceSquared > profile.strikeStartRangeSquared()) {
            return false;
        }

        Vec3 interceptPosition = calculateInterceptPosition(
                predator,
                prey,
                profile
        );

        facePredictedPosition(
                predator,
                interceptPosition
        );

        Vec3 lungeVelocity = calculateLungeVelocity(
                predator,
                prey,
                interceptPosition,
                profile
        );

        applyLungeVelocity(
                predator,
                interceptPosition,
                lungeVelocity,
                profile
        );

        predator.swing(
                InteractionHand.MAIN_HAND
        );

        boolean hit = canHitWithLunge(
                predator,
                prey,
                interceptPosition,
                lungeVelocity,
                profile
        );

        if (hit) {
            RetoldPredatorAttackGuards.doRetoldPredatorHurt(
                    predator,
                    prey
            );
        }

        STRIKES.put(
                predator,
                new StrikeState(
                        prey.getUUID(),
                        hit
                                ? gameTime + profile.hitCooldownTicks()
                                : gameTime + profile.missCooldownTicks(),
                        hit
                                ? gameTime + profile.hitRecoveryTicks()
                                : gameTime + profile.missRecoveryTicks()
                )
        );

        return true;
    }

    public static double chaseSpeedMultiplier(
            PathfinderMob predator,
            long gameTime
    ) {
        /*
         * Do not slow predators in strike range.
         * Cooldown controls attack rhythm, not movement speed.
         */
        return 1.0D;
    }

    public static boolean isRecovering(
            PathfinderMob predator,
            long gameTime
    ) {
        StrikeState state = STRIKES.get(predator);

        return state != null
                && state.recoveryUntil > gameTime;
    }

    public static void clear(PathfinderMob predator) {
        STRIKES.remove(predator);
    }

    private static Vec3 calculateInterceptPosition(
            PathfinderMob predator,
            LivingEntity prey,
            StrikeProfile profile
    ) {
        Vec3 predatorPosition = predator.position();
        Vec3 preyPosition = prey.position();

        Vec3 preyVelocity = getCappedHorizontalVelocity(
                prey,
                profile.maxPreyLeadSpeed()
        );

        Vec3 horizontalRelativePosition = new Vec3(
                preyPosition.x - predatorPosition.x,
                0.0D,
                preyPosition.z - predatorPosition.z
        );

        double interceptTicks = solveInterceptTicks(
                horizontalRelativePosition,
                preyVelocity,
                profile.interceptSpeed(),
                profile.minLeadTicks(),
                profile.maxLeadTicks()
        );

        Vec3 predicted = preyPosition.add(
                preyVelocity.scale(interceptTicks)
        );

        return new Vec3(
                predicted.x,
                preyPosition.y,
                predicted.z
        );
    }

    private static double solveInterceptTicks(
            Vec3 relativePosition,
            Vec3 targetVelocity,
            double interceptSpeed,
            double minLeadTicks,
            double maxLeadTicks
    ) {
        double a = targetVelocity.lengthSqr() - interceptSpeed * interceptSpeed;
        double b = 2.0D * dotHorizontal(
                relativePosition,
                targetVelocity
        );
        double c = relativePosition.lengthSqr();

        double fallbackTicks = Math.sqrt(c) / Math.max(
                interceptSpeed,
                EPSILON
        );

        double solvedTicks = fallbackTicks;

        if (Math.abs(a) < EPSILON) {
            if (Math.abs(b) > EPSILON) {
                double linearTicks = -c / b;

                if (linearTicks > 0.0D && Double.isFinite(linearTicks)) {
                    solvedTicks = linearTicks;
                }
            }
        } else {
            double discriminant = b * b - 4.0D * a * c;

            if (discriminant >= 0.0D) {
                double sqrt = Math.sqrt(discriminant);

                double t1 = (-b - sqrt) / (2.0D * a);
                double t2 = (-b + sqrt) / (2.0D * a);

                solvedTicks = chooseBestPositiveTicks(
                        t1,
                        t2,
                        fallbackTicks
                );
            }
        }

        return clamp(
                solvedTicks,
                minLeadTicks,
                maxLeadTicks
        );
    }

    private static double chooseBestPositiveTicks(
            double first,
            double second,
            double fallback
    ) {
        boolean firstValid = first > 0.0D && Double.isFinite(first);
        boolean secondValid = second > 0.0D && Double.isFinite(second);

        if (firstValid && secondValid) {
            return Math.min(
                    first,
                    second
            );
        }

        if (firstValid) {
            return first;
        }

        if (secondValid) {
            return second;
        }

        return fallback;
    }

    private static Vec3 getCappedHorizontalVelocity(
            LivingEntity prey,
            double maxSpeed
    ) {
        Vec3 movement = prey.getDeltaMovement();

        Vec3 horizontalMovement = new Vec3(
                movement.x,
                0.0D,
                movement.z
        );

        double speed = horizontalMovement.length();

        if (speed <= maxSpeed) {
            return horizontalMovement;
        }

        if (speed <= EPSILON) {
            return Vec3.ZERO;
        }

        return horizontalMovement.normalize()
                .scale(maxSpeed);
    }

    private static double dotHorizontal(
            Vec3 first,
            Vec3 second
    ) {
        return first.x * second.x
                + first.z * second.z;
    }

    private static void facePredictedPosition(
            PathfinderMob predator,
            Vec3 predictedPosition
    ) {
        predator.getLookControl().setLookAt(
                predictedPosition.x,
                predictedPosition.y + 0.25D,
                predictedPosition.z,
                55.0F,
                55.0F
        );
    }

    private static Vec3 calculateLungeVelocity(
            PathfinderMob predator,
            LivingEntity prey,
            Vec3 interceptPosition,
            StrikeProfile profile
    ) {
        Vec3 toIntercept = interceptPosition.subtract(
                predator.position()
        );

        Vec3 horizontalToIntercept = new Vec3(
                toIntercept.x,
                0.0D,
                toIntercept.z
        );

        if (horizontalToIntercept.lengthSqr() <= EPSILON) {
            Vec3 fallback = prey.position()
                    .subtract(predator.position());

            horizontalToIntercept = new Vec3(
                    fallback.x,
                    0.0D,
                    fallback.z
            );
        }

        if (horizontalToIntercept.lengthSqr() <= EPSILON) {
            return Vec3.ZERO;
        }

        Vec3 forward = horizontalToIntercept.normalize();

        double distanceToIntercept = horizontalToIntercept.length();

        double distanceRatio = clamp(
                distanceToIntercept / profile.strikeStartRange(),
                0.0D,
                1.0D
        );

        double lungePower = profile.minLungeStrength()
                + distanceRatio * (profile.maxLungeStrength() - profile.minLungeStrength());

        Vec3 preyVelocity = getCappedHorizontalVelocity(
                prey,
                profile.maxPreyLeadSpeed()
        );

        Vec3 desiredVelocity = forward.scale(lungePower)
                .add(preyVelocity.scale(profile.preyVelocityCarry()));

        double desiredSpeed = desiredVelocity.length();

        if (desiredSpeed > profile.maxFinalHorizontalSpeed()) {
            desiredVelocity = desiredVelocity.normalize()
                    .scale(profile.maxFinalHorizontalSpeed());
        }

        return new Vec3(
                desiredVelocity.x,
                0.0D,
                desiredVelocity.z
        );
    }

    private static void applyLungeVelocity(
            PathfinderMob predator,
            Vec3 interceptPosition,
            Vec3 lungeVelocity,
            StrikeProfile profile
    ) {
        if (lungeVelocity.lengthSqr() <= EPSILON) {
            return;
        }

        Vec3 currentMovement = predator.getDeltaMovement();

        double verticalLift = calculateVerticalLift(
                predator,
                interceptPosition,
                profile
        );

        predator.setDeltaMovement(
                currentMovement.x * profile.currentMomentumCarry() + lungeVelocity.x,
                Math.max(0.0D, currentMovement.y) * 0.12D + verticalLift,
                currentMovement.z * profile.currentMomentumCarry() + lungeVelocity.z
        );
    }

    private static double calculateVerticalLift(
            PathfinderMob predator,
            Vec3 interceptPosition,
            StrikeProfile profile
    ) {
        double verticalDelta = interceptPosition.y - predator.getY();

        double lift = profile.minLiftStrength();

        if (verticalDelta > 0.0D) {
            lift += verticalDelta * 0.02D;
        }

        return clamp(
                lift,
                profile.minLiftStrength(),
                profile.maxLiftStrength()
        );
    }

    private static boolean canHitWithLunge(
            PathfinderMob predator,
            LivingEntity prey,
            Vec3 interceptPosition,
            Vec3 lungeVelocity,
            StrikeProfile profile
    ) {
        if (predator.distanceToSqr(prey) <= profile.hitRangeSquared()) {
            return true;
        }

        Vec3 futurePredatorPosition = predator.position()
                .add(
                        lungeVelocity.x,
                        0.0D,
                        lungeVelocity.z
                );

        if (futurePredatorPosition.distanceToSqr(prey.position()) <= profile.futureHitRangeSquared()) {
            return true;
        }

        return futurePredatorPosition.distanceToSqr(interceptPosition) <= profile.predictedHitRangeSquared();
    }

    private static StrikeProfile getProfile(PathfinderMob predator) {
        String path = RetoldMobRules.getEntityTypePath(
                predator.getType()
        );

        if (path.equals("cat") || path.equals("ocelot")) {
            /*
             * Cats/ocelots:
             * very fast low pounce for chickens/rabbits.
             * Almost no vertical hop.
             */
            return new StrikeProfile(
                    4.3D,
                    2.15D,
                    1.0D,
                    4.0D,
                    0.52D,
                    0.88D,
                    1.62D,
                    1.82D,
                    0.58D,
                    0.005D,
                    0.035D,
                    0.035D,
                    14,
                    6,
                    2,
                    2,
                    0.52D
            );
        }

        if (path.equals("fox")) {
            /*
             * Fox:
             * low forward intercept pounce.
             * The vertical vanilla jump is suppressed in RetoldControlledHuntingEvents.
             */
            return new StrikeProfile(
                    4.2D,
                    2.15D,
                    1.0D,
                    4.5D,
                    0.48D,
                    0.84D,
                    1.58D,
                    1.76D,
                    0.54D,
                    0.005D,
                    0.04D,
                    0.04D,
                    18,
                    7,
                    2,
                    2,
                    0.48D
            );
        }

        if (path.equals("spider") || path.equals("cave_spider")) {
            return new StrikeProfile(
                    3.0D,
                    2.0D,
                    1.0D,
                    3.5D,
                    0.32D,
                    0.38D,
                    0.72D,
                    0.92D,
                    0.26D,
                    0.02D,
                    0.065D,
                    0.08D,
                    22,
                    11,
                    5,
                    7,
                    0.30D
            );
        }

        if (path.equals("dolphin")) {
            return new StrikeProfile(
                    4.0D,
                    2.15D,
                    1.5D,
                    5.0D,
                    0.40D,
                    0.58D,
                    1.02D,
                    1.26D,
                    0.36D,
                    0.0D,
                    0.025D,
                    0.06D,
                    18,
                    9,
                    4,
                    5,
                    0.38D
            );
        }

        /*
         * Wolf/default:
         * after a bite, about one second of chase/re-aim before another bite.
         * miss retries faster than hit follow-ups.
         */
        return new StrikeProfile(
                4.1D,
                2.20D,
                1.5D,
                5.5D,
                0.42D,
                0.68D,
                1.05D,
                1.34D,
                0.38D,
                0.015D,
                0.055D,
                0.06D,
                22,
                9,
                5,
                3,
                0.38D
        );
    }

    private static double clamp(
            double value,
            double min,
            double max
    ) {
        if (value < min) {
            return min;
        }

        return Math.min(
                value,
                max
        );
    }

    private record StrikeProfile(
            double strikeStartRange,
            double hitRange,
            double minLeadTicks,
            double maxLeadTicks,
            double interceptSpeed,
            double minLungeStrength,
            double maxLungeStrength,
            double maxFinalHorizontalSpeed,
            double preyVelocityCarry,
            double minLiftStrength,
            double maxLiftStrength,
            double currentMomentumCarry,
            int hitCooldownTicks,
            int missCooldownTicks,
            int hitRecoveryTicks,
            int missRecoveryTicks,
            double maxPreyLeadSpeed
    ) {
        private double strikeStartRangeSquared() {
            return strikeStartRange * strikeStartRange;
        }

        private double hitRangeSquared() {
            return hitRange * hitRange;
        }

        private double predictedHitRangeSquared() {
            double predictedRange = hitRange + 0.85D;
            return predictedRange * predictedRange;
        }

        private double futureHitRangeSquared() {
            double futureRange = hitRange + 0.55D;
            return futureRange * futureRange;
        }
    }

    private record StrikeState(
            UUID preyId,
            long nextStrikeAt,
            long recoveryUntil
    ) {
    }
}