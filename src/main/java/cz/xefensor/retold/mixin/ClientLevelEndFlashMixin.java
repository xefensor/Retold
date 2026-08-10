package cz.xefensor.retold.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.EndFlashState;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class ClientLevelEndFlashMixin {
    @Shadow
    @Final
    @Mutable
    private @Nullable EndFlashState endFlashState;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void retold$disableVanillaEndFlashes(CallbackInfo ci) {
        ClientLevel level = (ClientLevel) (Object) this;
        if (Level.END.equals(level.dimension())) {
            endFlashState = null;
        }
    }
}
