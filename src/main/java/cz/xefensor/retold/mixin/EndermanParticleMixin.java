package cz.xefensor.retold.mixin;

import cz.xefensor.retold.client.stage.RetoldClientStage;
import cz.xefensor.retold.client.render.RetoldEndermanParticleColor;
import cz.xefensor.retold.stage.RetoldWorldStage;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EnderMan.class)
public abstract class EndermanParticleMixin {
    @Redirect(
            method = "aiStep",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"
            )
    )
    private void retold$markGreenEndermanPortalParticle(
            Level level,
            ParticleOptions particle,
            double x,
            double y,
            double z,
            double xd,
            double yd,
            double zd
    ) {
        if (RetoldClientStage.getStage() != RetoldWorldStage.STAGE_3) {
            level.addParticle(particle, x, y, z, xd, yd, zd);
            return;
        }

        RetoldEndermanParticleColor.beginGreenEndermanPortal();

        try {
            level.addParticle(particle, x, y, z, xd, yd, zd);
        } finally {
            RetoldEndermanParticleColor.endGreenEndermanPortal();
        }
    }
}
