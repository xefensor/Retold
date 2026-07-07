package cz.xefensor.retold.behavior;

public final class RetoldMobState {
    private int hunger;
    private int stress;
    private int confidence = 50;

    private long lastHungerTickAt;
    private long lastAteAt;
    private long lastSeenAt;

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

    public long lastSeenAt() {
        return lastSeenAt;
    }

    public void markSeen(long gameTime) {
        lastSeenAt = gameTime;
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