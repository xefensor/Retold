package cz.xefensor.retold.dragon;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.end.EnderDragonFight;

public final class RetoldEndExitFountain {
    private static final int RADIUS = 8;
    private static final int MIN_Y = 0;
    private static final int MAX_Y = 128;

    private RetoldEndExitFountain() {
    }

    public static void hideUntilDragonKilled(ServerLevel endLevel, EnderDragonFight dragonFight) {
        if (dragonFight.hasPreviouslyKilledDragon()) {
            return;
        }

        removeFountainBlocks(endLevel);
    }

    private static void removeFountainBlocks(ServerLevel level) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int z = -RADIUS; z <= RADIUS; z++) {
                for (int y = MIN_Y; y <= MAX_Y; y++) {
                    pos.set(x, y, z);

                    if (isFountainBlock(level, pos)) {
                        level.removeBlock(pos, false);
                    }
                }
            }
        }
    }

    private static boolean isFountainBlock(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).is(Blocks.BEDROCK)
                || level.getBlockState(pos).is(Blocks.END_PORTAL)
                || level.getBlockState(pos).is(Blocks.DRAGON_EGG)
                || level.getBlockState(pos).is(Blocks.TORCH)
                || level.getBlockState(pos).is(Blocks.WALL_TORCH);
    }
}