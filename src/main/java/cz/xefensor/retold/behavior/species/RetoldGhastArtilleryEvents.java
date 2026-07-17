package cz.xefensor.retold.behavior.species;

import cz.xefensor.retold.behavior.control.RetoldAiControlOwner;
import cz.xefensor.retold.behavior.control.RetoldAiPriorities;
import cz.xefensor.retold.behavior.performance.RetoldAiScanCache;
import cz.xefensor.retold.behavior.performance.RetoldAiSightCache;
import cz.xefensor.retold.behavior.core.RetoldBehaviorCombat;
import cz.xefensor.retold.behavior.core.RetoldBehaviorTiming;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;

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

public final class RetoldGhastArtilleryEvents {
    private static final int THINK_INTERVAL_TICKS = 16;
    private static final int ARTILLERY_SCAN_CACHE_TICKS = 8;
    private static final int ARTILLERY_CONTROL_TICKS = 20 * 5;
    private static final int ARTILLERY_PRIORITY = RetoldAiPriorities.SPECIAL_RANGED;

    private static final double TARGET_SEARCH_RADIUS_BLOCKS = 64.0D;
    private static final double TARGET_SEARCH_RADIUS_SQUARED =
            TARGET_SEARCH_RADIUS_BLOCKS * TARGET_SEARCH_RADIUS_BLOCKS;

    private static final double TARGET_KEEP_RADIUS_BLOCKS = 80.0D;
    private static final double TARGET_KEEP_RADIUS_SQUARED =
            TARGET_KEEP_RADIUS_BLOCKS * TARGET_KEEP_RADIUS_BLOCKS;

    private RetoldGhastArtilleryEvents() {
    }

    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Mob ghast)) {
            return;
        }

        if (!(ghast.level() instanceof ServerLevel level)) {
            return;
        }

        if (!RetoldMobRules.isGhastArtillery(ghast)) {
            return;
        }

        long gameTime = level.getGameTime();

        if (!RetoldBehaviorTiming.shouldThink(
                ghast,
                gameTime,
                THINK_INTERVAL_TICKS
        )) {
            return;
        }

        LivingEntity target = ghast.getTarget();

        if (isValidArtilleryTarget(ghast, target, TARGET_KEEP_RADIUS_SQUARED)) {
            keepTarget(
                    ghast,
                    target,
                    gameTime
            );
            return;
        }

        clearArtilleryTargetIfOwned(
                ghast,
                target
        );

        if (!canAcquireTarget(ghast)) {
            return;
        }

        LivingEntity newTarget = findBestArtilleryTarget(
                level,
                ghast
        );

        if (newTarget == null) {
            return;
        }

        keepTarget(
                ghast,
                newTarget,
                gameTime
        );
    }

    private static LivingEntity findBestArtilleryTarget(
            ServerLevel level,
            Mob ghast
    ) {
        List<LivingEntity> candidates = RetoldAiScanCache.nearby(
                level,
                ghast,
                LivingEntity.class,
                TARGET_SEARCH_RADIUS_BLOCKS,
                level.getGameTime(),
                ARTILLERY_SCAN_CACHE_TICKS
        );

        LivingEntity bestTarget = null;
        double bestScore = Double.MAX_VALUE;

        for (LivingEntity candidate : candidates) {
            if (!isValidArtilleryTarget(ghast, candidate, TARGET_SEARCH_RADIUS_SQUARED)) {
                continue;
            }

            double distanceSquared = ghast.distanceToSqr(candidate);

            if (distanceSquared > TARGET_SEARCH_RADIUS_SQUARED) {
                continue;
            }

            double score = distanceSquared;

            if (RetoldAiSightCache.canSee(ghast, candidate, level.getGameTime())) {
                score -= 500.0D;
            }

            if (candidate instanceof Player) {
                score -= 120.0D;
            }

            if (RetoldFactionMembers.isNetherRemnant(candidate)) {
                score -= 80.0D;
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
            Mob ghast,
            LivingEntity target,
            long gameTime
    ) {
        if (!isValidArtilleryTarget(ghast, target, TARGET_KEEP_RADIUS_SQUARED)) {
            return;
        }

        if (!RetoldBehaviorCombat.claimAttackControl(
                ghast,
                RetoldAiControlOwner.SPECIAL_UNDEAD,
                ARTILLERY_PRIORITY,
                "ghast_artillery",
                gameTime,
                ARTILLERY_CONTROL_TICKS
        )) {
            return;
        }

        if (!RetoldBehaviorCombat.applyAttackTargetOrClearOwner(
                ghast,
                target,
                RetoldTargetSource.FACTION_COMBAT,
                RetoldAiControlOwner.SPECIAL_UNDEAD
        )) {
            return;
        }
    }

    private static boolean canAcquireTarget(Mob ghast) {
        return RetoldBehaviorCombat.canUseAttackControl(
                ghast,
                RetoldAiControlOwner.SPECIAL_UNDEAD
        );
    }

    private static boolean isValidArtilleryTarget(
            Mob ghast,
            LivingEntity target,
            double maxDistanceSquared
    ) {
        return RetoldBehaviorCombat.isValidEnemyTarget(
                ghast,
                target,
                maxDistanceSquared,
                true
        );
    }

    private static void clearArtilleryTargetIfOwned(
            Mob ghast,
            LivingEntity target
    ) {
        RetoldBehaviorCombat.clearAttackControlIfOwned(
                ghast,
                target,
                RetoldAiControlOwner.SPECIAL_UNDEAD,
                RetoldTargetSource.FACTION_COMBAT
        );
    }
}
