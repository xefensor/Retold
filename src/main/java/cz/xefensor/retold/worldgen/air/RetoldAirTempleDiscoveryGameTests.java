package cz.xefensor.retold.worldgen.air;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.stage.RetoldStageManager;
import cz.xefensor.retold.stage.RetoldStageRuntime;
import cz.xefensor.retold.stage.RetoldWorldData;
import cz.xefensor.retold.stage.RetoldWorldStage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.BuiltinTestFunctions;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.MapDecorations;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.Objects;
import java.util.function.Consumer;

public final class RetoldAirTempleDiscoveryGameTests {
    private static final Identifier EMPTY_STRUCTURE =
            Identifier.withDefaultNamespace("empty");

    private RetoldAirTempleDiscoveryGameTests() {
    }

    public static void register(
            RegisterGameTestsEvent event,
            Holder<TestEnvironmentDefinition<?>> environment
    ) {
        TestData<Holder<TestEnvironmentDefinition<?>>> testData =
                new TestData<>(environment, EMPTY_STRUCTURE, 100, 0, true);

        event.registerTest(
                id("air_temple_map_trade_is_stage_gated_and_unique"),
                new InlineGameTest(
                        testData,
                        RetoldAirTempleDiscoveryGameTests::mapTradeIsStageGatedAndUnique
                )
        );
    }

    private static void mapTradeIsStageGatedAndUnique(
            GameTestHelper helper
    ) {
        ServerLevel level = helper.getLevel();
        RetoldWorldData data = RetoldWorldData.get(level);
        RetoldWorldStage originalSavedStage = data.getStage();
        RetoldWorldStage originalRuntimeStage =
                RetoldStageRuntime.getOverworldStage();
        Villager cartographer = createCartographer(level);
        MerchantOffer existingOffer = cartographer.getOffers().getFirst();
        BlockPos templePos = helper.absolutePos(new BlockPos(8, 2, 8));

        try {
            data.setStage(RetoldWorldStage.STAGE_1);
            RetoldStageRuntime.setOverworldStage(RetoldWorldStage.STAGE_1);

            helper.assertFalse(
                    AirTempleDiscoveryEvents.ensureExplorerMapOffer(
                            level,
                            cartographer,
                            templePos
                    ),
                    "A Stage 1 cartographer must not sell the Air Temple map"
            );
            helper.assertValueEqual(
                    cartographer.getOffers().size(),
                    1,
                    "Stage 1 must leave the cartographer's offers unchanged"
            );
            helper.assertTrue(
                    cartographer.getOffers().getFirst() == existingOffer,
                    "Stage 1 must preserve the existing saved offer"
            );

            RetoldStageManager.setStage(level, RetoldWorldStage.STAGE_2);
            helper.assertTrue(
                    AirTempleDiscoveryEvents.ensureExplorerMapOffer(
                            level,
                            cartographer,
                            templePos
                    ),
                    "A journeyman cartographer must gain the map in Stage 2"
            );
            helper.assertValueEqual(
                    cartographer.getOffers().size(),
                    2,
                    "The cartographer must retain its offer and gain one map"
            );
            helper.assertTrue(
                    cartographer.getOffers().getFirst() == existingOffer,
                    "Adding the map must preserve the existing saved offer"
            );

            MerchantOffer offer = cartographer.getOffers().getLast();
            assertOffer(helper, offer, templePos);

            helper.assertFalse(
                    AirTempleDiscoveryEvents.ensureExplorerMapOffer(
                            level,
                            cartographer,
                            templePos
                    ),
                    "Repeated interaction must not add another map offer"
            );
            helper.assertValueEqual(
                    cartographer.getOffers().size(),
                    2,
                    "The Air Temple map offer must remain unique"
            );
            helper.succeed();
        } finally {
            cartographer.discard();
            data.setStage(originalSavedStage);
            RetoldStageRuntime.setOverworldStage(originalRuntimeStage);
        }
    }

    private static Villager createCartographer(ServerLevel level) {
        Villager villager = Objects.requireNonNull(
                EntityTypes.VILLAGER.create(
                        level,
                        EntitySpawnReason.COMMAND
                )
        );
        villager.setVillagerData(
                villager.getVillagerData()
                        .withProfession(
                                level.registryAccess(),
                                VillagerProfession.CARTOGRAPHER
                        )
                        .withLevel(3)
        );
        MerchantOffers offers = new MerchantOffers();
        offers.add(new MerchantOffer(
                new ItemCost(Items.PAPER, 24),
                new ItemStack(Items.EMERALD),
                16,
                2,
                0.05F
        ));
        villager.setOffers(offers);
        return villager;
    }

    private static void assertOffer(
            GameTestHelper helper,
            MerchantOffer offer,
            BlockPos templePos
    ) {
        helper.assertValueEqual(
                offer.getBaseCostA().getCount(),
                12,
                "The Air Temple map must cost 12 emeralds"
        );
        helper.assertTrue(
                offer.getBaseCostA().is(Items.EMERALD),
                "The primary cost must be emeralds"
        );
        helper.assertTrue(
                offer.getCostB().is(Items.COMPASS),
                "The secondary cost must be a compass"
        );

        ItemStack result = offer.getResult();
        MapDecorations decorations = result.getOrDefault(
                DataComponents.MAP_DECORATIONS,
                MapDecorations.EMPTY
        );
        helper.assertTrue(
                result.is(Items.FILLED_MAP),
                "The cartographer must sell a filled map"
        );
        helper.assertTrue(
                decorations.decorations().containsKey(
                        AirTempleDiscoveryEvents.MAP_DECORATION_KEY
                ),
                "The map must carry the exact Air Temple marker"
        );
        MapDecorations.Entry marker = Objects.requireNonNull(
                decorations.decorations().get(
                        AirTempleDiscoveryEvents.MAP_DECORATION_KEY
                )
        );
        helper.assertValueEqual(
                marker.type(),
                MapDecorationTypes.TARGET_X,
                "The Air Temple marker must use the exact-target X"
        );
        helper.assertValueEqual(
                marker.x(),
                (double) templePos.getX(),
                "The marker X coordinate must match the Air Temple"
        );
        helper.assertValueEqual(
                marker.z(),
                (double) templePos.getZ(),
                "The marker Z coordinate must match the Air Temple"
        );
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Retold.MODID, path);
    }

    private static final class InlineGameTest
            extends FunctionGameTestInstance {
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
