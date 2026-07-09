package cz.xefensor.retold.behavior;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.List;

public final class RetoldControlledRegroupEvents {
    private static final int REGROUP_THINK_INTERVAL_TICKS = 30;
    private static final int REGROUP_CONTROL_TICKS = 20 * 5;

    private static final double HERD_SEARCH_RADIUS_BLOCKS = 18.0D;
    private static final double HERD_SEARCH_RADIUS_SQUARED =
            HERD_SEARCH_RADIUS_BLOCKS * HERD_SEARCH_RADIUS_BLOCKS;

    private static final double LOOSE_GROUP_SEARCH_RADIUS_BLOCKS = 12.0D;
    private static final double LOOSE_GROUP_SEARCH_RADIUS_SQUARED =
            LOOSE_GROUP_SEARCH_RADIUS_BLOCKS * LOOSE_GROUP_SEARCH_RADIUS_BLOCKS;

    private static final double REGROUP_STOP_DISTANCE_BLOCKS = 4.5D;
    private static final double REGROUP_STOP_DISTANCE_SQUARED =
            REGROUP_STOP_DISTANCE_BLOCKS * REGROUP_STOP_DISTANCE_BLOCKS;

    private static final double HERD_REGROUP_START_DISTANCE_BLOCKS = 8.0D;
    private static final double HERD_REGROUP_START_DISTANCE_SQUARED =
            HERD_REGROUP_START_DISTANCE_BLOCKS * HERD_REGROUP_START_DISTANCE_BLOCKS;

    private static final double LOOSE_REGROUP_START_DISTANCE_BLOCKS = 6.0D;
    private static final double LOOSE_REGROUP_START_DISTANCE_SQUARED =
            LOOSE_REGROUP_START_DISTANCE_BLOCKS * LOOSE_REGROUP_START_DISTANCE_BLOCKS;
    private static final double FEAR_REGROUP_START_BONUS_BLOCKS = 5.0D;

    private static final double PREDATOR_PRESSURE_RADIUS_BLOCKS = 20.0D;
    private static final double PREDATOR_PRESSURE_RADIUS_SQUARED =
            PREDATOR_PRESSURE_RADIUS_BLOCKS * PREDATOR_PRESSURE_RADIUS_BLOCKS;

    private static final double HERD_REGROUP_SPEED = 0.82D;
    private static final double LOOSE_REGROUP_SPEED = 0.72D;
    private static final double FEAR_REGROUP_SPEED_BONUS = 0.12D;

    private static final int MIN_HERD_NEIGHBORS = 2;
    private static final int MIN_LOOSE_NEIGHBORS = 1;

