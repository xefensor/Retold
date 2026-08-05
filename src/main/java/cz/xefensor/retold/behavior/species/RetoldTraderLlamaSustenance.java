package cz.xefensor.retold.behavior.species;

import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.control.RetoldAiControlMode;
import cz.xefensor.retold.behavior.control.RetoldAiControlOwner;
import cz.xefensor.retold.behavior.core.RetoldBehaviorMovement;
import cz.xefensor.retold.behavior.food.RetoldFeedingAnimations;
import cz.xefensor.retold.behavior.food.RetoldFeedingPose;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;
import cz.xefensor.retold.behavior.profiles.RetoldMobState;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;

public final class RetoldTraderLlamaSustenance {
    private static final int CARAVAN_FODDER_RELIEF = 8;
    private static final int CARAVAN_FODDER_INTERVAL_TICKS = 20 * 30;

    private RetoldTraderLlamaSustenance() {
    }

    public static boolean tick(
            ServerLevel level,
            PathfinderMob llama,
            RetoldMobState state,
            long gameTime
    ) {
        Entity leashHolder = llama == null ? null : llama.getLeashHolder();

        if (level == null
                || llama == null
                || state == null
                || llama.level() != level
                || !llama.isAlive()
                || llama.isRemoved()
                || !RetoldMobRules.isEntityPath(llama, "trader_llama")
                || !(leashHolder instanceof WanderingTrader trader)
                || trader.level() != level
                || !trader.isAlive()
                || trader.isRemoved()) {
            return false;
        }

        stopFoodSearchLeavingCaravan(llama);

        if (state.hunger() <= 0
                || state.lastAteAt() > 0L
                && gameTime - state.lastAteAt() < CARAVAN_FODDER_INTERVAL_TICKS) {
            return true;
        }

        state.addHunger(-CARAVAN_FODDER_RELIEF);
        state.markFed(gameTime);
        RetoldFeedingAnimations.play(llama);
        RetoldFeedingPose.begin(llama, trader.position(), gameTime);
        return true;
    }

    private static void stopFoodSearchLeavingCaravan(PathfinderMob llama) {
        if (RetoldAiControl.getOwner(llama) != RetoldAiControlOwner.FOOD) {
            return;
        }

        RetoldAiControlMode mode = RetoldAiControl.getMode(llama);
        String reason = RetoldAiControl.getReason(llama);

        if (mode == RetoldAiControlMode.SEARCH
                || mode == RetoldAiControlMode.FEED
                && !"feeding_pose".equals(reason)) {
            RetoldBehaviorMovement.stopOwnedMovement(
                    llama,
                    RetoldAiControlOwner.FOOD
            );
        }
    }
}
