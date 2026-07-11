package cz.xefensor.retold.behavior;

import cz.xefensor.retold.combat.RetoldAiTargets;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

public final class RetoldBehaviorTargets {
    private RetoldBehaviorTargets() {
    }

    public static void setTargetAndAggression(
            Mob mob,
            LivingEntity target,
            boolean aggressive
    ) {
        RetoldAiTargets.setTargetAndAggression(
                mob,
                target,
                aggressive
        );
    }

    public static void clearTargetAndAggression(
            Mob mob,
            LivingEntity target,
            boolean stopNavigation
    ) {
        RetoldAiTargets.clearTargetAndAggression(
                mob,
                target,
                stopNavigation
        );
    }

    public static void setAggression(
            Mob mob,
            boolean aggressive
    ) {
        RetoldAiTargets.setAggression(
                mob,
                aggressive
        );
    }
}
