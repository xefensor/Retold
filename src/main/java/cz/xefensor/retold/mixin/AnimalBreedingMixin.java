package cz.xefensor.retold.mixin;

import cz.xefensor.retold.behavior.breeding.RetoldAnimalBreeding;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Animal.class)
public abstract class AnimalBreedingMixin {
    @Unique
    private ItemStack retold$pendingBreedingFood = ItemStack.EMPTY;

    @Inject(
            method = "mobInteract(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;",
            at = @At("HEAD")
    )
    private void retold$rememberBreedingFood(
            Player player,
            InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        Animal animal = (Animal) (Object) this;
        ItemStack held = player.getItemInHand(hand);

        retold$pendingBreedingFood = !animal.level().isClientSide()
                && RetoldAnimalBreeding.shouldReplacePlayerLove(animal)
                && animal.isFood(held)
                ? held.copyWithCount(1)
                : ItemStack.EMPTY;
    }

    @Inject(
            method = "mobInteract(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;",
            at = @At("RETURN")
    )
    private void retold$clearRememberedBreedingFood(
            Player player,
            InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        retold$pendingBreedingFood = ItemStack.EMPTY;
    }

    @Inject(
            method = "canFallInLove()Z",
            at = @At("RETURN"),
            cancellable = true
    )
    private void retold$requireActualHungerForPlayerFood(
            CallbackInfoReturnable<Boolean> cir
    ) {
        Animal animal = (Animal) (Object) this;

        if (cir.getReturnValue()
                && RetoldAnimalBreeding.shouldReplacePlayerLove(animal)) {
            cir.setReturnValue(
                    RetoldAnimalBreeding.canAcceptPlayerBreedingFood(animal)
            );
        }
    }

    @Inject(
            method = "setInLove(Lnet/minecraft/world/entity/player/Player;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void retold$replacePlayerLoveWithHunger(
            Player player,
            CallbackInfo ci
    ) {
        Animal animal = (Animal) (Object) this;

        if (player == null
                || !RetoldAnimalBreeding.shouldReplacePlayerLove(animal)) {
            return;
        }

        RetoldAnimalBreeding.onPlayerLoveAttempt(
                animal,
                retold$pendingBreedingFood
        );
        ci.cancel();
    }
}
