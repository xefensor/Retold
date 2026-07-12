package cz.xefensor.retold.behavior;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;

final class RetoldPackCombat {
    private static final int PARTY_HUNT_CONTROL_TICKS = 20 * 5;
    private static final int PARTY_HUNT_PATH_INTERVAL_TICKS = 8;

    private RetoldPackCombat() {
    }

    static void forcePartyHunt(
            PathfinderMob leader,
            RetoldPackParty party,
            LivingEntity prey,
            long gameTime
    ) {
        if (!RetoldPackSenses.isValidPartyPrey(leader, prey, gameTime)) {
            return;
        }

        if (!RetoldPackControl.claim(
                leader,
                RetoldAiControlMode.HUNT,
                gameTime,
                PARTY_HUNT_CONTROL_TICKS
        )) {
            return;
        }

        leader.setSprinting(true);

        if (!RetoldBehaviorTargets.setTargetAndAggression(leader, prey, true)) {
            RetoldPackControl.clearIfOwned(leader);
            return;
        }

        leader.getLookControl().setLookAt(
                prey,
                35.0F,
                35.0F
        );

        RetoldBehaviorMovement.throttledMoveTo(
                leader,
                prey,
                RetoldPackTuning.huntSpeed(getPath(leader)),
                gameTime,
                PARTY_HUNT_PATH_INTERVAL_TICKS,
                2.5D * 2.5D
        );

        RetoldPackLifecycle.cleanPartyMembers(
                leader,
                party
        );

        RetoldPackMovement.updatePartyHunt(
                leader,
                party,
                prey,
                gameTime
        );
    }

    private static String getPath(PathfinderMob mob) {
        return RetoldMobRules.getEntityTypePath(
                mob.getType()
        );
    }
}
