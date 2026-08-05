package cz.xefensor.retold.villager;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.control.RetoldAiControlMode;
import cz.xefensor.retold.behavior.control.RetoldAiControlOwner;
import cz.xefensor.retold.behavior.profiles.RetoldMobState;
import cz.xefensor.retold.behavior.profiles.RetoldMobStates;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.BuiltinTestFunctions;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.function.Consumer;

public final class RetoldVillagerCommunalFoodGameTests {
    private static final Identifier EMPTY_STRUCTURE =
            Identifier.withDefaultNamespace("empty");

    private RetoldVillagerCommunalFoodGameTests() {
    }

    public static void register(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment =
                event.registerEnvironment(
                        id("isolated_villager_communal_food"),
                        new TestEnvironmentDefinition.AllOf()
                );
        Holder<TestEnvironmentDefinition<?>> pathingEnvironment =
                event.registerEnvironment(
                        id("isolated_villager_communal_food_pathing"),
                        new TestEnvironmentDefinition.AllOf()
                );
        Holder<TestEnvironmentDefinition<?>> supplyPathingEnvironment =
                event.registerEnvironment(
                        id("isolated_villager_communal_supply_pathing"),
                        new TestEnvironmentDefinition.AllOf()
                );

        registerTest(
                event,
                environment,
                "villager_communal_food_consumes_and_persists_exactly_one_item",
                80,
                RetoldVillagerCommunalFoodGameTests::villagerCommunalFoodConsumesAndPersistsExactlyOneItem
        );
        registerTest(
                event,
                environment,
                "villager_communal_food_uses_personal_stock_before_storage",
                80,
                RetoldVillagerCommunalFoodGameTests::villagerUsesPersonalFoodBeforeStorage
        );
        registerTest(
                event,
                environment,
                "villager_communal_food_supports_barrels_but_rejects_other_inventories",
                80,
                RetoldVillagerCommunalFoodGameTests::villagerCommunalFoodSupportsBarrelsButRejectsOtherInventories
        );
        registerTest(
                event,
                environment,
                "villager_communal_food_stays_inside_its_village_and_yields_to_panic",
                80,
                RetoldVillagerCommunalFoodGameTests::villagerCommunalFoodStaysInsideItsVillageAndYieldsToPanic
        );
        registerTest(
                event,
                pathingEnvironment,
                "villager_paths_to_communal_food_storage",
                240,
                RetoldVillagerCommunalFoodGameTests::villagerPathsToCommunalFoodStorage
        );
        registerTest(
                event,
                environment,
                "farmer_communal_supply_preserves_personal_food",
                80,
                RetoldVillagerCommunalFoodGameTests::farmerDepositsOnlySurplusIntoCommunalStorage
        );
        registerTest(
                event,
                supplyPathingEnvironment,
                "farmer_communal_supply_paths_to_storage",
                240,
                RetoldVillagerCommunalFoodGameTests::farmerPathsToStockCommunalFoodStorage
        );
    }

