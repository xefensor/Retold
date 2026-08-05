package cz.xefensor.retold.mixin;

import cz.xefensor.retold.behavior.species.RetoldBatColonyEvents;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ambient.Bat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Bat.class)
public abstract class BatAiMixin {
    @Unique
    private boolean retold$wasRestingBeforeVanillaAi;

    @Unique
    private boolean retold$ownedFlightApplied;

    @Inject(
            method = "customServerAiStep(Lnet/minecraft/server/level/ServerLevel;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void retold$applyOwnedBatFlight(
            ServerLevel level,
            CallbackInfo ci
    ) {
        Bat bat = (Bat) (Object) this;
        this.retold$wasRestingBeforeVanillaAi = bat.isResting();
        this.retold$ownedFlightApplied = RetoldBatColonyEvents.applyOwnedFlightStep(
                level,
                bat
        );

        if (this.retold$ownedFlightApplied) {
            ci.cancel();
        }
    }

    @Inject(
            method = "customServerAiStep(Lnet/minecraft/server/level/ServerLevel;)V",
            at = @At("TAIL")
    )
    private void retold$applyDaytimeRoostBias(
            ServerLevel level,
            CallbackInfo ci
    ) {
        Bat bat = (Bat) (Object) this;

        if (!this.retold$ownedFlightApplied
                && this.retold$wasRestingBeforeVanillaAi
                && !bat.isResting()) {
            RetoldBatColonyEvents.onVanillaRoostDisturbed(level, bat);
        }

        RetoldBatColonyEvents.applyDaytimeRoostBiasStep(
                level,
                bat
        );
    }
}
