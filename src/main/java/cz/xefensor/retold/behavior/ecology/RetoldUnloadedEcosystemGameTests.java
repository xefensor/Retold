package cz.xefensor.retold.behavior.ecology;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.behavior.breeding.RetoldAnimalBreeding;
import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.food.RetoldAnimalFeederBehavior;
import cz.xefensor.retold.behavior.food.RetoldRangeForage;
import cz.xefensor.retold.behavior.core.RetoldMobGriefing;
import cz.xefensor.retold.behavior.home.RetoldAnimalHomeMemory;
import cz.xefensor.retold.behavior.home.RetoldAnimalHomeType;
import cz.xefensor.retold.behavior.home.RetoldAnimalHomes;
import cz.xefensor.retold.behavior.hunting.RetoldPreyTargeting;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;
import cz.xefensor.retold.behavior.profiles.RetoldMobState;
import cz.xefensor.retold.behavior.profiles.RetoldMobStates;
import cz.xefensor.retold.behavior.species.RetoldSlimeItemStorage;
import cz.xefensor.retold.block.AnimalFeederBlockEntity;
import cz.xefensor.retold.registry.RetoldBlocks;
import cz.xefensor.retold.villager.RetoldUnloadedFarmerProduction;
import cz.xefensor.retold.villager.RetoldVillageContainerOwnership;
import cz.xefensor.retold.villager.RetoldVillageCropOwnership;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.BuiltinTestFunctions;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class RetoldUnloadedEcosystemGameTests {
    private static final Identifier EMPTY_STRUCTURE =
            Identifier.withDefaultNamespace("empty");
    private static final int TEST_PADDING = 64;

    private RetoldUnloadedEcosystemGameTests() {
    }

    public static void register(RegisterGameTestsEvent event) {
        registerTest(
                event,
                "unloaded_hunger_catch_up_is_capped_and_budgeted",
                RetoldUnloadedEcosystemGameTests::catchUpIsCappedAndBudgeted
        );
        registerTest(
                event,
                "unloaded_starvation_damages_wild_and_protects_named_tamed_animals",
                RetoldUnloadedEcosystemGameTests::starvationDamagesWildAndProtectsKeptAnimals
        );
        registerTest(
                event,
                "unloaded_cube_starvation_splits_once",
                RetoldUnloadedEcosystemGameTests::cubeStarvationSplitsOnce
        );
        registerTest(
                event,
                "unloaded_farmer_production_uses_owned_crops_and_storage_provenance",
                RetoldUnloadedEcosystemGameTests::farmerProductionUsesOwnedCropsAndProvenance
        );
        registerTest(
                event,
                "unloaded_natural_spawning_deduplicates_chunks_and_respects_rules",
                RetoldUnloadedEcosystemGameTests::naturalSpawningDeduplicatesChunksAndRespectsRules
        );
        registerTest(
                event,
                "unloaded_feeder_catch_up_consumes_one_daily_meal",
                RetoldUnloadedEcosystemGameTests::feederCatchUpConsumesOneDailyMeal
        );
        registerTest(
                event,
                "unloaded_natural_forage_catch_up_consumes_real_daily_blocks",
                RetoldUnloadedEcosystemGameTests::naturalForageConsumesRealDailyBlocks
        );
        registerTest(
                event,
                "unloaded_natural_forage_catch_up_respects_mob_griefing",
                RetoldUnloadedEcosystemGameTests::naturalForageRespectsMobGriefing
        );
        registerTest(
                event,
                "unloaded_aquatic_forage_catch_up_consumes_real_daily_plants",
                RetoldUnloadedEcosystemGameTests::aquaticForageConsumesRealDailyPlants
        );
        registerTest(
                event,
                "unloaded_predation_consumes_one_wild_prey_per_day_without_drops",
                RetoldUnloadedEcosystemGameTests::predationConsumesOneWildPreyPerDay
        );
        registerTest(
                event,
                "unloaded_predation_protects_named_tamed_and_tamed_hunter_animals",
                RetoldUnloadedEcosystemGameTests::predationProtectsNamedAndTamedAnimals
        );
        registerTest(
                event,
                "unloaded_predation_respects_diets_and_closed_barriers",
                RetoldUnloadedEcosystemGameTests::predationRespectsDietsAndBarriers
        );
        registerTest(
                event,
                "unloaded_predation_uses_feeder_before_wild_prey",
                RetoldUnloadedEcosystemGameTests::predationUsesFeederFirst
        );
        registerTest(
                event,
                "unloaded_breeding_uses_food_satisfaction_without_population_cap",
                RetoldUnloadedEcosystemGameTests::breedingUsesFoodWithoutPopulationCap
        );
        registerTest(
                event,
                "unloaded_land_migration_requires_one_day_and_no_food",
                RetoldUnloadedEcosystemGameTests::landMigrationRequiresOneDayAndNoFood
        );
        registerTest(
                event,
                "unloaded_land_migration_relocates_reachable_real_herd",
                RetoldUnloadedEcosystemGameTests::landMigrationRelocatesReachableHerd
        );
        registerTest(
                event,
                "unloaded_land_migration_respects_closed_barriers",
                RetoldUnloadedEcosystemGameTests::landMigrationRespectsClosedBarriers
        );
        registerTest(
                event,
                "unloaded_pig_migration_relocates_reachable_foraging_group",
                RetoldUnloadedEcosystemGameTests::pigMigrationRelocatesForagingGroup
        );
        registerTest(
                event,
                "unloaded_aquatic_migration_relocates_reachable_real_school",
                RetoldUnloadedEcosystemGameTests::aquaticMigrationRelocatesReachableSchool
        );
    }

    private static void catchUpIsCappedAndBudgeted(GameTestHelper helper) {
        RetoldUnloadedEcosystemCatchUp.clear();
        placeFloor(helper);
        List<Bat> bats = new ArrayList<>();
        long lastSimulatedAt = Math.max(1L, helper.getLevel().getGameTime());
        long catchUpAt = lastSimulatedAt
                + RetoldUnloadedEcosystemCatchUp.MAX_CATCH_UP_TICKS
                + 24_000L;

        try {
            for (int index = 0;
                 index < RetoldUnloadedEcosystemCatchUp.MAX_TASKS_PER_TICK + 2;
                 index++) {
                Bat bat = helper.spawn(
                        EntityTypes.BAT,
                        1 + index % 6,
                        3,
                        1 + index / 6
                );
                RetoldMobState state = RetoldMobStates.getOrCreate(
                        bat,
                        lastSimulatedAt
                );
                state.setHunger(0);
                state.markHungerTick(lastSimulatedAt);
                bat.setCustomName(Component.literal("Catch-up protected " + index));

                if (index == 0) {
                    bat.setHealth(1.0F);
                }

                helper.assertTrue(
                        RetoldUnloadedEcosystemCatchUp.deferLongGap(
                                helper.getLevel(),
                                bat,
                                state,
                                catchUpAt,
                                RetoldMobRules.hungerInterval(bat)
                        ),
                        "A multi-pulse unloaded gap must enter the bounded catch-up queue"
                );
                bats.add(bat);
            }

            helper.assertValueEqual(
                    RetoldUnloadedEcosystemCatchUp.pendingCount(),
                    bats.size(),
                    "Every distinct returning mob must be queued exactly once"
            );

            int firstBatch = RetoldUnloadedEcosystemCatchUp.processPending(
                    RetoldUnloadedEcosystemCatchUp.MAX_TASKS_PER_TICK
            );
            helper.assertValueEqual(
                    firstBatch,
                    RetoldUnloadedEcosystemCatchUp.MAX_TASKS_PER_TICK,
                    "One server tick must not process beyond the catch-up work budget"
            );
            helper.assertValueEqual(
                    RetoldUnloadedEcosystemCatchUp.pendingCount(),
                    2,
                    "Excess catch-up work must remain queued for a later tick"
            );

            for (int index = 0;
                 index < RetoldUnloadedEcosystemCatchUp.MAX_TASKS_PER_TICK;
                 index++) {
                assertCaughtUp(
                        helper,
                        bats.get(index),
                        catchUpAt
                );
            }

            for (int index = RetoldUnloadedEcosystemCatchUp.MAX_TASKS_PER_TICK;
                 index < bats.size();
                 index++) {
                helper.assertValueEqual(
                        RetoldMobStates.get(bats.get(index)).hunger(),
                        0,
                        "Deferred mobs must remain unchanged until their queued batch runs"
                );
            }

            RetoldUnloadedEcosystemCatchUp.processPending(
                    RetoldUnloadedEcosystemCatchUp.MAX_TASKS_PER_TICK
            );

            for (int index = RetoldUnloadedEcosystemCatchUp.MAX_TASKS_PER_TICK;
                 index < bats.size();
                 index++) {
                assertCaughtUp(
                        helper,
                        bats.get(index),
                        catchUpAt
                );
            }

            helper.assertValueEqual(
                    RetoldUnloadedEcosystemCatchUp.pendingCount(),
                    0,
                    "The second bounded batch must drain the remaining work"
            );
            helper.succeed();
        } finally {
            RetoldUnloadedEcosystemCatchUp.clear();

            for (Bat bat : bats) {
                bat.discard();
            }
        }
    }

    private static void assertCaughtUp(
            GameTestHelper helper,
            Bat bat,
            long catchUpAt
    ) {
        RetoldMobState state = RetoldMobStates.get(bat);
        helper.assertValueEqual(
                state.hunger(),
                100,
                "Seven capped days of metabolism must reach critical hunger"
        );
        helper.assertValueEqual(
                state.lastHungerTickAt(),
                catchUpAt,
                "A capped catch-up must discard debt older than seven days"
        );
        helper.assertValueEqual(
                bat.getHealth(),
                1.0F,
                "Named mobs must receive offline starvation damage only to the one-health floor"
        );
        helper.assertValueEqual(
                bat.getPersistentData()
                        .getCompoundOrEmpty("RetoldMobState")
                        .getLong("lastHungerTickAt")
                        .orElse(-1L),
                catchUpAt,
                "The reconciled metabolism timestamp must persist on the real entity"
        );
    }

    private static void starvationDamagesWildAndProtectsKeptAnimals(
            GameTestHelper helper
    ) {
        RetoldUnloadedEcosystemCatchUp.clear();
        placeFloor(helper);
        Cow wildCow = helper.spawn(EntityTypes.COW, 2, 2, 2);
        Cow namedCow = helper.spawn(EntityTypes.COW, 4, 2, 2);
        Horse tamedHorse = helper.spawn(EntityTypes.HORSE, 6, 2, 2);
        long lastSimulatedAt = Math.max(1L, helper.getLevel().getGameTime());

        wildCow.setHealth(2.0F);
        namedCow.setCustomName(Component.literal("Offline protected"));
        namedCow.setHealth(2.0F);
        tamedHorse.setTamed(true);
        tamedHorse.setHealth(2.0F);

        queueCriticalCatchUp(helper, wildCow, lastSimulatedAt);
        queueCriticalCatchUp(helper, namedCow, lastSimulatedAt);
        queueCriticalCatchUp(helper, tamedHorse, lastSimulatedAt);
        RetoldUnloadedEcosystemCatchUp.processPending(
                RetoldUnloadedEcosystemCatchUp.MAX_TASKS_PER_TICK
        );

        try {
            helper.assertFalse(
                    wildCow.isAlive(),
                    "An unprotected wild animal must be able to die from accumulated offline starvation"
            );
            helper.assertTrue(
                    namedCow.isAlive() && namedCow.getHealth() == 1.0F,
                    "A named animal must return alive at the offline starvation health floor"
            );
            helper.assertTrue(
                    tamedHorse.isAlive() && tamedHorse.getHealth() == 1.0F,
                    "A tamed animal must return alive at the offline starvation health floor"
            );
            helper.succeed();
        } finally {
            RetoldUnloadedEcosystemCatchUp.clear();
            wildCow.discard();
            namedCow.discard();
            tamedHorse.discard();
        }
    }

    private static void cubeStarvationSplitsOnce(GameTestHelper helper) {
        RetoldUnloadedEcosystemCatchUp.clear();
        placeFloor(helper);
        var slime = helper.spawn(EntityTypes.SLIME, 4, 2, 2);
        slime.setSize(4, true);
        ItemStack storedApple = new ItemStack(Items.APPLE);
        helper.assertTrue(
                RetoldSlimeItemStorage.swallow(slime, storedApple),
                "The returning Cube Mob must begin with real swallowed storage"
        );
        long lastSimulatedAt = Math.max(1L, helper.getLevel().getGameTime());
        var splitArea = slime.getBoundingBox().inflate(2.0D);

        queueCriticalCatchUp(helper, slime, lastSimulatedAt);
        RetoldUnloadedEcosystemCatchUp.processPending(
                RetoldUnloadedEcosystemCatchUp.MAX_TASKS_PER_TICK
        );

        try {
            List<AbstractCubeMob> children = helper.getLevel().getEntitiesOfClass(
                    AbstractCubeMob.class,
                    splitArea,
                    child -> child.getType() == EntityTypes.SLIME
            );
            helper.assertTrue(
                    slime.isRemoved(),
                    "A critically hungry returning Cube Mob must replace its original entity"
            );
            helper.assertValueEqual(
                    children.size(),
                    2,
                    "One catch-up transaction must create exactly two half-size children"
            );
            int storedItems = 0;

            for (AbstractCubeMob child : children) {
                helper.assertValueEqual(
                        child.getSize(),
                        2,
                        "Each offline-starvation child must have half the parent's size"
                );
                helper.assertValueEqual(
                        RetoldMobStates.getOrCreate(child, lastSimulatedAt).hunger(),
                        50,
                        "Each offline-starvation child must inherit half the parent's hunger"
                );
                storedItems += RetoldSlimeItemStorage.getStoredItemCount(child);
            }

            helper.assertValueEqual(
                    storedItems,
                    storedApple.getCount(),
                    "Offline splitting must conserve swallowed storage exactly once"
            );
            helper.succeed();
        } finally {
            RetoldUnloadedEcosystemCatchUp.clear();
        }
    }

    private static void farmerProductionUsesOwnedCropsAndProvenance(
            GameTestHelper helper
    ) {
        RetoldUnloadedEcosystemCatchUp.clear();
        placeFarmerRouteFloor(helper);
        var level = helper.getLevel();
        BlockPos cropRelative = new BlockPos(5, 2, 2);
        BlockPos cropPos = helper.absolutePos(cropRelative);
        BlockPos chestRelative = new BlockPos(3, 2, 2);
        BlockPos chestPos = helper.absolutePos(chestRelative);
        CropBlock carrots = (CropBlock) Blocks.CARROTS;
        BlockState planted = carrots.getStateForAge(0);
        helper.setBlock(cropRelative.below(), Blocks.FARMLAND);
        helper.setBlock(cropRelative, planted);
        helper.setBlock(chestRelative, Blocks.BARREL);
        BlockPos meetingRelative = new BlockPos(4, 2, 4);
        helper.setBlock(meetingRelative, Blocks.BELL);
        Container storage = containerAt(helper, chestRelative);
        storage.setChanged();
        helper.assertTrue(
                level.getChunkAt(chestPos).getBlockEntities().containsKey(chestPos),
                "The Farmer fixture barrel must be registered in its loaded chunk"
        );
        RetoldVillageCropOwnership.afterFarmerWork(
                level,
                cropPos,
                Blocks.AIR.defaultBlockState(),
                planted
        );
        helper.assertTrue(
                RetoldVillageCropOwnership.isOwned(level, cropPos),
                "The production fixture crop must carry Farmer provenance"
        );
        Villager farmer = helper.spawn(EntityTypes.VILLAGER, 4, 2, 2);
        farmer.setVillagerData(farmer.getVillagerData().withProfession(
                level.registryAccess(),
                VillagerProfession.FARMER
        ));
        farmer.setVillagerXp(1);
        farmer.getBrain().setMemory(
                MemoryModuleType.MEETING_POINT,
                GlobalPos.of(
                        level.dimension(),
                        helper.absolutePos(meetingRelative)
                )
        );
        farmer.getInventory().setItem(
                farmer.getInventory().getContainerSize() - 1,
                new ItemStack(Items.BREAD, 6)
        );
        boolean originalMobGriefing = level.getGameRules().get(
                GameRules.MOB_GRIEFING
        );

        level.getGameRules().set(
                GameRules.MOB_GRIEFING,
                true,
                level.getServer()
        );
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    helper.assertTrue(
                            level.getGameRules().get(GameRules.MOB_GRIEFING),
                            "The enabled production phase requires mobGriefing"
                    );
                    helper.assertTrue(
                            farmer.getVillagerData()
                                    .profession()
                                    .is(VillagerProfession.FARMER),
                            "The production fixture must retain its Farmer profession"
                    );
                    helper.assertTrue(
                            RetoldMobGriefing.canBreakBlock(
                                    level,
                                    farmer,
                                    cropPos
                            ),
                            "The Farmer must be allowed to harvest the fixture crop"
                    );
                    helper.assertTrue(
                            RetoldMobGriefing.canPlaceBlock(
                                    level,
                                    farmer,
                                    cropPos
                            ),
                            "The Farmer must be allowed to replant the fixture crop"
                    );
                    RetoldUnloadedFarmerProduction.enqueue(level, farmer, 1);
                    helper.assertValueEqual(
                            RetoldUnloadedFarmerProduction.pendingCount(),
                            1,
                            "The eligible Farmer must enter the production queue"
                    );
                })
                .thenWaitUntil(() -> {
                    RetoldUnloadedFarmerProduction.processPending(
                            RetoldUnloadedFarmerProduction.MAX_FARMERS_PER_TICK
                    );
                    helper.assertValueEqual(
                            RetoldUnloadedFarmerProduction.pendingCount(),
                            0,
                            "The one-day Farmer transaction must finish"
                    );
                })
                .thenExecute(() -> {
                    try {
                        Container chest = storage;
                        int producedCarrots = chest.countItem(Items.CARROT);
                        helper.assertTrue(
                                producedCarrots > 0,
                                "A reachable owned crop must produce real communal food; "
                                        + "chestBread=" + chest.countItem(Items.BREAD)
                                        + ", farmerCarrots="
                                        + farmer.getInventory().countItem(Items.CARROT)
                                        + ", farmerBread="
                                        + farmer.getInventory().countItem(Items.BREAD)
                        );
                        helper.assertValueEqual(
                                RetoldVillageContainerOwnership.ownedCount(
                                        level,
                                        chestPos,
                                        new ItemStack(Items.CARROT)
                                ),
                                producedCarrots,
                                "Every catch-up-produced Carrot must be marked village-owned"
                        );
                        helper.assertValueEqual(
                                farmer.getInventory().countItem(Items.BREAD),
                                6,
                                "The Farmer must retain its 24-point personal reserve"
                        );
                        helper.assertValueEqual(
                                carrots.getAge(level.getBlockState(cropPos)),
                                0,
                                "The harvested owned crop must be replanted"
                        );
                    } finally {
                        level.getGameRules().set(
                                GameRules.MOB_GRIEFING,
                                originalMobGriefing,
                                level.getServer()
                        );
                        RetoldUnloadedEcosystemCatchUp.clear();
                        farmer.discard();
                    }
                })
                .thenSucceed();
    }

    private static void naturalSpawningDeduplicatesChunksAndRespectsRules(
            GameTestHelper helper
    ) {
        RetoldUnloadedEcosystemCatchUp.clear();
        var level = helper.getLevel();
        BlockPos returningPos = helper.absolutePos(new BlockPos(2, 2, 2));
        boolean originalSpawnMobs = level.getGameRules().get(
                GameRules.SPAWN_MOBS
        );

        level.getChunkAt(returningPos);
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    try {
                        level.getGameRules().set(
                                GameRules.SPAWN_MOBS,
                                true,
                                level.getServer()
                        );
                        RetoldUnloadedNaturalSpawning.enqueue(
                                level,
                                returningPos,
                                2
                        );
                        RetoldUnloadedNaturalSpawning.enqueue(
                                level,
                                returningPos,
                                5
                        );
                        helper.assertValueEqual(
                                RetoldUnloadedNaturalSpawning.pendingCount(),
                                1,
                                "Returning mobs in one chunk must share one spawn task"
                        );
                        helper.assertValueEqual(
                                RetoldUnloadedNaturalSpawning.pendingAttemptCount(),
                                5,
                                "The shared task must retain the largest capped daily debt"
                        );

                        RetoldUnloadedNaturalSpawning.processPending(1);
                        helper.assertValueEqual(
                                RetoldUnloadedNaturalSpawning.pendingAttemptCount(),
                                4,
                                "One bounded chunk pass must consume exactly one daily attempt"
                        );

                        level.getGameRules().set(
                                GameRules.SPAWN_MOBS,
                                false,
                                level.getServer()
                        );
                        RetoldUnloadedNaturalSpawning.processPending(1);
                        helper.assertValueEqual(
                                RetoldUnloadedNaturalSpawning.pendingCount(),
                                0,
                                "Disabled mob spawning must discard the remaining spawn debt"
                        );
                    } finally {
                        level.getGameRules().set(
                                GameRules.SPAWN_MOBS,
                                originalSpawnMobs,
                                level.getServer()
                        );
                        RetoldUnloadedEcosystemCatchUp.clear();
                    }
                })
                .thenSucceed();
    }

    private static void queueCriticalCatchUp(
            GameTestHelper helper,
            Mob mob,
            long lastSimulatedAt
    ) {
        RetoldMobState state = RetoldMobStates.getOrCreate(mob, lastSimulatedAt);
        int hungerInterval = RetoldMobRules.hungerInterval(mob);
        state.setHunger(100);
        state.markHungerTick(lastSimulatedAt);
        helper.assertTrue(
                RetoldUnloadedEcosystemCatchUp.deferLongGap(
                        helper.getLevel(),
                        mob,
                        state,
                        lastSimulatedAt + 2L * hungerInterval,
                        hungerInterval
                ),
                "The critical two-pulse gap must enter unloaded reconciliation"
        );
    }

    private static void feederCatchUpConsumesOneDailyMeal(GameTestHelper helper) {
        RetoldUnloadedEcosystemCatchUp.clear();
        placeFloor(helper);
        BlockPos feederPos = new BlockPos(4, 2, 2);
        helper.setBlock(feederPos, RetoldBlocks.ANIMAL_FEEDER.get());
        AnimalFeederBlockEntity feeder = feederAt(helper, feederPos);
        feeder.setItem(0, new ItemStack(Items.WHEAT, 4));
        Cow cow = helper.spawn(EntityTypes.COW, 2, 2, 2);
        Chicken chicken = helper.spawn(EntityTypes.CHICKEN, 6, 2, 2);
        long lastSimulatedAt = Math.max(1L, helper.getLevel().getGameTime());
        long catchUpAt = lastSimulatedAt + 2L * 24_000L;
        RetoldMobState cowState = RetoldMobStates.getOrCreate(cow, lastSimulatedAt);
        RetoldMobState chickenState = RetoldMobStates.getOrCreate(
                chicken,
                lastSimulatedAt
        );
        RetoldAnimalHomeMemory cowRange = RetoldAnimalHomes.getOrCreatePackHome(
                helper.getLevel(),
                cow,
                List.of(),
                cow.blockPosition(),
                lastSimulatedAt
        );
        float cowHealth = cow.getHealth();

        try {
            cowState.setHunger(0);
            cowState.markHungerTick(lastSimulatedAt);
            chickenState.setHunger(0);
            chickenState.markHungerTick(lastSimulatedAt);
            helper.assertTrue(
                    cowRange != null
                            && RetoldAnimalFeederBehavior.hasUsableFoodNearby(
                            helper.getLevel(),
                            cow,
                            cowRange.pos(),
                            helper.getLevel().getGameTime()
                    ),
                    "The test Cow must begin with a compatible feeder in its persisted range"
            );
            helper.assertFalse(
                    RetoldAnimalFeederBehavior.hasUsableFoodNearby(
                            helper.getLevel(),
                            chicken,
                            chicken.blockPosition(),
                            helper.getLevel().getGameTime()
                    ),
                    "Wheat must not become an offline Chicken meal"
            );
            helper.assertTrue(
                    RetoldUnloadedEcosystemCatchUp.deferLongGap(
                            helper.getLevel(),
                            cow,
                            cowState,
                            catchUpAt,
                            RetoldMobRules.hungerInterval(cow)
                    ),
                    "The Cow's two-day gap must be queued"
            );
            helper.assertTrue(
                    RetoldUnloadedEcosystemCatchUp.deferLongGap(
                            helper.getLevel(),
                            chicken,
                            chickenState,
                            catchUpAt,
                            RetoldMobRules.hungerInterval(chicken)
                    ),
                    "The Chicken's two-day gap must be queued"
            );

            RetoldUnloadedEcosystemCatchUp.processPending(
                    RetoldUnloadedEcosystemCatchUp.MAX_TASKS_PER_TICK
            );
            helper.assertValueEqual(
                    feeder.getItem(0).getCount(),
                    2,
                    "Two simulated days must consume exactly two compatible feeder items"
            );
            helper.assertValueEqual(
                    cowState.hunger(),
                    53,
                    "Daily Wheat meals must be interleaved with the Cow's real metabolism cadence"
            );
            helper.assertValueEqual(
                    cowState.lastAteAt(),
                    catchUpAt,
                    "Offline feeder consumption must persist the last meal timestamp"
            );
            helper.assertValueEqual(
                    chickenState.hunger(),
                    100,
                    "An incompatible feeder must not relieve offline Chicken hunger"
            );
            helper.assertValueEqual(
                    cow.getHealth(),
                    cowHealth,
                    "Offline feeder reconciliation must not damage the Cow"
            );
            helper.assertFalse(
                    chicken.isAlive(),
                    "An incompatible feeder must not protect the Chicken from offline starvation"
            );
            helper.succeed();
        } finally {
            RetoldUnloadedEcosystemCatchUp.clear();
            cow.discard();
            chicken.discard();
        }
    }

    private static void naturalForageConsumesRealDailyBlocks(
            GameTestHelper helper
    ) {
        RetoldUnloadedEcosystemCatchUp.clear();
        placeFloor(helper);
        BlockPos firstForage = new BlockPos(4, 2, 2);
        BlockPos secondForage = new BlockPos(2, 2, 4);
        helper.setBlock(firstForage, Blocks.GRASS_BLOCK);
        helper.setBlock(secondForage, Blocks.GRASS_BLOCK);
        Cow cow = helper.spawn(EntityTypes.COW, 2, 2, 2);
        long lastSimulatedAt = Math.max(1L, helper.getLevel().getGameTime());
        long catchUpAt = lastSimulatedAt + 2L * 24_000L;
        RetoldMobState state = RetoldMobStates.getOrCreate(
                cow,
                lastSimulatedAt
        );

        try {
            state.setHunger(0);
            state.markHungerTick(lastSimulatedAt);
            helper.assertTrue(
                    RetoldUnloadedEcosystemCatchUp.deferLongGap(
                            helper.getLevel(),
                            cow,
                            state,
                            catchUpAt,
                            RetoldMobRules.hungerInterval(cow)
                    ),
                    "The Cow's two-day forage gap must be queued"
            );

            RetoldUnloadedEcosystemCatchUp.processPending(
                    RetoldUnloadedEcosystemCatchUp.MAX_TASKS_PER_TICK
            );
            helper.assertBlockPresent(Blocks.DIRT, firstForage);
            helper.assertBlockPresent(Blocks.DIRT, secondForage);
            helper.assertValueEqual(
                    state.hunger(),
                    61,
                    "Two daily grass-block meals must interleave exact Cow metabolism and forage relief"
            );
            helper.assertValueEqual(
                    state.lastAteAt(),
                    catchUpAt,
                    "The second real forage meal must persist the final daily timestamp"
            );
            helper.succeed();
        } finally {
            RetoldUnloadedEcosystemCatchUp.clear();
            cow.discard();
        }
    }

    private static void naturalForageRespectsMobGriefing(
            GameTestHelper helper
    ) {
        RetoldUnloadedEcosystemCatchUp.clear();
        placeFloor(helper);
        BlockPos foragePos = new BlockPos(4, 2, 2);
        helper.setBlock(foragePos, Blocks.GRASS_BLOCK);
        Cow cow = helper.spawn(EntityTypes.COW, 2, 2, 2);
        long lastSimulatedAt = Math.max(1L, helper.getLevel().getGameTime());
        long catchUpAt = lastSimulatedAt + 24_000L;
        RetoldMobState state = RetoldMobStates.getOrCreate(
                cow,
                lastSimulatedAt
        );
        boolean originalMobGriefing = helper.getLevel()
                .getGameRules()
                .get(GameRules.MOB_GRIEFING);

        try {
            helper.getLevel().getGameRules().set(
                    GameRules.MOB_GRIEFING,
                    false,
                    helper.getLevel().getServer()
            );
            state.setHunger(0);
            state.markHungerTick(lastSimulatedAt);
            helper.assertTrue(
                    RetoldUnloadedEcosystemCatchUp.deferLongGap(
                            helper.getLevel(),
                            cow,
                            state,
                            catchUpAt,
                            RetoldMobRules.hungerInterval(cow)
                    ),
                    "The Cow's one-day forage gap must be queued"
            );

            RetoldUnloadedEcosystemCatchUp.processPending(
                    RetoldUnloadedEcosystemCatchUp.MAX_TASKS_PER_TICK
            );
            helper.assertBlockPresent(Blocks.GRASS_BLOCK, foragePos);
            helper.assertValueEqual(
                    state.hunger(),
                    54,
                    "Denied terrain mutation must not grant offline forage relief"
            );
            helper.assertValueEqual(
                    state.lastAteAt(),
                    0L,
                    "Denied forage must not persist a meal timestamp"
            );
            helper.succeed();
        } finally {
            helper.getLevel().getGameRules().set(
                    GameRules.MOB_GRIEFING,
                    originalMobGriefing,
                    helper.getLevel().getServer()
            );
            RetoldUnloadedEcosystemCatchUp.clear();
            cow.discard();
        }
    }

    private static void aquaticForageConsumesRealDailyPlants(
            GameTestHelper helper
    ) {
        RetoldUnloadedEcosystemCatchUp.clear();
        buildWaterArena(helper);
        BlockPos firstForage = new BlockPos(4, 3, 2);
        BlockPos secondForage = new BlockPos(2, 3, 4);
        helper.setBlock(firstForage, Blocks.SEAGRASS);
        helper.setBlock(secondForage, Blocks.SEAGRASS);
        PathfinderMob cod = helper.spawn(EntityTypes.COD, 2, 3, 2);
        long lastSimulatedAt = Math.max(1L, helper.getLevel().getGameTime());
        long catchUpAt = lastSimulatedAt + 2L * 24_000L;
        RetoldMobState state = RetoldMobStates.getOrCreate(
                cod,
                lastSimulatedAt
        );

        try {
            state.setHunger(0);
            state.markHungerTick(lastSimulatedAt);
            helper.assertTrue(
                    RetoldUnloadedEcosystemCatchUp.deferLongGap(
                            helper.getLevel(),
                            cod,
                            state,
                            catchUpAt,
                            RetoldMobRules.hungerInterval(cod)
                    ),
                    "The Cod's two-day aquatic forage gap must be queued"
            );

            RetoldUnloadedEcosystemCatchUp.processPending(
                    RetoldUnloadedEcosystemCatchUp.MAX_TASKS_PER_TICK
            );
            helper.assertFalse(
                    helper.getBlockState(firstForage).is(Blocks.SEAGRASS),
                    "The first daily aquatic plant must be removed"
            );
            helper.assertFalse(
                    helper.getBlockState(secondForage).is(Blocks.SEAGRASS),
                    "The second daily aquatic plant must be removed"
            );
            helper.assertValueEqual(
                    state.hunger(),
                    60,
                    "Two daily Seagrass meals must interleave exact Cod metabolism and forage relief"
            );
            helper.assertValueEqual(
                    state.lastAteAt(),
                    catchUpAt,
                    "The second aquatic forage meal must persist the final daily timestamp"
            );
            helper.succeed();
        } finally {
            RetoldUnloadedEcosystemCatchUp.clear();
            cod.discard();
        }
    }

    private static void predationConsumesOneWildPreyPerDay(
            GameTestHelper helper
    ) {
        RetoldUnloadedEcosystemCatchUp.clear();
        placeFloor(helper);
        Wolf wolf = helper.spawn(EntityTypes.WOLF, 2, 2, 2);
        Sheep firstSheep = helper.spawn(EntityTypes.SHEEP, 4, 2, 2);
        Sheep secondSheep = helper.spawn(EntityTypes.SHEEP, 2, 2, 4);
        Sheep survivingSheep = helper.spawn(EntityTypes.SHEEP, 5, 2, 4);
        long lastSimulatedAt = Math.max(1L, helper.getLevel().getGameTime());
        long catchUpAt = lastSimulatedAt + 2L * 24_000L;
        RetoldMobState state = RetoldMobStates.getOrCreate(
                wolf,
                lastSimulatedAt
        );

        state.setHunger(0);
        state.setStress(50);
        state.setConfidence(50);
        state.markHungerTick(lastSimulatedAt);
        helper.assertTrue(
                RetoldPreyTargeting.isValidMobRulePrey(
                        wolf,
                        firstSheep,
                        lastSimulatedAt
                ),
                "The fixture Sheep must be valid Wolf prey; decision="
                        + RetoldPreyTargeting.shortMobRulePreyDecision(
                        wolf,
                        firstSheep,
                        lastSimulatedAt
                )
        );
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> helper.assertTrue(
                        RetoldUnloadedEcosystemCatchUp.deferLongGap(
                                helper.getLevel(),
                                wolf,
                                state,
                                catchUpAt,
                                RetoldMobRules.hungerInterval(wolf)
                        ),
                        "The Wolf's two-day predation gap must be queued"
                ))
                .thenWaitUntil(() -> {
                    RetoldUnloadedEcosystemCatchUp.processPending(
                            RetoldUnloadedEcosystemCatchUp.MAX_TASKS_PER_TICK
                    );
                    helper.assertValueEqual(
                            RetoldUnloadedEcosystemCatchUp.pendingCount(),
                            0,
                            "Budgeted predation reconciliation must eventually drain its queued task"
                    );
                    helper.assertValueEqual(
                            (firstSheep.isRemoved() ? 1 : 0)
                                    + (secondSheep.isRemoved() ? 1 : 0)
                                    + (survivingSheep.isRemoved() ? 1 : 0),
                            2,
                            "Two simulated days must remove exactly two reachable wild prey"
                    );
                    helper.assertValueEqual(
                            state.hunger(),
                            48,
                            "Two daily prey meals must interleave Wolf metabolism and prey relief"
                    );
                    helper.assertValueEqual(
                            state.lastAteAt(),
                            catchUpAt,
                            "The final offline prey meal must persist the daily meal timestamp"
                    );
                    helper.assertValueEqual(
                            state.lastSuccessfulHuntAt(),
                            catchUpAt,
                            "The final offline predation must persist the hunt outcome timestamp"
                    );
                    helper.assertValueEqual(
                            state.stress(),
                            40,
                            "Offline prey meals must apply normal feeding and successful-hunt stress relief"
                    );
                    helper.assertValueEqual(
                            state.confidence(),
                            62,
                            "Offline prey meals must apply normal feeding and successful-hunt confidence"
                    );
                    helper.assertTrue(
                            helper.getLevel().getEntitiesOfClass(
                                    ItemEntity.class,
                                    wolf.getBoundingBox().inflate(18.0D)
                            ).isEmpty(),
                            "Offline predation must not create loot drops"
                    );
                    helper.assertTrue(
                            helper.getLevel().getEntitiesOfClass(
                                    ExperienceOrb.class,
                                    wolf.getBoundingBox().inflate(18.0D)
                            ).isEmpty(),
                            "Offline predation must not create experience orbs"
                    );
                })
                .thenExecute(RetoldUnloadedEcosystemCatchUp::clear)
                .thenSucceed();
    }

    private static void predationProtectsNamedAndTamedAnimals(
            GameTestHelper helper
    ) {
        RetoldUnloadedEcosystemCatchUp.clear();
        placeFloor(helper);
        Spider spider = helper.spawnWithNoFreeWill(EntityTypes.SPIDER, 2, 2, 2);
        Cow namedCow = helper.spawnWithNoFreeWill(EntityTypes.COW, 3, 2, 2);
        Horse tamedHorse = helper.spawnWithNoFreeWill(EntityTypes.HORSE, 4, 2, 2);
        Cow wildCow = helper.spawnWithNoFreeWill(EntityTypes.COW, 5, 2, 2);
        Wolf tamedWolf = helper.spawnWithNoFreeWill(EntityTypes.WOLF, 2, 2, 4);
        Sheep wildSheep = helper.spawnWithNoFreeWill(EntityTypes.SHEEP, 5, 2, 4);
        long lastSimulatedAt = Math.max(1L, helper.getLevel().getGameTime());
        long catchUpAt = lastSimulatedAt + 24_000L;
        RetoldMobState spiderState = RetoldMobStates.getOrCreate(
                spider,
                lastSimulatedAt
        );
        RetoldMobState wolfState = RetoldMobStates.getOrCreate(
                tamedWolf,
                lastSimulatedAt
        );

        namedCow.setCustomName(Component.literal("Offline protected"));
        tamedHorse.setTamed(true);
        tamedWolf.setTame(true, true);
        spider.setOnGround(true);
        namedCow.setOnGround(true);
        tamedHorse.setOnGround(true);
        wildCow.setOnGround(true);
        tamedWolf.setOnGround(true);
        wildSheep.setOnGround(true);
        spiderState.setHunger(0);
        spiderState.markHungerTick(lastSimulatedAt);
        wolfState.setHunger(0);
        wolfState.markHungerTick(lastSimulatedAt);
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    helper.assertTrue(
                            RetoldUnloadedEcosystemCatchUp.deferLongGap(
                                    helper.getLevel(),
                                    spider,
                                    spiderState,
                                    catchUpAt,
                                    RetoldMobRules.hungerInterval(spider)
                            ),
                            "The Spider's protected-prey gap must be queued"
                    );
                    helper.assertTrue(
                            RetoldUnloadedEcosystemCatchUp.deferLongGap(
                                    helper.getLevel(),
                                    tamedWolf,
                                    wolfState,
                                    catchUpAt,
                                    RetoldMobRules.hungerInterval(tamedWolf)
                            ),
                            "The tamed Wolf's gap must be queued"
                    );
                })
                .thenWaitUntil(() -> {
                    RetoldUnloadedEcosystemCatchUp.processPending(
                            RetoldUnloadedEcosystemCatchUp.MAX_TASKS_PER_TICK
                    );
                    helper.assertValueEqual(
                            RetoldUnloadedEcosystemCatchUp.pendingCount(),
                            0,
                            "Protected-animal reconciliation must eventually drain its queued tasks"
                    );
                    helper.assertFalse(
                            namedCow.isRemoved(),
                            "A named prey animal must never die during offline predation"
                    );
                    helper.assertFalse(
                            tamedHorse.isRemoved(),
                            "A tamed prey animal must never die during offline predation"
                    );
                    helper.assertTrue(
                            wildCow.isRemoved(),
                            "The same predator must still consume a compatible unprotected wild prey"
                    );
                    helper.assertFalse(
                            wildSheep.isRemoved(),
                            "A tamed predator must not hunt during unloaded reconciliation"
                    );
                    helper.assertValueEqual(
                            wolfState.lastSuccessfulHuntAt(),
                            0L,
                            "A tamed predator must not receive an offline hunt outcome"
                    );
                })
                .thenExecute(RetoldUnloadedEcosystemCatchUp::clear)
                .thenSucceed();
    }

    private static void predationRespectsDietsAndBarriers(
            GameTestHelper helper
    ) {
        RetoldUnloadedEcosystemCatchUp.clear();
        placeFloor(helper);

        for (int z = 0; z <= 5; z++) {
            helper.setBlock(new BlockPos(4, 2, z), Blocks.STONE);
            helper.setBlock(new BlockPos(4, 3, z), Blocks.STONE);
        }

        Wolf wolf = helper.spawn(EntityTypes.WOLF, 2, 2, 2);
        Cow incompatibleCow = helper.spawn(EntityTypes.COW, 3, 2, 4);
        Sheep blockedSheep = helper.spawn(EntityTypes.SHEEP, 6, 2, 2);
        long lastSimulatedAt = Math.max(1L, helper.getLevel().getGameTime());
        long catchUpAt = lastSimulatedAt + 24_000L;
        RetoldMobState state = RetoldMobStates.getOrCreate(
                wolf,
                lastSimulatedAt
        );

        state.setHunger(0);
        state.markHungerTick(lastSimulatedAt);
        helper.assertFalse(
                RetoldMobRules.canHuntPrey(
                        wolf,
                        incompatibleCow,
                        lastSimulatedAt
                ),
                "The accessible Cow must remain outside the Wolf's prey diet"
        );
        helper.assertTrue(
                RetoldMobRules.canHuntPrey(
                        wolf,
                        blockedSheep,
                        lastSimulatedAt
                ),
                "The blocked Sheep must otherwise be compatible Wolf prey"
        );
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> helper.assertTrue(
                        RetoldUnloadedEcosystemCatchUp.deferLongGap(
                                helper.getLevel(),
                                wolf,
                                state,
                                catchUpAt,
                                RetoldMobRules.hungerInterval(wolf)
                        ),
                        "The Wolf's blocked-prey gap must be queued"
                ))
                .thenWaitUntil(() -> {
                    RetoldUnloadedEcosystemCatchUp.processPending(
                            RetoldUnloadedEcosystemCatchUp.MAX_TASKS_PER_TICK
                    );
                    helper.assertValueEqual(
                            RetoldUnloadedEcosystemCatchUp.pendingCount(),
                            0,
                            "Blocked-prey reconciliation must drain after its bounded path probe"
                    );
                    helper.assertFalse(
                            incompatibleCow.isRemoved(),
                            "Offline predation must never bypass the loaded prey diet"
                    );
                    helper.assertFalse(
                            blockedSheep.isRemoved(),
                            "Offline predation must not remove compatible prey through a closed barrier"
                    );
                    helper.assertValueEqual(
                            state.hunger(),
                            52,
                            "Rejected prey must grant no offline hunger relief"
                    );
                    helper.assertValueEqual(
                            state.lastSuccessfulHuntAt(),
                            0L,
                            "Rejected prey must not persist a successful hunt"
                    );
                })
                .thenExecute(RetoldUnloadedEcosystemCatchUp::clear)
                .thenSucceed();
    }

    private static void predationUsesFeederFirst(GameTestHelper helper) {
        RetoldUnloadedEcosystemCatchUp.clear();
        placeFloor(helper);
        BlockPos feederPos = new BlockPos(4, 2, 2);
        helper.setBlock(feederPos, RetoldBlocks.ANIMAL_FEEDER.get());
        AnimalFeederBlockEntity feeder = feederAt(helper, feederPos);
        feeder.setItem(0, new ItemStack(Items.BEEF));
        Wolf wolf = helper.spawn(EntityTypes.WOLF, 2, 2, 2);
        Sheep sheep = helper.spawn(EntityTypes.SHEEP, 2, 2, 4);
        long lastSimulatedAt = Math.max(1L, helper.getLevel().getGameTime());
        long catchUpAt = lastSimulatedAt + 24_000L;
        RetoldMobState state = RetoldMobStates.getOrCreate(
                wolf,
                lastSimulatedAt
        );

        try {
            state.setHunger(0);
            state.markHungerTick(lastSimulatedAt);
            helper.assertTrue(
                    RetoldUnloadedEcosystemCatchUp.deferLongGap(
                            helper.getLevel(),
                            wolf,
                            state,
                            catchUpAt,
                            RetoldMobRules.hungerInterval(wolf)
                    ),
                    "The fed-predator gap must be queued"
            );
            RetoldUnloadedEcosystemCatchUp.processPending(
                    RetoldUnloadedEcosystemCatchUp.MAX_TASKS_PER_TICK
            );
            helper.assertValueEqual(
                    RetoldUnloadedEcosystemCatchUp.pendingCount(),
                    0,
                    "Feeder-first reconciliation must drain its queued task"
            );
            helper.assertTrue(
                    feeder.getItem(0).isEmpty(),
                    "The compatible feeder meal must be consumed first"
            );
            helper.assertFalse(
                    sheep.isRemoved(),
                    "Available feeder food must prevent unnecessary offline predation"
            );
            helper.assertValueEqual(
                    state.hunger(),
                    24,
                    "The feeder meal must apply ordinary predator food relief"
            );
            helper.assertValueEqual(
                    state.lastSuccessfulHuntAt(),
                    0L,
                    "A feeder meal must not be recorded as a successful hunt"
            );
            helper.succeed();
        } finally {
            RetoldUnloadedEcosystemCatchUp.clear();
            wolf.discard();
            sheep.discard();
        }
    }

    private static void breedingUsesFoodWithoutPopulationCap(
            GameTestHelper helper
    ) {
        RetoldUnloadedEcosystemCatchUp.clear();
        buildBreedingArena(helper);
        Cow first = helper.spawn(EntityTypes.COW, 2, 2, 2);
        Cow second = helper.spawn(EntityTypes.COW, 3, 2, 2);
        Cow hungry = helper.spawn(EntityTypes.COW, 5, 2, 2);
        List<Cow> crowdedCows = new ArrayList<>();
        long lastSimulatedAt = Math.max(
                1L,
                helper.getLevel().getGameTime()
        );
        long catchUpAt = lastSimulatedAt + 6_600L;
        RetoldMobState firstState = RetoldMobStates.getOrCreate(
                first,
                lastSimulatedAt
        );
        RetoldMobState secondState = RetoldMobStates.getOrCreate(
                second,
                lastSimulatedAt
        );
        RetoldMobState hungryState = RetoldMobStates.getOrCreate(
                hungry,
                lastSimulatedAt
        );

        try {
            firstState.setHunger(0);
            secondState.setHunger(0);
            hungryState.setHunger(21);
            hungryState.setBreedingSatisfiedTicks(
                    RetoldAnimalBreeding.SATISFIED_TICKS - 100L
            );
            firstState.markHungerTick(lastSimulatedAt);
            secondState.markHungerTick(lastSimulatedAt);
            hungryState.markHungerTick(lastSimulatedAt);

            for (int index = 0; index < 8; index++) {
                Cow crowd = helper.spawn(
                        EntityTypes.COW,
                        10 + index % 4,
                        2,
                        2 + index / 4
                );
                RetoldMobStates.getOrCreate(crowd, lastSimulatedAt)
                        .setHunger(100);
                crowdedCows.add(crowd);
            }

            helper.assertTrue(
                    RetoldUnloadedEcosystemCatchUp.deferLongGap(
                            helper.getLevel(),
                            first,
                            firstState,
                            catchUpAt,
                            RetoldMobRules.hungerInterval(first)
                    ) && RetoldUnloadedEcosystemCatchUp.deferLongGap(
                            helper.getLevel(),
                            second,
                            secondState,
                            catchUpAt,
                            RetoldMobRules.hungerInterval(second)
                    ) && RetoldUnloadedEcosystemCatchUp.deferLongGap(
                            helper.getLevel(),
                            hungry,
                            hungryState,
                            catchUpAt,
                            RetoldMobRules.hungerInterval(hungry)
                    ),
                    "Every returning breeder must enter the bounded catch-up queue"
            );
            RetoldUnloadedEcosystemCatchUp.processPending(
                    RetoldUnloadedEcosystemCatchUp.MAX_TASKS_PER_TICK
            );

            helper.assertTrue(
                    firstState.breedingSatisfiedTicks()
                            >= RetoldAnimalBreeding.SATISFIED_TICKS
                            && secondState.breedingSatisfiedTicks()
                            >= RetoldAnimalBreeding.SATISFIED_TICKS,
                    "A continuously full unloaded pair must become breeding-ready"
            );
            helper.assertValueEqual(
                    hungryState.breedingSatisfiedTicks(),
                    0L,
                    "Unloaded hunger must reset earlier breeding satisfaction"
            );
            helper.assertTrue(
                    helper.getLevel().getEntitiesOfClass(
                            Cow.class,
                            first.getBoundingBox().inflate(12.0D)
                    ).size() >= 11,
                    "The fixture must be crowded enough to guard the no-cap rule"
            );

            int firstHungerBeforeBirth = firstState.hunger();
            int secondHungerBeforeBirth = secondState.hunger();
            RetoldAnimalBreeding.tick(helper.getLevel(), first, catchUpAt);
            helper.assertTrue(
                    first.isInLove() && second.isInLove(),
                    "Food-earned readiness must arm a compatible pair even in a crowded area"
            );

            helper.succeedWhen(() -> {
                long babies = helper.getLevel().getEntitiesOfClass(
                        Cow.class,
                        first.getBoundingBox().inflate(12.0D)
                ).stream().filter(Cow::isBaby).count();
                helper.assertTrue(
                        babies >= 1L,
                        "Catch-up readiness must return actual offspring creation to vanilla"
                );
                helper.assertValueEqual(
                        firstState.hunger(),
                        firstHungerBeforeBirth
                                + RetoldAnimalBreeding.PARENT_HUNGER_COST,
                        "The first parent must pay the normal breeding hunger cost"
                );
                helper.assertValueEqual(
                        secondState.hunger(),
                        secondHungerBeforeBirth
                                + RetoldAnimalBreeding.PARENT_HUNGER_COST,
                        "The second parent must pay the normal breeding hunger cost"
                );
            });
        } finally {
            RetoldUnloadedEcosystemCatchUp.clear();
        }
    }

    private static void landMigrationRequiresOneDayAndNoFood(
            GameTestHelper helper
    ) {
        RetoldUnloadedEcosystemCatchUp.clear();
        buildMigrationLandArena(helper);
        Cow shortGapCow = helper.spawn(EntityTypes.COW, 4, 2, 4);
        Cow fedCow = helper.spawn(EntityTypes.COW, 4, 2, 12);
        BlockPos feederPos = new BlockPos(5, 2, 12);
        helper.setBlock(feederPos, RetoldBlocks.ANIMAL_FEEDER.get());
        AnimalFeederBlockEntity feeder = feederAt(helper, feederPos);
        feeder.setItem(0, new ItemStack(Items.WHEAT, 2));
        long lastSimulatedAt = Math.max(1L, helper.getLevel().getGameTime());
        RetoldAnimalHomeMemory shortGapHome = RetoldAnimalHomes.replacePackHome(
                helper.getLevel(),
                shortGapCow,
                List.of(),
                shortGapCow.blockPosition(),
                lastSimulatedAt
        );
        RetoldAnimalHomeMemory fedHome = RetoldAnimalHomes.replacePackHome(
                helper.getLevel(),
                fedCow,
                List.of(),
                fedCow.blockPosition(),
                lastSimulatedAt
        );
        RetoldMobState shortGapState = RetoldMobStates.getOrCreate(
                shortGapCow,
                lastSimulatedAt
        );
        RetoldMobState fedState = RetoldMobStates.getOrCreate(
                fedCow,
                lastSimulatedAt
        );

        try {
            shortGapState.setHunger(100);
            shortGapState.markHungerTick(lastSimulatedAt);
            fedState.setHunger(100);
            fedState.markHungerTick(lastSimulatedAt);
            helper.assertTrue(
                    RetoldUnloadedEcosystemCatchUp.deferLongGap(
                            helper.getLevel(),
                            shortGapCow,
                            shortGapState,
                            lastSimulatedAt + 12_000L,
                            RetoldMobRules.hungerInterval(shortGapCow)
                    ),
                    "A half-day gap must still use normal bounded metabolism catch-up"
            );
            helper.assertTrue(
                    RetoldUnloadedEcosystemCatchUp.deferLongGap(
                            helper.getLevel(),
                            fedCow,
                            fedState,
                            lastSimulatedAt + 24_000L,
                            RetoldMobRules.hungerInterval(fedCow)
                    ),
                    "A full-day supplied gap must enter catch-up"
            );

            RetoldUnloadedEcosystemCatchUp.processPending(
                    RetoldUnloadedEcosystemCatchUp.MAX_TASKS_PER_TICK
            );
            helper.assertTrue(
                    RetoldAnimalHomes.get(shortGapCow).pos().equals(shortGapHome.pos())
                            && shortGapCow.blockPosition().distSqr(shortGapHome.pos()) <= 2.0D,
                    "Less than one unloaded day must not physically migrate an animal"
            );
            helper.assertTrue(
                    RetoldAnimalHomes.get(fedCow).pos().equals(fedHome.pos())
                            && fedCow.blockPosition().distSqr(fedHome.pos()) <= 2.0D,
                    "Compatible food at the placed range must anchor the returning animal"
            );
            helper.assertValueEqual(
                    feeder.getItem(0).getCount(),
                    1,
                    "The anchored animal must consume its real daily meal"
            );
            helper.assertValueEqual(
                    RetoldUnloadedMigration.pendingCount(),
                    0,
                    "Neither an undersized gap nor a supplied range may leave migration work"
            );
            helper.succeed();
        } finally {
            RetoldUnloadedEcosystemCatchUp.clear();
        }
    }

    private static void landMigrationRelocatesReachableHerd(
            GameTestHelper helper
    ) {
        RetoldUnloadedEcosystemCatchUp.clear();
        buildMigrationLandArena(helper);
        var cow = helper.spawn(EntityTypes.COW, 4, 2, 8);
        var mooshroom = helper.spawn(EntityTypes.MOOSHROOM, 5, 2, 8);
        long lastSimulatedAt = Math.max(1L, helper.getLevel().getGameTime());
        long catchUpAt = lastSimulatedAt + 24_000L;
        RetoldAnimalHomeMemory originalHome = RetoldAnimalHomes.replacePackHome(
                helper.getLevel(),
                cow,
                List.of(mooshroom),
                cow.blockPosition(),
                lastSimulatedAt
        );

        helper.startSequence()
                .thenIdle(5)
                .thenExecute(() -> {
                    queueHungryCatchUp(helper, cow, lastSimulatedAt, catchUpAt);
                    queueHungryCatchUp(helper, mooshroom, lastSimulatedAt, catchUpAt);
                    RetoldUnloadedEcosystemCatchUp.processPending(
                            RetoldUnloadedEcosystemCatchUp.MAX_TASKS_PER_TICK
                    );
                    helper.assertTrue(
                            RetoldUnloadedMigration.pendingCount() >= 1,
                            "An eligible hungry herd must enter the bounded migration queue"
                    );
                })
                .thenWaitUntil(() -> {
                    RetoldUnloadedMigration.processPending(
                            RetoldUnloadedMigration.MAX_GROUPS_PER_TICK
                    );
                    RetoldAnimalHomeMemory migratedHome = RetoldAnimalHomes.get(cow);
                    helper.assertTrue(
                            migratedHome != null
                                    && !migratedHome.pos().equals(originalHome.pos()),
                            "Budgeted herd migration must eventually select a better range; "
                                    + "pending=" + RetoldUnloadedMigration.pendingCount()
                                    + ", mode=" + RetoldAiControl.getMode(cow)
                                    + ", hunger=" + RetoldMobStates.get(cow).hunger()
                                    + ", onGround=" + cow.onGround()
                    );
                })
                .thenExecute(() -> {
                    try {
                        RetoldAnimalHomeMemory migratedHome = RetoldAnimalHomes.get(cow);
                        helper.assertTrue(
                                originalHome.pos().distSqr(migratedHome.pos())
                                        <= 32.0D * 32.0D,
                                "One foodless unloaded day may move the herd only to a nearby better range"
                        );
                        helper.assertTrue(
                                RetoldAnimalHomes.hasSameValidHomeAs(
                                        helper.getLevel(),
                                        mooshroom,
                                        migratedHome
                                ),
                                "The complete real herd must keep one migrated persisted range"
                        );
                        helper.assertTrue(
                                cow.blockPosition().distSqr(migratedHome.pos()) <= 5.0D * 5.0D
                                        && mooshroom.blockPosition().distSqr(migratedHome.pos())
                                        <= 5.0D * 5.0D,
                                "Every real herd member must return at a distinct safe landing near the range"
                        );
                    } finally {
                        RetoldUnloadedEcosystemCatchUp.clear();
                    }
                })
                .thenSucceed();
    }

    private static void landMigrationRespectsClosedBarriers(
            GameTestHelper helper
    ) {
        RetoldUnloadedEcosystemCatchUp.clear();
        buildMigrationLandArena(helper);

        for (int z = 0; z <= 16; z++) {
            for (int y = 2; y <= 5; y++) {
                helper.setBlock(new BlockPos(12, y, z), Blocks.STONE);
            }
        }

        var cow = helper.spawn(EntityTypes.COW, 4, 2, 8);
        var mooshroom = helper.spawn(EntityTypes.MOOSHROOM, 5, 2, 8);
        long lastSimulatedAt = Math.max(1L, helper.getLevel().getGameTime());
        long catchUpAt = lastSimulatedAt + 24_000L;
        RetoldAnimalHomeMemory originalHome = RetoldAnimalHomes.replacePackHome(
                helper.getLevel(),
                cow,
                List.of(mooshroom),
                cow.blockPosition(),
                lastSimulatedAt
        );

        helper.startSequence()
                .thenIdle(5)
                .thenExecute(() -> {
                    queueHungryCatchUp(helper, cow, lastSimulatedAt, catchUpAt);
                    queueHungryCatchUp(helper, mooshroom, lastSimulatedAt, catchUpAt);
                    RetoldUnloadedEcosystemCatchUp.processPending(
                            RetoldUnloadedEcosystemCatchUp.MAX_TASKS_PER_TICK
                    );
                    helper.assertTrue(
                            RetoldUnloadedMigration.pendingCount() >= 1,
                            "The unreachable hungry herd must still attempt bounded migration"
                    );
                })
                .thenWaitUntil(() -> {
                    RetoldUnloadedMigration.processPending(
                            RetoldUnloadedMigration.MAX_GROUPS_PER_TICK
                    );
                    helper.assertValueEqual(
                            RetoldUnloadedMigration.pendingCount(),
                            0,
                            "The bounded unreachable migration attempt must eventually finish"
                    );
                })
                .thenExecute(() -> {
                    try {
                        helper.assertTrue(
                                RetoldAnimalHomes.get(cow).pos().equals(originalHome.pos())
                                        && RetoldAnimalHomes.hasSameValidHomeAs(
                                        helper.getLevel(),
                                        mooshroom,
                                        originalHome
                                ),
                                "An unreachable forage region must not change the shared persisted range"
                        );
                        helper.assertTrue(
                                cow.getX() < helper.absolutePos(new BlockPos(12, 2, 8)).getX()
                                        && mooshroom.getX()
                                        < helper.absolutePos(new BlockPos(12, 2, 8)).getX(),
                                "Unloaded migration must not teleport real animals across a closed barrier"
                        );
                    } finally {
                        RetoldUnloadedEcosystemCatchUp.clear();
                    }
                })
                .thenSucceed();
    }

    private static void aquaticMigrationRelocatesReachableSchool(
            GameTestHelper helper
    ) {
        RetoldUnloadedEcosystemCatchUp.clear();
        buildMigrationWaterArena(helper);
        placeAquaticForagePatch(helper, 13, 19, 5, 11);
        var first = helper.spawn(EntityTypes.COD, 4, 3, 8);
        var second = helper.spawn(EntityTypes.COD, 5, 3, 8);
        long lastSimulatedAt = Math.max(1L, helper.getLevel().getGameTime());
        long catchUpAt = lastSimulatedAt + 24_000L;
        RetoldAnimalHomeMemory originalHome = RetoldAnimalHomes.replacePackHome(
                helper.getLevel(),
                first,
                List.of(second),
                first.blockPosition(),
                lastSimulatedAt
        );

        helper.startSequence()
                .thenIdle(5)
                .thenExecute(() -> {
                    queueHungryCatchUp(helper, first, lastSimulatedAt, catchUpAt);
                    queueHungryCatchUp(helper, second, lastSimulatedAt, catchUpAt);
                    RetoldUnloadedEcosystemCatchUp.processPending(
                            RetoldUnloadedEcosystemCatchUp.MAX_TASKS_PER_TICK
                    );
                    helper.assertTrue(
                            RetoldUnloadedMigration.pendingCount() >= 1,
                            "An eligible hungry school must enter the bounded migration queue"
                    );
                })
                .thenWaitUntil(() -> {
                    RetoldUnloadedMigration.processPending(
                            RetoldUnloadedMigration.MAX_GROUPS_PER_TICK
                    );
                    RetoldAnimalHomeMemory migratedHome = RetoldAnimalHomes.get(first);
                    helper.assertTrue(
                            migratedHome != null
                                    && !migratedHome.pos().equals(originalHome.pos()),
                            "Budgeted school migration must eventually select a better range; "
                                    + "pending=" + RetoldUnloadedMigration.pendingCount()
                                    + ", mode=" + RetoldAiControl.getMode(first)
                                    + ", hunger=" + RetoldMobStates.get(first).hunger()
                                    + ", inWater=" + first.isInWater()
                    );
                })
                .thenExecute(() -> {
                    try {
                        RetoldAnimalHomeMemory migratedHome = RetoldAnimalHomes.get(first);
                        helper.assertTrue(
                                originalHome.pos().distSqr(migratedHome.pos())
                                        <= 32.0D * 32.0D,
                                "A hungry school may move only to a nearby better aquatic range"
                        );
                        helper.assertTrue(
                                RetoldAnimalHomes.hasSameValidHomeAs(
                                        helper.getLevel(),
                                        second,
                                        migratedHome
                                )
                                        && first.blockPosition().distSqr(migratedHome.pos())
                                        <= 5.0D * 5.0D
                                        && second.blockPosition().distSqr(migratedHome.pos())
                                        <= 5.0D * 5.0D,
                                "Every real school member must share the migrated range and a water landing"
                        );
                        helper.assertTrue(
                                first.isInWater() && second.isInWater(),
                                "Physical school migration must preserve aquatic placement"
                        );
                    } finally {
                        RetoldUnloadedEcosystemCatchUp.clear();
                    }
                })
                .thenSucceed();
    }

    private static void pigMigrationRelocatesForagingGroup(
            GameTestHelper helper
    ) {
        RetoldUnloadedEcosystemCatchUp.clear();
        buildPigMigrationArena(helper);
        var first = helper.spawn(EntityTypes.PIG, 4, 2, 8);
        var second = helper.spawn(EntityTypes.PIG, 5, 2, 8);
        long lastSimulatedAt = Math.max(1L, helper.getLevel().getGameTime());
        long catchUpAt = lastSimulatedAt + 24_000L;
        RetoldAnimalHomeMemory originalHome = RetoldAnimalHomes.replacePackHome(
                helper.getLevel(),
                first,
                List.of(second),
                first.blockPosition(),
                lastSimulatedAt
        );

        helper.startSequence()
                .thenIdle(5)
                .thenExecute(() -> {
                    int currentScore = RetoldRangeForage.forageScore(
                            helper.getLevel(),
                            first,
                            originalHome.pos(),
                            8,
                            2
                    );
                    BlockPos candidate = RetoldRangeForage.findBetterForageCenter(
                            helper.getLevel(),
                            first,
                            originalHome.pos(),
                            8,
                            2,
                            currentScore,
                            12
                    );
                    helper.assertTrue(
                            currentScore <= 5 && candidate != null,
                            "The Pig fixture needs a depleted old range and a viable plant range; "
                                    + "currentScore=" + currentScore + ", candidate=" + candidate
                    );
                    queueHungryCatchUp(helper, first, lastSimulatedAt, catchUpAt);
                    queueHungryCatchUp(helper, second, lastSimulatedAt, catchUpAt);
                    RetoldUnloadedEcosystemCatchUp.processPending(
                            RetoldUnloadedEcosystemCatchUp.MAX_TASKS_PER_TICK
                    );
                    helper.assertTrue(
                            RetoldUnloadedMigration.pendingCount() >= 1,
                            "An eligible Pig group must enter unloaded migration"
                    );
                })
                .thenWaitUntil(() -> {
                    RetoldUnloadedMigration.processPending(
                            RetoldUnloadedMigration.MAX_GROUPS_PER_TICK
                    );
                    RetoldAnimalHomeMemory migratedHome = RetoldAnimalHomes.get(first);
                    helper.assertTrue(
                            migratedHome != null
                                    && !migratedHome.pos().equals(originalHome.pos()),
                            "Budgeted Pig migration must eventually select a better range; "
                                    + "pending=" + RetoldUnloadedMigration.pendingCount()
                                    + ", mode=" + RetoldAiControl.getMode(first)
                                    + ", hunger=" + RetoldMobStates.get(first).hunger()
                                    + ", onGround=" + first.onGround()
                    );
                })
                .thenExecute(() -> {
                    try {
                        RetoldAnimalHomeMemory migratedHome = RetoldAnimalHomes.get(first);
                        helper.assertTrue(
                                migratedHome.type() == RetoldAnimalHomeType.FORAGING_RANGE
                                        && RetoldAnimalHomes.hasSameValidHomeAs(
                                        helper.getLevel(),
                                        second,
                                        migratedHome
                                ),
                                "Pigs must relocate their real shared foraging range to better plants"
                        );
                    } finally {
                        RetoldUnloadedEcosystemCatchUp.clear();
                    }
                })
                .thenSucceed();
    }

    private static void queueHungryCatchUp(
            GameTestHelper helper,
            PathfinderMob mob,
            long lastSimulatedAt,
            long catchUpAt
    ) {
        RetoldMobState state = RetoldMobStates.getOrCreate(
                mob,
                lastSimulatedAt
        );
        mob.getNavigation().stop();
        RetoldAiControl.clear(mob);
        mob.setSprinting(false);

        if (mob.level().getFluidState(mob.blockPosition()).is(
                net.minecraft.tags.FluidTags.WATER
        ) && !mob.isInWater()) {
            mob.baseTick();
        } else if (!mob.isInWater()) {
            mob.setOnGround(true);
        }

        int hungerPulses = RetoldUnloadedCatchUpPlan.calculate(
                lastSimulatedAt,
                catchUpAt,
                RetoldMobRules.hungerInterval(mob)
        ).hungerPulses();
        // Arrive very hungry but below the first critical pulse; starvation damage legitimately
        // acquires FLEE and therefore overrides otherwise eligible migration.
        state.setHunger(Math.max(0, 99 - hungerPulses));
        state.markHungerTick(lastSimulatedAt);
        helper.assertTrue(
                RetoldUnloadedEcosystemCatchUp.deferLongGap(
                        helper.getLevel(),
                        mob,
                        state,
                        catchUpAt,
                        RetoldMobRules.hungerInterval(mob)
                ),
                "The full-day hungry migration fixture must enter catch-up"
        );
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

    private static Container containerAt(
            GameTestHelper helper,
            BlockPos relativePos
    ) {
        BlockEntity blockEntity = helper.getLevel().getBlockEntity(
                helper.absolutePos(relativePos)
        );
        helper.assertTrue(
                blockEntity instanceof Container,
                "The fixture storage must provide a container"
        );
        return (Container) blockEntity;
    }

    private static void placeFloor(GameTestHelper helper) {
        for (int x = 0; x <= 7; x++) {
            for (int z = 0; z <= 5; z++) {
                helper.setBlock(x, 1, z, Blocks.STONE);
            }
        }
    }

    private static void placeFarmerRouteFloor(GameTestHelper helper) {
        for (int x = -4; x <= 12; x++) {
            for (int z = -4; z <= 8; z++) {
                helper.setBlock(x, 1, z, Blocks.STONE);
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

                helper.setBlock(new BlockPos(x, 7, z), Blocks.GLASS);
            }
        }
    }

    private static void buildPigMigrationArena(GameTestHelper helper) {
        for (int x = 0; x <= 32; x++) {
            for (int z = 0; z <= 16; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
            }
        }

        for (int x = 17; x <= 23; x++) {
            for (int z = 5; z <= 11; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.GRASS_BLOCK);
                helper.setBlock(new BlockPos(x, 2, z), Blocks.SHORT_GRASS);
            }
        }
    }

    private static void buildBreedingArena(GameTestHelper helper) {
        for (int x = 0; x <= 14; x++) {
            for (int z = 0; z <= 5; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
            }
        }

        for (int x = 8; x <= 14; x++) {
            for (int y = 2; y <= 4; y++) {
                helper.setBlock(new BlockPos(x, y, 0), Blocks.STONE);
                helper.setBlock(new BlockPos(x, y, 5), Blocks.STONE);
            }
        }

        for (int z = 0; z <= 5; z++) {
            for (int y = 2; y <= 4; y++) {
                helper.setBlock(new BlockPos(8, y, z), Blocks.STONE);
                helper.setBlock(new BlockPos(14, y, z), Blocks.STONE);
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

    private static void buildWaterArena(GameTestHelper helper) {
        for (int x = 0; x <= 7; x++) {
            for (int z = 0; z <= 5; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.GLASS);

                for (int y = 2; y <= 4; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.WATER);
                }
            }
        }
    }

    private static void registerTest(
            RegisterGameTestsEvent event,
            String name,
            Consumer<GameTestHelper> test
    ) {
        Holder<TestEnvironmentDefinition<?>> environment =
                event.registerEnvironment(
                        id("isolated_" + name),
                        new TestEnvironmentDefinition.AllOf()
                );
        TestData<Holder<TestEnvironmentDefinition<?>>> testData =
                new TestData<>(
                        environment,
                        EMPTY_STRUCTURE,
                        240,
                        0,
                        true,
                        Rotation.NONE,
                        false,
                        1,
                        1,
                        false,
                        TEST_PADDING
                );

        event.registerTest(id(name), new InlineGameTest(testData, test));
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
