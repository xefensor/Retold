package cz.xefensor.retold.registry;

import cz.xefensor.retold.Retold;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

/** Client equipment definitions use resource keys rather than a separate content registry. */
public final class RetoldEquipmentAssets {
    public static final ResourceKey<EquipmentAsset> STEEL = ResourceKey.create(
            EquipmentAssets.ROOT_ID,
            Identifier.fromNamespaceAndPath(Retold.MODID, "steel")
    );

    private RetoldEquipmentAssets() {
    }
}
