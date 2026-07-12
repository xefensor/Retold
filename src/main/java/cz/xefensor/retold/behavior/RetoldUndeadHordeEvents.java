package cz.xefensor.retold.behavior;

import cz.xefensor.retold.combat.RetoldTargetSource;
import cz.xefensor.retold.faction.RetoldFaction;
import cz.xefensor.retold.faction.RetoldFactionMembers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.List;

public final class RetoldUndeadHordeEvents {
    private static final int THINK_INTERVAL_TICKS = 12;
    private static final int HORDE_SCAN_CACHE_TICKS = 6;
    private static final int HORDE_PATH_INTERVAL_TICKS = 6;
    private static final int HORDE_CONTROL_TICKS = 20 * 4;
    private static final int HORDE_PRIORITY = RetoldAiPriorities.FACTION_PRESSURE;

    private static final double TARGET_SHARE_RADIUS_BLOCKS = 22.0D;
    private static final double TARGET_SHARE_RADIUS_SQUARED =
            TARGET_SHARE_RADIUS_BLOCKS * TARGET_SHARE_RADIUS_BLOCKS;

    private static final double HUNGRY_NOTICE_RADIUS_BLOCKS = 18.0D;
    private static final double HUNGRY_NOTICE_RADIUS_SQUARED =
            HUNGRY_NOTICE_RADIUS_BLOCKS * HUNGRY_NOTICE_RADIUS_BLOCKS;

    private static final double CONVERGE_SPEED = 1.0D;

