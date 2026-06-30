package cz.xefensor.retold.event;

import cz.xefensor.retold.stage.RetoldWorldData;
import cz.xefensor.retold.stage.RetoldWorldStage;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;

public final class RetoldGolemEvents {
    private RetoldGolemEvents() {
    }

    @SubscribeEvent
    public static void onIronGolemFinalizeSpawn(FinalizeSpawnEvent event) {
        if (!(event.getEntity() instanceof IronGolem)) {
            return;
        }

        if (event.getSpawnType() != EntitySpawnReason.STRUCTURE
                && event.getSpawnType() != EntitySpawnReason.MOB_SUMMONED) {
            return;
        }

        ServerLevel serverLevel = event.getLevel().getLevel();
        RetoldWorldStage stage = RetoldWorldData.get(serverLevel).getStage();

        if (stage == RetoldWorldStage.STAGE_1) {
            event.setSpawnCancelled(true);
        }
    }
}