package cz.xefensor.retold.mixin;

import cz.xefensor.retold.stage.RetoldRaidProgression;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.effect.BadOmenMobEffect")
public abstract class BadOmenMobEffectMixin {
    @Inject(
            method = "applyEffectTick",
            at = @At("HEAD"),
            cancellable = true
    )
    private void retold$keepBadOmenBeforeRaidStage(
            ServerLevel level,
            LivingEntity entity,
            int amplifier,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!RetoldRaidProgression.canStartRaid(level)) {
            cir.setReturnValue(true);
        }
    }
}
