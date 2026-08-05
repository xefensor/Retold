package cz.xefensor.retold.behavior.food;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.control.RetoldAiControlMode;
import cz.xefensor.retold.behavior.control.RetoldAiControlOwner;
import cz.xefensor.retold.behavior.control.RetoldAiPriorities;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;
import cz.xefensor.retold.behavior.profiles.RetoldMobState;
import cz.xefensor.retold.behavior.profiles.RetoldMobStates;
import cz.xefensor.retold.block.AnimalFeederBlockEntity;
import cz.xefensor.retold.combat.RetoldFactionTargetGuards;
import cz.xefensor.retold.registry.RetoldBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.BuiltinTestFunctions;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.EatBlockGoal;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.function.Consumer;

public final class RetoldAnimalFeederGameTests {
    private static final Identifier EMPTY_STRUCTURE =
            Identifier.withDefaultNamespace("empty");

    private RetoldAnimalFeederGameTests() {
    }

    public static void register(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment =
                event.registerEnvironment(
                        id("isolated_animal_feeder"),
                        new TestEnvironmentDefinition.AllOf()
                );

        registerTest(
                event,
                environment,
                "animal_feeder_stores_retrieves_and_persists_one_stack",
                80,
                RetoldAnimalFeederGameTests::animalFeederStoresRetrievesAndPersistsOneStack
        );
        registerTest(
                event,
                environment,
                "animal_feeder_serves_only_compatible_land_animals",
                80,
                RetoldAnimalFeederGameTests::animalFeederServesOnlyCompatibleLandAnimals
        );
        registerTest(
                event,
                environment,
                "animal_feeder_uses_paths_without_mob_griefing",
                240,
                RetoldAnimalFeederGameTests::animalFeederUsesPathsWithoutMobGriefing
        );
        registerTest(
                event,
                environment,
                "animal_feeder_does_not_override_live_combat",
                80,
                RetoldAnimalFeederGameTests::animalFeederDoesNotOverrideLiveCombat
        );
        registerTest(
                event,
                environment,
                "feeding_pose_stops_and_faces_food_sources",
                120,
                RetoldAnimalFeederGameTests::feedingPoseStopsAndFacesFoodSources
        );
        registerTest(
                event,
                environment,
                "fleeing_sheep_cannot_eat_until_danger_ends",
                80,
                RetoldAnimalFeederGameTests::fleeingSheepCannotEatUntilDangerEnds
        );
    }

    private static void fleeingSheepCannotEatUntilDangerEnds(
            GameTestHelper helper
    ) {
        ServerLevel level = helper.getLevel();
        Sheep sheep = helper.spawn(EntityTypes.SHEEP, 3, 3, 3);
        long gameTime = level.getGameTime();
        RetoldMobState state = RetoldMobStates.getOrCreate(sheep, gameTime);
        BlockPos grassPos = sheep.blockPosition();
        ItemEntity wheat = new ItemEntity(
                level,
                sheep.getX(),
                sheep.getY(),
                sheep.getZ(),
                new ItemStack(Items.WHEAT, 2)
        );

        state.setHunger(100);
        level.setBlockAndUpdate(grassPos, Blocks.SHORT_GRASS.defaultBlockState());
        level.addFreshEntity(wheat);

        EatBlockGoal vanillaGrazing = sheep.goalSelector.getAvailableGoals()
                .stream()
                .map(goal -> goal.getGoal())
                .filter(EatBlockGoal.class::isInstance)
                .map(EatBlockGoal.class::cast)
                .findFirst()
                .orElseThrow();

        vanillaGrazing.start();
        helper.assertTrue(
                RetoldAiControl.tryClaim(
                        sheep,
                        RetoldAiControlMode.FLEE,
                        RetoldAiControlOwner.FLEEING,
                        RetoldAiPriorities.FLEE,
                        "test_predator_flight",
                        gameTime,
                        60
                ),
                "The fixture must give urgent flight control to the Sheep"
        );

        helper.assertFalse(
                vanillaGrazing.canContinueToUse(),
                "Urgent flight must immediately interrupt an active vanilla grazing goal"
        );

        for (int tick = 0; tick < 45; tick++) {
            vanillaGrazing.tick();
        }

        helper.assertTrue(
                level.getBlockState(grassPos).is(Blocks.SHORT_GRASS),
                "An interrupted grazing goal must not consume its block later"
        );
        helper.assertFalse(
                RetoldFoodBehaviorEvents.tryConsumeForageBlock(
                        level,
                        sheep,
                        grassPos,
                        gameTime
                ),
                "Retold forage must not complete while the Sheep is fleeing"
        );
        helper.assertFalse(
                RetoldFoodBehaviorEvents.tryConsumeDroppedFood(
                        sheep,
                        wheat,
                        gameTime
                ),
                "Dropped food must not be consumed while the Sheep is fleeing"
        );
        helper.assertTrue(
                state.hunger() == 100
                        && wheat.getItem().getCount() == 2,
                "Flight must preserve hunger and every food source"
        );

        RetoldAiControl.clearIfControlledAs(sheep, RetoldAiControlMode.FLEE);

        helper.assertTrue(
                RetoldFoodBehaviorEvents.tryConsumeDroppedFood(
                        sheep,
                        wheat,
                        gameTime
                ),
                "The same Sheep must be able to resume eating after danger ends"
        );
        helper.assertTrue(
                state.hunger() < 100
                        && wheat.getItem().getCount() == 1,
                "Resumed feeding must consume exactly one item and relieve hunger"
        );

        RetoldFeedingPose.finish(sheep);
        RetoldMobStates.remove(sheep);
        sheep.discard();
        wheat.discard();
        helper.succeed();
    }

