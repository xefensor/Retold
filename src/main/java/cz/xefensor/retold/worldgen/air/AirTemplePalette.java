package cz.xefensor.retold.worldgen.air;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

record AirTemplePalette(AirTemplePaletteKind kind) {
    BlockState islandTop(int x, int z) {
        if (kind == AirTemplePaletteKind.STONY) {
            return Blocks.STONE.defaultBlockState();
        }

        int noise = AirTempleBlocks.noise2(x, z, 9157);

        if (noise % 11 == 0) {
            return Blocks.PACKED_ICE.defaultBlockState();
        }

        if (noise % 3 == 0) {
            return Blocks.SNOW_BLOCK.defaultBlockState();
        }

        return Blocks.STONE.defaultBlockState();
    }

    BlockState islandBody(int x, int y, int z) {
        if (kind == AirTemplePaletteKind.FROZEN && AirTempleBlocks.noise3(x, y, z, 4211) % 17 == 0) {
            return Blocks.PACKED_ICE.defaultBlockState();
        }

        return Blocks.STONE.defaultBlockState();
    }

    BlockState craterOuter() {
        return kind == AirTemplePaletteKind.FROZEN
                ? Blocks.SNOW_BLOCK.defaultBlockState()
                : Blocks.COARSE_DIRT.defaultBlockState();
    }

    BlockState craterMiddle() {
        return Blocks.GRAVEL.defaultBlockState();
    }

    BlockState craterInner() {
        return Blocks.STONE.defaultBlockState();
    }
}
