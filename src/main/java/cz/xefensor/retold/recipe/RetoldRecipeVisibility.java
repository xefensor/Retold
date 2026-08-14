package cz.xefensor.retold.recipe;

import cz.xefensor.retold.api.recipe.RetoldRecipeKnowledge;
import cz.xefensor.retold.client.recipe.RetoldClientRecipeKnowledge;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/** Shared recipe-visibility authority for vanilla and optional recipe viewers. */
public final class RetoldRecipeVisibility {
    private static final Set<RecipeType<?>> DISCOVERY_MANAGED_TYPES = Set.of(
            RecipeType.CRAFTING,
            RecipeType.SMELTING,
            RecipeType.BLASTING,
            RecipeType.SMOKING,
            RecipeType.CAMPFIRE_COOKING,
            RecipeType.STONECUTTING,
            RecipeType.SMITHING
    );
    private static final CopyOnWriteArrayList<RecipeType<?>> OPTIONAL_MANAGED_TYPES =
            new CopyOnWriteArrayList<>();

    private RetoldRecipeVisibility() {
    }

    public static boolean isDiscoveryManaged(RecipeHolder<?> recipe) {
        return recipe != null
                && isDiscoveryManaged(recipe.value().getType());
    }

    public static boolean isDiscoveryManaged(RecipeType<?> recipeType) {
        return recipeType != null
                && (DISCOVERY_MANAGED_TYPES.contains(recipeType)
                || OPTIONAL_MANAGED_TYPES.contains(recipeType));
    }

    public static RetoldRecipeKnowledge.DiscoveryRegistration registerDiscoveryManagedType(
            RecipeType<?> recipeType
    ) {
        OPTIONAL_MANAGED_TYPES.add(recipeType);
        return () -> OPTIONAL_MANAGED_TYPES.remove(recipeType);
    }

    public static boolean isKnown(
            ServerPlayer player,
            ResourceKey<Recipe<?>> recipeId
    ) {
        if (player == null || recipeId == null) {
            return false;
        }

        return RetoldKnownRecipeData.get(player.level()).hasKnown(player, recipeId);
    }

    public static boolean isVisibleTo(ServerPlayer player, RecipeHolder<?> recipe) {
        if (player == null || recipe == null) {
            return false;
        }

        return isVisibleTo(player, recipe.id(), recipe.value().getType());
    }

    public static boolean isVisibleTo(
            ServerPlayer player,
            ResourceKey<Recipe<?>> recipeId,
            RecipeType<?> recipeType
    ) {
        if (player == null || recipeId == null || recipeType == null) {
            return false;
        }

        return !isDiscoveryManaged(recipeType) || isKnown(player, recipeId);
    }

    public static boolean isVisibleTo(Player player, RecipeHolder<?> recipe) {
        if (player == null || recipe == null) {
            return false;
        }

        return isVisibleTo(player, recipe.id(), recipe.value().getType());
    }

    public static boolean isVisibleTo(
            Player player,
            ResourceKey<Recipe<?>> recipeId,
            RecipeType<?> recipeType
    ) {
        if (player == null || recipeId == null || recipeType == null) {
            return false;
        }

        if (!isDiscoveryManaged(recipeType)) {
            return true;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            return isKnown(serverPlayer, recipeId);
        }

        return RetoldClientRecipeKnowledge.isKnown(recipeId);
    }
}
