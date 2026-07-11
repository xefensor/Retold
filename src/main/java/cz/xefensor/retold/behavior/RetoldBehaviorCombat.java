package cz.xefensor.retold.behavior;

import cz.xefensor.retold.combat.RetoldCombatTargets;
import cz.xefensor.retold.combat.RetoldFactionTargetMemory;
import cz.xefensor.retold.combat.RetoldTargetSource;
import cz.xefensor.retold.faction.RetoldFactionRelations;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

public final class RetoldBehaviorCombat {
    private RetoldBehaviorCombat() {
    }

    public static boolean canUseAttackControl(
            Mob mob,
            RetoldAiControlOwner owner
    ) {
        RetoldAiControlMode mode = RetoldAiControl.getMode(mob);

        return mode == RetoldAiControlMode.NONE
                || RetoldAiControl.isControlledAsBy(
                mob,
                RetoldAiControlMode.ATTACK,
                owner
        );
    }

    public static boolean claimAttackControl(
            Mob mob,
            RetoldAiControlOwner owner,
            int priority,
            String reason,
            long gameTime,
            int ticks
    ) {
        return RetoldAiControl.tryClaim(
                mob,
                RetoldAiControlMode.ATTACK,
                owner,
                priority,
                reason,
                gameTime,
                ticks
        );
    }

    public static boolean isValidEnemyTarget(
            Mob attacker,
            LivingEntity target,
            double maxDistanceSquared,
            boolean requireLineOfSight
    ) {
        if (attacker == null || target == null || target == attacker) {
            return false;
        }

        if (!RetoldBehaviorCoordinator.isValidAssignmentTarget(attacker, target)) {
            return false;
        }

        if (attacker.distanceToSqr(target) > maxDistanceSquared) {
            return false;
        }

        if (requireLineOfSight && !attacker.hasLineOfSight(target)) {
            return false;
        }

        return RetoldFactionRelations.shouldAttack(
                attacker,
                target
        );
    }

    public static boolean applyAttackTarget(
            Mob mob,
            LivingEntity target,
            RetoldTargetSource source
    ) {
        return RetoldCombatTargets.applyAttackTarget(
                mob,
                target,
                source
        );
    }

    public static boolean applyAttackTargetOrClearOwner(
            Mob mob,
            LivingEntity target,
            RetoldTargetSource source,
            RetoldAiControlOwner owner
    ) {
        if (applyAttackTarget(mob, target, source)) {
            return true;
        }

        RetoldAiControl.clearIfOwnedBy(mob, owner);
        return false;
    }

    public static void clearAttackControlIfOwned(
            Mob mob,
            LivingEntity target,
            RetoldAiControlOwner owner,
            RetoldTargetSource... targetSources
    ) {
        if (target != null && targetSources != null && targetSources.length > 0) {
            RetoldFactionTargetMemory.clearTargetIfOwnedByAny(
                    mob,
                    target,
                    targetSources
            );
        }

        if (RetoldAiControl.isControlledAsBy(
                mob,
                RetoldAiControlMode.ATTACK,
                owner
        )) {
            RetoldAiControl.clearIfOwnedBy(
                    mob,
                    owner
            );
        }
    }
}
