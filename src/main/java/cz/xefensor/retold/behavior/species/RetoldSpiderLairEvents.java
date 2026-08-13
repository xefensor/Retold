package cz.xefensor.retold.behavior.species;

import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.control.RetoldAiControlMode;
import cz.xefensor.retold.behavior.control.RetoldAiControlOwner;
import cz.xefensor.retold.behavior.control.RetoldAiPriorities;
import cz.xefensor.retold.behavior.core.RetoldBehaviorCoordinator;
import cz.xefensor.retold.behavior.core.RetoldBehaviorMovement;
import cz.xefensor.retold.behavior.core.RetoldMobGriefing;
import cz.xefensor.retold.behavior.home.RetoldAnimalDailyRhythm;
import cz.xefensor.retold.behavior.home.RetoldAnimalHomeMemory;
import cz.xefensor.retold.behavior.home.RetoldAnimalHomeType;
import cz.xefensor.retold.behavior.home.RetoldAnimalHomes;
import cz.xefensor.retold.behavior.home.RetoldAnimalSocialGroups;
import cz.xefensor.retold.behavior.performance.RetoldAiScanCache;
import cz.xefensor.retold.behavior.performance.RetoldBlockTargetSearch;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;
import cz.xefensor.retold.behavior.profiles.RetoldMobState;
import cz.xefensor.retold.behavior.profiles.RetoldMobStates;
import cz.xefensor.retold.registry.RetoldTags;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class RetoldSpiderLairEvents {
    private static final RetoldAiControlOwner CONTROL_OWNER =
            RetoldAiControlOwner.SPIDER_LAIR;
    private static final String REASON_RETURN_TO_LAIR =
            "return_to_spider_lair";

    private static final int MEMBER_SCAN_CACHE_TICKS = 20;
    private static final int WEB_SEARCH_CACHE_TICKS = 40;
    private static final int FAILED_WORK_RETRY_TICKS = 80;
    private static final int SUCCESSFUL_WEB_COOLDOWN_TICKS = 20 * 30;
    private static final int RETURN_CONTROL_TICKS = 20 * 5;
    private static final int RECENT_FEEDING_WINDOW_TICKS = 20 * 120;

    private static final int MAX_LAIR_WEBS = 50;
    private static final int WEB_HORIZONTAL_RADIUS = 3;
    private static final int WEB_VERTICAL_RADIUS = 2;

    private static final double WEB_RADIUS_BLOCKS = 4.0D;
    private static final double WEB_RADIUS_SQUARED =
            WEB_RADIUS_BLOCKS * WEB_RADIUS_BLOCKS;
    private static final double WEAVE_DISTANCE_BLOCKS = 6.0D;
    private static final double WEAVE_DISTANCE_SQUARED =
            WEAVE_DISTANCE_BLOCKS * WEAVE_DISTANCE_BLOCKS;
    private static final double RETURN_STOP_BLOCKS = 4.0D;
    private static final double RETURN_STOP_SQUARED =
            RETURN_STOP_BLOCKS * RETURN_STOP_BLOCKS;
    private static final double RETURN_SPEED = 1.0D;

    private static final Map<ServerLevel, Map<BlockPos, Long>> NEXT_WORK_AT =
            new WeakHashMap<>();

    private RetoldSpiderLairEvents() {
    }

    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof PathfinderMob spider)) {
            return;
        }

        if (!(spider.level() instanceof ServerLevel level)) {
            return;
        }

        tick(
                level,
                spider,
                level.getGameTime()
        );
    }

    static void tick(
            ServerLevel level,
            PathfinderMob spider,
            long gameTime
    ) {
        if (!isUsableSpider(level, spider)) {
            releaseLairMovement(spider);
            return;
        }

        RetoldAnimalHomeMemory home = RetoldAnimalHomes.get(spider);

        if (!isValidLair(level, spider, home)) {
            home = tryAdoptNearbyLair(
                    level,
                    spider,
                    gameTime
            );
        }

        if (!isValidLair(level, spider, home)) {
            if (!tryBuildOrRepairLair(level, spider, gameTime)) {
                releaseLairMovement(spider);
                return;
            }

            home = RetoldAnimalHomes.get(spider);
        }

        if (!isValidLair(level, spider, home)) {
            releaseLairMovement(spider);
            return;
        }

        updateDaylightReturn(
                level,
                spider,
                home,
                gameTime
        );
        tryBuildOrRepairLair(
                level,
                spider,
                gameTime
        );
    }

    static boolean tryBuildOrRepairLair(
            ServerLevel level,
            PathfinderMob spider,
            long gameTime
    ) {
        if (!canWeaveNow(level, spider)) {
            return false;
        }

        RetoldAnimalHomeMemory home = RetoldAnimalHomes.get(spider);
        boolean establishing = !isValidLair(level, spider, home);

        if (establishing && !hasRecentFoodSuccess(spider, gameTime)) {
            return false;
        }

        BlockPos center = establishing
                ? spider.blockPosition().immutable()
                : home.pos();

        if (!establishing
                && spider.blockPosition().distSqr(center) > WEAVE_DISTANCE_SQUARED) {
            return false;
        }

        if (!reserveWorkAttempt(level, center, gameTime)) {
            return false;
        }

        if (!establishing && countCobwebs(level, center) >= MAX_LAIR_WEBS) {
            delayNextWork(
                    level,
                    center,
                    gameTime + SUCCESSFUL_WEB_COOLDOWN_TICKS
            );
            return false;
        }

        BlockPos placement = RetoldBlockTargetSearch.findCobwebPlacement(
                level,
                spider,
                center,
                WEB_HORIZONTAL_RADIUS,
                WEB_VERTICAL_RADIUS,
                WEB_RADIUS_SQUARED,
                gameTime,
                WEB_SEARCH_CACHE_TICKS
        );

        if (placement == null
                || placement.equals(spider.blockPosition())
                || !RetoldBlockTargetSearch.canPlaceCobwebAt(level, placement)) {
            return false;
        }

        if (!level.setBlock(
                placement,
                Blocks.COBWEB.defaultBlockState(),
                Block.UPDATE_ALL
        )) {
            return false;
        }

        if (establishing) {
            List<PathfinderMob> members = findNearbyLairMembers(
                    level,
                    spider,
                    gameTime
            );
            home = RetoldAnimalHomes.getOrCreatePackHome(
                    level,
                    spider,
                    members,
                    placement,
                    gameTime
            );

            if (!isValidLair(level, spider, home)) {
                level.setBlock(
                        placement,
                        Blocks.AIR.defaultBlockState(),
                        Block.UPDATE_ALL
                );
                return false;
            }

            center = home.pos();
        }

        delayNextWork(
                level,
                center,
                gameTime + SUCCESSFUL_WEB_COOLDOWN_TICKS
        );
        return true;
    }

    static int countCobwebs(
            ServerLevel level,
            BlockPos center
    ) {
        if (level == null || center == null) {
            return 0;
        }

        int count = 0;
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int dx = -WEB_HORIZONTAL_RADIUS; dx <= WEB_HORIZONTAL_RADIUS; dx++) {
            for (int dy = -WEB_VERTICAL_RADIUS; dy <= WEB_VERTICAL_RADIUS; dy++) {
                for (int dz = -WEB_HORIZONTAL_RADIUS; dz <= WEB_HORIZONTAL_RADIUS; dz++) {
                    if (dx * dx + dy * dy + dz * dz > WEB_RADIUS_SQUARED) {
                        continue;
                    }

                    mutable.set(
                            center.getX() + dx,
                            center.getY() + dy,
                            center.getZ() + dz
                    );

                    if (level.getBlockState(mutable).is(
                            RetoldTags.SPIDER_LAIR_WEB_BLOCKS
                    )) {
                        count++;
                    }
                }
            }
        }

        return count;
    }

    private static RetoldAnimalHomeMemory tryAdoptNearbyLair(
            ServerLevel level,
            PathfinderMob spider,
            long gameTime
    ) {
        if (!RetoldBehaviorCoordinator.canStartLowPriorityHomeBehavior(spider)) {
            return null;
        }

        List<PathfinderMob> members = findNearbyLairMembers(
                level,
                spider,
                gameTime
        );
        boolean hasLairOwner = members.stream().anyMatch(candidate ->
                isValidLair(
                        level,
                        candidate,
                        RetoldAnimalHomes.get(candidate)
                )
        );

        if (!hasLairOwner) {
            return null;
        }

        return RetoldAnimalHomes.getOrCreatePackHome(
                level,
                spider,
                members,
                spider.blockPosition(),
                gameTime
        );
    }

    private static List<PathfinderMob> findNearbyLairMembers(
            ServerLevel level,
            PathfinderMob spider,
            long gameTime
    ) {
        double radius = RetoldAnimalSocialGroups.homeSeparationBlocks(spider);
        List<PathfinderMob> members = new ArrayList<>(RetoldAiScanCache.nearby(
                level,
                spider,
                PathfinderMob.class,
                radius,
                gameTime,
                MEMBER_SCAN_CACHE_TICKS
        ));
        members.removeIf(candidate ->
                candidate == spider
                        || !RetoldAnimalSocialGroups.canShareHomeOrRange(
                        spider,
                        candidate
                )
        );
        members.sort(
                Comparator
                        .<PathfinderMob>comparingDouble(spider::distanceToSqr)
                        .thenComparingInt(PathfinderMob::getId)
        );
        return members;
    }

    private static void updateDaylightReturn(
            ServerLevel level,
            PathfinderMob spider,
            RetoldAnimalHomeMemory home,
            long gameTime
    ) {
        if (RetoldAnimalDailyRhythm.isNight(level)
                || RetoldBehaviorCoordinator.hasLiveTarget(spider)) {
            releaseLairMovement(spider);
            return;
        }

        RetoldAiControlMode mode = RetoldAiControl.getMode(spider);

        if (mode != RetoldAiControlMode.NONE
                && !RetoldAiControl.isControlledAsBy(
                spider,
                RetoldAiControlMode.REGROUP,
                CONTROL_OWNER
        )) {
            return;
        }

        double distanceSquared = spider.blockPosition().distSqr(home.pos());

        if (distanceSquared <= RETURN_STOP_SQUARED) {
            releaseLairMovement(spider);
            RetoldAnimalHomes.markUsed(spider, gameTime);
            return;
        }

        RetoldBehaviorMovement.claimAndMoveToBlock(
                spider,
                home.pos(),
                RetoldAiControlMode.REGROUP,
                CONTROL_OWNER,
                RetoldAiPriorities.REST,
                REASON_RETURN_TO_LAIR,
                gameTime,
                RETURN_CONTROL_TICKS,
                RETURN_SPEED,
                false
        );
    }

    private static boolean canWeaveNow(
            ServerLevel level,
            PathfinderMob spider
    ) {
        if (!isUsableSpider(level, spider)
                || spider.getLightLevelDependentMagicValue() >= 0.5F
                || !RetoldBlockTargetSearch.hasLowRawSkyLight(
                        level,
                        spider.blockPosition()
                )
                || !RetoldMobGriefing.canModifyBlocks(level, spider)
                || RetoldBehaviorCoordinator.hasLiveTarget(spider)) {
            return false;
        }

        RetoldAiControlMode mode = RetoldAiControl.getMode(spider);

        return mode == RetoldAiControlMode.NONE
                || RetoldAiControl.isControlledAsBy(
                spider,
                RetoldAiControlMode.REGROUP,
                CONTROL_OWNER
        );
    }

    private static boolean isUsableSpider(
            ServerLevel level,
            PathfinderMob spider
    ) {
        return level != null
                && spider != null
                && spider.level() == level
                && RetoldBehaviorCoordinator.isUsableMob(spider)
                && RetoldMobRules.canUseOrdinaryLifeSystems(spider)
                && RetoldMobRules.isHungrySwarmPredator(spider);
    }

    private static boolean isValidLair(
            ServerLevel level,
            PathfinderMob spider,
            RetoldAnimalHomeMemory home
    ) {
        return home != null
                && home.type() == RetoldAnimalHomeType.SPIDER_LAIR
                && RetoldAnimalHomes.isValidFor(level, spider, home);
    }

    private static boolean hasRecentFoodSuccess(
            PathfinderMob spider,
            long gameTime
    ) {
        RetoldMobState state = RetoldMobStates.getOrCreate(
                spider,
                gameTime
        );
        long lastFoodSuccess = Math.max(
                state.lastAteAt(),
                state.lastSuccessfulHuntAt()
        );

        return lastFoodSuccess > 0L
                && gameTime >= lastFoodSuccess
                && gameTime - lastFoodSuccess <= RECENT_FEEDING_WINDOW_TICKS;
    }

    private static boolean reserveWorkAttempt(
            ServerLevel level,
            BlockPos center,
            long gameTime
    ) {
        Map<BlockPos, Long> nextByLair = NEXT_WORK_AT.computeIfAbsent(
                level,
                ignored -> new java.util.HashMap<>()
        );
        long nextWorkAt = nextByLair.getOrDefault(center, Long.MIN_VALUE);

        if (gameTime < nextWorkAt) {
            return false;
        }

        nextByLair.put(
                center.immutable(),
                gameTime + FAILED_WORK_RETRY_TICKS
        );
        return true;
    }

    private static void delayNextWork(
            ServerLevel level,
            BlockPos center,
            long nextWorkAt
    ) {
        NEXT_WORK_AT.computeIfAbsent(
                level,
                ignored -> new java.util.HashMap<>()
        ).put(
                center.immutable(),
                nextWorkAt
        );
    }

    private static void releaseLairMovement(PathfinderMob spider) {
        if (RetoldAiControl.isControlledAsBy(
                spider,
                RetoldAiControlMode.REGROUP,
                CONTROL_OWNER
        )) {
            RetoldBehaviorMovement.stopOwnedMovement(
                    spider,
                    CONTROL_OWNER
            );
        }
    }
}
