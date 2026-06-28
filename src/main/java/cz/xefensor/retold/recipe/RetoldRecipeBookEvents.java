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
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class RetoldRecipeBookEvents {
    private RetoldRecipeBookEvents() {
    }

    private static final Set<RecipeType<?>> COOKING_RECIPE_TYPES = Set.of(
            RecipeType.SMELTING,
            RecipeType.BLASTING,
            RecipeType.SMOKING,
            RecipeType.CAMPFIRE_COOKING
    );

    private static final Set<RecipeType<?>> OUTPUT_ONLY_RECIPE_TYPES = Set.of(
            RecipeType.STONECUTTING,
            RecipeType.SMITHING
    );

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        lockUnknownRecipes(serverPlayer);
    }

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        Player player = event.getEntity();

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        Optional<RecipeHolder<CraftingRecipe>> recipe = findCraftingRecipe(
                serverPlayer,
                event.getInventory()
        );

        if (recipe.isPresent()) {
            markAndUnlockRecipe(serverPlayer, recipe.get());
            lockUnknownRecipes(serverPlayer);
            return;
        }

        // Fallback pro bloky, které nedávají 2x2 / 3x3 crafting grid.
        // Typicky stonecutter / smithing apod. Tam NeoForge event nedává přesný RecipeInput,
        // takže to hledáme podle výsledného itemu.
        markAndUnlockRecipesByResult(
                serverPlayer,
                event.getCrafting(),
                OUTPUT_ONLY_RECIPE_TYPES
        );

        lockUnknownRecipes(serverPlayer);
    }

    @SubscribeEvent
    public static void onItemSmelted(PlayerEvent.ItemSmeltedEvent event) {
        Player player = event.getEntity();

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        markAndUnlockRecipesByResult(
                serverPlayer,
                event.getSmelting(),
                COOKING_RECIPE_TYPES
        );

        lockUnknownRecipes(serverPlayer);
    }

    @SubscribeEvent
    public static void onPlayerTickPost(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        // Safety cleanup. Vanilla recipe advancements can unlock recipes after inventory changes.
        if (serverPlayer.tickCount % 40 != 0) {
            return;
        }

        lockUnknownRecipes(serverPlayer);
    }

    private static Optional<RecipeHolder<CraftingRecipe>> findCraftingRecipe(
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

        return serverLevel
                .getServer()
                .getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, input, serverLevel);
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

            ItemStack recipeResult = getRecipeResult(recipe);

            if (recipeResult.isEmpty()) {
                continue;
            }

            if (!ItemStack.isSameItemSameComponents(recipeResult, craftedResult)) {
                continue;
            }

            markAndUnlockRecipe(player, recipe);
        }
    }

    private static ItemStack getRecipeResult(RecipeHolder<?> recipe) {
        if (recipe.value() instanceof AbstractCookingRecipe cookingRecipe) {
            return cookingRecipe.assemble(new SingleRecipeInput(ItemStack.EMPTY));
        }

        if (recipe.value() instanceof SingleItemRecipe singleItemRecipe) {
            return singleItemRecipe.assemble(new SingleRecipeInput(ItemStack.EMPTY));
        }

        return ItemStack.EMPTY;
    }

    private static void markAndUnlockRecipe(
            ServerPlayer player,
            RecipeHolder<?> recipe
    ) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        RetoldCraftedRecipeData data = RetoldCraftedRecipeData.get(serverLevel);
        data.markCrafted(player, recipe.id());

        unlockRecipeSilently(player, recipe);
    }

    private static void unlockRecipeSilently(
            ServerPlayer player,
            RecipeHolder<?> recipe
    ) {
        player.getRecipeBook().add(recipe.id());
        player.getRecipeBook().removeHighlight(recipe.id());
        player.getRecipeBook().sendInitialRecipeBook(player);
    }

    private static void lockUnknownRecipes(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        MinecraftServer server = serverLevel.getServer();
        RetoldCraftedRecipeData data = RetoldCraftedRecipeData.get(serverLevel);

        List<RecipeHolder<?>> recipesToLock = new ArrayList<>();

        for (RecipeHolder<?> recipe : server.getRecipeManager().getRecipes()) {
            if (!player.getRecipeBook().contains(recipe.id())) {
                continue;
            }

            if (data.hasCrafted(player, recipe.id())) {
                continue;
            }

            recipesToLock.add(recipe);
        }

        if (!recipesToLock.isEmpty()) {
            player.getRecipeBook().removeRecipes(recipesToLock, player);
        }
    }
}