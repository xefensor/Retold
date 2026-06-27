package cz.xefensor.retold.enderman;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.EnderMan;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public final class RetoldEndermanBehavior {
    private static final Set<EnderMan> UPDATED_ENDERMEN =
            Collections.newSetFromMap(new WeakHashMap<>());

    private RetoldEndermanBehavior() {
    }

    public static void disableEyeContactAggro(EnderMan enderman) {
        if (UPDATED_ENDERMEN.contains(enderman)) {
            return;
        }

        enderman.targetSelector.removeAllGoals(RetoldEndermanBehavior::isEyeContactAggroGoal);
        enderman.goalSelector.removeAllGoals(RetoldEndermanBehavior::isEyeContactFreezeGoal);

        UPDATED_ENDERMEN.add(enderman);
    }

    private static boolean isEyeContactAggroGoal(Goal goal) {
        String className = goal.getClass().getName();
        return className.contains("EndermanLookForPlayerGoal");
    }

    private static boolean isEyeContactFreezeGoal(Goal goal) {
        String className = goal.getClass().getName();
        return className.contains("EndermanFreezeWhenLookedAt");
    }
}