package cz.xefensor.retold.worldgen.earth;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;

record EarthLabyrinthSource(BlockPos guardianPosition, BoundingBox structureBounds) {
    private static final double DUPLICATE_SEARCH_MARGIN = 8.0D;

    EarthLabyrinthSource {
        guardianPosition = guardianPosition.immutable();
    }

    long key() {
        return guardianPosition.asLong();
    }

    AABB duplicateSearchBounds() {
        return AABB.of(structureBounds).inflate(DUPLICATE_SEARCH_MARGIN);
    }
}
