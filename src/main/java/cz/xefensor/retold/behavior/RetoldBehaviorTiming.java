package cz.xefensor.retold.behavior;

import net.minecraft.world.entity.Entity;

public final class RetoldBehaviorTiming {
    private RetoldBehaviorTiming() {
    }

    public static boolean shouldThink(
            Entity entity,
            long gameTime,
            int intervalTicks
    ) {
        if (entity == null || intervalTicks <= 0) {
            return false;
        }

        int offset = Math.floorMod(
                entity.getId(),
                intervalTicks
        );

        return (gameTime + offset) % intervalTicks == 0L;
    }
}
