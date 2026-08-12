package cz.xefensor.retold.progression;

import cz.xefensor.retold.registry.RetoldTags;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;

public final class RetoldDiamondDurability {
    public static final int UNENCHANTED_TOOL_DURABILITY = 64;
    public static final int DIAMOND_BODY_ARMOR_DURABILITY = 528;
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
            fragileMaxDamage = fragileArmorDurability(baseMaxDamage);
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

    public static int fragileArmorDurability(int fullDurability) {
        return Math.max(
                1,
                fullDurability * ARMOR_DURABILITY_NUMERATOR
                        / VANILLA_DIAMOND_ARMOR_DURABILITY
        );
    }

    public static void hurtAnimalBodyArmor(Mob mob, float incomingDamage) {
        if (incomingDamage <= 0.0F) {
            return;
        }

        ItemStack bodyArmor = mob.getItemBySlot(EquipmentSlot.BODY);
        if (!bodyArmor.is(RetoldTags.FRAGILE_UNENCHANTED_DIAMOND_ARMOR)) {
            return;
        }

        int durabilityDamage = Math.max(1, (int) (incomingDamage / 4.0F));
        bodyArmor.hurtAndBreak(durabilityDamage, mob, EquipmentSlot.BODY);
    }
}
