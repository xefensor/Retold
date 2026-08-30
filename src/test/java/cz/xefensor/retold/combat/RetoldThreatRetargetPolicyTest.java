package cz.xefensor.retold.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RetoldThreatRetargetPolicyTest {
    @Test
    void materiallyLargerHitOvercomesTargetInertia() {
        double current = RetoldThreatRetargetPolicy.score(6.0F, 6.0F);
        double challenger = RetoldThreatRetargetPolicy.score(8.0F, 8.0F);

        assertTrue(RetoldThreatRetargetPolicy.shouldSwitch(
                current,
                challenger,
                RetoldThreatRetargetPolicy.RETARGET_LOCK_TICKS
        ));
    }

    @Test
    void smallDamageDifferenceDoesNotCauseTargetChurn() {
        double current = RetoldThreatRetargetPolicy.score(6.0F, 6.0F);
        double challenger = RetoldThreatRetargetPolicy.score(7.0F, 7.0F);

        assertFalse(RetoldThreatRetargetPolicy.shouldSwitch(
                current,
                challenger,
                RetoldThreatRetargetPolicy.RETARGET_LOCK_TICKS
        ));
    }

    @Test
    void burstDamageCanOutweighSeveralSmallHits() {
        double current = RetoldThreatRetargetPolicy.score(9.0F, 3.0F);
        double challenger = RetoldThreatRetargetPolicy.score(6.0F, 6.0F);

        assertTrue(RetoldThreatRetargetPolicy.shouldSwitch(
                current,
                challenger,
                RetoldThreatRetargetPolicy.RETARGET_LOCK_TICKS
        ));
    }

    @Test
    void oneSecondLockPreventsImmediatePingPong() {
        assertFalse(RetoldThreatRetargetPolicy.shouldSwitch(
                0.0D,
                100.0D,
                RetoldThreatRetargetPolicy.RETARGET_LOCK_TICKS - 1L
        ));
        assertTrue(RetoldThreatRetargetPolicy.shouldSwitch(
                0.0D,
                100.0D,
                RetoldThreatRetargetPolicy.RETARGET_LOCK_TICKS
        ));
    }

    @Test
    void ownerAndTerritoryDutiesRemainAuthoritative() {
        assertTrue(RetoldThreatRetargetPolicy.preservesExistingDuty(
                RetoldTargetSource.OWNER_DEFENSE
        ));
        assertTrue(RetoldThreatRetargetPolicy.preservesExistingDuty(
                RetoldTargetSource.TERRITORY_ATTACK
        ));
        assertFalse(RetoldThreatRetargetPolicy.preservesExistingDuty(
                RetoldTargetSource.FACTION_COMBAT
        ));
        assertFalse(RetoldThreatRetargetPolicy.preservesExistingDuty(
                RetoldTargetSource.RETALIATION
        ));
    }
}
