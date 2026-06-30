package cz.xefensor.retold.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;

public final class RetoldRecipeResultHelper {
    private RetoldRecipeResultHelper() {
    }

    public static ItemStack getResultWithoutCraftingGuess(
            RecipeHolder<?> recipe
    ) {
        try {
            if (recipe.value() instanceof AbstractCookingRecipe cookingRecipe) {
                return cookingRecipe.assemble(new SingleRecipeInput(ItemStack.EMPTY));
            }

            if (recipe.value() instanceof SingleItemRecipe singleItemRecipe) {
                return singleItemRecipe.assemble(new SingleRecipeInput(ItemStack.EMPTY));
            }
        } catch (Exception ignored) {
            return ItemStack.EMPTY;
        }

        return ItemStack.EMPTY;
    }

    public static boolean hasSameResultWithoutCraftingGuess(
            RecipeHolder<?> recipe,
            ItemStack expectedResult
    ) {
        if (expectedResult.isEmpty()) {
            return false;
        }

        ItemStack actualResult = getResultWithoutCraftingGuess(recipe);

        if (actualResult.isEmpty()) {
            return false;
        }

        return ItemStack.isSameItemSameComponents(actualResult, expectedResult);
    }
}