package cz.xefensor.retold.behavior;

import net.minecraft.world.entity.PathfinderMob;

final class RetoldPackHomeControl {
    private RetoldPackHomeControl() {
    }

    static void clearCombatState(PathfinderMob mob) {
        RetoldBehaviorTargets.setTargetAndAggression(mob, null, false);

        RetoldPredatorStrike.clear(mob);
        mob.setSprinting(false);
    }
}
