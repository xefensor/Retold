package cz.xefensor.retold.villager;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.behavior.profiles.RetoldMobStates;

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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.function.Consumer;

public final class RetoldVillageAnimalReputationGameTests {
    private static final Identifier EMPTY_STRUCTURE =
            Identifier.withDefaultNamespace("empty");

    private RetoldVillageAnimalReputationGameTests() {
    }

    public static void register(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment =
                event.registerEnvironment(
                        id("isolated_village_animal_reputation"),
                        new TestEnvironmentDefinition.AllOf()
                );
        registerTest(
                event,
                environment,
                "village_animal_reputation_uses_profession_species_roles",
                RetoldVillageAnimalReputationGameTests::usesProfessionSpeciesRoles
        );
        registerTest(
                event,
                environment,
                "village_animal_reputation_consumes_two_storage_items",
                RetoldVillageAnimalReputationGameTests::consumesTwoStorageItems
        );
        registerTest(
                event,
                environment,
                "village_animal_reputation_protects_player_animals",
                RetoldVillageAnimalReputationGameTests::protectsPlayerAnimals
        );
        registerTest(
                event,
                environment,
                "village_animal_reputation_penalizes_witnessed_player_kills",
                RetoldVillageAnimalReputationGameTests::penalizesWitnessedPlayerKills
        );
    }

    private static void usesProfessionSpeciesRoles(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        placeFloor(helper);
        Villager shepherd = villager(
                helper,
                VillagerProfession.SHEPHERD,
                new BlockPos(2, 2, 2)
        );
        Villager leatherworker = villager(
                helper,
                VillagerProfession.LEATHERWORKER,
                new BlockPos(2, 2, 3)
        );
        Villager butcher = villager(
                helper,
                VillagerProfession.BUTCHER,
                new BlockPos(2, 2, 4)
        );
        Sheep sheep = helper.spawn(EntityTypes.SHEEP, 4, 2, 2);
        Goat goat = helper.spawn(EntityTypes.GOAT, 5, 2, 2);
        Cow cow = helper.spawn(EntityTypes.COW, 4, 2, 3);
        MushroomCow mooshroom = helper.spawn(
                EntityTypes.MOOSHROOM,
                5,
                2,
                3
        );
        Pig pig = helper.spawn(EntityTypes.PIG, 4, 2, 4);
        Chicken chicken = helper.spawn(EntityTypes.CHICKEN, 5, 2, 4);
        Rabbit rabbit = helper.spawn(EntityTypes.RABBIT, 6, 2, 4);

        helper.assertTrue(
                RetoldVillageAnimalOwnership.canTend(shepherd, sheep)
                        && RetoldVillageAnimalOwnership.canTend(
                        shepherd,
                        goat
                ),
                "Shepherds must tend Sheep and Goats"
        );
        helper.assertTrue(
                RetoldVillageAnimalOwnership.canTend(leatherworker, cow)
                        && RetoldVillageAnimalOwnership.canTend(
                        leatherworker,
                        mooshroom
                ),
                "Leatherworkers must tend Cows and Mooshrooms"
        );
        helper.assertTrue(
                RetoldVillageAnimalOwnership.canTend(butcher, pig)
                        && RetoldVillageAnimalOwnership.canTend(
                        butcher,
                        chicken
                )
                        && RetoldVillageAnimalOwnership.canTend(
                        butcher,
                        rabbit
                ),
                "Butchers must tend Pigs, Chickens, and Rabbits"
        );
        helper.assertTrue(
                !RetoldVillageAnimalOwnership.canTend(shepherd, cow)
                        && !RetoldVillageAnimalOwnership.canTend(
                        butcher,
                        sheep
                ),
                "Each profession must stay inside its livestock role"
        );

        ServerPlayer player = makePlayer(helper, GameType.SURVIVAL);
        RetoldVillageAnimalEvents.onPlayerInteractAnimal(
                new PlayerInteractEvent.EntityInteract(
                        player,
                        InteractionHand.MAIN_HAND,
                        rabbit
                )
        );
        helper.assertTrue(
                !RetoldVillageAnimalOwnership.canTend(butcher, rabbit),
                "Player-handled livestock must not be newly claimed by a village"
        );
        removePlayer(level, player);
        helper.succeed();
    }

