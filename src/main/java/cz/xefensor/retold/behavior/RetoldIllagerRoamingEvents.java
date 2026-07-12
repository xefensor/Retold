package cz.xefensor.retold.behavior;

import cz.xefensor.retold.territory.RetoldTerritoryConfig;
import cz.xefensor.retold.territory.RetoldTerritoryConfigs;
import cz.xefensor.retold.territory.RetoldTerritoryDetector;
import cz.xefensor.retold.territory.RetoldTerritoryMobStates;
import cz.xefensor.retold.territory.RetoldTerritoryRules;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class RetoldIllagerRoamingEvents {
    private static final Map<PathfinderMob, RoamMemory> ROAM_MEMORIES = new WeakHashMap<>();

    private static final int THINK_INTERVAL_TICKS = 30;
    private static final int ROAM_SCAN_CACHE_TICKS = 10;
    private static final int ROAM_CONTROL_TICKS = 20 * 8;
    private static final int ROAM_PRIORITY = RetoldAiPriorities.above(RetoldAiPriorities.REGROUP, 4);

    private static final double ROAM_SEGMENT_BLOCKS = 30.0D;
    private static final double ROAM_SIDE_WOBBLE_BLOCKS = 7.0D;
    private static final double ROAM_REACHED_DISTANCE_BLOCKS = 4.0D;
    private static final double ROAM_REACHED_DISTANCE_SQUARED =
            ROAM_REACHED_DISTANCE_BLOCKS * ROAM_REACHED_DISTANCE_BLOCKS;

    private static final double VILLAGE_ENTITY_SIGNAL_RADIUS_BLOCKS = 30.0D;
    private static final double VILLAGE_ENTITY_SIGNAL_RADIUS_SQUARED =
            VILLAGE_ENTITY_SIGNAL_RADIUS_BLOCKS * VILLAGE_ENTITY_SIGNAL_RADIUS_BLOCKS;

    private static final int BELL_SIGNAL_HORIZONTAL_RADIUS = 14;
    private static final int BELL_SIGNAL_VERTICAL_RADIUS = 5;

    private static final double ROAM_SPEED = 0.72D;

    private RetoldIllagerRoamingEvents() {
    }

    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof PathfinderMob raider)) {
            return;
        }

        if (!(raider.level() instanceof ServerLevel level)) {
            return;
        }

        if (!isRoamingRaider(raider)) {
            ROAM_MEMORIES.remove(raider);
            return;
        }

        long gameTime = level.getGameTime();

        if (!shouldThink(raider, gameTime)) {
            return;
        }

        if (shouldYieldRoam(level, raider, gameTime)) {
            stopRoamingIfOwned(raider);
            return;
        }

        RoamMemory memory = ROAM_MEMORIES.get(raider);

        if (memory == null || hasReachedRoamTarget(raider, memory)) {
            memory = createNextRoamMemory(raider);
            ROAM_MEMORIES.put(
                    raider,
                    memory
            );
        }

        roamToward(
                raider,
                memory,
                gameTime
        );
    }

    private static boolean isRoamingRaider(PathfinderMob mob) {
        return RetoldMobRules.isIllagerRaider(mob)
                && !RetoldMobRules.isVex(mob);
    }

    private static boolean shouldThink(
            PathfinderMob mob,
            long gameTime
    ) {
        return RetoldBehaviorTiming.shouldThink(
                mob,
                gameTime,
                THINK_INTERVAL_TICKS
        );
    }

    private static boolean shouldYieldRoam(
            ServerLevel level,
            PathfinderMob raider,
            long gameTime
    ) {
        if (!raider.isAlive() || raider.isRemoved()) {
            return true;
        }

        if (RetoldBehaviorCoordinator.hasLiveTarget(raider)) {
            return true;
        }

        RetoldAiControlMode mode = RetoldAiControl.getMode(raider);

        if (
                mode != RetoldAiControlMode.NONE
                        && !RetoldAiControl.isControlledAsBy(
                        raider,
                        RetoldAiControlMode.SEARCH,
                        RetoldAiControlOwner.RAIDER_ROAM
                )
        ) {
            return true;
        }

        if (RetoldTerritoryRules.isInActiveRaid(level, raider)) {
            return true;
        }

        if (RetoldTerritoryMobStates.get(raider) != null) {
            return true;
        }

        RetoldTerritoryConfig config = RetoldTerritoryConfigs.getForEntity(raider);

        if (
                config != null
                        && RetoldTerritoryDetector.isNearTerritory(
                        level,
                        raider,
                        config,
                        gameTime
                )
        ) {
            return true;
        }

        return hasVillageSignalNearby(
                level,
                raider
        );
    }

    private static boolean hasVillageSignalNearby(
            ServerLevel level,
            PathfinderMob raider
    ) {
        List<LivingEntity> candidates = RetoldAiScanCache.nearby(
                level,
                raider,
                LivingEntity.class,
                VILLAGE_ENTITY_SIGNAL_RADIUS_BLOCKS,
                level.getGameTime(),
                ROAM_SCAN_CACHE_TICKS
        );

        for (LivingEntity candidate : candidates) {
            if (isVillageSignalEntity(raider, candidate)) {
                return true;
            }
        }

        return hasBellNearby(
                level,
                raider.blockPosition()
        );
    }

    private static boolean isVillageSignalEntity(
            PathfinderMob raider,
            LivingEntity candidate
    ) {
        if (candidate == null || candidate == raider) {
            return false;
        }

        if (!RetoldBehaviorCoordinator.isAliveInSameLevel(raider, candidate)) {
            return false;
        }

        if (raider.distanceToSqr(candidate) > VILLAGE_ENTITY_SIGNAL_RADIUS_SQUARED) {
            return false;
        }

        String path = RetoldMobRules.getEntityTypePath(candidate.getType());

        return path.equals("villager")
                || path.equals("iron_golem")
                || path.equals("snow_golem")
                || path.equals("wandering_trader");
    }

    private static boolean hasBellNearby(
            ServerLevel level,
            BlockPos center
    ) {
        for (int dx = -BELL_SIGNAL_HORIZONTAL_RADIUS; dx <= BELL_SIGNAL_HORIZONTAL_RADIUS; dx += 2) {
            for (int dz = -BELL_SIGNAL_HORIZONTAL_RADIUS; dz <= BELL_SIGNAL_HORIZONTAL_RADIUS; dz += 2) {
                for (int dy = -BELL_SIGNAL_VERTICAL_RADIUS; dy <= BELL_SIGNAL_VERTICAL_RADIUS; dy++) {
                    BlockPos pos = center.offset(dx, dy, dz);

                    if (level.getBlockState(pos).is(Blocks.BELL)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static RoamMemory createNextRoamMemory(PathfinderMob raider) {
        Vec3 heading = randomHorizontalDirection(raider);
        Vec3 side = new Vec3(
                -heading.z,
                0.0D,
                heading.x
        );

        double sideOffset = (raider.getRandom().nextDouble() - 0.5D) * ROAM_SIDE_WOBBLE_BLOCKS;
        Vec3 destination = raider.position()
                .add(heading.scale(ROAM_SEGMENT_BLOCKS))
                .add(side.scale(sideOffset));

        return new RoamMemory(
                new BlockPos(
                        (int) Math.floor(destination.x),
                        raider.blockPosition().getY(),
                        (int) Math.floor(destination.z)
                )
        );
    }

    private static boolean hasReachedRoamTarget(
            PathfinderMob raider,
            RoamMemory memory
    ) {
        return memory == null
                || raider.blockPosition().distSqr(memory.target()) <= ROAM_REACHED_DISTANCE_SQUARED;
    }

    private static void roamToward(
            PathfinderMob raider,
            RoamMemory memory,
            long gameTime
    ) {
        if (memory == null) {
            return;
        }

        RetoldBehaviorMovement.claimAndMoveToBlock(
                raider,
                memory.target(),
                RetoldAiControlMode.SEARCH,
                RetoldAiControlOwner.RAIDER_ROAM,
                ROAM_PRIORITY,
                "illager_roam",
                gameTime,
                ROAM_CONTROL_TICKS,
                ROAM_SPEED,
                false
        );
    }

    private static void stopRoamingIfOwned(PathfinderMob raider) {
        ROAM_MEMORIES.remove(raider);

        if (
                RetoldAiControl.isControlledAsBy(
                        raider,
                        RetoldAiControlMode.SEARCH,
                        RetoldAiControlOwner.RAIDER_ROAM
                )
        ) {
            RetoldAiControl.clearIfOwnedBy(
                    raider,
                    RetoldAiControlOwner.RAIDER_ROAM
            );
            raider.getNavigation().stop();
        }
    }

    private static Vec3 randomHorizontalDirection(PathfinderMob mob) {
        double angle = mob.getRandom().nextDouble() * Math.PI * 2.0D;

        return new Vec3(
                Math.cos(angle),
                0.0D,
                Math.sin(angle)
        );
    }

    private record RoamMemory(BlockPos target) {
    }
}
