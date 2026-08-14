package cz.xefensor.retold.combat;

import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.control.RetoldAiControlMode;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;

import cz.xefensor.retold.faction.RetoldFaction;
import cz.xefensor.retold.faction.RetoldFactionMembers;
import cz.xefensor.retold.faction.RetoldFactionRelations;
import cz.xefensor.retold.territory.RetoldTerritoryTargetBlocker;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;

public final class RetoldFactionTargetGuards {
    private static final ThreadLocal<Boolean> IGNORE_TARGET_GUARD =
            ThreadLocal.withInitial(() -> false);

    private static final ThreadLocal<Boolean> SOURCE_VALIDATED_TARGET =
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

        if (!SOURCE_VALIDATED_TARGET.get()
                && RetoldMobTargetPolicy.shouldBlockDeliberateHostility(
                mob,
                target
        )) {
            return true;
        }

        if (RetoldAiTargets.isInvalidPlayerTarget(target)) {
            return true;
        }

        /*
         * The vanilla Wither goal knows only Minecraft's static wither-friend tag. Retold's
         * Undead membership can be extended by datapacks and can change per entity when an
         * undead mount is claimed, so its primary head must use the live faction relation.
         */
        if (mob instanceof WitherBoss
                && !RetoldFactionRelations.shouldAttack(mob, target)) {
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
        setTargetIgnoringGuard(mob, target, null);
    }

    public static void setTargetIgnoringGuard(
            Mob mob,
            LivingEntity target,
            RetoldTargetSource source
    ) {
        if (mob == null) {
            return;
        }

        boolean previous = IGNORE_TARGET_GUARD.get();
        boolean previousSourceValidation = SOURCE_VALIDATED_TARGET.get();
        boolean sourceValidated = source != null
                && !RetoldMobTargetPolicy.shouldBlockDeliberateHostility(
                mob,
                target,
                source
        );
        IGNORE_TARGET_GUARD.set(true);
        SOURCE_VALIDATED_TARGET.set(sourceValidated);

        try {
            mob.setTarget(target);
        } finally {
            IGNORE_TARGET_GUARD.set(previous);
            SOURCE_VALIDATED_TARGET.set(previousSourceValidation);
        }
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

        if (!RetoldFactionMembers.isNetherRemnant(piglin)) {
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
