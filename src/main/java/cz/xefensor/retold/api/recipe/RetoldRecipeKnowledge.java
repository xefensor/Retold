package cz.xefensor.retold.api.recipe;

import cz.xefensor.retold.recipe.RetoldKnownRecipeData;
import cz.xefensor.retold.recipe.RetoldRecipeBookEvents;
import cz.xefensor.retold.recipe.RetoldRecipeVisibility;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.Set;

/**
 * Stable integration surface for Retold's per-player recipe-discovery rules.
 *
 * <p>Unknown recipe types are visible by default. An optional integration can
 * register a custom machine recipe type when it has a reliable way to teach
 * those recipes through {@link #teachAndUnlock(ServerPlayer, RecipeHolder)}.</p>
 */
public final class RetoldRecipeKnowledge {
    private RetoldRecipeKnowledge() {
    }

    public static boolean isDiscoveryManaged(RecipeHolder<?> recipe) {
        return RetoldRecipeVisibility.isDiscoveryManaged(recipe);
    }

    public static boolean isDiscoveryManaged(RecipeType<?> recipeType) {
        return RetoldRecipeVisibility.isDiscoveryManaged(recipeType);
    }

    public static boolean isKnown(
            ServerPlayer player,
            ResourceKey<Recipe<?>> recipeId
    ) {
        return RetoldRecipeVisibility.isKnown(player, recipeId);
    }

    public static boolean isVisibleTo(Player player, RecipeHolder<?> recipe) {
        return RetoldRecipeVisibility.isVisibleTo(player, recipe);
    }

    public static boolean isVisibleTo(
            Player player,
            ResourceKey<Recipe<?>> recipeId,
            RecipeType<?> recipeType
    ) {
        return RetoldRecipeVisibility.isVisibleTo(player, recipeId, recipeType);
    }

    public static Set<ResourceKey<Recipe<?>>> knownRecipes(ServerPlayer player) {
        if (player == null) {
            return Set.of();
        }

        return RetoldKnownRecipeData.get(player.level()).knownRecipes(player);
    }

    public static void teachAndUnlock(
            ServerPlayer player,
            RecipeHolder<?> recipe
    ) {
        if (player == null || recipe == null) {
            return;
        }

        RetoldRecipeBookEvents.markKnownAndUnlockRecipe(player, recipe);
    }

    public static DiscoveryRegistration registerDiscoveryManagedType(
            RecipeType<?> recipeType
    ) {
        if (recipeType == null) {
            throw new IllegalArgumentException("recipeType must not be null");
        }

        return RetoldRecipeVisibility.registerDiscoveryManagedType(recipeType);
    }

    @FunctionalInterface
    public interface DiscoveryRegistration extends AutoCloseable {
        @Override
        void close();
    }
}
