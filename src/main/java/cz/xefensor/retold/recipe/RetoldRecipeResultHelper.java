package cz.xefensor.retold.recipe;

import cz.xefensor.retold.Retold;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public final class RetoldRecipeResultHelper {
    private static final Set<RecipeHolder<?>> REPORTED_FAILURES =
            Collections.synchronizedSet(
                    Collections.newSetFromMap(new WeakHashMap<>())
            );

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
        } catch (RuntimeException exception) {
            if (REPORTED_FAILURES.add(recipe)) {
                Retold.LOGGER.error(
                        "Failed to inspect the result of recipe {}",
                        recipe.id(),
                        exception
                );
            }

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