    private static void animalFeederStoresRetrievesAndPersistsOneStack(
            GameTestHelper helper
    ) {
        BlockPos feederPos = new BlockPos(2, 2, 2);
        helper.setBlock(feederPos, RetoldBlocks.ANIMAL_FEEDER.get());
        AnimalFeederBlockEntity feeder = feederAt(helper, feederPos);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        player.setItemInHand(
                InteractionHand.MAIN_HAND,
                new ItemStack(Items.WHEAT, 12)
        );
        helper.useBlock(feederPos, player);

        helper.assertTrue(
                player.getItemInHand(InteractionHand.MAIN_HAND).is(Items.WHEAT)
                        && player.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 11
                        && feeder.getItem(0).is(Items.WHEAT)
                        && feeder.getItem(0).getCount() == 1,
                "Right-clicking with compatible food must transfer exactly one item"
        );

        player.setShiftKeyDown(true);
        helper.useBlock(feederPos, player);

        helper.assertTrue(
                player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()
                        && feeder.getItem(0).is(Items.WHEAT)
                        && feeder.getItem(0).getCount() == 12,
                "Sneak-right-clicking with compatible food must transfer the remaining stack"
        );

        helper.useBlock(feederPos, player);
        player.setShiftKeyDown(false);

        helper.assertTrue(
                feeder.isEmpty()
                        && player.getInventory().countItem(Items.WHEAT) == 12,
                "Sneak-right-clicking must return the stored stack to the player"
        );

        player.getInventory().clearContent();
        verifyPlayerTransferCounts(
                helper,
                feederPos,
                feeder,
                player,
                1,
                false,
                true,
                "Survival single-item"
        );

        Player creativePlayer = helper.makeMockPlayer(GameType.CREATIVE);
        creativePlayer.getAbilities().instabuild = true;
        verifyPlayerTransferCounts(
                helper,
                feederPos,
                feeder,
                creativePlayer,
                25,
                false,
                false,
                "Creative single-item"
        );
        verifyPlayerTransferCounts(
                helper,
                feederPos,
                feeder,
                creativePlayer,
                25,
                true,
                false,
                "Creative full-stack"
        );
        player.discard();
        creativePlayer.discard();

        ItemStack carrots = new ItemStack(Items.CARROT, 7);
        helper.assertTrue(
                feeder.insert(carrots) == 7 && carrots.isEmpty(),
                "The feeder must accept a complete compatible stack"
        );
        helper.assertTrue(
                feeder.insert(new ItemStack(Items.STONE, 3)) == 0,
                "The feeder must reject items outside every supported animal diet"
        );

        CompoundTag saved = feeder.saveWithFullMetadata(
                helper.getLevel().registryAccess()
        );
        BlockEntity loaded = BlockEntity.loadStatic(
                helper.absolutePos(feederPos),
                helper.getBlockState(feederPos),
                saved,
                helper.getLevel().registryAccess()
        );

        helper.assertTrue(
                loaded instanceof AnimalFeederBlockEntity restored
                        && restored.getItem(0).is(Items.CARROT)
                        && restored.getItem(0).getCount() == 7,
                "The feeder's exact stored stack must survive block-entity serialization"
        );
        helper.succeed();
    }

