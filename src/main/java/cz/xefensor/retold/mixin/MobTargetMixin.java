package cz.xefensor.retold.mixin;

import cz.xefensor.retold.combat.RetoldFactionTargetGuards;
import cz.xefensor.retold.combat.RetoldMobTargetPolicy;
import cz.xefensor.retold.worldgen.fire.WildfireEncounterTargets;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class MobTargetMixin {
    @Inject(method = "canAttack", at = @At("HEAD"), cancellable = true)
    private void retold$allowFactionEnemyGhastTarget(
            LivingEntity target,
            CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        Mob mob = (Mob) (Object) this;

        if (WildfireEncounterTargets.shouldBlockTarget(mob, target)) {
            callbackInfo.setReturnValue(false);
            return;
        }

        if (RetoldMobTargetPolicy.shouldAllowFactionGhastTarget(mob, target)) {
            callbackInfo.setReturnValue(true);
        }
    }

    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void retold$blockWarningProtectedTarget(
            LivingEntity target,
            CallbackInfo callbackInfo
    ) {
        if (target == null) {
            return;
        }

        Mob mob = (Mob) (Object) this;

        if (RetoldFactionTargetGuards.shouldBlockTarget(mob, target)) {
            callbackInfo.cancel();
        }
    }
}
