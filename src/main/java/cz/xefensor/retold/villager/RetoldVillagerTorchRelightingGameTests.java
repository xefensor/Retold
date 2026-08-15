package cz.xefensor.retold.villager;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.control.RetoldAiControlMode;
import cz.xefensor.retold.behavior.control.RetoldAiControlOwner;
import cz.xefensor.retold.behavior.control.RetoldAiPriorities;
import cz.xefensor.retold.behavior.profiles.RetoldMobStates;
import cz.xefensor.retold.event.TorchWeatherEvents;
import cz.xefensor.retold.registry.RetoldBlocks;
import cz.xefensor.retold.stage.RetoldWorldData;
import cz.xefensor.retold.stage.RetoldWorldStage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.BuiltinTestFunctions;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.function.Consumer;

public final class RetoldVillagerTorchRelightingGameTests {
    private static final Identifier EMPTY_STRUCTURE =
            Identifier.withDefaultNamespace("empty");

    private RetoldVillagerTorchRelightingGameTests() {
    }

    public static void register(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment =
                event.registerEnvironment(
                        id("isolated_villager_torch_relighting"),
                        new TestEnvironmentDefinition.AllOf()
                );

        registerTest(
                event,
                environment,
                "villager_relights_extinguished_torches_in_every_stage",
                260,
                RetoldVillagerTorchRelightingGameTests::relightsInEveryStage
        );
        registerTest(
                event,
                environment,
                "villager_relighting_requires_village_and_yields_to_needs",
                220,
                RetoldVillagerTorchRelightingGameTests::requiresVillageAndYieldsToNeeds
        );
        registerTest(
                event,
                environment,
                "villager_relighting_nitwit_uses_fake_flint_and_steel_up_close",
                260,
                RetoldVillagerTorchRelightingGameTests::nitwitUsesFakeToolUpClose
        );
        registerTest(
                event,
                environment,
                "villager_relights_nearby_torches_in_one_maintenance_run",
                230,
                RetoldVillagerTorchRelightingGameTests::relightsNearbyBatch
        );
    }

    private static void relightsInEveryStage(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        RetoldWorldData worldData = RetoldWorldData.get(level);
        RetoldWorldStage originalStage = worldData.getStage();
        RetoldWorldStage[] stages = RetoldWorldStage.values();
        int[] stageIndex = {0};
        boolean[] observedContinuousFacing = {false};
        BlockPos torchPos = new BlockPos(6, 2, 6);

        placeFloor(helper, 1, 11, 1, 11);
        var observer = helper.makeMockPlayer(GameType.SURVIVAL);
        Villager villager = helper.spawn(EntityTypes.VILLAGER, 6, 2, 9);
        villager.setPersistenceRequired();
        setVillageHome(helper, villager, torchPos);
        RetoldMobStates.getOrCreate(villager, level.getGameTime()).setHunger(0);
        worldData.setStage(stages[stageIndex[0]]);
        placeExtinguishedFloorTorch(helper, torchPos);

        helper.onEachTick(() -> {
            pinIdleVillager(helper, villager, new BlockPos(6, 2, 9));
            setVillageHome(helper, villager, torchPos);
            RetoldMobStates.getOrCreate(
                    villager,
                    level.getGameTime()
            ).setHunger(0);
            boolean continuingCast = RetoldAiControl.isControlledBy(
                    villager,
                    RetoldAiControlOwner.VILLAGER_TORCH_RELIGHT
            );

            if (continuingCast) {
                float wrongYaw = villager.getYRot() + 120.0F;
                villager.setYRot(wrongYaw);
                villager.yBodyRot = wrongYaw;
                villager.setYHeadRot(wrongYaw);
            }

            RetoldVillagerTorchRelighting.tick(
                    level,
                    villager,
                    level.getGameTime()
            );

            if (continuingCast) {
                assertFacesBlock(helper, villager, torchPos);
                helper.assertTrue(
                        RetoldVillagerTorchRelighting.requiresContinuousTick(villager)
                                || helper.getBlockState(torchPos).is(Blocks.TORCH),
                        "An unfinished magical relight must request continuous visual ticks"
                );
                observedContinuousFacing[0] = true;
            }

            if (!helper.getBlockState(torchPos).is(Blocks.TORCH)) {
                return;
            }

            helper.assertTrue(
                    observedContinuousFacing[0],
                    "Each stage's relighting cast must visibly face its torch throughout the action"
            );

            stageIndex[0]++;

            if (stageIndex[0] >= stages.length) {
                villager.discard();
                observer.discard();
                worldData.setStage(originalStage);
                helper.succeed();
                return;
            }

            observedContinuousFacing[0] = false;
            worldData.setStage(stages[stageIndex[0]]);
            placeExtinguishedFloorTorch(helper, torchPos);
        });
    }

