package cz.xefensor.retold.progression;

import cz.xefensor.retold.registry.RetoldTags;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

public final class RetoldDiamondDurability {
    public static final int UNENCHANTED_TOOL_DURABILITY = 64;
    private static final int ARMOR_DURABILITY_NUMERATOR = 6;
    private static final int VANILLA_DIAMOND_ARMOR_DURABILITY = 33;

    private RetoldDiamondDurability() {
    }

    public static int effectiveMaxDamage(
            ItemStack stack,
            int baseMaxDamage
    ) {
        if (baseMaxDamage <= 0 || stack.isEnchanted()) {
            return baseMaxDamage;
        }

        int fragileMaxDamage;
        if (stack.is(RetoldTags.FRAGILE_UNENCHANTED_DIAMOND_TOOLS)) {
            fragileMaxDamage = UNENCHANTED_TOOL_DURABILITY;
        } else if (stack.is(
                RetoldTags.FRAGILE_UNENCHANTED_DIAMOND_ARMOR
        )) {
            fragileMaxDamage = Math.max(
                    1,
                    baseMaxDamage * ARMOR_DURABILITY_NUMERATOR
                            / VANILLA_DIAMOND_ARMOR_DURABILITY
            );
        } else {
            return baseMaxDamage;
        }

        int minimumValidMaxDamage = Math.min(
                baseMaxDamage,
                stack.getOrDefault(DataComponents.DAMAGE, 0) + 1
        );
        return Math.min(
                baseMaxDamage,
                Math.max(fragileMaxDamage, minimumValidMaxDamage)
        );
    }
}