    private static void villagerCommunalFoodConsumesAndPersistsExactlyOneItem(
            GameTestHelper helper
    ) {
        placeFloor(helper, 1, 5, 1, 3);
        BlockPos chestPos = new BlockPos(4, 2, 2);
        helper.setBlock(chestPos, Blocks.CHEST);
        Container chest = containerAt(helper, chestPos);
        chest.setItem(0, new ItemStack(Items.BREAD, 3));
        chest.setItem(1, new ItemStack(Items.CARROT, 5));
        chest.setItem(2, new ItemStack(Items.STONE, 7));
        chest.setChanged();

        Villager villager = helper.spawn(EntityTypes.VILLAGER, 3, 2, 2);
        setVillageHome(helper, villager, new BlockPos(2, 2, 2));
        long gameTime = helper.getLevel().getGameTime();
        RetoldMobState state = RetoldMobStates.getOrCreate(villager, gameTime);
        state.setHunger(100);

        helper.assertTrue(
                RetoldVillagerCommunalFood.tryConsume(
                        helper.getLevel(),
                        villager,
                        state,
                        helper.absolutePos(chestPos),
                        gameTime
                ),
                "A hungry Villager beside its village chest must consume communal food"
        );
        helper.assertTrue(
                chest.getItem(0).isEmpty()
                        && chest.getItem(1).is(Items.CARROT)
                        && chest.getItem(1).getCount() == 5
                        && chest.getItem(2).is(Items.STONE)
                        && chest.getItem(2).getCount() == 7,
                "Restocking must prefer Bread, take exactly 12 food points, and preserve other items"
        );
        helper.assertTrue(
                villager.getInventory().countItem(Items.BREAD) == 2,
                "The Villager must eat one Bread and retain two personal meals"
        );
        helper.assertValueEqual(
                state.hunger(),
                76,
                "Bread must apply its Villager food relief exactly once"
        );

        CompoundTag stateTag = villager.getPersistentData()
                .getCompoundOrEmpty("RetoldMobState");
        helper.assertValueEqual(
                stateTag.getInt("hunger").orElse(-1),
                76,
                "Villager hunger must be written to persistent entity data"
        );

        BlockEntity chestEntity = helper.getLevel().getBlockEntity(
                helper.absolutePos(chestPos)
        );
        CompoundTag saved = chestEntity.saveWithFullMetadata(
                helper.getLevel().registryAccess()
        );
        BlockEntity loaded = BlockEntity.loadStatic(
                helper.absolutePos(chestPos),
                helper.getBlockState(chestPos),
                saved,
                helper.getLevel().registryAccess()
        );
        helper.assertTrue(
                loaded instanceof Container restored
                        && restored.getItem(0).isEmpty()
                        && restored.getItem(1).is(Items.CARROT)
                        && restored.getItem(1).getCount() == 5
                        && restored.getItem(2).is(Items.STONE)
                        && restored.getItem(2).getCount() == 7,
                "The exact communal chest inventory must survive block-entity serialization"
        );

        chest.clearContent();
        villager.discard();
        helper.succeed();
    }

    private static void villagerUsesPersonalFoodBeforeStorage(
            GameTestHelper helper
    ) {
        placeFloor(helper, 1, 5, 1, 3);
        BlockPos chestPos = new BlockPos(4, 2, 2);
        helper.setBlock(chestPos, Blocks.CHEST);
        Container chest = containerAt(helper, chestPos);
        chest.setItem(0, new ItemStack(Items.BREAD, 4));
        chest.setChanged();

        Villager villager = helper.spawn(EntityTypes.VILLAGER, 3, 2, 2);
        setVillageHome(helper, villager, new BlockPos(2, 2, 2));
        villager.getInventory().setItem(0, new ItemStack(Items.CARROT, 3));
        villager.getInventory().setItem(1, new ItemStack(Items.BREAD, 2));
        long gameTime = helper.getLevel().getGameTime();
        RetoldMobState state = RetoldMobStates.getOrCreate(villager, gameTime);
        state.setHunger(100);

        RetoldVillagerCommunalFood.tick(
                helper.getLevel(),
                villager,
                gameTime
        );

        helper.assertTrue(
                chest.getItem(0).is(Items.BREAD)
                        && chest.getItem(0).getCount() == 4,
                "A Villager with personal food must not remove communal food"
        );
        helper.assertTrue(
                villager.getInventory().countItem(Items.BREAD) == 1
                        && villager.getInventory().countItem(Items.CARROT) == 3,
                "Personal eating must prefer Bread and retain other carried food"
        );
        helper.assertValueEqual(
                state.hunger(),
                76,
                "A personal Bread meal must apply hunger relief exactly once"
        );
        helper.assertTrue(
                !RetoldAiControl.isControlledAsBy(
                        villager,
                        RetoldAiControlMode.SEARCH,
                        RetoldAiControlOwner.FOOD
                ),
                "Personal food must avoid a communal-storage search"
        );

        chest.clearContent();
        villager.discard();
        helper.succeed();
    }

