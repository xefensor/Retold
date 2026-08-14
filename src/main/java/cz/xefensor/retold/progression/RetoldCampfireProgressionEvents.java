package cz.xefensor.retold.progression;

import cz.xefensor.retold.registry.RetoldTags;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class RetoldCampfireProgressionEvents {
    private RetoldCampfireProgressionEvents() {
    }

    @SubscribeEvent
    public static void onFlintUsedOnCampfire(
            PlayerInteractEvent.RightClickBlock event
    ) {
        if (!lightWithFlint(
                event.getLevel(),
                event.getPos(),
                event.getEntity(),
                event.getItemStack()
        )) {
            return;
        }

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    public static BlockState unlitPlacementState(BlockState state) {
        if (!state.is(BlockTags.CAMPFIRES)
                || !state.hasProperty(CampfireBlock.LIT)
                || !state.getValue(CampfireBlock.LIT)) {
            return state;
        }

        return state.setValue(CampfireBlock.LIT, false);
    }

    static boolean lightWithFlint(
            Level level,
            BlockPos pos,
            Player player,
            ItemStack stack
    ) {
        BlockState state = level.getBlockState(pos);
        if (!stack.is(RetoldTags.CAMPFIRE_CONSUMABLE_IGNITERS)
                || !CampfireBlock.canLight(state)) {
            return false;
        }

        if (level.isClientSide()) {
            return true;
        }

        level.setBlock(
                pos,
                state.setValue(CampfireBlock.LIT, true),
                Block.UPDATE_ALL
        );
        level.playSound(
                null,
                pos,
                SoundEvents.FLINTANDSTEEL_USE,
                SoundSource.BLOCKS,
                1.0F,
                level.getRandom().nextFloat() * 0.4F + 0.8F
        );
        level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
        player.awardStat(Stats.ITEM_USED.get(stack.getItem()));

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        return true;
    }
}
