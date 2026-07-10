package cz.xefensor.retold.behavior;

import net.minecraft.world.entity.PathfinderMob;

import java.util.Iterator;

final class RetoldPackLifecycle {
    private static final double PACK_MEMBER_TOO_FAR_BLOCKS = 52.0D;
    private static final double PACK_MEMBER_TOO_FAR_SQUARED =
            PACK_MEMBER_TOO_FAR_BLOCKS * PACK_MEMBER_TOO_FAR_BLOCKS;

    private RetoldPackLifecycle() {
    }

    static void transferLeadership(
            PathfinderMob oldLeader,
            PathfinderMob newLeader,
            RetoldPackParty party
    ) {
        RetoldPackParties.clearMappings(
                oldLeader,
                party
        );

        party.members.remove(newLeader);

        if (
                oldLeader != newLeader
                        && RetoldPackAnimals.isValidPackAnimal(oldLeader)
                        && !party.members.contains(oldLeader)
        ) {
            party.members.add(oldLeader);
        }

        RetoldPackParties.setParty(
                newLeader,
                party
        );

        RetoldPackParties.bindLeaderAndMembers(
                newLeader,
                party
        );
    }

    static void cleanPartyMembers(
            PathfinderMob leader,
            RetoldPackParty party
    ) {
        Iterator<PathfinderMob> iterator = party.members.iterator();

        while (iterator.hasNext()) {
            PathfinderMob member = iterator.next();

            if (!RetoldPackAnimals.isValidPackAnimal(member)) {
                RetoldPackParties.removeLeader(member);
                iterator.remove();
                continue;
            }

            if (leader.distanceToSqr(member) > PACK_MEMBER_TOO_FAR_SQUARED) {
                releaseMember(member);
                iterator.remove();
            }
        }
    }

    static void dissolveParty(
            PathfinderMob leader,
            RetoldPackParty party,
            boolean clearControlledMembers
    ) {
        RetoldPackParties.clearMappings(
                leader,
                party
        );

        if (clearControlledMembers) {
            RetoldPackControl.clearIfOwned(leader);
        }

        for (PathfinderMob member : party.members) {
            if (clearControlledMembers) {
                RetoldPackControl.clearIfOwned(member);
            }
        }

        party.members.clear();
    }

    static void releaseMember(PathfinderMob member) {
        RetoldPackParties.removeLeader(member);
        RetoldPackControl.clearIfOwned(member);
    }
}
