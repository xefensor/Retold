package cz.xefensor.retold.api.world;

import net.minecraft.core.BlockPos;

/** Inclusive block bounds for the area a Retold mutation may affect. */
public record RetoldWorldMutationBounds(
        int minX,
        int minY,
        int minZ,
        int maxX,
        int maxY,
        int maxZ
) {
    public RetoldWorldMutationBounds {
        if (maxX < minX || maxY < minY || maxZ < minZ) {
            throw new IllegalArgumentException("World mutation bounds must not be inverted");
        }
    }

    public static RetoldWorldMutationBounds single(BlockPos pos) {
        if (pos == null) {
            throw new IllegalArgumentException("pos must not be null");
        }

        return new RetoldWorldMutationBounds(
                pos.getX(),
                pos.getY(),
                pos.getZ(),
                pos.getX(),
                pos.getY(),
                pos.getZ()
        );
    }

    public boolean contains(BlockPos pos) {
        return pos != null
                && pos.getX() >= minX
                && pos.getX() <= maxX
                && pos.getY() >= minY
                && pos.getY() <= maxY
                && pos.getZ() >= minZ
                && pos.getZ() <= maxZ;
    }
}
