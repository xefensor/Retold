package cz.xefensor.retold.enchanting;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RetoldEnchantingCostsTest {
    @Test
    void chargesThreeLapisAndFiveExperienceLevelsPerEnchantmentLevel() {
        assertEquals(3, RetoldEnchantingCosts.LAPIS_PER_CAST);
        assertEquals(5, RetoldEnchantingCosts.experienceLevelCost(1));
        assertEquals(25, RetoldEnchantingCosts.experienceLevelCost(5));
        assertThrows(
                IllegalArgumentException.class,
                () -> RetoldEnchantingCosts.experienceLevelCost(0)
        );
    }
}
