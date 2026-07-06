package cz.xefensor.retold.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class ExtinguishedWallTorchBlock extends WallTorchBlock {
    private final Block litBlock;
    private final Item cloneItem;

    public ExtinguishedWallTorchBlock(Block litBlock, Item cloneItem, BlockBehaviour.Properties properties) {
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
        if (!ExtinguishedTorchBlock.isTorchIgniter(stack)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }

        BlockState litState = litBlock.defaultBlockState()
                .setValue(WallTorchBlock.FACING, state.getValue(WallTorchBlock.FACING));

        ExtinguishedTorchBlock.relight(level, pos, player, hand, stack, litState);
        return InteractionResult.SUCCESS;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData, Player player) {
        return new ItemStack(cloneItem);
    }
}