    private static void verifyPlayerTransferCounts(
            GameTestHelper helper,
            BlockPos feederPos,
            AnimalFeederBlockEntity feeder,
            Player player,
            int count,
            boolean insertWholeStack,
            boolean consumesHeldStack,
            String scenario
    ) {
        player.getInventory().clearContent();
        player.setItemInHand(
                InteractionHand.MAIN_HAND,
                new ItemStack(Items.WHEAT, count)
        );
        player.setShiftKeyDown(insertWholeStack);
        helper.useBlock(feederPos, player);
        player.setShiftKeyDown(false);

        int expectedInserted = insertWholeStack ? count : 1;
        int expectedHeldCount = consumesHeldStack
                ? count - expectedInserted
                : count;

        helper.assertTrue(
                player.getInventory().countItem(Items.WHEAT) == expectedHeldCount
                        && feeder.getItem(0).is(Items.WHEAT)
                        && feeder.getItem(0).getCount() == expectedInserted,
                scenario + " insertion must store exactly " + expectedInserted
                        + " Wheat and leave " + expectedHeldCount + " held"
        );

        ItemStack remaining = player.getItemInHand(InteractionHand.MAIN_HAND);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);

        if (!remaining.isEmpty()) {
            player.getInventory().setItem(1, remaining);
        }

        player.setShiftKeyDown(true);
        helper.useBlock(feederPos, player);
        player.setShiftKeyDown(false);

