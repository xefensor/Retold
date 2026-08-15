package cz.xefensor.retold.behavior.breeding;

import cz.xefensor.retold.behavior.core.RetoldBehaviorCoordinator;
import cz.xefensor.retold.behavior.performance.RetoldAiScanCache;
import cz.xefensor.retold.behavior.profiles.RetoldHungerStage;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;
import cz.xefensor.retold.behavior.profiles.RetoldMobState;
import cz.xefensor.retold.behavior.profiles.RetoldMobStates;
import cz.xefensor.retold.registry.RetoldTags;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import java.util.List;

/**
 * Replaces item-triggered love mode with reproduction earned by sustained
 * access to food while loaded or during bounded catch-up. Vanilla remains
 * responsible for mate movement, offspring, genetics, ownership, eggs, and
 * species-specific birth behavior.
 */
public final class RetoldAnimalBreeding {
    public static final int SATISFIED_TICKS = 20 * 60 * 5;
    public static final int PARENT_HUNGER_COST = 40;

    private static final double MATE_RADIUS = 8.0D;
    private static final double MATE_RADIUS_SQUARED =
            MATE_RADIUS * MATE_RADIUS;
    private static final int MATE_SCAN_CACHE_TICKS = 100;
    private static final int FAILED_ATTEMPT_COOLDOWN_TICKS = 20 * 60;
    private static final int DISPATCH_INTERVAL_TICKS = 20;

    private RetoldAnimalBreeding() {
    }

    public static void tick(
            ServerLevel level,
            Animal animal,
            long gameTime
    ) {
        if (!isSupported(level, animal)) {
            return;
        }

        RetoldMobState state = RetoldMobStates.getOrCreate(
                animal,
                gameTime
        );

        if (state.automaticBreedingArmedAt() > 0L) {
            continueArmedAttempt(animal, state, gameTime);
            return;
        }

        if (animal.isInLove()) {
            /*
             * Existing-world or third-party love state must not bypass the
             * hunger contract. Retold-created love always carries the armed
             * marker above.
             */
            animal.resetLove();
        }

        if (!updateSatisfaction(animal, state, gameTime)
                || gameTime < state.nextBreedingAttemptAt()
                || !isReady(state, gameTime)) {
            return;
        }

        Animal partner = findReadyPartner(level, animal, gameTime);

        if (partner == null) {
            state.scheduleNextBreedingAttempt(
                    gameTime + FAILED_ATTEMPT_COOLDOWN_TICKS
            );
            return;
        }

        armPair(animal, partner, gameTime);
    }

    public static boolean shouldReplacePlayerLove(Animal animal) {
        return animal != null
                && animal.getType().builtInRegistryHolder().is(
                        RetoldTags.AUTOMATIC_BREEDERS
                );
    }

    public static boolean supportsUnloadedProgress(
            ServerLevel level,
            Animal animal
    ) {
        return isSupported(level, animal);
    }

    public static boolean canAccumulateUnloadedProgress(
            Animal animal,
            RetoldMobState state
    ) {
        return animal != null
                && state != null
                && animal.isAlive()
                && !animal.isRemoved()
                && !animal.isBaby()
                && animal.getAge() == 0
                && !animal.isPanicking()
                && !RetoldBehaviorCoordinator.hasLiveTarget(animal)
                && !animal.isInLove()
                && state.automaticBreedingArmedAt() == 0L;
    }

    public static boolean canAcceptPlayerBreedingFood(Animal animal) {
        if (animal == null || animal.level().isClientSide()) {
            return true;
        }

        if (!shouldReplacePlayerLove(animal)) {
            return animal.getInLoveTime() <= 0;
        }

        RetoldMobState state = RetoldMobStates.getOrCreate(
                animal,
                animal.level().getGameTime()
        );
        return animal.getAge() == 0
                && !animal.isInLove()
                && state.hunger() > 0;
    }

    public static boolean needsTendingFood(Animal animal) {
        if (animal == null
                || !shouldReplacePlayerLove(animal)
                || animal.getAge() != 0) {
            return false;
        }

        RetoldMobState state = RetoldMobStates.getOrCreate(
                animal,
                animal.level().getGameTime()
        );
        return RetoldMobRules.hasEatDrive(animal, state);
    }

    public static void onPlayerLoveAttempt(
            Animal animal,
            ItemStack food
    ) {
        if (animal == null
                || !(animal.level() instanceof ServerLevel level)
                || !shouldReplacePlayerLove(animal)) {
            return;
        }

        feed(animal, food, level.getGameTime());
    }

    public static boolean feed(
            Animal animal,
            ItemStack food,
            long gameTime
    ) {
        if (animal == null || !shouldReplacePlayerLove(animal)) {
            return false;
        }

        RetoldMobState state = RetoldMobStates.getOrCreate(
                animal,
                gameTime
        );
        int relief = food == null || food.isEmpty()
                ? 20
                : RetoldMobRules.foodRelief(
                        animal,
                        food
                );

        state.addHunger(-Math.max(1, relief));
        state.markFed(gameTime);
        return true;
    }

    public static void onLivingDamage(LivingDamageEvent.Post event) {
        if (event.getHealthDamage() <= 0.0F
                || !(event.getEntity() instanceof Animal animal)
                || !(animal.level() instanceof ServerLevel level)
                || !shouldReplacePlayerLove(animal)) {
            return;
        }

        interruptReadiness(animal, level.getGameTime());
    }

