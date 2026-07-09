package cz.xefensor.retold.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public final class RetoldAnimalHomeMemory {
    private final RetoldAnimalHomeType type;
    private final ResourceKey<Level> dimension;
    private final BlockPos pos;
    private final long createdAt;
    private long lastUsedAt;

    public RetoldAnimalHomeMemory(
            RetoldAnimalHomeType type,
            ResourceKey<Level> dimension,
            BlockPos pos,
            long createdAt
    ) {
        this.type = type;
        this.dimension = dimension;
        this.pos = pos.immutable();
        this.createdAt = createdAt;
        this.lastUsedAt = createdAt;
    }

    public RetoldAnimalHomeMemory(
            RetoldAnimalHomeType type,
            ResourceKey<Level> dimension,
            BlockPos pos,
            long createdAt,
            long lastUsedAt
    ) {
        this.type = type;
        this.dimension = dimension;
        this.pos = pos.immutable();
        this.createdAt = createdAt;
        this.lastUsedAt = Math.max(createdAt, lastUsedAt);
    }

    public RetoldAnimalHomeType type() {
        return type;
    }

    public ResourceKey<Level> dimension() {
        return dimension;
    }

    public BlockPos pos() {
        return pos;
    }

    public long createdAt() {
        return createdAt;
    }

    public long lastUsedAt() {
        return lastUsedAt;
    }

    public void markUsed(long gameTime) {
        lastUsedAt = gameTime;
    }
}
