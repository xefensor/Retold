package cz.xefensor.retold.worldgen.air.wind;

import cz.xefensor.retold.worldgen.air.AirTempleDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;

public record AirTempleWindSource(int centerX, int centerZ, int islandY, AABB bounds) {
    private static final double RADIUS = 48.0D;

    public static AirTempleWindSource fromTemple(int centerX, int centerZ, int islandY) {
        return new AirTempleWindSource(
                centerX,
                centerZ,
                islandY,
                new AABB(
                        centerX - RADIUS,
                        AirTempleDimensions.windMinY(islandY),
                        centerZ - RADIUS,
                        centerX + RADIUS + 1.0D,
                        AirTempleDimensions.windMaxY(islandY) + 1.0D,
                        centerZ + RADIUS + 1.0D
                )
        );
    }

    public static AirTempleWindSource fromStructureBounds(BoundingBox bounds) {
        int centerX = bounds.getCenter().getX();
        int centerZ = bounds.getCenter().getZ();
        int islandY = bounds.maxY()
                - AirTempleDimensions.WIND_ABOVE_TOWER
                - AirTempleDimensions.TOWER_HEIGHT;

        return fromTemple(centerX, centerZ, islandY);
    }

    public long key() {
        return ChunkPos.pack(centerX >> 4, centerZ >> 4);
    }

    public boolean hasGeneratedTemple(ServerLevel level) {
        BlockPos centerPillar = new BlockPos(centerX, islandY + 2, centerZ);
        BlockPos floor = new BlockPos(centerX + 4, islandY + 2, centerZ);

        if (!level.hasChunk(centerPillar.getX() >> 4, centerPillar.getZ() >> 4)
                || !level.hasChunk(floor.getX() >> 4, floor.getZ() >> 4)) {
            return false;
        }

        return level.getBlockState(centerPillar).is(Blocks.CHISELED_TUFF)
                && level.getBlockState(floor).is(Blocks.TUFF_BRICKS);
    }
}
