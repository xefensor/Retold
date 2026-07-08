package cz.xefensor.retold.mixin;

import cz.xefensor.retold.registry.RetoldGameRules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.players.SleepStatus;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class ServerLevelSleepMixin {
    @Shadow
    @Final
    private SleepStatus sleepStatus;

    @Inject(method = "updateSleepingPlayerList", at = @At("HEAD"), cancellable = true)
    private void retold$disableBedNightSkipping(CallbackInfo ci) {
        ServerLevel level = (ServerLevel) (Object) this;

        if (!RetoldGameRules.doBedNightSkipping(level)) {
            this.sleepStatus.removeAllSleepers();
            ci.cancel();
        }
    }
}