package cz.xefensor.retold.behavior;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

final class RetoldPackSenses {
    private static final int PACK_SCAN_CACHE_TICKS = 6;

    private static final double PARTY_PREY_SENSE_RADIUS_BLOCKS = 24.0D;
    private static final double PARTY_PREY_SENSE_RADIUS_SQUARED =
            PARTY_PREY_SENSE_RADIUS_BLOCKS * PARTY_PREY_SENSE_RADIUS_BLOCKS;

    private static final double PARTY_SIGHT_RADIUS_BLOCKS = 24.0D;
    private static final double PARTY_SIGHT_RADIUS_SQUARED =
            PARTY_SIGHT_RADIUS_BLOCKS * PARTY_SIGHT_RADIUS_BLOCKS;

    private static final double PARTY_HEARING_RADIUS_BLOCKS = 9.0D;
    private static final double PARTY_HEARING_RADIUS_SQUARED =
            PARTY_HEARING_RADIUS_BLOCKS * PARTY_HEARING_RADIUS_BLOCKS;

    private static final double PARTY_SMELL_RADIUS_BLOCKS = 6.0D;
    private static final double PARTY_SMELL_RADIUS_SQUARED =
            PARTY_SMELL_RADIUS_BLOCKS * PARTY_SMELL_RADIUS_BLOCKS;

    private static final double AUDIBLE_MOVEMENT_THRESHOLD_SQUARED = 0.0016D;

    private static final double PARTY_FOOD_RADIUS_BLOCKS = 16.0D;
    private static final double PARTY_FOOD_RADIUS_SQUARED =
            PARTY_FOOD_RADIUS_BLOCKS * PARTY_FOOD_RADIUS_BLOCKS;

    private RetoldPackSenses() {
    }

    static LivingEntity findPreySensedByAnyPartyMember(
            ServerLevel level,
            PathfinderMob leader,
            RetoldPackParty party,
            long gameTime
    ) {
        LivingEntity bestPrey = findSensedPreyForSensor(
                level,
                leader,
                gameTime
        );

        double bestScore = bestPrey == null
                ? Double.MAX_VALUE
                : leader.distanceToSqr(bestPrey);

        for (PathfinderMob member : party.members) {
            LivingEntity prey = findSensedPreyForSensor(
                    level,
                    member,
                    gameTime
            );

            if (prey == null) {
                continue;
            }

            double score = member.distanceToSqr(prey);

            if (score < bestScore) {
                bestScore = score;
                bestPrey = prey;
            }
        }

        return bestPrey;
    }

    static LivingEntity findSensedPreyForSensor(
            ServerLevel level,
            PathfinderMob sensor,
            long gameTime
    ) {
        if (!RetoldPackAnimals.isValidPackAnimal(sensor)) {
            return null;
        }

        RetoldAiControlMode mode = RetoldAiControl.getMode(sensor);

        if (
                mode == RetoldAiControlMode.FEED
                        || mode == RetoldAiControlMode.FLEE
                        || mode == RetoldAiControlMode.ATTACK
                        || mode == RetoldAiControlMode.TERRITORY
                        || mode == RetoldAiControlMode.SHELTER
        ) {
            return null;
        }

        List<LivingEntity> candidates = RetoldAiScanCache.nearby(
                level,
                sensor,
                LivingEntity.class,
                PARTY_PREY_SENSE_RADIUS_BLOCKS,
                gameTime,
                PACK_SCAN_CACHE_TICKS
        );

        LivingEntity bestPrey = null;
        double bestScore = Double.MAX_VALUE;

        for (LivingEntity candidate : candidates) {
            if (!isValidSensedPartyPrey(sensor, candidate, gameTime)) {
                continue;
            }

            double distanceSquared = sensor.distanceToSqr(candidate);

            if (distanceSquared > PARTY_PREY_SENSE_RADIUS_SQUARED) {
                continue;
            }

            double score = distanceSquared;

            if (RetoldAiSightCache.canSee(sensor, candidate, gameTime)) {
                score -= 24.0D;
            }

            if (RetoldMobRules.isSmallFoodPrey(candidate)) {
                score -= 10.0D;
            }

            if (score < bestScore) {
                bestScore = score;
                bestPrey = candidate;
            }
        }

        return bestPrey;
    }