    private static void villagerCommunalFoodSupportsBarrelsButRejectsOtherInventories(
            GameTestHelper helper
    ) {
        placeFloor(helper, 1, 5, 1, 5);
        BlockPos barrelPos = new BlockPos(4, 2, 2);
        BlockPos dispenserPos = new BlockPos(2, 2, 4);
        helper.setBlock(barrelPos, Blocks.BARREL);
        helper.setBlock(dispenserPos, Blocks.DISPENSER);
        Container barrel = containerAt(helper, barrelPos);
        Container dispenser = containerAt(helper, dispenserPos);
        barrel.setItem(0, new ItemStack(Items.CARROT, 20));
        dispenser.setItem(0, new ItemStack(Items.BREAD, 4));
        barrel.setChanged();
        dispenser.setChanged();

        Villager villager = helper.spawn(EntityTypes.VILLAGER, 3, 2, 2);
        setVillageHome(helper, villager, new BlockPos(3, 2, 3));
        long gameTime = helper.getLevel().getGameTime();
        RetoldMobState state = RetoldMobStates.getOrCreate(villager, gameTime);
        state.setHunger(100);

        helper.assertFalse(
                RetoldVillagerCommunalFood.tryConsume(
                        helper.getLevel(),
                        villager,
                        state,
                        helper.absolutePos(dispenserPos),
                        gameTime
                ),
                "Furnaces, hoppers, dispensers, and other machine inventories must not become communal stores"
        );
        helper.assertTrue(
                dispenser.getItem(0).getCount() == 4,
                "Rejected inventories must retain their exact contents"
        );
        helper.assertTrue(
                RetoldVillagerCommunalFood.tryConsume(
                        helper.getLevel(),
                        villager,
                        state,
                        helper.absolutePos(barrelPos),
                        gameTime
                ),
                "An ordinary barrel inside the remembered village must provide communal food"
        );
        helper.assertTrue(
                barrel.getItem(0).is(Items.CARROT)
                        && barrel.getItem(0).getCount() == 8
                        && villager.getInventory().countItem(Items.CARROT) == 11,
                "Barrel restocking must take 12 Carrots, consume one, and retain eleven"
        );

        barrel.clearContent();
        villager.discard();
        helper.succeed();
    }

    private static void villagerCommunalFoodStaysInsideItsVillageAndYieldsToPanic(
            GameTestHelper helper
    ) {
        placeFloor(helper, 1, 7, 1, 4);
        BlockPos chestPos = new BlockPos(4, 2, 2);
        helper.setBlock(chestPos, Blocks.CHEST);
        Container chest = containerAt(helper, chestPos);
        chest.setItem(0, new ItemStack(Items.BREAD, 4));
        chest.setChanged();

        long gameTime = helper.getLevel().getGameTime();
        Villager outsideVillager = helper.spawn(EntityTypes.VILLAGER, 3, 2, 2);
        setVillageHome(helper, outsideVillager, new BlockPos(40, 2, 2));
        RetoldMobState outsideState = RetoldMobStates.getOrCreate(
                outsideVillager,
                gameTime
        );
        outsideState.setHunger(100);
        helper.assertFalse(
                RetoldVillagerCommunalFood.tryConsume(
                        helper.getLevel(),
                        outsideVillager,
                        outsideState,
                        helper.absolutePos(chestPos),
                        gameTime
                ),
                "A Villager must not consume from a container outside its remembered village"
        );

        Villager panickingVillager = helper.spawn(EntityTypes.VILLAGER, 3, 2, 3);
        var threat = helper.spawn(EntityTypes.ZOMBIE, 6, 2, 3);
        setVillageHome(helper, panickingVillager, new BlockPos(3, 2, 3));
        panickingVillager.setLastHurtByMob(threat);
        RetoldMobState panicState = RetoldMobStates.getOrCreate(
                panickingVillager,
                gameTime
        );
        panicState.setHunger(100);
        RetoldVillagerCommunalFood.tick(
                helper.getLevel(),
                panickingVillager,
                gameTime
        );
        helper.assertTrue(
                chest.getItem(0).getCount() == 4
                        && !RetoldAiControl.isControlledAsBy(
                        panickingVillager,
                        RetoldAiControlMode.FEED,
                        RetoldAiControlOwner.FOOD
                ),
                "Panic must retain priority over communal food movement and consumption"
        );

        chest.clearContent();
        outsideVillager.discard();
        panickingVillager.discard();
        threat.discard();
        helper.succeed();
    }

