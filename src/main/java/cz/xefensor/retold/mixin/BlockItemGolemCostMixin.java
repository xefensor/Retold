package cz.xefensor.retold.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import cz.xefensor.retold.golem.RetoldGolemAnimation;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class BlockItemGolemCostMixin {
    @Inject(
            method = "place",
            at = @At("HEAD"),
            cancellable = true
    )
    private void retold$beginGolemPumpkinPlacement(
            BlockPlaceContext context,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        BlockItem self = (BlockItem) (Object) this;

        if (self.getBlock() instanceof CarvedPumpkinBlock pumpkin
                && !RetoldGolemAnimation.beginPlayerPumpkinPlacement(
                context,
                pumpkin
        )) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    @WrapOperation(
            method = "place",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/BlockItem;placeBlock(Lnet/minecraft/world/item/context/BlockPlaceContext;Lnet/minecraft/world/level/block/state/BlockState;)Z"
            )
    )
    private boolean retold$chargeCompletedIronGolemPlacement(
            BlockItem item,
            BlockPlaceContext context,
            BlockState state,
            Operation<Boolean> original
    ) {
        boolean ironGolemBase = item.getBlock() instanceof CarvedPumpkinBlock pumpkin
                && RetoldGolemAnimation.isIronGolemPlacement(context, pumpkin);
        boolean placed = original.call(item, context, state);

        if (placed && ironGolemBase) {
            RetoldGolemAnimation.chargeSuccessfulPlayerPlacement(
                    context,
                    true
            );
        }

        return placed;
    }
}
