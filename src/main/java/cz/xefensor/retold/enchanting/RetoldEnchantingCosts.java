package cz.xefensor.retold.enchanting;

/** Confirmed deterministic resource costs for a three-glyph enchanting-table cast. */
public final class RetoldEnchantingCosts {
    public static final int LAPIS_PER_CAST = 3;
    private static final int EXPERIENCE_LEVELS_PER_ENCHANTMENT_LEVEL = 5;

    private RetoldEnchantingCosts() {
    }

    public static int experienceLevelCost(int enchantmentLevel) {
        if (enchantmentLevel <= 0) {
            throw new IllegalArgumentException("Enchantment level must be positive");
        }
        return Math.multiplyExact(
                EXPERIENCE_LEVELS_PER_ENCHANTMENT_LEVEL,
                enchantmentLevel
        );
    }
}
