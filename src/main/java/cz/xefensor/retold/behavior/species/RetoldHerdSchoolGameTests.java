package cz.xefensor.retold.behavior.species;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.control.RetoldAiControlMode;
import cz.xefensor.retold.behavior.control.RetoldAiControlOwner;
import cz.xefensor.retold.behavior.home.RetoldAnimalHomeMemory;
import cz.xefensor.retold.behavior.home.RetoldAnimalHomeType;
import cz.xefensor.retold.behavior.home.RetoldAnimalHomes;
import cz.xefensor.retold.behavior.home.RetoldAnimalSocialGroups;
import cz.xefensor.retold.behavior.profiles.RetoldMobStates;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.BuiltinTestFunctions;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.Path;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.List;
import java.util.function.Consumer;

public final class RetoldHerdSchoolGameTests {
    private static final Identifier EMPTY_STRUCTURE =
            Identifier.withDefaultNamespace("empty");

    private RetoldHerdSchoolGameTests() {
    }

    public static void register(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment =
                event.registerEnvironment(
                        id("isolated_herd_school"),
                        new TestEnvironmentDefinition.AllOf()
                );

        registerTest(
                event,
                environment,
                "herd_school_land_groups_share_ranges",
                RetoldHerdSchoolGameTests::confirmedLandHerdsShareRangesBySocialGroup
        );
        registerTest(
                event,
                environment,
                "herd_school_fish_use_species_paths",
                RetoldHerdSchoolGameTests::fishSchoolBySpeciesWithAquaticPaths
        );
        registerTest(
                event,
                environment,
                "herd_school_squid_panic_is_species_local",
                RetoldHerdSchoolGameTests::squidSharePanicOnlyWithTheirSpecies
        );
    }

    private static void confirmedLandHerdsShareRangesBySocialGroup(
            GameTestHelper helper
    ) {
        ServerLevel level = helper.getLevel();
        var cow = helper.spawn(EntityTypes.COW, 2, 2, 2);
        var mooshroom = helper.spawn(EntityTypes.MOOSHROOM, 3, 2, 2);
        var sheep = helper.spawn(EntityTypes.SHEEP, 4, 2, 2);
        var horse = helper.spawn(EntityTypes.HORSE, 2, 2, 4);
        var donkey = helper.spawn(EntityTypes.DONKEY, 3, 2, 4);
        var mule = helper.spawn(EntityTypes.MULE, 4, 2, 4);
        var llama = helper.spawn(EntityTypes.LLAMA, 2, 2, 6);
        var traderLlama = helper.spawn(EntityTypes.TRADER_LLAMA, 3, 2, 6);
        long gameTime = level.getGameTime();

        try {
            helper.assertTrue(
                    RetoldAnimalSocialGroups.canShareHomeOrRange(cow, mooshroom),
                    "Cows and mooshrooms must share the bovine herd group"
            );
            helper.assertFalse(
                    RetoldAnimalSocialGroups.canShareHomeOrRange(cow, sheep),
                    "Other ordinary herds must remain species-specific"
            );
            helper.assertTrue(
                    RetoldAnimalSocialGroups.canShareHomeOrRange(horse, donkey)
                            && RetoldAnimalSocialGroups.canShareHomeOrRange(horse, mule),
                    "Horses, donkeys, and mules must share the equine group"
            );
            helper.assertTrue(
                    RetoldAnimalSocialGroups.canShareHomeOrRange(llama, traderLlama),
                    "Llamas and trader llamas must share the llama group"
            );

            RetoldAnimalHomeMemory bovineRange = RetoldAnimalHomes.getOrCreatePackHome(
                    level,
                    cow,
                    List.of(mooshroom),
                    cow.blockPosition(),
                    gameTime
            );
            helper.assertTrue(
                    bovineRange != null
                            && bovineRange.type() == RetoldAnimalHomeType.HERD_RANGE
                            && RetoldAnimalHomes.hasSameValidHomeAs(
                            level,
                            cow,
                            bovineRange
                    )
                            && RetoldAnimalHomes.hasSameValidHomeAs(
                            level,
                            mooshroom,
                            bovineRange
                    )
                            && !RetoldAnimalHomes.hasSameValidHomeAs(
                            level,
                            sheep,
                            bovineRange
                    ),
                    "A confirmed mixed bovine herd must receive one shared persisted range"
            );
            helper.succeed();
        } finally {
            cleanup(cow, mooshroom, sheep, horse, donkey, mule, llama, traderLlama);
        }
    }

