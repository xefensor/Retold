package cz.xefensor.retold.undead;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.FleeSunGoal;
import net.minecraft.world.entity.ai.goal.RestrictSunGoal;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public final class RetoldUndeadSunFear {
    private static final Set<PathfinderMob> UPDATED_MOBS =
            Collections.newSetFromMap(new WeakHashMap<>());

    private RetoldUndeadSunFear() {
    }

    public static void removeSunFearGoalsOnce(PathfinderMob mob) {
        if (!UPDATED_MOBS.add(mob)) {
            return;
        }

        removeSunFearGoals(mob);
    }

    private static void removeSunFearGoals(PathfinderMob mob) {
        mob.goalSelector.removeAllGoals(goal ->
                goal instanceof FleeSunGoal
                        || goal instanceof RestrictSunGoal
        );
    }
}