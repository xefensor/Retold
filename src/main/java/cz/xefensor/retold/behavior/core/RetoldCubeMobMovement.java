package cz.xefensor.retold.behavior.core;

import cz.xefensor.retold.mixin.CubeMobMoveControlInvoker;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;

public final class RetoldCubeMobMovement {
    private RetoldCubeMobMovement() {
    }

    public static boolean moveToward(
            AbstractCubeMob cubeMob,
            double x,
            double z,
            double speed
    ) {
        if (cubeMob == null
                || !(cubeMob.getMoveControl() instanceof CubeMobMoveControlInvoker moveControl)) {
            return false;
        }

        double dx = x - cubeMob.getX();
        double dz = z - cubeMob.getZ();

        if (dx * dx + dz * dz < 0.0001D) {
            return false;
        }

        float direction = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;

        moveControl.retold$setDirection(direction, false);
        moveControl.retold$setWantedMovement(speed);
        return true;
    }

    public static void stop(AbstractCubeMob cubeMob) {
        if (cubeMob != null
                && cubeMob.getMoveControl() instanceof CubeMobMoveControlInvoker moveControl) {
            moveControl.retold$setWantedMovement(0.0D);
        }
    }
}
