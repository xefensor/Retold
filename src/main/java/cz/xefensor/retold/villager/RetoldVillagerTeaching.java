package cz.xefensor.retold.villager;

import cz.xefensor.retold.recipe.RetoldKnownRecipeData;
import cz.xefensor.retold.recipe.RetoldRecipeBookEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import cz.xefensor.retold.mixin.MerchantMenuAccessor;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import java.util.Optional;

public final class RetoldVillagerTeaching {
    private RetoldVillagerTeaching() {
    }

    public static void tryTeachHeldItemRecipe(ServerPlayer player) {
        if (!(player.containerMenu instanceof MerchantMenu merchantMenu)) {
            actionBar(player, "You need to talk to a villager.");
            return;
        }

        Merchant merchant = ((MerchantMenuAccessor) merchantMenu).retold$getTrader();

        if (!(merchant instanceof Villager villager)) {
            actionBar(player, "Only villagers can teach recipes.");
            return;
        }

        Holder<VillagerProfession> profession = villager.getVillagerData().profession();

        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        ItemStack shownItem = player.getMainHandItem();

        if (shownItem.isEmpty()) {
            actionBar(player, "Show the villager an item first.");
            return;
        }

        Identifier professionId = profession.unwrapKey()
                .map(key -> key.identifier())
                .orElse(null);

        if (professionId == null) {
            actionBar(player, "This villager has no known profession.");
            return;
        }

        Optional<RetoldVillagerTeachingEntry> teachingEntryOptional =
                RetoldVillagerTeachingReloadListener.get(professionId);

        if (teachingEntryOptional.isEmpty()) {
            actionBar(player, "This villager cannot teach recipes.");
            return;
        }

        RetoldVillagerTeachingEntry teachingEntry = teachingEntryOptional.get();

        Optional<RecipeHolder<?>> recipeOptional = findTeachableRecipeByResult(
                player,
                shownItem,
                teachingEntry
        );

        if (recipeOptional.isEmpty()) {
            actionBar(player, "This villager does not know this recipe.");
            return;
        }

        RecipeHolder<?> recipe = recipeOptional.get();
        Identifier recipeId = recipe.id().identifier();

        int emeraldCost = teachingEntry.emeraldCostFor(recipeId);

        if (emeraldCost < 0) {
            actionBar(player, "This villager does not know this recipe.");
            return;
        }

        RetoldKnownRecipeData data = RetoldKnownRecipeData.get(serverLevel);

        if (data.hasKnown(player, recipe.id())) {
            actionBar(player, "You already know this recipe.");
            return;
        }

        if (!hasEmeralds(player, emeraldCost)) {
            actionBar(player, "You need " + emeraldText(emeraldCost) + ".");
            return;
        }

        takeEmeralds(player, emeraldCost);
        RetoldRecipeBookEvents.markKnownAndUnlockRecipe(player, recipe);

        actionBar(player, "Learned recipe for " + shownItem.getHoverName().getString()
                + " for " + emeraldText(emeraldCost) + ".");
    }

    private static Optional<RecipeHolder<?>> findTeachableRecipeByResult(
            ServerPlayer player,
            ItemStack shownItem,
            RetoldVillagerTeachingEntry teachingEntry
    ) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return Optional.empty();
        }

        MinecraftServer server = serverLevel.getServer();

        for (RecipeHolder<?> recipe : server.getRecipeManager().getRecipes()) {
            Identifier recipeId = recipe.id().identifier();

            if (teachingEntry.emeraldCostFor(recipeId) < 0) {
                continue;
            }

            ItemStack result = getRecipeResult(recipe);

            if (result.isEmpty()) {
                continue;
            }

            if (!ItemStack.isSameItemSameComponents(result, shownItem)) {
                continue;
            }

            return Optional.of(recipe);
        }

        return Optional.empty();
    }

    private static ItemStack getRecipeResult(RecipeHolder<?> recipe) {
        if (recipe.value() instanceof CraftingRecipe craftingRecipe) {
            NonNullList<ItemStack> emptyGrid = NonNullList.withSize(9, ItemStack.EMPTY);
            return craftingRecipe.assemble(CraftingInput.of(3, 3, emptyGrid));
        }

        if (recipe.value() instanceof AbstractCookingRecipe cookingRecipe) {
            return cookingRecipe.assemble(new SingleRecipeInput(ItemStack.EMPTY));
        }

        if (recipe.value() instanceof SingleItemRecipe singleItemRecipe) {
            return singleItemRecipe.assemble(new SingleRecipeInput(ItemStack.EMPTY));
        }

        return ItemStack.EMPTY;
    }

    private static boolean hasEmeralds(ServerPlayer player, int amount) {
        int found = 0;

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);

            if (!stack.is(Items.EMERALD)) {
                continue;
            }

            found += stack.getCount();

            if (found >= amount) {
                return true;
            }
        }

        return false;
    }

    private static void takeEmeralds(ServerPlayer player, int amount) {
        int remaining = amount;

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);

            if (!stack.is(Items.EMERALD)) {
                continue;
            }

            int taken = Math.min(stack.getCount(), remaining);
            stack.shrink(taken);
            remaining -= taken;

            if (remaining <= 0) {
                player.getInventory().setChanged();
                return;
            }
        }
    }

    private static void actionBar(ServerPlayer player, String message) {
        player.sendSystemMessage(Component.literal(message), true);
    }

    private static String emeraldText(int amount) {
        return amount == 1 ? "1 emerald" : amount + " emeralds";
    }
}