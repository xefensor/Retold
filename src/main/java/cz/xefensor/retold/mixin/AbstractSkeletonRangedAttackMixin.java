package cz.xefensor.retold.mixin;

import cz.xefensor.retold.behavior.species.RetoldSkeletonRangedEvents;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSkeleton.class)
public abstract class AbstractSkeletonRangedAttackMixin {
    @Inject(
            method = "performRangedAttack(Lnet/minecraft/world/entity/LivingEntity;F)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void retold$holdFireForUndeadAlly(
            LivingEntity target,
            float power,
            CallbackInfo ci
    ) {
        AbstractSkeleton skeleton = (AbstractSkeleton) (Object) this;

        if (RetoldSkeletonRangedEvents.shouldHoldFire(skeleton, target)) {
            ci.cancel();
        }
    }
}
