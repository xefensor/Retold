package cz.xefensor.retold.behavior.species;

import cz.xefensor.retold.behavior.performance.RetoldAiScanCache;
import cz.xefensor.retold.behavior.performance.RetoldAiSightCache;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;
import cz.xefensor.retold.faction.RetoldFactionMembers;
import cz.xefensor.retold.stage.RetoldWorldData;
import cz.xefensor.retold.stage.RetoldWorldStage;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;

public final class RetoldUndeadStagePressure {
    private static final int CROSS_FAMILY_SCAN_CACHE_TICKS = 6;
    private static final int CROSS_FAMILY_RESPONDER_DIVISOR = 3;

    private static final double STAGE_ONE_ZOMBIE_SHARE_RADIUS_BLOCKS = 10.0D;
    private static final double STAGE_TWO_ZOMBIE_SHARE_RADIUS_BLOCKS = 22.0D;
    private static final double STAGE_ONE_ZOMBIE_NOTICE_RADIUS_BLOCKS = 12.0D;
    private static final double STAGE_TWO_ZOMBIE_NOTICE_RADIUS_BLOCKS = 18.0D;

    private static final double STAGE_ONE_SKELETON_SHARE_RADIUS_BLOCKS = 10.0D;
    private static final double STAGE_TWO_SKELETON_SHARE_RADIUS_BLOCKS = 24.0D;
    private static final double STAGE_ONE_SKELETON_NOTICE_RADIUS_BLOCKS = 14.0D;
    private static final double STAGE_TWO_SKELETON_NOTICE_RADIUS_BLOCKS = 22.0D;

    private static final double STAGE_TWO_CROSS_FAMILY_RADIUS_BLOCKS = 12.0D;
    private static final double CLOSE_HEARING_RADIUS_SQUARED = 6.0D * 6.0D;

    private RetoldUndeadStagePressure() {
    }

    public static double zombieShareRadius(ServerLevel level) {
        return isEscalated(level)
                ? STAGE_TWO_ZOMBIE_SHARE_RADIUS_BLOCKS
                : STAGE_ONE_ZOMBIE_SHARE_RADIUS_BLOCKS;
    }

    public static double zombieNoticeRadius(ServerLevel level) {
        return isEscalated(level)
                ? STAGE_TWO_ZOMBIE_NOTICE_RADIUS_BLOCKS
                : STAGE_ONE_ZOMBIE_NOTICE_RADIUS_BLOCKS;
    }

    public static double skeletonShareRadius(ServerLevel level) {
        return isEscalated(level)
                ? STAGE_TWO_SKELETON_SHARE_RADIUS_BLOCKS
                : STAGE_ONE_SKELETON_SHARE_RADIUS_BLOCKS;
    }

    public static double skeletonNoticeRadius(ServerLevel level) {
        return isEscalated(level)
                ? STAGE_TWO_SKELETON_NOTICE_RADIUS_BLOCKS
                : STAGE_ONE_SKELETON_NOTICE_RADIUS_BLOCKS;
    }

    public static void spreadCrossFamilyTarget(
            ServerLevel level,
            PathfinderMob source,
            LivingEntity target,
            long gameTime
    ) {
        if (!isEscalated(level)
                || source == null
                || target == null
                || !target.isAlive()) {
            return;
        }

        boolean zombieSource = RetoldMobRules.isZombieHordeUndead(source);
        boolean rangedSource = RetoldSkeletonRangedEvents.isRangedUndead(source);

        if (!zombieSource && !rangedSource) {
            return;
        }

        for (PathfinderMob recruit : RetoldAiScanCache.nearby(
                level,
                source,
                PathfinderMob.class,
                STAGE_TWO_CROSS_FAMILY_RADIUS_BLOCKS,
                gameTime,
                CROSS_FAMILY_SCAN_CACHE_TICKS
        )) {
            boolean matchingCrossFamily = zombieSource
                    ? RetoldSkeletonRangedEvents.isRangedUndead(recruit)
                    : RetoldMobRules.isZombieHordeUndead(recruit);

            if (!matchingCrossFamily
                    || !canHearOrSeeIncident(level, source, recruit, gameTime)
                    || !isOccasionalResponder(recruit)) {
                continue;
            }

            if (zombieSource) {
                RetoldSkeletonRangedEvents.adoptStagePressureTarget(
                        recruit,
                        target,
                        gameTime
                );
            } else {
                RetoldUndeadHordeEvents.adoptStagePressureTarget(
                        recruit,
                        target,
                        gameTime
                );
            }
        }
    }

    static boolean isEscalated(ServerLevel level) {
        return level != null
                && RetoldWorldData.get(level).getStage() == RetoldWorldStage.STAGE_2;
    }

    static boolean isOccasionalResponder(PathfinderMob recruit) {
        return recruit != null
                && Math.floorMod(
                recruit.getUUID().hashCode(),
                CROSS_FAMILY_RESPONDER_DIVISOR
        ) == 0;
    }

    private static boolean canHearOrSeeIncident(
            ServerLevel level,
            PathfinderMob source,
            PathfinderMob recruit,
            long gameTime
    ) {
        if (recruit == null
                || recruit == source
                || !recruit.isAlive()
                || recruit.isRemoved()
                || recruit.level() != level
                || !RetoldFactionMembers.isUndead(recruit)) {
            return false;
        }

        double distanceSquared = source.distanceToSqr(recruit);

        if (distanceSquared
                > STAGE_TWO_CROSS_FAMILY_RADIUS_BLOCKS
                * STAGE_TWO_CROSS_FAMILY_RADIUS_BLOCKS) {
            return false;
        }

        return distanceSquared <= CLOSE_HEARING_RADIUS_SQUARED
                || RetoldAiSightCache.canSee(recruit, source, gameTime);
    }
}
