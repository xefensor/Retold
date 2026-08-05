package cz.xefensor.retold.villager;

import cz.xefensor.retold.Retold;

import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.BuiltinTestFunctions;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.function.Consumer;

public final class RetoldVillagerTradeRefreshGameTests {
    private static final Identifier EMPTY_STRUCTURE =
            Identifier.withDefaultNamespace("empty");

    private RetoldVillagerTradeRefreshGameTests() {
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
                id("villager_trades_refresh_once_per_day_without_rerolling"),
                new InlineGameTest(
                        testData,
                        RetoldVillagerTradeRefreshGameTests::villagerTradesRefreshOncePerDayWithoutRerolling
                )
        );
    }

    private static void villagerTradesRefreshOncePerDayWithoutRerolling(
            GameTestHelper helper
    ) {
        var villager = helper.spawn(EntityTypes.VILLAGER, 1, 2, 1);
        villager.setVillagerData(
                villager.getVillagerData().withProfession(
                        helper.getLevel().registryAccess(),
                        VillagerProfession.FARMER
                )
        );

        MerchantOffer offer = new MerchantOffer(
                new ItemCost(Items.EMERALD),
                new ItemStack(Items.BREAD),
                3,
                1,
                0.05F
        );
        MerchantOffers offers = new MerchantOffers();
        offers.add(offer);
        villager.setOffers(offers);

        try {
            helper.assertFalse(
                    RetoldVillagerTradeRefresh.refreshForDay(villager, 10L),
                    "The first observation must establish the current day without inventing a refresh"
            );

            offer.setToOutOfStock();
            helper.assertFalse(
                    RetoldVillagerTradeRefresh.refreshForDay(villager, 10L),
                    "A villager must not refresh twice during the same day"
            );
            helper.assertTrue(
                    offer.isOutOfStock(),
                    "A same-day check must preserve exhausted stock"
            );

            helper.assertTrue(
                    RetoldVillagerTradeRefresh.refreshForDay(villager, 11L),
                    "A new day must refresh a professional adult villager"
            );
            helper.assertValueEqual(
                    offer.getUses(),
                    0,
                    "The daily refresh must restore trade uses"
            );
            helper.assertTrue(
                    villager.getOffers().size() == 1
                            && villager.getOffers().getFirst() == offer,
                    "The daily refresh must retain the villager's existing offer identity"
            );

            offer.setToOutOfStock();
            helper.assertFalse(
                    RetoldVillagerTradeRefresh.refreshForDay(villager, 11L),
                    "The daily refresh marker must prevent another refresh that day"
            );
            helper.assertTrue(
                    offer.isOutOfStock(),
                    "Stock used after the daily refresh must stay exhausted until a later day"
            );
            helper.succeed();
        } finally {
            villager.discard();
        }
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
