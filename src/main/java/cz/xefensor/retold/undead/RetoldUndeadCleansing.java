package cz.xefensor.retold.undead;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

public final class RetoldUndeadCleansing {
    private RetoldUndeadCleansing() {
    }

    public static void cleanse(Entity entity) {
        if (!(entity.level() instanceof ServerLevel serverLevel)) {
            entity.discard();
            return;
        }

        double x = entity.getX();
        double y = entity.getY() + entity.getBbHeight() * 0.5;
        double z = entity.getZ();

        serverLevel.sendParticles(
                ParticleTypes.SOUL,
                x,
                y,
                z,
                20,
                0.35,
                0.5,
                0.35,
                0.02
        );

        serverLevel.sendParticles(
                ParticleTypes.ENCHANT,
                x,
                y,
                z,
                25,
                0.45,
                0.6,
                0.45,
                0.15
        );

        serverLevel.playSound(
                null,
                entity.blockPosition(),
                SoundEvents.SOUL_ESCAPE.value(),
                SoundSource.HOSTILE,
                2.0F,
                1.2F
        );

        entity.discard();
    }
}