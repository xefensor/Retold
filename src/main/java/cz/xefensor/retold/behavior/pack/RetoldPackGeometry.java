package cz.xefensor.retold.behavior.pack;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.Vec3;

import java.util.List;

final class RetoldPackGeometry {
    private static final double EPSILON = 0.0001D;

    private RetoldPackGeometry() {
    }

    static BlockPos packCenter(
            PathfinderMob leader,
            List<PathfinderMob> nearbyPack
    ) {
        double x = leader.getX();
        double y = leader.getY();
        double z = leader.getZ();
        int count = 1;

        for (PathfinderMob member : nearbyPack) {
            if (member == leader) {
                continue;
            }

            x += member.getX();
            y += member.getY();
            z += member.getZ();
            count++;
        }

        return new BlockPos(
                (int) Math.floor(x / count),
                (int) Math.floor(y / count),
                (int) Math.floor(z / count)
        ).immutable();
    }

    static Vec3 leaderDirection(PathfinderMob leader) {
        Vec3 movement = leader.getDeltaMovement();

        Vec3 horizontalMovement = new Vec3(
                movement.x,
                0.0D,
                movement.z
        );

        if (horizontalMovement.lengthSqr() > EPSILON) {
            return horizontalMovement.normalize();
        }

        Vec3 look = leader.getLookAngle();

        Vec3 horizontalLook = new Vec3(
                look.x,
                0.0D,
                look.z
        );

        if (horizontalLook.lengthSqr() > EPSILON) {
            return horizontalLook.normalize();
        }

        return randomHorizontalDirection(leader);
    }

    static Vec3 safeDirection(
            PathfinderMob mob,
            Vec3 direction
    ) {
        if (direction != null && direction.lengthSqr() > EPSILON) {
            return new Vec3(
                    direction.x,
                    0.0D,
                    direction.z
            ).normalize();
        }

        return randomHorizontalDirection(mob);
    }

    private static Vec3 randomHorizontalDirection(PathfinderMob mob) {
        double angle = mob.getRandom().nextDouble() * Math.PI * 2.0D;

        return new Vec3(
                Math.cos(angle),
                0.0D,
                Math.sin(angle)
        );
    }
}
