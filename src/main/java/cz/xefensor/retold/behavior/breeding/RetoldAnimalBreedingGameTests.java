package cz.xefensor.retold.behavior.breeding;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;
import cz.xefensor.retold.behavior.profiles.RetoldMobState;
import cz.xefensor.retold.behavior.profiles.RetoldMobStates;
import cz.xefensor.retold.registry.RetoldTags;

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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.armadillo.Armadillo;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.equine.Donkey;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.animal.nautilus.Nautilus;
import net.minecraft.world.entity.animal.turtle.Turtle;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.function.Consumer;

public final class RetoldAnimalBreedingGameTests {
    private static final Identifier EMPTY_STRUCTURE =
            Identifier.withDefaultNamespace("empty");

    private RetoldAnimalBreedingGameTests() {
    }

    public static void register(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment =
                event.registerEnvironment(
                        id("isolated_hunger_breeding"),
                        new TestEnvironmentDefinition.AllOf()
                );
        registerTest(
                event,
                environment,
                "animal_breeding_covers_every_vanilla_breeder",
                RetoldAnimalBreedingGameTests::coversEveryVanillaBreeder
        );
        registerTest(
                event,
                environment,
                "animal_breeding_player_food_only_relieves_hunger",
                RetoldAnimalBreedingGameTests::playerFoodOnlyRelievesHunger
        );
        registerTest(
                event,
                environment,
                "animal_breeding_requires_sustained_satisfaction",
                RetoldAnimalBreedingGameTests::requiresSustainedSatisfaction
        );
        registerTest(
                event,
                environment,
                "animal_breeding_persists_and_preserves_special_pairs",
                RetoldAnimalBreedingGameTests::persistsAndPreservesSpecialPairs
        );
    }

    private static void coversEveryVanillaBreeder(GameTestHelper helper) {
        assertTagged(
                helper,
                EntityTypes.ARMADILLO,
                EntityTypes.AXOLOTL,
                EntityTypes.BEE,
                EntityTypes.CAMEL,
                EntityTypes.CAT,
                EntityTypes.CHICKEN,
                EntityTypes.COW,
                EntityTypes.DONKEY,
                EntityTypes.FOX,
                EntityTypes.FROG,
                EntityTypes.GOAT,
                EntityTypes.HOGLIN,
                EntityTypes.HORSE,
                EntityTypes.LLAMA,
                EntityTypes.MOOSHROOM,
                EntityTypes.NAUTILUS,
                EntityTypes.OCELOT,
                EntityTypes.PANDA,
                EntityTypes.PIG,
                EntityTypes.RABBIT,
                EntityTypes.SHEEP,
                EntityTypes.SNIFFER,
                EntityTypes.STRIDER,
                EntityTypes.TRADER_LLAMA,
                EntityTypes.TURTLE,
                EntityTypes.WOLF
        );

        Armadillo armadillo = helper.spawn(EntityTypes.ARMADILLO, 2, 2, 2);
        Turtle turtle = helper.spawn(EntityTypes.TURTLE, 3, 2, 2);
        Strider strider = helper.spawn(EntityTypes.STRIDER, 4, 2, 2);
        Nautilus nautilus = helper.spawn(EntityTypes.NAUTILUS, 5, 2, 2);

        helper.assertTrue(
                RetoldMobRules.hungerInterval(armadillo) > 0
                        && RetoldMobRules.hungerInterval(turtle) > 0
                        && RetoldMobRules.hungerInterval(strider) > 0
                        && RetoldMobRules.hungerInterval(nautilus) > 0,
                "Every newly covered breeder must have active hunger"
        );
        helper.succeed();
    }

