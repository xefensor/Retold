package cz.xefensor.retold.recipe;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class RetoldRecipeBookEvents {
    private static final Set<RecipeType<?>> OUTPUT_ONLY_RECIPE_TYPES = Set.of(
            RecipeType.STONECUTTING,
            RecipeType.SMITHING
    );

    private RetoldRecipeBookEvents() {
    }

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        Player player = event.getEntity();

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        Optional<RecipeHolder<?>> recipe = findCraftingRecipe(
                serverPlayer,
                event.getInventory()
        );

        if (recipe.isPresent()) {
            markAndUnlockRecipe(serverPlayer, recipe.get());
            return;
        }

        markAndUnlockRecipesByResult(
                serverPlayer,
                event.getCrafting(),
                OUTPUT_ONLY_RECIPE_TYPES
        );
    }

    private static Optional<RecipeHolder<?>> findCraftingRecipe(
            ServerPlayer player,
            Container craftingInventory
    ) {
        int size = craftingInventory.getContainerSize();

        int width;
        int height;

        if (size == 4) {
            width = 2;
            height = 2;
        } else if (size == 9) {
            width = 3;
            height = 3;
        } else {
            return Optional.empty();
        }

        List<ItemStack> stacks = new ArrayList<>();

        for (int slot = 0; slot < size; slot++) {
            stacks.add(craftingInventory.getItem(slot).copy());
        }

        CraftingInput input = CraftingInput.of(width, height, stacks);

        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return Optional.empty();
        }

        Optional<RecipeHolder<CraftingRecipe>> recipe =
                serverLevel
                        .getServer()
                        .getRecipeManager()
                        .getRecipeFor(RecipeType.CRAFTING, input, serverLevel);

        return recipe.map(recipeHolder -> recipeHolder);
    }

    private static void markAndUnlockRecipesByResult(
            ServerPlayer player,
            ItemStack craftedResult,
            Set<RecipeType<?>> allowedTypes
    ) {
        if (craftedResult.isEmpty()) {
            return;
        }

        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        MinecraftServer server = serverLevel.getServer();

        for (RecipeHolder<?> recipe : server.getRecipeManager().getRecipes()) {
            if (!allowedTypes.contains(recipe.value().getType())) {
                continue;
            }

            if (!RetoldRecipeResultHelper.hasSameResultWithoutCraftingGuess(
                    recipe,
                    craftedResult
            )) {
                continue;
            }

            markAndUnlockRecipe(player, recipe);
        }
    }

    public static void markKnownRecipe(
            ServerPlayer player,
            RecipeHolder<?> recipe
    ) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        RetoldKnownRecipeData data = RetoldKnownRecipeData.get(serverLevel);
        data.markKnown(player, recipe.id());
    }

    public static void markKnownRecipeAndUnlockCookingSiblings(
            ServerPlayer player,
            RecipeHolder<?> recipe
    ) {
        markKnownRecipe(player, recipe);
        markAndUnlockCookingSiblings(player, recipe);
    }

    private static void markAndUnlockRecipe(
            ServerPlayer player,
            RecipeHolder<?> recipe
    ) {
        markAndUnlockRecipe(player, recipe, true);
    }

    private static void markAndUnlockRecipe(
            ServerPlayer player,
            RecipeHolder<?> recipe,
            boolean unlockCookingSiblings
    ) {
        markKnownRecipe(player, recipe);
        unlockRecipeSilently(player, recipe);

        if (unlockCookingSiblings) {
            markAndUnlockCookingSiblings(player, recipe);
        }
    }

    private static void markAndUnlockCookingSiblings(
            ServerPlayer player,
            RecipeHolder<?> recipe
    ) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        List<RecipeHolder<?>> siblings =
                RetoldCookingRecipeSiblingHelper.findCookingSiblings(
                        serverLevel.getServer(),
                        recipe
                );

        for (RecipeHolder<?> sibling : siblings) {
            markAndUnlockRecipe(player, sibling, false);
        }
    }

    public static void markKnownAndUnlockRecipe(
            ServerPlayer player,
            RecipeHolder<?> recipe
    ) {
        markAndUnlockRecipe(player, recipe);
    }

    private static void unlockRecipeSilently(
            ServerPlayer player,
            RecipeHolder<?> recipe
    ) {
        if (player.getRecipeBook().contains(recipe.id())) {
            return;
        }

        RetoldRecipeUnlockContext.beginInternalUnlock();

        try {
            player.getRecipeBook().addRecipes(List.of(recipe), player);
        } finally {
            RetoldRecipeUnlockContext.endInternalUnlock();
        }
    }
}