package cz.xefensor.retold.behavior.pack;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

final class RetoldPackParty {
    final BlockPos packCenter;
    final long createdAt;
    final List<PathfinderMob> members = new ArrayList<>();

    Vec3 searchDirection;
    long searchDirectionExpiresAt;

    RetoldPackParty(
            BlockPos packCenter,
            long createdAt
    ) {
        this.packCenter = packCenter;
        this.createdAt = createdAt;
    }
}
