package cz.xefensor.retold.behavior.ecology;

import cz.xefensor.retold.behavior.breeding.RetoldAnimalBreeding;
import cz.xefensor.retold.behavior.food.RetoldAnimalFeederBehavior;
import cz.xefensor.retold.behavior.food.RetoldFoodBehaviorEvents;
import cz.xefensor.retold.behavior.home.RetoldAnimalHomeMemory;
import cz.xefensor.retold.behavior.home.RetoldAnimalHomes;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;
import cz.xefensor.retold.behavior.profiles.RetoldMobState;
import cz.xefensor.retold.behavior.species.RetoldSlimeStarvationBehavior;
import cz.xefensor.retold.block.AnimalFeederBlockEntity;
import cz.xefensor.retold.villager.RetoldVillagerCommunalFood;
import cz.xefensor.retold.villager.RetoldUnloadedFarmerProduction;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

/**
 * Bounded first-stage reconciliation for hunger-aware mobs returning from an
 * unloaded chunk. This advances persisted metabolism and can consume real
 * feeder, natural-forage, personal Villager, and village-storage food. A
 * supported adult's continuous well-fed breeding progress advances on the
 * same timeline. Accumulated critical pulses apply bounded starvation damage;
 * named/tamed mobs retain a one-health floor and Cube Mobs use their normal
 * split-or-die transaction.
 */
public final class RetoldUnloadedEcosystemCatchUp {
    public static final long MAX_CATCH_UP_TICKS =
            RetoldUnloadedCatchUpPlan.MAX_CATCH_UP_TICKS;
    public static final int MAX_TASKS_PER_TICK = 16;

    private static final long MEAL_INTERVAL_TICKS = 24_000L;
    private static final int MAX_PENDING_TASKS = 4_096;

    private static final Queue<CatchUpTask> PENDING = new ArrayDeque<>();
    private static final Set<UUID> QUEUED_MOBS = new HashSet<>();

    private RetoldUnloadedEcosystemCatchUp() {
    }

    /**
     * Defers gaps containing at least two metabolism pulses. A single due
     * pulse remains on the ordinary loaded path so normal cadence is unchanged.
     */
    public static synchronized boolean deferLongGap(
            ServerLevel level,
            Mob mob,
            RetoldMobState state,
            long gameTime,
            int hungerInterval
    ) {
        if (level == null
                || mob == null
                || state == null
                || hungerInterval <= 0
                || mob.level() != level) {
            return false;
        }

        RetoldUnloadedCatchUpPlan.Plan plan = RetoldUnloadedCatchUpPlan.calculate(
                state.lastHungerTickAt(),
                gameTime,
                hungerInterval
        );

        if (plan.hungerPulses() < 2) {
            return false;
        }

        RetoldUnloadedNaturalSpawning.enqueue(
                level,
                mob.blockPosition(),
                plan.mealOpportunities()
        );

        UUID mobId = mob.getUUID();

        if (QUEUED_MOBS.contains(mobId)) {
            return true;
        }

        if (PENDING.size() >= MAX_PENDING_TASKS) {
            return true;
        }

        PENDING.add(new CatchUpTask(
                level,
                mob,
                state,
                state.lastHungerTickAt(),
                state.breedingSatisfiedTicks(),
                hungerInterval,
                gameTime
        ));
        QUEUED_MOBS.add(mobId);
        return true;
    }

