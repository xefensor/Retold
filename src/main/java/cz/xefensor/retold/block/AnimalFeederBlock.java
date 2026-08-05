package cz.xefensor.retold.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public final class AnimalFeederBlock extends BaseEntityBlock {
    public static final MapCodec<AnimalFeederBlock> CODEC = simpleCodec(AnimalFeederBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape NORTH_SOUTH_SHAPE = Shapes.or(
            Block.box(1.0D, 0.0D, 3.0D, 15.0D, 4.0D, 13.0D),
            Block.box(1.0D, 4.0D, 3.0D, 15.0D, 10.0D, 5.0D),
            Block.box(1.0D, 4.0D, 11.0D, 15.0D, 10.0D, 13.0D),
            Block.box(1.0D, 4.0D, 5.0D, 3.0D, 10.0D, 11.0D),
            Block.box(13.0D, 4.0D, 5.0D, 15.0D, 10.0D, 11.0D)
    );
    private static final VoxelShape EAST_WEST_SHAPE = Shapes.or(
            Block.box(3.0D, 0.0D, 1.0D, 13.0D, 4.0D, 15.0D),
            Block.box(3.0D, 4.0D, 1.0D, 5.0D, 10.0D, 15.0D),
            Block.box(11.0D, 4.0D, 1.0D, 13.0D, 10.0D, 15.0D),
            Block.box(5.0D, 4.0D, 1.0D, 11.0D, 10.0D, 3.0D),
            Block.box(5.0D, 4.0D, 13.0D, 11.0D, 10.0D, 15.0D)
    );

    public AnimalFeederBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(
                this.stateDefinition.any().setValue(FACING, Direction.NORTH)
        );
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AnimalFeederBlockEntity(pos, state);
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
        if (!AnimalFeederBlockEntity.canStore(stack)) {
            return player.isShiftKeyDown()
                    ? takeStoredFood(level, pos, player)
                    : InteractionResult.TRY_WITH_EMPTY_HAND;
        }

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (!(level.getBlockEntity(pos) instanceof AnimalFeederBlockEntity feeder)) {
            return InteractionResult.FAIL;
        }

        int offeredCount = player.isShiftKeyDown() ? stack.getCount() : 1;
        ItemStack offered = stack.copyWithCount(offeredCount);
        int inserted = feeder.insert(offered);

        if (inserted > 0) {
            if (!player.getAbilities().instabuild) {
                stack.shrink(inserted);

                if (stack.isEmpty()) {
                    player.setItemInHand(hand, ItemStack.EMPTY);
                }
            }

            level.playSound(
                    null,
                    pos,
                    SoundEvents.COMPOSTER_FILL,
                    SoundSource.BLOCKS,
                    0.8F,
                    1.0F
            );
        }

        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit
    ) {
        if (!player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }

        return takeStoredFood(level, pos, player);
    }

    private static InteractionResult takeStoredFood(
            Level level,
            BlockPos pos,
            Player player
    ) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (!(level.getBlockEntity(pos) instanceof AnimalFeederBlockEntity feeder)) {
            return InteractionResult.FAIL;
        }

        ItemStack removed = feeder.takeAll();

        if (!removed.isEmpty()) {
            player.addItem(removed);

            if (!removed.isEmpty()) {
                player.drop(removed, false);
            }

            level.playSound(
                    null,
                    pos,
                    SoundEvents.COMPOSTER_EMPTY,
                    SoundSource.BLOCKS,
                    0.8F,
                    1.0F
            );
        }

        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(
            BlockState state,
            Level level,
            BlockPos pos,
            Direction direction
    ) {
        return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(
                level.getBlockEntity(pos)
        );
    }

    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        Direction facing = state.getValue(FACING);
        return facing.getAxis() == Direction.Axis.X
                ? EAST_WEST_SHAPE
                : NORTH_SOUTH_SHAPE;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(
                FACING,
                context.getHorizontalDirection().getOpposite()
        );
    }
}