    private static void requiresVillageAndYieldsToNeeds(
            GameTestHelper helper
    ) {
        ServerLevel level = helper.getLevel();
        long startedAt = level.getGameTime();
        BlockPos wallTorchPos = new BlockPos(6, 2, 40);
        BlockPos wallSupportPos = wallTorchPos.relative(Direction.SOUTH);
        BlockPos farTorchPos = new BlockPos(6, 2, 52);

        placeFloor(helper, 1, 11, 35, 53);
        helper.setBlock(wallSupportPos, Blocks.STONE);
        BlockState extinguishedWall = RetoldBlocks.EXTINGUISHED_WALL_TORCH
                .get()
                .defaultBlockState()
                .setValue(WallTorchBlock.FACING, Direction.NORTH);
        helper.setBlock(wallTorchPos, extinguishedWall);
        TorchWeatherEvents.trackExtinguishedTorch(
                level,
                helper.absolutePos(wallTorchPos),
                extinguishedWall
        );
        placeExtinguishedFloorTorch(helper, farTorchPos);

        Villager villager = helper.spawn(EntityTypes.VILLAGER, 6, 2, 43);
        villager.setPersistenceRequired();
        var state = RetoldMobStates.getOrCreate(villager, startedAt);
        state.setHunger(0);
        boolean[] checkedMissingVillage = {false};
        boolean[] checkedHunger = {false};
        boolean[] checkedDanger = {false};

        helper.onEachTick(() -> {
            long gameTime = level.getGameTime();
            long elapsed = gameTime - startedAt;

            pinIdleVillager(helper, villager, new BlockPos(6, 2, 43));

            if (elapsed >= 35L) {
                setVillageHome(helper, villager, wallTorchPos);
            }

            if (elapsed == 35L) {
                assertStillExtinguished(
                        helper,
                        wallTorchPos,
                        "A Villager without village context must not maintain torches"
                );
                checkedMissingVillage[0] = true;
                setVillageHome(helper, villager, wallTorchPos);
                state.setHunger(100);
            } else if (elapsed == 75L) {
                assertStillExtinguished(
                        helper,
                        wallTorchPos,
                        "Hunger must take priority over torch maintenance"
                );
                checkedHunger[0] = true;
                state.setHunger(0);
                RetoldAiControl.tryClaim(
                        villager,
                        RetoldAiControlMode.FLEE,
                        RetoldAiControlOwner.FLEEING,
                        RetoldAiPriorities.FLEE,
                        "torch_relighting_test_danger",
                        gameTime,
                        45
                );
            } else if (elapsed == 115L) {
                assertStillExtinguished(
                        helper,
                        wallTorchPos,
                        "Danger control must interrupt torch maintenance"
                );
                checkedDanger[0] = true;
                RetoldAiControl.clearIfOwnedBy(
                        villager,
                        RetoldAiControlOwner.FLEEING
                );
            }

            RetoldVillagerTorchRelighting.tick(level, villager, gameTime);
        });

        helper.succeedWhen(() -> {
            BlockState relit = helper.getBlockState(wallTorchPos);
            helper.assertTrue(
                    checkedMissingVillage[0]
                            && checkedHunger[0]
                            && checkedDanger[0],
                    "The test must observe village, hunger, and danger gates"
            );
            helper.assertTrue(
                    relit.is(Blocks.WALL_TORCH)
                            && relit.getValue(WallTorchBlock.FACING)
                            == Direction.NORTH,
                    "The Villager must restore the matching wall torch and facing"
            );
            helper.assertTrue(
                    TorchWeatherEvents.isExtinguishedTorch(
                            helper.getBlockState(farTorchPos)
                    ),
                    "A torch beyond the eight-block relighting radius must remain extinguished"
            );
            villager.discard();
        });
    }