    @SubscribeEvent
    public static void onServerTickPost(ServerTickEvent.Post event) {
        processPending(MAX_TASKS_PER_TICK);
        RetoldUnloadedMigration.processPending(
                RetoldUnloadedMigration.MAX_GROUPS_PER_TICK
        );
        RetoldUnloadedFarmerProduction.processPending(
                RetoldUnloadedFarmerProduction.MAX_FARMERS_PER_TICK
        );

        if (pendingCount() == 0) {
            RetoldUnloadedNaturalSpawning.processPending(
                    RetoldUnloadedNaturalSpawning.MAX_CHUNKS_PER_TICK
            );
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        clear();
    }

    public static synchronized int processPending(int maximumTasks) {
        int processed = 0;
        int limit = Math.max(0, maximumTasks);

        while (processed < limit && !PENDING.isEmpty()) {
            CatchUpTask task = PENDING.remove();
            QUEUED_MOBS.remove(task.mob().getUUID());
            processed++;

            if (!task.isStillValid()) {
                continue;
            }

            RetoldUnloadedCatchUpPlan.Plan plan = RetoldUnloadedCatchUpPlan.calculate(
                    task.expectedLastHungerTickAt(),
                    task.gameTime(),
                    task.hungerInterval()
            );

            if (plan.hungerPulses() <= 0) {
                continue;
            }

            int hungerGain = task.mob() instanceof PathfinderMob pathfinderMob
                    ? RetoldSlimeStarvationBehavior.hungerGain(pathfinderMob)
                    : 1;
            FoodSourceResult foodSourceResult = findFoodSources(
                    task,
                    plan,
                    hungerGain
            );

            if (foodSourceResult.deferred()) {
                requeue(task);
                continue;
            }

            Reconciliation reconciliation = reconcileHunger(
                    task,
                    plan,
                    hungerGain,
                    foodSourceResult.sources()
            );
            task.state().applyHungerCatchUp(
                    reconciliation.hunger(),
                    plan.simulatedThrough(),
                    reconciliation.mealsConsumed(),
                    reconciliation.lastMealAt(),
                    reconciliation.successfulHunts(),
                    reconciliation.lastSuccessfulHuntAt()
            );
            if (!RetoldUnloadedStarvation.apply(
                    task.level(),
                    task.mob(),
                    task.state(),
                    plan.simulatedThrough(),
                    reconciliation.criticalPulses()
            )) {
                continue;
            }

            if (task.mob() instanceof Villager villager) {
                RetoldUnloadedFarmerProduction.enqueue(
                        task.level(),
                        villager,
                        plan.mealOpportunities()
                );
            }

            applyBreedingProgress(task, plan, reconciliation);

            if (task.mob() instanceof PathfinderMob pathfinderMob) {
                RetoldUnloadedMigration.enqueueIfEligible(
                        task.level(),
                        pathfinderMob,
                        task.state(),
                        plan,
                        reconciliation.mealsConsumed()
                );
            }
        }

        return processed;
    }

    private static FoodSourceResult findFoodSources(
            CatchUpTask task,
            RetoldUnloadedCatchUpPlan.Plan plan,
            int hungerGain
    ) {
        if (plan.mealOpportunities() <= 0
                || !couldNeedMeal(task, plan, hungerGain)) {
            return FoodSourceResult.none();
        }

        long searchTime = task.level().getGameTime();

        if (task.mob() instanceof Villager villager) {
            BlockPos storagePos = null;

            if (RetoldVillagerCommunalFood.personalMealCount(villager)
                    < plan.mealOpportunities()) {
                RetoldVillagerCommunalFood.CatchUpStorageResult storageResult =
                        RetoldVillagerCommunalFood.findCatchUpStorage(
                                task.level(),
                                villager,
                                searchTime
                        );

                if (storageResult.deferred()) {
                    return FoodSourceResult.deferredResult();
                }

                storagePos = storageResult.storagePos();
            }

            return new FoodSourceResult(
                    new FoodSources(
                            null,
                            List.of(),
                            storagePos,
                            List.of()
                    ),
                    false
            );
        }

        if (!(task.mob() instanceof PathfinderMob pathfinderMob)) {
            return FoodSourceResult.none();
        }

        AnimalFeederBlockEntity feeder = null;

        if (RetoldMobRules.canUseAnimalFeeder(pathfinderMob)) {
            RetoldAnimalHomeMemory home = RetoldAnimalHomes.get(pathfinderMob);
            BlockPos center = RetoldAnimalHomes.isValidFor(
                    task.level(),
                    pathfinderMob,
                    home
            ) ? home.pos() : pathfinderMob.blockPosition();
            RetoldAnimalFeederBehavior.CatchUpFeederResult feederResult =
                    RetoldAnimalFeederBehavior.findCatchUpFeeder(
                            task.level(),
                            pathfinderMob,
                            center,
                            searchTime
                    );

            if (feederResult.deferred()) {
                return FoodSourceResult.deferredResult();
            }

            feeder = feederResult.feeder();
        }

        int feederMeals = feeder != null && feeder.hasFoodFor(pathfinderMob)
                ? feeder.getItem(0).getCount()
                : 0;
        int forageMealsNeeded = Math.max(
                0,
                plan.mealOpportunities() - feederMeals
        );
        List<BlockPos> forageTargets = List.of();

        if (forageMealsNeeded > 0) {
            RetoldFoodBehaviorEvents.CatchUpForageResult forageResult =
                    RetoldFoodBehaviorEvents.findCatchUpForage(
                            task.level(),
                            pathfinderMob,
                            searchTime,
                            forageMealsNeeded
                    );

            if (forageResult.deferred()) {
                return FoodSourceResult.deferredResult();
            }

            forageTargets = forageResult.targets();
        }

        int forageMeals = forageCapacity(
                task.level(),
                pathfinderMob,
                forageTargets,
                plan.mealOpportunities()
        );
        int preyMealsNeeded = Math.max(
                0,
                plan.mealOpportunities() - feederMeals - forageMeals
        );
        List<LivingEntity> preyTargets = List.of();

        if (preyMealsNeeded > 0) {
            RetoldUnloadedPredation.FindResult predationResult =
                    RetoldUnloadedPredation.findReachablePrey(
                            task.level(),
                            pathfinderMob,
                            searchTime,
                            preyMealsNeeded
                    );

            if (predationResult.deferred()) {
                return FoodSourceResult.deferredResult();
            }

            preyTargets = predationResult.prey();
        }

        return new FoodSourceResult(
                new FoodSources(
                        feeder,
                        forageTargets,
                        null,
                        preyTargets
                ),
                false
        );
    }

    private static int forageCapacity(
            ServerLevel level,
            PathfinderMob mob,
            List<BlockPos> forageTargets,
            int maximumMeals
    ) {
        for (BlockPos target : forageTargets) {
            if (RetoldMobRules.isRenewableEnvironmentalForage(
                    mob,
                    level.getBlockState(target)
            )) {
                return maximumMeals;
            }
        }

        return forageTargets.size();
    }

    private static boolean couldNeedMeal(
            CatchUpTask task,
            RetoldUnloadedCatchUpPlan.Plan plan,
            int hungerGain
    ) {
        int eatThreshold = RetoldMobRules.eatThreshold(task.mob());

        return eatThreshold <= 100
                && (long) task.state().hunger()
                + (long) plan.hungerPulses() * hungerGain >= eatThreshold;
    }

    private static Reconciliation reconcileHunger(
            CatchUpTask task,
            RetoldUnloadedCatchUpPlan.Plan plan,
            int hungerGain,
            FoodSources foodSources
    ) {
        int hunger = task.state().hunger();
        int appliedPulses = 0;
        int mealsConsumed = 0;
        long lastMealAt = 0L;
        int successfulHunts = 0;
        long lastSuccessfulHuntAt = 0L;
        int criticalPulses = 0;
        int eatThreshold = RetoldMobRules.eatThreshold(task.mob());
        RetoldUnloadedBreedingProgress breedingProgress =
                createBreedingProgress(task, plan, hunger);

        for (int day = 1; day <= plan.mealOpportunities(); day++) {
            int pulsesThroughDay = Math.min(
                    plan.hungerPulses(),
                    (int) Math.min(
                            Integer.MAX_VALUE,
                            day * MEAL_INTERVAL_TICKS / task.hungerInterval()
                    )
            );
            HungerProgress hungerProgress = applyHungerPulses(
                    task,
                    plan,
                    breedingProgress,
                    hunger,
                    hungerGain,
                    appliedPulses,
                    pulsesThroughDay
            );
            hunger = hungerProgress.hunger();
            criticalPulses += hungerProgress.criticalPulses();
            appliedPulses = pulsesThroughDay;

            if (breedingProgress != null) {
                breedingProgress.updateHunger(
                        hunger,
                        plan.simulatedFrom() + day * MEAL_INTERVAL_TICKS
                );
            }

            if (hunger < eatThreshold) {
                continue;
            }

            long mealAt = plan.simulatedFrom() + day * MEAL_INTERVAL_TICKS;
            Meal meal = foodSources.takeMeal(
                    task,
                    hunger,
                    mealAt
            );

            if (meal.relief() <= 0) {
                continue;
            }

            hunger = addHunger(hunger, -meal.relief());

            if (breedingProgress != null) {
                breedingProgress.updateHunger(hunger, mealAt);
            }
            mealsConsumed++;
            lastMealAt = mealAt;

            if (meal.successfulHunt()) {
                successfulHunts++;
                lastSuccessfulHuntAt = mealAt;
            }
        }

        HungerProgress hungerProgress = applyHungerPulses(
                task,
                plan,
                breedingProgress,
                hunger,
                hungerGain,
                appliedPulses,
                plan.hungerPulses()
        );
        hunger = hungerProgress.hunger();
        criticalPulses += hungerProgress.criticalPulses();
        long breedingSatisfiedTicks = breedingProgress == null
                ? -1L
                : breedingProgress.finish(plan.simulatedThrough());
        return new Reconciliation(
                hunger,
                mealsConsumed,
                lastMealAt,
                successfulHunts,
                lastSuccessfulHuntAt,
                breedingSatisfiedTicks,
                criticalPulses
        );
    }

    private static RetoldUnloadedBreedingProgress createBreedingProgress(
            CatchUpTask task,
            RetoldUnloadedCatchUpPlan.Plan plan,
            int hunger
    ) {
        if (!(task.mob() instanceof Animal animal)
                || !RetoldAnimalBreeding.supportsUnloadedProgress(
                task.level(),
                animal
        )) {
            return null;
        }

        boolean canAccumulate = RetoldAnimalBreeding
                .canAccumulateUnloadedProgress(animal, task.state());
        long existingProgress = canAccumulate
                ? task.expectedBreedingSatisfiedTicks()
                : 0L;

        return new RetoldUnloadedBreedingProgress(
                existingProgress,
                hunger,
                plan.simulatedFrom(),
                plan.capped(),
                canAccumulate
        );
    }

    private static HungerProgress applyHungerPulses(
            CatchUpTask task,
            RetoldUnloadedCatchUpPlan.Plan plan,
            RetoldUnloadedBreedingProgress breedingProgress,
            int hunger,
            int hungerGain,
            int appliedPulses,
            int pulsesThrough
    ) {
        int updatedHunger = hunger;
        int criticalPulses = 0;

        for (int pulse = appliedPulses + 1; pulse <= pulsesThrough; pulse++) {
            long pulseAt = plan.simulatedFrom()
                    + (long) pulse * task.hungerInterval();
            updatedHunger = addHunger(updatedHunger, hungerGain);

            if (updatedHunger >= 100) {
                criticalPulses++;
            }

            if (breedingProgress != null) {
                breedingProgress.updateHunger(updatedHunger, pulseAt);
            }
        }

        return new HungerProgress(updatedHunger, criticalPulses);
    }

    private static void applyBreedingProgress(
            CatchUpTask task,
            RetoldUnloadedCatchUpPlan.Plan plan,
            Reconciliation reconciliation
    ) {
        if (reconciliation.breedingSatisfiedTicks() < 0L) {
            return;
        }

        task.state().applyBreedingCatchUp(
                reconciliation.breedingSatisfiedTicks(),
                plan.simulatedThrough()
        );
    }

    private static int addHunger(int hunger, int amount) {
        return Math.max(0, Math.min(100, hunger + amount));
    }

    private static void requeue(CatchUpTask task) {
        UUID mobId = task.mob().getUUID();

        if (PENDING.size() >= MAX_PENDING_TASKS || QUEUED_MOBS.contains(mobId)) {
            return;
        }

        PENDING.add(task);
        QUEUED_MOBS.add(mobId);
    }

    static synchronized int pendingCount() {
        return PENDING.size();
    }

    static synchronized void clear() {
        PENDING.clear();
        QUEUED_MOBS.clear();
        RetoldUnloadedMigration.clear();
        RetoldUnloadedFarmerProduction.clear();
        RetoldUnloadedNaturalSpawning.clear();
    }

    private record Reconciliation(
            int hunger,
            int mealsConsumed,
            long lastMealAt,
            int successfulHunts,
            long lastSuccessfulHuntAt,
            long breedingSatisfiedTicks,
            int criticalPulses
    ) {
    }

    private record HungerProgress(int hunger, int criticalPulses) {
    }

    private record Meal(
            int relief,
            boolean successfulHunt
    ) {
        private static Meal none() {
            return new Meal(0, false);
        }

        private static Meal food(int relief) {
            return new Meal(relief, false);
        }

        private static Meal prey(int relief) {
            return new Meal(relief, true);
        }
    }

    private record FoodSourceResult(
            FoodSources sources,
            boolean deferred
    ) {
        private static FoodSourceResult none() {
            return new FoodSourceResult(FoodSources.none(), false);
        }

        private static FoodSourceResult deferredResult() {
            return new FoodSourceResult(FoodSources.none(), true);
        }
    }

    private static final class FoodSources {
        private final AnimalFeederBlockEntity feeder;
        private final List<BlockPos> forageTargets;
        private final BlockPos villagerStorage;
        private final List<LivingEntity> preyTargets;
        private int forageIndex;
        private int preyIndex;

        private FoodSources(
                AnimalFeederBlockEntity feeder,
                List<BlockPos> forageTargets,
                BlockPos villagerStorage,
                List<LivingEntity> preyTargets
        ) {
            this.feeder = feeder;
            this.forageTargets = forageTargets == null
                    ? List.of()
                    : forageTargets;
            this.villagerStorage = villagerStorage;
            this.preyTargets = preyTargets == null
                    ? List.of()
                    : preyTargets;
        }

        private static FoodSources none() {
            return new FoodSources(null, List.of(), null, List.of());
        }

        private Meal takeMeal(
                CatchUpTask task,
                int hunger,
                long simulatedTime
        ) {
            if (task.mob() instanceof Villager villager) {
                return Meal.food(RetoldVillagerCommunalFood.consumeCatchUpMeal(
                        task.level(),
                        villager,
                        villagerStorage
                ));
            }

            if (!(task.mob() instanceof PathfinderMob pathfinderMob)) {
                return Meal.none();
            }

            if (feeder != null) {
                ItemStack meal = feeder.takeOneFor(pathfinderMob);

                if (!meal.isEmpty()) {
                    return Meal.food(RetoldMobRules.foodRelief(
                            pathfinderMob,
                            meal
                    ));
                }
            }

            while (forageIndex < forageTargets.size()) {
                BlockPos target = forageTargets.get(forageIndex);
                RetoldFoodBehaviorEvents.CatchUpForageMeal meal =
                        RetoldFoodBehaviorEvents.consumeCatchUpForage(
                                task.level(),
                                pathfinderMob,
                                target
                        );

                if (!meal.reusable()) {
                    forageIndex++;
                }

                if (meal.relief() > 0) {
                    return Meal.food(meal.relief());
                }
            }

            if (hunger < RetoldMobRules.adjustedHuntThreshold(
                    pathfinderMob,
                    task.state(),
                    simulatedTime
            )) {
                return Meal.none();
            }

            while (preyIndex < preyTargets.size()) {
                LivingEntity prey = preyTargets.get(preyIndex++);
                int relief = RetoldUnloadedPredation.consumePrey(
                        pathfinderMob,
                        prey,
                        simulatedTime
                );

                if (relief > 0) {
                    return Meal.prey(relief);
                }
            }

            return Meal.none();
        }
    }

    private record CatchUpTask(
            ServerLevel level,
            Mob mob,
            RetoldMobState state,
            long expectedLastHungerTickAt,
            long expectedBreedingSatisfiedTicks,
            int hungerInterval,
            long gameTime
    ) {
        private boolean isStillValid() {
            return mob.level() == level
                    && mob.isAlive()
                    && !mob.isRemoved()
                    && state.lastHungerTickAt() == expectedLastHungerTickAt;
        }
    }
}
