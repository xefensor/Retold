package cz.xefensor.retold.mixin;

import cz.xefensor.retold.recipe.RetoldKnownRecipeData;
import net.minecraft.network.protocol.game.ClientboundRecipeBookAddPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.ServerRecipeBook;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Mixin(ServerRecipeBook.class)
public abstract class ServerRecipeBookMixin {
    @Shadow
    @Final
    private ServerRecipeBook.DisplayResolver displayResolver;

    @Shadow
    public abstract void add(ResourceKey<Recipe<?>> id);

    @Shadow
    public abstract boolean contains(ResourceKey<Recipe<?>> id);

    @Shadow
    public abstract void removeHighlight(ResourceKey<Recipe<?>> id);

    @Inject(method = "addRecipes", at = @At("HEAD"), cancellable = true)
    private void retold$onlyUnlockActuallyCraftedRecipes(
            Collection<RecipeHolder<?>> recipes,
            ServerPlayer player,
            CallbackInfoReturnable<Integer> cir
    ) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            cir.setReturnValue(0);
            return;
        }

        RetoldKnownRecipeData data = RetoldKnownRecipeData.get(serverLevel);

        List<ClientboundRecipeBookAddPacket.Entry> entries = new ArrayList<>();
        int added = 0;

        for (RecipeHolder<?> recipe : recipes) {
            ResourceKey<Recipe<?>> recipeId = recipe.id();

            if (!data.hasKnown(player, recipeId)) {
                continue;
            }

            if (contains(recipeId)) {
                continue;
            }

            add(recipeId);
            removeHighlight(recipeId);
            added++;

            displayResolver.displaysForRecipe(recipeId, display -> {
                entries.add(new ClientboundRecipeBookAddPacket.Entry(
                        display,
                        false, // notification
                        false  // highlight
                ));
            });
        }

        if (!entries.isEmpty()) {
            player.connection.send(new ClientboundRecipeBookAddPacket(entries, false));
        }

        cir.setReturnValue(added);
    }
}