    private static void nitwitUsesFakeToolUpClose(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.setTime(6_000L);
        BlockPos torchPos = new BlockPos(6, 2, 6);
        BlockPos startPos = new BlockPos(6, 2, 7);

        placeFloor(helper, 1, 11, 1, 11);
        placeExtinguishedFloorTorch(helper, torchPos);
        Villager nitwit = helper.spawn(
                EntityTypes.VILLAGER,
                startPos.getX(),
                startPos.getY(),
                startPos.getZ()
        );
        nitwit.setPersistenceRequired();
        setProfession(helper, nitwit, VillagerProfession.NITWIT);
        setVillageHome(helper, nitwit, torchPos);
        var state = RetoldMobStates.getOrCreate(nitwit, level.getGameTime());
        state.setHunger(0);
        BlockPos absoluteTorch = helper.absolutePos(torchPos);
        boolean[] started = {false};
        boolean[] sawFakeTool = {false};
        boolean[] wasCloseWhileUsingTool = {false};
        boolean[] clearedVisualOnce = {false};
        boolean[] recoveredClearedVisual = {false};
        int[] visibleToolTicks = {0};

        helper.runAfterDelay(5, () -> {
            BlockPos indexedTorch = TorchWeatherEvents.findNearestExtinguishedTorch(
                    level,
                    nitwit.blockPosition(),
                    RetoldVillagerTorchRelighting.HORIZONTAL_RADIUS,
                    RetoldVillagerTorchRelighting.VERTICAL_RADIUS,
                    ignored -> true
            );
            helper.assertValueEqual(
                    indexedTorch,
                    absoluteTorch,
                    "The Nitwit fixture's extinguished torch must be indexed in range"
            );
            BlockPos initialAccess = RetoldVillagerTorchRelighting
                    .findPhysicalAccess(level, nitwit, absoluteTorch);
            helper.assertTrue(
                    initialAccess != null,
                    "The Nitwit route fixture must provide an adjacent stable cell"
                            + "; torch=" + absoluteTorch
                            + ", nitwit=" + nitwit.blockPosition()
            );
            nitwit.getBrain().setActiveActivityIfPossible(Activity.IDLE);
            RetoldVillagerTorchRelighting.tick(
                    level,
                    nitwit,
                    level.getGameTime()
            );
            helper.assertTrue(
                    RetoldAiControl.isControlledBy(
                            nitwit,
                            RetoldAiControlOwner.VILLAGER_TORCH_RELIGHT
                    ),
                    "The close Nitwit fixture must begin an owned physical interaction"
                            + "; torch=" + absoluteTorch
                            + ", nitwit=" + nitwit.blockPosition()
                            + ", indexed=" + indexedTorch
                            + ", access=" + initialAccess
                            + ", activity=" + nitwit.getBrain()
                            .getActiveNonCoreActivity()
            );
            helper.assertTrue(
                    RetoldVillagerTorchRelighting.requiresContinuousTick(
                            nitwit
                    ),
                    "Active Nitwit tool use must request continuous visual ticks"
            );
            started[0] = true;
        });

        helper.onEachTick(() -> {
            if (!started[0]) {
                return;
            }
            state.setHunger(0);
            setVillageHome(helper, nitwit, torchPos);
            nitwit.getBrain().setActiveActivityIfPossible(Activity.IDLE);

            if (sawFakeTool[0] && !clearedVisualOnce[0]) {
                nitwit.setItemInHand(
                        InteractionHand.MAIN_HAND,
                        ItemStack.EMPTY
                );
                clearedVisualOnce[0] = true;
            }

            RetoldVillagerTorchRelighting.tick(
                    level,
                    nitwit,
                    level.getGameTime()
            );
            boolean toolVisible = nitwit.getMainHandItem().is(
                    Items.FLINT_AND_STEEL
            );
            sawFakeTool[0] |= toolVisible;
            visibleToolTicks[0] += toolVisible ? 1 : 0;
            recoveredClearedVisual[0] |= clearedVisualOnce[0] && toolVisible;
            wasCloseWhileUsingTool[0] |= toolVisible && nitwit.distanceToSqr(
                    Vec3.atCenterOf(absoluteTorch)
            ) <= 2.5D * 2.5D;
        });

        helper.succeedWhen(() -> {
            helper.assertTrue(
                    helper.getBlockState(torchPos).is(Blocks.TORCH),
                    "The Nitwit must relight the torch through a close physical interaction"
                            + "; block=" + helper.getBlockState(torchPos)
                            + ", pos=" + nitwit.blockPosition()
                            + ", profession=" + nitwit.getVillagerData().profession()
                            + ", activity=" + nitwit.getBrain()
                            .getActiveNonCoreActivity()
                            + ", control=" + RetoldAiControl.getMode(nitwit)
                            + ", owner=" + RetoldAiControl.getOwner(nitwit)
                            + ", reason=" + RetoldAiControl.getReason(nitwit)
                            + ", navigationDone="
                            + nitwit.getNavigation().isDone()
                            + ", hand=" + nitwit.getMainHandItem()
                            + ", sawTool=" + sawFakeTool[0]
                            + ", visibleToolTicks=" + visibleToolTicks[0]
            );
            helper.assertTrue(
                    sawFakeTool[0],
                    "The Nitwit must visibly hold Flint and Steel during the close interaction"
            );
            helper.assertTrue(
                    wasCloseWhileUsingTool[0],
                    "The Nitwit must be close while visibly using Flint and Steel"
            );
            helper.assertTrue(
                    clearedVisualOnce[0] && recoveredClearedVisual[0],
                    "Active Nitwit tool use must restore a visual cleared between ticks"
            );
            helper.assertTrue(
                    visibleToolTicks[0] >= 10,
                    "Flint and Steel must remain visible instead of appearing for one tick"
            );
            helper.assertValueEqual(
                    nitwit.getInventory().countItem(Items.FLINT_AND_STEEL),
                    0,
                    "The visual Flint and Steel must not enter the Nitwit's inventory"
            );
            helper.assertTrue(
                    nitwit.getMainHandItem().isEmpty(),
                    "The fake Flint and Steel must disappear after the interaction"
            );
            nitwit.discard();
        });
    }

