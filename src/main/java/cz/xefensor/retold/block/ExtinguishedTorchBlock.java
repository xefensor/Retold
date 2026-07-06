package cz.xefensor.retold.block;

import cz.xefensor.retold.registry.RetoldTags;
import cz.xefensor.retold.event.TorchWeatherEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.ItemAbilities;

public class ExtinguishedTorchBlock extends TorchBlock {
    private final Block litBlock;
    private final Item cloneItem;

    public ExtinguishedTorchBlock(Block litBlock, Item cloneItem, BlockBehaviour.Properties properties) {
        super(ParticleTypes.SMOKE, properties);
        this.litBlock = litBlock;
        this.cloneItem = cloneItem;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, net.minecraft.util.RandomSource random) {
        // No passive particles while extinguished.
    }

    @Override
    protected InteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit
    ) {
        if (!isTorchIgniter(stack)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }

        relight(level, pos, player, hand, stack, litBlock.defaultBlockState());
        return InteractionResult.SUCCESS;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData, Player player) {
        return new ItemStack(cloneItem);
    }

    public static boolean isTorchIgniter(ItemStack stack) {
        return stack.canPerformAction(ItemAbilities.FIRESTARTER_LIGHT)
                || stack.is(RetoldTags.TORCH_IGNITERS);
    }

    public static void relight(
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            ItemStack stack,
            BlockState litState
    ) {
        if (level.isClientSide()) {
            return;
        }

        level.setBlock(pos, litState, Block.UPDATE_ALL);
        TorchWeatherEvents.trackTorch(level, pos, litState);
        level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    ParticleTypes.FLAME,
                    pos.getX() + 0.5,
                    pos.getY() + 0.7,
                    pos.getZ() + 0.5,
                    8,
                    0.15,
                    0.15,
                    0.15,
                    0.01
            );
        }

        consumeIgniter(stack, player, hand);
    }

    private static void consumeIgniter(ItemStack stack, Player player, InteractionHand hand) {
        if (player.getAbilities().instabuild) {
            return;
        }

        if (stack.is(Items.FLINT_AND_STEEL) || stack.isDamageableItem()) {
            stack.hurtAndBreak(1, player, hand);
        } else {
            stack.shrink(1);
        }
    }
}