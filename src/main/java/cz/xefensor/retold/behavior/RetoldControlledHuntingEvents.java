package cz.xefensor.retold.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public final class RetoldControlledHuntingEvents {
    private static final Map<PathfinderMob, HuntMemory> HUNT_MEMORIES = new WeakHashMap<>();

    private static final int HUNT_THINK_INTERVAL_TICKS = 10;
    private static final int HUNT_CONTROL_TICKS = 20 * 4;
    private static final int FEED_LOCK_AFTER_KILL_TICKS = 20 * 8;
    private static final int HUNT_TRAIL_MEMORY_TICKS = 20 * 10;

    private static final double PREY_SEARCH_RADIUS_BLOCKS = 18.0D;
    private static final double PREY_SEARCH_RADIUS_SQUARED =
            PREY_SEARCH_RADIUS_BLOCKS * PREY_SEARCH_RADIUS_BLOCKS;

    private static final double SIGHT_RADIUS_BLOCKS = 18.0D;
    private static final double SIGHT_RADIUS_SQUARED =
            SIGHT_RADIUS_BLOCKS * SIGHT_RADIUS_BLOCKS;

    private static final double HEARING_RADIUS_BLOCKS = 6.0D;
    private static final double HEARING_RADIUS_SQUARED =
            HEARING_RADIUS_BLOCKS * HEARING_RADIUS_BLOCKS;

    private static final double SMELL_RADIUS_BLOCKS = 7.0D;
    private static final double SMELL_RADIUS_SQUARED =
            SMELL_RADIUS_BLOCKS * SMELL_RADIUS_BLOCKS;

    private static final double TRAIL_REACQUIRE_RADIUS_BLOCKS = 10.0D;
    private static final double TRAIL_REACQUIRE_RADIUS_SQUARED =
            TRAIL_REACQUIRE_RADIUS_BLOCKS * TRAIL_REACQUIRE_RADIUS_BLOCKS;

    private static final double EASY_FOOD_RADIUS_BLOCKS = 8.0D;
    private static final double EASY_FOOD_RADIUS_SQUARED =
            EASY_FOOD_RADIUS_BLOCKS * EASY_FOOD_RADIUS_BLOCKS;

    private static final double CLOSE_CHASE_DISTANCE_BLOCKS = 5.0D;
    private static final double CLOSE_CHASE_DISTANCE_SQUARED =
            CLOSE_CHASE_DISTANCE_BLOCKS * CLOSE_CHASE_DISTANCE_BLOCKS;

    private static final double HUNT_ABANDON_DISTANCE_BLOCKS = 44.0D;
    private static final double HUNT_ABANDON_DISTANCE_SQUARED =
            HUNT_ABANDON_DISTANCE_BLOCKS * HUNT_ABANDON_DISTANCE_BLOCKS;

    private static final double SEARCH_POINT_REACHED_DISTANCE_BLOCKS = 2.5D;
    private static final double SEARCH_POINT_REACHED_DISTANCE_SQUARED =
            SEARCH_POINT_REACHED_DISTANCE_BLOCKS * SEARCH_POINT_REACHED_DISTANCE_BLOCKS;

    private static final double DEFAULT_HUNT_SPEED = 1.30D;
    private static final double WOLF_HUNT_SPEED = 1.30D;
    private static final double FOX_HUNT_SPEED = 1.38D;

    /*
     * Cats and ocelots were feeling slow/glitchy in HUNT.
     * They need a faster chase layer before the strike system takes over.
     */
    private static final double CAT_HUNT_SPEED = 1.62D;

    private static final double SPIDER_HUNT_SPEED = 1.18D;
    private static final double DOLPHIN_HUNT_SPEED = 1.40D;

    private static final double MIN_HUNT_SPEED = 0.95D;
    private static final double MAX_HUNT_SPEED = 1.85D;

    private static final double AUDIBLE_MOVEMENT_THRESHOLD_SQUARED = 0.0016D;

    private RetoldControlledHuntingEvents() {
    }

    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof PathfinderMob hunter)) {
            return;
        }

        if (!(hunter.level() instanceof ServerLevel level)) {
            return;
        }

        if (!RetoldMobRules.isManagedPredator(hunter)) {
            clearHuntMemory(hunter);
            RetoldPredatorStrike.clear(hunter);
            return;
        }

        long gameTime = level.getGameTime();

        /*
         * Hard stale cleanup:
         * if Retold is no longer controlling this predator and it has no target,
         * remove old hunt/strike state.
         */
        if (!RetoldAiControl.isControlled(hunter)) {
            if (hunter.getTarget() == null) {
                hunter.setSprinting(false);
                clearHuntMemory(hunter);
                RetoldPredatorStrike.clear(hunter);
            }
        }

        if (RetoldAiControl.isControlledAs(hunter, RetoldAiControlMode.HUNT)) {
            stabilizeHuntMotion(
                    hunter
            );

            if (isStaleHunt(level, hunter, gameTime)) {
                stopHunt(
                        hunter,
                        gameTime
                );
                return;
            }

            if (shouldThink(hunter, gameTime)) {
                continueActiveHunt(
                        level,
                        hunter,
                        gameTime
                );
            }

            return;
        }

        if (!shouldThink(hunter, gameTime)) {
            return;
        }

        if (!shouldStartHunt(level, hunter, gameTime)) {
            clearHuntMemory(hunter);
            RetoldPredatorStrike.clear(hunter);
            return;
        }

        LivingEntity prey = findBestPrey(
                level,
                hunter,
                gameTime
        );

        if (prey == null) {
            return;
        }

        beginHunt(
                hunter,
                prey,
                gameTime
        );
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity killed = event.getEntity();

        if (!(killed.level() instanceof ServerLevel level)) {
            return;
        }

        Entity sourceEntity = event.getSource().getEntity();

        if (!(sourceEntity instanceof PathfinderMob killer)) {
            return;
        }

        if (!RetoldMobRules.isManagedPredator(killer)) {
            return;
        }

        long gameTime = level.getGameTime();

        if (!RetoldPreyTargeting.isValidMobRulePrey(
                killer,
                killed,
                gameTime
        )) {
            return;
        }

        RetoldMobStates.getOrCreate(
                killer,
                gameTime
        ).markSuccessfulHunt(gameTime);

        clearHuntMemory(killer);
        RetoldPredatorStrike.clear(killer);

        RetoldAiControl.claim(
                killer,
                RetoldAiControlMode.FEED,
                gameTime,
                FEED_LOCK_AFTER_KILL_TICKS
        );

        RetoldBehaviorTargets.setTargetAndAggression(killer, null, false);

        killer.setSprinting(false);
        killer.getNavigation().stop();
    }

    private static boolean shouldThink(
            PathfinderMob hunter,
            long gameTime
    ) {
        return RetoldBehaviorTiming.shouldThink(
                hunter,
                gameTime,
                HUNT_THINK_INTERVAL_TICKS
        );
    }

    private static boolean shouldStartHunt(
            ServerLevel level,
            PathfinderMob hunter,
            long gameTime
    ) {
        if (hunter == null || !hunter.isAlive() || hunter.isRemoved()) {
            return false;
        }

        if (hunter instanceof TamableAnimal tamableAnimal && tamableAnimal.isTame()) {
            return false;
        }

        if (RetoldAiControl.isControlled(hunter)) {
            return false;
        }

        if (RetoldBehaviorCoordinator.hasLiveTarget(hunter)) {
            return false;
        }

        RetoldMobState state = RetoldMobStates.getOrCreate(
                hunter,
                gameTime
        );

        if (!RetoldMobRules.hasHuntDrive(hunter, state)) {
            return false;
        }

        return !hasEasyFoodNearby(
                level,
                hunter,
                gameTime
        );
    }

    private static void beginHunt(
            PathfinderMob hunter,
            LivingEntity prey,
            long gameTime
    ) {
        rememberPrey(
                hunter,
                prey,
                gameTime
        );

        RetoldAiControl.claim(
                hunter,
                RetoldAiControlMode.HUNT,
                gameTime,
                HUNT_CONTROL_TICKS
        );

        hunter.setSprinting(true);

        RetoldBehaviorTargets.setTargetAndAggression(hunter, prey, true);

        hunter.getLookControl().setLookAt(
                prey,
                35.0F,
                35.0F
        );

        RetoldAiControl.withNavigationBypass(() -> {
            hunter.getNavigation().moveTo(
                    prey,
                    getHuntSpeed(
                            hunter,
                            gameTime
                    )
            );
        });
    }

    private static void continueActiveHunt(
            ServerLevel level,
            PathfinderMob hunter,
            long gameTime
    ) {
        LivingEntity prey = getCurrentOrRememberedPrey(
                level,
                hunter,
                gameTime
        );

        HuntMemory memory = HUNT_MEMORIES.get(hunter);

        if (prey == null) {
            if (memory != null && !memory.isExpired(gameTime)) {
                moveToRememberedSearchPoint(
                        hunter,
                        memory,
                        gameTime
                );
                return;
            }

            stopHunt(
                    hunter,
                    gameTime
            );
            return;
        }

        if (!isValidHuntPrey(hunter, prey, gameTime)) {
            stopHunt(
                    hunter,
                    gameTime
            );
            return;
        }

        if (hunter.distanceToSqr(prey) > HUNT_ABANDON_DISTANCE_SQUARED) {
            stopHunt(
                    hunter,
                    gameTime
            );
            return;
        }

        /*
         * Food-first rule during active hunt:
         * if edible dropped food is nearby, stop chasing and let FEED take over.
         */
        if (hasEasyFoodNearby(level, hunter, gameTime)) {
            stopHuntForFood(
                    hunter,
                    gameTime
            );
            return;
        }

        boolean canDirectlySense = canDirectlySensePrey(
                hunter,
                prey
        );

        boolean canUseTrail = canUseTrailSense(
                hunter,
                prey,
                memory,
                gameTime
        );

        if (canDirectlySense || canUseTrail) {
            rememberPrey(
                    hunter,
                    prey,
                    gameTime
            );

            RetoldAiControl.refresh(
                    hunter,
                    RetoldAiControlMode.HUNT,
                    gameTime,
                    HUNT_CONTROL_TICKS
            );

            hunter.setSprinting(true);

            RetoldBehaviorTargets.setTargetAndAggression(hunter, prey, true);

            boolean struck = RetoldPredatorStrike.tryStrike(
                    level,
                    hunter,
                    prey,
                    gameTime
            );

            if (struck) {
                return;
            }

            RetoldAiControl.withNavigationBypass(() -> {
                hunter.getNavigation().moveTo(
                        prey,
                        getHuntSpeed(
                                hunter,
                                gameTime
                        )
                );
            });

            return;
        }

        memory = HUNT_MEMORIES.get(hunter);

        if (memory != null && !memory.isExpired(gameTime)) {
            moveToRememberedSearchPoint(
                    hunter,
                    memory,
                    gameTime
            );
            return;
        }

        stopHunt(
                hunter,
                gameTime
        );
    }

    private static boolean isStaleHunt(
            ServerLevel level,
            PathfinderMob hunter,
            long gameTime
    ) {
        LivingEntity target = hunter.getTarget();

        if (
                target != null
                        && isValidHuntPrey(hunter, target, gameTime)
                        && hunter.distanceToSqr(target) <= HUNT_ABANDON_DISTANCE_SQUARED
        ) {
            return false;
        }

        HuntMemory memory = HUNT_MEMORIES.get(hunter);

        if (memory == null || memory.isExpired(gameTime)) {
            return true;
        }

        LivingEntity rememberedPrey = getRememberedPrey(
                level,
                memory
        );

        if (
                rememberedPrey != null
                        && isValidHuntPrey(hunter, rememberedPrey, gameTime)
                        && hunter.distanceToSqr(rememberedPrey) <= HUNT_ABANDON_DISTANCE_SQUARED
        ) {
            return false;
        }

        return blockDistanceSquared(
                hunter.blockPosition(),
                memory.lastKnownPos()
        ) > HUNT_ABANDON_DISTANCE_SQUARED;
    }

    private static LivingEntity findBestPrey(
            ServerLevel level,
            PathfinderMob hunter,
            long gameTime
    ) {
        AABB area = hunter.getBoundingBox().inflate(PREY_SEARCH_RADIUS_BLOCKS);

        List<LivingEntity> candidates = level.getEntitiesOfClass(
                LivingEntity.class,
                area,
                candidate -> isValidSearchPrey(
                        hunter,
                        candidate,
                        gameTime
                )
        );

        LivingEntity bestPrey = null;
        double bestScore = Double.MAX_VALUE;

        for (LivingEntity candidate : candidates) {
            double distanceSquared = hunter.distanceToSqr(candidate);

            if (distanceSquared > PREY_SEARCH_RADIUS_SQUARED) {
                continue;
            }

            if (!canDirectlySensePrey(hunter, candidate)) {
                continue;
            }

            double score = distanceSquared;

            if (hunter.hasLineOfSight(candidate)) {
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

    private static boolean isValidSearchPrey(
            PathfinderMob hunter,
            LivingEntity prey,
            long gameTime
    ) {
        return isValidHuntPrey(
                hunter,
                prey,
                gameTime
        ) && hunter.distanceToSqr(prey) <= PREY_SEARCH_RADIUS_SQUARED;
    }

    private static boolean isValidHuntPrey(
            PathfinderMob hunter,
            LivingEntity prey,
            long gameTime
    ) {
        return RetoldPreyTargeting.isValidMobRulePrey(
                hunter,
                prey,
                gameTime
        );
    }

    private static boolean canDirectlySensePrey(
            PathfinderMob hunter,
            LivingEntity prey
    ) {
        double distanceSquared = hunter.distanceToSqr(prey);

        if (
                distanceSquared <= SIGHT_RADIUS_SQUARED
                        && hunter.hasLineOfSight(prey)
        ) {
            return true;
        }

        if (
                distanceSquared <= HEARING_RADIUS_SQUARED
                        && isAudible(prey)
        ) {
            return true;
        }

        return distanceSquared <= SMELL_RADIUS_SQUARED;
    }

    private static boolean canUseTrailSense(
            PathfinderMob hunter,
            LivingEntity prey,
            HuntMemory memory,
            long gameTime
    ) {
        if (memory == null || memory.isExpired(gameTime)) {
            return false;
        }

        if (!memory.preyId().equals(prey.getUUID())) {
            return false;
        }

        if (hunter.distanceToSqr(prey) <= SMELL_RADIUS_SQUARED) {
            return true;
        }

        return blockDistanceSquared(
                hunter.blockPosition(),
                memory.lastKnownPos()
        ) <= TRAIL_REACQUIRE_RADIUS_SQUARED;
    }

    private static boolean isAudible(LivingEntity entity) {
        Vec3 movement = entity.getDeltaMovement();
        double horizontalMovementSquared = movement.x * movement.x + movement.z * movement.z;

        return horizontalMovementSquared >= AUDIBLE_MOVEMENT_THRESHOLD_SQUARED;
    }

    private static LivingEntity getCurrentOrRememberedPrey(
            ServerLevel level,
            PathfinderMob hunter,
            long gameTime
    ) {
        LivingEntity target = hunter.getTarget();

        if (isValidHuntPrey(hunter, target, gameTime)) {
            return target;
        }

        HuntMemory memory = HUNT_MEMORIES.get(hunter);

        if (memory == null || memory.isExpired(gameTime)) {
            return null;
        }

        LivingEntity remembered = getRememberedPrey(
                level,
                memory
        );

        if (isValidHuntPrey(hunter, remembered, gameTime)) {
            return remembered;
        }

        return null;
    }

    private static LivingEntity getRememberedPrey(
            ServerLevel level,
            HuntMemory memory
    ) {
        if (level == null || memory == null) {
            return null;
        }

        Entity entity = level.getEntity(
                memory.preyId()
        );

        if (entity instanceof LivingEntity livingEntity) {
            return livingEntity;
        }

        return null;
    }

    private static void rememberPrey(
            PathfinderMob hunter,
            LivingEntity prey,
            long gameTime
    ) {
        Vec3 movement = prey.getDeltaMovement();

        Vec3 horizontalMovement = new Vec3(
                movement.x,
                0.0D,
                movement.z
        );

        HUNT_MEMORIES.put(
                hunter,
                new HuntMemory(
                        prey.getUUID(),
                        prey.blockPosition().immutable(),
                        horizontalMovement,
                        gameTime,
                        gameTime + HUNT_TRAIL_MEMORY_TICKS,
                        0
                )
        );
    }

    private static void moveToRememberedSearchPoint(
            PathfinderMob hunter,
            HuntMemory memory,
            long gameTime
    ) {
        if (memory == null || memory.isExpired(gameTime)) {
            stopHunt(
                    hunter,
                    gameTime
            );
            return;
        }

        if (
                blockDistanceSquared(
                        hunter.blockPosition(),
                        memory.lastKnownPos()
                ) <= SEARCH_POINT_REACHED_DISTANCE_SQUARED
        ) {
            memory.searchStep++;
        }

        Vec3 movement = memory.lastKnownMovement();

        Vec3 direction;

        if (movement != null && movement.lengthSqr() > 0.0001D) {
            direction = movement.normalize();
        } else {
            direction = randomHorizontalDirection(hunter);
        }

        Vec3 side = new Vec3(
                -direction.z,
                0.0D,
                direction.x
        );

        double sideOffset = switch (memory.searchStep() % 5) {
            case 0 -> 0.0D;
            case 1 -> 2.0D;
            case 2 -> -2.0D;
            case 3 -> 4.0D;
            default -> -4.0D;
        };

        double forwardOffset = 3.0D + memory.searchStep() * 1.5D;

        Vec3 target = new Vec3(
                memory.lastKnownPos().getX() + 0.5D,
                memory.lastKnownPos().getY(),
                memory.lastKnownPos().getZ() + 0.5D
        )
                .add(direction.scale(forwardOffset))
                .add(side.scale(sideOffset));

        RetoldAiControl.refresh(
                hunter,
                RetoldAiControlMode.HUNT,
                gameTime,
                HUNT_CONTROL_TICKS
        );

        hunter.setSprinting(true);

        RetoldAiControl.withNavigationBypass(() -> {
            hunter.getNavigation().moveTo(
                    target.x,
                    target.y,
                    target.z,
                    getHuntSpeed(
                            hunter,
                            gameTime
                    )
            );
        });
    }

    private static boolean hasEasyFoodNearby(
            ServerLevel level,
            PathfinderMob hunter,
            long gameTime
    ) {
        if (level == null || hunter == null || !hunter.isAlive() || hunter.isRemoved()) {
            return false;
        }

        RetoldMobState state = RetoldMobStates.getOrCreate(
                hunter,
                gameTime
        );

        if (state.hunger() < RetoldMobRules.eatThreshold(hunter)) {
            return false;
        }

        AABB area = hunter.getBoundingBox().inflate(EASY_FOOD_RADIUS_BLOCKS);

        List<ItemEntity> foods = level.getEntitiesOfClass(
                ItemEntity.class,
                area,
                item -> isValidEasyFood(
                        hunter,
                        item
                )
        );

        for (ItemEntity food : foods) {
            if (hunter.distanceToSqr(food) <= EASY_FOOD_RADIUS_SQUARED) {
                return true;
            }
        }

        return false;
    }

    private static boolean isValidEasyFood(
            PathfinderMob hunter,
            ItemEntity item
    ) {
        if (hunter == null || item == null) {
            return false;
        }

        if (!item.isAlive() || item.isRemoved()) {
            return false;
        }

        if (item.getItem().isEmpty()) {
            return false;
        }

        if (hunter.distanceToSqr(item) > EASY_FOOD_RADIUS_SQUARED) {
            return false;
        }

        if (!hunter.hasLineOfSight(item) && hunter.distanceToSqr(item) > CLOSE_CHASE_DISTANCE_SQUARED) {
            return false;
        }

        return RetoldMobRules.canEatDroppedItem(
                hunter,
                item.getItem()
        );
    }

    private static void stopHuntForFood(
            PathfinderMob hunter,
            long gameTime
    ) {
        clearHuntMemory(hunter);
        RetoldPredatorStrike.clear(hunter);

        RetoldAiControl.claim(
                hunter,
                RetoldAiControlMode.FEED,
                gameTime,
                FEED_LOCK_AFTER_KILL_TICKS
        );

        RetoldBehaviorTargets.setTargetAndAggression(hunter, null, false);

        hunter.setSprinting(false);
        hunter.getNavigation().stop();
    }

    private static void stopHunt(
            PathfinderMob hunter,
            long gameTime
    ) {
        RetoldMobStates.getOrCreate(
                hunter,
                gameTime
        ).markFailedHunt(gameTime);

        clearHuntMemory(hunter);
        RetoldPredatorStrike.clear(hunter);

        RetoldAiControl.clear(hunter);

        RetoldBehaviorTargets.setTargetAndAggression(hunter, null, false);

        hunter.setSprinting(false);
        hunter.getNavigation().stop();
    }

    private static void clearHuntMemory(PathfinderMob hunter) {
        HUNT_MEMORIES.remove(hunter);
    }

    private static double getHuntSpeed(
            PathfinderMob hunter,
            long gameTime
    ) {
        String path = RetoldMobRules.getEntityTypePath(
                hunter.getType()
        );

        double baseSpeed = DEFAULT_HUNT_SPEED;

        if (path.equals("wolf")) {
            baseSpeed = WOLF_HUNT_SPEED;
        } else if (path.equals("cat") || path.equals("ocelot")) {
            baseSpeed = CAT_HUNT_SPEED;
        } else if (path.equals("fox")) {
            baseSpeed = FOX_HUNT_SPEED;
        } else if (path.equals("spider") || path.equals("cave_spider")) {
            baseSpeed = SPIDER_HUNT_SPEED;
        } else if (path.equals("dolphin")) {
            baseSpeed = DOLPHIN_HUNT_SPEED;
        }

        /*
         * Strike multiplier is currently 1.0 in the latest strike file,
         * but keeping the hook here lets strike logic tune chase speed later.
         */
        double speed = baseSpeed * RetoldPredatorStrike.chaseSpeedMultiplier(
                hunter,
                gameTime
        );

        return clamp(
                speed,
                MIN_HUNT_SPEED,
                MAX_HUNT_SPEED
        );
    }

    private static Vec3 randomHorizontalDirection(PathfinderMob hunter) {
        double angle = hunter.getRandom().nextDouble() * Math.PI * 2.0D;

        return new Vec3(
                Math.cos(angle),
                0.0D,
                Math.sin(angle)
        );
    }

    private static double blockDistanceSquared(
            BlockPos first,
            BlockPos second
    ) {
        double x = first.getX() - second.getX();
        double y = first.getY() - second.getY();
        double z = first.getZ() - second.getZ();

        return x * x + y * y + z * z;
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

    private static void stabilizeHuntMotion(PathfinderMob hunter) {
        String path = RetoldMobRules.getEntityTypePath(
                hunter.getType()
        );

        hunter.setSprinting(true);

        /*
         * Foxes still have vanilla pounce/jump behavior fighting Retold.
         * During Retold HUNT, suppress the big upward vanilla leap.
         * RetoldPredatorStrike still handles the forward intercept lunge.
         */
        if (path.equals("fox")) {
            Vec3 movement = hunter.getDeltaMovement();

            if (movement.y > 0.075D) {
                hunter.setDeltaMovement(
                        movement.x,
                        0.075D,
                        movement.z
                );
            }
        }

        /*
         * Cats/ocelots should stay committed during HUNT.
         * Their speed is controlled by getHuntSpeed(...), but keeping sprint on
         * prevents some vanilla-looking slow/stutter behavior.
         */
        if (path.equals("cat") || path.equals("ocelot")) {
            hunter.setSprinting(true);
        }
    }

    private static final class HuntMemory {
        private final UUID preyId;
        private final BlockPos lastKnownPos;
        private final Vec3 lastKnownMovement;
        private final long lastSeenAt;
        private final long expiresAt;
        private int searchStep;

        private HuntMemory(
                UUID preyId,
                BlockPos lastKnownPos,
                Vec3 lastKnownMovement,
                long lastSeenAt,
                long expiresAt,
                int searchStep
        ) {
            this.preyId = preyId;
            this.lastKnownPos = lastKnownPos;
            this.lastKnownMovement = lastKnownMovement;
            this.lastSeenAt = lastSeenAt;
            this.expiresAt = expiresAt;
            this.searchStep = searchStep;
        }

        private UUID preyId() {
            return preyId;
        }

        private BlockPos lastKnownPos() {
            return lastKnownPos;
        }

        private Vec3 lastKnownMovement() {
            return lastKnownMovement;
        }

        private int searchStep() {
            return searchStep;
        }

        private boolean isExpired(long gameTime) {
            return gameTime > expiresAt;
        }
    }
}
