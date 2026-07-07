package cz.xefensor.retold.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class RetoldControlledFleeEvents {
    private static final Map<PathfinderMob, FleeMemory> FLEE_MEMORIES = new WeakHashMap<>();

    private static final int FLEE_THINK_INTERVAL_TICKS = 8;
    private static final int FLEE_CONTROL_TICKS = 20 * 3;

    /*
     * New:
     * prey keeps fleeing even after it loses direct sight/hearing/smell of the hunter.
     */
    private static final int FLEE_MEMORY_TICKS = 20 * 10;

    private static final double ACTIVE_THREAT_RADIUS_BLOCKS = 22.0D;
    private static final double ACTIVE_THREAT_RADIUS_SQUARED =
            ACTIVE_THREAT_RADIUS_BLOCKS * ACTIVE_THREAT_RADIUS_BLOCKS;

    private static final double WARNING_THREAT_RADIUS_BLOCKS = 9.0D;
    private static final double WARNING_THREAT_RADIUS_SQUARED =
            WARNING_THREAT_RADIUS_BLOCKS * WARNING_THREAT_RADIUS_BLOCKS;

    private static final double BASE_FLEE_DISTANCE_BLOCKS = 13.0D;
    private static final double CLOSE_FLEE_DISTANCE_BLOCKS = 15.0D;
    private static final double FAR_FLEE_DISTANCE_BLOCKS = 10.0D;

    /*
     * Used when the hunter is no longer directly sensed but the prey is still scared.
     */
    private static final double MEMORY_FLEE_DISTANCE_BLOCKS = 12.0D;

    private static final double BASE_FLEE_SPEED = 1.16D;
    private static final double MEMORY_FLEE_SPEED = 1.10D;
    private static final double MIN_FLEE_SPEED = 0.86D;
    private static final double MAX_FLEE_SPEED = 1.46D;

    private static final double CLOSE_THREAT_DISTANCE_BLOCKS = 5.0D;
    private static final double CLOSE_THREAT_DISTANCE_SQUARED =
            CLOSE_THREAT_DISTANCE_BLOCKS * CLOSE_THREAT_DISTANCE_BLOCKS;

    private static final double FAR_THREAT_DISTANCE_BLOCKS = 14.0D;
    private static final double FAR_THREAT_DISTANCE_SQUARED =
            FAR_THREAT_DISTANCE_BLOCKS * FAR_THREAT_DISTANCE_BLOCKS;

    private static final double SIGHT_RADIUS_BLOCKS = 18.0D;
    private static final double SIGHT_RADIUS_SQUARED =
            SIGHT_RADIUS_BLOCKS * SIGHT_RADIUS_BLOCKS;

    private static final double HEARING_RADIUS_BLOCKS = 7.0D;
    private static final double HEARING_RADIUS_SQUARED =
            HEARING_RADIUS_BLOCKS * HEARING_RADIUS_BLOCKS;

    private static final double SMELL_RADIUS_BLOCKS = 5.0D;
    private static final double SMELL_RADIUS_SQUARED =
            SMELL_RADIUS_BLOCKS * SMELL_RADIUS_BLOCKS;

    private static final double AUDIBLE_MOVEMENT_THRESHOLD_SQUARED = 0.0016D;

    private RetoldControlledFleeEvents() {
    }

    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof PathfinderMob prey)) {
            return;
        }

        if (!(prey.level() instanceof ServerLevel level)) {
            return;
        }

        if (!isFleeingPrey(prey)) {
            FLEE_MEMORIES.remove(prey);
            return;
        }

        long gameTime = level.getGameTime();

        if (!shouldThink(prey, gameTime)) {
            return;
        }

        LivingEntity threat = findBestThreat(
                level,
                prey,
                gameTime
        );

        if (threat != null) {
            rememberThreat(
                    prey,
                    threat,
                    gameTime
            );

            fleeFromThreat(
                    prey,
                    threat,
                    gameTime
            );
            return;
        }

        /*
         * New:
         * If the prey lost the hunter, it still keeps running from the remembered
         * danger direction for a while.
         */
        FleeMemory memory = getActiveFleeMemory(
                prey,
                gameTime
        );

        if (memory != null) {
            fleeFromMemory(
                    prey,
                    memory,
                    gameTime
            );
            return;
        }

        if (RetoldAiControl.isControlledAs(prey, RetoldAiControlMode.FLEE)) {
            RetoldAiControl.clear(prey);
            prey.setSprinting(false);
            prey.getNavigation().stop();
        }
    }

    private static boolean shouldThink(
            PathfinderMob prey,
            long gameTime
    ) {
        int offset = Math.floorMod(
                prey.getId(),
                FLEE_THINK_INTERVAL_TICKS
        );

        return (gameTime + offset) % FLEE_THINK_INTERVAL_TICKS == 0L;
    }

    private static LivingEntity findBestThreat(
            ServerLevel level,
            PathfinderMob prey,
            long gameTime
    ) {
        AABB area = prey.getBoundingBox().inflate(ACTIVE_THREAT_RADIUS_BLOCKS);

        List<LivingEntity> candidates = level.getEntitiesOfClass(
                LivingEntity.class,
                area,
                candidate -> isValidThreat(
                        prey,
                        candidate,
                        gameTime
                )
        );

        LivingEntity bestThreat = null;
        double bestScore = Double.MAX_VALUE;

        for (LivingEntity candidate : candidates) {
            double distanceSquared = prey.distanceToSqr(candidate);

            if (distanceSquared > ACTIVE_THREAT_RADIUS_SQUARED) {
                continue;
            }

            double score = distanceSquared;

            if (candidate instanceof PathfinderMob threatMob) {
                if (threatMob.getTarget() == prey) {
                    score -= 80.0D;
                }

                if (RetoldAiControl.isControlledAs(threatMob, RetoldAiControlMode.HUNT)) {
                    score -= 60.0D;
                }

                if (RetoldAiControl.isControlledAs(threatMob, RetoldAiControlMode.ATTACK)) {
                    score -= 40.0D;
                }

                if (RetoldAiControl.isControlledAs(threatMob, RetoldAiControlMode.SEARCH)) {
                    score -= 14.0D;
                }
            }

            if (prey.hasLineOfSight(candidate)) {
                score -= 16.0D;
            }

            if (score < bestScore) {
                bestScore = score;
                bestThreat = candidate;
            }
        }

        return bestThreat;
    }

    private static boolean isValidThreat(
            PathfinderMob prey,
            LivingEntity candidate,
            long gameTime
    ) {
        if (prey == null || candidate == null) {
            return false;
        }

        if (prey == candidate) {
            return false;
        }

        if (!candidate.isAlive() || candidate.isRemoved()) {
            return false;
        }

        if (candidate instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return false;
        }

        if (prey.level() != candidate.level()) {
            return false;
        }

        if (prey.distanceToSqr(candidate) > ACTIVE_THREAT_RADIUS_SQUARED) {
            return false;
        }

        if (!(candidate instanceof PathfinderMob threatMob)) {
            return false;
        }

        if (!RetoldMobRules.isManagedPredator(threatMob)) {
            return false;
        }

        if (!RetoldMobRules.canHuntPrey(threatMob, prey, gameTime)) {
            return false;
        }

        if (threatMob.getTarget() == prey) {
            return canSenseThreat(prey, threatMob);
        }

        if (RetoldAiControl.isControlledAs(threatMob, RetoldAiControlMode.HUNT)) {
            return canSenseThreat(prey, threatMob);
        }

        if (RetoldAiControl.isControlledAs(threatMob, RetoldAiControlMode.ATTACK)) {
            return canSenseThreat(prey, threatMob);
        }

        /*
         * Warning flee:
         * prey can flee a close hungry predator before the hunt starts.
         */
        if (prey.distanceToSqr(threatMob) > WARNING_THREAT_RADIUS_SQUARED) {
            return false;
        }

        RetoldMobState predatorState = RetoldMobStates.get(threatMob);

        if (predatorState == null) {
            return false;
        }

        if (predatorState.hunger() < RetoldMobRules.huntThreshold(threatMob)) {
            return false;
        }

        return canSenseThreat(prey, threatMob);
    }

    private static boolean canSenseThreat(
            PathfinderMob prey,
            LivingEntity threat
    ) {
        double distanceSquared = prey.distanceToSqr(threat);

        if (
                distanceSquared <= SIGHT_RADIUS_SQUARED
                        && prey.hasLineOfSight(threat)
        ) {
            return true;
        }

        if (
                distanceSquared <= HEARING_RADIUS_SQUARED
                        && isAudible(threat)
        ) {
            return true;
        }

        return distanceSquared <= SMELL_RADIUS_SQUARED;
    }

    private static boolean isAudible(LivingEntity entity) {
        Vec3 movement = entity.getDeltaMovement();
        double horizontalMovementSquared = movement.x * movement.x + movement.z * movement.z;

        if (horizontalMovementSquared >= AUDIBLE_MOVEMENT_THRESHOLD_SQUARED) {
            return true;
        }

        return entity instanceof Mob mob
                && mob.getTarget() != null;
    }

    private static void rememberThreat(
            PathfinderMob prey,
            LivingEntity threat,
            long gameTime
    ) {
        Vec3 away = new Vec3(
                prey.getX() - threat.getX(),
                0.0D,
                prey.getZ() - threat.getZ()
        );

        if (away.lengthSqr() <= 0.0001D) {
            away = randomHorizontalDirection(prey);
        } else {
            away = away.normalize();
        }

        FLEE_MEMORIES.put(
                prey,
                new FleeMemory(
                        threat.blockPosition().immutable(),
                        away,
                        gameTime,
                        gameTime + FLEE_MEMORY_TICKS
                )
        );
    }

    private static FleeMemory getActiveFleeMemory(
            PathfinderMob prey,
            long gameTime
    ) {
        FleeMemory memory = FLEE_MEMORIES.get(prey);

        if (memory == null) {
            return null;
        }

        if (memory.isExpired(gameTime)) {
            FLEE_MEMORIES.remove(prey);
            return null;
        }

        return memory;
    }

    private static void fleeFromThreat(
            PathfinderMob prey,
            LivingEntity threat,
            long gameTime
    ) {
        Vec3 away = new Vec3(
                prey.getX() - threat.getX(),
                0.0D,
                prey.getZ() - threat.getZ()
        );

        if (away.lengthSqr() <= 0.0001D) {
            away = randomHorizontalDirection(prey);
        } else {
            away = away.normalize();
        }

        double distanceSquared = prey.distanceToSqr(threat);
        double fleeDistance = getFleeDistance(distanceSquared);

        moveInFleeDirection(
                prey,
                away,
                fleeDistance,
                getFleeSpeed(prey, threat),
                gameTime
        );
    }

    private static void fleeFromMemory(
            PathfinderMob prey,
            FleeMemory memory,
            long gameTime
    ) {
        Vec3 away = memory.awayDirection();

        if (away == null || away.lengthSqr() <= 0.0001D) {
            away = randomHorizontalDirection(prey);
        } else {
            away = away.normalize();
        }

        /*
         * Small direction drift while scared.
         * This prevents all prey from running in a perfect straight line forever,
         * but still keeps the main direction away from the remembered threat.
         */
        Vec3 side = new Vec3(
                -away.z,
                0.0D,
                away.x
        );

        double sideDrift = (prey.getRandom().nextDouble() - 0.5D) * 0.38D;

        Vec3 rememberedDirection = away
                .add(side.scale(sideDrift));

        if (rememberedDirection.lengthSqr() <= 0.0001D) {
            rememberedDirection = away;
        } else {
            rememberedDirection = rememberedDirection.normalize();
        }

        moveInFleeDirection(
                prey,
                rememberedDirection,
                MEMORY_FLEE_DISTANCE_BLOCKS,
                getMemoryFleeSpeed(prey),
                gameTime
        );
    }

    private static void moveInFleeDirection(
            PathfinderMob prey,
            Vec3 direction,
            double fleeDistance,
            double speed,
            long gameTime
    ) {
        Vec3 safeDirection = direction;

        if (safeDirection == null || safeDirection.lengthSqr() <= 0.0001D) {
            safeDirection = randomHorizontalDirection(prey);
        } else {
            safeDirection = safeDirection.normalize();
        }

        Vec3 side = new Vec3(
                -safeDirection.z,
                0.0D,
                safeDirection.x
        );

        double sideOffset = (prey.getRandom().nextDouble() - 0.5D) * 4.5D;

        Vec3 target = prey.position()
                .add(safeDirection.scale(fleeDistance))
                .add(side.scale(sideOffset));

        BlockPos targetPos = new BlockPos(
                (int) Math.floor(target.x),
                prey.blockPosition().getY(),
                (int) Math.floor(target.z)
        );

        RetoldAiControl.refresh(
                prey,
                RetoldAiControlMode.FLEE,
                gameTime,
                FLEE_CONTROL_TICKS
        );

        prey.setSprinting(true);

        RetoldAiControl.withNavigationBypass(() -> {
            prey.getNavigation().moveTo(
                    targetPos.getX() + 0.5D,
                    targetPos.getY(),
                    targetPos.getZ() + 0.5D,
                    speed
            );
        });
    }

    private static double getFleeDistance(double distanceSquared) {
        if (distanceSquared <= CLOSE_THREAT_DISTANCE_SQUARED) {
            return CLOSE_FLEE_DISTANCE_BLOCKS;
        }

        if (distanceSquared > FAR_THREAT_DISTANCE_SQUARED) {
            return FAR_FLEE_DISTANCE_BLOCKS;
        }

        return BASE_FLEE_DISTANCE_BLOCKS;
    }

    private static double getFleeSpeed(
            PathfinderMob prey,
            LivingEntity threat
    ) {
        double distanceSquared = prey.distanceToSqr(threat);

        double modifier = 1.0D;

        if (distanceSquared <= CLOSE_THREAT_DISTANCE_SQUARED) {
            modifier += 0.12D + prey.getRandom().nextDouble() * 0.13D;
        }

        if (distanceSquared > FAR_THREAT_DISTANCE_SQUARED) {
            modifier -= 0.12D + prey.getRandom().nextDouble() * 0.06D;
        }

        double roll = prey.getRandom().nextDouble();

        if (roll < 0.20D) {
            modifier -= 0.10D + prey.getRandom().nextDouble() * 0.14D;
        }

        if (roll >= 0.20D && roll < 0.36D) {
            modifier += 0.08D + prey.getRandom().nextDouble() * 0.10D;
        }

        return clamp(
                BASE_FLEE_SPEED * modifier,
                MIN_FLEE_SPEED,
                MAX_FLEE_SPEED
        );
    }

    private static double getMemoryFleeSpeed(PathfinderMob prey) {
        double modifier = 1.0D;

        double roll = prey.getRandom().nextDouble();

        /*
         * Fear-running is slightly less intense than direct panic,
         * but still fast enough that the animal does not stop right after breaking sight.
         */
        if (roll < 0.18D) {
            modifier -= 0.08D + prey.getRandom().nextDouble() * 0.10D;
        }

        if (roll >= 0.18D && roll < 0.34D) {
            modifier += 0.06D + prey.getRandom().nextDouble() * 0.08D;
        }

        return clamp(
                MEMORY_FLEE_SPEED * modifier,
                MIN_FLEE_SPEED,
                MAX_FLEE_SPEED
        );
    }

    private static Vec3 randomHorizontalDirection(PathfinderMob mob) {
        double angle = mob.getRandom().nextDouble() * Math.PI * 2.0D;

        return new Vec3(
                Math.cos(angle),
                0.0D,
                Math.sin(angle)
        );
    }

    private static boolean isFleeingPrey(PathfinderMob mob) {
        String path = RetoldMobRules.getEntityTypePath(
                mob.getType()
        );

        if (path.equals("villager")) {
            return false;
        }

        return path.equals("cow")
                || path.equals("sheep")
                || path.equals("pig")
                || path.equals("chicken")
                || path.equals("rabbit")
                || path.equals("goat")
                || path.equals("horse")
                || path.equals("donkey")
                || path.equals("mule")
                || path.equals("llama")
                || path.equals("trader_llama")
                || path.equals("camel")
                || path.equals("cod")
                || path.equals("salmon")
                || path.equals("tropical_fish")
                || path.equals("pufferfish");
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

    private record FleeMemory(
            BlockPos lastThreatPos,
            Vec3 awayDirection,
            long lastThreatSeenAt,
            long expiresAt
    ) {
        public boolean isExpired(long gameTime) {
            return gameTime > expiresAt;
        }
    }
}