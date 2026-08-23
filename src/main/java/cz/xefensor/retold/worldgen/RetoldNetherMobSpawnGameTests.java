package cz.xefensor.retold.worldgen;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.mixin.NetherFossilPieceInvoker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.BuiltinTestFunctions;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.structures.NetherFossilPieces;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class RetoldNetherMobSpawnGameTests {
    private static final Identifier EMPTY_STRUCTURE =
            Identifier.withDefaultNamespace("empty");
    private static final Identifier NETHER_FOSSIL_TEMPLATE =
            Identifier.withDefaultNamespace("nether_fossils/fossil_1");

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
        registerTest(
                event,
                environment,
                "nether_fossils_omit_dried_ghasts",
                RetoldNetherMobSpawnGameTests::netherFossilsOmitDriedGhasts
        );
        registerTest(
                event,
                environment,
                "nether_forests_use_sparse_desert_vegetation",
                RetoldNetherMobSpawnGameTests::netherForestsUseSparseDesertVegetation
        );
    }

    private static void netherForestsUseSparseDesertVegetation(GameTestHelper helper) {
        var biomes = helper.getLevel()
                .registryAccess()
                .lookupOrThrow(Registries.BIOME);
        Set<Identifier> crimsonFeatures = featureIds(
                biomes.getValueOrThrow(Biomes.CRIMSON_FOREST)
        );
        Set<Identifier> warpedFeatures = featureIds(
                biomes.getValueOrThrow(Biomes.WARPED_FOREST)
        );

        assertFeaturesAbsent(
                helper,
                crimsonFeatures,
                "Crimson Forest",
                List.of(
                        vanillaId("crimson_fungi"),
                        vanillaId("crimson_forest_vegetation"),
                        vanillaId("weeping_vines")
                )
        );
        assertFeaturesPresent(
                helper,
                crimsonFeatures,
                "Crimson Forest",
                List.of(
                        id("sparse_crimson_fungi"),
                        id("sparse_crimson_scrub"),
                        id("sparse_weeping_vines"),
                        vanillaId("ore_quartz_nether"),
                        vanillaId("spring_lava")
                )
        );

        assertFeaturesAbsent(
                helper,
                warpedFeatures,
                "Warped Forest",
                List.of(
                        vanillaId("warped_fungi"),
                        vanillaId("warped_forest_vegetation"),
                        vanillaId("nether_sprouts"),
                        vanillaId("twisting_vines")
                )
        );
        assertFeaturesPresent(
                helper,
                warpedFeatures,
                "Warped Forest",
                List.of(
                        id("sparse_warped_fungi"),
                        id("sparse_warped_scrub"),
                        id("sparse_nether_sprouts"),
                        id("sparse_twisting_vines"),
                        vanillaId("ore_quartz_nether"),
                        vanillaId("spring_lava")
                )
        );

        Set<Identifier> sparseForestFeatures = Set.of(
                id("sparse_crimson_fungi"),
                id("sparse_crimson_scrub"),
                id("sparse_weeping_vines"),
                id("sparse_warped_fungi"),
                id("sparse_warped_scrub"),
                id("sparse_nether_sprouts"),
                id("sparse_twisting_vines")
        );
        for (Biome unchangedBiome : List.of(
                biomes.getValueOrThrow(Biomes.NETHER_WASTES),
                biomes.getValueOrThrow(Biomes.SOUL_SAND_VALLEY),
                biomes.getValueOrThrow(Biomes.BASALT_DELTAS)
        )) {
            helper.assertTrue(
                    Collections.disjoint(featureIds(unchangedBiome), sparseForestFeatures),
                    "Sparse forest vegetation must not leak into other Nether biomes"
            );
        }
        helper.succeed();
    }

    private static Set<Identifier> featureIds(Biome biome) {
        return biome.getGenerationSettings()
                .features()
                .stream()
                .flatMap(holderSet -> holderSet.stream())
                .flatMap(feature -> feature.unwrapKey().stream())
                .map(key -> key.identifier())
                .collect(Collectors.toSet());
    }

    private static void assertFeaturesPresent(
            GameTestHelper helper,
            Set<Identifier> actual,
            String biomeName,
            List<Identifier> expected
    ) {
        for (Identifier feature : expected) {
            helper.assertTrue(
                    actual.contains(feature),
                    biomeName + " must retain or add " + feature
            );
        }
    }

    private static void assertFeaturesAbsent(
            GameTestHelper helper,
            Set<Identifier> actual,
            String biomeName,
            List<Identifier> removed
    ) {
        for (Identifier feature : removed) {
            helper.assertTrue(
                    !actual.contains(feature),
                    biomeName + " must replace dense vegetation feature " + feature
            );
        }
    }

    private static void netherFossilsOmitDriedGhasts(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos candidate = findDriedGhastCandidate(helper, level);
        BoundingBox candidateBox = new BoundingBox(
                candidate.getX(),
                candidate.getY(),
                candidate.getZ(),
                candidate.getX(),
                candidate.getY(),
                candidate.getZ()
        );
        NetherFossilPieces.NetherFossilPiece piece =
                new NetherFossilPieces.NetherFossilPiece(
                        level.getStructureManager(),
                        NETHER_FOSSIL_TEMPLATE,
                        candidate,
                        Rotation.NONE
                );

        level.setBlock(candidate, Blocks.AIR.defaultBlockState(), 3);
        ((NetherFossilPieceInvoker) piece).retold$invokePlaceDriedGhast(
                level,
                RandomSource.create(0L),
                candidateBox,
                candidateBox
        );

        helper.assertTrue(
                level.getBlockState(candidate).isAir(),
                "Nether fossil generation must not place a Dried Ghast"
        );

        BlockPos retainedBlock = candidate.above();
        level.setBlock(retainedBlock, Blocks.DRIED_GHAST.defaultBlockState(), 3);
        helper.assertTrue(
                level.getBlockState(retainedBlock).is(Blocks.DRIED_GHAST),
                "Dried Ghasts must remain available outside natural fossil generation"
        );
        helper.succeed();
    }

    private static BlockPos findDriedGhastCandidate(
            GameTestHelper helper,
            ServerLevel level
    ) {
        for (int offset = 0; offset < 64; offset++) {
            BlockPos candidate = helper.absolutePos(new BlockPos(1, 2 + offset, 1));
            RandomSource positionalRandom = RandomSource
                    .createThreadLocalInstance(level.getSeed())
                    .forkPositional()
                    .at(candidate);

            if (positionalRandom.nextFloat() < 0.5F) {
                return candidate;
            }
        }

        helper.fail("Could not find a deterministic Dried Ghast placement candidate");
        return BlockPos.ZERO;
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

    private static Identifier vanillaId(String path) {
        return Identifier.withDefaultNamespace(path);
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
