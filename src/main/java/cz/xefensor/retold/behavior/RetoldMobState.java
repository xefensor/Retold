package cz.xefensor.retold.behavior;

import net.minecraft.nbt.CompoundTag;

public final class RetoldMobState {
    private static final int SAVE_VERSION = 1;

    private int hunger;
    private int stress;
    private int confidence = 50;

    private long lastHungerTickAt;
    private long lastAteAt;
    private long lastSeenAt;
    private long lastDangerAt;
    private long lastFleeEndedAt;
    private long lastSuccessfulHuntAt;
    private long lastFailedHuntAt;

    private Runnable saveCallback;

    void setSaveCallback(Runnable saveCallback) {
        this.saveCallback = saveCallback;
    }

    public int hunger() {
        return hunger;
    }

    public void setHunger(int hunger) {
        this.hunger = clampPercent(hunger);
        markChanged();
    }

    public void addHunger(int amount) {
        hunger = clampPercent(hunger + amount);
        markChanged();
    }

    public int stress() {
        return stress;
    }

    public void setStress(int stress) {
        this.stress = clampPercent(stress);
        markChanged();
    }

    public void addStress(int amount) {
        stress = clampPercent(stress + amount);
        markChanged();
    }

    public int confidence() {
        return confidence;
    }

    public void setConfidence(int confidence) {
        this.confidence = clampPercent(confidence);
        markChanged();
    }

    public void addConfidence(int amount) {
        confidence = clampPercent(confidence + amount);
        markChanged();
    }

    public long lastHungerTickAt() {
        return lastHungerTickAt;
    }

    public void markHungerTick(long gameTime) {
        lastHungerTickAt = gameTime;
        markChanged();
    }

    public long lastAteAt() {
        return lastAteAt;
    }

    public void markAte(long gameTime) {
        lastAteAt = gameTime;
        markChanged();
    }

    public void markFed(long gameTime) {
        addStress(-2);
        addConfidence(2);
        markAte(gameTime);
    }

    public long lastSeenAt() {
        return lastSeenAt;
    }

    public void markSeen(long gameTime) {
        lastSeenAt = gameTime;
    }

    public long lastDangerAt() {
        return lastDangerAt;
    }

    public void markDanger(long gameTime) {
        lastDangerAt = gameTime;
        markChanged();
    }

    public long lastFleeEndedAt() {
        return lastFleeEndedAt;
    }

    public void markFleeEnded(long gameTime) {
        lastFleeEndedAt = gameTime;
        markChanged();
    }

    public long lastSuccessfulHuntAt() {
        return lastSuccessfulHuntAt;
    }

    public void markSuccessfulHunt(long gameTime) {
        lastSuccessfulHuntAt = gameTime;
        addStress(-3);
        addConfidence(4);
        markChanged();
    }

    public long lastFailedHuntAt() {
        return lastFailedHuntAt;
    }

    public void markFailedHunt(long gameTime) {
        lastFailedHuntAt = gameTime;
        addStress(4);
        addConfidence(-3);
        markChanged();
    }

    CompoundTag save() {
        CompoundTag tag = new CompoundTag();

        tag.putInt("version", SAVE_VERSION);
        tag.putInt("hunger", hunger);
        tag.putInt("stress", stress);
        tag.putInt("confidence", confidence);
        tag.putLong("lastHungerTickAt", lastHungerTickAt);
        tag.putLong("lastAteAt", lastAteAt);
        tag.putLong("lastDangerAt", lastDangerAt);
        tag.putLong("lastFleeEndedAt", lastFleeEndedAt);
        tag.putLong("lastSuccessfulHuntAt", lastSuccessfulHuntAt);
        tag.putLong("lastFailedHuntAt", lastFailedHuntAt);

        return tag;
    }

    static RetoldMobState load(CompoundTag tag) {
        RetoldMobState state = new RetoldMobState();

        if (tag == null || tag.isEmpty()) {
            return state;
        }

        state.hunger = clampPercent(tag.getInt("hunger").orElse(0));
        state.stress = clampPercent(tag.getInt("stress").orElse(0));
        state.confidence = clampPercent(tag.getInt("confidence").orElse(50));
        state.lastHungerTickAt = tag.getLong("lastHungerTickAt").orElse(0L);
        state.lastAteAt = tag.getLong("lastAteAt").orElse(0L);
        state.lastDangerAt = tag.getLong("lastDangerAt").orElse(0L);
        state.lastFleeEndedAt = tag.getLong("lastFleeEndedAt").orElse(0L);
        state.lastSuccessfulHuntAt = tag.getLong("lastSuccessfulHuntAt").orElse(0L);
        state.lastFailedHuntAt = tag.getLong("lastFailedHuntAt").orElse(0L);

        return state;
    }

    private void markChanged() {
        if (saveCallback != null) {
            saveCallback.run();
        }
    }

    private static int clampPercent(int value) {
        if (value < 0) {
            return 0;
        }

        return Math.min(
                100,
                value
        );
    }
}
