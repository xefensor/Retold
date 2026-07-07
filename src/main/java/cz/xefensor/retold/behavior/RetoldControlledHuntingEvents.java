package cz.xefensor.retold.behavior;

import cz.xefensor.retold.combat.RetoldFactionTargetGuards;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
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

    private static final double PREY_SIGHT_RADIUS_BLOCKS = 18.0D;
    private static final double PREY_SIGHT_RADIUS_SQUARED =
            PREY_SIGHT_RADIUS_BLOCKS * PREY_SIGHT_RADIUS_BLOCKS;

    private static final double PREY_HEARING_RADIUS_BLOCKS = 6.0D;
    private static final double PREY_HEARING_RADIUS_SQUARED =
            PREY_HEARING_RADIUS_BLOCKS * PREY_HEARING_RADIUS_BLOCKS;

    private static final double PREY_SMELL_RADIUS_BLOCKS = 4.0D;
    private static final double PREY_SMELL_RADIUS_SQUARED =
            PREY_SMELL_RADIUS_BLOCKS * PREY_SMELL_RADIUS_BLOCKS;

    private static final double TRAIL_REACQUIRE_RADIUS_BLOCKS = 10.0D;
    private static final double TRAIL_REACQUIRE_RADIUS_SQUARED =
            TRAIL_REACQUIRE_RADIUS_BLOCKS * TRAIL_REACQUIRE_RADIUS_BLOCKS;

    private static final double TRAIL_DIRECT_SMELL_RADIUS_BLOCKS = 7.0D;
    private static final double TRAIL_DIRECT_SMELL_RADIUS_SQUARED =
            TRAIL_DIRECT_SMELL_RADIUS_BLOCKS * TRAIL_DIRECT_SMELL_RADIUS_BLOCKS;

    private static final double EASY_FOOD_RADIUS_BLOCKS = 8.0D;
    private static final double EASY_FOOD_RADIUS_SQUARED =
            EASY_FOOD_RADIUS_BLOCKS * EASY_FOOD_RADIUS_BLOCKS;

    private static final double PREY_MOVEMENT_HEARING_THRESHOLD_SQUARED = 0.0016D;

    private static final double CLOSE_CHASE_DISTANCE_BLOCKS = 5.0D;
    private static final double CLOSE_CHASE_DISTANCE_SQUARED =
            CLOSE_CHASE_DISTANCE_BLOCKS * CLOSE_CHASE_DISTANCE_BLOCKS;

    private static final double FAR_CHASE_DISTANCE_BLOCKS = 14.0D;
    private static final double FAR_CHASE_DISTANCE_SQUARED =
            FAR_CHASE_DISTANCE_BLOCKS * FAR_CHASE_DISTANCE_BLOCKS;

    private static final double HUNT_ABANDON_DISTANCE_BLOCKS = 44.0D;
    private static final double HUNT_ABANDON_DISTANCE_SQUARED =
            HUNT_ABANDON_DISTANCE_BLOCKS * HUNT_ABANDON_DISTANCE_BLOCKS;

    private static final double SEARCH_POINT_REACHED_DISTANCE_BLOCKS = 2.5D;
    private static final double SEARCH_POINT_REACHED_DISTANCE_SQUARED =
            SEARCH_POINT_REACHED_DISTANCE_BLOCKS * SEARCH_POINT_REACHED_DISTANCE_BLOCKS;

    private static final double DEFAULT_HUNT_BASE_SPEED = 1.30D;
    private static final double CAT_HUNT_BASE_SPEED = 1.34D;
    private static final double FOX_HUNT_BASE_SPEED = 1.34D;
    private static final double SPIDER_HUNT_BASE_SPEED = 1.18D;
    private static final double DOLPHIN_HUNT_BASE_SPEED = 1.40D;

    private static final double MIN_HUNT_SPEED = 0.95D;
    private static final double MAX_HUNT_SPEED = 1.66D;

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
            return;
        }

        long gameTime = level.getGameTime();

        /*
         * Hard stale cleanup:
         * If no Retold system owns the predator anymore, it must not keep sprinting
         * or keeping old strike/trail state.
         */
        if (!RetoldAiControl.isControlled(hunter)) {
            if (hunter.getTarget() == null) {
                hunter.setSprinting(false);
                clearHuntMemory(hunter);
                RetoldPredatorStrike.clear(hunter);
            }
        }

        /*
         * HUNT state is high priority and should be cleaned even between normal
         * think ticks. This prevents predators staying in HUNT with no prey.
         */
        if (RetoldAiControl.isControlledAs(hunter, RetoldAiControlMode.HUNT)) {
            if (isStaleHunt(level, hunter, gameTime)) {
                stopHunt(hunter);
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

        PathfinderMob killer = findResponsibleKiller(
                event,
                killed
        );

        if (killer == null) {
            return;
        }

        if (!RetoldMobRules.isManagedPredator(killer)) {
            return;
        }

        if (!RetoldMobRules.canHuntPrey(killer, killed, level.getGameTime())) {
            return;
        }

        /*
         * Kill does not reduce hunger.
         * Eating dropped food is the only thing that reduces hunger.
         */
        RetoldAiControl.claim(
                killer,
                RetoldAiControlMode.FEED,
                level.getGameTime(),
                FEED_LOCK_AFTER_KILL_TICKS
        );

        clearHuntMemory(killer);
        RetoldPredatorStrike.clear(killer);

        RetoldFactionTargetGuards.setTargetIgnoringGuard(
                killer,
                null
        );

        RetoldFactionTargetGuards.setAggressiveIgnoringGuard(
                killer,
                false
        );

        /*
         * Important:
         * kill exits sprint/hunt movement immediately.
         * Food behavior can move the predator normally after this.
         */
        killer.setSprinting(false);
        killer.getNavigation().stop();
    }

    private static PathfinderMob findResponsibleKiller(
            LivingDeathEvent event,
            LivingEntity killed
    ) {
        Entity sourceEntity = event.getSource().getEntity();

        if (sourceEntity instanceof PathfinderMob mob) {
            return mob;
        }

        if (killed.getLastHurtByMob() instanceof PathfinderMob mob) {
            return mob;
        }

        return null;
    }

    private static boolean shouldThink(
            PathfinderMob hunter,
            long gameTime
    ) {
        int offset = Math.floorMod(
                hunter.getId(),
                HUNT_THINK_INTERVAL_TICKS
        );

        return (gameTime + offset) % HUNT_THINK_INTERVAL_TICKS == 0L;
    }

    private static boolean shouldStartHunt(
            ServerLevel level,
            PathfinderMob hunter,
            long gameTime
    ) {
        if (hunter == null || level == null) {
            return false;
        }

        if (!hunter.isAlive() || hunter.isRemoved()) {
            return false;
        }

        if (hunter instanceof TamableAnimal tamableAnimal && tamableAnimal.isTame()) {
            return false;
        }

        if (RetoldAiControl.isControlled(hunter)) {
            return false;
        }

        if (hunter.getTarget() != null && hunter.getTarget().isAlive()) {
            return false;
        }

        RetoldMobState state = RetoldMobStates.getOrCreate(
                hunter,
                gameTime
        );

        if (state.hunger() < RetoldMobRules.huntThreshold(hunter)) {
            return false;
        }

        return !hasEasyFoodNearby(
                level,
                hunter
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
                        && target.isAlive()
                        && RetoldMobRules.canHuntPrey(hunter, target, gameTime)
                        && hunter.distanceToSqr(target) <= HUNT_ABANDON_DISTANCE_SQUARED
        ) {
            return false;
        }

        HuntMemory memory = getActiveHuntMemory(
                hunter,
                gameTime
        );

        if (memory == null) {
            return true;
        }

        Entity rememberedEntity = level.getEntity(memory.preyUuid());

        if (!(rememberedEntity instanceof LivingEntity rememberedPrey)) {
            return true;
        }

        if (
                !rememberedPrey.isAlive()
                        || rememberedPrey.isRemoved()
                        || !RetoldMobRules.canHuntPrey(hunter, rememberedPrey, gameTime)
                        || hunter.distanceToSqr(rememberedPrey) > HUNT_ABANDON_DISTANCE_SQUARED
        ) {
            return true;
        }

        return false;
    }

    private static boolean hasEasyFoodNearby(
            ServerLevel level,
            PathfinderMob hunter
    ) {
        List<ItemEntity> items = level.getEntitiesOfClass(
                ItemEntity.class,
                hunter.getBoundingBox().inflate(EASY_FOOD_RADIUS_BLOCKS),
                item -> isEasyFood(hunter, item)
        );

        return !items.isEmpty();
    }

    private static boolean isEasyFood(
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

        if (!hunter.hasLineOfSight(item) && hunter.distanceToSqr(item) > 16.0D) {
            return false;
        }

        return RetoldMobRules.canEatDroppedItem(
                hunter,
                item.getItem()
        );
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
                prey -> isValidPrey(
                        hunter,
                        prey,
                        gameTime
                )
        );

        LivingEntity bestPrey = null;
        double bestScore = Double.MAX_VALUE;

        for (LivingEntity prey : candidates) {
            double distanceSquared = hunter.distanceToSqr(prey);

            if (distanceSquared > PREY_SEARCH_RADIUS_SQUARED) {
                continue;
            }

            double score = distanceSquared;

            if (hunter.hasLineOfSight(prey)) {
                score -= 24.0D;
            }

            if (RetoldMobRules.isSmallFoodPrey(prey)) {
                score -= 12.0D;
            }

            if (score < bestScore) {
                bestScore = score;
                bestPrey = prey;
            }
        }

        return bestPrey;
    }

    private static boolean isValidPrey(
            PathfinderMob hunter,
            LivingEntity prey,
            long gameTime
    ) {
        if (hunter == null || prey == null) {
            return false;
        }

        if (hunter == prey) {
            return false;
        }

        if (!prey.isAlive() || prey.isRemoved()) {
            return false;
        }

        if (prey instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return false;
        }

        if (hunter.distanceToSqr(prey) > PREY_SEARCH_RADIUS_SQUARED) {
            return false;
        }

        if (!RetoldMobRules.canHuntPrey(hunter, prey, gameTime)) {
            return false;
        }

        return canDirectlySensePrey(
                hunter,
                prey
        );
    }

    private static boolean canDirectlySensePrey(
            PathfinderMob hunter,
            LivingEntity prey
    ) {
        double distanceSquared = hunter.distanceToSqr(prey);

        if (
                distanceSquared <= PREY_SIGHT_RADIUS_SQUARED
                        && hunter.hasLineOfSight(prey)
        ) {
            return true;
        }

        if (
                distanceSquared <= PREY_HEARING_RADIUS_SQUARED
                        && isAudiblePrey(prey)
        ) {
            return true;
        }

        return distanceSquared <= PREY_SMELL_RADIUS_SQUARED;
    }

    private static boolean canFollowTrailToPrey(
            PathfinderMob hunter,
            LivingEntity prey,
            HuntMemory memory,
            long gameTime
    ) {
        if (hunter == null || prey == null || memory == null) {
            return false;
        }

        if (memory.isExpired(gameTime)) {
            return false;
        }

        if (hunter.distanceToSqr(prey) <= TRAIL_DIRECT_SMELL_RADIUS_SQUARED) {
            return true;
        }

        return hunter.blockPosition().distSqr(memory.lastKnownPos()) <= TRAIL_REACQUIRE_RADIUS_SQUARED
                && prey.blockPosition().distSqr(memory.lastKnownPos()) <= TRAIL_REACQUIRE_RADIUS_SQUARED;
    }

    private static boolean isAudiblePrey(LivingEntity prey) {
        Vec3 movement = prey.getDeltaMovement();
        double horizontalMovementSquared = movement.x * movement.x + movement.z * movement.z;

        return horizontalMovementSquared >= PREY_MOVEMENT_HEARING_THRESHOLD_SQUARED;
    }

    private static void beginHunt(
            PathfinderMob hunter,
            LivingEntity prey,
            long gameTime
    ) {
        rememberPrey(
                hunter,
                prey,
                gameTime,
                0
        );

        RetoldAiControl.claim(
                hunter,
                RetoldAiControlMode.HUNT,
                gameTime,
                HUNT_CONTROL_TICKS
        );

        hunter.setSprinting(true);

        RetoldFactionTargetGuards.setTargetIgnoringGuard(
                hunter,
                prey
        );

        RetoldFactionTargetGuards.setAggressiveIgnoringGuard(
                hunter,
                true
        );

        hunter.getLookControl().setLookAt(
                prey,
                30.0F,
                30.0F
        );

        RetoldAiControl.withNavigationBypass(() -> {
            hunter.getNavigation().moveTo(
                    prey,
                    getHuntSpeed(hunter, prey, gameTime)
            );
        });
    }

    private static void continueActiveHunt(
            ServerLevel level,
            PathfinderMob hunter,
            long gameTime
    ) {
        LivingEntity prey = getRememberedOrCurrentPrey(
                level,
                hunter,
                gameTime
        );

        if (prey == null) {
            stopHunt(hunter);
            return;
        }

        if (hunter.distanceToSqr(prey) > HUNT_ABANDON_DISTANCE_SQUARED) {
            stopHunt(hunter);
            return;
        }

        /*
         * Food-first rule during active hunt:
         * if edible dropped food is nearby, stop chasing and let FEED take over.
         */
        if (hasEasyFoodNearby(level, hunter)) {
            stopHuntForFood(
                    hunter,
                    gameTime
            );
            return;
        }

        HuntMemory memory = getActiveHuntMemory(
                hunter,
                gameTime
        );

        boolean directSense = canDirectlySensePrey(
                hunter,
                prey
        );

        boolean trailSense = !directSense
                && canFollowTrailToPrey(
                hunter,
                prey,
                memory,
                gameTime
        );

        if (directSense || trailSense) {
            rememberPrey(
                    hunter,
                    prey,
                    gameTime,
                    0
            );
        }

        memory = getActiveHuntMemory(
                hunter,
                gameTime
        );

        if (!directSense && !trailSense && memory == null) {
            stopHunt(hunter);
            return;
        }

        RetoldAiControl.refresh(
                hunter,
                RetoldAiControlMode.HUNT,
                gameTime,
                HUNT_CONTROL_TICKS
        );

        hunter.setSprinting(true);

        if (hunter.getTarget() != prey) {
            RetoldFactionTargetGuards.setTargetIgnoringGuard(
                    hunter,
                    prey
            );
        }

        RetoldFactionTargetGuards.setAggressiveIgnoringGuard(
                hunter,
                true
        );

        if (directSense || trailSense) {
            if (
                    RetoldPredatorStrike.tryStrike(
                            level,
                            hunter,
                            prey,
                            gameTime
                    )
            ) {
                return;
            }

            hunter.getLookControl().setLookAt(
                    prey,
                    30.0F,
                    30.0F
            );

            RetoldAiControl.withNavigationBypass(() -> {
                hunter.getNavigation().moveTo(
                        prey,
                        getHuntSpeed(hunter, prey, gameTime)
                );
            });

            return;
        }

        BlockPos searchPos = getSearchPosition(
                hunter,
                memory
        );

        if (hunter.blockPosition().distSqr(searchPos) <= SEARCH_POINT_REACHED_DISTANCE_SQUARED) {
            memory = memory.nextSearchStep(gameTime + HUNT_TRAIL_MEMORY_TICKS);

            HUNT_MEMORIES.put(
                    hunter,
                    memory
            );

            searchPos = getSearchPosition(
                    hunter,
                    memory
            );
        }

        hunter.getLookControl().setLookAt(
                searchPos.getX() + 0.5D,
                searchPos.getY() + 0.5D,
                searchPos.getZ() + 0.5D
        );

        BlockPos finalSearchPos = searchPos;

        RetoldAiControl.withNavigationBypass(() -> {
            hunter.getNavigation().moveTo(
                    finalSearchPos.getX() + 0.5D,
                    finalSearchPos.getY(),
                    finalSearchPos.getZ() + 0.5D,
                    getTrackingSpeed(hunter, gameTime)
            );
        });
    }

    private static LivingEntity getRememberedOrCurrentPrey(
            ServerLevel level,
            PathfinderMob hunter,
            long gameTime
    ) {
        LivingEntity target = hunter.getTarget();

        if (
                target != null
                        && target.isAlive()
                        && RetoldMobRules.canHuntPrey(hunter, target, gameTime)
        ) {
            return target;
        }

        HuntMemory memory = getActiveHuntMemory(
                hunter,
                gameTime
        );

        if (memory == null) {
            return null;
        }

        Entity entity = level.getEntity(memory.preyUuid());

        if (!(entity instanceof LivingEntity rememberedPrey)) {
            return null;
        }

        if (
                !rememberedPrey.isAlive()
                        || rememberedPrey.isRemoved()
                        || !RetoldMobRules.canHuntPrey(hunter, rememberedPrey, gameTime)
        ) {
            return null;
        }

        return rememberedPrey;
    }

    private static void rememberPrey(
            PathfinderMob hunter,
            LivingEntity prey,
            long gameTime,
            int searchStep
    ) {
        Vec3 movement = prey.getDeltaMovement();

        HUNT_MEMORIES.put(
                hunter,
                new HuntMemory(
                        prey.getUUID(),
                        prey.blockPosition().immutable(),
                        new Vec3(
                                movement.x,
                                0.0D,
                                movement.z
                        ),
                        gameTime,
                        gameTime + HUNT_TRAIL_MEMORY_TICKS,
                        searchStep
                )
        );
    }

    private static HuntMemory getActiveHuntMemory(
            PathfinderMob hunter,
            long gameTime
    ) {
        HuntMemory memory = HUNT_MEMORIES.get(hunter);

        if (memory == null) {
            return null;
        }

        if (memory.isExpired(gameTime)) {
            HUNT_MEMORIES.remove(hunter);
            return null;
        }

        return memory;
    }

    private static BlockPos getSearchPosition(
            PathfinderMob hunter,
            HuntMemory memory
    ) {
        Vec3 movement = memory.lastKnownMovement();

        Vec3 direction;

        if (movement.lengthSqr() > 0.0001D) {
            direction = movement.normalize();
        } else {
            Vec3 fromHunterToTrail = new Vec3(
                    memory.lastKnownPos().getX() + 0.5D - hunter.getX(),
                    0.0D,
                    memory.lastKnownPos().getZ() + 0.5D - hunter.getZ()
            );

            if (fromHunterToTrail.lengthSqr() <= 0.0001D) {
                double angle = hunter.getRandom().nextDouble() * Math.PI * 2.0D;

                direction = new Vec3(
                        Math.cos(angle),
                        0.0D,
                        Math.sin(angle)
                );
            } else {
                direction = fromHunterToTrail.normalize();
            }
        }

        Vec3 side = new Vec3(
                -direction.z,
                0.0D,
                direction.x
        );

        int step = Math.max(
                0,
                memory.searchStep()
        );

        double forwardDistance = 4.0D + Math.min(
                12.0D,
                step * 3.0D
        );

        double sideDistance = switch (step % 4) {
            case 1 -> 3.5D;
            case 2 -> -3.5D;
            case 3 -> 6.0D;
            default -> 0.0D;
        };

        Vec3 predicted = new Vec3(
                memory.lastKnownPos().getX() + 0.5D,
                memory.lastKnownPos().getY(),
                memory.lastKnownPos().getZ() + 0.5D
        )
                .add(direction.scale(forwardDistance))
                .add(side.scale(sideDistance));

        return new BlockPos(
                (int) Math.floor(predicted.x),
                memory.lastKnownPos().getY(),
                (int) Math.floor(predicted.z)
        ).immutable();
    }

    private static void clearHuntMemory(PathfinderMob hunter) {
        if (hunter == null) {
            return;
        }

        HUNT_MEMORIES.remove(hunter);
    }

    private static void stopHunt(PathfinderMob hunter) {
        clearHuntMemory(hunter);
        RetoldPredatorStrike.clear(hunter);

        RetoldAiControl.clear(hunter);

        RetoldFactionTargetGuards.setTargetIgnoringGuard(
                hunter,
                null
        );

        RetoldFactionTargetGuards.setAggressiveIgnoringGuard(
                hunter,
                false
        );

        hunter.setSprinting(false);
        hunter.getNavigation().stop();
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

        RetoldFactionTargetGuards.setTargetIgnoringGuard(
                hunter,
                null
        );

        RetoldFactionTargetGuards.setAggressiveIgnoringGuard(
                hunter,
                false
        );

        hunter.setSprinting(false);
        hunter.getNavigation().stop();
    }

    private static double getTrackingSpeed(
            PathfinderMob hunter,
            long gameTime
    ) {
        return clamp(
                getBaseHuntSpeed(hunter) * 0.96D * RetoldPredatorStrike.chaseSpeedMultiplier(hunter, gameTime),
                MIN_HUNT_SPEED,
                MAX_HUNT_SPEED
        );
    }

    private static double getHuntSpeed(
            PathfinderMob hunter,
            LivingEntity prey,
            long gameTime
    ) {
        double baseSpeed = getBaseHuntSpeed(hunter);

        double distanceSquared = prey == null
                ? FAR_CHASE_DISTANCE_SQUARED
                : hunter.distanceToSqr(prey);

        double modifier = 1.0D;

        if (distanceSquared > FAR_CHASE_DISTANCE_SQUARED) {
            modifier -= 0.04D;
        }

        double roll = hunter.getRandom().nextDouble();

        if (distanceSquared <= CLOSE_CHASE_DISTANCE_SQUARED && roll < 0.42D) {
            modifier += 0.18D + hunter.getRandom().nextDouble() * 0.12D;
        }

        if (roll >= 0.42D && roll < 0.56D) {
            modifier -= 0.08D + hunter.getRandom().nextDouble() * 0.10D;
        }

        modifier *= RetoldPredatorStrike.chaseSpeedMultiplier(
                hunter,
                gameTime
        );

        return clamp(
                baseSpeed * modifier,
                MIN_HUNT_SPEED,
                MAX_HUNT_SPEED
        );
    }

    private static double getBaseHuntSpeed(PathfinderMob hunter) {
        String hunterPath = RetoldMobRules.getEntityTypePath(
                hunter.getType()
        );

        if (
                hunterPath.equals("cat")
                        || hunterPath.equals("ocelot")
        ) {
            return CAT_HUNT_BASE_SPEED;
        }

        if (hunterPath.equals("fox")) {
            return FOX_HUNT_BASE_SPEED;
        }

        if (
                hunterPath.equals("spider")
                        || hunterPath.equals("cave_spider")
        ) {
            return SPIDER_HUNT_BASE_SPEED;
        }

        if (hunterPath.equals("dolphin")) {
            return DOLPHIN_HUNT_BASE_SPEED;
        }

        return DEFAULT_HUNT_BASE_SPEED;
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

    private record HuntMemory(
            UUID preyUuid,
            BlockPos lastKnownPos,
            Vec3 lastKnownMovement,
            long lastSeenAt,
            long expiresAt,
            int searchStep
    ) {
        public boolean isExpired(long gameTime) {
            return gameTime > expiresAt;
        }

        public HuntMemory nextSearchStep(long newExpiresAt) {
            return new HuntMemory(
                    preyUuid,
                    lastKnownPos,
                    lastKnownMovement,
                    lastSeenAt,
                    newExpiresAt,
                    searchStep + 1
            );
        }
    }
}