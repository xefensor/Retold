package cz.xefensor.retold.aender.portal;

/**
 * Pure placement policy for generated Aender portal counterparts.
 */
final class AenderPortalPlacement {
    static final int AENDER_VOID_FALLBACK_Y = 100;

    private AenderPortalPlacement() {
    }

    static boolean canUseSurface(boolean destinationIsAender, int surfaceY, int minY) {
        return !destinationIsAender || surfaceY > minY;
    }

    static int fallbackY(
            boolean destinationIsAender,
            int surfaceY,
            int approximateY,
            int minPortalY,
            int maxPortalY
    ) {
        int requestedY = destinationIsAender
                ? AENDER_VOID_FALLBACK_Y
                : Math.max(surfaceY, approximateY);
        return Math.max(minPortalY, Math.min(requestedY, maxPortalY));
    }
}
