package cz.xefensor.retold.behavior;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.item.ItemEntity;

final class RetoldPackFeeding {
    private static final int PARTY_FEED_CONTROL_TICKS = 20 * 8;
    private static final double PARTY_FEED_SPEED = 0.95D;

    private RetoldPackFeeding() {
    }

    static boolean feedHungryPartyMembersFromSharedFood(
            ServerLevel level,
            PathfinderMob leader,
            RetoldPackParty party,
            long gameTime
    ) {
        ItemEntity seenFood = RetoldPackSenses.findBestFoodSeenByParty(
                level,
                leader,
                party
        );

        if (seenFood == null) {
            return false;
        }

        boolean sentSomeoneToFood = false;

        if (isHungryEnoughToEat(leader, gameTime)) {
            sentSomeoneToFood |= sendMemberToFood(
                    leader,
                    seenFood,
                    gameTime
            );
        }

        for (PathfinderMob member : party.members) {
            if (!isHungryEnoughToEat(member, gameTime)) {
                continue;
            }

            sentSomeoneToFood |= sendMemberToFood(
                    member,
                    seenFood,
                    gameTime
            );
        }

        return sentSomeoneToFood;
    }

    private static boolean sendMemberToFood(
            PathfinderMob member,
            ItemEntity seenFood,
            long gameTime
    ) {
        if (member == null || seenFood == null) {
            return false;
        }

        if (!member.isAlive() || member.isRemoved()) {
            return false;
        }

        if (!seenFood.isAlive() || seenFood.isRemoved() || seenFood.getItem().isEmpty()) {
            return false;
        }

        if (!RetoldMobRules.canEatDroppedItem(member, seenFood.getItem())) {
            return false;
        }

        if (!canOverrideMemberForFood(member)) {
            return false;
        }

        RetoldBehaviorTargets.setTargetAndAggression(member, null, false);

        RetoldPredatorStrike.clear(member);

        member.setSprinting(false);

        if (!RetoldPackControl.claim(
                member,
                RetoldAiControlMode.FEED,
                gameTime,
                PARTY_FEED_CONTROL_TICKS
        )) {
            return false;
        }

        member.getNavigation().stop();

        RetoldAiControl.withNavigationBypass(() -> {
            member.getNavigation().moveTo(
                    seenFood,
                    PARTY_FEED_SPEED
            );
        });

        return true;
    }

    private static boolean isHungryEnoughToEat(
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

        return state.hunger() >= RetoldMobRules.eatThreshold(member);
    }

    private static boolean canOverrideMemberForFood(PathfinderMob member) {
        RetoldAiControlMode mode = RetoldAiControl.getMode(member);

        return mode == RetoldAiControlMode.NONE
                || mode == RetoldAiControlMode.SEARCH
                || mode == RetoldAiControlMode.HUNT
                || mode == RetoldAiControlMode.REGROUP
                || mode == RetoldAiControlMode.FEED;
    }

}
