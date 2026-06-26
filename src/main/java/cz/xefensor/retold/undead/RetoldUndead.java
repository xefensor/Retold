package cz.xefensor.retold.undead;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;

public final class RetoldUndead {
    private RetoldUndead() {
    }

    public static boolean isUndead(Entity entity) {
        return entity instanceof Zombie
                || entity instanceof AbstractSkeleton;
    }
}