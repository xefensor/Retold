package cz.xefensor.retold.behavior;

import cz.xefensor.retold.combat.RetoldFactionTargetGuards;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;

final class RetoldPackHuntMovement {
    private static final int PARTY_HUNT_CONTROL_TICKS = 20 * 5;

    private RetoldPackHuntMovement() {
    }

    static void updatePartyHunt(
            PathfinderMob leader,
            RetoldPackParty party,
            LivingEntity prey,
            long gameTime
    ) {
        String path = RetoldPackMovementRules.getPath(leader);

        for (PathfinderMob member : party.members) {
            if (!RetoldPackSenses.isValidPartyPrey(member, prey, gameTime)) {
                RetoldPackReturnMovement.holdMemberNearLeader(
                        leader,
                        member,
                        gameTime
                );
                continue;
            }

            moveMemberInHunt(
                    member,
                    prey,
                    path,
                    gameTime
            );
        }
    }

    static void moveMemberInHunt(
            PathfinderMob member,
            LivingEntity prey,
            String leaderPath,
            long gameTime
    ) {
        if (!RetoldPackMovementRules.canOverrideMemberMode(member)) {
            return;
        }

        if (!RetoldPackControl.claim(
                member,
                RetoldAiControlMode.HUNT,
                gameTime,
                PARTY_HUNT_CONTROL_TICKS
        )) {
            return;
        }

        member.setSprinting(true);

        RetoldFactionTargetGuards.setTargetIgnoringGuard(
                member,
                prey
        );

        RetoldFactionTargetGuards.setAggressiveIgnoringGuard(
                member,
                true
        );

        member.getLookControl().setLookAt(
                prey,
                35.0F,
                35.0F
        );

        RetoldAiControl.withNavigationBypass(() -> {
            member.getNavigation().moveTo(
                    prey,
                    RetoldPackTuning.huntSpeed(leaderPath)
            );
        });
    }
}
