package cz.xefensor.retold.behavior.profiles;

import net.minecraft.nbt.CompoundTag;

public final class RetoldMobState {
    private static final int SAVE_VERSION = 3;

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
    private long breedingSatisfiedTicks;
    private long lastBreedingProgressAt;
    private long automaticBreedingArmedAt;
    private long nextBreedingAttemptAt;

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

    public void applyHungerCatchUp(
            int reconciledHunger,
            long simulatedThrough,
            int mealsConsumed,
            long lastMealAt
    ) {
        applyHungerCatchUp(
                reconciledHunger,
                simulatedThrough,
                mealsConsumed,
                lastMealAt,
                0,
                0L
        );
    }

    public void applyHungerCatchUp(
            int reconciledHunger,
            long simulatedThrough,
            int mealsConsumed,
            long lastMealAt,
            int successfulHunts,
            long lastSuccessfulHuntAt
    ) {
        hunger = clampPercent(reconciledHunger);
        lastHungerTickAt = Math.max(0L, simulatedThrough);

        if (mealsConsumed > 0) {
            stress = clampPercent(stress - 2 * mealsConsumed);
            confidence = clampPercent(confidence + 2 * mealsConsumed);
            lastAteAt = Math.max(0L, lastMealAt);
        }

        if (successfulHunts > 0) {
            stress = clampPercent(stress - 3 * successfulHunts);
            confidence = clampPercent(confidence + 4 * successfulHunts);
            this.lastSuccessfulHuntAt = Math.max(0L, lastSuccessfulHuntAt);
        }

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

    public long breedingSatisfiedTicks() {
        return breedingSatisfiedTicks;
    }

    public void setBreedingSatisfiedTicks(long ticks) {
        long updated = Math.max(0L, ticks);

        if (breedingSatisfiedTicks == updated) {
            return;
        }

        breedingSatisfiedTicks = updated;
        markChanged();
    }

    public void advanceBreedingSatisfaction(
            long gameTime,
            int maximumLoadedTicks
    ) {
        long updatedAt = Math.max(0L, gameTime);
        long gained = lastBreedingProgressAt > 0L
                && updatedAt > lastBreedingProgressAt
                ? Math.min(
                        Math.max(0, maximumLoadedTicks),
                        updatedAt - lastBreedingProgressAt
                )
                : 0L;

        breedingSatisfiedTicks += gained;
        lastBreedingProgressAt = updatedAt;
        markChanged();
    }

    public void applyBreedingCatchUp(
            long satisfiedTicks,
            long simulatedThrough
    ) {
        breedingSatisfiedTicks = Math.max(0L, satisfiedTicks);
        lastBreedingProgressAt = Math.max(0L, simulatedThrough);
        markChanged();
    }

    public void clearBreedingSatisfaction() {
        if (breedingSatisfiedTicks == 0L
                && lastBreedingProgressAt == 0L) {
            return;
        }

        breedingSatisfiedTicks = 0L;
        lastBreedingProgressAt = 0L;
        markChanged();
    }

    public long automaticBreedingArmedAt() {
        return automaticBreedingArmedAt;
    }

    public void markAutomaticBreedingArmed(long gameTime) {
        long updated = Math.max(1L, gameTime);

        if (automaticBreedingArmedAt == updated) {
            return;
        }

        automaticBreedingArmedAt = updated;
        markChanged();
    }

    public void clearAutomaticBreedingArmed() {
        if (automaticBreedingArmedAt == 0L) {
            return;
        }

        automaticBreedingArmedAt = 0L;
        markChanged();
    }

    public long nextBreedingAttemptAt() {
        return nextBreedingAttemptAt;
    }

    public void scheduleNextBreedingAttempt(long gameTime) {
        long updated = Math.max(0L, gameTime);

        if (nextBreedingAttemptAt == updated) {
            return;
        }

        nextBreedingAttemptAt = updated;
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
        tag.putLong("breedingSatisfiedTicks", breedingSatisfiedTicks);
        tag.putLong("lastBreedingProgressAt", lastBreedingProgressAt);
        tag.putLong("automaticBreedingArmedAt", automaticBreedingArmedAt);
        tag.putLong("nextBreedingAttemptAt", nextBreedingAttemptAt);

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
        state.breedingSatisfiedTicks = tag.getLong(
                "breedingSatisfiedTicks"
        ).orElse(0L);
        state.lastBreedingProgressAt = tag.getLong(
                "lastBreedingProgressAt"
        ).orElse(0L);
        state.automaticBreedingArmedAt = tag.getLong(
                "automaticBreedingArmedAt"
        ).orElse(0L);
        state.nextBreedingAttemptAt = tag.getLong(
                "nextBreedingAttemptAt"
        ).orElse(0L);

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
