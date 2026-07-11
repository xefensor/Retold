package cz.xefensor.retold.behavior;

import cz.xefensor.retold.combat.RetoldAiTargets;
import cz.xefensor.retold.combat.RetoldCombatTargets;
import cz.xefensor.retold.combat.RetoldTargetSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

public final class RetoldBehaviorTargets {
    private RetoldBehaviorTargets() {
    }

    public static boolean setTargetAndAggression(
            Mob mob,
            LivingEntity target,
            boolean aggressive
    ) {
        if (aggressive && target != null) {
            return RetoldCombatTargets.applyAttackTarget(
                    mob,
                    target,
                    RetoldTargetSource.BEHAVIOR_COMBAT
            );
        }

        if (target == null) {
            LivingEntity existingTarget = mob == null ? null : mob.getTarget();

            if (existingTarget != null) {
                RetoldCombatTargets.clearTargetReferencesAndAggression(
                        mob,
                        existingTarget,
                        false
                );
                return true;
            }
        }

        RetoldAiTargets.setTargetAndAggression(
                mob,
                target,
                aggressive
        );

        return true;
    }

    public static boolean setAttackTargetOrClearOwner(
            Mob mob,
            LivingEntity target,
            RetoldAiControlOwner owner
    ) {
        if (setTargetAndAggression(mob, target, true)) {
            return true;
        }

        RetoldAiControl.clearIfOwnedBy(mob, owner);
        return false;
    }

    public static boolean setAttackTargetOrClearMode(
            Mob mob,
            LivingEntity target,
            RetoldAiControlMode mode
    ) {
        if (setTargetAndAggression(mob, target, true)) {
            return true;
        }

        RetoldAiControl.clearIfControlledAs(mob, mode);
        return false;
    }

    public static void clearTargetAndAggression(
            Mob mob,
            LivingEntity target,
            boolean stopNavigation
    ) {
        RetoldCombatTargets.clearTargetReferencesAndAggression(
                mob,
                target,
                stopNavigation
        );
    }

    public static void setAggression(
            Mob mob,
            boolean aggressive
    ) {
        RetoldAiTargets.setAggression(
                mob,
                aggressive
        );
    }
}
