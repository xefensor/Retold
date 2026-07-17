package cz.xefensor.retold.behavior.pack;

import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.control.RetoldAiControlMode;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;

final class RetoldPackUpdates {
    private RetoldPackUpdates() {
    }

    static void updateLeaderParty(
            ServerLevel level,
            PathfinderMob leader,
            RetoldPackParty party,
            long gameTime
    ) {
        if (!RetoldPackAnimals.isValidPackAnimal(leader)) {
            RetoldPackLifecycle.dissolveParty(
                    leader,
                    party,
                    false
            );
            return;
        }

        RetoldPackLifecycle.cleanPartyMembers(
                leader,
                party
        );

        /*
         * Highest priority:
         * if any party member sees edible food, hungry party members go eat.
         * The party stays alive so they can regroup afterward.
         */
        if (RetoldPackFeeding.feedHungryPartyMembersFromSharedFood(level, leader, party, gameTime)) {
            return;
        }

        RetoldPackRecruitment.recruitPartyIfOpen(
                level,
                leader,
                party,
                gameTime
        );

        RetoldAiControlMode leaderMode = RetoldAiControl.getMode(leader);

        if (leaderMode == RetoldAiControlMode.HUNT) {
            LivingEntity prey = leader.getTarget();

            if (RetoldPackSenses.isValidPartyPrey(leader, prey, gameTime)) {
                updatePartyHunt(
                        leader,
                        party,
                        prey,
                        gameTime
                );
                return;
            }
        }

        LivingEntity preySeenByParty = RetoldPackSenses.findPreySensedByAnyPartyMember(
                level,
                leader,
                party,
                gameTime
        );

        if (preySeenByParty != null) {
            RetoldPackCombat.forcePartyHunt(
                    leader,
                    party,
                    preySeenByParty,
                    gameTime
            );
            return;
        }

        if (leaderMode == RetoldAiControlMode.SEARCH) {
            updatePartySearch(
                    leader,
                    party,
                    gameTime
            );
            return;
        }

        PathfinderMob hungryLeader = RetoldPackHunger.findHungryAvailablePartyLeader(
                leader,
                party,
                gameTime
        );

        if (hungryLeader != null) {
            if (hungryLeader != leader) {
                RetoldPackLifecycle.transferLeadership(
                        leader,
                        hungryLeader,
                        party
                );

                RetoldPackParty transferredParty = RetoldPackParties.partyOf(hungryLeader);

                if (transferredParty != null) {
                    RetoldPackMovement.startLeaderSearch(
                            hungryLeader,
                            transferredParty,
                            gameTime
                    );

                    updateLeaderParty(
                            level,
                            hungryLeader,
                            transferredParty,
                            gameTime
                    );
                }

                return;
            }

            RetoldPackMovement.startLeaderSearch(
                    leader,
                    party,
                    gameTime
            );

            updatePartySearch(
                    leader,
                    party,
                    gameTime
            );
            return;
        }

        if (RetoldPackHunger.hasAnyHungryPartyMember(leader, party, gameTime)) {
            RetoldPackMovement.holdPartyTogetherNearLeader(
                    leader,
                    party,
                    gameTime
            );
            return;
        }

        updatePartyReturn(
                leader,
                party,
                gameTime
        );
    }

    static void updateMember(
            ServerLevel level,
            PathfinderMob leader,
            PathfinderMob member,
            RetoldPackParty party,
            long gameTime
    ) {
        if (!RetoldPackAnimals.isValidPackAnimal(member)) {
            RetoldPackLifecycle.releaseMember(member);
            return;
        }

        /*
         * Member-side food broadcast:
         * if this member sees meat, hungry party members are sent to eat.
         */
        if (RetoldPackFeeding.feedHungryPartyMembersFromSharedFood(level, leader, party, gameTime)) {
            return;
        }

        RetoldAiControlMode leaderMode = RetoldAiControl.getMode(leader);

        if (leaderMode == RetoldAiControlMode.SEARCH) {
            LivingEntity prey = RetoldPackSenses.findSensedPreyForSensor(
                    level,
                    member,
                    gameTime
            );

            if (prey != null) {
                RetoldPackCombat.forcePartyHunt(
                        leader,
                        party,
                        prey,
                        gameTime
                );
                return;
            }

            RetoldPackMovement.moveMemberInSearchFormation(
                    leader,
                    member,
                    party,
                    gameTime
            );
            return;
        }

        if (leaderMode == RetoldAiControlMode.HUNT) {
            LivingEntity prey = leader.getTarget();

            if (RetoldPackSenses.isValidPartyPrey(member, prey, gameTime)) {
                RetoldPackMovement.moveMemberInHunt(
                        member,
                        prey,
                        getPath(leader),
                        gameTime
                );
                return;
            }
        }

        if (RetoldPackHunger.hasAnyHungryPartyMember(leader, party, gameTime)) {
            RetoldPackMovement.holdMemberNearLeader(
                    leader,
                    member,
                    gameTime
            );
            return;
        }

        RetoldPackMovement.moveMemberBackToPack(
                member,
                party.packCenter,
                gameTime
        );
    }

    private static void updatePartySearch(
            PathfinderMob leader,
            RetoldPackParty party,
            long gameTime
    ) {
        RetoldPackLifecycle.cleanPartyMembers(
                leader,
                party
        );

        RetoldPackMovement.updatePartySearch(
                leader,
                party,
                gameTime
        );
    }

    private static void updatePartyHunt(
            PathfinderMob leader,
            RetoldPackParty party,
            LivingEntity prey,
            long gameTime
    ) {
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

    private static void updatePartyReturn(
            PathfinderMob leader,
            RetoldPackParty party,
            long gameTime
    ) {
        RetoldPackLifecycle.cleanPartyMembers(
                leader,
                party
        );

        if (RetoldPackMovement.updatePartyReturn(
                leader,
                party,
                gameTime
        )) {
            RetoldPackLifecycle.dissolveParty(
                    leader,
                    party,
                    true
            );
        }
    }

    private static String getPath(PathfinderMob mob) {
        return RetoldMobRules.getEntityTypePath(
                mob.getType()
        );
    }
}
