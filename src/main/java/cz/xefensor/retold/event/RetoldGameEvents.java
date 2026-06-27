package cz.xefensor.retold.event;

import cz.xefensor.retold.command.RetoldCommands;
import cz.xefensor.retold.stage.RetoldWorldData;
import cz.xefensor.retold.stage.RetoldWorldStage;
import cz.xefensor.retold.undead.RetoldUndead;
import cz.xefensor.retold.stage.RetoldStageManager;
import cz.xefensor.retold.undead.RetoldUndeadCleansing;
import cz.xefensor.retold.effect.RetoldRitualEffects;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.minecraft.world.entity.animal.golem.IronGolem;
import cz.xefensor.retold.undead.RetoldUndeadSunFear;
import net.minecraft.world.entity.PathfinderMob;
import cz.xefensor.retold.enderman.RetoldEndermanBehavior;
import net.minecraft.world.entity.monster.EnderMan;

public final class RetoldGameEvents {
    private RetoldGameEvents() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        RetoldCommands.register(event.getDispatcher());
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
                RetoldUndeadSunFear.removeSunFearGoals(pathfinderMob);
            }

            return;
        }

        if (stage == RetoldWorldStage.STAGE_3) {
            RetoldUndeadCleansing.cleanse(entity);
        }
    }

    @SubscribeEvent
    public static void onServerTickPost(ServerTickEvent.Post event) {
        ServerLevel endLevel = event.getServer().getLevel(Level.END);

        if (endLevel == null) {
            return;
        }

        EnderDragonFight dragonFight = endLevel.getDragonFight();

        if (dragonFight == null) {
            return;
        }

        if (!dragonFight.hasPreviouslyKilledDragon()) {
            return;
        }

        RetoldWorldData data = RetoldWorldData.get(endLevel);

        if (data.getStage() != RetoldWorldStage.STAGE_1) {
            return;
        }

        RetoldStageManager.setStage(endLevel, RetoldWorldStage.STAGE_2);
    }

    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (!RetoldUndead.isUndead(event.getEntity())) {
            return;
        }

        if (event.getSpawnType() != EntitySpawnReason.NATURAL && event.getSpawnType() != EntitySpawnReason.SPAWNER) {
            return;
        }

        ServerLevel serverLevel = event.getLevel().getLevel();

        RetoldWorldStage stage = RetoldWorldData.get(serverLevel).getStage();

        if (stage == RetoldWorldStage.STAGE_3) {
            event.setSpawnCancelled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        if (event.getLevel().isClientSide()) {
            return;
        }

        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (!event.getLevel().getBlockState(event.getPos()).is(Blocks.DRAGON_EGG)) {
            return;
        }

        ItemStack stack = event.getItemStack();

        if (!stack.is(Items.NETHER_STAR)) {
            return;
        }

        RetoldWorldData data = RetoldWorldData.get(serverLevel);

        if (data.getStage() != RetoldWorldStage.STAGE_2) {
            Player player = event.getEntity();

            player.sendOverlayMessage(
                    Component.literal("The egg does not respond.")
            );

            return;
        }

        Player player = event.getEntity();

        if (!player.isCreative()) {
            stack.shrink(1);
        }

        RetoldRitualEffects.playDragonEggStage3Ritual(serverLevel, event.getPos());
        serverLevel.removeBlock(event.getPos(), false);
        RetoldStageManager.setStage(serverLevel, RetoldWorldStage.STAGE_3);

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onIronGolemFinalizeSpawn(FinalizeSpawnEvent event) {
        if (!(event.getEntity() instanceof IronGolem)) {
            return;
        }

        if (event.getSpawnType() != EntitySpawnReason.STRUCTURE && event.getSpawnType() != EntitySpawnReason.MOB_SUMMONED) {
            return;
        }

        ServerLevel serverLevel = event.getLevel().getLevel();

        RetoldWorldStage stage = RetoldWorldData.get(serverLevel).getStage();

        if (stage == RetoldWorldStage.STAGE_1) {
            event.setSpawnCancelled(true);
        }
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

        if (stage == RetoldWorldStage.STAGE_2 || stage == RetoldWorldStage.STAGE_3) {
            RetoldEndermanBehavior.disableEyeContactAggro(enderman);
        }
    }
}