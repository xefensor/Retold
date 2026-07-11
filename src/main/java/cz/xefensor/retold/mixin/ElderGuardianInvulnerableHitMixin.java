package cz.xefensor.retold.mixin;

import cz.xefensor.retold.event.RetoldElderGuardianBoss;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.ElderGuardian;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class ElderGuardianInvulnerableHitMixin {
    @Inject(method = "hurtServer", at = @At("HEAD"))
    private void retold$playElderGuardianBlockedFeedbackDuringInvulnerability(
            ServerLevel level,
            DamageSource damageSource,
            float amount,
            CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        LivingEntity self = (LivingEntity) (Object) this;

        if (!(self instanceof ElderGuardian elderGuardian)) {
            return;
        }

        if (elderGuardian.invulnerableTime <= 10) {
            return;
        }

        RetoldElderGuardianBoss.onInvulnerableHitAttempt(
                elderGuardian,
                damageSource.getEntity(),
                damageSource.getDirectEntity()
        );
    }
}
