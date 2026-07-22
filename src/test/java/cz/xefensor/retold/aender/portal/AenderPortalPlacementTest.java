package cz.xefensor.retold.aender.portal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AenderPortalPlacementTest {
    @Test
    void aenderSearchSkipsVoidColumnsButAcceptsIslandTerrain() {
        assertFalse(AenderPortalPlacement.canUseSurface(true, 0, 0));
        assertTrue(AenderPortalPlacement.canUseSurface(true, 72, 0));
        assertTrue(AenderPortalPlacement.canUseSurface(false, -64, -64));
    }

    @Test
    void aenderVoidFallbackUsesYOneHundredWithinDimensionBounds() {
        assertEquals(100, AenderPortalPlacement.fallbackY(true, 0, 64, 8, 250));
        assertEquals(80, AenderPortalPlacement.fallbackY(true, 0, 64, 8, 80));
    }

    @Test
    void overworldFallbackKeepsTheHigherSurfaceOrApproximateHeight() {
        assertEquals(96, AenderPortalPlacement.fallbackY(false, 72, 96, -56, 314));
        assertEquals(120, AenderPortalPlacement.fallbackY(false, 120, 70, -56, 314));
    }
}
