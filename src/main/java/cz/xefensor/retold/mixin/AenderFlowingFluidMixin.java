package cz.xefensor.retold.mixin;

import cz.xefensor.retold.aender.RetoldAenderDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.WaterFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FlowingFluid.class)
public abstract class AenderFlowingFluidMixin {
    @Shadow
    public abstract FluidState getFlowing(int amount, boolean falling);

    @Inject(method = "getNewLiquid", at = @At("RETURN"), cancellable = true)
    private void retold$aenderWaterKeepsSomeHorizontalStrength(
            ServerLevel level,
            BlockPos pos,
            BlockState state,
            CallbackInfoReturnable<FluidState> cir
    ) {
        if (!((Object) this instanceof WaterFluid)
                || level.dimension() != RetoldAenderDimensions.AENDER
                || ((pos.getX() + pos.getZ()) & 1) == 0) {
            return;
        }

        FluidState result = cir.getReturnValue();

        if (result.isEmpty() || result.isSource() || result.getValue(FlowingFluid.FALLING)) {
            return;
        }

        int amount = result.getAmount();

        if (amount > 1 && amount < 7) {
            cir.setReturnValue(this.getFlowing(amount + 1, false));
        }
    }
}
