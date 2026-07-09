package cz.xefensor.retold.chronolith;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.UUID;

public final class ChronolithChannel {
    private final UUID playerId;
    private final ResourceKey<Level> dimension;
    private final BlockPos pos;
    private float xpDrainProgress;
    private float timeTickProgress;
    private int activeTicks;
    private int activeSoundCooldown;
    private int activeParticleCooldown;

    public ChronolithChannel(UUID playerId, ResourceKey<Level> dimension, BlockPos pos) {
        this.playerId = playerId;
        this.dimension = dimension;
        this.pos = pos;
    }

    public UUID playerId() {
        return playerId;
    }

    public ResourceKey<Level> dimension() {
        return dimension;
    }

    public BlockPos pos() {
        return pos;
    }

    public float xpDrainProgress() {
        return xpDrainProgress;
    }

    public void setXpDrainProgress(float xpDrainProgress) {
        this.xpDrainProgress = xpDrainProgress;
    }

    public float timeTickProgress() {
        return timeTickProgress;
    }

    public void setTimeTickProgress(float timeTickProgress) {
        this.timeTickProgress = timeTickProgress;
    }

    public int activeTicks() {
        return activeTicks;
    }

    public void incrementActiveTicks() {
        activeTicks++;
    }

    public int activeSoundCooldown() {
        return activeSoundCooldown;
    }

    public void resetActiveSoundCooldown() {
        activeSoundCooldown = 0;
    }

    public void incrementActiveSoundCooldown() {
        activeSoundCooldown++;
    }

    public int activeParticleCooldown() {
        return activeParticleCooldown;
    }

    public void resetActiveParticleCooldown() {
        activeParticleCooldown = 0;
    }

    public void incrementActiveParticleCooldown() {
        activeParticleCooldown++;
    }

    public boolean isSameBlock(ResourceKey<Level> dimension, BlockPos pos) {
        return this.dimension.equals(dimension) && this.pos.equals(pos);
    }
}
