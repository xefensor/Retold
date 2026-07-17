package cz.xefensor.retold.behavior.pack;

import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.control.RetoldAiControlMode;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;
import cz.xefensor.retold.behavior.profiles.RetoldMobState;
import cz.xefensor.retold.behavior.profiles.RetoldMobStates;

import net.minecraft.world.entity.PathfinderMob;

final class RetoldPackHunger {
    private RetoldPackHunger() {
    }

    static PathfinderMob findHungryAvailablePartyLeader(
            PathfinderMob leader,
            RetoldPackParty party,
            long gameTime
    ) {
        PathfinderMob best = null;
        int bestHunger = -1;

        if (isHungryEnoughToContinueParty(leader, gameTime) && canLeadContinuedSearch(leader)) {
            best = leader;
            bestHunger = RetoldMobStates.getOrCreate(
                    leader,
                    gameTime
            ).hunger();
        }

        for (PathfinderMob member : party.members) {
            if (!isHungryEnoughToContinueParty(member, gameTime)) {
                continue;
            }

            if (!canLeadContinuedSearch(member)) {
                continue;
            }

            int hunger = RetoldMobStates.getOrCreate(
                    member,
                    gameTime
            ).hunger();

            if (hunger > bestHunger) {
                best = member;
                bestHunger = hunger;
            }
        }

        return best;
    }

    static boolean hasAnyHungryPartyMember(
            PathfinderMob leader,
            RetoldPackParty party,
            long gameTime
    ) {
        if (isHungryEnoughToContinueParty(leader, gameTime)) {
            return true;
        }

        for (PathfinderMob member : party.members) {
            if (isHungryEnoughToContinueParty(member, gameTime)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isHungryEnoughToContinueParty(
            PathfinderMob member,
            long gameTime
    ) {
        if (!RetoldPackAnimals.isValidPackAnimal(member)) {
            return false;
        }

        RetoldMobState state = RetoldMobStates.getOrCreate(
                member,
                gameTime
        );

        return RetoldMobRules.hasHuntDrive(member, state);
    }

    private static boolean canLeadContinuedSearch(PathfinderMob member) {
        RetoldAiControlMode mode = RetoldAiControl.getMode(member);

        return mode == RetoldAiControlMode.NONE
                || mode == RetoldAiControlMode.SEARCH
                || mode == RetoldAiControlMode.HUNT
                || mode == RetoldAiControlMode.REGROUP;
    }
}
