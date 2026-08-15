package cz.xefensor.retold.behavior.species;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.control.RetoldAiControlMode;
import cz.xefensor.retold.behavior.control.RetoldAiControlOwner;
import cz.xefensor.retold.behavior.home.RetoldAnimalHomeMemory;
import cz.xefensor.retold.behavior.home.RetoldAnimalHomeType;
import cz.xefensor.retold.behavior.home.RetoldAnimalHomes;
import cz.xefensor.retold.behavior.home.RetoldAnimalSocialGroups;
import cz.xefensor.retold.behavior.home.RetoldHerdRangeEvents;
import cz.xefensor.retold.behavior.profiles.RetoldMobStates;
import cz.xefensor.retold.block.AnimalFeederBlockEntity;
import cz.xefensor.retold.registry.RetoldBlocks;

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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
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
                "herd_school_land_ranges_follow_local_food",
                RetoldHerdSchoolGameTests::landRangesRemainSuppliedAndMigrateAfterDepletion
        );
        registerTest(
                event,
                environment,
                "herd_school_aquatic_ranges_follow_local_food",
                RetoldHerdSchoolGameTests::aquaticRangesRemainSuppliedAndMigrateAfterDepletion
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

    private static void landRangesRemainSuppliedAndMigrateAfterDepletion(
            GameTestHelper helper
    ) {
        buildMigrationLandArena(helper);
        ServerLevel level = helper.getLevel();
        var cow = helper.spawn(EntityTypes.COW, 4, 2, 8);
        var mooshroom = helper.spawn(EntityTypes.MOOSHROOM, 5, 2, 8);
        BlockPos feederPos = new BlockPos(6, 2, 8);
        helper.setBlock(feederPos, RetoldBlocks.ANIMAL_FEEDER.get());
        AnimalFeederBlockEntity feeder = feederAt(helper, feederPos);
        feeder.setItem(0, new ItemStack(Items.WHEAT, 8));
        long gameTime = level.getGameTime();
        RetoldAnimalHomeMemory originalRange = RetoldAnimalHomes.getOrCreatePackHome(
                level,
                cow,
                List.of(mooshroom),
                cow.blockPosition(),
                gameTime
        );

        try {
            helper.assertTrue(
                    originalRange != null
                            && originalRange.type() == RetoldAnimalHomeType.HERD_RANGE,
                    "The test herd must begin with one persisted grazing range"
            );
            RetoldMobStates.getOrCreate(cow, gameTime).setHunger(60);
            RetoldHerdRangeEvents.tick(level, cow, gameTime);
            RetoldAnimalHomeMemory suppliedRange = RetoldAnimalHomes.get(cow);
            helper.assertTrue(
                    suppliedRange != null
                            && suppliedRange.pos().equals(originalRange.pos())
                            && RetoldAnimalHomes.hasSameValidHomeAs(
                            level,
                            mooshroom,
                            suppliedRange
                    ),
                    "A compatible stocked feeder must hold the herd's existing range"
            );

            helper.setBlock(feederPos, Blocks.AIR);
            RetoldAiControl.clear(cow);
            long migrationTime = gameTime + 100L;
            RetoldHerdRangeEvents.tick(level, cow, migrationTime);
            RetoldAnimalHomeMemory migratedRange = RetoldAnimalHomes.get(cow);
            helper.assertTrue(
                    migratedRange != null
                            && !migratedRange.pos().equals(originalRange.pos())
                            && RetoldAnimalHomes.hasSameValidHomeAs(
                            level,
                            mooshroom,
                            migratedRange
                    ),
                    "A hungry herd must shift its shared range after local supply is removed"
            );
            helper.assertTrue(
                    RetoldAiControl.isControlledAsBy(
                            cow,
                            RetoldAiControlMode.REGROUP,
                            RetoldAiControlOwner.REGROUP
                    )
                            && "migrate_depleted_range".equals(
                            RetoldAiControl.getReason(cow)
                    ),
                    "Land migration must use reasoned shared regroup ownership"
            );
            helper.succeed();
        } finally {
            cleanup(cow, mooshroom);
        }
    }

    private static void aquaticRangesRemainSuppliedAndMigrateAfterDepletion(
            GameTestHelper helper
    ) {
        buildMigrationWaterArena(helper);
        placeAquaticForagePatch(helper, 2, 6, 6, 10);
        placeAquaticForagePatch(helper, 17, 23, 5, 11);
        ServerLevel level = helper.getLevel();
        var leader = helper.spawn(EntityTypes.COD, 4, 3, 8);
        var member = helper.spawn(EntityTypes.COD, 5, 3, 8);
        long gameTime = level.getGameTime();
        RetoldAnimalHomeMemory originalRange = RetoldAnimalHomes.getOrCreatePackHome(
                level,
                leader,
                List.of(member),
                leader.blockPosition(),
                gameTime
        );

        helper.startSequence()
                .thenIdle(1)
                .thenExecute(() -> {
                    try {
                        helper.assertTrue(
                                originalRange != null
                                        && originalRange.type()
                                        == RetoldAnimalHomeType.AQUATIC_SCHOOL_RANGE,
                                "The test school must begin with one persisted aquatic range"
                        );
                        long readyTime = level.getGameTime();
                        RetoldMobStates.getOrCreate(leader, readyTime).setHunger(60);
                        RetoldAquaticSchoolEvents.tick(level, leader, readyTime);
                        helper.assertTrue(
                                RetoldAnimalHomes.get(leader).pos().equals(originalRange.pos()),
                                "Nearby edible aquatic plants must hold the school's existing range"
                        );

                        placeAquaticWaterPatch(helper, 2, 6, 6, 10);
                        RetoldAiControl.clear(leader);
                        long migrationTime = readyTime + 100L;
                        helper.assertTrue(
                                RetoldAquaticSchoolEvents.tick(level, leader, migrationTime),
                                "A depleted hungry school must begin range migration"
                        );
                        RetoldAnimalHomeMemory migratedRange = RetoldAnimalHomes.get(leader);
                        Path path = leader.getNavigation().getPath();
                        helper.assertTrue(
                                migratedRange != null
                                        && migratedRange.type()
                                        == RetoldAnimalHomeType.AQUATIC_SCHOOL_RANGE
                                        && !migratedRange.pos().equals(originalRange.pos())
                                        && RetoldAnimalHomes.hasSameValidHomeAs(
                                        level,
                                        member,
                                        migratedRange
                                ),
                                "Aquatic migration must shift the shared persisted school range"
                        );
                        helper.assertTrue(
                                RetoldAiControl.isControlledAsBy(
                                        leader,
                                        RetoldAiControlMode.REGROUP,
                                        RetoldAiControlOwner.AQUATIC_SCHOOL
                                )
                                        && "migrate_depleted_aquatic_range".equals(
                                        RetoldAiControl.getReason(leader)
                                        )
                                        && path != null
                                        && path.canReach(),
                                "Aquatic migration must use owned regroup movement and a reachable water path; "
                                        + "mode="
                                        + RetoldAiControl.getMode(leader)
                                        + ", owner="
                                        + RetoldAiControl.getOwner(leader)
                                        + ", reason="
                                        + RetoldAiControl.getReason(leader)
                                        + ", path="
                                        + path
                                        + ", range="
                                        + migratedRange.pos()
                                        + ", position="
                                        + leader.position()
                        );
                    } finally {
                        cleanup(leader, member);
                    }
                })
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

    private static void buildMigrationLandArena(GameTestHelper helper) {
        for (int x = 0; x <= 32; x++) {
            for (int z = 0; z <= 16; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
            }
        }

        for (int x = 17; x <= 23; x++) {
            for (int z = 5; z <= 11; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.GRASS_BLOCK);
            }
        }
    }

    private static void buildMigrationWaterArena(GameTestHelper helper) {
        for (int x = 0; x <= 32; x++) {
            for (int z = 0; z <= 16; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.GLASS);

                for (int y = 2; y <= 6; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.WATER);
                }
            }
        }
    }

    private static void placeAquaticForagePatch(
            GameTestHelper helper,
            int minX,
            int maxX,
            int minZ,
            int maxZ
    ) {
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                helper.setBlock(new BlockPos(x, 2, z), Blocks.SEAGRASS);
            }
        }
    }

    private static void placeAquaticWaterPatch(
            GameTestHelper helper,
            int minX,
            int maxX,
            int minZ,
            int maxZ
    ) {
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                helper.setBlock(new BlockPos(x, 2, z), Blocks.WATER);
            }
        }
    }

    private static AnimalFeederBlockEntity feederAt(
            GameTestHelper helper,
            BlockPos relativePos
    ) {
        BlockEntity blockEntity = helper.getLevel().getBlockEntity(
                helper.absolutePos(relativePos)
        );
        helper.assertTrue(
                blockEntity instanceof AnimalFeederBlockEntity,
                "Animal Feeder placement must create its block entity"
        );
        return (AnimalFeederBlockEntity) blockEntity;
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
