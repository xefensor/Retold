package cz.xefensor.retold.behavior;

import net.minecraft.world.entity.PathfinderMob;

final class RetoldPackMovementRules {
    private RetoldPackMovementRules() {
    }

    static boolean canOverrideMemberMode(PathfinderMob member) {
        RetoldAiControlMode mode = RetoldAiControl.getMode(member);

        return mode == RetoldAiControlMode.NONE
                || mode == RetoldAiControlMode.SEARCH
                || mode == RetoldAiControlMode.HUNT
                || mode == RetoldAiControlMode.REGROUP;
    }

    static boolean isBusyReturnMode(PathfinderMob mob) {
        RetoldAiControlMode mode = RetoldAiControl.getMode(mob);

        return mode == RetoldAiControlMode.FEED
                || mode == RetoldAiControlMode.FLEE
                || mode == RetoldAiControlMode.ATTACK
                || mode == RetoldAiControlMode.TERRITORY
                || mode == RetoldAiControlMode.SHELTER;
    }

    static String getPath(PathfinderMob mob) {
        return RetoldMobRules.getEntityTypePath(
                mob.getType()
        );
    }
}
