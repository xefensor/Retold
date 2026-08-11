package cz.xefensor.retold.progression;

import cz.xefensor.retold.registry.RetoldBlocks;
import cz.xefensor.retold.registry.RetoldTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.BuiltinTestFunctions;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmokingRecipe;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class RetoldToolProgressionGameTests {
    private static final Identifier EMPTY_STRUCTURE =
            Identifier.withDefaultNamespace("empty");
    private static final ResourceKey<PlacedFeature> COPPER_ORE_PLACEMENT =
            ResourceKey.create(
                    Registries.PLACED_FEATURE,
                    Identifier.withDefaultNamespace("ore_copper")
            );
    private static final ResourceKey<PlacedFeature> LARGE_COPPER_ORE_PLACEMENT =
            ResourceKey.create(
                    Registries.PLACED_FEATURE,
                    Identifier.withDefaultNamespace("ore_copper_large")
            );
    private static final List<String> REMOVED_TOOL_RECIPE_IDS = List.of(
            "wooden_axe",
            "wooden_hoe",
            "wooden_pickaxe",
            "wooden_shovel",
            "wooden_spear",
            "wooden_sword",
            "stone_axe",
            "stone_hoe",
            "stone_pickaxe",
            "stone_shovel",
            "stone_spear",
            "stone_sword"
    );
    private static final List<String> STEEL_EQUIPMENT_RECIPE_IDS = List.of(
            "steel_sword",
            "steel_spear",
            "steel_shovel",
            "steel_pickaxe",
            "steel_axe",
            "steel_hoe",
            "steel_helmet",
            "steel_chestplate",
            "steel_leggings",
            "steel_boots"
    );
    private static final int LEAF_LOOT_SAMPLE_COUNT = 512;
    private static final int BUSH_LOOT_SAMPLE_COUNT = 128;

    private RetoldToolProgressionGameTests() {
    }

    public static void register(
            RegisterGameTestsEvent event,
            Holder<TestEnvironmentDefinition<?>> environment
    ) {
        registerTest(
                event,
                environment,
                "tool_progression_harvest_rules_enforce_material_ladder",
                RetoldToolProgressionGameTests::harvestRulesEnforceMaterialLadder
        );
        registerTest(
                event,
                environment,
                "tool_progression_recipes_enforce_opening_loop",
                RetoldToolProgressionGameTests::recipesEnforceOpeningLoop
        );
        registerTest(
                event,
                environment,
                "tool_progression_leaves_supply_sticks",
                RetoldToolProgressionGameTests::leavesSupplySticks
        );
        registerTest(
                event,
                environment,
                "tool_progression_campfires_require_ignition",
                RetoldToolProgressionGameTests::campfiresRequireIgnition
        );
        registerTest(
                event,
                environment,
                "tool_progression_copper_generation_is_reduced",
                RetoldToolProgressionGameTests::copperGenerationIsReduced
        );
        registerTest(
                event,
                environment,
                "tool_progression_all_diamond_items_require_enchanting_for_durability",
                RetoldToolProgressionGameTests
                        ::allDiamondItemsRequireEnchantingForDurability
        );
    }

    private static void allDiamondItemsRequireEnchantingForDurability(
            GameTestHelper helper
    ) {
        Set<Item> expectedDiamondEquipment = Set.of(
                Items.DIAMOND_SWORD,
                Items.DIAMOND_SHOVEL,
                Items.DIAMOND_PICKAXE,
                Items.DIAMOND_AXE,
                Items.DIAMOND_HOE,
                Items.DIAMOND_SPEAR,
                Items.DIAMOND_HELMET,
                Items.DIAMOND_CHESTPLATE,
                Items.DIAMOND_LEGGINGS,
                Items.DIAMOND_BOOTS,
                Items.DIAMOND_HORSE_ARMOR,
                Items.DIAMOND_NAUTILUS_ARMOR
        );
        Set<Item> enchantableDiamondItems = BuiltInRegistries.ITEM.stream()
                .filter(item -> {
                    Identifier id = BuiltInRegistries.ITEM.getKey(item);
                    return id.getNamespace().equals("minecraft")
                            && id.getPath().startsWith("diamond_")
                            && item.getDefaultInstance().isEnchantable();
                })
                .collect(Collectors.toUnmodifiableSet());
        helper.assertValueEqual(
                enchantableDiamondItems,
                expectedDiamondEquipment,
                "The durability policy must enumerate every enchantable Diamond item"
        );

        Holder<net.minecraft.world.item.enchantment.Enchantment> unbreaking =
                helper.getLevel()
                        .registryAccess()
                        .lookupOrThrow(Registries.ENCHANTMENT)
                        .getOrThrow(Enchantments.UNBREAKING);
        for (Item item : expectedDiamondEquipment) {
            ItemStack stack = item.getDefaultInstance();
            int fullDurability = stack.getOrDefault(
                    DataComponents.MAX_DAMAGE,
                    0
            );
            helper.assertTrue(
                    fullDurability > 0,
                    item + " must have a full durability component"
            );

            int fragileDurability = stack.is(
                    RetoldTags.FRAGILE_UNENCHANTED_DIAMOND_TOOLS
            )
                    ? RetoldDiamondDurability.UNENCHANTED_TOOL_DURABILITY
                    : RetoldDiamondDurability.fragileArmorDurability(
                            fullDurability
                    );
            helper.assertValueEqual(
                    stack.getMaxDamage(),
                    fragileDurability,
                    item + " must begin with fragile unenchanted durability"
            );

            stack.enchant(unbreaking, 1);
            helper.assertValueEqual(
                    stack.getMaxDamage(),
                    fullDurability,
                    item + " must regain full durability when enchanted"
            );
            stack.set(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
            helper.assertValueEqual(
                    stack.getMaxDamage(),
                    fragileDurability,
                    item + " must become fragile after removing enchantments"
            );
        }

        var attacker = helper.spawn(EntityTypes.ZOMBIE, 1, 2, 1);
        attacker.setNoAi(true);
        var horse = helper.spawn(EntityTypes.HORSE, 2, 2, 1);
        var nautilus = helper.spawn(EntityTypes.NAUTILUS, 3, 2, 1);
        horse.setItemSlot(
                EquipmentSlot.BODY,
                new ItemStack(Items.DIAMOND_HORSE_ARMOR)
        );
        nautilus.setItemSlot(
                EquipmentSlot.BODY,
                new ItemStack(Items.DIAMOND_NAUTILUS_ARMOR)
        );

        var damageSource = helper.getLevel().damageSources().mobAttack(attacker);
        horse.hurtServer(helper.getLevel(), damageSource, 8.0F);
        nautilus.hurtServer(helper.getLevel(), damageSource, 8.0F);
        helper.assertTrue(
                horse.getItemBySlot(EquipmentSlot.BODY).getDamageValue() > 0,
                "Diamond Horse Armor must lose durability from protected hits"
        );
        helper.assertTrue(
                nautilus.getItemBySlot(EquipmentSlot.BODY).getDamageValue() > 0,
                "Diamond Nautilus Armor must lose durability from protected hits"
        );

        helper.succeed();
    }

    private static void copperGenerationIsReduced(GameTestHelper helper) {
        assertCopperPlacement(helper, COPPER_ORE_PLACEMENT, 10);
        assertCopperPlacement(helper, LARGE_COPPER_ORE_PLACEMENT, 20);
        helper.succeed();
    }

    private static void assertCopperPlacement(
            GameTestHelper helper,
            ResourceKey<PlacedFeature> key,
            int expectedVeinSize
    ) {
        var level = helper.getLevel();
        PlacedFeature placedFeature = level.registryAccess()
                .lookupOrThrow(Registries.PLACED_FEATURE)
                .getOrThrow(key)
                .value();
        CountPlacement countPlacement = placedFeature.placement()
                .stream()
                .filter(CountPlacement.class::isInstance)
                .map(CountPlacement.class::cast)
                .findFirst()
                .orElseThrow(() -> helper.assertionException(
                        key.identifier() + " must use count placement"
                ));
        PlacementContext context = new PlacementContext(
                level,
                level.getChunkSource().getGenerator(),
                Optional.of(placedFeature)
        );
        List<BlockPos> sampleOrigins = List.of(
                new BlockPos(0, 48, 0),
                new BlockPos(15, 48, 15),
                new BlockPos(16, 48, 16)
        );

        for (int sample = 0; sample < sampleOrigins.size(); sample++) {
            long placements = countPlacement.getPositions(
                    context,
                    RandomSource.create(1000L + sample),
                    sampleOrigins.get(sample)
            ).count();
            helper.assertTrue(
                    placements == 6,
                    key.identifier()
                            + " must make six attempts across seed and chunk-border samples"
            );
        }

        helper.assertTrue(
                placedFeature.feature().value().config()
                        instanceof OreConfiguration oreConfiguration
                        && oreConfiguration.size == expectedVeinSize,
                key.identifier()
                        + " must preserve rewarding vanilla vein size "
                        + expectedVeinSize
        );
    }

    private static void campfiresRequireIgnition(GameTestHelper helper) {
        BlockPos supportPos = helper.absolutePos(BlockPos.ZERO);
        BlockPos campfirePos = supportPos.above();
        helper.getLevel().setBlockAndUpdate(
                supportPos,
                Blocks.STONE.defaultBlockState()
        );
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(
                GameType.SURVIVAL
        );
        ItemStack campfire = new ItemStack(Items.CAMPFIRE);
        player.setItemInHand(InteractionHand.MAIN_HAND, campfire);
        BlockHitResult placementHit = new BlockHitResult(
                Vec3.atCenterOf(supportPos),
                Direction.UP,
                supportPos,
                false
        );
        BlockState campfirePlacement = Blocks.CAMPFIRE.getStateForPlacement(
                new BlockPlaceContext(
                        player,
                        InteractionHand.MAIN_HAND,
                        campfire,
                        placementHit
                )
        );
        helper.assertTrue(
                campfirePlacement != null,
                "A Campfire must provide an initial placement state"
        );
        helper.assertFalse(
                campfirePlacement.getValue(CampfireBlock.LIT),
                "A Campfire's initial placement state must already be unlit"
        );

        helper.getLevel().setBlockAndUpdate(campfirePos, campfirePlacement);
        ItemStack flint = new ItemStack(Items.FLINT, 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, flint);
        BlockHitResult hit = new BlockHitResult(
                Vec3.atCenterOf(campfirePos),
                Direction.UP,
                campfirePos,
                false
        );
        PlayerInteractEvent.RightClickBlock flintUse =
                new PlayerInteractEvent.RightClickBlock(
                        player,
                        InteractionHand.MAIN_HAND,
                        campfirePos,
                        hit
                );
        RetoldCampfireProgressionEvents.onFlintUsedOnCampfire(flintUse);
        helper.assertTrue(
                flintUse.isCanceled()
                        && flintUse.getCancellationResult()
                        == InteractionResult.SUCCESS,
                "Bare Flint must handle the Campfire ignition interaction"
        );
        helper.assertTrue(
                helper.getLevel()
                        .getBlockState(campfirePos)
                        .getValue(CampfireBlock.LIT),
                "Bare Flint must light an unlit Campfire"
        );
        helper.assertTrue(
                flint.getCount() == 1,
                "Lighting a Campfire with bare Flint must consume one Flint"
        );

        helper.getLevel().setBlockAndUpdate(campfirePos, campfirePlacement);
        ItemStack flintAndSteel = new ItemStack(Items.FLINT_AND_STEEL);
        player.setItemInHand(InteractionHand.MAIN_HAND, flintAndSteel);
        Items.FLINT_AND_STEEL.useOn(new UseOnContext(
                player,
                InteractionHand.MAIN_HAND,
                hit
        ));
        helper.assertTrue(
                helper.getLevel()
                        .getBlockState(campfirePos)
                        .getValue(CampfireBlock.LIT),
                "Flint and Steel must retain vanilla Campfire ignition"
        );
        helper.assertTrue(
                flintAndSteel.getDamageValue() == 1,
                "Flint and Steel must use durability instead of being consumed"
        );

        helper.succeed();
    }

    private static void leavesSupplySticks(GameTestHelper helper) {
        int unenchantedSticks = countBlockSticks(
                helper,
                Blocks.OAK_LEAVES.defaultBlockState(),
                ItemStack.EMPTY,
                LEAF_LOOT_SAMPLE_COUNT
        );
        helper.assertTrue(
                unenchantedSticks >= 100,
                "Retold leaves must provide substantially more Sticks than the vanilla 2% pool"
        );

        ItemStack fortuneTool = new ItemStack(Items.DIAMOND_PICKAXE);
        var enchantments = helper.getLevel()
                .registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT);
        fortuneTool.enchant(
                enchantments.getOrThrow(Enchantments.FORTUNE),
                3
        );
        int fortuneSticks = countBlockSticks(
                helper,
                Blocks.OAK_LEAVES.defaultBlockState(),
                fortuneTool,
                LEAF_LOOT_SAMPLE_COUNT
        );
        helper.assertTrue(
                fortuneSticks > unenchantedSticks,
                "Fortune III must improve Retold's supplemental leaf Stick chance"
        );

        helper.assertTrue(
                countBlockSticks(
                        helper,
                        Blocks.OAK_LEAVES.defaultBlockState(),
                        new ItemStack(Items.SHEARS),
                        LEAF_LOOT_SAMPLE_COUNT
                ) == 0,
                "Shears must keep the leaf-block harvest path without extra Sticks"
        );

        ItemStack silkTouchTool = new ItemStack(Items.DIAMOND_PICKAXE);
        silkTouchTool.enchant(
                enchantments.getOrThrow(Enchantments.SILK_TOUCH),
                1
        );
        helper.assertTrue(
                countBlockSticks(
                        helper,
                        Blocks.OAK_LEAVES.defaultBlockState(),
                        silkTouchTool,
                        LEAF_LOOT_SAMPLE_COUNT
                ) == 0,
                "Silk Touch must keep the leaf-block harvest path without extra Sticks"
        );
        float baseChance = RetoldLeafStickLootModifier.chanceForFortune(0);
        float fortuneThreeChance =
                RetoldLeafStickLootModifier.chanceForFortune(3);
        helper.assertTrue(
                Math.abs(baseChance - 0.20F) < 0.0001F
                        && Math.abs(fortuneThreeChance - 0.35F) < 0.0001F,
                "Leaf Stick chances must remain 20% base and 35% at Fortune III"
        );

        int deadBushSticks = countBlockSticks(
                helper,
                Blocks.DEAD_BUSH.defaultBlockState(),
                ItemStack.EMPTY,
                BUSH_LOOT_SAMPLE_COUNT
        );
        helper.assertTrue(
                deadBushSticks >= BUSH_LOOT_SAMPLE_COUNT * 2
                        && deadBushSticks <= BUSH_LOOT_SAMPLE_COUNT * 4,
                "Dead Bushes must drop two to four Sticks"
        );

        helper.assertTrue(
                Blocks.BUSH.defaultBlockState().is(
                        RetoldTags.STICK_DROPPING_LIVING_BUSHES
                )
                        && Blocks.FIREFLY_BUSH.defaultBlockState().is(
                        RetoldTags.STICK_DROPPING_LIVING_BUSHES
                )
                        && Blocks.ROSE_BUSH.defaultBlockState().is(
                        RetoldTags.STICK_DROPPING_LIVING_BUSHES
                )
                        && Blocks.SWEET_BERRY_BUSH.defaultBlockState().is(
                        RetoldTags.STICK_DROPPING_LIVING_BUSHES
                ),
                "Every woody vanilla bush must use the living-bush Stick policy"
        );
        int livingBushSticks = countBlockSticks(
                helper,
                Blocks.BUSH.defaultBlockState(),
                ItemStack.EMPTY,
                BUSH_LOOT_SAMPLE_COUNT
        );
        helper.assertTrue(
                livingBushSticks >= 4 && livingBushSticks <= 25,
                "Living bushes must occasionally drop one Stick; sampled "
                        + livingBushSticks
        );
        helper.assertValueEqual(
                countBlockSticks(
                        helper,
                        Blocks.BUSH.defaultBlockState(),
                        new ItemStack(Items.SHEARS),
                        BUSH_LOOT_SAMPLE_COUNT
                ),
                0,
                "Shears must preserve living bushes without extra Sticks"
        );

        helper.succeed();
    }

    private static int countBlockSticks(
            GameTestHelper helper,
            BlockState state,
            ItemStack tool,
            int samples
    ) {
        var level = helper.getLevel();
        LootParams params = new LootParams.Builder(level)
                .withParameter(
                        LootContextParams.ORIGIN,
                        Vec3.atCenterOf(helper.absolutePos(BlockPos.ZERO))
                )
                .withParameter(
                        LootContextParams.BLOCK_STATE,
                        state
                )
                .withParameter(LootContextParams.TOOL, tool)
                .create(LootContextParamSets.BLOCK);
        LootTable lootTable = level.getServer()
                .reloadableRegistries()
                .getLootTable(
                        state.getBlock().getLootTable().orElseThrow()
                );
        int sticks = 0;

        for (int sample = 0; sample < samples; sample++) {
            for (ItemStack stack : lootTable.getRandomItems(params)) {
                if (stack.is(Items.STICK)) {
                    sticks += stack.getCount();
                }
            }
        }

        return sticks;
    }

    private static void harvestRulesEnforceMaterialLadder(
            GameTestHelper helper
    ) {
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(
                GameType.SURVIVAL
        );
        var level = helper.getLevel();
        var pos = helper.absolutePos(BlockPos.ZERO);

        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        helper.assertFalse(
                EventHooks.doPlayerHarvestCheck(
                        player,
                        Blocks.OAK_LOG.defaultBlockState(),
                        level,
                        pos
                ),
                "Logs must not drop when broken by hand"
        );

        ItemStack flintMultiTool = new ItemStack(
                RetoldBlocks.FLINT_MULTI_TOOL.get()
        );
        player.setItemInHand(InteractionHand.MAIN_HAND, flintMultiTool);
        helper.assertTrue(
                EventHooks.doPlayerHarvestCheck(
                        player,
                        Blocks.OAK_LOG.defaultBlockState(),
                        level,
                        pos
                ),
                "The Flint Multi-tool must harvest logs"
        );
        helper.assertTrue(
                flintMultiTool.isCorrectToolForDrops(
                        Blocks.COPPER_ORE.defaultBlockState()
                ),
                "The Flint Multi-tool must harvest exposed Copper ore"
        );
        helper.assertTrue(
                flintMultiTool.isCorrectToolForDrops(
                        Blocks.TUFF.defaultBlockState()
                ),
                "The Flint Multi-tool must harvest soft early stone"
        );
        helper.assertFalse(
                flintMultiTool.isCorrectToolForDrops(
                        Blocks.STONE.defaultBlockState()
                ),
                "The Flint Multi-tool must not harvest normal Stone"
        );
        helper.assertTrue(
                flintMultiTool.getMaxDamage() == 48,
                "The Flint Multi-tool must use the approved provisional durability"
        );

        player.setItemInHand(
                InteractionHand.MAIN_HAND,
                new ItemStack(Items.COPPER_PICKAXE)
        );
        helper.assertTrue(
                EventHooks.doPlayerHarvestCheck(
                        player,
                        Blocks.STONE.defaultBlockState(),
                        level,
                        pos
                ),
                "A Copper Pickaxe must harvest Stone and receive Cobblestone"
        );

        PlayerEvent.BreakSpeed copperStoneSpeed =
                new PlayerEvent.BreakSpeed(
                        player,
                        Blocks.STONE.defaultBlockState(),
                        5.0F,
                        pos
                );
        RetoldToolProgressionEvents.onBreakSpeed(copperStoneSpeed);
        helper.assertTrue(
                Math.abs(copperStoneSpeed.getNewSpeed() - 1.25F) < 0.0001F,
                "A Copper Pickaxe must mine Stone at 25% of normal speed"
        );

        player.setItemInHand(
                InteractionHand.MAIN_HAND,
                new ItemStack(Items.IRON_PICKAXE)
        );
        PlayerEvent.BreakSpeed ironStoneSpeed =
                new PlayerEvent.BreakSpeed(
                        player,
                        Blocks.STONE.defaultBlockState(),
                        6.0F,
                        pos
                );
        RetoldToolProgressionEvents.onBreakSpeed(ironStoneSpeed);
        helper.assertTrue(
                ironStoneSpeed.getNewSpeed() == 6.0F,
                "Iron must retain its normal practical Stone-mining speed"
        );

        PlayerEvent.BreakSpeed ironDeepslateSpeed =
                new PlayerEvent.BreakSpeed(
                        player,
                        Blocks.DEEPSLATE.defaultBlockState(),
                        6.0F,
                        pos
                );
        RetoldToolProgressionEvents.onBreakSpeed(ironDeepslateSpeed);
        helper.assertTrue(
                ironDeepslateSpeed.getNewSpeed() == 1.5F,
                "Iron must mine Deepslate at 25% speed before Steel"
        );
        helper.assertFalse(
                flintMultiTool.isCorrectToolForDrops(
                        Blocks.DEEPSLATE.defaultBlockState()
                ),
                "Flint must not harvest Deepslate"
        );
        helper.assertFalse(
                Items.WOODEN_PICKAXE.getDefaultInstance()
                        .isCorrectToolForDrops(
                                Blocks.DEEPSLATE.defaultBlockState()
                        ),
                "Wood must not harvest Deepslate"
        );
        helper.assertFalse(
                Items.STONE_PICKAXE.getDefaultInstance()
                        .isCorrectToolForDrops(
                                Blocks.DEEPSLATE.defaultBlockState()
                        ),
                "Stone must not harvest Deepslate"
        );
        helper.assertFalse(
                Items.COPPER_PICKAXE.getDefaultInstance()
                        .isCorrectToolForDrops(
                                Blocks.DEEPSLATE.defaultBlockState()
                        ),
                "Copper must not harvest Deepslate"
        );
        helper.assertFalse(
                Items.IRON_PICKAXE.getDefaultInstance()
                        .isCorrectToolForDrops(
                                Blocks.DEEPSLATE_IRON_ORE.defaultBlockState()
                        ),
                "Iron must not harvest Deepslate ores"
        );
        helper.assertFalse(
                Items.GOLDEN_PICKAXE.getDefaultInstance()
                        .isCorrectToolForDrops(
                                Blocks.DEEPSLATE.defaultBlockState()
                ),
                "Gold must remain a sidegrade and not bypass Steel"
        );
        helper.assertFalse(
                Items.IRON_PICKAXE.getDefaultInstance()
                        .isCorrectToolForDrops(
                                Blocks.DEEPSLATE_TILE_STAIRS.defaultBlockState()
                        ),
                "Iron must not harvest constructed Deepslate variants"
        );

        ItemStack steelPickaxe = new ItemStack(
                RetoldBlocks.STEEL_PICKAXE.get()
        );
        player.setItemInHand(InteractionHand.MAIN_HAND, steelPickaxe);
        PlayerEvent.BreakSpeed steelDeepslateSpeed =
                new PlayerEvent.BreakSpeed(
                        player,
                        Blocks.DEEPSLATE.defaultBlockState(),
                        7.0F,
                        pos
                );
        RetoldToolProgressionEvents.onBreakSpeed(steelDeepslateSpeed);
        helper.assertTrue(
                steelDeepslateSpeed.getNewSpeed() == 7.0F,
                "Steel must make Deepslate practical"
        );
        helper.assertTrue(
                steelPickaxe.isCorrectToolForDrops(
                        Blocks.DEEPSLATE.defaultBlockState()
                ),
                "Steel must unlock Deepslate harvesting"
        );
        helper.assertTrue(
                steelPickaxe.isCorrectToolForDrops(
                        Blocks.DEEPSLATE_DIAMOND_ORE.defaultBlockState()
                ),
                "Steel must harvest deep Diamond ore"
        );
        helper.assertTrue(
                Items.DIAMOND_PICKAXE.getDefaultInstance()
                        .isCorrectToolForDrops(
                                Blocks.DEEPSLATE.defaultBlockState()
                        ),
                "Diamond must retain Deepslate harvesting after Steel"
        );
        helper.assertFalse(
                steelPickaxe.isCorrectToolForDrops(
                        Blocks.OBSIDIAN.defaultBlockState()
                ),
                "Steel must not replace Diamond for Obsidian access"
        );
        helper.assertTrue(
                steelPickaxe.getMaxDamage() == 750,
                "Steel tools must use the approved provisional durability"
        );
        helper.assertTrue(
                steelPickaxe.is(ItemTags.PICKAXES),
                "Steel tools must join their vanilla item-family tags"
        );
        helper.assertTrue(
                Items.IRON_PICKAXE.getDefaultInstance().is(ItemTags.PICKAXES),
                "Steel tag additions must preserve vanilla tool entries"
        );
        ItemStack steelHelmet = RetoldBlocks.STEEL_HELMET.get()
                .getDefaultInstance();
        helper.assertTrue(
                steelHelmet.getMaxDamage() == 275,
                "Steel armor must use the provisional 25x durability"
        );
        helper.assertTrue(
                steelHelmet.is(ItemTags.HEAD_ARMOR)
                        && steelHelmet.is(ItemTags.TRIMMABLE_ARMOR),
                "Steel armor must support normal armor systems and trims"
        );

        var enchantments = helper.getLevel()
                .registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT);
        ItemStack diamondPickaxe = Items.DIAMOND_PICKAXE
                .getDefaultInstance();
        helper.assertTrue(
                diamondPickaxe.getMaxDamage() == 64,
                "Unenchanted Diamond tools must use fragile durability"
        );
        diamondPickaxe.enchant(
                enchantments.getOrThrow(Enchantments.EFFICIENCY),
                1
        );
        helper.assertTrue(
                diamondPickaxe.getMaxDamage() == 1561,
                "Any enchantment must restore full Diamond tool durability"
        );
        diamondPickaxe.set(
                DataComponents.ENCHANTMENTS,
                ItemEnchantments.EMPTY
        );
        helper.assertTrue(
                diamondPickaxe.getMaxDamage() == 64,
                "Removing all enchantments must restore fragile durability"
        );

        diamondPickaxe.enchant(
                enchantments.getOrThrow(Enchantments.EFFICIENCY),
                1
        );
        diamondPickaxe.setDamageValue(1000);
        diamondPickaxe.set(
                DataComponents.ENCHANTMENTS,
                ItemEnchantments.EMPTY
        );
        helper.assertTrue(
                diamondPickaxe.getMaxDamage() == 1001
                        && diamondPickaxe.nextDamageWillBreak(),
                "Stripping heavily damaged Diamond gear must leave one use instead of an invalid stack"
        );

        ItemStack diamondHelmet = Items.DIAMOND_HELMET
                .getDefaultInstance();
        helper.assertTrue(
                diamondHelmet.getMaxDamage() == 66,
                "Unenchanted Diamond armor must use 6x durability"
        );
        diamondHelmet.enchant(
                enchantments.getOrThrow(Enchantments.PROTECTION),
                1
        );
        helper.assertTrue(
                diamondHelmet.getMaxDamage() == 363,
                "Any enchantment must restore full Diamond armor durability"
        );
        diamondHelmet.set(
                DataComponents.ENCHANTMENTS,
                ItemEnchantments.EMPTY
        );
        helper.assertTrue(
                diamondHelmet.getMaxDamage() == 66,
                "Stripped Diamond armor must become fragile again"
        );

        helper.succeed();
    }

    private static void recipesEnforceOpeningLoop(GameTestHelper helper) {
        RecipeManager recipes = helper.getLevel().getServer().getRecipeManager();

        for (String removedRecipeId : REMOVED_TOOL_RECIPE_IDS) {
            helper.assertTrue(
                    recipes.byKey(minecraftRecipeKey(removedRecipeId)).isEmpty(),
                    "Wooden and Stone tool recipes must be removed: "
                            + removedRecipeId
            );
        }

        CraftingInput flintMultiToolInput = CraftingInput.of(
                2,
                2,
                List.of(
                        new ItemStack(Items.FLINT),
                        new ItemStack(Items.FLINT),
                        ItemStack.EMPTY,
                        new ItemStack(Items.STICK)
                )
        );
        RecipeHolder<CraftingRecipe> flintMultiToolRecipe = recipes.getRecipeFor(
                RecipeType.CRAFTING,
                flintMultiToolInput,
                helper.getLevel()
        ).orElseThrow(() -> helper.assertionException(
                "The Flint Multi-tool must be craftable in the 2x2 inventory grid"
        ));
        helper.assertTrue(
                flintMultiToolRecipe.id().equals(
                        retoldRecipeKey("flint_multi_tool")
                ),
                "The confirmed two-Flint-and-one-Stick shape must select the Flint Multi-tool recipe"
        );
        helper.assertTrue(
                flintMultiToolRecipe.value()
                        .assemble(flintMultiToolInput)
                        .is(RetoldBlocks.FLINT_MULTI_TOOL.get()),
                "The opening recipe must produce the Flint Multi-tool"
        );
        assertSpearRecipe(
                helper,
                recipes,
                Items.FLINT,
                RetoldBlocks.FLINT_SPEAR.get(),
                "Flint"
        );
        helper.assertTrue(
                RetoldBlocks.FLINT_SPEAR.get().getDefaultInstance()
                        .is(ItemTags.SPEARS)
                        && RetoldBlocks.FLINT_SPEAR.get()
                        .getDefaultInstance()
                        .getMaxDamage() == 48,
                "The Flint Spear must join the Spear family at Flint durability"
        );

        CraftingInput unfueledCampfireInput = campfireInput(ItemStack.EMPTY);
        RecipeHolder<?> loadedCampfireRecipe = recipes.byKey(
                minecraftRecipeKey("campfire")
        ).orElseThrow(() -> helper.assertionException(
                "The replaced Campfire recipe must load"
        ));
        helper.assertTrue(
                loadedCampfireRecipe.value() instanceof CraftingRecipe,
                "The replaced Campfire recipe must remain a crafting recipe"
        );
        CraftingRecipe unfueledCampfireRecipe =
                (CraftingRecipe) loadedCampfireRecipe.value();
        helper.assertTrue(
                unfueledCampfireRecipe.matches(
                        unfueledCampfireInput,
                        helper.getLevel()
                ) && unfueledCampfireRecipe
                        .assemble(unfueledCampfireInput)
                        .is(Items.CAMPFIRE),
                "The Campfire recipe must no longer require Coal or Flint"
        );

        CraftingInput oldCampfireInput = campfireInput(
                Items.COAL.getDefaultInstance()
        );
        boolean coalStillCraftsCampfire = recipes.getRecipeFor(
                RecipeType.CRAFTING,
                oldCampfireInput,
                helper.getLevel()
        ).map(recipe -> recipe.value()
                .assemble(oldCampfireInput)
                .is(Items.CAMPFIRE)
        ).orElse(false);
        helper.assertFalse(
                coalStillCraftsCampfire,
                "The old Coal-based Campfire recipe must be replaced"
        );

        assertCampfireCookingResult(
                helper,
                recipes,
                Items.CLAY_BALL.getDefaultInstance(),
                Items.BRICK,
                "A Campfire must fire Clay Balls into Bricks"
        );

        CraftingInput brickFurnaceInput = furnaceRingInput(Items.BRICK);
        RecipeHolder<CraftingRecipe> brickFurnaceRecipe = recipes.getRecipeFor(
                RecipeType.CRAFTING,
                brickFurnaceInput,
                helper.getLevel()
        ).orElseThrow(() -> helper.assertionException(
                "Eight Bricks in a ring must craft the Brick Furnace"
        ));
        helper.assertTrue(
                brickFurnaceRecipe.id().equals(minecraftRecipeKey("smoker")),
                "The Brick Furnace must reuse the vanilla Smoker recipe identity"
        );
        helper.assertTrue(
                brickFurnaceRecipe.value()
                        .assemble(brickFurnaceInput)
                        .is(Items.SMOKER),
                "The Brick Furnace recipe must produce the repurposed Smoker block"
        );

        CraftingInput unfiredClayRing = furnaceRingInput(Items.CLAY_BALL);
        boolean unfiredClayCraftsFurnace = recipes.getRecipeFor(
                RecipeType.CRAFTING,
                unfiredClayRing,
                helper.getLevel()
        ).map(recipe -> recipe.value()
                .assemble(unfiredClayRing)
                .is(Items.SMOKER)
        ).orElse(false);
        helper.assertFalse(
                unfiredClayCraftsFurnace,
                "Unfired Clay Balls must not bypass the Campfire Brick step"
        );

        assertSmokingResult(
                helper,
                recipes,
                Items.RAW_COPPER.getDefaultInstance(),
                Items.COPPER_INGOT,
                "The Brick Furnace must smelt Raw Copper"
        );
        assertSmokingResult(
                helper,
                recipes,
                Items.OAK_LOG.getDefaultInstance(),
                Items.CHARCOAL,
                "The Brick Furnace must make Charcoal from burnable logs"
        );
        assertBlastingResult(
                helper,
                recipes,
                Items.IRON_INGOT.getDefaultInstance(),
                RetoldBlocks.STEEL_INGOT.get(),
                "The Blast Furnace must process Iron directly into Steel"
        );

        for (String steelRecipeId : STEEL_EQUIPMENT_RECIPE_IDS) {
            helper.assertTrue(
                    recipes.byKey(retoldRecipeKey(steelRecipeId)).isPresent(),
                    "Steel equipment recipe must load: " + steelRecipeId
            );
        }
        assertSpearRecipe(
                helper,
                recipes,
                RetoldBlocks.STEEL_INGOT.get(),
                RetoldBlocks.STEEL_SPEAR.get(),
                "Steel"
        );
        helper.assertTrue(
                RetoldBlocks.STEEL_SPEAR.get().getDefaultInstance()
                        .is(ItemTags.SPEARS)
                        && RetoldBlocks.STEEL_SPEAR.get()
                        .getDefaultInstance()
                        .getMaxDamage() == 750,
                "The Steel Spear must join the Spear family at Steel durability"
        );

        CraftingInput steelPickaxeInput = CraftingInput.of(
                3,
                3,
                List.of(
                        RetoldBlocks.STEEL_INGOT.get().getDefaultInstance(),
                        RetoldBlocks.STEEL_INGOT.get().getDefaultInstance(),
                        RetoldBlocks.STEEL_INGOT.get().getDefaultInstance(),
                        ItemStack.EMPTY,
                        Items.STICK.getDefaultInstance(),
                        ItemStack.EMPTY,
                        ItemStack.EMPTY,
                        Items.STICK.getDefaultInstance(),
                        ItemStack.EMPTY
                )
        );
        RecipeHolder<CraftingRecipe> steelPickaxeRecipe = recipes.getRecipeFor(
                RecipeType.CRAFTING,
                steelPickaxeInput,
                helper.getLevel()
        ).orElseThrow(() -> helper.assertionException(
                "Steel must use the familiar Pickaxe crafting shape"
        ));
        helper.assertTrue(
                steelPickaxeRecipe.value()
                        .assemble(steelPickaxeInput)
                        .is(RetoldBlocks.STEEL_PICKAXE.get()),
                "The Steel Pickaxe recipe must produce the registered tool"
        );

        helper.assertTrue(
                recipes.byKey(minecraftRecipeKey("furnace")).isPresent(),
                "The normal Furnace recipe must remain available after Copper"
        );
        helper.succeed();
    }

    private static void assertSpearRecipe(
            GameTestHelper helper,
            RecipeManager recipes,
            Item point,
            Item expectedResult,
            String tier
    ) {
        CraftingInput input = CraftingInput.of(
                3,
                3,
                List.of(
                        ItemStack.EMPTY,
                        ItemStack.EMPTY,
                        point.getDefaultInstance(),
                        ItemStack.EMPTY,
                        Items.STICK.getDefaultInstance(),
                        ItemStack.EMPTY,
                        Items.STICK.getDefaultInstance(),
                        ItemStack.EMPTY,
                        ItemStack.EMPTY
                )
        );
        RecipeHolder<CraftingRecipe> recipe = recipes.getRecipeFor(
                RecipeType.CRAFTING,
                input,
                helper.getLevel()
        ).orElseThrow(() -> helper.assertionException(
                tier + " must use the standard diagonal Spear recipe"
        ));
        helper.assertTrue(
                recipe.value().assemble(input).is(expectedResult),
                tier + " Spear recipe must produce the registered Spear"
        );
    }

    private static void assertSmokingResult(
            GameTestHelper helper,
            RecipeManager recipes,
            ItemStack ingredient,
            Item expectedResult,
            String message
    ) {
        SingleRecipeInput input = new SingleRecipeInput(ingredient);
        RecipeHolder<SmokingRecipe> recipe = recipes.getRecipeFor(
                RecipeType.SMOKING,
                input,
                helper.getLevel()
        ).orElseThrow(() -> helper.assertionException(message));
        helper.assertTrue(recipe.value().assemble(input).is(expectedResult), message);
    }

    private static void assertCampfireCookingResult(
            GameTestHelper helper,
            RecipeManager recipes,
            ItemStack ingredient,
            Item expectedResult,
            String message
    ) {
        SingleRecipeInput input = new SingleRecipeInput(ingredient);
        RecipeHolder<CampfireCookingRecipe> recipe = recipes.getRecipeFor(
                RecipeType.CAMPFIRE_COOKING,
                input,
                helper.getLevel()
        ).orElseThrow(() -> helper.assertionException(message));
        helper.assertTrue(
                recipe.id().equals(retoldRecipeKey(
                        "brick_from_campfire_cooking_clay_ball"
                )),
                "The Brick recipe must be Retold's explicit Campfire step"
        );
        helper.assertTrue(recipe.value().assemble(input).is(expectedResult), message);
    }

    private static CraftingInput furnaceRingInput(Item ingredient) {
        return CraftingInput.of(
                3,
                3,
                List.of(
                        new ItemStack(ingredient),
                        new ItemStack(ingredient),
                        new ItemStack(ingredient),
                        new ItemStack(ingredient),
                        ItemStack.EMPTY,
                        new ItemStack(ingredient),
                        new ItemStack(ingredient),
                        new ItemStack(ingredient),
                        new ItemStack(ingredient)
                )
        );
    }

    private static CraftingInput campfireInput(ItemStack center) {
        return CraftingInput.of(
                3,
                3,
                List.of(
                        ItemStack.EMPTY,
                        Items.STICK.getDefaultInstance(),
                        ItemStack.EMPTY,
                        Items.STICK.getDefaultInstance(),
                        center,
                        Items.STICK.getDefaultInstance(),
                        Items.OAK_LOG.getDefaultInstance(),
                        Items.OAK_LOG.getDefaultInstance(),
                        Items.OAK_LOG.getDefaultInstance()
                )
        );
    }

    private static void assertBlastingResult(
            GameTestHelper helper,
            RecipeManager recipes,
            ItemStack ingredient,
            Item expectedResult,
            String message
    ) {
        SingleRecipeInput input = new SingleRecipeInput(ingredient);
        RecipeHolder<BlastingRecipe> recipe = recipes.getRecipeFor(
                RecipeType.BLASTING,
                input,
                helper.getLevel()
        ).orElseThrow(() -> helper.assertionException(message));
        helper.assertTrue(recipe.value().assemble(input).is(expectedResult), message);
    }

    private static ResourceKey<Recipe<?>> minecraftRecipeKey(String path) {
        return ResourceKey.create(
                Registries.RECIPE,
                Identifier.withDefaultNamespace(path)
        );
    }

    private static ResourceKey<Recipe<?>> retoldRecipeKey(String path) {
        return ResourceKey.create(
                Registries.RECIPE,
                Identifier.fromNamespaceAndPath("retold", path)
        );
    }

    private static void registerTest(
            RegisterGameTestsEvent event,
            Holder<TestEnvironmentDefinition<?>> environment,
            String name,
            Consumer<GameTestHelper> test
    ) {
        TestData<Holder<TestEnvironmentDefinition<?>>> testData =
                new TestData<>(
                        environment,
                        EMPTY_STRUCTURE,
                        40,
                        0,
                        true
                );

        event.registerTest(
                retoldId(name),
                new InlineGameTest(testData, test)
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
