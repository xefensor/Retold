package cz.xefensor.retold.behavior;

public final class RetoldMobState {
    private int hunger;
    private int stress;
    private int confidence = 50;

    private long lastHungerTickAt;
    private long lastAteAt;
    private long lastSeenAt;
    private long lastDangerAt;
    private long lastFleeEndedAt;

    public int hunger() {
        return hunger;
    }

    public void setHunger(int hunger) {
        this.hunger = clampPercent(hunger);
    }

    public void addHunger(int amount) {
        hunger = clampPercent(hunger + amount);
    }

    public int stress() {
        return stress;
    }

    public void setStress(int stress) {
        this.stress = clampPercent(stress);
    }

    public void addStress(int amount) {
        stress = clampPercent(stress + amount);
    }

    public int confidence() {
        return confidence;
    }

    public void setConfidence(int confidence) {
        this.confidence = clampPercent(confidence);
    }

    public void addConfidence(int amount) {
        confidence = clampPercent(confidence + amount);
    }

    public long lastHungerTickAt() {
        return lastHungerTickAt;
    }

    public void markHungerTick(long gameTime) {
        lastHungerTickAt = gameTime;
    }

    public long lastAteAt() {
        return lastAteAt;
    }

    public void markAte(long gameTime) {
        lastAteAt = gameTime;
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
    }

    public long lastFleeEndedAt() {
        return lastFleeEndedAt;
    }

    public void markFleeEnded(long gameTime) {
        lastFleeEndedAt = gameTime;
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