    private static void consumesTwoStorageItems(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        placeFloor(helper);
        BlockPos chestRelative = new BlockPos(3, 2, 2);
        helper.setBlock(chestRelative, Blocks.CHEST);
        Container chest = containerAt(helper, chestRelative);
        chest.setItem(0, new ItemStack(Items.WHEAT, 5));

        Villager shepherd = villager(
                helper,
                VillagerProfession.SHEPHERD,
                new BlockPos(3, 2, 3)
        );
        Sheep first = helper.spawn(EntityTypes.SHEEP, 4, 2, 3);
        Sheep second = helper.spawn(EntityTypes.SHEEP, 5, 2, 3);
        RetoldMobStates.getOrCreate(first, level.getGameTime())
                .setHunger(40);
        RetoldMobStates.getOrCreate(second, level.getGameTime())
                .setHunger(40);
        helper.assertTrue(
                RetoldVillagerAnimalTending.takeIntoInventory(
                        level,
                        shepherd,
                        helper.absolutePos(chestRelative),
                        new ItemStack(Items.WHEAT),
                        2
                ) == 2,
                "The Shepherd must collect exactly two Wheat from storage"
        );
        helper.assertTrue(
                RetoldVillagerAnimalTending.performFeeding(
                        shepherd,
                        first,
                        second,
                        new ItemStack(Items.WHEAT),
                        level.getGameTime()
                ),
                "The valid nearby Shepherd and Sheep pair must be fed"
        );

        helper.assertValueEqual(
                chest.getItem(0).getCount(),
                3,
                "Tending one pair must remove exactly two Wheat from storage"
        );
        helper.assertTrue(
                !first.isInLove()
                        && !second.isInLove()
                        && RetoldMobStates.get(first).hunger() == 12
                        && RetoldMobStates.get(second).hunger() == 12,
                "Tending must satisfy hunger without causing instant love mode"
        );
        helper.assertTrue(
                RetoldVillageAnimalOwnership.isVillageOwned(first)
                        && RetoldVillageAnimalOwnership.isVillageOwned(second),
                "Only successfully fed adults must become village-owned"
        );
        helper.assertValueEqual(
                countItem(shepherd.getInventory(), new ItemStack(Items.WHEAT)),
                0,
                "The two collected Wheat must be consumed without duplication"
        );
        helper.succeed();
    }

    private static void protectsPlayerAnimals(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        placeFloor(helper);
        Sheep first = helper.spawn(EntityTypes.SHEEP, 3, 2, 2);
        Sheep second = helper.spawn(EntityTypes.SHEEP, 4, 2, 2);
        Sheep villageChild = helper.spawn(EntityTypes.SHEEP, 5, 2, 2);
        RetoldVillageAnimalOwnership.markVillageOwned(first);
        RetoldVillageAnimalOwnership.markVillageOwned(second);
        RetoldVillageAnimalEvents.onBabySpawn(
                new BabyEntitySpawnEvent(first, second, villageChild)
        );
        helper.assertTrue(
                RetoldVillageAnimalOwnership.isVillageOwned(villageChild),
                "A non-player offspring of two village-owned parents must inherit ownership"
        );
        TagValueOutput output = TagValueOutput.createWithContext(
                ProblemReporter.DISCARDING,
                level.registryAccess()
        );
        villageChild.saveWithoutId(output);
        CompoundTag saved = output.buildResult();
        Sheep restored = EntityTypes.SHEEP.create(
                level,
                EntitySpawnReason.COMMAND
        );
        helper.assertTrue(
                restored != null,
                "The persistence fixture must create a Sheep"
        );
        restored.load(TagValueInput.create(
                ProblemReporter.DISCARDING,
                level.registryAccess(),
                saved
        ));
        helper.assertTrue(
                RetoldVillageAnimalOwnership.isVillageOwned(restored),
                "Village livestock ownership must survive entity save and load"
        );

        Sheep playerParent = helper.spawn(EntityTypes.SHEEP, 6, 2, 2);
        RetoldVillageAnimalOwnership.markPlayerAssociated(playerParent);
        Sheep playerChild = helper.spawn(EntityTypes.SHEEP, 6, 2, 2);
        RetoldVillageAnimalEvents.onBabySpawn(
                new BabyEntitySpawnEvent(first, playerParent, playerChild)
        );
        helper.assertTrue(
                !RetoldVillageAnimalOwnership.isVillageOwned(playerChild),
                "Automatic offspring of a protected player animal must remain outside village ownership"
        );

        Villager shepherd = villager(
                helper,
                VillagerProfession.SHEPHERD,
                new BlockPos(3, 2, 4)
        );
        helper.assertTrue(
                !RetoldVillageAnimalOwnership.canTend(
                        shepherd,
                        playerChild
                ),
                "A protected player's offspring must remain ineligible for later village claiming"
        );
        helper.succeed();
    }