    private static void villagerPathsToCommunalFoodStorage(
            GameTestHelper helper
    ) {
        for (int x = 0; x <= 10; x++) {
            for (int z = 0; z <= 4; z++) {
                helper.setBlock(new BlockPos(x, 19, z), Blocks.STONE);
            }
        }

        BlockPos barrelPos = new BlockPos(8, 20, 2);
        BlockPos meetingPos = new BlockPos(5, 20, 0);
        helper.setBlock(barrelPos, Blocks.BARREL);
        helper.setBlock(meetingPos, Blocks.BELL);
        Container barrel = containerAt(helper, barrelPos);
        barrel.setItem(0, new ItemStack(Items.BREAD, 2));
        barrel.setChanged();

        Villager villager = helper.spawn(EntityTypes.VILLAGER, 1, 20, 2);
        villager.setPersistenceRequired();
        setVillageMeetingPoint(helper, villager, meetingPos);
        RetoldMobState state = RetoldMobStates.getOrCreate(
                villager,
                helper.getLevel().getGameTime()
        );
        state.setHunger(100);
        driveCommunalFoodUntilFirstMeal(
                helper,
                villager,
                barrel,
                48
        );

        helper.succeedWhen(() -> {
            helper.assertTrue(
                    barrel.getItem(0).isEmpty()
                            && villager.getInventory().countItem(Items.BREAD) == 1,
                    "The Villager must path to the barrel, take both Bread, consume one, and retain one"
                            + "; count=" + barrel.getItem(0).getCount()
                            + ", hunger=" + state.hunger()
                            + ", pos=" + villager.blockPosition()
                            + ", control=" + RetoldAiControl.getMode(villager)
                            + ", owner=" + RetoldAiControl.getOwner(villager)
                            + ", reason=" + RetoldAiControl.getReason(villager)
                            + ", navigationDone=" + villager.getNavigation().isDone()
                            + ", home=" + villager.getBrain().hasMemoryValue(MemoryModuleType.HOME)
                            + ", meeting=" + villager.getBrain().hasMemoryValue(MemoryModuleType.MEETING_POINT)
                            + ", job=" + villager.getBrain().hasMemoryValue(MemoryModuleType.JOB_SITE)
                            + ", closeToVillage=" + helper.getLevel().isCloseToVillage(
                            villager.blockPosition(),
                            1
                    )
                            + ", profile=" + RetoldMobRules.profileType(villager)
                            + ", noAi=" + villager.isNoAi()
                            + ", sleeping=" + villager.isSleeping()
                            + ", urgent=" + RetoldVillagerCommunalFood
                            .hasUrgentVanillaActivity(villager)
                            + ", storageValid="
                            + RetoldVillagerCommunalFoodSearch
                            .isVillageStorageWithFood(
                                    helper.getLevel(),
                                    villager,
                                    helper.absolutePos(barrelPos)
                            )
            );
            helper.assertTrue(
                    state.hunger() == 76,
                    "Natural communal feeding must apply one Bread meal"
            );
            helper.assertTrue(
                    villager.distanceToSqr(
                            net.minecraft.world.phys.Vec3.atCenterOf(
                                    helper.absolutePos(barrelPos)
                            )
                    ) <= 3.0D * 3.0D,
                    "The Villager must physically reach the communal barrel"
            );
            villager.discard();
        });
    }

