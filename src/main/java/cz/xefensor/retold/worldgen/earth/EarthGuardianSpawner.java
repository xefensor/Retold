package cz.xefensor.retold.worldgen.earth;

import cz.xefensor.retold.registry.RetoldEntityTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;

import java.util.Comparator;
import java.util.List;

final class EarthGuardianSpawner {
    private EarthGuardianSpawner() {
    }

    static boolean spawnIfNeeded(ServerLevel level, EarthLabyrinthSource source) {
        EarthGuardianEncounterData data = EarthGuardianEncounterData.get(level);
        EarthGuardianLifecycle lifecycle = data.lifecycle(source.key());
        List<EarthGuardian> existing = level.getEntities(
                (Entity) null,
                source.duplicateSearchBounds(),
                entity -> entity instanceof EarthGuardian guardian
                        && guardian.isStructureBound()
                        && guardian.labyrinthKey() == source.key()
        ).stream()
                .map(EarthGuardian.class::cast)
                .sorted(Comparator.comparingDouble(guardian -> guardian.distanceToSqr(
                        source.guardianPosition().getX() + 0.5D,
                        source.guardianPosition().getY(),
                        source.guardianPosition().getZ() + 0.5D
                )))
                .toList();

        if (lifecycle == EarthGuardianLifecycle.DEFEATED) {
            existing.forEach(Entity::discard);
            return true;
        }

        if (!existing.isEmpty()) {
            EarthGuardian keeper = existing.getFirst();
            keeper.configureForLabyrinth(level, source, lifecycle);

            for (int index = 1; index < existing.size(); index++) {
                existing.get(index).discard();
            }

            return true;
        }

        if (!level.hasChunkAt(source.guardianPosition())) {
            return false;
        }

        EarthGuardian guardian = RetoldEntityTypes.EARTH_GUARDIAN.get().create(
                level,
                EntitySpawnReason.STRUCTURE
        );

        if (guardian == null) {
            return false;
        }

        guardian.configureForLabyrinth(level, source, lifecycle);
        guardian.finalizeSpawn(
                level,
                level.getCurrentDifficultyAt(source.guardianPosition()),
                EntitySpawnReason.STRUCTURE,
                null
        );
        level.addFreshEntityWithPassengers(guardian);
        return true;
    }
}
