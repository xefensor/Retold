package cz.xefensor.retold.aender;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.feature.EndPlatformFeature;
import net.minecraft.world.phys.Vec3;

public final class RetoldAenderEntryPlatform {
    public static final Vec3 ENTRY_POSITION =
            new Vec3(0.5D, 128.0D, 0.5D);

    private static final BlockPos PLATFORM_ORIGIN =
            BlockPos.containing(ENTRY_POSITION);

    private RetoldAenderEntryPlatform() {
    }

    public static void createInLevel(ServerLevel level) {
        EndPlatformFeature.createEndPlatform(
                level,
                PLATFORM_ORIGIN,
                true
        );
    }

    public static void generateInChunk(ChunkAccess chunk) {
        int chunkMinX = chunk.getPos().getMinBlockX();
        int chunkMaxX = chunkMinX + 15;
        int chunkMinZ = chunk.getPos().getMinBlockZ();
        int chunkMaxZ = chunkMinZ + 15;
        int minY = chunk.getMinY();
        int maxY = minY + chunk.getHeight() - 1;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int dz = -2; dz <= 2; dz++) {
            for (int dx = -2; dx <= 2; dx++) {
                int x = PLATFORM_ORIGIN.getX() + dx;
                int z = PLATFORM_ORIGIN.getZ() + dz;

                if (x < chunkMinX || x > chunkMaxX || z < chunkMinZ || z > chunkMaxZ) {
                    continue;
                }

                for (int dy = -1; dy < 3; dy++) {
                    int y = PLATFORM_ORIGIN.getY() + dy;

                    if (y < minY || y > maxY) {
                        continue;
                    }

                    Block block = dy == -1 ? Blocks.OBSIDIAN : Blocks.AIR;
                    pos.set(x, y, z);

                    if (!chunk.getBlockState(pos).is(block)) {
                        chunk.setBlockState(pos, block.defaultBlockState(), 0);
                    }
                }
            }
        }
    }
}
