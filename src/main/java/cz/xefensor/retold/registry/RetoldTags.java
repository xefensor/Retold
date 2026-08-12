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
    public static final TagKey<Item> ANIMAL_ARMOR = TagKey.create(
            Registries.ITEM,
            Identifier.fromNamespaceAndPath(Retold.MODID, "animal_armor")
    );
    public static final TagKey<Item> FLINT_MULTI_TOOL_REPAIR_MATERIALS =
            TagKey.create(
                    Registries.ITEM,
                    Identifier.fromNamespaceAndPath(
                            Retold.MODID,
                            "flint_multi_tool_repair_materials"
                    )
            );
    public static final TagKey<Item> STEEL_REPAIR_MATERIALS =
            TagKey.create(
                    Registries.ITEM,
                    Identifier.fromNamespaceAndPath(
                            Retold.MODID,
                            "steel_repair_materials"
                    )
            );
    public static final TagKey<Item> FRAGILE_UNENCHANTED_DIAMOND_TOOLS =
            TagKey.create(
                    Registries.ITEM,
                    Identifier.fromNamespaceAndPath(
                            Retold.MODID,
                            "fragile_unenchanted_diamond_tools"
                    )
            );
    public static final TagKey<Item> FRAGILE_UNENCHANTED_DIAMOND_ARMOR =
            TagKey.create(
                    Registries.ITEM,
                    Identifier.fromNamespaceAndPath(
                            Retold.MODID,
                            "fragile_unenchanted_diamond_armor"
                    )
            );
    public static final TagKey<Block> FLINT_MULTI_TOOL_MINEABLE =
            TagKey.create(
                    Registries.BLOCK,
                    Identifier.fromNamespaceAndPath(
                            Retold.MODID,
                            "mineable/flint_multi_tool"
                    )
            );
    public static final TagKey<Block> INCORRECT_FOR_FLINT_MULTI_TOOL =
            TagKey.create(
                    Registries.BLOCK,
                    Identifier.fromNamespaceAndPath(
                            Retold.MODID,
                            "incorrect_for_flint_multi_tool"
                    )
            );
    public static final TagKey<Block> INCORRECT_FOR_STEEL_TOOL =
            TagKey.create(
                    Registries.BLOCK,
                    Identifier.fromNamespaceAndPath(
                            Retold.MODID,
                            "incorrect_for_steel_tool"
                    )
            );
    public static final TagKey<Block> STEEL_TIER_BLOCKS =
            TagKey.create(
                    Registries.BLOCK,
                    Identifier.fromNamespaceAndPath(
                            Retold.MODID,
                            "steel_tier_blocks"
                    )
            );
    public static final TagKey<Block> STICK_DROPPING_LIVING_BUSHES =
            TagKey.create(
                    Registries.BLOCK,
                    Identifier.fromNamespaceAndPath(
                            Retold.MODID,
                            "stick_dropping_living_bushes"
                    )
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
