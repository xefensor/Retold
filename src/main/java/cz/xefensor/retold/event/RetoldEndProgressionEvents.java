package cz.xefensor.retold.event;

import cz.xefensor.retold.effect.RetoldRitualEffects;
import cz.xefensor.retold.registry.RetoldBlocks;
import cz.xefensor.retold.stage.RetoldElementType;
import cz.xefensor.retold.stage.RetoldStageManager;
import cz.xefensor.retold.stage.RetoldWorldData;
import cz.xefensor.retold.stage.RetoldWorldStage;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class RetoldEndProgressionEvents {
    private static final int DRAGON_EGG_CRACK_BREAKER_ID = -4711026;
    private static final int DRAGON_EGG_CRACK_REFRESH_INTERVAL_TICKS = 20;

    private RetoldEndProgressionEvents() {
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

        if (data.getStage() == RetoldWorldStage.STAGE_1) {
            RetoldStageManager.setStage(endLevel, RetoldWorldStage.STAGE_2);
        }

        refreshDragonEggElementCrack(endLevel, data);
    }

    @SubscribeEvent
    public static void onDragonEggRightClick(PlayerInteractEvent.RightClickBlock event) {
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
        RetoldWorldData data = RetoldWorldData.get(serverLevel);
        RetoldElementType element = getElementForStack(stack);

        if (element != null) {
            handleElementUse(event, serverLevel, data, stack, element);
            return;
        }
    }

    private static void handleElementUse(
            PlayerInteractEvent.RightClickBlock event,
            ServerLevel serverLevel,
            RetoldWorldData data,
            ItemStack stack,
            RetoldElementType element
    ) {
        Player player = event.getEntity();

        if (data.getStage() != RetoldWorldStage.STAGE_2) {
            player.sendOverlayMessage(
                    Component.literal("The egg does not respond.")
            );

            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
            return;
        }

        if (data.hasElementOffered(element)) {
            player.sendOverlayMessage(
                    Component.literal("The egg has already absorbed " + element.absorbedName() + ".")
            );

            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
            return;
        }

        if (!player.isCreative()) {
            stack.shrink(1);
        }

        data.offerElement(element);
        data.setDragonEggPos(event.getPos());
        RetoldRitualEffects.playDragonEggElementAccepted(serverLevel, event.getPos());
        showDragonEggElementCrack(serverLevel, event.getPos(), data.offeredElementCount());
        player.sendOverlayMessage(
                Component.literal("The egg absorbs the " + element.displayName() + ".")
        );

        if (data.hasAllElements()) {
            hatchDragonEgg(serverLevel, event.getPos());
        }

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    private static RetoldElementType getElementForStack(ItemStack stack) {
        if (stack.is(RetoldBlocks.WATER_ELEMENT)) {
            return RetoldElementType.WATER;
        }

        if (stack.is(RetoldBlocks.AIR_ELEMENT)) {
            return RetoldElementType.AIR;
        }

        return null;
    }

    private static void hatchDragonEgg(ServerLevel serverLevel, BlockPos pos) {
        RetoldRitualEffects.playDragonEggStage3Ritual(serverLevel, pos);
        serverLevel.destroyBlockProgress(DRAGON_EGG_CRACK_BREAKER_ID, pos, -1);

        serverLevel.removeBlock(pos, false);
        RetoldWorldData.get(serverLevel).clearDragonEggPos();
        RetoldStageManager.setStage(serverLevel, RetoldWorldStage.STAGE_3);
    }

    private static void refreshDragonEggElementCrack(ServerLevel level, RetoldWorldData data) {
        if (data.getStage() != RetoldWorldStage.STAGE_2) {
            return;
        }

        int offeredElementCount = data.offeredElementCount();

        if (offeredElementCount <= 0) {
            return;
        }

        BlockPos dragonEggPos = data.getDragonEggPos();

        if (dragonEggPos == null) {
            return;
        }

        if (!level.getBlockState(dragonEggPos).is(Blocks.DRAGON_EGG)) {
            level.destroyBlockProgress(DRAGON_EGG_CRACK_BREAKER_ID, dragonEggPos, -1);
            data.clearDragonEggPos();
            return;
        }

        if (level.getGameTime() % DRAGON_EGG_CRACK_REFRESH_INTERVAL_TICKS != 0) {
            return;
        }

        showDragonEggElementCrack(level, dragonEggPos, offeredElementCount);
    }

    private static void showDragonEggElementCrack(ServerLevel level, BlockPos pos, int offeredElementCount) {
        int crackProgress = Math.min(9, Math.max(1, offeredElementCount * 2));

        level.destroyBlockProgress(DRAGON_EGG_CRACK_BREAKER_ID, pos, crackProgress);
    }

    @SubscribeEvent
    public static void onEndCrystalUsedNearExitPortal(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        if (event.getLevel().isClientSide()) {
            return;
        }

        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (serverLevel.dimension() != Level.END) {
            return;
        }

        ItemStack stack = event.getItemStack();

        if (!stack.is(Items.END_CRYSTAL)) {
            return;
        }

        RetoldWorldStage stage = RetoldWorldData.get(serverLevel).getStage();

        if (stage == RetoldWorldStage.STAGE_1) {
            return;
        }

        BlockPos clickedPos = event.getPos();

        if (!isNearEndExitPortal(clickedPos)) {
            return;
        }

        if (!canPlaceEndCrystalOn(serverLevel, clickedPos)) {
            return;
        }

        BlockPos crystalPos = clickedPos.above();

        if (!canPlaceEndCrystalAt(serverLevel, crystalPos)) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
            return;
        }

        placeEndCrystalWithoutDragonRespawn(serverLevel, crystalPos);

        if (!event.getEntity().isCreative()) {
            stack.shrink(1);
        }

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    private static boolean isNearEndExitPortal(BlockPos pos) {
        return Math.abs(pos.getX()) <= 8
                && Math.abs(pos.getZ()) <= 8;
    }

    private static boolean canPlaceEndCrystalOn(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).is(Blocks.BEDROCK)
                || level.getBlockState(pos).is(Blocks.OBSIDIAN);
    }

    private static boolean canPlaceEndCrystalAt(ServerLevel level, BlockPos pos) {
        if (!level.isEmptyBlock(pos)) {
            return false;
        }

        return level.noCollision(new AABB(pos));
    }

    private static void placeEndCrystalWithoutDragonRespawn(
            ServerLevel level,
            BlockPos pos
    ) {
        EndCrystal crystal = new EndCrystal(
                level,
                pos.getX() + 0.5,
                pos.getY(),
                pos.getZ() + 0.5
        );

        crystal.setShowBottom(false);
        level.addFreshEntity(crystal);
    }
}
