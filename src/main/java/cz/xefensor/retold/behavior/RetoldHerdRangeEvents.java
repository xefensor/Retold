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

public final class RetoldHerdRangeEvents {
    private static final RetoldAiControlOwner CONTROL_OWNER = RetoldAiControlOwner.REGROUP;
    private static final String REASON_RETURN_RANGE = "return_herd_range";

    private static final int THINK_INTERVAL_TICKS = 40;
    private static final int RANGE_RETURN_CONTROL_TICKS = 20 * 6;
    private static final int RANGE_RETURN_PRIORITY = 18;
    private static final int MAX_RANGE_MEMBERS = 8;
    private static final int PANIC_RECOVERY_TICKS = 20 * 18;

    private static final double RANGE_CREATION_RADIUS_BLOCKS = 18.0D;
    private static final double RANGE_CREATION_RADIUS_SQUARED =
            RANGE_CREATION_RADIUS_BLOCKS * RANGE_CREATION_RADIUS_BLOCKS;

    private static final double RANGE_RETURN_START_BLOCKS = 26.0D;
    private static final double RANGE_RETURN_START_SQUARED =
            RANGE_RETURN_START_BLOCKS * RANGE_RETURN_START_BLOCKS;

    private static final double PANIC_RECOVERY_RETURN_START_BLOCKS = 11.0D;
    private static final double PANIC_RECOVERY_RETURN_START_SQUARED =
            PANIC_RECOVERY_RETURN_START_BLOCKS * PANIC_RECOVERY_RETURN_START_BLOCKS;

    private static final double RANGE_RETURN_STOP_BLOCKS = 9.0D;
    private static final double RANGE_RETURN_STOP_SQUARED =
            RANGE_RETURN_STOP_BLOCKS * RANGE_RETURN_STOP_BLOCKS;

    private static final double RANGE_RETURN_SPEED = 0.72D;

    private RetoldHerdRangeEvents() {
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

        if (!isGrazer(animal)) {
            return;
        }

        RetoldAnimalHomeMemory range = RetoldAnimalHomes.get(animal);

        if (!RetoldAnimalHomes.isValidFor(level, animal, range)) {
            range = tryCreateHerdRange(
                    level,
                    animal,
                    gameTime
            );

            if (!RetoldAnimalHomes.isValidFor(level, animal, range)) {
                return;
            }
        }

        updateRangeReturn(
                animal,
                range,
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

    private static RetoldAnimalHomeMemory tryCreateHerdRange(
            ServerLevel level,
            PathfinderMob animal,
            long gameTime
    ) {
        if (!canCreateRange(animal)) {
            return null;
        }

        AABB area = animal.getBoundingBox().inflate(RANGE_CREATION_RADIUS_BLOCKS);

        List<PathfinderMob> candidates = level.getEntitiesOfClass(
                PathfinderMob.class,
                area,
                candidate -> isRangeCandidate(
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
            if (members.size() >= MAX_RANGE_MEMBERS - 1) {
                break;
            }

            members.add(candidate);
        }

        return RetoldAnimalHomes.getOrCreatePackHome(
                level,
                animal,
                members,
                calculateRangeCenter(animal, members),
                gameTime
        );
    }

    private static boolean canCreateRange(PathfinderMob animal) {
        if (animal.getTarget() != null && animal.getTarget().isAlive()) {
            return false;
        }

        return RetoldAiControl.getMode(animal) == RetoldAiControlMode.NONE;
    }

    private static boolean isRangeCandidate(
            ServerLevel level,
            PathfinderMob animal,
            PathfinderMob candidate
    ) {
        if (animal == null || candidate == null || animal == candidate) {
            return false;
        }

        if (!isGrazer(candidate)) {
            return false;
        }

        if (!sameHerdGroup(animal, candidate)) {
            return false;
        }

        if (!candidate.isAlive() || candidate.isRemoved()) {
            return false;
        }

        if (animal.level() != candidate.level()) {
            return false;
        }

        if (animal.distanceToSqr(candidate) > RANGE_CREATION_RADIUS_SQUARED) {
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

        RetoldAnimalHomeMemory candidateRange = RetoldAnimalHomes.get(candidate);

        return candidateRange == null
                || RetoldAnimalHomes.isValidFor(
                level,
                candidate,
                candidateRange
        );
    }

    private static void updateRangeReturn(
            PathfinderMob animal,
            RetoldAnimalHomeMemory range,
            long gameTime
    ) {
        RetoldAiControlMode mode = RetoldAiControl.getMode(animal);

        if (!canUseRangeReturn(animal, mode)) {
            return;
        }

        double distanceSquared = animal.blockPosition().distSqr(range.pos());
        boolean recoveringFromPanic = isRecoveringFromPanic(
                animal,
                gameTime
        );

        if (distanceSquared <= RANGE_RETURN_STOP_SQUARED) {
            if (
                    RetoldAiControl.isControlledAsBy(
                            animal,
                            RetoldAiControlMode.REGROUP,
                            CONTROL_OWNER
                    )
                            && REASON_RETURN_RANGE.equals(RetoldAiControl.getReason(animal))
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

        if (
                mode == RetoldAiControlMode.NONE
                        && distanceSquared < getReturnStartDistanceSquared(recoveringFromPanic)
        ) {
            return;
        }

        if (!RetoldAiControl.tryClaim(
                animal,
                RetoldAiControlMode.REGROUP,
                CONTROL_OWNER,
                RANGE_RETURN_PRIORITY,
                REASON_RETURN_RANGE,
                gameTime,
                RANGE_RETURN_CONTROL_TICKS
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
                    range.pos().getX() + 0.5D,
                    range.pos().getY(),
                    range.pos().getZ() + 0.5D,
                    RANGE_RETURN_SPEED
            );
        });
    }

    private static double getReturnStartDistanceSquared(boolean recoveringFromPanic) {
        return recoveringFromPanic
                ? PANIC_RECOVERY_RETURN_START_SQUARED
                : RANGE_RETURN_START_SQUARED;
    }

    private static boolean isRecoveringFromPanic(
            PathfinderMob animal,
            long gameTime
    ) {
        RetoldMobState state = RetoldMobStates.get(animal);

        return state != null
                && gameTime - state.lastFleeEndedAt() <= PANIC_RECOVERY_TICKS;
    }

    private static boolean canUseRangeReturn(
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

    private static BlockPos calculateRangeCenter(
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

    private static boolean isGrazer(PathfinderMob mob) {
        return mob != null
                && mob.isAlive()
                && !mob.isRemoved()
                && RetoldMobRules.profileType(mob) == RetoldMobProfileType.HUNGRY_GRAZER;
    }

    private static boolean sameHerdGroup(
            PathfinderMob first,
            PathfinderMob second
    ) {
        return herdGroup(first).equals(
                herdGroup(second)
        );
    }

    private static String herdGroup(PathfinderMob mob) {
        String path = RetoldMobRules.getEntityTypePath(
                mob.getType()
        );

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
}
