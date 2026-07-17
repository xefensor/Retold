package cz.xefensor.retold.behavior.pack;

import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.control.RetoldAiControlMode;
import cz.xefensor.retold.behavior.control.RetoldAiControlOwner;
import cz.xefensor.retold.behavior.control.RetoldAiPriorities;
import cz.xefensor.retold.behavior.core.RetoldBehaviorTargets;
import cz.xefensor.retold.behavior.hunting.RetoldPredatorStrike;

import net.minecraft.world.entity.PathfinderMob;

final class RetoldPackControl {
    static final RetoldAiControlOwner OWNER = RetoldAiControlOwner.PACK_HUNTING;

    private static final int PARTY_FEED_PRIORITY = RetoldAiPriorities.above(RetoldAiPriorities.FEED, 2);

    private RetoldPackControl() {
    }

    static boolean claim(
            PathfinderMob mob,
            RetoldAiControlMode mode,
            long gameTime,
            int ticks
    ) {
        if (mode == RetoldAiControlMode.FEED) {
            return RetoldAiControl.tryClaim(
                    mob,
                    mode,
                    OWNER,
                    PARTY_FEED_PRIORITY,
                    "party_feed",
                    gameTime,
                    ticks
            );
        }

        return RetoldAiControl.tryClaim(
                mob,
                mode,
                OWNER,
                gameTime,
                ticks
        );
    }

    static boolean canClearEffects(PathfinderMob mob) {
        return !RetoldAiControl.isControlled(mob)
                || RetoldAiControl.isControlledBy(mob, OWNER);
    }

    static void clearIfOwned(PathfinderMob mob) {
        if (!canClearEffects(mob)) {
            return;
        }

        RetoldAiControl.clearIfOwnedBy(
                mob,
                OWNER
        );

        RetoldBehaviorTargets.setTargetAndAggression(mob, null, false);

        RetoldPredatorStrike.clear(mob);

        if (mob != null) {
            mob.setSprinting(false);
        }
    }
}