    static boolean isValidPartyPrey(
            PathfinderMob predator,
            LivingEntity prey,
            long gameTime
    ) {
        return RetoldPreyTargeting.isValidMobRulePrey(
                predator,
                prey,
                gameTime
        );
    }

    static ItemEntity findBestFoodSeenByParty(
            ServerLevel level,
            PathfinderMob leader,
            RetoldPackParty party
    ) {
        ItemEntity best = findBestFoodSeenBySensor(
                level,
                leader
        );

        double bestDistance = best == null
                ? Double.MAX_VALUE
                : leader.distanceToSqr(best);

        for (PathfinderMob member : party.members) {
            ItemEntity food = findBestFoodSeenBySensor(
                    level,
                    member
            );

            if (food == null) {
                continue;
            }

            double distance = member.distanceToSqr(food);

            if (distance < bestDistance) {
                best = food;
                bestDistance = distance;
            }
        }

        return best;
    }

    private static ItemEntity findBestFoodSeenBySensor(
            ServerLevel level,
            PathfinderMob sensor
    ) {
        if (level == null || sensor == null || !sensor.isAlive() || sensor.isRemoved()) {
            return null;
        }

        List<ItemEntity> foods = RetoldAiScanCache.nearby(
                level,
                sensor,
                ItemEntity.class,
                PARTY_FOOD_RADIUS_BLOCKS,
                level.getGameTime(),
                PACK_SCAN_CACHE_TICKS
        );

        ItemEntity bestFood = null;
        double bestDistance = Double.MAX_VALUE;

        for (ItemEntity food : foods) {
            if (!isObservablePartyFood(sensor, food)) {
                continue;
            }

            double distanceSquared = sensor.distanceToSqr(food);

            if (distanceSquared > PARTY_FOOD_RADIUS_SQUARED) {
                continue;
            }

            if (distanceSquared < bestDistance) {
                bestDistance = distanceSquared;
                bestFood = food;
            }
        }

        return bestFood;
    }

    private static boolean isValidSensedPartyPrey(
            PathfinderMob sensor,
            LivingEntity prey,
            long gameTime
    ) {
        if (!isValidPartyPrey(sensor, prey, gameTime)) {
            return false;
        }

        if (sensor.distanceToSqr(prey) > PARTY_PREY_SENSE_RADIUS_SQUARED) {
            return false;
        }

        return canPartySensorSensePrey(
                sensor,
                prey,
                gameTime
        );
    }

    private static boolean canPartySensorSensePrey(
            PathfinderMob sensor,
            LivingEntity prey,
            long gameTime
    ) {
        double distanceSquared = sensor.distanceToSqr(prey);

        if (
                distanceSquared <= PARTY_SIGHT_RADIUS_SQUARED
                        && RetoldAiSightCache.canSee(sensor, prey, gameTime)
        ) {
            return true;
        }

        if (
                distanceSquared <= PARTY_HEARING_RADIUS_SQUARED
                        && isAudible(prey)
        ) {
            return true;
        }

        return distanceSquared <= PARTY_SMELL_RADIUS_SQUARED;
    }

    private static boolean isObservablePartyFood(
            PathfinderMob sensor,
            ItemEntity item
    ) {
        if (sensor == null || item == null) {
            return false;
        }

        if (!item.isAlive() || item.isRemoved()) {
            return false;
        }

        if (item.getItem().isEmpty()) {
            return false;
        }

        if (sensor.distanceToSqr(item) > PARTY_FOOD_RADIUS_SQUARED) {
            return false;
        }

        if (
                !RetoldAiSightCache.canSee(sensor, item, sensor.level().getGameTime())
                        && sensor.distanceToSqr(item) > 25.0D
        ) {
            return false;
        }

        return RetoldMobRules.canEatDroppedItem(
                sensor,
                item.getItem()
        );
    }

    private static boolean isAudible(LivingEntity entity) {
        Vec3 movement = entity.getDeltaMovement();
        double horizontalMovementSquared = movement.x * movement.x + movement.z * movement.z;

        return horizontalMovementSquared >= AUDIBLE_MOVEMENT_THRESHOLD_SQUARED;
    }

}
