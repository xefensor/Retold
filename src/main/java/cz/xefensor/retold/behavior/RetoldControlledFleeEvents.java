package cz.xefensor.retold.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
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
     * Direct predator fear memory.
     */
    private static final int FLEE_MEMORY_TICKS = 20 * 10;

    /*
     * Herd panic memory is shorter than direct predator memory.
     * This is copied fear from another prey animal.
     */
    private static final int HERD_PANIC_MEMORY_TICKS = 20 * 7;

    private static final double ACTIVE_THREAT_RADIUS_BLOCKS = 22.0D;
    private static final double ACTIVE_THREAT_RADIUS_SQUARED =
            ACTIVE_THREAT_RADIUS_BLOCKS * ACTIVE_THREAT_RADIUS_BLOCKS;

    private static final double WARNING_THREAT_RADIUS_BLOCKS = 9.0D;
    private static final double WARNING_THREAT_RADIUS_SQUARED =
            WARNING_THREAT_RADIUS_BLOCKS * WARNING_THREAT_RADIUS_BLOCKS;
    private static final double FEAR_WARNING_RADIUS_BONUS_BLOCKS = 4.0D;

    private static final double BASE_FLEE_DISTANCE_BLOCKS = 16.0D;
    private static final double CLOSE_FLEE_DISTANCE_BLOCKS = 19.0D;
    private static final double FAR_FLEE_DISTANCE_BLOCKS = 13.0D;
    private static final double FEAR_FLEE_DISTANCE_BONUS_BLOCKS = 3.0D;

    private static final double MEMORY_FLEE_DISTANCE_BLOCKS = 15.0D;
    private static final double HERD_PANIC_FLEE_DISTANCE_BLOCKS = 13.5D;

    private static final double BASE_FLEE_SPEED = 1.26D;
    private static final double MEMORY_FLEE_SPEED = 1.17D;
    private static final double HERD_PANIC_FLEE_SPEED = 1.13D;
    private static final double MIN_FLEE_SPEED = 0.94D;
    private static final double MAX_FLEE_SPEED = 1.62D;
    private static final double FEAR_FLEE_SPEED_BONUS = 0.14D;

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

    /*
     * Explicit herd panic spread.
     */
    private static final double HERD_PANIC_RADIUS_BLOCKS = 13.0D;
    private static final double HERD_PANIC_RADIUS_SQUARED =
            HERD_PANIC_RADIUS_BLOCKS * HERD_PANIC_RADIUS_BLOCKS;

    private static final double HERD_PANIC_SIGHT_RADIUS_BLOCKS = 13.0D;
    private static final double HERD_PANIC_SIGHT_RADIUS_SQUARED =
            HERD_PANIC_SIGHT_RADIUS_BLOCKS * HERD_PANIC_SIGHT_RADIUS_BLOCKS;

    private static final double HERD_PANIC_HEARING_RADIUS_BLOCKS = 7.0D;
    private static final double HERD_PANIC_HEARING_RADIUS_SQUARED =
            HERD_PANIC_HEARING_RADIUS_BLOCKS * HERD_PANIC_HEARING_RADIUS_BLOCKS;

    private static final double WARREN_FLEE_MAX_DISTANCE_BLOCKS = 32.0D;
    private static final double WARREN_FLEE_MAX_DISTANCE_SQUARED =
            WARREN_FLEE_MAX_DISTANCE_BLOCKS * WARREN_FLEE_MAX_DISTANCE_BLOCKS;

    private static final double WARREN_FLEE_MIN_ALIGNMENT = -0.35D;
    private static final double WARREN_FLEE_MIN_SPEED = 1.34D;
    private static final double WARREN_HIDE_DISTANCE_BLOCKS = 3.0D;
    private static final double WARREN_HIDE_DISTANCE_SQUARED =
            WARREN_HIDE_DISTANCE_BLOCKS * WARREN_HIDE_DISTANCE_BLOCKS;
    private static final double WARREN_HIDE_CLOSE_THREAT_BLOCKS = 4.0D;
    private static final double WARREN_HIDE_CLOSE_THREAT_SQUARED =
            WARREN_HIDE_CLOSE_THREAT_BLOCKS * WARREN_HIDE_CLOSE_THREAT_BLOCKS;

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
         * Existing fear memory has priority.
         * Important: do this BEFORE copying herd panic again, otherwise animals
         * can refresh each other forever.
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

        /*
         * Explicit herd panic:
         * this only copies from animals that have direct predator fear.
         * Copied herd panic cannot spread again.
         */
        PathfinderMob panicSource = findBestHerdPanicSource(
                level,
                prey,
                gameTime
        );

        if (panicSource != null) {
            rememberHerdPanic(
                    prey,
                    panicSource,
                    gameTime
            );

            FleeMemory copiedMemory = getActiveFleeMemory(
                    prey,
                    gameTime
            );

            if (copiedMemory != null) {
                fleeFromMemory(
                        prey,
                        copiedMemory,
                        gameTime
                );
                return;
            }
        }

        if (RetoldAiControl.isControlledAs(prey, RetoldAiControlMode.FLEE)) {
            RetoldAiControl.clearIfControlledAs(prey, RetoldAiControlMode.FLEE);
            prey.setSprinting(false);
            prey.getNavigation().stop();
            markFleeEnded(
                    prey,
                    gameTime
            );
        }
    }

    private static boolean shouldThink(
            PathfinderMob prey,
            long gameTime
    ) {
        return RetoldBehaviorTiming.shouldThink(
                prey,
                gameTime,
                FLEE_THINK_INTERVAL_TICKS
        );
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

        if (!RetoldBehaviorCoordinator.isValidAssignmentTarget(prey, candidate)) {
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

        if (!RetoldPreyTargeting.isValidMobRulePrey(
                threatMob,
                prey,
                gameTime
        )) {
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
        if (prey.distanceToSqr(threatMob) > getWarningThreatRadiusSquared(prey)) {
            return false;
        }

        RetoldMobState predatorState = RetoldMobStates.get(threatMob);

        if (predatorState == null) {
            return false;
        }

        if (!RetoldMobRules.hasHuntDrive(threatMob, predatorState)) {
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

    private static PathfinderMob findBestHerdPanicSource(
            ServerLevel level,
            PathfinderMob prey,
            long gameTime
    ) {
        AABB area = prey.getBoundingBox().inflate(HERD_PANIC_RADIUS_BLOCKS);

        List<PathfinderMob> candidates = level.getEntitiesOfClass(
                PathfinderMob.class,
                area,
                candidate -> isValidHerdPanicSource(
                        prey,
                        candidate,
                        gameTime
                )
        );

        PathfinderMob bestSource = null;
        double bestScore = Double.MAX_VALUE;

        for (PathfinderMob candidate : candidates) {
            double distanceSquared = prey.distanceToSqr(candidate);

            if (distanceSquared > HERD_PANIC_RADIUS_SQUARED) {
                continue;
            }

            double score = distanceSquared;

            if (isSameAnimalType(prey, candidate)) {
                score -= 18.0D;
            }

            if (prey.hasLineOfSight(candidate)) {
                score -= 10.0D;
            }

            if (candidate.isSprinting()) {
                score -= 8.0D;
            }

            if (score < bestScore) {
                bestScore = score;
                bestSource = candidate;
            }
        }

        return bestSource;
    }

    private static boolean isValidHerdPanicSource(
            PathfinderMob prey,
            PathfinderMob candidate,
            long gameTime
    ) {
        if (prey == null || candidate == null) {
            return false;
        }

        if (prey == candidate) {
            return false;
        }

        if (!RetoldBehaviorCoordinator.isAliveInSameLevel(prey, candidate)) {
            return false;
        }

        if (!isFleeingPrey(candidate)) {
            return false;
        }

        if (!canSharePanic(prey, candidate)) {
            return false;
        }

        if (prey.distanceToSqr(candidate) > HERD_PANIC_RADIUS_SQUARED) {
            return false;
        }

        FleeMemory sourceMemory = getActiveFleeMemory(
                candidate,
                gameTime
        );

        /*
         * Critical anti-loop rule:
         * only direct predator fear can broadcast herd panic.
         * Herd panic copied from another animal cannot spread again.
         */
        if (sourceMemory == null || sourceMemory.fromHerdPanic()) {
            return false;
        }

        return canSensePanicSource(
                prey,
                candidate
        );
    }

    private static boolean canSensePanicSource(
            PathfinderMob prey,
            PathfinderMob panicSource
    ) {
        double distanceSquared = prey.distanceToSqr(panicSource);

        if (
                distanceSquared <= HERD_PANIC_SIGHT_RADIUS_SQUARED
                        && prey.hasLineOfSight(panicSource)
        ) {
            return true;
        }

        return distanceSquared <= HERD_PANIC_HEARING_RADIUS_SQUARED
                && isAudible(panicSource);
    }

    private static boolean canSharePanic(
            PathfinderMob prey,
            PathfinderMob panicSource
    ) {
        String preyPath = getPath(prey);
        String sourcePath = getPath(panicSource);

        /*
         * Fish panic with fish, land animals panic with land animals.
         * This prevents weird cases like a cow reacting to a salmon.
         */
        if (isFishPath(preyPath) || isFishPath(sourcePath)) {
            return isFishPath(preyPath) && isFishPath(sourcePath);
        }

        return isLandPreyPath(preyPath) && isLandPreyPath(sourcePath);
    }

    private static boolean isSameAnimalType(
            PathfinderMob first,
            PathfinderMob second
    ) {
        return getPath(first).equals(
                getPath(second)
        );
    }

    private static void rememberThreat(
            PathfinderMob prey,
            LivingEntity threat,
            long gameTime
    ) {
        markDanger(
                prey,
                gameTime,
                false
        );

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
                        gameTime + FLEE_MEMORY_TICKS,
                        false
                )
        );
    }

    private static void rememberHerdPanic(
            PathfinderMob prey,
            PathfinderMob panicSource,
            long gameTime
    ) {
        markDanger(
                prey,
                gameTime,
                true
        );

        FleeMemory sourceMemory = getActiveFleeMemory(
                panicSource,
                gameTime
        );

        Vec3 copiedDirection = null;

        if (sourceMemory != null && sourceMemory.awayDirection() != null) {
            copiedDirection = sourceMemory.awayDirection();
        }

        if (copiedDirection == null || copiedDirection.lengthSqr() <= 0.0001D) {
            Vec3 movement = panicSource.getDeltaMovement();

            copiedDirection = new Vec3(
                    movement.x,
                    0.0D,
                    movement.z
            );
        }

        if (copiedDirection == null || copiedDirection.lengthSqr() <= 0.0001D) {
            copiedDirection = new Vec3(
                    prey.getX() - panicSource.getX(),
                    0.0D,
                    prey.getZ() - panicSource.getZ()
            );
        }

        if (copiedDirection.lengthSqr() <= 0.0001D) {
            copiedDirection = randomHorizontalDirection(prey);
        } else {
            copiedDirection = copiedDirection.normalize();
        }

        /*
         * Small herd spread, but still mostly same direction.
         */
        Vec3 side = new Vec3(
                -copiedDirection.z,
                0.0D,
                copiedDirection.x
        );

        double sideDrift = (prey.getRandom().nextDouble() - 0.5D) * 0.28D;

        Vec3 finalDirection = copiedDirection
                .add(side.scale(sideDrift));

        if (finalDirection.lengthSqr() <= 0.0001D) {
            finalDirection = copiedDirection;
        } else {
            finalDirection = finalDirection.normalize();
        }

        FLEE_MEMORIES.put(
                prey,
                new FleeMemory(
                        panicSource.blockPosition().immutable(),
                        finalDirection,
                        gameTime,
                        gameTime + HERD_PANIC_MEMORY_TICKS,
                        true
                )
        );
    }

    private static void markDanger(
            PathfinderMob prey,
            long gameTime,
            boolean copiedPanic
    ) {
        RetoldMobState state = RetoldMobStates.getOrCreate(
                prey,
                gameTime
        );

        state.markDanger(gameTime);
        state.addStress(copiedPanic ? 2 : 4);
        state.addConfidence(copiedPanic ? -1 : -3);
    }

    private static void markFleeEnded(
            PathfinderMob prey,
            long gameTime
    ) {
        RetoldMobState state = RetoldMobStates.getOrCreate(
                prey,
                gameTime
        );

        state.markFleeEnded(gameTime);
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
        if (
                prey.distanceToSqr(threat) > WARREN_HIDE_CLOSE_THREAT_SQUARED
                        && hideAtWarrenIfReached(
                        prey,
                        gameTime
                )
        ) {
            return;
        }

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
        double fleeDistance = getFleeDistance(
                prey,
                distanceSquared
        );

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
        if (hideAtWarrenIfReached(prey, gameTime)) {
            return;
        }

        Vec3 away = memory.awayDirection();

        if (away == null || away.lengthSqr() <= 0.0001D) {
            away = randomHorizontalDirection(prey);
        } else {
            away = away.normalize();
        }

        Vec3 side = new Vec3(
                -away.z,
                0.0D,
                away.x
        );

        double sideDrift = memory.fromHerdPanic()
                ? (prey.getRandom().nextDouble() - 0.5D) * 0.26D
                : (prey.getRandom().nextDouble() - 0.5D) * 0.38D;

        Vec3 rememberedDirection = away
                .add(side.scale(sideDrift));

        if (rememberedDirection.lengthSqr() <= 0.0001D) {
            rememberedDirection = away;
        } else {
            rememberedDirection = rememberedDirection.normalize();
        }

        double fleeDistance = memory.fromHerdPanic()
                ? HERD_PANIC_FLEE_DISTANCE_BLOCKS
                : MEMORY_FLEE_DISTANCE_BLOCKS;
        fleeDistance += getFearPressure(prey) * FEAR_FLEE_DISTANCE_BONUS_BLOCKS;

        double speed = memory.fromHerdPanic()
                ? getHerdPanicFleeSpeed(prey)
                : getMemoryFleeSpeed(prey);

        moveInFleeDirection(
                prey,
                rememberedDirection,
                fleeDistance,
                speed,
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
        BlockPos homeFleeTarget = getHomeFleeTarget(
                prey,
                safeDirection
        );

        if (homeFleeTarget != null) {
            targetPos = homeFleeTarget;
            speed = Math.max(
                    speed,
                    WARREN_FLEE_MIN_SPEED
            );
        }

        RetoldAiControl.refresh(
                prey,
                RetoldAiControlMode.FLEE,
                gameTime,
                FLEE_CONTROL_TICKS
        );

        prey.setSprinting(true);

        BlockPos finalTargetPos = targetPos;
        double finalSpeed = speed;

        RetoldAiControl.withNavigationBypass(() -> {
            prey.getNavigation().moveTo(
                    finalTargetPos.getX() + 0.5D,
                    finalTargetPos.getY(),
                    finalTargetPos.getZ() + 0.5D,
                    finalSpeed
            );
        });
    }

    private static boolean hideAtWarrenIfReached(
            PathfinderMob prey,
            long gameTime
    ) {
        RetoldAnimalHomeMemory home = getValidWarrenHome(prey);

        if (home == null) {
            return false;
        }

        if (prey.blockPosition().distSqr(home.pos()) > WARREN_HIDE_DISTANCE_SQUARED) {
            return false;
        }

        RetoldAiControl.refresh(
                prey,
                RetoldAiControlMode.FLEE,
                gameTime,
                FLEE_CONTROL_TICKS
        );

        prey.setSprinting(false);
        prey.getNavigation().stop();

        RetoldAnimalHomes.markUsed(
                prey,
                gameTime
        );

        return true;
    }

    private static BlockPos getHomeFleeTarget(
            PathfinderMob prey,
            Vec3 safeDirection
    ) {
        RetoldAnimalHomeMemory home = getValidWarrenHome(prey);

        if (home == null) {
            return null;
        }

        if (prey.blockPosition().distSqr(home.pos()) > WARREN_FLEE_MAX_DISTANCE_SQUARED) {
            return null;
        }

        Vec3 toHome = new Vec3(
                home.pos().getX() + 0.5D - prey.getX(),
                0.0D,
                home.pos().getZ() + 0.5D - prey.getZ()
        );

        if (toHome.lengthSqr() <= 0.0001D) {
            return home.pos();
        }

        Vec3 normalizedHome = toHome.normalize();

        if (normalizedHome.dot(safeDirection) < WARREN_FLEE_MIN_ALIGNMENT) {
            return null;
        }

        RetoldAnimalHomes.markUsed(
                prey,
                ((ServerLevel) prey.level()).getGameTime()
        );

        return home.pos();
    }

    private static RetoldAnimalHomeMemory getValidWarrenHome(PathfinderMob prey) {
        if (!(prey.level() instanceof ServerLevel level)) {
            return null;
        }

        RetoldAnimalHomeMemory home = RetoldAnimalHomes.get(prey);

        if (!RetoldAnimalHomes.isValidFor(level, prey, home)) {
            return null;
        }

        if (home.type() != RetoldAnimalHomeType.WARREN) {
            return null;
        }

        return home;
    }

    private static double getWarningThreatRadiusSquared(PathfinderMob prey) {
        double radius = WARNING_THREAT_RADIUS_BLOCKS
                + getFearPressure(prey) * FEAR_WARNING_RADIUS_BONUS_BLOCKS;

        return radius * radius;
    }

    private static double getFleeDistance(
            PathfinderMob prey,
            double distanceSquared
    ) {
        double fearBonus = getFearPressure(prey) * FEAR_FLEE_DISTANCE_BONUS_BLOCKS;

        if (distanceSquared <= CLOSE_THREAT_DISTANCE_SQUARED) {
            return CLOSE_FLEE_DISTANCE_BLOCKS + fearBonus;
        }

        if (distanceSquared > FAR_THREAT_DISTANCE_SQUARED) {
            return FAR_FLEE_DISTANCE_BLOCKS + fearBonus;
        }

        return BASE_FLEE_DISTANCE_BLOCKS + fearBonus;
    }

    private static double getFleeSpeed(
            PathfinderMob prey,
            LivingEntity threat
    ) {
        double distanceSquared = prey.distanceToSqr(threat);

        double modifier = 1.0D;
        modifier += getFearPressure(prey) * FEAR_FLEE_SPEED_BONUS;

        if (distanceSquared <= CLOSE_THREAT_DISTANCE_SQUARED) {
            modifier += 0.20D + prey.getRandom().nextDouble() * 0.18D;
        }

        if (distanceSquared > FAR_THREAT_DISTANCE_SQUARED) {
            modifier -= 0.06D + prey.getRandom().nextDouble() * 0.04D;
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
        modifier += getFearPressure(prey) * (FEAR_FLEE_SPEED_BONUS * 0.75D);

        double roll = prey.getRandom().nextDouble();

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

    private static double getHerdPanicFleeSpeed(PathfinderMob prey) {
        double modifier = 1.0D;
        modifier += getFearPressure(prey) * (FEAR_FLEE_SPEED_BONUS * 0.55D);

        double roll = prey.getRandom().nextDouble();

        if (roll < 0.16D) {
            modifier -= 0.06D + prey.getRandom().nextDouble() * 0.08D;
        }

        if (roll >= 0.16D && roll < 0.36D) {
            modifier += 0.05D + prey.getRandom().nextDouble() * 0.08D;
        }

        return clamp(
                HERD_PANIC_FLEE_SPEED * modifier,
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

    private static double getFearPressure(PathfinderMob prey) {
        RetoldMobState state = RetoldMobStates.get(prey);

        if (state == null) {
            return 0.0D;
        }

        double stressPressure = Math.max(
                0.0D,
                (state.stress() - 15.0D) / 85.0D
        );
        double confidencePressure = Math.max(
                0.0D,
                (50.0D - state.confidence()) / 50.0D
        );

        return clamp(
                stressPressure * 0.65D + confidencePressure * 0.45D,
                0.0D,
                1.0D
        );
    }

    private static boolean isFleeingPrey(PathfinderMob mob) {
        String path = getPath(mob);

        if (path.equals("villager")) {
            return false;
        }

        return path.equals("cow")
                || path.equals("mooshroom")
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

    private static boolean isLandPreyPath(String path) {
        return path.equals("cow")
                || path.equals("mooshroom")
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
                || path.equals("camel");
    }

    private static boolean isFishPath(String path) {
        return path.equals("cod")
                || path.equals("salmon")
                || path.equals("tropical_fish")
                || path.equals("pufferfish");
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

    private static String getPath(PathfinderMob mob) {
        return RetoldMobRules.getEntityTypePath(
                mob.getType()
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

    private record FleeMemory(
            BlockPos lastThreatPos,
            Vec3 awayDirection,
            long lastThreatSeenAt,
            long expiresAt,
            boolean fromHerdPanic
    ) {
        public boolean isExpired(long gameTime) {
            return gameTime > expiresAt;
        }
    }
}
