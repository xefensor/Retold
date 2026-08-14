package cz.xefensor.retold.client.recipe;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;

import java.util.Collection;
import java.util.Set;

/** Read-only client snapshot used by recipe UIs and optional viewer integrations. */
public final class RetoldClientRecipeKnowledge {
    private static Set<ResourceKey<Recipe<?>>> knownRecipes = Set.of();

    private RetoldClientRecipeKnowledge() {
    }

    public static boolean isKnown(ResourceKey<Recipe<?>> recipeId) {
        return recipeId != null && knownRecipes.contains(recipeId);
    }

    public static Set<ResourceKey<Recipe<?>>> snapshot() {
        return knownRecipes;
    }

    public static void replace(Collection<ResourceKey<Recipe<?>>> recipeIds) {
        knownRecipes = Set.copyOf(recipeIds);
    }

    public static void clear() {
        knownRecipes = Set.of();
    }
}
