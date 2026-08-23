package cz.xefensor.retold.worldgen.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;

/** Adds portal-drain work only after a vanilla Nether transition succeeds. */
public final class RetoldNetherPortalDrainTravel {
    private RetoldNetherPortalDrainTravel() {
    }

    public static TeleportTransition attachSuccessfulTravelPulse(
            ServerLevel sourceLevel,
            BlockPos sourcePortalPos,
            TeleportTransition transition
    ) {
        boolean leavesOverworld = sourceLevel.dimension() == Level.OVERWORLD
                && transition.newLevel().dimension() == Level.NETHER;
        boolean entersOverworld = sourceLevel.dimension() == Level.NETHER
                && transition.newLevel().dimension() == Level.OVERWORLD;

        if (!leavesOverworld && !entersOverworld) {
            return transition;
        }

        TeleportTransition.PostTeleportTransition drainPulse = entity -> {
            if (leavesOverworld) {
                RetoldNetherPortalDrainEvents.queueTravelPulse(
                        sourceLevel,
                        sourcePortalPos
                );
            } else if (entity.level() instanceof ServerLevel destinationLevel) {
                RetoldNetherPortalDrainEvents.queueTravelPulse(
                        destinationLevel,
                        entity.blockPosition()
                );
            }
        };

        return new TeleportTransition(
                transition.newLevel(),
                transition.position(),
                transition.deltaMovement(),
                transition.yRot(),
                transition.xRot(),
                transition.missingRespawnBlock(),
                transition.asPassenger(),
                transition.relatives(),
                transition.postTeleportTransition().then(drainPulse)
        );
    }
}
