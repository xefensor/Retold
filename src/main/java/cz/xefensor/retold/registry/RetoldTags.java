package cz.xefensor.retold.registry;

import cz.xefensor.retold.Retold;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class RetoldTags {
    public static final TagKey<Item> TORCH_IGNITERS = TagKey.create(
            Registries.ITEM,
            Identifier.fromNamespaceAndPath(Retold.MODID, "torch_igniters")
    );

    private RetoldTags() {
    }
}