package cz.xefensor.retold.undead;

import net.minecraft.world.entity.Entity;

public final class RetoldUndeadCleansing {
    private RetoldUndeadCleansing() {
    }

    public static void cleanse(Entity entity) {
        entity.discard();
    }
}