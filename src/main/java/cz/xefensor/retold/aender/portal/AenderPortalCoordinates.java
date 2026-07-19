package cz.xefensor.retold.aender.portal;

import cz.xefensor.retold.aender.RetoldAenderDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.Vec3;

final class AenderPortalCoordinates {
    private static final double OVERWORLD_TO_AENDER_SCALE = 8.0D;

    private AenderPortalCoordinates() {
    }

    static BlockPos scaleAndClamp(
            ResourceKey<Level> sourceDimension,
            WorldBorder destinationBorder,
            Vec3 sourcePosition
    ) {
        double scale = horizontalScale(sourceDimension);
        return destinationBorder.clampToBounds(
                sourcePosition.x() * scale,
                sourcePosition.y(),
                sourcePosition.z() * scale
        );
    }

    private static double horizontalScale(ResourceKey<Level> sourceDimension) {
        if (sourceDimension == Level.OVERWORLD) {
            return OVERWORLD_TO_AENDER_SCALE;
        }

        if (sourceDimension == RetoldAenderDimensions.AENDER) {
            return 1.0D / OVERWORLD_TO_AENDER_SCALE;
        }

        throw new IllegalArgumentException(
                "Unsupported Aender portal source dimension: " + sourceDimension.identifier()
        );
    }
}
