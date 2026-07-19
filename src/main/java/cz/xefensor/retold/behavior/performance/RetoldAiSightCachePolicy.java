package cz.xefensor.retold.behavior.performance;

final class RetoldAiSightCachePolicy {
    private static final double MAX_OBSERVER_DRIFT_SQUARED = 3.0D * 3.0D;
    private static final double MAX_TARGET_DRIFT_SQUARED = 3.0D * 3.0D;
    private static final double MAX_STALE_DRIFT_SQUARED = 6.0D * 6.0D;

    private RetoldAiSightCachePolicy() {
    }

    static boolean isNormalCacheHit(
            long gameTime,
            long expiresAt,
            double observerDriftSquared,
            double targetDriftSquared
    ) {
        return gameTime < expiresAt
                && observerDriftSquared <= MAX_OBSERVER_DRIFT_SQUARED
                && targetDriftSquared <= MAX_TARGET_DRIFT_SQUARED;
    }

    static boolean isStaleCandidate(
            double observerDriftSquared,
            double targetDriftSquared
    ) {
        return observerDriftSquared <= MAX_STALE_DRIFT_SQUARED
                && targetDriftSquared <= MAX_STALE_DRIFT_SQUARED;
    }
}