    private static void farmerDepositsOnlySurplusIntoCommunalStorage(
            GameTestHelper helper
    ) {
        placeFloor(helper, 1, 6, 1, 5);
        BlockPos chestPos = new BlockPos(4, 2, 2);
        BlockPos dispenserPos = new BlockPos(3, 2, 4);
        helper.setBlock(chestPos, Blocks.CHEST);
        helper.setBlock(dispenserPos, Blocks.DISPENSER);
        Container chest = containerAt(helper, chestPos);
        Container dispenser = containerAt(helper, dispenserPos);
        chest.setItem(0, new ItemStack(Items.BREAD, 63));
        dispenser.setItem(0, new ItemStack(Items.BREAD, 1));
        chest.setChanged();
        dispenser.setChanged();

        Villager farmer = helper.spawn(EntityTypes.VILLAGER, 3, 2, 2);
        setFarmerProfession(helper, farmer);
        setVillageHome(helper, farmer, new BlockPos(3, 2, 3));
        farmer.getInventory().setItem(0, new ItemStack(Items.BREAD, 8));
        farmer.getInventory().setItem(
                1,
                new ItemStack(Items.WHEAT_SEEDS, 12)
        );
        farmer.getInventory().setItem(2, new ItemStack(Items.STONE, 5));

        helper.assertValueEqual(
                RetoldVillagerCommunalSupply.tryDeposit(
                        helper.getLevel(),
                        farmer,
                        helper.absolutePos(dispenserPos)
                ),
                0,
                "Farmers must not deposit food into machine inventories"
        );
        helper.assertValueEqual(
                RetoldVillagerCommunalSupply.tryDeposit(
                        helper.getLevel(),
                        farmer,
                        helper.absolutePos(chestPos)
                ),
                2,
                "The farmer must deposit exactly the food above its personal reserve"
        );
        helper.assertTrue(
                farmer.getInventory().getItem(0).is(Items.BREAD)
                        && farmer.getInventory().getItem(0).getCount() == 6,
                "The farmer must retain the vanilla 24-point personal food reserve"
        );
        helper.assertTrue(
                farmer.getInventory().getItem(1).is(Items.WHEAT_SEEDS)
                        && farmer.getInventory().getItem(1).getCount() == 12
                        && farmer.getInventory().getItem(2).is(Items.STONE)
                        && farmer.getInventory().getItem(2).getCount() == 5,
                "Depositing food must preserve seeds and unrelated inventory items"
        );
        helper.assertValueEqual(
                chest.countItem(Items.BREAD),
                65,
                "The chest must receive the exact two-item surplus without duplication"
        );
        helper.assertValueEqual(
                dispenser.countItem(Items.BREAD),
                1,
                "Rejected machine inventories must remain unchanged"
        );
        helper.assertValueEqual(
                RetoldVillagerCommunalSupply.foodPointsInInventory(farmer),
                RetoldVillagerCommunalSupply.PERSONAL_FOOD_RESERVE,
                "The farmer's retained food must equal the personal reserve"
        );

        Villager nonFarmer = helper.spawn(EntityTypes.VILLAGER, 3, 2, 3);
        setVillageHome(helper, nonFarmer, new BlockPos(3, 2, 3));
        nonFarmer.getInventory().setItem(0, new ItemStack(Items.BREAD, 8));
        helper.assertValueEqual(
                RetoldVillagerCommunalSupply.tryDeposit(
                        helper.getLevel(),
                        nonFarmer,
                        helper.absolutePos(chestPos)
                ),
                0,
                "Only farmers may stock communal village food"
        );

        Villager panickingFarmer = helper.spawn(
                EntityTypes.VILLAGER,
                3,
                2,
                4
        );
        var threat = helper.spawn(EntityTypes.ZOMBIE, 6, 2, 4);
        setFarmerProfession(helper, panickingFarmer);
        setVillageHome(helper, panickingFarmer, new BlockPos(3, 2, 3));
        panickingFarmer.getInventory().setItem(
                0,
                new ItemStack(Items.BREAD, 8)
        );
        RetoldMobStates.getOrCreate(
                panickingFarmer,
                helper.getLevel().getGameTime()
        ).setHunger(0);
        panickingFarmer.setLastHurtByMob(threat);
        RetoldVillagerCommunalSupply.tick(
                helper.getLevel(),
                panickingFarmer,
                helper.getLevel().getGameTime()
        );
        helper.assertTrue(
                chest.countItem(Items.BREAD) == 65
                        && !RetoldAiControl.isControlledBy(
                        panickingFarmer,
                        RetoldAiControlOwner.VILLAGER_COMMUNAL
                ),
                "Danger must interrupt farmer stocking without moving any food"
        );

        chest.clearContent();
        dispenser.clearContent();
        farmer.discard();
        nonFarmer.discard();
        panickingFarmer.discard();
        threat.discard();
        helper.succeed();
    }

