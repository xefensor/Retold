package cz.xefensor.retold.behavior.food;

import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.control.RetoldAiControlMode;
import cz.xefensor.retold.behavior.control.RetoldAiControlOwner;
import cz.xefensor.retold.behavior.control.RetoldAiPriorities;
import cz.xefensor.retold.behavior.core.RetoldActionFacing;
import cz.xefensor.retold.behavior.core.RetoldBehaviorCoordinator;
import cz.xefensor.retold.behavior.core.RetoldCubeMobMovement;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;

public final class RetoldFeedingPose {
    public static final int DURATION_TICKS = 20 * 2;

    private static final int CONTROL_PRIORITY = RetoldAiPriorities.above(
            RetoldAiPriorities.FEED,
            2
    );
    private static final String CONTROL_REASON = "feeding_pose";
    private static final Map<Mob, FeedingPose> POSES = new WeakHashMap<>();

    private RetoldFeedingPose() {
    }

    public static boolean begin(
            Mob mob,
            Vec3 foodSource,
            long gameTime
    ) {
        if (foodSource == null
                || !RetoldBehaviorCoordinator.canCompleteMeal(mob)) {
            return false;
        }

        if (!RetoldAiControl.tryClaim(
                mob,
                RetoldAiControlMode.FEED,
                RetoldAiControlOwner.FOOD,
                CONTROL_PRIORITY,
                CONTROL_REASON,
                gameTime,
                DURATION_TICKS
        )) {
            return false;
        }

        POSES.put(
                mob,
                new FeedingPose(
                        foodSource,
                        gameTime + DURATION_TICKS
                )
        );
        apply(mob, foodSource);
        return true;
    }

    public static boolean tick(Mob mob, long gameTime) {
        FeedingPose pose = mob == null ? null : POSES.get(mob);

        if (pose == null) {
            return false;
        }

        if (!mob.isAlive()
                || mob.isRemoved()
                || gameTime > pose.expiresAt()
                || !RetoldAiControl.isControlledAsByWithReason(
                mob,
                RetoldAiControlMode.FEED,
                RetoldAiControlOwner.FOOD,
                CONTROL_REASON
        )) {
            finish(mob);
            return false;
        }

        apply(mob, pose.foodSource());
        return true;
    }

    static Vec3 foodSource(Mob mob) {
        FeedingPose pose = mob == null ? null : POSES.get(mob);
        return pose == null ? null : pose.foodSource();
    }

    static void finish(Mob mob) {
        if (mob == null) {
            return;
        }

        POSES.remove(mob);
        RetoldAiControl.clearIfControlledAsByWithReason(
                mob,
                RetoldAiControlMode.FEED,
                RetoldAiControlOwner.FOOD,
                CONTROL_REASON
        );
    }

    private static void apply(Mob mob, Vec3 foodSource) {
        mob.setSprinting(false);
        mob.getNavigation().stop();
        mob.getMoveControl().setWait();
        mob.setSpeed(0.0F);
        mob.setDeltaMovement(Vec3.ZERO);

        if (mob instanceof AbstractCubeMob cubeMob) {
            RetoldCubeMobMovement.stop(cubeMob);
        }

        RetoldActionFacing.face(mob, foodSource);
    }

    private record FeedingPose(
            Vec3 foodSource,
            long expiresAt
    ) {
    }
}
