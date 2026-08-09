package cz.xefensor.retold.villager;

import cz.xefensor.retold.mixin.MerchantMenuAccessor;
import cz.xefensor.retold.mixin.VillagerInvoker;
import cz.xefensor.retold.network.RetoldTeachingPreviewPayload;
import cz.xefensor.retold.recipe.RetoldKnownRecipeData;
import cz.xefensor.retold.recipe.RetoldRecipeBookEvents;
import cz.xefensor.retold.recipe.RetoldRecipeResultHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundMerchantOffersPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.trading.Merchant;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Optional;

public final class RetoldVillagerTeaching {
    private RetoldVillagerTeaching() {
    }

    public static void tryTeachHeldItemRecipe(ServerPlayer player) {
        RetoldTeachingPreviewPayload.Feedback feedback = teachHeldItemRecipe(player);
        sendPreviewToClient(player, feedback);
    }

    static RetoldTeachingPreviewPayload.Feedback teachHeldItemRecipe(
            ServerPlayer player
    ) {
        TeachingPreview preview = createTeachingPreview(player);

        if (!preview.active()) {
            actionBar(player, preview.tooltip());
            return RetoldTeachingPreviewPayload.Feedback.REJECTED;
        }

        takeEmeralds(player, preview.emeraldCost());
        RetoldRecipeBookEvents.markKnownAndUnlockRecipe(player, preview.recipe());

        if (player.level() instanceof ServerLevel serverLevel) {
            rewardVillagerTeachingXp(serverLevel, preview.villager(), preview.villagerXpReward());
            syncOpenMerchantMenu(player, preview.villager());
        }

        actionBar(player, Component.translatable("message.retold.teaching.learned"));
        return RetoldTeachingPreviewPayload.Feedback.SUCCESS;
    }

    public static void sendPreviewToClient(ServerPlayer player) {
        sendPreviewToClient(player, RetoldTeachingPreviewPayload.Feedback.NONE);
    }

    private static void sendPreviewToClient(
            ServerPlayer player,
            RetoldTeachingPreviewPayload.Feedback feedback
    ) {
        if (player.connection == null) {
            return;
        }

        TeachingPreview preview = createTeachingPreview(player);

        PacketDistributor.sendToPlayer(
                player,
                new RetoldTeachingPreviewPayload(
                        preview.active(),
                        preview.buttonLabel(),
                        preview.status(),
                        preview.cost(),
                        preview.tooltip(),
                        feedback
                )
        );
    }

    static TeachingPreview createTeachingPreview(ServerPlayer player) {
        if (!(player.containerMenu instanceof MerchantMenu merchantMenu)) {
            return preview(
                    false,
                    Component.translatable("container.retold.teaching.status.talk_to_villager"),
                    Component.translatable("container.retold.teaching.cost.none"),
                    Component.translatable("container.retold.teaching.tooltip.talk_to_villager")
            );
        }

        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return preview(
                    false,
                    Component.translatable("container.retold.teaching.status.server_only"),
                    Component.translatable("container.retold.teaching.cost.none"),
                    Component.translatable("container.retold.teaching.tooltip.server_only")
            );
        }

        Merchant merchant = ((MerchantMenuAccessor) merchantMenu).retold$getTrader();

        if (!(merchant instanceof Villager villager)) {
            return preview(
                    false,
                    Component.translatable("container.retold.teaching.status.not_villager"),
                    Component.translatable("container.retold.teaching.cost.none"),
                    Component.translatable("container.retold.teaching.tooltip.not_villager")
            );
        }

        ItemStack shownItem = getShownItem(player);

        if (shownItem.isEmpty()) {
            return preview(
                    false,
                    Component.translatable("container.retold.teaching.status.place_item"),
                    Component.translatable("container.retold.teaching.cost.none"),
                    Component.translatable("container.retold.teaching.tooltip.place_item")
            );
        }

        Identifier professionId = villager.getVillagerData().profession()
                .unwrapKey()
                .map(key -> key.identifier())
                .orElse(null);

        if (professionId == null) {
            return preview(
                    false,
                    Component.translatable("container.retold.teaching.status.unknown_profession"),
                    Component.translatable("container.retold.teaching.cost.none"),
                    Component.translatable("container.retold.teaching.tooltip.unknown_profession")
            );
        }

        Optional<RetoldVillagerTeachingEntry> teachingEntryOptional =
                RetoldVillagerTeachingReloadListener.get(professionId);

        if (teachingEntryOptional.isEmpty()) {
            return preview(
                    false,
                    Component.translatable("container.retold.teaching.status.cannot_teach"),
                    Component.translatable("container.retold.teaching.cost.none"),
                    Component.translatable("container.retold.teaching.tooltip.cannot_teach")
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
                    Component.translatable("container.retold.teaching.status.does_not_know"),
                    Component.translatable("container.retold.teaching.cost.none"),
                    Component.translatable("container.retold.teaching.tooltip.does_not_know")
            );
        }

        RecipeHolder<?> recipe = recipeOptional.get();
        Identifier recipeId = recipe.id().identifier();

        int emeraldCost = teachingEntry.emeraldCostFor(recipeId);

        if (emeraldCost < 0) {
            return preview(
                    false,
                    Component.translatable("container.retold.teaching.status.does_not_know"),
                    Component.translatable("container.retold.teaching.cost.none"),
                    Component.translatable("container.retold.teaching.tooltip.does_not_know")
            );
        }

        int villagerXpReward = teachingEntry.villagerXpRewardFor(recipeId, emeraldCost);