    private static void farmerPathsToStockCommunalFoodStorage(
            GameTestHelper helper
    ) {
        for (int x = 0; x <= 10; x++) {
            for (int z = 0; z <= 4; z++) {
                helper.setBlock(new BlockPos(x, 39, z), Blocks.STONE);
            }
        }

        BlockPos barrelPos = new BlockPos(8, 40, 2);
        BlockPos meetingPos = new BlockPos(5, 40, 0);
        BlockPos jobPos = new BlockPos(2, 40, 0);
        helper.setBlock(barrelPos, Blocks.BARREL);
        helper.setBlock(meetingPos, Blocks.BELL);
        helper.setBlock(jobPos, Blocks.COMPOSTER);
        Container barrel = containerAt(helper, barrelPos);

        Villager farmer = helper.spawn(EntityTypes.VILLAGER, 1, 40, 2);
        farmer.setPersistenceRequired();
        setFarmerProfession(helper, farmer);
        setVillageMeetingPoint(helper, farmer, meetingPos);
        setVillageJobSite(helper, farmer, jobPos);
        farmer.getInventory().setItem(0, new ItemStack(Items.BREAD, 8));
        RetoldMobState state = RetoldMobStates.getOrCreate(
                farmer,
                helper.getLevel().getGameTime()
        );
        state.setHunger(0);
        driveCommunalSupplyUntilStocked(helper, farmer, barrel, 48);

        helper.succeedWhen(() -> {
            helper.assertValueEqual(
                    barrel.countItem(Items.BREAD),
                    2,
                    "The farmer must path to and stock its two-item Bread surplus"
                            + "; inventory=" + farmer.getInventory()
                            + ", profession=" + farmer.getVillagerData()
                            .profession()
                            + ", hunger=" + state.hunger()
                            + ", pos=" + farmer.blockPosition()
                            + ", activity=" + farmer.getBrain()
                            .getActiveNonCoreActivity()
                            + ", control=" + RetoldAiControl.getMode(farmer)
                            + ", owner=" + RetoldAiControl.getOwner(farmer)
                            + ", reason=" + RetoldAiControl.getReason(farmer)
                            + ", navigationDone="
                            + farmer.getNavigation().isDone()
                            + ", home=" + farmer.getBrain()
                            .hasMemoryValue(MemoryModuleType.HOME)
                            + ", meeting=" + farmer.getBrain()
                            .hasMemoryValue(MemoryModuleType.MEETING_POINT)
                            + ", job=" + farmer.getBrain()
                            .hasMemoryValue(MemoryModuleType.JOB_SITE)
            );
            helper.assertTrue(
                    farmer.getInventory().countItem(Items.BREAD) == 6
                            && RetoldVillagerCommunalSupply
                            .foodPointsInInventory(farmer)
                            == RetoldVillagerCommunalSupply
                            .PERSONAL_FOOD_RESERVE,
                    "Natural stocking must preserve the farmer's personal reserve"
            );
            helper.assertTrue(
                    farmer.distanceToSqr(
                            net.minecraft.world.phys.Vec3.atCenterOf(
                                    helper.absolutePos(barrelPos)
                            )
                    ) <= 3.0D * 3.0D,
                    "The farmer must physically reach the communal barrel"
            );
            farmer.discard();
        });
    }

