package cz.xefensor.retold.behavior;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RetoldHungerStageTest {
    @Test
    void mapsEveryBoundaryToTheExpectedStage() {
        assertEquals(RetoldHungerStage.FULL, RetoldHungerStage.fromHunger(-1));
        assertEquals(RetoldHungerStage.FULL, RetoldHungerStage.fromHunger(20));
        assertEquals(RetoldHungerStage.EASY_FOOD, RetoldHungerStage.fromHunger(21));
        assertEquals(RetoldHungerStage.EASY_FOOD, RetoldHungerStage.fromHunger(40));
        assertEquals(RetoldHungerStage.ACTIVE_SEARCH, RetoldHungerStage.fromHunger(41));
        assertEquals(RetoldHungerStage.ACTIVE_SEARCH, RetoldHungerStage.fromHunger(65));
        assertEquals(RetoldHungerStage.RISKY_FOOD, RetoldHungerStage.fromHunger(66));
        assertEquals(RetoldHungerStage.RISKY_FOOD, RetoldHungerStage.fromHunger(85));
        assertEquals(RetoldHungerStage.DESPERATE, RetoldHungerStage.fromHunger(86));
        assertEquals(RetoldHungerStage.DESPERATE, RetoldHungerStage.fromHunger(100));
        assertEquals(RetoldHungerStage.DISABLED, RetoldHungerStage.fromHunger(101));
    }

    @Test
    void disabledIsOnlyAtLeastItself() {
        assertTrue(RetoldHungerStage.DISABLED.isAtLeast(RetoldHungerStage.DISABLED));
        assertFalse(RetoldHungerStage.DISABLED.isAtLeast(RetoldHungerStage.DESPERATE));
        assertFalse(RetoldHungerStage.DESPERATE.isAtLeast(RetoldHungerStage.DISABLED));
        assertFalse(RetoldHungerStage.DESPERATE.isAtLeast(null));
    }
}
