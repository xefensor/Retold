package cz.xefensor.retold.behavior.flee;

import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.control.RetoldAiControlMode;
import cz.xefensor.retold.behavior.control.RetoldAiControlOwner;
import cz.xefensor.retold.behavior.control.RetoldAiPriorities;
import cz.xefensor.retold.behavior.core.RetoldBehaviorMovement;
import cz.xefensor.retold.behavior.performance.RetoldAiScanCache;
import cz.xefensor.retold.behavior.performance.RetoldAiSightCache;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;
import cz.xefensor.retold.combat.RetoldCombatTargets;
import cz.xefensor.retold.faction.RetoldFactionMembers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class RetoldCreeperAwareness {
    private static final Map<Mob, ReactionState> REACTIONS = new WeakHashMap<>();

    private static final Set<String> INTELLIGENT_PATHS = Set.of(
            "villager",
            "wandering_trader",
            "pillager",
            "vindicator",
            "evoker",
            "illusioner",
            "witch",
            "piglin",
            "piglin_brute",
            "allay",
            "wolf",
            "dolphin"
    );

    private static final Set<String> SLOW_REACTION_PATHS = Set.of(
            "slime",
            "magma_cube",
            "cow",
            "mooshroom",
            "sheep",
            "pig",
            "chicken",
            "rabbit",
            "goat",
            "horse",
            "donkey",
            "mule",
            "llama",
            "trader_llama",
            "camel"
    );

    private static final int SCAN_CACHE_TICKS = 4;
    private static final int CONTROL_TICKS = 12;
    private static final int MEMORY_TICKS = 20;
    private static final int PATH_INTERVAL_TICKS = 4;
    private static final int FLEE_PRIORITY = RetoldAiPriorities.above(
            RetoldAiPriorities.TERRITORY,
            10
    );

    private static final double MAX_SCAN_RADIUS_BLOCKS = 18.0D;
    private static final double IGNITED_SIGHT_RADIUS_BLOCKS = 18.0D;
    private static final double IGNITED_HEARING_RADIUS_BLOCKS = 12.0D;
    private static final double URGENT_RADIUS_BLOCKS = 5.0D;
    private static final double CAT_AVOID_RADIUS_BLOCKS = 8.0D;
    private static final double CAT_CLOSE_SENSE_RADIUS_BLOCKS = 4.0D;
    private static final double FLEE_DISTANCE_BLOCKS = 15.0D;
    private static final double CAT_FLEE_DISTANCE_BLOCKS = 11.0D;
    private static final double FLEE_SPEED = 1.32D;
    private static final double CAT_FLEE_SPEED = 1.38D;

    private static final String CONTROL_REASON = "creeper_danger";

    private RetoldCreeperAwareness() {
    }

    public static void tick(
            ServerLevel level,
            Mob mob,
            long gameTime,
            boolean scanAllowed
    ) {
        if (level == null || mob == null || !mob.isAlive() || mob.isRemoved()) {
            stopReaction(mob);
            return;
        }

        if (mob instanceof Zombie) {
            stopReaction(mob);
            return;
        }

        ReactionState state = REACTIONS.get(mob);
        Creeper threat = validRememberedThreat(mob, state);

        if (scanAllowed && threat == null) {
            threat = findBestPerceivedCreeper(level, mob, gameTime);
        }

        if (threat != null) {
            state = observeThreat(mob, threat, state, gameTime);
        } else if (state == null || gameTime > state.lastSensedAt + MEMORY_TICKS) {
            stopReaction(mob);
            return;
        }

        if (gameTime < state.reactAt) {
            return;
        }

        react(mob, state, gameTime);
    }

    public static boolean isReacting(Mob mob) {
        return mob != null
                && RetoldAiControl.isControlledAsByWithReason(
                mob,
                RetoldAiControlMode.FLEE,
                RetoldAiControlOwner.FLEEING,
                CONTROL_REASON
        );
    }

    private static Creeper findBestPerceivedCreeper(
            ServerLevel level,
            Mob mob,
            long gameTime
    ) {
        Creeper best = null;
        double bestDistanceSquared = Double.MAX_VALUE;

        for (Creeper candidate : RetoldAiScanCache.nearby(
                level,
                mob,
                Creeper.class,
                MAX_SCAN_RADIUS_BLOCKS,
                gameTime,
                SCAN_CACHE_TICKS
        )) {
            if (candidate == mob || !canPerceive(mob, candidate, gameTime)) {
                continue;
            }

            double distanceSquared = mob.distanceToSqr(candidate);

            if (distanceSquared < bestDistanceSquared) {
                best = candidate;
                bestDistanceSquared = distanceSquared;
            }
        }

        return best;
    }

    private static boolean canPerceive(
            Mob mob,
            Creeper creeper,
            long gameTime
    ) {
        double distanceSquared = mob.distanceToSqr(creeper);

        if (mob instanceof Cat) {
            if (distanceSquared > square(CAT_AVOID_RADIUS_BLOCKS)) {
                return false;
            }

            return distanceSquared <= square(CAT_CLOSE_SENSE_RADIUS_BLOCKS)
                    || RetoldAiSightCache.canSee(mob, creeper, gameTime)
                    || isFuseActive(creeper);
        }

        if (!isFuseActive(creeper)) {
            return false;
        }

        if (distanceSquared <= square(IGNITED_HEARING_RADIUS_BLOCKS)) {
            return true;
        }

        return distanceSquared <= square(IGNITED_SIGHT_RADIUS_BLOCKS)
                && RetoldAiSightCache.canSee(mob, creeper, gameTime);
    }

    private static Creeper validRememberedThreat(
            Mob mob,
            ReactionState state
    ) {
        if (state == null || state.threat == null) {
            return null;
        }

        Creeper threat = state.threat;

        if (!threat.isAlive() || threat.isRemoved() || threat.level() != mob.level()) {
            state.threat = null;
            return null;
        }

        double allowedRadius = mob instanceof Cat
                ? CAT_AVOID_RADIUS_BLOCKS
                : MAX_SCAN_RADIUS_BLOCKS;

        if (mob.distanceToSqr(threat) > square(allowedRadius)) {
            state.threat = null;
            return null;
        }

        if (!(mob instanceof Cat) && !isFuseActive(threat)) {
            state.threat = null;
            return null;
        }

        return threat;
    }

    private static ReactionState observeThreat(
            Mob mob,
            Creeper threat,
            ReactionState state,
            long gameTime
    ) {
        if (state == null || state.threat != threat) {
            state = new ReactionState();
            state.threat = threat;
            state.reactAt = gameTime + reactionDelayTicks(mob);
            REACTIONS.put(mob, state);
        }

        state.lastSensedAt = gameTime;
        state.lastThreatPos = threat.blockPosition().immutable();

        if (mob.distanceToSqr(threat) <= square(URGENT_RADIUS_BLOCKS)) {
            state.reactAt = Math.min(state.reactAt, gameTime + 1L);
        }

        return state;
    }

    private static int reactionDelayTicks(Mob mob) {
        if (mob instanceof Cat) {
            return 0;
        }

        if (RetoldFactionMembers.isVillageDefender(mob)) {
            return randomDelay(mob, 0, 2);
        }

        String path = RetoldMobRules.getEntityTypePath(mob.getType());

        if (INTELLIGENT_PATHS.contains(path)) {
            return randomDelay(mob, 1, 4);
        }

        if (SLOW_REACTION_PATHS.contains(path)) {
            return randomDelay(mob, 4, 9);
        }

        return randomDelay(mob, 2, 7);
    }

    private static int randomDelay(
            Mob mob,
            int minimum,
            int maximum
    ) {
        return minimum + mob.getRandom().nextInt(maximum - minimum + 1);
    }

    private static void react(
            Mob mob,
            ReactionState state,
            long gameTime
    ) {
        if (!RetoldAiControl.tryClaim(
                mob,
                RetoldAiControlMode.FLEE,
                RetoldAiControlOwner.FLEEING,
                FLEE_PRIORITY,
                CONTROL_REASON,
                gameTime,
                CONTROL_TICKS
        )) {
            return;
        }

        LivingEntity currentTarget = mob.getTarget();

        if (currentTarget != null) {
            RetoldCombatTargets.clearTargetReferencesAndAggression(
                    mob,
                    currentTarget,
                    true
            );
        }

        if (mob instanceof Cat cat && !state.hissed) {
            cat.hiss();
            state.hissed = true;
        }

        Vec3 away = awayFromThreat(mob, state);
        double fleeDistance = mob instanceof Cat
                ? CAT_FLEE_DISTANCE_BLOCKS
                : FLEE_DISTANCE_BLOCKS;
        double speed = mob instanceof Cat ? CAT_FLEE_SPEED : FLEE_SPEED;
        Vec3 destination = mob.position().add(away.scale(fleeDistance));
        BlockPos destinationPos = BlockPos.containing(
                destination.x,
                mob.getY(),
                destination.z
        );

        mob.setSprinting(true);
        if (mob instanceof PathfinderMob pathfinderMob) {
            RetoldBehaviorMovement.throttledMoveTo(
                    pathfinderMob,
                    destinationPos,
                    speed,
                    gameTime,
                    PATH_INTERVAL_TICKS,
                    2.0D * 2.0D
            );
        } else {
            mob.getMoveControl().setWantedPosition(
                    destination.x,
                    mob.getY(),
                    destination.z,
                    speed
            );
        }
    }

    private static Vec3 awayFromThreat(
            Mob mob,
            ReactionState state
    ) {
        BlockPos threatPos = state.lastThreatPos;
        Vec3 away = threatPos == null
                ? Vec3.ZERO
                : new Vec3(
                mob.getX() - (threatPos.getX() + 0.5D),
                0.0D,
                mob.getZ() - (threatPos.getZ() + 0.5D)
        );

        if (away.lengthSqr() <= 0.0001D) {
            double angle = mob.getRandom().nextDouble() * Math.PI * 2.0D;
            return new Vec3(Math.cos(angle), 0.0D, Math.sin(angle));
        }

        return away.normalize();
    }

    private static void stopReaction(Mob mob) {
        if (mob == null) {
            return;
        }

        REACTIONS.remove(mob);

        if (RetoldAiControl.clearIfControlledAsByWithReason(
                mob,
                RetoldAiControlMode.FLEE,
                RetoldAiControlOwner.FLEEING,
                CONTROL_REASON
        )) {
            mob.setSprinting(false);

            if (mob instanceof PathfinderMob pathfinderMob) {
                pathfinderMob.getNavigation().stop();
            } else {
                mob.getMoveControl().setWait();
            }
        }
    }

    private static boolean isFuseActive(Creeper creeper) {
        return creeper.isIgnited() || creeper.getSwellDir() > 0;
    }

    private static double square(double value) {
        return value * value;
    }

    private static final class ReactionState {
        private Creeper threat;
        private BlockPos lastThreatPos;
        private long lastSensedAt;
        private long reactAt;
        private boolean hissed;
    }
}