    private static void relightsNearbyBatch(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        long startedAt = level.getGameTime();
        BlockPos villagerPos = new BlockPos(6, 2, 11);
        BlockPos villageAnchor = new BlockPos(6, 2, 7);
        BlockPos[] torches = {
                new BlockPos(4, 2, 5),
                new BlockPos(6, 2, 5),
                new BlockPos(8, 2, 5),
                new BlockPos(3, 2, 7),
                new BlockPos(6, 2, 7),
                new BlockPos(9, 2, 7),
                new BlockPos(4, 2, 9),
                new BlockPos(6, 2, 9),
                new BlockPos(8, 2, 9)
        };

        placeFloor(helper, 1, 11, 1, 12);

        for (BlockPos torch : torches) {
            placeExtinguishedFloorTorch(helper, torch);
        }

        var observer = helper.makeMockPlayer(GameType.SURVIVAL);
        Villager villager = helper.spawn(
                EntityTypes.VILLAGER,
                villagerPos.getX(),
                villagerPos.getY(),
                villagerPos.getZ()
        );
        villager.setPersistenceRequired();
        setVillageHome(helper, villager, villageAnchor);
        var state = RetoldMobStates.getOrCreate(villager, startedAt);
        state.setHunger(0);

        helper.onEachTick(() -> {
            pinIdleVillager(helper, villager, villagerPos);
            setVillageHome(helper, villager, villageAnchor);
            state.setHunger(0);
            RetoldVillagerTorchRelighting.tick(
                    level,
                    villager,
                    level.getGameTime()
            );

            int relit = 0;

            for (BlockPos torch : torches) {
                relit += helper.getBlockState(torch).is(Blocks.TORCH) ? 1 : 0;
            }

            if (relit < RetoldVillagerTorchRelighting.MAX_RELIGHTS_PER_RUN) {
                return;
            }

            helper.assertValueEqual(
                    relit,
                    RetoldVillagerTorchRelighting.MAX_RELIGHTS_PER_RUN,
                    "One maintenance run must stop at its bounded nearby-torch limit"
            );
            helper.assertTrue(
                    level.getGameTime() - startedAt < 190L,
                    "Nearby torches must be relit consecutively without a success cooldown"
            );
            helper.assertTrue(
                    !RetoldAiControl.isControlledBy(
                            villager,
                            RetoldAiControlOwner.VILLAGER_TORCH_RELIGHT
                    ),
                    "The Villager must release action ownership after the bounded run"
            );
            villager.discard();
            observer.discard();
            helper.succeed();
        });
    }

