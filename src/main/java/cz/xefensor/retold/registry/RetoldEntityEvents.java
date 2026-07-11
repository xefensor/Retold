package cz.xefensor.retold.registry;

import cz.xefensor.retold.aender.entity.AenderEye;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

public final class RetoldEntityEvents {
    private RetoldEntityEvents() {
    }

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(RetoldEntityTypes.AENDER_EYE.get(), AenderEye.createAttributes().build());
    }

    public static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(
                RetoldEntityTypes.AENDER_EYE.get(),
                SpawnPlacementTypes.NO_RESTRICTIONS,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                AenderEye::checkAenderEyeSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE
        );
    }
}
