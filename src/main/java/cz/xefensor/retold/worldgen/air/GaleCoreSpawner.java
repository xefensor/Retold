package cz.xefensor.retold.worldgen.air;

import cz.xefensor.retold.registry.RetoldEntityTypes;
import cz.xefensor.retold.worldgen.air.wind.AirTempleWindSource;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.AABB;

public final class GaleCoreSpawner {
    private static final double WIND_LEASH_MARGIN = 20.0D;

    private GaleCoreSpawner() {
    }

    public static boolean spawnIfNeeded(ServerLevel level, AirTempleWindSource source) {
        AABB combatBounds = source.bounds().inflate(WIND_LEASH_MARGIN);
        boolean hasBoss = !level.getEntities(
                (Entity) null,
                combatBounds,
                entity -> entity instanceof GaleCore
        ).isEmpty();

        if (hasBoss) {
            return true;
        }

        BlockPos spawnPos = new BlockPos(source.centerX(), source.islandY() + 31, source.centerZ() - 4);

        if (!level.hasChunk(spawnPos.getX() >> 4, spawnPos.getZ() >> 4)) {
            return false;
        }

        GaleCore boss = RetoldEntityTypes.GALE_CORE.get().create(level, EntitySpawnReason.STRUCTURE);

        if (boss == null) {
            return false;
        }

        boss.setPersistenceRequired();
        boss.setNoGravity(true);
        boss.setPos(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D);
        boss.setHomePosition(boss.position());
        boss.setCombatBounds(combatBounds);
        boss.finalizeSpawn(
                level,
                level.getCurrentDifficultyAt(spawnPos),
                EntitySpawnReason.STRUCTURE,
                null
        );
        level.addFreshEntityWithPassengers(boss);
        return true;
    }
}