    private static void penalizesWitnessedPlayerKills(
            GameTestHelper helper
    ) {
        ServerLevel level = helper.getLevel();
        placeFloor(helper);
        Villager witness = villager(
                helper,
                VillagerProfession.FARMER,
                new BlockPos(4, 2, 3)
        );
        Sheep villageSheep = helper.spawn(EntityTypes.SHEEP, 4, 2, 2);
        RetoldVillageAnimalOwnership.markVillageOwned(villageSheep);
        ServerPlayer player = makePlayer(helper, GameType.SURVIVAL);
        RetoldVillageAnimalEvents.onVillageAnimalDeath(
                new LivingDeathEvent(
                        villageSheep,
                        player.damageSources().playerAttack(player)
                )
        );
        helper.assertValueEqual(
                witness.getPlayerReputation(player),
                -50,
                "A witnessed direct player kill must add strong major-negative gossip"
        );

        Sheep environmentalSheep = helper.spawn(
                EntityTypes.SHEEP,
                5,
                2,
                2
        );
        RetoldVillageAnimalOwnership.markVillageOwned(environmentalSheep);
        RetoldVillageAnimalEvents.onVillageAnimalDeath(
                new LivingDeathEvent(
                        environmentalSheep,
                        level.damageSources().generic()
                )
        );
        helper.assertValueEqual(
                witness.getPlayerReputation(player),
                -50,
                "Environmental livestock deaths must not blame a player"
        );

        Zombie zombie = helper.spawn(EntityTypes.ZOMBIE, 5, 2, 3);
        Sheep monsterSheep = helper.spawn(EntityTypes.SHEEP, 6, 2, 3);
        RetoldVillageAnimalOwnership.markVillageOwned(monsterSheep);
        RetoldVillageAnimalEvents.onVillageAnimalDeath(
                new LivingDeathEvent(
                        monsterSheep,
                        zombie.damageSources().mobAttack(zombie)
                )
        );
        helper.assertValueEqual(
                witness.getPlayerReputation(player),
                -50,
                "Monster livestock kills must not blame a player"
        );

        ServerPlayer creative = makePlayer(helper, GameType.CREATIVE);
        Sheep creativeSheep = helper.spawn(EntityTypes.SHEEP, 6, 2, 2);
        RetoldVillageAnimalOwnership.markVillageOwned(creativeSheep);
        RetoldVillageAnimalEvents.onVillageAnimalDeath(
                new LivingDeathEvent(
                        creativeSheep,
                        creative.damageSources().playerAttack(creative)
                )
        );
        helper.assertValueEqual(
                witness.getPlayerReputation(creative),
                0,
                "Creative livestock kills must not damage village reputation"
        );

        removePlayer(level, creative);
        removePlayer(level, player);
        helper.succeed();
    }

    private static Villager villager(
            GameTestHelper helper,
            net.minecraft.resources.ResourceKey<VillagerProfession> profession,
            BlockPos relativePos
    ) {
        Villager villager = helper.spawn(
                EntityTypes.VILLAGER,
                relativePos.getX(),
                relativePos.getY(),
                relativePos.getZ()
        );
        villager.setVillagerData(
                villager.getVillagerData().withProfession(
                        helper.getLevel().registryAccess(),
                        profession
                )
        );
        villager.getBrain().setMemory(
                MemoryModuleType.HOME,
                GlobalPos.of(
                        helper.getLevel().dimension(),
                        helper.absolutePos(relativePos)
                )
        );
        return villager;
    }

    private static ServerPlayer makePlayer(
            GameTestHelper helper,
            GameType gameType
    ) {
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(
                gameType
        );
        BlockPos position = helper.absolutePos(new BlockPos(4, 2, 4));
        player.snapTo(
                position.getX() + 0.5D,
                position.getY(),
                position.getZ() + 0.5D,
                0.0F,
                0.0F
        );
        return player;
    }

    private static void removePlayer(
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

        throw new IllegalStateException("Expected village storage fixture");
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

    private static void placeFloor(GameTestHelper helper) {
        for (int x = 1; x <= 8; x++) {
            for (int z = 1; z <= 5; z++) {
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
                new TestData<>(environment, EMPTY_STRUCTURE, 100, 0, true);
        event.registerTest(id(name), new InlineGameTest(testData, function));
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
