package cz.xefensor.retold.mixin;

import cz.xefensor.retold.behavior.control.RetoldAiControl;
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.entity.monster.cubemob.AbstractCubeMob$CubeMobRandomDirectionGoal")
public abstract class CubeMobRandomDirectionGoalMixin {
    @Shadow
    @Final
    private AbstractCubeMob cubeMob;

    @Inject(
            method = "canUse",
            at = @At("HEAD"),
            cancellable = true
    )
    private void retold$preserveOwnedMovementDirection(
            CallbackInfoReturnable<Boolean> cir
    ) {
        // Cube Mobs ignore path coordinates. Retold drives their controller heading directly,
        // so vanilla wandering must not replace that heading while Retold owns movement.
        if (RetoldAiControl.shouldBlockVanillaNavigation(this.cubeMob)) {
            cir.setReturnValue(false);
        }
    }
}
