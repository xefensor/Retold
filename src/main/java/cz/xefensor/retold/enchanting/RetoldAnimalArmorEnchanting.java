package cz.xefensor.retold.enchanting;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantable;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.List;

/** Owns vanilla animal-armor enchantability and Wolf Armor protection bridging. */
public final class RetoldAnimalArmorEnchanting {
    private static final List<AnimalArmorDefinition> ANIMAL_ARMOR = List.of(
            armor(Items.WOLF_ARMOR, ArmorMaterials.ARMADILLO_SCUTE),
            armor(Items.LEATHER_HORSE_ARMOR, ArmorMaterials.LEATHER),
            armor(Items.COPPER_HORSE_ARMOR, ArmorMaterials.COPPER),
            armor(Items.IRON_HORSE_ARMOR, ArmorMaterials.IRON),
            armor(Items.GOLDEN_HORSE_ARMOR, ArmorMaterials.GOLD),
            armor(Items.DIAMOND_HORSE_ARMOR, ArmorMaterials.DIAMOND),
            armor(Items.NETHERITE_HORSE_ARMOR, ArmorMaterials.NETHERITE),
            armor(Items.COPPER_NAUTILUS_ARMOR, ArmorMaterials.COPPER),
            armor(Items.IRON_NAUTILUS_ARMOR, ArmorMaterials.IRON),
            armor(Items.GOLDEN_NAUTILUS_ARMOR, ArmorMaterials.GOLD),
            armor(Items.DIAMOND_NAUTILUS_ARMOR, ArmorMaterials.DIAMOND),
            armor(Items.NETHERITE_NAUTILUS_ARMOR, ArmorMaterials.NETHERITE)
    );

    private RetoldAnimalArmorEnchanting() {
    }

    public static void modifyDefaultComponents(
            ModifyDefaultComponentsEvent event
    ) {
        for (AnimalArmorDefinition definition : ANIMAL_ARMOR) {
            event.modify(
                    definition.item(),
                    (components, context, item) -> components.set(
                            DataComponents.ENCHANTABLE,
                            new Enchantable(definition.enchantability())
                    )
            );
        }
    }

    @SubscribeEvent
    public static void applyEnchantmentsToAbsorbedWolfArmorDamage(
            LivingIncomingDamageEvent event
    ) {
        if (!(event.getEntity() instanceof Wolf wolf)
                || !(wolf.level() instanceof ServerLevel level)
                || !wolf.getItemBySlot(EquipmentSlot.BODY).is(Items.WOLF_ARMOR)
                || event.getSource().is(DamageTypeTags.BYPASSES_WOLF_ARMOR)
                || event.getSource().is(DamageTypeTags.BYPASSES_ENCHANTMENTS)) {
            return;
        }

        float protection = EnchantmentHelper.getDamageProtection(
                level,
                wolf,
                event.getSource()
        );
        if (protection > 0.0F) {
            event.setAmount(CombatRules.getDamageAfterMagicAbsorb(
                    event.getAmount(),
                    protection
            ));
        }
    }

    private static AnimalArmorDefinition armor(
            Item item,
            ArmorMaterial material
    ) {
        return new AnimalArmorDefinition(item, material.enchantmentValue());
    }

    private record AnimalArmorDefinition(Item item, int enchantability) {
    }
}