    private RetoldUndeadHordeEvents() {
    }

    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof PathfinderMob undead)) {
            return;
        }

        if (!(undead.level() instanceof ServerLevel level)) {
            return;
        }

        if (!isZombieHordeUndead(undead)) {
            return;
        }

        long gameTime = level.getGameTime();

        if (!shouldThink(undead, gameTime)) {
            return;
        }

        LivingEntity currentTarget = undead.getTarget();

        if (isValidHordeTarget(undead, currentTarget)) {
            spreadTargetToNearbyHorde(
                    level,
                    undead,
                    currentTarget,
                    gameTime
            );
            return;
        }

        if (!canAdoptHordeTarget(undead)) {
            return;
        }

        LivingEntity sharedTarget = findSharedHordeTarget(
                level,
                undead
        );

        if (sharedTarget == null && shouldSeekHungryTarget(undead, gameTime)) {
            sharedTarget = findHungryTarget(
                    level,
                    undead
            );
        }

        if (sharedTarget == null) {
            clearHordeControlIfOwned(undead);
            return;
        }

        adoptAndConverge(
                undead,
                sharedTarget,
                gameTime
        );
    }

    private static boolean shouldThink(
            PathfinderMob undead,
            long gameTime
    ) {
        return RetoldBehaviorTiming.shouldThink(
                undead,
                gameTime,
                THINK_INTERVAL_TICKS
        );
    }

    private static boolean isZombieHordeUndead(PathfinderMob mob) {
        return RetoldMobRules.isZombieHordeUndead(mob);
    }

    private static boolean canAdoptHordeTarget(PathfinderMob undead) {
        if (undead == null || !undead.isAlive() || undead.isRemoved()) {
            return false;
        }

        RetoldAiControlMode mode = RetoldAiControl.getMode(undead);

        return mode == RetoldAiControlMode.NONE
                || RetoldAiControl.isControlledAsBy(
                undead,
                RetoldAiControlMode.ATTACK,
                RetoldAiControlOwner.UNDEAD_HORDE
        );
    }

    private static boolean shouldSeekHungryTarget(
            PathfinderMob undead,
            long gameTime
    ) {
        RetoldMobState state = RetoldMobStates.getOrCreate(
                undead,
                gameTime
        );

        return RetoldMobRules.hasActiveSearchDrive(state);
    }

    private static LivingEntity findSharedHordeTarget(
            ServerLevel level,
            PathfinderMob undead
    ) {
        List<PathfinderMob> sources = RetoldAiScanCache.nearby(
                level,
                undead,
                PathfinderMob.class,
                TARGET_SHARE_RADIUS_BLOCKS,
                level.getGameTime(),
                HORDE_SCAN_CACHE_TICKS
        );

        LivingEntity bestTarget = null;
        double bestScore = Double.MAX_VALUE;

        for (PathfinderMob source : sources) {
            if (!isValidHordeSource(undead, source)) {
                continue;
            }

            LivingEntity target = source.getTarget();

            if (!isValidHordeTarget(undead, target)) {
                continue;
            }

            double score = undead.distanceToSqr(source);

            if (RetoldAiSightCache.canSee(source, target, level.getGameTime())) {
                score -= 18.0D;
            }

            if (RetoldAiSightCache.canSee(undead, source, level.getGameTime())) {
                score -= 8.0D;
            }

            if (score < bestScore) {
                bestScore = score;
                bestTarget = target;
            }
        }

        return bestTarget;
    }

    private static LivingEntity findHungryTarget(
            ServerLevel level,
            PathfinderMob undead
    ) {
        List<LivingEntity> candidates = RetoldAiScanCache.nearby(
                level,
                undead,
                LivingEntity.class,
                HUNGRY_NOTICE_RADIUS_BLOCKS,
                level.getGameTime(),
                HORDE_SCAN_CACHE_TICKS
        );

        LivingEntity bestTarget = null;
        double bestScore = Double.MAX_VALUE;

        for (LivingEntity candidate : candidates) {
            if (!isValidHungrySearchTarget(undead, candidate)) {
                continue;
            }

            double distanceSquared = undead.distanceToSqr(candidate);

            if (distanceSquared > HUNGRY_NOTICE_RADIUS_SQUARED) {
                continue;
            }

            double score = distanceSquared;

            if (RetoldAiSightCache.canSee(undead, candidate, level.getGameTime())) {
                score -= 22.0D;
            }

            if (candidate instanceof Player) {
                score -= 12.0D;
            }

            if (score < bestScore) {
                bestScore = score;
                bestTarget = candidate;
            }
        }

        return bestTarget;
    }

    private static boolean isValidHordeSource(
            PathfinderMob undead,
            PathfinderMob source
    ) {
        if (source == null || source == undead) {
            return false;
        }

        if (!isZombieHordeUndead(source)) {
            return false;
        }

        if (!RetoldBehaviorCoordinator.isAliveInSameLevel(undead, source)) {
            return false;
        }

        if (undead.distanceToSqr(source) > TARGET_SHARE_RADIUS_SQUARED) {
            return false;
        }

        LivingEntity target = source.getTarget();

        return isValidHordeTarget(
                undead,
                target
        );
    }

    private static boolean isValidHungrySearchTarget(
            PathfinderMob undead,
            LivingEntity candidate
    ) {
        if (!isValidHordeTarget(undead, candidate)) {
            return false;
        }

        if (undead.distanceToSqr(candidate) > HUNGRY_NOTICE_RADIUS_SQUARED) {
            return false;
        }

        return RetoldAiSightCache.canSee(undead, candidate, undead.level().getGameTime())
                || undead.distanceToSqr(candidate) <= 36.0D;
    }

    private static boolean isValidHordeTarget(
            PathfinderMob undead,
            LivingEntity target
    ) {
        if (!RetoldBehaviorCombat.isValidEnemyTarget(
                undead,
                target,
                Double.MAX_VALUE,
                false
        )) {
            return false;
        }

        if (RetoldFactionMembers.isUndead(target)) {
            return false;
        }

        return true;
    }

    private static void spreadTargetToNearbyHorde(
            ServerLevel level,
            PathfinderMob source,
            LivingEntity target,
            long gameTime
    ) {
        for (PathfinderMob ally : RetoldAiScanCache.nearby(
                level,
                source,
                PathfinderMob.class,
                TARGET_SHARE_RADIUS_BLOCKS,
                gameTime,
                HORDE_SCAN_CACHE_TICKS
        )) {
            if (!isValidHordeRecruit(source, ally)) {
                continue;
            }

            adoptAndConverge(
                    ally,
                    target,
                    gameTime
            );
        }
    }

    private static boolean isValidHordeRecruit(
            PathfinderMob source,
            PathfinderMob recruit
    ) {
        if (recruit == null || recruit == source) {
            return false;
        }

        if (!isZombieHordeUndead(recruit)) {
            return false;
        }

        if (!RetoldBehaviorCoordinator.isAliveInSameLevel(source, recruit)) {
            return false;
        }

        if (source.distanceToSqr(recruit) > TARGET_SHARE_RADIUS_SQUARED) {
            return false;
        }

        LivingEntity currentTarget = recruit.getTarget();

        return currentTarget == null
                || !currentTarget.isAlive()
                || currentTarget == source.getTarget();
    }

    private static void adoptAndConverge(
            PathfinderMob undead,
            LivingEntity target,
            long gameTime
    ) {
        if (!isValidHordeTarget(undead, target)) {
            return;
        }

        if (!RetoldBehaviorCombat.claimAttackControl(
                undead,
                RetoldAiControlOwner.UNDEAD_HORDE,
                HORDE_PRIORITY,
                "undead_horde",
                gameTime,
                HORDE_CONTROL_TICKS
        )) {
            return;
        }

        if (!RetoldBehaviorCombat.applyAttackTargetOrClearOwner(
                undead,
                target,
                RetoldTargetSource.FACTION_ASSIST,
                RetoldAiControlOwner.UNDEAD_HORDE
        )) {
            return;
        }

        RetoldBehaviorMovement.throttledMoveTo(
                undead,
                target,
                CONVERGE_SPEED,
                gameTime,
                HORDE_PATH_INTERVAL_TICKS,
                2.0D * 2.0D
        );
    }

    private static void clearHordeControlIfOwned(PathfinderMob undead) {
        RetoldBehaviorCombat.clearAttackControlIfOwned(
                undead,
                undead.getTarget(),
                RetoldAiControlOwner.UNDEAD_HORDE,
                RetoldTargetSource.FACTION_ASSIST
        );
    }
}
