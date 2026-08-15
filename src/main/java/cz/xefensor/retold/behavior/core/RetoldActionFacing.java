package cz.xefensor.retold.behavior.core;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * Keeps a stationary Retold interaction visually aimed at its concrete subject.
 * Moving behavior should normally use LookControl alone so navigation remains free
 * to turn the body along its route.
 */
public final class RetoldActionFacing {
    private static final float MAX_HEAD_YAW = 90.0F;
    private static final float MAX_HEAD_PITCH = 90.0F;
    private static final double MIN_HORIZONTAL_DISTANCE_SQUARED = 0.0001D;

    private RetoldActionFacing() {
    }

    public static void face(Mob mob, Vec3 subject) {
        if (mob == null || subject == null) {
            return;
        }

        mob.getLookControl().setLookAt(
                subject.x(),
                subject.y(),
                subject.z(),
                MAX_HEAD_YAW,
                MAX_HEAD_PITCH
        );

        double dx = subject.x() - mob.getX();
        double dz = subject.z() - mob.getZ();

        if (dx * dx + dz * dz <= MIN_HORIZONTAL_DISTANCE_SQUARED) {
            return;
        }

        float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
        mob.setYRot(yaw);
        mob.yBodyRot = yaw;
        mob.setYHeadRot(yaw);
    }
}
