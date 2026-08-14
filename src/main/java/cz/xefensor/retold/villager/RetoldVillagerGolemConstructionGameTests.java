package cz.xefensor.retold.villager;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;
import cz.xefensor.retold.behavior.profiles.RetoldMobState;
import cz.xefensor.retold.behavior.profiles.RetoldMobStates;
import cz.xefensor.retold.event.RetoldGolemEvents;
import cz.xefensor.retold.golem.RetoldGolemAnimation;
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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.entity.player.TradeWithVillagerEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class RetoldVillagerGolemConstructionGameTests {
    private static final Identifier EMPTY_STRUCTURE =
            Identifier.withDefaultNamespace("empty");

    private RetoldVillagerGolemConstructionGameTests() {
    }

    public static void register(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> eligibilityEnvironment =
                event.registerEnvironment(
                        id("isolated_golem_eligibility"),
                        new TestEnvironmentDefinition.AllOf()
                );
        Holder<TestEnvironmentDefinition<?>> constructionEnvironment =
                event.registerEnvironment(
                        id("isolated_golem_staged_construction"),
                        new TestEnvironmentDefinition.AllOf()
                );
        Holder<TestEnvironmentDefinition<?>> utilityEnvironment =
                event.registerEnvironment(
                        id("isolated_golem_costs"),
                        new TestEnvironmentDefinition.AllOf()
                );

        registerTest(
                event,
                eligibilityEnvironment,
                "golem_construction_keeps_vanilla_villager_eligibility",
                80,
                RetoldVillagerGolemConstructionGameTests::keepsVanillaVillagerEligibility
        );
        registerTest(
                event,
                eligibilityEnvironment,
                "golem_construction_requires_magic_profession",
                80,
                RetoldVillagerGolemConstructionGameTests::requiresMagicProfession
        );
        registerTest(
                event,
                constructionEnvironment,
                "golem_construction_stages_and_conserves_village_emerald",
                360,
                RetoldVillagerGolemConstructionGameTests::stagesAndConservesVillageEmerald
        );
        registerTest(
                event,
                utilityEnvironment,
                "golem_construction_retains_one_traded_emerald",
                80,
                RetoldVillagerGolemConstructionGameTests::retainsOneTradedEmerald
        );
        registerTest(
                event,
                utilityEnvironment,
                "golem_player_animation_costs_five_levels_and_creative_is_free",
                80,
                RetoldVillagerGolemConstructionGameTests::playerAnimationCostAndCreativeException
        );
    }

    private static void keepsVanillaVillagerEligibility(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        RetoldWorldData data = RetoldWorldData.get(level);
        RetoldWorldStage originalStage = data.getStage();
        List<Villager> villagers = new ArrayList<>();

        try {
            data.setStage(RetoldWorldStage.STAGE_2);
            placeFloor(helper, 1, 13, 1, 13);
            long gameTime = level.getGameTime();

            for (int index = 0; index < 4; index++) {
                villagers.add(spawnEligibleVillager(
                        helper,
                        6 + index % 2,
                        2,
                        6 + index / 2,
                        gameTime
                ));
            }

            Villager builder = villagers.getFirst();
            builder.getInventory().addItem(Items.EMERALD.getDefaultInstance());
            int golemsBefore = level.getEntitiesOfClass(
                    IronGolem.class,
                    builder.getBoundingBox().inflate(12.0D)
            ).size();
            builder.spawnGolemIfNeeded(level, gameTime, 5);
            helper.assertFalse(
                    RetoldVillagerGolemConstruction.isBuilding(builder),
                    "Four agreeing Villagers must not bypass vanilla's five-Villager rule"
            );

            villagers.add(spawnEligibleVillager(
                    helper,
                    8,
                    2,
                    6,
                    gameTime
            ));
            builder.spawnGolemIfNeeded(level, gameTime, 5);
            helper.assertTrue(
                    RetoldVillagerGolemConstruction.isBuilding(builder),
                    "Five normally eligible Villagers must start the Retold construction"
            );
            helper.assertTrue(
                    level.getEntitiesOfClass(
                            IronGolem.class,
                            builder.getBoundingBox().inflate(12.0D)
                    ).size() == golemsBefore,
                    "The intercepted vanilla decision must not spawn an instant golem"
            );
            helper.succeed();
        } finally {
            villagers.forEach(Villager::discard);
            data.setStage(originalStage);
        }
    }

    private static void requiresMagicProfession(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        RetoldWorldData data = RetoldWorldData.get(level);
        RetoldWorldStage originalStage = data.getStage();
        List<Villager> villagers = new ArrayList<>();

        try {
            data.setStage(RetoldWorldStage.STAGE_2);
            placeFloor(helper, 1, 13, 1, 13);
            long gameTime = level.getGameTime();

            for (int index = 0; index < 5; index++) {
                villagers.add(spawnEligibleVillager(
                        helper,
                        6 + index % 2,
                        2,
                        6 + index / 2,
                        gameTime
                ));
            }

            Villager builder = villagers.getFirst();
            builder.getInventory().addItem(Items.EMERALD.getDefaultInstance());
            assertCanConstruct(helper, builder, VillagerProfession.CLERIC);
            assertCanConstruct(helper, builder, VillagerProfession.LIBRARIAN);
            assertCanConstruct(helper, builder, VillagerProfession.ARMORER);
            assertCanConstruct(helper, builder, VillagerProfession.TOOLSMITH);
            assertCanConstruct(helper, builder, VillagerProfession.WEAPONSMITH);
            assertCannotConstruct(helper, builder, VillagerProfession.NITWIT);
            assertCannotConstruct(helper, builder, VillagerProfession.NONE);
            assertCannotConstruct(helper, builder, VillagerProfession.FARMER);

            setProfession(helper, builder, VillagerProfession.NITWIT);
            builder.spawnGolemIfNeeded(level, gameTime, 5);
            helper.assertFalse(
                    RetoldVillagerGolemConstruction.isBuilding(builder),
                    "A Nitwit must not start magical Iron Golem construction"
            );

            setProfession(helper, builder, VillagerProfession.ARMORER);
            builder.spawnGolemIfNeeded(level, gameTime, 5);
            helper.assertTrue(
                    RetoldVillagerGolemConstruction.isBuilding(builder),
                    "An Armorer must be able to start otherwise eligible construction"
            );
            helper.succeed();
        } finally {
            villagers.forEach(Villager::discard);
            data.setStage(originalStage);
        }
    }

    private static void stagesAndConservesVillageEmerald(
            GameTestHelper helper
    ) {
        ServerLevel level = helper.getLevel();
        RetoldWorldData data = RetoldWorldData.get(level);
        RetoldWorldStage originalStage = data.getStage();
        data.setStage(RetoldWorldStage.STAGE_2);
        placeFloor(helper, 1, 14, 1, 14);

        BlockPos chestPos = new BlockPos(4, 2, 4);
        helper.setBlock(chestPos, Blocks.CHEST);
        Container chest = containerAt(helper, chestPos);
        chest.setItem(0, Items.EMERALD.getDefaultInstance());
        chest.setChanged();

        long startTime = level.getGameTime();
        List<Villager> villagers = new ArrayList<>();

        for (int index = 0; index < 5; index++) {
            Villager villager = spawnEligibleVillager(
                    helper,
                    7 + index % 2,
                    2,
                    7 + index / 2,
                    startTime
            );
            setVillageHome(helper, villager, new BlockPos(6, 2, 6));
            villagers.add(villager);
        }

        Villager builder = villagers.getFirst();
        builder.spawnGolemIfNeeded(level, startTime, 5);
        RetoldVillagerGolemConstruction.BuildState initial =
                RetoldVillagerGolemConstruction.constructionState(builder);
        helper.assertTrue(initial != null, "An emerald in village storage must qualify");
        boolean[] observations = new boolean[3];
        helper.onEachTick(() -> {
            RetoldVillagerGolemConstruction.BuildState state =
                    RetoldVillagerGolemConstruction.constructionState(builder);

            if (state == null) {
                return;
            }

            builder.getBrain().setActiveActivityIfPossible(Activity.IDLE);
            builder.snapTo(
                    state.access().getX() + 0.5D,
                    state.access().getY(),
                    state.access().getZ() + 0.5D,
                    0.0F,
                    0.0F
            );
            builder.getNavigation().stop();
            RetoldVillagerGolemConstruction.tick(
                    level,
                    builder,
                    level.getGameTime()
            );

            RetoldVillagerGolemConstruction.BuildState advanced =
                    RetoldVillagerGolemConstruction.constructionState(builder);

            if (advanced != null) {
                observations[0] |= advanced.step() > 0 && advanced.step() < 5;
                observations[1] |= advanced.step() >= 5
                        && level.getBlockState(advanced.top()).is(Blocks.PUMPKIN);
                observations[2] |= advanced.step() == 6
                        && builder.getMainHandItem().is(Items.EMERALD);
            }
        });

        helper.succeedWhen(() -> {
            List<IronGolem> golems = level.getEntitiesOfClass(
                    IronGolem.class,
                    new AABB(initial.center()).inflate(2.0D)
            );
            RetoldVillagerGolemConstruction.BuildState current =
                    RetoldVillagerGolemConstruction.constructionState(builder);
            RetoldMobState mobState = RetoldMobStates.getOrCreate(
                    builder,
                    level.getGameTime()
            );
            helper.assertTrue(
                    !golems.isEmpty(),
                    "The staged structure must animate into a golem; step="
                            + (current == null ? "none" : current.step())
                            + ", emeralds="
                            + countItem(chest, Items.EMERALD.getDefaultInstance())
                            + ", top="
                            + level.getBlockState(initial.top())
                            + ", profession="
                            + builder.getVillagerData().profession().unwrapKey()
                            .map(ResourceKey::identifier)
                            .orElse(null)
                            + ", alive="
                            + builder.isAlive()
                            + ", removed="
                            + builder.isRemoved()
                            + ", hunger="
                            + mobState.hunger()
                            + ", eatDrive="
                            + RetoldMobRules.hasEatDrive(builder, mobState)
                            + ", urgent="
                            + RetoldVillagerCommunalFood.hasUrgentVanillaActivity(builder)
                            + ", activity="
                            + builder.getBrain().getActiveNonCoreActivity()
                            + ", control="
                            + RetoldAiControl.getOwner(builder)
                            + "/"
                            + RetoldAiControl.getPriority(builder)
                            + "/"
                            + RetoldAiControl.getReason(builder)
            );
            helper.assertFalse(
                    golems.getFirst().isPlayerCreated(),
                    "A Villager-built golem must remain a village defender"
            );
            helper.assertTrue(observations[0], "Construction must expose staged iron placement");
            helper.assertTrue(observations[1], "Construction must visibly place the pumpkin");
            helper.assertTrue(observations[2], "The builder must visibly hold the paid emerald");
            helper.assertValueEqual(
                    countItem(chest, Items.EMERALD.getDefaultInstance()),
                    0,
                    "Animating one golem must consume exactly one stored emerald"
            );
            helper.assertFalse(
                    RetoldVillagerGolemConstruction.isBuilding(builder),
                    "Successful construction state must be cleared"
            );
            data.setStage(originalStage);
            villagers.forEach(Villager::discard);
        });
    }

    private static void retainsOneTradedEmerald(GameTestHelper helper) {
        placeFloor(helper, 1, 8, 1, 8);
        Villager villager = helper.spawn(EntityTypes.VILLAGER, 4, 2, 4);
        setVillageHome(helper, villager, new BlockPos(4, 2, 4));

        for (int slot = 0; slot < villager.getInventory().getContainerSize(); slot++) {
            villager.getInventory().setItem(
                    slot,
                    new ItemStack(Items.COBBLESTONE, 64)
            );
        }

        BlockPos chestPos = new BlockPos(6, 2, 4);
        helper.setBlock(chestPos, Blocks.BARREL);
        Container storage = containerAt(helper, chestPos);
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(
                GameType.SURVIVAL
        );
        MerchantOffer offer = new MerchantOffer(
                new ItemCost(Items.EMERALD, 25),
                new ItemStack(Items.DIAMOND),
                4,
                1,
                0.0F
        );

        RetoldGolemEvents.onTradeWithVillager(
                new TradeWithVillagerEvent(player, offer, villager)
        );
        helper.assertValueEqual(
                countItem(storage, Items.EMERALD.getDefaultInstance()),
                1,
                "A completed emerald trade must retain exactly one physical emerald, not its price"
        );
        helper.assertValueEqual(
                RetoldVillageContainerOwnership.ownedCount(
                        helper.getLevel(),
                        helper.absolutePos(chestPos),
                        Items.EMERALD.getDefaultInstance()
                ),
                1,
                "A Villager-retained trade emerald stored in the village must be village-owned"
        );
        helper.succeed();
    }

    private static void playerAnimationCostAndCreativeException(
            GameTestHelper helper
    ) {
        ServerLevel level = helper.getLevel();
        placeFloor(helper, 1, 14, 1, 14);

        BlockPos survivalCenter = helper.absolutePos(new BlockPos(5, 2, 5));
        placeIronGolemBase(level, survivalCenter);
        ServerPlayer survival = (ServerPlayer) helper.makeMockServerPlayer(
                GameType.SURVIVAL
        );
        ItemStack survivalPumpkins = new ItemStack(Items.CARVED_PUMPKIN, 3);
        survival.setItemInHand(InteractionHand.MAIN_HAND, survivalPumpkins);
        survival.experienceLevel = 4;

        InteractionResult denied = usePumpkinOnBase(survival, survivalCenter);
        helper.assertTrue(
                denied == InteractionResult.FAIL,
                "A Survival player below five levels must not complete the golem"
        );
        helper.assertTrue(
                level.getBlockState(survivalCenter.above(2)).isAir(),
                "A rejected pumpkin placement must leave the structure unchanged"
        );
        helper.assertValueEqual(survivalPumpkins.getCount(), 3, "A rejected placement must not consume the pumpkin");
        helper.assertValueEqual(survival.experienceLevel, 4, "A rejected placement must not consume XP");

        survival.experienceLevel = RetoldGolemAnimation.PLAYER_LEVEL_COST;
        survival.snapTo(
                survivalCenter.getX() + 0.5D,
                survivalCenter.getY(),
                survivalCenter.getZ() + 2.5D,
                0.0F,
                0.0F
        );
        IronGolem advancementProbe = helper.spawn(
                EntityTypes.IRON_GOLEM,
                2,
                2,
                12
        );
        helper.assertFalse(
                RetoldGolemAnimation.suppressesSummonedEntityAdvancement(
                        advancementProbe
                ),
                "Ordinary Iron Golem animation must retain Hired Help"
        );
        RetoldGolemAnimation.animateVillagerBuiltGolem(() -> {
            helper.assertTrue(
                    RetoldGolemAnimation.suppressesSummonedEntityAdvancement(
                            advancementProbe
                    ),
                    "Villager construction must suppress Hired Help"
            );
            return true;
        });
        helper.assertFalse(
                RetoldGolemAnimation.suppressesSummonedEntityAdvancement(
                        advancementProbe
                ),
                "Hired Help suppression must end with Villager animation"
        );
        advancementProbe.discard();
        level.setBlock(
                survivalCenter.above(2).west(),
                Blocks.STONE.defaultBlockState(),
                3
        );
        helper.assertTrue(
                usePumpkinOnBase(survival, survivalCenter).consumesAction(),
                "An obstructed T may accept a pumpkin without animating"
        );
        helper.assertValueEqual(
                survival.experienceLevel,
                RetoldGolemAnimation.PLAYER_LEVEL_COST,
                "A structure that cannot animate must not consume levels"
        );
        level.setBlock(
                survivalCenter.above(2),
                Blocks.AIR.defaultBlockState(),
                3
        );
        level.setBlock(
                survivalCenter.above(2).west(),
                Blocks.AIR.defaultBlockState(),
                3
        );

        InteractionResult paid = usePumpkinOnBase(survival, survivalCenter);
        helper.assertTrue(paid.consumesAction(), "Five levels must allow animation");
        helper.assertValueEqual(survival.experienceLevel, 0, "Animation must consume exactly five levels");
        helper.assertValueEqual(survivalPumpkins.getCount(), 1, "Successful Survival placement must consume one pumpkin");
        IronGolem paidGolem = level.getEntitiesOfClass(
                IronGolem.class,
                new AABB(survivalCenter).inflate(3.0D)
        ).stream().findFirst().orElse(null);
        helper.assertTrue(paidGolem != null && paidGolem.isPlayerCreated(), "The paid golem must retain vanilla player-created ownership");

        BlockPos creativeCenter = helper.absolutePos(new BlockPos(10, 2, 5));
        placeIronGolemBase(level, creativeCenter);
        ServerPlayer creative = (ServerPlayer) helper.makeMockServerPlayer(
                GameType.CREATIVE
        );
        ItemStack creativePumpkin = new ItemStack(Items.CARVED_PUMPKIN);
        creative.setItemInHand(InteractionHand.MAIN_HAND, creativePumpkin);
        creative.experienceLevel = 0;
        helper.assertTrue(
                usePumpkinOnBase(creative, creativeCenter).consumesAction(),
                "Creative must animate an Iron Golem for free"
        );
        helper.assertValueEqual(creative.experienceLevel, 0, "Creative must not lose levels");
        helper.assertValueEqual(creativePumpkin.getCount(), 1, "Creative must not consume the pumpkin");

        BlockPos snowBottom = helper.absolutePos(new BlockPos(8, 2, 10));
        level.setBlock(snowBottom, Blocks.SNOW_BLOCK.defaultBlockState(), 3);
        level.setBlock(snowBottom.above(), Blocks.SNOW_BLOCK.defaultBlockState(), 3);
        ItemStack snowPumpkin = new ItemStack(Items.CARVED_PUMPKIN);
        survival.setItemInHand(InteractionHand.MAIN_HAND, snowPumpkin);
        survival.experienceLevel = 0;
        helper.assertTrue(
                usePumpkinAt(survival, snowBottom.above()).consumesAction(),
                "Snow Golems must remain free"
        );
        helper.assertValueEqual(survival.experienceLevel, 0, "A Snow Golem must not consume levels");
        helper.assertTrue(
                !level.getEntitiesOfClass(
                        SnowGolem.class,
                        new AABB(snowBottom).inflate(3.0D)
                ).isEmpty(),
                "The free Snow Golem regression fixture must actually spawn"
        );
        removeMockServerPlayer(level, creative);
        removeMockServerPlayer(level, survival);
        helper.succeed();
    }

    private static void removeMockServerPlayer(
            ServerLevel level,
            ServerPlayer player
    ) {
        level.players().remove(player);
        player.discard();
    }

    private static Villager spawnEligibleVillager(
            GameTestHelper helper,
            int x,
            int y,
            int z,
            long gameTime
    ) {
        Villager villager = helper.spawn(EntityTypes.VILLAGER, x, y, z);
        setProfession(helper, villager, VillagerProfession.CLERIC);
        villager.setVillagerXp(1);
        villager.getBrain().setMemory(MemoryModuleType.LAST_SLEPT, gameTime);
        return villager;
    }

    private static void assertCanConstruct(
            GameTestHelper helper,
            Villager villager,
            ResourceKey<VillagerProfession> profession
    ) {
        setProfession(helper, villager, profession);
        helper.assertTrue(
                RetoldVillagerGolemConstruction.canConstructGolems(villager),
                profession.identifier() + " must be a magical golem builder"
        );
    }

    private static void assertCannotConstruct(
            GameTestHelper helper,
            Villager villager,
            ResourceKey<VillagerProfession> profession
    ) {
        setProfession(helper, villager, profession);
        helper.assertFalse(
                RetoldVillagerGolemConstruction.canConstructGolems(villager),
                profession.identifier() + " must not be a magical golem builder"
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

    private static void placeIronGolemBase(
            ServerLevel level,
            BlockPos center
    ) {
        level.setBlock(center, Blocks.IRON_BLOCK.defaultBlockState(), 3);
        level.setBlock(center.above(), Blocks.IRON_BLOCK.defaultBlockState(), 3);
        level.setBlock(center.above().west(), Blocks.IRON_BLOCK.defaultBlockState(), 3);
        level.setBlock(center.above().east(), Blocks.IRON_BLOCK.defaultBlockState(), 3);
    }

    private static InteractionResult usePumpkinOnBase(
            ServerPlayer player,
            BlockPos center
    ) {
        return usePumpkinAt(player, center.above());
    }

    private static InteractionResult usePumpkinAt(
            ServerPlayer player,
            BlockPos clickedBlock
    ) {
        BlockHitResult hit = new BlockHitResult(
                Vec3.atCenterOf(clickedBlock),
                Direction.UP,
                clickedBlock,
                false
        );
        return player.getMainHandItem().useOn(
                new UseOnContext(player, InteractionHand.MAIN_HAND, hit)
        );
    }

    private static Container containerAt(
            GameTestHelper helper,
            BlockPos relativePos
    ) {
        return RetoldVillagerCommunalFoodSearch.containerAt(
                helper.getLevel(),
                helper.absolutePos(relativePos)
        );
    }

    private static int countItem(Container container, ItemStack wanted) {
        int count = 0;

        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);

            if (ItemStack.isSameItemSameComponents(stack, wanted)) {
                count += stack.getCount();
            }
        }

        return count;
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
