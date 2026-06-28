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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public final class RetoldRecipeBookEvents {
    private RetoldRecipeBookEvents() {
    }

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

        if (recipe.isEmpty()) {
            return;
        }

        if (!(serverPlayer.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        RetoldCraftedRecipeData data = RetoldCraftedRecipeData.get(serverLevel);
        data.markCrafted(serverPlayer, recipe.get().id());

        unlockRecipeSilently(serverPlayer, recipe.get());

        lockUnknownRecipes(serverPlayer);
    }

    private static void unlockRecipeSilently(
            ServerPlayer player,
            RecipeHolder<CraftingRecipe> recipe
    ) {
        player.getRecipeBook().add(recipe.id());
        player.getRecipeBook().removeHighlight(recipe.id());
        player.getRecipeBook().sendInitialRecipeBook(player);
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