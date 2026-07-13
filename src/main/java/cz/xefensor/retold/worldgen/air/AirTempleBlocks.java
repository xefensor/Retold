package cz.xefensor.retold.worldgen.air;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.WeatheringCopperCollection;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

final class AirTempleBlocks {
    private AirTempleBlocks() {
    }

    static BlockState copper(WeatheringCopperCollection<Block> blocks, WeatheringCopper.WeatherState weather) {
        return blocks.weathering().pick(weather).defaultBlockState();
    }

    static BlockState cutCopper(WeatheringCopper.WeatherState weather) {
        return copper(Blocks.CUT_COPPER, weather);
    }

    static BlockState copperGrate(WeatheringCopper.WeatherState weather) {
        return copper(Blocks.COPPER_GRATE, weather);
    }

    static BlockState copperBulb(WeatheringCopper.WeatherState weather) {
        return copper(Blocks.COPPER_BULB, weather);
    }

    static void place(WorldGenLevel level, BoundingBox chunkBB, BlockPos pos, BlockState state) {
        if (chunkBB.isInside(pos)) {
            level.setBlock(pos, state, 2);
        }
    }

    static int noise2(int x, int z, int salt) {
        int value = x * 73428767 ^ z * 912931 ^ salt * 19349663;
        value ^= value >>> 13;
        value *= 1274126177;
        value ^= value >>> 16;
        return value & 0x7fffffff;
    }

    static int noise3(int x, int y, int z, int salt) {
        int value = x * 73428767 ^ y * 19349663 ^ z * 912931 ^ salt * 83492791;
        value ^= value >>> 13;
        value *= 1274126177;
        value ^= value >>> 16;
        return value & 0x7fffffff;
    }
}
