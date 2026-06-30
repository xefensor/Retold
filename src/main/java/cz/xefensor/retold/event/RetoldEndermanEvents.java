package cz.xefensor.retold.event;

import cz.xefensor.retold.enderman.RetoldEndermanBehavior;
import cz.xefensor.retold.stage.RetoldWorldData;
import cz.xefensor.retold.stage.RetoldWorldStage;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.EnderMan;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public final class RetoldEndermanEvents {
    private RetoldEndermanEvents() {
    }

    @SubscribeEvent
    public static void onEndermanTickPost(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();

        if (entity.level().isClientSide()) {
            return;
        }

        if (!(entity instanceof EnderMan enderman)) {
            return;
        }

        if (!(entity.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        RetoldWorldStage stage = RetoldWorldData.get(serverLevel).getStage();

        if (stage == RetoldWorldStage.STAGE_2
                || stage == RetoldWorldStage.STAGE_3) {
            RetoldEndermanBehavior.disableEyeContactAggro(enderman);
        }
    }
}