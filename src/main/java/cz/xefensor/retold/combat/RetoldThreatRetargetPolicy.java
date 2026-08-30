package cz.xefensor.retold.combat;

/**
 * Pure scoring and hysteresis policy for recent-damage target changes.
 */
public final class RetoldThreatRetargetPolicy {
    public static final int RECENT_DAMAGE_WINDOW_TICKS = 20 * 5;
    public static final int RETARGET_LOCK_TICKS = 20;

    private static final double LARGEST_HIT_WEIGHT = 2.0D;
    private static final double TOTAL_DAMAGE_WEIGHT = 0.5D;
    private static final double REQUIRED_SCORE_MULTIPLIER = 1.2D;
    private static final double REQUIRED_SCORE_ADVANTAGE = 1.0D;

    private RetoldThreatRetargetPolicy() {
    }

    public static double score(float totalDamage, float largestHit) {
        double safeTotal = Math.max(0.0D, totalDamage);
        double safeLargestHit = Math.max(0.0D, largestHit);

        return safeLargestHit * LARGEST_HIT_WEIGHT
                + safeTotal * TOTAL_DAMAGE_WEIGHT;
    }

    public static boolean shouldSwitch(
            double currentScore,
            double challengerScore,
            long ticksSinceLastSwitch
    ) {
        if (ticksSinceLastSwitch >= 0L
                && ticksSinceLastSwitch < RETARGET_LOCK_TICKS) {
            return false;
        }

        double safeCurrent = Math.max(0.0D, currentScore);
        double safeChallenger = Math.max(0.0D, challengerScore);
        double requiredScore = safeCurrent * REQUIRED_SCORE_MULTIPLIER
                + REQUIRED_SCORE_ADVANTAGE;

        return safeChallenger >= requiredScore;
    }

    public static boolean preservesExistingDuty(RetoldTargetSource source) {
        return source == RetoldTargetSource.OWNER_DEFENSE
                || source == RetoldTargetSource.TERRITORY_ATTACK;
    }
}
