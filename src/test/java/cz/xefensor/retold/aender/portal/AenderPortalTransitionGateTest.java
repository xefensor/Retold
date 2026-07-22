package cz.xefensor.retold.aender.portal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AenderPortalTransitionGateTest {
    private static final int SURVIVAL_MINIMUM_CHARGE = 100;

    @Test
    void enteringAenderWaitsAfterChargeUntilSafeCoreIsReady() {
        assertEquals(
                100,
                AenderPortalTransitionGate.transitionTime(
                        80, false, true, false, 40, SURVIVAL_MINIMUM_CHARGE
                )
        );
        assertEquals(
                121,
                AenderPortalTransitionGate.transitionTime(
                        80, false, true, false, 120, SURVIVAL_MINIMUM_CHARGE
                )
        );
        assertEquals(
                100,
                AenderPortalTransitionGate.transitionTime(
                        80, false, true, true, 120, SURVIVAL_MINIMUM_CHARGE
                )
        );
    }

    @Test
    void leavingAenderKeepsConfiguredPortalDelay() {
        assertEquals(
                80,
                AenderPortalTransitionGate.transitionTime(
                        80, false, false, false, 120, SURVIVAL_MINIMUM_CHARGE
                )
        );
    }

    @Test
    void creativePlayersStillWaitForSafetyWithoutAnArtificialCharge() {
        assertEquals(
                1,
                AenderPortalTransitionGate.transitionTime(
                        0, true, true, false, 0, SURVIVAL_MINIMUM_CHARGE
                )
        );
        assertEquals(
                0,
                AenderPortalTransitionGate.transitionTime(
                        0, true, true, true, 5, SURVIVAL_MINIMUM_CHARGE
                )
        );
    }
}
