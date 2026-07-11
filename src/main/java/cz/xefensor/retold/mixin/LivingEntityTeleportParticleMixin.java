package cz.xefensor.retold.mixin;

import cz.xefensor.retold.client.render.RetoldEndermanParticleColor;
import cz.xefensor.retold.client.stage.RetoldClientStage;
import cz.xefensor.retold.stage.RetoldWorldStage;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntity.class)
public abstract class LivingEntityTeleportParticleMixin {
    @Redirect(
            method = "handleEntityEvent",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"
            )
    )
    private void retold$markGreenEndermanTeleportParticle(
            Level level,
            ParticleOptions particle,
            double x,
            double y,
            double z,
            double xd,
            double yd,
            double zd,
            byte id
    ) {
        if (!shouldColorTeleportParticle(particle, id)) {
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

    private boolean shouldColorTeleportParticle(ParticleOptions particle, byte id) {
        return id == 46
                && particle == ParticleTypes.PORTAL
                && (Object) this instanceof EnderMan
                && RetoldClientStage.getStage() == RetoldWorldStage.STAGE_3;
    }
}
