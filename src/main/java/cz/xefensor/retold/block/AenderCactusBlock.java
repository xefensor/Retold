package cz.xefensor.retold.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.CommonHooks;

/** Lavender Aender cactus that grows without producing the Overworld cactus flower. */
public final class AenderCactusBlock extends CactusBlock {
    public AenderCactusBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockPos above = pos.above();

        if (!level.isEmptyBlock(above)) {
            return;
        }

        int height = 1;

        while (level.getBlockState(pos.below(height)).is(this)) {
            height++;
        }

        int age = state.getValue(AGE);

        if (!CommonHooks.canCropGrow(level, above, state, true)) {
            return;
        }

        if (age == MAX_AGE && height < 3 && canSurvive(defaultBlockState(), level, above)) {
            level.setBlockAndUpdate(above, defaultBlockState());
            level.setBlock(pos, state.setValue(AGE, 0), 260);
            CommonHooks.fireCropGrowPost(level, pos, state);
        } else if (age < MAX_AGE) {
            level.setBlock(pos, state.setValue(AGE, age + 1), 260);
        }
    }
}
