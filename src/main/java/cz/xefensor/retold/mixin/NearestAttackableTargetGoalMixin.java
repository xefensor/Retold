package cz.xefensor.retold.mixin;

import cz.xefensor.retold.event.RetoldFactionTargetGuards;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NearestAttackableTargetGoal.class)
public abstract class NearestAttackableTargetGoalMixin {
    @Shadow
    protected LivingEntity target;

    @Inject(method = "canUse", at = @At("RETURN"), cancellable = true)
    private void retold$blockWarningProtectedTargetGoal(CallbackInfoReturnable<Boolean> callbackInfo) {
        if (!callbackInfo.getReturnValue()) {
            return;
        }

        if (this.target == null) {
            return;
        }

        Mob mob = ((TargetGoalAccessor) this).retold$getMob();

        if (RetoldFactionTargetGuards.shouldBlockTarget(mob, this.target)) {
            this.target = null;
            callbackInfo.setReturnValue(false);
        }
    }
}