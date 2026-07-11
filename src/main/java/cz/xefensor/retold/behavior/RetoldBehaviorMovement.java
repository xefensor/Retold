package cz.xefensor.retold.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;

public final class RetoldBehaviorMovement {
    private RetoldBehaviorMovement() {
    }

    public static boolean claimAndMoveToBlock(
            PathfinderMob mob,
            BlockPos target,
            RetoldAiControlMode mode,
            RetoldAiControlOwner owner,
            int priority,
            String reason,
            long gameTime,
            int controlTicks,
            double speed,
            boolean sprinting
    ) {
        if (mob == null || target == null) {
            return false;
        }

        if (!RetoldAiControl.tryClaim(
                mob,
                mode,
                owner,
                priority,
                reason,
                gameTime,
                controlTicks
        )) {
            return false;
        }

        mob.setSprinting(sprinting);

        RetoldAiControl.withNavigationBypass(() -> {
            mob.getNavigation().moveTo(
                    target.getX() + 0.5D,
                    target.getY(),
                    target.getZ() + 0.5D,
                    speed
            );
        });

        return true;
    }

    public static void stopOwnedMovement(
            PathfinderMob mob,
            RetoldAiControlOwner owner
    ) {
        if (mob == null) {
            return;
        }

        mob.setSprinting(false);
        mob.getNavigation().stop();

        RetoldAiControl.clearIfOwnedBy(
                mob,
                owner
        );
    }
}
