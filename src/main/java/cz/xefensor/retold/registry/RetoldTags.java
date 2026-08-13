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
    public static final TagKey<Item> CAMPFIRE_CONSUMABLE_IGNITERS = itemTag(
            "campfire_consumable_igniters"
    );
    public static final TagKey<Item> LEAF_PRESERVING_TOOLS = itemTag(
            "leaf_preserving_tools"
    );
    public static final TagKey<Item> MEAT_FOODS = itemTag("meat_foods");
    public static final TagKey<Item> FISH_FOODS = itemTag("fish_foods");
    public static final TagKey<Item> BERRY_FOODS = itemTag("berry_foods");
    public static final TagKey<Item> GRAZER_FOODS = itemTag("grazer_foods");
    public static final TagKey<Item> SMALL_PASSIVE_FOODS = itemTag(
            "small_passive_foods"
    );
    public static final TagKey<Item> FLOWER_FOODS = itemTag("flower_foods");
    public static final TagKey<Item> NETHER_FUNGUS_FOODS = itemTag(
            "nether_fungus_foods"
    );
    public static final TagKey<Item> BAT_FOODS = itemTag("bat_foods");
    public static final TagKey<Item> FELINE_SCAVENGE_FOODS = itemTag(
            "feline_scavenge_foods"
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
    public static final TagKey<Block> ARMADILLO_GRUB_SOILS = blockTag(
            "armadillo_grub_soils"
    );
    public static final TagKey<Block> ARMADILLO_SCRUB_RANGE_BLOCKS = blockTag(
            "armadillo_scrub_range_blocks"
    );
    public static final TagKey<Block> PANDA_BAMBOO_BLOCKS = blockTag(
            "panda_bamboo_blocks"
    );
    public static final TagKey<Block> TURTLE_BEACH_BLOCKS = blockTag(
            "turtle_beach_blocks"
    );
    public static final TagKey<Block> DESERT_BROWSE_BLOCKS = blockTag(
            "desert_browse_blocks"
    );
    public static final TagKey<Block> GOAT_SCRAPE_BLOCKS = blockTag(
            "goat_scrape_blocks"
    );
    public static final TagKey<Block> MOOSHROOM_GRAZING_BLOCKS = blockTag(
            "mooshroom_grazing_blocks"
    );
    public static final TagKey<Block> FORAGE_CROPS = blockTag(
            "forage_crops"
    );
    public static final TagKey<Block> FORAGE_FLOWERS = blockTag(
            "forage_flowers"
    );
    public static final TagKey<Block> GRAZER_FORAGE_PLANTS = blockTag(
            "grazer_forage_plants"
    );
    public static final TagKey<Block> SMALL_PASSIVE_FORAGE_PLANTS = blockTag(
            "small_passive_forage_plants"
    );
    public static final TagKey<Block> TURTLE_FORAGE_BLOCKS = blockTag(
            "turtle_forage_blocks"
    );
    public static final TagKey<Block> HOGLIN_FORAGE_BLOCKS = blockTag(
            "hoglin_forage_blocks"
    );
    public static final TagKey<Block> PIGLIN_FORAGE_BLOCKS = blockTag(
            "piglin_forage_blocks"
    );
    public static final TagKey<Block> STRIDER_FORAGE_BLOCKS = blockTag(
            "strider_forage_blocks"
    );
    public static final TagKey<Block> SPIDER_LAIR_WEB_BLOCKS = blockTag(
            "spider_lair_web_blocks"
    );
    public static final TagKey<Block> ILLAGER_VILLAGE_SIGNAL_BLOCKS = blockTag(
            "illager_village_signal_blocks"
    );
    public static final TagKey<Block> NETHER_REMNANT_GUARD_ANCHOR_BLOCKS = blockTag(
            "nether_remnant_guard_anchor_blocks"
    );
    public static final TagKey<Block> OCEAN_MONUMENT_GUARD_ANCHOR_BLOCKS = blockTag(
            "ocean_monument_guard_anchor_blocks"
    );
    public static final TagKey<Block> OCEAN_MONUMENT_PROTECTED_BLOCKS = blockTag(
            "ocean_monument_protected_blocks"
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

    private static TagKey<Block> blockTag(String path) {
        return TagKey.create(
                Registries.BLOCK,
                Identifier.fromNamespaceAndPath(Retold.MODID, path)
        );
    }

    private static TagKey<Item> itemTag(String path) {
        return TagKey.create(
                Registries.ITEM,
                Identifier.fromNamespaceAndPath(Retold.MODID, path)
        );
    }
}
