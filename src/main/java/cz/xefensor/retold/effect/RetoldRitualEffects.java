package cz.xefensor.retold.effect;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public final class RetoldRitualEffects {
    private RetoldRitualEffects() {
    }

    public static void playDragonEggStage3Ritual(ServerLevel level, BlockPos pos) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.8;
        double z = pos.getZ() + 0.5;

        level.playSound(
                null,
                pos,
                SoundEvents.END_PORTAL_SPAWN,
                SoundSource.BLOCKS,
                1.5F,
                1.0F
        );

        level.playSound(
                null,
                pos,
                SoundEvents.BEACON_ACTIVATE,
                SoundSource.BLOCKS,
                1.0F,
                0.7F
        );

        level.sendParticles(
                ParticleTypes.PORTAL,
                x,
                y,
                z,
                120,
                1.0,
                1.0,
                1.0,
                0.25
        );

        level.sendParticles(
                ParticleTypes.END_ROD,
                x,
                y + 0.4,
                z,
                60,
                0.6,
                0.8,
                0.6,
                0.05
        );

        level.sendParticles(
                ParticleTypes.ENCHANT,
                x,
                y,
                z,
                80,
                1.2,
                0.7,
                1.2,
                0.4
        );
    }
}