package cz.xefensor.retold.enderman;

import cz.xefensor.retold.behavior.performance.RetoldAiScanCache;
import cz.xefensor.retold.combat.RetoldAiTargets;
import cz.xefensor.retold.combat.RetoldCombatTargets;
import cz.xefensor.retold.combat.RetoldTargetSource;
import cz.xefensor.retold.stage.RetoldWorldStage;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.EnderMan;

public final class RetoldEndermanDefense {
    private static final double DEFENSE_RADIUS_BLOCKS = 32.0D;
    private static final int SCAN_CACHE_TICKS = 6;

    private RetoldEndermanDefense() {
    }

    public static int onEndermanAttacked(
            ServerLevel level,
            EnderMan victim,
            LivingEntity attacker,
            RetoldWorldStage stage
    ) {
        if (level == null
                || victim == null
                || attacker == null
                || attacker == victim
                || stage == null
                || !RetoldAiTargets.isAliveInSameLevel(victim, attacker)) {
            return 0;
        }

        int responders = applyDefenseTarget(
                victim,
                attacker,
                RetoldTargetSource.RETALIATION
        ) ? 1 : 0;

        if (responders == 0 || stage != RetoldWorldStage.STAGE_3) {
            return responders;
        }

        for (EnderMan ally : RetoldAiScanCache.nearby(
                level,
                victim,
                EnderMan.class,
                DEFENSE_RADIUS_BLOCKS,
                level.getGameTime(),
                SCAN_CACHE_TICKS
        )) {
            if (!canJoinDefense(victim, ally, attacker)) {
                continue;
            }

            if (applyDefenseTarget(
                    ally,
                    attacker,
                    RetoldTargetSource.FACTION_ASSIST
            )) {
                responders++;
            }
        }

        return responders;
    }

    private static boolean canJoinDefense(
            EnderMan victim,
            EnderMan ally,
            LivingEntity attacker
    ) {
        if (ally == null || ally == victim) {
            return false;
        }

        if (!RetoldAiTargets.isAliveInSameLevel(victim, ally)) {
            return false;
        }

        LivingEntity currentTarget = ally.getTarget();

        return currentTarget == null
                || currentTarget == attacker
                || !RetoldAiTargets.isAliveInSameLevel(ally, currentTarget);
    }

    private static boolean applyDefenseTarget(
            EnderMan enderman,
            LivingEntity attacker,
            RetoldTargetSource source
    ) {
        return RetoldCombatTargets.applyAttackTarget(
                enderman,
                attacker,
                source
        );
    }
}
