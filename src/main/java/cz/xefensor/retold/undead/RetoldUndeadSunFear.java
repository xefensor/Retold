package cz.xefensor.retold.undead;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.FleeSunGoal;
import net.minecraft.world.entity.ai.goal.RestrictSunGoal;

public final class RetoldUndeadSunFear {
    private RetoldUndeadSunFear() {
    }

    public static void removeSunFearGoals(PathfinderMob mob) {
        mob.goalSelector.removeAllGoals(goal ->
                goal instanceof FleeSunGoal
                        || goal instanceof RestrictSunGoal
        );
    }
}