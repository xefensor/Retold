package cz.xefensor.retold.behavior;

import cz.xefensor.retold.combat.RetoldAiTargets;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;

public final class RetoldBehaviorCoordinator {
    private RetoldBehaviorCoordinator() {
    }

    public static boolean isUsableMob(PathfinderMob mob) {
        return isUsableEntity(mob);
    }

    public static boolean isUsableEntity(Entity entity) {
        return entity != null
                && entity.isAlive()
                && !entity.isRemoved();
    }

    public static boolean isAliveInSameLevel(
            Entity observer,
            LivingEntity target
    ) {
        return RetoldAiTargets.isAliveInSameLevel(
                observer,
                target
        );
    }

    public static boolean isValidAssignmentTarget(
            Mob mob,
            LivingEntity target
    ) {
        return RetoldAiTargets.isValidAssignmentTarget(
                mob,
                target
        );
    }

    public static boolean isInvalidPlayerTarget(Entity entity) {
        return RetoldAiTargets.isInvalidPlayerTarget(entity);
    }

    public static boolean hasLiveTarget(PathfinderMob mob) {
        if (mob == null) {
            return false;
        }

        LivingEntity target = mob.getTarget();

        return isValidAssignmentTarget(mob, target);
    }

    public static boolean canStartLowPriorityHomeBehavior(PathfinderMob mob) {
        if (!isUsableMob(mob) || hasLiveTarget(mob)) {
            return false;
        }

        return RetoldAiControl.getMode(mob) == RetoldAiControlMode.NONE;
    }

    public static boolean canContinueOwnedRegroup(
            PathfinderMob mob,
            RetoldAiControlOwner owner
    ) {
        if (!isUsableMob(mob)) {
            return false;
        }

        RetoldAiControlMode mode = RetoldAiControl.getMode(mob);

        if (mode == RetoldAiControlMode.NONE) {
            return !hasLiveTarget(mob);
        }

        return mode == RetoldAiControlMode.REGROUP
                && (
                RetoldAiControl.getOwner(mob) == owner
                        || RetoldAiControl.getOwner(mob) == RetoldAiControlOwner.SYSTEM
        );
    }

    public static boolean canUseOwnedModeWithoutLiveTarget(
            PathfinderMob mob,
            RetoldAiControlMode currentMode,
            RetoldAiControlMode ownedMode,
            RetoldAiControlOwner owner
    ) {
        if (hasLiveTarget(mob)) {
            return false;
        }

        if (currentMode == RetoldAiControlMode.NONE) {
            return true;
        }

        return RetoldAiControl.isControlledAsBy(
                mob,
                ownedMode,
                owner
        );
    }

    public static boolean canFeedNow(PathfinderMob mob) {
        if (!isUsableMob(mob) || hasLiveTarget(mob)) {
            return false;
        }

        RetoldAiControlMode mode = RetoldAiControl.getMode(mob);

        return mode == RetoldAiControlMode.NONE
                || mode == RetoldAiControlMode.FEED
                || mode == RetoldAiControlMode.SEARCH
                || mode == RetoldAiControlMode.REGROUP;
    }
}
