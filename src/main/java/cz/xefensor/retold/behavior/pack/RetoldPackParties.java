package cz.xefensor.retold.behavior.pack;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

final class RetoldPackParties {
    private static final Map<PathfinderMob, RetoldPackParty> PARTIES_BY_LEADER = new WeakHashMap<>();
    private static final Map<PathfinderMob, PathfinderMob> LEADER_BY_MEMBER = new WeakHashMap<>();

    private RetoldPackParties() {
    }

    static PathfinderMob leaderOf(PathfinderMob member) {
        return LEADER_BY_MEMBER.get(member);
    }

    static RetoldPackParty partyOf(PathfinderMob leader) {
        return PARTIES_BY_LEADER.get(leader);
    }

    static void setParty(
            PathfinderMob leader,
            RetoldPackParty party
    ) {
        PARTIES_BY_LEADER.put(
                leader,
                party
        );
    }

    static void setLeader(
            PathfinderMob member,
            PathfinderMob leader
    ) {
        LEADER_BY_MEMBER.put(
                member,
                leader
        );
    }

    static boolean hasLeader(PathfinderMob member) {
        return LEADER_BY_MEMBER.containsKey(member);
    }

    static RetoldPackParty createParty(
            PathfinderMob leader,
            List<PathfinderMob> members,
            BlockPos packCenter,
            long gameTime
    ) {
        RetoldPackParty party = new RetoldPackParty(
                packCenter,
                gameTime
        );

        if (members != null) {
            party.members.addAll(members);
        }

        setParty(
                leader,
                party
        );
        bindLeaderAndMembers(
                leader,
                party
        );

        return party;
    }

    static void addMember(
            PathfinderMob leader,
            RetoldPackParty party,
            PathfinderMob member
    ) {
        if (party == null || member == null) {
            return;
        }

        if (!party.members.contains(member)) {
            party.members.add(member);
        }

        setLeader(
                member,
                leader
        );
    }

    static void bindLeaderAndMembers(
            PathfinderMob leader,
            RetoldPackParty party
    ) {
        if (leader == null || party == null) {
            return;
        }

        setLeader(
                leader,
                leader
        );

        for (PathfinderMob member : party.members) {
            setLeader(
                    member,
                    leader
            );
        }
    }

    static void clearMappings(
            PathfinderMob leader,
            RetoldPackParty party
    ) {
        removeParty(leader);
        removeLeader(leader);

        if (party == null) {
            return;
        }

        for (PathfinderMob member : party.members) {
            removeLeader(member);
        }
    }

    static void removeParty(PathfinderMob leader) {
        PARTIES_BY_LEADER.remove(leader);
    }

    static void removeLeader(PathfinderMob member) {
        LEADER_BY_MEMBER.remove(member);
    }
}
