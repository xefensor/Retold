package cz.xefensor.retold.event;

import cz.xefensor.retold.stage.RetoldWorldData;
import cz.xefensor.retold.stage.RetoldWorldStage;
import cz.xefensor.retold.villager.RetoldVillagerGolemConstruction;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.player.TradeWithVillagerEvent;

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

    @SubscribeEvent
    public static void onTradeWithVillager(TradeWithVillagerEvent event) {
        if (!(event.getAbstractVillager() instanceof Villager villager)) {
            return;
        }

        var offer = event.getMerchantOffer();

        if (!offer.getCostA().is(Items.EMERALD)
                && !offer.getCostB().is(Items.EMERALD)) {
            return;
        }

        if (villager.level() instanceof ServerLevel level) {
            RetoldVillagerGolemConstruction.retainTradeEmerald(
                    level,
                    villager,
                    level.getGameTime()
            );
        }
    }

    @SubscribeEvent
    public static void onBreakConstructedGolemBlock(BreakBlockEvent event) {
        if (event.getLevel() instanceof ServerLevel level
                && RetoldVillagerGolemConstruction.isProtectedBuildBlock(
                level,
                event.getPos()
        )) {
            event.setCanceled(true);
        }
    }
}