        RetoldKnownRecipeData data = RetoldKnownRecipeData.get(serverLevel);

        if (data.hasKnown(player, recipe.id())) {
            return new TeachingPreview(
                    false,
                    Component.translatable("container.retold.teaching.learn"),
                    Component.translatable("container.retold.teaching.status.already_known"),
                    costText(emeraldCost),
                    Component.translatable("container.retold.teaching.tooltip.already_known"),
                    recipe,
                    emeraldCost,
                    villager,
                    villagerXpReward
            );
        }

        if (!hasEmeralds(player, emeraldCost)) {
            return new TeachingPreview(
                    false,
                    Component.translatable("container.retold.teaching.learn"),
                    Component.translatable("container.retold.teaching.status.not_enough_emeralds"),
                    costText(emeraldCost),
                    Component.translatable(
                            "container.retold.teaching.tooltip.not_enough_emeralds",
                            emeraldText(emeraldCost)
                    ),
                    recipe,
                    emeraldCost,
                    villager,
                    villagerXpReward
            );
        }

        return new TeachingPreview(
                true,
                Component.translatable("container.retold.teaching.learn"),
                Component.translatable(
                        "container.retold.teaching.status.can_learn",
                        shownItem.getHoverName()
                ),
                costText(emeraldCost),
                Component.translatable(
                        "container.retold.teaching.tooltip.pay",
                        emeraldText(emeraldCost)
                ),
                recipe,
                emeraldCost,
                villager,
                villagerXpReward
        );
    }

    private static TeachingPreview preview(
            boolean active,
            Component status,
            Component cost,
            Component tooltip
    ) {
        return new TeachingPreview(
                active,
                Component.translatable("container.retold.teaching.learn"),
                status,
                cost,
                tooltip,
                null,
                -1,
                null,
                0
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

        Optional<RecipeHolder<?>> exactItemIdRecipe =
                findConfiguredRecipeByShownItemId(server, shownItem, teachingEntry);

        if (exactItemIdRecipe.isPresent()) {
            return exactItemIdRecipe;
        }

        for (RecipeHolder<?> recipe : server.getRecipeManager().getRecipes()) {
            Identifier recipeId = recipe.id().identifier();

            if (teachingEntry.emeraldCostFor(recipeId) < 0) {
                continue;
            }

            if (!RetoldRecipeResultHelper.hasSameResultWithoutCraftingGuess(
                    recipe,
                    shownItem
            )) {
                continue;
            }

            return Optional.of(recipe);
        }

        return Optional.empty();
    }

    private static Optional<RecipeHolder<?>> findConfiguredRecipeByShownItemId(
            MinecraftServer server,
            ItemStack shownItem,
            RetoldVillagerTeachingEntry teachingEntry
    ) {
        Identifier shownItemId =
                BuiltInRegistries.ITEM.getKey(shownItem.getItem());

        for (RetoldVillagerTeachingEntry.TeachableRecipe teachableRecipe
                : teachingEntry.recipes()) {
            if (!teachableRecipe.id().equals(shownItemId)) {
                continue;
            }

            for (RecipeHolder<?> recipe : server.getRecipeManager().getRecipes()) {
                if (!recipe.id().identifier().equals(teachableRecipe.id())) {
                    continue;
                }

                return Optional.of(recipe);
            }
        }

        return Optional.empty();
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

    private static Component costText(int amount) {
        return Component.translatable(
                "container.retold.teaching.cost",
                emeraldText(amount)
        );
    }

    private static Component emeraldText(int amount) {
        if (amount == 1) {
            return Component.translatable("container.retold.teaching.emerald.one");
        }
        return Component.translatable(
                "container.retold.teaching.emerald.many",
                amount
        );
    }

    private static void actionBar(ServerPlayer player, Component message) {
        if (player.connection != null) {
            player.sendSystemMessage(message, true);
        }
    }

    private static void rewardVillagerTeachingXp(ServerLevel serverLevel, Villager villager, int amount) {
        if (villager == null || amount <= 0) {
            return;
        }

        villager.setVillagerXp(villager.getVillagerXp() + amount);

        while (canVillagerLevelUp(villager)) {
            int currentLevel = villager.getVillagerData().level();

            villager.setVillagerData(
                    villager.getVillagerData().withLevel(currentLevel + 1)
            );

            ((VillagerInvoker) villager).retold$updateTrades(serverLevel);
        }
    }

    private static boolean canVillagerLevelUp(Villager villager) {
        int currentLevel = villager.getVillagerData().level();

        if (!VillagerData.canLevelUp(currentLevel)) {
            return false;
        }

        int nextLevel = currentLevel + 1;
        return villager.getVillagerXp() >= VillagerData.getMinXpPerLevel(nextLevel);
    }

    private static void syncOpenMerchantMenu(ServerPlayer player, Villager villager) {
        if (!(player.containerMenu instanceof MerchantMenu merchantMenu)) {
            return;
        }

        int villagerLevel = villager.getVillagerData().level();

        if (player.connection != null) {
            player.connection.send(new ClientboundMerchantOffersPacket(
                    merchantMenu.containerId,
                    villager.getOffers(),
                    villagerLevel,
                    villager.getVillagerXp(),
                    VillagerData.canLevelUp(villagerLevel),
                    true
            ));
        }

        merchantMenu.broadcastChanges();
    }

    record TeachingPreview(
            boolean active,
            Component buttonLabel,
            Component status,
            Component cost,
            Component tooltip,
            RecipeHolder<?> recipe,
            int emeraldCost,
            Villager villager,
            int villagerXpReward
    ) {
    }
}