    private static void placeExtinguishedFloorTorch(
            GameTestHelper helper,
            BlockPos pos
    ) {
        BlockState state = RetoldBlocks.EXTINGUISHED_TORCH
                .get()
                .defaultBlockState();
        helper.setBlock(pos, state);
        TorchWeatherEvents.trackExtinguishedTorch(
                helper.getLevel(),
                helper.absolutePos(pos),
                state
        );
    }

    private static void assertStillExtinguished(
            GameTestHelper helper,
            BlockPos pos,
            String message
    ) {
        helper.assertTrue(
                TorchWeatherEvents.isExtinguishedTorch(helper.getBlockState(pos)),
                message
        );
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

    private static void setProfession(
            GameTestHelper helper,
            Villager villager,
            ResourceKey<VillagerProfession> profession
    ) {
        villager.setVillagerData(
                villager.getVillagerData().withProfession(
                        helper.getLevel().registryAccess(),
                        profession
                )
        );
    }

    private static void pinIdleVillager(
            GameTestHelper helper,
            Villager villager,
            BlockPos relativePos
    ) {
        BlockPos absolutePos = helper.absolutePos(relativePos);
        villager.snapTo(
                absolutePos.getX() + 0.5D,
                absolutePos.getY(),
                absolutePos.getZ() + 0.5D,
                villager.getYRot(),
                villager.getXRot()
        );
        villager.getNavigation().stop();
        villager.getBrain().setActiveActivityIfPossible(Activity.IDLE);
    }

    private static void assertFacesBlock(
            GameTestHelper helper,
            Villager villager,
            BlockPos relativeTarget
    ) {
        Vec3 target = Vec3.atCenterOf(helper.absolutePos(relativeTarget));
        double dx = target.x() - villager.getX();
        double dz = target.z() - villager.getZ();
        float expectedYaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;

        helper.assertTrue(
                Math.abs(Mth.wrapDegrees(villager.getYRot() - expectedYaw)) < 0.1F
                        && Math.abs(Mth.wrapDegrees(villager.yBodyRot - expectedYaw)) < 0.1F
                        && Math.abs(Mth.wrapDegrees(villager.getYHeadRot() - expectedYaw)) < 0.1F
                        && Math.abs(villager.getLookControl().getWantedX() - target.x()) < 0.001D
                        && Math.abs(villager.getLookControl().getWantedY() - target.y()) < 0.001D
                        && Math.abs(villager.getLookControl().getWantedZ() - target.z()) < 0.001D,
                "A relighting Villager must turn its body, head, and look control toward the torch"
                        + "; yaw=" + villager.getYRot()
                        + ", bodyYaw=" + villager.yBodyRot
                        + ", headYaw=" + villager.getYHeadRot()
                        + ", expectedYaw=" + expectedYaw
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
