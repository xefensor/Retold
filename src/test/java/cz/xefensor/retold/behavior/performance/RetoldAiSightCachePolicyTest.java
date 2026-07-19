package cz.xefensor.retold.behavior.performance;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RetoldAiSightCachePolicyTest {
    private static final long EXPIRES_AT = 100L;

    @Test
    void normalEntryIsUsableOneTickBeforeExpiry() {
        assertTrue(RetoldAiSightCachePolicy.isNormalCacheHit(
                EXPIRES_AT - 1L,
                EXPIRES_AT,
                0.0D,
                0.0D
        ));
    }

    @Test
    void normalEntryIsUnusableExactlyAtExpiry() {
        assertFalse(RetoldAiSightCachePolicy.isNormalCacheHit(
                EXPIRES_AT,
                EXPIRES_AT,
                0.0D,
                0.0D
        ));
    }

    @Test
    void normalEntryRemainsUnusableAfterExpiry() {
        assertFalse(RetoldAiSightCachePolicy.isNormalCacheHit(
                EXPIRES_AT + 1L,
                EXPIRES_AT,
                0.0D,
                0.0D
        ));
    }

    @Test
    void expiredEntryCanStillQualifyAsAStaleCandidate() {
        assertFalse(RetoldAiSightCachePolicy.isNormalCacheHit(
                EXPIRES_AT,
                EXPIRES_AT,
                0.0D,
                0.0D
        ));
        assertTrue(RetoldAiSightCachePolicy.isStaleCandidate(0.0D, 0.0D));
    }

    @Test
    void driftBeyondNormalLimitButWithinStaleLimitIsStaleOnly() {
        double staleOnlyDriftSquared = 4.0D * 4.0D;

        assertFalse(RetoldAiSightCachePolicy.isNormalCacheHit(
                EXPIRES_AT - 1L,
                EXPIRES_AT,
                staleOnlyDriftSquared,
                staleOnlyDriftSquared
        ));
        assertTrue(RetoldAiSightCachePolicy.isStaleCandidate(
                staleOnlyDriftSquared,
                staleOnlyDriftSquared
        ));
    }
}
