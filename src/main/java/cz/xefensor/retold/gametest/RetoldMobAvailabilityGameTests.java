package cz.xefensor.retold.gametest;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.event.RetoldMobAvailability;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.BuiltinTestFunctions;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;

import java.util.function.Consumer;

public final class RetoldMobAvailabilityGameTests {
    private static final Identifier EMPTY_STRUCTURE =
            Identifier.withDefaultNamespace("empty");
    private static final ResourceKey<LootTable> WARM_OCEAN_RUIN_ARCHAEOLOGY =
            ResourceKey.create(
                    Registries.LOOT_TABLE,
                    Identifier.withDefaultNamespace("archaeology/ocean_ruin_warm")
            );
    private static final int LOOT_SAMPLE_COUNT = 512;

    private RetoldMobAvailabilityGameTests() {
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
                id("sniffers_are_creative_only_but_remain_functional"),
                new InlineGameTest(
                        testData,
                        RetoldMobAvailabilityGameTests::sniffersAreCreativeOnlyButRemainFunctional
                )
        );
        event.registerTest(
                id("endermites_are_creative_only_but_remain_functional"),
                new InlineGameTest(
                        testData,
                        RetoldMobAvailabilityGameTests::endermitesAreCreativeOnlyButRemainFunctional
                )
        );
    }

    private static void sniffersAreCreativeOnlyButRemainFunctional(
            GameTestHelper helper
    ) {
        ServerLevel level = helper.getLevel();
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(net.minecraft.core.BlockPos.ZERO)))
                .withParameter(LootContextParams.THIS_ENTITY, player)
                .withParameter(LootContextParams.TOOL, new ItemStack(Items.BRUSH))
                .create(LootContextParamSets.ARCHAEOLOGY);
        LootTable lootTable = level.getServer()
                .reloadableRegistries()
                .getLootTable(WARM_OCEAN_RUIN_ARCHAEOLOGY);
        boolean sawPreservedRuinReward = false;

        for (long seed = 1L; seed <= LOOT_SAMPLE_COUNT; seed++) {
            for (ItemStack stack : lootTable.getRandomItems(params, seed)) {
                helper.assertFalse(
                        stack.is(Items.SNIFFER_EGG),
                        "Warm ocean ruin archaeology must not provide Sniffer Eggs"
                );

                if (stack.is(Items.ANGLER_POTTERY_SHERD)
                        || stack.is(Items.SHELTER_POTTERY_SHERD)
                        || stack.is(Items.SNORT_POTTERY_SHERD)) {
                    sawPreservedRuinReward = true;
                }
            }
        }

        helper.assertTrue(
                sawPreservedRuinReward,
                "Removing Sniffer Eggs must preserve ordinary warm-ocean-ruin archaeology rewards"
        );

        var sniffer = EntityTypes.SNIFFER.create(level, EntitySpawnReason.COMMAND);
        helper.assertTrue(
                sniffer != null && level.addFreshEntity(sniffer),
                "Sniffers must remain functional when created through commands or Creative"
        );
        sniffer.discard();
        helper.succeed();
    }

    private static void endermitesAreCreativeOnlyButRemainFunctional(
            GameTestHelper helper
    ) {
        ServerLevel level = helper.getLevel();

        helper.assertTrue(
                RetoldMobAvailability.createEndermiteIfAvailable(
                        EntityTypes.ENDERMITE,
                        level,
                        EntitySpawnReason.TRIGGERED
                ) == null,
                "Ender Pearls must not create Endermites"
        );

        var naturalEndermite = EntityTypes.ENDERMITE.create(
                level,
                EntitySpawnReason.COMMAND
        );
        helper.assertTrue(
                naturalEndermite != null,
                "The Endermite entity type must remain available"
        );
        MobSpawnEvent.PositionCheck naturalSpawn =
                new MobSpawnEvent.PositionCheck(
                        naturalEndermite,
                        level,
                        EntitySpawnReason.NATURAL,
                        null
                );
        RetoldMobAvailability.onPositionCheck(naturalSpawn);
        helper.assertValueEqual(
                naturalSpawn.getResult(),
                MobSpawnEvent.PositionCheck.Result.FAIL,
                "Natural Endermite spawning must be rejected"
        );

        var creativeEndermite = EntityTypes.ENDERMITE.create(
                level,
                EntitySpawnReason.COMMAND
        );
        helper.assertTrue(
                creativeEndermite != null && level.addFreshEntity(creativeEndermite),
                "Endermites must remain functional when created through commands or Creative"
        );
        naturalEndermite.discard();
        creativeEndermite.discard();
        helper.succeed();
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
