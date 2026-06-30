package cz.xefensor.retold.mixin;

import cz.xefensor.retold.recipe.RetoldRecipeBookEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityMixin {
    @Unique
    private static final ThreadLocal<ServerPlayer> retold$currentFurnaceRecipePlayer =
            new ThreadLocal<>();

    @Inject(
            method = "awardUsedRecipesAndPopExperience",
            at = @At("HEAD")
    )
    private void retold$rememberRecipeAwardPlayer(
            ServerPlayer player,
            CallbackInfo ci
    ) {
        retold$currentFurnaceRecipePlayer.set(player);
    }

    @Inject(
            method = "awardUsedRecipesAndPopExperience",
            at = @At("TAIL")
    )
    private void retold$forgetRecipeAwardPlayer(
            ServerPlayer player,
            CallbackInfo ci
    ) {
        retold$currentFurnaceRecipePlayer.remove();
    }

    @Inject(
            method = "getRecipesToAwardAndPopExperience",
            at = @At("RETURN")
    )
    private void retold$markExactFurnaceRecipesKnown(
            ServerLevel level,
            Vec3 position,
            CallbackInfoReturnable<List<RecipeHolder<?>>> cir
    ) {
        ServerPlayer player = retold$currentFurnaceRecipePlayer.get();

        if (player == null) {
            return;
        }

        if (player.level() != level) {
            return;
        }

        List<RecipeHolder<?>> recipes = cir.getReturnValue();

        if (recipes == null || recipes.isEmpty()) {
            return;
        }

        for (RecipeHolder<?> recipe : recipes) {
            RetoldRecipeBookEvents.markKnownRecipe(player, recipe);
        }
    }
}