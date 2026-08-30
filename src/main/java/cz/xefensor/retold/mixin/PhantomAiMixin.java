package cz.xefensor.retold.mixin;

import cz.xefensor.retold.behavior.species.RetoldPhantomStalkerEvents;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Phantom.class)
public abstract class PhantomAiMixin {
    @Inject(
            method = "travel(Lnet/minecraft/world/phys/Vec3;)V",
            at = @At("HEAD")
    )
    private void retold$applyOwnedSwoopFlight(
            Vec3 input,
            CallbackInfo ci
    ) {
        Phantom phantom = (Phantom) (Object) this;

        if (phantom.level() instanceof ServerLevel level) {
            RetoldPhantomStalkerEvents.applyOwnedAttackFlight(
                    level,
                    phantom
            );
        }
    }
}
