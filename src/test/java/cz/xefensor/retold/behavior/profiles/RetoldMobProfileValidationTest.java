package cz.xefensor.retold.behavior.profiles;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RetoldMobProfileValidationTest {
    @Test
    void acceptsValidProfile() {
        assertTrue(RetoldMobProfileValidation.validate("pack_predator", 460, 18, 36).isEmpty());
    }

    @Test
    void rejectsReservedFallbackType() {
        assertEquals(
                "The NONE profile type is reserved for fallback behavior",
                RetoldMobProfileValidation.validate("none", 0, 101, 101).orElseThrow()
        );
    }

    @Test
    void rejectsInvalidNumericRanges() {
        assertTrue(RetoldMobProfileValidation.validate("pack_predator", -1, 18, 36).isPresent());
        assertTrue(RetoldMobProfileValidation.validate("pack_predator", 460, 102, 36).isPresent());
        assertTrue(RetoldMobProfileValidation.validate("pack_predator", 460, 18, -1).isPresent());
    }
}
