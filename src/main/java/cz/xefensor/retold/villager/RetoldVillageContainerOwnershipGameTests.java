package cz.xefensor.retold.villager;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import cz.xefensor.retold.Retold;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.BuiltinTestFunctions;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.gossip.GossipType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.function.Consumer;

public final class RetoldVillageContainerOwnershipGameTests {
    private static final Identifier EMPTY_STRUCTURE =
            Identifier.withDefaultNamespace("empty");

    private RetoldVillageContainerOwnershipGameTests() {
    }

    public static void register(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment =
                event.registerEnvironment(
                        id("isolated_village_container_ownership"),
                        new TestEnvironmentDefinition.AllOf()
                );

        registerTest(
                event,
                environment,
                "village_container_ownership_protects_generated_loot_and_saves",
                RetoldVillageContainerOwnershipGameTests::protectsGeneratedLootAndSaves
        );
        registerTest(
                event,
                environment,
                "village_container_ownership_keeps_player_deposits_safe",
                RetoldVillageContainerOwnershipGameTests::keepsPlayerDepositsSafe
        );
        registerTest(
                event,
                environment,
                "village_container_ownership_uses_vanilla_reputation",
                RetoldVillageContainerOwnershipGameTests::usesVanillaReputation
        );
        registerTest(
                event,
                environment,
                "village_container_ownership_reports_village_status",
                RetoldVillageContainerOwnershipGameTests::reportsVillageStatus
        );
    }

    private static void protectsGeneratedLootAndSaves(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        placeFloor(helper, 1, 6, 1, 4);
        BlockPos chestPos = new BlockPos(3, 2, 2);
        BlockPos absoluteChest = helper.absolutePos(chestPos);
        helper.setBlock(chestPos, Blocks.CHEST);
        RetoldVillageContainerOwnership.clear(level, absoluteChest);
        BlockEntity blockEntity = level.getBlockEntity(absoluteChest);

        helper.assertTrue(
                blockEntity instanceof RandomizableContainer,
                "The generated-loot fixture must use a randomizable chest"
        );
        RandomizableContainer generated = (RandomizableContainer) blockEntity;
        generated.setLootTable(BuiltInLootTables.VILLAGE_WEAPONSMITH, 17L);
        generated.isEmpty();
        Container chest = (Container) generated;
        int generatedCount = countAll(chest);

        helper.assertTrue(
                generatedCount > 0,
                "The deterministic village loot fixture must generate items"
        );
        helper.assertValueEqual(
                RetoldVillageContainerOwnership.totalOwned(
                        level,
                        absoluteChest
                ),
                generatedCount,
                "Every generated village-loot item must begin village-owned"
        );

        RetoldVillageContainerOwnershipData saved =
                RetoldVillageContainerOwnershipData.get(level);
        RetoldVillageContainerOwnershipData restored =
                RetoldVillageContainerOwnershipData.fromSerializedState(
                        saved.serializeState()
                );
        helper.assertValueEqual(
                restored.totalOwned(level, absoluteChest),
                generatedCount,
                "Village-owned quantities must survive SavedData serialization"
        );
        helper.succeed();
    }

