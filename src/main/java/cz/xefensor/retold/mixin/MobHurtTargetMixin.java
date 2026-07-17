package cz.xefensor.retold.mixin;

import cz.xefensor.retold.behavior.hunting.RetoldPredatorAttackGuards;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class MobHurtTargetMixin {
    @Inject(
            method = "doHurtTarget(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void retold$blockVanillaPredatorHurt(
            ServerLevel level,
            Entity target,
            CallbackInfoReturnable<Boolean> cir
    ) {
        Mob attacker = (Mob) (Object) this;

        if (!(target instanceof LivingEntity livingTarget)) {
            return;
        }

        if (
                RetoldPredatorAttackGuards.shouldBlockVanillaPredatorHurt(
                        attacker,
                        livingTarget
                )
        ) {
            cir.setReturnValue(false);
        }
    }
}