        helper.assertTrue(
                feeder.isEmpty()
                        && player.getInventory().countItem(Items.WHEAT)
                        == expectedHeldCount + expectedInserted,
                scenario + " retrieval must return exactly the stored stack"
        );
    }

    private static void animalFeederServesOnlyCompatibleLandAnimals(
            GameTestHelper helper
    ) {
        Cow cow = helper.spawn(EntityTypes.COW, 1, 3, 1);
        Wolf wolf = helper.spawn(EntityTypes.WOLF, 2, 3, 1);
        PathfinderMob axolotl = helper.spawn(EntityTypes.AXOLOTL, 3, 3, 1);
        PathfinderMob zombie = helper.spawn(EntityTypes.ZOMBIE, 4, 3, 1);
        PathfinderMob slime = helper.spawn(EntityTypes.SLIME, 5, 3, 1);

        helper.assertTrue(
                RetoldMobRules.canUseAnimalFeeder(cow),
                "Managed passive land animals must use animal feeders"
        );
        helper.assertTrue(
                RetoldMobRules.canUseAnimalFeeder(wolf),
                "Managed non-monster land predators must use compatible animal feeders"
        );
        helper.assertFalse(
                RetoldMobRules.canUseAnimalFeeder(axolotl),
                "Aquatic animals must remain outside the land feeder slice"
        );
        helper.assertFalse(
                RetoldMobRules.canUseAnimalFeeder(zombie),
                "Hostile monsters must not use animal feeders"
        );
        helper.assertFalse(
                RetoldMobRules.canUseAnimalFeeder(slime),
                "Slimes and Magma Cubes must remain outside animal feeders"
        );
        helper.assertTrue(
                RetoldMobRules.canEatDroppedItem(cow, new ItemStack(Items.WHEAT))
                        && !RetoldMobRules.canEatDroppedItem(cow, new ItemStack(Items.BEEF))
                        && RetoldMobRules.canEatDroppedItem(wolf, new ItemStack(Items.BEEF))
                        && !RetoldMobRules.canEatDroppedItem(wolf, new ItemStack(Items.WHEAT)),
                "Every feeder user must retain its existing species diet"
        );

        cleanup(cow, wolf, axolotl, zombie, slime);
        helper.succeed();
    }

    private static void animalFeederUsesPathsWithoutMobGriefing(
            GameTestHelper helper
    ) {
        buildFloor(helper);

        ServerLevel level = helper.getLevel();
        BlockPos feederPos = new BlockPos(10, 2, 8);
        Sheep sheep = helper.spawn(EntityTypes.SHEEP, 3, 2, 8);
        RetoldMobState state = RetoldMobStates.getOrCreate(sheep, level.getGameTime());

        helper.setBlock(feederPos, RetoldBlocks.ANIMAL_FEEDER.get());
        AnimalFeederBlockEntity feeder = feederAt(helper, feederPos);
        feeder.insert(new ItemStack(Items.WHEAT, 3));
        state.setHunger(40);
        RetoldAiControl.clear(sheep);
        sheep.setOnGround(true);

        helper.startSequence()
                .thenWaitUntil(() -> assertFeederPathStarted(
                        helper,
                        level,
                        sheep,
                        state
                ))
                .thenWaitUntil(() -> assertSheepArrivedAndAte(
                        helper,
                        level,
                        sheep,
                        state,
                        feeder
                ))
                .thenExecute(() -> {
                    helper.assertTrue(
                            state.hunger() == 12,
                            "Natural feeder arrival must apply the Sheep's existing wheat hunger relief"
                    );
                    helper.assertTrue(
                            RetoldAiControl.isControlledAsByWithReason(
                                    sheep,
                                    RetoldAiControlMode.FEED,
                                    RetoldAiControlOwner.FOOD,
                                    "feeding_pose"
                            ),
                            "Feeder consumption must retain stationary FOOD control for its feeding pose"
                    );
                    assertFeedingPose(
                            helper,
                            sheep,
                            Vec3.atCenterOf(helper.absolutePos(feederPos))
                    );
                    cleanup(sheep);
                })
                .thenSucceed();
    }

    private static void feedingPoseStopsAndFacesFoodSources(
            GameTestHelper helper
    ) {
        buildFloor(helper);

        ServerLevel level = helper.getLevel();
        long gameTime = level.getGameTime();
        Cow cow = helper.spawn(EntityTypes.COW, 4, 2, 4);
        RetoldMobState state = RetoldMobStates.getOrCreate(cow, gameTime);
        Vec3 droppedSource = helper.absoluteVec(new Vec3(6.0D, 2.0D, 4.5D));
        ItemEntity wheat = new ItemEntity(
                level,
                droppedSource.x(),
                droppedSource.y(),
                droppedSource.z(),
                new ItemStack(Items.WHEAT, 2)
        );

        state.setHunger(100);
        level.addFreshEntity(wheat);
        cow.setDeltaMovement(0.4D, 0.1D, 0.2D);

        helper.assertTrue(
                RetoldFoodBehaviorEvents.tryConsumeDroppedFood(cow, wheat, gameTime),
                "The dropped-food setup must feed the Cow"
        );
        assertFeedingPose(helper, cow, droppedSource);

        RetoldFeedingPose.finish(cow);
        state.setHunger(100);
        BlockPos foragePos = new BlockPos(5, 2, 4);
        helper.setBlock(foragePos, Blocks.GRASS_BLOCK);
        cow.setDeltaMovement(-0.3D, 0.0D, 0.25D);

        helper.assertTrue(
                RetoldFoodBehaviorEvents.tryConsumeForageBlock(
                        level,
                        cow,
                        helper.absolutePos(foragePos),
                        gameTime
                ),
                "The forage setup must feed the Cow"
        );
        assertFeedingPose(
                helper,
                cow,
                Vec3.atCenterOf(helper.absolutePos(foragePos))
        );

        RetoldFeedingPose.finish(cow);
        Bat bat = helper.spawn(EntityTypes.BAT, 4, 5, 4);
        Vec3 batSource = helper.absoluteVec(new Vec3(2.5D, 5.5D, 4.5D));
        bat.setDeltaMovement(0.2D, -0.1D, 0.3D);

        helper.assertTrue(
                RetoldFeedingPose.begin(bat, batSource, gameTime),
                "The shared feeding pose must support non-pathfinding food users"
        );
        assertFeedingPose(helper, bat, batSource);
        helper.assertTrue(
                RetoldAiControl.tryClaim(
                        bat,
                        RetoldAiControlMode.FLEE,
                        RetoldAiControlOwner.SYSTEM,
                        1_000,
                        "feeding_pose_interrupt",
                        gameTime + 1L,
                        80
                ),
                "Urgent danger control must interrupt an active feeding pose"
        );
        helper.assertFalse(
                RetoldFeedingPose.tick(bat, gameTime + 1L),
                "The interrupted feeding pose must release without another stop application"
        );
        helper.assertTrue(
                RetoldAiControl.isControlledAsBy(
                        bat,
                        RetoldAiControlMode.FLEE,
                        RetoldAiControlOwner.SYSTEM
                ),
                "Releasing the feeding pose must preserve the urgent replacement owner"
        );

        cleanup(cow);
        RetoldFeedingPose.finish(bat);
        RetoldAiControl.clear(bat);
        bat.discard();
        wheat.discard();
        helper.succeed();
    }

    private static void animalFeederDoesNotOverrideLiveCombat(
            GameTestHelper helper
    ) {
        buildFloor(helper);

        ServerLevel level = helper.getLevel();
        BlockPos feederPos = new BlockPos(3, 2, 2);
        Wolf wolf = helper.spawn(EntityTypes.WOLF, 2, 2, 2);
        Cow target = helper.spawn(EntityTypes.COW, 5, 2, 2);
        RetoldMobState state = RetoldMobStates.getOrCreate(wolf, level.getGameTime());

        helper.setBlock(feederPos, RetoldBlocks.ANIMAL_FEEDER.get());
        AnimalFeederBlockEntity feeder = feederAt(helper, feederPos);
        feeder.insert(new ItemStack(Items.BEEF, 3));
        state.setHunger(100);
        RetoldFactionTargetGuards.setTargetIgnoringGuard(wolf, target);

        helper.assertFalse(
                RetoldAnimalFeederBehavior.tryUse(
                        level,
                        wolf,
                        state,
                        level.getGameTime()
                ),
                "A live combat target must prevent feeder ownership"
        );
        helper.assertTrue(
                wolf.getTarget() == target
                        && feeder.getItem(0).getCount() == 3
                        && !RetoldAiControl.isControlledAsBy(
                        wolf,
                        RetoldAiControlMode.FEED,
                        RetoldAiControlOwner.FOOD
                ),
                "The feeder must preserve combat and consume no food"
        );

        cleanup(wolf, target);
        helper.succeed();
    }

    private static void assertFeederPathStarted(
            GameTestHelper helper,
            ServerLevel level,
            Sheep sheep,
            RetoldMobState state
    ) {
        boolean previousMobGriefing = level.getGameRules().get(GameRules.MOB_GRIEFING);

        try {
            level.getGameRules().set(
                    GameRules.MOB_GRIEFING,
                    false,
                    level.getServer()
            );

            BlockPos discovered = RetoldAnimalFeederSearch.find(
                    level,
                    sheep,
                    8,
                    2,
                    level.getGameTime(),
                    60
            );
            BlockPos accessPos = discovered == null
                    ? null
                    : RetoldAnimalFeederBehavior.findAccessPos(level, sheep, discovered);
            boolean started = RetoldAnimalFeederBehavior.tryUse(
                    level,
                    sheep,
                    state,
                    level.getGameTime()
            );
            Path path = sheep.getNavigation().getPath();

            helper.assertTrue(
                    started
                            && RetoldAiControl.isControlledAsBy(
                            sheep,
                            RetoldAiControlMode.FEED,
                            RetoldAiControlOwner.FOOD
                    )
                            && path != null
                            && path.canReach()
                            && path.getEndNode() != null
                            && path.getEndNode().asBlockPos().equals(accessPos),
                    "A distant feeder must acquire FOOD control and an exact path to its adjacent access cell; mode="
                            + RetoldAiControl.getMode(sheep)
                            + ", owner="
                            + RetoldAiControl.getOwner(sheep)
                            + ", eligible="
                            + RetoldMobRules.canUseAnimalFeeder(sheep)
                            + ", eatDrive="
                            + RetoldMobRules.hasEatDrive(sheep, state)
                            + ", noAi="
                            + sheep.isNoAi()
                            + ", onGround="
                            + sheep.onGround()
                            + ", sheepPos="
                            + sheep.blockPosition()
                            + ", discovered="
                            + discovered
                            + ", access="
                            + accessPos
                            + ", path="
                            + path
            );
        } finally {
            level.getGameRules().set(
                    GameRules.MOB_GRIEFING,
                    previousMobGriefing,
                    level.getServer()
            );
        }
    }

    private static void assertSheepArrivedAndAte(
            GameTestHelper helper,
            ServerLevel level,
            Sheep sheep,
            RetoldMobState state,
            AnimalFeederBlockEntity feeder
    ) {
        RetoldAnimalFeederBehavior.tryUse(
                level,
                sheep,
                state,
                level.getGameTime()
        );
        helper.assertTrue(
                feeder.getItem(0).getCount() == 2,
                "A Sheep must naturally reach the adjacent feeder cell and consume exactly one wheat; position="
                        + sheep.position()
                        + ", navigationDone="
                        + sheep.getNavigation().isDone()
        );
    }

    private static void assertFeedingPose(
            GameTestHelper helper,
            Mob mob,
            Vec3 foodSource
    ) {
        Vec3 rememberedSource = RetoldFeedingPose.foodSource(mob);
        double dx = foodSource.x() - mob.getX();
        double dz = foodSource.z() - mob.getZ();
        float expectedYaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;

        helper.assertTrue(
                rememberedSource != null
                        && rememberedSource.distanceToSqr(foodSource) < 0.0001D
                        && RetoldAiControl.isControlledAsByWithReason(
                        mob,
                        RetoldAiControlMode.FEED,
                        RetoldAiControlOwner.FOOD,
                        "feeding_pose"
                )
                        && mob.getNavigation().isDone()
                        && mob.getDeltaMovement().lengthSqr() < 0.0001D
                        && Math.abs(Mth.wrapDegrees(mob.getYRot() - expectedYaw)) < 0.1F
                        && Math.abs(mob.getLookControl().getWantedX() - foodSource.x()) < 0.001D
                        && Math.abs(mob.getLookControl().getWantedY() - foodSource.y()) < 0.001D
                        && Math.abs(mob.getLookControl().getWantedZ() - foodSource.z()) < 0.001D,
                "Feeding must stop the mob and turn its body and look control toward the food source; mob="
                        + mob.getType()
                        + ", velocity="
                        + mob.getDeltaMovement()
                        + ", yaw="
                        + mob.getYRot()
                        + ", expectedYaw="
                        + expectedYaw
                        + ", rememberedSource="
                        + rememberedSource
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

    private static void buildFloor(GameTestHelper helper) {
        for (int x = 0; x <= 15; x++) {
            for (int z = 0; z <= 15; z++) {
                helper.setBlock(x, 1, z, Blocks.STONE);
            }
        }
    }

    private static void cleanup(PathfinderMob... mobs) {
        for (PathfinderMob mob : mobs) {
            RetoldFeedingPose.finish(mob);
            RetoldAiControl.clear(mob);
            RetoldMobStates.remove(mob);
            mob.discard();
        }
    }

    private static void registerTest(
            RegisterGameTestsEvent event,
            Holder<TestEnvironmentDefinition<?>> environment,
            String name,
            int timeoutTicks,
            Consumer<GameTestHelper> test
    ) {
        TestData<Holder<TestEnvironmentDefinition<?>>> testData =
                new TestData<>(
                        environment,
                        EMPTY_STRUCTURE,
                        timeoutTicks,
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