    private static Container containerAt(
            GameTestHelper helper,
            BlockPos relativePos
    ) {
        BlockEntity blockEntity = helper.getLevel().getBlockEntity(
                helper.absolutePos(relativePos)
        );

        if (blockEntity instanceof Container container) {
            return container;
        }

        throw new IllegalStateException("Expected container at " + relativePos);
    }

    private static void driveCommunalFoodUntilFirstMeal(
            GameTestHelper helper,
            Villager villager,
            Container storage,
            int attemptsRemaining
    ) {
        if (storage.getItem(0).getCount() < 2
                || attemptsRemaining <= 0
                || !villager.isAlive()) {
            return;
        }

        RetoldVillagerCommunalFood.tick(
                helper.getLevel(),
                villager,
                helper.getLevel().getGameTime()
        );
        helper.runAfterDelay(
                5,
                () -> driveCommunalFoodUntilFirstMeal(
                        helper,
                        villager,
                        storage,
                        attemptsRemaining - 1
                )
        );
    }

    private static void driveCommunalSupplyUntilStocked(
            GameTestHelper helper,
            Villager farmer,
            Container storage,
            int attemptsRemaining
    ) {
        if (storage.countItem(Items.BREAD) >= 2
                || attemptsRemaining <= 0
                || !farmer.isAlive()) {
            return;
        }

        RetoldVillagerCommunalSupply.tick(
                helper.getLevel(),
                farmer,
                helper.getLevel().getGameTime()
        );
        helper.runAfterDelay(
                5,
                () -> driveCommunalSupplyUntilStocked(
                        helper,
                        farmer,
                        storage,
                        attemptsRemaining - 1
                )
        );
    }

    private static void placeFloor(
            GameTestHelper helper,
            int minX,
            int maxX,
            int minZ,
            int maxZ
    ) {
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
            }
        }
    }

    private static void setVillageHome(
            GameTestHelper helper,
            Villager villager,
            BlockPos relativePos
    ) {
        villager.getBrain().setMemory(
                MemoryModuleType.HOME,
                GlobalPos.of(
                        helper.getLevel().dimension(),
                        helper.absolutePos(relativePos)
                )
        );
    }

    private static void setVillageMeetingPoint(
            GameTestHelper helper,
            Villager villager,
            BlockPos relativePos
    ) {
        villager.getBrain().setMemory(
                MemoryModuleType.MEETING_POINT,
                GlobalPos.of(
                        helper.getLevel().dimension(),
                        helper.absolutePos(relativePos)
                )
        );
    }

    private static void setFarmerProfession(
            GameTestHelper helper,
            Villager villager
    ) {
        villager.setVillagerData(
                villager.getVillagerData().withProfession(
                        helper.getLevel().registryAccess(),
                        VillagerProfession.FARMER
                )
        );
    }

    private static void setVillageJobSite(
            GameTestHelper helper,
            Villager villager,
            BlockPos relativePos
    ) {
        villager.getBrain().setMemory(
                MemoryModuleType.JOB_SITE,
                GlobalPos.of(
                        helper.getLevel().dimension(),
                        helper.absolutePos(relativePos)
                )
        );
    }

    private static void registerTest(
            RegisterGameTestsEvent event,
            Holder<TestEnvironmentDefinition<?>> environment,
            String name,
            int maxTicks,
            Consumer<GameTestHelper> test
    ) {
        TestData<Holder<TestEnvironmentDefinition<?>>> testData =
                new TestData<>(
                        environment,
                        EMPTY_STRUCTURE,
                        maxTicks,
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
