package cz.xefensor.retold.behavior.pack;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;

final class RetoldPackMovement {
    private RetoldPackMovement() {
    }

    static void updatePartySearch(
            PathfinderMob leader,
            RetoldPackParty party,
            long gameTime
    ) {
        RetoldPackSearchMovement.updatePartySearch(
                leader,
                party,
                gameTime
        );
    }

    static void updatePartyHunt(
            PathfinderMob leader,
            RetoldPackParty party,
            LivingEntity prey,
            long gameTime
    ) {
        RetoldPackHuntMovement.updatePartyHunt(
                leader,
                party,
                prey,
                gameTime
        );
    }

    static boolean updatePartyReturn(
            PathfinderMob leader,
            RetoldPackParty party,
            long gameTime
    ) {
        return RetoldPackReturnMovement.updatePartyReturn(
                leader,
                party,
                gameTime
        );
    }

    static void startLeaderSearch(
            PathfinderMob leader,
            RetoldPackParty party,
            long gameTime
    ) {
        RetoldPackSearchMovement.startLeaderSearch(
                leader,
                party,
                gameTime
        );
    }

    static void holdPartyTogetherNearLeader(
            PathfinderMob leader,
            RetoldPackParty party,
            long gameTime
    ) {
        RetoldPackReturnMovement.holdPartyTogetherNearLeader(
                leader,
                party,
                gameTime
        );
    }

    static void holdMemberNearLeader(
            PathfinderMob leader,
            PathfinderMob member,
            long gameTime
    ) {
        RetoldPackReturnMovement.holdMemberNearLeader(
                leader,
                member,
                gameTime
        );
    }

    static void moveMemberInSearchFormation(
            PathfinderMob leader,
            PathfinderMob member,
            RetoldPackParty party,
            long gameTime
    ) {
        RetoldPackSearchMovement.moveMemberInSearchFormation(
                leader,
                member,
                party,
                gameTime
        );
    }

    static void moveMemberInHunt(
            PathfinderMob member,
            LivingEntity prey,
            String leaderPath,
            long gameTime
    ) {
        RetoldPackHuntMovement.moveMemberInHunt(
                member,
                prey,
                leaderPath,
                gameTime
        );
    }

    static boolean moveMemberBackToPack(
            PathfinderMob member,
            BlockPos packCenter,
            long gameTime
    ) {
        return RetoldPackReturnMovement.moveMemberBackToPack(
                member,
                packCenter,
                gameTime
        );
    }
}
