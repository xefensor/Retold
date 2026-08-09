package cz.xefensor.retold.villager;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.network.RetoldTeachingPreviewPayload;
import cz.xefensor.retold.recipe.RetoldKnownRecipeData;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.BuiltinTestFunctions;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.function.Consumer;

public final class RetoldVillagerTeachingGameTests {
    private static final Identifier EMPTY_STRUCTURE =
            Identifier.withDefaultNamespace("empty");

    private RetoldVillagerTeachingGameTests() {
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
                id("villager_teaching_transactions_are_atomic"),
                new InlineGameTest(
                        testData,
                        RetoldVillagerTeachingGameTests::teachingTransactionsAreAtomic
                )
        );
        event.registerTest(
                id("villager_teaching_preview_payload_round_trips"),
                new InlineGameTest(
                        testData,
                        RetoldVillagerTeachingGameTests::previewPayloadRoundTrips
                )
        );
    }

    private static void teachingTransactionsAreAtomic(GameTestHelper helper) {
        Villager villager = helper.spawn(EntityTypes.VILLAGER, 1, 2, 1);
        villager.setVillagerData(
                villager.getVillagerData().withProfession(
                        helper.getLevel().registryAccess(),
                        VillagerProfession.LIBRARIAN
                )
        );
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(
                GameType.SURVIVAL
        );
        MerchantMenu menu = new MerchantMenu(73, player.getInventory(), villager);
        player.containerMenu = menu;
        villager.setTradingPlayer(player);

        try {
            RetoldTeachingSlotMenu teachingMenu = (RetoldTeachingSlotMenu) menu;
            int teachingSlot = teachingMenu.retold$getTeachingSlotIndex();
            helper.assertTrue(
                    teachingSlot >= 0,
                    "The real merchant menu must install its teaching slot"
            );
            RetoldVillagerTeaching.TeachingPreview empty =
                    RetoldVillagerTeaching.createTeachingPreview(player);
            helper.assertFalse(
                    empty.active(),
                    "An empty teaching slot must not produce a lesson"
            );
            assertTranslationKey(
                    helper,
                    empty.status(),
                    "container.retold.teaching.status.place_item"
            );
            menu.getSlot(teachingSlot).set(new ItemStack(Items.BOOKSHELF));
            player.getInventory().add(new ItemStack(Items.EMERALD, 2));

            RetoldVillagerTeaching.TeachingPreview insufficient =
                    RetoldVillagerTeaching.createTeachingPreview(player);
            helper.assertFalse(
                    insufficient.active(),
                    "A lesson must stay disabled when the player cannot pay"
            );
            assertTranslationKey(
                    helper,
                    insufficient.status(),
                    "container.retold.teaching.status.not_enough_emeralds"
            );
            helper.assertValueEqual(
                    RetoldVillagerTeaching.teachHeldItemRecipe(player),
                    RetoldTeachingPreviewPayload.Feedback.REJECTED,
                    "An unaffordable lesson must return rejection feedback"
            );
            helper.assertValueEqual(
                    countItem(player, Items.EMERALD),
                    2,
                    "A rejected lesson must not consume emeralds"
            );
            helper.assertFalse(
                    RetoldKnownRecipeData.get(helper.getLevel()).hasKnown(
                            player,
                            insufficient.recipe().id()
                    ),
                    "A rejected lesson must not teach its recipe"
            );

            player.getInventory().add(new ItemStack(Items.EMERALD, 4));
            RetoldVillagerTeaching.TeachingPreview available =
                    RetoldVillagerTeaching.createTeachingPreview(player);
            helper.assertTrue(
                    available.active(),
                    "The configured Librarian lesson must activate once affordable"
            );
            assertTranslationKey(
                    helper,
                    available.status(),
                    "container.retold.teaching.status.can_learn"
            );
            helper.assertValueEqual(
                    RetoldVillagerTeaching.teachHeldItemRecipe(player),
                    RetoldTeachingPreviewPayload.Feedback.SUCCESS,
                    "An affordable configured lesson must succeed"
            );
            helper.assertValueEqual(
                    countItem(player, Items.EMERALD),
                    3,
                    "A successful Bookshelf lesson must consume exactly three emeralds"
            );
            helper.assertTrue(
                    RetoldKnownRecipeData.get(helper.getLevel()).hasKnown(
                            player,
                            available.recipe().id()
                    ),
                    "A successful lesson must teach the selected recipe"
            );
            helper.assertValueEqual(
                    villager.getVillagerXp(),
                    2,
                    "A successful default lesson must grant its configured Villager XP"
            );

            RetoldVillagerTeaching.TeachingPreview alreadyKnown =
                    RetoldVillagerTeaching.createTeachingPreview(player);
            helper.assertFalse(
                    alreadyKnown.active(),
                    "A known recipe must disable the same lesson"
            );
            assertTranslationKey(
                    helper,
                    alreadyKnown.status(),
                    "container.retold.teaching.status.already_known"
            );
            helper.assertValueEqual(
                    RetoldVillagerTeaching.teachHeldItemRecipe(player),
                    RetoldTeachingPreviewPayload.Feedback.REJECTED,
                    "A repeated request for the now-known recipe must be rejected"
            );
            helper.assertValueEqual(
                    countItem(player, Items.EMERALD),
                    3,
                    "A rapid duplicate request must not charge the lesson twice"
            );
            helper.assertValueEqual(
                    villager.getVillagerXp(),
                    2,
                    "A rapid duplicate request must not reward Villager XP twice"
            );

            menu.getSlot(teachingSlot).set(new ItemStack(Items.DIRT));
            RetoldVillagerTeaching.TeachingPreview unsupported =
                    RetoldVillagerTeaching.createTeachingPreview(player);
            helper.assertFalse(
                    unsupported.active(),
                    "An unsupported item must not produce a lesson"
            );
            assertTranslationKey(
                    helper,
                    unsupported.status(),
                    "container.retold.teaching.status.does_not_know"
            );
            helper.assertValueEqual(
                    RetoldVillagerTeaching.teachHeldItemRecipe(player),
                    RetoldTeachingPreviewPayload.Feedback.REJECTED,
                    "An unsupported item request must be rejected"
            );
            helper.assertValueEqual(
                    countItem(player, Items.EMERALD),
                    3,
                    "An unsupported item must not consume emeralds"
            );

            villager.setVillagerData(
                    villager.getVillagerData().withProfession(
                            helper.getLevel().registryAccess(),
                            VillagerProfession.NITWIT
                    )
            );
            menu.getSlot(teachingSlot).set(new ItemStack(Items.BOOKSHELF));
            RetoldVillagerTeaching.TeachingPreview wrongProfession =
                    RetoldVillagerTeaching.createTeachingPreview(player);
            helper.assertFalse(
                    wrongProfession.active(),
                    "A profession without teaching data must not teach recipes"
            );
            assertTranslationKey(
                    helper,
                    wrongProfession.status(),
                    "container.retold.teaching.status.cannot_teach"
            );
            helper.assertValueEqual(
                    RetoldVillagerTeaching.teachHeldItemRecipe(player),
                    RetoldTeachingPreviewPayload.Feedback.REJECTED,
                    "A profession without teaching data must reject the request"
            );
            helper.assertValueEqual(
                    countItem(player, Items.EMERALD),
                    3,
                    "An unsupported profession must not consume emeralds"
            );

            menu.removed(player);
            helper.assertTrue(
                    teachingMenu.retold$getTeachingItem().isEmpty(),
                    "Closing the screen must clear the teaching slot"
            );
            helper.assertValueEqual(
                    countItem(player, Items.BOOKSHELF),
                    1,
                    "Closing the screen must return the teaching item to the player"
            );
            helper.succeed();
        } finally {
            if (!((RetoldTeachingSlotMenu) menu).retold$getTeachingItem().isEmpty()) {
                menu.removed(player);
            }
            player.containerMenu = player.inventoryMenu;
            villager.setTradingPlayer(null);
            player.discard();
            villager.discard();
        }
    }

    private static void previewPayloadRoundTrips(GameTestHelper helper) {
        RetoldTeachingPreviewPayload original = new RetoldTeachingPreviewPayload(
                true,
                Component.translatable("container.retold.teaching.learn"),
                Component.translatable(
                        "container.retold.teaching.status.can_learn",
                        Component.translatable("block.minecraft.bookshelf")
                ),
                Component.translatable(
                        "container.retold.teaching.cost",
                        Component.translatable(
                                "container.retold.teaching.emerald.many",
                                3
                        )
                ),
                Component.translatable(
                        "container.retold.teaching.tooltip.pay",
                        Component.translatable(
                                "container.retold.teaching.emerald.many",
                                3
                        )
                ),
                RetoldTeachingPreviewPayload.Feedback.SUCCESS
        );
        ByteBuf buffer = Unpooled.buffer();

        try {
            RetoldTeachingPreviewPayload.STREAM_CODEC.encode(buffer, original);
            RetoldTeachingPreviewPayload decoded =
                    RetoldTeachingPreviewPayload.STREAM_CODEC.decode(buffer);
            helper.assertValueEqual(
                    decoded,
                    original,
                    "The preview payload must preserve translated text and feedback"
            );
            helper.succeed();
        } finally {
            buffer.release();
        }
    }

    private static void assertTranslationKey(
            GameTestHelper helper,
            Component component,
            String expected
    ) {
        helper.assertTrue(
                component.getContents() instanceof TranslatableContents,
                "Teaching preview text must remain client-localizable"
        );
        TranslatableContents translated =
                (TranslatableContents) component.getContents();
        helper.assertValueEqual(
                translated.getKey(),
                expected,
                "Teaching preview must use the expected translation key"
        );
    }

    private static int countItem(ServerPlayer player, Item item) {
        int count = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
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
