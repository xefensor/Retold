package cz.xefensor.retold.worldgen;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.BuiltinTestFunctions;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.function.Consumer;

public final class RetoldStructureRemovalGameTests {
    private static final Identifier EMPTY_STRUCTURE =
            Identifier.withDefaultNamespace("empty");

    private RetoldStructureRemovalGameTests() {
    }

    public static void register(
            RegisterGameTestsEvent event,
            Holder<TestEnvironmentDefinition<?>> environment
    ) {
        TestData<Holder<TestEnvironmentDefinition<?>>> testData =
                new TestData<>(environment, EMPTY_STRUCTURE, 40, 0, true);

        event.registerTest(
                id("trial_chambers_remain_registered_without_generation_biomes"),
                new InlineGameTest(
                        testData,
                        RetoldStructureRemovalGameTests::trialChambersHaveNoGenerationBiomes
                )
        );
        event.registerTest(
                id("ancient_cities_remain_registered_without_generation_biomes"),
                new InlineGameTest(
                        testData,
                        RetoldStructureRemovalGameTests::ancientCitiesHaveNoGenerationBiomes
                )
        );
        event.registerTest(
                id("overworld_biome_source_excludes_deep_dark"),
                new InlineGameTest(
                        testData,
                        RetoldStructureRemovalGameTests::overworldBiomeSourceExcludesDeepDark
                )
        );
    }

    private static void trialChambersHaveNoGenerationBiomes(GameTestHelper helper) {
        assertStructureHasNoGenerationBiomes(
                helper,
                BuiltinStructures.TRIAL_CHAMBERS,
                "Trial Chambers"
        );
    }

    private static void ancientCitiesHaveNoGenerationBiomes(GameTestHelper helper) {
        assertStructureHasNoGenerationBiomes(
                helper,
                BuiltinStructures.ANCIENT_CITY,
                "Ancient Cities"
        );
    }

    private static void assertStructureHasNoGenerationBiomes(
            GameTestHelper helper,
            ResourceKey<Structure> structureKey,
            String structureName
    ) {
        Registry<Structure> structures = helper.getLevel()
                .registryAccess()
                .lookupOrThrow(Registries.STRUCTURE);
        Structure structure = structures.getValueOrThrow(structureKey);

        helper.assertTrue(
                structure.biomes().isBound(),
                structureName + " must remain registered with a resolved biome tag"
        );
        helper.assertValueEqual(
                structure.biomes().size(),
                0,
                structureName + " must have no eligible generation biomes"
        );
        helper.succeed();
    }

    private static void overworldBiomeSourceExcludesDeepDark(GameTestHelper helper) {
        Registry<MultiNoiseBiomeSourceParameterList> parameterLists = helper.getLevel()
                .registryAccess()
                .lookupOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST);
        MultiNoiseBiomeSourceParameterList overworld = parameterLists.getValueOrThrow(
                MultiNoiseBiomeSourceParameterLists.OVERWORLD
        );

        helper.assertTrue(
                overworld.parameters()
                        .values()
                        .stream()
                        .noneMatch(entry -> entry.getSecond().is(Biomes.DEEP_DARK)),
                "The default Overworld biome source must not select the Deep Dark"
        );
        helper.succeed();
    }

    private static Identifier id(String path) {
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
