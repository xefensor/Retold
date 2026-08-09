package cz.xefensor.retold.enchanting;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RetoldTransferredEnchantmentsTest {
    private static final String SHARPNESS = "minecraft:sharpness";
    private static final String FEATHER_FALLING = "minecraft:feather_falling";
    private static final String UNBREAKING = "minecraft:unbreaking";

    @Test
    void findsEveryBookEnchantmentThatImprovedTheOutput() {
        Set<String> transferred = RetoldTransferredEnchantments.find(
                Map.of(SHARPNESS, 1),
                Map.of(SHARPNESS, 1, UNBREAKING, 2),
                Map.of(SHARPNESS, 2, UNBREAKING, 2)
        );

        assertEquals(Set.of(SHARPNESS, UNBREAKING), transferred);
    }

    @Test
    void excludesIncompatibleUnchangedAndUnrelatedOutputEnchantments() {
        Set<String> transferred = RetoldTransferredEnchantments.find(
                Map.of(SHARPNESS, 2),
                Map.of(SHARPNESS, 1, FEATHER_FALLING, 4),
                Map.of(SHARPNESS, 2, UNBREAKING, 1)
        );

        assertEquals(Set.of(), transferred);
    }
}
