package cz.xefensor.retold.combat;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

public final class RetoldCombatTargets {
    private RetoldCombatTargets() {
    }

    public static boolean applyAttackTarget(
            Mob mob,
            LivingEntity target,
            RetoldTargetSource source
    ) {
        return applyOwnedTarget(
                mob,
                target,
                source,
                true,
                true
        );
    }

    public static boolean applyOwnedTarget(
            Mob mob,
            LivingEntity target,
            RetoldTargetSource source,
            boolean aggressive,
            boolean faceTarget
    ) {
        if (mob == null || target == null) {
            return false;
        }

        boolean applied = RetoldFactionTargetMemory.trySetTarget(
                mob,
                target,
                source
        );

        if (!applied && mob.getTarget() != target) {
            return false;
        }

        RetoldFactionTargetGuards.setAggressiveIgnoringGuard(
                mob,
                aggressive
        );

        if (faceTarget) {
            mob.getLookControl().setLookAt(
                    target,
                    30.0F,
                    30.0F
            );
        }

        return true;
    }

    public static void clearTargetReferencesAndAggression(
            Mob mob,
            LivingEntity target,
            boolean stopNavigation
    ) {
        if (mob == null || target == null) {
            return;
        }

        RetoldFactionTargetMemory.clearTargetOwnership(
                mob,
                target
        );

        RetoldAiTargets.clearTargetAndAggression(
                mob,
                target,
                stopNavigation
        );
    }
}
