package cz.xefensor.retold.behavior;

import cz.xefensor.retold.combat.RetoldFactionTargetGuards;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;

final class RetoldPackCombat {
    private static final int PARTY_HUNT_CONTROL_TICKS = 20 * 5;

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

        RetoldFactionTargetGuards.setTargetIgnoringGuard(
                leader,
                prey
        );

        RetoldFactionTargetGuards.setAggressiveIgnoringGuard(
                leader,
                true
        );

        leader.getLookControl().setLookAt(
                prey,
                35.0F,
                35.0F
        );

        RetoldAiControl.withNavigationBypass(() -> {
            leader.getNavigation().moveTo(
                    prey,
                    RetoldPackTuning.huntSpeed(getPath(leader))
            );
        });

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
