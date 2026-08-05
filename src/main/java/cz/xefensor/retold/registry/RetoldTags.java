package cz.xefensor.retold.registry;

import cz.xefensor.retold.Retold;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class RetoldTags {
    public static final TagKey<Item> TORCH_IGNITERS = TagKey.create(
            Registries.ITEM,
            Identifier.fromNamespaceAndPath(Retold.MODID, "torch_igniters")
    );
    public static final TagKey<Block> WEAK_MOB_BARRIERS = TagKey.create(
            Registries.BLOCK,
            Identifier.fromNamespaceAndPath(Retold.MODID, "weak_mob_barriers")
    );
    public static final TagKey<EntityType<?>> AUTOMATIC_BREEDERS =
            TagKey.create(
                    Registries.ENTITY_TYPE,
                    Identifier.fromNamespaceAndPath(
                            Retold.MODID,
                            "automatic_breeders"
                    )
            );

    private RetoldTags() {
    }
}