    private static void playerFoodOnlyRelievesHunger(
            GameTestHelper helper
    ) {
        placeFloor(helper);
        ServerLevel level = helper.getLevel();
        Cow cow = helper.spawn(EntityTypes.COW, 3, 2, 3);
        RetoldMobState state = RetoldMobStates.getOrCreate(
                cow,
                level.getGameTime()
        );
        state.setHunger(40);
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(
                GameType.SURVIVAL
        );
        player.setItemInHand(
                InteractionHand.MAIN_HAND,
                new ItemStack(Items.WHEAT)
        );

        cow.mobInteract(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(
                player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(),
                "Player feeding must consume the final held item"
        );
        helper.assertValueEqual(
                state.hunger(),
                12,
                "Player Wheat must relieve Cow hunger"
        );
        helper.assertFalse(
                cow.isInLove(),
                "Player food must not directly put an adult into love mode"
        );

        player.setItemInHand(
                InteractionHand.MAIN_HAND,
                new ItemStack(Items.WHEAT)
        );
        state.setHunger(0);
        cow.mobInteract(player, InteractionHand.MAIN_HAND);
        helper.assertValueEqual(
                player.getItemInHand(InteractionHand.MAIN_HAND).getCount(),
                1,
                "A fully satisfied adult must not waste another breeding item"
        );
        helper.assertFalse(
                cow.isInLove(),
                "A full adult still must wait for automatic breeding"
        );
        level.players().remove(player);
        player.discard();
        helper.succeed();
    }

    private static void requiresSustainedSatisfaction(
            GameTestHelper helper
    ) {
        placeFloor(helper);
        ServerLevel level = helper.getLevel();
        Cow first = helper.spawn(EntityTypes.COW, 3, 2, 3);
        Cow second = helper.spawn(EntityTypes.COW, 4, 2, 3);
        long testTime = level.getGameTime()
                + RetoldAnimalBreeding.SATISFIED_TICKS + 100L;
        RetoldMobState firstState = readyState(first, testTime);
        RetoldMobState secondState = readyState(second, testTime);

        secondState.setHunger(21);
        RetoldAnimalBreeding.tick(level, first, testTime);
        helper.assertFalse(
                first.isInLove() || second.isInLove(),
                "One hungry parent must block automatic breeding"
        );

        secondState.setHunger(0);
        secondState.setBreedingSatisfiedTicks(
                RetoldAnimalBreeding.SATISFIED_TICKS
        );
        firstState.scheduleNextBreedingAttempt(testTime);
        RetoldAnimalBreeding.tick(level, first, testTime + 1L);
        helper.assertTrue(
                first.isInLove() && second.isInLove(),
                "Two continuously satisfied compatible adults must automatically begin breeding"
        );

        helper.succeedWhen(() -> {
            long cows = level.getEntitiesOfClass(
                    Cow.class,
                    first.getBoundingBox().inflate(12.0D)
            ).stream().filter(Animal::isBaby).count();
            helper.assertTrue(
                    cows >= 1L,
                    "The armed pair must use vanilla movement and produce an offspring"
            );
            helper.assertValueEqual(
                    firstState.hunger(),
                    RetoldAnimalBreeding.PARENT_HUNGER_COST,
                    "Breeding must make the first parent hungry again"
            );
            helper.assertValueEqual(
                    secondState.hunger(),
                    RetoldAnimalBreeding.PARENT_HUNGER_COST,
                    "Breeding must make the second parent hungry again"
            );
        });
    }

    private static void persistsAndPreservesSpecialPairs(
            GameTestHelper helper
    ) {
        placeFloor(helper);
        ServerLevel level = helper.getLevel();
        long testTime = level.getGameTime()
                + RetoldAnimalBreeding.SATISFIED_TICKS + 200L;
        Cow source = helper.spawn(EntityTypes.COW, 2, 2, 2);
        RetoldMobState sourceState = RetoldMobStates.getOrCreate(
                source,
                testTime
        );
        sourceState.setHunger(0);
        sourceState.setBreedingSatisfiedTicks(1_234L);
        sourceState.advanceBreedingSatisfaction(testTime, 20);
        sourceState.scheduleNextBreedingAttempt(testTime + 77L);
        TagValueOutput output = TagValueOutput.createWithContext(
                ProblemReporter.DISCARDING,
                level.registryAccess()
        );
        source.saveWithoutId(output);
        CompoundTag saved = output.buildResult();
        Cow restored = EntityTypes.COW.create(
                level,
                EntitySpawnReason.COMMAND
        );
        helper.assertTrue(restored != null, "The fixture must create a Cow");
        restored.load(TagValueInput.create(
                ProblemReporter.DISCARDING,
                level.registryAccess(),
                saved
        ));
        RetoldMobState restoredState = RetoldMobStates.getOrCreate(
                restored,
                testTime
        );
        helper.assertValueEqual(
                restoredState.breedingSatisfiedTicks(),
                sourceState.breedingSatisfiedTicks(),
                "Sustained satisfaction must survive entity save/load"
        );
        restoredState.advanceBreedingSatisfaction(testTime + 10_000L, 20);
        helper.assertValueEqual(
                restoredState.breedingSatisfiedTicks(),
                1_254L,
                "Unloaded elapsed time must add at most the current loaded dispatcher interval"
        );
        helper.assertValueEqual(
                restoredState.nextBreedingAttemptAt(),
                testTime + 77L,
                "The breeding retry schedule must survive entity save/load"
        );

        Horse horse = helper.spawn(EntityTypes.HORSE, 4, 2, 3);
        Donkey donkey = helper.spawn(EntityTypes.DONKEY, 5, 2, 3);
        horse.setTamed(true);
        donkey.setTamed(true);
        horse.setHealth(horse.getMaxHealth());
        donkey.setHealth(donkey.getMaxHealth());
        helper.assertTrue(
                RetoldAnimalBreeding.armPair(horse, donkey, testTime),
                "Automatic readiness must preserve vanilla Horse/Donkey crossbreeding"
        );
        RetoldAnimalBreeding.interruptReadiness(horse, testTime + 1L);
        helper.assertFalse(
                horse.isInLove(),
                "Damage or danger interruption must clear breeding readiness"
        );
        helper.assertValueEqual(
                RetoldMobStates.get(horse).breedingSatisfiedTicks(),
                0L,
                "Interrupted readiness must restart its five-minute clock"
        );
        helper.succeed();
    }

    private static RetoldMobState readyState(
            Animal animal,
            long gameTime
    ) {
        RetoldMobState state = RetoldMobStates.getOrCreate(
                animal,
                gameTime
        );
        state.setHunger(0);
        state.setBreedingSatisfiedTicks(
                RetoldAnimalBreeding.SATISFIED_TICKS
        );
        return state;
    }

    private static void assertTagged(
            GameTestHelper helper,
            EntityType<?>... types
    ) {
        for (EntityType<?> type : types) {
            helper.assertTrue(
                    type.builtInRegistryHolder().is(
                            RetoldTags.AUTOMATIC_BREEDERS
                    ),
                    "Missing automatic breeder tag entry for " + type
            );
        }
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
                new TestData<>(environment, EMPTY_STRUCTURE, 200, 0, true);
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
