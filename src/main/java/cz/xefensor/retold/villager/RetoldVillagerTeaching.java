package cz.xefensor.retold.villager;

import cz.xefensor.retold.mixin.MerchantMenuAccessor;
import cz.xefensor.retold.network.RetoldTeachingPreviewPayload;
import cz.xefensor.retold.recipe.RetoldKnownRecipeData;
import cz.xefensor.retold.recipe.RetoldRecipeBookEvents;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.trading.Merchant;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Optional;

public final class RetoldVillagerTeaching {
    private RetoldVillagerTeaching() {
    }

    private record TeachingPreview(
            boolean active,
            String buttonLabel,
            String status,
            String cost,
            String tooltip,
            RecipeHolder<?> recipe,
            int emeraldCost
    ) {
    }

    public static void tryTeachHeldItemRecipe(ServerPlayer player) {
        TeachingPreview preview = createTeachingPreview(player);

        if (!preview.active()) {
            actionBar(player, preview.tooltip());
            sendPreviewToClient(player);
            return;
        }

        takeEmeralds(player, preview.emeraldCost());
        RetoldRecipeBookEvents.markKnownAndUnlockRecipe(player, preview.recipe());

        actionBar(player, "Learned recipe.");
        sendPreviewToClient(player);
    }

    public static void sendPreviewToClient(ServerPlayer player) {
        TeachingPreview preview = createTeachingPreview(player);

        PacketDistributor.sendToPlayer(
                player,
                new RetoldTeachingPreviewPayload(
                        preview.active(),
                        preview.buttonLabel(),
                        preview.status(),
                        preview.cost(),
                        preview.tooltip()
                )
        );
    }

    private static TeachingPreview createTeachingPreview(ServerPlayer player) {
        if (!(player.containerMenu instanceof MerchantMenu merchantMenu)) {
            return preview(
                    false,
                    "Status: Talk to a villager",
                    "Cost: -",
                    "You need to talk to a villager."
            );
        }

        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return preview(
                    false,
                    "Status: Server only",
                    "Cost: -",
                    "This can only be used on the server."
            );
        }

        Merchant merchant = ((MerchantMenuAccessor) merchantMenu).retold$getTrader();

        if (!(merchant instanceof Villager villager)) {
            return preview(
                    false,
                    "Status: Not a villager",
                    "Cost: -",
                    "Only villagers can teach recipes."
            );
        }

        ItemStack shownItem = getShownItem(player);

        if (shownItem.isEmpty()) {
            return preview(
                    false,
                    "Status: Place item into slot",
                    "Cost: -",
                    "Place the item you want to learn a recipe for into the teaching slot."
            );
        }

        Identifier professionId = villager.getVillagerData().profession()
                .unwrapKey()
                .map(key -> key.identifier())
                .orElse(null);

        if (professionId == null) {
            return preview(
                    false,
                    "Status: Unknown profession",
                    "Cost: -",
                    "This villager has no known profession."
            );
        }

        Optional<RetoldVillagerTeachingEntry> teachingEntryOptional =
                RetoldVillagerTeachingReloadListener.get(professionId);

        if (teachingEntryOptional.isEmpty()) {
            return preview(
                    false,
                    "Status: Cannot teach recipes",
                    "Cost: -",
                    "This villager cannot teach recipes."
            );
        }

        RetoldVillagerTeachingEntry teachingEntry = teachingEntryOptional.get();

        Optional<RecipeHolder<?>> recipeOptional = findTeachableRecipeByResult(
                player,
                shownItem,
                teachingEntry
        );

        if (recipeOptional.isEmpty()) {
            return preview(
                    false,
                    "Status: Villager does not know this",
                    "Cost: -",
                    "This villager does not know this recipe."
            );
        }

        RecipeHolder<?> recipe = recipeOptional.get();
        Identifier recipeId = recipe.id().identifier();

        int emeraldCost = teachingEntry.emeraldCostFor(recipeId);

        if (emeraldCost < 0) {
            return preview(
                    false,
                    "Status: Villager does not know this",
                    "Cost: -",
                    "This villager does not know this recipe."
            );
        }

        RetoldKnownRecipeData data = RetoldKnownRecipeData.get(serverLevel);

        if (data.hasKnown(player, recipe.id())) {
            return new TeachingPreview(
                    false,
                    "Learn",
                    "Status: Already known",
                    "Cost: " + emeraldText(emeraldCost),
                    "You already know this recipe.",
                    recipe,
                    emeraldCost
            );
        }

        if (!hasEmeralds(player, emeraldCost)) {
            return new TeachingPreview(
                    false,
                    "Learn",
                    "Status: Not enough emeralds",
                    "Cost: " + emeraldText(emeraldCost),
                    "You need " + emeraldText(emeraldCost) + ".",
                    recipe,
                    emeraldCost
            );
        }

        return new TeachingPreview(
                true,
                "Learn",
                "Status: Can learn " + shownItem.getHoverName().getString(),
                "Cost: " + emeraldText(emeraldCost),
                "Pay " + emeraldText(emeraldCost) + " to learn this recipe.",
                recipe,
                emeraldCost
        );
    }

    private static TeachingPreview preview(
            boolean active,
            String status,
            String cost,
            String tooltip
    ) {
        return new TeachingPreview(
                active,
                "Learn",
                status,
                cost,
                tooltip,
                null,
                -1
        );
    }

    private static ItemStack getShownItem(ServerPlayer player) {
        if (player.containerMenu instanceof RetoldTeachingSlotMenu teachingSlotMenu) {
            return teachingSlotMenu.retold$getTeachingItem();
        }

        return ItemStack.EMPTY;
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

    private static String emeraldText(int amount) {
        return amount == 1 ? "1 emerald" : amount + " emeralds";
    }

    private static void actionBar(ServerPlayer player, String message) {
        player.sendSystemMessage(Component.literal(message), true);
    }
}