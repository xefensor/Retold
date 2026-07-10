package cz.xefensor.retold.behavior;

import cz.xefensor.retold.combat.RetoldFactionTargetGuards;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.Vec3;

final class RetoldPackSearchMovement {
    private static final int PARTY_SEARCH_CONTROL_TICKS = 20 * 6;
    private static final int PARTY_SEARCH_DIRECTION_LIFE_TICKS = 20 * 45;

    private static final double SEARCH_FORWARD_BLOCKS = 14.0D;
    private static final double SEARCH_SIDE_BLOCKS = 5.5D;

    private RetoldPackSearchMovement() {
    }

    static void updatePartySearch(
            PathfinderMob leader,
            RetoldPackParty party,
            long gameTime
    ) {
        getPartySearchDirection(
                leader,
                party,
                gameTime
        );

        int index = 0;

        for (PathfinderMob member : party.members) {
            moveMemberInSearchFormation(
                    leader,
                    member,
                    party,
                    gameTime,
                    index
            );

            index++;
        }
    }

    static void startLeaderSearch(
            PathfinderMob leader,
            RetoldPackParty party,
            long gameTime
    ) {
        if (!RetoldPackControl.claim(
                leader,
                RetoldAiControlMode.SEARCH,
                gameTime,
                PARTY_SEARCH_CONTROL_TICKS
        )) {
            return;
        }

        RetoldFactionTargetGuards.setTargetIgnoringGuard(
                leader,
                null
        );

        RetoldFactionTargetGuards.setAggressiveIgnoringGuard(
                leader,
                false
        );

        RetoldPredatorStrike.clear(leader);

        leader.setSprinting(false);

        Vec3 direction = getPartySearchDirection(
                leader,
                party,
                gameTime
        );

        Vec3 target = leader.position()
                .add(direction.scale(SEARCH_FORWARD_BLOCKS));

        leader.getLookControl().setLookAt(
                target.x,
                target.y + 0.5D,
                target.z,
                25.0F,
                25.0F
        );

        RetoldAiControl.withNavigationBypass(() -> {
            leader.getNavigation().moveTo(
                    target.x,
                    target.y,
                    target.z,
                    RetoldPackTuning.searchSpeed(RetoldPackMovementRules.getPath(leader))
            );
        });
    }

    static void moveMemberInSearchFormation(
            PathfinderMob leader,
            PathfinderMob member,
            RetoldPackParty party,
            long gameTime
    ) {
        int index = party.members.indexOf(member);

        if (index < 0) {
            index = 0;
        }

        moveMemberInSearchFormation(
                leader,
                member,
                party,
                gameTime,
                index
        );
    }

    private static void moveMemberInSearchFormation(
            PathfinderMob leader,
            PathfinderMob member,
            RetoldPackParty party,
            long gameTime,
            int index
    ) {
        if (!RetoldPackMovementRules.canOverrideMemberMode(member)) {
            return;
        }

        if (!RetoldPackControl.claim(
                member,
                RetoldAiControlMode.SEARCH,
                gameTime,
                PARTY_SEARCH_CONTROL_TICKS
        )) {
            return;
        }

        RetoldFactionTargetGuards.setTargetIgnoringGuard(
                member,
                null
        );

        RetoldFactionTargetGuards.setAggressiveIgnoringGuard(
                member,
                false
        );

        member.setSprinting(false);

        Vec3 target = getSearchFormationPoint(
                leader,
                member,
                party,
                gameTime,
                index
        );

        member.getLookControl().setLookAt(
                target.x,
                target.y + 0.5D,
                target.z,
                25.0F,
                25.0F
        );

        RetoldAiControl.withNavigationBypass(() -> {
            member.getNavigation().moveTo(
                    target.x,
                    target.y,
                    target.z,
                    RetoldPackTuning.searchSpeed(RetoldPackMovementRules.getPath(leader))
            );
        });
    }

    private static Vec3 getSearchFormationPoint(
            PathfinderMob leader,
            PathfinderMob member,
            RetoldPackParty party,
            long gameTime,
            int index
    ) {
        Vec3 direction = getPartySearchDirection(
                leader,
                party,
                gameTime
        );

        Vec3 side = new Vec3(
                -direction.z,
                0.0D,
                direction.x
        );

        double sideOffset = switch (index % 6) {
            case 0 -> SEARCH_SIDE_BLOCKS;
            case 1 -> -SEARCH_SIDE_BLOCKS;
            case 2 -> SEARCH_SIDE_BLOCKS * 0.5D;
            case 3 -> -SEARCH_SIDE_BLOCKS * 0.5D;
            case 4 -> SEARCH_SIDE_BLOCKS * 1.45D;
            default -> -SEARCH_SIDE_BLOCKS * 1.45D;
        };

        double forwardOffset = SEARCH_FORWARD_BLOCKS + (index / 2) * 2.0D;

        Vec3 target = leader.position()
                .add(direction.scale(forwardOffset))
                .add(side.scale(sideOffset));

        return new Vec3(
                target.x,
                member.getY(),
                target.z
        );
    }

    private static Vec3 getPartySearchDirection(
            PathfinderMob leader,
            RetoldPackParty party,
            long gameTime
    ) {
        if (
                party.searchDirection == null
                        || party.searchDirection.lengthSqr() <= 0.0001D
                        || gameTime > party.searchDirectionExpiresAt
        ) {
            party.searchDirection = RetoldPackGeometry.leaderDirection(
                    leader
            );

            party.searchDirectionExpiresAt = gameTime + PARTY_SEARCH_DIRECTION_LIFE_TICKS;
        }

        return RetoldPackGeometry.safeDirection(
                leader,
                party.searchDirection
        );
    }
}
