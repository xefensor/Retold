package cz.xefensor.retold.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class RetoldSmallForagerHomeEvents {
    private static final RetoldAiControlOwner CONTROL_OWNER = RetoldAiControlOwner.REGROUP;
    private static final String REASON_RETURN_HOME = "return_small_home";
    private static final String REASON_ROOSTING = "roosting";
    private static final String REASON_RESTING = "resting";

    private static final int THINK_INTERVAL_TICKS = 40;
    private static final int HOME_RETURN_CONTROL_TICKS = 20 * 5;
    private static final int HOME_RETURN_PRIORITY = 17;
    private static final int ROOST_CONTROL_TICKS = 20 * 5;
    private static final int ROOST_PRIORITY = 16;
    private static final int REST_CONTROL_TICKS = 20 * 5;
    private static final int REST_PRIORITY = 15;
    private static final int MAX_HOME_MEMBERS = 6;
    private static final int PANIC_RECOVERY_TICKS = 20 * 18;

    private static final double HOME_CREATION_RADIUS_BLOCKS = 14.0D;
    private static final double HOME_CREATION_RADIUS_SQUARED =
            HOME_CREATION_RADIUS_BLOCKS * HOME_CREATION_RADIUS_BLOCKS;

    private static final double HOME_RETURN_START_BLOCKS = 20.0D;
    private static final double HOME_RETURN_START_SQUARED =
            HOME_RETURN_START_BLOCKS * HOME_RETURN_START_BLOCKS;

    private static final double PANIC_RECOVERY_RETURN_START_BLOCKS = 8.0D;
    private static final double PANIC_RECOVERY_RETURN_START_SQUARED =
            PANIC_RECOVERY_RETURN_START_BLOCKS * PANIC_RECOVERY_RETURN_START_BLOCKS;

    private static final double HOME_RETURN_STOP_BLOCKS = 6.0D;
    private static final double HOME_RETURN_STOP_SQUARED =
            HOME_RETURN_STOP_BLOCKS * HOME_RETURN_STOP_BLOCKS;

    private static final double HOME_RETURN_SPEED = 0.68D;

    private RetoldSmallForagerHomeEvents() {
    }

    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof PathfinderMob animal)) {
            return;
        }

        if (!(animal.level() instanceof ServerLevel level)) {
            return;
        }

        long gameTime = level.getGameTime();

        if (!shouldThink(animal, gameTime)) {
            return;
        }

        if (!isSmallForager(animal)) {
            return;
        }

        RetoldAnimalHomeMemory home = RetoldAnimalHomes.get(animal);

        if (!RetoldAnimalHomes.isValidFor(level, animal, home)) {
            home = tryCreateSmallHome(
                    level,
                    animal,
                    gameTime
            );

            if (!RetoldAnimalHomes.isValidFor(level, animal, home)) {
                return;
            }
        }

        updateHomeReturn(
                level,
                animal,
                home,
                gameTime
        );
    }

    private static boolean shouldThink(
            PathfinderMob animal,
            long gameTime
    ) {
        int offset = Math.floorMod(
                animal.getId(),
                THINK_INTERVAL_TICKS
        );

        return (gameTime + offset) % THINK_INTERVAL_TICKS == 0L;
    }

    private static RetoldAnimalHomeMemory tryCreateSmallHome(
            ServerLevel level,
            PathfinderMob animal,
            long gameTime
    ) {
        if (!canCreateHome(animal)) {
            return null;
        }

        AABB area = animal.getBoundingBox().inflate(HOME_CREATION_RADIUS_BLOCKS);

        List<PathfinderMob> candidates = level.getEntitiesOfClass(
                PathfinderMob.class,
                area,
                candidate -> isHomeCandidate(
                        level,
                        animal,
                        candidate
                )
        );

        if (candidates.isEmpty()) {
            return null;
        }

        candidates.sort(
                Comparator.comparingDouble(candidate -> animal.distanceToSqr(candidate))
        );

        List<PathfinderMob> members = new ArrayList<>();

        for (PathfinderMob candidate : candidates) {
            if (members.size() >= MAX_HOME_MEMBERS - 1) {
                break;
            }

            members.add(candidate);
        }

        return RetoldAnimalHomes.getOrCreatePackHome(
                level,
                animal,
                members,
                calculateHomeCenter(animal, members),
                gameTime
        );
    }

    private static boolean canCreateHome(PathfinderMob animal) {
        if (animal.getTarget() != null && animal.getTarget().isAlive()) {
            return false;
        }

        return RetoldAiControl.getMode(animal) == RetoldAiControlMode.NONE;
    }

    private static boolean isHomeCandidate(
            ServerLevel level,
            PathfinderMob animal,
            PathfinderMob candidate
    ) {
        if (animal == null || candidate == null || animal == candidate) {
            return false;
        }

        if (!isSmallForager(candidate)) {
            return false;
        }

        if (!sameSpecies(animal, candidate)) {
            return false;
        }

        if (animal.level() != candidate.level()) {
            return false;
        }

        if (animal.distanceToSqr(candidate) > HOME_CREATION_RADIUS_SQUARED) {
            return false;
        }

        if (candidate.getTarget() != null && candidate.getTarget().isAlive()) {
            return false;
        }

        RetoldAiControlMode mode = RetoldAiControl.getMode(candidate);

        if (
                mode != RetoldAiControlMode.NONE
                        && !RetoldAiControl.isControlledAsBy(
                        candidate,
                        RetoldAiControlMode.REGROUP,
                        CONTROL_OWNER
                )
        ) {
            return false;
        }

        RetoldAnimalHomeMemory candidateHome = RetoldAnimalHomes.get(candidate);

        return candidateHome == null
                || RetoldAnimalHomes.isValidFor(
                level,
                candidate,
                candidateHome
        );
    }

    private static void updateHomeReturn(
            ServerLevel level,
            PathfinderMob animal,
            RetoldAnimalHomeMemory home,
            long gameTime
    ) {
        RetoldAiControlMode mode = RetoldAiControl.getMode(animal);

        if (!canUseHomeReturn(animal, mode)) {
            return;
        }

        double distanceSquared = animal.blockPosition().distSqr(home.pos());
        boolean shouldRoost = shouldReturnToRoost(
                level,
                animal,
                home
        );
        boolean shouldRest = shouldReturnToRestSite(
                level,
                animal,
                home
        );
        boolean recoveringFromPanic = isRecoveringFromPanic(
                animal,
                gameTime
        );

        if (distanceSquared <= HOME_RETURN_STOP_SQUARED) {
            if (shouldRoost) {
                holdAtHome(
                        animal,
                        gameTime,
                        ROOST_PRIORITY,
                        REASON_ROOSTING,
                        ROOST_CONTROL_TICKS
                );
                return;
            }

            if (shouldRest) {
                holdAtHome(
                        animal,
                        gameTime,
                        REST_PRIORITY,
                        REASON_RESTING,
                        REST_CONTROL_TICKS
                );
                return;
            }

            if (
                    RetoldAiControl.isControlledAsBy(
                            animal,
                            RetoldAiControlMode.REGROUP,
                            CONTROL_OWNER
                    )
                            && (
                            REASON_RETURN_HOME.equals(RetoldAiControl.getReason(animal))
                                    || REASON_ROOSTING.equals(RetoldAiControl.getReason(animal))
                                    || REASON_RESTING.equals(RetoldAiControl.getReason(animal))
                    )
            ) {
                RetoldAiControl.clear(animal);
                animal.getNavigation().stop();
            }

            RetoldAnimalHomes.markUsed(
                    animal,
                    gameTime
            );
            return;
        }

        boolean forcedReturn = shouldRoost || shouldRest;

        if (
                !forcedReturn
                        && mode == RetoldAiControlMode.NONE
                        && distanceSquared < getReturnStartDistanceSquared(recoveringFromPanic)
        ) {
            return;
        }

        int priority = getReturnPriority(
                shouldRoost,
                shouldRest
        );
        String reason = getReturnReason(
                shouldRoost,
                shouldRest
        );

        if (!RetoldAiControl.tryClaim(
                animal,
                RetoldAiControlMode.REGROUP,
                CONTROL_OWNER,
                priority,
                reason,
                gameTime,
                HOME_RETURN_CONTROL_TICKS
        )) {
            return;
        }

        animal.setSprinting(false);

        RetoldAnimalHomes.markUsed(
                animal,
                gameTime
        );

        RetoldAiControl.withNavigationBypass(() -> {
            animal.getNavigation().moveTo(
                    home.pos().getX() + 0.5D,
                    home.pos().getY(),
                    home.pos().getZ() + 0.5D,
                    HOME_RETURN_SPEED
            );
        });
    }

    private static double getReturnStartDistanceSquared(boolean recoveringFromPanic) {
        return recoveringFromPanic
                ? PANIC_RECOVERY_RETURN_START_SQUARED
                : HOME_RETURN_START_SQUARED;
    }

    private static boolean isRecoveringFromPanic(
            PathfinderMob animal,
            long gameTime
    ) {
        RetoldMobState state = RetoldMobStates.get(animal);

        return state != null
                && gameTime - state.lastFleeEndedAt() <= PANIC_RECOVERY_TICKS;
    }

    private static void holdAtHome(
            PathfinderMob animal,
            long gameTime,
            int priority,
            String reason,
            int ticks
    ) {
        if (!RetoldAiControl.tryClaim(
                animal,
                RetoldAiControlMode.REGROUP,
                CONTROL_OWNER,
                priority,
                reason,
                gameTime,
                ticks
        )) {
            return;
        }

        animal.setSprinting(false);
        animal.getNavigation().stop();

        RetoldAnimalHomes.markUsed(
                animal,
                gameTime
        );
    }

    private static boolean shouldReturnToRoost(
            ServerLevel level,
            PathfinderMob animal,
            RetoldAnimalHomeMemory home
    ) {
        if (home.type() != RetoldAnimalHomeType.ROOST) {
            return false;
        }

        if (!RetoldMobRules.getEntityTypePath(animal.getType()).equals("chicken")) {
            return false;
        }

        return isNight(level)
                || level.isRainingAt(animal.blockPosition());
    }

    private static boolean shouldReturnToRestSite(
            ServerLevel level,
            PathfinderMob animal,
            RetoldAnimalHomeMemory home
    ) {
        if (home.type() != RetoldAnimalHomeType.FORAGING_RANGE) {
            return false;
        }

        if (!RetoldMobRules.getEntityTypePath(animal.getType()).equals("pig")) {
            return false;
        }

        return isNight(level)
                || level.isRainingAt(animal.blockPosition());
    }

    private static int getReturnPriority(
            boolean shouldRoost,
            boolean shouldRest
    ) {
        if (shouldRoost) {
            return ROOST_PRIORITY;
        }

        if (shouldRest) {
            return REST_PRIORITY;
        }

        return HOME_RETURN_PRIORITY;
    }

    private static String getReturnReason(
            boolean shouldRoost,
            boolean shouldRest
    ) {
        if (shouldRoost) {
            return REASON_ROOSTING;
        }

        if (shouldRest) {
            return REASON_RESTING;
        }

        return REASON_RETURN_HOME;
    }

    private static boolean isNight(ServerLevel level) {
        long dayTime = Math.floorMod(
                level.getOverworldClockTime(),
                24000L
        );

        return dayTime >= 12500L && dayTime <= 23500L;
    }

    private static boolean canUseHomeReturn(
            PathfinderMob animal,
            RetoldAiControlMode mode
    ) {
        if (animal.getTarget() != null && animal.getTarget().isAlive()) {
            return false;
        }

        if (mode == RetoldAiControlMode.NONE) {
            return true;
        }

        return RetoldAiControl.isControlledAsBy(
                animal,
                RetoldAiControlMode.REGROUP,
                CONTROL_OWNER
        );
    }

    private static BlockPos calculateHomeCenter(
            PathfinderMob animal,
            List<PathfinderMob> members
    ) {
        double x = animal.getX();
        double y = animal.getY();
        double z = animal.getZ();
        int count = 1;

        for (PathfinderMob member : members) {
            x += member.getX();
            y += member.getY();
            z += member.getZ();
            count++;
        }

        return new BlockPos(
                (int) Math.floor(x / count),
                (int) Math.floor(y / count),
                (int) Math.floor(z / count)
        ).immutable();
    }

    private static boolean isSmallForager(PathfinderMob mob) {
        return mob != null
                && mob.isAlive()
                && !mob.isRemoved()
                && RetoldMobRules.profileType(mob) == RetoldMobProfileType.SMALL_FORAGER;
    }

    private static boolean sameSpecies(
            PathfinderMob first,
            PathfinderMob second
    ) {
        return RetoldMobRules.getEntityTypePath(first.getType())
                .equals(RetoldMobRules.getEntityTypePath(second.getType()));
    }
}
