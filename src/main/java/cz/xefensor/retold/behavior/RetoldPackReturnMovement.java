package cz.xefensor.retold.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.Vec3;

final class RetoldPackReturnMovement {
    private static final int PARTY_RETURN_CONTROL_TICKS = 20 * 6;
    private static final int PARTY_RETURN_PATH_INTERVAL_TICKS = 10;

    private static final double RETURN_TO_PACK_SPEED = 0.86D;

    private static final double PACK_RETURN_DISTANCE_BLOCKS = 7.0D;
    private static final double PACK_RETURN_DISTANCE_SQUARED =
            PACK_RETURN_DISTANCE_BLOCKS * PACK_RETURN_DISTANCE_BLOCKS;

    private RetoldPackReturnMovement() {
    }

    static boolean updatePartyReturn(
            PathfinderMob leader,
            RetoldPackParty party,
            long gameTime
    ) {
        boolean leaderReturned = moveLeaderBackToPackIfFree(
                leader,
                party.packCenter,
                gameTime
        );

        boolean allMembersReturned = true;

        for (PathfinderMob member : party.members) {
            boolean memberReturned = moveMemberBackToPack(
                    member,
                    party.packCenter,
                    gameTime
            );

            if (!memberReturned) {
                allMembersReturned = false;
            }
        }

        return leaderReturned && allMembersReturned;
    }

    static void holdPartyTogetherNearLeader(
            PathfinderMob leader,
            RetoldPackParty party,
            long gameTime
    ) {
        int index = 0;

        for (PathfinderMob member : party.members) {
            holdMemberNearLeader(
                    leader,
                    member,
                    gameTime,
                    index
            );

            index++;
        }
    }

    static void holdMemberNearLeader(
            PathfinderMob leader,
            PathfinderMob member,
            long gameTime
    ) {
        int index = Math.max(
                0,
                member.getId() % 4
        );

        holdMemberNearLeader(
                leader,
                member,
                gameTime,
                index
        );
    }

    private static void holdMemberNearLeader(
            PathfinderMob leader,
            PathfinderMob member,
            long gameTime,
            int index
    ) {
        if (!RetoldPackMovementRules.canOverrideMemberMode(member)) {
            return;
        }

        if (!RetoldPackControl.claim(
                member,
                RetoldAiControlMode.REGROUP,
                gameTime,
                PARTY_RETURN_CONTROL_TICKS
        )) {
            return;
        }

        RetoldBehaviorTargets.setTargetAndAggression(member, null, false);

        RetoldPredatorStrike.clear(member);

        member.setSprinting(false);

        Vec3 direction = RetoldPackGeometry.leaderDirection(
                leader
        );

        Vec3 side = new Vec3(
                -direction.z,
                0.0D,
                direction.x
        );

        double sideOffset = switch (index % 4) {
            case 0 -> 3.0D;
            case 1 -> -3.0D;
            case 2 -> 5.0D;
            default -> -5.0D;
        };

        Vec3 target = leader.position()
                .add(side.scale(sideOffset));

        RetoldBehaviorMovement.throttledMoveTo(
                member,
                target.x,
                member.getY(),
                target.z,
                RETURN_TO_PACK_SPEED,
                gameTime,
                PARTY_RETURN_PATH_INTERVAL_TICKS,
                2.0D * 2.0D
        );
    }

    static boolean moveMemberBackToPack(
            PathfinderMob member,
            BlockPos packCenter,
            long gameTime
    ) {
        if (RetoldPackMovementRules.isBusyReturnMode(member)) {
            return false;
        }

        return moveBackToPack(
                member,
                packCenter,
                gameTime
        );
    }

    private static boolean moveLeaderBackToPackIfFree(
            PathfinderMob leader,
            BlockPos packCenter,
            long gameTime
    ) {
        if (RetoldPackMovementRules.isBusyReturnMode(leader)) {
            return false;
        }

        return moveBackToPack(
                leader,
                packCenter,
                gameTime
        );
    }

    private static boolean moveBackToPack(
            PathfinderMob mob,
            BlockPos packCenter,
            long gameTime
    ) {
        if (RetoldAiControl.isControlled(mob) && !RetoldAiControl.isControlledBy(mob, RetoldPackControl.OWNER)) {
            return false;
        }

        if (mob.blockPosition().distSqr(packCenter) <= PACK_RETURN_DISTANCE_SQUARED) {
            RetoldAiControl.clearIfControlledAsByAny(
                    mob,
                    RetoldPackControl.OWNER,
                    RetoldAiControlMode.REGROUP,
                    RetoldAiControlMode.SEARCH,
                    RetoldAiControlMode.HUNT
            );

            RetoldBehaviorTargets.setTargetAndAggression(mob, null, false);

            RetoldPredatorStrike.clear(mob);
            mob.setSprinting(false);
            mob.getNavigation().stop();
            return true;
        }

        if (!RetoldPackControl.claim(
                mob,
                RetoldAiControlMode.REGROUP,
                gameTime,
                PARTY_RETURN_CONTROL_TICKS
        )) {
            return false;
        }

        RetoldBehaviorTargets.setTargetAndAggression(mob, null, false);

        RetoldPredatorStrike.clear(mob);

        mob.setSprinting(false);

        RetoldBehaviorMovement.throttledMoveTo(
                mob,
                packCenter,
                RETURN_TO_PACK_SPEED,
                gameTime,
                PARTY_RETURN_PATH_INTERVAL_TICKS,
                2.0D * 2.0D
        );

        return false;
    }
}
