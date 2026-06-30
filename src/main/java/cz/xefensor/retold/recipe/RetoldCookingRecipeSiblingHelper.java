package cz.xefensor.retold.recipe;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class RetoldCookingRecipeSiblingHelper {
    private static final Set<RecipeType<?>> COOKING_RECIPE_TYPES = Set.of(
            RecipeType.SMELTING,
            RecipeType.BLASTING,
            RecipeType.SMOKING,
            RecipeType.CAMPFIRE_COOKING
    );

    private RetoldCookingRecipeSiblingHelper() {
    }

    public static List<RecipeHolder<?>> findCookingSiblings(
            MinecraftServer server,
            RecipeHolder<?> sourceRecipe
    ) {
        if (!(sourceRecipe.value() instanceof AbstractCookingRecipe sourceCookingRecipe)) {
            return List.of();
        }

        if (!COOKING_RECIPE_TYPES.contains(sourceRecipe.value().getType())) {
            return List.of();
        }

        ItemStack sourceResult =
                RetoldRecipeResultHelper.getResultWithoutCraftingGuess(sourceRecipe);

        if (sourceResult.isEmpty()) {
            return List.of();
        }

        List<RecipeHolder<?>> siblings = new ArrayList<>();

        for (RecipeHolder<?> candidateRecipe : server.getRecipeManager().getRecipes()) {
            if (candidateRecipe.id().equals(sourceRecipe.id())) {
                continue;
            }

            if (!(candidateRecipe.value() instanceof AbstractCookingRecipe candidateCookingRecipe)) {
                continue;
            }

            if (!COOKING_RECIPE_TYPES.contains(candidateRecipe.value().getType())) {
                continue;
            }

            if (candidateRecipe.value().getType() == sourceRecipe.value().getType()) {
                continue;
            }

            if (!sourceCookingRecipe.input().equals(candidateCookingRecipe.input())) {
                continue;
            }

            ItemStack candidateResult =
                    RetoldRecipeResultHelper.getResultWithoutCraftingGuess(candidateRecipe);

            if (candidateResult.isEmpty()) {
                continue;
            }

            if (!ItemStack.isSameItemSameComponents(sourceResult, candidateResult)) {
                continue;
            }

            siblings.add(candidateRecipe);
        }

        return siblings;
    }
}