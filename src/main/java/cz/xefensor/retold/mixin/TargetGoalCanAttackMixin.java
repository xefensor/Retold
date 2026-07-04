package cz.xefensor.retold.mixin;

import cz.xefensor.retold.event.RetoldFactionTargetGuards;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TargetGoal.class)
public abstract class TargetGoalCanAttackMixin {
    @Shadow
    @Final
    protected Mob mob;

    @Inject(
            method = "canAttack(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/ai/targeting/TargetingConditions;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void retold$blockWarningProtectedTargetGoalCheck(
            LivingEntity target,
            TargetingConditions targetingConditions,
            CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        if (target == null) {
            return;
        }

        if (RetoldFactionTargetGuards.shouldBlockTarget(this.mob, target)) {
            callbackInfo.setReturnValue(false);
        }
    }
}