    private static void keepsPlayerDepositsSafe(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        placeFloor(helper, 1, 7, 1, 5);
        BlockPos chestPos = new BlockPos(4, 2, 2);
        BlockPos absoluteChest = helper.absolutePos(chestPos);
        helper.setBlock(chestPos, Blocks.CHEST);
        Container chest = containerAt(helper, chestPos);
        RetoldVillageContainerOwnership.clear(level, absoluteChest);

        Villager farmer = helper.spawn(EntityTypes.VILLAGER, 3, 2, 2);
        setProfession(helper, farmer, VillagerProfession.FARMER);
        setVillageHome(helper, farmer, new BlockPos(3, 2, 3));
        farmer.getInventory().setItem(0, new ItemStack(Items.CARROT, 30));

        helper.assertValueEqual(
                RetoldVillagerCommunalSupply.tryDeposit(
                        level,
                        farmer,
                        absoluteChest
                ),
                6,
                "The Farmer must deposit the six Carrots above its reserve"
        );
        helper.assertValueEqual(
                RetoldVillageContainerOwnership.ownedCount(
                        level,
                        absoluteChest,
                        new ItemStack(Items.CARROT)
                ),
                6,
                "Farmer-deposited food must become village-owned"
        );

        ServerPlayer player = makePlayer(helper, GameType.SURVIVAL, chestPos);
        ChestMenu menu = ChestMenu.threeRows(
                31,
                player.getInventory(),
                chest
        );
        player.containerMenu = menu;
        menu.setCarried(new ItemStack(Items.CARROT, 4));
        menu.clicked(0, 0, ContainerInput.PICKUP, player);

        helper.assertValueEqual(
                chest.getItem(0).getCount(),
                10,
                "The menu click must merge the player's four Carrots"
        );
        helper.assertValueEqual(
                RetoldVillageContainerOwnership.ownedCount(
                        level,
                        absoluteChest,
                        new ItemStack(Items.CARROT)
                ),
                6,
                "Player additions must not become village-owned"
        );

        RetoldVillageContainerOwnership.PlayerTransaction safeWithdrawal =
                RetoldVillageContainerOwnership.beginPlayerTransaction(
                        player,
                        menu.slots
                );
        chest.removeItem(0, 4);
        RetoldVillageContainerOwnership.finishPlayerTransaction(
                safeWithdrawal
        );

        helper.assertValueEqual(
                farmer.getPlayerReputation(player),
                0,
                "Retrieving the player-deposited quantity must not be theft"
        );
        helper.assertValueEqual(
                RetoldVillageContainerOwnership.ownedCount(
                        level,
                        absoluteChest,
                        new ItemStack(Items.CARROT)
                ),
                6,
                "Safe retrieval must leave the village quantity protected"
        );

        removeMockServerPlayer(level, player);
        farmer.discard();
        helper.succeed();
    }

    private static void usesVanillaReputation(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        placeFloor(helper, 1, 9, 1, 5);
        BlockPos chestPos = new BlockPos(4, 2, 2);
        BlockPos breakPos = new BlockPos(7, 2, 2);
        BlockPos absoluteChest = helper.absolutePos(chestPos);
        BlockPos absoluteBreakChest = helper.absolutePos(breakPos);
        helper.setBlock(chestPos, Blocks.CHEST);
        helper.setBlock(breakPos, Blocks.CHEST);
        Container chest = containerAt(helper, chestPos);
        Container breakChest = containerAt(helper, breakPos);
        RetoldVillageContainerOwnership.clear(level, absoluteChest);
        RetoldVillageContainerOwnership.clear(level, absoluteBreakChest);
        chest.setItem(0, new ItemStack(Items.EMERALD, 2));
        breakChest.setItem(0, new ItemStack(Items.IRON_INGOT, 3));
        RetoldVillageContainerOwnership.markVillageOwned(
                level,
                absoluteChest,
                new ItemStack(Items.EMERALD),
                2
        );
        RetoldVillageContainerOwnership.markVillageOwned(
                level,
                absoluteBreakChest,
                new ItemStack(Items.IRON_INGOT),
                3
        );

        Villager witness = helper.spawn(EntityTypes.VILLAGER, 5, 2, 3);
        setVillageHome(helper, witness, new BlockPos(5, 2, 3));
        ServerPlayer player = makePlayer(helper, GameType.SURVIVAL, chestPos);
        ChestMenu menu = ChestMenu.threeRows(
                32,
                player.getInventory(),
                chest
        );
        player.containerMenu = menu;
        menu.clicked(0, 0, ContainerInput.PICKUP, player);

        helper.assertValueEqual(
                witness.getPlayerReputation(player),
                -25,
                "A witnessed withdrawal of village-owned items must add vanilla minor-negative gossip"
        );
        helper.assertTrue(
                witness.getUnhappyCounter() > 0,
                "A theft witness must visibly object"
        );
        helper.assertValueEqual(
                RetoldVillageContainerOwnership.totalOwned(
                        level,
                        absoluteChest
                ),
                0,
                "Stolen items must leave the protected ledger"
        );

        helper.assertValueEqual(
                RetoldVillageContainerOwnership.handleProtectedContainerBreak(
                        level,
                        absoluteBreakChest,
                        player
                ),
                3,
                "Breaking storage must account for every protected item"
        );
        helper.assertTrue(
                witness.getPlayerReputation(player) <= -100,
                "Breaking protected storage must reach vanilla Iron Golem hostility"
        );

        ServerPlayer creative = makePlayer(
                helper,
                GameType.CREATIVE,
                breakPos
        );
        RetoldVillageContainerOwnership.markVillageOwned(
                level,
                absoluteBreakChest,
                new ItemStack(Items.DIAMOND),
                1
        );
        RetoldVillageContainerOwnership.handleProtectedContainerBreak(
                level,
                absoluteBreakChest,
                creative
        );
        helper.assertValueEqual(
                witness.getPlayerReputation(creative),
                0,
                "Creative container changes must not damage village reputation"
        );

        removeMockServerPlayer(level, creative);
        removeMockServerPlayer(level, player);
        witness.discard();
        helper.succeed();
    }

