package cz.xefensor.retold.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.TintedParticleLeavesBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Decaying, waterloggable Aender foliage with a persistent migration default.
 */
public final class AenderLeavesBlock extends TintedParticleLeavesBlock {
    public static final MapCodec<AenderLeavesBlock> CODEC = simpleCodec(AenderLeavesBlock::new);

    public AenderLeavesBlock(BlockBehaviour.Properties properties) {
        super(0.01F, properties);

        /*
         * Saved states from the former generic Block have no leaf properties.
         * Default them to persistent so upgrading does not destroy player builds.
         * World generation explicitly opts natural foliage into decay.
         */
        registerDefaultState(defaultBlockState().setValue(LeavesBlock.PERSISTENT, true));
    }

    @Override
    public MapCodec<? extends AenderLeavesBlock> codec() {
        return CODEC;
    }
}