    private static void fishSchoolBySpeciesWithAquaticPaths(
            GameTestHelper helper
    ) {
        buildWaterArena(helper);
        ServerLevel level = helper.getLevel();
        var isolatedCod = helper.spawn(EntityTypes.COD, 2, 3, 3);
        var firstCod = helper.spawn(EntityTypes.COD, 10, 3, 2);
        var secondCod = helper.spawn(EntityTypes.COD, 10, 3, 4);
        var salmon = helper.spawn(EntityTypes.SALMON, 10, 3, 3);

        helper.startSequence()
                .thenIdle(1)
                .thenWaitUntil(() -> assertFishSchool(
                        helper,
                        level,
                        isolatedCod,
                        firstCod,
                        secondCod,
                        salmon
                ))
                .thenExecute(() -> cleanup(isolatedCod, firstCod, secondCod, salmon))
                .thenSucceed();
    }

    private static void assertFishSchool(
            GameTestHelper helper,
            ServerLevel level,
            PathfinderMob isolatedCod,
            PathfinderMob firstCod,
            PathfinderMob secondCod,
            PathfinderMob salmon
    ) {
        long gameTime = level.getGameTime();

        helper.assertTrue(
                RetoldAnimalSocialGroups.canSchoolWith(isolatedCod, firstCod)
                        && RetoldAnimalSocialGroups.canSchoolWith(isolatedCod, secondCod),
                "Fish of one species must be compatible school members"
        );
        helper.assertFalse(
                RetoldAnimalSocialGroups.canSchoolWith(isolatedCod, salmon),
                "Different fish species must not merge into one school"
        );
        boolean started = RetoldAquaticSchoolEvents.tick(
                level,
                isolatedCod,
                gameTime
        );
        Path path = isolatedCod.getNavigation().getPath();
        helper.assertTrue(
                started
                        && RetoldAiControl.isControlledAsBy(
                        isolatedCod,
                        RetoldAiControlMode.REGROUP,
                        RetoldAiControlOwner.AQUATIC_SCHOOL
                )
                        && path != null
                        && path.canReach(),
                "School cohesion must own REGROUP and use a reachable aquatic path; started="
                        + started
                        + ", mode="
                        + RetoldAiControl.getMode(isolatedCod)
                        + ", owner="
                        + RetoldAiControl.getOwner(isolatedCod)
                        + ", path="
                        + path
                        + ", position="
                        + isolatedCod.position()
                        + ", firstCod="
                        + firstCod.position()
                        + ", secondCod="
                        + secondCod.position()
                        + ", water="
                        + isolatedCod.isInWater()
        );
        helper.assertFalse(
                RetoldAquaticSchoolEvents.tick(level, salmon, gameTime + 1L),
                "A Salmon must not join a nearby Cod school"
        );
        helper.assertFalse(
                RetoldAiControl.isControlledBy(
                        salmon,
                        RetoldAiControlOwner.AQUATIC_SCHOOL
                ),
                "Cross-species proximity must not claim school control"
        );
    }

    private static void squidSharePanicOnlyWithTheirSpecies(
            GameTestHelper helper
    ) {
        buildWaterArena(helper);
        ServerLevel level = helper.getLevel();
        var frightenedSquid = helper.spawn(EntityTypes.SQUID, 5, 3, 5);
        var nearbySquid = helper.spawn(EntityTypes.SQUID, 7, 3, 5);
        var glowSquid = helper.spawn(EntityTypes.GLOW_SQUID, 6, 3, 7);

        helper.startSequence()
                .thenIdle(1)
                .thenExecute(() -> {
                    float healthBeforeDamage = frightenedSquid.getHealth();
                    frightenedSquid.hurtServer(
                                level,
                                level.damageSources().generic(),
                                1.0F
                        );
                    helper.assertTrue(
                            frightenedSquid.getHealth() < healthBeforeDamage,
                            "The source Squid must take real environmental damage"
                    );
                })
                .thenWaitUntil(() -> helper.assertTrue(
                        RetoldAiControl.isControlledAs(
                                nearbySquid,
                                RetoldAiControlMode.FLEE
                        ),
                        "A nearby Squid must copy panic from its own species"
                ))
                .thenExecute(() -> helper.assertFalse(
                        RetoldAiControl.isControlledAs(
                                glowSquid,
                                RetoldAiControlMode.FLEE
                        ),
                        "Glow Squid must not join an ordinary Squid danger group"
                ))
                .thenExecute(() -> cleanup(frightenedSquid, nearbySquid, glowSquid))
                .thenSucceed();
    }

    private static void buildWaterArena(GameTestHelper helper) {
        for (int x = 0; x <= 14; x++) {
            for (int z = 0; z <= 14; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.GLASS);

                for (int y = 2; y <= 6; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.WATER);
                }
            }
        }
    }

    private static void cleanup(PathfinderMob... mobs) {
        for (PathfinderMob mob : mobs) {
            RetoldAiControl.clear(mob);
            RetoldMobStates.remove(mob);
            RetoldAnimalHomes.remove(mob);
            mob.discard();
        }
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
                        100,
                        0,
                        true
                );

        event.registerTest(
                id(name),
                new InlineGameTest(testData, test)
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
