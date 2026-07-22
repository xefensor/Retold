package cz.xefensor.retold.block;

import com.mojang.serialization.MapCodec;
import cz.xefensor.retold.Retold;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.placement.VegetationPlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.SpreadingSnowyBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import java.util.List;
import java.util.Optional;

/**
 * Living Aender surface block that spreads only across Aender soil.
 */
public final class AenderGrassBlock extends SpreadingSnowyBlock implements BonemealableBlock {
    public static final MapCodec<AenderGrassBlock> CODEC = simpleCodec(AenderGrassBlock::new);

    private static final ResourceKey<Block> AENDER_SOIL = ResourceKey.create(
            Registries.BLOCK,
            Identifier.fromNamespaceAndPath(Retold.MODID, "aender_soil")
    );

    public AenderGrassBlock(BlockBehaviour.Properties properties) {
        super(properties, AENDER_SOIL);
    }

    @Override
    protected MapCodec<? extends SpreadingSnowyBlock> codec() {
        return CODEC;
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return level.getBlockState(pos.above()).isAir() && level.isInsideBuildHeight(pos.above());
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        BlockPos above = pos.above();
        BlockState shortGrass = Blocks.SHORT_GRASS.defaultBlockState();
        Optional<Holder.Reference<PlacedFeature>> grassFeature = level.registryAccess()
                .lookupOrThrow(Registries.PLACED_FEATURE)
                .get(VegetationPlacements.GRASS_BONEMEAL);

        bonemealAttempts:
        for (int attempt = 0; attempt < 128; attempt++) {
            BlockPos target = above;

            for (int step = 0; step < attempt / 16; step++) {
                target = target.offset(
                        random.nextInt(3) - 1,
                        (random.nextInt(3) - 1) * random.nextInt(3) / 2,
                        random.nextInt(3) - 1
                );

                if (!level.getBlockState(target.below()).is(this)
                        || level.getBlockState(target).isCollisionShapeFullBlock(level, target)) {
                    continue bonemealAttempts;
                }
            }

            BlockState targetState = level.getBlockState(target);

            if (targetState.is(shortGrass.getBlock()) && random.nextInt(10) == 0) {
                BonemealableBlock bonemealable = (BonemealableBlock) shortGrass.getBlock();

                if (bonemealable.isValidBonemealTarget(level, target, targetState)) {
                    bonemealable.performBonemeal(level, random, target, targetState);
                }
            }

            if (!targetState.isAir() || level.isOutsideBuildHeight(target)) {
                continue;
            }

            if (random.nextInt(8) == 0) {
                List<ConfiguredFeature<?, ?>> features = level.getBiome(target)
                        .value()
                        .getGenerationSettings()
                        .getBoneMealFeatures();

                if (!features.isEmpty()) {
                    Util.getRandom(features, random)
                            .place(level, level.getChunkSource().getGenerator(), random, target);
                }
            } else if (grassFeature.isPresent()) {
                grassFeature.get().value().place(level, level.getChunkSource().getGenerator(), random, target);
            }
        }
    }

    @Override
    public BonemealableBlock.Type getType() {
        return BonemealableBlock.Type.NEIGHBOR_SPREADER;
    }
}
