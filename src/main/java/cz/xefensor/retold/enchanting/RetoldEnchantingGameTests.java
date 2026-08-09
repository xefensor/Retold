package cz.xefensor.retold.enchanting;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.client.enchanting.RetoldClientEnchantmentCatalog;
import cz.xefensor.retold.client.enchanting.RetoldClientEnchantmentKnowledge;
import cz.xefensor.retold.client.enchanting.RetoldClientEnchantingOptions;
import cz.xefensor.retold.client.enchanting.RetoldEnchantmentTooltip;
import cz.xefensor.retold.network.RetoldEnchantmentCatalogSyncPayload;
import cz.xefensor.retold.network.RetoldEnchantingCastPayload;
import cz.xefensor.retold.network.RetoldEnchantingCastResultPayload;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.BuiltinTestFunctions;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public final class RetoldEnchantingGameTests {
    private static final Identifier EMPTY_STRUCTURE =
            Identifier.withDefaultNamespace("empty");

    private RetoldEnchantingGameTests() {
    }

    public static void register(
            RegisterGameTestsEvent event,
            Holder<TestEnvironmentDefinition<?>> environment
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
                id("anvil_teaches_only_successfully_transferred_book_enchantments"),
                new InlineGameTest(
                        testData,
                        RetoldEnchantingGameTests::anvilTeachesOnlyTransferredEnchantments
                )
        );
        event.registerTest(
                id("enchantment_catalog_payload_round_trips"),
                new InlineGameTest(
                        testData,
                        RetoldEnchantingGameTests::enchantmentCatalogPayloadRoundTrips
                )
        );
        event.registerTest(
                id("enchantment_catalog_covers_registry"),
                new InlineGameTest(
                        testData,
                        RetoldEnchantingGameTests::enchantmentCatalogCoversRegistry
                )
        );
        event.registerTest(
                id("enchantment_tooltips_hide_unknown_names"),
                new InlineGameTest(
                        testData,
                        RetoldEnchantingGameTests::enchantmentTooltipsHideUnknownNames
                )
        );
        event.registerTest(
                id("deterministic_enchanting_casts_are_atomic"),
                new InlineGameTest(
                        testData,
                        RetoldEnchantingGameTests::deterministicEnchantingCastsAreAtomic
                )
        );
        event.registerTest(
                id("enchanting_menu_casts_update_synchronized_slots"),
                new InlineGameTest(
                        testData,
                        RetoldEnchantingGameTests::enchantingMenuCastsUpdateSynchronizedSlots
                )
        );
        event.registerTest(
                id("enchanting_cast_payload_round_trips"),
                new InlineGameTest(
                        testData,
                        RetoldEnchantingGameTests::enchantingCastPayloadRoundTrips
                )
        );
        event.registerTest(
                id("known_enchanting_options_follow_inserted_item"),
                new InlineGameTest(
                        testData,
                        RetoldEnchantingGameTests::knownEnchantingOptionsFollowInsertedItem
                )
        );
    }

    private static void anvilTeachesOnlyTransferredEnchantments(GameTestHelper helper) {
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        Holder<Enchantment> sharpness = helper.getLevel()
                .registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.SHARPNESS);
        Holder<Enchantment> featherFalling = helper.getLevel()
                .registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.FEATHER_FALLING);
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        EnchantmentHelper.updateEnchantments(book, enchantments -> {
            enchantments.set(sharpness, 1);
            enchantments.set(featherFalling, 4);
        });
        AnvilMenu menu = new AnvilMenu(1, player.getInventory());

        try {
            player.giveExperienceLevels(100);
            menu.getSlot(AnvilMenu.INPUT_SLOT).set(new ItemStack(Items.IRON_SWORD));
            menu.getSlot(AnvilMenu.ADDITIONAL_SLOT).set(book);

            ItemStack preview = menu.getSlot(AnvilMenu.RESULT_SLOT).getItem();
            helper.assertFalse(preview.isEmpty(), "A compatible book spell must produce an anvil result");
            helper.assertValueEqual(
                    EnchantmentHelper.getEnchantmentsForCrafting(preview).getLevel(sharpness),
                    1,
                    "Sharpness must transfer to the sword"
            );
            helper.assertValueEqual(
                    EnchantmentHelper.getEnchantmentsForCrafting(preview).getLevel(featherFalling),
                    0,
                    "Incompatible Feather Falling must not transfer to the sword"
            );

            ItemStack taken = menu.getSlot(AnvilMenu.RESULT_SLOT).safeTake(1, 1, player);
            helper.assertFalse(taken.isEmpty(), "The test player must complete the anvil transaction");
            helper.assertTrue(
                    RetoldEnchantmentKnowledge.isKnown(player, Enchantments.SHARPNESS.identifier()),
                    "Taking the output must teach the transferred Sharpness spell"
            );
            helper.assertFalse(
                    RetoldEnchantmentKnowledge.isKnown(player, Enchantments.FEATHER_FALLING.identifier()),
                    "An incompatible spell that was not transferred must remain unknown"
            );
            helper.succeed();
        } finally {
            player.discard();
        }
    }

    private static void enchantmentCatalogPayloadRoundTrips(GameTestHelper helper) {
        RetoldEnchantmentSpellDefinition sharpness = new RetoldEnchantmentSpellDefinition(
                "minecraft:sharpness",
                new RetoldEnchantmentWord(
                        "retold:weapon",
                        "retold:damage",
                        "retold:general"
                )
        );
        RetoldEnchantmentSpellDefinition featherFalling = new RetoldEnchantmentSpellDefinition(
                "minecraft:feather_falling",
                new RetoldEnchantmentWord(
                        "retold:armor",
                        "retold:protect",
                        "retold:fall"
                )
        );
        RetoldEnchantmentCatalogSyncPayload original =
                new RetoldEnchantmentCatalogSyncPayload(
                        List.of(sharpness, featherFalling)
                );
        ByteBuf buffer = Unpooled.buffer();

        try {
            RetoldEnchantmentCatalogSyncPayload.STREAM_CODEC.encode(buffer, original);
            RetoldEnchantmentCatalogSyncPayload decoded =
                    RetoldEnchantmentCatalogSyncPayload.STREAM_CODEC.decode(buffer);

            helper.assertValueEqual(
                    decoded,
                    original,
                    "The catalog payload must preserve every semantic spell definition"
            );
            helper.assertValueEqual(
                    decoded.definitions(),
                    List.of(featherFalling, sharpness),
                    "The catalog payload must use deterministic enchantment-id ordering"
            );
            helper.succeed();
        } finally {
            buffer.release();
        }
    }

    private static void enchantmentCatalogCoversRegistry(GameTestHelper helper) {
        List<Identifier> registeredEnchantments = helper.getLevel()
                .registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .listElementIds()
                .map(key -> key.identifier())
                .sorted()
                .toList();

        helper.assertValueEqual(
                registeredEnchantments.size(),
                43,
                "The coverage fixture must be updated when Minecraft adds an enchantment"
        );
        helper.assertValueEqual(
                RetoldEnchantmentCatalog.size(),
                registeredEnchantments.size(),
                "Every registered enchantment must have one Retold spell definition"
        );

        for (Identifier enchantment : registeredEnchantments) {
            RetoldEnchantmentSpellDefinition definition =
                    RetoldEnchantmentCatalog.byEnchantment(enchantment).orElse(null);
            helper.assertTrue(
                    definition != null,
                    "Missing spell definition for " + enchantment
            );
            helper.assertValueEqual(
                    RetoldEnchantmentGlyphVocabulary.glyphWord(definition.word()).length(),
                    3,
                    "Every spell must render as exactly three SGA glyphs"
            );
            helper.assertValueEqual(
                    RetoldEnchantmentCatalog.byWord(definition.word()).orElse(null),
                    definition,
                    "Every spell word must resolve back to " + enchantment
            );
        }

        helper.succeed();
    }

    private static void enchantmentTooltipsHideUnknownNames(GameTestHelper helper) {
        Holder<Enchantment> sharpness = helper.getLevel()
                .registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.SHARPNESS);
        Holder<Enchantment> smite = helper.getLevel()
                .registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.SMITE);
        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        EnchantmentHelper.updateEnchantments(sword, enchantments -> {
            enchantments.set(sharpness, 2);
            enchantments.set(smite, 3);
        });
        Component before = Component.literal("before");
        Component after = Component.literal("after");
        Component sharpnessName = Enchantment.getFullname(sharpness, 2);
        Component smiteName = Enchantment.getFullname(smite, 3);
        List<Component> tooltip = new ArrayList<>(
                List.of(before, sharpnessName, smiteName, after)
        );

        RetoldClientEnchantmentCatalog.replace(RetoldEnchantmentCatalog.definitions());
        RetoldClientEnchantmentKnowledge.replace(
                List.of(Enchantments.SHARPNESS.identifier())
        );

        try {
            RetoldEnchantmentTooltip.rewriteTooltip(sword, tooltip);

            helper.assertValueEqual(
                    tooltip.size(),
                    5,
                    "A known spell must add exactly one glyph line"
            );
            helper.assertValueEqual(
                    tooltip.getFirst(),
                    before,
                    "Tooltip rewriting must preserve preceding lines"
            );
            helper.assertValueEqual(
                    tooltip.get(1),
                    sharpnessName,
                    "A known enchantment must retain its readable name"
            );
            helper.assertValueEqual(
                    tooltip.getLast(),
                    after,
                    "Tooltip rewriting must preserve following lines"
            );
            helper.assertValueEqual(
                    tooltip.get(2).getString(),
                    "XEJ",
                    "A known enchantment must show its SGA word after its readable name"
            );
            helper.assertValueEqual(
                    tooltip.get(3).getString(),
                    "XEV III",
                    "An unknown enchantment must show only its SGA word and level"
            );
            helper.assertFalse(
                    tooltip.contains(smiteName),
                    "An unknown enchantment must not retain its readable name"
            );
            helper.assertValueEqual(
                    tooltip.get(2).getStyle().getFont(),
                    new FontDescription.Resource(Identifier.withDefaultNamespace("alt")),
                    "Spell words must use Minecraft's SGA font"
            );

            ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
            EnchantmentHelper.updateEnchantments(
                    book,
                    enchantments -> enchantments.set(smite, 1)
            );
            Component bookSmiteName = Enchantment.getFullname(smite, 1);
            List<Component> bookTooltip = new ArrayList<>(List.of(bookSmiteName));

            RetoldEnchantmentTooltip.rewriteTooltip(book, bookTooltip);
            helper.assertValueEqual(
                    bookTooltip.getFirst().getString(),
                    "XEV I",
                    "Enchanted books must hide unknown readable names too"
            );
            helper.assertFalse(
                    bookTooltip.contains(bookSmiteName),
                    "The stored-enchantment tooltip line must be replaced"
            );
            helper.succeed();
        } finally {
            RetoldClientEnchantmentCatalog.clear();
            RetoldClientEnchantmentKnowledge.clear();
        }
    }

    private static void deterministicEnchantingCastsAreAtomic(GameTestHelper helper) {
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        Holder<Enchantment> sharpness = helper.getLevel()
                .registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.SHARPNESS);
        Holder<Enchantment> smite = helper.getLevel()
                .registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.SMITE);
        RetoldEnchantmentWord sharpnessWord = new RetoldEnchantmentWord(
                "retold:weapon",
                "retold:damage",
                "retold:general"
        );
        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        ItemStack lapis = new ItemStack(Items.LAPIS_LAZULI, 3);

        try {
            player.giveExperienceLevels(30);
            RetoldEnchantingCastService.Result invalidWord =
                    RetoldEnchantingCastService.tryCast(
                            player,
                            sword,
                            lapis,
                            new RetoldEnchantmentWord(
                                    "retold:armor",
                                    "retold:damage",
                                    "retold:general"
                            ),
                            3
                    );
            helper.assertValueEqual(
                    invalidWord.status(),
                    RetoldEnchantingCastService.Status.INVALID_WORD,
                    "A nonexistent word must be rejected"
            );
            assertResources(helper, player, lapis, 30, 3, "invalid word");
            helper.assertValueEqual(
                    EnchantmentHelper.getEnchantmentsForCrafting(sword).getLevel(sharpness),
                    0,
                    "A rejected word must not mutate its target"
            );

            RetoldEnchantingCastService.Result success =
                    RetoldEnchantingCastService.tryCast(
                            player,
                            sword,
                            lapis,
                            sharpnessWord,
                            3
                    );
            helper.assertTrue(success.success(), "A valid Sharpness III cast must succeed");
            helper.assertValueEqual(
                    EnchantmentHelper.getEnchantmentsForCrafting(success.output())
                            .getLevel(sharpness),
                    3,
                    "The output must receive exactly the requested level"
            );
            helper.assertValueEqual(
                    EnchantmentHelper.getEnchantmentsForCrafting(sword).getLevel(sharpness),
                    0,
                    "Validation must prepare a separate output before the caller commits it"
            );
            assertResources(helper, player, lapis, 15, 0, "successful level-three cast");
            helper.assertTrue(
                    RetoldEnchantmentKnowledge.isKnown(
                            player,
                            Enchantments.SHARPNESS.identifier()
                    ),
                    "Successfully deducing and casting Sharpness must teach it"
            );

            ItemStack conflictingSword = new ItemStack(Items.IRON_SWORD);
            conflictingSword.enchant(smite, 1);
            ItemStack conflictLapis = new ItemStack(Items.LAPIS_LAZULI, 3);
            RetoldEnchantingCastService.Result conflict =
                    RetoldEnchantingCastService.tryCast(
                            player,
                            conflictingSword,
                            conflictLapis,
                            sharpnessWord,
                            1
                    );
            helper.assertValueEqual(
                    conflict.status(),
                    RetoldEnchantingCastService.Status.CONFLICTING_ENCHANTMENT,
                    "Vanilla-exclusive enchantments must remain incompatible"
            );
            assertResources(helper, player, conflictLapis, 15, 3, "incompatible cast");
            helper.assertValueEqual(
                    EnchantmentHelper.getEnchantmentsForCrafting(conflictingSword)
                            .getLevel(smite),
                    1,
                    "A rejected conflict must preserve the existing enchantment"
            );

            ItemStack insufficientLapis = new ItemStack(Items.LAPIS_LAZULI, 2);
            RetoldEnchantingCastService.Result missingLapis =
                    RetoldEnchantingCastService.tryCast(
                            player,
                            new ItemStack(Items.IRON_SWORD),
                            insufficientLapis,
                            sharpnessWord,
                            1
                    );
            helper.assertValueEqual(
                    missingLapis.status(),
                    RetoldEnchantingCastService.Status.INSUFFICIENT_LAPIS,
                    "Every valid cast must require all three lapis"
            );
            assertResources(helper, player, insufficientLapis, 15, 2, "insufficient lapis");

            player.giveExperienceLevels(-15);
            ItemStack experienceLapis = new ItemStack(Items.LAPIS_LAZULI, 3);
            RetoldEnchantingCastService.Result missingExperience =
                    RetoldEnchantingCastService.tryCast(
                            player,
                            new ItemStack(Items.IRON_SWORD),
                            experienceLapis,
                            sharpnessWord,
                            1
                    );
            helper.assertValueEqual(
                    missingExperience.status(),
                    RetoldEnchantingCastService.Status.INSUFFICIENT_EXPERIENCE,
                    "A level-one cast must require five experience levels"
            );
            assertResources(helper, player, experienceLapis, 0, 3, "insufficient experience");

            player.giveExperienceLevels(10);
            RetoldEnchantingCastService.Result unavailableMending =
                    RetoldEnchantingCastService.tryCast(
                            player,
                            new ItemStack(Items.IRON_SWORD),
                            new ItemStack(Items.LAPIS_LAZULI, 3),
                            new RetoldEnchantmentWord(
                                    "retold:item",
                                    "retold:restore",
                                    "retold:general"
                            ),
                            1
                    );
            helper.assertValueEqual(
                    unavailableMending.status(),
                    RetoldEnchantingCastService.Status.UNAVAILABLE_ENCHANTMENT,
                    "Vanilla non-table enchantments must remain unavailable"
            );

            ItemStack bookLapis = new ItemStack(Items.LAPIS_LAZULI, 3);
            RetoldEnchantingCastService.Result enchantedBook =
                    RetoldEnchantingCastService.tryCast(
                            player,
                            new ItemStack(Items.BOOK),
                            bookLapis,
                            sharpnessWord,
                            1
                    );
            helper.assertTrue(
                    enchantedBook.success(),
                    "A plain book must retain vanilla enchanting-table support"
            );
            helper.assertTrue(
                    enchantedBook.output().is(Items.ENCHANTED_BOOK),
                    "Vanilla item transformation must produce an enchanted book"
            );
            helper.assertValueEqual(
                    EnchantmentHelper.getEnchantmentsForCrafting(enchantedBook.output())
                            .getLevel(sharpness),
                    1,
                    "The transformed book must store the cast spell"
            );
            assertResources(helper, player, bookLapis, 5, 0, "successful book cast");
            helper.succeed();
        } finally {
            player.discard();
        }
    }

    private static void enchantingMenuCastsUpdateSynchronizedSlots(GameTestHelper helper) {
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        EnchantmentMenu menu = new EnchantmentMenu(73, player.getInventory());
        Holder<Enchantment> sharpness = helper.getLevel()
                .registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.SHARPNESS);
        RetoldEnchantmentWord sharpnessWord = new RetoldEnchantmentWord(
                "retold:weapon",
                "retold:damage",
                "retold:general"
        );

        try {
            player.containerMenu = menu;
            player.giveExperienceLevels(20);
            menu.getSlot(0).set(new ItemStack(Items.IRON_SWORD));
            menu.getSlot(1).set(new ItemStack(Items.LAPIS_LAZULI, 3));

            helper.assertFalse(
                    RetoldEnchantingMenuActions.tryCast(player, 74, sharpnessWord, 3),
                    "A stale or forged container id must not cast"
            );
            helper.assertValueEqual(
                    menu.getSlot(1).getItem().getCount(),
                    3,
                    "A rejected menu request must preserve lapis"
            );

            helper.assertTrue(
                    RetoldEnchantingMenuActions.tryCast(player, 73, sharpnessWord, 3),
                    "The active enchanting menu must accept a valid cast"
            );
            helper.assertValueEqual(
                    EnchantmentHelper.getEnchantmentsForCrafting(menu.getSlot(0).getItem())
                            .getLevel(sharpness),
                    3,
                    "The synchronized target slot must receive the cast enchantment"
            );
            helper.assertTrue(
                    menu.getSlot(1).getItem().isEmpty(),
                    "The synchronized lapis slot must become empty after spending three"
            );
            helper.assertValueEqual(
                    player.experienceLevel,
                    5,
                    "The active-menu transaction must charge the confirmed level cost"
            );
            helper.succeed();
        } finally {
            player.discard();
        }
    }

    private static void enchantingCastPayloadRoundTrips(GameTestHelper helper) {
        RetoldEnchantingCastPayload original = new RetoldEnchantingCastPayload(
                73,
                new RetoldEnchantmentWord(
                        "retold:weapon",
                        "retold:damage",
                        "retold:undead"
                ),
                5
        );
        ByteBuf buffer = Unpooled.buffer();

        try {
            RetoldEnchantingCastPayload.STREAM_CODEC.encode(buffer, original);
            RetoldEnchantingCastPayload decoded =
                    RetoldEnchantingCastPayload.STREAM_CODEC.decode(buffer);

            helper.assertValueEqual(
                    decoded,
                    original,
                    "The cast request must preserve menu, semantic word, and level"
            );

            RetoldEnchantingCastResultPayload result =
                    new RetoldEnchantingCastResultPayload(73, true);
            ByteBuf resultBuffer = Unpooled.buffer();
            try {
                RetoldEnchantingCastResultPayload.STREAM_CODEC.encode(
                        resultBuffer,
                        result
                );
                helper.assertValueEqual(
                        RetoldEnchantingCastResultPayload.STREAM_CODEC.decode(
                                resultBuffer
                        ),
                        result,
                        "The result response must preserve the menu id and outcome"
                );
            } finally {
                resultBuffer.release();
            }
            helper.succeed();
        } finally {
            buffer.release();
        }
    }

    private static void knownEnchantingOptionsFollowInsertedItem(
            GameTestHelper helper
    ) {
        Set<Identifier> known = Set.of(
                Enchantments.MENDING.identifier(),
                Enchantments.PROTECTION.identifier(),
                Enchantments.SHARPNESS.identifier()
        );
        List<RetoldEnchantmentSpellDefinition> definitions =
                RetoldEnchantmentCatalog.definitions();

        List<RetoldEnchantmentSpellDefinition> emptyOptions =
                RetoldClientEnchantingOptions.availableKnownSpells(
                        helper.getLevel().registryAccess(),
                        definitions,
                        known,
                        ItemStack.EMPTY
                );
        helper.assertTrue(
                containsEnchantment(emptyOptions, Enchantments.SHARPNESS.identifier()),
                "An empty target must retain table-eligible known spells"
        );
        helper.assertFalse(
                containsEnchantment(emptyOptions, Enchantments.MENDING.identifier()),
                "A non-table treasure spell must not appear as a writable option"
        );

        List<RetoldEnchantmentSpellDefinition> bootOptions =
                RetoldClientEnchantingOptions.availableKnownSpells(
                        helper.getLevel().registryAccess(),
                        definitions,
                        known,
                        new ItemStack(Items.IRON_BOOTS)
                );
        helper.assertTrue(
                containsEnchantment(bootOptions, Enchantments.PROTECTION.identifier()),
                "Armor-compatible known spells must remain available for boots"
        );
        helper.assertFalse(
                containsEnchantment(bootOptions, Enchantments.SHARPNESS.identifier()),
                "Weapon-only spells must be hidden for boots"
        );

        Holder<Enchantment> smite = helper.getLevel()
                .registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.SMITE);
        ItemStack smiteSword = new ItemStack(Items.IRON_SWORD);
        EnchantmentHelper.updateEnchantments(
                smiteSword,
                enchantments -> enchantments.set(smite, 1)
        );
        List<RetoldEnchantmentSpellDefinition> swordOptions =
                RetoldClientEnchantingOptions.availableKnownSpells(
                        helper.getLevel().registryAccess(),
                        definitions,
                        known,
                        smiteSword
                );
        helper.assertFalse(
                containsEnchantment(swordOptions, Enchantments.SHARPNESS.identifier()),
                "Spells conflicting with an existing enchantment must be hidden"
        );

        RetoldEnchantmentSpellDefinition sharpness = RetoldEnchantmentCatalog
                .byEnchantment(Enchantments.SHARPNESS.identifier())
                .orElseThrow();
        helper.assertValueEqual(
                RetoldClientEnchantingOptions.resolve(
                        helper.getLevel().registryAccess(),
                        sharpness
                ).orElseThrow().value().getMaxLevel(),
                5,
                "The client option must expose the registered maximum level"
        );
        helper.succeed();
    }

    private static boolean containsEnchantment(
            List<RetoldEnchantmentSpellDefinition> definitions,
            Identifier enchantment
    ) {
        return definitions.stream().anyMatch(
                definition -> definition.enchantment().equals(enchantment.toString())
        );
    }

    private static void assertResources(
            GameTestHelper helper,
            ServerPlayer player,
            ItemStack lapis,
            int expectedExperienceLevels,
            int expectedLapis,
            String context
    ) {
        helper.assertValueEqual(
                player.experienceLevel,
                expectedExperienceLevels,
                context + " must preserve the expected experience levels"
        );
        helper.assertValueEqual(
                lapis.getCount(),
                expectedLapis,
                context + " must preserve the expected lapis count"
        );
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Retold.MODID, path);
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
