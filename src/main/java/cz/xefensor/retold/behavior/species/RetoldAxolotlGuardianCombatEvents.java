package cz.xefensor.retold.behavior.species;

import cz.xefensor.retold.behavior.performance.RetoldAiScanCache;
import cz.xefensor.retold.behavior.performance.RetoldAiSightCache;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;
import cz.xefensor.retold.combat.RetoldAiTargets;
import cz.xefensor.retold.combat.RetoldTargetSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.monster.Guardian;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

public final class RetoldAxolotlGuardianCombatEvents {
    private static final double ASSIST_RADIUS_BLOCKS = 14.0D;
    private static final double CLOSE_WITNESS_RADIUS_SQUARED = 6.0D * 6.0D;
    private static final int ASSIST_SCAN_CACHE_TICKS = 6;

    private RetoldAxolotlGuardianCombatEvents() {
    }

    @SubscribeEvent
    public static void onLivingIncomingDamage(
            LivingIncomingDamageEvent event
    ) {
        if (!(event.getEntity() instanceof PathfinderMob victim)
                || !RetoldMobRules.isAquaticHelperPredator(victim)
                || !(event.getSource().getEntity() instanceof Guardian guardian)
                || !(victim.level() instanceof ServerLevel level)) {
            return;
        }

        long gameTime = level.getGameTime();

        RetoldAxolotlHelperEvents.beginGuardianDefense(
                victim,
                guardian,
                RetoldTargetSource.RETALIATION,
                gameTime
        );

        for (PathfinderMob ally : RetoldAiScanCache.nearby(
                level,
                victim,
                PathfinderMob.class,
                ASSIST_RADIUS_BLOCKS,
                gameTime,
                ASSIST_SCAN_CACHE_TICKS
        )) {
            if (!canAssist(victim, ally, guardian, gameTime)) {
                continue;
            }

            RetoldAxolotlHelperEvents.beginGuardianDefense(
                    ally,
                    guardian,
                    RetoldTargetSource.FACTION_ASSIST,
                    gameTime
            );
        }
    }

    private static boolean canAssist(
            PathfinderMob victim,
            PathfinderMob ally,
            Guardian guardian,
            long gameTime
    ) {
        if (ally == victim
                || !RetoldMobRules.isAquaticHelperPredator(ally)
                || !RetoldAiTargets.isAliveInSameLevel(victim, ally)) {
            return false;
        }

        if (ally.getTarget() != null && ally.getTarget() != guardian) {
            return false;
        }

        return ally.distanceToSqr(victim) <= CLOSE_WITNESS_RADIUS_SQUARED
                || RetoldAiSightCache.canSee(ally, guardian, gameTime);
    }
}
