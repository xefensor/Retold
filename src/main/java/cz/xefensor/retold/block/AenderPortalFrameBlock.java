package cz.xefensor.retold.block;

import com.mojang.serialization.MapCodec;
import cz.xefensor.retold.aender.portal.AenderPortalLogic;
import cz.xefensor.retold.aender.portal.AenderPortalShape;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public final class AenderPortalFrameBlock extends Block {
    public static final MapCodec<AenderPortalFrameBlock> CODEC = simpleCodec(AenderPortalFrameBlock::new);

    public AenderPortalFrameBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);

        if (level instanceof ServerLevel serverLevel && !oldState.is(state.getBlock())) {
            AenderPortalShape.findEmptyNextToFrame(level, pos)
                    .ifPresent(shape -> AenderPortalLogic.activatePortal(serverLevel, shape));
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        AenderPortalShape.invalidateNextToMissingFrame(level, pos);
    }
}
