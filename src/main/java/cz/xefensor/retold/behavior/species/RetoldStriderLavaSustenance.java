package cz.xefensor.retold.behavior.species;

import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.control.RetoldAiControlMode;
import cz.xefensor.retold.behavior.control.RetoldAiControlOwner;
import cz.xefensor.retold.behavior.core.RetoldBehaviorMovement;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;
import cz.xefensor.retold.behavior.profiles.RetoldMobState;
import cz.xefensor.retold.behavior.profiles.RetoldMobStates;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.PathfinderMob;

public final class RetoldStriderLavaSustenance {
    private static final int LAVA_RELIEF = 2;
    private static final int LAVA_MEAL_INTERVAL_TICKS = 20 * 10;

    private RetoldStriderLavaSustenance() {
    }

    public static boolean tick(
            ServerLevel level,
            PathfinderMob strider,
            long gameTime
    ) {
        if (!isSustainedByLava(level, strider)) {
            return false;
        }

        stopFoodSearchLeavingLava(strider);

        RetoldMobState state = RetoldMobStates.getOrCreate(strider, gameTime);

        if (state.hunger() <= 0
                || state.lastAteAt() > 0L
                && gameTime - state.lastAteAt() < LAVA_MEAL_INTERVAL_TICKS) {
            return true;
        }

        state.addHunger(-LAVA_RELIEF);
        state.markFed(gameTime);
        return true;
    }

    public static boolean isSustainedByLava(
            ServerLevel level,
            PathfinderMob mob
    ) {
        if (level == null
                || mob == null
                || mob.level() != level
                || !mob.isAlive()
                || mob.isRemoved()
                || !RetoldMobRules.isEntityPath(mob, "strider")) {
            return false;
        }

        return mob.isInLava()
                || level.getFluidState(mob.blockPosition()).is(FluidTags.LAVA)
                || level.getFluidState(mob.blockPosition().below()).is(FluidTags.LAVA);
    }

    private static void stopFoodSearchLeavingLava(PathfinderMob strider) {
        RetoldAiControlMode mode = RetoldAiControl.getMode(strider);

        if (RetoldAiControl.getOwner(strider) != RetoldAiControlOwner.FOOD) {
            return;
        }

        String reason = RetoldAiControl.getReason(strider);
        boolean ordinaryFoodSearch = mode == RetoldAiControlMode.SEARCH
                || mode == RetoldAiControlMode.FEED
                && ("nether_food_search".equals(reason)
                || "seek_food".equals(reason)
                || "use_animal_feeder".equals(reason));

        if (!ordinaryFoodSearch) {
            return;
        }

        RetoldBehaviorMovement.stopOwnedMovement(
                strider,
                RetoldAiControlOwner.FOOD
        );
    }
}
