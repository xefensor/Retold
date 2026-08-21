package cz.xefensor.retold.progression;

import cz.xefensor.retold.registry.RetoldBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.BuiltinTestFunctions;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public final class RetoldProgressionAcquisitionGameTests {
    private static final Identifier EMPTY_STRUCTURE =
            Identifier.withDefaultNamespace("empty");
    private static final Set<Item> COPPER_EQUIPMENT = Set.of(
            Items.COPPER_AXE, Items.COPPER_HOE, Items.COPPER_PICKAXE,
            Items.COPPER_SHOVEL, Items.COPPER_SWORD, Items.COPPER_SPEAR,
            Items.COPPER_HELMET, Items.COPPER_CHESTPLATE,
            Items.COPPER_LEGGINGS, Items.COPPER_BOOTS
    );
    private static final Set<Item> IRON_EQUIPMENT = Set.of(
            Items.IRON_AXE, Items.IRON_HOE, Items.IRON_PICKAXE,
            Items.IRON_SHOVEL, Items.IRON_SWORD, Items.IRON_SPEAR,
            Items.IRON_HELMET, Items.IRON_CHESTPLATE,
            Items.IRON_LEGGINGS, Items.IRON_BOOTS
    );
    private static final Set<Item> DIAMOND_EQUIPMENT = Set.of(
            Items.DIAMOND_AXE, Items.DIAMOND_HOE, Items.DIAMOND_PICKAXE,
            Items.DIAMOND_SHOVEL, Items.DIAMOND_SWORD, Items.DIAMOND_SPEAR,
            Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE,
            Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS
    );
    private static final Set<Item> VILLAGE_CHEST_FORBIDDEN_ITEMS = Set.of(
            Items.DIAMOND, Items.DIAMOND_HORSE_ARMOR, Items.OBSIDIAN,
            Items.IRON_AXE, Items.IRON_HOE, Items.IRON_PICKAXE,
            Items.IRON_SHOVEL, Items.IRON_SWORD, Items.IRON_SPEAR,
            Items.IRON_HELMET, Items.IRON_CHESTPLATE,
            Items.IRON_LEGGINGS, Items.IRON_BOOTS, Items.IRON_HORSE_ARMOR
    );

    private RetoldProgressionAcquisitionGameTests() {
    }

    public static void register(
            RegisterGameTestsEvent event,
            Holder<TestEnvironmentDefinition<?>> environment
    ) {
        TestData<Holder<TestEnvironmentDefinition<?>>> testData =
                new TestData<>(environment, EMPTY_STRUCTURE, 40, 0, true);
        event.registerTest(
                retoldId("tool_progression_alternative_acquisition_respects_tiers"),
                new InlineGameTest(
                        testData,
                        RetoldProgressionAcquisitionGameTests
                                ::alternativeAcquisitionRespectsTiers
                )
        );
        event.registerTest(
                retoldId("buried_treasure_excludes_heart_of_the_sea"),
                new InlineGameTest(
                        testData,
                        RetoldProgressionAcquisitionGameTests
                                ::buriedTreasureExcludesHeartOfTheSea
                )
        );
    }

    private static void buriedTreasureExcludesHeartOfTheSea(
            GameTestHelper helper
    ) {
        for (long seed = 1L; seed <= 8L; seed++) {
            List<ItemStack> loot = chestLoot(
                    helper,
                    "chests/buried_treasure",
                    seed
            );
            helper.assertFalse(
                    loot.isEmpty(),
                    "Buried treasure must retain its non-ritual rewards"
            );
            helper.assertFalse(
                    loot.stream().anyMatch(stack -> stack.is(
                            Items.HEART_OF_THE_SEA
                    )),
                    "Buried treasure must not bypass the Elder Guardian path"
            );
        }
        helper.succeed();
    }

    private static void alternativeAcquisitionRespectsTiers(
            GameTestHelper helper
    ) {
        assertBonusChestStartsAtFlint(helper);
        assertVillageSmithChestsStopAtCopper(helper);

        for (String profession : List.of(
                "toolsmith",
                "weaponsmith",
                "armorer"
        )) {
            assertEquipmentTradeTier(helper, profession, 1, Set.of(), 0, 0);
            assertEquipmentTradeTier(
                    helper, profession, 2, COPPER_EQUIPMENT, 8, 12
            );
            assertEquipmentTradeTier(helper, profession, 3, Set.of(), 0, 0);
            assertEquipmentTradeTier(
                    helper, profession, 4, IRON_EQUIPMENT, 24, 32
            );
            assertEquipmentTradeTier(
                    helper, profession, 5, DIAMOND_EQUIPMENT, 48, 64
            );
            assertMasterEnchantingMix(helper, profession);
        }

        assertWanderingTraderIronPrice(helper);

        var enchantments = helper.getLevel()
                .registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT);
        Holder<net.minecraft.world.item.enchantment.Enchantment> mending =
                enchantments.getOrThrow(Enchantments.MENDING);
        helper.assertFalse(
                mending.is(EnchantmentTags.TRADEABLE),
                "Mending must not be generated by Librarian trades"
        );
        helper.assertFalse(
                mending.is(EnchantmentTags.ON_RANDOM_LOOT),
                "Mending must not be generated in new random loot"
        );
        helper.assertTrue(
                mending.is(EnchantmentTags.TREASURE),
                "Mending must remain registered for commands and existing items"
        );
        helper.succeed();
    }

    private static void assertBonusChestStartsAtFlint(GameTestHelper helper) {
        for (long seed = 1L; seed <= 32L; seed++) {
            List<ItemStack> loot = chestLoot(
                    helper,
                    "chests/spawn_bonus_chest",
                    seed
            );
            helper.assertValueEqual(
                    loot.stream()
                            .filter(stack -> stack.is(
                                    RetoldBlocks.FLINT_MULTI_TOOL.get()
                            ))
                            .count(),
                    1L,
                    "Every bonus chest must supply one Flint Multi-tool"
            );
            helper.assertFalse(
                    loot.stream().anyMatch(stack -> stack.is(Items.WOODEN_AXE)
                            || stack.is(Items.WOODEN_PICKAXE)
                            || stack.is(Items.STONE_AXE)
                            || stack.is(Items.STONE_PICKAXE)),
                    "Bonus chests must not bypass the Flint starting tier"
            );
        }
    }

    private static void assertVillageSmithChestsStopAtCopper(
            GameTestHelper helper
    ) {
        for (String table : List.of(
                "chests/village/village_toolsmith",
                "chests/village/village_weaponsmith",
                "chests/village/village_armorer"
        )) {
            for (long seed = 1L; seed <= 128L; seed++) {
                List<ItemStack> loot = chestLoot(helper, table, seed);
                int ironIngots = loot.stream()
                        .filter(stack -> stack.is(Items.IRON_INGOT))
                        .mapToInt(ItemStack::getCount)
                        .sum();
                helper.assertTrue(
                        ironIngots <= 2,
                        "Village smith chests may contain at most two Iron Ingots"
                );
                helper.assertFalse(
                        loot.stream().anyMatch(stack ->
                                VILLAGE_CHEST_FORBIDDEN_ITEMS.contains(
                                        stack.getItem()
                                )),
                        "Safe Village smith chests must stop at Copper equipment"
                );
            }
        }
    }

    private static List<ItemStack> chestLoot(
            GameTestHelper helper,
            String path,
            long seed
    ) {
        var level = helper.getLevel();
        LootParams params = new LootParams.Builder(level)
                .withParameter(
                        LootContextParams.ORIGIN,
                        Vec3.atCenterOf(helper.absolutePos(BlockPos.ZERO))
                )
                .create(LootContextParamSets.CHEST);
        ResourceKey<LootTable> key = ResourceKey.create(
                Registries.LOOT_TABLE,
                Identifier.withDefaultNamespace(path)
        );
        return level.getServer()
                .reloadableRegistries()
                .getLootTable(key)
                .getRandomItems(params, seed);
    }

    private static void assertEquipmentTradeTier(
            GameTestHelper helper,
            String profession,
            int level,
            Set<Item> allowedEquipment,
            int minimumPrice,
            int maximumPrice
    ) {
        var trades = helper.getLevel()
                .registryAccess()
                .lookupOrThrow(Registries.VILLAGER_TRADE);
        TagKey<VillagerTrade> tag = TagKey.create(
                Registries.VILLAGER_TRADE,
                Identifier.withDefaultNamespace(
                        profession + "/level_" + level
                )
        );
        var entries = trades.get(tag).orElseThrow(() ->
                helper.assertionException("Missing Villager trade tag " + tag)
        );
        LootContext context = villagerTradeContext(helper, 42L);
        boolean foundEquipment = false;

        for (Holder<VillagerTrade> entry : entries) {
            MerchantOffer offer = entry.value().getOffer(context);
            helper.assertTrue(
                    offer != null && !offer.getResult().isEmpty(),
                    "Every configured Villager trade must produce an item"
            );
            Item result = offer.getResult().getItem();
            if (!isProgressionEquipment(result)) {
                continue;
            }

            foundEquipment = true;
            helper.assertTrue(
                    allowedEquipment.contains(result),
                    profession + " level " + level
                            + " must offer only its assigned equipment tier"
            );
            helper.assertTrue(
                    offer.getBaseCostA().is(Items.EMERALD)
                            && offer.getBaseCostA().getCount() >= minimumPrice
                            && offer.getBaseCostA().getCount() <= maximumPrice,
                    profession + " level " + level
                            + " equipment price is outside the approved range"
            );
        }

        helper.assertValueEqual(
                foundEquipment,
                !allowedEquipment.isEmpty(),
                profession + " level " + level
                        + " equipment availability must match progression"
        );
    }

    private static boolean isProgressionEquipment(Item item) {
        return COPPER_EQUIPMENT.contains(item)
                || IRON_EQUIPMENT.contains(item)
                || DIAMOND_EQUIPMENT.contains(item);
    }

    private static LootContext villagerTradeContext(
            GameTestHelper helper,
            long seed
    ) {
        var villager = helper.spawn(EntityTypes.VILLAGER, 1, 2, 1);
        LootParams params = new LootParams.Builder(helper.getLevel())
                .withParameter(LootContextParams.ORIGIN, villager.position())
                .withParameter(LootContextParams.THIS_ENTITY, villager)
                .withParameter(
                        LootContextParams.ADDITIONAL_COST_COMPONENT_ALLOWED,
                        Unit.INSTANCE
                )
                .create(LootContextParamSets.VILLAGER_TRADE);
        return new LootContext.Builder(params)
                .withOptionalRandomSeed(seed)
                .create(Optional.empty());
    }

    private static void assertMasterEnchantingMix(
            GameTestHelper helper,
            String profession
    ) {
        var trades = helper.getLevel()
                .registryAccess()
                .lookupOrThrow(Registries.VILLAGER_TRADE);
        TagKey<VillagerTrade> tag = TagKey.create(
                Registries.VILLAGER_TRADE,
                Identifier.withDefaultNamespace(profession + "/level_5")
        );
        var entries = trades.get(tag).orElseThrow();
        long enchanted = entries.stream()
                .flatMap(holder -> holder.unwrapKey().stream())
                .map(ResourceKey::identifier)
                .filter(id -> id.getNamespace().equals("retold")
                        && id.getPath().contains("enchanted_diamond"))
                .count();
        long unenchanted = entries.stream()
                .flatMap(holder -> holder.unwrapKey().stream())
                .map(ResourceKey::identifier)
                .filter(id -> id.getNamespace().equals("retold")
                        && id.getPath().startsWith("progression/diamond_"))
                .count();
        helper.assertTrue(
                enchanted > 0 && enchanted < unenchanted,
                "Master " + profession
                        + " pools must make enchanted Diamond rarer"
        );
    }

    private static void assertWanderingTraderIronPrice(
            GameTestHelper helper
    ) {
        var trades = helper.getLevel()
                .registryAccess()
                .lookupOrThrow(Registries.VILLAGER_TRADE);
        ResourceKey<VillagerTrade> key = ResourceKey.create(
                Registries.VILLAGER_TRADE,
                Identifier.withDefaultNamespace(
                        "wandering_trader/emerald_enchanted_iron_pickaxe"
                )
        );
        int totalPrice = 0;
        for (long seed = 1L; seed <= 16L; seed++) {
            MerchantOffer offer = trades.getOrThrow(key)
                    .value()
                    .getOffer(villagerTradeContext(helper, seed));
            helper.assertTrue(
                    offer != null
                            && offer.getResult().is(Items.IRON_PICKAXE)
                            && offer.getResult().isEnchanted()
                            && offer.getBaseCostA().is(Items.EMERALD)
                            && offer.getBaseCostA().getCount() >= 40
                            && offer.getBaseCostA().getCount() <= 56,
                    "Wandering Trader Iron Pickaxe must cost about 48 Emeralds"
            );
            totalPrice += offer.getBaseCostA().getCount();
        }
        helper.assertTrue(
                totalPrice / 16 >= 44 && totalPrice / 16 <= 52,
                "Wandering Trader Iron Pickaxe must average about 48 Emeralds"
        );
    }

    private static Identifier retoldId(String path) {
        return Identifier.fromNamespaceAndPath("retold", path);
    }

    private static final class InlineGameTest extends FunctionGameTestInstance {
        private final Consumer<GameTestHelper> test;

        private InlineGameTest(
                TestData<Holder<TestEnvironmentDefinition<?>>> testData,
                Consumer<GameTestHelper> test
        ) {
            super(BuiltinTestFunctions.ALWAYS_PASS, testData);
            this.test = test;
        }

        @Override
        public void run(GameTestHelper helper) {
            test.accept(helper);
        }
    }
}
