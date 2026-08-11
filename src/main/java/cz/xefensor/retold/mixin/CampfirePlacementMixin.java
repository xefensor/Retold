package cz.xefensor.retold.mixin;

import cz.xefensor.retold.progression.RetoldCampfireProgressionEvents;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CampfireBlock.class)
public abstract class CampfirePlacementMixin {
    @Inject(method = "getStateForPlacement", at = @At("RETURN"), cancellable = true)
    private void retold$placeCampfiresUnlit(
            BlockPlaceContext context,
            CallbackInfoReturnable<BlockState> callbackInfo
    ) {
        BlockState placementState = callbackInfo.getReturnValue();
        if (placementState != null) {
            callbackInfo.setReturnValue(
                    RetoldCampfireProgressionEvents.unlitPlacementState(
                            placementState
                    )
            );
        }
    }
}