    public static void interruptReadiness(
            Animal animal,
            long gameTime
    ) {
        if (animal == null) {
            return;
        }

        RetoldMobState state = RetoldMobStates.getOrCreate(
                animal,
                gameTime
        );
        state.clearBreedingSatisfaction();
        state.clearAutomaticBreedingArmed();
        state.scheduleNextBreedingAttempt(gameTime + SATISFIED_TICKS);
        animal.resetLove();
    }

    static boolean armPair(
            Animal first,
            Animal second,
            long gameTime
    ) {
        if (!canMateNow(first, second)) {
            return false;
        }

        RetoldMobState firstState = RetoldMobStates.getOrCreate(
                first,
                gameTime
        );
        RetoldMobState secondState = RetoldMobStates.getOrCreate(
                second,
                gameTime
        );

        firstState.markAutomaticBreedingArmed(gameTime);
        secondState.markAutomaticBreedingArmed(gameTime);
        first.setInLove(null);
        second.setInLove(null);
        return true;
    }

    static boolean isReady(
            RetoldMobState state,
            long gameTime
    ) {
        return state != null
                && state.breedingSatisfiedTicks() >= SATISFIED_TICKS;
    }

    private static void continueArmedAttempt(
            Animal animal,
            RetoldMobState state,
            long gameTime
    ) {
        if (animal.getAge() > 0) {
            state.addHunger(PARENT_HUNGER_COST);
            state.clearBreedingSatisfaction();
            state.clearAutomaticBreedingArmed();
            state.scheduleNextBreedingAttempt(
                    gameTime + Math.max(0, animal.getAge())
            );
            return;
        }

        if (!isPhysicallySatisfied(animal, state)) {
            interruptReadiness(animal, gameTime);
            return;
        }

        if (!animal.isInLove()) {
            state.clearAutomaticBreedingArmed();
            state.scheduleNextBreedingAttempt(
                    gameTime + FAILED_ATTEMPT_COOLDOWN_TICKS
            );
        }
    }

    private static Animal findReadyPartner(
            ServerLevel level,
            Animal animal,
            long gameTime
    ) {
        List<Animal> nearby = RetoldAiScanCache.nearby(
                level,
                animal,
                Animal.class,
                MATE_RADIUS,
                gameTime,
                MATE_SCAN_CACHE_TICKS
        );
        Animal best = null;
        double bestDistance = Double.MAX_VALUE;

        for (Animal candidate : nearby) {
            if (candidate == animal
                    || candidate.level() != level
                    || animal.distanceToSqr(candidate)
                    > MATE_RADIUS_SQUARED
                    || !isSupported(level, candidate)) {
                continue;
            }

            RetoldMobState candidateState = RetoldMobStates.getOrCreate(
                    candidate,
                    gameTime
            );

            if (candidateState.automaticBreedingArmedAt() > 0L
                    || gameTime < candidateState.nextBreedingAttemptAt()
                    || !updateSatisfaction(
                            candidate,
                            candidateState,
                            gameTime
                    )
                    || !isReady(candidateState, gameTime)
                    || !canMateNow(animal, candidate)) {
                continue;
            }

            double distance = animal.distanceToSqr(candidate);

            if (distance < bestDistance) {
                best = candidate;
                bestDistance = distance;
            }
        }

        return best;
    }

    private static boolean canMateNow(
            Animal first,
            Animal second
    ) {
        if (first == null
                || second == null
                || first == second
                || !first.isAlive()
                || !second.isAlive()
                || first.isRemoved()
                || second.isRemoved()
                || first.level() != second.level()
                || first.getAge() != 0
                || second.getAge() != 0
                || first.isInLove()
                || second.isInLove()) {
            return false;
        }

        int firstLoveTime = first.getInLoveTime();
        int secondLoveTime = second.getInLoveTime();

        try {
            first.setInLoveTime(1);
            second.setInLoveTime(1);
            return first.canMate(second) || second.canMate(first);
        } finally {
            first.setInLoveTime(firstLoveTime);
            second.setInLoveTime(secondLoveTime);
        }
    }

    private static boolean updateSatisfaction(
            Animal animal,
            RetoldMobState state,
            long gameTime
    ) {
        if (!isPhysicallySatisfied(animal, state)) {
            state.clearBreedingSatisfaction();
            return false;
        }

        state.advanceBreedingSatisfaction(
                gameTime,
                DISPATCH_INTERVAL_TICKS
        );

        return true;
    }

    private static boolean isPhysicallySatisfied(
            Animal animal,
            RetoldMobState state
    ) {
        return animal != null
                && state != null
                && animal.isAlive()
                && !animal.isRemoved()
                && !animal.isBaby()
                && animal.getAge() == 0
                && !animal.isPanicking()
                && !RetoldBehaviorCoordinator.hasLiveTarget(animal)
                && RetoldMobRules.hungerStage(state)
                == RetoldHungerStage.FULL;
    }

    private static boolean isSupported(
            ServerLevel level,
            Animal animal
    ) {
        return level != null
                && animal != null
                && animal.level() == level
                && shouldReplacePlayerLove(animal)
                && RetoldMobRules.canUseOrdinaryLifeSystems(animal)
                && RetoldMobRules.hungerInterval(animal) > 0;
    }
}
