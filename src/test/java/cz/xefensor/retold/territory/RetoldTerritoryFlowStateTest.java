package cz.xefensor.retold.territory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RetoldTerritoryFlowStateTest {
    @Test
    void mapsWarningLevelsToExplicitFlowStates() {
        assertEquals(RetoldTerritoryFlowState.OBSERVING,
                RetoldTerritoryFlowState.fromWarningLevel(RetoldWarningLevel.NONE));
        assertEquals(RetoldTerritoryFlowState.OBSERVING,
                RetoldTerritoryFlowState.fromWarningLevel(RetoldWarningLevel.NOTICED));
        assertEquals(RetoldTerritoryFlowState.WARNING,
                RetoldTerritoryFlowState.fromWarningLevel(RetoldWarningLevel.WARNING));
        assertEquals(RetoldTerritoryFlowState.FINAL_WARNING,
                RetoldTerritoryFlowState.fromWarningLevel(RetoldWarningLevel.FINAL_WARNING));
        assertEquals(RetoldTerritoryFlowState.FINAL_WARNING,
                RetoldTerritoryFlowState.fromWarningLevel(RetoldWarningLevel.ATTACK));
    }

    @Test
    void attackEndsThroughCooldown() {
        assertTrue(RetoldTerritoryFlowState.ATTACKING.canTransitionTo(RetoldTerritoryFlowState.COOLDOWN));
        assertTrue(RetoldTerritoryFlowState.COOLDOWN.canTransitionTo(RetoldTerritoryFlowState.INACTIVE));
        assertFalse(RetoldTerritoryFlowState.ATTACKING.canTransitionTo(RetoldTerritoryFlowState.WARNING));
        assertFalse(RetoldTerritoryFlowState.COOLDOWN.canTransitionTo(RetoldTerritoryFlowState.WARNING));
    }

    @Test
    void retaliationCanEnterAttackFromNonCombatStates() {
        assertTrue(RetoldTerritoryFlowState.INACTIVE.canTransitionTo(RetoldTerritoryFlowState.ATTACKING));
        assertTrue(RetoldTerritoryFlowState.OBSERVING.canTransitionTo(RetoldTerritoryFlowState.ATTACKING));
        assertTrue(RetoldTerritoryFlowState.WARNING.canTransitionTo(RetoldTerritoryFlowState.ATTACKING));
        assertTrue(RetoldTerritoryFlowState.FINAL_WARNING.canTransitionTo(RetoldTerritoryFlowState.ATTACKING));
        assertTrue(RetoldTerritoryFlowState.COOLDOWN.canTransitionTo(RetoldTerritoryFlowState.ATTACKING));
    }

    @Test
    void onlyAnActiveAttackContinuesOutsideTerritory() {
        for (RetoldTerritoryFlowState state : RetoldTerritoryFlowState.values()) {
            assertEquals(
                    state == RetoldTerritoryFlowState.ATTACKING,
                    state.continuesOutsideTerritory(),
                    () -> "Unexpected outside-territory policy for " + state
            );
        }
    }
}
