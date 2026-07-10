package cz.xefensor.retold.behavior;

import cz.xefensor.retold.combat.RetoldFactionTargetGuards;
import net.minecraft.world.entity.PathfinderMob;

final class RetoldPackHomeControl {
    private RetoldPackHomeControl() {
    }

    static void clearCombatState(PathfinderMob mob) {
        RetoldFactionTargetGuards.setTargetIgnoringGuard(
                mob,
                null
        );

        RetoldFactionTargetGuards.setAggressiveIgnoringGuard(
                mob,
                false
        );

        RetoldPredatorStrike.clear(mob);
        mob.setSprinting(false);
    }
}
