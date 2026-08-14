package cz.xefensor.retold.recipe;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.api.recipe.RetoldRecipeKnowledge;
import cz.xefensor.retold.client.recipe.RetoldClientRecipeKnowledge;
import cz.xefensor.retold.network.RetoldRecipeKnowledgeSyncPayload;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.BuiltinTestFunctions;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.List;
import java.util.function.Consumer;

public final class RetoldRecipeCompatibilityGameTests {
    private static final Identifier EMPTY_STRUCTURE =
            Identifier.withDefaultNamespace("empty");

    private RetoldRecipeCompatibilityGameTests() {
    }

    public static void register(
            RegisterGameTestsEvent event,
            Holder<TestEnvironmentDefinition<?>> environment
    ) {
        TestData<Holder<TestEnvironmentDefinition<?>>> testData =
                new TestData<>(environment, EMPTY_STRUCTURE, 40, 0, true);

        event.registerTest(
                id("recipe_visibility_uses_shared_knowledge_authority"),
                new InlineGameTest(
                        testData,
                        RetoldRecipeCompatibilityGameTests::visibilityUsesSharedAuthority
                )
        );
        event.registerTest(
                id("unknown_recipe_types_fail_open_for_viewers"),
                new InlineGameTest(
                        testData,
                        RetoldRecipeCompatibilityGameTests::unknownTypesFailOpen
                )
        );
    }

    private static void visibilityUsesSharedAuthority(GameTestHelper helper) {
        ServerPlayer learner = (ServerPlayer) helper.makeMockServerPlayer(
                GameType.SURVIVAL
        );
        ServerPlayer otherPlayer = (ServerPlayer) helper.makeMockServerPlayer(
                GameType.SURVIVAL
        );
        RecipeHolder<?> recipe = helper.getLevel()
                .getServer()
                .getRecipeManager()
                .getRecipes()
                .stream()
                .filter(candidate -> candidate.value().getType() == RecipeType.CRAFTING)
                .findFirst()
                .orElseThrow();

        for (RecipeType<?> managedType : List.of(
                RecipeType.CRAFTING,
                RecipeType.SMELTING,
                RecipeType.BLASTING,
                RecipeType.SMOKING,
                RecipeType.CAMPFIRE_COOKING,
                RecipeType.STONECUTTING,
                RecipeType.SMITHING
        )) {
            helper.assertTrue(
                    RetoldRecipeKnowledge.isDiscoveryManaged(managedType),
                    "Every existing Retold discovery recipe type must use the shared authority"
            );
        }

        helper.assertFalse(
                RetoldRecipeKnowledge.isVisibleTo(learner, recipe),
                "An undiscovered managed recipe must be hidden"
        );
        helper.assertFalse(
                RetoldRecipeKnowledge.isVisibleTo(otherPlayer, recipe),
                "Recipe visibility must remain per player"
        );

        RetoldRecipeKnowledge.teachAndUnlock(learner, recipe);

        helper.assertTrue(
                RetoldRecipeKnowledge.isVisibleTo(learner, recipe),
                "Learning a recipe must make it visible through the shared authority"
        );
        helper.assertFalse(
                RetoldRecipeKnowledge.isVisibleTo(otherPlayer, recipe),
                "One player's discovery must not reveal a recipe to another player"
        );
        helper.assertTrue(
                learner.getRecipeBook().contains(recipe.id()),
                "The existing vanilla recipe-book unlock must remain synchronized"
        );

        RetoldRecipeKnowledgeSyncPayload original =
                new RetoldRecipeKnowledgeSyncPayload(
                        RetoldKnownRecipeData.get(helper.getLevel())
                                .knownRecipes(learner)
                );
        ByteBuf buffer = Unpooled.buffer();

        try {
            RetoldRecipeKnowledgeSyncPayload.STREAM_CODEC.encode(buffer, original);
            RetoldRecipeKnowledgeSyncPayload decoded =
                    RetoldRecipeKnowledgeSyncPayload.STREAM_CODEC.decode(buffer);

            helper.assertTrue(
                    decoded.knownRecipes().contains(recipe.id()),
                    "The synchronized snapshot must retain learned recipe ids"
            );
            RetoldClientRecipeKnowledge.replace(decoded.knownRecipes());
            helper.assertTrue(
                    RetoldClientRecipeKnowledge.isKnown(recipe.id()),
                    "Client recipe viewers must consume the same learned snapshot"
            );
        } finally {
            RetoldClientRecipeKnowledge.clear();
            buffer.release();
        }

        helper.succeed();
    }

    private static void unknownTypesFailOpen(GameTestHelper helper) {
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(
                GameType.SURVIVAL
        );
        Identifier typeId = Identifier.fromNamespaceAndPath(
                "examplemod",
                "pressing"
        );
        RecipeType<?> unknownType = RecipeType.simple(typeId);
        ResourceKey<Recipe<?>> unknownRecipe = ResourceKey.create(
                Registries.RECIPE,
                Identifier.fromNamespaceAndPath("examplemod", "pressed_plate")
        );

        helper.assertFalse(
                RetoldRecipeKnowledge.isDiscoveryManaged(unknownType),
                "Unknown third-party recipe types must not be managed implicitly"
        );
        helper.assertTrue(
                RetoldRecipeKnowledge.isVisibleTo(
                        player,
                        unknownRecipe,
                        unknownType
                ),
                "Unknown third-party recipe types must fail open for compatibility"
        );

        try (RetoldRecipeKnowledge.DiscoveryRegistration ignored =
                     RetoldRecipeKnowledge.registerDiscoveryManagedType(unknownType)) {
            helper.assertTrue(
                    RetoldRecipeKnowledge.isDiscoveryManaged(unknownType),
                    "An integration must be able to opt a custom recipe type into discovery"
            );
            helper.assertFalse(
                    RetoldRecipeKnowledge.isVisibleTo(
                            player,
                            unknownRecipe,
                            unknownType
                    ),
                    "An opted-in custom recipe must use the same per-player knowledge authority"
            );
        }

        helper.assertTrue(
                RetoldRecipeKnowledge.isVisibleTo(player, unknownRecipe, unknownType),
                "Closing an optional registration must restore fail-open behavior"
        );
        helper.succeed();
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
