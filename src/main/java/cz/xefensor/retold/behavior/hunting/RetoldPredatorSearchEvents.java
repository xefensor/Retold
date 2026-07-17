package cz.xefensor.retold.behavior.hunting;

import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.control.RetoldAiControlMode;
import cz.xefensor.retold.behavior.performance.RetoldAiScanCache;
import cz.xefensor.retold.behavior.performance.RetoldAiSightCache;
import cz.xefensor.retold.behavior.core.RetoldBehaviorCoordinator;
import cz.xefensor.retold.behavior.core.RetoldBehaviorMovement;
import cz.xefensor.retold.behavior.core.RetoldBehaviorTargets;
import cz.xefensor.retold.behavior.core.RetoldBehaviorTiming;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;
import cz.xefensor.retold.behavior.profiles.RetoldMobState;
import cz.xefensor.retold.behavior.profiles.RetoldMobStates;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class RetoldPredatorSearchEvents {
    private static final Map<PathfinderMob, SearchMemory> SEARCH_MEMORIES = new WeakHashMap<>();

    private static final int SEARCH_THINK_INTERVAL_TICKS = 20;
    private static final int SEARCH_SCAN_CACHE_TICKS = 8;
    private static final int SEARCH_PATH_INTERVAL_TICKS = 10;
    private static final int SEARCH_CONTROL_TICKS = 20 * 5;

    /*
     * One point lasts only a few seconds, but the direction lasts longer.
     * This makes predators commit to a direction instead of circling randomly.
     */
    private static final int SEARCH_POINT_LIFE_TICKS = 20 * 5;
    private static final int SEARCH_HEADING_LIFE_TICKS = 20 * 45;

    private static final double DIRECT_PREY_SEARCH_RADIUS_BLOCKS = 20.0D;
    private static final double DIRECT_PREY_SEARCH_RADIUS_SQUARED =
            DIRECT_PREY_SEARCH_RADIUS_BLOCKS * DIRECT_PREY_SEARCH_RADIUS_BLOCKS;

    private static final double SCENT_CLUE_RADIUS_BLOCKS = 34.0D;
    private static final double SCENT_CLUE_RADIUS_SQUARED =
            SCENT_CLUE_RADIUS_BLOCKS * SCENT_CLUE_RADIUS_BLOCKS;

    private static final double NEAR_SCENT_RADIUS_BLOCKS = 16.0D;
    private static final double NEAR_SCENT_RADIUS_SQUARED =
            NEAR_SCENT_RADIUS_BLOCKS * NEAR_SCENT_RADIUS_BLOCKS;

    private static final double SIGHT_RADIUS_BLOCKS = 20.0D;
    private static final double SIGHT_RADIUS_SQUARED =
            SIGHT_RADIUS_BLOCKS * SIGHT_RADIUS_BLOCKS;

    private static final double HEARING_RADIUS_BLOCKS = 8.0D;
    private static final double HEARING_RADIUS_SQUARED =
            HEARING_RADIUS_BLOCKS * HEARING_RADIUS_BLOCKS;

    private static final double SMELL_RADIUS_BLOCKS = 6.0D;
    private static final double SMELL_RADIUS_SQUARED =
            SMELL_RADIUS_BLOCKS * SMELL_RADIUS_BLOCKS;

    private static final double EASY_FOOD_RADIUS_BLOCKS = 8.0D;
    private static final double EASY_FOOD_RADIUS_SQUARED =
            EASY_FOOD_RADIUS_BLOCKS * EASY_FOOD_RADIUS_BLOCKS;

    private static final double SEARCH_POINT_REACHED_DISTANCE_BLOCKS = 2.75D;
    private static final double SEARCH_POINT_REACHED_DISTANCE_SQUARED =
            SEARCH_POINT_REACHED_DISTANCE_BLOCKS * SEARCH_POINT_REACHED_DISTANCE_BLOCKS;

    private static final double SEARCH_SEGMENT_DISTANCE_BLOCKS = 18.0D;
    private static final double SEARCH_SIDE_WOBBLE_BLOCKS = 1.75D;

    private static final double AUDIBLE_MOVEMENT_THRESHOLD_SQUARED = 0.0016D;

    private static final double DEFAULT_SEARCH_SPEED = 0.90D;
    private static final double CAT_SEARCH_SPEED = 0.96D;
    private static final double FOX_SEARCH_SPEED = 0.98D;
    private static final double SPIDER_SEARCH_SPEED = 0.82D;
    private static final double DOLPHIN_SEARCH_SPEED = 1.08D;

    private static final double OPENING_HUNT_SPEED = 1.25D;

    private RetoldPredatorSearchEvents() {
    }

    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof PathfinderMob predator)) {
            return;
        }

        if (!(predator.level() instanceof ServerLevel level)) {
            return;
        }

        if (!RetoldMobRules.canUseOrdinaryPredatorSystems(predator)) {
            return;
        }

        long gameTime = level.getGameTime();

        if (!shouldThink(predator, gameTime)) {
            return;
        }

        if (!canUseSearchLayer(level, predator, gameTime)) {
            if (RetoldAiControl.isControlledAs(predator, RetoldAiControlMode.SEARCH)) {
                stopSearch(predator);
            }

            return;
        }

        LivingEntity sensedPrey = findSensedPrey(
                level,
                predator,
                gameTime
        );

        if (sensedPrey != null) {
            beginHuntFromSearch(
                    predator,
                    sensedPrey,
                    gameTime
            );
            return;
        }

        continueSearch(
                level,
                predator,
                gameTime
        );
    }

    private static boolean shouldThink(
            PathfinderMob predator,
            long gameTime
    ) {
        return RetoldBehaviorTiming.shouldThink(
                predator,
                gameTime,
                SEARCH_THINK_INTERVAL_TICKS
        );
    }

    private static boolean canUseSearchLayer(
            ServerLevel level,
            PathfinderMob predator,
            long gameTime
    ) {
        if (predator == null || level == null) {
            return false;
        }

        if (!predator.isAlive() || predator.isRemoved()) {
            return false;
        }

        if (predator instanceof TamableAnimal tamableAnimal && tamableAnimal.isTame()) {
            return false;
        }

        RetoldAiControlMode mode = RetoldAiControl.getMode(predator);

        if (mode != RetoldAiControlMode.NONE && mode != RetoldAiControlMode.SEARCH) {
            return false;
        }

        if (RetoldBehaviorCoordinator.hasLiveTarget(predator)) {
            return false;
        }

        RetoldMobState state = RetoldMobStates.getOrCreate(
                predator,
                gameTime
        );

        if (!RetoldMobRules.hasHuntDrive(predator, state)) {
            return false;
        }

        return !hasEasyFoodNearby(
                level,
                predator
        );
    }

    private static LivingEntity findSensedPrey(
            ServerLevel level,
            PathfinderMob predator,
            long gameTime
    ) {
        List<LivingEntity> candidates = RetoldAiScanCache.nearby(
                level,
                predator,
                LivingEntity.class,
                DIRECT_PREY_SEARCH_RADIUS_BLOCKS,
                gameTime,
                SEARCH_SCAN_CACHE_TICKS
        );

        LivingEntity bestPrey = null;
        double bestScore = Double.MAX_VALUE;

        for (LivingEntity prey : candidates) {
            if (!isValidSensedPrey(predator, prey, gameTime)) {
                continue;
            }

            double distanceSquared = predator.distanceToSqr(prey);

            if (distanceSquared > DIRECT_PREY_SEARCH_RADIUS_SQUARED) {
                continue;
            }

            double score = distanceSquared;

            if (RetoldAiSightCache.canSee(predator, prey, gameTime)) {
                score -= 24.0D;
            }

            if (RetoldMobRules.isSmallFoodPrey(prey)) {
                score -= 10.0D;
            }

            if (score < bestScore) {
                bestScore = score;
                bestPrey = prey;
            }
        }

        return bestPrey;
    }

    private static boolean isValidSensedPrey(
            PathfinderMob predator,
            LivingEntity prey,
            long gameTime
    ) {
        if (!isValidSearchPrey(predator, prey, gameTime)) {
            return false;
        }

        if (predator.distanceToSqr(prey) > DIRECT_PREY_SEARCH_RADIUS_SQUARED) {
            return false;
        }

        return canSensePrey(
                predator,
                prey,
                gameTime
        );
    }

    private static boolean canSensePrey(
            PathfinderMob predator,
            LivingEntity prey,
            long gameTime
    ) {
        double distanceSquared = predator.distanceToSqr(prey);

        if (
                distanceSquared <= SIGHT_RADIUS_SQUARED
                        && RetoldAiSightCache.canSee(predator, prey, gameTime)
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

    private static void beginHuntFromSearch(
            PathfinderMob predator,
            LivingEntity prey,
            long gameTime
    ) {
        SEARCH_MEMORIES.remove(predator);

        RetoldAiControl.claim(
                predator,
                RetoldAiControlMode.HUNT,
                gameTime,
                SEARCH_CONTROL_TICKS
        );

        predator.setSprinting(true);

        if (!RetoldBehaviorTargets.setAttackTargetOrClearMode(
                predator,
                prey,
                RetoldAiControlMode.HUNT
        )) {
            predator.setSprinting(false);
            return;
        }

        predator.getLookControl().setLookAt(
                prey,
                35.0F,
                35.0F
        );

        RetoldBehaviorMovement.throttledMoveTo(
                predator,
                prey,
                OPENING_HUNT_SPEED,
                gameTime,
                SEARCH_PATH_INTERVAL_TICKS,
                2.5D * 2.5D
        );
    }

    private static void continueSearch(
            ServerLevel level,
            PathfinderMob predator,
            long gameTime
    ) {
        SearchMemory memory = SEARCH_MEMORIES.get(predator);

        if (memory == null || memory.isHeadingExpired(gameTime)) {
            memory = createNewHeadingMemory(
                    level,
                    predator,
                    gameTime
            );

            SEARCH_MEMORIES.put(
                    predator,
                    memory
            );
        } else if (
                memory.isPointExpired(gameTime)
                        || predator.blockPosition().distSqr(memory.searchPos()) <= SEARCH_POINT_REACHED_DISTANCE_SQUARED
        ) {
            memory = advanceAlongHeading(
                    predator,
                    memory,
                    gameTime
            );

            SEARCH_MEMORIES.put(
                    predator,
                    memory
            );
        }

        RetoldAiControl.refresh(
                predator,
                RetoldAiControlMode.SEARCH,
                gameTime,
                SEARCH_CONTROL_TICKS
        );

        predator.setSprinting(false);

        BlockPos searchPos = memory.searchPos();

        predator.getLookControl().setLookAt(
                searchPos.getX() + 0.5D,
                searchPos.getY() + 0.5D,
                searchPos.getZ() + 0.5D,
                25.0F,
                25.0F
        );

        RetoldBehaviorMovement.throttledMoveTo(
                predator,
                searchPos,
                getSearchSpeed(predator),
                gameTime,
                SEARCH_PATH_INTERVAL_TICKS,
                2.0D * 2.0D
        );
    }

    private static SearchMemory createNewHeadingMemory(
            ServerLevel level,
            PathfinderMob predator,
            long gameTime
    ) {
        BlockPos cluePos = findScentCluePosition(
                level,
                predator,
                gameTime
        );

        if (cluePos != null) {
            Vec3 direction = horizontalDirectionToward(
                    predator,
                    cluePos
            );

            return new SearchMemory(
                    cluePos.immutable(),
                    direction,
                    0,
                    gameTime + SEARCH_POINT_LIFE_TICKS,
                    gameTime + SEARCH_HEADING_LIFE_TICKS
            );
        }

        Vec3 direction = randomHorizontalDirection(predator);

        return new SearchMemory(
                getDirectionalSearchPoint(
                        predator,
                        direction,
                        0
                ),
                direction,
                0,
                gameTime + SEARCH_POINT_LIFE_TICKS,
                gameTime + SEARCH_HEADING_LIFE_TICKS
        );
    }

    private static SearchMemory advanceAlongHeading(
            PathfinderMob predator,
            SearchMemory previous,
            long gameTime
    ) {
        int nextStep = previous.step() + 1;
        Vec3 direction = previous.direction();

        return new SearchMemory(
                getDirectionalSearchPoint(
                        predator,
                        direction,
                        nextStep
                ),
                direction,
                nextStep,
                gameTime + SEARCH_POINT_LIFE_TICKS,
                previous.headingExpiresAt()
        );
    }

    private static BlockPos getDirectionalSearchPoint(
            PathfinderMob predator,
            Vec3 direction,
            int step
    ) {
        Vec3 safeDirection = safeHorizontalDirection(
                predator,
                direction
        );

        Vec3 side = new Vec3(
                -safeDirection.z,
                0.0D,
                safeDirection.x
        );

        /*
         * Same heading, small side checks.
         * This avoids a perfect straight line while still preventing circling.
         */
        double sideOffset = switch (step % 5) {
            case 1 -> SEARCH_SIDE_WOBBLE_BLOCKS;
            case 2 -> -SEARCH_SIDE_WOBBLE_BLOCKS;
            case 3 -> SEARCH_SIDE_WOBBLE_BLOCKS * 0.5D;
            case 4 -> -SEARCH_SIDE_WOBBLE_BLOCKS * 0.5D;
            default -> 0.0D;
        };

        Vec3 target = predator.position()
                .add(safeDirection.scale(SEARCH_SEGMENT_DISTANCE_BLOCKS))
                .add(side.scale(sideOffset));

        return new BlockPos(
                (int) Math.floor(target.x),
                predator.blockPosition().getY(),
                (int) Math.floor(target.z)
        ).immutable();
    }

    private static BlockPos findScentCluePosition(
            ServerLevel level,
            PathfinderMob predator,
            long gameTime
    ) {
        List<LivingEntity> candidates = RetoldAiScanCache.nearby(
                level,
                predator,
                LivingEntity.class,
                SCENT_CLUE_RADIUS_BLOCKS,
                gameTime,
                SEARCH_SCAN_CACHE_TICKS
        );

        LivingEntity bestPrey = null;
        double bestScore = Double.MAX_VALUE;

        for (LivingEntity prey : candidates) {
            if (!isValidSearchPrey(predator, prey, gameTime)) {
                continue;
            }

            double distanceSquared = predator.distanceToSqr(prey);

            if (distanceSquared > SCENT_CLUE_RADIUS_SQUARED) {
                continue;
            }

            /*
             * Hidden far prey only gives an occasional clue.
             * Near prey, audible prey, or small prey are easier to track.
             */
            if (
                    distanceSquared > NEAR_SCENT_RADIUS_SQUARED
                            && !isAudible(prey)
                            && !RetoldMobRules.isSmallFoodPrey(prey)
                            && predator.getRandom().nextDouble() > 0.45D
            ) {
                continue;
            }

            double score = distanceSquared;

            if (isAudible(prey)) {
                score -= 18.0D;
            }

            if (RetoldMobRules.isSmallFoodPrey(prey)) {
                score -= 8.0D;
            }

            if (score < bestScore) {
                bestScore = score;
                bestPrey = prey;
            }
        }

        if (bestPrey == null) {
            return null;
        }

        return getOffsetCluePosition(
                predator,
                bestPrey
        );
    }

    private static BlockPos getOffsetCluePosition(
            PathfinderMob predator,
            LivingEntity prey
    ) {
        Vec3 movement = prey.getDeltaMovement();

        Vec3 direction;

        if (movement.lengthSqr() > 0.0001D) {
            direction = new Vec3(
                    movement.x,
                    0.0D,
                    movement.z
            ).normalize();
        } else {
            direction = horizontalDirectionToward(
                    predator,
                    prey.blockPosition()
            );
        }

        Vec3 side = new Vec3(
                -direction.z,
                0.0D,
                direction.x
        );

        double forwardOffset = 2.0D + predator.getRandom().nextDouble() * 5.0D;
        double sideOffset = (predator.getRandom().nextDouble() - 0.5D) * 8.0D;

        Vec3 target = prey.position()
                .add(direction.scale(forwardOffset))
                .add(side.scale(sideOffset));

        return new BlockPos(
                (int) Math.floor(target.x),
                prey.blockPosition().getY(),
                (int) Math.floor(target.z)
        ).immutable();
    }

    private static boolean isValidSearchPrey(
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

    private static boolean hasEasyFoodNearby(
            ServerLevel level,
            PathfinderMob predator
    ) {
        List<ItemEntity> items = RetoldAiScanCache.nearby(
                level,
                predator,
                ItemEntity.class,
                EASY_FOOD_RADIUS_BLOCKS,
                level.getGameTime(),
                SEARCH_SCAN_CACHE_TICKS
        );

        for (ItemEntity item : items) {
            if (isEasyFood(predator, item)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isEasyFood(
            PathfinderMob predator,
            ItemEntity item
    ) {
        if (predator == null || item == null) {
            return false;
        }

        if (!item.isAlive() || item.isRemoved()) {
            return false;
        }

        if (item.getItem().isEmpty()) {
            return false;
        }

        if (predator.distanceToSqr(item) > EASY_FOOD_RADIUS_SQUARED) {
            return false;
        }

        if (
                !RetoldAiSightCache.canSee(predator, item, predator.level().getGameTime())
                        && predator.distanceToSqr(item) > 16.0D
        ) {
            return false;
        }

        return RetoldMobRules.canEatDroppedItem(
                predator,
                item.getItem()
        );
    }

    private static boolean isAudible(LivingEntity entity) {
        Vec3 movement = entity.getDeltaMovement();
        double horizontalMovementSquared = movement.x * movement.x + movement.z * movement.z;

        return horizontalMovementSquared >= AUDIBLE_MOVEMENT_THRESHOLD_SQUARED;
    }

    private static Vec3 horizontalDirectionToward(
            PathfinderMob predator,
            BlockPos target
    ) {
        Vec3 direction = new Vec3(
                target.getX() + 0.5D - predator.getX(),
                0.0D,
                target.getZ() + 0.5D - predator.getZ()
        );

        return safeHorizontalDirection(
                predator,
                direction
        );
    }

    private static Vec3 safeHorizontalDirection(
            PathfinderMob predator,
            Vec3 direction
    ) {
        if (direction != null && direction.lengthSqr() > 0.0001D) {
            return new Vec3(
                    direction.x,
                    0.0D,
                    direction.z
            ).normalize();
        }

        return randomHorizontalDirection(predator);
    }

    private static Vec3 randomHorizontalDirection(PathfinderMob predator) {
        double angle = predator.getRandom().nextDouble() * Math.PI * 2.0D;

        return new Vec3(
                Math.cos(angle),
                0.0D,
                Math.sin(angle)
        );
    }

    private static double getSearchSpeed(PathfinderMob predator) {
        String path = RetoldMobRules.getEntityTypePath(
                predator.getType()
        );

        if (path.equals("cat") || path.equals("ocelot")) {
            return CAT_SEARCH_SPEED;
        }

        if (path.equals("fox")) {
            return FOX_SEARCH_SPEED;
        }

        if (path.equals("spider") || path.equals("cave_spider")) {
            return SPIDER_SEARCH_SPEED;
        }

        if (path.equals("dolphin")) {
            return DOLPHIN_SEARCH_SPEED;
        }

        return DEFAULT_SEARCH_SPEED;
    }

    private static void stopSearch(PathfinderMob predator) {
        SEARCH_MEMORIES.remove(predator);
        RetoldAiControl.clear(predator);
        predator.setSprinting(false);
        predator.getNavigation().stop();
    }

    private record SearchMemory(
            BlockPos searchPos,
            Vec3 direction,
            int step,
            long pointExpiresAt,
            long headingExpiresAt
    ) {
        public boolean isPointExpired(long gameTime) {
            return gameTime > pointExpiresAt;
        }

        public boolean isHeadingExpired(long gameTime) {
            return gameTime > headingExpiresAt;
        }
    }
}
