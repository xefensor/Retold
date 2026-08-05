package cz.xefensor.retold.behavior.species;

import cz.xefensor.retold.behavior.profiles.RetoldMobState;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;
import cz.xefensor.retold.behavior.profiles.RetoldMobStates;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.MobSplitEvent;

public final class RetoldSlimeSplitBehavior {
    private RetoldSlimeSplitBehavior() {
    }

    @SubscribeEvent
    public static void onMobSplit(MobSplitEvent event) {
        if (!(event.getParent() instanceof AbstractCubeMob parent)
                || !RetoldMobRules.isSlimeHungry(parent)) {
            return;
        }

        long gameTime = parent.level().getGameTime();

        for (Mob mob : event.getChildren()) {
            if (mob instanceof AbstractCubeMob child) {
                inheritHalfHunger(
                        parent,
                        child,
                        gameTime
                );
            }
        }
    }

    public static void inheritHalfHunger(
            AbstractCubeMob parent,
            AbstractCubeMob child,
            long gameTime
    ) {
        if (parent == null || child == null || parent == child) {
            return;
        }

        RetoldMobState parentState = RetoldMobStates.getOrCreate(
                parent,
                gameTime
        );
        int inheritedHunger = parentState.hunger() / 2;

        RetoldMobStates.remove(child);
        RetoldMobStates.getOrCreate(child, gameTime)
                .setHunger(inheritedHunger);
    }
}
