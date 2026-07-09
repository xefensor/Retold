package cz.xefensor.retold.behavior;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.List;

public final class RetoldMobStateRecoveryEvents {
    private static final int THINK_INTERVAL_TICKS = 20 * 5;
    private static final int RECENT_DANGER_TICKS = 20 * 10;

    private static final int BASE_CONFIDENCE_TARGET = 50;
    private static final int GROUP_CONFIDENCE_TARGET = 60;
    private static final int HOME_CONFIDENCE_TARGET = 68;
    private static final int ISOLATION_STRESS_SOFT_CAP = 45;
    private static final int ISOLATION_CONFIDENCE_FLOOR = 35;

    private static final double HOME_COMFORT_RADIUS_BLOCKS = 12.0D;
    private static final double HOME_COMFORT_RADIUS_SQUARED =
            HOME_COMFORT_RADIUS_BLOCKS * HOME_COMFORT_RADIUS_BLOCKS;

    private static final double GROUP_COMFORT_RADIUS_BLOCKS = 12.0D;
    private static final double GROUP_COMFORT_RADIUS_SQUARED =
            GROUP_COMFORT_RADIUS_BLOCKS * GROUP_COMFORT_RADIUS_BLOCKS;

    private RetoldMobStateRecoveryEvents() {
    }

    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof PathfinderMob mob)) {
            return;
        }

        if (!(mob.level() instanceof ServerLevel level)) {
            return;
        }

        if (!RetoldMobRules.isManagedMob(mob)) {
            return;
        }

        long gameTime = level.getGameTime();

        if (!shouldThink(mob, gameTime)) {
            return;
        }

        RetoldMobState state = RetoldMobStates.getOrCreate(
                mob,
                gameTime
        );

        if (!canRecover(mob, state, gameTime)) {
            return;
        }

        recoverState(
                level,
                mob,
                state
        );
    }

    private static boolean shouldThink(
            PathfinderMob mob,
            long gameTime
    ) {
        int offset = Math.floorMod(
                mob.getId(),
                THINK_INTERVAL_TICKS
        );

        return (gameTime + offset) % THINK_INTERVAL_TICKS == 0L;
    }

    private static boolean canRecover(
            PathfinderMob mob,
            RetoldMobState state,
            long gameTime
    ) {
        if (mob.getTarget() != null && mob.getTarget().isAlive()) {
            return false;
        }

        RetoldAiControlMode mode = RetoldAiControl.getMode(mob);

        if (
                mode == RetoldAiControlMode.FLEE
                        || mode == RetoldAiControlMode.HUNT
                        || mode == RetoldAiControlMode.ATTACK
                        || mode == RetoldAiControlMode.TERRITORY
        ) {
            return false;
        }

        return gameTime - state.lastDangerAt() > RECENT_DANGER_TICKS;
    }

    private static void recoverState(
            ServerLevel level,
            PathfinderMob mob,
            RetoldMobState state
    ) {
        boolean nearHome = isNearHome(
                level,
                mob
        );
        boolean nearGroup = isNearCompatibleGroup(
                level,
                mob
        );

        if (
                isSocialRecoveryMob(mob)
                        && !nearHome
                        && !nearGroup
        ) {
            applyIsolationPressure(state);
            return;
        }

        if (state.stress() > 0) {
            state.addStress(nearHome ? -2 : -1);
        }

        int confidenceTarget = nearHome
                ? HOME_CONFIDENCE_TARGET
                : nearGroup
                ? GROUP_CONFIDENCE_TARGET
                : BASE_CONFIDENCE_TARGET;

        if (state.confidence() < confidenceTarget) {
            state.addConfidence(nearHome || nearGroup ? 2 : 1);
        }
    }

    private static void applyIsolationPressure(RetoldMobState state) {
        if (state.stress() < ISOLATION_STRESS_SOFT_CAP) {
            state.addStress(1);
        }

        if (state.confidence() > ISOLATION_CONFIDENCE_FLOOR) {
            state.addConfidence(-1);
        }
    }

    private static boolean isNearHome(
            ServerLevel level,
            PathfinderMob mob
    ) {
        RetoldAnimalHomeMemory home = RetoldAnimalHomes.get(mob);

        if (!RetoldAnimalHomes.isValidFor(level, mob, home)) {
            return false;
        }

        return mob.blockPosition().distSqr(home.pos()) <= HOME_COMFORT_RADIUS_SQUARED;
    }

    private static boolean isNearCompatibleGroup(
            ServerLevel level,
            PathfinderMob mob
    ) {
        AABB area = mob.getBoundingBox().inflate(GROUP_COMFORT_RADIUS_BLOCKS);

        List<PathfinderMob> candidates = level.getEntitiesOfClass(
                PathfinderMob.class,
                area,
                candidate -> isCompatibleGroupMember(
                        mob,
                        candidate
                )
        );

        return !candidates.isEmpty();
    }

    private static boolean isCompatibleGroupMember(
            PathfinderMob mob,
            PathfinderMob candidate
    ) {
        if (mob == null || candidate == null || mob == candidate) {
            return false;
        }

        if (!candidate.isAlive() || candidate.isRemoved()) {
            return false;
        }

        if (mob.level() != candidate.level()) {
            return false;
        }

        if (mob.distanceToSqr(candidate) > GROUP_COMFORT_RADIUS_SQUARED) {
            return false;
        }

        RetoldMobProfileType profileType = RetoldMobRules.profileType(mob);

        if (profileType != RetoldMobRules.profileType(candidate)) {
            return false;
        }

        String mobPath = getPath(mob);
        String candidatePath = getPath(candidate);

        if (profileType == RetoldMobProfileType.HUNGRY_GRAZER) {
            return herdGroup(mobPath).equals(
                    herdGroup(candidatePath)
            );
        }

        return mobPath.equals(candidatePath);
    }

    private static boolean isSocialRecoveryMob(PathfinderMob mob) {
        RetoldMobProfileType profileType = RetoldMobRules.profileType(mob);

        return profileType == RetoldMobProfileType.HUNGRY_GRAZER
                || profileType == RetoldMobProfileType.SMALL_FORAGER
                || profileType == RetoldMobProfileType.PACK_PREDATOR
                || profileType == RetoldMobProfileType.AQUATIC_PREDATOR;
    }

    private static String herdGroup(String path) {
        if (
                path.equals("horse")
                        || path.equals("donkey")
                        || path.equals("mule")
        ) {
            return "equine";
        }

        if (
                path.equals("llama")
                        || path.equals("trader_llama")
        ) {
            return "llama";
        }

        return path;
    }

    private static String getPath(PathfinderMob mob) {
        return RetoldMobRules.getEntityTypePath(
                mob.getType()
        );
    }
}
