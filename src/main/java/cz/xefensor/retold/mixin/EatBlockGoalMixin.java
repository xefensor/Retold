package cz.xefensor.retold.mixin;

import cz.xefensor.retold.behavior.core.RetoldBehaviorCoordinator;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.EatBlockGoal;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vanilla's EatBlockGoal does not know about Retold movement ownership. Without
 * this guard a Sheep whose grazing animation had already started could keep the
 * goal alive while Retold made it flee from a predator.
 */
@Mixin(EatBlockGoal.class)
public abstract class EatBlockGoalMixin {
    @Shadow
    @Final
    private Mob mob;

    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true)
    private void retold$blockStartingMealDuringUrgentBehavior(
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!RetoldBehaviorCoordinator.canFeedNow(this.mob)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "canContinueToUse", at = @At("HEAD"), cancellable = true)
    private void retold$interruptMealForUrgentBehavior(
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!RetoldBehaviorCoordinator.canFeedNow(this.mob)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void retold$preventLateMealTransaction(CallbackInfo ci) {
        if (!RetoldBehaviorCoordinator.canCompleteMeal(this.mob)) {
            ci.cancel();
        }
    }
}
