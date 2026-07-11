package cz.xefensor.retold.mixin;

import cz.xefensor.retold.aender.RetoldAenderDimensions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.material.WaterFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WaterFluid.class)
public abstract class AenderWaterFluidMixin {
    private static final int AENDER_WATER_SLOPE_FIND_DISTANCE = 4;
    private static final int AENDER_WATER_TICK_DELAY = 3;

    @Inject(method = "getSlopeFindDistance", at = @At("HEAD"), cancellable = true)
    private void retold$aenderWaterReachesFarther(
            LevelReader level,
            CallbackInfoReturnable<Integer> cir
    ) {
        if (retold$isAender(level)) {
            cir.setReturnValue(AENDER_WATER_SLOPE_FIND_DISTANCE);
        }
    }

    @Inject(method = "getTickDelay", at = @At("HEAD"), cancellable = true)
    private void retold$aenderWaterFlowsFaster(
            LevelReader level,
            CallbackInfoReturnable<Integer> cir
    ) {
        if (retold$isAender(level)) {
            cir.setReturnValue(AENDER_WATER_TICK_DELAY);
        }
    }

    private static boolean retold$isAender(LevelReader level) {
        return level instanceof Level concreteLevel
                && concreteLevel.dimension() == RetoldAenderDimensions.AENDER;
    }
}
