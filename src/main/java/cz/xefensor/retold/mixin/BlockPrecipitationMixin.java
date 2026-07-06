package cz.xefensor.retold.mixin;

import cz.xefensor.retold.registry.RetoldBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Block.class)
public abstract class BlockPrecipitationMixin {
    @Inject(method = "handlePrecipitation", at = @At("HEAD"), cancellable = true)
    private void retold$extinguishTorches(
            BlockState state,
            Level level,
            BlockPos pos,
            Biome.Precipitation precipitation,
            CallbackInfo ci
    ) {
        if (level.isClientSide()) {
            return;
        }

        if (precipitation != Biome.Precipitation.RAIN && precipitation != Biome.Precipitation.SNOW) {
            return;
        }

        BlockState extinguishedState = getExtinguishedState(state);
        if (extinguishedState == null) {
            return;
        }

        level.setBlock(pos, extinguishedState, Block.UPDATE_ALL);
        playExtinguishEffects(level, pos);

        ci.cancel();
    }

    private static BlockState getExtinguishedState(BlockState state) {
        if (state.is(Blocks.TORCH)) {
            return RetoldBlocks.EXTINGUISHED_TORCH.get().defaultBlockState();
        }

        if (state.is(Blocks.SOUL_TORCH)) {
            return RetoldBlocks.EXTINGUISHED_SOUL_TORCH.get().defaultBlockState();
        }

        if (state.is(Blocks.COPPER_TORCH)) {
            return RetoldBlocks.EXTINGUISHED_COPPER_TORCH.get().defaultBlockState();
        }

        if (state.is(Blocks.WALL_TORCH)) {
            return RetoldBlocks.EXTINGUISHED_WALL_TORCH.get()
                    .defaultBlockState()
                    .setValue(WallTorchBlock.FACING, state.getValue(WallTorchBlock.FACING));
        }

        if (state.is(Blocks.SOUL_WALL_TORCH)) {
            return RetoldBlocks.EXTINGUISHED_SOUL_WALL_TORCH.get()
                    .defaultBlockState()
                    .setValue(WallTorchBlock.FACING, state.getValue(WallTorchBlock.FACING));
        }

        if (state.is(Blocks.COPPER_WALL_TORCH)) {
            return RetoldBlocks.EXTINGUISHED_COPPER_WALL_TORCH.get()
                    .defaultBlockState()
                    .setValue(WallTorchBlock.FACING, state.getValue(WallTorchBlock.FACING));
        }

        return null;
    }

    private static void playExtinguishEffects(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.7F, 1.4F);

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    ParticleTypes.SMOKE,
                    pos.getX() + 0.5,
                    pos.getY() + 0.7,
                    pos.getZ() + 0.5,
                    12,
                    0.18,
                    0.18,
                    0.18,
                    0.01
            );
        }
    }
}