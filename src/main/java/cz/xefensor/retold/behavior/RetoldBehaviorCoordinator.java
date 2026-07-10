package cz.xefensor.retold.behavior;

import net.minecraft.world.entity.PathfinderMob;

public final class RetoldBehaviorCoordinator {
    private RetoldBehaviorCoordinator() {
    }

    public static boolean isUsableMob(PathfinderMob mob) {
        return mob != null
                && mob.isAlive()
                && !mob.isRemoved();
    }

    public static boolean hasLiveTarget(PathfinderMob mob) {
        return mob != null
                && mob.getTarget() != null
                && mob.getTarget().isAlive();
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
