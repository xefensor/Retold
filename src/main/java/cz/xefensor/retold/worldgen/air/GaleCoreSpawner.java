package cz.xefensor.retold.worldgen.air;

import cz.xefensor.retold.registry.RetoldEntityTypes;
import cz.xefensor.retold.worldgen.air.wind.AirTempleWindSource;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

public final class GaleCoreSpawner {
    private static final double WIND_LEASH_MARGIN = 20.0D;
    private static final double DUPLICATE_SEARCH_MARGIN = 96.0D;

    private GaleCoreSpawner() {
    }

    public static boolean spawnIfNeeded(ServerLevel level, AirTempleWindSource source) {
        AABB combatBounds = source.bounds().inflate(WIND_LEASH_MARGIN);
        AABB duplicateSearchBounds = source.bounds().inflate(DUPLICATE_SEARCH_MARGIN);
        BlockPos spawnPos = new BlockPos(source.centerX(), source.islandY() + 31, source.centerZ() - 4);
        List<GaleCore> existingBosses = level.getEntities(
                (Entity) null,
                duplicateSearchBounds,
                entity -> entity instanceof GaleCore
        ).stream()
                .map(GaleCore.class::cast)
                .sorted(Comparator.comparingDouble(boss -> boss.distanceToSqr(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D)))
                .toList();

        if (!existingBosses.isEmpty()) {
            repairExistingBosses(
                    existingBosses,
                    new Vec3(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D),
                    combatBounds
            );
            return true;
        }

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

    private static void repairExistingBosses(List<GaleCore> bosses, Vec3 homePosition, AABB combatBounds) {
        GaleCore keeper = bosses.getFirst();
        keeper.setPersistenceRequired();
        keeper.setCombatBounds(combatBounds);
        keeper.setHomePosition(homePosition);

        for (int i = 1; i < bosses.size(); i++) {
            bosses.get(i).discard();
        }
    }
}
