package cz.xefensor.retold.aender.portal;

/**
 * Pure timing policy for holding an Aender portal transition until its arrival area is ready.
 */
public final class AenderPortalTransitionGate {
    private AenderPortalTransitionGate() {
    }

    public static int transitionTime(
            int configuredDelay,
            boolean invulnerable,
            boolean enteringAender,
            boolean arrivalReady,
            int portalTime,
            int survivalMinimumCharge
    ) {
        if (!enteringAender) {
            return configuredDelay;
        }

        int minimumCharge = invulnerable
                ? configuredDelay
                : Math.max(configuredDelay, survivalMinimumCharge);

        if (arrivalReady || portalTime < minimumCharge) {
            return minimumCharge;
        }

        return portalTime + 1;
    }
}
