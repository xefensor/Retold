package cz.xefensor.retold.behavior;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;

import java.util.ArrayList;
import java.util.List;

public final class RetoldPackHuntingDebug {
    private RetoldPackHuntingDebug() {
    }

    public static String buildText(PathfinderMob mob) {
        if (mob == null) {
            return "Retold pack debug\nMob: none";
        }

        PathfinderMob leader = RetoldPackParties.leaderOf(mob);

        if (leader == null) {
            leader = mob;
        }

        RetoldPackParty party = RetoldPackParties.partyOf(leader);

        if (party == null) {
            return format(
                    new PackSnapshot(
                            getDebugEntityName(mob),
                            false,
                            leader == mob ? "none" : getDebugEntityName(leader),
                            0,
                            RetoldAiControl.getMode(mob),
                            getDebugEntityName(mob.getTarget()),
                            RetoldAiControl.getOwner(mob),
                            getControlReasonText(mob),
                            "none",
                            0L,
                            List.of()
                    )
            );
        }

        RetoldPackLifecycle.cleanPartyMembers(
                leader,
                party
        );

        List<PackMemberSnapshot> members = new ArrayList<>();

        members.add(
                createPackMemberDebug(
                        leader,
                        "leader"
                )
        );

        for (PathfinderMob member : party.members) {
            members.add(
                    createPackMemberDebug(
                            member,
                            "member"
                    )
            );
        }

        return format(
                new PackSnapshot(
                        getDebugEntityName(mob),
                        true,
                        getDebugEntityName(leader),
                        1 + party.members.size(),
                        RetoldAiControl.getMode(leader),
                        getDebugEntityName(leader.getTarget()),
                        RetoldAiControl.getOwner(leader),
                        getControlReasonText(leader),
                        party.packCenter.toShortString(),
                        party.createdAt,
                        members
                )
        );
    }

    public static String format(PackSnapshot snapshot) {
        if (snapshot == null) {
            return "Retold pack debug\nMob: none";
        }

        StringBuilder text = new StringBuilder();

        text.append("\nRetold pack debug");
        text.append("\nMob: ").append(snapshot.mobName());

        if (!snapshot.active()) {
            text.append("\nParty: none");
            text.append("\nLeader: ").append(snapshot.leaderName());
            text.append("\nParty size: 0");
            text.append("\nCurrent mode: ").append(snapshot.currentMode());
            text.append("\nPrey: ").append(snapshot.preyName());
            text.append("\nControl owner: ").append(snapshot.controlOwner());
            text.append("\nControl reason: ").append(snapshot.controlReason());
            return text.toString();
        }

        text.append("\nParty: active");
        text.append("\nLeader: ").append(snapshot.leaderName());
        text.append("\nParty size: ").append(snapshot.partySize());
        text.append("\nCurrent mode: ").append(snapshot.currentMode());
        text.append("\nPrey: ").append(snapshot.preyName());
        text.append("\nPack center: ").append(snapshot.packCenter());
        text.append("\nCreated: ").append(snapshot.createdAt());
        text.append("\nMembers:");

        for (PackMemberSnapshot member : snapshot.members()) {
            text.append("\n- ");
            text.append(member.role());
            text.append(" ");
            text.append(member.mobName());
            text.append(" mode=").append(member.mode());
            text.append(" owner=").append(member.owner());
            text.append(" reason=").append(member.reason());
            text.append(" prey=").append(member.preyName());
        }

        return text.toString();
    }

    private static PackMemberSnapshot createPackMemberDebug(
            PathfinderMob mob,
            String role
    ) {
        if (mob == null) {
            return new PackMemberSnapshot(
                    role,
                    "none",
                    RetoldAiControlMode.NONE,
                    RetoldAiControlOwner.SYSTEM,
                    "none",
                    "none"
            );
        }

        return new PackMemberSnapshot(
                role,
                getDebugEntityName(mob),
                RetoldAiControl.getMode(mob),
                RetoldAiControl.getOwner(mob),
                getControlReasonText(mob),
                getDebugEntityName(mob.getTarget())
        );
    }

    private static String getDebugEntityName(LivingEntity entity) {
        if (entity == null) {
            return "none";
        }

        return RetoldMobRules.getEntityTypePath(
                entity.getType()
        ) + " #" + entity.getId();
    }

    private static String getControlReasonText(PathfinderMob mob) {
        String reason = RetoldAiControl.getReason(mob);

        return reason == null || reason.isBlank()
                ? "none"
                : reason;
    }

    public record PackSnapshot(
            String mobName,
            boolean active,
            String leaderName,
            int partySize,
            RetoldAiControlMode currentMode,
            String preyName,
            RetoldAiControlOwner controlOwner,
            String controlReason,
            String packCenter,
            long createdAt,
            List<PackMemberSnapshot> members
    ) {
    }

    public record PackMemberSnapshot(
            String role,
            String mobName,
            RetoldAiControlMode mode,
            RetoldAiControlOwner owner,
            String reason,
            String preyName
    ) {
    }
}
