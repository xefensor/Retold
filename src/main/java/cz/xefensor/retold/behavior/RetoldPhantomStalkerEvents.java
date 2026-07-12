package cz.xefensor.retold.behavior;

import cz.xefensor.retold.combat.RetoldTargetSource;
import cz.xefensor.retold.faction.RetoldFaction;
import cz.xefensor.retold.faction.RetoldFactionMembers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.List;

public final class RetoldPhantomStalkerEvents {
    private static final int THINK_INTERVAL_TICKS = 12;
    private static final int STALK_SCAN_CACHE_TICKS = 6;
    private static final int STALK_CONTROL_TICKS = 20 * 5;
    private static final int STALK_PRIORITY = RetoldAiPriorities.SPECIAL_STALK;

    private static final double TARGET_SEARCH_RADIUS_BLOCKS = 42.0D;
    private static final double TARGET_SEARCH_RADIUS_SQUARED =
            TARGET_SEARCH_RADIUS_BLOCKS * TARGET_SEARCH_RADIUS_BLOCKS;

    private static final double TARGET_KEEP_RADIUS_BLOCKS = 56.0D;
    private static final double TARGET_KEEP_RADIUS_SQUARED =
            TARGET_KEEP_RADIUS_BLOCKS * TARGET_KEEP_RADIUS_BLOCKS;

    private RetoldPhantomStalkerEvents() {
    }

    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Mob phantom)) {
            return;
        }

        if (!(phantom.level() instanceof ServerLevel level)) {
            return;
        }

        if (!RetoldMobRules.isPhantomStalker(phantom)) {
            return;
        }

        long gameTime = level.getGameTime();

        if (!RetoldBehaviorTiming.shouldThink(
                phantom,
                gameTime,
                THINK_INTERVAL_TICKS
        )) {
            return;
        }

        LivingEntity target = phantom.getTarget();

        if (isValidStalkTarget(level, phantom, target, TARGET_KEEP_RADIUS_SQUARED)) {
            keepTarget(
                    phantom,
                    target,
                    gameTime
            );
            return;
        }

        clearStalkTargetIfOwned(
                phantom,
                target
        );

        if (!canAcquireTarget(level, phantom)) {
            return;
        }

        LivingEntity newTarget = findBestStalkTarget(
                level,
                phantom
        );

        if (newTarget == null) {
            return;
        }

        keepTarget(
                phantom,
                newTarget,
                gameTime
        );
    }

    private static LivingEntity findBestStalkTarget(
            ServerLevel level,
            Mob phantom
    ) {
        List<LivingEntity> candidates = RetoldAiScanCache.nearby(
                level,
                phantom,
                LivingEntity.class,
                TARGET_SEARCH_RADIUS_BLOCKS,
                level.getGameTime(),
                STALK_SCAN_CACHE_TICKS
        );

        LivingEntity bestTarget = null;
        double bestScore = Double.MAX_VALUE;

        for (LivingEntity candidate : candidates) {
            if (!isValidStalkTarget(level, phantom, candidate, TARGET_SEARCH_RADIUS_SQUARED)) {
                continue;
            }

            double distanceSquared = phantom.distanceToSqr(candidate);

            if (distanceSquared > TARGET_SEARCH_RADIUS_SQUARED) {
                continue;
            }

            double score = distanceSquared;

            if (isExposedToSky(level, candidate)) {
                score -= 180.0D;
            }

            if (RetoldAiSightCache.canSee(phantom, candidate, level.getGameTime())) {
                score -= 80.0D;
            }

            if (candidate instanceof Player) {
                score -= 120.0D;
            }

            if (RetoldFactionMembers.isUndead(candidate)) {
                score += 300.0D;
            }

            if (score < bestScore) {
                bestScore = score;
                bestTarget = candidate;
            }
        }

        return bestTarget;
    }

    private static void keepTarget(
            Mob phantom,
            LivingEntity target,
            long gameTime
    ) {
        if (!(phantom.level() instanceof ServerLevel level)) {
            return;
        }

        if (!isValidStalkTarget(level, phantom, target, TARGET_KEEP_RADIUS_SQUARED)) {
            return;
        }

        if (!RetoldBehaviorCombat.claimAttackControl(
                phantom,
                RetoldAiControlOwner.SPECIAL_UNDEAD,
                STALK_PRIORITY,
                "phantom_stalker",
                gameTime,
                STALK_CONTROL_TICKS
        )) {
            return;
        }

        if (!RetoldBehaviorCombat.applyAttackTargetOrClearOwner(
                phantom,
                target,
                RetoldTargetSource.FACTION_COMBAT,
                RetoldAiControlOwner.SPECIAL_UNDEAD
        )) {
            return;
        }
    }

    private static boolean canAcquireTarget(
            ServerLevel level,
            Mob phantom
    ) {
        if (!isStalkingTime(level)) {
            return false;
        }

        return RetoldBehaviorCombat.canUseAttackControl(
                phantom,
                RetoldAiControlOwner.SPECIAL_UNDEAD
        );
    }

    private static boolean isValidStalkTarget(
            ServerLevel level,
            Mob phantom,
            LivingEntity target,
            double maxDistanceSquared
    ) {
        if (!isStalkingTime(level)) {
            return false;
        }

        if (!RetoldBehaviorCombat.isValidEnemyTarget(
                phantom,
                target,
                maxDistanceSquared,
                false
        )) {
            return false;
        }

        if (!isExposedToSky(level, target) && !RetoldAiSightCache.canSee(phantom, target, phantom.level().getGameTime())) {
            return false;
        }

        return true;
    }

    private static boolean isStalkingTime(ServerLevel level) {
        return RetoldAnimalDailyRhythm.isNight(level)
                || RetoldAnimalDailyRhythm.isDusk(level)
                || level.isRaining();
    }

    private static boolean isExposedToSky(
            ServerLevel level,
            LivingEntity target
    ) {
        return level.canSeeSky(
                target.blockPosition()
        );
    }

    private static void clearStalkTargetIfOwned(
            Mob phantom,
            LivingEntity target
    ) {
        RetoldBehaviorCombat.clearAttackControlIfOwned(
                phantom,
                target,
                RetoldAiControlOwner.SPECIAL_UNDEAD,
                RetoldTargetSource.FACTION_COMBAT
        );
    }
}
