package cz.xefensor.retold.worldgen.delayed;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.stage.RetoldStageRuntime;
import cz.xefensor.retold.stage.RetoldWorldStage;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;

public final class RetoldPatrolStageEvents {
    private RetoldPatrolStageEvents() {
    }

    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (RetoldStageRuntime.isAtLeast(RetoldWorldStage.STAGE_3)) {
            return;
        }

        Entity entity = event.getEntity();

        if (!isPillager(entity)) {
            return;
        }

        String spawnType = event.getSpawnType().name();

        Retold.LOGGER.debug(
                "Pillager FinalizeSpawnEvent before Stage 3, spawn type: {}",
                spawnType
        );

        if (!"PATROL".equals(spawnType)) {
            return;
        }

        event.setSpawnCancelled(true);

        Retold.LOGGER.info(
                "Blocked pillager patrol spawn before Stage 3 at [{}, {}, {}]",
                event.getX(),
                event.getY(),
                event.getZ()
        );
    }

    private static boolean isPillager(Entity entity) {
        return "entity.minecraft.pillager".equals(entity.getType().getDescriptionId());
    }
}