    private RetoldControlledRegroupEvents() {
    }

    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof PathfinderMob animal)) {
            return;
        }

        if (!(animal.level() instanceof ServerLevel level)) {
            return;
        }

        if (!isRegroupAnimal(animal)) {
            return;
        }

        long gameTime = level.getGameTime();

        if (!shouldThink(animal, gameTime)) {
            return;
        }

        if (hasPredatorPressure(level, animal, gameTime)) {
            if (RetoldAiControl.isControlledAs(animal, RetoldAiControlMode.REGROUP)) {
                stopRegroup(animal);
            }

            return;
        }

        if (!canRegroupNow(animal)) {
            return;
        }

        GroupInfo groupInfo = findGroupCenter(
                level,
                animal
        );

        if (!groupInfo.hasEnoughNeighbors()) {
            if (RetoldAiControl.isControlledAs(animal, RetoldAiControlMode.REGROUP)) {
                stopRegroup(animal);
            }

            return;
        }

        double distanceSquared = animal.position().distanceToSqr(groupInfo.center());

        if (distanceSquared <= REGROUP_STOP_DISTANCE_SQUARED) {
            if (RetoldAiControl.isControlledAs(animal, RetoldAiControlMode.REGROUP)) {
                stopRegroup(animal);
            }

            animal.setSprinting(false);
            return;
        }

        double startDistanceSquared = getRegroupStartDistanceSquared(animal);

        if (
                distanceSquared < startDistanceSquared
                        && !RetoldAiControl.isControlledAs(animal, RetoldAiControlMode.REGROUP)
        ) {
            return;
        }

        regroupToward(
                animal,
                groupInfo.center(),
                gameTime
        );
    }

    private static boolean shouldThink(
            PathfinderMob animal,
            long gameTime
    ) {
        int offset = Math.floorMod(
                animal.getId(),
                REGROUP_THINK_INTERVAL_TICKS
        );

        return (gameTime + offset) % REGROUP_THINK_INTERVAL_TICKS == 0L;
    }

    private static boolean canRegroupNow(PathfinderMob animal) {
        if (animal == null || !animal.isAlive() || animal.isRemoved()) {
            return false;
        }

        RetoldAiControlMode mode = RetoldAiControl.getMode(animal);

        if (mode == RetoldAiControlMode.NONE) {
            return true;
        }

        /*
         * REGROUP may refresh itself.
         * Everything else has higher priority.
         */
        return mode == RetoldAiControlMode.REGROUP;
    }

    private static GroupInfo findGroupCenter(
            ServerLevel level,
            PathfinderMob animal
    ) {
        double searchRadius = isLooseGroupAnimal(animal)
                ? LOOSE_GROUP_SEARCH_RADIUS_BLOCKS
                : HERD_SEARCH_RADIUS_BLOCKS;

        double searchRadiusSquared = isLooseGroupAnimal(animal)
                ? LOOSE_GROUP_SEARCH_RADIUS_SQUARED
                : HERD_SEARCH_RADIUS_SQUARED;

        AABB area = animal.getBoundingBox().inflate(searchRadius);

        List<PathfinderMob> candidates = level.getEntitiesOfClass(
                PathfinderMob.class,
                area,
                candidate -> isValidGroupMember(
                        animal,
                        candidate,
                        searchRadiusSquared
                )
        );

        if (candidates.isEmpty()) {
            return GroupInfo.empty();
        }

        double x = 0.0D;
        double y = 0.0D;
        double z = 0.0D;
        int count = 0;

        for (PathfinderMob candidate : candidates) {
            x += candidate.getX();
            y += candidate.getY();
            z += candidate.getZ();
            count++;
        }

        if (count <= 0) {
            return GroupInfo.empty();
        }

        Vec3 center = new Vec3(
                x / count,
                y / count,
                z / count
        );

        int requiredNeighbors = isLooseGroupAnimal(animal)
                ? MIN_LOOSE_NEIGHBORS
                : MIN_HERD_NEIGHBORS;

        return new GroupInfo(
                center,
                count,
                requiredNeighbors
        );
    }

    private static boolean isValidGroupMember(
            PathfinderMob animal,
            PathfinderMob candidate,
            double searchRadiusSquared
    ) {
        if (animal == null || candidate == null) {
            return false;
        }

        if (animal == candidate) {
            return false;
        }

        if (!candidate.isAlive() || candidate.isRemoved()) {
            return false;
        }

        if (animal.level() != candidate.level()) {
            return false;
        }

        if (animal.distanceToSqr(candidate) > searchRadiusSquared) {
            return false;
        }

        if (!isCompatibleGroupMember(animal, candidate)) {
            return false;
        }

        RetoldAiControlMode candidateMode = RetoldAiControl.getMode(candidate);

        /*
         * Do not regroup toward mobs that are still fleeing.
         */
        return candidateMode != RetoldAiControlMode.FLEE
                && candidateMode != RetoldAiControlMode.HUNT
                && candidateMode != RetoldAiControlMode.ATTACK;
    }

    private static boolean isCompatibleGroupMember(
            PathfinderMob animal,
            PathfinderMob candidate
    ) {
        String animalPath = getPath(animal);
        String candidatePath = getPath(candidate);

        if (animalPath.equals(candidatePath)) {
            return true;
        }

        if (isLooseGroupAnimal(animal) || isLooseGroupAnimal(candidate)) {
            return false;
        }

        /*
         * Large passive animals can loosely gather with other herd animals.
         * This keeps fields from looking permanently scattered after panic.
         */
        return isHerdAnimalPath(animalPath)
                && isHerdAnimalPath(candidatePath);
    }

    private static boolean hasPredatorPressure(
            ServerLevel level,
            PathfinderMob animal,
            long gameTime
    ) {
        AABB area = animal.getBoundingBox().inflate(PREDATOR_PRESSURE_RADIUS_BLOCKS);

        List<PathfinderMob> predators = level.getEntitiesOfClass(
                PathfinderMob.class,
                area,
                predator -> isPredatorPressure(
                        animal,
                        predator,
                        gameTime
                )
        );

        return !predators.isEmpty();
    }

    private static boolean isPredatorPressure(
            PathfinderMob animal,
            PathfinderMob predator,
            long gameTime
    ) {
        if (animal == null || predator == null) {
            return false;
        }

        if (animal == predator) {
            return false;
        }

        if (!predator.isAlive() || predator.isRemoved()) {
            return false;
        }

        if (animal.level() != predator.level()) {
            return false;
        }

        if (animal.distanceToSqr(predator) > PREDATOR_PRESSURE_RADIUS_SQUARED) {
            return false;
        }

        if (!RetoldMobRules.isManagedPredator(predator)) {
            return false;
        }

        if (!RetoldMobRules.canHuntPrey(predator, animal, gameTime)) {
            return false;
        }

        if (predator.getTarget() == animal) {
            return true;
        }

        if (RetoldAiControl.isControlledAs(predator, RetoldAiControlMode.HUNT)) {
            return true;
        }

        RetoldMobState predatorState = RetoldMobStates.get(predator);

        return predatorState != null
                && RetoldMobRules.hasHuntDrive(predator, predatorState)
                && animal.distanceToSqr(predator) <= 81.0D;
    }

    private static void regroupToward(
            PathfinderMob animal,
            Vec3 center,
            long gameTime
    ) {
        RetoldAiControl.refresh(
                animal,
                RetoldAiControlMode.REGROUP,
                gameTime,
                REGROUP_CONTROL_TICKS
        );

        animal.setSprinting(false);

        double baseSpeed = isLooseGroupAnimal(animal)
                ? LOOSE_REGROUP_SPEED
                : HERD_REGROUP_SPEED;
        double speed = baseSpeed + getSocialPressure(animal) * FEAR_REGROUP_SPEED_BONUS;

        RetoldAiControl.withNavigationBypass(() -> {
            animal.getNavigation().moveTo(
                    center.x,
                    center.y,
                    center.z,
                    speed
            );
        });
    }

    private static void stopRegroup(PathfinderMob animal) {
        RetoldAiControl.clear(animal);
        animal.setSprinting(false);
        animal.getNavigation().stop();
    }

    private static boolean isRegroupAnimal(PathfinderMob mob) {
        String path = getPath(mob);

        return isHerdAnimalPath(path)
                || isLooseGroupAnimalPath(path);
    }

    private static boolean isLooseGroupAnimal(PathfinderMob mob) {
        return isLooseGroupAnimalPath(
                getPath(mob)
        );
    }

    private static double getRegroupStartDistanceSquared(PathfinderMob animal) {
        double baseDistance = isLooseGroupAnimal(animal)
                ? LOOSE_REGROUP_START_DISTANCE_BLOCKS
                : HERD_REGROUP_START_DISTANCE_BLOCKS;

        double distance = baseDistance
                + getSocialPressure(animal) * FEAR_REGROUP_START_BONUS_BLOCKS;

        return distance * distance;
    }

    private static double getSocialPressure(PathfinderMob animal) {
        RetoldMobState state = RetoldMobStates.get(animal);

        if (state == null) {
            return 0.0D;
        }

        double stressPressure = Math.max(
                0.0D,
                (state.stress() - 10.0D) / 90.0D
        );
        double confidencePressure = Math.max(
                0.0D,
                (55.0D - state.confidence()) / 55.0D
        );

        return clamp(
                stressPressure * 0.55D + confidencePressure * 0.55D,
                0.0D,
                1.0D
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

    private static boolean isLooseGroupAnimalPath(String path) {
        return path.equals("pig")
                || path.equals("chicken")
                || path.equals("rabbit");
    }

    private static boolean isHerdAnimalPath(String path) {
        return path.equals("cow")
                || path.equals("sheep")
                || path.equals("goat")
                || path.equals("horse")
                || path.equals("donkey")
                || path.equals("mule")
                || path.equals("llama")
                || path.equals("trader_llama")
                || path.equals("camel");
    }

    private static String getPath(PathfinderMob mob) {
        return RetoldMobRules.getEntityTypePath(
                mob.getType()
        );
    }

    private record GroupInfo(
            Vec3 center,
            int neighbors,
            int requiredNeighbors
    ) {
        public static GroupInfo empty() {
            return new GroupInfo(
                    Vec3.ZERO,
                    0,
                    1
            );
        }

        public boolean hasEnoughNeighbors() {
            return neighbors >= requiredNeighbors;
        }
    }
}
