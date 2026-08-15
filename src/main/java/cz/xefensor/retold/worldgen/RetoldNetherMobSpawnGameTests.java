package cz.xefensor.retold.worldgen;

import cz.xefensor.retold.Retold;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.BuiltinTestFunctions;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.function.Consumer;

public final class RetoldNetherMobSpawnGameTests {
    private static final Identifier EMPTY_STRUCTURE =
            Identifier.withDefaultNamespace("empty");

    private RetoldNetherMobSpawnGameTests() {
    }

    public static void register(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
                id("isolated_nether_mob_spawn_contract"),
                new TestEnvironmentDefinition.AllOf()
        );

        registerTest(
                event,
                environment,
                "wither_skeletons_spawn_rarely_in_soul_sand_valleys",
                RetoldNetherMobSpawnGameTests::witherSkeletonsSpawnRarelyInSoulSandValleys
        );
    }

    private static void witherSkeletonsSpawnRarelyInSoulSandValleys(
            GameTestHelper helper
    ) {
        var biomes = helper.getLevel()
                .registryAccess()
                .lookupOrThrow(Registries.BIOME);
        Biome soulSandValley = biomes.getValueOrThrow(Biomes.SOUL_SAND_VALLEY);
        Biome netherWastes = biomes.getValueOrThrow(Biomes.NETHER_WASTES);

        var valleyEntries = soulSandValley
                .getMobSettings()
                .getMobs(MobCategory.MONSTER)
                .unwrap()
                .stream()
                .filter(entry -> entry.value().type() == EntityTypes.WITHER_SKELETON)
                .toList();

        helper.assertValueEqual(
                valleyEntries.size(),
                1,
                "Soul Sand Valleys must have exactly one Wither Skeleton biome-spawn entry"
        );

        var rareEntry = valleyEntries.getFirst();
        MobSpawnSettings.SpawnerData spawn = rareEntry.value();

        helper.assertTrue(
                rareEntry.weight() == 1
                        && spawn.minCount() == 1
                        && spawn.maxCount() == 1,
                "The Soul Sand Valley entry must use the smallest positive weight and a solitary pack"
        );
        helper.assertTrue(
                netherWastes.getMobSettings()
                        .getMobs(MobCategory.MONSTER)
                        .unwrap()
                        .stream()
                        .noneMatch(entry -> entry.value().type() == EntityTypes.WITHER_SKELETON),
                "The rare biome spawn must not leak into ordinary Nether biomes"
        );
        helper.succeed();
    }

    private static void registerTest(
            RegisterGameTestsEvent event,
            Holder<TestEnvironmentDefinition<?>> environment,
            String path,
            Consumer<GameTestHelper> test
    ) {
        event.registerTest(
                id(path),
                new InlineGameTest(
                        new TestData<>(environment, EMPTY_STRUCTURE, 40, 0, true),
                        test
                )
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
            this.test.accept(helper);
        }
    }
}
