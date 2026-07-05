package cz.xefensor.retold.territory;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.PathfinderMob;

public final class RetoldWarningEffects {
    private RetoldWarningEffects() {
    }

    public static void playWarningEffects(
            ServerLevel level,
            PathfinderMob mob,
            RetoldTerritoryConfig config,
            RetoldWarningLevel warningLevel
    ) {
        int intensity = getWarningIntensity(warningLevel);

        int particleCount = RetoldTerritoryConstants.BASE_WARNING_PARTICLE_COUNT
                + intensity * RetoldTerritoryConstants.WARNING_PARTICLE_COUNT_PER_INTENSITY;

        float volume = RetoldTerritoryConstants.BASE_WARNING_SOUND_VOLUME
                + intensity * RetoldTerritoryConstants.WARNING_SOUND_VOLUME_PER_INTENSITY;

        level.sendParticles(
                ParticleTypes.ANGRY_VILLAGER,
                mob.getX(),
                mob.getEyeY() + RetoldTerritoryConstants.WARNING_PARTICLE_EYE_OFFSET_Y,
                mob.getZ(),
                particleCount,
                RetoldTerritoryConstants.WARNING_PARTICLE_SPREAD_X,
                RetoldTerritoryConstants.WARNING_PARTICLE_SPREAD_Y,
                RetoldTerritoryConstants.WARNING_PARTICLE_SPREAD_Z,
                RetoldTerritoryConstants.WARNING_PARTICLE_SPEED
        );

        level.playSound(
                null,
                mob.blockPosition(),
                config.warningSound,
                SoundSource.HOSTILE,
                volume,
                RetoldTerritoryConstants.WARNING_SOUND_PITCH
        );
    }

    private static int getWarningIntensity(RetoldWarningLevel warningLevel) {
        return switch (warningLevel) {
            case NONE -> 0;
            case NOTICED -> 1;
            case WARNING -> 2;
            case FINAL_WARNING -> 4;
            case ATTACK -> 5;
        };
    }
}