    private static void reportsVillageStatus(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        placeFloor(helper, 1, 8, 1, 5);
        ServerPlayer player = makePlayer(
                helper,
                GameType.SURVIVAL,
                new BlockPos(3, 2, 2)
        );
        Villager hostile = helper.spawn(EntityTypes.VILLAGER, 4, 2, 2);
        Villager neutral = helper.spawn(EntityTypes.VILLAGER, 5, 2, 2);
        Villager noVillageContext = helper.spawn(
                EntityTypes.VILLAGER,
                6,
                2,
                2
        );
        setVillageHome(helper, hostile, new BlockPos(4, 2, 3));
        setVillageHome(helper, neutral, new BlockPos(5, 2, 3));
        hostile.getGossips().add(
                player.getUUID(),
                GossipType.MAJOR_NEGATIVE,
                20
        );

        RetoldVillageReputationStatus.Snapshot status =
                RetoldVillageReputationStatus.inspect(level, player);
        helper.assertValueEqual(
                status.villagerCount(),
                2,
                "The status must include only nearby village-context Villagers"
        );
        helper.assertValueEqual(
                status.averageReputation(),
                -50,
                "The status must average individual vanilla reputations"
        );
        helper.assertValueEqual(
                status.worstReputation(),
                -100,
                "The status must expose the worst local reputation"
        );
        helper.assertValueEqual(
                status.bestReputation(),
                0,
                "The status must expose the best local reputation"
        );
        helper.assertValueEqual(
                status.negativeVillagers(),
                1,
                "The status must count negative Villagers"
        );
        helper.assertTrue(
                status.hasGolemHostilityRisk(),
                "The status must flag vanilla golem-hostility reputation"
        );
        helper.assertValueEqual(
                status.standing(),
                RetoldVillageReputationStatus.Standing.HOSTILE,
                "Any hostile local Villager must make the summary hostile"
        );
        int commandResult;

        try {
            commandResult = level.getServer()
                    .getCommands()
                    .getDispatcher()
                    .execute(
                            "retold village status",
                            level.getServer()
                                    .createCommandSourceStack()
                                    .withEntity(player)
                                    .withLevel(level)
                                    .withPosition(player.position())
                    );
        } catch (CommandSyntaxException exception) {
            throw new IllegalStateException(
                    "The village-status command must be registered",
                    exception
            );
        }

        helper.assertValueEqual(
                commandResult,
                2,
                "The village-status command must return its Villager count"
        );

        removeMockServerPlayer(level, player);
        hostile.discard();
        neutral.discard();
        noVillageContext.discard();
        helper.succeed();
    }

    private static ServerPlayer makePlayer(
            GameTestHelper helper,
            GameType gameType,
            BlockPos relativePos
    ) {
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(
                gameType
        );
        BlockPos position = helper.absolutePos(relativePos);
        player.snapTo(
                position.getX() + 0.5D,
                position.getY(),
                position.getZ() + 1.5D,
                0.0F,
                0.0F
        );
        return player;
    }

    private static void removeMockServerPlayer(
            ServerLevel level,
            ServerPlayer player
    ) {
        level.players().remove(player);
        player.discard();
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

        throw new IllegalStateException(
                "Expected container at " + relativePos
        );
    }

    private static int countAll(Container container) {
        int count = 0;

        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            count += container.getItem(slot).getCount();
        }

        return count;
    }

    private static void setProfession(
            GameTestHelper helper,
            Villager villager,
            net.minecraft.resources.ResourceKey<VillagerProfession> profession
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
            Consumer<GameTestHelper> function
    ) {
        TestData<Holder<TestEnvironmentDefinition<?>>> testData =
                new TestData<>(environment, EMPTY_STRUCTURE, 80, 0, true);
        event.registerTest(
                id(name),
                new InlineGameTest(testData, function)
        );
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Retold.MODID, path);
    }

    private static final class InlineGameTest
            extends FunctionGameTestInstance {
        private final Consumer<GameTestHelper> function;

        private InlineGameTest(
                TestData<Holder<TestEnvironmentDefinition<?>>> testData,
                Consumer<GameTestHelper> function
        ) {
            super(BuiltinTestFunctions.ALWAYS_PASS, testData);
            this.function = function;
        }

        @Override
        public void run(GameTestHelper helper) {
            function.accept(helper);
        }
    }
}
