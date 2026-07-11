package cz.xefensor.retold.mixin;

import cz.xefensor.retold.client.render.RetoldEndermanParticleColor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.PortalParticle;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PortalParticle.class)
public abstract class PortalParticleMixin extends SingleQuadParticle {
    private PortalParticleMixin(ClientLevel level, double x, double y, double z, TextureAtlasSprite sprite) {
        super(level, x, y, z, sprite);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void retold$colorGreenEndermanPortalParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            double xd,
            double yd,
            double zd,
            TextureAtlasSprite sprite,
            CallbackInfo ci
    ) {
        if (!RetoldEndermanParticleColor.isGreenEndermanPortal()) {
            return;
        }

        float brightness = 0.85F + this.random.nextFloat() * 0.15F;
        this.rCol = brightness * 0.53F;
        this.gCol = brightness;
        this.bCol = brightness * 0.48F;
    }
}
