package cz.xefensor.retold.behavior.core;

import cz.xefensor.retold.registry.RetoldTags;

import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class RetoldWeakBarriers {
    private RetoldWeakBarriers() {
    }

    public static boolean isBreakable(BlockState state) {
        if (state == null || !state.is(RetoldTags.WEAK_MOB_BARRIERS)) {
            return false;
        }

        return !state.hasProperty(FenceGateBlock.OPEN)
                || !state.getValue(FenceGateBlock.OPEN);
    }
}
