package cz.xefensor.retold.combat;

import cz.xefensor.retold.behavior.RetoldAiControl;
import cz.xefensor.retold.behavior.RetoldAiControlMode;
import cz.xefensor.retold.behavior.RetoldMobRules;
import cz.xefensor.retold.faction.RetoldFaction;
import cz.xefensor.retold.faction.RetoldFactionMembers;
import cz.xefensor.retold.territory.RetoldTerritoryTargetBlocker;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;

public final class RetoldFactionTargetGuards {
    private static final ThreadLocal<Boolean> IGNORE_TARGET_GUARD =
            ThreadLocal.withInitial(() -> false);

    private static final ThreadLocal<Boolean> IGNORE_AGGRESSIVE_GUARD =
            ThreadLocal.withInitial(() -> false);

    private RetoldFactionTargetGuards() {
    }

    public static boolean shouldBlockTarget(
            Mob mob,
            LivingEntity target
    ) {
        if (mob == null || target == null) {
            return false;
        }

        if (RetoldAiTargets.isInvalidPlayerTarget(target)) {
            return true;
        }

        if (IGNORE_TARGET_GUARD.get()) {
            return false;
        }

        if (
                mob instanceof PathfinderMob pathfinderMob
                        && RetoldMobRules.shouldBlockVanillaPredatorTarget(
                        pathfinderMob,
                        target
                )
        ) {
            return true;
        }

        if (RetoldAiControl.shouldBlockVanillaTarget(mob, target)) {
            return true;
        }

        if (!(mob instanceof PathfinderMob pathfinderMob)) {
            return false;
        }

        return RetoldTerritoryTargetBlocker.shouldBlockTargetDuringWarning(
                pathfinderMob,
                target
        );
    }

    public static void setTargetIgnoringGuard(
            Mob mob,
            LivingEntity target
    ) {
        if (mob == null) {
            return;
        }

        boolean previous = IGNORE_TARGET_GUARD.get();
        IGNORE_TARGET_GUARD.set(true);

        try {
            mob.setTarget(target);
        } finally {
            IGNORE_TARGET_GUARD.set(previous);
        }
    }

    public static void setTargetIgnoringWarning(
            Mob mob,
            LivingEntity target
    ) {
        setTargetIgnoringGuard(
                mob,
                target
        );
    }

    public static void setAggressiveIgnoringGuard(
            Mob mob,
            boolean aggressive
    ) {
        if (mob == null) {
            return;
        }

        boolean previous = IGNORE_AGGRESSIVE_GUARD.get();
        IGNORE_AGGRESSIVE_GUARD.set(true);

        try {
            mob.setAggressive(aggressive);
        } finally {
            IGNORE_AGGRESSIVE_GUARD.set(previous);
        }
    }

    public static void setAggressiveIgnoringWarning(
            Mob mob,
            boolean aggressive
    ) {
        setAggressiveIgnoringGuard(
                mob,
                aggressive
        );
    }

    public static boolean shouldBlockAggressiveState(
            Mob mob,
            boolean aggressive
    ) {
        if (mob == null || !aggressive) {
            return false;
        }

        if (IGNORE_AGGRESSIVE_GUARD.get()) {
            return false;
        }

        if (RetoldAiControl.shouldBlockVanillaAggression(mob, aggressive)) {
            return true;
        }

        if (
                mob instanceof PathfinderMob pathfinderMob
                        && RetoldMobRules.isManagedPredator(pathfinderMob)
                        && !RetoldAiControl.isControlledAs(mob, RetoldAiControlMode.HUNT)
                        && !RetoldAiControl.isControlledAs(mob, RetoldAiControlMode.ATTACK)
        ) {
            return true;
        }

        if (!(mob instanceof AbstractPiglin piglin)) {
            return false;
        }

        if (!RetoldFactionMembers.isMemberOf(piglin, RetoldFaction.NETHER_REMNANTS)) {
            return false;
        }

        LivingEntity mobTarget = piglin.getTarget();

        if (RetoldAiTargets.isValidAssignmentTarget(piglin, mobTarget)) {
            return false;
        }

        LivingEntity brainAttackTarget = RetoldAiTargets.getBrainAttackTargetSafely(piglin);

        return !RetoldAiTargets.isValidAssignmentTarget(piglin, brainAttackTarget);
    }
}
