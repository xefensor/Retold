package cz.xefensor.retold.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class RetoldSnifferForagerEvents {
    private static final Map<PathfinderMob, SnifferMemory> MEMORIES = new WeakHashMap<>();

    private static final int THINK_INTERVAL_TICKS = 20;
    private static final int SNIFFER_SCAN_CACHE_TICKS = 10;
    private static final int SNIFFER_BLOCK_SEARCH_CACHE_TICKS = 45;
    private static final int SEARCH_CONTROL_TICKS = 20 * 7;
    private static final int RETURN_CONTROL_TICKS = 20 * 7;

    private static final int SEARCH_PRIORITY = RetoldAiPriorities.below(RetoldAiPriorities.SEARCH, 2);
    private static final int RETURN_PRIORITY = RetoldAiPriorities.above(RetoldAiPriorities.REGROUP, 4);

    private static final int RANGE_SEARCH_HORIZONTAL_RADIUS = 18;
    private static final int RANGE_SEARCH_VERTICAL_RADIUS = 4;
    private static final double RANGE_MEMBER_SEARCH_RADIUS_BLOCKS = 18.0D;

    private static final int DIGGABLE_SEARCH_HORIZONTAL_RADIUS = 14;
    private static final int DIGGABLE_SEARCH_VERTICAL_RADIUS = 3;

    private static final int RECENT_DANGER_RETURN_TICKS = 20 * 45;
    private static final int FORAGE_COOLDOWN_TICKS = 20 * 28;

    private static final double FAR_FROM_RANGE_BLOCKS = 36.0D;
    private static final double FAR_FROM_RANGE_SQUARED =
            FAR_FROM_RANGE_BLOCKS * FAR_FROM_RANGE_BLOCKS;

    private static final double RANGE_REACHED_BLOCKS = 7.0D;
    private static final double RANGE_REACHED_SQUARED =
            RANGE_REACHED_BLOCKS * RANGE_REACHED_BLOCKS;

    private static final double FORAGE_POINT_REACHED_BLOCKS = 3.0D;
    private static final double FORAGE_POINT_REACHED_SQUARED =
            FORAGE_POINT_REACHED_BLOCKS * FORAGE_POINT_REACHED_BLOCKS;

    private static final int FORAGE_RELIEF = 12;

    private static final double SNIFFER_SEARCH_SPEED = 0.62D;
    private static final double SNIFFER_RETURN_SPEED = 0.66D;

    private RetoldSnifferForagerEvents() {
    }

    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof PathfinderMob sniffer)) {
            return;
        }

        if (!(sniffer.level() instanceof ServerLevel level)) {
            return;
        }

        if (!RetoldMobRules.isSnifferForager(sniffer)) {
            MEMORIES.remove(sniffer);
            return;
        }

        long gameTime = level.getGameTime();

        if (!RetoldBehaviorTiming.shouldThink(
                sniffer,
                gameTime,
                THINK_INTERVAL_TICKS
        )) {
            return;
        }

        handleSniffer(
                level,
                sniffer,
                gameTime
        );
    }

    private static void handleSniffer(
            ServerLevel level,
            PathfinderMob sniffer,
            long gameTime
    ) {
        RetoldAnimalHomeMemory range = getOrCreateRange(
                level,
                sniffer,
                gameTime
        );

        LivingEntity threat = findThreat(sniffer);

        if (threat != null) {
            rememberDanger(
                    sniffer,
                    gameTime
            );

            if (range != null) {
                returnToRange(
                        sniffer,
                        range.pos(),
                        gameTime,
                        "sniffer_danger_return"
                );
            }

            return;
        }

        if (range == null) {
            return;
        }

        if (shouldReturnToRange(level, sniffer, range, gameTime)) {
            returnToRange(
                    sniffer,
                    range.pos(),
                    gameTime,
                    "sniffer_range_return"
            );
            return;
        }

        if (RetoldAiControl.isControlledAsBy(
                sniffer,
                RetoldAiControlMode.SEARCH,
                RetoldAiControlOwner.SNIFFER_FORAGER
        )) {
            continueForageSearch(
                    level,
                    sniffer,
                    gameTime
            );
            return;
        }

        if (shouldForage(sniffer, gameTime)) {
            BlockPos diggable = findForagePoint(
                    level,
                    sniffer,
                    range.pos()
            );

            if (diggable != null) {
                moveToForagePoint(
                        sniffer,
                        diggable,
                        gameTime
                );
            }
        }
    }

    private static RetoldAnimalHomeMemory getOrCreateRange(
            ServerLevel level,
            PathfinderMob sniffer,
            long gameTime
    ) {
        RetoldAnimalHomeMemory existing = RetoldAnimalHomes.get(sniffer);

        if (RetoldAnimalHomes.isValidFor(level, sniffer, existing) && isForagingRange(level, existing.pos())) {
            RetoldAnimalHomes.markUsed(
                    sniffer,
                    gameTime
            );
            return existing;
        }

        BlockPos rangePos = findNearestRangeAnchor(
                level,
                sniffer
        );

        if (rangePos == null) {
            return null;
        }

        return RetoldAnimalHomes.getOrCreatePackHome(
                level,
                sniffer,
                findNearbyRangeMembers(
                        level,
                        sniffer
                ),
                rangePos,
                gameTime
        );
    }

    private static List<PathfinderMob> findNearbyRangeMembers(
            ServerLevel level,
            PathfinderMob sniffer
    ) {
        return RetoldAiScanCache.nearby(
                level,
                sniffer,
                PathfinderMob.class,
                RANGE_MEMBER_SEARCH_RADIUS_BLOCKS,
                level.getGameTime(),
                SNIFFER_SCAN_CACHE_TICKS
        ).stream()
                .filter(
                candidate -> candidate != sniffer
                        && RetoldAnimalSocialGroups.canShareHomeOrRange(
                        sniffer,
                        candidate
                )
        ).toList();
    }

    private static LivingEntity findThreat(PathfinderMob sniffer) {
        LivingEntity attacker = sniffer.getLastHurtByMob();

        if (isValidThreat(sniffer, attacker)) {
            return attacker;
        }

        LivingEntity target = sniffer.getTarget();

        if (isValidThreat(sniffer, target)) {
            return target;
        }

        return null;
    }

    private static boolean isValidThreat(
            PathfinderMob sniffer,
            LivingEntity threat
    ) {
        if (sniffer == null || threat == null || threat == sniffer) {
            return false;
        }

        if (!RetoldBehaviorCoordinator.isAliveInSameLevel(sniffer, threat)) {
            return false;
        }

        return sniffer.distanceToSqr(threat) <= FAR_FROM_RANGE_SQUARED
                || threat == sniffer.getLastHurtByMob();
    }

    private static void rememberDanger(
            PathfinderMob sniffer,
            long gameTime
    ) {
        RetoldMobState state = RetoldMobStates.getOrCreate(
                sniffer,
                gameTime
        );

        state.markDanger(gameTime);
        state.addStress(1);
        state.addConfidence(-1);
    }

    private static boolean shouldReturnToRange(
            ServerLevel level,
            PathfinderMob sniffer,
            RetoldAnimalHomeMemory range,
            long gameTime
    ) {
        if (range == null) {
            return false;
        }

        if (sniffer.blockPosition().distSqr(range.pos()) <= RANGE_REACHED_SQUARED) {
            return false;
        }

        RetoldMobState state = RetoldMobStates.getOrCreate(
                sniffer,
                gameTime
        );

        boolean recentDanger = state.lastDangerAt() > 0L
                && gameTime - state.lastDangerAt() <= RECENT_DANGER_RETURN_TICKS;

        boolean farFromRange = sniffer.blockPosition().distSqr(range.pos()) >= FAR_FROM_RANGE_SQUARED;
        boolean restTime = RetoldAnimalDailyRhythm.isNight(level)
                || level.isRainingAt(sniffer.blockPosition());

        return recentDanger || (farFromRange && restTime);
    }

    private static boolean shouldForage(
            PathfinderMob sniffer,
            long gameTime
    ) {
        if (RetoldAiControl.isControlled(sniffer)) {
            return false;
        }

        SnifferMemory memory = MEMORIES.computeIfAbsent(
                sniffer,
                ignored -> new SnifferMemory()
        );

        if (gameTime - memory.lastForageAt < FORAGE_COOLDOWN_TICKS) {
            return false;
        }

        RetoldMobState state = RetoldMobStates.getOrCreate(
                sniffer,
                gameTime
        );

        return RetoldMobRules.hasEatDrive(
                sniffer,
                state
        )
                || state.stress() > 35;
    }

    private static void continueForageSearch(
            ServerLevel level,
            PathfinderMob sniffer,
            long gameTime
    ) {
        SnifferMemory memory = MEMORIES.get(sniffer);

        if (memory == null || memory.target == null || !isDiggable(level, memory.target)) {
            stopControl(sniffer);
            return;
        }

        if (sniffer.blockPosition().distSqr(memory.target) <= FORAGE_POINT_REACHED_SQUARED) {
            markForaged(
                    sniffer,
                    memory,
                    gameTime
            );
            return;
        }

        moveToForagePoint(
                sniffer,
                memory.target,
                gameTime
        );
    }

    private static void moveToForagePoint(
            PathfinderMob sniffer,
            BlockPos target,
            long gameTime
    ) {
        SnifferMemory memory = MEMORIES.computeIfAbsent(
                sniffer,
                ignored -> new SnifferMemory()
        );

        memory.target = target.immutable();

        RetoldBehaviorMovement.claimAndMoveToBlock(
                sniffer,
                target,
                RetoldAiControlMode.SEARCH,
                RetoldAiControlOwner.SNIFFER_FORAGER,
                SEARCH_PRIORITY,
                "sniffer_foraging_search",
                gameTime,
                SEARCH_CONTROL_TICKS,
                SNIFFER_SEARCH_SPEED,
                false
        );
    }

    private static void markForaged(
            PathfinderMob sniffer,
            SnifferMemory memory,
            long gameTime
    ) {
        memory.lastForageAt = gameTime;
        memory.target = null;

        RetoldMobState state = RetoldMobStates.getOrCreate(
                sniffer,
                gameTime
        );

        state.addHunger(-FORAGE_RELIEF);
        state.markAte(gameTime);
        state.addStress(-1);
        state.addConfidence(1);

        stopControl(sniffer);
    }

    private static void returnToRange(
            PathfinderMob sniffer,
            BlockPos range,
            long gameTime,
            String reason
    ) {
        RetoldBehaviorMovement.claimAndMoveToBlock(
                sniffer,
                range,
                RetoldAiControlMode.REGROUP,
                RetoldAiControlOwner.SNIFFER_FORAGER,
                RETURN_PRIORITY,
                reason,
                gameTime,
                RETURN_CONTROL_TICKS,
                SNIFFER_RETURN_SPEED,
                false
        );
    }

    private static void stopControl(PathfinderMob sniffer) {
        RetoldBehaviorMovement.stopOwnedMovement(
                sniffer,
                RetoldAiControlOwner.SNIFFER_FORAGER
        );
    }

    private static BlockPos findNearestRangeAnchor(
            ServerLevel level,
            PathfinderMob sniffer
    ) {
        return RetoldBlockTargetSearch.findSnifferRangeAnchor(
                level,
                sniffer,
                RANGE_SEARCH_HORIZONTAL_RADIUS,
                RANGE_SEARCH_VERTICAL_RADIUS,
                level.getGameTime(),
                SNIFFER_BLOCK_SEARCH_CACHE_TICKS
        );
    }

    private static BlockPos findForagePoint(
            ServerLevel level,
            PathfinderMob sniffer,
            BlockPos rangeCenter
    ) {
        BlockPos center = rangeCenter == null ? sniffer.blockPosition() : rangeCenter;

        return RetoldBlockTargetSearch.findSnifferDiggable(
                level,
                sniffer,
                center,
                DIGGABLE_SEARCH_HORIZONTAL_RADIUS,
                DIGGABLE_SEARCH_VERTICAL_RADIUS,
                level.getGameTime(),
                SNIFFER_BLOCK_SEARCH_CACHE_TICKS
        );
    }

    private static boolean isForagingRange(
            ServerLevel level,
            BlockPos pos
    ) {
        return isDiggable(level, pos)
                || hasNearbyDiggable(level, pos, 5);
    }

    private static boolean hasNearbyDiggable(
            ServerLevel level,
            BlockPos pos,
            int radius
    ) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    mutable.set(
                            pos.getX() + dx,
                            pos.getY() + dy,
                            pos.getZ() + dz
                    );

                    if (isDiggable(level, mutable)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static boolean isDiggable(
            ServerLevel level,
            BlockPos pos
    ) {
        if (!level.getBlockState(pos.above()).isAir()) {
            return false;
        }

        BlockState state = level.getBlockState(pos);

        return state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.DIRT)
                || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.ROOTED_DIRT)
                || state.is(Blocks.PODZOL)
                || state.is(Blocks.MUD)
                || state.is(Blocks.MOSS_BLOCK)
                || state.is(Blocks.FARMLAND);
    }

    private static final class SnifferMemory {
        private BlockPos target;
        private long lastForageAt;
    }
}
