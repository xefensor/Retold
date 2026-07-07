package cz.xefensor.retold.behavior;

import cz.xefensor.retold.combat.RetoldFactionTargetGuards;
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

public final class RetoldControlledHuntingEvents {
    private static final int HUNT_THINK_INTERVAL_TICKS = 20;
    private static final int HUNT_CONTROL_TICKS = 20 * 4;
    private static final int FEED_LOCK_AFTER_KILL_TICKS = 20 * 8;

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

    private static final double EASY_FOOD_RADIUS_BLOCKS = 8.0D;
    private static final double EASY_FOOD_RADIUS_SQUARED =
            EASY_FOOD_RADIUS_BLOCKS * EASY_FOOD_RADIUS_BLOCKS;

    private static final double PREY_MOVEMENT_HEARING_THRESHOLD_SQUARED = 0.0016D;

    private static final double DEFAULT_HUNT_SPEED = 1.0D;
    private static final double CAT_HUNT_SPEED = 1.05D;
    private static final double SPIDER_HUNT_SPEED = 0.95D;
    private static final double DOLPHIN_HUNT_SPEED = 1.15D;

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

        if (!shouldThink(hunter, gameTime)) {
            return;
        }

        if (RetoldAiControl.isControlledAs(hunter, RetoldAiControlMode.HUNT)) {
            continueActiveHunt(
                    hunter,
                    gameTime
            );
            return;
        }

        if (!shouldStartHunt(level, hunter, gameTime)) {
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
         * Important:
         * kill does NOT reduce hunger.
         * The kill only forces a feeding phase.
         */
        RetoldAiControl.claim(
                killer,
                RetoldAiControlMode.FEED,
                level.getGameTime(),
                FEED_LOCK_AFTER_KILL_TICKS
        );

        RetoldFactionTargetGuards.setTargetIgnoringGuard(
                killer,
                null
        );

        RetoldFactionTargetGuards.setAggressiveIgnoringGuard(
                killer,
                false
        );

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

        /*
         * Tamed predators do not autonomously hunt.
         * Retold owner-defense will be rebuilt later as a separate combat layer.
         */
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

        /*
         * Food-first rule:
         * if there is edible food nearby, do not hunt.
         */
        return !hasEasyFoodNearby(
                level,
                hunter
        );
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

        return canSensePrey(
                hunter,
                prey
        );
    }

    private static boolean canSensePrey(
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
        RetoldAiControl.claim(
                hunter,
                RetoldAiControlMode.HUNT,
                gameTime,
                HUNT_CONTROL_TICKS
        );

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
                    getHuntSpeed(hunter)
            );
        });
    }

    private static void continueActiveHunt(
            PathfinderMob hunter,
            long gameTime
    ) {
        LivingEntity prey = hunter.getTarget();

        if (
                prey == null
                        || !prey.isAlive()
                        || !RetoldMobRules.canHuntPrey(hunter, prey, gameTime)
        ) {
            RetoldAiControl.clear(hunter);

            RetoldFactionTargetGuards.setTargetIgnoringGuard(
                    hunter,
                    null
            );

            RetoldFactionTargetGuards.setAggressiveIgnoringGuard(
                    hunter,
                    false
            );

            hunter.getNavigation().stop();
            return;
        }

        RetoldAiControl.refresh(
                hunter,
                RetoldAiControlMode.HUNT,
                gameTime,
                HUNT_CONTROL_TICKS
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
                    getHuntSpeed(hunter)
            );
        });
    }

    private static double getHuntSpeed(PathfinderMob hunter) {
        String hunterPath = RetoldMobRules.getEntityTypePath(
                hunter.getType()
        );

        if (
                hunterPath.equals("cat")
                        || hunterPath.equals("ocelot")
                        || hunterPath.equals("fox")
        ) {
            return CAT_HUNT_SPEED;
        }

        if (
                hunterPath.equals("spider")
                        || hunterPath.equals("cave_spider")
        ) {
            return SPIDER_HUNT_SPEED;
        }

        if (hunterPath.equals("dolphin")) {
            return DOLPHIN_HUNT_SPEED;
        }

        return DEFAULT_HUNT_SPEED;
    }
}