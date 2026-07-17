package cz.xefensor.retold.behavior.hunting;

import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.control.RetoldAiControlMode;
import cz.xefensor.retold.behavior.performance.RetoldAiSightCache;
import cz.xefensor.retold.behavior.core.RetoldBehaviorMovement;
import cz.xefensor.retold.behavior.core.RetoldBehaviorTargets;
import cz.xefensor.retold.behavior.core.RetoldBehaviorTiming;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;
import cz.xefensor.retold.behavior.profiles.RetoldMobStates;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

public final class RetoldPredatorStaminaEvents {
    private static final Map<PathfinderMob, StaminaState> STATES = new WeakHashMap<>();

    private static final int THINK_INTERVAL_TICKS = 10;
    private static final int STAMINA_PATH_INTERVAL_TICKS = 8;
    private static final int CLEANUP_INTERVAL_TICKS = 20 * 10;

    private static final int GIVE_UP_CONTROL_TICKS = 20 * 4;
    private static final int HUNT_COOLDOWN_TICKS = 20 * 16;

    private static final double FATIGUE_GIVE_UP_THRESHOLD = 98.0D;
    private static final double FATIGUE_SLOW_THRESHOLD = 68.0D;

    private static final double FATIGUE_BASE_GAIN = 2.0D;
    private static final double FATIGUE_FAR_GAIN = 1.7D;
    private static final double FATIGUE_NO_LINE_OF_SIGHT_GAIN = 1.3D;
    private static final double FATIGUE_CLOSE_RECOVERY = 5.2D;
    private static final double FATIGUE_IDLE_DECAY = 5.0D;

    private static final double CLOSE_DISTANCE_BLOCKS = 4.5D;
    private static final double CLOSE_DISTANCE_SQUARED =
            CLOSE_DISTANCE_BLOCKS * CLOSE_DISTANCE_BLOCKS;

    private static final double FAR_DISTANCE_BLOCKS = 15.0D;
    private static final double FAR_DISTANCE_SQUARED =
            FAR_DISTANCE_BLOCKS * FAR_DISTANCE_BLOCKS;

    private static final double GIVE_UP_DISTANCE_BLOCKS = 9.0D;
    private static final double GIVE_UP_DISTANCE_SQUARED =
            GIVE_UP_DISTANCE_BLOCKS * GIVE_UP_DISTANCE_BLOCKS;

    private static final int LONG_CHASE_TICKS = 20 * 20;
    private static final int LONG_WITHOUT_CLOSE_TICKS = 20 * 8;

    private static final double TIRED_WOLF_SPEED = 1.16D;
    private static final double TIRED_FOX_SPEED = 1.08D;
    private static final double TIRED_CAT_SPEED = 1.10D;
    private static final double TIRED_SPIDER_SPEED = 0.96D;
    private static final double TIRED_DOLPHIN_SPEED = 1.14D;
    private static final double TIRED_DEFAULT_SPEED = 1.04D;

    private static final double EXHAUSTED_SPEED_MULTIPLIER = 0.84D;

    private RetoldPredatorStaminaEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof PathfinderMob predator)) {
            return;
        }

        if (!(predator.level() instanceof ServerLevel level)) {
            return;
        }

        if (!RetoldMobRules.canUseOrdinaryPredatorSystems(predator)) {
            STATES.remove(predator);
            return;
        }

        long gameTime = level.getGameTime();

        if (!shouldThink(predator, gameTime)) {
            return;
        }

        StaminaState state = STATES.computeIfAbsent(
                predator,
                ignored -> new StaminaState(gameTime)
        );

        state.lastSeenAt = gameTime;

        if (isCoolingDown(state, gameTime)) {
            blockTiredHunt(
                    predator,
                    state,
                    gameTime
            );
            return;
        }

        if (!RetoldAiControl.isControlledAs(predator, RetoldAiControlMode.HUNT)) {
            recoverWhileNotHunting(
                    predator,
                    state
            );
            return;
        }

        LivingEntity prey = predator.getTarget();

        if (!isValidPrey(predator, prey, gameTime)) {
            recoverWhileNotHunting(
                    predator,
                    state
            );
            return;
        }

        updateChaseFatigue(
                predator,
                prey,
                state,
                gameTime
        );

        if (shouldGiveUp(predator, prey, state, gameTime)) {
            giveUpChase(
                    predator,
                    state,
                    gameTime
            );
            return;
        }

        if (state.fatigue >= FATIGUE_SLOW_THRESHOLD) {
            slowTiredPredator(
                    predator,
                    prey,
                    state,
                    gameTime
            );
        }
    }

    @SubscribeEvent
    public static void onServerTickPost(ServerTickEvent.Post event) {
        ServerLevel level = event.getServer().overworld();
        long gameTime = level.getGameTime();

        if (gameTime % CLEANUP_INTERVAL_TICKS != 0L) {
            return;
        }

        Iterator<Map.Entry<PathfinderMob, StaminaState>> iterator = STATES.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<PathfinderMob, StaminaState> entry = iterator.next();
            PathfinderMob predator = entry.getKey();
            StaminaState state = entry.getValue();

            if (
                    predator == null
                            || !predator.isAlive()
                            || predator.isRemoved()
                            || gameTime - state.lastSeenAt > 20L * 60L
            ) {
                iterator.remove();
            }
        }
    }

    private static boolean shouldThink(
            PathfinderMob predator,
            long gameTime
    ) {
        return RetoldBehaviorTiming.shouldThink(
                predator,
                gameTime,
                THINK_INTERVAL_TICKS
        );
    }

    private static void updateChaseFatigue(
            PathfinderMob predator,
            LivingEntity prey,
            StaminaState state,
            long gameTime
    ) {
        if (state.chaseStartedAt <= 0L) {
            state.chaseStartedAt = gameTime;
            state.lastCloseAt = gameTime;
        }

        double distanceSquared = predator.distanceToSqr(prey);

        if (distanceSquared <= CLOSE_DISTANCE_SQUARED) {
            state.lastCloseAt = gameTime;
            state.fatigue -= FATIGUE_CLOSE_RECOVERY;
        } else {
            state.fatigue += FATIGUE_BASE_GAIN;
        }

        if (distanceSquared >= FAR_DISTANCE_SQUARED) {
            state.fatigue += FATIGUE_FAR_GAIN;
        }

        if (!RetoldAiSightCache.canSee(predator, prey, predator.level().getGameTime())) {
            state.fatigue += FATIGUE_NO_LINE_OF_SIGHT_GAIN;
        }

        state.fatigue = clamp(
                state.fatigue,
                0.0D,
                FATIGUE_GIVE_UP_THRESHOLD + 20.0D
        );
    }

    private static boolean shouldGiveUp(
            PathfinderMob predator,
            LivingEntity prey,
            StaminaState state,
            long gameTime
    ) {
        double distanceSquared = predator.distanceToSqr(prey);

        if (
                state.fatigue >= FATIGUE_GIVE_UP_THRESHOLD
                        && (
                        distanceSquared >= GIVE_UP_DISTANCE_SQUARED
                                || !RetoldAiSightCache.canSee(predator, prey, gameTime)
                )
        ) {
            return true;
        }

        long chaseTime = gameTime - state.chaseStartedAt;
        long timeWithoutClose = gameTime - state.lastCloseAt;

        return chaseTime >= LONG_CHASE_TICKS
                && timeWithoutClose >= LONG_WITHOUT_CLOSE_TICKS
                && distanceSquared >= GIVE_UP_DISTANCE_SQUARED;
    }

    private static void slowTiredPredator(
            PathfinderMob predator,
            LivingEntity prey,
            StaminaState state,
            long gameTime
    ) {
        if (predator.distanceToSqr(prey) <= CLOSE_DISTANCE_SQUARED) {
            return;
        }

        double tiredRatio = clamp(
                (state.fatigue - FATIGUE_SLOW_THRESHOLD)
                        / (FATIGUE_GIVE_UP_THRESHOLD - FATIGUE_SLOW_THRESHOLD),
                0.0D,
                1.0D
        );

        double speed = getTiredSpeed(predator)
                * (1.0D - tiredRatio * (1.0D - EXHAUSTED_SPEED_MULTIPLIER));

        predator.setSprinting(false);

        RetoldAiControl.refresh(
                predator,
                RetoldAiControlMode.HUNT,
                gameTime,
                PARTY_SAFE_HUNT_REFRESH_TICKS()
        );

        RetoldBehaviorMovement.throttledMoveTo(
                predator,
                prey,
                speed,
                gameTime,
                STAMINA_PATH_INTERVAL_TICKS,
                2.5D * 2.5D
        );
    }

    private static int PARTY_SAFE_HUNT_REFRESH_TICKS() {
        return 20 * 3;
    }

    private static void giveUpChase(
            PathfinderMob predator,
            StaminaState state,
            long gameTime
    ) {
        RetoldMobStates.getOrCreate(
                predator,
                gameTime
        ).markFailedHunt(gameTime);

        state.fatigue = Math.max(
                state.fatigue,
                FATIGUE_SLOW_THRESHOLD
        );

        state.huntBlockedUntil = gameTime + HUNT_COOLDOWN_TICKS;
        state.chaseStartedAt = 0L;
        state.lastCloseAt = 0L;

        RetoldBehaviorTargets.setTargetAndAggression(predator, null, false);

        RetoldPredatorStrike.clear(predator);

        predator.setSprinting(false);
        predator.getNavigation().stop();

        RetoldAiControl.claim(
                predator,
                RetoldAiControlMode.REGROUP,
                gameTime,
                GIVE_UP_CONTROL_TICKS
        );
    }

    private static void blockTiredHunt(
            PathfinderMob predator,
            StaminaState state,
            long gameTime
    ) {
        if (RetoldAiControl.isControlledAs(predator, RetoldAiControlMode.HUNT)) {
            RetoldBehaviorTargets.setTargetAndAggression(predator, null, false);

            RetoldPredatorStrike.clear(predator);

            predator.setSprinting(false);
            predator.getNavigation().stop();

            RetoldAiControl.claim(
                    predator,
                    RetoldAiControlMode.REGROUP,
                    gameTime,
                    GIVE_UP_CONTROL_TICKS
            );
        }

        state.fatigue = Math.max(
                0.0D,
                state.fatigue - FATIGUE_IDLE_DECAY * 0.5D
        );
    }

    private static void recoverWhileNotHunting(
            PathfinderMob predator,
            StaminaState state
    ) {
        state.chaseStartedAt = 0L;
        state.lastCloseAt = 0L;

        state.fatigue = Math.max(
                0.0D,
                state.fatigue - FATIGUE_IDLE_DECAY
        );

        if (
                state.fatigue <= 0.0D
                        && !RetoldAiControl.isControlledAs(predator, RetoldAiControlMode.REGROUP)
        ) {
            state.huntBlockedUntil = 0L;
        }
    }

    private static boolean isCoolingDown(
            StaminaState state,
            long gameTime
    ) {
        return state.huntBlockedUntil > gameTime;
    }

    private static boolean isValidPrey(
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

    private static double getTiredSpeed(PathfinderMob predator) {
        String path = RetoldMobRules.getEntityTypePath(
                predator.getType()
        );

        if (path.equals("wolf")) {
            return TIRED_WOLF_SPEED;
        }

        if (path.equals("fox")) {
            return TIRED_FOX_SPEED;
        }

        if (path.equals("cat") || path.equals("ocelot")) {
            return TIRED_CAT_SPEED;
        }

        if (path.equals("spider") || path.equals("cave_spider")) {
            return TIRED_SPIDER_SPEED;
        }

        if (path.equals("dolphin")) {
            return TIRED_DOLPHIN_SPEED;
        }

        return TIRED_DEFAULT_SPEED;
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

    private static final class StaminaState {
        private double fatigue;
        private long chaseStartedAt;
        private long lastCloseAt;
        private long huntBlockedUntil;
        private long lastSeenAt;

        private StaminaState(long gameTime) {
            this.lastSeenAt = gameTime;
        }
    }
}
