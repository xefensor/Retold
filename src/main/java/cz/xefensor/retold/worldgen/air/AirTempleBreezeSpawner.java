package cz.xefensor.retold.worldgen.air;

import cz.xefensor.retold.worldgen.air.wind.AirTempleWindSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.breeze.Breeze;

public final class AirTempleBreezeSpawner {
    private static final int TARGET_BREEZE_COUNT = 7;
    private static final int[][] MAIN_ISLAND_SPAWNS = {
            {10, 0, 2},
            {-10, 0, 2},
            {0, 10, 2},
            {0, -10, 2}
    };
    private static final int[][] SATELLITE_SPAWNS = {
            {30, 7, -3},
            {-27, 24, 1},
            {12, -33, 4}
    };

    private AirTempleBreezeSpawner() {
    }

    public static boolean spawnIfNeeded(ServerLevel level, AirTempleWindSource source) {
        int existing = level.getEntities(
                (Entity) null,
                source.bounds(),
                entity -> entity.getType() == EntityTypes.BREEZE
        ).size();

        if (existing >= TARGET_BREEZE_COUNT) {
            return true;
        }

        int spawned = 0;

        for (int[] offset : MAIN_ISLAND_SPAWNS) {
            if (spawnAtSurface(
                    level,
                    source.centerX() + offset[0],
                    source.centerZ() + offset[1],
                    source.islandY() + offset[2]
            )) {
                spawned++;
            }
        }

        for (int[] offset : SATELLITE_SPAWNS) {
            if (spawnAtSurface(
                    level,
                    source.centerX() + offset[0],
                    source.centerZ() + offset[1],
                    source.islandY() + offset[2]
            )) {
                spawned++;
            }
        }

        return existing + spawned >= TARGET_BREEZE_COUNT;
    }

    private static boolean spawnAtSurface(
            ServerLevel level,
            int x,
            int z,
            int roughY
    ) {
        if (!level.hasChunk(x >> 4, z >> 4)) {
            return false;
        }

        BlockPos spawnPos = findSpawnPos(level, x, z, roughY);

        if (spawnPos == null) {
            return false;
        }

        Breeze breeze = EntityTypes.BREEZE.create(level, EntitySpawnReason.STRUCTURE);

        if (breeze == null) {
            return false;
        }

        breeze.setPersistenceRequired();
        breeze.setPos(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D);
        breeze.finalizeSpawn(
                level,
                level.getCurrentDifficultyAt(spawnPos),
                EntitySpawnReason.STRUCTURE,
                null
        );
        level.addFreshEntityWithPassengers(breeze);
        return true;
    }

    private static BlockPos findSpawnPos(ServerLevel level, int x, int z, int roughY) {
        int minY = Math.max(level.getMinY() + 1, roughY - 8);
        int maxY = Math.min(level.getMaxY() - 2, roughY + 8);
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int y = maxY; y >= minY; y--) {
            mutable.set(x, y, z);

            if (!level.getBlockState(mutable).isAir()) {
                BlockPos spawnPos = new BlockPos(x, y + 1, z);

                if (level.getBlockState(spawnPos).isAir()
                        && level.getBlockState(spawnPos.above()).isAir()) {
                    return spawnPos;
                }
            }
        }

        return null;
    }
}
