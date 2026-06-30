package cz.xefensor.retold.event;

import cz.xefensor.retold.stage.RetoldWorldData;
import cz.xefensor.retold.stage.RetoldWorldStage;
import cz.xefensor.retold.undead.RetoldUndead;
import cz.xefensor.retold.undead.RetoldUndeadCleansing;
import cz.xefensor.retold.undead.RetoldUndeadSunFear;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.PathfinderMob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public final class RetoldUndeadEvents {
    private RetoldUndeadEvents() {
    }

    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();

        if (entity.level().isClientSide()) {
            return;
        }

        if (!RetoldUndead.isUndead(entity)) {
            return;
        }

        if (!(entity.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        RetoldWorldStage stage = RetoldWorldData.get(serverLevel).getStage();

        if (stage == RetoldWorldStage.STAGE_2) {
            entity.clearFire();

            if (entity instanceof PathfinderMob pathfinderMob) {
                RetoldUndeadSunFear.removeSunFearGoalsOnce(pathfinderMob);
            }

            return;
        }

        if (stage == RetoldWorldStage.STAGE_3) {
            RetoldUndeadCleansing.cleanse(entity);
        }
    }

    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (!RetoldUndead.isUndead(event.getEntity())) {
            return;
        }

        if (event.getSpawnType() != EntitySpawnReason.NATURAL
                && event.getSpawnType() != EntitySpawnReason.SPAWNER) {
            return;
        }

        ServerLevel serverLevel = event.getLevel().getLevel();
        RetoldWorldStage stage = RetoldWorldData.get(serverLevel).getStage();

        if (stage == RetoldWorldStage.STAGE_3) {
            event.setSpawnCancelled(true);
        }
    }
}