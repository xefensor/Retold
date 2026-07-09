package cz.xefensor.retold.chronolith;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public final class ChronolithFeedback {
    private ChronolithFeedback() {
    }

    public static void playStart(ServerLevel level, BlockPos pos) {
        playSound(level, pos, SoundEvents.BEACON_ACTIVATE, 0.7F, 1.45F);
        level.sendParticles(
                ParticleTypes.HAPPY_VILLAGER,
                pos.getX() + 0.5D,
                pos.getY() + 0.75D,
                pos.getZ() + 0.5D,
                8,
                0.25D,
                0.2D,
                0.25D,
                0.02D
        );
    }

    public static void playActivePulse(ServerLevel level, BlockPos pos, float power) {
        int count = Math.max(1, (int) (4 + power * 8));

        level.sendParticles(
                ParticleTypes.HAPPY_VILLAGER,
                pos.getX() + 0.5D,
                pos.getY() + 0.72D,
                pos.getZ() + 0.5D,
                count,
                0.22D + power * 0.12D,
                0.16D + power * 0.1D,
                0.22D + power * 0.12D,
                0.01D + power * 0.015D
        );

        level.sendParticles(
                ParticleTypes.REVERSE_PORTAL,
                pos.getX() + 0.5D,
                pos.getY() + 0.58D,
                pos.getZ() + 0.5D,
                Math.max(1, (int) (2 + power * 5)),
                0.18D,
                0.12D,
                0.18D,
                0.01D
        );
    }

    public static void playActiveSound(ServerLevel level, BlockPos pos) {
        playSound(level, pos, SoundEvents.AMETHYST_BLOCK_CHIME, 0.35F, 0.85F);
    }

    public static void playStop(ServerLevel level, BlockPos pos, ChronolithStopReason reason) {
        switch (reason) {
            case MANUAL -> playManualStop(level, pos);
            case BLOCK_REMOVED -> playBlockRemoved(level, pos);
            case OUT_OF_XP, INTERRUPTED -> playInterrupted(level, pos);
            case REPLACED -> playReplaced(level, pos);
            case PLAYER_LEFT, SERVER_STOP -> {
            }
        }
    }

    public static void playFailActivation(ServerLevel level, BlockPos pos) {
        playSound(level, pos, SoundEvents.BEACON_DEACTIVATE, 0.35F, 0.55F);
        sendFailureParticles(level, pos, 10, 8);
    }

    private static void playManualStop(ServerLevel level, BlockPos pos) {
        playSound(level, pos, SoundEvents.BEACON_DEACTIVATE, 0.55F, 0.9F);
        level.sendParticles(
                ParticleTypes.HAPPY_VILLAGER,
                pos.getX() + 0.5D,
                pos.getY() + 0.65D,
                pos.getZ() + 0.5D,
                5,
                0.2D,
                0.14D,
                0.2D,
                0.01D
        );
    }

    private static void playReplaced(ServerLevel level, BlockPos pos) {
        playSound(level, pos, SoundEvents.BEACON_DEACTIVATE, 0.45F, 1.05F);
        level.sendParticles(
                ParticleTypes.REVERSE_PORTAL,
                pos.getX() + 0.5D,
                pos.getY() + 0.65D,
                pos.getZ() + 0.5D,
                6,
                0.25D,
                0.16D,
                0.25D,
                0.015D
        );
    }

    private static void playInterrupted(ServerLevel level, BlockPos pos) {
        playSound(level, pos, SoundEvents.BEACON_DEACTIVATE, 0.6F, 0.55F);
        sendFailureParticles(level, pos, 8, 10);
    }

    private static void playBlockRemoved(ServerLevel level, BlockPos pos) {
        playSound(level, pos, SoundEvents.BEACON_DEACTIVATE, 0.65F, 0.7F);
        sendFailureParticles(level, pos, 12, 12);
    }

    private static void sendFailureParticles(ServerLevel level, BlockPos pos, int smokeCount, int portalCount) {
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.65D;
        double z = pos.getZ() + 0.5D;

        level.sendParticles(
                ParticleTypes.SMOKE,
                x,
                y,
                z,
                smokeCount,
                0.25D,
                0.18D,
                0.25D,
                0.015D
        );

        level.sendParticles(
                ParticleTypes.REVERSE_PORTAL,
                x,
                y,
                z,
                portalCount,
                0.3D,
                0.2D,
                0.3D,
                0.02D
        );
    }

    private static void playSound(ServerLevel level, BlockPos pos, SoundEvent sound, float volume, float pitch) {
        level.playSound(
                null,
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                sound,
                SoundSource.BLOCKS,
                volume,
                pitch
        );
    }
}
